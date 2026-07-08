package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.infrastructure.api.OpenAiErrorResponse;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DiagnosticClassifierTest {

    private final DiagnosticClassifier classifier = new DiagnosticClassifier();
    private final SecretRedactor redactor = new SecretRedactor();

    @Test
    public void classifiesUnauthorizedAsCredentials() {
        DiagnosticClassifier.Classification classification = classifier.classifyHttpFailure(401, OpenAiErrorResponse.parse("{\"error\":{\"message\":\"bad key\"}}", redactor), configuration(), "windows");

        assertThat(classification.getCategory()).isEqualTo(DiagnosticFailureCategory.CREDENTIALS);
        assertThat(classification.getGuidanceMessageKey()).endsWith(".windows");
    }

    @Test
    public void classifiesModelMissingAsModelAccess() {
        DiagnosticClassifier.Classification classification = classifier.classifyHttpFailure(
            400,
            OpenAiErrorResponse.parse("{\"error\":{\"message\":\"The model gpt-x does not exist\"}}", redactor),
            configuration(),
            "generic"
        );

        assertThat(classification.getCategory()).isEqualTo(DiagnosticFailureCategory.MODEL_ACCESS);
    }

    @Test
    public void classifiesEndpointMismatchSeparatelyFromMalformedRequest() {
        DiagnosticClassifier.Classification classification = classifier.classifyHttpFailure(
            400,
            OpenAiErrorResponse.parse("{\"error\":{\"message\":\"This endpoint does not support chat/completions. Use the Responses API.\"}}", redactor),
            configuration(),
            "generic"
        );

        assertThat(classification.getCategory()).isEqualTo(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY);
    }

    @Test
    public void classifiesRateLimitAndServerFailures() {
        DiagnosticClassifier.Classification rateLimit = classifier.classifyHttpFailure(429, OpenAiErrorResponse.parse("{\"error\":{\"message\":\"Too many requests\"}}", redactor), configuration(), "generic");
        DiagnosticClassifier.Classification serverFailure = classifier.classifyHttpFailure(503, OpenAiErrorResponse.parse("{\"error\":{\"message\":\"Service unavailable\"}}", redactor), configuration(), "generic");

        assertThat(rateLimit.getCategory()).isEqualTo(DiagnosticFailureCategory.RATE_LIMIT_OR_CAPACITY);
        assertThat(serverFailure.getCategory()).isEqualTo(DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE);
    }

    private MarkupServiceConfiguration configuration() {
        return new MarkupServiceConfiguration(
            "https://api.openai.com",
            "/v1/chat/completions",
            "gpt-test",
            "sk-example-key",
            30000,
            MarkupServiceConfiguration.ENDPOINT_KIND_OPENAI_HOSTED,
            true
        );
    }
}
