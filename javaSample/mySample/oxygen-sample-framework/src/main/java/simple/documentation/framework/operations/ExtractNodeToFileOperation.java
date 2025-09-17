package simple.documentation.framework.operations;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.swing.JFileChooser;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.basic.util.URLUtil;

/**
 * Operation to save the Author node at caret in a separate document and refresh the new file path in the project.
 */
public class ExtractNodeToFileOperation implements AuthorOperation {

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap args) throws IllegalArgumentException,
      AuthorOperationException {
    int caretOffset = authorAccess.getEditorAccess().getCaretOffset();
    try {
      // Get node at caret
      AuthorNode nodeAtCaret = authorAccess.getDocumentController().getNodeAtOffset(caretOffset);
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setMultiSelectionEnabled(false);

      // Show Save Dialog
      if (fileChooser.showSaveDialog((Component) authorAccess.getWorkspaceAccess().getParentFrame()) 
          == JFileChooser.APPROVE_OPTION) {
        File outputFile = fileChooser.getSelectedFile();
        FileOutputStream fos = new FileOutputStream(outputFile);
        OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");

        // Write the node fragment
        AuthorDocumentFragment fragment = authorAccess.getDocumentController().createDocumentFragment(nodeAtCaret, true);
        String xmlFragment = authorAccess.getDocumentController().serializeFragmentToXML(fragment);
        writer.write(xmlFragment);
        writer.close();

        // Open file
        URL outputFileUrl = URLUtil.correct(outputFile);
        authorAccess.getWorkspaceAccess().open(outputFileUrl);

        // Refresh in project
        authorAccess.getWorkspaceAccess().refreshInProject(outputFileUrl);
      }
    } catch (Exception e) {
      authorAccess.getWorkspaceAccess().showErrorMessage(e.getMessage());
    }
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#getArguments()
   */
  public ArgumentDescriptor[] getArguments() {
    return null;
  }

  /**
   * @see ro.sync.ecss.extensions.api.Extension#getDescription()
   */
  public String getDescription() {
    return "Save the Author node at caret in a separate document and refresh the new file path in the project";
  }

}
