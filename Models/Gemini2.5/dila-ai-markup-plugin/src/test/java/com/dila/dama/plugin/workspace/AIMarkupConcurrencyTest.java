package com.dila.dama.plugin.workspace;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T022 — a second invocation while one is in flight is ignored, and the editor is told which
 * selection is being processed (FR-015, US1 scenario 5).
 */
public class AIMarkupConcurrencyTest {

    private static final String IN_FLIGHT = "(T 1442)，大正23，頁869中";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void firstInvocationStartsTheOperation() {
        assertThat(extension.tryStartAiMarkupOperationForTests(IN_FLIGHT)).isTrue();
        assertThat(extension.isAiMarkupInProgressForTests()).isTrue();
    }

    @Test
    public void secondInvocationIsRefusedWhileTheFirstIsInFlight() {
        extension.tryStartAiMarkupOperationForTests(IN_FLIGHT);

        assertThat(extension.tryStartAiMarkupOperationForTests("a different selection")).isFalse();
    }

    @Test
    public void refusedInvocationShowsTheAlreadyInProgressMessage() {
        extension.tryStartAiMarkupOperationForTests(IN_FLIGHT);
        extension.tryStartAiMarkupOperationForTests("a different selection");

        extension.showAiMarkupAlreadyInProgress();

        assertThat(extension.getResultAreaForTests().getText()).contains("ai.markup.diagnostic.in.progress");
    }

    @Test
    public void refusedInvocationShowsTheInFlightSelectionNotTheNewOne() {
        extension.tryStartAiMarkupOperationForTests(IN_FLIGHT);
        extension.tryStartAiMarkupOperationForTests("a different selection");

        extension.showAiMarkupAlreadyInProgress();

        String shown = extension.getResultAreaForTests().getText();
        assertThat(shown).contains(IN_FLIGHT);
        assertThat(shown).doesNotContain("a different selection");
    }

    @Test
    public void refusedInvocationOffersNoButtons() {
        extension.tryStartAiMarkupOperationForTests(IN_FLIGHT);
        extension.showAiMarkupAlreadyInProgress();

        assertThat(extension.isButtonPanelVisibleForTests()).isFalse();
        assertThat(extension.getExportButtonForTests().isVisible()).isFalse();
    }

    @Test
    public void operationCanRestartOnceTheFirstFinishes() {
        extension.tryStartAiMarkupOperationForTests(IN_FLIGHT);
        extension.finishAiMarkupOperationForTests();

        assertThat(extension.isAiMarkupInProgressForTests()).isFalse();
        assertThat(extension.tryStartAiMarkupOperationForTests("a later selection")).isTrue();
    }

    @Test
    public void inFlightSelectionTracksWhicheverOperationActuallyStarted() {
        extension.tryStartAiMarkupOperationForTests(IN_FLIGHT);
        extension.finishAiMarkupOperationForTests();
        extension.tryStartAiMarkupOperationForTests("a later selection");

        extension.showAiMarkupAlreadyInProgress();

        assertThat(extension.getResultAreaForTests().getText()).contains("a later selection");
    }
}
