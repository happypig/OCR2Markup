package DILA.converter;


import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * 	ConversionPlugin.java
 * 
 * 
 * @author mircea
 * @created Mar 5, 2003
 * 
 */
public class ConversionPlugin extends Plugin {

  private static ConversionPlugin instance = null;

  /** @link dependency 
   * @label has a*/
  /*#ConversionPluginExtension lnkConversionPluginExtension;*/

  /**
   *  ConversionPlugin.
   * 
   *@param descriptor Plugin descriptor.
   */
  public ConversionPlugin(PluginDescriptor descriptor) {
    super(descriptor);
    
    if (instance != null) {
      throw new IllegalStateException("Already instantiated!");
    }
    
    instance = this;
  }
  
  /**
   * @return The conversion plugin instance.
   */
  public static ConversionPlugin getInstance() {
    return instance;
  }

}
