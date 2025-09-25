package dila.converter;


import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ro.sync.xml.parser.ParserCreator;

/**
 *  Description of the Class
 *
 *@author     dan, radu
 *@created    October 14, 2002
 *@version    $Revision: 1.22 $
 */
public class ConversionFormatTest extends TestCase {
    /**
     *  Logger for logging.
     */
    private static Logger logger = LoggerFactory.getLogger(ConversionFormatTest.class.getName());

    /** @link dependency
     * @stereotype use*/
    /** @link dependency
     * @stereotype tests*/
    /*#ConversionFormat lnkConversionFormat;*/

    /**
     *  Constructor for the ConversionFormatTest object
     *
     *@param  testName  Description of Parameter
     */
    public ConversionFormatTest(String testName) {
        super(testName);
    }


    /**
     *  A unit test suite for JUnit
     *
     *@return    The test suite
     */
    public static Test suite() {
        return new TestSuite(ConversionFormatTest.class);
    }


    /**
     *  The main program for the AutomaticLicenseGeneratorTest class
     *
     *@param  args  The command line arguments
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        ConversionFormatTest test = new ConversionFormatTest("test");
        test.testGetS1Values();
        test.testGetS2Values();
        test.testGetAssociatedLabel();
        test.testGetName();
        test.testConvert();
        test.testConversionFormat();
    }


    /**
     *  A unit test for JUnit
     * @author dan
     */
    public void testGetS1Values() {
        String fileName = "test" + File.separator + "conversion.xml";

        try {
            Element rootElem = getRootElement(fileName);

            // Get Lenght format
            Element formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "Length");
            ConversionFormat cf = new ConversionFormat(formatElem);

            // Get all s1Values
            Set<String> result = cf.getMasterValues(null);
            TreeSet<String> expected = new TreeSet<String>();
            expected.add("Foot");
            expected.add("Inches");
            expected.add("Miles");
            expected.add("Yard");
            assertEquals(expected, result);

            // Get master value for slave value = Meters
            result = cf.getMasterValues("Meters");
            if(logger.isDebugEnabled()){
              logger.debug("Master values for slave value = meters" + result);
            }
            expected = new TreeSet<String>();
            expected.add("Foot");
            expected.add("Inches");
            expected.add("Yard");
            assertEquals(expected, result);

            // Get Temperature format
            formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "Temperature");
            if(logger.isDebugEnabled()){
              logger.debug("Elem name : " + formatElem.getNodeName());
            }
            cf = new ConversionFormat(formatElem);

