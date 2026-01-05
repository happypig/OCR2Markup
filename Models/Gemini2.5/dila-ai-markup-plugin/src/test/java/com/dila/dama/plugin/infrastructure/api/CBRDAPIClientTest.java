package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.TransformedComponents;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CBRDAPIClientTest {

    @Test
    public void convertToFirstLink_buildsEncodedRequestAndParsesResponse() throws Exception {
        CapturingConnectionFactory factory = new CapturingConnectionFactory(
            200,
            "{\"success\":true,\"found\":[\"https://cbetaonline.dila.edu.tw/X0116_p0249a\"]}"
        );

        CBRDAPIClient client = new CBRDAPIClient(
            "https://cbss.dila.edu.tw/dev/cbrd/link",
            "CBRD@dila.edu.tw",
            3000,
            factory
        );

        String url = client.convertToFirstLink(new TransformedComponents("X", "1.16", null, "249", "a", null));

        assertThat(url).isEqualTo("https://cbetaonline.dila.edu.tw/X0116_p0249a");
        assertThat(factory.lastUrl.toString()).contains("?q=");
        assertThat(factory.lastUrl.toString()).doesNotContain("<ref>");
        assertThat(factory.lastRequestProperties.get("Referer")).isEqualTo("CBRD@dila.edu.tw");
        assertThat(factory.lastRequestProperties.get("User-Agent")).isEqualTo("DILA-AI-Markup/0.4.0");
    }

    private static final class CapturingConnectionFactory extends HttpUrlConnectionFactory {
        private final int responseCode;
        private final String responseBody;

        private URL lastUrl;
        private Map<String, String> lastRequestProperties = new HashMap<>();

        private CapturingConnectionFactory(int responseCode, String responseBody) {
            this.responseCode = responseCode;
            this.responseBody = responseBody;
        }

        @Override
        public HttpURLConnection openConnection(URL url) {
            this.lastUrl = url;
            return new FakeHttpURLConnection(url, responseCode, responseBody, lastRequestProperties);
        }
    }

    private static final class FakeHttpURLConnection extends HttpURLConnection {
        private final int responseCode;
        private final byte[] responseBytes;
        private final Map<String, String> requestProperties;

        FakeHttpURLConnection(URL u, int responseCode, String responseBody, Map<String, String> requestProperties) {
            super(u);
            this.responseCode = responseCode;
            this.responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            this.requestProperties = requestProperties;
        }

        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.put(key, value);
        }

        @Override
        public String getRequestProperty(String key) {
            return requestProperties.get(key);
        }

        @Override
        public int getResponseCode() {
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
