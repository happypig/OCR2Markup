package dila.reviewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dila.AuthorComponentAdditionalView;
import dila.SplitPaneUtil;
import dila.reviewer.EditModeAuthorDocumentFilter.EditMode;

import ro.sync.azcheck.ui.SpellCheckOptions;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.AuthorReviewController;
import ro.sync.ecss.extensions.api.component.AuthorComponentException;
import ro.sync.ecss.extensions.api.component.AuthorComponentFactory;
import ro.sync.ecss.extensions.api.component.AuthorComponentProvider;
import ro.sync.ecss.extensions.api.component.EditorComponentProvider;
import ro.sync.ecss.extensions.api.component.listeners.AuthorComponentListener;
import ro.sync.ecss.extensions.api.component.listeners.OpenURLHandler;
import ro.sync.ecss.extensions.api.structure.AuthorPopupMenuCustomizer;
import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorComponentEditorPage;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.basic.util.URLUtil;

/**
 * This is an example of how you can build a standalone reviewer application using the Author Component API.
 * The startup scripts for the reviewer application are "reviewer.bat" and "reviewerMac.sh".
 */
@SuppressWarnings("serial")
public class AuthorComponentReviewerSample extends JComponent {
	/**
	 * Logger.
	 */
	static Logger logger = LoggerFactory.getLogger(AuthorComponentReviewerSample.class.getName());

	/**
	 * The actions toolbar
	 */
	private final JToolBar allActionsToolbar;

	/**
	 * The author component factory
	 */
	private AuthorComponentFactory factory;

	/**
	 * The author component.
	 */
	private final EditorComponentProvider editorComponent;

	/**
	 * The author page.
	 */
	private final WSAuthorComponentEditorPage authorPage;

	/**
	 * Side view showing the reviews.
	 */
	private AuthorComponentAdditionalView reviewView;

	/**
	 * Split pane for the editing area and review.
	 */
	private JSplitPane editorAndReviewSplit;

	/**
	 * The filter used to set an editing mode for the document.
	 */
	private final EditModeAuthorDocumentFilter editBlockingDocumentFilter;

	/**
	 * Constructor. Builds the sample.
	 * 
	 * @param frameworkZips
	 *            Array of ZIP URLs containing the archived Oxygen document
	 *            types which will be used for editing (can be null or empty).
	 *            These files contain schemas, catalogs, stylesheets. In the
	 *            oXygen standalone distribution the frameworks are expanded in
	 *            the <code>installation_dir/frameworks</code> folder.
	 * @param optionsZipURL
	 *            Optional URL pointing to a ZIP archive which contains a fixed
	 *            set of options which will be used by the Author component. The
	 *            standalone oXygen options can be exported to a file using the
	 *            menu item <code>Options/Export Global Options</code>
	 * @param servletURL
	 *            URL of license servlet from where to get the floating license key.
	 * @param userName User name to access servlet URL
	 * @param password Password to access servlet URL           
	 * @throws AuthorComponentException
	 */
	public AuthorComponentReviewerSample(URL[] frameworkZips, URL optionsZipURL, String servletURL,
		      String userName,
		      String password)
			throws AuthorComponentException {

		// Getting the component factory
		factory = AuthorComponentFactory.getInstance();
		factory.init(frameworkZips, optionsZipURL, null, null, servletURL, userName, password);

		// Enable spell checking.
		SpellCheckOptions spellCheckOptions = factory.getSpellCheckOptions();
		spellCheckOptions.automaticSpellCheck = true;
		factory.setSpellCheckOptions(spellCheckOptions);

		// Create the AuthorComponent
		editorComponent = factory.createEditorComponentProvider(
				new String[] { EditorPageConstants.PAGE_AUTHOR },
				// The initial page
				EditorPageConstants.PAGE_AUTHOR);
		authorPage = (WSAuthorComponentEditorPage) editorComponent.getWSEditorAccess()
				.getCurrentPage();

		// Show the Bread Crumb
		authorPage.showBreadCrumb(true);

		// Set the document filter to block editing.
		AuthorDocumentController controller = authorPage.getAuthorAccess().getDocumentController();
		editBlockingDocumentFilter = new EditModeAuthorDocumentFilter(controller);
		controller.setDocumentFilter(editBlockingDocumentFilter);
		editBlockingDocumentFilter.setEditMode(EditMode.TEXT);

		// Open a clicked link in the same component
		factory.setOpenURLHandler(new OpenURLHandler() {
			public void handleOpenURL(final URL url) throws IOException {
				final IOException[] ex = new IOException[1];
				Runnable runnableWrapper = new Runnable() {
					public void run() {
						try {
							editorComponent.showLocation(url, null);
						} catch (AuthorComponentException e) {
							ex[0] = new IOException(e.getMessage(), e);
						}
					}
				};

				if (SwingUtilities.isEventDispatchThread()) {
					runnableWrapper.run();
				} else {
					// This will replace the content of the editor. If not done
					// of AWT concurrency issues can arise.
					try {
						SwingUtilities.invokeAndWait(runnableWrapper);
					} catch (InterruptedException e) {
						logger.error(e, e);
					} catch (InvocationTargetException e) {
						logger.error(e, e);
					}
				}

				if (ex[0] != null) {
					throw ex[0];
				}
			}
		});

		// Layout.
		setLayout(new BorderLayout());

		allActionsToolbar = new JToolBar();

		// Add the component in the center.
		add(createSampleUIControls());
	}

