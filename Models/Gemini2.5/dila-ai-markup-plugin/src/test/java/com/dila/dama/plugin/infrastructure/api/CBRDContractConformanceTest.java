package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.TransformedComponents;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Offline guard against vendor-contract drift.
 *
 * <p>Until 2026-08-03 the repository vendored CBRD v1.0.0 ({@code GET /link?q=}) while the live
 * service served v1.1.0 ({@code POST /link} + JSON body). Nothing compared the two, so the drift
 * surfaced as a production outage rather than a build failure. This test closes that: it asserts
 * that the version the client is built against, the version recorded in the vendored contract,
 * and the request shape the client actually puts on the wire all agree.
 *
 * <p>Deliberately offline and dependency-free. There is no YAML library in this project's pom,
 * and adding one to read two scalars is not warranted (research D3); the live-server check lives
 * in {@link CBRDLiveContractProbeTest}, which is opt-in so that {@code mvn test} stays
 * deterministic and runnable without a network.
 */
public class CBRDContractConformanceTest {

    private static final String CONTRACT_PATH =
        "specs/001-ref-to-link-action/contracts/cbrd-api.yaml";

    private static String contractText() throws Exception {
        File contract = RepoRootLocator.repoFile(CONTRACT_PATH);
        return new String(Files.readAllBytes(contract.toPath()), StandardCharsets.UTF_8);
    }

    /** The version the client targets and the version the repository vendored MUST agree. */
    @Test
    public void vendoredContractVersionMatchesClientContractVersion() throws Exception {
        String yaml = contractText();

        Matcher m = Pattern.compile("(?m)^\\s{2}version:\\s*(\\S+)\\s*$").matcher(yaml);
        if (!m.find()) {
            fail("Could not read info.version from " + CONTRACT_PATH
                + " — the guard cannot verify anything, which is itself a failure.");
        }
        String vendored = m.group(1);

        assertThat(vendored)
            .as("Vendored contract %s declares version %s, but CBRDAPIClient is built against "
                    + "version %s. Either the vendor changed and the client must be updated, or "
                    + "the client changed and the vendored contract must be re-synced. Do not "
                    + "silence this by editing one side to match the other without checking the "
                    + "live service.",
                CONTRACT_PATH, vendored, CBRDAPIClient.CONTRACT_VERSION)
            .isEqualTo(CBRDAPIClient.CONTRACT_VERSION);
    }

    /** The contract must describe a POST with a JSON body keyed on "q" — what the client sends. */
    @Test
    public void vendoredContractDescribesThePostJsonShape() throws Exception {
        String yaml = contractText();

        assertThat(yaml)
            .as("/link must be declared as POST — a GET is what produced the production 404")
            .contains("  /link:")
            .contains("    post:");
        assertThat(yaml)
            .as("the request body must be JSON with a required 'q' member")
            .contains("application/json:")
            .contains("LinkRequest");
        assertThat(yaml)
            .as("v1.0.0's query-parameter form must be gone")
            .doesNotContain("in: query");
    }

    /** What the contract describes and what the client actually transmits must be the same thing. */
    @Test
    public void clientRequestShapeMatchesTheContract() throws Exception {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(
            200,
            "{\"success\":true,\"found\":[\"https://cbetaonline.dila.edu.tw/T04n0202_p0376b04\"]}"
        );

        new CBRDAPIClient("https://cbss.dila.edu.tw/cbrd/link", "CBRD@dila.edu.tw", 10000, factory)
            .convertToFirstLink(new TransformedComponents("T", "25", null, "917", null, null));

        assertThat(factory.getLastMethod()).isEqualTo("POST");
        assertThat(factory.getRequestProperty("Content-Type")).startsWith("application/json");
        assertThat(factory.getCapturedRequestBody()).contains("\"q\":");
        assertThat(factory.getLastUrl().toString()).doesNotContain("?q=");
    }
}
