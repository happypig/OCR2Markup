package dila.formsentences;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * 	FormSentencesPlugin is a sample plugin for the selection plugin type.
 * 
 * 
 * @created February 13, 2003
 * 
 */
public class FormSentencesPlugin extends Plugin {
  /**
   * Plugin instance.
   */
  private static FormSentencesPlugin instance = null;  

  /**
   * FormSentencesPlugin constructor.
   * 
   * @param descriptor Plugin descriptor.
   */
  public FormSentencesPlugin(PluginDescriptor descriptor) {
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
  public static FormSentencesPlugin getInstance() {
    return instance;
  }
}
