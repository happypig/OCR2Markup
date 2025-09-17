package DILA.converter;


/**
 *  Implements a factor based conversion.
 *
 *@author     dan, radu
 *@created    October 14, 2002
 *@version    $Revision: 1.13 $
 */
public class FactorConversionEntry extends ConversionEntry {

	/**
	 *  The conversion factor.
	 */
	private double factor;

	/** @link dependency 
	 * @stereotype throws*/
	/*#ConversionException lnkConversionException;*/

	/**
	 *  Constructor for the FactorConversionEntry object.
	 *
	 *@param  masterLabel              The master measure label.
	 *@param  slaveLabel               The slave measure label.
	 *@param  masterValue              The master measure value.
	 *@param  slaveValue               The second section value.
	 *@param  decimalsToShow           The decimals number to be included in the
	 *      result.
	 *@param  factor                   The conversion factor.
	 *@exception  ConversionException  Trown when this conversion entry is
	 *      invalid, that means one of the argument is null, or conversion
	 *      factor is 0.
	 */
	public FactorConversionEntry(
		String masterLabel,
		String slaveLabel,
		String masterValue,
		String slaveValue,
		int decimalsToShow,
		double factor)
		throws ConversionException {
		if (masterValue == null
			|| slaveValue == null
			|| masterLabel == null
			|| slaveLabel == null
			|| factor == 0) {
			throw new ConversionException("Invalid conversion entry");
		} else {
			this.masterLabel = masterLabel;
			this.slaveLabel = slaveLabel;
			this.masterValue = masterValue;
			this.slaveValue = slaveValue;
			this.factor = factor;
			this.decimalsNumber = decimalsToShow;
		}
	}

	/**
	 *  Converts the argument from the master measure to the slave measure if
	 *  direct attribute is true, otherwise reverse.
	 *
	 *@param  what            The conversion argument.
	 *@param  direct          If true, then make the conversion from s1 to s2,
	 *      otherwise reverse.
	 *@param  decimalsNumber  The decimals number witch be included in the
	 *      result.
	 *@return                 The conversion result.
	 */
	public double convert(double what, boolean direct, int decimalsNumber) {
		double result = 0;
		if (direct) {
			result = factor * what;
		} else {
			result = what / factor;
		}

		result = formatDouble(result, decimalsNumber);

		return result;
	}

}
