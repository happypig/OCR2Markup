package dila.net.protocol.http.handlers.js;

/**
 * Defines the status of the XMLHttpRequest.
 * 
 * @author alin_balasa
 */
public enum RequestState {
	/**
	 * 0 - Request not initialized.
	 */
	UNSENT,
	
	/**
	 * 1 - Server connection established.
	 * The open() method has been successfully invoked.
	 * During this state request headers can be set using setRequestHeader() 
	 * and the request can be made using the send() method.
	 */
	OPENED,
	
	/**
	 * 2 - Request received.
	 * All redirects (if any) have been followed and all 
	 * HTTP headers of the final response have been received.
	 */
	HEADERS_RECEIVED,
	
	/**
	 * 3 - Processing request. 
	 * The response entity body is being received.
	 */
	LOADING,
	
	/**
	 * 4 - Request finished and response is ready.
	 * The data transfer has been completed or something went wrong during the transfer
	 */
	DONE;
}