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
            if (status >= 200 && status < 300) {
                return CbrdParseResponse.success(readFully(connection.getInputStream()), status, trace);
            }

            String errorBody = readFully(connection.getErrorStream());
            ParseError error = parseErrorCode(errorBody);
            PluginLogger.warn("[CbrdParseApiClient]Parse failed: status=" + status + ", error=" + error);
            return CbrdParseResponse.failure(status, error, redactor.redact(errorBody), trace, null);
        } catch (Exception e) {
            PluginLogger.error("[CbrdParseApiClient]Connection error: " + e.getMessage());
            return CbrdParseResponse.failure(null, ParseError.CONNECTIVITY_FAILURE, "", trace, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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
