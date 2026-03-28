package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.query.LoadReleaseNotesQuery;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DAMAWorkspaceAccessPluginExtensionAboutDialogTest {

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void exposesConfiguredUserManualUrlAndGearMenuOrderMetadata() {
        assertThat(extension.getUserManualUrlForTests())
            .isEqualTo("https://docs.google.com/document/d/1JHWAu4KJ6eb-UZhh-uYW8HbzsKc6fD5i_lVKTQWj9HQ/edit?usp=sharing");
        assertThat(extension.getOptionsMenuItemKeysForTests())
            .containsExactly("menuItem.preferences", "menuItem.user.manual", "menuItem.about");
    }

    @Test
    public void createMenuBarPreservesPreferencesAndAddsSupportItemsInOrder() {
        JMenuBar menuBar = extension.createMenuBarForTests();
        JMenu optionsMenu = findOptionsMenu(menuBar);

        assertThat(optionsMenu).isNotNull();
        assertThat(readMenuTexts(optionsMenu))
            .containsExactly("menuItem.preferences", "menuItem.user.manual", "menuItem.about");
    }

    @Test
    public void userManualMenuItemNavigatesToConfiguredUrl() {
        RecordingExternalUrlOpener opener = new RecordingExternalUrlOpener();
        extension.setExternalUrlOpenerForTests(opener);

        JMenuItem userManualItem = findOptionsMenuItem(extension.createMenuBarForTests(), "menuItem.user.manual");
        userManualItem.doClick();

        assertThat(opener.lastUrl).isEqualTo(extension.getUserManualUrlForTests());
    }

    @Test
    public void aboutMenuItemShowsVersionAndReleaseNotes() {
        RecordingAboutDialogPresenter presenter = new RecordingAboutDialogPresenter();
        extension.setAboutDialogPresenterForTests(presenter);
        extension.setPluginVersionForTests("0.4.2-test");
        extension.setLoadReleaseNotesQueryForTests(new LoadReleaseNotesQuery(new FixedReleaseNotesLoader(
            "<section><h3>Release Notes</h3><ul><li>Support visibility update</li></ul></section>"
        )));

        JMenuItem aboutItem = findOptionsMenuItem(extension.createMenuBarForTests(), "menuItem.about");
        aboutItem.doClick();

        assertThat(presenter.title).isEqualTo("dialog.about.title");
        assertThat(presenter.html).contains("0.4.2-test");
        assertThat(presenter.html).contains("Support visibility update");
        assertThat(countOccurrences(presenter.html, "Release Notes")).isEqualTo(1);
    }

    @Test
    public void aboutDialogFallsBackWhenReleaseNotesAreUnavailable() {
        RecordingAboutDialogPresenter presenter = new RecordingAboutDialogPresenter();
        extension.setAboutDialogPresenterForTests(presenter);
        extension.setPluginVersionForTests("0.4.2-test");
        extension.setLoadReleaseNotesQueryForTests(new LoadReleaseNotesQuery(new ReleaseNotesLoaderFailure()));

        JMenuItem aboutItem = findOptionsMenuItem(extension.createMenuBarForTests(), "menuItem.about");
        aboutItem.doClick();

        assertThat(presenter.html).contains("0.4.2-test");
        assertThat(presenter.html).contains("about.release.notes.unavailable");
    }

    private JMenuItem findOptionsMenuItem(JMenuBar menuBar, String text) {
        JMenu optionsMenu = findOptionsMenu(menuBar);
        for (int i = 0; i < optionsMenu.getItemCount(); i++) {
            JMenuItem item = optionsMenu.getItem(i);
            if (item != null && text.equals(item.getText())) {
                return item;
            }
        }
        throw new AssertionError("Could not find menu item: " + text);
    }

    private JMenu findOptionsMenu(JMenuBar menuBar) {
        for (int i = menuBar.getMenuCount() - 1; i >= 0; i--) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null && menu.getItemCount() == 3) {
                return menu;
            }
        }
        return null;
    }

    private List<String> readMenuTexts(JMenu menu) {
        String[] values = new String[menu.getItemCount()];
        for (int i = 0; i < menu.getItemCount(); i++) {
            values[i] = menu.getItem(i).getText();
        }
        return Arrays.asList(values);
    }

    private int countOccurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static class RecordingExternalUrlOpener implements DAMAWorkspaceAccessPluginExtension.ExternalUrlOpener {
        private String lastUrl;

        @Override
        public void open(String url) {
            this.lastUrl = url;
        }
    }

    private static class RecordingAboutDialogPresenter implements DAMAWorkspaceAccessPluginExtension.AboutDialogPresenter {
        private String title;
        private String html;

        @Override
        public void show(String title, String html) {
            this.title = title;
            this.html = html;
        }
    }

    private static class FixedReleaseNotesLoader extends com.dila.dama.plugin.infrastructure.release.ReleaseNotesResourceLoader {
        private final String markup;

        private FixedReleaseNotesLoader(String markup) {
            this.markup = markup;
        }

        @Override
        public String load() {
            return markup;
        }
    }

    private static class ReleaseNotesLoaderFailure extends com.dila.dama.plugin.infrastructure.release.ReleaseNotesResourceLoader {
        @Override
        public String load() throws Exception {
            throw new IllegalStateException("missing");
        }
    }
}
