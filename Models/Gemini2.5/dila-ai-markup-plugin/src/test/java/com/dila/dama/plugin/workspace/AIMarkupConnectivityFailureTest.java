package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.application.command.RunAiMarkupDiagnosticsCommand;
import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.service.DiagnosticClassifier;
import com.dila.dama.plugin.domain.service.RequestValidationService;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import com.dila.dama.plugin.infrastructure.api.CapturingConnectionFactory;
import com.dila.dama.plugin.infrastructure.api.CbrdParseApiClient;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import com.dila.dama.plugin.infrastructure.logging.SanitizedDiagnosticLogger;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T046 — being unable to reach the service is reported differently from the service reporting
 * a problem (FR-013, US4 scenario 5).
 */
public class AIMarkupConnectivityFailureTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";

    private DAMAWorkspaceAccessPluginExtension extension;

    @Before
    public void setUp() {
        extension = new DAMAWorkspaceAccessPluginExtension();
        extension.initializeUiForTests();
    }

    @Test
    public void connectionRefusedYieldsTheConnectivityGuidance() {
        assertThat(guidanceFor(new IOException("Connection refused")))
            .contains("ai.markup.error.connectivity");
    }

    @Test
    public void readTimeoutYieldsTheConnectivityGuidance() {
        assertThat(guidanceFor(new SocketTimeoutException("Read timed out")))
            .contains("ai.markup.error.connectivity");
    }

    @Test
    public void unknownHostYieldsTheConnectivityGuidance() {
        assertThat(guidanceFor(new UnknownHostException("cbss.dila.edu.tw")))
            .contains("ai.markup.error.connectivity");
    }

    @Test
    public void connectivityGuidanceIsDistinctFromEveryServiceSideCause() {
        String connectivity = guidanceFor(new IOException("Connection refused"));

        assertThat(connectivity).doesNotContain("ai.markup.error.openai_unavailable");
        assertThat(connectivity).doesNotContain("ai.markup.error.parse_api_not_configured");
        assertThat(connectivity).doesNotContain("ai.markup.error.unauthorized");
        assertThat(connectivity).doesNotContain("ai.markup.error.unexpected");
    }

    @Test
    public void nothingIsOfferedForReplacementAfterAConnectivityFailure() {
        guidanceFor(new IOException("Connection refused"));

        assertThat(extension.getReplaceButtonForTests().isVisible()).isFalse();
    }

    private String guidanceFor(Exception failure) {
        RunAiMarkupDiagnosticsCommand command = new RunAiMarkupDiagnosticsCommand(
            new RequestValidationService(),
            new DiagnosticClassifier(),
            new CbrdParseApiClient(CapturingConnectionFactory.throwing((IOException) failure)),
            new SecretRedactor(),
            new SanitizedDiagnosticLogger()
        );

        extension.completeAiMarkupOperation(command.execute(
            new CbrdParseRequest("(T 1442)", "zh"),
            new CbrdParseConfiguration(ENDPOINT, 30000, "token"),
            "windows"
        ));
        return extension.getResultAreaForTests().getText();
    }
}
