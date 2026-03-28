package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class OpenAiCompatibleChatClient {

    public static final class Response {
        private final boolean success;
        private final String content;
        private final Integer httpStatus;
        private final OpenAiErrorResponse errorResponse;
        private final RequestTraceSnapshot traceSnapshot;
        private final Exception exception;

        private Response(boolean success, String content, Integer httpStatus, OpenAiErrorResponse errorResponse, RequestTraceSnapshot traceSnapshot, Exception exception) {
            this.success = success;
            this.content = content;
            this.httpStatus = httpStatus;
            this.errorResponse = errorResponse;
            this.traceSnapshot = traceSnapshot;
            this.exception = exception;
        }

        public static Response success(String content, Integer httpStatus, RequestTraceSnapshot traceSnapshot) {
            return new Response(true, content, httpStatus, null, traceSnapshot, null);
        }

        public static Response failure(Integer httpStatus, OpenAiErrorResponse errorResponse, RequestTraceSnapshot traceSnapshot, Exception exception) {
            return new Response(false, "", httpStatus, errorResponse, traceSnapshot, exception);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getContent() {
            return content;
        }

        public Integer getHttpStatus() {
            return httpStatus;
        }

        public OpenAiErrorResponse getErrorResponse() {
            return errorResponse;
        }

        public RequestTraceSnapshot getTraceSnapshot() {
            return traceSnapshot;
        }

        public Exception getException() {
            return exception;
        }
    }

    private final HttpUrlConnectionFactory connectionFactory;
    private final SecretRedactor redactor;

    public OpenAiCompatibleChatClient() {
        this(new HttpUrlConnectionFactory(), new SecretRedactor());
    }

    public OpenAiCompatibleChatClient(HttpUrlConnectionFactory connectionFactory, SecretRedactor redactor) {
        this.connectionFactory = connectionFactory;
        this.redactor = redactor;
    }

    public Response execute(MarkupServiceConfiguration configuration, String systemPrompt, String userContent) {
        String requestId = UUID.randomUUID().toString();
        String requestBody = buildRequestBody(configuration.getModelName(), systemPrompt, userContent);
        RequestTraceSnapshot trace = new RequestTraceSnapshot(
            requestId,
            configuration.getEndpointSummary(),
            buildRequestMetadataSummary(configuration, userContent, requestBody)
        );

        HttpURLConnection connection = null;
        try {
            URL url = configuration.toRequestUri().toURL();
            connection = connectionFactory.openConnection(url);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(configuration.getTimeoutMs());
            connection.setReadTimeout(configuration.getTimeoutMs());
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + configuration.getApiKey());
            connection.setDoOutput(true);

            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
                writer.write(requestBody);
                writer.flush();
            }

            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                String body = readFully(connection.getInputStream());
                return Response.success(parseContent(body), status, trace);
            }

            String errorBody = readFully(connection.getErrorStream());
            return Response.failure(status, OpenAiErrorResponse.parse(errorBody, redactor), trace, null);
        } catch (Exception e) {
            return Response.failure(null, OpenAiErrorResponse.parse(e.getMessage(), redactor), trace, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    String buildRequestBody(String model, String systemPrompt, String userContent) {
        JSONObject request = new JSONObject();
        request.put("model", model);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        messages.put(new JSONObject().put("role", "user").put("content", userContent));
        request.put("messages", messages);
        request.put("max_tokens", 1000);
        return request.toString();
    }

    String parseContent(String responseBody) {
        JSONObject object = new JSONObject(responseBody);
        JSONArray choices = object.getJSONArray("choices");
        if (choices.length() == 0) {
            return "";
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        return message.optString("content", "");
    }

    private String buildRequestMetadataSummary(MarkupServiceConfiguration configuration, String userContent, String requestBody) {
        StringBuilder builder = new StringBuilder();
        builder.append("endpoint=").append(configuration.getEndpointSummary());
        builder.append(", model=").append(configuration.getModelName());
        builder.append(", textLength=").append(userContent == null ? 0 : userContent.length());
        builder.append(", apiKey=").append(configuration.getApiKeyFingerprint());
        builder.append(", request=").append(redactor.redact(requestBody));
        return builder.toString();
    }

    private String readFully(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
