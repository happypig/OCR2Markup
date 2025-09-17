package DILA.converter;


import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  Manages a set of conversion entries, associated to a format. ("Length",
 *  "Force", etc)
 *
 *@author     dan, radu
 *@created    October 14, 2002
 *@version    $Revision: 1.18 $
 */
public class ConversionFormat {

  /**
   *  All entries for this format.
   */
  List<ConversionEntry> entries;

  /**
   *  The name of the format, for example "Length".
   */
  private String name;

  /** @link dependency 
   * @stereotype throws*/
  /*#ConversionException lnkConversionException;*/

  /**
   *  Constructor for the ConversionFormat object.
   *
   *@param  formatNode               The root node for this format.
   *@exception  ConversionException  If an entry for this format is invalid,
   *      or root node is null.
   */
  public ConversionFormat(Node formatNode) throws ConversionException {
    // If root node is null.
    if (formatNode == null) {
      throw new ConversionException("Format node is null.");
    } else {
      // Try to parse the format node ant construct the measure format.
      try {
        // Set format name from the xml file.
        this.name = ((Element) formatNode).getAttribute("format");
        // Parse this format node and get all nodes with 'entry' tag
        NodeList allEntries = ((Element) formatNode).getElementsByTagName("entry");

        this.entries = new Vector<ConversionEntry>();
        // For all 'entry' nodes contruct an FactorConversionEntry or
        //    FormulaConversionEntry and put them in entries list
        for (int i = 0; i < allEntries.getLength(); i++) {
          Element currentEntry = (Element) allEntries.item(i);
          // Get masterValue from current entry
          String masterValue = getTextFromElement(currentEntry, "s1Value");
          // Get masterLabel from current node
          String masterLabel = getTextFromElement(currentEntry, "s1Label");
          // Get slaveValue from current node
          String slaveValue = getTextFromElement(currentEntry, "s2Value");
          // Get slaveLabel from current node
          String slaveLabel = getTextFromElement(currentEntry, "s2Label");
          // Get decimals number
          String decimalsStr = getTextFromElement(currentEntry, "decimals");
          // If decimals node not found in xml file then decimals = -1
          //    decimals = -1 => Show all result decimals
          int decimals = ConversionEntry.ALL_DECIMALS;
          if (decimalsStr != null) {
            decimals = Integer.parseInt(decimalsStr);
          }
          // Verify if current entry has formula or factor conversion
          String convertFormula = getTextFromElement(currentEntry, "v1");
          String convertFactor = getTextFromElement(currentEntry, "conversionFactor");

          // If current entry has conversion formula.
          if (convertFormula != null) {
            String v1Value = getTextFromElement(currentEntry, "v1");
            double v1 = Double.parseDouble(v1Value);

            String v2Value = getTextFromElement(currentEntry, "v2");
            double v2 = Double.parseDouble(v2Value);

            // Create a FormulaConversionEntry and put it to the list.
            FormulaConversionEntry fce =
              new FormulaConversionEntry(
                masterLabel,
                slaveLabel,
                masterValue,
                slaveValue,
                decimals,
                v1,
                v2);
            this.entries.add(fce);
          } else {
            // If current entry has conversion factor.
            if (convertFactor != null) {
              double factor = Double.parseDouble(convertFactor);
              FactorConversionEntry fce =
                new FactorConversionEntry(
                  masterLabel,
                  slaveLabel,
                  masterValue,
                  slaveValue,
                  decimals,
                  factor);
              // Create a FactorConversionEntry and put it to the list.
              this.entries.add(fce);
            } else {
              // Invalid entry.
              throw new ConversionException("Invalid entry to " + this.name + " format.");
            }
          }
        }
        // When Double.parseDouble throw an exception
      } catch (NumberFormatException ex) {
        throw new ConversionException(ex.getMessage());
      }
    }
  }

  /**
   *  Get the format name.
   *
   *@return    The format name.
   */
  public String getName() {
    return name;
  }

  /**
   *  Get all the possible values that can be listed in the first combo box,
   *  in the context of the selected value in the second combo box.
   *
   *@param  slaveValue  The value that is selected in the second combo box. If
   *      null, all the possible values for the first combo box will be
   *      returned.
   *@return             The master value.
   */
  public Set<String> getMasterValues(String slaveValue) {
    TreeSet<String> result = new TreeSet<String>();
    // If slaveValue == null return all master measure units.
    Iterator<ConversionEntry> iterator = entries.iterator();
    // For all entries.
    while (iterator.hasNext()) {
      ConversionEntry ce = iterator.next();
      if (slaveValue == null || slaveValue.equals(ce.getSlaveValue())) {
        result.add(ce.getMasterValue());
      }
    }
    return result;
  }

