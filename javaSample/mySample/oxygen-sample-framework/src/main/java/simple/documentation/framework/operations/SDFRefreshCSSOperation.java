package simple.documentation.framework.operations;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.access.AuthorEditorAccess;

/**
 * Refresh CSS operation.
 *
 */
public class SDFRefreshCSSOperation implements AuthorOperation {

  public void doOperation(AuthorAccess authorAccess, ArgumentsMap args)
  throws IllegalArgumentException, AuthorOperationException {
    AuthorEditorAccess access = authorAccess.getEditorAccess();
    // Reload the CSS files and perform a refresh on the whole document to recompute
    // the layout 
    access.refresh();
  }

  /**
   * Arguments.
   */
  public ArgumentDescriptor[] getArguments() {
    // No arguments
    return null;
  }

  /**
   * Description.
   */
  public String getDescription() {
    return "Refresh CSS operation for Simple Documentation Framework";
  }
}
