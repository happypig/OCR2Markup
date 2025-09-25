package dila.net.protocol.http.handlers.js;

import java.applet.Applet;

/**
 * Defines constants representing methods of a <code>netscape.javascript.JSObject</code>.
 * 
 * @author alin_balasa
 */
enum JsObjectMethod {
  /**
   * Method that returns a JSObject for the window containing the given applet.
   */
  GET_WINDOW("getWindow", Applet.class),
  /**
   * Method that calls a JavaScript method.
   */
  CALL("call", String.class, Object[].class),
  /**
   * Method that evaluates a JavaScript expression.
   */
  EVAL("eval", String.class),
  /**
   * Method that retrieves a named member of a JavaScript object.
   */
  GET_MEMBER("getMember", String.class),
  /**
   * Method that sets a named member of a JavaScript object.
   */
  SET_MEMBER("setMember", String.class, Object.class),
  /**
   * Method that removes a named member of a JavaScript object.
   */
  REMOVE_MEMBER("removeMember", String.class),
  /**
   * Method that retrieves an indexed member of a JavaScript object.
   */
  GET_SLOT("getSlot", Integer.class),
  /**
   * Method that sets an indexed member of a JavaScript object.
   */
  SET_SLOT("setSlot", Integer.class, Object.class);
 
  /**
   * The name of this method.
   */
  String name;
  
  /**
   * The array containing the types of the arguments of this method.
   */
  private Class<?>[] argumentTypes;
  
  /**
   * Constructor.
   * 
   * @param name The name of the method.
   * @param argumentTypes The type of the method arguments.
   */
  private JsObjectMethod(String name, Class<?>... argumentTypes) {
    this.name = name;
    this.argumentTypes = argumentTypes;
  }
  
  /**
   * Returns the name of this method.
   * 
   * @return The name of this method.
   */
  public String getName() {
    return name;
  }
  
  /**
   * Returns the array containing the types of the arguments of this method. 
   * 
   * @return The array containing the types of the arguments of this method.
   */
  public Class<?>[] getArgumentTypes() {
    return argumentTypes;
  }
}
