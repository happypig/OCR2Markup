package DILA.chtc;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.content.TextContentIterator;
import ro.sync.ecss.extensions.api.content.TextContext;
import ro.sync.ecss.extensions.api.structure.AuthorPopupMenuCustomizer;
import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.Platform;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

/**
 * Converts a sequence of 4 Hexadecimal digits to a character. 
 */
public class CHTCPluginExtension implements
		WorkspaceAccessPluginExtension {

	/**
	 * Logger for logging.
	 */
	private static final Logger logger = LoggerFactory.getLogger(CHTCPluginExtension.class.getName());
		
	/**
	 * The keystroke for the convert action.
	 */
	private static final KeyStroke ACTION_KEYSTROKE = KeyStroke.getKeyStroke("F8");
	
	/**
	 * The key for the action in the action map.
	 */
	private static final String ACTION_MAP_KEY = "converthtc";
	
	/**
	 * The action name. This appears in the Author contextual menu.
	 */
	private static final String ACTION_NAME = "Convert Hex to Char";
	
	/**
	 * Implementation for the Text mode. 
	 */
	@SuppressWarnings("serial")
	private final static class ConvertHTCTextAction extends AbstractAction {

		/**
		 * The Text mode editor.
		 */
		private JTextComponent textComponent;

		/**
		 * Constructor.
		 * 
		 * @param name The action name.
		 * @param textComponent The component used for editing in the Text mode.
		 */
		public ConvertHTCTextAction(String name, JTextComponent textComponent) {
			this.textComponent = textComponent;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int caretPosition = textComponent.getCaretPosition();

			if (caretPosition > NUMBER_OF_HEXA_DIGITS_TO_CONVERT) {
				try {
					String str = textComponent.getText(caretPosition
							- NUMBER_OF_HEXA_DIGITS_TO_CONVERT,
							NUMBER_OF_HEXA_DIGITS_TO_CONVERT);

					try{
						int value = Integer.parseInt(str, 16);
						
						int st = caretPosition - NUMBER_OF_HEXA_DIGITS_TO_CONVERT;
						// Now replace the text with the character
						// equivalent.	
						textComponent.getDocument().remove(
								st,
								NUMBER_OF_HEXA_DIGITS_TO_CONVERT);
						textComponent.getDocument().insertString(st,
								"" + (char) value, null);

					} catch (NumberFormatException ex) {
						complainAboutNumber(textComponent, str);
					}
				} catch (BadLocationException ex) {
					logger.error(ex, ex);
				}
			}
		}
	}

	/**
	 * The conversion action for the Author page.
	 */
	@SuppressWarnings("serial")
	private final static class ConvertHTCAuthorAction extends AbstractAction {
		
		/**
		 * Access to the author page models.
		 */
		private final AuthorAccess authorAccess;

		/**
		 * Constructor.
		 * 
		 * @param name The name of the action.
		 * @param authorAccess Access to the Author page models.
		 */
		private ConvertHTCAuthorAction(String name, AuthorAccess authorAccess) {
			super(name);
			this.authorAccess = authorAccess;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			int caretOffset = authorAccess.getEditorAccess().getCaretOffset();

			
			// Check some ranges.
			if (caretOffset > NUMBER_OF_HEXA_DIGITS_TO_CONVERT) {
				
				AuthorDocumentController controller = authorAccess.getDocumentController();

				// Now examine a section from the content, just before the caret.
				TextContentIterator textContentIterator = controller.getTextContentIterator(caretOffset - NUMBER_OF_HEXA_DIGITS_TO_CONVERT, caretOffset - 1);
				if (textContentIterator.hasNext()) {
					TextContext tc = textContentIterator.next();
					
					// There should be a single piece of text, of the required length. 
					// Otherwise, it means there is some markup and no continuous text - nothing to do in this case.
					CharSequence text = tc.getText();
					if (text.length() == NUMBER_OF_HEXA_DIGITS_TO_CONVERT) {
						String str = text.toString();
						try {
							int value = Integer.parseInt(str, 16);
					
							// Now replace the text with the character
							// equivalant.
							controller.beginCompoundEdit();
							try {
								controller.delete(
										tc.getTextStartOffset(),
										tc.getTextEndOffset() - 1);
								controller.insertText(
										tc.getTextStartOffset(), ""
												+ (char) value);
							} finally {
								controller.endCompoundEdit();
							}
					    } catch (NumberFormatException ex) {
					    	complainAboutNumber(((JFrame)authorAccess.getWorkspaceAccess().getParentFrame()), str);
						}
					} 
				}
			}
		}		
	}

	private static final int NUMBER_OF_HEXA_DIGITS_TO_CONVERT = 4;

	/**
	 * Complain that the number cannot be converted.
	 * 
	 * @param nrStr The string that cannot be converted.
	 */
	private static void complainAboutNumber(Component comp, String nrStr) {
		JOptionPane.showMessageDialog(
				comp, 
				"Cannot parse as hexadecimal integer the text fragment: '" + nrStr + "'", 
				"Error", 
				JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
	 */
	@Override
	public void applicationStarted(
			final StandalonePluginWorkspace pluginWorkspaceAccess) {

		// Prevent 'UnsupportedOprationException'
		if (!pluginWorkspaceAccess.getPlatform().equals(Platform.WEBAPP)) {

			pluginWorkspaceAccess.addEditorChangeListener(
					new WSEditorChangeListener() {
						@Override
						public void editorOpened(URL editorLocation) {
							customizePopupMenu();
						}

						/**
						 * The current author page which has been customizer
						 */
						private WSAuthorEditorPage currentAuthorPageAccess;
						/**
						 * The pop-up menu customizer which has been set on it
						 */
						private AuthorPopupMenuCustomizer authorPopupMenuCustomizer;

						// Customize popup menu
						private void customizePopupMenu() {
							if (currentAuthorPageAccess != null
									&& authorPopupMenuCustomizer != null) {
								// Remove the old popup menu customizer in order
								// to avoid adding two customizers on the same
								// page from the same plugin.
								currentAuthorPageAccess
										.removePopUpMenuCustomizer(authorPopupMenuCustomizer);
							}
							WSEditor editorAccess = pluginWorkspaceAccess
									.getCurrentEditorAccess(StandalonePluginWorkspace.MAIN_EDITING_AREA);
							
							if (editorAccess != null) {
								String currentPageID = editorAccess.getCurrentPageID();
								
								///////////////////////////////////
								//
								// Customize menu for Author page
								// and add the action in the input map.
								//
								if (EditorPageConstants.PAGE_AUTHOR.equals(currentPageID)) {
									currentAuthorPageAccess = (WSAuthorEditorPage) editorAccess.getCurrentPage();
									final AbstractAction convertAction = new ConvertHTCAuthorAction(
											ACTION_NAME, 
											currentAuthorPageAccess.getAuthorAccess());
									convertAction.putValue(AbstractAction.ACCELERATOR_KEY, ACTION_KEYSTROKE);

									authorPopupMenuCustomizer = new AuthorPopupMenuCustomizer() {
										// Customize popup menu
										@Override
										public void customizePopUpMenu(
												Object popUp,
												final AuthorAccess authorAccess) {

											// Add the CMS menu
											((JPopupMenu) popUp).add(convertAction);
										}
									};
									currentAuthorPageAccess.addPopUpMenuCustomizer(authorPopupMenuCustomizer);
									
									// Register the action in the action map.
									JComponent comp = ((JComponent)currentAuthorPageAccess.getAuthorComponent());
									comp.getActionMap().put(ACTION_MAP_KEY, convertAction);
									comp.getInputMap().put(ACTION_KEYSTROKE, ACTION_MAP_KEY);		
									
								} else if (EditorPageConstants.PAGE_TEXT.equals(currentPageID)) {
									
									///////////////////////////////////
									//
									// Add the action in the input map of the text page.
									//
									
									WSTextEditorPage currentTextPageAccess = (WSTextEditorPage) editorAccess.getCurrentPage();
									
									JTextComponent textComponent = (JTextComponent)currentTextPageAccess.getTextComponent();
									final AbstractAction convertAction = new ConvertHTCTextAction(
											ACTION_NAME, 
											textComponent);
									
									// Register the action in the action map.
									textComponent.getActionMap().put(ACTION_MAP_KEY, convertAction);
									textComponent.getInputMap().put(ACTION_KEYSTROKE, ACTION_MAP_KEY);						
								}
							}
						}


						@Override
						public void editorPageChanged(URL editorLocation) {
							customizePopupMenu();
						}

						@Override
						public void editorSelected(URL editorLocation) {
							customizePopupMenu();
						}
					}, StandalonePluginWorkspace.MAIN_EDITING_AREA);

		}
	}

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationClosing()
	 */
	@Override
	public boolean applicationClosing() {
		return true;
	}

}