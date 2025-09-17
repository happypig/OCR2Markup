package DILA.converter;


import ro.sync.exml.plugin.selection.SelectionPluginContext;
import ro.sync.exml.plugin.selection.SelectionPluginExtension;
import ro.sync.exml.plugin.selection.SelectionPluginResult;

/**
 *  Conversion plugin extension.
 *
 *@author     dan
 *@author     mircea
 *@created     October 17, 2002
 *@version    $Revision: 1.6 $
 */

public class ConversionPluginExtension implements SelectionPluginExtension {

  /**
   * The conversion dialog.
   */
  private ConversionDialog conversionDialog;

  /**
   *  Process context.
   *
   *@param  context  Selection context.
   *@return          Selection plugin result.
   */
  public SelectionPluginResult process(SelectionPluginContext context) {
    if (conversionDialog == null) {
      conversionDialog =
        new ConversionDialog(
          context.getFrame(),
          ConversionPlugin.getInstance().getDescriptor().getName());
    }
    conversionDialog.setToConvert(context.getSelection());
    return null;
  }
}
