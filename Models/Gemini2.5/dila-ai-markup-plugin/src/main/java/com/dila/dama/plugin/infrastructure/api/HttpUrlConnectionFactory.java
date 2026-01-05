package com.dila.dama.plugin.infrastructure.api;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.util.List;

public class HttpUrlConnectionFactory {

    public HttpURLConnection openConnection(URL url) throws IOException {
        Proxy proxy = Proxy.NO_PROXY;
        try {
            ProxySelector selector = ProxySelector.getDefault();
            if (selector != null) {
                List<Proxy> proxies = selector.select(url.toURI());
                if (proxies != null && !proxies.isEmpty()) {
                    proxy = proxies.get(0);
                }
            }
        } catch (Exception ignored) {
            // Fall back to direct connection.
        }
        return (HttpURLConnection) url.openConnection(proxy);
    }
}
