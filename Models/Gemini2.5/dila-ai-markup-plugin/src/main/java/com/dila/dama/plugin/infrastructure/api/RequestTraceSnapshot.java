package com.dila.dama.plugin.infrastructure.api;

public final class RequestTraceSnapshot {

    private final String requestId;
    private final String endpointSummary;
    private final String requestMetadataSummary;

    public RequestTraceSnapshot(String requestId, String endpointSummary, String requestMetadataSummary) {
        this.requestId = requestId;
        this.endpointSummary = endpointSummary;
        this.requestMetadataSummary = requestMetadataSummary;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getEndpointSummary() {
        return endpointSummary;
    }

    public String getRequestMetadataSummary() {
        return requestMetadataSummary;
    }
}
