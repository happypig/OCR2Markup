package DILA.brokenlinkschecker.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cyberneko.html.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import DILA.brokenlinkschecker.ui.CheckerPanel;
import DILA.brokenlinkschecker.ui.UIUpdater;

/**
 * Class that contains the implementation of the broken links finding.
 * 
 * @author sorin_carbunaru
 * 
 */
public class BrokenLinksFinder {
	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger(BrokenLinksFinder.class.getName());

	/**
	 * Find broken links.
	 * 
	 * @param collectedInfo
	 *            the collected information containing the start pages and the
	 *            search depth.
	 * @param updater
	 *            a Java interface used for UI - implementation communication.
	 */
	public void findBrokenLinks(CollectedInfo collectedInfo, UIUpdater updater) {

		boolean isStoppedByUser = false;
		updater.doAfterSearchStarted();

		// extract the collected information
		List<String> urls = collectedInfo.getUrls();
		int searchDepth = collectedInfo.getSearchDepth();

		for (int i = 0; i < urls.size(); i++) {
			if (isStoppedByUser) {
				break;
			}
			updater.setStatus(CheckerPanel.SEARCH_IS_PERFORMING_STATUS, i);
			isStoppedByUser = parseUrls(i, urls.get(i), new URLInfo(
					urls.get(i), new Location(urls.get(i), 0, 0), 1), updater,
					searchDepth);
			updater.doWhenSearchEndedForRow(i);
		}
		updater.doAfterSearchEnded(isStoppedByUser);
	}

	/**
	 * Parse URLs.
	 * 
	 * @param tableRow
	 *            the row from UI table corresponding to the start page for
	 *            which the current search is being performed
	 * @param startPage
	 *            the start page URL
	 * @param url
	 *            the current URL
	 * @param updater
	 *            the interface used for implementation - UI interaction
	 * @param maximumSearchDepth
	 *            the maximum search depth
	 * @return true if the user has requested to stop the search, false
	 *         otherwise
	 */
	public boolean parseUrls(int tableRow, String startPage, URLInfo url,
			UIUpdater updater, int maximumSearchDepth) {
		Location auxLoc = null;
		Location parentLocation = null;

		// set and queue used for breadth-first search
		Set<String> alreadyVisitedResources = new HashSet<String>();
		Deque<URLInfo> resourcesQueue = new ArrayDeque<URLInfo>();

		alreadyVisitedResources.add(url.getValue());
		resourcesQueue.add(url);

		while (!resourcesQueue.isEmpty()) {
			// stop if requested by user
			if (updater.shouldStopSearch()) {
				return true;
			}

			url = resourcesQueue.remove();

			// System.err.println("Current page: " + url.getValue() +
			// ", level: " + url.getLevel());

			try {
				SAXParser parser = new SAXParser();
				SAXParserHandler handler = new SAXParserHandler();
				parser.setContentHandler(handler);

				// Parse HTML
				InputSource inputSource = new InputSource(url.getValue());
				updater.parsingResource(url.getValue());
				parser.parse(inputSource);

				// Get parsed URLs
				List<Location> locations = handler.getLocations();

				// Get the list of IDs of all the HTML elements
				List<String> parsedIdsAndNames = handler.getIdsAndNames();

				boolean idListContainsAnchor = false;
				
				// if the current URL is an anchor, check if it has a corresponding element name or id
				if (url.getValue().contains("#")) {
					for (String id : parsedIdsAndNames) {
						if (url.getValue()
								.substring(url.getValue().indexOf("#") + 1)
								.equals(id)) {
							idListContainsAnchor = true;
							break;
						}
					}
					if (!idListContainsAnchor) {
						updater.reportProblem(
								new LinkProblem(startPage, url
										.getParentLocation().getUrl(), url
										.getParentLocation().getLine(), url
										.getParentLocation().getColumn(), url
										.getValue(), "Broken anchor"), tableRow);
					}
				} else if (url.getDepth() <= maximumSearchDepth
						|| maximumSearchDepth == -2) {
					// for (Location loc : locations) {
					// System.err.println("    Links on current page: "
					// + loc.getUrl() /*+ " " + loc.getLine() + " "
					// + loc.getColumn()*/);
					// }

					for (Location loc : locations) {

						// stop if requested by user
						if (updater.shouldStopSearch()) {
							return true;
						}

						try {
							// if the value of the URL is not absolute, then
							// make it absolute
							auxLoc = loc;
							URL urlParamStringToUrl = new URL(url.getValue());
							if (new URI(loc.getUrl()).isAbsolute() == false) {
								loc.setUrl(new URL(urlParamStringToUrl, loc
										.getUrl()).toString());
							}

							parentLocation = new Location(url.getValue(),
									loc.getLine(), loc.getColumn());

							// we don't want to process not visited pages
							// from
							// outside the domain of the user provided URL
							// or
							// outside the search depth
							String oldDomain = new URL(url.getValue())
									.getHost();
							String newDomain = new URL(loc.getUrl()).getHost();
							if (oldDomain.equals(newDomain)
									&& !alreadyVisitedResources.contains(loc
											.getUrl())
									&& (url.getDepth() <= maximumSearchDepth || maximumSearchDepth == -2)) {
								alreadyVisitedResources.add(loc.getUrl());
								resourcesQueue.addLast(new URLInfo(
										loc.getUrl(), new Location(url
												.getValue(), loc.getLine(), loc
												.getColumn()),
										url.getDepth() + 1));
							}
						} catch (URISyntaxException ex) {
							updater.reportProblem(
									new LinkProblem(startPage, url.getValue(),
											auxLoc.getLine(), auxLoc
													.getColumn(), auxLoc
													.getUrl(),
											"Invalid URL found!"), tableRow);
							logger.debug(ex);
						}
					}
				}
			} catch (MalformedURLException ex) {
				updater.reportProblem(new LinkProblem(startPage,
						url.getValue(), parentLocation.getLine(),
						parentLocation.getColumn(), auxLoc.getUrl(),
						"Invalid URL found!"), tableRow);
				logger.debug(ex);
			} catch (IOException ex) {
				updater.reportProblem(
						new LinkProblem(startPage, url.getParentLocation()
								.getUrl(), url.getParentLocation().getLine(),
								url.getParentLocation().getColumn(), url
										.getValue(), "Resource not found"),
						tableRow);
				logger.debug(ex);
			} catch (SAXException ex) {
				updater.displayError(CheckerPanel.INTERNAL_ERROR_MESSAGE);
				logger.debug(ex);
			}
		}
		return false;
	}
}
