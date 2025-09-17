package DILA;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.Reader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.ecss.extensions.api.component.AuthorComponentException;
import ro.sync.ecss.extensions.api.component.AuthorComponentFactory;
import ro.sync.ecss.extensions.api.component.ditamap.DITAMapTreeComponentProvider;
import ro.sync.ecss.extensions.api.component.listeners.DITAMapTreeComponentListener;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

/**
 * Sample making use of a DITA Map tree component.
 */
@SuppressWarnings("serial")
public class DITAMapComponentSample extends JPanel {

	/**
	 * Logger.
	 */
	private static Logger logger = LoggerFactory.getLogger(DITAMapComponentSample.class.getName());

	/**
	 * The actions toolbar
	 */
	private JToolBar actionsToolbar;

	/**
	 * The author component.
	 */
	private final DITAMapTreeComponentProvider ditaMapComponent;

	/**
	 * Constructor. Builds the sample.
	 * 
	 * @throws AuthorComponentException
	 */
	public DITAMapComponentSample(AuthorComponentFactory factory)
			throws AuthorComponentException {

		// Create the AuthorComponent
		ditaMapComponent = factory.createDITAMapTreeComponentProvider();

		// Layout.
		setLayout(new BorderLayout());

		actionsToolbar = new JToolBar();
		actionsToolbar.setFloatable(false);

		// Add the component in the center.
		add(createSampleUIControls(true, true));
	}

	/**
	 * Check if the text from the editor was modified.
	 * 
	 * @return true if it was modified.
	 */
	public boolean isModified() {
		return ditaMapComponent.isModified();
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
	public void setDocument(final String xmlSystemId, final Reader xmlContent)
			throws AuthorComponentException {
		final Exception[] ex = new Exception[1];
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				// Installs a document modification listener.
				try {
					ditaMapComponent
							.load((xmlSystemId != null && xmlSystemId.trim()
									.length() > 0) ? new URL(xmlSystemId)
									: null, xmlContent);
				} catch (Exception e) {
					ex[0] = e;
				}
				return null;
			}
		});
		if (ex[0] != null) {
			throw new AuthorComponentException(ex[0]);
		}
	};

	/**
	 * Get the content serialized back to XML from the component
	 * 
	 * @return The content serialized back to XML from the component
	 */
	public String getSerializedDocument() {
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				StringBuilder builder = new StringBuilder();
				BufferedReader reader = new BufferedReader(ditaMapComponent
						.createReader());
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
	 * Reconfigure the actions toolbar
	 */
	private void reconfigureActionsToolbar() {
		actionsToolbar.removeAll();
		Map<String, Object> authorCommonActions = 
		    ditaMapComponent.getDITAAccess().getActionsProvider().getActions();
		
		// Cut/Copy/Paste
		actionsToolbar.add(new ToolbarButton((Action) authorCommonActions
				.get("Edit/Edit_Cut"), false));
		actionsToolbar.add(new ToolbarButton((Action) authorCommonActions
				.get("Edit/Edit_Copy"), false));
		actionsToolbar.add(new ToolbarButton((Action) authorCommonActions
				.get("Edit/Edit_Paste"), false));
		actionsToolbar.addSeparator();

		// Undo/Redo
		actionsToolbar.add(new ToolbarButton((Action) authorCommonActions
				.get("Edit/Edit_Undo"), false));
		actionsToolbar.add(new ToolbarButton((Action) authorCommonActions
				.get("Edit/Edit_Redo"), false));
		actionsToolbar.addSeparator();

		// Author extensions
		Iterator<String> iter = authorCommonActions.keySet().iterator();
		while (iter.hasNext()) {
			String actionID = iter.next();
			AbstractAction action = (AbstractAction) authorCommonActions.get(actionID);
			if (action.getValue(Action.SMALL_ICON) != null) {
				actionsToolbar.add(new ToolbarButton(action, false));
			}
		}

		if (actionsToolbar.getParent() != null) {
			actionsToolbar.getParent().invalidate();
			actionsToolbar.revalidate();
		}
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
	private JPanel createSampleUIControls(boolean addStatus, boolean addToolbar) {
		JPanel samplePanel = new JPanel(new BorderLayout());
		if (addStatus) {
			// Add status in south
			samplePanel.add(ditaMapComponent.getStatusComponent(),
					BorderLayout.SOUTH);
		}
		if (addToolbar) {
			// Add toolbar in north
			samplePanel.add(actionsToolbar, BorderLayout.NORTH);
		}

		samplePanel.add(ditaMapComponent.getEditorComponent(),
				BorderLayout.CENTER);

		// Reconfigures the toolbar when the document type detection changes.
		ditaMapComponent
				.addDITAMapTreeComponentListener(new DITAMapTreeComponentListener() {
					public void modifiedStateChanged(boolean arg0) {
					}

					/**
					 * Reconfigures the actions toolbar
					 * 
					 * @see ro.sync.ecss.extensions.api.component.listeners.AuthorComponentListener#documentTypeChanged()
					 */
					public void documentTypeChanged() {
						reconfigureActionsToolbar();
					}

					/**
					 * Reconfigures the actions toolbar
					 * 
					 * @see ro.sync.ecss.extensions.api.component.listeners.AuthorComponentListener#loadedDocumentChanged()
					 */
					public void loadedDocumentChanged() {
						reconfigureActionsToolbar();
					}
				});
		return samplePanel;
	}
}
