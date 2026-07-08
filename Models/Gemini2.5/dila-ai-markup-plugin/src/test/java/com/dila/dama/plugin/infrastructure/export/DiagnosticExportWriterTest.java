package com.dila.dama.plugin.infrastructure.export;

import com.dila.dama.plugin.domain.model.DiagnosticFailureCategory;
import com.dila.dama.plugin.domain.model.ExportedDiagnosticPackage;
import com.dila.dama.plugin.domain.model.SanitizedTroubleshootingRecord;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class DiagnosticExportWriterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final DiagnosticExportWriter writer = new DiagnosticExportWriter();

    @Test
    public void serializesSchemaCompliantSanitizedPackage() throws Exception {
        File output = temporaryFolder.newFile("diagnostics.json");
        ExportedDiagnosticPackage diagnosticPackage = new ExportedDiagnosticPackage(
            "1.0.0",
            "2026-03-27T00:00:00Z",
            "session-1",
            new SanitizedTroubleshootingRecord(
                "request-1",
                "windows",
                "endpoint",
                "snapshot",
                429,
                "sanitized-body",
                DiagnosticFailureCategory.RATE_LIMIT_OR_CAPACITY,
                "ai.markup.diagnostic.rate.limit.windows",
                100L,
                true
            ),
            "manual_support_export"
        );

        writer.write(diagnosticPackage, output);

        String content = new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8);
        JSONObject root = new JSONObject(content);
        assertThat(root.getString("schemaVersion")).isEqualTo("1.0.0");
        assertThat(root.getJSONObject("record").getBoolean("redactionApplied")).isTrue();
    }
}