	/**
	 * Reconfigure the actions toolbar
	 */
	private void reconfigureActionsToolbar() {
		JToolBar actionsToolbar = new JToolBar();
		actionsToolbar.setFloatable(false);
		actionsToolbar.removeAll();

		// Show "Open & Save file" actions
		AbstractAction openAction = new AbstractAction("Open", createIcon("/images/Open24.png")) {
			public void actionPerformed(ActionEvent e) {
				openFile();
			}
		};
		AbstractAction saveAction = new AbstractAction("Save", createIcon("/images/Save24.png")) {
			public void actionPerformed(ActionEvent e) {
				saveFile();
			}
		};
		actionsToolbar.add(new ToolbarButton(openAction, false));
		actionsToolbar.add(new ToolbarButton(saveAction, false));
		actionsToolbar.addSeparator();

		actionsToolbar.add(createEditModeChooser());
		actionsToolbar.addSeparator();

		if (editBlockingDocumentFilter.getEditMode().allows(EditMode.TEXT)) {
			// Cut/Copy/Paste
			actionsToolbar.add(createToolbarButton("Edit/Edit_Cut"));
			actionsToolbar.add(createToolbarButton("Edit/Edit_Copy"));
			actionsToolbar.add(createToolbarButton("Edit/Edit_Paste"));
			actionsToolbar.addSeparator();
		}
		
		// Undo/Redo
		actionsToolbar.add(createToolbarButton("Edit/Edit_Undo"));
		actionsToolbar.add(createToolbarButton("Edit/Edit_Redo"));
		actionsToolbar.addSeparator();

		// Find replace and check spelling.
		actionsToolbar.add(createToolbarButton("Find/Find_replace"));
		actionsToolbar.add(createToolbarButton("Spelling/Check_spelling"));
		actionsToolbar.addSeparator();

		// Review toolbar.
		allActionsToolbar.removeAll();
		allActionsToolbar.add(actionsToolbar);
		allActionsToolbar.add(authorPage.createReviewToolbar());

		if (editBlockingDocumentFilter.getEditMode().allows(EditMode.STRUCTURE)) {
			// Add the toolbars specified in the defined document type.
			for (JToolBar toolbar : authorPage.createExtensionActionsToolbars()) {
				allActionsToolbar.add(toolbar);
			}
		}

		if (allActionsToolbar.getParent() != null) {
			allActionsToolbar.getParent().invalidate();
			allActionsToolbar.revalidate();
		}
		allActionsToolbar.repaint();
	}

