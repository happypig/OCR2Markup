package DILA.eclipse;

import javax.swing.text.BadLocationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.part.ViewPart;

import ro.sync.ecss.extensions.api.AuthorCaretEvent;
import ro.sync.ecss.extensions.api.AuthorCaretListener;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.listeners.WSEditorPageChangedListener;

/**
 * A sample view which shows information from the Author or Text pages.
 */
public class SampleView extends ViewPart {

	/**
	 * Some text to present
	 */
	private Text someText;

	/**
	 * A sample Eclipse helper view showing some text from the editor while the
	 * caret moves.
	 */
	public SampleView() {
		// Register some listeners on the active part.
		Workbench.getInstance().getActiveWorkbenchWindow().getPartService()
				.addPartListener(new IPartListener() {
					public void partOpened(IWorkbenchPart iworkbenchpart) {						
					}

					public void partDeactivated(IWorkbenchPart iworkbenchpart) {
					}

					public void partClosed(IWorkbenchPart iworkbenchpart) {
					}

					public void partBroughtToTop(IWorkbenchPart iworkbenchpart) {
					}

					public void partActivated(IWorkbenchPart iworkbenchpart) {
						if (iworkbenchpart instanceof WSEditor) {
							final WSEditor ed = (WSEditor) iworkbenchpart;
							editorPageChanged(ed);
							ed.addPageChangedListener(new WSEditorPageChangedListener() {
										@Override
										public void editorPageChanged() {
											SampleView.this.editorPageChanged(ed);
										}
									});
						}
					}
				});
		// Look at the current opened editor
		IWorkbenchPart activePart = Workbench.getInstance()
				.getActiveWorkbenchWindow().getPartService().getActivePart();
		if (activePart instanceof WSEditor) {
			final WSEditor ed = (WSEditor) activePart;
			editorPageChanged(ed);
			ed.addPageChangedListener(new WSEditorPageChangedListener() {
				@Override
				public void editorPageChanged() {
					SampleView.this.editorPageChanged(ed);
				}
			});
		}
	}

	/**
	 * An editor was selected or an editor page was changed.
	 * @param ed The editor
	 */
	private void editorPageChanged(WSEditor ed) {
		//Remove the last registered listener
		if(lastListenedPage != null) {
			if (lastListenedPage instanceof WSAuthorEditorPage) {
				WSAuthorEditorPage aep = (WSAuthorEditorPage) lastListenedPage;
				aep.removeAuthorCaretListener(authorCaretListener);
			} else if (lastListenedPage instanceof WSTextEditorPage) {
				WSTextEditorPage tep = (WSTextEditorPage) lastListenedPage;
				final StyledText st = (StyledText) tep.getTextComponent();
				st.removeKeyListener(textKeyNavigationListener);
				st.removeMouseListener(textMouseNavigationListener);
			}
		}
		//Add a new listener
		WSEditorPage cp = ed.getCurrentPage();
		if (cp instanceof WSAuthorEditorPage) {
			WSAuthorEditorPage aep = (WSAuthorEditorPage) cp;
			aep.addAuthorCaretListener(authorCaretListener);
			lastListenedPage = cp;
		} else if (cp instanceof WSTextEditorPage) {
			WSTextEditorPage tep = (WSTextEditorPage) cp;
			final StyledText st = (StyledText) tep.getTextComponent();
			st.addKeyListener(textKeyNavigationListener);
			st.addMouseListener(textMouseNavigationListener);
			lastListenedPage = cp;
		}
	}
	
	/**
	 * The last listened page.
	 */
	private WSEditorPage lastListenedPage = null;
	
	/**
	 * Author caret listener.
	 */
	private AuthorCaretListener authorCaretListener = new AuthorCaretListener() {
		public void caretMoved(AuthorCaretEvent authorcaretevent) {
			try {
				someText.setText(authorcaretevent.getNode()
						.getTextContent());
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * Text key navigation listener.
	 */
	private KeyAdapter textKeyNavigationListener = new KeyAdapter() {
		public void keyPressed(KeyEvent arg0) {
			StyledText st = (StyledText) arg0.getSource();
			someText.setText(st.getText(st.getCaretOffset() - 20, st
					.getCaretOffset() + 20));
		}
	};

	

	/**
	 * Gets some text around the caret position and place it in the view.
	 */	
	private MouseListener textMouseNavigationListener = new MouseListener() {
		
		@Override
		public void mouseUp(MouseEvent arg0) {
		}
		
		@Override
		public void mouseDown(MouseEvent arg0) {
			StyledText st = (StyledText) arg0.getSource();
			someText.setText(st.getText(st.getCaretOffset() - 20, st
						.getCaretOffset() + 20));	
		}
		
		@Override
		public void mouseDoubleClick(MouseEvent arg0) {
		}
	};
	
	/**
	 * Create the text area
	 */
	public void createPartControl(Composite arg0) {
		someText = new Text(arg0, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
	}

	/**
	 * Request focus to the text area.
	 */
	public void setFocus() {
		someText.setFocus();
	}
}
