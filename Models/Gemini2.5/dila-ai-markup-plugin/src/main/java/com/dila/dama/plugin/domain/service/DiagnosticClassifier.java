package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.infrastructure.api.OpenAiErrorResponse;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class DiagnosticClassifier {

    public static final class Classification {
        private final DiagnosticFailureCategory category;
        private final String guidanceMessageKey;
        private final int confidence;

        public Classification(DiagnosticFailureCategory category, String guidanceMessageKey, int confidence) {
            this.category = category;
            this.guidanceMessageKey = guidanceMessageKey;
            this.confidence = confidence;
        }

        public DiagnosticFailureCategory getCategory() {
            return category;
        }

        public String getGuidanceMessageKey() {
            return guidanceMessageKey;
        }

        public int getConfidence() {
            return confidence;
        }
    }

    public Classification classifyValidationFailure(RequestValidationService.ValidationResult validationResult) {
        return new Classification(validationResult.getCategory(), validationResult.getGuidanceMessageKey(), 100);
    }

    public Classification classifyHttpFailure(int httpStatus, OpenAiErrorResponse error, MarkupServiceConfiguration configuration, String platform) {
        String detail = error == null ? "" : error.asSearchableText();
        if (httpStatus == 401) {
            return classify(DiagnosticFailureCategory.CREDENTIALS, "ai.markup.diagnostic.credentials", platform, 100);
        }
        if (httpStatus == 403) {
            if (containsAny(detail, "model", "permission", "access")) {
                return classify(DiagnosticFailureCategory.MODEL_ACCESS, "ai.markup.diagnostic.model.access", platform, 95);
            }
            return classify(DiagnosticFailureCategory.CREDENTIALS, "ai.markup.diagnostic.credentials", platform, 80);
        }
        if (httpStatus == 404) {
            return classify(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY, "ai.markup.diagnostic.endpoint.compatibility", platform, 100);
        }
        if (httpStatus == 429) {
            return classify(DiagnosticFailureCategory.RATE_LIMIT_OR_CAPACITY, "ai.markup.diagnostic.rate.limit", platform, 100);
        }
        if (httpStatus >= 500) {
            return classify(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE, "ai.markup.diagnostic.service.failure", platform, 90);
        }
        if (httpStatus == 400) {
            if (containsAny(detail, "model", "does not exist", "not found", "not available")) {
                return classify(DiagnosticFailureCategory.MODEL_ACCESS, "ai.markup.diagnostic.model.access", platform, 95);
            }
            if (containsAny(detail, "chat/completions", "unsupported", "endpoint", "responses api")) {
                return classify(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY, "ai.markup.diagnostic.endpoint.compatibility", platform, 95);
            }
            return classify(DiagnosticFailureCategory.MALFORMED_REQUEST, "ai.markup.diagnostic.malformed.request", platform, 90);
        }
        if (containsAny(detail, "proxy", "tunnel", "connect")) {
            return classify(DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY, "ai.markup.diagnostic.connectivity.proxy", platform, 80);
        }
        return classify(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE, "ai.markup.diagnostic.unknown", platform, 50);
    }

    public Classification classifyException(Exception exception, String platform) {
        if (exception instanceof SocketTimeoutException) {
            return classify(DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY, "ai.markup.diagnostic.connectivity.proxy", platform, 90);
        }
        if (exception instanceof IOException) {
            return classify(DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY, "ai.markup.diagnostic.connectivity.proxy", platform, 80);
        }
        return classify(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE, "ai.markup.diagnostic.unknown", platform, 40);
    }

    public String resolvePlatformGuidanceKey(String baseKey, String platform) {
        if (baseKey == null) {
            return null;
        }
        if ("windows".equalsIgnoreCase(platform)) {
            return baseKey + ".windows";
        }
        if ("macos".equalsIgnoreCase(platform)) {
            return baseKey + ".macos";
        }
        return baseKey;
    }

    private Classification classify(DiagnosticFailureCategory category, String baseKey, String platform, int confidence) {
        return new Classification(category, resolvePlatformGuidanceKey(baseKey, platform), confidence);
    }

    private boolean containsAny(String text, String a, String b, String c) {
        String lower = text == null ? "" : text.toLowerCase();
        return lower.contains(a) || lower.contains(b) || lower.contains(c);
    }

    private boolean containsAny(String text, String a, String b, String c, String d) {
        String lower = text == null ? "" : text.toLowerCase();
        return lower.contains(a) || lower.contains(b) || lower.contains(c) || lower.contains(d);
    }
}
