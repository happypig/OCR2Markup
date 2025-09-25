package dila.zeroindent;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * 	ZeroIndentPlugin is a sample plugin for the selection plugin type.
 * 
 * 
 * @author george
 * @created April 26, 2006
 * @version    $Revision: 1.2 $
 */
public class ZeroIndentPlugin extends Plugin {

  private static ZeroIndentPlugin instance = null;

  /**
   *  Zero indent plugin.
   * 
   *@param descriptor Plugin descriptor.
   */
  public ZeroIndentPlugin(PluginDescriptor descriptor) {
    super(descriptor);
    
    if (instance != null) {
      throw new IllegalStateException("Already instantiated!");
    }
    
    instance = this;
  }
  
  /**
   * Get the plugin instance.
   * 
   * @return the shared plugin instance.
   */
  public static ZeroIndentPlugin getInstance() {
    return instance;
  }
}
