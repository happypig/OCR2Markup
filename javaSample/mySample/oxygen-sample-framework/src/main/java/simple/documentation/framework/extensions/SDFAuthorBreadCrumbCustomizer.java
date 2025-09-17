package simple.documentation.framework.extensions;

import javax.swing.JPopupMenu;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.api.structure.AuthorBreadCrumbCustomizer;
import ro.sync.ecss.extensions.api.structure.RenderingInformation;

/**
 * Simple Document Framework Author customizer used for custom nodes rendering
 * in the Breadcrumb. 
 */
public class SDFAuthorBreadCrumbCustomizer extends AuthorBreadCrumbCustomizer {

  /**
   * For every node render its name in the Outline (with the first letter capitalized).
   * Set 'Paragraph' as render for 'para' nodes 
   */
  @Override
  public void customizeRenderingInformation(RenderingInformation renderInfo) {
    // Get the node to be rendered
    AuthorNode node = renderInfo.getNode();

    String name = node.getName();
    if (name.length() > 0) {
      if ("para".equals(name)) {
        name = "Paragraph";
      } else {
        if(renderInfo.getNode().getName().equals("customcol")) {
          // Do not display the customcol elements in the BreadCrumb
          name = null;
        } else {
          // Capitalize the first letter
          name = name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
        }
      }
    }

    if (name == null) {
      renderInfo.setIgnoreNodeFromDisplay(true);
    } else {
      // Set the render text
      renderInfo.setRenderedText("[" + name + "]");
    }
  }

  /**
   * Don't show the popUp menu for the bread crumb. 
   */
  @Override
  public void customizePopUpMenu(Object popUp, AuthorAccess authorAccess) {
    if (popUp instanceof JPopupMenu) {
      JPopupMenu popupMenu = (JPopupMenu)popUp;
      // Remove all the components from the popUp menu .
      popupMenu.removeAll();
    }
  }
  
}
