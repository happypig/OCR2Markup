package com.dila.dama.plugin.domain.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T004 — CbrdParseConfiguration value object (FR-002, FR-003, FR-016).
 */
public class CbrdParseConfigurationTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";

    @Test
    public void trimsAndExposesConfiguredValues() {
        CbrdParseConfiguration configuration = new CbrdParseConfiguration("  " + ENDPOINT + "  ", 30000, "  token-abcd  ");

        assertThat(configuration.getEndpointUrl()).isEqualTo(ENDPOINT);
        assertThat(configuration.getTimeoutMs()).isEqualTo(30000);
        assertThat(configuration.getSharedToken()).isEqualTo("token-abcd");
        assertThat(configuration.hasSharedToken()).isTrue();
    }

    @Test
    public void treatsNullValuesAsEmptyRatherThanThrowing() {
        CbrdParseConfiguration configuration = new CbrdParseConfiguration(null, 30000, null);

        assertThat(configuration.getEndpointUrl()).isEmpty();
        assertThat(configuration.getSharedToken()).isEmpty();
        assertThat(configuration.hasSharedToken()).isFalse();
    }

    @Test
    public void fingerprintExposesOnlyTheLastFourCharacters() {
        CbrdParseConfiguration configuration = new CbrdParseConfiguration(ENDPOINT, 30000, "super-secret-9876");

        assertThat(configuration.getTokenFingerprint()).isEqualTo("****9876");
        assertThat(configuration.getTokenFingerprint()).doesNotContain("super-secret");
    }

    @Test
    public void fingerprintFullyMasksShortTokens() {
        assertThat(new CbrdParseConfiguration(ENDPOINT, 30000, "abcd").getTokenFingerprint()).isEqualTo("****");
        assertThat(new CbrdParseConfiguration(ENDPOINT, 30000, "ab").getTokenFingerprint()).isEqualTo("****");
    }

    @Test
    public void fingerprintIsEmptyWhenNoTokenConfigured() {
        assertThat(new CbrdParseConfiguration(ENDPOINT, 30000, "").getTokenFingerprint()).isEmpty();
    }

    @Test
    public void endpointSummaryNeverLeaksTheToken() {
        CbrdParseConfiguration configuration = new CbrdParseConfiguration(ENDPOINT, 30000, "super-secret-9876");

        assertThat(configuration.getEndpointSummary()).contains(ENDPOINT);
        assertThat(configuration.getEndpointSummary()).doesNotContain("super-secret-9876");
    }

    @Test
    public void toRequestUriReturnsTheConfiguredEndpoint() throws Exception {
        CbrdParseConfiguration configuration = new CbrdParseConfiguration(ENDPOINT, 30000, "token");

        assertThat(configuration.toRequestUri().toString()).isEqualTo(ENDPOINT);
    }

    @Test
    public void valueEquality() {
        CbrdParseConfiguration one = new CbrdParseConfiguration(ENDPOINT, 30000, "token");
        CbrdParseConfiguration two = new CbrdParseConfiguration(ENDPOINT, 30000, "token");
        CbrdParseConfiguration different = new CbrdParseConfiguration(ENDPOINT, 10000, "token");

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one).isNotEqualTo(different);
    }
}
