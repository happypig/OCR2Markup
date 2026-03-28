package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest {

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void showsReplaceButtonForSuccessfulAiMarkupResult() {
        extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.success("<ref><ptr target='x'/></ref>", session()));

        assertThat(extension.getResultAreaForTests().getText()).isEqualTo("<ref><ptr target='x'/></ref>");
        assertThat(extension.getReplaceButtonForTests().isVisible()).isTrue();
        assertThat(extension.getExportButtonForTests().isVisible()).isFalse();
    }

    @Test
    public void showsExportButtonForFailedDiagnosticResult() {
        extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.failure(
            "ai.markup.diagnostic.credentials.windows",
            DiagnosticFailureCategory.CREDENTIALS,
            record(),
            session()
        ));

        assertThat(extension.getResultAreaForTests().getText()).contains("ai.markup.diagnostic.credentials");
        assertThat(extension.getReplaceButtonForTests().isVisible()).isFalse();
        assertThat(extension.getExportButtonForTests().isVisible()).isTrue();
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
            401,
            "sanitized-body",
            DiagnosticFailureCategory.CREDENTIALS,
            "ai.markup.diagnostic.credentials.windows",
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
