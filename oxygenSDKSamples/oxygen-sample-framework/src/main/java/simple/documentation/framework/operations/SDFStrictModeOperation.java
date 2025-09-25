package simple.documentation.framework.operations;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;

/**
 * 
 * Strict Mode Operation used to change the permissions to change parts of the document content. 
 *
 */
public class SDFStrictModeOperation implements AuthorOperation {

  // The strict mode key used to store the strict mode option.
  private String strictModeOptionKey = "strictMode";

  /**
   * The Strict mode has changed.
   */
  public void doOperation(final AuthorAccess authorAccess, ArgumentsMap args)
  throws IllegalArgumentException, AuthorOperationException {

    // Get the strict mode option value from the option storage.
    String strictMode = authorAccess.getOptionsStorage().getOption(strictModeOptionKey, "false");
    boolean enabled = Boolean.parseBoolean(strictMode);

    // Change the strict mode option state
    enabled = !enabled;

    // Save the new value of the strict mode option
    authorAccess.getOptionsStorage().setOption(strictModeOptionKey, String.valueOf(enabled));

    // Show the strict mode operation status.
    String statusMessage = "Strict Mode: " + (enabled ? " ON " : "OFF");
    authorAccess.getWorkspaceAccess().showStatusMessage(statusMessage);
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
    return "Strict mode operation";
  }
}
