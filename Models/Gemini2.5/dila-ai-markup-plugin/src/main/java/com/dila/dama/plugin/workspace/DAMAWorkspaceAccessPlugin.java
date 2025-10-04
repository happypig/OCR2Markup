package com.dila.dama.plugin.workspace;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * DAMA Workspace Access plugin.
 */
public class DAMAWorkspaceAccessPlugin extends Plugin {
	/**
	 * The static plugin instance.
	 */
	private static DAMAWorkspaceAccessPlugin instance = null;

	/**
	 * Constructs the plugin.
	 * 
	 * @param descriptor
	 *            The plugin descriptor
	 */
	public DAMAWorkspaceAccessPlugin(PluginDescriptor descriptor) {
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
	public static DAMAWorkspaceAccessPlugin getInstance() {
		return instance;
	}
}