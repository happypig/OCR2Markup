package com.dila.dama.plugin.domain.model;

import java.util.Objects;

public final class SanitizedTroubleshootingRecord {

    private final String requestId;
    private final String platform;
    private final String endpointSummary;
    private final String requestSnapshot;
    private final Integer httpStatus;
    private final String serviceErrorBody;
    private final DiagnosticFailureCategory failureCategory;
    private final String guidanceMessageKey;
    private final long capturedAtEpochMs;
    private final boolean redactionApplied;
    private final String transportError;

    public SanitizedTroubleshootingRecord(
        String requestId,
        String platform,
        String endpointSummary,
        String requestSnapshot,
        Integer httpStatus,
        String serviceErrorBody,
        DiagnosticFailureCategory failureCategory,
        String guidanceMessageKey,
        long capturedAtEpochMs,
        boolean redactionApplied
    ) {
        this(requestId, platform, endpointSummary, requestSnapshot, httpStatus, serviceErrorBody,
            failureCategory, guidanceMessageKey, capturedAtEpochMs, redactionApplied, null);
    }

    public SanitizedTroubleshootingRecord(
        String requestId,
        String platform,
        String endpointSummary,
        String requestSnapshot,
        Integer httpStatus,
        String serviceErrorBody,
        DiagnosticFailureCategory failureCategory,
        String guidanceMessageKey,
        long capturedAtEpochMs,
        boolean redactionApplied,
        String transportError
    ) {
        this.requestId = requestId;
        this.platform = platform;
        this.endpointSummary = endpointSummary;
        this.requestSnapshot = requestSnapshot;
        this.httpStatus = httpStatus;
        this.serviceErrorBody = serviceErrorBody;
        this.failureCategory = failureCategory;
        this.guidanceMessageKey = guidanceMessageKey;
        this.capturedAtEpochMs = capturedAtEpochMs;
        this.redactionApplied = redactionApplied;
        this.transportError = transportError;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPlatform() {
        return platform;
    }

    public String getEndpointSummary() {
        return endpointSummary;
    }

    public String getRequestSnapshot() {
        return requestSnapshot;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getServiceErrorBody() {
        return serviceErrorBody;
    }

    public DiagnosticFailureCategory getFailureCategory() {
        return failureCategory;
    }

    public String getGuidanceMessageKey() {
        return guidanceMessageKey;
    }

    public long getCapturedAtEpochMs() {
        return capturedAtEpochMs;
    }

    public boolean isRedactionApplied() {
        return redactionApplied;
    }

    /** Null when the failure carried no transport-level exception (e.g. a known HTTP status). */
    public String getTransportError() {
        return transportError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SanitizedTroubleshootingRecord)) return false;
        SanitizedTroubleshootingRecord that = (SanitizedTroubleshootingRecord) o;
        return capturedAtEpochMs == that.capturedAtEpochMs
            && redactionApplied == that.redactionApplied
            && Objects.equals(requestId, that.requestId)
            && Objects.equals(platform, that.platform)
            && Objects.equals(endpointSummary, that.endpointSummary)
            && Objects.equals(requestSnapshot, that.requestSnapshot)
            && Objects.equals(httpStatus, that.httpStatus)
            && Objects.equals(serviceErrorBody, that.serviceErrorBody)
            && failureCategory == that.failureCategory
            && Objects.equals(guidanceMessageKey, that.guidanceMessageKey)
            && Objects.equals(transportError, that.transportError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, platform, endpointSummary, requestSnapshot, httpStatus, serviceErrorBody, failureCategory, guidanceMessageKey, capturedAtEpochMs, redactionApplied, transportError);
    }
}
