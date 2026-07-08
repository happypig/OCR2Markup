package com.dila.dama.plugin.domain.service;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DiagnosticClassifierPlatformParityTest {

    private final DiagnosticClassifier classifier = new DiagnosticClassifier();

    @Test
    public void resolvesPlatformSpecificGuidanceKeys() {
        assertThat(classifier.resolvePlatformGuidanceKey("ai.markup.diagnostic.connectivity.proxy", "windows"))
            .isEqualTo("ai.markup.diagnostic.connectivity.proxy.windows");
        assertThat(classifier.resolvePlatformGuidanceKey("ai.markup.diagnostic.connectivity.proxy", "macos"))
            .isEqualTo("ai.markup.diagnostic.connectivity.proxy.macos");
    }

    @Test
    public void preservesEquivalentBaseGuidanceAcrossPlatforms() {
        String windowsKey = classifier.resolvePlatformGuidanceKey("ai.markup.diagnostic.credentials", "windows");
        String macKey = classifier.resolvePlatformGuidanceKey("ai.markup.diagnostic.credentials", "macos");

        assertThat(windowsKey).endsWith(".windows");
        assertThat(macKey).endsWith(".macos");
        assertThat(windowsKey.replace(".windows", "")).isEqualTo(macKey.replace(".macos", ""));
    }
}
