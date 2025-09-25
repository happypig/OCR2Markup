package dila.zeroindent;

import java.util.StringTokenizer;

import ro.sync.exml.plugin.selection.SelectionPluginContext;
import ro.sync.exml.plugin.selection.SelectionPluginExtension;
import ro.sync.exml.plugin.selection.SelectionPluginResult;
import ro.sync.exml.plugin.selection.SelectionPluginResultImpl;

/**
 *  Zero indent plugin extension.
 *
 * @author george
 * @created April 26, 2006
 * @version    $Revision: 1.2 $
 */

public class ZeroIndentPluginExtension implements SelectionPluginExtension {

  /**
   *  Process context.
   *
   *@param  context  Selection context.
   *@return          Selection plugin result.
   */
  public SelectionPluginResult process(SelectionPluginContext context) {
    String selection = context.getSelection(); 
    StringTokenizer tok = new StringTokenizer(selection, "\n");
    StringBuffer result = new StringBuffer();
    int k;
    int n;
    while (tok.hasMoreTokens()) {
      String token = tok.nextToken();
      n = token.length();
      k = 0;
      while (k<n && Character.isWhitespace(token.charAt(k))) { k++;}
      result.append(token.substring(k));
      if (tok.hasMoreTokens()) {
        result.append("\n");
      }
    }
    return new SelectionPluginResultImpl(result.toString());
  }
}
