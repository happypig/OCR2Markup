package dila.converter;


/**
 *  Defines the abstract conversion entry.
 *
 *@author     dan, radu
 *@created    October 14, 2002
 *@version    $Revision: 1.16 $
 */
public abstract class ConversionEntry {

    /**
     *  Constant for defining that all decimals should be included in the
     *  result.
     */
    public final static int ALL_DECIMALS = -1;

    /**
     *  The slave measure label.
     */
    protected String slaveLabel;

    /**
     *  The master measure label.
     */
    protected String masterLabel;

    /**
     *  The slave measure value.
     */
    protected String slaveValue;

    /**
     *  The master measure label.
     */
    protected String masterValue;

    /**
     *  The decimals number to show.
     */
    protected int decimalsNumber;


    /**
     *  Return decimals number to show.
     *
     *@return    The decimals number.
     */
    public int getDecimalsNumber() {
        return decimalsNumber;
    }


    /**
     *  Return slave measure label.
     *
     *@return    The slave measure label.
     */
    public String getSlaveLabel() {
        return slaveLabel;
    }


    /**
     *  Return master measure label.
     *
     *@return    The master measure label.
     */
    public String getMasterLabel() {
        return masterLabel;
    }


    /**
     *  Return slave measure value.
     *
     *@return    The slave measure value.
     */
    public String getSlaveValue() {
        return slaveValue;
    }


    /**
     *  Return master measure value.
     *
     *@return    The master measure value.
     */
    public String getMasterValue() {
        return masterValue;
    }


    /**
     *  Converts the argument from the master measure to the slave measure if
     *  direct attribute is true, else reverse.
     *
     *@param  what            The conversion argument.
     *@param  direct          If true, then make the conversion from master to
     *      slave, otherwise reverse.
     *@param  decimalsNumber  The decimals number witch be included in the
     *      result.
     *@return                 The conversion result.
     */
    public abstract double convert(double what, boolean direct, int decimalsNumber);


    /**
     *  Format a double number with specified decimals number to be included in
     *  the result.
     *
     *@param  numberToFormat             Number to format.
     *@param  decimalsNumberToRepresent  The decimals number.
     *@return                            The formated number.
     */
    protected double formatDouble(double numberToFormat, int decimalsNumberToRepresent) {
        String numberStr = "" + numberToFormat;
        double result = 0;

        int decNumber = 0;
        if (decimalsNumberToRepresent == -2) {
            decNumber = this.decimalsNumber;
        } else if (decimalsNumberToRepresent == -1) {
            decNumber = ALL_DECIMALS;
        } else {
            decNumber = decimalsNumberToRepresent;
        }

        // If decimals number >= 0 then format number
        // else leave number intact
        if (decNumber != ALL_DECIMALS) {
            // Find index of '.'
            int index = numberStr.indexOf('.');

            int endIndex = index + decNumber + 1;
            if (endIndex > numberStr.length()) {
                endIndex = numberStr.length();
            }
            if (decNumber == 0) {
                endIndex = index;
            }

            String resultStr = numberStr.substring(0, endIndex);
            result = Double.parseDouble(resultStr);
        } else {
            result = numberToFormat;
        }
        return result;
    }
}
