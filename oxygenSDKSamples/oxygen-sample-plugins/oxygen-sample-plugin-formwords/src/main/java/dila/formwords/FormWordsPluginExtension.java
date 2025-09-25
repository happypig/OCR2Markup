package dila.formwords;

import ro.sync.exml.plugin.selection.SelectionPluginContext;
import ro.sync.exml.plugin.selection.SelectionPluginExtension;
import ro.sync.exml.plugin.selection.SelectionPluginResult;
import ro.sync.exml.plugin.selection.SelectionPluginResultImpl;

/**
 * FormWords plugin extension.
 *
 *@created    February 13, 2003
 *@version    $Revision: 1.7 $
 */

public class FormWordsPluginExtension implements SelectionPluginExtension {

  /**
   * Capitalize the first letter of each word.
   *
   *@param  context  Selection context.
   *@return          Selection plugin result.
   */
  public SelectionPluginResult process(SelectionPluginContext context) {
    // The selection on wich the processing is applied
    String selection = context.getSelection();

    // The current position in the selection string.
    int pos = 0;

    while (pos < selection.length()) {
      // While the end of selection is not reached.
      while (pos < selection.length() && Character.isWhitespace(selection.charAt(pos))) {
        // Eat up white spaces.
        pos++;
      }
      if (pos < selection.length()) {
        // The first letter of the found word is capitalized.
        selection =
          selection.substring(0, pos)
            + selection.substring(pos, pos + 1).toUpperCase()
            + selection.substring(pos + 1);
        pos++;
        while (pos < selection.length() && !Character.isWhitespace(selection.charAt(pos))) {
          // Go to the end of the word.
          pos++;
        }
        pos++;
      }
    }

    return new SelectionPluginResultImpl(selection);
  }
}
