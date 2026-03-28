package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DAMAWorkspaceAccessPluginExtensionAsyncDiagnosticsTest {

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
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
