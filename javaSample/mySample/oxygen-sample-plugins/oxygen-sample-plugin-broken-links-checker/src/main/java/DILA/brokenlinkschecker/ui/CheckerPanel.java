package DILA.brokenlinkschecker.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import DILA.brokenlinkschecker.impl.BrokenLinksFinder;
import DILA.brokenlinkschecker.impl.CheckerTableModel;
import DILA.brokenlinkschecker.impl.CollectedInfo;
import DILA.brokenlinkschecker.impl.LinkProblem;
import DILA.brokenlinkschecker.impl.URLParameterManager;

@SuppressWarnings("serial")
/**
 * The panel containing all the UI items.
 * 
 * @author sorin_carbunaru
 *
 */
public class CheckerPanel extends JPanel implements UIUpdater {
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(CheckerPanel.class.getName());

	/**
	 * The find button.
	 */
	private JButton findButton;

	/**
	 * The document associated with the console.
	 */
	private HTMLDocument doc;

	/**
	 * The console where the problems are reported.
	 */
	JEditorPane console;

	/**
	 * HTML Editor Kit
	 */
	HTMLEditorKit htmlEditorKit;

	/**
	 * Progress bar.
	 */
	JProgressBar progressBar;

	/**
	 * The URLs table.
	 */
	final JTable table;

	/**
	 * A list of row indexes corresponding to table rows which had reported
	 * problems.
	 */
	private List<Integer> rowsWithProblems;

	/**
	 * The text on the addition button.
	 */
	public static final String ADD_BUTTON_TEXT = "Add";

	/**
	 * The text on the removal button.
	 */
	public static final String REMOVE_BUTTON_TEXT = "Remove";

	/**
	 * The text on the editing button.
	 */
	public static final String EDIT_BUTTON_TEXT = "Edit";

	/**
	 * The text of the label from the search depth combo box.
	 */
	public static final String SEARCH_DEPTH_LABEL = "Search depth:";

	/**
	 * The text on the button triggering the broken links search before clicking
	 * on it for performing the search.
	 */
	public static final String FIND_BUTTON_TEXT = "Find broken links";

	/**
	 * The value from the combo box specifying that the search is done only for
	 * the current page.
	 */
	public static final String SEARCH_DEPTH_CURRENT_PAGE = "Current page";

	/**
	 * The value from the combo box specifying that the search is done for all
	 * pages of the website.
	 */
	public static final String SEARCH_DEPTH_ALL_PAGES_DOMAIN = "Domain wide";

	/**
	 * The label from the console presenting the problems.
	 */
	public static final String CONSOLE_HEADER = "Problems:";

	/**
	 * The label from above the table where the URLs are provided by the user.
	 */
	public static final String TABLE_LABEL = "Please provide the URLs of the start pages:";

	/**
	 * The text from the dialog asking for user confirmation for removal.
	 */
	public static final String CONFIRM_REMOVAL = "Do you want to remove the selected row?";

	/**
	 * The title of the removal dialog.
	 */
	public static final String CONFIRM_REMOVAL_DIALOG_TITLE = "Remove?";

	/**
	 * The message from the dialog displayed when the user tries to provide an
	 * empty string as an URL.
	 */
	public static final String EMPTY_STRING_ERROR = "Empty values are not accepted! Please provide a valid URL!";

	/**
	 * The message from the dialog displayed when the user tries to provide an
	 * empty string as an URL.
	 */
	public static final String INVALID_URL_ERROR = "The URL is invalid! Please provide a valid one!";

	/**
	 * The text from the finding button after the search has started.
	 */
	public static final String STOP_BUTTON_TEXT = "Stop!";

	/**
	 * Text for the dialog displayed when the user provides an invalid value for
	 * the search depth.
	 */
	public static final String INVALID_SEARCH_DEPTH = "The search depth must have one of the following values: "
			+ "\""
			+ CheckerPanel.SEARCH_DEPTH_CURRENT_PAGE
			+ "\""
			+ ", \""
			+ CheckerPanel.SEARCH_DEPTH_ALL_PAGES_DOMAIN
			+ "\""
			+ " or a provided positive integer.";

