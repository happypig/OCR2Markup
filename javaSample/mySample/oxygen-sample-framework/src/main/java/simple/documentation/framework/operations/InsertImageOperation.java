package simple.documentation.framework.operations;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;


/**
 * Insert image operation.
 */
public class InsertImageOperation implements AuthorOperation {

  //
  // Implementing the Author Operation Interface.
  //

  /**
   * Performs the operation.
   */
  public void doOperation(AuthorAccess authorAccess, 
      ArgumentsMap arguments)
  throws IllegalArgumentException, 
  AuthorOperationException {

    JFrame oxygenFrame = (JFrame) authorAccess.getWorkspaceAccess().getParentFrame();
    String href = displayURLDialog(oxygenFrame);
    if (href.length() != 0) {		
      // Creates the image XML fragment.
      String imageFragment = 
        "<image xmlns='http://www.oxygenxml.com/sample/documentation'" +
        " href='" + href + "'/>";

      // Inserts this fragment at the caret position.
      int caretPosition = authorAccess.getEditorAccess().getCaretOffset();		
      authorAccess.getDocumentController().insertXMLFragment(imageFragment, caretPosition);
    }
  }

  /**
   * Has no arguments.
   * 
   * @return null.
   */
  public ArgumentDescriptor[] getArguments() {
    return null;
  }

  /**
   * @return A description of the operation.
   */
  public String getDescription() {
    return "Inserts an image element. Asks the" + 
    " user for a URL reference.";
  }

  //
  // End of interface implementation.
  //

  //
  // Auxiliary methods.
  //

  /**
   * Displays the URL dialog. 
   * 
   * @param parentFrame The parent frame for 
   * the dialog.
   * @return The selected URL string value, 
   * or the empty string if the user canceled 
   * the URL selection.
   */
  private String displayURLDialog(JFrame parentFrame) {

    final JDialog dlg = new JDialog(parentFrame, 
        "Enter the value for the href attribute", true);
    JPanel mainContent = new JPanel(new GridBagLayout());

    // The text field.
    GridBagConstraints cstr = new GridBagConstraints();
    cstr.gridx = 0;
    cstr.gridy = 0;
    cstr.weightx = 0;
    cstr.gridwidth = 1;
    cstr.fill = GridBagConstraints.HORIZONTAL;
    mainContent.add(new JLabel("Image URI:"), cstr);

    cstr.gridx = 1;
    cstr.weightx = 1;
    final JTextField urlField = new JTextField();
    urlField.setColumns(15);
    mainContent.add(urlField, cstr);

    // Add the "Browse button."
    cstr.gridx = 2;
    cstr.weightx = 0;
    JButton browseButton = new JButton("Browse");
    browseButton.addActionListener(new ActionListener() {

      /**
       * Shows a file chooser dialog.
       */
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setMultiSelectionEnabled(false);
        // Accepts only the image files.
        fileChooser.setFileFilter(new FileFilter() {
          @Override
          public String getDescription() {
            return "Image files";
          }

          @Override
          public boolean accept(File f) {
            String fileName = f.getName();
            return f.isFile() && 
            ( fileName.endsWith(".jpeg")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".gif")
                || fileName.endsWith(".png")
                || fileName.endsWith(".svg"));
          }
        });				
        if (fileChooser.showOpenDialog(dlg) 
            == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();
          try {
            // Set the file into the text field.
            urlField.setText(file.toURI().toURL().toString());
          } catch (MalformedURLException ex) {
            // This should not happen.
            ex.printStackTrace();
          }
        }
      }
    });
    mainContent.add(browseButton, cstr);

    // Add the "Ok" button to the layout.
    cstr.gridx = 0;
    cstr.gridy = 1;
    cstr.weightx = 0;
    JButton okButton = new JButton("Ok");
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dlg.setVisible(false);
      }
    });
    mainContent.add(okButton, cstr);
    mainContent.setBorder(
        BorderFactory.createEmptyBorder(10, 5, 10, 5));

    // Add the "Cancel" button to the layout.
    cstr.gridx = 2;
    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        urlField.setText("");
        dlg.setVisible(false);
      }
    });
    mainContent.add(cancelButton, cstr);

    // When the user closes the dialog 
    // from the window decoration,
    // assume "Cancel" action.
    dlg.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        urlField.setText("");
      }		
    });

    dlg.getContentPane().add(mainContent);
    dlg.pack();
    dlg.setLocationRelativeTo(parentFrame);
    dlg.setVisible(true);
    return urlField.getText();
  }

  /**
   * Test method.
   *  
   * @param args The arguments are ignored.
   */
  public static void main(String[] args) {
    InsertImageOperation operation = 
      new InsertImageOperation();
    System.out.println("Choosen URL: " +  
        operation.displayURLDialog(new JFrame()));
  }	
}