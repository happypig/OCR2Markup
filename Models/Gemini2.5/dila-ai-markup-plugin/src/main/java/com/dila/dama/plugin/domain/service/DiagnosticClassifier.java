package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.ParseError;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class DiagnosticClassifier {

    private final CbrdParseErrorClassifier parseErrorClassifier = new CbrdParseErrorClassifier();

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

    /**
     * Classifies a CBRD Parse failure (FR-011, FR-012, FR-013). The DILA service enumerates its
     * causes, so the wire code maps straight to guidance with full confidence and no
     * platform-specific variant is needed.
     */
    public Classification classifyParseError(ParseError error) {
        CbrdParseErrorClassifier.Classification classification = parseErrorClassifier.classify(error);
        return new Classification(classification.getCategory(), classification.getGuidanceMessageKey(), 100);
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


}
