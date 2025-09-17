package simple.documentation.framework.operations.highlight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlighter;
import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;

/**
 * Operation used to edit the highlights from the caret position.
 */
public class EditHighlightsOperation implements AuthorOperation {

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap args)
      throws IllegalArgumentException, AuthorOperationException {
    AuthorPersistentHighlighter highlighter = authorAccess.getEditorAccess().getPersistentHighlighter();
    AuthorPersistentHighlight[] highlights = highlighter.getHighlights();
    if (highlights.length > 0) {
      int caretOffset = authorAccess.getEditorAccess().getCaretOffset();
      List<AuthorPersistentHighlight> caretHighlights = new ArrayList<AuthorPersistentHighlight>();
      // Remove highlights from the caret position
      for (AuthorPersistentHighlight highlight : highlights) {
        // Get the highlights from the caret position
        if (highlight.getStartOffset() <= caretOffset && highlight.getEndOffset() >= caretOffset) {
          caretHighlights.add(highlight);
        }
      }
      
      if (caretHighlights.size() > 0) {
        // Show edit highlights dialog
        EditHighlightsDialog commentDlg = new EditHighlightsDialog(
            (JFrame) authorAccess.getWorkspaceAccess().getParentFrame(), 
            "Add highlight comment", 
            true, 
            caretHighlights, 
            authorAccess);
        commentDlg.showDialog();
        // Save edited highlights if dialog result is OK
        if (commentDlg.getResult() == OKCancelDialog.RESULT_OK) {
          Map<AuthorPersistentHighlight, LinkedHashMap<String, String>> mapHighlightsToProps = commentDlg.getMapHighlightsToProps();
          Set<AuthorPersistentHighlight> highlightsSet = mapHighlightsToProps.keySet();
          for (AuthorPersistentHighlight h : highlightsSet) {
            // Save edited properties
            highlighter.setProperties(h, mapHighlightsToProps.get(h));
          }
        }
      }
    }
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#getArguments()
   */
  public ArgumentDescriptor[] getArguments() {
    return null;
  }

  /**
   * @see ro.sync.ecss.extensions.api.Extension#getDescription()
   */
  public String getDescription() {
    return "Edit Highlights from the caret position";
  }
  
}
