package simple.documentation.framework.listeners;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorMouseEvent;
import ro.sync.ecss.extensions.api.AuthorMouseListener;
import ro.sync.exml.workspace.api.Platform;
import simple.documentation.framework.ui.SDFPopupWindow;

/**
 * Custom author mouse listener used to display the mouse coordinates in a popup window 
 * on mouse clicked.
 *
 */
public class SDFAuthorMouseListener implements AuthorMouseListener {

  /**
   * Access to the author specific functions. 
   */
  private AuthorAccess authorAccess;

  /**
   * The popup used to display the mouse coordinates.
   */
  private SDFPopupWindow popupWindow;

  /**
   * Constructor. 
   * 
   * @param authorAccess Access to the author specific functions  
   */
  public SDFAuthorMouseListener(AuthorAccess authorAccess) {
    this.authorAccess = authorAccess;
    // Use the information popup only if this is the standalone Oxygen version.
    if (authorAccess.getWorkspaceAccess().getPlatform() == Platform.STANDALONE) {
      popupWindow = new SDFPopupWindow(authorAccess, "Position");
    } 
  }


  public void mouseClicked(AuthorMouseEvent e) {
    // Display the mouse coordinates.
    if (authorAccess.getWorkspaceAccess().getPlatform() == Platform.STANDALONE) {
      if (e.clickCount == 2) {
        String toDisplay = "X: " + e.X + " Y: " + e.Y;
        popupWindow.setTimeToDisplay(2);
        popupWindow.display(toDisplay, e.X, e.Y, 10);
      }
    }
  }

  public void mousePressed(AuthorMouseEvent e) {
  }

  public void mouseReleased(AuthorMouseEvent e) {
  }

  public void mouseDragged(AuthorMouseEvent e) {
  }

  public void mouseMoved(AuthorMouseEvent e) {
  }
}
