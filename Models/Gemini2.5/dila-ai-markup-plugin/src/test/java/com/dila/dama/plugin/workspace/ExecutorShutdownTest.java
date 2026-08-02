package com.dila.dama.plugin.workspace;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T053a — the background executor is shut down on the existing plugin shutdown path after the
 * CBRD Parse retarget (NFR-003).
 */
public class ExecutorShutdownTest {

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void executorIsRunningBeforeShutdown() {
        assertThat(extension.isExecutorShutdownForTests()).isFalse();
    }

    @Test
    public void applicationClosingShutsDownTheExecutor() {
        extension.applicationClosing();

        assertThat(extension.isExecutorShutdownForTests()).isTrue();
    }

    @Test
    public void applicationClosingReturnsTrueSoOxygenCanProceed() {
        assertThat(extension.applicationClosing()).isTrue();
    }

    @Test
    public void shutdownIsIdempotent() {
        extension.applicationClosing();
        extension.applicationClosing();

        assertThat(extension.isExecutorShutdownForTests()).isTrue();
    }

    @Test
    public void inFlightWorkIsCancelledBeforeTheExecutorIsShutDown() {
        CompletableFuture<String> inFlight = new CompletableFuture<>();
        extension.tryStartAiMarkupOperationForTests("selection");
        extension.setInFlightAiMarkupFutureForTests(inFlight);

        extension.applicationClosing();

        assertThat(inFlight.isCancelled()).isTrue();
        assertThat(extension.isExecutorShutdownForTests()).isTrue();
    }
}