	/**
	 * The beginning of an HTML document.
	 */
	public static final String HTML_BEGIN = "<html><body><ul>";

	/**
	 * The end of an HTML document.
	 */
	public static final String HTML_END = "</ul></body></html>";

	/**
	 * Message for internal exception.
	 */
	public static final String INTERNAL_ERROR_MESSAGE = "Internal error!";

	/**
	 * Message when accessing the browser.
	 */
	public static final String BROWSER_PROBLEM = "There was a problem when trying to open your web browser!";

	/**
	 * Message when a URL not provided by user is invalid.
	 */
	public static final String INVALID_URL_NOT_BY_USER = "There was a problem when trying to process a URL!";

	/**
	 * Table status when search is performing.
	 */
	public static final String SEARCH_IS_PERFORMING_STATUS = "Searching...";

	/**
	 * Table status when search is done without any problems.
	 */
	public static final String NO_PROBLEMS_FOUND_STATUS = "No problems found.";

	/**
	 * Table status when problems have been found.
	 */
	public static final String PROBLEMS_FOUND_STATUS = "Some problems have been found. Please see the \"Problems\" console.";

	/**
	 * The name of the URL query parameter corresponding to the line of a
	 * reported problem.
	 */
	public static final String PROBLEM_LINE_QUERY_PARAM_NAME = "oXyLine";

	/**
	 * The name of the URL query parameter corresponding to the column of a
	 * reported problem.
	 */
	public static final String PROBLEM_COLUMN_QUERY_PARAM_NAME = "oXyCol";

	/**
	 * Find/stop button click counter.
	 */
	boolean shouldStop = true;

	/**
	 * Constructor.
	 */
	public CheckerPanel() {
		
		// Instantiate the list of rows with reported problems
		rowsWithProblems = new ArrayList<Integer>();

		// UI elements list...

		// Add button.
		JButton addButton;

		// Remove button.
		final JButton removeButton;

		// Edit button.
		final JButton editButton;

		// Search depth combo box.
		final JComboBox<String> searchDepthCombo;

		// The label next to the search depth combo box.
		JLabel searchDepthLabel;

		// The scroll pane containing the table
		JScrollPane tableScrollPane;

		// The scroll pane containing the console
		JScrollPane consoleScrollPane;

		// Layout and constraints declaration
		this.setLayout(new GridBagLayout());
		GridBagConstraints layoutConstraints = new GridBagConstraints();

		// Table label
		JLabel tableLabel = new JLabel();
		tableLabel.setBorder(new EmptyBorder(2, 4, 0, 0));
		tableLabel.setText(CheckerPanel.TABLE_LABEL);
		layoutConstraints = new GridBagConstraints();
		layoutConstraints.gridx = 0;
		layoutConstraints.gridy = 0;
		layoutConstraints.gridheight = 1;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.anchor = GridBagConstraints.LINE_START;
		this.add(tableLabel, layoutConstraints);

		// Table
		final CheckerTableModel tableModel = new CheckerTableModel();
		table = new JTable(tableModel) {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer,
					int row, int col) {
				// get the current row
				Component comp = super.prepareRenderer(renderer, row, col);
				if (col == 1) {
					setToolTipText((String) tableModel.getValueAt(row, col));
				} else {
					setToolTipText(null);
				}
				return comp;

			}
		};

		// Don't let the user reorder the table columns
		table.getTableHeader().setReorderingAllowed(false);

		table.getColumnModel().getColumn(0).setPreferredWidth(200);

		Border border = BorderFactory.createLineBorder(Color.GRAY);
		table.setBorder(BorderFactory.createCompoundBorder(border,
				BorderFactory.createEmptyBorder(-1, 1, -1, -1)));

		// Create border for the table header
		
