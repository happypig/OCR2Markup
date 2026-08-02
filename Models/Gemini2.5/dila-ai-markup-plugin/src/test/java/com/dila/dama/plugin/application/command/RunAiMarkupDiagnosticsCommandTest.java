package com.dila.dama.plugin.application.command;

import com.dila.dama.plugin.domain.model.CbrdParseConfiguration;
import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.ParseError;
import com.dila.dama.plugin.domain.service.CbrdParseErrorClassifier;
import com.dila.dama.plugin.domain.service.DiagnosticClassifier;
import com.dila.dama.plugin.domain.service.RequestValidationService;
import com.dila.dama.plugin.domain.service.SecretRedactor;
import com.dila.dama.plugin.infrastructure.api.CapturingConnectionFactory;
import com.dila.dama.plugin.infrastructure.api.CbrdParseApiClient;
import com.dila.dama.plugin.infrastructure.api.CbrdParseRequest;
import com.dila.dama.plugin.infrastructure.logging.SanitizedDiagnosticLogger;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T019 — the AI Markup command against the CBRD Parse endpoint (FR-001, FR-006, FR-011).
 */
public class RunAiMarkupDiagnosticsCommandTest {

    private static final String ENDPOINT = "https://cbss.dila.edu.tw/cbrd/parse";
    private static final String MARKUP =
        "<ref>(<canon>T</canon> <w>1442</w>)，大正<v>23</v>，頁<p>869</p><c>中</c></ref>";

    private static CbrdParseConfiguration configuration() {
        return new CbrdParseConfiguration(ENDPOINT, 30000, "shared-token-9876");
    }

    private static CbrdParseRequest request() {
        return new CbrdParseRequest("(T 1442)，大正23", "zh");
    }

    private static RunAiMarkupDiagnosticsCommand commandBackedBy(CapturingConnectionFactory factory) {
        return new RunAiMarkupDiagnosticsCommand(
            new RequestValidationService(),
            new DiagnosticClassifier(),
            new CbrdParseApiClient(factory),
            new SecretRedactor(),
            new SanitizedDiagnosticLogger()
        );
    }

