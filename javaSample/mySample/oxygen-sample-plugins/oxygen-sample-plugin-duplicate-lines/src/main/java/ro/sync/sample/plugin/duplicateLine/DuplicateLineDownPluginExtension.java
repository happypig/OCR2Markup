/*
 * Copyright (c) 2018 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */
package ro.sync.sample.plugin.duplicateLine;

import ro.sync.exml.plugin.document.DocumentPluginContext;
import ro.sync.exml.plugin.document.DocumentPluginResult;
/**
 * @author bogdan_cercelaru
 *
 */
public class DuplicateLineDownPluginExtension extends DuplicateLinePluginExtension {
	@Override
	public DocumentPluginResult process(DocumentPluginContext context) {
		duplicateLine(context, true);
		return null;
  }
}

