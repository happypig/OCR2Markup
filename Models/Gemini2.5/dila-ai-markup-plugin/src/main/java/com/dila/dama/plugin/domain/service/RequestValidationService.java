package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;

import java.net.URI;

public class RequestValidationService {

    interface SystemPropertyReader {
        String getProperty(String name, String defaultValue);
    }

    private final SystemPropertyReader systemPropertyReader;

    public RequestValidationService() {
        this(System::getProperty);
    }

    RequestValidationService(SystemPropertyReader systemPropertyReader) {
        this.systemPropertyReader = systemPropertyReader;
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final DiagnosticFailureCategory category;
        private final String guidanceMessageKey;
        private final String detail;

        private ValidationResult(boolean valid, DiagnosticFailureCategory category, String guidanceMessageKey, String detail) {
            this.valid = valid;
            this.category = category;
            this.guidanceMessageKey = guidanceMessageKey;
            this.detail = detail;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null, null, "");
        }

        public static ValidationResult invalid(DiagnosticFailureCategory category, String guidanceMessageKey, String detail) {
            return new ValidationResult(false, category, guidanceMessageKey, detail);
        }

        public boolean isValid() {
            return valid;
        }

        public DiagnosticFailureCategory getCategory() {
            return category;
        }

        public String getGuidanceMessageKey() {
            return guidanceMessageKey;
        }

        public String getDetail() {
            return detail;
        }
    }

    public ValidationResult validate(MarkupServiceConfiguration configuration, String selectedText) {
        if (configuration == null) {
            return ValidationResult.invalid(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE, "ai.markup.diagnostic.unknown", "Missing configuration");
        }
        if (selectedText == null || selectedText.trim().isEmpty()) {
            return ValidationResult.invalid(DiagnosticFailureCategory.MALFORMED_REQUEST, "ai.markup.diagnostic.selection.required", "No selected text");
        }
        if (!configuration.hasApiKey()) {
            return ValidationResult.invalid(DiagnosticFailureCategory.CREDENTIALS, "ai.markup.diagnostic.credentials", "Missing API key");
        }
        if (configuration.getApiKey().contains(" ")) {
            return ValidationResult.invalid(DiagnosticFailureCategory.CREDENTIALS, "ai.markup.diagnostic.credentials", "Malformed API key");
        }
        if (configuration.getModelName() == null || configuration.getModelName().trim().isEmpty()) {
            return ValidationResult.invalid(DiagnosticFailureCategory.MODEL_ACCESS, "ai.markup.diagnostic.model.access", "Missing model identifier");
        }
        if (configuration.getBaseUrl() == null || configuration.getBaseUrl().trim().isEmpty()) {
            return ValidationResult.invalid(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY, "ai.markup.diagnostic.endpoint.compatibility", "Missing endpoint base URL");
        }
        try {
            URI uri = new URI(configuration.getBaseUrl());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                return ValidationResult.invalid(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY, "ai.markup.diagnostic.endpoint.compatibility", "Endpoint base URL must be absolute HTTPS");
            }
            if (uri.getUserInfo() != null) {
                return ValidationResult.invalid(DiagnosticFailureCategory.CREDENTIALS, "ai.markup.diagnostic.credentials", "Endpoint URL must not contain embedded credentials");
            }
        } catch (Exception e) {
            return ValidationResult.invalid(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY, "ai.markup.diagnostic.endpoint.compatibility", "Invalid endpoint base URL");
        }
        if (configuration.getChatCompletionsPath() == null || configuration.getChatCompletionsPath().trim().isEmpty()) {
            return ValidationResult.invalid(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY, "ai.markup.diagnostic.endpoint.compatibility", "Missing chat completions path");
        }
        if (!configuration.getChatCompletionsPath().startsWith("/")) {
            return ValidationResult.invalid(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY, "ai.markup.diagnostic.endpoint.compatibility", "Chat completions path must start with /");
        }
        if (configuration.getTimeoutMs() <= 0) {
            return ValidationResult.invalid(DiagnosticFailureCategory.MALFORMED_REQUEST, "ai.markup.diagnostic.malformed.request", "Timeout must be positive");
        }
        if (configuration.isProxyExpected()
            && !"true".equalsIgnoreCase(systemPropertyReader.getProperty("java.net.useSystemProxies", "false"))) {
            return ValidationResult.invalid(DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY, "ai.markup.diagnostic.connectivity.proxy", "System proxy discovery is disabled");
        }
        return ValidationResult.valid();
    }
}
