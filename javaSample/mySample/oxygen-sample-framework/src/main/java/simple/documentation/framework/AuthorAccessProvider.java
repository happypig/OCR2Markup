package simple.documentation.framework;

import ro.sync.ecss.extensions.api.AuthorAccess;

/**
 * Provides access to Author functions, to specific components corresponding to 
 * editor, document, workspace, tables, change tracking and utility informations and actions.
 */
public interface AuthorAccessProvider {
  /**
   * Gets access to Author functions and components.
   * 
   * @return The Author access. 
   */
  AuthorAccess getAuthorAccess();
}