  /**
   *  Get all the possible values that can be listed in the second combo box,
   *  in the context of the selected value in the first combo box.
   *
   *@param  masterValue  The value that is selected in the first combo box. If
   *      null, all the possible values for the second combo box will be
   *      returned.
   *@return              The slave value.
   */
  public Set<String> getSlaveValues(String masterValue) {
    TreeSet<String> result = new TreeSet<String>();
    // If slaveValue == null return all masterValues
    Iterator<ConversionEntry> iterator = entries.iterator();
    while (iterator.hasNext()) {
      ConversionEntry ce = iterator.next();
      if (masterValue == null || masterValue.equals(ce.getMasterValue())) {
        result.add(ce.getSlaveValue());
      }
    }
    return result;
  }

  /**
   *  Get the label associated with a measure. Used for displaying the labels
   *  in the text edit fields. If label not found then return null.
   *
   *@param  measure  The measure, for example "milimeters"
   *@return          The lable for the measure, example "mm"
   */
  public String getAssociatedLabel(String measure) {
    String result = null;
    // Parse all entries until find label associated with this measure.
    int entriesLenght = entries.size();
    for (int i = 0; i < entriesLenght; i++) {
      String masterValue = ((ConversionEntry) entries.get(i)).getMasterValue();
      String slaveValue = ((ConversionEntry) entries.get(i)).getSlaveValue();
      if (masterValue.equals(measure)) {
        result = ((ConversionEntry) entries.get(i)).getMasterLabel();
        break;
      } else {
        if (slaveValue.equals(measure)) {
          result = ((ConversionEntry) entries.get(i)).getSlaveLabel();
          break;
        }
      }
    }
    return result;
  }

  /**
   *  Based on masterValue and slaveValue it determines the conversion entry
   *  that knows how to convert the two values.
   *
   *@param  masterValue              The first section conversion measure. Eg
   *      "foot".
   *@param  slaveValue               The second section conversion measure. Eg
   *      "meter".
   *@param  arg1                     The value existing in the first edit.
   *@param  arg2                     The value existing in the first edit.
   *@param  direct                   Is true when the conversion is made
   *      directly from the first measure into the second, or reversed.
   *@param  decimalsNumber           The decimals number.
   *@return                          The conversion result.
   *@exception  ConversionException  Description of Exception
   */
  public double convert(
    String masterValue,
    String slaveValue,
    double arg1,
    double arg2,
    boolean direct,
    int decimalsNumber)
    throws ConversionException {
    double result = -1;
    // If an entry is found => foundEntry = 'true'
    boolean foundEntry = false;
    // Find entry with masterValue and slaveValue.
    Iterator<ConversionEntry> it = entries.iterator();
    while (it.hasNext()) {
      ConversionEntry entry = it.next();

      // If this entry has masterValue and slaveValue.
      if (entry.getMasterValue() != null
        && entry.getMasterValue().equals(masterValue)
        && entry.getSlaveValue() != null
        && entry.getSlaveValue().equals(slaveValue)) {
        foundEntry = true;

        // If current entry is a FactorConversionEntry
        if (entry instanceof FactorConversionEntry) {
          // If direct conversion.
          if (direct) {
            result = ((FactorConversionEntry) entry).convert(arg1, true, decimalsNumber);
          } else {
            result = ((FactorConversionEntry) entry).convert(arg2, false, decimalsNumber);
          }
          // Current entry is a FormulaConversionEntry.
        } else {
          if (direct) {
            result = ((FormulaConversionEntry) entry).convert(arg1, true, decimalsNumber);
          } else {
            result = ((FormulaConversionEntry) entry).convert(arg2, false, decimalsNumber);
          }
        }
      }
    }
    if (!foundEntry) {
      throw new ConversionException("Could not find entry for: " + masterValue + ", " + slaveValue);
    }
    return result;
  }

  /**
   *  Gets the text that is first child of element of an element, or null if
   *  element not found.
   *
   *@param  element    The element name.
   *@param  childName  The first child name.
   *@return            The node text.
   */
  private String getTextFromElement(Node element, String childName) {
    String result = null;

    NodeList nodeList = ((Element) element).getElementsByTagName(childName);
    // If element exist.
    if (nodeList.getLength() > 0) {
      result = nodeList.item(0).getFirstChild().getNodeValue();
    }
    return result;
  }
}
