package com.dila.dama.plugin.domain.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class MarkupServiceConfiguration {

    public static final String ENDPOINT_KIND_OPENAI_HOSTED = "openai_hosted";
    public static final String ENDPOINT_KIND_OPENAI_COMPATIBLE = "openai_compatible";

    private final String baseUrl;
    private final String chatCompletionsPath;
    private final String modelName;
    private final String apiKey;
    private final int timeoutMs;
    private final String endpointKind;
    private final boolean proxyExpected;

    public MarkupServiceConfiguration(
        String baseUrl,
        String chatCompletionsPath,
        String modelName,
        String apiKey,
        int timeoutMs,
        String endpointKind,
        boolean proxyExpected
    ) {
        this.baseUrl = trim(baseUrl);
        this.chatCompletionsPath = normalizePath(chatCompletionsPath);
        this.modelName = trim(modelName);
        this.apiKey = trim(apiKey);
        this.timeoutMs = timeoutMs;
        this.endpointKind = trim(endpointKind);
        this.proxyExpected = proxyExpected;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getChatCompletionsPath() {
        return chatCompletionsPath;
    }

    public String getModelName() {
        return modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public String getEndpointKind() {
        return endpointKind;
    }

    public boolean isProxyExpected() {
        return proxyExpected;
    }

    public String getApiKeyFingerprint() {
        if (!hasApiKey()) {
            return "";
        }
        int length = apiKey.length();
        if (length <= 4) {
            return "****";
        }
        return "****" + apiKey.substring(length - 4);
    }

    public String getEndpointSummary() {
        return baseUrl + chatCompletionsPath + " [" + modelName + "]";
    }

    public URI toRequestUri() throws URISyntaxException {
        String trimmedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return new URI(trimmedBase + chatCompletionsPath);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizePath(String value) {
        String trimmed = trim(value);
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarkupServiceConfiguration)) return false;
        MarkupServiceConfiguration that = (MarkupServiceConfiguration) o;
        return timeoutMs == that.timeoutMs
            && proxyExpected == that.proxyExpected
            && Objects.equals(baseUrl, that.baseUrl)
            && Objects.equals(chatCompletionsPath, that.chatCompletionsPath)
            && Objects.equals(modelName, that.modelName)
            && Objects.equals(apiKey, that.apiKey)
            && Objects.equals(endpointKind, that.endpointKind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseUrl, chatCompletionsPath, modelName, apiKey, timeoutMs, endpointKind, proxyExpected);
    }
}
