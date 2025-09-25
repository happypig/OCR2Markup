package simple.documentation.framework.operations.highlight;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight;
import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;

/**
 * Dialog used for adding or editing highlights.
 */
@SuppressWarnings("serial")
public class EditHighlightsDialog extends OKCancelDialog {
  /**
   * Information label (presents highlight ID and author).
   */
  private JLabel infoLabel;
  /**
   * Comment area.
   */
  private JTextArea commentArea;
  /**
   * Map between highlight and edited properties.
   */
  private Map<AuthorPersistentHighlight, LinkedHashMap<String, String>> mapHighlightsToProps;
  /**
   * Current edited highlight index.
   */
  private final int[] currentIndex = new int[1];
  /**
   * Current edited highlight.
   */
  private AuthorPersistentHighlight currentHighlight;
  /**
   * The Author access.
   */
  private final AuthorAccess authorAccess;

  /**
   * Constructor.
   * 
   * @param parentFrame The parent frame.
   * @param title The dialog title.
   * @param modal <code>true</code> if modal.
   * @param highlights List of highlights to be edited.
   * @param auhorAccess The Author access
   */
  public EditHighlightsDialog(
      JFrame parentFrame, 
      String title, 
      boolean modal, 
      final List<AuthorPersistentHighlight> highlights, 
      final AuthorAccess auhorAccess) {
    super(parentFrame, title, modal);
    this.authorAccess = auhorAccess;
    
    boolean editHighlights = highlights != null && highlights.size() > 0;
    
    commentArea = new JTextArea();
    commentArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    
    // Add scroll pane
    JScrollPane commentAreaPane = new JScrollPane(commentArea);
    commentAreaPane.setPreferredSize(new Dimension(400, 200));
    commentArea.setMargin(new Insets(10, 10, 10 , 10));
    // Add comment area
    add(commentAreaPane, BorderLayout.CENTER);
    
    if (editHighlights) {
      mapHighlightsToProps = new LinkedHashMap<AuthorPersistentHighlight, LinkedHashMap<String,String>>();
      currentHighlight = highlights.get(0);
      JPanel northPanel = new JPanel();
      infoLabel = new JLabel();
      northPanel.add(infoLabel);
      // Add next button if necessary
      if (highlights.size() > 1) {
        final JButton nextButton = new JButton("Next");
        nextButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            saveCurrentHighlightProps(currentHighlight);

            currentIndex[0] = currentIndex[0] + 1;
            currentHighlight = highlights.get(currentIndex[0]);

            // Display next highlight
            displayHighlightProps(currentHighlight);

            // Disable next button if necessary
            if (highlights.size() == currentIndex[0] + 1) {
              nextButton.setEnabled(false);
            }
          }
        });
        // Add next button
        northPanel.add(nextButton);
      }
      add(northPanel, BorderLayout.NORTH);
      
      // Display properties for the current highlight 
      displayHighlightProps(currentHighlight);
    }
  }
  
  private void saveCurrentHighlightProps(AuthorPersistentHighlight highlight) {
    // Save edited properties for the current map
    LinkedHashMap<String, String> props = highlight.getClonedProperties();
    props.put(HighlightProperties.COMMENT, commentArea.getText());
    mapHighlightsToProps.put(highlight, props);
  }
  
  /**
   * @see ro.sync.ecss.extensions.commons.ui.OKCancelDialog#doOK()
   */
  @Override
  protected void doOK() {
    if (currentHighlight != null) {
      saveCurrentHighlightProps(currentHighlight);
      if (mapHighlightsToProps != null) {
        Set<AuthorPersistentHighlight> highlights = mapHighlightsToProps.keySet();
        for (AuthorPersistentHighlight highlight : highlights) {
          // Update the timestamp
          mapHighlightsToProps.get(highlight).put(HighlightProperties.ID, authorAccess.getReviewController().getCurrentTimestamp());
        }
      }
    }
    super.doOK();
  }

  /**
   * Display highlight properties.
   * 
   * @param highlight The current highlight.
   */
  private void displayHighlightProps(AuthorPersistentHighlight highlight) {
    LinkedHashMap<String, String> cloneProperties = highlight.getClonedProperties();
    
    // Highlight properties
    String id = cloneProperties.get(HighlightProperties.ID);
    String author = cloneProperties.get(HighlightProperties.AUTHOR);
    String comment = cloneProperties.get(HighlightProperties.COMMENT);
     
    // Display highlight comment
    commentArea.setText(comment);
    
    // Display highlight ID and author
    infoLabel.setText("Id: " + id + " Author: " + author);
  }
  
  /**
   * Show the dialog.
   */
  public void showDialog() {
    setLocationRelativeTo(null);
    pack();
    setVisible(true);
  }

  /**
   * Get the last edited comment.
   * 
   * @return The inserted comment.
   */
  public String getComment() {
    return commentArea.getText();
  }
  
  /**
   * @return Returns the highlights to properties map.
   */
  public Map<AuthorPersistentHighlight, LinkedHashMap<String, String>> getMapHighlightsToProps() {
    return mapHighlightsToProps;
  }
}
