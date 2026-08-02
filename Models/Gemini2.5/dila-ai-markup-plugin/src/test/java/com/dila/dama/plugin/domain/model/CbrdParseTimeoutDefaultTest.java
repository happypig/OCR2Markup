package com.dila.dama.plugin.domain.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T004a — the CBRD Parse timeout default (FR-016).
 *
 * The parse endpoint runs a model transformation server-side, so the default deliberately
 * inherits the previous AI Markup timeout (30,000 ms) rather than the shorter Ref-to-Link
 * lookup timeout (10,000 ms).
 */
public class CbrdParseTimeoutDefaultTest {

    @Test
    public void defaultTimeoutIsThirtySeconds() {
        assertThat(CbrdParseConfiguration.DEFAULT_TIMEOUT_MS).isEqualTo(30000);
    }

    @Test
    public void defaultTimeoutIsNotTheRefToLinkLookupTimeout() {
        assertThat(CbrdParseConfiguration.DEFAULT_TIMEOUT_MS).isNotEqualTo(10000);
    }

    @Test
    public void unsetPreferenceResolvesToTheDefault() {
        assertThat(CbrdParseConfiguration.resolveTimeoutMs(null)).isEqualTo(30000);
        assertThat(CbrdParseConfiguration.resolveTimeoutMs("")).isEqualTo(30000);
        assertThat(CbrdParseConfiguration.resolveTimeoutMs("   ")).isEqualTo(30000);
    }

    @Test
    public void unparseablePreferenceFallsBackToTheDefault() {
        assertThat(CbrdParseConfiguration.resolveTimeoutMs("not-a-number")).isEqualTo(30000);
        assertThat(CbrdParseConfiguration.resolveTimeoutMs("30_000")).isEqualTo(30000);
    }

    @Test
    public void nonPositivePreferenceFallsBackToTheDefault() {
        assertThat(CbrdParseConfiguration.resolveTimeoutMs("0")).isEqualTo(30000);
        assertThat(CbrdParseConfiguration.resolveTimeoutMs("-5000")).isEqualTo(30000);
    }

    @Test
    public void validPreferenceIsHonoured() {
        assertThat(CbrdParseConfiguration.resolveTimeoutMs("45000")).isEqualTo(45000);
        assertThat(CbrdParseConfiguration.resolveTimeoutMs("  12000  ")).isEqualTo(12000);
    }

    @Test
    public void unsetEndpointUrlResolvesToTheDilaProductionEndpoint() {
        assertThat(CbrdParseConfiguration.resolveEndpointUrl(null))
            .isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
        assertThat(CbrdParseConfiguration.resolveEndpointUrl("  "))
            .isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
    }

    @Test
    public void customEndpointUrlIsHonoured() {
        assertThat(CbrdParseConfiguration.resolveEndpointUrl("https://staging.example.org/cbrd/parse"))
            .isEqualTo("https://staging.example.org/cbrd/parse");
    }
}
