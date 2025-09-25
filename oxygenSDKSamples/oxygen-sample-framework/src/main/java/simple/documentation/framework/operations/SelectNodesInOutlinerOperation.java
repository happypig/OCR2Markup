package simple.documentation.framework.operations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.tree.TreePath;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.node.AuthorDocument;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.api.structure.AuthorOutlineCustomizer;
import simple.documentation.framework.extensions.SDFAuthorOutlineCustomizer;

/**
 * Select in the Outline the children nodes of the node at caret.
 */
public class SelectNodesInOutlinerOperation implements AuthorOperation {

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap args)
      throws IllegalArgumentException, AuthorOperationException {
    //  Renderer customizer used to find if a given node is filtered in the Outline.
    AuthorOutlineCustomizer rendererCustomizer = new SDFAuthorOutlineCustomizer(); 
    
     int caretOffset = authorAccess.getEditorAccess().getCaretOffset();
     try {
       // Node at caret position
      AuthorNode currentNode = authorAccess.getDocumentController().getNodeAtOffset(caretOffset);
      if (currentNode instanceof AuthorElement) {
        AuthorElement currentElement = (AuthorElement)currentNode;
        // Find the content nodes
        List<AuthorNode> contentNodes = currentElement.getContentNodes();
        List<TreePath> selectTreePaths = new ArrayList<TreePath>();
        LinkedList<AuthorNode> reversedPath = findReversedPath(authorAccess, rendererCustomizer,
            currentNode);
        
        for (AuthorNode authorNode : contentNodes) {
          if (!rendererCustomizer.ignoreNode(authorNode)) {
            LinkedList<AuthorNode> pathList = new LinkedList<AuthorNode>(reversedPath);
            pathList.add(authorNode);
            // Add the children tree path in the selected tree paths list
            selectTreePaths.add(new TreePath(pathList.toArray(new AuthorNode[0])));
          }
        }
        
        // Select the children tree paths in the Outline
        authorAccess.getOutlineAccess().setSelectionPaths(selectTreePaths.toArray(new TreePath[0]));
      }
     } catch (BadLocationException e) {
       e.printStackTrace();
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
    return "Select nodes in the Outliner";
  }

  /**
   * Builds the list of node parents up to and including the root node, where the
   * original node is the last element in the returned array. 
   * 
   * @param authorAccess The author access.
   * @param rendererCustomizer Renderer customizer used to find if a given node 
   * is filtered in the Outline.
   * @param node The node to reverse path for.
   * @return The path nodes list.
   */
  private LinkedList<AuthorNode> findReversedPath(AuthorAccess authorAccess,
      AuthorOutlineCustomizer rendererCustomizer, AuthorNode node) {
    AuthorDocument document = authorAccess.getDocumentController().getAuthorDocumentNode();
    
    // Builds the path.
    LinkedList<AuthorNode> reversedPath = new LinkedList<AuthorNode>();
    while (node != null && !rendererCustomizer.ignoreNode(node)) {
      reversedPath.addFirst(node);
      if (node == document) {
        // Just added root.
        break;
      }
      node = node.getParent();
    }
    return reversedPath;
  }
}
