package dila.net.protocol.http.handlers.js;

import java.lang.reflect.Method;

/**
 * Contains utility methods for dealing with a {@link dila.net.protocol.http.handlers.js.JsObject} and <code>netscape.javascript.JSObject</code>
 * 
 * @author alin_balasa
 */
class JsObjectUtil {

  /**
   * The <code>netscape.javascript.JSObject</code> class name.
   */
  public static final String JSOBJECT_CLASS_NAME = "netscape.javascript.JSObject";
  
  /**
   * Invokes a method for the given <code>netscape.javascript.JSObject</code>.
   * 
   * @param jsObjectMethod The description of the method to be invoked.
   * @param targetJsObject The object invoking the method. 
   * @param methodArgs The array containing the objects to be passed as arguments to the invoked method.
   *   
   * @return The result of the invoked method or <code>null</code> if the method has no return type.
   * 
   * @throws JsException Thrown if the method invocation fails.
   */
  public static Object invokeJsObjectMethod(
      JsObjectMethod jsObjectMethod,
      Object targetJsObject,
      Object[] methodArgs) throws JsException {
 
    try {
      Class<?> jsObjectClass = getJsObjectClass();
      Method method = jsObjectClass.getMethod(jsObjectMethod.getName(), jsObjectMethod.getArgumentTypes());
      method.setAccessible(true);
      Object methodReturn = method.invoke(targetJsObject, methodArgs);
      
      if (methodReturn != null && jsObjectClass.isAssignableFrom(methodReturn.getClass())) {
        methodReturn = new JsObject(methodReturn);
      }
      
      return methodReturn;

    } catch (NoSuchMethodException e) {
      throw new JsException("Cannot find method " + jsObjectMethod.getName(), e);
    } catch (JsException e) {
      throw e;
    } catch (Throwable e) {
      throw new JsException("Exception while invoking " + JSOBJECT_CLASS_NAME + "." + jsObjectMethod.getName() + " method", e);
    }
  }

  /**
   * Returns the Java Class object for the <code>netscape.javascript.JSObject</code>
   * 
   * @return The Java Class object for the <code>netscape.javascript.JSObject</code>
   * 
   * @throws JsException Thrown if the class cannot be found.
   */
  private static Class<?> getJsObjectClass() throws JsException {
    Class<?> jsObjectClass;
    try {
      jsObjectClass = Class.forName(JSOBJECT_CLASS_NAME);
    } catch (ClassNotFoundException e) {
      throw new JsException("Cannot find class: " + JSOBJECT_CLASS_NAME, e);
    }
    return jsObjectClass;
  }
  
  /**
   * Checks if the given object is an instance of <code>netscape.javascript.JSObject</code> class.
   * 
   * @param jsObject The object to be checked against the <code>netscape.javascript.JSObject</code> class.
   * 
   * @throws JsException Thrown if the given object is not an instance of <code>netscape.javascript.JSObject</code> class.
   */
  public static void checkJsObject(Object jsObject) throws JsException {
    Class<?> jsObjectClass = getJsObjectClass();
    if (jsObject == null) {
      throw new JsException("Null JSObject is not accepted", null);
    } else if (!jsObjectClass.isAssignableFrom(jsObject.getClass())) {
      throw new JsException(jsObject.getClass()  + " is not a subclass of " + JSOBJECT_CLASS_NAME, null);
    }
  }
  
  /**
   * Unwraps a <code>netscape.javascript.JSObject</code> from a {@link dila.net.protocol.http.handlers.js.JsObject}.
   * If the given object is not a {@link dila.net.protocol.http.handlers.js.JsObject} it is returned as is.
   * 
   * @param obj The object to be processed.
   * 
   * @return The wrapped <code>netscape.javascript.JSObject</code> or the provided object if it is not 
   *         an instance of {@link dila.net.protocol.http.handlers.js.JsObject}. 
   */
  public static Object unwrapJSObject(Object obj) {
    if (obj instanceof JsObject) {
      obj = ((JsObject)obj).getInnerJSObject();
    }
    return obj;
  }
}
