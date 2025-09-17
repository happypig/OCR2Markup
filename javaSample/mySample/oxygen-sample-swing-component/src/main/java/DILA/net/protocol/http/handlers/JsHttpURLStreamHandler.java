package DILA.net.protocol.http.handlers;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.swing.JApplet;


/**
 * Custom URL Stream Handler that executes HTTP requests through JavaScript.. 
 */
class JsHttpURLStreamHandler extends URLStreamHandler {

  /**
   * The name of the class that implements the URLConnection.
   * Instantiate the <code>JsHttpURLConnection</code> using reflection, otherwise a
   * <code>java.lang.ClassCircularityError</code> is thrown by the ClassLoader that loads the Java Applet classes.
   */
  private final static String URL_CONNECTION_CLASS = JsHttpURLStreamHandler.class.getPackage().getName() + ".js.JsHttpURLConnection";

  /**
   * Stores the constructor of the URLConnection implementation.
   */
  private Constructor<?> constructor;

  /**
   * The current Java Applet
   */
  private final JApplet applet;    
  
  /**
   * Constructor.
   * 
   * @param applet The current Java Applet
   */
  public JsHttpURLStreamHandler(JApplet applet){
    this.applet = applet;
  }
  
  
  /**
   * Returns the URLConnection for the given URL.
   *
   * @param u The associated  URL.
   * @return The URLConnection.
   * 
   * @exception IOException if the connection could not be built.
   */
  @Override
  public URLConnection openConnection(URL u) throws IOException{
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try {
      if(constructor == null){
        Class<?> connectionClass = null;
        
        // Instantiate the <code>JsHttpURLConnection</code> using reflection, otherwise a
        // java.lang.ClassCircularityError is thrown by the ClassLoader that loads the Java Applet classes.
        if(classLoader != null) {
          connectionClass = classLoader.loadClass(URL_CONNECTION_CLASS);
        } else {
          connectionClass = ClassLoader.getSystemClassLoader().loadClass(URL_CONNECTION_CLASS);
        }
        
        // The constructor for the URL Connection.
        constructor =
          connectionClass.getConstructor(new Class[] {URL.class, JApplet.class });
      }

      Object c = constructor.newInstance(new Object[] { u, applet });
      return (URLConnection) c;
    } catch (Exception e) {
      throw new IOException(e.getMessage(), e);
    }
  }
  
  /**
   * Compare 2 URLs by avoid resolving the host of the URL because 
   * resolving the host is a long operation.
   * 
   * @param u1 First URL.
   * @param u2 Second URL.
   * @return True if the 2 URLs are equals, false if they are not equal.
   */
  @Override
  protected boolean equals(URL u1, URL u2) {
    boolean equal = super.equals(u1, u2);
    if(equal) {
      //EXM-17751 Also compare the user name
      if(u1.getUserInfo() == null && u2.getUserInfo() == null) {
        //OK
      } else if(u1.getUserInfo() != null && u1.getUserInfo().equals(u2.getUserInfo())) {
        //OK
      } else {
        //Different user info, The Oxygen hash maps prefer to treat them as different URLs
        equal = false;
      }
    }
    return equal;
  }

  /**
   * Compares the host components of two URLs.
   * 
   * @param u1 the URL of the first host to compare 
   * @param u2 the URL of the second host to compare
   *  
   * @return  <code>true</code> if and only if they  are equal.
   */
  @Override
  protected boolean hostsEqual(URL u1, URL u2) {
    // EXM-17751 - Do not resolve Hosts...
    if (u1.getHost() != null && u2.getHost() != null) {
      return u1.getHost().equalsIgnoreCase(u2.getHost());
    } else {
      return u1.getHost() == null && u2.getHost() == null;
    }
  }
  
  /**
   * Provides the default hash calculation. May be overidden by handlers for
   * other protocols that have different requirements for hashCode
   * calculation.
   * 
   * @param u an URL object
   * 
   * @return an <code>int</code> suitable for hash table indexing
   */
  @Override
  protected int hashCode(URL u) {
    int h = 0;

    // Generate the protocol part.
    String protocol = u.getProtocol();
    if (protocol != null) {
      h += protocol.hashCode();
    }

    // Generate the host part.
    String host = u.getHost();
    if (host != null) {
      h += host.toLowerCase().hashCode();
    }

    // Generate the file part.
    String file = u.getFile();
    if (file != null) {
      h += file.hashCode();
    }

    // Generate the port part.
    if (u.getPort() == -1) {
      h += getDefaultPort();
    } else {
      h += u.getPort();
    }

    // Generate the ref part.
    String ref = u.getRef();
    if (ref != null) {
      h += ref.hashCode();
    }

    return h;
  }
}