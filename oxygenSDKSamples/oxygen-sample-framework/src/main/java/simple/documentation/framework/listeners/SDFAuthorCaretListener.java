package simple.documentation.framework.listeners;

import javax.swing.text.BadLocationException;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorCaretEvent;
import ro.sync.ecss.extensions.api.AuthorCaretListener;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.exml.view.graphics.Rectangle;
import ro.sync.exml.workspace.api.Platform;
import simple.documentation.framework.ui.SDFPopupWindow;

/**
 * Author caret listener used to display the XML fragment corresponding to a para element 
 * found at the caret position.
 *
 */
public class SDFAuthorCaretListener implements AuthorCaretListener {

  /**
   * Access to the author specific functions. 
   */
  private AuthorAccess authorAcess;

  /**
   * The popup used to display the XML fragment.
   */
  private SDFPopupWindow popupWindow;

  /**
   * Constructor. 
   * 
   * @param authorAccess Access to the author specific functions.
   */
  public SDFAuthorCaretListener(AuthorAccess authorAccess) {
    this.authorAcess = authorAccess;
    // Use the information popup only if this is the standalone Oxygen version.
    if (authorAccess.getWorkspaceAccess().getPlatform() == Platform.STANDALONE) {
      // Create the information popup window.
      popupWindow = new SDFPopupWindow(authorAccess, "XML Fragment:");
    } 
  }

  /**
   * Caret moved. 
   * Display the XML fragment corresponding to a para element found at the caret 
   * position in a popup window. 
   */
  public void caretMoved(AuthorCaretEvent caretEvent) {
    // Verify if the node corresponding to the new caret position corresponds to a "para" element. 
    if (authorAcess.getWorkspaceAccess().getPlatform() == Platform.STANDALONE) { 
      int caretOffset = authorAcess.getEditorAccess().getCaretOffset();
      try {
        AuthorNode nodeAtOffset = authorAcess.getDocumentController().getNodeAtOffset(caretOffset);
        if (nodeAtOffset != null && nodeAtOffset instanceof AuthorElement) {
          AuthorElement element = (AuthorElement) nodeAtOffset;
          if ("para".equals(element.getLocalName())) {
            AuthorDocumentFragment paraFragment = authorAcess.getDocumentController().createDocumentFragment(element, true);
            String serializeFragmentToXML = authorAcess.getDocumentController().serializeFragmentToXML(paraFragment);
            // Find the x and y coordinates from the caret shape  (the popup window location).
            Rectangle modelToView = authorAcess.getEditorAccess().modelToViewRectangle(authorAcess.getEditorAccess().getCaretOffset());
            popupWindow.setTimeToDisplay(3);
            popupWindow.display(
                serializeFragmentToXML, 
                modelToView.x + modelToView.width, 
                modelToView.y + modelToView.height, 
                10);
          }
        }
      } catch (BadLocationException e) {
      }
    }
  }
}
