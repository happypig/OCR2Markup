package com.dila.dama.plugin.workspace;

import com.dila.dama.plugin.domain.service.RefElementRewriter;
import org.junit.Test;
import org.mockito.Mockito;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;

import javax.swing.text.PlainDocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class RefToLinkReplaceFlowTest {

    @Test
    public void replaceSelectionText_insertsRewrittenRef() throws Exception {
        String original = "<ref><canon>T</canon><v>25</v><p>917</p><ptr href=\"old\"/></ref>";
        String url = "https://cbetaonline.dila.edu.tw/T1514";

        RefElementRewriter rewriter = new RefElementRewriter();
        String replacement = rewriter.rewrite(original, url);

        PlainDocument document = new PlainDocument();
        document.insertString(0, original, null);

        WSTextEditorPage textPage = Mockito.mock(WSTextEditorPage.class);
        when(textPage.getSelectionStart()).thenReturn(0);
        when(textPage.getSelectionEnd()).thenReturn(original.length());
        when(textPage.getCaretOffset()).thenReturn(0);
        when(textPage.getDocument()).thenReturn(document);

        doAnswer(invocation -> {
            document.remove(0, document.getLength());
            return null;
        }).when(textPage).deleteSelection();

        DAMAWorkspaceAccessPluginExtension plugin = new DAMAWorkspaceAccessPluginExtension();
        boolean replaced = plugin.replaceSelectionText(textPage, replacement);

        assertThat(replaced).isTrue();
        String updated = document.getText(0, document.getLength());
        assertThat(updated).contains("checked=\"2\"");
        assertThat(updated).contains("href=\"" + url + "\"");
        assertThat(updated).doesNotContain("href=\"old\"");
    }
}
