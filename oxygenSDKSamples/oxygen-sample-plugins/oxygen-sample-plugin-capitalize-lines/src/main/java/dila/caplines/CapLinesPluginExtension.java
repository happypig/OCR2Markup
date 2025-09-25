package dila.caplines;

import ro.sync.exml.plugin.selection.SelectionPluginContext;
import ro.sync.exml.plugin.selection.SelectionPluginExtension;
import ro.sync.exml.plugin.selection.SelectionPluginResult;
import ro.sync.exml.plugin.selection.SelectionPluginResultImpl;

/**
 * CapLines plugin extension.
 *
 *@created    February 13, 2003
 *@version    $Revision: 1.9 $
 */

public class CapLinesPluginExtension implements SelectionPluginExtension {

  /**
   * Capitalize the first word of each sentence.
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
        // Eat up white spaces at the begining of the line.
        pos++;
      }

      if (pos < selection.length()) {
        // The first letter of the first word found is capitalized.        
        selection =
          selection.substring(0, pos)
            + selection.substring(pos, pos + 1).toUpperCase()
            + selection.substring(pos + 1);
        while (pos < selection.length() && selection.charAt(pos) != '\n') {
          // Go to next line.
          pos++;
        }
        pos++;
      }
    }

    return new SelectionPluginResultImpl(selection);
  }
}