		// JTableHeader tableHeader = table.getTableHeader();
		// Enumeration<TableColumn> columns =
		// tableHeader.getColumnModel().getColumns();
		// while (columns.hasMoreElements()) {
		// TableColumn column = (TableColumn) columns.nextElement();
		// column.setHeaderRenderer(new TableCellRenderer() {
		// @Override
		// public Component getTableCellRendererComponent(JTable table, Object
		// value, boolean isSelected, boolean hasFocus, int row, int column) {
		// JComponent component =
		// (JComponent)table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table,
		// value, false, false, -1, 0);
		// if (column == 0) {
		// component.setBorder(new MatteBorder(1, 1, 1, 1, Color.GRAY));
		// } else {
		// component.setBorder(new MatteBorder(1, 0, 1, 1, Color.GRAY));
		// }
		// return component;
		// }
		// });
		// }

		// Scroll pane for table
		tableScrollPane = new JScrollPane(table);
		border = BorderFactory.createEmptyBorder();
		tableScrollPane.setBorder(BorderFactory.createCompoundBorder(border,
				BorderFactory.createEmptyBorder(1, 3, 3, 3)));

		layoutConstraints.gridx = 0;
		layoutConstraints.gridy = 1;
		layoutConstraints.gridheight = 1;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.weightx = 1;
		layoutConstraints.weighty = 1;
		layoutConstraints.fill = GridBagConstraints.BOTH;

		JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BorderLayout());
		tablePanel.add(tableScrollPane, BorderLayout.CENTER);

		this.add(tablePanel, layoutConstraints);

		// Table buttons panel
		JPanel tableButtonsPanel = new JPanel();

		// addition button
		addButton = new JButton(
				new AbstractAction(CheckerPanel.ADD_BUTTON_TEXT) {
					@Override
					public void actionPerformed(ActionEvent e) {
						String output = JOptionPane.showInputDialog("URL:");
						if (output != null) {
							if (!output.isEmpty()) {
								// add new value
								try {
									@SuppressWarnings("unused")
									URL url = new URL(output);

									tableModel.addValueAtEnd(output);

									// set selection
									table.getSelectionModel()
											.setSelectionInterval(
													table.getModel()
															.getRowCount() - 1,
													table.getModel()
															.getRowCount() - 1);
								} catch (MalformedURLException e1) {
									JOptionPane.showMessageDialog(null,
											CheckerPanel.INVALID_URL_ERROR);
								}
							} else {
								JOptionPane.showMessageDialog(null,
										CheckerPanel.EMPTY_STRING_ERROR);
							}
						}
					}
				});
		addButton.setText(CheckerPanel.ADD_BUTTON_TEXT);
		tableButtonsPanel.add(addButton);

		// editing button
		editButton = new JButton(new AbstractAction(
				CheckerPanel.EDIT_BUTTON_TEXT) {
			@Override
			public void actionPerformed(ActionEvent e) {
				String urlFromRow = (String) tableModel.getUrlForRow(table
						.getSelectedRow());
				String output = JOptionPane.showInputDialog("URL:", urlFromRow);
				if (output != null) {
					if (!output.isEmpty()) {
						if (!output.equals(urlFromRow)) {
							try {
								@SuppressWarnings("unused")
								URL url = new URL(output);

								// set new value
								tableModel.setValueAt(output,
										table.getSelectedRow(), 0);
							} catch (MalformedURLException e1) {
								JOptionPane.showMessageDialog(null,
										CheckerPanel.INVALID_URL_ERROR);
							}
						}
					} else {
						JOptionPane.showMessageDialog(null,
								CheckerPanel.EMPTY_STRING_ERROR);
					}
				}
			}
		});
		editButton.setText(CheckerPanel.EDIT_BUTTON_TEXT);
		editButton.setEnabled(false);
		tableButtonsPanel.add(editButton);

		// removal button
		removeButton = new JButton(new AbstractAction(
				CheckerPanel.REMOVE_BUTTON_TEXT) {
			@Override
			public void actionPerformed(ActionEvent e) {
				int output = JOptionPane.showConfirmDialog(null,
						CheckerPanel.CONFIRM_REMOVAL, "",
						JOptionPane.YES_NO_OPTION);
				if (output == 0) {
					int selectedRow = table.getSelectedRow();
					int rowCount = tableModel.getRowCount();
					ListSelectionModel selectionModel = table
							.getSelectionModel();

					// remove row
					tableModel.removeRowFromIndex(selectedRow);
					rowCount--;

					// set selection
					if (selectedRow > 0 && selectedRow < rowCount) {
						selectionModel.setSelectionInterval(selectedRow,
								selectedRow);
					} else if (selectedRow == 0 && rowCount > 0) {
						selectionModel.setSelectionInterval(0, 0);
					} else if (selectedRow == rowCount) {
						selectionModel.setSelectionInterval(selectedRow - 1,
								selectedRow - 1);
					}

				}
			}
		});
		removeButton.setText(CheckerPanel.REMOVE_BUTTON_TEXT);
		removeButton.setEnabled(false);
		tableButtonsPanel.add(removeButton);

		layoutConstraints = new GridBagConstraints();
		layoutConstraints.gridx = 0;
		layoutConstraints.gridy = 2;
		layoutConstraints.gridheight = 1;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.anchor = GridBagConstraints.LINE_END;
		this.add(tableButtonsPanel, layoutConstraints);

		// Separator between table buttons panel and search panel
		layoutConstraints = new GridBagConstraints();
		layoutConstraints.gridx = 0;
		layoutConstraints.gridy = 3;
		layoutConstraints.gridheight = 1;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.insets = new Insets(0, 5, 0, 5);
		layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
		this.add(new JSeparator(JSeparator.HORIZONTAL), layoutConstraints);

		// Search depth panel
		JPanel searchDepthPanel = new JPanel();

		searchDepthLabel = new JLabel();
		searchDepthLabel.setText(CheckerPanel.SEARCH_DEPTH_LABEL);
		searchDepthPanel.add(searchDepthLabel);

		searchDepthCombo = new JComboBox<String>();
		searchDepthCombo.addItem(CheckerPanel.SEARCH_DEPTH_CURRENT_PAGE);
		searchDepthCombo.addItem(CheckerPanel.SEARCH_DEPTH_ALL_PAGES_DOMAIN);
		searchDepthCombo.addItem("");
		searchDepthCombo.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean editable = searchDepthCombo.getSelectedIndex() == -1
						|| searchDepthCombo.getSelectedIndex() == 2;
				searchDepthCombo.setEditable(editable);
			}
		});
		searchDepthPanel.add(searchDepthCombo);

		layoutConstraints = new GridBagConstraints();
		layoutConstraints.gridx = 0;
		layoutConstraints.gridy = 4;
		layoutConstraints.gridheight = 1;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.anchor = GridBagConstraints.LINE_START;
		this.add(searchDepthPanel, layoutConstraints);

		// HTML and CSS related stuff to be used later for console
		htmlEditorKit = new HTMLEditorKit();
		StyleSheet styleSheet = htmlEditorKit.getStyleSheet();
		styleSheet.addRule("ul {}");
		doc = (HTMLDocument) htmlEditorKit.createDefaultDocument();

		// Find broken links button
		findButton = new JButton(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {

					// if clicked stop, kill the search
					shouldStop = !shouldStop;

					// clear console
					if (!shouldStop) {
						clearConsole();
					}

					// Collect the urls from table
					List<String> urls = new ArrayList<String>();
					for (int i = 0; i < tableModel.getRowCount(); i++) {
						urls.add((String) tableModel.getValueAt(i, 0));
					}

					// Get search depth from combo box
					int selectedIndex = searchDepthCombo.getSelectedIndex();
					String searchDepth = null;
					int searchDepthInt = 0;
					if (selectedIndex != 2 && selectedIndex != -1) {
						searchDepth = (String) searchDepthCombo
								.getSelectedItem();
						if (searchDepth
								.equals(CheckerPanel.SEARCH_DEPTH_CURRENT_PAGE)) {
							searchDepthInt = 1;
						} else if (searchDepth
								.equals(CheckerPanel.SEARCH_DEPTH_ALL_PAGES_DOMAIN)) {
							searchDepthInt = -2; // a special value
						}
					} else {
						searchDepthInt = Integer
								.parseInt((String) searchDepthCombo
										.getSelectedItem());

						// a positive integer is required for the search depth
						if (searchDepthInt <= 0) {
							throw new NumberFormatException();
						}
					}

					final CollectedInfo collectedInfo = new CollectedInfo(urls,
							searchDepthInt);

					// Start new thread and perform search
					final BrokenLinksFinder brokenLinksFinder = new BrokenLinksFinder();
					if (!shouldStop) {
						rowsWithProblems.clear();
						new Thread(new Runnable() {
							@Override
							public void run() {
								brokenLinksFinder.findBrokenLinks(
										collectedInfo, CheckerPanel.this);
							}
						}).start();
					}
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(null,
							CheckerPanel.INVALID_SEARCH_DEPTH);
				}

			}

		});
		findButton.setPreferredSize(new Dimension(111, 23));
		findButton.setText(CheckerPanel.FIND_BUTTON_TEXT);
		findButton.setEnabled(false);
		layoutConstraints.gridx = 0;
		layoutConstraints.gridy = 4;
		layoutConstraints.gridheight = 1;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.anchor = GridBagConstraints.LINE_END;
		layoutConstraints.insets = new Insets(0, 0, 0, 5);
		this.add(findButton, layoutConstraints);

		// Enable and disable buttons as needed
		tableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (tableModel.getRowCount() == 0) {
					editButton.setEnabled(false);
					removeButton.setEnabled(false);
					findButton.setEnabled(false);
				} else {
					editButton.setEnabled(true);
					removeButton.setEnabled(true);
					findButton.setEnabled(true);
				}
			}
		});

		// Console
		JLabel consoleHeader = new JLabel();
		consoleHeader.setText(CheckerPanel.CONSOLE_HEADER);
		layoutConstraints = new GridBagConstraints();
		layoutConstraints.gridx = 1;
		layoutConstraints.gridy = 0;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.gridheight = 1;
		layoutConstraints.weightx = 0;
		layoutConstraints.weighty = 0;
		layoutConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
		layoutConstraints.insets = new InsetsUIResource(2, 4, 0, 0);
		this.add(consoleHeader, layoutConstraints);

		JPanel auxPanel = new JPanel();
		auxPanel.setLayout(new BorderLayout());

		console = new JEditorPane();
		console.setEditable(false);
		console.setEditorKit(htmlEditorKit);
		console.setDocument(doc);
		console.setContentType("text/html");
		border = BorderFactory.createLineBorder(Color.GRAY);
		console.setBorder(BorderFactory.createCompoundBorder(border,
				BorderFactory.createEmptyBorder(3, 13, 3, 30)));
		auxPanel.add(console, BorderLayout.CENTER);

		layoutConstraints = new GridBagConstraints();
		layoutConstraints.gridx = 1;
		layoutConstraints.gridy = 1;
		layoutConstraints.weightx = 1;
		layoutConstraints.weighty = 1;
		layoutConstraints.fill = GridBagConstraints.BOTH;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.gridheight = 3;

		consoleScrollPane = new JScrollPane(auxPanel);
		consoleScrollPane.setPreferredSize(new Dimension(450,
				(int) getPreferredSize().getHeight()));
		border = BorderFactory.createEmptyBorder();
		consoleScrollPane.setBorder(BorderFactory.createCompoundBorder(border,
				BorderFactory.createEmptyBorder(1, 3, 3, 3)));
		this.add(consoleScrollPane, layoutConstraints);

		// Progress bar
		progressBar = new JProgressBar();
		progressBar.setVisible(false);
		progressBar.setStringPainted(true);
		layoutConstraints = new GridBagConstraints();
		layoutConstraints.gridx = 1;
		layoutConstraints.gridy = 4;
		layoutConstraints.weightx = 1;
		layoutConstraints.weighty = 0;
		layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
		layoutConstraints.gridwidth = 1;
		layoutConstraints.gridheight = 1;
		layoutConstraints.insets = new Insets(0, 3, 0, 3);
		layoutConstraints.ipady = 8;

		this.add(progressBar, layoutConstraints);
	}

	@Override
	public void reportProblem(LinkProblem problem, int row) {

		if (!rowsWithProblems.contains(row)) {
			rowsWithProblems.add(row);
		}

		try {
			String htmlToBeInserted;

			// add the line and column of the problem to href, so that their
			// values can be used to set the caret at the corresponding position
			// in Oxygen text editor; see applicationStarted from
			// BrokenLinksCheckerPluginExtension for more info
			URL urlWithLineAndCol = URLParameterManager.addParameter(new URL(
					problem.getParent()),
					CheckerPanel.PROBLEM_LINE_QUERY_PARAM_NAME, Integer
							.toString(problem.getLine()));
			urlWithLineAndCol = URLParameterManager.addParameter(
					urlWithLineAndCol,
					CheckerPanel.PROBLEM_COLUMN_QUERY_PARAM_NAME,
					Integer.toString(problem.getColumn()));
			htmlToBeInserted = "<b>Problem:</b>"
					+ "<ul>"
					+ "  <li>Start page:"
					+ "<span style=\"color:blue; text-decoration: underline;\">"
					+ problem.getStartPageUrl()
					+ "</span></li>"
					+ "  <li>Page where problem found: <a href="
					+ urlWithLineAndCol
					+ ">"
					+ problem.getParent()
					+ "</a></li>"
					+ "  <li>Line: "
					+ problem.getLine()
					+ "</li>"
					+ "  <li>Column: "
					+ problem.getColumn()
					+ "</li>"
					+ "  <li>Problematic link: "
					+ "<span style=\"color:blue; text-decoration: underline;\">"
					+ problem.getProblematicLink()
					+ "</span></li>"
					+ "  <li>Problem description: "
					+ problem.getProblemDescription()
					+ "</li>"
					+ "</ul>"
					+ "<br/>";

			// print new problem to console
			htmlEditorKit.read(new StringReader(htmlToBeInserted), doc,
					doc.getLength());
		} catch (BadLocationException ex) {
			displayError(CheckerPanel.INTERNAL_ERROR_MESSAGE);
			logger.debug(ex);
		} catch (IOException ex) {
			displayError(CheckerPanel.INTERNAL_ERROR_MESSAGE);
			logger.debug(ex);
		}

	}

	@Override
	public void doAfterSearchStarted() {
		for (int i = 0; i < table.getRowCount(); i++) {
			table.getModel().setValueAt("", i, 1);
		}

		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);
		findButton.setText(CheckerPanel.STOP_BUTTON_TEXT);

	}

	@Override
	public void doAfterSearchEnded(boolean isStoppedByUser) {
		if (!isStoppedByUser) {
			shouldStop = !shouldStop;
		}
		progressBar.setVisible(false);
		progressBar.setIndeterminate(false);
		findButton.setText(CheckerPanel.FIND_BUTTON_TEXT);

	}

	@Override
	public boolean shouldStopSearch() {
		if (shouldStop) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Clear console.
	 */
	public void clearConsole() {
		console.setText("");
	}

	@Override
	public void parsingResource(String resource) {
		progressBar.setString(resource);
	}

	@Override
	public void displayError(String message) {
		JOptionPane.showMessageDialog(null, message);
	}

	@Override
	public void setStatus(String status, int row) {
		table.getModel().setValueAt(status, row, 1);
	}

	@Override
	public void doWhenSearchEndedForRow(int row) {
		// Set status for table row
		if (rowsWithProblems.contains(row)) {
			table.getModel().setValueAt(CheckerPanel.PROBLEMS_FOUND_STATUS,
					row, 1);
		} else {
			table.getModel().setValueAt(CheckerPanel.NO_PROBLEMS_FOUND_STATUS,
					row, 1);
		}
	}

	/**
	 * Get console.
	 * 
	 * @return the console.
	 */
	public JEditorPane getConsole() {
		return console;
	}

}
