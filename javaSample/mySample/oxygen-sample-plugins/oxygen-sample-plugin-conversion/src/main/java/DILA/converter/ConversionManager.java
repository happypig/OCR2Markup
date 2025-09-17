package DILA.converter;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ro.sync.exml.plugin.PluginDescriptor;
import ro.sync.xml.parser.ParserCreator;

/**
 *  Contain all formats.
 *
 *@author     dan
 *@created    October 14, 2002
 *@version    $Revision: 1.31 $
 */
public class ConversionManager {

	/**
	 *  The path to conversion.xml file.
	 */
	public static String CONFIG_RESOURCE = "etc/conversion.xml";

	/**
	 *  All available formats.
	 */
	private final List<ConversionFormat> formats;

	private final Properties stringResources = new Properties();
	private final Properties defaultValues = new Properties();

	/**
	 *  Extract all formats from configuration file.
	 *
	 *@exception  IOException          Thrown when conversion.xml file not
	 *      found.
	 *@exception  ConversionException  Thrown when invalid format is found.
	 */
	public ConversionManager() throws IOException, ConversionException {
		ConversionPlugin plugIn = ConversionPlugin.getInstance();
		PluginDescriptor descriptor = plugIn.getDescriptor();
		File baseDir = descriptor.getBaseDir();
		String configResource = new File(baseDir, CONFIG_RESOURCE).getPath();

		Element xmlRoot;
		try {
			// Get root element from xml file.
			xmlRoot = getRootElement(configResource);
		} catch (Exception ex) {
		  ex.printStackTrace();
			throw new ConversionException(ex.getMessage());
		}

		// Seet labels and default values.
		createStringResources(xmlRoot);
		createDefaultValues(xmlRoot);

		// Get all formats node
		NodeList formatsNodeList = xmlRoot.getElementsByTagName("entries");
		this.formats = new Vector<ConversionFormat>();
		// Put in list all formats.
		for (int i = 0; i < formatsNodeList.getLength(); i++) {
			Element current = (Element) formatsNodeList.item(i);
			ConversionFormat conversionFormat = new ConversionFormat(current);
			formats.add(conversionFormat);
		}
	}

	/**
	 *  Return all formats.
	 *
	 *@return    All formats.
	 */
	public List<ConversionFormat> getFormats() {
		return formats;
	}

	/**
	 *  Gets a conversion format by name. If format not found then return null.
	 *
	 *@param  formatName  The format name.
	 *@return             The conversion format
	 */
	public ConversionFormat getFormatByName(String formatName) {
		ConversionFormat result = null;
		Iterator<ConversionFormat> it = formats.iterator();
		while (it.hasNext()) {
			ConversionFormat currentFormat = (ConversionFormat) it.next();
			if (currentFormat.getName().equals(formatName)) {
				result = currentFormat;
			}
		}
		return result;
	}

	/**
	 *  Gets the stringResource attribute of the ConversionManager object
	 *
	 *@param  key  Description of Parameter
	 *@return      The stringResource value
	 */
	public String getStringResource(String key) {
		return stringResources.getProperty(key, key);
	}

	/**
	 *  Gets the defaultValueAsString attribute of the ConversionManager object
	 *
	 *@param  key  Description of Parameter
	 *@return      The defaultValueAsString value
	 */
	public String getDefaultValueAsString(String key) {
		return defaultValues.getProperty(key, key);
	}

	/**
	 *  Gets the defaultValueAsBoolean attribute of the ConversionManager object
	 *
	 *@param  key  Description of Parameter
	 *@return      The defaultValueAsBoolean value
	 */
	public Boolean getDefaultValueAsBoolean(String key) {
		return Boolean.valueOf(defaultValues.getProperty(key, "false").equalsIgnoreCase("true"));
	}

	/**
	 *  Open xml file and return xml root element. If an exception has occurs
	 *  then return null.
	 *
	 *@param  xmlFileResource                   The name of xml file. It must be
	 *      a qualified resource name.
	 *@return                                   The root element.
	 *@exception  IOException                   Thrown when xml file not found.
	 *@exception  ParserConfigurationException  Description of Exception
	 *@exception  SAXException                  Description of Exception
	 */
	private Element getRootElement(String xmlFileResource)
		throws IOException, ParserConfigurationException, SAXException {
		InputStream inputStream = new FileInputStream(xmlFileResource);

		InputSource is = new InputSource(inputStream);
		DocumentBuilder parser = ParserCreator.newDocumentBuilderFakeResolver();

		return parser.parse(is).getDocumentElement();
	}

	/**
	 *  Set the default values.
	 *
	 *@param  xmlRoot  The root element from xml file
	 */
	private void createDefaultValues(Element xmlRoot) {
		// Set default values.
		Element defaultsElement = (Element) xmlRoot.getElementsByTagName("defaults").item(0);
		NodeList defaults = defaultsElement.getElementsByTagName("defaultvalue");

		String key;
		String value;
		for (int i = 0; i < defaults.getLength(); i++) {
			key = ((Element) defaults.item(i)).getAttribute("key");
			value = ((Element) defaults.item(i)).getAttribute("value");
			defaultValues.setProperty(key, value);
		}
	}

	/**
	 *  Set labels for internationalization.
	 *
	 *@param  xmlRoot  The root element from xml file.
	 */
	private void createStringResources(Element xmlRoot) {
		// Get labels node list.
		Element labelsElement = (Element) xmlRoot.getElementsByTagName("labels").item(0);
		NodeList labels = labelsElement.getElementsByTagName("label");

		// Parse all labels.
		String key = "";
		String value = "";
		for (int i = 0; i < labels.getLength(); i++) {
			key = ((Element) labels.item(i)).getAttribute("key");
			value = ((Element) labels.item(i)).getAttribute("value");
			stringResources.setProperty(key, value);
		}
	}
}