	/**
	 * Creates a combo box responsible with choosing the edit mode.
	 * 
	 * @return the combo box to be added on the toolbar.
	 */
	private JComboBox<EditMode> createEditModeChooser() {
		final JComboBox<EditMode> editModeChooser = new JComboBox<>();
		for (EditMode editMode : EditMode.values()) {
			editModeChooser.addItem(editMode);
		}
		editModeChooser.setSelectedItem(editBlockingDocumentFilter.getEditMode());
		editModeChooser.addActionListener(new ActionListener() {
			/**
			 * Sets the edit mode and reconfigures the toolbar.
			 */
			public void actionPerformed(ActionEvent e) {
				EditMode editMode = (EditMode) editModeChooser.getSelectedItem();
				// If we switch to a more restrictive edit mode, we flush the undo manager.
				if (!editMode.allows(editBlockingDocumentFilter.getEditMode())) {
					authorPage.getAuthorAccess().getDocumentController().getUndoManager()
							.discardAllEdits();
				}
				editBlockingDocumentFilter.setEditMode(editMode);
				reconfigureActionsToolbar();
			}
		});
		editModeChooser.setMaximumSize(editModeChooser.getPreferredSize());
		return editModeChooser;
	}

	/**
	 * Creates a toolbar button for the action with the specified name.
	 * 
	 * @param actionName
	 *            the name of the action.
	 * @return the toolbar button that triggers the action.
	 */
	private ToolbarButton createToolbarButton(String actionName) {
		return new ToolbarButton((Action) authorPage.getActionsProvider()
				.getAuthorCommonActions().get(actionName), false);
	}

