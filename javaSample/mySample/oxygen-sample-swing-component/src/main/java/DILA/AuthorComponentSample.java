package DILA;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.azcheck.ui.SpellCheckOptions;
import ro.sync.document.DocumentPositionedInfo;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.component.AuthorComponentException;
import ro.sync.ecss.extensions.api.component.AuthorComponentFactory;
import ro.sync.ecss.extensions.api.component.AuthorComponentProvider;
import ro.sync.ecss.extensions.api.component.EditorComponentProvider;
import ro.sync.ecss.extensions.api.component.PopupMenuCustomizer;
import ro.sync.ecss.extensions.api.component.listeners.AuthorComponentListener;
import ro.sync.ecss.extensions.api.component.listeners.OpenURLHandler;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.api.structure.AuthorPopupMenuCustomizer;
import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.WSTextBasedEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorComponentEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.editor.validation.ValidationProblems;
import ro.sync.exml.workspace.api.editor.validation.ValidationProblemsFilter;
import ro.sync.exml.workspace.api.listeners.WSEditorPageChangedListener;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.basic.util.URLUtil;

/**
 * Sample making use of an Author component. This can be used as a starting
 * point for creating web frontends to your CMS.
 * 
 * @author dan
 */
@SuppressWarnings("serial")
public class AuthorComponentSample extends JComponent {

	/**
	 * The default document content.
	 */
	public static final String DEFAULT_DOC_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<!DOCTYPE concept PUBLIC \"-//OASIS//DTD DITA Concept//EN\" \"http://docs.oasis-open.org/dita/v1.1/OS/dtd/concept.dtd\">\n"
			+ "<concept id=\"conceptId\">\n"
			+ "    <title>Winter Flowers</title>\n"
			+ "    <conbody>\n"
			+ "        <p>Winter is the season of cold weather. The season occurs during December - February in\n"
			+ "            Northern hemisphere . In the Southern hemisphere winter occurs during June - August. </p>\n"
			+ "        <p>Some of the flowers blooming in winter are: Acashia, Alstromeria, Amaryllis, Carnation,\n"
			+ "            Chrysanthemums, Cyclamen, Evergreens, Gerbera Daisy, Ginger, Helleborus, Holly berry,\n"
			+ "            Lily, Asiatic Lily, Casa Blanca Lily, Narcissus, Orchid, Pansy, Pepperberry, Phlox,\n"
			+ "            Protea, Queen Ann's Lace, Roses, Star of Bethlehem, Statice. </p>\n"
			+ "    </conbody>\n" + "</concept>\n" + "";

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(AuthorComponentSample.class
			.getName());

	/**
	 * The actions toolbar
	 */
	private JPanel allActionsToolbar;

	/**
	 * The author component factory
	 */
	private final AuthorComponentFactory factory;

