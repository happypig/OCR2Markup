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
public class FactorConversionEntryTest extends TestCase {
	/**
	 *  Logger for logging.
	 */
	private static Logger logger = LoggerFactory.getLogger(FactorConversionEntryTest.class.getName());

	/** @link dependency 
	 * @stereotype tests*/
	/*#FactorConversionEntry lnkFactorConversionEntry;*/

	/**
	 *  Constructor for the FactorConversionEntryTest object
	 *
	 *@param  testName  Description of Parameter
	 */
	public FactorConversionEntryTest(String testName) {
		super(testName);
	}

	/**
	 *  A unit test suite for JUnit
	 *
	 *@return    The test suite
	 */
	public static Test suite() {
		return new TestSuite(FactorConversionEntryTest.class);
	}

	/**
	 *  A unit test for convert method
	 * @author dan
	 */
	public void testConvert() {
		try {
			// Create a FactorConversionEntry with
			//    s1Value, s2s1Value, s1Label, s2Label
			//    factor = 0,5, decimals = 3
			FactorConversionEntry fce = new FactorConversionEntry("s1L", "s2L", "s1V", "s2V", 10, 0.5);
			assertEquals("s1L", fce.getMasterLabel());
			assertEquals("s2L", fce.getSlaveLabel());
			assertEquals("s1V", fce.getMasterValue());
			assertEquals("s2V", fce.getSlaveValue());
			assertEquals(10, fce.getDecimalsNumber());
			// Convert 90.7 s1 in s2 :: 90.7 / 0.5
			assertEquals(181.4, fce.convert(90.7, false, -1), 0.000001);
			// Convert 90.7 s2 in s1 :: 90.7 * 0.5
			assertEquals(45.35, fce.convert(90.7, true, -1), 0.000001);
		} catch (Exception ex) {
   if(logger.isDebugEnabled()){
  			logger.debug(ex, ex);
   }
			fail(ex.getMessage());
		}
	}

	/**
	 *  A unit test for FactorConversionEntry contructor
	 * @author dan
	 */
	public void testFactorConversionEntry() {
		try {
			// Create a FactorConversionEntry with
			//  s1Value, s2s1Value, s1Label, s2Label
			//  factor = decimals = 3
			//  => construct without exception
			FactorConversionEntry fce = new FactorConversionEntry("s1L", "s2L", "s1V", "s2V", 10, 10.3);

			//  Create a FactorConversionEntry with
			//    s1Value = null => throw ConversionException
			fce = null;
			try {
				fce = new FactorConversionEntry("s1L", "s2L", null, "s2V", 3, 10.3);
			} catch (ConversionException ex) {
				logger.debug("Error");
    if(logger.isDebugEnabled()){
  				logger.debug(ex, ex);
    }
			}
			// fce must be null
			assertNull(fce);

			// Create a FactorConversionEntry with
			//  s1Value, s2s1Value, s1Label, s2Label
			//  decimals = 10

			// factor = 0 => ConversionException
			try {
				fce = new FactorConversionEntry("s1L", "s2L", "s1V", "s2V", 3, 0);
			} catch (ConversionException ex) {
				logger.debug("Error");
    if(logger.isDebugEnabled()){
  				logger.debug(ex, ex);
    }
			}
			// fce must be null
			assertNull(fce);
		} catch (Exception ex) {
			logger.error(ex, ex);
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