	/**
	 * Creates a sample editor panel.
	 * 
	 * @return The sample panel
	 */
	private JPanel createSampleUIControls() {
		JPanel samplePanel = new JPanel(new BorderLayout());

		// Add toolbar in north
		allActionsToolbar.setFloatable(false);
		allActionsToolbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 24, 4));	
		samplePanel.add(allActionsToolbar, BorderLayout.NORTH);

		// Editor, outline and review panels
		editorAndReviewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		SplitPaneUtil.minimizeUI(editorAndReviewSplit);
		editorAndReviewSplit.setDividerLocation(0.8);
		editorAndReviewSplit.setResizeWeight(1);
		editorAndReviewSplit.setLeftComponent(editorComponent.getEditorComponent());
		reviewView = new AuthorComponentAdditionalView("Review",
				editorComponent.getAdditionalEditHelper(AuthorComponentProvider.REVIEWS_PANEL_ID));
		editorAndReviewSplit.setRightComponent(reviewView);

		samplePanel.add(editorAndReviewSplit, BorderLayout.CENTER);
		samplePanel.add(editorComponent.getStatusComponent(), BorderLayout.SOUTH);

		// Add the editor Pop-up Customizer
		authorPage.addPopUpMenuCustomizer(new AuthorPopupMenuCustomizer() {
			public void customizePopUpMenu(Object menu, AuthorAccess authorAccess) {
				((JPopupMenu) menu).addSeparator();
				((JPopupMenu) menu).add(new AbstractAction("Toggle Auto Spell Check") {
					public void actionPerformed(ActionEvent e) {
						SpellCheckOptions spellCheckOptions = factory.getSpellCheckOptions();
						spellCheckOptions.automaticSpellCheck = !spellCheckOptions.automaticSpellCheck;
						factory.setSpellCheckOptions(spellCheckOptions);
					}
				});
			}
		});

		// Reconfigures the toolbar when the document type detection changes.
		editorComponent.addAuthorComponentListener(new AuthorComponentListener() {
			public void modifiedStateChanged(boolean modified) {
				//
			}

			/**
			 * Reconfigures the actions toolbar
			 * 
			 * @see ro.sync.ecss.extensions.api.component.listeners.AuthorComponentListener#documentTypeChanged()
			 */
			public void documentTypeChanged() {
				invokeOnAWT(new Runnable() {
					public void run() {
						reconfigureActionsToolbar();
					}
				});
			}

			/**
			 * Reconfigures the actions toolbar.
			 * 
			 * @see ro.sync.ecss.extensions.api.component.listeners.AuthorComponentListener#loadedDocumentChanged()
			 */
			public void loadedDocumentChanged() {
				invokeOnAWT(new Runnable() {
					public void run() {
						// Start in track changes mode.
						AuthorReviewController reviewController = authorPage.getAuthorAccess()
								.getReviewController();
						if (!reviewController.isTrackingChanges()) {
							reviewController.toggleTrackChanges();
						}

						reconfigureActionsToolbar();
					}
				});
			}
		});

		return samplePanel;
	}

	/**
	 * Set a new content
	 * 
	 * @param xmlSystemId
	 *            The system ID (URL) of the XML content, used to solve images,
	 *            etc
	 * @param xmlContent
	 *            The reader over the content.
	 * @throws AuthorComponentException
	 */
	private void setDocument(final String xmlSystemId, final Reader xmlContent)
			throws AuthorComponentException {
		try {
			editorComponent.load(
					(xmlSystemId != null && xmlSystemId.trim().length() > 0) ? new URL(xmlSystemId)
							: null, xmlContent);
		} catch (Exception e) {
			throw new AuthorComponentException(e);
		}
	};

	/**
	 * Public method, used from the JavaScript. It uses the AWT thread to do the
	 * real execution, since the security manager will not allow operations on
	 * the browser thread.
	 * 
	 * @param url
	 *            The URL of the file to load, can be null if XML content is
	 *            specified. If no XML content is given, the URL will be used
	 *            both to obtain the content and to solve relative references
	 *            (eg: images). If the XML content is also given, the URL will
	 *            only be used to solve relative references from the file.
	 * 
	 * @param xmlContent
	 *            The xml content.
	 * @throws Exception
	 */
	public void setDocumentContent(final String url, final String xmlContent)
			throws AuthorComponentException {
		final Exception[] recorded = new Exception[1];
		invokeOnAWT(new Runnable() {
			public void run() {
				try {
					setDocument(url,
							xmlContent != null && xmlContent.length() > 0 ? new StringReader(
									xmlContent) : null);
				} catch (Exception ex) {
					recorded[0] = ex;
				}
			}
		});
		if (recorded[0] != null) {
			throw new AuthorComponentException(recorded[0]);
		}
	}

	/**
	 * Creates the toolbar icon from the file with the given name.
	 * 
	 * @return The icon.
	 */
	private ImageIcon createIcon(String filename) {
		ImageIcon icon = null;
		InputStream is = AuthorComponentReviewerSample.class.getResourceAsStream(filename);
		try {
			icon = new ImageIcon(ImageIO.read(is));
		} catch (IOException e) {
			logger.warn("Cannot load icon: " + filename, e);
		}
		return icon;
	}

	/**
	 * Saves the content of the document in the given file.
	 */
	private void saveFile() {
		editorComponent.getWSEditorAccess().save();
	}


	/**
	 * Shows a file chooser and opens the selected file.
	 */
	private void openFile() {
		JFileChooser fc = new JFileChooser();
		FileFilter fileFilter = new FileFilter() {
			public String getDescription() {
				return "XML files";
			}

			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".xml")
						|| f.getName().endsWith(".dita");
			}
		};
		fc.setFileFilter(fileFilter);
		int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			try {
				editorComponent.load(URLUtil.correct(file.toURI().toURL()),
						null);
			} catch (MalformedURLException e1) {
				JOptionPane.showMessageDialog(null, e1.getMessage());
			} catch (AuthorComponentException e1) {
				JOptionPane.showMessageDialog(null, "AuthorComponent problem: "
						+ e1.getMessage());
			}
		}
	}

	/**
	 * Invokes a runnable on AWT. Useful when calling Java methods from
	 * JavaScript.
	 * 
	 * @param runnable
	 *            The runnable
	 */
	private static void invokeOnAWT(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (InterruptedException e) {
				logger.error(e, e);
			} catch (InvocationTargetException e) {
				logger.error(e, e);
			}
		}
	}
}
