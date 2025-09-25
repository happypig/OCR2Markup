package simple.documentation.framework.filters;

import javax.swing.text.BadLocationException;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorDocumentFilter;
import ro.sync.ecss.extensions.api.AuthorDocumentFilterBypass;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;

/**
 * Simple Documentation Framework document filter used to restrict insertion and deletion of 
 * different nodes from the document when the strict mode is activated. 
 *
 */
public class SDFDocumentFilter extends AuthorDocumentFilter {

  /**
   * The author access.
   */
  private final AuthorAccess authorAccess;

  /**
   * Constructor.
   * 
   * @param access The author access.
   */
  public SDFDocumentFilter(AuthorAccess access) {
    this.authorAccess = access;
  }

  /**
   * Check if the strict mode is activated
   * @return <code>True</code> if the strict mode is activated.
   */
  private boolean isStrictModeActivated() {
    String strictMode = authorAccess.getOptionsStorage().getOption("strictMode", "false");
    return "true".equals(strictMode);
  }

  /**
   * Insert node filter.
   */
  @Override
  public boolean insertNode(AuthorDocumentFilterBypass filterBypass,
      int caretOffset, AuthorNode element) {
    // Restrict the insertion of the "title" element if the parent element already contains a 
    // title element.
    if (isStrictModeActivated()) {
      String restrict = "title";
      if(element instanceof AuthorElement) {
        String elementName = ((AuthorElement) element).getLocalName();
        if (restrict.equals(elementName)) {
          try {
            AuthorNode nodeAtOffset = authorAccess.getDocumentController().getNodeAtOffset(caretOffset);
            if (nodeAtOffset != null && nodeAtOffset instanceof AuthorElement) {
              AuthorElement[] elements = ((AuthorElement) nodeAtOffset).getElementsByLocalName(restrict);
              if (elements != null && elements.length > 0) {
                AuthorElement titleChild = elements[0];
                if (titleChild != null) {
                  authorAccess.getWorkspaceAccess().showInformationMessage("Title already added.");
                  return false;
                }
              }
            }
          } catch (BadLocationException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return super.insertNode(filterBypass, caretOffset, element);
  }

  /**
   * Insert text filter.
   */
  @Override
  public void insertText(AuthorDocumentFilterBypass filterBypass, int caretOffset,
      String toInsert) {
    super.insertText(filterBypass, caretOffset, toInsert);
    // If the strict mode is activated and the element where the text is inserted is the "content"
    // element then surround the inserted text into a "para" element.  
    if (isStrictModeActivated()) {
      try {
        AuthorNode nodeAtOffset = authorAccess.getDocumentController().getNodeAtOffset(caretOffset);
        if (nodeAtOffset != null && nodeAtOffset instanceof AuthorElement) {
          if ("content".equals(((AuthorElement)nodeAtOffset).getLocalName())) {
            try {
              filterBypass.surroundInFragment("<para/>", caretOffset, caretOffset + toInsert.length() - 1);
              authorAccess.getEditorAccess().setCaretPosition(caretOffset + toInsert.length() + 1);
            } catch (AuthorOperationException e) {
              e.printStackTrace();
            }
          }
        }
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }

  }
}
