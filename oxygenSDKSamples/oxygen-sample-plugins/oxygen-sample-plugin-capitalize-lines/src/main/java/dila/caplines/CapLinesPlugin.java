package dila.caplines;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * 	CaplinesPlugin is a sample plugin for the selection plugin type.
 * 
 * 
 * @created February 13, 2003
 * 
 */
public class CapLinesPlugin extends Plugin {
  /**
   * Plugin instance.
   */
  private static CapLinesPlugin instance = null;  

  /**
   * LowercasePlugin constructor.
   * 
   * @param descriptor Plugin descriptor.
   */
  public CapLinesPlugin(PluginDescriptor descriptor) {
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
  public static CapLinesPlugin getInstance() {
    return instance;
  }
}
