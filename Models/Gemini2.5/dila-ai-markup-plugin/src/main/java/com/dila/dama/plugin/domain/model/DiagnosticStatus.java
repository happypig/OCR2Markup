package com.dila.dama.plugin.domain.model;

public enum DiagnosticStatus {
    INITIALIZED,
    VALIDATING_CONFIGURATION,
    BUILDING_REQUEST,
    CALLING_ENDPOINT,
    PARSING_RESPONSE,
    CLASSIFIED_FAILURE,
    COMPLETED_SUCCESS,
    EXPORT_READY,
    EXPORTED
}
