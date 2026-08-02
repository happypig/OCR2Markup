package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.TransformedComponents;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CBRDAPIClientTest {

    @Test
    public void convertToFirstLink_buildsRequestAndParsesResponse() throws Exception {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(
            200,
            "{\"success\":true,\"found\":[\"https://cbetaonline.dila.edu.tw/X0116_p0249a\"]}"
        );

        CBRDAPIClient client = new CBRDAPIClient(
            "https://cbss.dila.edu.tw/cbrd/link",
            "CBRD@dila.edu.tw",
            10000,
            factory
        );

        String url = client.convertToFirstLink(new TransformedComponents("X", "1.16", null, "249", "a", null));

        assertThat(url).isEqualTo("https://cbetaonline.dila.edu.tw/X0116_p0249a");
        assertThat(factory.getRequestProperty("Referer")).isEqualTo("CBRD@dila.edu.tw");

        // Assert the invariant, not the literal. This line previously pinned
        // "DILA-AI-Markup/0.4.2", so a correct version bump turned the suite red and the suite
        // defended a stale header. The rule is "the client identifies itself as this product",
        // not "the version is frozen at 0.4.2". Single-sourcing the version itself is feature 006.
        assertThat(factory.getRequestProperty("User-Agent"))
            .startsWith("DILA-AI-Markup/")
            .matches("DILA-AI-Markup/\\d+\\.\\d+\\.\\d+");
    }

    // The request-shape assertions this class used to make (contains("?q="),
    // doesNotContain("<ref>")) were positive assertions of the superseded CBRD v1.0.0 contract:
    // they passed while production was entirely broken, and could only fail once the client was
    // corrected. They were observed failing in feature 005 (tasks.md T011) and removed here.
    // Method, Content-Type and body are now asserted in CBRDAPIClientRequestShapeTest against the
    // shared CapturingConnectionFactory, which — unlike the file-local fake deleted from this
    // file — records setRequestMethod and getOutputStream.
}
