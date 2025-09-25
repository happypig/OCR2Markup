package simple.documentation.framework.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.IllegalComponentStateException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JWindow;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.exml.view.graphics.Point;

/**
 * Popup window used to display Simple Documentation Framework specific information.
 *
 */
@SuppressWarnings("serial")
public class SDFPopupWindow extends JWindow {

  /**
   * Text area used to display useful informations.
   */
  private JTextArea infoTextArea;

  /**
   * Access to the author specific functions. 
   */
  AuthorAccess authorAccess;

  /**
   * The display time of the popup window (seconds).
   */
  private int timeToDisplay;

  /**
   * 
   * @param access Author access.
   * @param infoDescription Description.
   */
  public SDFPopupWindow(AuthorAccess access, String infoDescription) {
    super((JFrame) access.getWorkspaceAccess().getParentFrame());
    this.authorAccess = access;

    // Create information text area.
    infoTextArea = new JTextArea();
    infoTextArea.setLineWrap(true);
    infoTextArea.setWrapStyleWord(true);
    infoTextArea.setEditable(false);
    infoTextArea.setFocusable(false);

    JPanel mainContent = new JPanel(new BorderLayout());
    if (infoDescription != null) {
      mainContent.add(new JLabel(infoDescription), BorderLayout.NORTH);
    }
    mainContent.setFocusable(false);
    mainContent.add(infoTextArea, BorderLayout.SOUTH);
    mainContent.setBorder(BorderFactory.createLineBorder(Color.black));
    getContentPane().add(mainContent);
    setVisible(false);
  }

  /**
   * Set the time to display this popup window.
   * 
   * @param timeToDisplay The display time in seconds.
   */
  public void setTimeToDisplay(int timeToDisplay) {
    this.timeToDisplay = timeToDisplay;
  }

  /**
   * Display the specified text information.
   * 
   * @param text	The text to be displayed in the popup window.
   * @param relX 	The "x" coordinate relative to the viewport.
   * @param relY 	The "y" coordinate relative to the viewport.
   * @param delta	The translation point where the popup should be displayed from the given (x, y) point. 
   */
  public void display(String text, int relX, int relY, int delta) {
    // Transform the given relative coordinates into absolute coordinates. 
    try {
      Point translatedPoint = authorAccess.getEditorAccess().getLocationOnScreenAsPoint(relX, relY);
      setVisible(false);
      infoTextArea.setText(text);
      setLocation(translatedPoint.x + delta, translatedPoint.y + delta);
      pack();
      // Show the information popup window
      setVisible(true);

      // Hide the window when the given display time is finished.
      if (timeToDisplay > 0) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            setVisible(false);
          }
        }, timeToDisplay * 1000);
      }
    } catch (IllegalComponentStateException e) {
      // Do nothing
    }
  }
}
