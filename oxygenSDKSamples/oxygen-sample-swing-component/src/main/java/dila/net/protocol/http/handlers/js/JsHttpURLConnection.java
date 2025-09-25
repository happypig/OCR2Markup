package dila.net.protocol.http.handlers.js;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.swing.JApplet;

/**
 * {@link HttpURLConnection} implementation through JavaScript.
 * 
 * @author alin_balasa
 */
public class JsHttpURLConnection extends HttpURLConnection /*implements ExtendedURLConnection*/ {
	
	/**
	 * Output stream.
	 *  
	 * @author alin_balasa
	 */
  private class JsOutputStream extends ByteArrayOutputStream {
    /**
     * Check if the resource is not already closed.
     */
    private boolean open = true;

    /**
     * Send the accumulated bytes and then closes this stream.
     * 
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException {
      if (!open) {
        //Already closed.
        return;
      }
      open = false;
      try {
        request = new JsXmlHttpRequest(parentApplet);
        byte[] bytes = toByteArray();
        request.executeRequest(usePostInsteadPut ? RequestType.POST : RequestType.PUT, url, requestHeaders, ResponseType.ARRAY_BUFFER, bytes);
        JsObject response = (JsObject) request.getResponse();

        if (response != null) {
          byte[] bytesArray = JsUtil.getBytesArray(parentApplet, response);
          if (bytesArray != null) {
            responseInputStream = new ByteArrayInputStream(bytesArray);
          }
        }
      } catch (Throwable t) {
        throw new IOException(t.getMessage(), t);
      } finally {
        //We do not disconnect the connection
        //because the response input stream must be read.
        super.close();
      }
    }    
  }
	
	/**
	 * The JavaScript request.
	 */
	private JsXmlHttpRequest request;
	
	/**
	 * The parent applet.
	 */
	private final JApplet parentApplet;
	
	/**
	 * The list of HTTP headers for the request.
	 */
	private final List<HttpHeader> requestHeaders = new ArrayList<HttpHeader>(0);
	
	/**
	 * The method getOutputStream may result in opening an input stream 
	 * for reading.
	 */
	private InputStream responseInputStream;
	
	/**
	 * <code>true</code> if the POST method should be used instead of PUT when sending data.
	 * The default value is <code>false</code>.
	 */
	private boolean usePostInsteadPut;
	
	/**
	 * Builds a new object representing the {@link HttpURLConnection}.
	 * 
	 * @param url The associated URL.
	 * @param parentApplet The parent Java Applet.
	 */
	public JsHttpURLConnection(URL url, JApplet parentApplet) {
		super(url);
		this.parentApplet = parentApplet;
	}


	/**
	 * Opens a communications link to the resource referenced by the associated URL.
	 */
	@Override
  public void connect() throws IOException {
		connected = true;
	}
	
	/**
	 * Connect method.
	 */
	private void establishConnection() {
		if (request == null) {
			try {
				getInputStream();
			} catch (IOException e) {}
		}
	}
	
	/**
	 * Returns the value of the HTTP header with the given name.
	 *
	 * @param name The name of the HTTP header field.
	 * @return the value of the HTTP header field.
	 */
	@Override
	public String getHeaderField(String name) {
		establishConnection();
		String responseHeader = null; 
		if (request != null) {
			try {
        responseHeader = request.getResponseHeader(name);
      } catch (JsException e) {
        e.printStackTrace();
      }
		}
		return responseHeader;
	}
	
	/**
	 * Returns the value of the HTTP header with the given index.
	 *
	 * @param index The index of the HTTP header.
	 * 
	 * @return the value of the HTTP header.
	 */
	@Override
	public String getHeaderField(int index) {
		establishConnection();

		String value = null;
		try {
      if (request != null){
      	if (index == 0){      
      		value = request.getStatusText();
      	} else {
      		List<HttpHeader> headers = request.getAllResponseHeaders();
      		if (headers != null && (index - 1) < headers.size()) {
      			value = headers.get(index - 1).getValue();
      		}
      	}
      }
    } catch (JsException e) {
      e.printStackTrace();
    }
		return value;
	}
	
	/**
	 * Returns the name of the HTTP header with the given index.
	 *
	 * @param index The index of the header field.
	 * @return Returns the name of the HTTP header.
	 */
	@Override
	public String getHeaderFieldKey(int index) {
		establishConnection();
		
		String name = null;
		if (request != null) {
			if (index == 0) {
				name = "Status";
			} else { 
				try {
          List<HttpHeader> headers = request.getAllResponseHeaders();
          if (headers != null && (index - 1) < headers.size()) {
          name = headers.get(index - 1).getName();
          }
        } catch (JsException e) {
          e.printStackTrace();
        }
			}
		}
		return name;
	}  
	
