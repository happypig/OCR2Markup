package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.ParseError;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T007 — CBRD Parse HTTP client, one case per documented response in contracts/openapi.yaml
 * (FR-001, FR-006, FR-011, FR-012, FR-013).
 */
public class CbrdParseApiClientTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";
    private static final String MARKUP =
        "<ref>(<canon>T</canon> <w>1442</w>)，大正<v>23</v>，頁<p>869</p><c>中</c></ref>";

    private static CbrdParseConfiguration configuration() {
        return new CbrdParseConfiguration(ENDPOINT, 30000, "shared-token-9876");
    }

    private static CbrdParseRequest request() {
        return new CbrdParseRequest("(T 1442)，大正23，頁869中", "zh");
    }

    @Test
    public void successfulParseReturnsTheServiceMarkupUnwrapped() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, MARKUP);

        CbrdParseResponse response = new CbrdParseApiClient(factory).execute(configuration(), request());

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMarkupXml()).isEqualTo(MARKUP);
        assertThat(response.getHttpStatus()).isEqualTo(200);
        assertThat(response.getError()).isNull();
    }

    @Test
    public void postsJsonToTheConfiguredEndpointWithBearerAuth() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, MARKUP);

        new CbrdParseApiClient(factory).execute(configuration(), request());

        assertThat(factory.getLastUrl().toString()).isEqualTo(ENDPOINT);
        assertThat(factory.getLastMethod()).isEqualTo("POST");
        assertThat(factory.getRequestProperty("Authorization")).isEqualTo("Bearer shared-token-9876");
        assertThat(factory.getRequestProperty("Content-Type")).isEqualTo("application/json");
        assertThat(factory.getRequestProperty("Accept")).isEqualTo("application/xml");
    }

    @Test
    public void alwaysDisconnectsTheConnection() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, MARKUP);

        new CbrdParseApiClient(factory).execute(configuration(), request());

        assertThat(factory.isDisconnected()).isTrue();
    }

    @Test
    public void mapsMissingInput() {
        assertThat(failureFor(400, "{\"success\":false,\"error\":\"text_is_required\"}").getError())
            .isEqualTo(ParseError.TEXT_IS_REQUIRED);
    }

    @Test
    public void mapsTooLongInput() {
        assertThat(failureFor(400, "{\"success\":false,\"error\":\"text_is_too_long\"}").getError())
            .isEqualTo(ParseError.TEXT_IS_TOO_LONG);
    }

    @Test
    public void mapsUnsupportedLanguage() {
        assertThat(failureFor(400, "{\"success\":false,\"error\":\"unsupported_language\"}").getError())
            .isEqualTo(ParseError.UNSUPPORTED_LANGUAGE);
    }

    @Test
    public void mapsUnauthorized() {
        assertThat(failureFor(401, "{\"success\":false,\"error\":\"unauthorized\"}").getError())
            .isEqualTo(ParseError.UNAUTHORIZED);
    }

    @Test
    public void mapsInvalidModelOutput() {
        assertThat(failureFor(422, "{\"success\":false,\"error\":\"invalid_model_output\"}").getError())
            .isEqualTo(ParseError.INVALID_MODEL_OUTPUT);
    }

    @Test
    public void mapsUpstreamProviderUnavailable() {
        assertThat(failureFor(502, "{\"success\":false,\"error\":\"openai_unavailable\"}").getError())
            .isEqualTo(ParseError.OPENAI_UNAVAILABLE);
    }

    @Test
    public void mapsAllThreeServiceUnavailableCauses() {
        assertThat(failureFor(503, "{\"success\":false,\"error\":\"parse_api_not_configured\"}").getError())
            .isEqualTo(ParseError.PARSE_API_NOT_CONFIGURED);
        assertThat(failureFor(503, "{\"success\":false,\"error\":\"openai_credentials_unavailable\"}").getError())
            .isEqualTo(ParseError.OPENAI_CREDENTIALS_UNAVAILABLE);
        assertThat(failureFor(503, "{\"success\":false,\"error\":\"openai_rate_limited\"}").getError())
            .isEqualTo(ParseError.OPENAI_RATE_LIMITED);
    }

    @Test
    public void failureCarriesTheHttpStatus() {
        CbrdParseResponse response = failureFor(401, "{\"success\":false,\"error\":\"unauthorized\"}");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getHttpStatus()).isEqualTo(401);
        assertThat(response.getMarkupXml()).isEmpty();
    }

    @Test
    public void unknownStatusCodeBecomesUnexpectedResponse() {
        assertThat(failureFor(418, "{\"success\":false,\"error\":\"i_am_a_teapot\"}").getError())
            .isEqualTo(ParseError.UNEXPECTED_RESPONSE);
    }

    @Test
    public void unparseableErrorBodyBecomesUnexpectedResponse() {
        assertThat(failureFor(500, "<html>Gateway Error</html>").getError())
            .isEqualTo(ParseError.UNEXPECTED_RESPONSE);
        assertThat(failureFor(400, "").getError()).isEqualTo(ParseError.UNEXPECTED_RESPONSE);
    }

    @Test
    public void absentErrorStreamBecomesUnexpectedResponse() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(500, "");

        CbrdParseResponse response = new CbrdParseApiClient(factory).execute(configuration(), request());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo(ParseError.UNEXPECTED_RESPONSE);
    }

    @Test
    public void transportFailureBecomesConnectivityFailure() {
        CapturingConnectionFactory factory =
            CapturingConnectionFactory.throwing(new IOException("Connection refused"));

        CbrdParseResponse response = new CbrdParseApiClient(factory).execute(configuration(), request());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo(ParseError.CONNECTIVITY_FAILURE);
        assertThat(response.getException()).isInstanceOf(IOException.class);
        assertThat(response.getHttpStatus()).isNull();
    }

    @Test
    public void readTimeoutBecomesConnectivityFailure() {
        CapturingConnectionFactory factory =
            CapturingConnectionFactory.throwing(new SocketTimeoutException("Read timed out"));

        CbrdParseResponse response = new CbrdParseApiClient(factory).execute(configuration(), request());

        assertThat(response.getError()).isEqualTo(ParseError.CONNECTIVITY_FAILURE);
        assertThat(response.getException()).isInstanceOf(SocketTimeoutException.class);
    }

    // T080 — Java-17 getErrorStream() quirk: S10 was mis-routed to FR-013 (connectivity) under
    // Oxygen's bundled Java 17 even though the live endpoint returned a clean 401 with the
    // contract-correct body. See spec.md Clarifications 2026-08-02, US4 scenario 12, Edge Cases;
    // plan.md "Java-17 getErrorStream() Quirk"; tasks.md Phase 8 (T080 - T084).

    @Test
    public void unauthorizedOnJava17GetErrorStreamThrow() {
        // Java 17 reproduces the canonical "Server returned HTTP response code: 401 for URL: ..."
        // message from sun.net.www.protocol.http.HttpURLConnection.getInputStream() on 4xx.
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingButErrorStreamThrows(
            401,
            new IOException(
                "Server returned HTTP response code: 401 for URL: " + ENDPOINT
            )
        );

        CbrdParseResponse response = new CbrdParseApiClient(factory).execute(configuration(), request());

        // Per FR-011 (spec.md): a 401 with the canonical Bearer-realm header and unauthorized
        // body MUST classify as UNAUTHORIZED — never as CONNECTIVITY_FAILURE.
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo(ParseError.UNAUTHORIZED);
        assertThat(response.getHttpStatus()).isEqualTo(401);
        // The diagnostics export's serviceErrorBody MUST stay non-empty so the export carries a
        // meaningful cause string rather than "" (which would defeat FR-022 in this scenario).
        assertThat(response.getErrorBody()).isNotEmpty();
    }

    @Test
    public void unknownHostExceptionRemainsConnectivityFailureEvenAfterJava17QuirkFix() {
        // T082: no-status transport failures MUST still classify as connectivity under FR-013.
        CapturingConnectionFactory factory = CapturingConnectionFactory.throwing(
            new java.net.UnknownHostException("cbss.dila.edu.tw")
        );

        CbrdParseResponse response = new CbrdParseApiClient(factory).execute(configuration(), request());

        assertThat(response.getError()).isEqualTo(ParseError.CONNECTIVITY_FAILURE);
        assertThat(response.getHttpStatus()).isNull();
    }

    @Test
    public void malformedEndpointUrlIsReportedRatherThanThrown() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, MARKUP);
        CbrdParseConfiguration broken = new CbrdParseConfiguration("not-a-url", 30000, "token");

        CbrdParseResponse response = new CbrdParseApiClient(factory).execute(broken, request());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo(ParseError.CONNECTIVITY_FAILURE);
    }

    @Test
    public void traceNeverContainsTheRawToken() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, MARKUP);

        CbrdParseResponse response = new CbrdParseApiClient(factory).execute(configuration(), request());

        RequestTraceSnapshot trace = response.getTrace();
        assertThat(trace).isNotNull();
        assertThat(trace.getRequestMetadataSummary()).doesNotContain("shared-token-9876");
        assertThat(trace.getRequestMetadataSummary()).contains("****9876");
        assertThat(trace.getEndpointSummary()).doesNotContain("shared-token-9876");
    }

    private CbrdParseResponse failureFor(int status, String errorBody) {
        CapturingConnectionFactory factory = CapturingConnectionFactory.failingWith(status, errorBody);
        return new CbrdParseApiClient(factory).execute(configuration(), request());
    }
}
