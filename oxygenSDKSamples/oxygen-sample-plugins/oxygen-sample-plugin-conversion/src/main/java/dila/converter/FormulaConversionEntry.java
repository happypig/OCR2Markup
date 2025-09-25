package dila.converter;


/**
 *  Implements a formula based convertor. The formula is rez = (arg -v1)/v2. The
 *  reverse of the formula is: rez = arg * v2 + v1.
 *
 *@author     dan, radu
 *@created    October 14, 2002
 *@version    $Revision: 1.13 $
 */
public class FormulaConversionEntry extends ConversionEntry {

	/**
	 *  The v1 formula constant.
	 */
	private double v1;

	/**
	 *  The v2 formula constant.
	 */
	private double v2;

	/** @link dependency 
	 * @stereotype throws*/
	/*#ConversionException lnkConversionException;*/

	/**
	 *  Constructor for the FormulaConversionEntry object
	 *
	 *@param  masterLabel              The master measure label.
	 *@param  slaveLabel               The slave measure label.
	 *@param  masterValue              The master measure value.
	 *@param  slaveValue               The second section value.
	 *@param  v1                       The v1 constant
	 *@param  v2                       The v2 constant
	 *@param  decimals                 The decimals number to be included in the
	 *      result.
	 *@exception  ConversionException  Trown when this conversion entry is
	 *      invalid, that means one of the argument is null, or v2 value is 0.
	 */
	public FormulaConversionEntry(
		String masterLabel,
		String slaveLabel,
		String masterValue,
		String slaveValue,
		int decimals,
		double v1,
		double v2)
		throws ConversionException {
		// If masterValue, slaveValue, masterLabel, slaveLabel is null or v2 = 0
		if (masterValue == null
			|| slaveValue == null
			|| masterLabel == null
			|| slaveLabel == null
			|| v2 == 0) {
			throw new ConversionException("I");
		} else {
			this.masterLabel = masterLabel;
			this.slaveLabel = slaveLabel;
			this.masterValue = masterValue;
			this.slaveValue = slaveValue;
			this.v1 = v1;
			this.v2 = v2;
			this.decimalsNumber = decimals;
		}
	}

	/**
	 *  Converts the argument from the master measure to the slave measure.
	 *
	 *@param  what            The conversion argument
	 *@param  direct          If true, then make the conversion from master to
	 *      slave, otherwise reverse.
	 *@param  decimalsNumber  The decimals number witch be included in the
	 *      result.
	 *@return                 The conversion result.
	 */
	public double convert(double what, boolean direct, int decimalsNumber) {
		double result = 0;
		if (direct) {
			result = (what - v1) / v2;
		} else {
			result = what * v2 + v1;
		}
		result = formatDouble(result, decimalsNumber);

		return result;
	}

}
