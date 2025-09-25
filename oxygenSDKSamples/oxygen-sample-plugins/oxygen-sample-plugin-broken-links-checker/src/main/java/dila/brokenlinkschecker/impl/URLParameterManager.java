package dila.brokenlinkschecker.impl;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manages the parameters of a URL.
 * 
 * @author sorin_carbunaru
 * 
 */
public class URLParameterManager {

	/**
	 * Add parameter.
	 * 
	 * @param url
	 *            the URL to which a parameter is to be added.
	 * @param paramName
	 *            the name of the parameter to be added.
	 * @param paramValue
	 *            the value of the parameter to be added.
	 * @return the URL obtained after adding the parameter.
	 * @throws MalformedURLException
	 */
	public static URL addParameter(URL url, String paramName, String paramValue)
			throws MalformedURLException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(url.toString());
		if (stringBuilder.indexOf("?") != -1) {
			if (stringBuilder.indexOf("?") + 1 <= stringBuilder.length() - 1
					&& stringBuilder.charAt(stringBuilder.indexOf("?") + 1) == '#'
					|| stringBuilder.indexOf("?") + 1 == stringBuilder.length()) {
				stringBuilder.insert(stringBuilder.indexOf("?") + 1, paramName
						+ "=" + paramValue);
			} else {
				stringBuilder.insert(stringBuilder.indexOf("?") + 1, paramName
						+ "=" + paramValue + "&");
			}
		} else {
			if (stringBuilder.indexOf("#") != -1) {
				stringBuilder.insert(stringBuilder.indexOf("#") - 1, "?"
						+ paramName + "=" + paramValue);
			} else {
				stringBuilder.insert(stringBuilder.length(), "?" + paramName
						+ "=" + paramValue);
			}
		}
		return new URL(stringBuilder.toString());
	}

	/**
	 * Remove parameter.
	 * 
	 * @param url
	 *            the URL from which a parameter is to be removed.
	 * @param parameterName
	 *            the name of the parameter to be removed.
	 * @return the URL obtained after removing the parameter or
	 *         <code>null</code> if the parameter does not exist in the query.
	 * @throws MalformedURLException
	 */
	public static URL removeParameter(URL url, String parameterName)
			throws MalformedURLException {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(url.toString());
		String paramNameValuePairs[] = url.getQuery().split("&");
		for (String pair : paramNameValuePairs) {
			String nameAndValue[] = pair.split("=");
			if (nameAndValue[0].equals(parameterName)) {
				if (stringBuilder.charAt(stringBuilder.indexOf(nameAndValue[0]) - 1) == '&') {
					stringBuilder.replace(
							stringBuilder.indexOf(nameAndValue[0]) - 1,
							stringBuilder.indexOf(nameAndValue[0])
									+ nameAndValue[0].length()
									+ nameAndValue[1].length() + 1, "");
				} else if (stringBuilder.indexOf("&", stringBuilder.indexOf(nameAndValue[0])) != -1) {
					stringBuilder.replace(
							stringBuilder.indexOf(nameAndValue[0]),
							stringBuilder.indexOf(nameAndValue[0])
									+ nameAndValue[0].length()
							 		+ nameAndValue[1].length() + 2, "");
				} else {
					stringBuilder.replace(
							stringBuilder.indexOf(nameAndValue[0]),
							stringBuilder.indexOf(nameAndValue[0])
									+ nameAndValue[0].length()
									+ nameAndValue[1].length() + 1, "");
				}
				return new URL(stringBuilder.toString());
			}
		}
		return null;
	}

	/**
	 * Get parameter.
	 * 
	 * @param url
	 *            the URL from which the value of a parameter is to be
	 *            retrieved.
	 * @param parameterName
	 *            the parameter name whose value is to be retrieved.
	 * @return the value of the parameter given as the second argument of the
	 *         method, if it exists; <code>null</code> otherwise.
	 */
	public static String getParameter(URL url, String parameterName) {
		String paramNameValuePairs[] = url.getQuery().split("&");
		for (String pair : paramNameValuePairs) {
			String nameAndValue[] = pair.split("=");
			if (nameAndValue[0].equals(parameterName)) {
				return nameAndValue[1];
			}
		}
		return null;
	}

}
