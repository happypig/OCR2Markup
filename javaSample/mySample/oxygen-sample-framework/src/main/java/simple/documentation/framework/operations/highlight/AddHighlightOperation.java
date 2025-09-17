package simple.documentation.framework.operations.highlight;

import java.util.LinkedHashMap;

import javax.swing.JFrame;
import javax.swing.text.BadLocationException;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlighter;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;

/**
 * Operation used to highlight element from the caret position.
 */
public class AddHighlightOperation implements AuthorOperation {

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap map) throws IllegalArgumentException,
      AuthorOperationException {
    // Show dialog for adding highlight comment
    AuthorPersistentHighlighter highlighter = authorAccess.getEditorAccess().getPersistentHighlighter();
    EditHighlightsDialog commentDlg = new EditHighlightsDialog(
        (JFrame) authorAccess.getWorkspaceAccess().getParentFrame(), 
        "Add highlight comment", 
        true, 
        null, 
        authorAccess);
    commentDlg.showDialog();
    if (commentDlg.getResult() == OKCancelDialog.RESULT_OK) {
      int caretOffset = authorAccess.getEditorAccess().getCaretOffset();
      try {
        // Highlight the node at caret
        AuthorNode nodeAtCaret = authorAccess.getDocumentController().getNodeAtOffset(caretOffset);
        String authorName = authorAccess.getReviewController().getReviewerAuthorName();
        String timestamp = authorAccess.getReviewController().getCurrentTimestamp();
        
        // Compose highlight properties
        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        properties.put(HighlightProperties.ID, timestamp);
        properties.put(HighlightProperties.AUTHOR, authorName);
        properties.put(HighlightProperties.COMMENT, commentDlg.getComment());
        // Add highlight
        highlighter.addHighlight(nodeAtCaret.getStartOffset(), nodeAtCaret.getEndOffset(), properties);
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }
  }
  
  /**
   * @see ro.sync.ecss.extensions.api.Extension#getDescription()
   */
  public String getDescription() {
    return "Highlight element from the caret position.";
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#getArguments()
   */
  public ArgumentDescriptor[] getArguments() {
    return null;
  }

}