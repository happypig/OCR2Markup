package DILA.eclipse;

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This is the top-level class of the oXygen sample extensions plugin.
 *
 * @see AbstractUIPlugin for additional information on UI plugins
 */
public class SamplePlugin extends AbstractUIPlugin {  

  /**
   * Singleton instance of the plugin.
   */
  private static SamplePlugin inst;
  
  /**
   * Creates the extensions plugin and caches its default instance.
   *
   * @param descriptor  The plugin descriptor which the receiver is made from.
   */
  public SamplePlugin() {
    super();
    if (inst == null) {
      inst = this;
      getBundle().getSymbolicName();
    }
  }
  
  /**
   * Gets the sample plugin singleton instance.
   *
   * @return the default instance of the sample plugin.
   */
  static public SamplePlugin getDefault() {
    return inst;
  }

  /**
   * Gets the symbolic name of the plugin, as set in the MANIFEST.MF 
   * @return
   */
  public static String getSymbolicName() {
    return getDefault().getBundle().getSymbolicName();
  }
  
}
