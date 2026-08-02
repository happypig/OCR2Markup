package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.ParseError;

/**
 * Turns a {@link ParseError} into the actionable guidance the result area shows (FR-011,
 * FR-012, FR-013).
 *
 * Replaces the OpenAI-shaped classification for the AI Markup path. Each cause keeps its own
 * message: an editor who has not configured a token and an editor whose token was rejected
 * need different instructions.
 */
public class CbrdParseErrorClassifier {

    /** Shown before any request is sent, when no token is configured (FR-010). */
    public static final String TOKEN_NOT_CONFIGURED_KEY = "ai.markup.error.token_not_configured";

    /** Shown before any request is sent, when the endpoint preference is unusable (FR-021). */
    public static final String ENDPOINT_URL_INVALID_KEY = "ai.markup.error.endpoint_url_invalid";

    public static final class Classification {
        private final DiagnosticFailureCategory category;
        private final String guidanceMessageKey;

        Classification(DiagnosticFailureCategory category, String guidanceMessageKey) {
            this.category = category;
            this.guidanceMessageKey = guidanceMessageKey;
        }

        public DiagnosticFailureCategory getCategory() {
            return category;
        }

        public String getGuidanceMessageKey() {
            return guidanceMessageKey;
        }
    }

    public Classification classify(ParseError error) {
        if (error == null) {
            return new Classification(
                ParseError.UNEXPECTED_RESPONSE.getFailureCategory(),
                ParseError.UNEXPECTED_RESPONSE.getGuidanceMessageKey()
            );
        }
        return new Classification(error.getFailureCategory(), error.getGuidanceMessageKey());
    }
}
