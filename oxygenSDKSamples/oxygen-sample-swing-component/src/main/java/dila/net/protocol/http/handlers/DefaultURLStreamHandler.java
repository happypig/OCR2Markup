package dila.net.protocol.http.handlers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Wraps a default implementation for {@link URLStreamHandler}.
 * Uses reflection to instantiate the wrapped handler.
 */
class DefaultURLStreamHandler extends URLStreamHandler {

  /**
   * The name of the class of the default {@link URLStreamHandler}.
   */
  private final String handlerClassName;

  /**
   * Stores the constructor of the URLStreamHandler impl.
   */
  private Object handlerInstance;

  /**
   * The default port number for this {@link URLStreamHandler}.
   */
  private final int defaultPort;

  /**
   * Constructor.
   * 
   * @param handlerClassName
   *          The name of the class of the default {@link URLStreamHandler}.
   * @param defaultPort
   *          The default port number for this {@link URLStreamHandler}.
   */
  public DefaultURLStreamHandler(String handlerClassName, int defaultPort) {
    this.handlerClassName = handlerClassName;
    this.defaultPort = defaultPort;
  }

  /**
   * Instantiate the handler by reflection.
   * 
   * @throws Exception If it fails.
   */
  private void initHandler() throws Exception {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    Class<?> connectionClass = null;

    if (classLoader != null) {
      connectionClass = classLoader.loadClass(handlerClassName);
    } else {
      connectionClass = ClassLoader.getSystemClassLoader().loadClass(
          handlerClassName);
    }

    // The constructor for the URL Stream Handler.
    Constructor<?> constructor = connectionClass.getConstructor();

    handlerInstance = constructor.newInstance();
  }

  /**
   * @see java.net.URLStreamHandler#openConnection(java.net.URL, java.net.Proxy)
   */
  @Override
  public URLConnection openConnection(URL u, Proxy p) throws IOException {
    try {
      if (handlerInstance == null) {
        initHandler();
      }

      Method method = handlerInstance.getClass().getDeclaredMethod("openConnection", new Class[] { URL.class, Proxy.class });
      if (method != null) {
        method.setAccessible(true);
        return (URLConnection) method.invoke(handlerInstance, u, p);
      }
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException(e.getMessage(), e);
    }
  }

  /**
   * @see java.net.URLStreamHandler#openConnection(java.net.URL)
   */
  @Override
  public URLConnection openConnection(URL u) throws IOException {
    try {
      if (handlerInstance == null) {
        initHandler();
      }

      Method method = handlerInstance.getClass().getDeclaredMethod("openConnection", new Class[] { java.net.URL.class });
      if (method != null) {
        method.setAccessible(true);
        return (URLConnection) method.invoke(handlerInstance, u);
      }

      return null;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException(e.getMessage(), e);
    }
  }

  /**
   * Compare 2 URLs by avoid resolving the host of the URL because resolving the
   * host is a long operation.
   * 
   * @param u1
   *          First URL.
   * @param u2
   *          Second URL.
   * @return True if the 2 URLs are equals, false if they are not equal.
   */
  @Override
  protected boolean equals(URL u1, URL u2) {
    if (u1 == null) {
      return u2 == null;
    }

    if (u2 == null) {
      return false;
    }

    return u1.toExternalForm().equals(u2.toExternalForm());
  }
  
  /**
   * Returns the default port for a URL parsed by this handler.
   * 
   * @return Returns the default port for a URL parsed by this handler.
   */
  @Override
  protected int getDefaultPort() {
    return defaultPort;
  };
}