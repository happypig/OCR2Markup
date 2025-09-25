package dila.formatpreserve;


import ro.sync.exml.plugin.selection.SelectionPluginContext;
import ro.sync.exml.plugin.selection.SelectionPluginExtension;
import ro.sync.exml.plugin.selection.SelectionPluginResult;
import ro.sync.exml.plugin.selection.SelectionPluginResultImpl;

/**
 * Format with Preserve Text Plugin Extension.
 */
public class FormatPreserveTextPluginExtension implements SelectionPluginExtension {
  
  /**
   * This plugin formats the selected XML content by breaking only in element tags and never in text.
   *
   *@param  context  Selection context.
   *@return          Selection plugin result.
   */
  public SelectionPluginResult process(SelectionPluginContext context) {
    // The selection on which the processing is applied
    String selection = context.getSelection();
    
    boolean inTag = false;
    StringBuffer result = new StringBuffer();
    int len = selection.length();
    char prevImportantChar = 0;
    for (int i = 0; i < len; i++) {
      char ch = selection.charAt(i);
      if('<' == ch) {
        inTag = true;
      } else if('>' == ch) {
        if(inTag) {
          if(i > 0 && i < len - 1) {
            if('\n' != prevImportantChar && 
                '?' != prevImportantChar &&
                '-' != prevImportantChar) {
              if('/' == prevImportantChar) {
                //Split empty tag before "/"
                result.insert(result.length() - 1, '\n');
              } else {
                result.append("\n");
              }
            }
          }
        }
        inTag = false;
      }
      result.append(ch);
      prevImportantChar = ch;
      if(ch != '\n' && ! Character.isWhitespace(ch)) {
        prevImportantChar = ch;
      }
    }
    return new SelectionPluginResultImpl(result.toString());
  }
}
