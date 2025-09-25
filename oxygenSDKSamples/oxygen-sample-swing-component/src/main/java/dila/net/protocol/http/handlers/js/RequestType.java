package dila.net.protocol.http.handlers.js;

/**
 * Defines the available request types.
 * 
 * @author alin_balasa
 */
public enum RequestType {
	/**
	 * HTTP Get.
	 */
	GET("GET"),
	
	/**
	 * HTTP Post.
	 */
	POST("POST"),
	
	/**
	 * HTTP Put.
	 */
	PUT("PUT"),
	
	/**
	 * HTTP Delete.
	 */
	DELETE("DELETE")
	
	// Add other request types here as needed.
	;
	
	/**
	 * The request type's name.
	 */
	private String name;
	
	/**
	 * Constructor.
	 * 
	 * @param name The type's name.
	 */
	private RequestType(String name) {
		this.name = name;
	}
	
	/**
	 * Returns the type's name.
	 * 
	 * @return Returns the type's name.
	 */
	public String getName() {
		return name;
	}
}