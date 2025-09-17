package DILA.brokenlinkschecker.impl;

import java.util.Collection;
import java.util.List;

/**
 * Class used to encapsulate the information collected from the UI.
 * 
 * @author sorin_carbunaru
 * 
 */
public class CollectedInfo {

	/**
	 * The list of URLs received from the UI.
	 */
	private List<String> urls;

	/**
	 * The depth of the search.
	 */
	private int searchDepth;

	/**
	 * Constructor.
	 * 
	 * @param urls
	 *            the list of URLs.
	 * @param searchDepth
	 *            the search depth.
	 */
	public CollectedInfo(List<String> urls, int searchDepth) {
		this.urls = urls;
		this.searchDepth = searchDepth;
	}

	/**
	 * Get the list of URLs.
	 * 
	 * @return the list of URLs.
	 */
	public List<String> getUrls() {
		return urls;
	}

	/**
	 * Set the list of URLs.
	 * 
	 * @param urls
	 *            the list of URLs.
	 */
	public void setUrls(List<String> urls) {
		this.urls = urls;
	}

	/**
	 * Get the search depth.
	 * 
	 * @return the search depth.
	 */
	public int getSearchDepth() {
		return searchDepth;
	}

	/**
	 * Set the search depth.
	 * 
	 * @param searchDepth
	 *            the search depth.
	 */
	public void setSearchDepth(int searchDepth) {
		this.searchDepth = searchDepth;
	}

	/**
	 * Add an URL to the list.
	 * 
	 * @param url
	 *            the URL to be added
	 */
	public void add(String url) {
		urls.add(url);
	}

	/**
	 * Remove and URL from the list.
	 * 
	 * @param url
	 *            the URL to be removed.
	 */
	public void remove(String url) {
		urls.remove(url);
	}

	/**
	 * Add a collection of URLs to the list.
	 * 
	 * @param urlsParam
	 *            the URLs to be added.
	 */
	public void addAll(Collection<String> urlsParam) {
		urls.addAll(urlsParam);
	}

	/**
	 * Remove a collection of URLs from the list.
	 * 
	 * @param urlsParam
	 *            the URLs to be removed.
	 */
	public void removeAll(Collection<String> urlsParam) {
		urls.removeAll(urlsParam);
	}

	/**
	 * Add an URL at a given index.
	 * 
	 * @param index
	 *            the index where the URL is to be added.
	 * @param url
	 *            the URL to be added.
	 */
	public void add(int index, String url) {
		urls.add(index, url);
	}

	/**
	 * Remove an URL from a given index.
	 * 
	 * @param index
	 *            the index where the URL is to be removed from.
	 */
	public void remove(int index) {
		urls.remove(index);
	}

}
