package DILA.brokenlinkschecker;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import junit.framework.TestCase;

import DILA.brokenlinkschecker.impl.BrokenLinksFinder;
import DILA.brokenlinkschecker.impl.CollectedInfo;
import DILA.brokenlinkschecker.impl.LinkProblem;
import DILA.brokenlinkschecker.ui.UIUpdater;

public class BrokenLinksFinderTest extends TestCase {

	/**
	 * List of reported problems
	 */
	private List<LinkProblem> problems = new ArrayList<LinkProblem>();

	/**
	 * Parsed resources
	 */
	private StringBuilder parsedResources = new StringBuilder();

	/**
	 * Test find broken links when having a single &lt;img> element with its
	 * "src" attribute ponting to a not existing resource.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFindBrokenLinksWithNotExistingImg() throws Exception {
		int searchDepth = 1;
		problems.clear();
		parsedResources.setLength(0);
		BrokenLinksFinder finder = new BrokenLinksFinder();
		finder.findBrokenLinks(
				new CollectedInfo(Arrays.asList(new String[] { new File(
						"src/test/resources/caz1.html").toURI().toURL().toString() }),
						searchDepth), new UIUpdater() {
					@Override
					public void reportProblem(LinkProblem problem1, int row) {
						problems.add(problem1);
					}

					@Override
					public boolean shouldStopSearch() {
						return false;
					}

					@Override
					public void doAfterSearchStarted() {
					}

					@Override
					public void doAfterSearchEnded(boolean isStoppedByUser) {
					}

					@Override
					public void parsingResource(String resource) {
						parsedResources.append(resource).append("\n");
					}

					@Override
					public void displayError(String message) {
					}

					@Override
					public void setStatus(String status, int row) {
					}

					@Override
					public void doWhenSearchEndedForRow(int row) {
					}
				});

		String urlBase = new File("src/test/resources/").toURI().toASCIIString();
		assertEquals(urlBase + "caz1.html\n" + urlBase + "notExisting.jpg\n",
				parsedResources.toString());
		assertEquals(2, problems.size());
		assertEquals("%&##*: Invalid URL found!" + urlBase
				+ "notExisting.jpg: Resource not found" + "", problems.get(0)
				.toString() + problems.get(1).toString());
	}

	/**
	 * Test broken links when having 2 anchors pointing inside the same
	 * document, one to an id and another one to a name.
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void testFindBrokenLinksWithAnchors() throws MalformedURLException {
		int searchDepth = 1;
		problems.clear();
		parsedResources.setLength(0);
		BrokenLinksFinder finder = new BrokenLinksFinder();
		finder.findBrokenLinks(
				new CollectedInfo(Arrays.asList(new String[] { new File(
						"src/test/resources/caz2.html").toURI().toURL().toString() }),
						searchDepth), new UIUpdater() {
					@Override
					public void reportProblem(LinkProblem problem1, int row) {
						problems.add(problem1);
					}

					@Override
					public boolean shouldStopSearch() {
						return false;
					}

					@Override
					public void doAfterSearchStarted() {
					}

					@Override
					public void doAfterSearchEnded(boolean isStoppedByUser) {
					}

					@Override
					public void parsingResource(String resource) {
						parsedResources.append(resource).append("\n");
					}

					@Override
					public void displayError(String message) {
					}

					@Override
					public void setStatus(String status, int row) {
					}

					@Override
					public void doWhenSearchEndedForRow(int row) {
					}
				});

		String urlBase = new File("src/test/resources/").toURI().toASCIIString();
		assertEquals(urlBase + "caz2.html\n" + urlBase + "caz2.html#pid\n"
				+ urlBase + "caz2.html#aID\n", parsedResources.toString());
		assertEquals(0, problems.size());
	}

	/**
	 * Testing what happens when having a reference to a existing ZIP and JPG
	 * files.
	 * 
	 * @throws MalformedURLException
	 * 
	 */
	@Test
	public void testFindBrokenLinksWithExistingBinaryFiles()
			throws MalformedURLException {
		int searchDepth = 1;
		problems.clear();
		parsedResources.setLength(0);
		BrokenLinksFinder finder = new BrokenLinksFinder();
		finder.findBrokenLinks(
				new CollectedInfo(Arrays.asList(new String[] { new File(
						"src/test/resources/caz3.xhtml").toURI().toURL().toString() }),
						searchDepth), new UIUpdater() {
					@Override
					public void reportProblem(LinkProblem problem1, int row) {
						problems.add(problem1);
					}

					@Override
					public boolean shouldStopSearch() {
						return false;
					}

					@Override
					public void doAfterSearchStarted() {
					}

					@Override
					public void doAfterSearchEnded(boolean isStoppedByUser) {
					}

					@Override
					public void parsingResource(String resource) {
						parsedResources.append(resource).append("\n");
					}

					@Override
					public void displayError(String message) {
					}

					@Override
					public void setStatus(String status, int row) {
					}

					@Override
					public void doWhenSearchEndedForRow(int row) {
					}
				});

		String urlBase = new File("src/test/resources/").toURI().toASCIIString();
		assertEquals(urlBase + "caz3.xhtml\n" + urlBase + "test.zip\n"
				+ urlBase + "awesome.jpg\n", parsedResources.toString());
		assertEquals(0, problems.size());
	}

