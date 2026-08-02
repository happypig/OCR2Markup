package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
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
 * T047a — the 002 diagnostics affordance survives the transport swap, and the shared token
 * appears only as a fingerprint (FR-022, FR-003, NFR-004, US4 scenario 9).
 */
public class AIMarkupDiagnosticsExportTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";
    private static final String TOKEN = "super-secret-token-9876";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void aParseFailureProducesATroubleshootingRecord() {
        assertThat(failureResult().getTroubleshootingRecord()).isNotNull();
    }

    @Test
    public void theExportControlIsOfferedAfterAParseFailure() {
        extension.completeAiMarkupOperation(failureResult());

        assertThat(extension.getExportButtonForTests().isVisible()).isTrue();
    }

    @Test
    public void theExportControlIsNotOfferedAfterASuccess() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, "<ref/>");
        extension.completeAiMarkupOperation(commandBackedBy(factory).execute(
            new CbrdParseRequest("(T 1442)", "zh"), configuration(), "windows"));

        assertThat(extension.getExportButtonForTests().isVisible()).isFalse();
    }

    @Test
    public void theRecordNamesTheParseEndpointNotAnOpenAiEndpoint() {
        SanitizedTroubleshootingRecord record = failureResult().getTroubleshootingRecord();

        assertThat(record.getEndpointSummary()).contains(ENDPOINT);
        assertThat(record.getEndpointSummary()).doesNotContain("api.openai.com");
    }

    @Test
    public void theRecordCarriesTheTokenOnlyAsAFingerprint() {
        SanitizedTroubleshootingRecord record = failureResult().getTroubleshootingRecord();

        String rendered = record.getEndpointSummary() + " " + record.getRequestSnapshot()
            + " " + record.getServiceErrorBody();
        assertThat(rendered).doesNotContain(TOKEN);
        assertThat(rendered).contains("****9876");
    }

    @Test
    public void theRecordKeepsTheHttpStatusForSupport() {
        assertThat(failureResult().getTroubleshootingRecord().getHttpStatus()).isEqualTo(401);
    }

    private RunAiMarkupDiagnosticsCommand.Result failureResult() {
        return commandBackedBy(
            CapturingConnectionFactory.failingWith(401, "{\"success\":false,\"error\":\"unauthorized\"}"))
            .execute(new CbrdParseRequest("(T 1442)", "zh"), configuration(), "windows");
    }

    private RunAiMarkupDiagnosticsCommand commandBackedBy(CapturingConnectionFactory factory) {
        return new RunAiMarkupDiagnosticsCommand(
            new RequestValidationService(),
            new DiagnosticClassifier(),
            new CbrdParseApiClient(factory),
            new SecretRedactor(),
            new SanitizedDiagnosticLogger()
        );
    }

    private CbrdParseConfiguration configuration() {
        return new CbrdParseConfiguration(ENDPOINT, 30000, TOKEN);
    }
}
