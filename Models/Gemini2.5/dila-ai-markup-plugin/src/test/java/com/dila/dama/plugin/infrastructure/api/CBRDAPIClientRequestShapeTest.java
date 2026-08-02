package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.TransformedComponents;
import org.json.JSONObject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wire-shape tests for the {@code /cbrd/link} request (feature 005).
 *
 * <p>These live in their own class rather than in {@link CBRDAPIClientTest} because that class
 * declares a nested {@code CapturingConnectionFactory} which shadows the shared seam of the same
 * simple name in this package. Sharing a file would force fully-qualified names, or deleting the
 * nested fake before the inverted-RED observation in tasks.md T011 — which is the one thing that
 * ordering forbids.
 *
 * <p>What is asserted here is exactly what the vendor drift changed and what the old tests were
 * structurally unable to see: the HTTP method and the request body. See
 * {@code exploration/ref2link_drift.md} §9.
 */
public class CBRDAPIClientRequestShapeTest {

    private static final String SUCCESS_BODY =
        "{\"success\":true,\"found\":[\"https://cbetaonline.dila.edu.tw/X0116_p0249a\"]}";

    private static CBRDAPIClient clientUsing(CapturingConnectionFactory factory) {
        return new CBRDAPIClient(
            "https://cbss.dila.edu.tw/cbrd/link",
            "CBRD@dila.edu.tw",
            10000,
            factory
        );
    }

    /** T004 — CBRD v1.1.0 routes only POST /link; a GET is what produced the production 404. */
    @Test
    public void convertToFirstLink_usesPostMethod() throws Exception {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, SUCCESS_BODY);

        clientUsing(factory).convertToFirstLink(new TransformedComponents("X", "1.16", null, "249", "a", null));

        assertThat(factory.getLastMethod()).isEqualTo("POST");
    }

    /**
     * T005 — the body must be declared as JSON, and the Referer header must still be sent.
     * v1.1.0 declares no security on /link so Referer is no longer required, but removing it is
     * out of scope for an outage fix (research D2) and the service still accepts it (FR-012,
     * verified live 2026-08-03).
     */
    @Test
    public void convertToFirstLink_sendsJsonContentTypeAndKeepsReferer() throws Exception {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, SUCCESS_BODY);

        clientUsing(factory).convertToFirstLink(new TransformedComponents("X", "1.16", null, "249", "a", null));

        assertThat(factory.getRequestProperty("Content-Type")).startsWith("application/json");
        assertThat(factory.getRequestProperty("Referer")).isEqualTo("CBRD@dila.edu.tw");
    }

    /** T006 — the citation travels in the body under key "q", not in the query string. */
    @Test
    public void convertToFirstLink_sendsCitationInJsonBody() throws Exception {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, SUCCESS_BODY);

        clientUsing(factory).convertToFirstLink(new TransformedComponents("X", "1.16", null, "249", "a", null));

        String body = factory.getCapturedRequestBody();
        assertThat(body).contains("\"q\":");
        assertThat(new JSONObject(body).getString("q")).startsWith("<ref>");
        assertThat(factory.getLastUrl().toString()).doesNotContain("?q=");
    }

    /**
     * T007 / FR-002 — the citation must reach the service byte-identical. Quotes, CJK and
     * full-width punctuation are content, not delimiters. This is the input class that defeats
     * hand-rolled escapers, which is why research D1 mandates JSONObject.
     */
    @Test
    public void convertToFirstLink_preservesCjkAndQuotesInBody() throws Exception {
        String citation =
            "<ref xml:id=\"r26\"><canon>大正</canon><v>二九</v>、<p>一</p><c>下</c>―<p>二</p><c>上</c></ref>";
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, SUCCESS_BODY);

        clientUsing(factory).convertToFirstLink(citation);

        assertThat(new JSONObject(factory.getCapturedRequestBody()).getString("q"))
            .isEqualTo(citation);
    }
}
