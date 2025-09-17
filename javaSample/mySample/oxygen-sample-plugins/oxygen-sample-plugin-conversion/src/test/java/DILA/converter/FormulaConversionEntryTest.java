package DILA.converter;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Description of the Class
 *
 *@author     dan, radu
 *@created    October 15, 2002
 *@version    $Revision: 1.13 $
 */
public class FormulaConversionEntryTest extends TestCase {
    /**
     *  Logger for logging.
     */
    private static Logger logger = LoggerFactory.getLogger(FormulaConversionEntryTest.class.getName());

    /** @link dependency 
     * @stereotype tests*/
    /*#FormulaConversionEntry lnkFormulaConversionEntry;*/

    /**
     *  Constructor for the FormulaConversionEntryTest object
     *
     *@param  testName  Description of Parameter
     */
    public FormulaConversionEntryTest(String testName) {
        super(testName);
    }


    /**
     *  A unit test suite for JUnit
     *
     *@return    The test suite
     */
    public static Test suite() {
        return new TestSuite(FormulaConversionEntryTest.class);
    }


    /**
     *  A unit test for convert method
     * @author dan
     */
    public void testConvert() {
        try {
            // Create a FormulaConversionEntry with
            //    s1Value, s2s1Value, s1Label, s2Label
            //    v1 = 10.3, v2 = 5.7, decimals = 10
            FormulaConversionEntry fce =
                    new FormulaConversionEntry("s1L", "s2L", "s1V", "s2V", 10, 10.3, 5.7);
            //  Test FormulaConversionEntry attributes
            assertEquals("s1L", fce.getMasterLabel());
            assertEquals("s2L", fce.getSlaveLabel());
            assertEquals("s1V", fce.getMasterValue());
            assertEquals("s2V", fce.getSlaveValue());
            assertEquals(10,fce.getDecimalsNumber());

            // Convert 12 Fahrenheit to Celsius
            double result = fce.convert(12, true, -1);
            assertEquals(0.298245, result, 0.00001);

            // Convert 12 Celsius to Fahrenheit
            result = fce.convert(12, false, -1);
            assertEquals(78.7, result, 0.00001);
        } catch (Exception ex) {
          if(logger.isDebugEnabled()){
            logger.debug(ex,ex);
          }
          fail(ex.getMessage());
        }
    }

    /**
     *  A unit test for FormulaConversionEntry contructor
     * @author dan
     */
    public void testFormulaConversionEntry() {
        try {
            // Create a FormulaConversionEntry with
            //  s1Value, s2s1Value, s1Label, s2Label
            //  v1 = 10.3, v2 = 5.7, decimals = 3
            FormulaConversionEntry fce =
              new FormulaConversionEntry("s1L", "s2L", "s1V", "s2V", 3, 10.3, 5.7);

            // Create a FormulaConversionEntry with
            //  s1Label = null => throw ConversionException
            fce = null;
            try{
              fce = new FormulaConversionEntry(null, "s2L", "s1V", "s2V", 3, 10.3, 5.7);
            } catch (ConversionException ex){
                logger.debug("Error");
                if(logger.isDebugEnabled()){
                  logger.debug(ex, ex);
                }
            }
            // fce must be null
            assertNull(fce);

            // Create a FormulaConversionEntry with
            //  s1Value, s2s1Value, s1Label, s2Label
            //  v1 = 10.3, decimals = 10

            //  v2 = 0 => ConversionException
            try{
              fce = new FormulaConversionEntry("s1L", "s2L", "s1V", "s2V", 3, 10.3, 0);
            } catch (ConversionException ex){
                logger.debug("Error");
                if(logger.isDebugEnabled()){
                  logger.debug(ex, ex);
                }
            }
            // fce must be null
            assertNull(fce);
        } catch (Exception ex) {
          logger.error(ex,ex);
        }
    }


    /**
     *  The JUnit setup method
     */
    protected void setUp() { 
      // Do nothing.
    }


    /**
     *  The teardown method for JUnit
     */
    protected void tearDown() {
      // Do nothing.
    }

}