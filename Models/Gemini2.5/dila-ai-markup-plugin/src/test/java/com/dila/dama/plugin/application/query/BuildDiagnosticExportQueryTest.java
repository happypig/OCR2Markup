package com.dila.dama.plugin.application.query;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.ExportedDiagnosticPackage;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildDiagnosticExportQueryTest {

    private final BuildDiagnosticExportQuery query = new BuildDiagnosticExportQuery();

    @Test
    public void assemblesSchemaAndSupportFields() {
        SanitizedTroubleshootingRecord record = new SanitizedTroubleshootingRecord(
            "request-1",
            "macos",
            "https://api.openai.com/v1/chat/completions [gpt-test]",
            "snapshot",
            401,
            "sanitized-body",
            DiagnosticFailureCategory.CREDENTIALS,
            "ai.markup.diagnostic.credentials.macos",
            10L,
            true
        );

        ExportedDiagnosticPackage diagnosticPackage = query.build("session-1", record, "manual_support_export");

        assertThat(diagnosticPackage.getSchemaVersion()).isEqualTo("1.0.0");
        assertThat(diagnosticPackage.getSessionId()).isEqualTo("session-1");
        assertThat(diagnosticPackage.getExportReason()).isEqualTo("manual_support_export");
        assertThat(diagnosticPackage.getRecord()).isEqualTo(record);
    }
}
