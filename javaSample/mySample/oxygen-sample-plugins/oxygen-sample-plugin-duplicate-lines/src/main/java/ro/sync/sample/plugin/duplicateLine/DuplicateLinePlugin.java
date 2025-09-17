/*
 * Copyright (c) 2018 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */
package ro.sync.sample.plugin.duplicateLine;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * @author bogdan_cercelaru
 *
 */
public class DuplicateLinePlugin extends Plugin {

  private static DuplicateLinePlugin instance = null;

  /** @link dependency 
   * @label has a*/
  /*#CommentPluginExtension lnkCommentPluginExtension;*/

  /**
   *  CommentPlugin.
   * 
   *@param descriptor Plugin descriptor.
   */
  public DuplicateLinePlugin(PluginDescriptor descriptor) {
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
  public static DuplicateLinePlugin getInstance() {
    return instance;
  }
}