            // Get all s1Values
            result = cf.getMasterValues(null);
            expected = new TreeSet<String>();
            expected.add("Fahrenheit");
            assertEquals(expected, result);
        } catch (Exception ex) {
            logger.error(ex, ex);
            fail(ex.getMessage());
        }
    }

    /**
     *  A unit test for ConversionFormat constructor
     * @author dan
     */
    public void testConversionFormat() {
        String fileName = "test" + File.separator + "conversion.xml";
        try {
            Element rootElem = getRootElement(fileName);

            // All formats must be valid
            // Get Lenght format
            Element formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "Length");
            if(logger.isDebugEnabled()){
              logger.debug("Format name : " + formatElem.getNodeName());
            }
            // This format must be valid
            ConversionFormat cf = new ConversionFormat(formatElem);
            assertNotNull(cf);

            // Get an invalid format 'nelu'
            formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "nelu");
            // This format must not be valid
            // => throw ConversionException
            cf = null;
            try{
              cf = new ConversionFormat(formatElem);
            } catch (ConversionException ex){
              if(logger.isDebugEnabled()){
                logger.debug(ex,ex);
              }
            }
            // cf must be null
            assertNull("This format is invalid, => cf must be null", cf);


        } catch (Exception ex) {
            logger.error(ex, ex);
            fail(ex.getMessage());
        }
    }


    /**
     *  A unit test for JUnit
     * @author dan
     */
    public void testGetS2Values() {
        String fileName = "test" + File.separator + "conversion.xml";

        try {
            Element rootElem = getRootElement(fileName);

            // Get Lenght format
            Element formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "Length");
            ConversionFormat cf = new ConversionFormat(formatElem);

            // Get all s1Values
            Set<String> result = cf.getSlaveValues(null);
            if(logger.isDebugEnabled()){
              logger.debug("Master values : " + result);
            }
            TreeSet<String> expected = new TreeSet<String>();
            expected.add("Millimeters");
            expected.add("Centimeters");
            expected.add("Kilometers");
            expected.add("Meters");
            assertEquals(expected, result);

            //get slave value for master = Inches
            result = cf.getSlaveValues("Inches");
            if(logger.isDebugEnabled()){
              logger.debug("Slave values for master = Inches" + result);
            }
            expected = new TreeSet<String>();
            expected.add("Millimeters");
            expected.add("Centimeters");
            expected.add("Meters");
            assertEquals(expected, result);

            result = cf.getSlaveValues("Foot");
            if(logger.isDebugEnabled()){
              logger.debug("Slave values for master = Foot" + result);
            }
            expected = new TreeSet<String>();
            expected.add("Meters");
            assertEquals(expected, result);
        } catch (Exception ex) {
            logger.error(ex, ex);
            fail(ex.getMessage());
        }
    }


    /**
     *  A unit test for getAssociatedLabel method
     * @author dan
     */
    public void testGetAssociatedLabel() {
        try {
            String fileName = "test" + File.separator + "conversion.xml";
            Element rootElem = getRootElement(fileName);

            // Get Lenght format
            Element formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "Length");
            if(logger.isDebugEnabled()){
              logger.debug("Elem name : " + formatElem.getTagName());
            }

            ConversionFormat cf = new ConversionFormat(formatElem);

            String result = cf.getAssociatedLabel("Inches");
            if(logger.isDebugEnabled()){
              logger.debug("Label : " + result);
            }
            String expected = "in.";
            assertEquals("Label for Inches must be in.", expected, result);

            result = cf.getAssociatedLabel("Meters");
            if(logger.isDebugEnabled()){
              logger.debug("Label : " + result);
            }
            expected = "m.";
            assertEquals("Label for Meters must be m.", expected, result);
        } catch (Exception ex) {
            logger.error(ex, ex);
            fail(ex.getMessage());
        }
    }


    /**
     *  A unit test for convert method
     * @author dan
     * @throws Exception 
     */
    public void testConvert() throws Exception{
      String fileName = "test" + File.separator + "conversion.xml";
      Element rootElem = getRootElement(fileName);

      // Get Lenght format
      Element formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "Length");
      if(logger.isDebugEnabled()){
        logger.debug("Elem name : " + formatElem.getTagName());
      }

      ConversionFormat cf = new ConversionFormat(formatElem);

      double result = cf.convert("Inches", "Centimeters", 2.3, 3.2, true, -1);
      double expected = 5.842;
      if(logger.isDebugEnabled()){
        logger.debug("result : " + result);
      }
      assertEquals("2.3 Inches is equal with 5.842 Centimeters", expected, result, 0.00001);

      result = cf.convert("Inches", "Centimeters", 2.3, 3.2, false, -1);
      expected = 1.25984251;
      if(logger.isDebugEnabled()){
        logger.debug("result : " + result);
      }
      assertEquals("3.2 Centimeters is equal with 1.25984251 Inches", expected, result, 0.00001);

      // Get Temperature format.
      formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "Temperature");
      if(logger.isDebugEnabled()){
        logger.debug("Elem name : " + formatElem.getTagName());
      }

      cf = new ConversionFormat(formatElem);

      result = cf.convert("Fahrenheit", "Celsius", 5*1.8 + 32, 0, true, -1);
      expected = 5;
      if(logger.isDebugEnabled()){
        logger.debug("result : " + result);
      }
      assertEquals("5*1.8 + 32 Fahrenheit => 5 Celsius", expected, result, 0.00001);

      String s1Val = "Foot";
      String s2Val = "Centimeters";
      // For Length format 'foot' and 'centimeters' values not exist
      try{
        result = cf.convert(s1Val, s2Val, 4, 0, true, -1);
      } catch (ConversionException ex){
        if(logger.isDebugEnabled()){
          logger.debug(ex);
        }
        assertEquals("Could not find entry for: Foot, Centimeters", ex.getMessage());
      }
    }


    /**
     *  A unit test for getName method
     * @author dan
     */
    public void testGetName() {
        try {
            String fileName = "test" + File.separator + "conversion.xml";
            Element rootElem = getRootElement(fileName);

            // Get Lenght format
            Element formatElem = getElementByElementAndAttribute(rootElem, "entries", "format", "Length");
            if(logger.isDebugEnabled()){
              logger.debug("Elem name : " + formatElem.getNodeName());
            }

            ConversionFormat cf = new ConversionFormat(formatElem);

            String result = cf.getName();
            String expected = "Length";

            assertEquals("Format name must be Length", expected, result);

        } catch (Exception ex) {
            logger.error(ex, ex);
            fail(ex.getMessage());
        }
    }


    /**
     *  The JUnit setup method
     */
    protected void setUp() {
      //
    }


    /**
     *  The teardown method for JUnit
     */
    protected void tearDown() {
      //
    }

    /**
     *  Open xml file and return root element.
     *
     *@param  xmlFile          The name of xml file.
     *@return                  The root element of xml file.
     *@exception  IOException  If xml file not found.
     */
    private Element getRootElement(String xmlFile) throws IOException {
        Element result = null;

        //try to parse the XML attachement
        DocumentBuilder db = null;

        try {
            db = ParserCreator.newDocumentBuilder();
            Document DOMdoc = db.parse(xmlFile);
            result = DOMdoc.getDocumentElement();
            if(logger.isDebugEnabled()){
              logger.debug("Root elem : " + result.getNodeName());
            }
            ro.sync.basic.xml.BasicXmlUtil.removeBlankTextNodes(result);
        } catch (ParserConfigurationException ex) {
            if(logger.isDebugEnabled()){
              logger.debug(ex, ex);
            }
        } catch (SAXException ex) {
            if(logger.isDebugEnabled()){
              logger.debug(ex, ex);
            }
        }
        return result;
    }

    /**
     * Return element by name and attribute.
     *
     *@param  elementName     The element name.
     *@param  attributeName   The attribute name.
     *@param  attributeValue  The attribute value.
     *@param  parent          The element parent.
     *@return                 An element, if it's found or null other.
     */
    private static Element getElementByElementAndAttribute(Element parent, String elementName, String attributeName, String attributeValue) {
        Element result = null;
        NodeList nodeList = parent.getElementsByTagName(elementName);

        if(logger.isDebugEnabled()){
          logger.debug("Node number : " + nodeList.getLength());
        }

        int nodeListLenght = nodeList.getLength();
        for (int i = 0; i < nodeListLenght; i++) {
            String attribute = ((Element) nodeList.item(i)).getAttribute(attributeName);
            if (attribute.equals(attributeValue)) {
                result = (Element) nodeList.item(i);
                if(logger.isDebugEnabled()){
                  logger.debug("Node found : " + nodeList.item(i).getNodeName());
                }
                break;
            }
        }

        return result;
    }



    /**
     *  Return element text. If element not found return null
     *
     *@param  parent       The element parent.
     *@param  elementName  The element name.
     *@return              The element value.
     */
    private static String getTextFromElement(Element parent, String elementName) {
        String result = null;

        NodeList nodeList = parent.getElementsByTagName(elementName);
        //if element exist
        if (nodeList.getLength() > 0) {
            result = nodeList.item(0).getFirstChild().getNodeValue();
            if(logger.isDebugEnabled()){
              logger.debug("Get text for element : " + parent.getNodeName() + " result : " + result);
            }
        }

        return result;
    }


    /**
     *  A unit test for getNodeByElementAndAttribute method
     * @author dan
     */
    public void testGetNodeByElementAndAttribute() {
        String fileName = "test" + File.separator + "conversion.xml";
        try {
            //get root element
            Element rootElement = getRootElement(fileName);

            //get element with name entries and attribute format = Lenght
            Node result = getElementByElementAndAttribute(rootElement, "entries", "format", "Length");

            NodeList nodeList = ((Element) result).getElementsByTagName("entry");
            if(logger.isDebugEnabled()){
              logger.debug("Entry number : " + nodeList.getLength());
            }

            assertEquals("Entry number must be 6 ", 6, nodeList.getLength());

            //first entry
            Element firstEntry = (Element)nodeList.item(0);
            //get s1Value
            String s1Value = getTextFromElement(firstEntry, "s1Value");
            assertEquals("s1 value must be Inches", "Inches", s1Value);

        } catch (Exception ex) {
            if(logger.isDebugEnabled()){
              logger.debug(ex, ex);
            }
            fail(ex.getMessage());
        }
    }

}