package DILA.net.protocol.http.handlers.js;

/**
 * Defines an HTTP Header.
 * 
 * @author alin_balasa
 */
public class HttpHeader {
  /**
	 * The name of this header.
	 */
	private final String name;
	
	/**
	 * The value of this header.
	 */
	private final String value;

	/**
	 * Builds a new object representing an HTTP header.
	 * 
	 * @param name The name of the new header.
	 * @param value The value of the new header.
	 */
	public HttpHeader(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Returns the name of this header.
	 * 
	 * @return The name of this header.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the value of this header.
	 * 
	 * @return The value of this header.
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Returns a string representation of this header.
	 */
	@Override
  public String toString() {
    return "HttpHeader [name=" + name + ", value=" + value + "]";
  }
}