package simple.documentation.framework.operations.highlight;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlighter;

/**
 * Operation used to remove all persistent highlights.
 */
public class RemoveAllHighlightsOperation implements AuthorOperation {

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap args)
      throws IllegalArgumentException, AuthorOperationException {
    AuthorPersistentHighlighter highlighter = authorAccess.getEditorAccess().getPersistentHighlighter();
    int highlights = highlighter.getHighlights().length;
    // Remove all highlights
    highlighter.removeAllHighlights();
    authorAccess.getWorkspaceAccess().showInformationMessage(highlights == 1 ? "1 highlight removed" : highlights + " highlights removed");
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
    return "Remove all persistent highlights";
  }
  
}
