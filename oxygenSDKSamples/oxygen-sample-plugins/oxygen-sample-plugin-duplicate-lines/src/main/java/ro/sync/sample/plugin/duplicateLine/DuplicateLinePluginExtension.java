/*
 * Copyright (c) 2018 Syncro Soft SRL - All Rights Reserved.
 *
 * This file contains proprietary and confidential source code.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */
package ro.sync.sample.plugin.duplicateLine;

import javax.swing.text.BadLocationException;

import ro.sync.exml.plugin.document.DocumentPluginContext;
import ro.sync.exml.plugin.document.DocumentPluginExtension;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
/**
 * @author bogdan_cercelaru
 *
 */
public abstract class DuplicateLinePluginExtension implements DocumentPluginExtension {
	protected void duplicateLine(final DocumentPluginContext context , final boolean isDown) {
		WSTextEditorPage textPage = (WSTextEditorPage) context.getTextPage();
		int caretOffset = textPage.getCaretOffset();

		textPage.beginCompoundUndoableEdit();
		try {
			int lineOfOffset = textPage.getLineOfOffset(caretOffset);
			int offsetOfLineStart = textPage.getOffsetOfLineStart(lineOfOffset);
			int offsetOfLineEnd = textPage.getOffsetOfLineEnd(lineOfOffset);
			// Get line
			String lineToDuplicate = textPage.getDocument().getText(offsetOfLineStart, offsetOfLineEnd - offsetOfLineStart - 1) + "\n";
			
			if (offsetOfLineEnd > textPage.getDocument().getLength()) {
				lineToDuplicate = lineToDuplicate.substring(0, offsetOfLineEnd - offsetOfLineStart - 1);
				
				offsetOfLineEnd = textPage.getDocument().getLength();
				textPage.getDocument().insertString(offsetOfLineEnd, "\n", null);
				offsetOfLineEnd++;
				if (isDown) {
					offsetOfLineStart++;
				}
			}
			
			// Duplicate line
			textPage.getDocument().insertString(offsetOfLineEnd, lineToDuplicate, null);

			if (isDown) {
				textPage.select(offsetOfLineEnd + (offsetOfLineEnd - offsetOfLineStart), offsetOfLineEnd);
			} else {
				textPage.select(offsetOfLineEnd, offsetOfLineStart);
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
		} finally {
			textPage.endCompoundUndoableEdit();
		}
	}
}
