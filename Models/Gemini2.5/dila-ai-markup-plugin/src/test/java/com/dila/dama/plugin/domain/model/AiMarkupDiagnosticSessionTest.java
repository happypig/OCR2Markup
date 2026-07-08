package com.dila.dama.plugin.domain.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AiMarkupDiagnosticSessionTest {

    @Test
    public void preventsConcurrentOperationStart() {
        AiMarkupDiagnosticSession session = new AiMarkupDiagnosticSession(8, configuration());

        assertThat(session.startOperation()).isTrue();
        assertThat(session.startOperation()).isFalse();

        session.finishOperation();

        assertThat(session.startOperation()).isTrue();
    }

    @Test
    public void tracksFailureAndExportLifecycle() {
        AiMarkupDiagnosticSession session = new AiMarkupDiagnosticSession("session-1", 8, configuration(), 100L);
        SanitizedTroubleshootingRecord record = new SanitizedTroubleshootingRecord(
            "request-1",
            "windows",
            configuration().getEndpointSummary(),
            "snapshot",
            400,
            "body",
            DiagnosticFailureCategory.MALFORMED_REQUEST,
            "ai.markup.diagnostic.malformed.request.windows",
            200L,
            true
        );

        session.validatingConfiguration();
        session.buildingRequest();
        session.callingEndpoint();
        session.parsingResponse();
        session.classifiedFailure(DiagnosticFailureCategory.MALFORMED_REQUEST, "ai.markup.diagnostic.malformed.request.windows", record);
        session.exportReady();
        session.exported(new ExportedDiagnosticPackage("1.0.0", "2026-03-27T00:00:00Z", "session-1", record, "manual"));

        assertThat(session.getStatus()).isEqualTo(DiagnosticStatus.EXPORTED);
        assertThat(session.getFailureCategory()).isEqualTo(DiagnosticFailureCategory.MALFORMED_REQUEST);
        assertThat(session.getTroubleshootingRecord()).isEqualTo(record);
        assertThat(session.getExportedPackage()).isNotNull();
    }

    private MarkupServiceConfiguration configuration() {
        return new MarkupServiceConfiguration(
            "https://api.openai.com",
            "/v1/chat/completions",
            "gpt-test",
            "sk-example-key",
            30000,
            MarkupServiceConfiguration.ENDPOINT_KIND_OPENAI_HOSTED,
            true
        );
    }
}
