package com.dila.dama.plugin.domain.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T005 — the nine service-enumerated codes plus the two local categories (FR-011, FR-012, FR-013).
 * Wire codes come from contracts/openapi.yaml.
 */
public class ParseErrorTest {

    @Test
    public void enumeratesTheNineServiceCodesPlusTwoLocalCategories() {
        assertThat(ParseError.values()).hasSize(11);
    }

    @Test
    public void mapsEveryDocumentedWireCode() {
        assertThat(ParseError.fromWireCode("text_is_required")).isEqualTo(ParseError.TEXT_IS_REQUIRED);
        assertThat(ParseError.fromWireCode("text_is_too_long")).isEqualTo(ParseError.TEXT_IS_TOO_LONG);
        assertThat(ParseError.fromWireCode("unsupported_language")).isEqualTo(ParseError.UNSUPPORTED_LANGUAGE);
        assertThat(ParseError.fromWireCode("unauthorized")).isEqualTo(ParseError.UNAUTHORIZED);
        assertThat(ParseError.fromWireCode("parse_api_not_configured")).isEqualTo(ParseError.PARSE_API_NOT_CONFIGURED);
        assertThat(ParseError.fromWireCode("openai_credentials_unavailable")).isEqualTo(ParseError.OPENAI_CREDENTIALS_UNAVAILABLE);
        assertThat(ParseError.fromWireCode("openai_rate_limited")).isEqualTo(ParseError.OPENAI_RATE_LIMITED);
        assertThat(ParseError.fromWireCode("openai_unavailable")).isEqualTo(ParseError.OPENAI_UNAVAILABLE);
        assertThat(ParseError.fromWireCode("invalid_model_output")).isEqualTo(ParseError.INVALID_MODEL_OUTPUT);
    }

    @Test
    public void wireCodeLookupIsCaseInsensitiveAndTrims() {
        assertThat(ParseError.fromWireCode("  UNAUTHORIZED  ")).isEqualTo(ParseError.UNAUTHORIZED);
    }

    @Test
    public void unknownWireCodeBecomesUnexpectedResponse() {
        assertThat(ParseError.fromWireCode("something_new")).isEqualTo(ParseError.UNEXPECTED_RESPONSE);
        assertThat(ParseError.fromWireCode(null)).isEqualTo(ParseError.UNEXPECTED_RESPONSE);
        assertThat(ParseError.fromWireCode("")).isEqualTo(ParseError.UNEXPECTED_RESPONSE);
    }

    @Test
    public void everyErrorCarriesADistinctGuidanceKey() {
        assertThat(ParseError.TEXT_IS_REQUIRED.getGuidanceMessageKey()).isEqualTo("ai.markup.error.text_is_required");
        assertThat(ParseError.TEXT_IS_TOO_LONG.getGuidanceMessageKey()).isEqualTo("ai.markup.error.text_is_too_long");
        assertThat(ParseError.UNSUPPORTED_LANGUAGE.getGuidanceMessageKey()).isEqualTo("ai.markup.error.unsupported_language");
        assertThat(ParseError.UNAUTHORIZED.getGuidanceMessageKey()).isEqualTo("ai.markup.error.unauthorized");
        assertThat(ParseError.PARSE_API_NOT_CONFIGURED.getGuidanceMessageKey()).isEqualTo("ai.markup.error.parse_api_not_configured");
        assertThat(ParseError.OPENAI_CREDENTIALS_UNAVAILABLE.getGuidanceMessageKey()).isEqualTo("ai.markup.error.openai_credentials_unavailable");
        assertThat(ParseError.OPENAI_RATE_LIMITED.getGuidanceMessageKey()).isEqualTo("ai.markup.error.openai_rate_limited");
        assertThat(ParseError.OPENAI_UNAVAILABLE.getGuidanceMessageKey()).isEqualTo("ai.markup.error.openai_unavailable");
        assertThat(ParseError.INVALID_MODEL_OUTPUT.getGuidanceMessageKey()).isEqualTo("ai.markup.error.invalid_model_output");
        assertThat(ParseError.UNEXPECTED_RESPONSE.getGuidanceMessageKey()).isEqualTo("ai.markup.error.unexpected");
        assertThat(ParseError.CONNECTIVITY_FAILURE.getGuidanceMessageKey()).isEqualTo("ai.markup.error.connectivity");
    }

    @Test
    public void guidanceKeysAreUniqueAcrossAllErrors() {
        long distinct = java.util.Arrays.stream(ParseError.values())
            .map(ParseError::getGuidanceMessageKey)
            .distinct()
            .count();

        assertThat(distinct).isEqualTo(ParseError.values().length);
    }

    @Test
    public void onlyUnauthorizedIsCategorisedAsTheEditorsCredentialProblem() {
        assertThat(ParseError.UNAUTHORIZED.getFailureCategory()).isEqualTo(DiagnosticFailureCategory.CREDENTIALS);
        // A server-side credential outage is not the editor's token being wrong.
        assertThat(ParseError.OPENAI_CREDENTIALS_UNAVAILABLE.getFailureCategory())
            .isEqualTo(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE);
    }

    @Test
    public void inputProblemsAreCategorisedAsMalformedRequests() {
        assertThat(ParseError.TEXT_IS_REQUIRED.getFailureCategory()).isEqualTo(DiagnosticFailureCategory.MALFORMED_REQUEST);
        assertThat(ParseError.TEXT_IS_TOO_LONG.getFailureCategory()).isEqualTo(DiagnosticFailureCategory.MALFORMED_REQUEST);
        assertThat(ParseError.UNSUPPORTED_LANGUAGE.getFailureCategory()).isEqualTo(DiagnosticFailureCategory.MALFORMED_REQUEST);
    }

    @Test
    public void rateLimitingIsCategorisedAsCapacity() {
        assertThat(ParseError.OPENAI_RATE_LIMITED.getFailureCategory()).isEqualTo(DiagnosticFailureCategory.RATE_LIMIT_OR_CAPACITY);
    }

    @Test
    public void connectivityFailureIsDistinctFromServiceSideFailures() {
        assertThat(ParseError.CONNECTIVITY_FAILURE.getFailureCategory()).isEqualTo(DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY);
        assertThat(ParseError.CONNECTIVITY_FAILURE.getFailureCategory())
            .isNotEqualTo(ParseError.OPENAI_UNAVAILABLE.getFailureCategory());
    }

    @Test
    public void localCategoriesHaveNoWireCode() {
        assertThat(ParseError.UNEXPECTED_RESPONSE.getWireCode()).isEmpty();
        assertThat(ParseError.CONNECTIVITY_FAILURE.getWireCode()).isEmpty();
        assertThat(ParseError.UNAUTHORIZED.getWireCode()).isEqualTo("unauthorized");
    }
}
