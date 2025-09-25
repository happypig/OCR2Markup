package dila.net.protocol.http.handlers.js;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JApplet;

/**
 * Wrapper class over a JavaScript XMLHttpRequest object.
 * 
 * @author alin_balasa
 */
public class JsXmlHttpRequest {
  
  /**
   * The set of not allowed request HTTP headers.
   */
  private  static final Set<String> NOT_ALLOWED_HEADERS = new HashSet<String>();
  
  /**
   * Static initialization block.
   */
  static {
    NOT_ALLOWED_HEADERS.add("accept-charset");
    NOT_ALLOWED_HEADERS.add("accept-encoding");
    NOT_ALLOWED_HEADERS.add("access-control-request-headers");
    NOT_ALLOWED_HEADERS.add("access-control-request-method");
    NOT_ALLOWED_HEADERS.add("connection");
    NOT_ALLOWED_HEADERS.add("content-length");
    NOT_ALLOWED_HEADERS.add("cookie");
    NOT_ALLOWED_HEADERS.add("cookie2");
    NOT_ALLOWED_HEADERS.add("date");
    NOT_ALLOWED_HEADERS.add("dnt");
    NOT_ALLOWED_HEADERS.add("expect");
    NOT_ALLOWED_HEADERS.add("host");
    NOT_ALLOWED_HEADERS.add("keep-alive");
    NOT_ALLOWED_HEADERS.add("origin");
    NOT_ALLOWED_HEADERS.add("referer");
    NOT_ALLOWED_HEADERS.add("te");
    NOT_ALLOWED_HEADERS.add("trailer");
    NOT_ALLOWED_HEADERS.add("transfer-encoding");
    NOT_ALLOWED_HEADERS.add("upgrade");
    NOT_ALLOWED_HEADERS.add("user-agent");
    NOT_ALLOWED_HEADERS.add("via");
  }

	/**
	 * The JavaScript XMLHttpRequest object.
	 */
	private final JsObject jsRequest;
	
	/**
	 * The parent applet.
	 */
  private final JApplet parentApplet;

	/**
	 * Constructor.
	 * 
	 * @param parentApplet The parent applet.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public JsXmlHttpRequest(JApplet parentApplet) throws JsException {
		this.parentApplet = parentApplet;
    JsObject window = JsObject.getWindow(parentApplet);
		JsObject doc = (JsObject) window.getMember("document");
		jsRequest = (JsObject) doc.eval("new XMLHttpRequest();");
	}
	
	/**
   * Executes an HTTP request and waits for the server response.
   * 
   * @param type The request type.
   * @param url The target URL.
   * @param requestHeaders The request headers. May be <code>null</code>.
   *   <p>Does not accept a header if it  is a case-insensitive match for one of the following headers:</p>
   *   <ul>
   *     <li><code>Accept-Charset</code></li>
   *     <li><code>Accept-Encoding</code></li>
   *     <li><code>Access-Control-Request-Headers</code></li>
   *     <li><code>Access-Control-Request-Method</code></li>
   *     <li><code>Connection</code></li>
   *     <li><code>Content-Length</code></li>
   *     <li><code>Cookie</code></li>
   *     <li><code>Cookie2</code></li>
   *     <li><code>Date</code></li>
   *     <li><code>DNT</code></li>
   *     <li><code>Expect</code></li>
   *     <li><code>Host</code></li>
   *     <li><code>Keep-Alive</code></li>
   *     <li><code>Origin</code></li>
   *     <li><code>Referer</code></li>
   *     <li><code>TE</code></li>
   *     <li><code>Trailer</code></li>
   *     <li><code>Transfer-Encoding</code></li>
   *     <li><code>Upgrade</code></li>
   *     <li><code>User-Agent</code></li>
   *     <li><code>Via</code></li>
   *     <li>or if the start of header is a case-insensitive match for Proxy- or Sec- (including when header is just Proxy- or Sec-).</p>
   *   </ul>
   *   <p> @see <a href="http://www.w3.org/TR/XMLHttpRequest/#dom-xmlhttprequest-setrequestheader">W3C XMLHttpRequest.setRequestHeader</a></p>
   * 
   * @param responseType The type of the expected response.                      
   * @param bytes The content to be sent.
   * 
	 * @throws JsException thrown when JavaScript code execution fails. 
   */
	public void executeRequest(RequestType type, URL url, List<HttpHeader> requestHeaders, ResponseType responseType, byte[] bytes) throws JsException {
	  
		if (jsRequest != null) {
			jsRequest.call("open", new Object[] {type.getName(), url.toExternalForm(), true});
			if (requestHeaders != null) {
				for (HttpHeader httpHeader : requestHeaders) {
					setRequestHeader(httpHeader);
				}
			}
			if (responseType != null) {
				setResponseType(responseType);
			}
			JsObject int8Array = null;
			if (bytes != null) {
				setRequestHeader(new HttpHeader("Content-length", String.valueOf(bytes.length)));
				int8Array = JsUtil.createInt8Array(parentApplet, bytes);
			}
			
			setReadTimeOut(30000);
			jsRequest.call("send", int8Array != null ? new Object[] {int8Array} : null);
			waitForResponse();
		}
	}
	
