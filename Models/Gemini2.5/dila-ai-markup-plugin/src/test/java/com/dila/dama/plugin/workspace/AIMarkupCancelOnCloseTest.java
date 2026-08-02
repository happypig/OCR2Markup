package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T023 — closing the document or Oxygen mid-flight cancels the request and discards the result
 * silently; nothing is written into a closed document (FR-020, US1 scenario 6).
 */
public class AIMarkupCancelOnCloseTest {

    private static final String MARKUP = "<ref><canon>T</canon></ref>";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void closingCancelsTheInFlightRequest() {
        CompletableFuture<String> inFlight = new CompletableFuture<>();
        extension.tryStartAiMarkupOperationForTests("selection");
        extension.setInFlightAiMarkupFutureForTests(inFlight);

        extension.applicationClosing();

        assertThat(inFlight.isCancelled()).isTrue();
    }

    @Test
    public void closingReleasesTheOperationGuard() {
        extension.tryStartAiMarkupOperationForTests("selection");
        extension.setInFlightAiMarkupFutureForTests(new CompletableFuture<String>());

        extension.applicationClosing();

        assertThat(extension.isAiMarkupInProgressForTests()).isFalse();
    }

    @Test
    public void closingStillShutsDownTheExecutor() {
        extension.tryStartAiMarkupOperationForTests("selection");
        extension.setInFlightAiMarkupFutureForTests(new CompletableFuture<String>());

        extension.applicationClosing();

        assertThat(extension.isExecutorShutdownForTests()).isTrue();
    }

    @Test
    public void closingWithNothingInFlightIsHarmless() {
        assertThat(extension.applicationClosing()).isTrue();
        assertThat(extension.isExecutorShutdownForTests()).isTrue();
    }

    @Test
    public void aLateResultIsDiscardedSilentlyAfterCancellation() {
        extension.tryStartAiMarkupOperationForTests("selection");
        extension.cancelInFlightAiMarkup();

        extension.completeAiMarkupOperation(
            RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession()));

        assertThat(extension.getResultAreaForTests().getText()).doesNotContain(MARKUP);
        // No Replace offered for a discarded result - it would apply stale markup.
        assertThat(extension.isButtonPanelVisibleForTests()).isFalse();
    }

    @Test
    public void aLateFailureIsAlsoDiscardedSilently() {
        extension.tryStartAiMarkupOperationForTests("selection");
        extension.cancelInFlightAiMarkup();

        extension.completeAiMarkupOperation(RunAiMarkupDiagnosticsCommand.Result.failure(
            "ai.markup.error.connectivity", null, null, parseSession()));

        assertThat(extension.getResultAreaForTests().getText()).isEmpty();
        assertThat(extension.getExportButtonForTests().isVisible()).isFalse();
        assertThat(extension.isButtonPanelVisibleForTests()).isFalse();
    }

    @Test
    public void cancellationDoesNotLeakIntoTheNextOperation() {
        extension.tryStartAiMarkupOperationForTests("first");
        extension.cancelInFlightAiMarkup();

        extension.tryStartAiMarkupOperationForTests("second");
        extension.completeAiMarkupOperation(
            RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession()));

        assertThat(extension.getResultAreaForTests().getText()).isEqualTo(MARKUP);
    }

    private AiMarkupDiagnosticSession parseSession() {
        return new AiMarkupDiagnosticSession(
            10, new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "token"));
    }
}
