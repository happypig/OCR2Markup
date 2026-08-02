package com.dila.dama.plugin.preferences;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import org.junit.Test;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T035b — the shared token is masked, stored securely, and never rendered in full
 * (FR-003, NFR-004, US2 scenario 4).
 */
public class SharedTokenNonDisclosureTest {

    private static final String TOKEN = "super-secret-token-9876";

    @Test
    public void tokenFieldIsMasked() {
        List<JTextField> fields = UpgradePreferencesTest.textFieldsOf(PreferencePageLayoutOrderTest.initialisedPage());

        assertThat(fields.get(4)).isInstanceOf(JPasswordField.class);
        assertThat(((JPasswordField) fields.get(4)).getEchoChar()).isNotEqualTo('\0');
    }

    @Test
    public void tokenIsPersistedThroughTheSecureOptionStorage() {
        WSOptionsStorage storage = emptyStorage();
        PluginWorkspace workspace = workspaceOver(storage);
        DAMAOptionPagePluginExtension page = new DAMAOptionPagePluginExtension();
        JComponent panel = page.init(workspace);
        UpgradePreferencesTest.textFieldsOf(panel).get(4).setText(TOKEN);

        page.apply(workspace);

        verify(storage).setSecretOption(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN, TOKEN);
        verify(storage, never()).setOption(eq(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN), anyString());
    }

    @Test
    public void tokenIsNeverWrittenToAPlainOption() {
        WSOptionsStorage storage = emptyStorage();
        PluginWorkspace workspace = workspaceOver(storage);
        DAMAOptionPagePluginExtension page = new DAMAOptionPagePluginExtension();
        JComponent panel = page.init(workspace);
        UpgradePreferencesTest.textFieldsOf(panel).get(4).setText(TOKEN);

        page.apply(workspace);

        verify(storage, never()).setOption(anyString(), eq(TOKEN));
    }

    @Test
    public void obsoleteOpenAiKeysAreNoLongerPersisted() {
        WSOptionsStorage storage = emptyStorage();
        PluginWorkspace workspace = workspaceOver(storage);
        DAMAOptionPagePluginExtension page = new DAMAOptionPagePluginExtension();
        page.init(workspace);

        page.apply(workspace);

        verify(storage, never()).setOption(eq(DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_BASE_URL), anyString());
        verify(storage, never()).setOption(eq(DAMAOptionPagePluginExtension.KEY_DILA_DAMA_CHAT_COMPLETIONS_PATH), anyString());
        verify(storage, never()).setOption(eq(DAMAOptionPagePluginExtension.KEY_DILA_DAMA_FT_PARSE_MODEL), anyString());
        verify(storage, never()).setOption(eq(DAMAOptionPagePluginExtension.KEY_DILA_DAMA_FT_DETECT_MODEL), anyString());
        verify(storage, never()).setOption(eq(DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_TIMEOUT_MS), anyString());
        verify(storage, never()).setSecretOption(eq(DAMAOptionPagePluginExtension.KEY_DILA_DAMA_API_KEY), anyString());
    }

    @Test
    public void cbrdParseOptionsArePersistedOnApply() {
        WSOptionsStorage storage = emptyStorage();
        PluginWorkspace workspace = workspaceOver(storage);
        DAMAOptionPagePluginExtension page = new DAMAOptionPagePluginExtension();
        page.init(workspace);

        page.apply(workspace);

        verify(storage).setOption(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_API_URL, "https://cbss.dila.edu.tw/cbrd/parse");
        verify(storage).setOption(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TIMEOUT_MS, "30000");
        verify(storage, times(1)).setSecretOption(eq(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN), anyString());
    }

    @Test
    public void onlyAFingerprintOfTheTokenIsEverExposed() {
        CbrdParseConfiguration configuration =
            new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, TOKEN);

        assertThat(configuration.getTokenFingerprint()).isEqualTo("****9876");
        assertThat(configuration.toString()).doesNotContain(TOKEN);
        assertThat(configuration.getEndpointSummary()).doesNotContain(TOKEN);
    }

    @Test
    public void bearerHeadersAreRedactedInDiagnostics() {
        SecretRedactor redactor = new SecretRedactor();

        String redacted = redactor.redact("Authorization: Bearer " + TOKEN);

        assertThat(redacted).doesNotContain(TOKEN);
        assertThat(redacted).contains("Bearer ****");
    }

    private static WSOptionsStorage emptyStorage() {
        WSOptionsStorage storage = mock(WSOptionsStorage.class);
        when(storage.getOption(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        when(storage.getSecretOption(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        return storage;
    }

    private static PluginWorkspace workspaceOver(WSOptionsStorage storage) {
        PluginWorkspace workspace = mock(PluginWorkspace.class);
        when(workspace.getOptionsStorage()).thenReturn(storage);
        return workspace;
    }
}
