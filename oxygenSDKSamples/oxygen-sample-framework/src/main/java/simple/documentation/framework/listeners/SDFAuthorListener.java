package simple.documentation.framework.listeners;

import java.util.List;

import ro.sync.ecss.extensions.api.AttributeChangedEvent;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorListenerAdapter;
import ro.sync.ecss.extensions.api.DocumentContentDeletedEvent;
import ro.sync.ecss.extensions.api.DocumentContentInsertedEvent;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;

/**
 * Simple Documentation Framework Author listener. 
 */
public class SDFAuthorListener extends AuthorListenerAdapter {

  /**
   * Access to the author specific functions. 
   */
  private AuthorAccess authorAccess;

  /**
   * Constructor. 
   * 
   * @param authorAccess Access to the author specific functions  
   */
  public SDFAuthorListener(AuthorAccess authorAccess) {
    this.authorAccess = authorAccess;
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorListenerAdapter#attributeChanged(ro.sync.ecss.extensions.api.AttributeChangedEvent)
   */
  @Override
  public void attributeChanged(AttributeChangedEvent e) {
    String strictMode = authorAccess.getOptionsStorage().getOption("strictMode", "false");
    if ("true".equals(strictMode)) {
      // If the changed attribute is the "column_span" or "row_span" attribute of the "td" 
      // element then verify if the new value is an integer value
      AuthorNode ownerAuthorNode = e.getOwnerAuthorNode(); 
      if (ownerAuthorNode instanceof AuthorElement) {
        AuthorElement ownerAuthorElement = (AuthorElement) ownerAuthorNode; 
        if ("td".equals(ownerAuthorElement.getLocalName())) {
          String attributeName = e.getAttributeName();
          if ("column_span".equals(attributeName) || "row_span".equals(attributeName)) {
            String spanValue = ownerAuthorElement.getAttribute(attributeName).getValue();
            try {
              Integer.parseInt(spanValue);
            } catch (NumberFormatException ex) {
              authorAccess.getWorkspaceAccess().showInformationMessage("The value " + spanValue + " of attribute " + attributeName +
              " is not valid.");
            }
          }
        }
      }
    }
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorListenerAdapter#beforeContentDelete(ro.sync.ecss.extensions.api.DocumentContentDeletedEvent)
   */
  @Override
  public void beforeContentDelete(DocumentContentDeletedEvent deleteEvent) {
    String strictMode = authorAccess.getOptionsStorage().getOption("strictMode", "false");
    if ("true".equals(strictMode)) {
      // If the section title is deleted an error message will inform the user that the title
      // is required.
      if (deleteEvent.getType() != DocumentContentDeletedEvent.INSERT_TEXT_EVENT
          && deleteEvent.getType() != DocumentContentDeletedEvent.DELETE_TEXT_EVENT) {
        AuthorNode changedNode = deleteEvent.getParentNode();
        if (changedNode instanceof AuthorElement) {
          AuthorElement changedElement = (AuthorElement) changedNode;
          // Section element
          if ("section".equals(changedElement.getLocalName())) {
            AuthorElement[] titleElements = changedElement.getElementsByLocalName("title");
            // If the section has one "title" child element
            if (titleElements.length == 1) {
              // Find if the deleted element is the "title" one.
              AuthorDocumentFragment deletedFragment = deleteEvent.getDeletedFragment();
              List<AuthorNode> contentNodes = deletedFragment.getContentNodes();
              for (AuthorNode authorNode : contentNodes) {
                if (authorNode instanceof AuthorElement) {
                  if ("title".equals(((AuthorElement)authorNode).getLocalName())) {
                    String errorMessage = "The section must have a title.";
                    authorAccess.getWorkspaceAccess().showErrorMessage(errorMessage);
                  }
                }
              }
            }
          }
        }
      }
    }
    super.beforeContentDelete(deleteEvent);
  }
  
  /**
   * @see ro.sync.ecss.extensions.api.AuthorListenerAdapter#contentInserted(ro.sync.ecss.extensions.api.DocumentContentInsertedEvent)
   */
  @Override
  public void contentInserted(DocumentContentInsertedEvent e) {
    AuthorNode node = e.getParentNode();
    AuthorNode parentNode = node.getParent();
    // For 'section' nodes the title text is rendered in the Outline
    // (see customizeRenderingInformation method from SDFAuthorOutlineCustomizer)
    // so we need to refresh the section node from the Outline when the title 
    // text has changed.
    if ("title".equals(node.getName()) && "section".equals(parentNode.getName())) {
      authorAccess.getOutlineAccess().refreshNodes(new AuthorNode[] {parentNode});
    }
  }
  
  /**
   * @see ro.sync.ecss.extensions.api.AuthorListenerAdapter#contentDeleted(ro.sync.ecss.extensions.api.DocumentContentDeletedEvent)
   */
  @Override
  public void contentDeleted(DocumentContentDeletedEvent e) {
    AuthorNode node = e.getParentNode();
    AuthorNode parentNode = node.getParent();
    // For 'section' nodes the title text is rendered in the Outline
    // (see customizeRenderingInformation method from SDFAuthorOutlineCustomizer)
    // so we need to refresh the section node from the Outline when the title 
    // text has changed.
    if ("title".equals(node.getName()) && "section".equals(parentNode.getName())) {
      authorAccess.getOutlineAccess().refreshNodes(new AuthorNode[] {parentNode});
    }
  }
}
