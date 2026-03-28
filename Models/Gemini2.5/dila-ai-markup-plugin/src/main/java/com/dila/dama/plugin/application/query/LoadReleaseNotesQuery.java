package com.dila.dama.plugin.application.query;

import com.dila.dama.plugin.infrastructure.release.ReleaseNotesResourceLoader;

public class LoadReleaseNotesQuery {

    private final ReleaseNotesResourceLoader resourceLoader;

    public LoadReleaseNotesQuery() {
        this(new ReleaseNotesResourceLoader());
    }

    public LoadReleaseNotesQuery(ReleaseNotesResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Result execute(String pluginVersion, String fallbackMessage) {
        String normalizedVersion = pluginVersion == null || pluginVersion.trim().isEmpty()
            ? "unknown"
            : pluginVersion.trim();
        try {
            return new Result(normalizedVersion, resourceLoader.load(), false);
        } catch (Exception e) {
            return new Result(normalizedVersion, fallbackMessage == null ? "" : fallbackMessage, true);
        }
    }

    public static class Result {
        private final String pluginVersion;
        private final String releaseNotesMarkup;
        private final boolean fallbackUsed;

        public Result(String pluginVersion, String releaseNotesMarkup, boolean fallbackUsed) {
            this.pluginVersion = pluginVersion;
            this.releaseNotesMarkup = releaseNotesMarkup;
            this.fallbackUsed = fallbackUsed;
        }

        public String getPluginVersion() {
            return pluginVersion;
        }

        public String getReleaseNotesMarkup() {
            return releaseNotesMarkup;
        }

        public boolean isFallbackUsed() {
            return fallbackUsed;
        }
    }
}
