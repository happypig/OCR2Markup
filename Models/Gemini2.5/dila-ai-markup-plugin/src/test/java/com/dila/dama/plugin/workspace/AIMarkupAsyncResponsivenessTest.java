package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.AiMarkupDiagnosticSession;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import org.junit.Before;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T008a — the parse request runs off the EDT and UI updates are marshalled back onto it
 * (FR-014, NFR-002).
 */
public class AIMarkupAsyncResponsivenessTest {

    private static final String MARKUP = "<ref><canon>T</canon></ref>";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
        extension.setOptionStorageForTests(AIMarkupSuccessFlowTest.configuredStorage());
    }

    @Test
    public void runAiMarkupCompletesOnABackgroundThreadWithoutTouchingSwing() throws Exception {
        extension.setAiMarkupDiagnosticsCommandForTests(succeedingCommand());
        AtomicReference<String> resultAreaDuringCall = new AtomicReference<>();
        AtomicBoolean ranOnEdt = new AtomicBoolean(true);
        CountDownLatch done = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            ranOnEdt.set(SwingUtilities.isEventDispatchThread());
            extension.runAiMarkup("(T 1442)");
            resultAreaDuringCall.set(extension.getResultAreaForTests().getText());
            done.countDown();
        });
        worker.start();

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ranOnEdt.get()).isFalse();
        // The network call itself must not write to the UI - that is completeAiMarkupOperation's job.
        assertThat(resultAreaDuringCall.get()).isEmpty();
    }

    @Test
    public void resultIsRenderedWhenMarshalledOntoTheEventDispatchThread() throws Exception {
        SwingUtilities.invokeAndWait(() -> extension.completeAiMarkupOperation(
            RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession())));

        assertThat(extension.getResultAreaForTests().getText()).isEqualTo(MARKUP);
    }

    @Test
    public void aSlowServiceDoesNotBlockTheCallerBeyondTheOperationItself() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        RunAiMarkupDiagnosticsCommand slow = mock(RunAiMarkupDiagnosticsCommand.class);
        when(slow.execute(any(CbrdParseRequest.class), any(CbrdParseConfiguration.class), anyString()))
            .thenAnswer(invocation -> {
                release.await(5, TimeUnit.SECONDS);
                return RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession());
            });
        extension.setAiMarkupDiagnosticsCommandForTests(slow);

        CountDownLatch finished = new CountDownLatch(1);
        new Thread(() -> {
            extension.runAiMarkup("(T 1442)");
            finished.countDown();
        }).start();

        // While the request is outstanding the EDT is free: a UI update still lands immediately.
        SwingUtilities.invokeAndWait(() -> extension.getResultAreaForTests().setText("still responsive"));
        assertThat(extension.getResultAreaForTests().getText()).isEqualTo("still responsive");

        release.countDown();
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void executorIsRunningUntilTheApplicationCloses() {
        assertThat(extension.isExecutorShutdownForTests()).isFalse();

        extension.applicationClosing();

        assertThat(extension.isExecutorShutdownForTests()).isTrue();
    }

    private RunAiMarkupDiagnosticsCommand succeedingCommand() {
        RunAiMarkupDiagnosticsCommand command = mock(RunAiMarkupDiagnosticsCommand.class);
        when(command.execute(any(CbrdParseRequest.class), any(CbrdParseConfiguration.class), anyString()))
            .thenReturn(RunAiMarkupDiagnosticsCommand.Result.success(MARKUP, parseSession()));
        return command;
    }

    private AiMarkupDiagnosticSession parseSession() {
        return new AiMarkupDiagnosticSession(
            10, new CbrdParseConfiguration("https://cbss.dila.edu.tw/cbrd/parse", 30000, "token"));
    }
}
