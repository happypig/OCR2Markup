package DILA.lowercase;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * 	LowercasePlugin is a sample plugin for the selection plugin type.
 * 
 * 
 * @created February 13, 2003
 * 
 */
public class LowercasePlugin extends Plugin {
  /**
   * Plugin instance.
   */
  private static LowercasePlugin instance = null;  

  /**
   * LowercasePlugin constructor.
   * 
   * @param descriptor Plugin descriptor.
   */
  public LowercasePlugin(PluginDescriptor descriptor) {
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
  public static LowercasePlugin getInstance() {
    return instance;
  }
}
