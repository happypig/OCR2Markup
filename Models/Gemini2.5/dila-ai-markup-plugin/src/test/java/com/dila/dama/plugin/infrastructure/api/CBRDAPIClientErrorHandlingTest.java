package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.TransformedComponents;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Outcome-mapping tests for {@code /cbrd/link}.
 *
 * <p>Migrated in feature 005 from a file-local fake onto the shared
 * {@link CapturingConnectionFactory}. The old fake overrode neither {@code setRequestMethod} nor
 * {@code getOutputStream}, so it was structurally blind to both dimensions the vendor changed —
 * and once the client began writing a request body it could not even accept one.
 *
 * <p>The distinction these tests defend is the substance of User Story 2: CBRD returns every
 * documented outcome as HTTP 200 and signals the result in the payload. Reporting any of them as
 * a transport error is the bug this feature exists to remove.
 */
public class CBRDAPIClientErrorHandlingTest {

    @Test
    public void convertToFirstLink_non200_reportsHttpErrorKey() {
        CBRDAPIClient client = newClient(CapturingConnectionFactory.failingWith(500, "{\"error\":\"boom\"}"));

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> {
                CBRDAPIException apiEx = (CBRDAPIException) ex;
                assertThat(apiEx.getMessageKey()).isEqualTo("error.api.http");
                assertThat(apiEx.getParams()).containsExactly(500);
            });
    }

    @Test
    public void convertToFirstLink_invalidJson_reportsResponseKey() {
        CBRDAPIClient client = newClient(CapturingConnectionFactory.respondingWith(200, "not-json"));

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> assertThat(((CBRDAPIException) ex).getMessageKey())
                .isEqualTo("error.api.response"));
    }

    /** T017 — LinkFailure carrying "msg". This is what the live service returns for an incomplete citation. */
    @Test
    public void convertToFirstLink_successFalseWithMsgField_reportsFailedKeyWithMessage() {
        CBRDAPIClient client = newClient(
            CapturingConnectionFactory.respondingWith(200, "{\"success\":false,\"msg\":\"冊號不存在\"}"));

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> {
                CBRDAPIException apiEx = (CBRDAPIException) ex;
                assertThat(apiEx.getMessageKey()).isEqualTo("error.api.failed");
                assertThat(apiEx.getParams()).containsExactly("冊號不存在");
            });
    }

    /** T018 — the other half of LinkFailure's anyOf: an "error" member instead of "msg". */
    @Test
    public void convertToFirstLink_successFalseWithErrorField_reportsFailedKey() {
        CBRDAPIClient client = newClient(
            CapturingConnectionFactory.respondingWith(200, "{\"success\":false,\"error\":\"no matching reference\"}"));

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> {
                CBRDAPIException apiEx = (CBRDAPIException) ex;
                assertThat(apiEx.getMessageKey()).isEqualTo("error.api.failed");
                assertThat(apiEx.getParams()).containsExactly("no matching reference");
            });
    }

    /** T016 — LinkSuccess with an empty found array is "no match", not a failure and not a transport error. */
    @Test
    public void convertToFirstLink_emptyFound_reportsNoResultsKey() {
        CBRDAPIClient client = newClient(
            CapturingConnectionFactory.respondingWith(200, "{\"success\":true,\"found\":[]}"));

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> assertThat(((CBRDAPIException) ex).getMessageKey())
                .isEqualTo("error.no.results"));
    }

    /**
     * T019 / FR-005 — the transport-error key must be unreachable for anything the service
     * returns normally. Every documented non-resolving outcome arrives as HTTP 200; if any of
     * them surfaces as {@code error.api.http}, the editor is told the network failed when in fact
     * the citation was the problem. That confusion is precisely what this feature removes.
     */
    @Test
    public void convertToFirstLink_documentedOutcomesNeverReportTransportError() {
        String[] documented200Bodies = {
            "{\"success\":true,\"found\":[]}",
            "{\"success\":false,\"msg\":\"經號或頁碼 至少要有一個\"}",
            "{\"success\":false,\"error\":\"no matching reference\"}"
        };

        for (String body : documented200Bodies) {
            CBRDAPIClient client = newClient(CapturingConnectionFactory.respondingWith(200, body));

            assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
                .isInstanceOf(CBRDAPIException.class)
                .satisfies(ex -> assertThat(((CBRDAPIException) ex).getMessageKey())
                    .as("body %s must not be reported as a transport error", body)
                    .isNotEqualTo("error.api.http"));
        }
    }

    /** Retry policy is unchanged: timeouts retry, everything else propagates immediately. */
    @Test
    public void convertToFirstLink_timeout_reportsTimeoutKey() {
        CBRDAPIClient client = newClient(
            CapturingConnectionFactory.throwing(new SocketTimeoutException("timeout")));

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> assertThat(((CBRDAPIException) ex).getMessageKey())
                .isEqualTo("error.api.timeout"));
    }

    @Test
    public void convertToFirstLink_connectionFailure_reportsConnectionKey() {
        CBRDAPIClient client = newClient(new HttpUrlConnectionFactory() {
            @Override
            public HttpURLConnection openConnection(URL url) throws IOException {
                throw new IOException("boom");
            }
        });

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> assertThat(((CBRDAPIException) ex).getMessageKey())
                .isEqualTo("error.api.connection"));
    }

    private CBRDAPIClient newClient(HttpUrlConnectionFactory factory) {
        return new CBRDAPIClient(
            "https://cbss.dila.edu.tw/cbrd/link",
            "CBRD@dila.edu.tw",
            10000,
            factory
        );
    }

    private TransformedComponents sampleComponents() {
        return new TransformedComponents("T", "25", null, "917", null, null);
    }
}
