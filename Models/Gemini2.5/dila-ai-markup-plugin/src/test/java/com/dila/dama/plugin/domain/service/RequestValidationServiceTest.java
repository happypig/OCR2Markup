package com.dila.dama.plugin.domain.service;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestValidationServiceTest {

    @Test
    public void rejectsMissingApiKey() {
        RequestValidationService service = serviceWithProxySetting("true");
        RequestValidationService.ValidationResult result = service.validate(configuration("", "gpt-test", true), "t1,n1");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getCategory()).isEqualTo(DiagnosticFailureCategory.CREDENTIALS);
    }

    @Test
    public void rejectsInvalidEndpointBaseUrl() {
        RequestValidationService service = serviceWithProxySetting("true");
        MarkupServiceConfiguration configuration = new MarkupServiceConfiguration(
            "http://localhost",
            "/v1/chat/completions",
            "gpt-test",
            "sk-example-key",
            30000,
            MarkupServiceConfiguration.ENDPOINT_KIND_OPENAI_COMPATIBLE,
            true
        );

        RequestValidationService.ValidationResult result = service.validate(configuration, "t1,n1");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getCategory()).isEqualTo(DiagnosticFailureCategory.ENDPOINT_COMPATIBILITY);
    }

    @Test
    public void rejectsWhenSystemProxyDiscoveryExpectedButDisabled() {
        RequestValidationService service = serviceWithProxySetting("false");

        RequestValidationService.ValidationResult result = service.validate(configuration("sk-example-key", "gpt-test", true), "t1,n1");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getCategory()).isEqualTo(DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY);
    }

    @Test
    public void acceptsValidOpenAiCompatibleConfiguration() {
        RequestValidationService service = serviceWithProxySetting("true");
        RequestValidationService.ValidationResult result = service.validate(configuration("sk-example-key", "gpt-test", false), "t1,n1");

        assertThat(result.isValid()).isTrue();
    }

    private RequestValidationService serviceWithProxySetting(final String proxySetting) {
        return new RequestValidationService((name, defaultValue) -> proxySetting);
    }

    private MarkupServiceConfiguration configuration(String apiKey, String model, boolean proxyExpected) {
        return new MarkupServiceConfiguration(
            "https://api.openai.com",
            "/v1/chat/completions",
            model,
            apiKey,
            30000,
            MarkupServiceConfiguration.ENDPOINT_KIND_OPENAI_HOSTED,
            proxyExpected
        );
    }
}
