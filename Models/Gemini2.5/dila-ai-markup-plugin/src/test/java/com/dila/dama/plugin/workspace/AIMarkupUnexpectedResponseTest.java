package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.service.DiagnosticClassifier;
import com.dila.dama.plugin.domain.service.RequestValidationService;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import com.dila.dama.plugin.infrastructure.api.CapturingConnectionFactory;
import com.dila.dama.plugin.infrastructure.api.CbrdParseApiClient;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import com.dila.dama.plugin.infrastructure.logging.SanitizedDiagnosticLogger;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T047 — a response the contract does not enumerate degrades to a generic message with no
 * crash and no document mutation (FR-012, US4 scenario 8).
 */
public class AIMarkupUnexpectedResponseTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void unknownStatusCodeYieldsTheGenericGuidance() {
        assertThat(guidanceFor(418, "{\"success\":false,\"error\":\"i_am_a_teapot\"}"))
            .contains("ai.markup.error.unexpected");
    }

    @Test
    public void nonJsonBodyYieldsTheGenericGuidance() {
        assertThat(guidanceFor(500, "<html><body>Gateway Error</body></html>"))
            .contains("ai.markup.error.unexpected");
    }

    @Test
    public void emptyBodyYieldsTheGenericGuidance() {
        assertThat(guidanceFor(400, "")).contains("ai.markup.error.unexpected");
    }

    @Test
    public void jsonWithoutAnErrorFieldYieldsTheGenericGuidance() {
        assertThat(guidanceFor(400, "{\"success\":false}")).contains("ai.markup.error.unexpected");
    }

    @Test
    public void anUnexpectedResponseNeverOffersReplacement() {
        guidanceFor(418, "nonsense");

        assertThat(extension.getReplaceButtonForTests().isVisible()).isFalse();
    }

    @Test
    public void anUnexpectedResponseDoesNotCrashTheOperationGuard() {
        guidanceFor(418, "nonsense");

        assertThat(extension.isAiMarkupInProgressForTests()).isFalse();
    }

    @Test
    public void noMarkupIsProducedForAnUnexpectedResponse() {
        RunAiMarkupDiagnosticsCommand.Result result = resultFor(418, "nonsense");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMarkupResult()).isEmpty();
    }

    private String guidanceFor(int status, String body) {
        extension.completeAiMarkupOperation(resultFor(status, body));
        return extension.getResultAreaForTests().getText();
    }

    private RunAiMarkupDiagnosticsCommand.Result resultFor(int status, String body) {
        RunAiMarkupDiagnosticsCommand command = new RunAiMarkupDiagnosticsCommand(
            new RequestValidationService(),
            new DiagnosticClassifier(),
            new CbrdParseApiClient(CapturingConnectionFactory.failingWith(status, body)),
            new SecretRedactor(),
            new SanitizedDiagnosticLogger()
        );
        return command.execute(
            new CbrdParseRequest("(T 1442)", "zh"),
            new CbrdParseConfiguration(ENDPOINT, 30000, "token"),
            "windows"
        );
    }
}