	/**
	 * Opens an input stream from the resource. 
	 * 
	 * @return The inputStream resulted from the last operation of getInputStream or 
	 * getOutputStream. Yes, get outputStream is returning the response that can be read.
	 * The disposals of streams is made on disconnect.
	 * 
	 * @exception IOException
	 *            If the stream cannot be created.
	 */
	@Override
	public InputStream getInputStream() throws IOException {

    // It was already opened by a getOutputStream?
    if (responseInputStream == null) {
      try {
        if (!connected) {
          connect();
        }

        //EXM-22742 Set accept header
        setRequestProperty("Accept","application/xml,*/*");

        // This might not be accepted by the JS object

        //EXM-10640 If server can gzip the response, 
        //the better because we can decode it
        //and the file will be opened faster.
        setRequestProperty("Accept-Encoding", "gzip");

        // create a GET method that reads a file over HTTP

        request = new JsXmlHttpRequest(parentApplet);
        request.executeRequest(RequestType.GET, url, requestHeaders, ResponseType.ARRAY_BUFFER, null);
        JsObject response = (JsObject) request.getResponse();
        if (response != null) {
          byte[] bytesArray = JsUtil.getBytesArray(parentApplet, response);
          if (bytesArray != null) {
            responseInputStream = new ByteArrayInputStream(bytesArray);
          }
        }

        String encoding = JsHttpURLConnection.super.getContentEncoding();
        if (encoding != null && encoding.toLowerCase().equals("gzip")) {
          //GZipped, unzip it as it is read.
          responseInputStream = new GZIPInputStream(responseInputStream);
        }

      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
		return responseInputStream;
	}

	/**
	 * We resolve here the input stream to a GZIP input stream if necessary so
	 * we do not show on the outside the real content encoding if gzip.
	 * 
	 * @see java.net.URLConnection#getContentEncoding()
	 */
	@Override
	public String getContentEncoding() {
		String encoding = super.getContentEncoding();
		if (encoding != null && encoding.toLowerCase().equals("gzip")) {
			encoding = null;
		}
		return encoding;
	}
	
	/**
	 * Opens an output stream to the resource.
	 *
	 * @return a new OutputStream.
	 * 
	 * @exception IOException If the stream cannot be created.
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {    
		if (!connected) {
			connect();
		}

		// Accumulate the send data in a byte array output stream and return it on close.
		JsOutputStream wos = new JsOutputStream();
		return wos;    
	}

	/**
	 * Disconnects from the server. Closes the HTTP streams.
	 */
	@Override
	public void disconnect() {
		if (request != null){
			try {
        request.abort();
      } catch (JsException e) {
        e.printStackTrace();
      }
			request = null;
		}
	}

	/**
	 * Indicates if the connection is going through a Proxy server.
	 * 
	 * @return a boolean indicating if the connection is  using a Proxy server.
	 */
	@Override
	public boolean usingProxy() {
		return false;
	}

	/**
	 * Sets the method for the URL request.
	 * If the method POST is used, then the following getOutputStream calls
	 * will use POST instead of PUT.
	 * 
	 * @param method The method name can be PUT or POST.
	 */
	@Override
	public void setRequestMethod(String method) throws ProtocolException {    
		super.setRequestMethod(method);
		usePostInsteadPut = "POST".equals(method);
	}
	
	/**
	 * Sets a request property.
	 * 
	 * @see java.net.URLConnection#setRequestProperty(java.lang.String, java.lang.String)
	 */
	@Override
	public void setRequestProperty(String key, String value) {
		requestHeaders.add(new HttpHeader(key, value));
	}
	
	/**
	 * Returns the last HTTP request response code. 
	 * 
	 * @return HTTP response codes, like 200, 500, etc..
	 */
	@Override
	public int getResponseCode() throws IOException {
		int responseCode = -1;    

		if (request == null){
			getInputStream();
		}    

		if (request != null){ 
			try {
        responseCode = request.getStatusCode();
      } catch (JsException e) {
        e.printStackTrace();
      }
		}

		return responseCode;
	}
	
	/**
	 * Returns the last HTTP response reason (no code) first line.
	 */
	@Override
	public String getResponseMessage() throws IOException {
		String responseMessage = null;

		if (request == null){
			getInputStream();
		}

		if (request != null){
			try {
        responseMessage = request.getStatusText();
      } catch (JsException e) {
        e.printStackTrace();
      }
		}

		return responseMessage;
	}
	
	/**
	 * Make sure the streams are closed before being GC-ed.
	 *
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		disconnect();
	}
	
	/**
	 * This is a hack to change the method for the output from PUT to POST.
	 * XMLRPC package needs this.
	 */
	@Override
	public void setDoOutput(boolean dooutput) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);
		new Exception().printStackTrace(ps);

		try {
			String str = bos.toString("UTF8").toLowerCase();
			// Called from XML RPC.

			// Use post in case of CUPS unix printing 
			if (str.indexOf("org.apache.xmlrpc") != -1 ||
					str.indexOf("printservice") != -1 && str.indexOf("sun.print") != -1) {
				usePostInsteadPut = true;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		super.setDoOutput(dooutput);
	}
	
	/**
	 * @see java.net.URLConnection#addRequestProperty(java.lang.String, java.lang.String)
	 */
	@Override
	public void addRequestProperty(String key, String value) {
		if (key != null && value != null) {
			requestHeaders.add(new HttpHeader(key, value));
		}
	}
	
	/**
	 * @see java.net.URLConnection#getHeaderFields()
	 */
	@Override
	public Map<String, List<String>> getHeaderFields() {
		establishConnection();
		Map<String, List<String>> toReturn = new HashMap<String, List<String>>();
		if (request != null) {
			try {
        List<HttpHeader> headers = request.getAllResponseHeaders();
        if (headers != null){
        	for (HttpHeader header : headers) {
        		List<String> valuesList = new ArrayList<String>(1);
        		String name = header.getName();
        		String value = header.getValue();
        		if (value != null) {
        			valuesList.add(value);
        		}
        		toReturn.put(name, valuesList);
        	}
        }
      } catch (JsException e) {
        e.printStackTrace();
      }
		}
		return toReturn;
	}
}
