package com.dila.dama.plugin.infrastructure.api;

import com.dila.dama.plugin.domain.model.TransformedComponents;
import com.dila.dama.plugin.util.PluginLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public class CBRDAPIClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 250L;

    private final String apiUrl;
    private final String refererHeaderValue;
    private final int timeoutMs;
    private final HttpUrlConnectionFactory connectionFactory;

    public CBRDAPIClient(String apiUrl, String refererHeaderValue, int timeoutMs, HttpUrlConnectionFactory connectionFactory) {
        this.apiUrl = apiUrl;
        this.refererHeaderValue = refererHeaderValue;
        this.timeoutMs = timeoutMs;
        this.connectionFactory = connectionFactory != null ? connectionFactory : new HttpUrlConnectionFactory();
    }

    public String convertToFirstLink(TransformedComponents components) throws CBRDAPIException {
        if (components == null) {
            throw new CBRDAPIException("error.api.response");
        }

        String queryXml = buildQueryXml(components);
        return convertToFirstLinkWithQuery(queryXml);
    }

    public String convertToFirstLink(String refXml) throws CBRDAPIException {
        if (refXml == null || refXml.trim().isEmpty()) {
            throw new CBRDAPIException("error.invalid.xml");
        }
        return convertToFirstLinkWithQuery(refXml.trim());
    }

    private String convertToFirstLinkWithQuery(String queryXml) throws CBRDAPIException {
        try {
            String encoded = URLEncoder.encode(queryXml, "UTF-8");
            String separator = apiUrl.contains("?")
                ? (apiUrl.endsWith("?") || apiUrl.endsWith("&") ? "" : "&")
                : "?";
            URL url = new URL(apiUrl + separator + "q=" + encoded);

            return executeWithRetries(url);
        } catch (CBRDAPIException e) {
            throw e;
        } catch (Exception e) {
            PluginLogger.error("[CBRDAPIClient]Connection error: " + e.getMessage(), e);
            throw new CBRDAPIException("error.api.connection", e);
        }
    }

    private String executeWithRetries(URL url) throws Exception {
        Throwable lastTimeout = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            int attemptTimeoutMs = computeAttemptTimeoutMs(attempt);
            long startNs = System.nanoTime();
            PluginLogger.debug("[CBRDAPIClient]Request start (attempt " + attempt + "/" + MAX_ATTEMPTS
                + ", timeout=" + attemptTimeoutMs + "ms): " + url);
            try {
                String result = executeOnce(url, attemptTimeoutMs);
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                PluginLogger.debug("[CBRDAPIClient]Request success (attempt " + attempt + "/" + MAX_ATTEMPTS
                    + ", elapsed=" + elapsedMs + "ms).");
                return result;
            } catch (CBRDAPIException e) {
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                PluginLogger.debug("[CBRDAPIClient]Request failed (attempt " + attempt + "/" + MAX_ATTEMPTS
                    + ", elapsed=" + elapsedMs + "ms, reason=" + e.getMessageKey() + ").");
                throw e;
            } catch (Exception e) {
                if (isTimeoutException(e)) {
                    long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                    PluginLogger.debug("[CBRDAPIClient]Request timeout (attempt " + attempt + "/" + MAX_ATTEMPTS
                        + ", elapsed=" + elapsedMs + "ms).");
                    lastTimeout = e;
                    if (attempt < MAX_ATTEMPTS) {
                        long backoffMs = computeBackoffMs(attempt);
                        PluginLogger.warn("[CBRDAPIClient]Timeout calling CBRD API (attempt " + attempt + "/" + MAX_ATTEMPTS
                            + ", timeout=" + attemptTimeoutMs + "ms). Retrying in " + backoffMs + "ms.");
                        sleepBackoff(backoffMs);
                        continue;
                    }
                    throw new CBRDAPIException("error.api.timeout", e);
                }
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                PluginLogger.debug("[CBRDAPIClient]Request error (attempt " + attempt + "/" + MAX_ATTEMPTS
                    + ", elapsed=" + elapsedMs + "ms, error=" + e.getClass().getSimpleName() + ").");
                throw e;
            }
        }
        throw new CBRDAPIException("error.api.timeout", lastTimeout);
    }

    private String executeOnce(URL url, int attemptTimeoutMs) throws Exception {
        HttpURLConnection conn = connectionFactory.openConnection(url);
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(attemptTimeoutMs);
        conn.setReadTimeout(attemptTimeoutMs);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Referer", refererHeaderValue);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        conn.setRequestProperty("User-Agent", "DILA-AI-Markup/0.4.0");
        logRequestHeaders(url, conn);

        int status = conn.getResponseCode();
        String body = readAll(status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream());

        if (status != 200) {
            PluginLogger.warn("[CBRDAPIClient]Non-200 HTTP status: " + status + ", body: " + body);
            throw new CBRDAPIException("error.api.http", status);
        }

        final CBRDResponse response;
        try {
            response = CBRDResponse.fromJson(body);
        } catch (Exception e) {
            PluginLogger.error("[CBRDAPIClient]Invalid JSON response: " + body);
            throw new CBRDAPIException("error.api.response", e);
        }

        if (!response.isSuccess()) {
            PluginLogger.warn("[CBRDAPIClient]API returned success=false: " + response.getError());
            throw new CBRDAPIException("error.api.response");
        }

        String first = response.getFirstUrl();
        if (first == null || first.trim().isEmpty()) {
            throw new CBRDAPIException("error.no.results");
        }

        return first;
    }

    private static void logRequestHeaders(URL url, HttpURLConnection conn) {
        if (!PluginLogger.isDebugEnabled()) {
            return;
        }
        PluginLogger.debug("[CBRDAPIClient]Request headers for " + url + " -> Referer="
            + conn.getRequestProperty("Referer")
            + ", Accept=" + conn.getRequestProperty("Accept")
            + ", Accept-Charset=" + conn.getRequestProperty("Accept-Charset")
            + ", User-Agent=" + conn.getRequestProperty("User-Agent"));
    }

    private int computeAttemptTimeoutMs(int attempt) {
        long scaled = (long) timeoutMs * attempt;
        return scaled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
    }

    private long computeBackoffMs(int attempt) {
        long backoff = BASE_BACKOFF_MS * (1L << (attempt - 1));
        return Math.max(0L, backoff);
    }

    private static boolean isTimeoutException(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timed out")) {
                return true;
            }
        }
        return false;
    }

    private static void sleepBackoff(long backoffMs) {
        if (backoffMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String buildQueryXml(TransformedComponents components) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ref>");
        sb.append("<canon>").append(escapeXml(components.getCanonCode())).append("</canon>");
        sb.append("<v>").append(escapeXml(components.getVolume())).append("</v>");
        if (components.getWork() != null) {
            sb.append(".<w>").append(escapeXml(components.getWork())).append("</w>");
        }
        if (components.getPage() != null) {
            sb.append("<p>").append(escapeXml(components.getPage())).append("</p>");
        }
        if (components.getColumn() != null) {
            sb.append("<c>").append(escapeXml(components.getColumn())).append("</c>");
        }
        if (components.getLine() != null) {
            sb.append("<l>").append(escapeXml(components.getLine())).append("</l>");
        }
        sb.append("</ref>");
        return sb.toString();
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private static String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
