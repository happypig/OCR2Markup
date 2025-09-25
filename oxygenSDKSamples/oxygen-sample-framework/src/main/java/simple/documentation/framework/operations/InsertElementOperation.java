package simple.documentation.framework.operations;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;

import ro.sync.contentcompletion.xml.CIElement;
import ro.sync.contentcompletion.xml.WhatElementsCanGoHereContext;
import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.AuthorSchemaManager;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.exml.view.graphics.Point;
import ro.sync.exml.view.graphics.Rectangle;

/**
 * Show a popup menu that contains the name of all elements that can be inserted at the caret offset.
 */
public class InsertElementOperation implements AuthorOperation {

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  /** 
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap) 
   */ 
  @SuppressWarnings("serial")
  public void doOperation(final AuthorAccess authorAccess, ArgumentsMap args) 
      throws IllegalArgumentException, AuthorOperationException { 
    try { 
      //Get the caret offset 
      final int caretOffset = authorAccess.getEditorAccess().getCaretOffset(); 
      //The schema manager 
      final AuthorSchemaManager schemaManager = authorAccess.getDocumentController().getAuthorSchemaManager(); 
      //The context of elements. 
      WhatElementsCanGoHereContext ctxt = schemaManager.createWhatElementsCanGoHereContext(caretOffset); 
      //Get the list of elements which can be inserted here 
      final List<CIElement> childrenElements = schemaManager.getChildrenElements(ctxt); 
      JPopupMenu jpo = new JPopupMenu(); 
      for (int i = 0; i < childrenElements.size(); i++) { 
        final int index = i; 
        jpo.add(new JMenuItem(new AbstractAction(childrenElements.get(index).getName()) { 
          public void actionPerformed(ActionEvent e) { 
            CIElement toInsert = childrenElements.get(index); 
            try { 
              //The CIElement contains all data necessary to make a small XML fragment with 
              //the string to insert and then call 
              // authorAccess.getDocumentController().createNewDocumentFragmentInContext(xmlFragment, caretOffset); 
              //But Oxygen 11.2 will come with a easier method: 
              //Create a document fragment from the CIElement. 
              AuthorDocumentFragment frag = schemaManager.createAuthorDocumentFragment(toInsert); 
              //Now you can process the fragment and remove/add attributes. 
              authorAccess.getDocumentController().insertFragment(caretOffset, frag); 
            } catch (BadLocationException e1) { 
              e1.printStackTrace(); 
            } 
          } 
        })); 
      } 
      Rectangle mtv = authorAccess.getEditorAccess().modelToViewRectangle(caretOffset); 
      Point popupLocation = authorAccess.getEditorAccess().getLocationOnScreenAsPoint(mtv.x, mtv.y);
      jpo.setLocation(popupLocation.x, popupLocation.y);
      JFrame oxygenFrame = (JFrame)authorAccess.getWorkspaceAccess().getParentFrame();
      
      // Get the author component
      JPanel authorComponent = (JPanel)authorAccess.getEditorAccess().getAuthorComponent();
      // Get the glass pane
      final Component glassPane = authorComponent.getRootPane().getGlassPane();
      if (glassPane != null) {
        glassPane.setVisible(true);
        // Set wait cursor
        glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      }
      
      // Show popup menu
      jpo.show(oxygenFrame, popupLocation.x - oxygenFrame.getLocation().x, popupLocation.y - oxygenFrame.getLocation().y); 
      
      // Add a popup menu listener
      jpo.addPopupMenuListener(new PopupMenuListener() {
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        }
        
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
          // Reset cursor to default
          if (glassPane != null) {
            glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            glassPane.setVisible(false);
          }
        }
        
        public void popupMenuCanceled(PopupMenuEvent e) {
        }
      });
      
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
    return "Insert element at the caret position.";
  }

}
