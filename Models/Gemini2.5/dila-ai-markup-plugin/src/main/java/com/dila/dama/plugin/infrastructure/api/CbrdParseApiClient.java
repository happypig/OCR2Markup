package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.ParseError;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import com.dila.dama.plugin.util.PluginLogger;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls the DILA-hosted CBRD Parse endpoint (FR-001, FR-006).
 *
 * Uses {@code HttpURLConnection} via {@link HttpUrlConnectionFactory} to stay inside the Java 8
 * baseline the constitution mandates, mirroring {@code CBRDAPIClient}. Every failure — HTTP
 * status, unparseable body, or transport error — comes back as a {@link CbrdParseResponse}
 * rather than an exception, so the caller never has to guess.
 */
public class CbrdParseApiClient {

    // TODO(005): hardcoded because src/main/java is not resource-filtered, so ${project.version}
    // does not reach it. Replace with a build-generated constant; see the single-source-version
    // feature. Until then this must be bumped by hand alongside pom.xml.
    private static final String USER_AGENT = "DILA-AI-Markup/0.5.0";

    /**
     * Matches the canonical message emitted by
     * {@code sun.net.www.protocol.http.HttpURLConnection.getInputStream()} on a non-2xx
     * response: {@code "Server returned HTTP response code: NNN for URL: ..."}. Stable across
     * JDK 8 through 25; used by {@link #recoverStatusFromException} to recover the status when
     * {@code getErrorStream()} throws the message up to the caller.
     */
    private static final Pattern STATUS_FROM_EXCEPTION_MESSAGE =
        Pattern.compile("Server returned HTTP response code: (\\d{3}) for URL:");

    private final HttpUrlConnectionFactory connectionFactory;
    private final SecretRedactor redactor;

    public CbrdParseApiClient() {
        this(new HttpUrlConnectionFactory(), new SecretRedactor());
    }

    public CbrdParseApiClient(HttpUrlConnectionFactory connectionFactory) {
        this(connectionFactory, new SecretRedactor());
    }

    public CbrdParseApiClient(HttpUrlConnectionFactory connectionFactory, SecretRedactor redactor) {
        this.connectionFactory = connectionFactory;
        this.redactor = redactor;
    }

