package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;

import java.net.URI;

public class RequestValidationService {

    /**
     * The DILA CBRD Parse service caps input at 4,000 characters
     * ({@code contracts/openapi.yaml}, {@code ParseRequest.text.maxLength}). Checking it here
     * avoids a round trip and gives the editor immediate feedback (FR-019).
     */
    public static final int MAX_SELECTION_LENGTH = 4000;

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

    /**
     * Pre-flight validation for the CBRD Parse path (FR-010, FR-019, FR-021).
     *
     * Configuration problems are reported before input problems so the editor fixes the deeper
     * cause first: a malformed endpoint beats a missing token, and a missing token beats an
     * unusable selection.
     */
    public ValidationResult validate(CbrdParseConfiguration configuration, String selectedText) {
        if (configuration == null) {
            return ValidationResult.invalid(
                DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE,
                "ai.markup.diagnostic.unknown",
                "Missing configuration"
            );
        }

        ValidationResult endpointResult = validateEndpointUrl(configuration.getEndpointUrl());
        if (!endpointResult.isValid()) {
            return endpointResult;
        }

        if (configuration.getTimeoutMs() <= 0) {
            return ValidationResult.invalid(
                DiagnosticFailureCategory.MALFORMED_REQUEST,
                "ai.markup.diagnostic.malformed.request",
                "Timeout must be positive"
            );
        }

        if (!configuration.hasSharedToken()) {
            return ValidationResult.invalid(
                DiagnosticFailureCategory.CREDENTIALS,
                CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY,
                "Missing shared parse token"
            );
        }
        if (containsWhitespace(configuration.getSharedToken())) {
            return ValidationResult.invalid(
                DiagnosticFailureCategory.CREDENTIALS,
                CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY,
                "Malformed shared parse token"
            );
        }

        if (selectedText == null || selectedText.trim().isEmpty()) {
            return ValidationResult.invalid(
                DiagnosticFailureCategory.MALFORMED_REQUEST,
                "ai.markup.error.text_is_required",
                "No selected text"
            );
        }
        if (selectedText.length() > MAX_SELECTION_LENGTH) {
            return ValidationResult.invalid(
                DiagnosticFailureCategory.MALFORMED_REQUEST,
                "ai.markup.error.text_is_too_long",
                "Selection exceeds " + MAX_SELECTION_LENGTH + " characters"
            );
        }

        return ValidationResult.valid();
    }

    private ValidationResult validateEndpointUrl(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            return ValidationResult.invalid(
                DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY,
                CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY,
                "Missing CBRD Parse endpoint URL"
            );
        }
        try {
            URI uri = new URI(endpointUrl);
            String scheme = uri.getScheme();
            boolean supportedScheme = "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
            if (!supportedScheme) {
                return ValidationResult.invalid(
                    DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY,
                    CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY,
                    "CBRD Parse endpoint URL must use http or https"
                );
            }
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                return ValidationResult.invalid(
                    DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY,
                    CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY,
                    "CBRD Parse endpoint URL has no host"
                );
            }
            if (uri.getUserInfo() != null) {
                return ValidationResult.invalid(
                    DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY,
                    CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY,
                    "CBRD Parse endpoint URL must not contain embedded credentials"
                );
            }
        } catch (Exception e) {
            return ValidationResult.invalid(
                DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY,
                CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY,
                "Invalid CBRD Parse endpoint URL"
            );
        }
        return ValidationResult.valid();
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

}
