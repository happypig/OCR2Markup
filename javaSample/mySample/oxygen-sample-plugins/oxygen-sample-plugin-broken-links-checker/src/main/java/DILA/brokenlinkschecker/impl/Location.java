package DILA.brokenlinkschecker.impl;

/**
 * Location class. The location is given by an URL, a line and a column.
 * 
 * @author sorin_carbunaru
 * 
 */
public class Location {

	/**
	 * URL.
	 */
	String url;

	/**
	 * Line in document.
	 */
	int line;

	/**
	 * Column in document.
	 */
	int column;

	/**
	 * Constructor.
	 * 
	 * @param url
	 *            the URL.
	 * @param line
	 *            the line.
	 * @param column
	 *            the column.
	 */
	public Location(String url, int line, int column) {
		super();
		this.url = url;
		this.line = line;
		this.column = column;
	}

	/**
	 * Get line.
	 * 
	 * @return the line.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Set line.
	 * 
	 * @param line
	 *            the value to be set.
	 */
	public void setLine(int line) {
		this.line = line;
	}

	/**
	 * Get column.
	 * 
	 * @return the column.
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Set column.
	 * 
	 * @param column
	 *            the value to be set.
	 */
	public void setColumn(int column) {
		this.column = column;
	}

	/**
	 * Get URL.
	 * 
	 * @return url.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set URL.
	 * 
	 * @param url
	 *            the value to be set.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

}