	/**
	 * Test. We have 2 &lt;form> elements, one with a valid "action" and the
	 * other one with an broken "action"
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void testFindBrokenLinksWithFormAction()
			throws MalformedURLException {
		int searchDepth = 1;
		problems.clear();
		parsedResources.setLength(0);
		BrokenLinksFinder finder = new BrokenLinksFinder();
		finder.findBrokenLinks(
				new CollectedInfo(Arrays.asList(new String[] { new File(
						"src/test/resources/caz4.html").toURI().toURL().toString() }),
						searchDepth), new UIUpdater() {
					@Override
					public void reportProblem(LinkProblem problem1, int row) {
						problems.add(problem1);
					}

					@Override
					public boolean shouldStopSearch() {
						return false;
					}

					@Override
					public void doAfterSearchStarted() {
					}

					@Override
					public void doAfterSearchEnded(boolean isStoppedByUser) {
					}

					@Override
					public void parsingResource(String resource) {
						parsedResources.append(resource).append("\n");
					}

					@Override
					public void displayError(String message) {
					}

					@Override
					public void setStatus(String status, int row) {
					}

					@Override
					public void doWhenSearchEndedForRow(int row) {
					}
				});

		String urlBase = new File("src/test/resources/").toURI().toASCIIString();
		assertEquals(urlBase + "caz4.html\n" + urlBase + "notExistingPhp.php\n"
				+ urlBase + "existingPhp.php\n", parsedResources.toString());
		assertEquals(1, problems.size());
		assertEquals(urlBase + "notExistingPhp.php: Resource not found",
				problems.get(0).toString());
	}

	/**
	 * Test for head elements.
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void testFindBrokenLinksWithHeadElements()
			throws MalformedURLException {
		int searchDepth = 1;
		problems.clear();
		parsedResources.setLength(0);
		BrokenLinksFinder finder = new BrokenLinksFinder();
		finder.findBrokenLinks(
				new CollectedInfo(Arrays.asList(new String[] { new File(
						"src/test/resources/caz5.html").toURI().toURL().toString() }),
						searchDepth), new UIUpdater() {
					@Override
					public void reportProblem(LinkProblem problem1, int row) {
						problems.add(problem1);
					}

					@Override
					public boolean shouldStopSearch() {
						return false;
					}

					@Override
					public void doAfterSearchStarted() {
					}

					@Override
					public void doAfterSearchEnded(boolean isStoppedByUser) {
					}

					@Override
					public void parsingResource(String resource) {
						parsedResources.append(resource).append("\n");
					}

					@Override
					public void displayError(String message) {
					}

					@Override
					public void setStatus(String status, int row) {
					}

					@Override
					public void doWhenSearchEndedForRow(int row) {
					}
				});

		String urlBase = new File("src/test/resources/").toURI().toASCIIString();
		assertEquals(urlBase + "caz5.html\n" + urlBase + "base.htm\n" + urlBase
				+ "link.css\n" + urlBase + "script.js\n",
				parsedResources.toString());
		assertEquals(3, problems.size());
		assertEquals(urlBase + "base.htm: Resource not found" + urlBase
				+ "link.css: Resource not found" + urlBase
				+ "script.js: Resource not found", problems.get(0).toString()
				+ problems.get(1).toString() + problems.get(2).toString());
	}

	/**
	 * Test for a page from Wikipedia which was problematic.
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void testFindBrokenLinksWikiPage() throws MalformedURLException {
		int searchDepth = 1;
		problems.clear();
		parsedResources.setLength(0);
		BrokenLinksFinder finder = new BrokenLinksFinder();
		finder.findBrokenLinks(
				new CollectedInfo(Arrays.asList(new String[] { new File(
						"src/test/resources/caz6.html").toURI().toURL().toString() }),
						searchDepth), new UIUpdater() {
					@Override
					public void reportProblem(LinkProblem problem1, int row) {
						problems.add(problem1);
					}

					@Override
					public boolean shouldStopSearch() {
						return false;
					}

					@Override
					public void doAfterSearchStarted() {
					}

					@Override
					public void doAfterSearchEnded(boolean isStoppedByUser) {
					}

					@Override
					public void parsingResource(String resource) {
						parsedResources.append(resource).append("\n");
					}

					@Override
					public void displayError(String message) {
					}

					@Override
					public void setStatus(String status, int row) {
					}

					@Override
					public void doWhenSearchEndedForRow(int row) {
					}
				});

		String urlBase = new File("src/test/resources/").toURI().toASCIIString();
		assertEquals(urlBase + "caz6.html\n", parsedResources.toString());
		assertEquals(1, problems.size());
		assertEquals("mailto:webmaster%W: Invalid URL found!", problems.get(0)
				.toString());
	}

	/**
	 * Test for search depth = 4.
	 * 
	 * @throws MalformedURLException
	 */
	@Test
	public void testFindBrokenLinksSearchDepth() throws MalformedURLException {
		int searchDepth = 4;
		problems.clear();
		parsedResources.setLength(0);
		BrokenLinksFinder finder = new BrokenLinksFinder();
		finder.findBrokenLinks(
				new CollectedInfo(Arrays.asList(new String[] { new File(
						"src/test/resources/test1.html").toURI().toURL().toString() }),
						searchDepth), new UIUpdater() {
					@Override
					public void reportProblem(LinkProblem problem1, int row) {
						problems.add(problem1);
					}

					@Override
					public boolean shouldStopSearch() {
						return false;
					}

					@Override
					public void doAfterSearchStarted() {
					}

					@Override
					public void doAfterSearchEnded(boolean isStoppedByUser) {
					}

					@Override
					public void parsingResource(String resource) {
						parsedResources.append(resource).append("\n");
					}

					@Override
					public void displayError(String message) {
					}

					@Override
					public void setStatus(String status, int row) {
					}

					@Override
					public void doWhenSearchEndedForRow(int row) {
					}
				});

		String urlBase = new File("src/test/resources/").toURI().toASCIIString();
		assertEquals(urlBase + "test1.html\n" + urlBase + "broken.html\n"
				+ urlBase + "test1.html#broken\n" + urlBase + "test2.html\n"
				+ urlBase + "test1.html#good_anchor\n" + urlBase
				+ "another_broken.html\n" + urlBase
				+ "yet_another_broken.html\n" + urlBase + "test3.html\n"
				+ urlBase + "test1.html#notExisting\n" + urlBase
				+ "test4.html\n" + urlBase + "test5.html\n" + urlBase
				+ "test6.html\n" + urlBase + "test7.html\n" + "",
				parsedResources.toString());
		assertEquals(6, problems.size());

		assertEquals("!@#$%: Invalid URL found!" + urlBase
				+ "broken.html: Resource not found" + urlBase
				+ "test1.html#broken: Broken anchor" + urlBase
				+ "another_broken.html: Resource not found" + urlBase
				+ "yet_another_broken.html: Resource not found" + urlBase
				+ "test1.html#notExisting: Broken anchor", problems.get(0)
				.toString()
				+ problems.get(1).toString()
				+ problems.get(2).toString()
				+ problems.get(3).toString()
				+ problems.get(4).toString() + problems.get(5).toString());
	}

}
