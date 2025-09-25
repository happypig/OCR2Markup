package dila.brokenlinkschecker.ui;

import dila.brokenlinkschecker.impl.LinkProblem;

/**
 * Interface used to separate UI from implementation.
 * 
 * @author sorin_carbunaru
 * 
 */
public interface UIUpdater {

	/**
	 * Report a problem when found.
	 * 
	 * @param problem
	 *            a problem related to an URL.
	 * @param row
	 *            the index of the UI table row corresponding to the start page
	 */
	void reportProblem(LinkProblem problem, int row);

	/**
	 * Tasks to be done after the search starts.
	 */
	void doAfterSearchStarted();

	/**
	 * Tasks to be done after search ended.
	 * 
	 * @param isStoppedByUser
	 *            specifies if the search has been stooped by user (when true)
	 *            or the search has been performed until the end (when false)
	 */
	void doAfterSearchEnded(boolean isStoppedByUser);

	/**
	 * Stop search.
	 */
	boolean shouldStopSearch();

	/**
	 * Method specifying the currently processed resource.
	 * 
	 * @param resource
	 *            the currently processed resource.
	 */
	void parsingResource(String resource);

	/**
	 * Display error.
	 * 
	 * @param message
	 *            the message of the error.
	 */
	void displayError(String message);

	/**
	 * Set row status in table.
	 * 
	 * @param status
	 *            the status.
	 * @param row
	 *            the UI table row index.
	 */
	public void setStatus(String status, int row);

	/**
	 * Tasks to be done when search ended for a certain row identified by its
	 * index.
	 * 
	 * @param row
	 *            the index of the row for which the search has ended.
	 */
	public void doWhenSearchEndedForRow(int row);
}
