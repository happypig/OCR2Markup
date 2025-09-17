package DILA.net.protocol.http.handlers.js;

/**
 * The available response types.
 * 
 * @author alin_balasa
 */
public enum ResponseType {
	 /**
	  * Corresponds to the text response entity body.
	  */
	 DEFAULT(""),
	 /**
	  * Corresponds to the text response entity body.
	  */
	 TEXT("text"),
	 /**
	  * Corresponds to the array buffer response entity body.
	  */
	 ARRAY_BUFFER("arraybuffer"),
	 /**
	  * Corresponds to the BLOB response entity body.
	  */
	 BLOB("blob"),
	 /**
	  * Corresponds to the document response entity body.
	  */
	 DOCUMENT("document"),
	 /**
	  * Corresponds to the JSON response entity body.
	  */
	 JSON("json");
	 
	/**
	 * The value of the response type.
	 */
	private String value;

	/**
	 * Constructor.
	 * 
	 * @param value The value of the response type.
	 */
	private ResponseType(String value) {
		this.value = value;
	}
	
	/**
	 * Returns the value of the response type.
	 * 
	 * @return The value of the response type.
	 */
	public String getValue() {
		return value;
	}
}