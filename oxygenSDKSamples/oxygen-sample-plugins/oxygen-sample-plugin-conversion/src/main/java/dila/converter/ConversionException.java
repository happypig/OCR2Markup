package dila.converter;


/**
 *  Description of the Class
 *
 *@author     dan, radu
 *@created    October 15, 2002
 *@version    $Revision: 1.8 $
 */
@SuppressWarnings("serial")
public class ConversionException extends Exception {
    /**
     * Constructs an ConversionException with null as its error detail message.
     */
    public ConversionException() {
        super();
    }


    /**
     * Constructs an ConversionException with the specified detail
     * message.
     *
     *@param  mes  The detail message.
     */
    public ConversionException(String mes) {
        super(mes);
    }
}