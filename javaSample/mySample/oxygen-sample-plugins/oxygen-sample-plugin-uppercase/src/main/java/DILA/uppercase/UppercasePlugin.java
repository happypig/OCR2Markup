package DILA.uppercase;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * 	UppercasePlugin is a sample plugin for the selection plugin type.
 * 
 * 
 * @created February 13, 2003
 * 
 */
public class UppercasePlugin extends Plugin {
  /**
   * Plugin instance.
   */
  private static UppercasePlugin instance = null;  

  /**
   * UppercasePlugin constructor.
   * 
   * @param descriptor Plugin descriptor.
   */
  public UppercasePlugin(PluginDescriptor descriptor) {
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
  public static UppercasePlugin getInstance() {
    return instance;
  }
}
