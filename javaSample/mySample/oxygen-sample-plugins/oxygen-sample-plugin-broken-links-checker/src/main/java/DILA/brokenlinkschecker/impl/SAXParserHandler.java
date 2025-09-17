package DILA.brokenlinkschecker.impl;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX parser handler.
 * 
 * @author sorin_carbunaru
 * 
 */
public class SAXParserHandler extends DefaultHandler {

	/**
	 * The list containing the locations of all elements that we are interested
	 * in.
	 */
	private List<Location> locations;

	/**
	 * The locator.
	 */
	private Locator locator;

	/**
	 * List of IDs or names of all the elements from the HTML document.
	 */
	private List<String> idsAndNames;

	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	@Override
	// Triggered at the start of the document
	public void startDocument() throws SAXException {
		idsAndNames = new ArrayList<String>();
		locations = new ArrayList<Location>();
	}

	@Override
	// Triggered when the start of tag is found.
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		// Add to locations information about almost all (X)HTML elements which
		// could have an URL associated
		if (qName.equalsIgnoreCase("a") || qName.equalsIgnoreCase("area")
				|| qName.equalsIgnoreCase("link")
				|| qName.equalsIgnoreCase("base")) {
			String href = attributes.getValue("href");
			if (href != null) {
				locations.add(new Location(href, locator.getLineNumber(),
						locator.getColumnNumber()));
			}
		}

		if (qName.equalsIgnoreCase("img") || qName.equalsIgnoreCase("frame")
				|| qName.equalsIgnoreCase("iframe")
				|| qName.equalsIgnoreCase("input")
				|| qName.equalsIgnoreCase("video")
				|| qName.equalsIgnoreCase("audio")
				|| qName.equalsIgnoreCase("source")
				|| qName.equalsIgnoreCase("embed")
				|| qName.equalsIgnoreCase("script")) {
			String src = attributes.getValue("src");
			if (src != null) {
				locations.add(new Location(src, locator.getLineNumber(),
						locator.getColumnNumber()));
			}
			if ((qName.equalsIgnoreCase("img") || qName
					.equalsIgnoreCase("input"))
					&& attributes.getValue("usemap") != null) {
				locations.add(new Location(attributes.getValue("usemap"),
						locator.getLineNumber(), locator.getColumnNumber()));
			}
		}

		if (qName.equalsIgnoreCase("applet")) {
			String codebase = attributes.getValue("codebase");
			if (codebase != null) {
				locations.add(new Location(codebase, locator.getLineNumber(),
						locator.getColumnNumber()));
			}
		}

		if (qName.equalsIgnoreCase("object")) {
			String attribute = attributes.getValue("data");
			if (attribute != null) {
				locations.add(new Location(attribute, locator.getLineNumber(),
						locator.getColumnNumber()));
			}

			attribute = attributes.getValue("usemap");
			if (attribute != null) {
				locations.add(new Location(attribute, locator.getLineNumber(),
						locator.getColumnNumber()));
			}

			attribute = attributes.getValue("codebase");
			if (attribute != null) {
				locations.add(new Location(attribute, locator.getLineNumber(),
						locator.getColumnNumber()));
			}
		}

		if (qName.equalsIgnoreCase("blockquote") || qName.equalsIgnoreCase("q")
				|| qName.equalsIgnoreCase("del")
				|| qName.equalsIgnoreCase("ins")) {
			String cite = attributes.getValue("cite");
			if (cite != null) {
				locations.add(new Location(cite, locator.getLineNumber(),
						locator.getColumnNumber()));
			}
		}

		if (qName.equalsIgnoreCase("body")) {
			String background = attributes.getValue("background");
			if (background != null) {
				locations.add(new Location(background, locator.getLineNumber(),
						locator.getColumnNumber()));
			}
		}

		if (qName.equalsIgnoreCase("form")) {
			String action = attributes.getValue("action");
			if (action != null) {
				locations.add(new Location(action, locator.getLineNumber(),
						locator.getColumnNumber()));
			}
		}

		if (qName.equalsIgnoreCase("button") || qName.equalsIgnoreCase("input")) {
			String formaction = attributes.getValue("formaction");
			if (formaction != null) {
				locations.add(new Location(formaction, locator.getLineNumber(),
						locator.getColumnNumber()));
			}
		}

		// Add the name or the id of the current element in the idsAndNames list
		String id = attributes.getValue("id");
		String name = attributes.getValue("name");
		if (id != null) {
			idsAndNames.add(id);
		}
		if (name != null) {
			idsAndNames.add(name);
		}
	}

	/**
	 * Get the IDs of all the tags from the HTML document.
	 * 
	 * @return the IDs of all the tags from the HTML document.
	 */
	public List<String> getIdsAndNames() {
		return idsAndNames;
	}

	/**
	 * Get locations.
	 * 
	 * @return the locations.
	 */
	public List<Location> getLocations() {
		return locations;
	}

}
