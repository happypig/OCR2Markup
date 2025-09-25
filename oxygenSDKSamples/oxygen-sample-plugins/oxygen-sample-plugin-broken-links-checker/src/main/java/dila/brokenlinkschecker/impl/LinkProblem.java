package dila.brokenlinkschecker.impl;

/**
 * Class used to encapsulate the elements of a problem found for a link.
 * 
 * @author sorin_carbunaru
 * 
 */
public class LinkProblem {

	/**
	 * The user-provided URL starting from which a problem has been found.
	 */
	String startPageUrl;

	/**
	 * The parent page.
	 */
	String parent;

	/**
	 * The line of a document where the problematic link has been found.
	 */
	int line;

	/**
	 * The column of a document where the problematic link has been found.
	 */
	int column;

	/**
	 * The value of the problematic link.
	 */
	String problematicLink;

	/**
	 * The problem description.
	 */
	String problemDescription;

	/**
	 * No args constructor.
	 */
	public LinkProblem() {

	}

	/**
	 * Constructor with parameters.
	 * 
	 * @param startPageUrl
	 *            The user-provided URL starting from which a problem has been
	 *            found.
	 * @param parent
	 *            The parent page.
	 * @param line
	 *            The line of a document where the problematic link has been
	 *            found.
	 * @param col
	 *            The column of a document where the problematic link has been
	 *            found.
	 * @param problematicLink
	 *            The value of the problematic link.
	 * @param problemDescription
	 *            The problem description.
	 */
	public LinkProblem(String startPageUrl, String parent, int line, int col,
			String problematicLink, String problemDescription) {
		super();
		this.startPageUrl = startPageUrl;
		this.parent = parent;
		this.line = line;
		this.column = col;
		this.problematicLink = problematicLink;
		this.problemDescription = problemDescription;
	}

	/**
	 * Get the user-provided URL starting from which a problem has been found.
	 * 
	 * @return the URL
	 */
	public String getStartPageUrl() {
		return startPageUrl;
	}

	/**
	 * Set a value for the user-provided URL starting from which a problem has
	 * been found.
	 * 
	 * @param url
	 *            the value to be set.
	 */
	public void setStartPageUrl(String startPageUrl) {
		this.startPageUrl = startPageUrl;
	}

	/**
	 * Get the line where a problem has been found.
	 * 
	 * @return the line where a problem has been found.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Set a value for the line where a problem has been found.
	 * 
	 * @param line
	 *            the value to be set.
	 */
	public void setLine(int line) {
		this.line = line;
	}

	/**
	 * Get the column where a problem has been found.
	 * 
	 * @return the column where a problem has been found.
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Set a value for the column where a problem has been found.
	 * 
	 * @param col
	 */
	public void setColumn(int col) {
		this.column = col;
	}

	/**
	 * Get the value of the problematic link.
	 * 
	 * @return the value of the problematic link.
	 */
	public String getProblematicLink() {
		return problematicLink;
	}

	/**
	 * Set a value for the problematic link.
	 * 
	 * @param problematicLink
	 *            the value to be set.
	 */
	public void setProblematicLink(String problematicLink) {
		this.problematicLink = problematicLink;
	}

	/**
	 * Get problem description.
	 * 
	 * @return the problem description.
	 */
	public String getProblemDescription() {
		return problemDescription;
	}

	/**
	 * Set a value for the problem description.
	 * 
	 * @param problemDescription
	 *            the value to be set.
	 */
	public void setProblemDescription(String problemDescription) {
		this.problemDescription = problemDescription;
	}

	/**
	 * Get parent.
	 * 
	 * @return parent.
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * Set parent.
	 * 
	 * @param parent
	 *            value to be set.
	 */
	public void setParentPage(String parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		return problematicLink + ": " + problemDescription;
	}
}
