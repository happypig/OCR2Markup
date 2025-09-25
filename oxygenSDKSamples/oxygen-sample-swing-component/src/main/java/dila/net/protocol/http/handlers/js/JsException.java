package dila.net.protocol.http.handlers.js;

/**
 * Exception thrown when JavaScript code execution fails.
 * 
 * @author alin_balasa
 */
@SuppressWarnings("serial")
public class JsException extends Exception {

  /**
   * Constructs a new exception with the specified detail message and
   * cause.
   *
   * @param message the detail message.
   * @param cause the cause for this exception.
   */
  public JsException(String message, Throwable cause) {
    super(message, cause);
  }
}
