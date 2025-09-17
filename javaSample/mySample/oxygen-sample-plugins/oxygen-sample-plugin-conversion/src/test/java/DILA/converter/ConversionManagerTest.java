package DILA.converter;


import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.exml.plugin.PluginManager;


/**
 *  Description of the Class
 *
 *@author     dan, radu
 *@created    October 15, 2002
 *@version    $Revision: 1.18 $
 */
public class ConversionManagerTest extends TestCase {

    /**
     *  Logger for logging.
     */
    static Logger logger = LoggerFactory.getLogger(ConversionManagerTest.class.getName());

    /**
     *  A unit test for getGormats method
     * @author dan
     * @throws ConversionException 
     * @author dan
     * @throws IOException 
     */
    public void testGetFormats() throws IOException, ConversionException {
      ConversionManager cm = new ConversionManager();
      // Get formats
      List<ConversionFormat> result = cm.getFormats();
      assertEquals("Number of formats must be ", 7, result.size());
    }


    /**
     *  A unit test for JUnit
     * @author dan
     * @throws ConversionException 
     * @author dan
     * @throws IOException 
     */
    public void testGetDefaultValueAsBoolean() throws IOException, ConversionException {
      ConversionManager cm = new ConversionManager();
      // Get formats
      Boolean result = cm.getDefaultValueAsBoolean("Lock labels");
      Boolean expected = new Boolean(true);
      assertEquals(result, expected);

      result = cm.getDefaultValueAsBoolean("Lock labels kkk");
      expected = new Boolean(false);
      assertEquals(result, expected);
    }

    /**
     *  A unit test for JUnit
     * @author dan
     * @throws ConversionException 
     * @author dan
     * @throws IOException 
     */
    public void testGetStringResource() throws IOException, ConversionException {
      ConversionManager cm = new ConversionManager();
      // Get formats
      String result = cm.getStringResource("Format");
      String expected = "Format";
      assertEquals(result, expected);

      result = cm.getStringResource("Master system");
      expected = "U.S. System";
      assertEquals(result, expected);

      result = cm.getStringResource("Copy value and close");
      expected = "Copy value and close";
      assertEquals(result, expected);

      result = cm.getStringResource("Include labels");
      expected = "Include labels";
      assertEquals(result, expected);

      result = cm.getStringResource("Slave system");
      expected = "Metric System";
      assertEquals(result, expected);

      result = cm.getStringResource("Swap values");
      expected = "Swap values";
      assertEquals(result, expected);

      result = cm.getStringResource("Type");
      expected = "Type";
      assertEquals(result, expected);
    }

    /**
     *  The JUnit setup method
     */
    protected void setUp() {
      PluginManager.disableLateDelegationCLForTests(true);
      PluginManager.setPluginsDir("..");
      PluginManager.getInstance();
    }


    /**
     *  The teardown method for JUnit
     */
    protected void tearDown() { 
      PluginManager.disableLateDelegationCLForTests(false);
    }

}