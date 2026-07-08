package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.MarkupServiceConfiguration;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import com.dila.dama.plugin.preferences.DAMAOptionPagePluginExtension;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ro.sync.exml.workspace.api.options.WSOptionsStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DAMAWorkspaceAccessPluginExtensionAiMarkupDiagnosticsTest {

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void successfulRunNeverMutatesStoredSettings() {
        WSOptionsStorage optionsStorage = stubbedOptionsStorage();
        extension.setOptionStorageForTests(optionsStorage);
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(anyString(), any(MarkupServiceConfiguration.class), anyString(), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.success("<ref><ptr target='x'/></ref>", session()));
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("selected text");

        assertNoSettingsMutated(optionsStorage);
    }

    @Test
    public void failedRunNeverMutatesStoredSettings() {
        WSOptionsStorage optionsStorage = stubbedOptionsStorage();
        extension.setOptionStorageForTests(optionsStorage);
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(anyString(), any(MarkupServiceConfiguration.class), anyString(), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.failure(
                "ai.markup.diagnostic.credentials.windows",
                DiagnosticFailureCategory.CREDENTIALS,
                record(),
                session()
            ));
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("selected text");

        assertNoSettingsMutated(optionsStorage);
    }

    @Test
    public void requestShapePassedToCommandMatchesStoredReadOnlyConfiguration() {
        WSOptionsStorage optionsStorage = stubbedOptionsStorage();
        extension.setOptionStorageForTests(optionsStorage);
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(anyString(), any(MarkupServiceConfiguration.class), anyString(), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.success("<ref><ptr target='x'/></ref>", session()));
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("selected text");
        extension.runAiMarkup("selected text again");

        ArgumentCaptor<MarkupServiceConfiguration> configurationCaptor = ArgumentCaptor.forClass(MarkupServiceConfiguration.class);
        verify(command, Mockito.times(2)).execute(anyString(), configurationCaptor.capture(), anyString(), anyString());
        for (MarkupServiceConfiguration configuration : configurationCaptor.getAllValues()) {
            assertThat(configuration.getBaseUrl()).isEqualTo("https://api.openai.com");
            assertThat(configuration.getChatCompletionsPath()).isEqualTo("/v1/chat/completions");
            assertThat(configuration.getModelName()).isEqualTo("gpt-test");
            assertThat(configuration.getApiKey()).isEqualTo("sk-example-key");
        }
        assertNoSettingsMutated(optionsStorage);
    }

    private void assertNoSettingsMutated(WSOptionsStorage optionsStorage) {
        verify(optionsStorage, never()).setOption(anyString(), anyString());
        verify(optionsStorage, never()).setSecretOption(anyString(), anyString());
        verify(optionsStorage, never()).setStringArrayOption(Mockito.anyString(), Mockito.any(String[].class));
        verify(optionsStorage, never()).setPersistentObjectOption(anyString(), any());
    }

    private WSOptionsStorage stubbedOptionsStorage() {
        WSOptionsStorage optionsStorage = mock(WSOptionsStorage.class);
        when(optionsStorage.getOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_BASE_URL.equals(key)) {
                return "https://api.openai.com";
            }
            if (DAMAOptionPagePluginExtension.KEY_DILA_DAMA_CHAT_COMPLETIONS_PATH.equals(key)) {
                return "/v1/chat/completions";
            }
            if (DAMAOptionPagePluginExtension.KEY_DILA_DAMA_FT_PARSE_MODEL.equals(key)) {
                return "gpt-test";
            }
            if (DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_TIMEOUT_MS.equals(key)) {
                return "30000";
            }
            return invocation.getArgument(1);
        });
        when(optionsStorage.getSecretOption(anyString(), anyString())).thenReturn("sk-example-key");
        return optionsStorage;
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
