package DILA.converter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *  The plugin dialog.
 *
 *@author     dan, radu
 *@created    October 15, 2002
 *@version    $Revision: 1.38 $
 */
@SuppressWarnings("serial")
public class ConversionDialog extends JDialog {

    /**
     *  The separator between result value and unit measure label.
     */
    private final static String LABEL_SEPARATOR = " ";

    /**
     *  This filed will hold the conversion result from slave to master, or
     *  master value to convert.
     */
    JTextField masterValueTextField;

    /**
     *  If this check box is set then include labels in master, slave text
     *  fields.
     */
    JCheckBox includeLabelCheck;

    /**
     *  This combo box will hold all formats.
     */
    private JComboBox<String> formatsComboBox;

    /**
     *  This combo box will hold the master units measure names.
     */
    private JComboBox<String> masterValueComboBox;

    /**
     *  This combo box will hold the slave values.
     */
    private JComboBox<String> slaveValueComboBox;

    /**
     *  This combo box will hold the decimal override values.
     */
    private JComboBox<String> decimalsOverrideComboBox;


    /**
     *  This filed will hold the conversion result from master to slave, or
     *  slave value to convert.
     */
    private JTextField slaveValueTextField;

    /**
     *  If this check box is set then convert from slave to master value, else
     *  reverse.
     */
    private JCheckBox swapValuesCheck;

    /**
     *  Conversion manager.
     */
    private ConversionManager conversionManager;

    /**
     *  If this flag is set then master combo box may be chaged.
     */
    private boolean masterActionFlag = true;

    /**
     *  If this flag is set then slave combo box may be chaged.
     */
    private boolean slaveActionFlag = true;

    /**
     * The button from master panel.
     */
    private JButton copyButtonMaster;

    /**
     * The button from slave panel.
     */
    private JButton copyButtonSlave;

    /**
     * Decimals number to include in result.
     */
    private int decimalsNumberToShow;


