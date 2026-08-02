package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.DiagnosticStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest {

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
        extension.setAiMarkupOperationContextForTests();
    }

    @Test
    public void successfulCompletionIsIdenticalAcrossWindowsAndMacos() {
        RunAiMarkupDiagnosticsCommand command = new RunAiMarkupDiagnosticsCommand(
            new RequestValidationService(),
            new DiagnosticClassifier(),
            new CbrdParseApiClient(CapturingConnectionFactory.respondingWith(200, "<ref>markup content</ref>")),
            new SecretRedactor(),
            new SanitizedDiagnosticLogger()
        );

        CbrdParseRequest request = new CbrdParseRequest("selected text", "zh");
        RunAiMarkupDiagnosticsCommand.Result windowsResult = command.execute(request, successConfiguration(), "windows");
        RunAiMarkupDiagnosticsCommand.Result macosResult = command.execute(request, successConfiguration(), "macos");

        assertThat(windowsResult.isSuccess()).isTrue();
        assertThat(macosResult.isSuccess()).isTrue();
        assertThat(windowsResult.getMarkupResult()).isEqualTo(macosResult.getMarkupResult());
        assertThat(windowsResult.getSession().getStatus()).isEqualTo(DiagnosticStatus.COMPLETED_SUCCESS);
        assertThat(macosResult.getSession().getStatus()).isEqualTo(DiagnosticStatus.COMPLETED_SUCCESS);

        extension.completeAiMarkupOperation(windowsResult);
        String windowsRenderedText = extension.getResultAreaForTests().getText();
        boolean windowsReplaceVisible = extension.getReplaceButtonForTests().isVisible();
        boolean windowsExportVisible = extension.getExportButtonForTests().isVisible();

        extension.completeAiMarkupOperation(macosResult);
        String macosRenderedText = extension.getResultAreaForTests().getText();
        boolean macosReplaceVisible = extension.getReplaceButtonForTests().isVisible();
        boolean macosExportVisible = extension.getExportButtonForTests().isVisible();

        assertThat(windowsRenderedText).isEqualTo(macosRenderedText);
        assertThat(windowsReplaceVisible).isTrue().isEqualTo(macosReplaceVisible);
        assertThat(windowsExportVisible).isFalse().isEqualTo(macosExportVisible);
        assertThat(windowsRenderedText).doesNotContain("ai.markup.diagnostic");
    }

    @Test
    public void successfulRunLeavesNoResidualFailureStateForSubsequentRun() {
        extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.failure(
            "ai.markup.diagnostic.credentials.windows",
            DiagnosticFailureCategory.CREDENTIALS,
            record(),
            session()
        ));
        assertThat(extension.getExportButtonForTests().isVisible()).isTrue();

        extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.success("<ref><ptr target='x'/></ref>", session()));

        assertThat(extension.getResultAreaForTests().getText()).isEqualTo("<ref><ptr target='x'/></ref>");
        assertThat(extension.getReplaceButtonForTests().isVisible()).isTrue();
        assertThat(extension.getExportButtonForTests().isVisible()).isFalse();
    }

    @Test
    public void suppressesConcurrentAiMarkupOperations() {
        assertThat(extension.tryStartAiMarkupOperationForTests()).isTrue();
        assertThat(extension.tryStartAiMarkupOperationForTests()).isFalse();

        extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.failure(
            "ai.markup.diagnostic.connectivity.proxy.windows",
            DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY,
            record(),
            session()
        ));

        assertThat(extension.isAiMarkupInProgressForTests()).isFalse();
    }

    @Test
    public void keepsFailureFeedbackAvailableWithoutReplaceAction() {
        extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.failure(
            "ai.markup.diagnostic.connectivity.proxy.windows",
            DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY,
            record(),
            session()
        ));

        assertThat(extension.getResultAreaForTests().getText()).contains("ai.markup.diagnostic.connectivity.proxy");
        assertThat(extension.getReplaceButtonForTests().isVisible()).isFalse();
    }

    private CbrdParseConfiguration successConfiguration() {
        return new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "shared-token-9876");
    }

    private AiMarkupDiagnosticSession session() {
        return new AiMarkupDiagnosticSession(10, configuration());
    }

    private SanitizedTroubleshootingRecord record() {
        return new SanitizedTroubleshootingRecord(
            "request-1",
            "windows",
            configuration().getEndpointSummary(),
            "snapshot",
            500,
            "sanitized-body",
            DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY,
            "ai.markup.diagnostic.connectivity.proxy.windows",
            10L,
            true
        );
    }

    private CbrdParseConfiguration configuration() {
        return new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "shared-token-9876");
    }
}
