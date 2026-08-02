package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import com.dila.dama.plugin.preferences.DAMAOptionPagePluginExtension;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T020 — the AI Markup success path through the DILA service (US1 scenarios 1-2).
 */
public class AIMarkupSuccessFlowTest {

    private static final String MARKUP =
        "<ref>(<canon>T</canon> <w>1442</w>)，大正<v>23</v>，頁<p>869</p><c>中</c></ref>";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
        extension.setOptionStorageForTests(configuredStorage());
    }

    @Test
    public void returnedMarkupIsShownWithTheReplaceButton() {
        extension.completeAiMarkupOperation(
            RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession()));

        assertThat(extension.getResultAreaForTests().getText()).isEqualTo(MARKUP);
        assertThat(extension.getReplaceButtonForTests().isVisible()).isTrue();
        assertThat(extension.getExportButtonForTests().isVisible()).isFalse();
    }

    @Test
    public void markupIsOnlyOfferedForReviewNotAppliedAutomatically() {
        // The result area is a review surface: the markup lands there behind an explicit
        // Replace, never straight into the document (FR-008, US1 scenario 2).
        // The operation context is what gates the Replace affordance.
        extension.setAiMarkupOperationContextForTests();
        extension.completeAiMarkupOperation(
            RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession()));

        assertThat(extension.getResultAreaForTests().getText()).isEqualTo(MARKUP);
        assertThat(extension.isButtonPanelVisibleForTests()).isTrue();
        assertThat(extension.getReplaceButtonForTests().isVisible()).isTrue();
        assertThat(extension.getExportButtonForTests().isVisible()).isFalse();
    }

    @Test
    public void runAiMarkupSendsTheSelectionToTheParseEndpoint() {
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(any(CbrdParseRequest.class), any(CbrdParseConfiguration.class), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession()));
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        RunAiMarkupDiagnosticsCommand.Result result = extension.runAiMarkup("(T 1442)，大正23");

        ArgumentCaptor<CbrdParseRequest> requestCaptor = ArgumentCaptor.forClass(CbrdParseRequest.class);
        verify(command).execute(requestCaptor.capture(), any(CbrdParseConfiguration.class), anyString());
        assertThat(requestCaptor.getValue().getText()).isEqualTo("(T 1442)，大正23");
        assertThat(result.getMarkupResult()).isEqualTo(MARKUP);
    }

    @Test
    public void runAiMarkupUsesTheConfiguredEndpointTokenAndTimeout() {
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(any(CbrdParseRequest.class), any(CbrdParseConfiguration.class), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession()));
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("(T 1442)");

        ArgumentCaptor<CbrdParseConfiguration> configCaptor = ArgumentCaptor.forClass(CbrdParseConfiguration.class);
        verify(command).execute(any(CbrdParseRequest.class), configCaptor.capture(), anyString());
        CbrdParseConfiguration configuration = configCaptor.getValue();
        assertThat(configuration.getEndpointUrl()).isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
        assertThat(configuration.getSharedToken()).isEqualTo("shared-token-9876");
        assertThat(configuration.getTimeoutMs()).isEqualTo(30000);
    }

    @Test
    public void runAiMarkupNeverMutatesStoredSettings() {
        WSOptionsStorage storage = configuredStorage();
        extension.setOptionStorageForTests(storage);
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(any(CbrdParseRequest.class), any(CbrdParseConfiguration.class), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession()));
        extension.setAiMarkupDiagnosticsCommandForTests(command);

        extension.runAiMarkup("(T 1442)");

        verify(storage, never()).setOption(anyString(), anyString());
        verify(storage, never()).setSecretOption(anyString(), anyString());
    }

    @Test
    public void buildAiMarkupConfigurationFallsBackToTheDocumentedDefaults() {
        WSOptionsStorage empty = mock(WSOptionsStorage.class);
        when(empty.getOption(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        when(empty.getSecretOption(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        extension.setOptionStorageForTests(empty);

        CbrdParseConfiguration configuration = extension.buildAiMarkupConfiguration();

        assertThat(configuration.getEndpointUrl()).isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
        assertThat(configuration.getTimeoutMs()).isEqualTo(30000);
        assertThat(configuration.hasSharedToken()).isFalse();
    }

    private AiMarkupDiagnosticSession parseSession() {
        return new AiMarkupDiagnosticSession(10, parseConfiguration());
    }

    private CbrdParseConfiguration parseConfiguration() {
        return new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "shared-token-9876");
    }

    static WSOptionsStorage configuredStorage() {
        WSOptionsStorage storage = mock(WSOptionsStorage.class);
        when(storage.getOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_API_URL.equals(key)) {
                return "https://cbss.dila.edu.tw/cbrd/parse";
            }
            if (DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TIMEOUT_MS.equals(key)) {
                return "30000";
            }
            return invocation.getArgument(1);
        });
        when(storage.getSecretOption(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN.equals(key)
                ? "shared-token-9876"
                : invocation.getArgument(1);
        });
        return storage;
    }
}
