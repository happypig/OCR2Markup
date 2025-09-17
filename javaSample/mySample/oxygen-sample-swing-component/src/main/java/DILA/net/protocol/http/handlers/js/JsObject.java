package DILA.net.protocol.http.handlers.js;

import java.applet.Applet;

/**
 * Wrapper object over a <code>netscape.javascript.JSObject</code>
 * 
 * @author alin_balasa
 */
public class JsObject {
  /**
   * The real <code>netscape.javascript.JSObject</code>
   */
  private Object innerJSObject = null;
  
  
  /**
   * Builds a wrapper object over the real <code>netscape.javascript.JSObject</code>
   * 
   * @param realJsObject The real JSObect.
   * 
   * @throws JsException thrown when the provided object is not a <code>netscape.javascript.JSObject</code>.
   */
  public JsObject(Object realJsObject) throws JsException {
    this.innerJSObject = realJsObject;
    JsObjectUtil.checkJsObject(realJsObject);
  }
  
  /**
   * Returns a JSObject for the window containing the given applet.
   * 
   * @param applet The current Java Applet.
   *  
   * @return a JSObject for the window containing the given applet.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public static JsObject getWindow(Applet applet) throws JsException {
    return (JsObject) JsObjectUtil.invokeJsObjectMethod(JsObjectMethod.GET_WINDOW, null, new Object[] {applet});
  }
  
  /**
   * Calls a JavaScript method. Equivalent to "this.methodName(args[0], args[1], ...)" in JavaScript.
   * 
   * @param methodName The name of the JavaScript method to be invoked.
   * @param args An array of Java objects to be passed as arguments to the method.
   * 
   * @return The result of the JavaScript method.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public Object call(String methodName, java.lang.Object[] args) throws JsException {
    if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        args[i] = JsObjectUtil.unwrapJSObject(args[i]);
      }
    }
    return JsObjectUtil.invokeJsObjectMethod(JsObjectMethod.CALL, innerJSObject, new Object[] {methodName, args});
  }
  
  /**
   * Evaluates a JavaScript expression. The expression is a string of JavaScript source code 
   * which will be evaluated in the context given by "this".
   * 
   * @param expression The JavaScript expression.
   * 
   * @return The result of the JavaScript evaluation.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public Object eval(String expression) throws JsException {
    return JsObjectUtil.invokeJsObjectMethod(JsObjectMethod.EVAL, innerJSObject, new Object[] {expression});
  }

  /**
   * Retrieves a named member of a JavaScript object. Equivalent to "this.memberName" in JavaScript.
   * 
   * @param memberName The name of the JavaScript property to be accessed.
   * 
   * @return The value of the property.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public Object getMember(String memberName) throws JsException {
    return JsObjectUtil.invokeJsObjectMethod(JsObjectMethod.GET_MEMBER, innerJSObject, new Object[] {memberName});
  }
  
  /**
   * Sets a named member of a JavaScript object. Equivalent to "this.memberName = value" in JavaScript.
   * 
   * @param memberName The name of the JavaScript property to be accessed.
   * 
   * @param value The value of the property.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public void setMember(String memberName, Object value) throws JsException {
    JsObjectUtil.invokeJsObjectMethod(JsObjectMethod.SET_MEMBER, innerJSObject, new Object[] {memberName, JsObjectUtil.unwrapJSObject(value)});
  }

  /**
   * Removes a named member of a JavaScript object. Equivalent to "delete this.memberName" in JavaScript.
   * 
   * @param memberName The name of the JavaScript property to be removed.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public void removeMember(String memberName) throws JsException {
    JsObjectUtil.invokeJsObjectMethod(JsObjectMethod.REMOVE_MEMBER, innerJSObject, new Object[] {memberName});
  }

  /**
   * Retrieves an indexed member of a JavaScript object. Equivalent to "this[index]" in JavaScript.
   * 
   * @param index The index of the array to be accessed.
   * 
   * @return The value of the indexed member.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public Object getSlot(int index) throws JsException {
    return JsObjectUtil.invokeJsObjectMethod(JsObjectMethod.GET_SLOT, innerJSObject, new Object[] {Integer.valueOf(index)});
  }

  /**
   * Sets an indexed member of a JavaScript object. Equivalent to "this[index] = value" in JavaScript.
   * 
   * @param index The index of the array to be accessed.
   * @param value The value of the indexed member.
   * 
   * @throws JsException thrown when JavaScript code execution fails.
   */
  public void setSlot(int index, Object value) throws JsException {
    JsObjectUtil.invokeJsObjectMethod(JsObjectMethod.SET_SLOT, innerJSObject, new Object[] {Integer.valueOf(index), JsObjectUtil.unwrapJSObject(value)});
  }
  
  /**
   * Returns the wrapped <code>netscape.javascript.JSObject</code>
   * 
   * @return the wrapped <code>netscape.javascript.JSObject</code>
   */
  public Object getInnerJSObject() {
    return innerJSObject;
  }
}
