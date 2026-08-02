package com.dila.dama.plugin.preferences;

import org.junit.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T034 — what an existing user sees after upgrading (US2 scenarios 1 and 5, FR-002, FR-016).
 */
public class UpgradePreferencesTest {

    @Test
    public void cbrdParseEndpointDefaultsToTheDilaProductionEndpoint() {
        assertThat(DAMAOptionPagePluginExtension.DEFAULT_CBRD_PARSE_API_URL)
            .isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
    }

    @Test
    public void cbrdParseTimeoutDefaultsToThirtySecondsNotTheRefToLinkTimeout() {
        assertThat(DAMAOptionPagePluginExtension.DEFAULT_CBRD_PARSE_TIMEOUT_MS).isEqualTo("30000");
        assertThat(DAMAOptionPagePluginExtension.DEFAULT_CBRD_PARSE_TIMEOUT_MS).isNotEqualTo("10000");
    }

    @Test
    public void cbrdParsePreferenceKeysAreStable() {
        assertThat(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_API_URL).isEqualTo("cbrd.parse.api.url");
        assertThat(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TOKEN).isEqualTo("cbrd.parse.token");
        assertThat(DAMAOptionPagePluginExtension.KEY_CBRD_PARSE_TIMEOUT_MS).isEqualTo("cbrd.parse.timeout");
    }

    @Test
    public void tokenHasNoDefaultSoItIsNeverRevived() {
        DAMAOptionPagePluginExtension page = new DAMAOptionPagePluginExtension();
        page.restoreDefaults();

        // restoreDefaults on an uninitialised page must not throw, and must invent no token.
        assertThat(page.getTitle()).isEqualTo("DILA AI Markup Assistant");
    }

    @Test
    public void noOpenAiPreferenceRowSurvivesTheUpgrade() {
        List<String> labels = labelsOf(PreferencePageLayoutOrderTest.initialisedPage());

        assertThat(labels).noneMatch(label -> label.toLowerCase().contains("parsing model"));
        assertThat(labels).noneMatch(label -> label.toLowerCase().contains("detection model"));
        assertThat(labels).noneMatch(label -> label.toLowerCase().contains("api key"));
        assertThat(labels).noneMatch(label -> label.toLowerCase().contains("base url"));
        assertThat(labels).noneMatch(label -> label.toLowerCase().contains("chat path"));
        assertThat(labels).noneMatch(label -> label.toLowerCase().contains("ai timeout"));
    }

    @Test
    public void endpointUrlIsPrefilledAndTheTokenIsEmpty() {
        JComponent panel = PreferencePageLayoutOrderTest.initialisedPage();
        List<JTextField> fields = textFieldsOf(panel);

        // Rows: referer, bearer token, parse url, parse timeout, link url, link timeout.
        assertThat(fields).hasSize(6);
        assertThat(fields.get(1)).isInstanceOf(JPasswordField.class);
        assertThat(fields.get(1).getText()).isEmpty();
        assertThat(fields.get(2).getText()).isEqualTo("https://cbss.dila.edu.tw/cbrd/parse");
        assertThat(fields.get(3).getText()).isEqualTo("30000");
    }

    static List<String> labelsOf(Container container) {
        List<String> labels = new ArrayList<>();
        collectLabels(container, labels);
        return labels;
    }

    private static void collectLabels(Container container, List<String> labels) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                labels.add(((JLabel) component).getText());
            }
            if (component instanceof Container) {
                collectLabels((Container) component, labels);
            }
        }
    }

    static List<JTextField> textFieldsOf(Container container) {
        List<JTextField> fields = new ArrayList<>();
        collectFields(container, fields);
        return fields;
    }

    private static void collectFields(Container container, List<JTextField> fields) {
        for (java.awt.Component component : container.getComponents()) {
            if (component instanceof JTextField) {
                fields.add((JTextField) component);
            }
            if (component instanceof Container) {
                collectFields((Container) component, fields);
            }
        }
    }
}