	/**
	 * Waits for response to be ready.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	private void waitForResponse() throws JsException {
    while(getState() != RequestState.DONE) {
      // Do nothing
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
	}
	
	/**
	 * <ol>
	 *   <li>If the state is UNSENT or OPENED, return <code>null</code>.</li>
	 *   <li>If the error flag is set, return <code>null</code>.</li>
	 *   <li>If header is a case-insensitive match for Set-Cookie or Set-Cookie2, return <code>null</code>.</li>
	 *   <li>If header is a case-insensitive match for multiple HTTP response headers, return the values of these headers
	 *       as a single concatenated string separated from each other by a U+002C COMMA U+0020 SPACE character pair.<li>
	 *   <li>If header is a case-insensitive match for a single HTTP response header, return the value of that headers.</li>
	 * </ol>
	 * 
	 * @param header The name of the header to be retrieved,
	 * 
	 * @return The header's value.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public String getResponseHeader(String header) throws JsException {
		String value = null;
		if (jsRequest != null && areHeadersReceived()) {
			value = (String) jsRequest.call("getResponseHeader", new Object[] {header});
		}
		return value;
	}
	
	/**
	 * Return all the HTTP headers, excluding headers that are a case-insensitive match for Set-Cookie or Set-Cookie2,
	 * as a single string, with each header line separated by a U+000D CR U+000A LF pair, excluding the status line,
	 * and with each header name and header value separated by a U+003A COLON U+0020 SPACE pair.
	 * 
	 * @return all the HTTP headers, excluding headers Set-Cookie or Set-Cookie2. If the state is UNSENT or OPENED, return <code>null</code>.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public List<HttpHeader> getAllResponseHeaders() throws JsException {
		List<HttpHeader> headers = null;
		if (jsRequest != null && areHeadersReceived()) {
			String headersString = (String) jsRequest.call("getAllResponseHeaders", null);
			if (headersString != null) {
				headers = new ArrayList<HttpHeader>();
				RequestUtil.parseHeadersString(headersString, headers);
			}
		}
		return headers;
	}
	
	/**
	 * Returns the current status of the XMLHttpRequest.
	 * 
	 * @return Returns the current status of the XMLHttpRequest.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public RequestState getState() throws JsException {
		RequestState state = RequestState.UNSENT;
		if (jsRequest != null) {
			int jsState = JsUtil.getIntValue(jsRequest.getMember("readyState"));
			switch (jsState) {
			case 0:
				state = RequestState.UNSENT;
				break;
			case 1:
				state = RequestState.OPENED;
				break;
			case 2:
				state = RequestState.HEADERS_RECEIVED;
				break;
			case 3:
				state = RequestState.LOADING;
				break;
			case 4:
				state = RequestState.DONE;
				break;
			}
		}
		return state;
	}
	
	/**
	 * Returns <code>true</code> if the the request finished and response is ready.
	 * 
	 * @return Returns <code>true</code> if the the request finished and response is ready.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public boolean isReady() throws JsException {
		return getState() == RequestState.DONE;
	}
	
	/**
	 * Returns <code>true</code> if all redirects (if any) have been followed and all 
	 * HTTP headers of the final response have been received.
	 * 
	 * @return Returns <code>true</code> if all redirects (if any) have been followed and all 
	 * HTTP headers of the final response have been received.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public boolean areHeadersReceived() throws JsException {
		return getState().compareTo(RequestState.HEADERS_RECEIVED) >= 0;
	}

	/**
	 * Sets the read timeout. Initially its value is 0.
	 * 
	 * @param millis The time out in milliseconds. When set to a non-zero value 
	 * 				 will cause fetching to terminate after the given time has passed.
	 *  
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public void setReadTimeOut(int millis) throws JsException {
		if (jsRequest != null) {
			jsRequest.setMember("timeout ", millis);
		}
	}
	
	/**
	 * Sets the given HTTP header to the request.
	 * <p>Does not accept a header if it  is a case-insensitive match for one of the following headers:</p>
	 * <ul>
	 *      <li><code>Accept-Charset</code></li>
	 *      <li><code>Accept-Encoding</code></li>
     *      <li><code>Access-Control-Request-Headers</code></li>
     *      <li><code>Access-Control-Request-Method</code></li>
     *      <li><code>Connection</code></li>
     *      <li><code>Content-Length</code></li>
     *      <li><code>Cookie</code></li>
     *      <li><code>Cookie2</code></li>
     *      <li><code>Date</code></li>
     *      <li><code>DNT</code></li>
     *      <li><code>Expect</code></li>
     *      <li><code>Host</code></li>
     *      <li><code>Keep-Alive</code></li>
     *      <li><code>Origin</code></li>
     *      <li><code>Referer</code></li>
     *      <li><code>TE</code></li>
     *      <li><code>Trailer</code></li>
     *      <li><code>Transfer-Encoding</code></li>
     *      <li><code>Upgrade</code></li>
     *      <li><code>User-Agent</code></li>
     *      <li><code>Via</code></li>
     * </ul>
     * <p>or if the start of header is a case-insensitive match for Proxy- or Sec- (including when header is just Proxy- or Sec-).</p>
	 * @see <a href="http://www.w3.org/TR/XMLHttpRequest/#dom-xmlhttprequest-setrequestheader">W3C XMLHttpRequest.setRequestHeader</a>
	 * 
	 * @param header The header to set.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	private void setRequestHeader(HttpHeader header) throws JsException {
		if (header != null && jsRequest != null && getState() == RequestState.OPENED) {
			if (isRequestHeaderAccepted(header)) {
			  jsRequest.call("setRequestHeader", new Object[] {header.getName(), header.getValue()});
			}
		}
	}
	
	/**
	 * Returns <code>true</code> if the given HTTP Header is accepted in the request. 
	 * 
	 * @param header The HTTP header.
	 * 
	 * @return <code>true</code> if the given HTTP Header is accepted in the request.
	 */
	private boolean isRequestHeaderAccepted(HttpHeader header) {
	  boolean accepted = false; 
	  if (header != null) {
	    String headerName = header.getName().toLowerCase();
	    accepted = !NOT_ALLOWED_HEADERS.contains(headerName) && !headerName.startsWith("Proxy-") && !headerName.startsWith("Sec-"); 
	  }
	  return accepted;
	}
	
