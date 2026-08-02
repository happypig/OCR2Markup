package com.dila.dama.plugin.workspace;

import org.junit.Test;
import org.mockito.Mockito;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;

import javax.swing.text.PlainDocument;
import javax.swing.undo.UndoManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * T021 — applying CBRD Parse markup replaces the selection and is undoable
 * (FR-008, FR-009, US1 scenario 3). Mirrors {@code RefToLinkReplaceFlowTest}.
 */
public class AIMarkupReplaceUndoTest {

    private static final String ORIGINAL = "(T 1442)，大正23，頁869中";
    private static final String MARKUP =
        "<ref>(<canon>T</canon> <w>1442</w>)，大正<v>23</v>，頁<p>869</p><c>中</c></ref>";

    @Test
    public void applyingMarkupReplacesTheSelectedReferenceText() throws Exception {
        PlainDocument document = documentContaining(ORIGINAL);
        WSTextEditorPage textPage = pageOver(document, ORIGINAL.length());

        boolean replaced = new DAMAWorkspaceAccessPluginExtension().replaceSelectionText(textPage, MARKUP);

        assertThat(replaced).isTrue();
        assertThat(document.getText(0, document.getLength())).isEqualTo(MARKUP);
    }

    @Test
    public void theChangeIsRecordedInUndoHistory() throws Exception {
        PlainDocument document = documentContaining(ORIGINAL);
        UndoManager undoManager = new UndoManager();
        document.addUndoableEditListener(undoManager);
        WSTextEditorPage textPage = pageOver(document, ORIGINAL.length());

        new DAMAWorkspaceAccessPluginExtension().replaceSelectionText(textPage, MARKUP);

        assertThat(undoManager.canUndo()).isTrue();
    }

    @Test
    public void undoingRestoresTheOriginalReferenceText() throws Exception {
        PlainDocument document = documentContaining(ORIGINAL);
        UndoManager undoManager = new UndoManager();
        document.addUndoableEditListener(undoManager);
        WSTextEditorPage textPage = pageOver(document, ORIGINAL.length());

        new DAMAWorkspaceAccessPluginExtension().replaceSelectionText(textPage, MARKUP);
        while (undoManager.canUndo()) {
            undoManager.undo();
        }

        assertThat(document.getText(0, document.getLength())).isEqualTo(ORIGINAL);
    }

    @Test
    public void nothingIsWrittenWhenThereIsNoSelection() throws Exception {
        PlainDocument document = documentContaining(ORIGINAL);
        WSTextEditorPage textPage = Mockito.mock(WSTextEditorPage.class);
        when(textPage.getSelectionStart()).thenReturn(4);
        when(textPage.getSelectionEnd()).thenReturn(4);
        when(textPage.getDocument()).thenReturn(document);

        boolean replaced = new DAMAWorkspaceAccessPluginExtension().replaceSelectionText(textPage, MARKUP);

        assertThat(replaced).isFalse();
        assertThat(document.getText(0, document.getLength())).isEqualTo(ORIGINAL);
    }

    private static PlainDocument documentContaining(String text) throws Exception {
        PlainDocument document = new PlainDocument();
        document.insertString(0, text, null);
        return document;
    }

    private static WSTextEditorPage pageOver(PlainDocument document, int selectionEnd) {
        WSTextEditorPage textPage = Mockito.mock(WSTextEditorPage.class);
        when(textPage.getSelectionStart()).thenReturn(0);
        when(textPage.getSelectionEnd()).thenReturn(selectionEnd);
        when(textPage.getCaretOffset()).thenReturn(0);
        when(textPage.getDocument()).thenReturn(document);
        doAnswer(invocation -> {
            document.remove(0, document.getLength());
            return null;
        }).when(textPage).deleteSelection();
        return textPage;
    }
}
