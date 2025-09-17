package DILA.eclipse.action;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.access.AuthorWorkspaceAccess;
import ro.sync.ecss.extensions.api.node.AttrValue;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;

/**
 * Action that copies all the attributes from a node selected in the Outline view shown for the Author page.
 * 
 * @author adrian_sorop
 */
public class CopyAttributes extends Action {
  
  /**
   * Provides access to Oxygen's Author API.
   */
  private AuthorAccess authorAccess;

  /**
   * Constructor.
   * 
   * @param authorAccess Provides access to Oxygen's Author API.
   */
  public CopyAttributes(AuthorAccess authorAccess) {
    this.authorAccess = authorAccess;
    setText("Copy Attributes");
  }
  
  /**
   * Run action.
   */
  @Override
  public void run() {
    StringBuilder attributes = collectAttributesFromSelectedNode();
    if (attributes.length() > 0) {
      setFragmentToClipboard(attributes.toString());
    } else {
      AuthorWorkspaceAccess workspaceAccess = authorAccess.getWorkspaceAccess();
      workspaceAccess.showInformationMessage("Nothing to copy.");
    }
  }
  
  /**
   * Gets the fully selected node and collects all its attributes.
   * 
   * @return All attributes.
   */
  private StringBuilder collectAttributesFromSelectedNode() {
    StringBuilder attributes = new StringBuilder();
    
    AuthorNode fullySelectedNode = authorAccess.getEditorAccess().getFullySelectedNode();
    if (fullySelectedNode instanceof AuthorElement) {
      AuthorElement el = (AuthorElement) fullySelectedNode;
      int attributesCount = el.getAttributesCount();
      for(int i = 0; i < attributesCount; i++) {
        String attributeAtIndex = el.getAttributeAtIndex(i);
        AttrValue attribute = el.getAttribute(attributeAtIndex);
        if (attribute != null && attribute.getValue() != null 
            // Skip default attributes.
            && attribute.isSpecified()) {
          attributes.append(attributeAtIndex);
          attributes.append("=\"");
          attributes.append(attribute.getValue());
          attributes.append("\"");
        }
        if (i != attributesCount - 1) {
          attributes.append(" ");
        }
      }
    }
    return attributes;
  }
   
  /**
   * Set a fragment to clipboard.
   * 
   * @param textToSet    The text fragment to set.
   */
  private static void setFragmentToClipboard(final String textToSet) {
    Clipboard clipboard = new Clipboard(Display.getCurrent());
    TextTransfer textTransfer = TextTransfer.getInstance();
    clipboard.setContents(new String[] { textToSet }, new Transfer[] { textTransfer });
    clipboard.dispose();
  }
}
