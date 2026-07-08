package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class DAMAWorkspaceAccessPluginExtensionExportDiagnosticsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exportsManualDiagnosticsWithoutSecrets() throws Exception {
        DAMAWorkspaceAccessPluginExtension extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
        extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.failure(
            "ai.markup.diagnostic.credentials.windows",
            DiagnosticFailureCategory.CREDENTIALS,
            record(),
            session()
        ));
        File output = temporaryFolder.newFile("diagnostics.json");

        boolean success = extension.exportDiagnostics(output, "manual_support_export");

        assertThat(success).isTrue();
        String content = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        assertThat(new JSONObject(content).getJSONObject("record").getBoolean("redactionApplied")).isTrue();
        assertThat(content).doesNotContain("sk-secret");
    }

    private AiMarkupDiagnosticSession session() {
        return new AiMarkupDiagnosticSession(10, configuration());
    }

    private SanitizedTroubleshootingRecord record() {
        return new SanitizedTroubleshootingRecord(
            "request-1",
            "windows",
            configuration().getEndpointSummary(),
            "snapshot Authorization: Bearer ****",
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
            "sk-secret",
            30000,
            MarkupServiceConfiguration.ENDPOINT_KIND_OPENAI_HOSTED,
            true
        );
    }
}
