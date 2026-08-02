package com.dila.dama.plugin.infrastructure.api;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * Opt-in probe against the live CBRD service.
 *
 * <p>{@link CBRDContractConformanceTest} proves the client and the vendored contract agree with
 * each other. Only this test can catch the case that actually caused the 2026-08 outage: the
 * vendor changing while both of ours stayed consistent and wrong.
 *
 * <p>It is deliberately **not** part of the default run. A suite that reddens because DILA is
 * down, or because the developer is on a train, is a suite people learn to ignore — and an
 * ignored guard is how the original drift survived. Enable it explicitly:
 *
 * <pre>
 *   CBRD_LIVE_CONTRACT_CHECK=1 mvn test -Dtest=CBRDLiveContractProbeTest      (bash)
 *   $env:CBRD_LIVE_CONTRACT_CHECK=1; mvn test -Dtest=CBRDLiveContractProbeTest  (PowerShell)
 * </pre>
 *
 * <p>Without the flag JUnit reports these as <em>skipped</em>, never as passed. A green tick for
 * a check that did not run is the same lie in a different font.
 */
public class CBRDLiveContractProbeTest {

    private static final String OPENAPI_URL = "https://cbss.dila.edu.tw/cbrd/openapi.json";
    private static final String FLAG = "CBRD_LIVE_CONTRACT_CHECK";

    private static void requireOptIn() {
        String flag = System.getenv(FLAG);
        assumeTrue(
            "Skipped: live contract probe is opt-in. Set " + FLAG + "=1 to run it.",
            flag != null && !flag.trim().isEmpty() && !"0".equals(flag.trim()));
    }

    private static String fetchOpenApiDocument() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(OPENAPI_URL).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "DILA-AI-Markup/contract-probe");

            assertThat(conn.getResponseCode())
                .as("fetching %s", OPENAPI_URL)
                .isEqualTo(200);

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * The live contract version must still be the one the client is built against. A mismatch is
     * a reviewable signal, not automatically a defect — but it must never pass unnoticed.
     */
    @Test
    public void liveContractVersionMatchesClientContractVersion() throws Exception {
        requireOptIn();

        String doc = fetchOpenApiDocument();

        assertThat(doc)
            .as("live CBRD contract at %s no longer declares version %s. Review the diff against "
                    + "the vendored contract before changing either.",
                OPENAPI_URL, CBRDAPIClient.CONTRACT_VERSION)
            .contains("\"version\":\"" + CBRDAPIClient.CONTRACT_VERSION + "\"");
    }

    /** /link must still be POST-only. Its move from GET to POST is what broke production. */
    @Test
    public void liveLinkEndpointStillAcceptsOnlyPost() throws Exception {
        requireOptIn();

        String doc = fetchOpenApiDocument();
        int linkAt = doc.indexOf("\"/link\"");
        assertThat(linkAt).as("live contract must still declare a /link path").isGreaterThan(-1);

        // Bound the scan to the /link path object so /parse's verbs are not picked up.
        int nextPath = doc.indexOf("\"/parse\"", linkAt);
        String linkSection = nextPath > linkAt ? doc.substring(linkAt, nextPath) : doc.substring(linkAt);

        assertThat(linkSection).as("/link must still expose post").contains("\"post\"");
        assertThat(linkSection)
            .as("/link has regained a GET verb — the plugin's request shape may need revisiting")
            .doesNotContain("\"get\"");
    }
}
