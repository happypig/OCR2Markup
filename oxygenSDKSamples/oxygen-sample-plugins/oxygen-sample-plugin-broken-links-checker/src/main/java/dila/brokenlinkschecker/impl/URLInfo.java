package dila.brokenlinkschecker.impl;

public class URLInfo {
	
	/**
	 * The value of the URL.
	 */
	String value;
	
	/**
	 * The parent location.
	 */
	Location parentLocation;
	
	/**
	 * The URL depth.
	 */
	int depth;

	/**
	 * Constructor.
	 * @param value The value of the URL.
	 * @param parentLocation The parent location.
	 * @param depth The URL depth.
	 */
	public URLInfo(String value, Location parentLocation, int depth) {
		super();
		this.value = value;
		this.parentLocation = parentLocation;
		this.depth = depth;
	}

	/**
	 * Get URL value.
	 * @return the value of the URL.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set a value for the URL.
	 * @param value the new value.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Get parent location.
	 * @return the parent location.
	 */
	public Location getParentLocation() {
		return parentLocation;
	}

	/**
	 * Set a parent location for the URL. 
	 * @param parentLocation
	 */
	public void setParentLocation(Location parentLocation) {
		this.parentLocation = parentLocation;
	}

	/**
	 * Get depth.
	 * @return the depth.
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Set depth.
	 * @param depth the depth.
	 */
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	

}