	/**
	 * Sets the <code>responseType</code> attribute for this request. Initially its value is the empty string.
	 * 
	 * @param responseType The new response type.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	private void setResponseType(ResponseType responseType) throws JsException {
		if (responseType != null && jsRequest != null) {
			jsRequest.setMember("responseType", responseType.getValue());
		}
	}
	
	/**
	 * Returns the response entity body.
	 * 
	 * @return Returns the response entity body.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public Object getResponse() throws JsException {
		Object response = null;
		if (jsRequest != null && isReady()) {
			response = jsRequest.getMember("response");
		}
		return response;
	}
	
	/**
	 * Returns the text response entity body. 
	 * <p>Returns <code>null</code> if responseType is not the empty string or "text".</p>
	 * <p>Returns <code>null</code> if the state is not <code>DONE</code>.</p>
	 * 
	 * @return Returns the text response entity body.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public String getResponseText() throws JsException {
		String responseText = null;
		if (jsRequest != null && isReady()) {
			responseText = (String) jsRequest.getMember("responseText");
		}
		return responseText;
	}
	
	/**
	 * Cancels any network activity.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public void abort() throws JsException {
		if (jsRequest != null) {
			jsRequest.call("abort", null);
		}
	}
	
	/**
	 * Returns the HTTP status code.
	 * 
	 * @return Returns the HTTP status code.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails. 
	 */
	public int getStatusCode() throws JsException {
		int statusCode = 0;
		if (jsRequest != null) {
			statusCode = JsUtil.getIntValue(jsRequest.getMember("status"));
		}
		return statusCode;
	}
	
	/**
	 * Returns the HTTP status text.
	 * 
	 * <ol>
	 * 	  <li>If the state is UNSENT or OPENED, returns the empty string.</li>
	 * 	  <li>If the error flag is set, returns the empty string.</li>
	 *    <li>Otherwise returns the HTTP status text.</li>
	 * </ol>
	 * 
	 * @return the HTTP status text.
	 * 
	 * @throws JsException thrown when JavaScript code execution fails.
	 */
	public String getStatusText() throws JsException {
		String statusText = "";
		if (jsRequest != null) {
			statusText = (String) jsRequest.getMember("statusText ");
		}
		return statusText;
	}
}
