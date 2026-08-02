package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.ParseError;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T008 — every ParseError maps to its own actionable guidance key (FR-011, FR-012, FR-013).
 */
public class CbrdParseErrorClassifierTest {

    private final CbrdParseErrorClassifier classifier = new CbrdParseErrorClassifier();

    @Test
    public void classifiesEveryEnumeratedServiceCause() {
        for (ParseError error : ParseError.values()) {
            CbrdParseErrorClassifier.Classification classification = classifier.classify(error);

            assertThat(classification).isNotNull();
            assertThat(classification.getGuidanceMessageKey()).isEqualTo(error.getGuidanceMessageKey());
            assertThat(classification.getCategory()).isEqualTo(error.getFailureCategory());
        }
    }

    @Test
    public void guidanceKeyIsDistinctForEveryCause() {
        long distinct = java.util.Arrays.stream(ParseError.values())
            .map(error -> classifier.classify(error).getGuidanceMessageKey())
            .distinct()
            .count();

        assertThat(distinct).isEqualTo(ParseError.values().length);
    }

    @Test
    public void serviceRejectedTokenDoesNotReuseTheMissingTokenKey() {
        // FR-010: "token not configured" and "token rejected" are different situations.
        assertThat(classifier.classify(ParseError.UNAUTHORIZED).getGuidanceMessageKey())
            .isNotEqualTo(CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY);
    }

    @Test
    public void connectivityGuidanceIsDistinctFromEveryServiceSideCause() {
        String connectivity = classifier.classify(ParseError.CONNECTIVITY_FAILURE).getGuidanceMessageKey();

        assertThat(connectivity).isNotEqualTo(classifier.classify(ParseError.OPENAI_UNAVAILABLE).getGuidanceMessageKey());
        assertThat(connectivity).isNotEqualTo(classifier.classify(ParseError.PARSE_API_NOT_CONFIGURED).getGuidanceMessageKey());
        assertThat(connectivity).isNotEqualTo(classifier.classify(ParseError.UNEXPECTED_RESPONSE).getGuidanceMessageKey());
    }

    @Test
    public void unexpectedResponseIsTheGenericFallback() {
        CbrdParseErrorClassifier.Classification classification = classifier.classify(ParseError.UNEXPECTED_RESPONSE);

        assertThat(classification.getGuidanceMessageKey()).isEqualTo("ai.markup.error.unexpected");
        assertThat(classification.getCategory()).isEqualTo(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE);
    }

    @Test
    public void nullErrorFallsBackToTheGenericGuidance() {
        CbrdParseErrorClassifier.Classification classification = classifier.classify(null);

        assertThat(classification.getGuidanceMessageKey()).isEqualTo("ai.markup.error.unexpected");
        assertThat(classification.getCategory()).isEqualTo(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE);
    }

    @Test
    public void exposesTheLocalGuardKeysUsedBeforeAnyRequestIsSent() {
        assertThat(CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY).isEqualTo("ai.markup.error.token_not_configured");
        assertThat(CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY).isEqualTo("ai.markup.error.endpoint_url_invalid");
    }

    @Test
    public void localGuardKeysDoNotCollideWithAnyServiceCauseKey() {
        for (ParseError error : ParseError.values()) {
            assertThat(error.getGuidanceMessageKey()).isNotEqualTo(CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY);
            assertThat(error.getGuidanceMessageKey()).isNotEqualTo(CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY);
        }
    }
}
