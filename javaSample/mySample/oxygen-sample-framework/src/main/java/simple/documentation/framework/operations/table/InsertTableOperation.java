package simple.documentation.framework.operations.table;

import java.awt.Component;

import javax.swing.JFrame;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.exml.workspace.api.Platform;
import simple.documentation.framework.operations.table.TableCustomizerDialog.TableInfo;

/**
 * Operation used to insert a SDF table.
 */
public class InsertTableOperation implements AuthorOperation {
  
  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap args)
      throws IllegalArgumentException, AuthorOperationException {
    // Show the 'Insert table' dialog
    TableInfo tableInfo = null;
    if(authorAccess.getWorkspaceAccess().getPlatform() == Platform.STANDALONE) {
      TableCustomizerDialog tableCustomizerDialog = new TableCustomizerDialog(
          (JFrame) authorAccess.getWorkspaceAccess().getParentFrame());
      tableCustomizerDialog.setLocationRelativeTo(
          (Component) authorAccess.getWorkspaceAccess().getParentFrame());
      tableInfo = tableCustomizerDialog.showDialog();
    }

    if (tableInfo != null) {
      // Create the table XML fragment
      StringBuffer tableXMLFragment = new StringBuffer();
      tableXMLFragment.append("<table xmlns=\"http://www.oxygenxml.com/sample/documentation\"");
      if (tableInfo.getTableBackgroundColor() != null) {
        tableXMLFragment.append(" bgcolor=\"rgb(" + 
            tableInfo.getTableBackgroundColor().getRed() + "," + 
            tableInfo.getTableBackgroundColor().getGreen() + "," +
            tableInfo.getTableBackgroundColor().getBlue() +
            ")\"");
      }
      tableXMLFragment.append(">");
      if(tableInfo.getTitle() != null && tableInfo.getTitle().trim().length() > 0) {
        tableXMLFragment.append("<title>" + tableInfo.getTitle().trim() + "</title>");
      }
      
      // Add table body
      int columns = tableInfo.getColumnsNumber();
      int rows = tableInfo.getRowsNumber();
      for (int i = 0; i < rows; i++) {
        tableXMLFragment.append("<tr>");
        for (int j = 0; j < columns; j++) {
          tableXMLFragment.append("<td></td>");
        }
        tableXMLFragment.append("</tr>");
      }

      tableXMLFragment.append("</table>");


      // Insert the table 
      authorAccess.getDocumentController().insertXMLFragmentSchemaAware(
          tableXMLFragment.toString(), 
          authorAccess.getEditorAccess().getCaretOffset());
    } else {
      // User canceled the operation 
    }
  }

  /**
   * No arguments. The operation will display a dialog for choosing the table attributes.
   * 
   * @see ro.sync.ecss.extensions.api.AuthorOperation#getArguments()
   */
  public ArgumentDescriptor[] getArguments() {
    return null;
  }

  /**
   * @see ro.sync.ecss.extensions.api.Extension#getDescription()
   */
  public String getDescription() {
    return "Insert a SDF table";
  }
}