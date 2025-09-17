package simple.documentation.framework.extensions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.tree.TreePath;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.api.structure.AuthorOutlineCustomizer;
import ro.sync.ecss.extensions.api.structure.RenderingInformation;

/**
 * Simple Document Framework Author Outline customizer used for custom filtering and nodes rendering
 * in the Outline. 
 */
public class SDFAuthorOutlineCustomizer extends AuthorOutlineCustomizer {

  /**
   * Ignore 'b' nodes.
   */
  @Override
  public boolean ignoreNode(AuthorNode node) {
    if (node.getName().equals("b")) {
      return true;
    }
    // Default behavior
    return false;
  }

  /**
   * For every node render its name in the Outline (with the first letter capitalized).
   * Set 'Paragraph' as render for 'para' nodes 
   * Render the title text for 'section' nodes and add 'Section' as additional info and tooltip.
   */
  @Override
  public void customizeRenderingInformation(
      RenderingInformation renderInfo) {
    // Get the node to be rendered
    AuthorNode node = renderInfo.getNode();

    String name = node.getName();
    if (name.length() > 0) {
      if ("para".equals(name)) {
        name = "Paragraph";
      } else {
        // Capitalize the first letter
        name = name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
      }
    }
    
    // Render additional attribute value
    String additionalAttrValue = renderInfo.getAdditionalRenderedAttributeValue();
    if (additionalAttrValue != null && additionalAttrValue.length() > 0) {
      renderInfo.setAdditionalRenderedAttributeValue("[" + additionalAttrValue + "]");
    }
    
    // Set the render text
    renderInfo.setRenderedText("[" + name + "]");

    // Render the title text in the 'section' additional info.
    if (node.getName().equals("section")) {
      if (node instanceof AuthorElement) {
        // Get the 'section' content nodes
        List<AuthorNode> contentNodes = ((AuthorElement) node).getContentNodes();
        if (contentNodes.size() > 0) {
          AuthorNode authorNode = contentNodes.get(0);
          if ("title".equals(authorNode.getName())) {
            try {
              // Render the title
              String textContent = authorNode.getTextContent();
              if (textContent != null && textContent.trim().length() > 0) {
                if (textContent.length() > 20) {
                  textContent = textContent.substring(0, 20);
                }
                renderInfo.setRenderedText(textContent);
                // Set 'Section' as additional info 
                renderInfo.setAdditionalRenderedText("[Section]");
                // Set the  tooltip text 
                renderInfo.setTooltipText(textContent);
              }
            } catch (BadLocationException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }

  /**
   * Customize the popUp menu.
   * If the selected node is a 'title' from 'section' remove the 'Delete' 
   * action from the popUp menu and add the 'Remove content' action if the selection
   * is single.
   */
  @Override
  public void customizePopUpMenu(Object popUp, final AuthorAccess authorAccess) {
    if (authorAccess != null) {
      if (popUp instanceof JPopupMenu) {
        JPopupMenu popupMenu = (JPopupMenu)popUp;
        // Find the selected paths from the Outliner 
        TreePath[] selectedPaths = authorAccess.getOutlineAccess().getSelectedPaths(true);
        for (TreePath treePath : selectedPaths) {
          Object[] authorNodesPath = treePath.getPath();
          if (authorNodesPath.length > 0) {
            // Find the selected node (last node from the list of path nodes)
            final AuthorNode selectedAuthorNode = (AuthorNode) authorNodesPath[authorNodesPath.length - 1];
            // Find if the current selected node is a title from section 
            if ("title".equals(selectedAuthorNode.getName()) && 
                "section".equals(selectedAuthorNode.getParent().getName())) {
              // Find the popUp item corresponding to the 'Delete' action
              Component[] components = popupMenu.getComponents();
              int deleteItemIndex = 0;
              for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JMenuItem) {
                  String itemText = ((JMenuItem) components[i]).getText();
                  if ("Delete".equals(itemText)) {
                    // The 'Remove Content' item will be added at the same index
                    deleteItemIndex = i;
                    // Remove the 'Delete' action
                    popupMenu.remove(deleteItemIndex);
                  }
                }
              }

              if (selectedPaths.length == 1) {
                // Add the 'Remove content' action
                JMenuItem sdfMenuItem = new JMenuItem("Remove Content"); 
                sdfMenuItem.addActionListener(new ActionListener() {
                  public void actionPerformed(ActionEvent arg0) {
                    // Remove title content
                    int startOffset = selectedAuthorNode.getStartOffset();
                    int endOffset = selectedAuthorNode.getEndOffset();
                    if (endOffset > startOffset + 1) {
                      authorAccess.getDocumentController().delete(
                          startOffset + 1, 
                          endOffset);
                    }
                  }
                });
                // Add the menu item in the menu 
                popupMenu.add(sdfMenuItem, deleteItemIndex);
              }
              break;
            }
          }
        } 
      }
    }
  }
}