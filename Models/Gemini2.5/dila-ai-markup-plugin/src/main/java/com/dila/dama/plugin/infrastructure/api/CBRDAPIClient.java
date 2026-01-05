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

public class CBRDAPIClient {

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
        try {
            String encoded = URLEncoder.encode(queryXml, "UTF-8");
            URL url = new URL(apiUrl + "?q=" + encoded);

            HttpURLConnection conn = connectionFactory.openConnection(url);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("Referer", refererHeaderValue);
            conn.setRequestProperty("Accept", "application/json");

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
        } catch (SocketTimeoutException e) {
            throw new CBRDAPIException("error.api.timeout", e);
        } catch (CBRDAPIException e) {
            throw e;
        } catch (Exception e) {
            throw new CBRDAPIException("error.api.connection", e);
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

