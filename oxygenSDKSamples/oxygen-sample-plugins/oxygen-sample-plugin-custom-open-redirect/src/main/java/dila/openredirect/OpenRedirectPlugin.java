package dila.openredirect;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * Sample plugin for customs open. 
 */
public class OpenRedirectPlugin extends Plugin {
  /**
   * The static plugin instance.
   */
  private static OpenRedirectPlugin instance = null;

  /**
   * Constructs the plugin.
   * 
   * @param descriptor The plugin descriptor
   */
  public OpenRedirectPlugin(PluginDescriptor descriptor) {
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
  public static OpenRedirectPlugin getInstance() {
    return instance;
  }
}