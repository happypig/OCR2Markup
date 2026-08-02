package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.ParseError;
import org.junit.Test;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T007a — the configured timeout actually reaches the connection, and expiry produces the
 * timeout/connectivity guidance rather than a hang (FR-016, US1 scenario 7).
 */
public class CbrdParseTimeoutBehaviourTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";

    @Test
    public void configuredTimeoutIsAppliedToBothConnectAndRead() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, "<ref/>");

        new CbrdParseApiClient(factory)
            .execute(new CbrdParseConfiguration(ENDPOINT, 45000, "token"), new CbrdParseRequest("text", "zh"));

        assertThat(factory.getLastConnectTimeout()).isEqualTo(45000);
        assertThat(factory.getLastReadTimeout()).isEqualTo(45000);
    }

    @Test
    public void defaultTimeoutIsThirtySecondsNotTen() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, "<ref/>");
        CbrdParseConfiguration defaults = new CbrdParseConfiguration(
            CbrdParseConfiguration.resolveEndpointUrl(null),
            CbrdParseConfiguration.resolveTimeoutMs(null),
            "token"
        );

        new CbrdParseApiClient(factory).execute(defaults, new CbrdParseRequest("text", "zh"));

        assertThat(factory.getLastReadTimeout()).isEqualTo(30000);
        assertThat(factory.getLastReadTimeout()).isNotEqualTo(10000);
    }

    @Test
    public void expiredTimeoutIsReportedAsConnectivityFailureAndYieldsNoMarkup() {
        CapturingConnectionFactory factory =
            CapturingConnectionFactory.throwing(new SocketTimeoutException("Read timed out"));

        CbrdParseResponse response = new CbrdParseApiClient(factory)
            .execute(new CbrdParseConfiguration(ENDPOINT, 30000, "token"), new CbrdParseRequest("text", "zh"));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo(ParseError.CONNECTIVITY_FAILURE);
        assertThat(response.getError().getGuidanceMessageKey()).isEqualTo("ai.markup.error.connectivity");
        assertThat(response.getMarkupXml()).isEmpty();
    }

    @Test
    public void connectionIsReleasedEvenWhenTheRequestTimesOut() {
        CapturingConnectionFactory factory =
            CapturingConnectionFactory.throwing(new SocketTimeoutException("Read timed out"));

        new CbrdParseApiClient(factory)
            .execute(new CbrdParseConfiguration(ENDPOINT, 30000, "token"), new CbrdParseRequest("text", "zh"));

        assertThat(factory.isDisconnected()).isTrue();
    }
}
