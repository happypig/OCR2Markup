package com.dila.dama.plugin.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class AiMarkupDiagnosticSession {

    private final String sessionId;
    private final int selectedTextLength;
    private final MarkupServiceConfiguration configuration;
    private final long createdAtEpochMs;

    private DiagnosticStatus status;
    private DiagnosticFailureCategory failureCategory;
    private String guidanceMessageKey;
    private SanitizedTroubleshootingRecord troubleshootingRecord;
    private ExportedDiagnosticPackage exportedPackage;
    private boolean operationInProgress;

    public AiMarkupDiagnosticSession(int selectedTextLength, MarkupServiceConfiguration configuration) {
        this(UUID.randomUUID().toString(), selectedTextLength, configuration, System.currentTimeMillis());
    }

    public AiMarkupDiagnosticSession(String sessionId, int selectedTextLength, MarkupServiceConfiguration configuration, long createdAtEpochMs) {
        this.sessionId = sessionId;
        this.selectedTextLength = selectedTextLength;
        this.configuration = configuration;
        this.createdAtEpochMs = createdAtEpochMs;
        this.status = DiagnosticStatus.INITIALIZED;
    }

    public synchronized boolean startOperation() {
        if (operationInProgress) {
            return false;
        }
        operationInProgress = true;
        return true;
    }

    public synchronized void finishOperation() {
        operationInProgress = false;
    }

    public synchronized boolean isOperationInProgress() {
        return operationInProgress;
    }

    public synchronized void validatingConfiguration() {
        this.status = DiagnosticStatus.VALIDATING_CONFIGURATION;
    }

    public synchronized void buildingRequest() {
        this.status = DiagnosticStatus.BUILDING_REQUEST;
    }

    public synchronized void callingEndpoint() {
        this.status = DiagnosticStatus.CALLING_ENDPOINT;
    }

    public synchronized void parsingResponse() {
        this.status = DiagnosticStatus.PARSING_RESPONSE;
    }

    public synchronized void classifiedFailure(
        DiagnosticFailureCategory category,
        String guidanceKey,
        SanitizedTroubleshootingRecord record
    ) {
        this.failureCategory = category;
        this.guidanceMessageKey = guidanceKey;
        this.troubleshootingRecord = record;
        this.status = DiagnosticStatus.CLASSIFIED_FAILURE;
    }

    public synchronized void completedSuccess() {
        this.status = DiagnosticStatus.COMPLETED_SUCCESS;
    }

    public synchronized void exportReady() {
        this.status = DiagnosticStatus.EXPORT_READY;
    }

    public synchronized void exported(ExportedDiagnosticPackage diagnosticPackage) {
        this.exportedPackage = diagnosticPackage;
        this.status = DiagnosticStatus.EXPORTED;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getSelectedTextLength() {
        return selectedTextLength;
    }

    public MarkupServiceConfiguration getConfiguration() {
        return configuration;
    }

    public synchronized DiagnosticStatus getStatus() {
        return status;
    }

    public synchronized DiagnosticFailureCategory getFailureCategory() {
        return failureCategory;
    }

    public synchronized String getGuidanceMessageKey() {
        return guidanceMessageKey;
    }

    public synchronized SanitizedTroubleshootingRecord getTroubleshootingRecord() {
        return troubleshootingRecord;
    }

    public synchronized ExportedDiagnosticPackage getExportedPackage() {
        return exportedPackage;
    }

    public long getCreatedAtEpochMs() {
        return createdAtEpochMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AiMarkupDiagnosticSession)) return false;
        AiMarkupDiagnosticSession that = (AiMarkupDiagnosticSession) o;
        return selectedTextLength == that.selectedTextLength
            && createdAtEpochMs == that.createdAtEpochMs
            && Objects.equals(sessionId, that.sessionId)
            && Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, selectedTextLength, configuration, createdAtEpochMs);
    }
}