    public CbrdParseResponse execute(CbrdParseConfiguration configuration, CbrdParseRequest request) {
        String requestBody = request.toJson();
        RequestTraceSnapshot trace = new RequestTraceSnapshot(
            UUID.randomUUID().toString(),
            configuration.getEndpointSummary(),
            buildRequestMetadataSummary(configuration, request, requestBody)
        );

        HttpURLConnection connection = null;
        // priorStatus is captured outside the inner try/finally so that if a downstream call
        // (readFully on the error stream) throws, the catch block can still see which HTTP
        // status the server actually returned. Without this, Java-17's quirk of throwing
        // IOException("Server returned HTTP response code: 401 for URL: ...") from
        // getErrorStream() would silently down-grade a 401 to CONNECTIVITY_FAILURE.
        int priorStatus = -1;
        try {
            URL url = configuration.toRequestUri().toURL();
            connection = connectionFactory.openConnection(url);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(configuration.getTimeoutMs());
            connection.setReadTimeout(configuration.getTimeoutMs());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/xml");
            connection.setRequestProperty("Authorization", "Bearer " + configuration.getSharedToken());
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setDoOutput(true);

            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
                writer.write(requestBody);
                writer.flush();
            }

            int status = connection.getResponseCode();
            priorStatus = status;
            if (status >= 200 && status < 300) {
                return CbrdParseResponse.success(readFully(connection.getInputStream()), status, trace);
            }

            String errorBody;
            try {
                errorBody = readFully(connection.getErrorStream());
            } catch (RuntimeException e) {
                // Java 17 (and some other JDKs) surfaces 4xx/5xx responses with no readable
                // body as IOException wrapped through getErrorStream(): the canonical message
                // is "Server returned HTTP response code: NNN for URL: ...". Recover status
                // and classify from the status code instead of falling through to the generic
                // connectivity path. See spec.md Clarifications 2026-08-02 + US4 scenario 12.
                String causeBody =
                    "Java-17 getErrorStream threw IOException; status " + status
                        + " recovered from prior getResponseCode()";
                return classifyStatusKnownFailure(status, causeBody, trace, unwrapCause(e));
            }
            ParseError error = parseErrorCode(errorBody);
            PluginLogger.warn("[CbrdParseApiClient]Parse failed: status=" + status + ", error=" + error);
            return CbrdParseResponse.failure(status, error, redactor.redact(errorBody), trace, null);
        } catch (Exception e) {
            PluginLogger.error("[CbrdParseApiClient]Connection error: " + e.getMessage());
            int recovered = recoverStatusFromException(e, priorStatus);
            if (recovered >= 0) {
                String causeBody =
                    "Java-17 getErrorStream threw IOException; status " + recovered
                        + " recovered from exception message";
                return classifyStatusKnownFailure(recovered, causeBody, trace, unwrapCause(e));
            }
            return CbrdParseResponse.failure(null, ParseError.CONNECTIVITY_FAILURE, "", trace, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Classifies a response whose HTTP status is known but whose body was unreadable. The cause
     * string in {@code serviceErrorBody} is still meaningful so the diagnostics export
     * (FR-022) carries something other than "".
     */
    private CbrdParseResponse classifyStatusKnownFailure(
        int status, String fallbackCauseBody, RequestTraceSnapshot trace, Exception cause
    ) {
        ParseError error;
        switch (status) {
            case 400:
                // The contract enumerates three 400 causes; without the body we cannot tell
                // which one, so degrade to the generic-unexpected branch rather than guessing.
                error = ParseError.UNEXPECTED_RESPONSE;
                break;
            case 401:
                // The only 401 the contract enumerates is unauthorized (FR-011).
                error = ParseError.UNAUTHORIZED;
                break;
            case 422:
                error = ParseError.INVALID_MODEL_OUTPUT;
                break;
            case 502:
                error = ParseError.OPENAI_UNAVAILABLE;
                break;
            case 503:
                // Three 503 codes — without the body we cannot disambiguate, so surface the
                // generic "service unavailable, retry shortly" branch (UNKNOWN_SERVICE_FAILURE).
                error = ParseError.UNEXPECTED_RESPONSE;
                break;
            default:
                error = ParseError.UNEXPECTED_RESPONSE;
        }
        PluginLogger.warn(
            "[CbrdParseApiClient]Classified from status (body unavailable): status="
                + status + ", error=" + error
        );
        return CbrdParseResponse.failure(status, error, fallbackCauseBody, trace, cause);
    }

    /**
     * Returns the HTTP status when one is recoverable. Prefers a status already captured from
     * {@code getResponseCode()} (the {@code priorStatus} argument), and otherwise parses it out
     * of the canonical {@code "Server returned HTTP response code: NNN for URL: ..."} message
     * that {@code sun.net.www.protocol.http.HttpURLConnection.getInputStream()} emits on a
     * non-2xx response. Returns a negative value when no status is recoverable, signalling a
     * genuine transport-level failure (DNS / TLS / timeout) that should route to FR-013.
     */
    private int recoverStatusFromException(Throwable throwable, int priorStatus) {
        if (priorStatus > 0) {
            return priorStatus;
        }
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                Matcher matcher = STATUS_FROM_EXCEPTION_MESSAGE.matcher(message);
                if (matcher.find()) {
                    try {
                        return Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ignored) {
                        // fall through and keep unwrapping
                    }
                }
            }
            current = current.getCause();
        }
        return -1;
    }

    private static Exception unwrapCause(Exception e) {
        // The test seam wraps the IOException in a RuntimeException because
        // HttpURLConnection.getErrorStream() does not declare a checked throws clause; preserve
        // the underlying IOException on the returned response for parity with the real path.
        Throwable cause = e.getCause();
        return (cause instanceof Exception) ? (Exception) cause : e;
    }

    /**
     * Reads the {@code error} field from a {@code ParseError} body. Anything the contract does
     * not enumerate — a non-JSON body, a missing field, an unknown code — becomes
     * {@link ParseError#UNEXPECTED_RESPONSE} (FR-012).
     */
    ParseError parseErrorCode(String errorBody) {
        if (errorBody == null || errorBody.trim().isEmpty()) {
            return ParseError.UNEXPECTED_RESPONSE;
        }
        try {
            JSONObject parsed = new JSONObject(errorBody);
            return ParseError.fromWireCode(parsed.optString("error", ""));
        } catch (Exception e) {
            return ParseError.UNEXPECTED_RESPONSE;
        }
    }

    private String buildRequestMetadataSummary(
        CbrdParseConfiguration configuration,
        CbrdParseRequest request,
        String requestBody
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("endpoint=").append(configuration.getEndpointSummary());
        builder.append(", lang=").append(request.getLang());
        builder.append(", textLength=").append(request.getText().length());
        builder.append(", timeoutMs=").append(configuration.getTimeoutMs());
        builder.append(", token=").append(configuration.getTokenFingerprint());
        builder.append(", requestBytes=").append(requestBody.length());
        return builder.toString();
    }

    /**
     * Reads a stream as UTF-8, preserving internal line breaks so returned XML is not mangled.
     */
    private String readFully(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (Reader reader = new InputStreamReader(stream, "UTF-8")) {
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString().trim();
    }
}
