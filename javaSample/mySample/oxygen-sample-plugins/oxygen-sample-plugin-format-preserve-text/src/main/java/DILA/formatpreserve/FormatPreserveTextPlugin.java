package DILA.formatpreserve;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * 	FormatPreserveTextPlugin is a sample plugin for the selection plugin type.
 */
public class FormatPreserveTextPlugin extends Plugin {
  /**
   * Plugin instance.
   */
  private static FormatPreserveTextPlugin instance = null;  

  /**
   * Constructor.
   * 
   * @param descriptor Plugin descriptor.
   */
  public FormatPreserveTextPlugin(PluginDescriptor descriptor) {
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
  public static FormatPreserveTextPlugin getInstance() {
    return instance;
  }
}
