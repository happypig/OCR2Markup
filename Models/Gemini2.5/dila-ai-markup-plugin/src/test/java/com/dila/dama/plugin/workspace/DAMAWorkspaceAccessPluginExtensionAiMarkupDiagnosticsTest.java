package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
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

/**
 * Retargeted from the OpenAI path to CBRD Parse (004-cbrd-parse-endpoint). The read-only
 * guarantee from 002 is unchanged; what the command receives is not.
 */
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
        extension.setAiMarkupDiagnosticsCommandForTests(succeedingCommand());

        extension.runAiMarkup("selected text");

        assertNoSettingsMutated(optionsStorage);
    }

    @Test
    public void failedRunNeverMutatesStoredSettings() {
        WSOptionsStorage optionsStorage = stubbedOptionsStorage();
        extension.setOptionStorageForTests(optionsStorage);
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(any(CbrdParseRequest.class), any(CbrdParseConfiguration.class), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.failure(
                "ai.markup.error.unauthorized",
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
        RunAiMarkupDiagnosticsCommand command = succeedingCommand();
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("selected text");
        extension.runAiMarkup("selected text again");

        ArgumentCaptor<CbrdParseConfiguration> configurationCaptor = ArgumentCaptor.forClass(CbrdParseConfiguration.class);
        verify(command, Mockito.times(2)).execute(any(CbrdParseRequest.class), configurationCaptor.capture(), anyString());
        for (CbrdParseConfiguration configuration : configurationCaptor.getAllValues()) {
            assertThat(configuration.getEndpointUrl()).isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
            assertThat(configuration.getSharedToken()).isEqualTo("shared-token-9876");
            assertThat(configuration.getTimeoutMs()).isEqualTo(30000);
        }
        assertNoSettingsMutated(optionsStorage);
    }

    @Test
    public void obsoleteOpenAiPreferenceValuesAreNeverSentToTheService() {
        // FR-004/FR-005: values left in storage by a previous version are ignored, not migrated.
        extension.setOptionStorageForTests(stubbedOptionsStorage());
        RunAiMarkupDiagnosticsCommand command = succeedingCommand();
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("selected text");

        ArgumentCaptor<CbrdParseConfiguration> captor = ArgumentCaptor.forClass(CbrdParseConfiguration.class);
        verify(command).execute(any(CbrdParseRequest.class), captor.capture(), anyString());
        CbrdParseConfiguration configuration = captor.getValue();
        assertThat(configuration.getEndpointUrl()).doesNotContain("api.openai.com");
        assertThat(configuration.getSharedToken()).isNotEqualTo("sk-example-key");
    }

    @Test
    public void requestCarriesTheSelectionAndALanguageIndicator() {
        extension.setOptionStorageForTests(stubbedOptionsStorage());
        RunAiMarkupDiagnosticsCommand command = succeedingCommand();
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("selected text");

        ArgumentCaptor<CbrdParseRequest> captor = ArgumentCaptor.forClass(CbrdParseRequest.class);
        verify(command).execute(captor.capture(), any(CbrdParseConfiguration.class), anyString());
        assertThat(captor.getValue().getText()).isEqualTo("selected text");
        assertThat(captor.getValue().getLang()).isIn("zh", "jp");
    }

    private RunAiMarkupDiagnosticsCommand succeedingCommand() {
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(any(CbrdParseRequest.class), any(CbrdParseConfiguration.class), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.success("<ref><ptr target='x'/></ref>", session()));
        return command;
    }

    private void assertNoSettingsMutated(WSOptionsStorage optionsStorage) {
        verify(optionsStorage, never()).setOption(anyString(), anyString());
        verify(optionsStorage, never()).setSecretOption(anyString(), anyString());
        verify(optionsStorage, never()).setStringArrayOption(Mockito.anyString(), Mockito.any(String[].class));
        verify(optionsStorage, never()).setPersistentObjectOption(anyString(), any());
    }

    /**
     * Deliberately still returns the obsolete OpenAI values as well, so the tests above can
     * prove they are ignored rather than merely absent.
     */
    private WSOptionsStorage stubbedOptionsStorage() {
        WSOptionsStorage optionsStorage = mock(WSOptionsStorage.class);
        when(optionsStorage.getOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_API_URL.equals(key)) {
                return "https://cbss.dila.edu.tw/cbrd/parse";
            }
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TIMEOUT_MS.equals(key)) {
                return "30000";
            }
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
        when(optionsStorage.getSecretOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN.equals(key)) {
                return "shared-token-9876";
            }
            if (DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_KEY.equals(key)) {
                return "sk-example-key";
            }
            return invocation.getArgument(1);
        });
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
            "ai.markup.error.unauthorized",
            DiagnosticFailureCategory.CREDENTIALS,
            record(),
            session()
        ));

        assertThat(extension.getResultAreaForTests().getText()).contains("ai.markup.error.unauthorized");
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
            "ai.markup.error.unauthorized",
            10L,
            true
        );
    }

    private CbrdParseConfiguration configuration() {
        return new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "shared-token-9876");
    }
}
