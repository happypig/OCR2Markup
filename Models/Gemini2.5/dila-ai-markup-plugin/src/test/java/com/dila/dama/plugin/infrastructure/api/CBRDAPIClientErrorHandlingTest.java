package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.TransformedComponents;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CBRDAPIClientErrorHandlingTest {

    @Test
    public void convertToFirstLink_non200_reportsHttpErrorKey() {
        CBRDAPIClient client = newClient(new FakeConnectionFactory(500, "{\"error\":\"boom\"}", false));

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
        CBRDAPIClient client = newClient(new FakeConnectionFactory(200, "not-json", false));

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> assertThat(((CBRDAPIException) ex).getMessageKey())
                .isEqualTo("error.api.response"));
    }

    @Test
    public void convertToFirstLink_emptyFound_reportsNoResultsKey() {
        CBRDAPIClient client = newClient(new FakeConnectionFactory(200, "{\"success\":true,\"found\":[]}", false));

        assertThatThrownBy(() -> client.convertToFirstLink(sampleComponents()))
            .isInstanceOf(CBRDAPIException.class)
            .satisfies(ex -> assertThat(((CBRDAPIException) ex).getMessageKey())
                .isEqualTo("error.no.results"));
    }

    @Test
    public void convertToFirstLink_timeout_reportsTimeoutKey() {
        CBRDAPIClient client = newClient(new FakeConnectionFactory(200, "", true));

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
            "https://cbss.dila.edu.tw/dev/cbrd/link",
            "CBRD@dila.edu.tw",
            3000,
            factory
        );
    }

    private TransformedComponents sampleComponents() {
        return new TransformedComponents("T", "25", null, "917", null, null);
    }

    private static final class FakeConnectionFactory extends HttpUrlConnectionFactory {
        private final int responseCode;
        private final String responseBody;
        private final boolean timeout;

        private FakeConnectionFactory(int responseCode, String responseBody, boolean timeout) {
            this.responseCode = responseCode;
            this.responseBody = responseBody;
            this.timeout = timeout;
        }

        @Override
        public HttpURLConnection openConnection(URL url) {
            return new FakeHttpURLConnection(url, responseCode, responseBody, timeout);
        }
    }

    private static final class FakeHttpURLConnection extends HttpURLConnection {
        private final int responseCode;
        private final byte[] responseBytes;
        private final boolean timeout;

        FakeHttpURLConnection(URL url, int responseCode, String responseBody, boolean timeout) {
            super(url);
            this.responseCode = responseCode;
            this.responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            this.timeout = timeout;
        }

        @Override
        public int getResponseCode() throws IOException {
            if (timeout) {
                throw new SocketTimeoutException("timeout");
            }
            return responseCode;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(responseBytes);
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(responseBytes);
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
        }
    }
}
