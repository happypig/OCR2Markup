package DILA.net.protocol.http.handlers;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import javax.swing.JApplet;

/**
 * Custom {@link URLStreamHandlerFactory} for HTTP and HTTPS protocols. 
 * 
 * @author alin_balasa
 *
 */
public class CustomURLStreamHandlerFactory implements URLStreamHandlerFactory {
  
  /**
   * The current Java Applet.
   */
  private final JApplet applet;

  /**
   * Constructor.
   * 
   * @param applet The current Java Applet.
   */
  public CustomURLStreamHandlerFactory(JApplet applet) {
    this.applet = applet;
  }
  
  /**
   * Creates a new <code>URLStreamHandler</code> instance with the specified protocol.
   *
   * @param   protocol   the protocol.
   * 
   * @return  a <code>URLStreamHandler</code> for the specific protocol.
   */
  public URLStreamHandler createURLStreamHandler(final String protocol) {
      
      if("http".equals(protocol) || "https".equals(protocol) ) {
        // Custom Http URLStream Handler
        return new URLStreamHandler() {
          @Override
          protected URLConnection openConnection(URL u) throws IOException {
            String urlStr = u.toString();
            
            URLConnection connection;
            if (
                // Let the JNLP Class Loader to load its JAR files through the default Sun Handlers
                urlStr.endsWith(".jar") ||
                // Let the requests to the certificate authority be executed through the default Sun Handlers
                u.getHost().contains("ocsp.thawte.com")) {
              if ("http".equals(protocol)) {
                connection = new DefaultURLStreamHandler("sun.net.www.protocol.http.Handler", 80).openConnection(u);                          
              } else {
                connection = new DefaultURLStreamHandler("sun.net.www.protocol.https.Handler", 443).openConnection(u);
              }
            } else {
              // JS
              connection = new JsHttpURLStreamHandler(applet).openConnection(u);
            }
            return connection;
          }
        };
      } else {
        return null;
      }
    }
}
