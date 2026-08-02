package com.dila.dama.plugin.infrastructure.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Test seam for the CBRD Parse client: records everything the client puts on the wire and
 * replays a canned response. Mirrors the private fake inside {@code CBRDAPIClientTest} but is
 * shared, because four CBRD Parse test classes need it.
 */
public final class CapturingConnectionFactory extends HttpUrlConnectionFactory {

    // Deliberately NOT named responseCode/responseMessage: HttpURLConnection declares protected
    // fields with those names, and an inherited member shadows the outer class's field inside
    // the inner connection below.
    private final int stubResponseCode;
    private final String stubResponseBody;
    private final String stubErrorBody;
    private final IOException stubFailure;

    private URL lastUrl;
    private String lastMethod;
    private final Map<String, String> lastRequestProperties = new HashMap<>();
    private final ByteArrayOutputStream capturedRequestBody = new ByteArrayOutputStream();
    private int lastConnectTimeout = -1;
    private int lastReadTimeout = -1;
    private boolean disconnected;

    private CapturingConnectionFactory(int responseCode, String responseBody, String errorBody, IOException failure) {
        this.stubResponseCode = responseCode;
        this.stubResponseBody = responseBody;
        this.stubErrorBody = errorBody;
        this.stubFailure = failure;
    }

    /** HTTP 200 with an XML success body. */
    public static CapturingConnectionFactory respondingWith(int responseCode, String responseBody) {
        return new CapturingConnectionFactory(responseCode, responseBody, null, null);
    }

    /** Non-2xx status with a JSON ParseError body on the error stream. */
    public static CapturingConnectionFactory failingWith(int responseCode, String errorBody) {
        return new CapturingConnectionFactory(responseCode, "", errorBody, null);
    }

    /** Transport-level failure (connection refused, read timeout, DNS failure). */
    public static CapturingConnectionFactory throwing(IOException failure) {
        return new CapturingConnectionFactory(0, "", null, failure);
    }

    @Override
    public HttpURLConnection openConnection(URL url) {
        this.lastUrl = url;
        return new FakeHttpURLConnection(url);
    }

    public URL getLastUrl() {
        return lastUrl;
    }

    public String getLastMethod() {
        return lastMethod;
    }

    public String getRequestProperty(String key) {
        return lastRequestProperties.get(key);
    }

    public String getCapturedRequestBody() {
        return new String(capturedRequestBody.toByteArray(), StandardCharsets.UTF_8);
    }

    public int getLastConnectTimeout() {
        return lastConnectTimeout;
    }

    public int getLastReadTimeout() {
        return lastReadTimeout;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    private final class FakeHttpURLConnection extends HttpURLConnection {

        FakeHttpURLConnection(URL url) {
            super(url);
        }

        @Override
        public void setRequestMethod(String method) {
            lastMethod = method;
        }

        @Override
        public void setRequestProperty(String key, String value) {
            lastRequestProperties.put(key, value);
        }

        @Override
        public String getRequestProperty(String key) {
            return lastRequestProperties.get(key);
        }

        @Override
        public void setConnectTimeout(int timeout) {
            lastConnectTimeout = timeout;
        }

        @Override
        public void setReadTimeout(int timeout) {
            lastReadTimeout = timeout;
        }

        @Override
        public OutputStream getOutputStream() {
            return capturedRequestBody;
        }

        @Override
        public int getResponseCode() throws IOException {
            if (stubFailure != null) {
                throw stubFailure;
            }
            return stubResponseCode;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (stubFailure != null) {
                throw stubFailure;
            }
            return new ByteArrayInputStream(stubResponseBody.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public InputStream getErrorStream() {
            if (stubErrorBody == null) {
                return null;
            }
            return new ByteArrayInputStream(stubErrorBody.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void disconnect() {
            disconnected = true;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
        }
    }
}