    /**
     *  Constructor for the ConversionDialog object
     *
     *@param  parentFrame  The parent frame of this dialog.
     *@param  dialogName   The dialog name.
     */
    public ConversionDialog(Frame parentFrame, String dialogName) {
        super(parentFrame, dialogName);
        try {
            conversionManager = new ConversionManager();
        } catch (ConversionException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame, ex.getClass().getName() + " " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            this.setVisible(false);
            return;
        } catch (IOException ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(parentFrame, ex.getClass().getName() + " " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            this.setVisible(false);
            return;
        }

        jbInit();
        this.getRootPane().registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doCancel();
                }
            },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // Add keyboard listener to this dialog.
        this.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        doCancel();
                    }
                }


                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        doCancel();
                    }
                }
            });
        // Add window listener to this dialog.
        this.addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    doCancel();
                }
            });
    }

    /**
     *  Set the conversion string and shows the dialog. Called from the plugin.
     *
     *@param  stringToConvert  The value to be converted.
     */
    public void setToConvert(String stringToConvert) {
        // If swapValuesCheck isn't selected then convert value from
        //   master to slave, else reverse.
        if (!swapValuesCheck.isSelected()) {
            masterValueTextField.selectAll();
            if (stringToConvert != null) {
                masterValueTextField.setText(stringToConvert);
                showLabels();
            } else {
                masterValueTextField.setText("");
            }
            masterValueTextField.setCaretPosition(0);
            convert(true);
            javax.swing.SwingUtilities.updateComponentTreeUI(this);
            setVisible(true);
            masterValueTextField.requestFocus();
        } else {
            slaveValueTextField.selectAll();
            if (stringToConvert != null) {
                slaveValueTextField.setText(stringToConvert);
                showLabels();
            } else {
                slaveValueTextField.setText("");
            }
            slaveValueTextField.setCaretPosition(0);
            convert(false);
            javax.swing.SwingUtilities.updateComponentTreeUI(this);
            setVisible(true);
            slaveValueTextField.requestFocus();
        }
    }


    /**
     *  Transfers the currently selected text to the system clipboard. Does
     *  nothing for null argument.
     *
     *@param  str  The string to be transfered.
     */
    public void copy(String str) {
        if (str != null) {
            Clipboard clipboard = getToolkit().getSystemClipboard();
            StringSelection contents = new StringSelection(str);
            clipboard.setContents(contents, null);
        }
    }


    /**
     *  Set text to master and slave text fields. If includeLabelCheck is check
     *  then include labels in the text.
     */
    public void showLabels() {
        String number1 = extractNumberStr(masterValueTextField.getText());
        if (number1.length() == 0) {
            number1 = "0";
        }
        String number2 = extractNumberStr(slaveValueTextField.getText());
        if (number2.length() == 0) {
            number2 = "0";
        }

        if (includeLabelCheck.isSelected()) {
            ConversionFormat currentFormat = conversionManager.getFormatByName(formatsComboBox.getSelectedItem().toString());
            String label1 = currentFormat.getAssociatedLabel((String) masterValueComboBox.getSelectedItem());
            String label2 = currentFormat.getAssociatedLabel((String) slaveValueComboBox.getSelectedItem());

            masterValueTextField.setText(formatNumberWithLabel(number1, label1));
            slaveValueTextField.setText(formatNumberWithLabel(number2, label2));
        } else {
            masterValueTextField.setText(number1);
            slaveValueTextField.setText(number2);
        }
    }


    /**
     *  Set default button. If swapValuesCheck is selected then default button
     *  will be button from master panel.
     */
    private void setDefaultButton() {
      if(swapValuesCheck.isSelected()){
          getRootPane().setDefaultButton(copyButtonMaster);
      } else {
          getRootPane().setDefaultButton(copyButtonSlave);
      }
    }


    /**
     *  If swapValuesCheck is selected then copy masterValueTextField value in
     *  slaveValueTextField and convert, else reverse.
     */
    void switchMasterAndSlave() {
        if (swapValuesCheck.isSelected()) {
            String masterValue = masterValueTextField.getText();
            slaveValueTextField.setText(masterValue);
            convert(false);
            showLabels();
        } else {
            String slaveValue = slaveValueTextField.getText();
            masterValueTextField.setText(slaveValue);
            convert(true);
            showLabels();
        }
        setDefaultButton();
    }


    /**
     *  Copy master text field value to clipboard.
     */
    void copyMasterValue() {
        copy(masterValueTextField.getText());
        setVisible(false);
    }



    /**
     *  Copy slave text field value to clipboard.
     */
    void copySlaveValue() {
        copy(slaveValueTextField.getText());
        setVisible(false);
    }


    /**
     *  Returns the string representing the number from a text with the format
     *  "number label".
     *
     *@param  number  The number string.
     *@return         The number without label.
     */
    String extractNumberStr(String number) {
        int pos = number.indexOf(LABEL_SEPARATOR);
        if (pos != -1) {
            return number.substring(0, pos);
        } else {
            return number;
        }
    }


    /**
     *  Appends a label to the number if the includeLabelCheck is selected. If
     *  the label is null then leave number without changes.
     *
     *@param  number  The number.
     *@param  label   The label.
     *@return         The number with label.
     */
    String formatNumberWithLabel(String number, String label) {
        if (label != null) {
            if (includeLabelCheck.isSelected()) {
                return number + LABEL_SEPARATOR + label;
            } else {
                return number + "";
            }

        } else {
            return "" + number;
        }
    }


    /**
     *  Get all the format names.
     *
     *@return    The format names.
     */
    private java.util.List<String> getFormatsName() {
        java.util.List<String> result = new Vector<String>();
        java.util.List<ConversionFormat> formats = conversionManager.getFormats();

        Iterator<ConversionFormat> it = formats.iterator();
        while (it.hasNext()) {
            result.add(it.next().getName());
        }
        return result;
    }


    /**
     *  Get the possible entries for the master combob box.
     *
     *@param  formatName  The name of the format.
     *@return             A list of strings.
     */
    private java.util.List<String> getSystem1Entries(String formatName) {
        java.util.List<String> result = new Vector<String>();
        java.util.List<ConversionFormat> formats = conversionManager.getFormats();

        Iterator<ConversionFormat> it = formats.iterator();
        while (it.hasNext()) {
            ConversionFormat currentFormat = it.next();
            if ((currentFormat.getName()).equals(formatName)) {
                result.addAll(currentFormat.getMasterValues(null));
            }
        }
        return result;
    }


    /**
     *  Get the possible entries for the slave combob box, in the context of the
     *  selection in the master combo box.
     *
     *@param  formatName          The name of the format.
     *@param  currentMasterEntry  The value selected in the master combo box.
     *@return                     A list of strings.
     */
    private java.util.List<String> getSystem2Entries(String formatName, String currentMasterEntry) {
        java.util.List<String> result = new Vector<String>();
        java.util.List<ConversionFormat> formats = conversionManager.getFormats();

        Iterator<ConversionFormat> it = formats.iterator();
        while (it.hasNext()) {
            ConversionFormat currentFormat = it.next();
            if ((currentFormat.getName()).equals(formatName)) {
                result.addAll(currentFormat.getSlaveValues(currentMasterEntry));
            }
        }
        return result;
    }


    /**
     *  Hide the dialog.
     */
    private void doCancel() {
        this.setVisible(false);
    }


    /**
     *  Generic convert method called when users interact with the dialog.
     *
     *@param  direct  If true convert from master to slave, otherwise reverse.
     */
    private void convert(boolean direct) {
        // Get current format
        ConversionFormat currentFormat = conversionManager.getFormatByName(formatsComboBox.getSelectedItem().toString());

        // Get masterval, slaveval from combo boxes
        String masterVal = (String) masterValueComboBox.getSelectedItem();
        String slaveVal = (String) slaveValueComboBox.getSelectedItem();

        if (direct) {
            String toConvert = extractNumberStr(masterValueTextField.getText());
            // Try to parse new value
            try {
                double number = Double.parseDouble(toConvert);
                // Get result
                double result = currentFormat.convert(masterVal, slaveVal, number, 0, true,decimalsNumberToShow);

                String label = currentFormat.getAssociatedLabel(
                        (String) slaveValueComboBox.getSelectedItem());
                slaveValueTextField.setText(formatNumberWithLabel("" + result, label));
                slaveValueTextField.setCaretPosition(0);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                slaveValueTextField.setText("");
            } catch (ConversionException ex) {
                ex.printStackTrace();
                slaveValueTextField.setText("");
            }
        } else {
            String toConvert = extractNumberStr(slaveValueTextField.getText());
            // Try to parse new value
            try {
                double number = Double.parseDouble(toConvert);
                // Get result
                double result = currentFormat.convert(masterVal, slaveVal, 0, number, false, decimalsNumberToShow);

                String label = currentFormat.getAssociatedLabel(
                        (String) masterValueComboBox.getSelectedItem());
                masterValueTextField.setText(formatNumberWithLabel("" + result, label));
                masterValueTextField.setCaretPosition(0);
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                masterValueTextField.setText("");
            } catch (ConversionException ex) {
                ex.printStackTrace();
                masterValueTextField.setText("");
            }
        }
    }


    /**
     *  Creates the GUI.
     */
    private void jbInit() {
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        FlowLayout centerFlowLayout = new FlowLayout(FlowLayout.CENTER);

        JPanel formatPanel = new JPanel();
        formatPanel.setBorder(BorderFactory.createTitledBorder(
                conversionManager.getStringResource(FORMAT)));
        GridLayout grid = new GridLayout(2, 2);
        grid.setHgap(5);
        grid.setVgap(5);

        formatPanel.setLayout(grid);
        // Create formats combo box and add listener
        formatsComboBox = new JComboBox<>((Vector<String>) getFormatsName());
        formatsComboBox.addActionListener(new FormatComboListener());
        // Create include label check.

        boolean lockOnLabelBool = conversionManager.getDefaultValueAsBoolean(DV_LOCK_LABELS).booleanValue();

        includeLabelCheck = new JCheckBox(
                conversionManager.getStringResource(INCLUDE_LABELS), lockOnLabelBool);
        // If default value is 'true' then disable include labels check
        includeLabelCheck.setEnabled(!lockOnLabelBool);

        includeLabelCheck.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showLabels();
                }
            });
        swapValuesCheck = new JCheckBox(conversionManager.getStringResource(SWAP_VALUES));
        swapValuesCheck.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    switchMasterAndSlave();
                }
            });

        decimalsOverrideComboBox = new JComboBox<>();
        // Fill decimalsOverrideComboBox.
        decimalsOverrideComboBox.addItem("None");
        for (int i = 0; i <= 7; i++) {
          decimalsOverrideComboBox.addItem(Integer.toString(i));
        }
        decimalsOverrideComboBox.addItem("All");

        // Set default decimals number to show.
        this.decimalsNumberToShow = -2;

        decimalsOverrideComboBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            String currentSelection = (String)decimalsOverrideComboBox.getSelectedItem();

            if (currentSelection.equals("None")) {
              decimalsNumberToShow = -2;
            }
            else if (currentSelection.equals("All")) {
              decimalsNumberToShow = -1;
            }
            else {
              decimalsNumberToShow = Integer.parseInt(currentSelection);
            }

            convert(!swapValuesCheck.isSelected());

          }
        });

        JPanel formatsComboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formatsComboBoxPanel.add(new JLabel(conversionManager.getStringResource(TYPE)));
        formatsComboBoxPanel.add(formatsComboBox);

        JPanel decimalsOverrideComboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        decimalsOverrideComboBoxPanel.add(
          new JLabel(conversionManager.getStringResource(DECIMALS_OVERRIDE)));
        decimalsOverrideComboBoxPanel.add(decimalsOverrideComboBox);

        JPanel includeLabelCheckPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        includeLabelCheckPanel.add(includeLabelCheck);

        JPanel swapValuesCheckPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        swapValuesCheckPanel.add(swapValuesCheck);

        formatPanel.add(formatsComboBoxPanel);
        formatPanel.add(decimalsOverrideComboBoxPanel);
        formatPanel.add(includeLabelCheckPanel);
        formatPanel.add(swapValuesCheckPanel);

        JPanel sytemsPanel = new JPanel();
        sytemsPanel.setLayout(new GridLayout(2, 1));

        JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BorderLayout());
        masterPanel.setBorder(BorderFactory.createTitledBorder(
                 conversionManager.getStringResource(MASTER_SYSTEM)));
        JPanel system1Panel = new JPanel();
        system1Panel.setLayout(new GridLayout(1, 2, 5, 5));
         
        
        // Components to system1 panel.
        masterValueTextField = new JTextField();
        masterValueTextField.setPreferredSize(new Dimension(200, 21));
        // Add document listener to this text field
        masterValueTextField.getDocument().addDocumentListener(
            new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    if (masterValueTextField.hasFocus()) {
                        convert(true);
                    }
                }


                public void removeUpdate(DocumentEvent e) {
                    if (masterValueTextField.hasFocus()) {
                        convert(true);
                    }
                }


                public void changedUpdate(DocumentEvent e) { 
                  //
                }
            });

        system1Panel.add(masterValueTextField);
        // Master values combo box
        masterValueComboBox = new JComboBox<>((Vector<String>) getSystem1Entries(formatsComboBox.getItemAt(0).toString()));
        masterValueComboBox.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateItemsInSlave();
                }
            });
        system1Panel.add(masterValueComboBox);

        JPanel masterButtonPanel = new JPanel();
        masterButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        copyButtonMaster = new JButton(conversionManager.getStringResource(COPY_VALUE_AND_CLOSE));
        copyButtonMaster.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    copyMasterValue();
                }
            });
        masterButtonPanel.add(copyButtonMaster);
        masterPanel.add(system1Panel, BorderLayout.NORTH);
        masterPanel.add(masterButtonPanel, BorderLayout.SOUTH);

        JPanel slavePanel = new JPanel();
        slavePanel.setLayout(new BorderLayout());
        slavePanel.setBorder(BorderFactory.createTitledBorder(
                 conversionManager.getStringResource(SLAVE_SYSTEM)));

        JPanel system2Panel = new JPanel();
        system2Panel.setLayout(new GridLayout(1, 2, 5, 5));
        slaveValueTextField = new JTextField();
        slaveValueTextField.setPreferredSize(new Dimension(200, 21));

        slaveValueTextField.getDocument().addDocumentListener(
            new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    if (slaveValueTextField.hasFocus()) {
                        convert(false);
                    }
                }


                public void removeUpdate(DocumentEvent e) {
                    if (slaveValueTextField.hasFocus()) {
                        convert(false);
                    }
                }


                public void changedUpdate(DocumentEvent e) {
                  //
                }
            });
        system2Panel.add(slaveValueTextField);

        // Slave combo box
        slaveValueComboBox = new JComboBox<>((Vector<String>)
                getSystem2Entries((String) formatsComboBox.getItemAt(0),
                (String) masterValueComboBox.getSelectedItem()));
        slaveValueComboBox.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateItemsInMaster();
                }
            });
        slaveValueComboBox.setPreferredSize(new Dimension(200, 21));
        system2Panel.add(slaveValueComboBox);

        JPanel slaveButtonPanel = new JPanel();
        slaveButtonPanel.setLayout(centerFlowLayout);
        copyButtonSlave = new JButton(conversionManager.getStringResource(COPY_VALUE_AND_CLOSE));
        copyButtonSlave.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    copySlaveValue();
                }
            });
        slaveButtonPanel.add(copyButtonSlave);
        slavePanel.add(system2Panel, BorderLayout.NORTH);
        slavePanel.add(slaveButtonPanel, BorderLayout.SOUTH);

        sytemsPanel.add(masterPanel);
        sytemsPanel.add(slavePanel);

        content.add(formatPanel, BorderLayout.NORTH);
        content.add(sytemsPanel, BorderLayout.CENTER);

        this.getContentPane().add(content, BorderLayout.CENTER);
        setDefaultButton();

        pack();
        this.setResizable(false);

        // Set location.
        Rectangle parentBounds = this.getParent().getBounds();

        setLocation((int)( parentBounds.getX() + 100),(int)( parentBounds.getY() + 100));
    }


    /**
     *  Update entries in slave combo box.
     */
    private void updateItemsInSlave() {
        if (masterActionFlag) {
            // Disable slave combo box listener.
            slaveActionFlag = false;
            String newEntry = (String) masterValueComboBox.getSelectedItem();
            // Change masterValueComboBox and slaveValueComboBox
            slaveValueComboBox.removeAllItems();
            List<String> newSlaveEntries = getSystem2Entries(formatsComboBox.getSelectedItem().toString(), newEntry);
            Iterator<String> it = newSlaveEntries.iterator();
            while (it.hasNext()) {
                String item = it.next();
                slaveValueComboBox.addItem(item);
            }
            convert(true);
            slaveActionFlag = true;
            showLabels();
        }
    }


    /**
     *  Update entries in master combo box.
     */
    private void updateItemsInMaster() {
        if (slaveActionFlag) {
            masterActionFlag = false;
            convert(true);
            masterActionFlag = true;
        }
    }


    /**
     *  Listener for format combo box.
     *
     *@author     radu_pisoi
     *@created    October 23, 2002
     */
    class FormatComboListener implements ActionListener {
        /**
         *  Description of the Method
         *
         *@param  e  Description of Parameter
         */
        public void actionPerformed(ActionEvent e) {
            // Disable master and slave combo box listeners.
            masterActionFlag = false;
            slaveActionFlag = false;
            String newFormat = (String) formatsComboBox.getSelectedItem();
            // Change master and slave combo box entries.
            masterValueComboBox.removeAllItems();
            java.util.List<String> entriesName = getSystem1Entries(newFormat);
            Iterator<String> it = entriesName.iterator();
            while (it.hasNext()) {
                masterValueComboBox.addItem(it.next());
            }
            // Enable master and slave combo box listeners.
            masterActionFlag = true;
            slaveActionFlag = true;
            // Update slave combo box entries respect the format combo
            //    box changes, and master combo box value.
            updateItemsInSlave();
            // Covert the master text field value.
            convert(true);
            showLabels();
        }
    }

    private final static String FORMAT = "Format";
    private final static String MASTER_SYSTEM = "Master system";
    private final static String SLAVE_SYSTEM = "Slave system";
    private final static String COPY_VALUE_AND_CLOSE = "Copy value and close";
    private final static String INCLUDE_LABELS = "Include labels";
    private final static String SWAP_VALUES = "Swap values";
    private final static String DECIMALS_OVERRIDE = "Decimals override";
    private final static String TYPE = "Type";

    private final static String DV_LOCK_LABELS = "Lock labels";

}
