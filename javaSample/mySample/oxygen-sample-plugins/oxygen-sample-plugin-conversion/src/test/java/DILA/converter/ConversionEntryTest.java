package DILA.converter;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *  Description of the Class
 *
 *@author     radu_pisoi
 *@created    October 16, 2002
 *@version    $Revision: 1.9 $
 */
public class ConversionEntryTest extends TestCase {
    /**
     *  Constructor for the ConversionEntryTest object
     *
     *@param  testName  Description of Parameter
     */
    public ConversionEntryTest(String testName) {
        super(testName);
    }


    /**
     *  A unit test suite for JUnit
     *
     *@return    The test suite
     */
    public static Test suite() {
        return new TestSuite(ConversionEntryTest.class);
    }


    /**
     *  A unit test for JUnit
     * @author dan
     */
    public void testFormatDouble() {
      ConversionEntry fake = new ConversionEntry(){
          public double convert(double what, boolean direct, int decimalsNumber){
            return 0;
          }
      };
      fake.decimalsNumber = 2;
      assertEquals(4.66, fake.formatDouble(4.66677, -2), 0.0001);
      fake.decimalsNumber = 0;
      assertEquals(4, fake.formatDouble(4.66677, -2), 0.0001);
      fake.decimalsNumber = 2;
      assertEquals(4, fake.formatDouble(4, -2), 0.0001);
      fake.decimalsNumber = -1;
      assertEquals(4.66677, fake.formatDouble(4.66677, -2), 0.0001);
      fake.decimalsNumber = 100;
      assertEquals(4.66677, fake.formatDouble(4.66677, -2), 0.0001);

      // Get all decimals.
      fake.decimalsNumber = 100;
      assertEquals(4.66677, fake.formatDouble(4.66677, -1), 0.0001);

      // Get 0 decimals.
      fake.decimalsNumber = 100;
      assertEquals(5.0, fake.formatDouble(5.555555, 0), 0.0001);

      // Get 1 decimals.
      fake.decimalsNumber = 100;
      assertEquals(4.6, fake.formatDouble(4.66677, 1), 0.0001);

      // Get decimals number from xml file.
      fake.decimalsNumber = 3;
      assertEquals(88.888, fake.formatDouble(88.88888, -2), 0.0001);

      // Get decimals number from xml file.
      fake.decimalsNumber = 3;
      assertEquals(88.8, fake.formatDouble(88.8, -2), 0.0001);

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

    /** @link dependency 
     * @stereotype tests*/
    /*#ConversionEntry lnkConversionEntry;*/
}