package simple.documentation.framework.operations.table;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import ro.sync.ecss.extensions.commons.table.operations.TableCustomizerConstants;
import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;

/**
 * Dialog used to customize the insertion of a table (number of rows, columns, table caption).
 * It is used on Standalone implementation.  
 */
@SuppressWarnings("serial")
public class TableCustomizerDialog extends OKCancelDialog implements TableCustomizerConstants {
  
  /**
   * If selected the user can specify the table title. 
   */
  private JCheckBox titleCheckbox;
  
  /**
   * Text field for specify the table title.
   */
  private JTextField titleTextField;
  
  /**
   * Used to specify the number of rows.
   */
  private JSpinner rowsSpinner;
  
  /**
   * Used to specify the number of columns.
   */
  private JSpinner columnsSpinner;
  
  /**
   * If selected the user can specify the table background color. 
   */
  private JCheckBox tableBgColorCheckbox;
  
  /**
   * Button used to choose table background color.
   */
  private JButton tableBgColorButton;
  
  /**
   * Constructor.
   * 
   * @param parentFrame The parent {@link JFrame} of the dialog.  
   */
  public TableCustomizerDialog(
      JFrame parentFrame) {
    super(parentFrame, "Insert Table", true);
    
    // The main panel
    JPanel mainPanel = new JPanel(new GridBagLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    GridBagConstraints gridBagConstr = new GridBagConstraints();
    gridBagConstr.anchor = GridBagConstraints.WEST;
    gridBagConstr.gridy = 0;
    gridBagConstr.gridx = 0;
    gridBagConstr.weightx = 0;
    gridBagConstr.gridwidth = 1;
    gridBagConstr.insets = new Insets(5, 0, 5, 5);
    gridBagConstr.fill = GridBagConstraints.NONE;
    
    // Title check box
    titleCheckbox = new JCheckBox("Title");
    titleCheckbox.setName("Title checkbox");    
    titleCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        titleTextField.setEditable(titleCheckbox.isSelected());
      }
    });
    titleCheckbox.setBorder(BorderFactory.createEmptyBorder());
    mainPanel.add(titleCheckbox, gridBagConstr);
    
    // Title text field
    titleTextField = new JTextField();
    titleTextField.setName("Title text field");
    gridBagConstr.gridx ++;
    gridBagConstr.weightx = 1;
    gridBagConstr.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstr.insets = new Insets(5, 0, 5, 0);
    mainPanel.add(titleTextField, gridBagConstr);
    
    gridBagConstr.gridy ++;
    gridBagConstr.gridx = 0;
    gridBagConstr.weightx = 0;
    gridBagConstr.gridwidth = 1;
    gridBagConstr.insets = new Insets(5, 0, 5, 5);
    gridBagConstr.fill = GridBagConstraints.BOTH;
    
    // Table bgcolor box
    tableBgColorCheckbox = new JCheckBox("Table Background");
    tableBgColorButton = new JButton();
    tableBgColorCheckbox.setName("Table Background");    
    tableBgColorCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        tableBgColorButton.setEnabled(tableBgColorCheckbox.isSelected());
      }
    });
    tableBgColorCheckbox.setBorder(BorderFactory.createEmptyBorder());
    mainPanel.add(tableBgColorCheckbox, gridBagConstr);
    
    // Table bg color
    tableBgColorButton.setIcon(new Icon() {
      public void paintIcon(Component parent, Graphics g, int x, int y) {
        Color color = tableBgColorButton.getBackground();
        if (color == null) {
          return;
        }
        Color used4Draw = color;
        if (parent != null && !parent.isEnabled()) {
          used4Draw = parent.getBackground();
        }
        g.setColor(used4Draw);
        g.fillRect(x, y, getIconWidth(), getIconHeight());
        g.setColor(used4Draw.darker());
        g.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
      }
      
      public int getIconWidth() {
        return tableBgColorButton.getWidth();
      }
      
      public int getIconHeight() {
        return tableBgColorButton.getHeight();
      }
    });
    
    tableBgColorButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        Color initialBackground = tableBgColorButton.getBackground();
        Color background = JColorChooser.showDialog(null,
            "Choose Color", initialBackground);
        if (background != null) {
          tableBgColorButton.setBackground(background);
         
          tableBgColorButton.setContentAreaFilled(true);
        }
      }
    });
    tableBgColorButton.setPreferredSize(new Dimension(100, 15));
    gridBagConstr.gridx ++;
    gridBagConstr.weightx = 1;
    gridBagConstr.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstr.insets = new Insets(2, 2, 2, 2);
    mainPanel.add(tableBgColorButton, gridBagConstr);

    // Table size panel
    JPanel sizePanel = new JPanel(new GridBagLayout());
    sizePanel.setBorder(BorderFactory.createTitledBorder("Table Size"));
    
    gridBagConstr.gridy ++;
    gridBagConstr.gridx = 0;
    gridBagConstr.weightx = 1;
    gridBagConstr.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstr.gridwidth = 2;
    gridBagConstr.insets = new Insets(5, 0, 5, 0);
    mainPanel.add(sizePanel, gridBagConstr);
       
    // 'Rows' label
    JLabel rowsLabel = new JLabel("Rows");
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.anchor = GridBagConstraints.WEST;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 0;
    c.insets = new Insets(0, 5, 5, 5);
    sizePanel.add(rowsLabel, c);
    
    // Number of rows text field
    rowsSpinner = new JSpinner();
    rowsSpinner.setName("Rows spinner");
    rowsSpinner.setModel(new SpinnerNumberModel(2, 0, 100, 1));
    c.gridx++;
    c.weightx = 1;
    sizePanel.add(rowsSpinner, c);
    
    // 'Columns' label
    JLabel columnsLabel = new JLabel("Columns");
    c.gridx++;
    c.weightx = 0;
    sizePanel.add(columnsLabel, c);
    
    // Number of rows text field
    columnsSpinner = new JSpinner();
    columnsSpinner.setName("Columns spinner");
    columnsSpinner.setModel(new SpinnerNumberModel(2, 0, 100, 1));
    c.gridx++;
    c.weightx = 1;
    sizePanel.add(columnsSpinner, c);

    //Add the main panel
    getContentPane().add(mainPanel, BorderLayout.CENTER);
    
    pack();
    setResizable(false);
  }
  
  /**
   * Contains informations about the table element. 
   */
  class TableInfo {
    /**
     * Table title.
     */
    private final String title;
    /**
     * Table rows number.
     */
    private final int rowsNumber;
    /**
     * Table columns number.
     */
    private final int columnsNumber;
    /**
     * Table background color.
     */
    private final Color tableBgColor;

    /**
     * @param title Table title.
     * @param rowsNumber Table rows number. 
     * @param columnsNumber Table columns number.
     * @param tableBgColor Table background color.
     */
    public TableInfo(String title, int rowsNumber, int columnsNumber, Color tableBgColor) {
      this.title = title;
      this.rowsNumber = rowsNumber;
      this.columnsNumber = columnsNumber;
      this.tableBgColor = tableBgColor;
    }
    
    /**
     * @return Returns the title.
     */
    public String getTitle() {
      return title;
    }
    
    /**
     * @return Returns the rows number.
     */
    public int getRowsNumber() {
      return rowsNumber;
    }
    
    /**
     * @return Returns the columns number.
     */
    public int getColumnsNumber() {
      return columnsNumber;
    }
    
    /**
     * @return Returns the table background color.
     */
    public Color getTableBackgroundColor() {
      return tableBgColor;
    }
  }
  
  /**
   * Show the dialog to customize the table attributes.
   * 
   * @return The object containing informations about the table to be inserted. 
   * If <code>null</code> then the user canceled the table insertion.
   */
  public TableInfo showDialog() {
    // Reset components to default values
    titleTextField.setEditable(true);
    titleTextField.setText("");
    titleCheckbox.setSelected(true);
    
    tableBgColorButton.setEnabled(false);
    tableBgColorButton.setBackground(tableBgColorCheckbox.getBackground());
    tableBgColorCheckbox.setSelected(false);

    // Set the default number of rows and columns
    rowsSpinner.setValue(new Integer(3));
    columnsSpinner.setValue(new Integer(2));

    // Request focus in title field
    titleTextField.requestFocus();
    
    super.setVisible(true);
    
    TableInfo tableInfo = null;
    if(getResult() == RESULT_OK) {
      // Title
      String title = null;
      if(titleCheckbox.isSelected()) {
        title = titleTextField.getText();
        title = ro.sync.basic.xml.BasicXmlUtil.escape(title);
      }
      // Table background color
      Color tableBgColor = null;
      if(tableBgColorCheckbox.isSelected()) {
        tableBgColor = tableBgColorButton.getBackground();
      }
      int rowsNumber = ((Integer)rowsSpinner.getValue()).intValue();
      int columnsNumber = ((Integer)columnsSpinner.getValue()).intValue();
     
      tableInfo = 
        new TableInfo(
            title, 
            rowsNumber, 
            columnsNumber, 
            tableBgColor);
    } else {
      // Cancel was pressed
    }
    return tableInfo;
  }
}