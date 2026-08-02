package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T008b — pre-flight validation for the CBRD Parse path (FR-010, FR-019, FR-021).
 *
 * These guards run before any request is sent, so each one must name its own cause.
 */
public class RequestValidationServiceCbrdParseTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";
    private static final String TEXT = "(T 1442)，大正23，頁869中";

    private final RequestValidationService service = new RequestValidationService();

    private static CbrdParseConfiguration config(String url, int timeoutMs, String token) {
        return new CbrdParseConfiguration(url, timeoutMs, token);
    }

    @Test
    public void fullyConfiguredRequestIsValid() {
        assertThat(service.validate(config(ENDPOINT, 30000, "token"), TEXT).isValid()).isTrue();
    }

    @Test
    public void missingConfigurationIsRejectedRatherThanThrowing() {
        RequestValidationService.ValidationResult result = service.validate((CbrdParseConfiguration) null, TEXT);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getCategory()).isEqualTo(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE);
    }

    @Test
    public void emptyEndpointUrlNamesTheEndpointPreference() {
        RequestValidationService.ValidationResult result = service.validate(config("", 30000, "token"), TEXT);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getGuidanceMessageKey()).isEqualTo(CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY);
        assertThat(result.getCategory()).isEqualTo(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY);
    }

    @Test
    public void malformedEndpointUrlNamesTheEndpointPreference() {
        assertThat(service.validate(config("not-a-url", 30000, "token"), TEXT).getGuidanceMessageKey())
            .isEqualTo(CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY);
        assertThat(service.validate(config("://missing-scheme", 30000, "token"), TEXT).getGuidanceMessageKey())
            .isEqualTo(CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY);
    }

    @Test
    public void unsupportedSchemeIsRejected() {
        assertThat(service.validate(config("ftp://cbss.dila.edu.tw/cbrd/parse", 30000, "token"), TEXT).isValid())
            .isFalse();
        assertThat(service.validate(config("file:///etc/passwd", 30000, "token"), TEXT).isValid())
            .isFalse();
    }

    @Test
    public void plainHttpIsAcceptedForOnPremiseDeployments() {
        assertThat(service.validate(config("http://cbrd.internal/cbrd/parse", 30000, "token"), TEXT).isValid())
            .isTrue();
    }

    @Test
    public void endpointUrlWithoutHostIsRejected() {
        assertThat(service.validate(config("https:///cbrd/parse", 30000, "token"), TEXT).getGuidanceMessageKey())
            .isEqualTo(CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY);
    }

    @Test
    public void endpointUrlWithEmbeddedCredentialsIsRejected() {
        assertThat(service.validate(config("https://user:pass@cbss.dila.edu.tw/cbrd/parse", 30000, "token"), TEXT).isValid())
            .isFalse();
    }

    @Test
    public void nonPositiveTimeoutIsRejected() {
        RequestValidationService.ValidationResult result = service.validate(config(ENDPOINT, 0, "token"), TEXT);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getCategory()).isEqualTo(DiagnosticFailureCategory.MALFORMED_REQUEST);
    }

    @Test
    public void missingTokenNamesTheTokenPreference() {
        RequestValidationService.ValidationResult result = service.validate(config(ENDPOINT, 30000, ""), TEXT);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getGuidanceMessageKey()).isEqualTo(CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY);
        assertThat(result.getCategory()).isEqualTo(DiagnosticFailureCategory.CREDENTIALS);
    }

    @Test
    public void tokenContainingWhitespaceIsRejectedBecauseItCannotFormAnAuthHeader() {
        assertThat(service.validate(config(ENDPOINT, 30000, "bad token"), TEXT).isValid()).isFalse();
    }

    @Test
    public void emptySelectionUsesTheServiceInputMissingGuidance() {
        RequestValidationService.ValidationResult result = service.validate(config(ENDPOINT, 30000, "token"), "");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getGuidanceMessageKey()).isEqualTo("ai.markup.error.text_is_required");
        assertThat(result.getCategory()).isEqualTo(DiagnosticFailureCategory.MALFORMED_REQUEST);
    }

    @Test
    public void whitespaceOnlySelectionCountsAsEmpty() {
        assertThat(service.validate(config(ENDPOINT, 30000, "token"), "   \n\t ").getGuidanceMessageKey())
            .isEqualTo("ai.markup.error.text_is_required");
        assertThat(service.validate(config(ENDPOINT, 30000, "token"), null).getGuidanceMessageKey())
            .isEqualTo("ai.markup.error.text_is_required");
    }

    @Test
    public void selectionOverFourThousandCharactersUsesTheTooLongGuidance() {
        String tooLong = repeat('好', RequestValidationService.MAX_SELECTION_LENGTH + 1);

        RequestValidationService.ValidationResult result = service.validate(config(ENDPOINT, 30000, "token"), tooLong);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getGuidanceMessageKey()).isEqualTo("ai.markup.error.text_is_too_long");
    }

    @Test
    public void selectionExactlyAtTheLimitIsAccepted() {
        String atLimit = repeat('好', RequestValidationService.MAX_SELECTION_LENGTH);

        assertThat(service.validate(config(ENDPOINT, 30000, "token"), atLimit).isValid()).isTrue();
    }

    @Test
    public void serviceInputLimitMatchesThePublishedContract() {
        assertThat(RequestValidationService.MAX_SELECTION_LENGTH).isEqualTo(4000);
    }

    @Test
    public void configurationProblemsAreReportedBeforeInputProblems() {
        // Endpoint beats token, token beats selection: the editor fixes the deeper cause first.
        assertThat(service.validate(config("not-a-url", 30000, ""), "").getGuidanceMessageKey())
            .isEqualTo(CbrdParseErrorClassifier.ENDPOINT_URL_INVALID_KEY);
        assertThat(service.validate(config(ENDPOINT, 30000, ""), "").getGuidanceMessageKey())
            .isEqualTo(CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY);
    }

    private static String repeat(char c, int times) {
        StringBuilder builder = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
