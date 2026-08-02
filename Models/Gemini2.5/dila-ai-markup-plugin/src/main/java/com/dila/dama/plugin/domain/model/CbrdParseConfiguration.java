package com.dila.dama.plugin.domain.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * The three preference-derived values needed to reach the DILA CBRD Parse endpoint
 * (FR-002, FR-003, FR-016).
 *
 * Replaces the OpenAI-shaped configuration object for the AI Markup path: the
 * model, its credentials, and the transformation prompt now live on the DILA server, so the
 * client stores only an endpoint, a shared bearer token, and a timeout.
 */
public final class CbrdParseConfiguration {

    public static final String DEFAULT_ENDPOINT_URL = "https://cbss.dila.edu.tw/cbrd/parse";

    /**
     * The parse endpoint runs a model transformation server-side, so the default inherits the
     * timeout the AI Markup action used before this feature rather than the shorter
     * Ref-to-Link lookup timeout (FR-016).
     */
    public static final int DEFAULT_TIMEOUT_MS = 30000;

    private final String endpointUrl;
    private final int timeoutMs;
    private final String sharedToken;

    public CbrdParseConfiguration(String endpointUrl, int timeoutMs, String sharedToken) {
        this.endpointUrl = trim(endpointUrl);
        this.timeoutMs = timeoutMs;
        this.sharedToken = trim(sharedToken);
    }

    /**
     * Resolves the stored endpoint preference, falling back to the DILA production endpoint.
     */
    public static String resolveEndpointUrl(String storedValue) {
        String trimmed = trim(storedValue);
        return trimmed.isEmpty() ? DEFAULT_ENDPOINT_URL : trimmed;
    }

    /**
     * Resolves the stored timeout preference. Anything missing, unparseable, or non-positive
     * falls back to {@link #DEFAULT_TIMEOUT_MS} rather than producing a broken connection.
     */
    public static int resolveTimeoutMs(String storedValue) {
        String trimmed = trim(storedValue);
        if (trimmed.isEmpty()) {
            return DEFAULT_TIMEOUT_MS;
        }
        try {
            int parsed = Integer.parseInt(trimmed);
            return parsed > 0 ? parsed : DEFAULT_TIMEOUT_MS;
        } catch (NumberFormatException e) {
            return DEFAULT_TIMEOUT_MS;
        }
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public String getSharedToken() {
        return sharedToken;
    }

    public boolean hasSharedToken() {
        return !sharedToken.isEmpty();
    }

    /**
     * The only representation of the token that may ever be displayed, logged, or exported
     * (FR-003, NFR-004).
     */
    public String getTokenFingerprint() {
        if (!hasSharedToken()) {
            return "";
        }
        if (sharedToken.length() <= 4) {
            return "****";
        }
        return "****" + sharedToken.substring(sharedToken.length() - 4);
    }

    /**
     * Safe for diagnostics: names the endpoint without revealing the token.
     */
    public String getEndpointSummary() {
        return endpointUrl + " [CBRD Parse]";
    }

    public URI toRequestUri() throws URISyntaxException {
        return new URI(endpointUrl);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CbrdParseConfiguration)) return false;
        CbrdParseConfiguration that = (CbrdParseConfiguration) o;
        return timeoutMs == that.timeoutMs
            && Objects.equals(endpointUrl, that.endpointUrl)
            && Objects.equals(sharedToken, that.sharedToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpointUrl, timeoutMs, sharedToken);
    }

    @Override
    public String toString() {
        // Never include the token itself.
        return "CbrdParseConfiguration{endpointUrl='" + endpointUrl
            + "', timeoutMs=" + timeoutMs
            + ", token=" + getTokenFingerprint() + "}";
    }
}
