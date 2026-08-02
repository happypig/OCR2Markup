package com.dila.dama.plugin.domain.model;

/**
 * The nine failure causes the DILA CBRD Parse service enumerates, plus the two the plugin
 * determines locally (FR-011, FR-012, FR-013).
 *
 * Wire codes are authoritative in {@code specs/004-cbrd-parse-endpoint/contracts/openapi.yaml}.
 */
public enum ParseError {

    TEXT_IS_REQUIRED("text_is_required", "ai.markup.error.text_is_required", DiagnosticFailureCategory.MALFORMED_REQUEST),
    TEXT_IS_TOO_LONG("text_is_too_long", "ai.markup.error.text_is_too_long", DiagnosticFailureCategory.MALFORMED_REQUEST),
    UNSUPPORTED_LANGUAGE("unsupported_language", "ai.markup.error.unsupported_language", DiagnosticFailureCategory.MALFORMED_REQUEST),

    /** The editor's shared token was rejected — the only cause that is their credential problem. */
    UNAUTHORIZED("unauthorized", "ai.markup.error.unauthorized", DiagnosticFailureCategory.CREDENTIALS),

    PARSE_API_NOT_CONFIGURED("parse_api_not_configured", "ai.markup.error.parse_api_not_configured", DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE),

    /**
     * The DILA server cannot reach its own model credentials. Deliberately NOT categorised as
     * CREDENTIALS: it is a server-side outage, not the editor's token being wrong.
     */
    OPENAI_CREDENTIALS_UNAVAILABLE("openai_credentials_unavailable", "ai.markup.error.openai_credentials_unavailable", DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE),

    OPENAI_RATE_LIMITED("openai_rate_limited", "ai.markup.error.openai_rate_limited", DiagnosticFailureCategory.RATE_LIMIT_OR_CAPACITY),
    OPENAI_UNAVAILABLE("openai_unavailable", "ai.markup.error.openai_unavailable", DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE),
    INVALID_MODEL_OUTPUT("invalid_model_output", "ai.markup.error.invalid_model_output", DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE),

    /** A status code or body shape the published contract does not enumerate (FR-012). */
    UNEXPECTED_RESPONSE("", "ai.markup.error.unexpected", DiagnosticFailureCategory.UNKNOWN_SERVICE_FAILURE),

    /** The plugin could not reach the service at all (FR-013). */
    CONNECTIVITY_FAILURE("", "ai.markup.error.connectivity", DiagnosticFailureCategory.CONNECTIVITY_OR_PROXY);

    private final String wireCode;
    private final String guidanceMessageKey;
    private final DiagnosticFailureCategory failureCategory;

    ParseError(String wireCode, String guidanceMessageKey, DiagnosticFailureCategory failureCategory) {
        this.wireCode = wireCode;
        this.guidanceMessageKey = guidanceMessageKey;
        this.failureCategory = failureCategory;
    }

    /**
     * Maps the service's {@code error} field to a cause. Anything the contract does not
     * enumerate becomes {@link #UNEXPECTED_RESPONSE} so the plugin degrades instead of failing.
     */
    public static ParseError fromWireCode(String wireCode) {
        if (wireCode == null) {
            return UNEXPECTED_RESPONSE;
        }
        String normalized = wireCode.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) {
            return UNEXPECTED_RESPONSE;
        }
        for (ParseError candidate : values()) {
            if (!candidate.wireCode.isEmpty() && candidate.wireCode.equals(normalized)) {
                return candidate;
            }
        }
        return UNEXPECTED_RESPONSE;
    }

    /** Empty for the two locally determined causes. */
    public String getWireCode() {
        return wireCode;
    }

    public String getGuidanceMessageKey() {
        return guidanceMessageKey;
    }

    public DiagnosticFailureCategory getFailureCategory() {
        return failureCategory;
    }
}
