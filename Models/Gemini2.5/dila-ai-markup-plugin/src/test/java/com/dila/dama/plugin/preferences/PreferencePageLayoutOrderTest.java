package com.dila.dama.plugin.preferences;

import org.junit.Test;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

import javax.swing.JComponent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T035a — the surviving preference rows keep their order after the six OpenAI rows are removed
 * (NFR-005): CBRD Link first, then CBRD Parse.
 */
public class PreferencePageLayoutOrderTest {

    @Test
    public void remainingRowsAreCbrdLinkThenCbrdParse() {
        List<String> labels = UpgradePreferencesTest.labelsOf(initialisedPage());

        assertThat(labels).containsExactly(
            "CBRD Link Endpoint:",
            "CBRD Referer header:",
            "CBRD Link timeout (ms):",
            "CBRD Parse endpoint URL:",
            "CBRD Parse token*:",
            "CBRD Parse timeout (ms):"
        );
    }

    @Test
    public void pageExposesExactlySixRows() {
        assertThat(UpgradePreferencesTest.labelsOf(initialisedPage())).hasSize(6);
        assertThat(UpgradePreferencesTest.textFieldsOf(initialisedPage())).hasSize(6);
    }

    /**
     * FR-017 — the Ref-to-Link surface is preserved unchanged by this feature. Its behaviour is
     * additionally guarded by its own suites (CBRDAPIClientTest, RefToLinkReplaceFlowTest,
     * ConvertReferenceCommandTest, ReferenceParserTest), which run untouched.
     */
    @Test
    public void refToLinkRowsAreUntouchedByThisFeature() {
        List<String> labels = UpgradePreferencesTest.labelsOf(initialisedPage());

        assertThat(labels.subList(0, 3)).containsExactly(
            "CBRD Link Endpoint:",
            "CBRD Referer header:",
            "CBRD Link timeout (ms):"
        );
    }

    @Test
    public void refToLinkPreferenceKeysAndDefaultsAreUnchanged() {
        // FR-017: this feature must not disturb the Ref-to-Link configuration contract.
        assertThat(DAMAOptionPagePluginExtension.KEY_CBRD_API_URL).isEqualTo("cbrd.api.url");
        assertThat(DAMAOptionPagePluginExtension.KEY_CBRD_REFERER_HEADER).isEqualTo("cbrd.referer.header");
        assertThat(DAMAOptionPagePluginExtension.KEY_CBRD_TIMEOUT_MS).isEqualTo("cbrd.timeout");

        List<javax.swing.JTextField> fields = UpgradePreferencesTest.textFieldsOf(initialisedPage());
        assertThat(fields.get(0).getText()).isEqualTo("https://cbss.dila.edu.tw/cbrd/link");
        assertThat(fields.get(1).getText()).isEqualTo("CBRD@dila.edu.tw");
        assertThat(fields.get(2).getText()).isEqualTo("10000");
    }

    @Test
    public void refToLinkTimeoutIsSeparateFromTheParseTimeout() {
        // FR-016/FR-017: the two timeouts are deliberately different values on different keys.
        List<javax.swing.JTextField> fields = UpgradePreferencesTest.textFieldsOf(initialisedPage());

        assertThat(fields.get(2).getText()).isEqualTo("10000");
        assertThat(fields.get(5).getText()).isEqualTo("30000");
    }

    @Test
    public void pageKeyAndTitleAreUnchanged() {
        DAMAOptionPagePluginExtension page = new DAMAOptionPagePluginExtension();

        assertThat(page.getKey()).isEqualTo("dila_ai_markup_options_page_key");
        assertThat(page.getTitle()).isEqualTo("DILA AI Markup Assistant");
    }

    /** A page initialised against empty option storage, so every field shows its default. */
    static JComponent initialisedPage() {
        WSOptionsStorage storage = mock(WSOptionsStorage.class);
        when(storage.getOption(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        when(storage.getSecretOption(anyString(), anyString())).thenAnswer(i -> i.getArgument(1));
        PluginWorkspace workspace = mock(PluginWorkspace.class);
        when(workspace.getOptionsStorage()).thenReturn(storage);

        return new DAMAOptionPagePluginExtension().init(workspace);
    }
}
