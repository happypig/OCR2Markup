package com.dila.dama.plugin.application.query;

import com.dila.dama.plugin.infrastructure.release.ReleaseNotesResourceLoader;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadReleaseNotesQueryTest {

    @Test
    public void returnsSharedReleaseNotesWhenResourceLoads() {
        LoadReleaseNotesQuery query = new LoadReleaseNotesQuery(new ReleaseNotesResourceLoader() {
            @Override
            public String load() {
                return "<section><h3>Release Notes</h3><p>Current release</p></section>";
            }
        });

        LoadReleaseNotesQuery.Result result = query.execute("0.4.2", "release.notes.unavailable");

        assertThat(result.getPluginVersion()).isEqualTo("0.4.2");
        assertThat(result.getReleaseNotesMarkup()).contains("Release Notes");
        assertThat(result.isFallbackUsed()).isFalse();
    }

    @Test
    public void fallsBackWhenSharedReleaseNotesCannotBeLoaded() {
        LoadReleaseNotesQuery query = new LoadReleaseNotesQuery(new ReleaseNotesResourceLoader() {
            @Override
            public String load() throws Exception {
                throw new IllegalStateException("missing");
            }
        });

        LoadReleaseNotesQuery.Result result = query.execute("0.4.2", "release.notes.unavailable");

        assertThat(result.getPluginVersion()).isEqualTo("0.4.2");
        assertThat(result.getReleaseNotesMarkup()).isEqualTo("release.notes.unavailable");
        assertThat(result.isFallbackUsed()).isTrue();
    }
}
