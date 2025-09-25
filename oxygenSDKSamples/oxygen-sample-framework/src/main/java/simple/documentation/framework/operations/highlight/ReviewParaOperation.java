package simple.documentation.framework.operations.highlight;

import java.awt.Component;
import java.util.LinkedHashMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.highlights.AuthorHighlighter;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlighter;
import ro.sync.ecss.extensions.api.highlights.ColorHighlightPainter;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;
import ro.sync.exml.view.graphics.Color;


/**
 * Operation used to highlight a paragraph as reviewed.
 */
public class ReviewParaOperation implements AuthorOperation {
  /**
   * Highlight author.
   */
  static final String AUTHOR = "author";
  /**
   * Highlight ID.
   */
  static final String ID = "id";
  /**
   * Highlight comment.
   */
  static final String COMMENT = "comment";

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap map) throws IllegalArgumentException,
  AuthorOperationException {
    // Highlight the selected paragraph if any.
    AuthorNode selectedNode = authorAccess.getEditorAccess().getFullySelectedNode();
    if (selectedNode != null) {
      // Show dialog for adding highlight comment
      AuthorPersistentHighlighter persistentHighlighter = authorAccess.getEditorAccess().getPersistentHighlighter();
      AuthorHighlighter highlighter = authorAccess.getEditorAccess().getHighlighter();
      EditHighlightsDialog commentDlg =  new EditHighlightsDialog(
          (JFrame) authorAccess.getWorkspaceAccess().getParentFrame(), 
          "Review Paragraph", 
          true, 
          null, 
          authorAccess);
      commentDlg.showDialog();
      if (commentDlg.getResult() == OKCancelDialog.RESULT_OK) {

        // Get author name and timestamp.
        String authorName = authorAccess.getReviewController().getReviewerAuthorName();
        String timestamp = authorAccess.getReviewController().getCurrentTimestamp();

        // Compose highlight properties
        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        properties.put(ID, timestamp);
        properties.put(AUTHOR, authorName);
        String comment = commentDlg.getComment();
        properties.put(COMMENT, comment);
        int startOffset = selectedNode.getStartOffset();
        int endOffset = selectedNode.getEndOffset();
        if (comment != null && comment.trim().length() > 0) {
          // Add a persistent highlight
          persistentHighlighter.addHighlight(startOffset, endOffset, properties);
        } else {
          // Add non-persistent highlight

          ColorHighlightPainter painter = new ColorHighlightPainter();
          painter.setTextForegroundColor(Color.COLOR_RED_DARKER);

          try {
            highlighter.addHighlight(startOffset,  endOffset, painter, null);
          } catch (BadLocationException e) {
            e.printStackTrace();
          }
        }
      }
    } else {
      JOptionPane.showMessageDialog(
          (Component) authorAccess.getWorkspaceAccess().getParentFrame(), 
          "Select the whole paragraph!!!");
    }
  }

  /**
   * @see ro.sync.ecss.extensions.api.Extension#getDescription()
   */
  public String getDescription() {
    return "Review selected paragraph.";
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#getArguments()
   */
  public ArgumentDescriptor[] getArguments() {
    return null;
  }
}