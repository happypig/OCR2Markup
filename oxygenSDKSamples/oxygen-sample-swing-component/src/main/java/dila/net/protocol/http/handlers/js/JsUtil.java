package dila.net.protocol.http.handlers.js;

import javax.swing.JApplet;


/**
 * Utility methods for dealing with JavaScript objects & data types.
 * 
 * <p><h4>Some of the methods from this class depend on the following JS code:</h4></p>
 * <p>(Add this JS code to your applet page) 
 *    <code><pre>
 * function JsUtil () {
 *   
 *   // Creates a new DataView over the given ArrayBuffer.
 *   this.createDataView = function (buffer) {
 *       return new DataView(buffer);
 *   };
 *   
 *   // Creates a new Int8Array over the given bytes array.
 *   this.createInt8Array = function(bytesArray) {
 *       var int8Array = new Int8Array(bytesArray.length);
 *       int8Array.set(bytesArray);
 *       return int8Array;
 *   }
 * };
 *    </pre></code>
 * </p>
 * 
 * @author alin_balasa
 */
public class JsUtil {
  
  /**
   * Wrapper over a JavaScript DataView object. 
   * 
   * @author alin_balasa
   *
   */
  public static class DataView {

    /**
     * The wrapped JavaScript DataView object.
     */
    private final JsObject jsDataView;

    /**
     * Constructor.
     * 
     * @param jsDataView The wrapped JavaScript DataView object.
     */
    public DataView(JsObject jsDataView) {
      this.jsDataView = jsDataView;
    }
    
    /**
     * Returns the number of bytes in the data view.
     * 
     * @return Returns the number of bytes in the data view.
     * 
     * @throws JsException thrown when JavaScript code execution fails. 
     */
    public int getLength() throws JsException {
      int length = 0;
      if (jsDataView != null) {
        length = getIntValue(jsDataView.getMember("byteLength"));
      }
      return length;
    }
    
    /**
     * Gets a signed 8-bit integer at the specified byte offset from the start of the view.
     * 
     * @param offset The byte offset.
     * 
     * @return Returns a signed 8-bit integer at the specified byte offset from the start of the view.
     * 
     * @throws JsException thrown when JavaScript code execution fails. 
     */
    public byte getByte(int offset) throws JsException {
      byte b = 0;
      if (jsDataView != null) {
       Object jsInt8 = jsDataView.call("getInt8", new Object[] {offset});
       b = (byte)getIntValue(jsInt8);
      }
      return b;
    }
  }
  
  
  /**
   * Returns the object representing the JavaScript document.
   * 
   * @param applet The current applet.
   * 
   * @return the object representing the JavaScript document.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public static JsObject getJSDocument(JApplet applet) throws JsException {
    JsObject window = JsObject.getWindow(applet);
    return (JsObject) window.getMember("document");
  }
  
  /**
   * Creates a JavaScript DataView object over the given ArrayBuffer. 
   * 
   * @param applet The current JApplet.
   * @param arrayBuffer The JavaScript ArrayBuffer object.
   * 
   * @return a new instance of a JavaScript DataView object.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public static JsObject createDataView(JApplet applet, JsObject arrayBuffer) throws JsException {
    
    JsObject jsDoc = getJSDocument(applet);
    JsObject jsUtil = (JsObject)jsDoc.eval("new JsUtil();");
    JsObject dView = (JsObject) jsUtil.call("createDataView", new Object[] {arrayBuffer});
    
    return dView;
  }

  /**
   * Retrieves the bytes array from the given JavaScript ArrayBuffer object.
   * 
   * @param applet The current JApplet.
   * @param arrayBuffer The javaScript ArrayBuffer object.
   * 
   * @return The bytes array contained by the given array buffer.
   * 
   * @throws JsException thrown when JavaScript code execution fails. 
   */
  public static byte[] getBytesArray(JApplet applet, JsObject arrayBuffer) throws JsException {
    byte[] bytes = null;
    JsObject jsDataView = createDataView(applet, arrayBuffer);
    DataView dataView = new DataView(jsDataView);
    
    int length = dataView.getLength();
    if (length >= 0) {
      bytes = new byte[length];
      for (int i = 0; i < length; i++) {
        bytes[i] = dataView.getByte(i);
      }
    }
    return bytes;
  }
  
  /**
   *  Creates a new JavaScript Int8Array object over the given bytes array.
   * 
   * @param applet The current JApplet.
   * @param bytesArray The array of bytes.
   * 
   * @return a new JavaScript Int8Array object containing the given bytes array.
   * 
   * @throws JsException thrown when JavaScript code execution fails. 
   */
  public static JsObject createInt8Array(JApplet applet, byte[] bytesArray) throws JsException {
    JsObject jsInt8Array = null;
    if (bytesArray != null) {
      JsObject jsDoc = getJSDocument(applet);
      JsObject jsUtil = (JsObject)jsDoc.eval("new JsUtil();");
      jsInt8Array = (JsObject) jsUtil.call("createInt8Array", new Object[] {bytesArray});
    }
    return jsInt8Array;
  }
  
  /**
   * Returns the integer value for the provided object, if the object represents a number.
   * Otherwise <code>0</code> is returned. 
   * 
   * @param numberObj The number object.
   * 
   * @return the integer value for the provided object, if the object represents a number.
   */
  public static int getIntValue(Object numberObj) {
    int intValue = 0;
    if (numberObj instanceof Number) {
      Number number = (Number) numberObj;
      intValue = number.intValue();
    } else {
      System.out.println("The provided object is not a number" + numberObj);
    }
    return intValue;
  }
}
