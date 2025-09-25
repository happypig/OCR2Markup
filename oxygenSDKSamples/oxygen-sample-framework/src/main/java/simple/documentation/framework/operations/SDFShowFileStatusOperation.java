package simple.documentation.framework.operations;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;

/**
 * Show file status operation.
 *
 */
public class SDFShowFileStatusOperation implements AuthorOperation {

  public void doOperation(AuthorAccess authorAccess, ArgumentsMap args)
  throws IllegalArgumentException, AuthorOperationException {
    // Build the file status message.
    StringBuilder message = new StringBuilder();
    // Editor location
    message.append("Location: " + authorAccess.getEditorAccess().getEditorLocation() + "\n");
    // Determine if the document from the editor contains unsaved modifications.
    message.append("Modified: " + authorAccess.getEditorAccess().isModified() + "\n");
    // Determine if the document from the editor was ever saved.
    message.append("Untitled: " + authorAccess.getEditorAccess().isNewDocument());

    // Show the informations about the file status
    authorAccess.getWorkspaceAccess().showInformationMessage(message.toString());
  }

  /**
   * Arguments. 
   */
  public ArgumentDescriptor[] getArguments() {
    // No arguments
    return null;
  }

  /**
   * Description
   */
  public String getDescription() {
    return "Show the status of the current file";
  }

}
