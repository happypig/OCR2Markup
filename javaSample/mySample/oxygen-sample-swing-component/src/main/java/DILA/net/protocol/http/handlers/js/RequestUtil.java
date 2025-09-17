package DILA.net.protocol.http.handlers.js;

import java.util.List;
import java.util.StringTokenizer;


/**
 * Utility methods for dealing with an {@link JsXmlHttpRequest}.
 * 
 * @author alin_balasa
 */
public class RequestUtil {
	
	/**
	 * Header line separator: a U+000D CR U+000A LF pair.
	 */
	private static final String HEADERS_SEPARATOR = "\r\n";
	
	/**
	 * Header name and value separator: a U+003A COLON U+0020 SPACE pair
	 */
	private static final String HEADER_NAME_VALUE_SEPARATOR = ": ";

	/**
	 * Parses the headers string.
	 * 
	 * @param headersString All the HTTP headers, as a single string, with each header line separated by a U+000D CR U+000A LF pair,
	 * 						excluding the status line, and with each header name and header value separated by a U+003A COLON U+0020 SPACE pair.
	 * @param headers The list where to add the headers.
	 */
	public static void parseHeadersString(String headersString, List<HttpHeader> headers) {
		if (headersString != null && headers != null) {
			StringTokenizer headersTokenizer = new StringTokenizer(headersString, HEADERS_SEPARATOR, false);
			while (headersTokenizer.hasMoreTokens()) {
				String currentHeader = headersTokenizer.nextToken();
				int sepIndex = currentHeader.indexOf(HEADER_NAME_VALUE_SEPARATOR);
				if (sepIndex != -1) {
					String headerName = currentHeader.substring(0, sepIndex);
					String headerValue = currentHeader.substring(sepIndex + HEADER_NAME_VALUE_SEPARATOR.length());
					headers.add(new HttpHeader(headerName, headerValue));
				}
			}
		}
	}
}
