package com.dila.dama.plugin.domain.model;

public enum DiagnosticFailureCategory {
    CREDENTIALS,
    MODEL_ACCESS,
    MALFORMED_REQUEST,
    ENDPOINT_COMPATIBILITY,
    CONNECTIVITY_OR_PROXY,
    RATE_LIMIT_OR_CAPACITY,
    UNKNOWN_SERVICE_FAILURE
}
