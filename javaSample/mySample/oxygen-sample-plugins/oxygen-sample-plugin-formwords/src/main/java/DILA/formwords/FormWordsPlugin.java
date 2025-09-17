package DILA.formwords;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * 	FormWordsPlugin is a sample plugin for the selection plugin type.
 * 
 * @created February 13, 2003
 * 
 */
public class FormWordsPlugin extends Plugin {
  /**
   * Plugin instance.
   */
  private static FormWordsPlugin instance = null;  

  /**
   * FormWordsPlugin constructor.
   * 
   * @param descriptor Plugin descriptor.
   */
  public FormWordsPlugin(PluginDescriptor descriptor) {
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
  public static FormWordsPlugin getInstance() {
    return instance;
  }
}
