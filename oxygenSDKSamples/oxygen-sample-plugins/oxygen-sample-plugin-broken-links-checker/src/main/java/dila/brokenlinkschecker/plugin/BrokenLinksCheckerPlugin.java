package dila.brokenlinkschecker.plugin;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * Broken links checker plugin.
 */
public class BrokenLinksCheckerPlugin extends Plugin {
	/**
	 * The static plugin instance.
	 */
	private static BrokenLinksCheckerPlugin instance = null;

	/**
	 * Constructs the plugin.
	 * 
	 * @param descriptor
	 *            The plugin descriptor
	 */
	public BrokenLinksCheckerPlugin(PluginDescriptor descriptor) {
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
	public static BrokenLinksCheckerPlugin getInstance() {
		return instance;
	}
}