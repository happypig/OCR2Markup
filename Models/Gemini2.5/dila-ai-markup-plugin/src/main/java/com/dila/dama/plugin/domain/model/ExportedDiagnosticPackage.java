package com.dila.dama.plugin.domain.model;

import java.util.Objects;

public final class ExportedDiagnosticPackage {

    private final String schemaVersion;
    private final String generatedAt;
    private final String sessionId;
    private final SanitizedTroubleshootingRecord record;
    private final String exportReason;

    public ExportedDiagnosticPackage(
        String schemaVersion,
        String generatedAt,
        String sessionId,
        SanitizedTroubleshootingRecord record,
        String exportReason
    ) {
        this.schemaVersion = schemaVersion;
        this.generatedAt = generatedAt;
        this.sessionId = sessionId;
        this.record = record;
        this.exportReason = exportReason;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SanitizedTroubleshootingRecord getRecord() {
        return record;
    }

    public String getExportReason() {
        return exportReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExportedDiagnosticPackage)) return false;
        ExportedDiagnosticPackage that = (ExportedDiagnosticPackage) o;
        return Objects.equals(schemaVersion, that.schemaVersion)
            && Objects.equals(generatedAt, that.generatedAt)
            && Objects.equals(sessionId, that.sessionId)
            && Objects.equals(record, that.record)
            && Objects.equals(exportReason, that.exportReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, generatedAt, sessionId, record, exportReason);
    }
}
