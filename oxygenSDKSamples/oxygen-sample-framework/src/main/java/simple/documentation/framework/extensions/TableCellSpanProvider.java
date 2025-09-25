package simple.documentation.framework.extensions;

import ro.sync.ecss.extensions.api.AuthorTableCellSpanProvider;
import ro.sync.ecss.extensions.api.node.AttrValue;
import ro.sync.ecss.extensions.api.node.AuthorElement;

/**
 * Simple Documentation Framework table cell span provider.
 *
 */
public class TableCellSpanProvider implements AuthorTableCellSpanProvider {

  /**
   * Extracts the integer specifying what is the width (in columns)  of the cell
   * representing in the table layout the cell element.
   */
  public Integer getColSpan(AuthorElement cell) {
    Integer colSpan = null;

    AttrValue attrValue = cell.getAttribute("column_span");
    if(attrValue != null) {
      // The attribute was found.
      String cs = attrValue.getValue();
      if(cs != null) {        
        try {
          colSpan = new Integer(cs);
        } catch (NumberFormatException ex) {
          // The attribute value was not a number.
        }     
      }   
    }
    return colSpan;
  }

  /**
   * Extracts the integer specifying what is the height (in rows) of the cell
   * representing in the table layout the cell element.
   */
  public Integer getRowSpan(AuthorElement cell) {
    Integer rowSpan = null;

    AttrValue attrValue = cell.getAttribute("row_span");
    if(attrValue != null) {
      // The attribute was found.
      String rs = attrValue.getValue();
      if(rs != null) {        
        try {
          rowSpan = new Integer(rs);
        } catch (NumberFormatException ex) {
          // The attribute value was not a number.
        }     
      }   
    }
    return rowSpan;
  }

  /**
   * @return true considering the column specifications always available.
   */
  public boolean hasColumnSpecifications(AuthorElement tableElement) {
    return true;
  }

  /**
   * Ignored. We do not extract data from the <code>table</code> element. 
   */
  public void init(AuthorElement table) {
    // Nothing to do.
  }

  public String getDescription() {
    return "Implementation for the Simple Documentation Framework table layout.";
  }
}