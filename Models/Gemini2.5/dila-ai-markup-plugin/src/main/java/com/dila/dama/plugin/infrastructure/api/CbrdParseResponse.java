package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.ParseError;

/**
 * Outcome of one call to the DILA CBRD Parse endpoint.
 *
 * Success carries the TEI P5 markup exactly as the service returned it — the service emits the
 * complete {@code <ref>} element, so the caller must not wrap it again.
 */
public final class CbrdParseResponse {

    private final boolean success;
    private final String markupXml;
    private final Integer httpStatus;
    private final ParseError error;
    private final String errorBody;
    private final RequestTraceSnapshot trace;
    private final Exception exception;

    private CbrdParseResponse(
        boolean success,
        String markupXml,
        Integer httpStatus,
        ParseError error,
        String errorBody,
        RequestTraceSnapshot trace,
        Exception exception
    ) {
        this.success = success;
        this.markupXml = markupXml;
        this.httpStatus = httpStatus;
        this.error = error;
        this.errorBody = errorBody;
        this.trace = trace;
        this.exception = exception;
    }

    public static CbrdParseResponse success(String markupXml, Integer httpStatus, RequestTraceSnapshot trace) {
        return new CbrdParseResponse(true, markupXml == null ? "" : markupXml, httpStatus, null, "", trace, null);
    }

    public static CbrdParseResponse failure(
        Integer httpStatus,
        ParseError error,
        String errorBody,
        RequestTraceSnapshot trace,
        Exception exception
    ) {
        return new CbrdParseResponse(false, "", httpStatus, error, errorBody == null ? "" : errorBody, trace, exception);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMarkupXml() {
        return markupXml;
    }

    /** Null when the request never reached the service. */
    public Integer getHttpStatus() {
        return httpStatus;
    }

    public ParseError getError() {
        return error;
    }

    /** Already redacted; safe for the diagnostics export. */
    public String getErrorBody() {
        return errorBody;
    }

    public RequestTraceSnapshot getTrace() {
        return trace;
    }

    public Exception getException() {
        return exception;
    }
}
