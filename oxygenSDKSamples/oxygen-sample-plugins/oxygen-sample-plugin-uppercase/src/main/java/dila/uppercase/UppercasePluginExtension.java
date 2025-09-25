package dila.uppercase;

import ro.sync.exml.plugin.selection.SelectionPluginContext;
import ro.sync.exml.plugin.selection.SelectionPluginExtension;
import ro.sync.exml.plugin.selection.SelectionPluginResult;
import ro.sync.exml.plugin.selection.SelectionPluginResultImpl;

/**
 * Uppercase plugin extension.
 *
 *@created    February 13, 2003
 *@version    $Revision: 1.7 $
 */

public class UppercasePluginExtension implements SelectionPluginExtension {

  /**
   * Convert the text to uppercase.
   *
   *@param  context  Selection context.
   *@return          Selection plugin result.
   */
  public SelectionPluginResult process(SelectionPluginContext context) {
    return new SelectionPluginResultImpl(context.getSelection().toUpperCase());
  }
}
