package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T041 — AI Markup sends a language indicator with no editor configuration (US3).
 *
 * With no Oxygen editor attached the resolver cannot read a document, which is precisely the
 * "no usable xml:lang" case: the request must still go out, carrying the Chinese default.
 */
public class AIMarkupLanguageIntegrationTest {

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
        extension.setOptionStorageForTests(AIMarkupSuccessFlowTest.configuredStorage());
    }

    @Test
    public void requestAlwaysCarriesALanguageIndicator() {
        RunAiMarkupDiagnosticsCommand command = succeedingCommand();
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("(T 1442)");

        ArgumentCaptor<CbrdParseRequest> captor = ArgumentCaptor.forClass(CbrdParseRequest.class);
        verify(command).execute(captor.capture(), any(CbrdParseConfiguration.class), anyString());
        assertThat(captor.getValue().getLang()).isNotEmpty();
    }

    @Test
    public void unavailableDocumentFallsBackToTheChineseDefault() {
        RunAiMarkupDiagnosticsCommand command = succeedingCommand();
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("(T 1442)");

        ArgumentCaptor<CbrdParseRequest> captor = ArgumentCaptor.forClass(CbrdParseRequest.class);
        verify(command).execute(captor.capture(), any(CbrdParseConfiguration.class), anyString());
        assertThat(captor.getValue().getLang()).isEqualTo("zh");
    }

    @Test
    public void languageResolutionNeverThrowsWhenNoEditorIsAttached() {
        assertThat(extension.fetchCurrentDocumentXml()).isNull();
        assertThat(extension.resolveAiMarkupLanguage()).isEqualTo("zh");
    }

    @Test
    public void resolvedLanguageIsOneTheServiceAccepts() {
        assertThat(extension.resolveAiMarkupLanguage()).isIn("zh", "jp");
    }

    @Test
    public void theEditorIsNeverAskedToPickALanguage() {
        // No preference key exists for language: it is inferred, never configured (FR-007).
        RunAiMarkupDiagnosticsCommand command = succeedingCommand();
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("(T 1442)");

        ArgumentCaptor<CbrdParseConfiguration> captor = ArgumentCaptor.forClass(CbrdParseConfiguration.class);
        verify(command).execute(any(CbrdParseRequest.class), captor.capture(), anyString());
        assertThat(captor.getValue().toString()).doesNotContain("lang");
    }

    private RunAiMarkupDiagnosticsCommand succeedingCommand() {
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(any(CbrdParseRequest.class), any(CbrdParseConfiguration.class), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.success("<ref/>", session()));
        return command;
    }

    private AiMarkupDiagnosticSession session() {
        return new AiMarkupDiagnosticSession(
            10, new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "token"));
    }
}
