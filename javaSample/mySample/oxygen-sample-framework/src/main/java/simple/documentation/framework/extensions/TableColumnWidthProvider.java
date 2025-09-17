package simple.documentation.framework.extensions;
import java.util.ArrayList;
import java.util.List;

import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider;
import ro.sync.ecss.extensions.api.WidthRepresentation;
import ro.sync.ecss.extensions.api.node.AttrValue;
import ro.sync.ecss.extensions.api.node.AuthorElement;

/**
 * Simple Documentation Framework table column width provider.
 *
 */
public class TableColumnWidthProvider implements AuthorTableColumnWidthProvider {

  /**
   * Cols start offset
   */
  private int colsStartOffset;

  /**
   * Cols end offset
   */
  private int colsEndOffset;

  /**
   * Column widths specifications
   */
  private List<WidthRepresentation> colWidthSpecs = new ArrayList<WidthRepresentation>();

  /**
   * The table element
   */
  private AuthorElement tableElement;

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#commitColumnWidthModifications(ro.sync.ecss.extensions.api.AuthorDocumentController, ro.sync.ecss.extensions.api.WidthRepresentation[], java.lang.String)
   */
  public void commitColumnWidthModifications(AuthorDocumentController authorDocumentController,
      WidthRepresentation[] colWidths, String tableCellsTagName) throws AuthorOperationException {
    if ("td".equals(tableCellsTagName)) {
      if (colWidths != null && tableElement != null) {
        if (colsStartOffset >= 0 && colsEndOffset >= 0 && colsStartOffset < colsEndOffset) {
          authorDocumentController.delete(colsStartOffset,
              colsEndOffset);
        }
        String xmlFragment = createXMLFragment(colWidths);
        int offset = -1;
        AuthorElement[] header = tableElement.getElementsByLocalName("header");
        if (header != null && header.length > 0) {
          // Insert the cols elements before the 'header' element 
          offset = header[0].getStartOffset();
        } else {
          AuthorElement[] title = tableElement.getElementsByLocalName("title");
          if (title != null && title.length > 0) {
            // Insert the cols elements after the 'title' element 
            offset = title[0].getStartOffset() + 1;
          } else {
            // Just insert after table start tag
            offset =  tableElement.getStartOffset() + 1;
          }
        }
        if (offset == -1) {
          throw new AuthorOperationException("No valid offset to insert the columns width specification.");
        }
        authorDocumentController.insertXMLFragment(xmlFragment, offset);
      }
    }
  }

  /**
   * Creates the XML fragment representing the column specifications.
   *
   * @param widthRepresentations
   * @return The XML fragment as a string.
   */
  private String createXMLFragment(WidthRepresentation[] widthRepresentations) {
    StringBuffer fragment = new StringBuffer();
    String ns = tableElement.getNamespace();
    for (int i = 0; i < widthRepresentations.length; i++) {
      WidthRepresentation width = widthRepresentations[i];
      fragment.append("<customcol");
      String strRepresentation = width.getWidthRepresentation();
      if (strRepresentation != null) {
        fragment.append(" width=\"" + width.getWidthRepresentation() + "\"");
      }
      if (ns != null && ns.length() > 0) {
        fragment.append(" xmlns=\"" + ns + "\"");
      }
      fragment.append("/>");
    }
    return fragment.toString();
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#commitTableWidthModification(ro.sync.ecss.extensions.api.AuthorDocumentController, int, java.lang.String)
   */
  public void commitTableWidthModification(AuthorDocumentController authorDocumentController,
      int newTableWidth, String tableCellsTagName) throws AuthorOperationException {
    if ("td".equals(tableCellsTagName)) {
      if (newTableWidth > 0) {
        if (tableElement != null) {
          String newWidth = String.valueOf(newTableWidth);

          authorDocumentController.setAttribute(
              "width",
              new AttrValue(newWidth),
              tableElement);
        } else {
          throw new AuthorOperationException("Cannot find the element representing the table.");
        }
      }
    }
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#getCellWidth(ro.sync.ecss.extensions.api.node.AuthorElement, int, int)
   */
  public List<WidthRepresentation> getCellWidth(AuthorElement cellElement, int colNumberStart,
      int colSpan) {
    List<WidthRepresentation> toReturn = null;
    int size = colWidthSpecs.size();
    if (size >= colNumberStart && size >= colNumberStart + colSpan) {
      toReturn = new ArrayList<WidthRepresentation>(colSpan);
      for (int i = colNumberStart; i < colNumberStart + colSpan; i ++) {
        // Add the column widths
        toReturn.add(colWidthSpecs.get(i));
      }
    }
    return toReturn;
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#getTableWidth(java.lang.String)
   */
  public WidthRepresentation getTableWidth(String tableCellsTagName) {
    WidthRepresentation toReturn = null;
    if (tableElement != null && "td".equals(tableCellsTagName)) {
      AttrValue widthAttr = tableElement.getAttribute("width");
      if (widthAttr != null) {
        String width = widthAttr.getValue();
        if (width != null) {
          toReturn = new WidthRepresentation(width, true);
        }
      }
    }
    return toReturn;
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#init(ro.sync.ecss.extensions.api.node.AuthorElement)
   */
  public void init(AuthorElement tableElement) {
    this.tableElement = tableElement;
    AuthorElement[] colChildren = tableElement.getElementsByLocalName("customcol");
    if (colChildren != null && colChildren.length > 0) {
      for (int i = 0; i < colChildren.length; i++) {
        AuthorElement colChild = colChildren[i];
        if (i == 0) {
          colsStartOffset = colChild.getStartOffset();
        } 
        if (i == colChildren.length - 1) {
          colsEndOffset = colChild.getEndOffset();
        }
        // Determine the 'width' for this col.
        AttrValue colWidthAttribute = colChild.getAttribute("width");
        String colWidth = null;
        if (colWidthAttribute != null) {
          colWidth = colWidthAttribute.getValue();
          // Add WidthRepresentation objects for the columns this 'customcol' specification
          // spans over.
          colWidthSpecs.add(new WidthRepresentation(colWidth, true));
        }
      }
    }
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#isAcceptingFixedColumnWidths(java.lang.String)
   */
  public boolean isAcceptingFixedColumnWidths(String tableCellsTagName) {
    return "td".equals(tableCellsTagName);
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#isAcceptingPercentageColumnWidths(java.lang.String)
   */
  public boolean isAcceptingPercentageColumnWidths(String tableCellsTagName) {
    return "td".equals(tableCellsTagName);
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#isAcceptingProportionalColumnWidths(java.lang.String)
   */
  public boolean isAcceptingProportionalColumnWidths(String tableCellsTagName) {
    return "td".equals(tableCellsTagName);
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#isTableAcceptingWidth(java.lang.String)
   */
  public boolean isTableAcceptingWidth(String tableCellsTagName) {
    return "td".equals(tableCellsTagName);
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider#isTableAndColumnsResizable(java.lang.String)
   */
  public boolean isTableAndColumnsResizable(String tableCellsTagName) {
    return "td".equals(tableCellsTagName);
  }

  /**
   * @see ro.sync.ecss.extensions.api.Extension#getDescription()
   */
  public String getDescription() {
    return "Implementation for the Simple Documentation Framework table layout.";
  } 
}