    @Test
    public void successReturnsTheServiceMarkupExactlyAsReceived() {
        RunAiMarkupDiagnosticsCommand command =
            commandBackedBy(CapturingConnectionFactory.respondingWith(200, MARKUP));

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(request(), configuration(), "windows");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMarkupResult()).isEqualTo(MARKUP);
    }

    @Test
    public void successDoesNotWrapTheMarkupAgain() {
        // The DILA service emits the whole <ref> element; the OpenAI path used to add it.
        RunAiMarkupDiagnosticsCommand command =
            commandBackedBy(CapturingConnectionFactory.respondingWith(200, MARKUP));

        String markup = command.execute(request(), configuration(), "windows").getMarkupResult();

        assertThat(markup).startsWith("<ref>");
        assertThat(markup).doesNotContain("<ref><ref>");
        assertThat(markup).doesNotContain("</ref></ref>");
    }

    @Test
    public void sessionIsBuiltFromTheParseConfiguration() {
        RunAiMarkupDiagnosticsCommand command =
            commandBackedBy(CapturingConnectionFactory.respondingWith(200, MARKUP));

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(request(), configuration(), "windows");

        assertThat(result.getSession()).isNotNull();
        assertThat(result.getSession().getParseConfiguration()).isEqualTo(configuration());
        assertThat(result.getSession().getEndpointSummary()).contains(ENDPOINT);
    }

    @Test
    public void rejectedTokenYieldsTheUnauthorizedGuidance() {
        RunAiMarkupDiagnosticsCommand command = commandBackedBy(
            CapturingConnectionFactory.failingWith(401, "{\"success\":false,\"error\":\"unauthorized\"}"));

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(request(), configuration(), "windows");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSummaryMessageKey()).isEqualTo(ParseError.UNAUTHORIZED.getGuidanceMessageKey());
        assertThat(result.getFailureCategory()).isEqualTo(DiagnosticFailureCategory.CREDENTIALS);
        assertThat(result.getMarkupResult()).isEmpty();
    }

    @Test
    public void serverSideOutageIsNotReportedAsTheEditorsCredentialProblem() {
        RunAiMarkupDiagnosticsCommand command = commandBackedBy(
            CapturingConnectionFactory.failingWith(503, "{\"success\":false,\"error\":\"openai_credentials_unavailable\"}"));

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(request(), configuration(), "windows");

        assertThat(result.getSummaryMessageKey()).isEqualTo("ai.markup.error.openai_credentials_unavailable");
        assertThat(result.getFailureCategory()).isNotEqualTo(DiagnosticFailureCategory.CREDENTIALS);
    }

    @Test
    public void unreachableServiceYieldsTheConnectivityGuidance() {
        RunAiMarkupDiagnosticsCommand command =
            commandBackedBy(CapturingConnectionFactory.throwing(new IOException("Connection refused")));

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(request(), configuration(), "windows");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSummaryMessageKey()).isEqualTo("ai.markup.error.connectivity");
    }

    @Test
    public void unenumeratedResponseYieldsTheGenericGuidance() {
        RunAiMarkupDiagnosticsCommand command =
            commandBackedBy(CapturingConnectionFactory.failingWith(418, "<html>teapot</html>"));

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(request(), configuration(), "windows");

        assertThat(result.getSummaryMessageKey()).isEqualTo("ai.markup.error.unexpected");
    }

    @Test
    public void missingTokenIsCaughtBeforeAnyRequestIsSent() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, MARKUP);
        RunAiMarkupDiagnosticsCommand command = commandBackedBy(factory);

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(
            request(), new CbrdParseConfiguration(ENDPOINT, 30000, ""), "windows");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getSummaryMessageKey()).isEqualTo(CbrdParseErrorClassifier.TOKEN_NOT_CONFIGURED_KEY);
        assertThat(factory.getLastUrl()).isNull();
    }

    @Test
    public void tooLongSelectionIsCaughtBeforeAnyRequestIsSent() {
        CapturingConnectionFactory factory = CapturingConnectionFactory.respondingWith(200, MARKUP);
        RunAiMarkupDiagnosticsCommand command = commandBackedBy(factory);
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i <= RequestValidationService.MAX_SELECTION_LENGTH; i++) {
            tooLong.append('好');
        }

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(
            new CbrdParseRequest(tooLong.toString(), "zh"), configuration(), "windows");

        assertThat(result.getSummaryMessageKey()).isEqualTo("ai.markup.error.text_is_too_long");
        assertThat(factory.getLastUrl()).isNull();
    }

    @Test
    public void failureProducesATroubleshootingRecordForExport() {
        RunAiMarkupDiagnosticsCommand command = commandBackedBy(
            CapturingConnectionFactory.failingWith(401, "{\"success\":false,\"error\":\"unauthorized\"}"));

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(request(), configuration(), "windows");

        assertThat(result.getTroubleshootingRecord()).isNotNull();
        assertThat(result.getTroubleshootingRecord().getHttpStatus()).isEqualTo(401);
    }

    @Test
    public void troubleshootingRecordNeverContainsTheRawToken() {
        RunAiMarkupDiagnosticsCommand command = commandBackedBy(
            CapturingConnectionFactory.failingWith(401, "{\"success\":false,\"error\":\"unauthorized\"}"));

        RunAiMarkupDiagnosticsCommand.Result result = command.execute(request(), configuration(), "windows");

        String rendered = result.getTroubleshootingRecord().getEndpointSummary()
            + " " + result.getTroubleshootingRecord().getRequestSnapshot();
        assertThat(rendered).doesNotContain("shared-token-9876");
        assertThat(rendered).contains("****9876");
    }

    @Test
    public void operationIsReleasedOnTheSessionAfterEveryOutcome() {
        RunAiMarkupDiagnosticsCommand success =
            commandBackedBy(CapturingConnectionFactory.respondingWith(200, MARKUP));
        RunAiMarkupDiagnosticsCommand failure =
            commandBackedBy(CapturingConnectionFactory.throwing(new IOException("boom")));

        assertThat(success.execute(request(), configuration(), "windows").getSession().isOperationInProgress()).isFalse();
        assertThat(failure.execute(request(), configuration(), "windows").getSession().isOperationInProgress()).isFalse();
    }
}