	/**
	 * <code>true</code> if the "Open" action should be available on toolbar.
	 */
	private final boolean showBrowseLocal;

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
	 * @param codeBase
	 *            The applet's code base (can be null if not run from an
	 *            applet), used to create an unique path where to store the
	 *            component's expanded document types.
	 * @param appletID
	 *            The applet's ID (can be null if not run from an applet), used
	 *            to create an unique path where to store the component's
	 *            expanded document types.
	 * @param addToolbar
	 *            <code>true</code> to add toolbar.
	 * @param addHelperViews
	 *            <code>true</code> to add helper views (Outline, Attributes,
	 *            etc)
	 * @param showBreadCrumb
	 *            <code>true</code> if the breadcrumb should be visible.
	 * @param showBrowseLocal
	 *            <code>true</code> if the applet should contain on toolbar
	 *            "Open" action (local).
	 * 
	 * @throws AuthorComponentException
	 */
	public AuthorComponentSample(URL[] frameworkZips, URL optionsZipURL,
			URL codeBase, String appletID, boolean addToolbar,
			boolean addHelperViews, final boolean showBreadCrumb,
			boolean showBrowseLocal) throws AuthorComponentException {

		this.showBrowseLocal = showBrowseLocal;

		// Getting the component factory
		factory = AuthorComponentFactory.getInstance();

		// See the other AuthorComponentFactory.init methods for different
		// ways to license the component: Floating License Server/Floating
		// License HTTP Servlet.
		factory.init(frameworkZips, optionsZipURL, codeBase, appletID,
				// TODO set here licensing details
				"http://licenseServletURL/", "user", "password");
		SpellCheckOptions spellCheckOptions = factory.getSpellCheckOptions();
		spellCheckOptions.automaticSpellCheck = true;
		factory.setSpellCheckOptions(spellCheckOptions);

		// Set fixed MathFlow licenses if you want to edit embedded MathML
		// equations with Mathflow
		// factory.setMathFlowFixedLicenseKeyForComposer("kkkkkkk-kkkkkk-kkkkk");
		// factory.setMathFlowFixedLicenseKeyForEditor("kkkkkkk-kkkkkk-kkkkk");

		// TODO THE USUAL WAY TO LICENSE THE COMPONENT WHEN USED AS A WEB APPLET
		// FOR DEPLOYMENT TO CLIENTS
		// IS BY PROVIDING THE ADDRESS OF A LICENSE SERVLET:
		// http://www.oxygenxml.com/license_server.html
		// WHICH HAS BEEN CONFIGURED TO SERVE FLOATING AUTHOR LICENSES.
		// factory.init(frameworkZips, optionsZipURL, codeBase, appletID,
		// //The servlet URL
		// "http://www.host.com/servlet",
		// //The HTTP credentials user name
		// "userName",
		// //The HTTP credentials password
		// "password");

		// Uncomment the lines below to add your special browse CMS action to
		// all input URL panels in all dialogs which
		// can be shown by the author component
		// Adds a customizer which can modify the list of "Browse" actions.
		// These actions are available in the component, in any control or
		// dialog that contains an URL input box.
		// factory.addInputURLChooserCustomizer(new InputURLChooserCustomizer()
		// {
		// public void customizeBrowseActions(List<Action> allActions, final
		// InputURLChooser chooser) {
		// allActions.clear();
		// AbstractAction customizeAction = new
		// AbstractAction("Custom CMS Action") {
		// public void actionPerformed(ActionEvent e) {
		// //Show your CMS browse dialog here.
		// //Then set the URL in the chooser.
		// try {
		// chooser.urlChosen(new URL("file://C:/file.xml"));
		// } catch (MalformedURLException e1) {
		// e1.printStackTrace();
		// }
		// }
		// };
		//
		// // Put an icon as it will be presented in a toolbar.
		// customizeAction.putValue(Action.SMALL_ICON,
		// Icons.getIcon(Icons.ADD));
		// allActions.add(customizeAction);
		// }
		// });

		// Create the AuthorComponent
		editorComponent = factory.createEditorComponentProvider(
				new String[] {
						// Comment this if you do not want a text page in the
						// component.
						EditorPageConstants.PAGE_TEXT,
						EditorPageConstants.PAGE_AUTHOR },
				// The initial page
				EditorPageConstants.PAGE_AUTHOR);

		if (EditorPageConstants.PAGE_AUTHOR.equals(editorComponent
				.getWSEditorAccess().getCurrentPageID())) {
			// Show/Hide Bread Crumb
			((WSAuthorComponentEditorPage) editorComponent.getWSEditorAccess()
					.getCurrentPage()).showBreadCrumb(showBreadCrumb);
		}

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
					// of AWT
					// concurrency issues can arise.
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

		if (addToolbar) {
			allActionsToolbar = new JPanel(new WrapToolbarLayout(FlowLayout.LEFT));
			allActionsToolbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 24, 4));			
		}

		// Add the component in the center.
		add(createSampleUIControls(false, addToolbar, addHelperViews));
	}

	/**
	 * Invokes a runnable on AWT. Useful when calling Java methods from
	 * JavaScript.
	 * 
	 * @param runnable
	 *            The runnable
	 */
	public static void invokeOnAWT(Runnable runnable) {
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

	/**
	 * The author component.
	 */
	private final EditorComponentProvider editorComponent;

	/**
	 * Side view showing the outline.
	 */
	private AuthorComponentAdditionalView outlineView;

	/**
	 * Side view showing the attributes of the current element.
	 */
	private AuthorComponentAdditionalView attributesView;

	/**
	 * Side view showing the elements that can be inserted.
	 */
	private AuthorComponentAdditionalView elementsView;

	/**
	 * Side view showing the reviews.
	 */
	private AuthorComponentAdditionalView reviewView;
	/**
	 * Side view showing the validation problems.
	 */
	private AuthorComponentAdditionalView validationProblemsView;

	/**
	 * Split pane for the outline and editing area.
	 */
	private JSplitPane editorAndOutlineSplit;

	/**
	 * Split pane for the attrbutes and elements lists.
	 */
	private JSplitPane attributesAndElementsSplit;

	/**
	 * The split pane between the (outline + editor) and (attributes + elements)
	 */
	private JSplitPane centerPanel;
	/**
	 * Split pane for the editing area and review.
	 */
	private JSplitPane editorAndReviewSplit;

	/**
	 * The review view should be visible in the Author page. 
	 */
	private boolean reviewViewShouldBeShowingInTheAuthorPage = true;

//	/**
//	 * The DITA Map component sample, commented out by default.
//	 */
//	private DITAMapComponentSample ditaMapSample;

	/**
	 * Brings up a frame with the component sample.
	 * 
	 * @param args
	 *            ignored.
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			URL frameworksZipURL = AuthorComponentSample.class.getResource("/frameworks.zip");
			URL optionsZipURL = AuthorComponentSample.class.getResource("/options.zip");

			logger.info("Loading options from: " + optionsZipURL);
			logger.info("Loading frameworks from: " + frameworksZipURL);
			
			JFrame frame = new JFrame("Author Component Sample Application");
			AuthorComponentSample sample = new AuthorComponentSample(
					// Frameworks Zip
					new URL[] { frameworksZipURL },
					// Options Zip
					optionsZipURL, new File("dist").toURI().toURL(), "ApplID",
					true, true, true, true);

			sample.setDocumentContent(null, DEFAULT_DOC_CONTENT);

			frame.setSize(1000, 800);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(sample);
			frame.setVisible(true);

		} catch (Exception e) {
			logger.error(e, e);
		}
	}

	/**
	 * Check if the text from the editor was modified.
	 * 
	 * @return true if it was modified.
	 */
	public boolean isModified() {
		return editorComponent.getWSEditorAccess().isModified();
	}
	
	/**
	 * Set the document's modified flag to a certain state. 
	 * 
	 * @param modified <code>true</code> to mark the document as modified, <code>false</code> to mark it as not dirty.
	 */
	public void setModified(final boolean modified) {
		//EXM-27477 This method might come from Javascript code, elevate privileges.
		AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				invokeOnAWT(new Runnable() {
					public void run() {
						editorComponent.getWSEditorAccess().setModified(modified);
					}
				});
				return null;
			}
		});
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
			editorComponent.load((xmlSystemId != null && xmlSystemId.trim()
					.length() > 0) ? new URL(xmlSystemId) : null, xmlContent);
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
	  Runnable runnable = new Runnable() {
	    public void run() {
	      //EXM-27477 This method might come from Javascript code, elevate privileges.
	      AccessController.doPrivileged(new PrivilegedAction<String>() {
	        public String run() {
	          invokeOnAWT(new Runnable() {
	            public void run() {
	              try {
	                StringReader reader = null;
                  if (xmlContent != null && xmlContent.length() > 0) {                    
                    reader = new StringReader(xmlContent);
                  }
                  setDocument(url, reader);
	              } catch (Exception ex) {
	                ex.printStackTrace();
	              }
	            }
	          });
	          return null;
	        }
	      });
	    }
	  };
	  // Fix for Chrome.
	  // If you don't run the code on another thread, the Chrome Browser will become unresponsive.
	  new Thread(runnable).start();
	}

	/**
	 * Controls the visibility state of the side views.
	 * 
	 * @param sideViewName
	 *            The name of the side view. Can be one of <code>outline</code>,
	 *            <code>attributes</code>, <code>elements</code>.
	 * @param visible
	 *            <code>true</code> makes the side view visible,
	 *            <code>false</code> otherwise.
	 * @throws AuthorComponentException
	 *             when the visibility change failed.
	 */
	public void setVisibleSideView(final String sideViewName,
			final boolean visible) throws AuthorComponentException {
		if ("review".equals(sideViewName)) {
			this.reviewViewShouldBeShowingInTheAuthorPage = visible;
		}
		setVisibleSideViewInternal(sideViewName, visible);
	}

	/**
	 * Controls the visibility state of the side views.
	 * 
	 * @param sideViewName
	 *            The name of the side view. Can be one of <code>outline</code>,
	 *            <code>attributes</code>, <code>elements</code>.
	 * @param visible
	 *            <code>true</code> makes the side view visible,
	 *            <code>false</code> otherwise.
	 * @throws AuthorComponentException
	 *             when the visibility change failed.
	 */
	private void setVisibleSideViewInternal(final String sideViewName,
			final boolean visible) throws AuthorComponentException {
		final Exception[] ex = new Exception[1];
		//EXM-27477 This method might come from Javascript code, elevate privileges.
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				try {
					if ("outline".equals(sideViewName)) {
						outlineView.setVisible(visible);
					} else if ("attributes".equals(sideViewName)) {
						attributesView.setVisible(visible);
						if (visible) {
							attributesAndElementsSplit.setVisible(true);
						}
					} else if ("elements".equals(sideViewName)) {
						elementsView.setVisible(visible);
						if (visible) {
							attributesAndElementsSplit.setVisible(true);
						}
					} else if ("validationProblems".equals(sideViewName)) {
						validationProblemsView.setVisible(visible);
					} else if ("review".equals(sideViewName)) {
						if (!visible
								|| EditorPageConstants.PAGE_AUTHOR
										.equals(editorComponent
												.getWSEditorAccess()
												.getCurrentPageID())) {
							//Review view can be null in form controls demo.
							if(reviewView != null) {
								reviewView.setVisible(visible);
							}
						}
					}

					//Attributes and elements views can be null in form controls demo.
					if((elementsView == null || ! elementsView.isVisible())
							&& (attributesView == null || ! attributesView.isVisible())
							&& attributesAndElementsSplit != null) {
						attributesAndElementsSplit.setVisible(false);
					}

					// Forces relayout.
					if(attributesAndElementsSplit != null) {
						attributesAndElementsSplit.setDividerLocation(0.5);
					}
					if(editorAndOutlineSplit != null) {
						editorAndOutlineSplit.setDividerLocation(200);
					}
					if(editorAndReviewSplit != null) {
						editorAndReviewSplit.setDividerLocation(0.8);
					}
					if(centerPanel != null) {
						centerPanel.setDividerLocation(0.8);
					}
					invalidate();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							invalidate();
							revalidate();
							repaint();
						}
					});

				} catch (Exception e) {
					ex[0] = e;
				}
				return null;
			}
		});
		if (ex[0] != null) {
			throw new AuthorComponentException(ex[0]);
		}
	}

	/**
	 * Get the content serialized back to XML from the component
	 * 
	 * @return The content serialized back to XML from the component
	 */
	public String getSerializedDocument() {
		//EXM-27477 This method might come from Javascript code, elevate privileges.
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				StringBuilder builder = new StringBuilder();
				BufferedReader reader = new BufferedReader(editorComponent
						.getWSEditorAccess().createContentReader());
				try {
					char[] cb = new char[1024];
					int r;
					r = reader.read(cb);
					while (r != -1) {
						builder.append(cb, 0, r);
						r = reader.read(cb);
					}
				} catch (Exception e) {
					// Does not happen.
					logger.error(e, e);
				}
				return builder.toString();
			}
		});
	}

	/**
	 * Changes the way the editor displays the XML tags.
	 * 
	 * @param mode
	 *            Can be one of <code>full_tags</code>,
	 *            <code>full_tags_with_attributes</code>, <code>no_tags</code>,
	 *            <code>partial_tags</code>.
	 * 
	 * @throws AuthorComponentException
	 *             when the change of the tags display fails.
	 */
	public void setTagsDisplayMode(final String mode) {
		//EXM-27477 This method might come from Javascript code, elevate privileges.
		AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				if (EditorPageConstants.PAGE_AUTHOR.equals(editorComponent
						.getWSEditorAccess().getCurrentPageID())) {
					Map<String, Object> authorCommonActions = ((WSAuthorComponentEditorPage) editorComponent
							.getWSEditorAccess().getCurrentPage()).getActionsProvider()
							.getAuthorCommonActions();
					if ("full_tags_with_attributes".equals(mode)) {
						((Action) authorCommonActions
								.get("Author/Full_tags_with_attributes"))
								.actionPerformed(null);
					} else if ("full_tags".equals(mode)) {
						((Action) authorCommonActions.get("Author/Full_tags"))
						.actionPerformed(null);
					} else if ("partial_tags".equals(mode)) {
						((Action) authorCommonActions.get("Author/Partial_tags"))
						.actionPerformed(null);
					} else if ("no_tags".equals(mode)) {
						((Action) authorCommonActions.get("Author/No_tags"))
						.actionPerformed(null);
					}
				}
				return null;
			}
		});
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
	 * Reconfigure the actions toolbar
	 */
	private void reconfigureActionsToolbar() {
		if (allActionsToolbar != null) {
			JToolBar actionsToolbar = new JToolBar();
			actionsToolbar.setFloatable(false);

			actionsToolbar.removeAll();

			// Workspace access Plugins can also be distributed with the author
			// component.
			// Add Acrolinx toolbar to Author Component Sample.
			// List<ToolbarComponentsCustomizer> customizers =
			// AuthorComponentFactory.getInstance().getPluginToolbarCustomizers();
			// ToolbarInfo ti = new ToolbarInfo("acrolinx_toolbar", new
			// JComponent[0], "The toolbar");
			// if(customizers.size() > 0) {
			// customizers.get(0).customizeToolbar(ti);
			// JComponent[] newComps = ti.getComponents();
			// for (int i = 0; i < newComps.length; i++) {
			// actionsToolbar.add(newComps[i]);
			// }
			// }

			if (showBrowseLocal) {
				// Show "Open file" action
				AbstractAction openAction = new AbstractAction("Open",
						createOpenIcon()) {
					public void actionPerformed(ActionEvent e) {
						// Open the file
						openFile();
					}
				};
				actionsToolbar.add(new ToolbarButton(openAction, false));
				// Add separator
				actionsToolbar.addSeparator();
			}

			if (EditorPageConstants.PAGE_AUTHOR.equals(editorComponent
					.getWSEditorAccess().getCurrentPageID())) {
				WSAuthorComponentEditorPage authorPage = (WSAuthorComponentEditorPage) editorComponent
						.getWSEditorAccess().getCurrentPage();

				Map<String, Object> authorCommonActions = authorPage
						.getActionsProvider().getAuthorCommonActions();

				// You can look in the list of actions to see what's available.
				// Uncomment this if you want to add the character map insertion
				// button.
//				 actionsToolbar.add(new ToolbarButton((Action) authorCommonActions
//				 .get("Edit/Insert_from_Character_Map"), false));

				// Cut/Copy/Paste
				actionsToolbar.add(new ToolbarButton(
						(Action) authorCommonActions.get("Edit/Edit_Cut"),
						false));
				actionsToolbar.add(new ToolbarButton(
						(Action) authorCommonActions.get("Edit/Edit_Copy"),
						false));
				actionsToolbar.add(new ToolbarButton(
						(Action) authorCommonActions.get("Edit/Edit_Paste"),
						false));
				actionsToolbar.addSeparator();

				// Undo/Redo
				actionsToolbar.add(new ToolbarButton(
						(Action) authorCommonActions.get("Edit/Edit_Undo"),
						false));
				actionsToolbar.add(new ToolbarButton(
						(Action) authorCommonActions.get("Edit/Edit_Redo"),
						false));
				actionsToolbar.addSeparator();

				// Uncomment this to set the author component as read only
				// AbstractAction readOnlyAction = new AbstractAction("R O") {
				// boolean editable = true;
				// public void actionPerformed(ActionEvent arg0) {
				// authorComponent.getAuthorAccess().getEditorAccess().setEditable(!editable);
				// editable = !editable;
				// }
				// };
				// readOnlyAction.putValue(Action.SHORT_DESCRIPTION,
				// "Read Only");
				// actionsToolbar.add(new JToggleButton(readOnlyAction));

				actionsToolbar.add(new ToolbarButton(
						(Action) authorCommonActions.get("Find/Find_replace"),
						false));
				actionsToolbar.add(new ToolbarButton(
						(Action) authorCommonActions
								.get("Spelling/Check_spelling"), false));
				actionsToolbar.addSeparator();

				// Review toolbar.
				allActionsToolbar.removeAll();
				allActionsToolbar.add(actionsToolbar);
				
				//Basic Author toolbar
				JToolBar basicAuthorToolbar = authorPage.createBasicAuthorToolbar();
				allActionsToolbar.add(basicAuthorToolbar);
				
				//Review toolbar
				JToolBar reviewToolbar = authorPage.createReviewToolbar();
				allActionsToolbar.add(reviewToolbar);
				
				//CSS alternatives toolbar.
				JToolBar cssAlternatives = authorPage.createCSSAlternativesToolbar();
				allActionsToolbar.add(cssAlternatives);

				// Add exactly the same toolbars specified in the defined
				// document type.
				List<JToolBar> tbs = authorPage
						.createExtensionActionsToolbars();
				for (int i = 0; i < tbs.size(); i++) {
					JToolBar tb = tbs.get(i);
					allActionsToolbar.add(tb);
				}

				// OR YOU CAN USE THIS MAP OF EXTENSION ACTIONS TO PICK THE
				// ACTIONS TO SHOW ON THE TOOLBAR.
				// authorComponent.getAuthorExtensionActions();
			} else if (EditorPageConstants.PAGE_TEXT.equals(editorComponent
					.getWSEditorAccess().getCurrentPageID())) {
				// Maybe running in the Text page.
				Map<String, Object> textCommonActions = ((WSTextEditorPage) editorComponent
						.getWSEditorAccess().getCurrentPage())
						.getActionsProvider().getTextActions();

				// You can look in the list of actions to see what's available.
				// Uncomment this if you want to add the character map insertion
				// button.
				// actionsToolbar.add(new ToolBarButton(authorCommonActions
				// .get("Edit/Insert_from_Character_Map")));

				// Cut/Copy/Paste
				actionsToolbar.add(new ToolbarButton((Action) textCommonActions
						.get("Edit/Edit_Cut"), false));
				actionsToolbar.add(new ToolbarButton((Action) textCommonActions
						.get("Edit/Edit_Copy"), false));
				actionsToolbar.add(new ToolbarButton((Action) textCommonActions
						.get("Edit/Edit_Paste"), false));
				actionsToolbar.addSeparator();

				// Undo/Redo
				actionsToolbar.add(new ToolbarButton((Action) textCommonActions
						.get("Edit/Edit_Undo"), false));
				actionsToolbar.add(new ToolbarButton((Action) textCommonActions
						.get("Edit/Edit_Redo"), false));
				actionsToolbar.addSeparator();

				actionsToolbar.add(new ToolbarButton((Action) textCommonActions
						.get("Find/Find_replace"), false));
				actionsToolbar.add(new ToolbarButton((Action) textCommonActions
						.get("Spelling/Check_spelling"), false));
				actionsToolbar.addSeparator();

				allActionsToolbar.removeAll();
				allActionsToolbar.add(actionsToolbar);
			} else {
				// Maybe running in the Grid page.
				allActionsToolbar.removeAll();
			}
			if (allActionsToolbar.getParent() != null) {
				allActionsToolbar.getParent().invalidate();
				allActionsToolbar.revalidate();
			}
			allActionsToolbar.repaint();
		}
	}

	/**
	 * Creates the toolbar icon for the 'Open File' action.
	 * 
	 * @return The icon for 'Open File' action.
	 */
	private ImageIcon createOpenIcon() {
		ImageIcon icon = null;
		InputStream is = AuthorComponentSample.class
				.getResourceAsStream("/images/OpenApplet.png");
		try {
			icon = new ImageIcon(ImageIO.read(is));
		} catch (IOException e) {
		}
		return icon;
	}
	
	
	/**
	 * Creates a sample editor panel.
	 * 
	 * @param addStatus
	 *            Add the status bar
	 * @param addToolbar
	 *            True to add a toolbar
	 * @return The sample panel
	 */
	private JPanel createSampleUIControls(boolean addStatus,
			boolean addToolbar, boolean addHelperViews) {
		JPanel samplePanel = new JPanel(new BorderLayout());
		if (addStatus) {
			// Add status in south
			samplePanel.add(editorComponent.getStatusComponent(),
					BorderLayout.SOUTH);
		}
		if (addToolbar) {
			// Add toolbar in north
			samplePanel.add(allActionsToolbar, BorderLayout.NORTH);
		}

		// Reconfigures the toolbar when the document type detection changes.
		editorComponent
				.addAuthorComponentListener(new AuthorComponentListener() {
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
					 * Reconfigures the actions toolbar
					 * 
					 * @see ro.sync.ecss.extensions.api.component.listeners.AuthorComponentListener#loadedDocumentChanged()
					 */
					public void loadedDocumentChanged() {
						invokeOnAWT(new Runnable() {
							public void run() {
								reconfigureActionsToolbar();
							}
						});
					}
				});

		// Reconfigure toolbar when editor page changes (from Author to Text for
		// example).
		editorComponent.getWSEditorAccess().addPageChangedListener(
				new WSEditorPageChangedListener() {
					public void editorPageChanged() {
						invokeOnAWT(new Runnable() {
							public void run() {
								reconfigureActionsToolbar();
								try {
									if (reviewViewShouldBeShowingInTheAuthorPage) {
										setVisibleSideViewInternal(
												"review",
												EditorPageConstants.PAGE_AUTHOR
														.equals(editorComponent
																.getWSEditorAccess()
																.getCurrentPageID()));
									}
								} catch (AuthorComponentException e) {
									e.printStackTrace();
								}
							}
						});
					}
				});

		if (addHelperViews) {
			// Editor and outline panels.
			// Editor and review panels
			editorAndReviewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					true);
			SplitPaneUtil.minimizeUI(editorAndReviewSplit);
			editorAndReviewSplit.setDividerLocation(0.8);
			editorAndReviewSplit.setResizeWeight(1);
			editorAndReviewSplit.setLeftComponent(editorComponent
					.getEditorComponent());
			reviewView = new AuthorComponentAdditionalView(
					"Review",
					editorComponent
							.getAdditionalEditHelper(AuthorComponentProvider.REVIEWS_PANEL_ID));
			editorAndReviewSplit.setRightComponent(reviewView);

			editorAndOutlineSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					true);
			SplitPaneUtil.minimizeUI(editorAndOutlineSplit);
			editorAndOutlineSplit.setDividerLocation(200);
			editorAndOutlineSplit.setResizeWeight(0);
			outlineView = new AuthorComponentAdditionalView(
					"Outline",
					editorComponent
							.getAdditionalEditHelper(AuthorComponentProvider.OUTLINER_PANEL_ID));
			editorAndOutlineSplit.setLeftComponent(outlineView);
			editorAndOutlineSplit.setRightComponent(editorAndReviewSplit);

			// TODO uncomment this to add the dita map component sample instead
			// of the outline.
			// DITAMapComponentSample ditaMapSample;
			// try {
			// ditaMapSample = new DITAMapComponentSample(factory);
			// ditaMapSample.setDocument("http://devel-new.sync.ro/~test/AuthorDemoApplet/samples/dita/flowers/flowers.ditamap",
			// null);
			// editorAndOutlineSplit.setLeftComponent(ditaMapSample);
			// } catch (AuthorComponentException e1) {
			// e1.printStackTrace();
			// }

			// Add the Outliner Pop-up Customizer
			if (EditorPageConstants.PAGE_AUTHOR.equals(editorComponent
					.getWSEditorAccess().getCurrentPageID())) {
				((WSAuthorComponentEditorPage) editorComponent
						.getWSEditorAccess().getCurrentPage())
						.setOutlinerPopUpCustomizer(new PopupMenuCustomizer() {
							public void customize(JPopupMenu menu) {
								menu.addSeparator();
								menu.add(new AbstractAction(
										"Selected Elements Info") {
									public void actionPerformed(ActionEvent e) {
										TreePath[] selectedPaths = ((WSAuthorComponentEditorPage) editorComponent
												.getWSEditorAccess()
												.getCurrentPage())
												.getAuthorAccess()
												.getOutlineAccess()
												.getSelectedPaths(true);
										if (selectedPaths != null
												&& selectedPaths.length > 0) {
											StringBuffer info = new StringBuffer();
											for (int i = 0; i < selectedPaths.length; i++) {
												info.append("Node <"
														+ ((AuthorNode) selectedPaths[i]
																.getLastPathComponent())
																.getName()
														+ ">\n");
											}
											JOptionPane.showMessageDialog(
													AuthorComponentSample.this,
													info.toString());
										}
									}
								});
							}
						});
			}

			// Attributes and elements.
			attributesAndElementsSplit = new JSplitPane(
					JSplitPane.VERTICAL_SPLIT, true);
			SplitPaneUtil.minimizeUI(attributesAndElementsSplit);

			attributesAndElementsSplit.setDividerLocation(0.5);
			attributesAndElementsSplit.setResizeWeight(0.5);
			attributesView = new AuthorComponentAdditionalView(
					"Attributes",
					editorComponent
							.getAdditionalEditHelper(AuthorComponentProvider.ATTRIBUTES_PANEL_ID));
			attributesAndElementsSplit.setTopComponent(attributesView);
			elementsView = new AuthorComponentAdditionalView(
					"Elements",
					editorComponent
							.getAdditionalEditHelper(AuthorComponentProvider.ELEMENTS_PANEL_ID));
			attributesAndElementsSplit.setBottomComponent(elementsView);

			// Validation problems model list
			final DefaultListModel<DocumentPositionedInfo> dlm = new DefaultListModel<>();
			// Validation problems list
			final JList<DocumentPositionedInfo> problemsList = new JList<>();
			JScrollPane validationProblemsScrollPanel = new JScrollPane(
					problemsList);
			problemsList.setModel(dlm);

			// Add validation problems filter to retain the validation problems
			// list
			editorComponent.getWSEditorAccess().addValidationProblemsFilter(
					new ValidationProblemsFilter() {
						public void filterValidationProblems(
								ValidationProblems validationProblems) {
							final List<DocumentPositionedInfo> problemsList = validationProblems.getProblemsList();
							//Update list of problems in JList on AWT
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									// Clear the old validation problems list
									dlm.clear();
									if (problemsList != null && problemsList != null) {
										// Update the validation problems list
										for (int i = 0; i < problemsList.size(); i++) {
											dlm.addElement(problemsList.get(i));
										}
									}
								}
							});
						}
					});
			// Set list cell renderer
			problemsList.setCellRenderer(new DefaultListCellRenderer() {
				public Component getListCellRendererComponent(JList<?> list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					JLabel label = (JLabel) super.getListCellRendererComponent(
							list, value, index, isSelected, cellHasFocus);
					label.setText(((DocumentPositionedInfo) value)
							.getMessageWithSeverity());
					return label;
				}
			});
			// When double clicking on a validation problem, the corresponding
			// document
			// content will be selected
			problemsList.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						DocumentPositionedInfo dpi = dlm
								.get(problemsList.getSelectedIndex());
						if (dpi != null) {
							try {
								WSEditorPage currentPage = editorComponent.getWSEditorAccess().getCurrentPage();
								int[] startEndOffsets = ((WSTextBasedEditorPage) currentPage)
										.getStartEndOffsets(dpi);
								if (startEndOffsets != null) {
									if(currentPage instanceof WSTextBasedEditorPage) {
										((WSTextBasedEditorPage) currentPage).select(
												startEndOffsets[0],
												startEndOffsets[1]);
									}
								}
							} catch (BadLocationException e1) {
								logger.error(e1, e1);
							}
						}
					}
				}
			});
			validationProblemsView = new AuthorComponentAdditionalView(
					"Validation Problems", validationProblemsScrollPanel);

			// Center panel containing components and additional views
			centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
			SplitPaneUtil.minimizeUI(centerPanel);

			centerPanel.setDividerLocation(0.8);
			centerPanel.setResizeWeight(1);
			centerPanel.setLeftComponent(editorAndOutlineSplit);
			centerPanel.setRightComponent(attributesAndElementsSplit);
			samplePanel.add(centerPanel, BorderLayout.CENTER);
			samplePanel.add(validationProblemsView, BorderLayout.SOUTH);

		} else {
			// No additional views, add component in CENTER
			samplePanel.add(editorComponent.getEditorComponent(),
					BorderLayout.CENTER);
		}

		// Add the editor Pop-up Customizer
		if (EditorPageConstants.PAGE_AUTHOR.equals(editorComponent
				.getWSEditorAccess().getCurrentPageID())) {
			((WSAuthorComponentEditorPage) editorComponent.getWSEditorAccess()
					.getCurrentPage())
					.addPopUpMenuCustomizer(new AuthorPopupMenuCustomizer() {
						public void customizePopUpMenu(Object menu,
								AuthorAccess authorAccess) {
							((JPopupMenu) menu).addSeparator();
							((JPopupMenu) menu).add(new AbstractAction(
									"Toggle Auto Spell Check") {
								public void actionPerformed(ActionEvent e) {
									SpellCheckOptions spellCheckOptions = factory
											.getSpellCheckOptions();
									spellCheckOptions.automaticSpellCheck = !spellCheckOptions.automaticSpellCheck;
									factory.setSpellCheckOptions(spellCheckOptions);
								}
							});
						}
					});
		}
		return samplePanel;
	}


}
