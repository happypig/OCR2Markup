package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.ParseError;
import com.dila.dama.plugin.domain.service.CbrdParseErrorClassifier;
import com.dila.dama.plugin.domain.service.DiagnosticClassifier;
import com.dila.dama.plugin.domain.service.RequestValidationService;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import com.dila.dama.plugin.infrastructure.api.CapturingConnectionFactory;
import com.dila.dama.plugin.infrastructure.api.CbrdParseApiClient;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import com.dila.dama.plugin.infrastructure.logging.SanitizedDiagnosticLogger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T045 — every enumerated DILA cause produces its own actionable message end to end
 * (FR-011, US4 scenarios 1-4).
 */
public class AIMarkupFailureMappingTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void everyServiceCodeYieldsItsOwnGuidanceKeyInTheResultArea() {
        assertThat(guidanceShownFor(400, "text_is_required")).contains("ai.markup.error.text_is_required");
        assertThat(guidanceShownFor(400, "text_is_too_long")).contains("ai.markup.error.text_is_too_long");
        assertThat(guidanceShownFor(400, "unsupported_language")).contains("ai.markup.error.unsupported_language");
        assertThat(guidanceShownFor(401, "unauthorized")).contains("ai.markup.error.unauthorized");
        assertThat(guidanceShownFor(503, "parse_api_not_configured")).contains("ai.markup.error.parse_api_not_configured");
        assertThat(guidanceShownFor(503, "openai_credentials_unavailable")).contains("ai.markup.error.openai_credentials_unavailable");
        assertThat(guidanceShownFor(503, "openai_rate_limited")).contains("ai.markup.error.openai_rate_limited");
        assertThat(guidanceShownFor(502, "openai_unavailable")).contains("ai.markup.error.openai_unavailable");
        assertThat(guidanceShownFor(422, "invalid_model_output")).contains("ai.markup.error.invalid_model_output");
    }

    @Test
    public void nineDistinctMessagesAreProducedForTheNineCauses() {
        List<String> messages = new ArrayList<>();
        messages.add(guidanceShownFor(400, "text_is_required"));
        messages.add(guidanceShownFor(400, "text_is_too_long"));
        messages.add(guidanceShownFor(400, "unsupported_language"));
        messages.add(guidanceShownFor(401, "unauthorized"));
        messages.add(guidanceShownFor(503, "parse_api_not_configured"));
        messages.add(guidanceShownFor(503, "openai_credentials_unavailable"));
        messages.add(guidanceShownFor(503, "openai_rate_limited"));
        messages.add(guidanceShownFor(502, "openai_unavailable"));
        messages.add(guidanceShownFor(422, "invalid_model_output"));

        assertThat(messages).doesNotHaveDuplicates();
    }

    @Test
    public void aRejectedTokenIsNotReportedAsAConnectivityProblem() {
        assertThat(guidanceShownFor(401, "unauthorized"))
            .isNotEqualTo(guidanceShownFor(0, null));
    }

    @Test
    public void aRejectedTokenIsNotReportedAsAMissingToken() {
        // FR-010: "token rejected" and "no token configured" are different situations.
        assertThat(guidanceShownFor(401, "unauthorized"))
            .doesNotContain(CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY);
    }

    @Test
    public void aFailureNeverShowsTheReplaceAffordance() {
        runFailure(401, "unauthorized");

        assertThat(extension.getReplaceButtonForTests().isVisible()).isFalse();
    }

    @Test
    public void aFailureOffersTheDiagnosticsExport() {
        runFailure(502, "openai_unavailable");

        assertThat(extension.getExportButtonForTests().isVisible()).isTrue();
    }

    @Test
    public void rateLimitingAndUpstreamOutageBothTellTheEditorToRetry() {
        assertThat(guidanceShownFor(503, "openai_rate_limited")).isNotEmpty();
        assertThat(guidanceShownFor(502, "openai_unavailable")).isNotEmpty();
        assertThat(guidanceShownFor(503, "openai_rate_limited"))
            .isNotEqualTo(guidanceShownFor(502, "openai_unavailable"));
    }

    private String guidanceShownFor(int status, String wireCode) {
        runFailure(status, wireCode);
        return extension.getResultAreaForTests().getText();
    }

    private void runFailure(int status, String wireCode) {
        CapturingConnectionFactory factory = wireCode == null
            ? CapturingConnectionFactory.throwing(new java.io.IOException("unreachable"))
            : CapturingConnectionFactory.failingWith(status, "{\"success\":false,\"error\":\"" + wireCode + "\"}");

        RunAiMarkupDiagnosticsCommand command = new RunAiMarkupDiagnosticsCommand(
            new RequestValidationService(),
            new DiagnosticClassifier(),
            new CbrdParseApiClient(factory),
            new SecretRedactor(),
            new SanitizedDiagnosticLogger()
        );

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(
            new CbrdParseRequest("(T 1442)", "zh"),
            new CbrdParseConfiguration(ENDPOINT, 30000, "token"),
            "windows"
        );
        extension.completeAiMarkupOperation(result);
    }

    @Test
    public void allElevenCausesAreRenderable() {
        for (ParseError error : ParseError.values()) {
            extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.failure(
                error.getGuidanceMessageKey(), error.getFailureCategory(), null, session()));

            assertThat(extension.getResultAreaForTests().getText()).isNotEmpty();
        }
    }

    private AiMarkupDiagnosticSession session() {
        return new AiMarkupDiagnosticSession(10, new CbrdParseConfiguration(ENDPOINT, 30000, "token"));
    }
}
