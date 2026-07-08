package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenAiCompatibleChatClientTest {

    @Test
    public void parsesSuccessfulChatCompletionResponse() throws Exception {
        StubHttpURLConnection connection = new StubHttpURLConnection(new URL("https://api.openai.com/v1/chat/completions"));
        connection.responseCode = 200;
        connection.inputBody = "{\"choices\":[{\"message\":{\"content\":\"<ptr target='x'/>\"}}]}";
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(new StubFactory(connection), new SecretRedactor());

        OpenAiCompatibleChatClient.Response response = client.execute(configuration(), "system", "user");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getContent()).isEqualTo("<ptr target='x'/>");
        assertThat(response.getTraceSnapshot().getRequestMetadataSummary()).contains("textLength=4");
    }

    @Test
    public void parsesStructuredErrorAndSanitizesSecrets() throws Exception {
        StubHttpURLConnection connection = new StubHttpURLConnection(new URL("https://api.openai.com/v1/chat/completions"));
        connection.responseCode = 400;
        connection.errorBody = "{\"error\":{\"message\":\"Bad request Authorization: Bearer sk-secret\",\"type\":\"invalid_request_error\"}}";
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(new StubFactory(connection), new SecretRedactor());

        OpenAiCompatibleChatClient.Response response = client.execute(configuration(), "system", "user");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getHttpStatus()).isEqualTo(400);
        assertThat(response.getErrorResponse().getSanitizedBody()).doesNotContain("sk-secret");
        assertThat(response.getErrorResponse().getSanitizedBody()).contains("Bearer ****");
    }

    @Test
    public void handlesPartialOrMissingErrorBody() throws Exception {
        StubHttpURLConnection connection = new StubHttpURLConnection(new URL("https://api.openai.com/v1/chat/completions"));
        connection.responseCode = 500;
        connection.errorBody = "upstream temporarily unavailable";
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(new StubFactory(connection), new SecretRedactor());

        OpenAiCompatibleChatClient.Response response = client.execute(configuration(), "system", "user");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorResponse().getMessage()).contains("upstream temporarily unavailable");
    }

    private MarkupServiceConfiguration configuration() {
        return new MarkupServiceConfiguration(
            "https://api.openai.com",
            "/v1/chat/completions",
            "gpt-test",
            "sk-example-key",
            30000,
            MarkupServiceConfiguration.ENDPOINT_KIND_OPENAI_HOSTED,
            true
        );
    }

    private static final class StubFactory extends HttpUrlConnectionFactory {
        private final HttpURLConnection connection;

        private StubFactory(HttpURLConnection connection) {
            this.connection = connection;
        }

        @Override
        public HttpURLConnection openConnection(URL url) {
            return connection;
        }
    }

    private static final class StubHttpURLConnection extends HttpURLConnection {
        private int responseCode;
        private String inputBody = "";
        private String errorBody = "";
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private StubHttpURLConnection(URL url) {
            super(url);
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(inputBody.getBytes());
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(errorBody.getBytes());
        }

        @Override
        public java.io.OutputStream getOutputStream() {
            return outputStream;
        }
    }
}
