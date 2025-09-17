package simple.documentation.framework.operations;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;

/**
 * Open the selected text in a new TXT editor.
 */
public class OpenInNewEditor implements AuthorOperation {

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap arg1) throws IllegalArgumentException,
      AuthorOperationException {
    String selectedText = authorAccess.getEditorAccess().getSelectedText();
    // Open in new editor
    authorAccess.getWorkspaceAccess().createNewEditor("txt", null, selectedText);
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#getArguments()
   */
  public ArgumentDescriptor[] getArguments() {
    // No arguments
    return null;
  }

  /**
   * @see ro.sync.ecss.extensions.api.Extension#getDescription()
   */
  public String getDescription() {
    return "Open new editor";
  }

}
