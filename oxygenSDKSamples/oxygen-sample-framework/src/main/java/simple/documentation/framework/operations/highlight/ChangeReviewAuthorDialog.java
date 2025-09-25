package simple.documentation.framework.operations.highlight;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;

/**
 * Dialog used for changing the current review author 
 */
@SuppressWarnings("serial")
public class ChangeReviewAuthorDialog extends OKCancelDialog {
  /**
   * Combo box containing all possible author names
   */
  private JComboBox<String> authorNamesComboBox;
  
  /**
   * Constructor.
   * 
   * @param parentFrame The parent frame.
   * @param title The dialog title.
   * @param authorNames All the possible author names.
   */
  public ChangeReviewAuthorDialog(
      JFrame parentFrame, 
      String title, 
      String[] authorNames) {
    super(parentFrame, title, true);
    // Add label 
    add(new JLabel("Choose Review Author: "));
    // Add the combobox containing possible author names
    authorNamesComboBox = new JComboBox<String>(authorNames);
    add(authorNamesComboBox);
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
   * Get the selected author name.
   * 
   * @return The selected author name.
   */
  public String getSelectedAuthorName() {
    return (String) authorNamesComboBox.getSelectedItem();
  }
}
