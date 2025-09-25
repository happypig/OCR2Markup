package dila.imposeoptions;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * Impose options plugin. 
 */
public class ImposeOptionsPlugin extends Plugin {
  /**
   * The static plugin instance.
   */
  private static ImposeOptionsPlugin instance = null;

  /**
   * Constructs the plugin.
   * 
   * @param descriptor The plugin descriptor
   */
  public ImposeOptionsPlugin(PluginDescriptor descriptor) {
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
  public static ImposeOptionsPlugin getInstance() {
    return instance;
  }
}