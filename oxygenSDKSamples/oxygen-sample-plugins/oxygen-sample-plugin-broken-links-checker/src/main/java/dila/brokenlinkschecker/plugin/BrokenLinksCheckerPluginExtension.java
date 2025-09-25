package dila.brokenlinkschecker.plugin;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.Platform;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.standalone.MenuBarCustomizer;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ToolbarComponentsCustomizer;
import ro.sync.exml.workspace.api.standalone.ToolbarInfo;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.exml.workspace.api.standalone.ui.Menu;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;

import dila.brokenlinkschecker.impl.URLParameterManager;
import dila.brokenlinkschecker.ui.CheckerPanel;

/**
 * BrokenLinksCheckerPluginExtension - workspace access extension. This plugin
 * provides the possibility to find broken links on user-provided pages or on
 * other pages accessible from the user-provided ones.
 */
public class BrokenLinksCheckerPluginExtension implements
		WorkspaceAccessPluginExtension {

	private static final Logger logger = LoggerFactory.getLogger(BrokenLinksCheckerPluginExtension.class.getName());

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
	 */
	@Override
	public void applicationStarted(
			final StandalonePluginWorkspace pluginWorkspaceAccess) {

		// Prevent 'UnsupportedOprationException'
		if(!pluginWorkspaceAccess.getPlatform().equals(Platform.WEBAPP)) {

			@SuppressWarnings("serial")
			final Action findBrokenLinksAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent actionevent) {
					pluginWorkspaceAccess.showView("BrokenLinksChecker", true);
				}
			};

			pluginWorkspaceAccess.addMenuBarCustomizer(new MenuBarCustomizer() {

				/**
				 * @see ro.sync.exml.workspace.api.standalone.MenuBarCustomizer#customizeMainMenu(javax.swing.JMenuBar)
				 */
				@Override
				public void customizeMainMenu(JMenuBar mainMenuBar) {
					// Broken Links Checker menu
					JMenu menuBrokenLinksChecker = createBrokenLinksCheckerMenu(findBrokenLinksAction);
					// Add the Broken Links Checker menu before the Help menu
					mainMenuBar.add(menuBrokenLinksChecker,
							mainMenuBar.getMenuCount() - 1);

				}
			});

			pluginWorkspaceAccess
			.addToolbarComponentsCustomizer(new ToolbarComponentsCustomizer() {
				/**
				 * @see ro.sync.exml.workspace.api.standalone.ToolbarComponentsCustomizer#customizeToolbar(ro.sync.exml.workspace.api.standalone.ToolbarInfo)
				 */
				@Override
				public void customizeToolbar(ToolbarInfo toolbarInfo) {
					// The toolbar ID is defined in the "plugin.xml"
					if ("BrokenLinksCheckerToolbarID".equals(toolbarInfo
							.getToolbarID())) {
						List<JComponent> comps = new ArrayList<JComponent>();

						// Find broken links button
						ToolbarButton findBrokenLinksButton = new ToolbarButton(
								findBrokenLinksAction, true);
						findBrokenLinksButton.setText("Find broken links");

						// Add in toolbar
						comps.add(findBrokenLinksButton);
						toolbarInfo.setComponents(comps
								.toArray(new JComponent[0]));
					}
				}
			});

			final CheckerPanel brokenLinksCheckerPanel = new CheckerPanel();
			pluginWorkspaceAccess
			.addViewComponentCustomizer(new ViewComponentCustomizer() {
				/**
				 * @see ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer#customizeView(ro.sync.exml.workspace.api.standalone.ViewInfo)
				 */
				@Override
				public void customizeView(final ViewInfo viewInfo) {
					if (
							// The view ID defined in the "plugin.xml"
							"BrokenLinksChecker".equals(viewInfo.getViewID())) {
						viewInfo.setComponent(brokenLinksCheckerPanel);
						viewInfo.setTitle("Broken Links Checker");
					}
				}
			});

			// add hyperlink listener to the console, to be able to open the parent
			// documents of the broken links in Oxygen
			brokenLinksCheckerPanel.getConsole().addHyperlinkListener(
					new HyperlinkListener() {
						public void hyperlinkUpdate(HyperlinkEvent e) {
							if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
								URL url = e.getURL();

								// get the line and the column where the broken
								// links have been found inside their parent
								// document; the URL parameters corresponding them
								// (i.e. line and column) have been manually added
								// before sending them here and will be removed
								// later
								int line = Integer.parseInt(URLParameterManager
										.getParameter(
												url,
												CheckerPanel.PROBLEM_LINE_QUERY_PARAM_NAME));
								int column = Integer.parseInt(URLParameterManager
										.getParameter(
												url,
												CheckerPanel.PROBLEM_COLUMN_QUERY_PARAM_NAME));
								try {
									// remove the manually added query parameters
									URLParameterManager
									.removeParameter(
											url,
											CheckerPanel.PROBLEM_LINE_QUERY_PARAM_NAME);
									URLParameterManager
									.removeParameter(
											url,
											CheckerPanel.PROBLEM_COLUMN_QUERY_PARAM_NAME);

									// open the parent document in Oxygen, in the
									// Text tab
									pluginWorkspaceAccess.open(url,
											EditorPageConstants.PAGE_TEXT);
									WSEditor editorAccess = pluginWorkspaceAccess
											.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);

									// set the carret position to the position given
									// by line and column
									if (editorAccess != null
											&& editorAccess.getCurrentPage() instanceof WSTextEditorPage) {
										int offset = ((WSTextEditorPage) editorAccess
												.getCurrentPage())
												.getOffsetOfLineStart(line);
										offset += column - 1;
										((WSTextEditorPage) editorAccess
												.getCurrentPage())
												.setCaretPosition(offset);
									}
								} catch (MalformedURLException ex) {
									brokenLinksCheckerPanel
									.displayError(CheckerPanel.INTERNAL_ERROR_MESSAGE);
									logger.debug(ex);
								} catch (BadLocationException ex) {
									brokenLinksCheckerPanel
									.displayError(CheckerPanel.INTERNAL_ERROR_MESSAGE);
									logger.debug(ex);
								}
							}
						}
					});

		}
	}

	/**
	 * Create <code>Broken links checker</code> menu that contains the actions
	 * <code>Find broken links</code>
	 * 
	 * @param findBrokenLinksAction
	 *            The <code>find broken links</code> action.
	 * 
	 * @return The <code>Broken links checker</code> menu.
	 */
	private JMenu createBrokenLinksCheckerMenu(
			final Action findBrokenLinksAction) {
		// Broken links checker menu
		Menu menuBrokenLinksChecker = new Menu("Broken links checker", true);

		// Add "Find broken links" action on the menu
		final JMenuItem findBrokenLinksItem = new JMenuItem(
				findBrokenLinksAction);
		findBrokenLinksItem.setText("Find broken links");
		menuBrokenLinksChecker.add(findBrokenLinksItem);

		return menuBrokenLinksChecker;
	}

	@Override
	public boolean applicationClosing() {
		return true;
	}

}