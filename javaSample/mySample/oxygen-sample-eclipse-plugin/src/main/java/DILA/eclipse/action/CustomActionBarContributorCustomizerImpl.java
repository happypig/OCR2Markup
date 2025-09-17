package DILA.eclipse.action;

import java.net.URL;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.CoolBar;

import DILA.eclipse.SamplePlugin;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.PluginWorkspaceProvider;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;

/**
 * Customize the contextual menu of Oxygen XML Editor using the Oxygen API.
 * 
 * @author adrian_sorop
 */
public class CustomActionBarContributorCustomizerImpl extends com.oxygenxml.editor.editors.ActionBarContributorCustomizer{
  
  /**
   * @see com.oxygenxml.editor.editors.ActionBarContributorCustomizer#customizeActionsContributedToDocumentMenu(List)
   */
  @Override
  public List<IAction> customizeActionsContributedToDocumentMenu(List<IAction> actions) {
    return actions;
  }
  
  /**
   * @see com.oxygenxml.editor.editors.ActionBarContributorCustomizer#customizeActionsContributedToDocumentToolbar(java.util.List)
   */
  @Override
  public List<IAction> customizeActionsContributedToDocumentToolbar(List<IAction> actions) {
    return actions;
  }

  /**
   * @see com.oxygenxml.editor.editors.ActionBarContributorCustomizer#customizeAuthorPageInternalCoolbar(CoolBar, WSAuthorEditorPage)
   */
  @Override
  public void customizeAuthorPageInternalCoolbar(CoolBar arg0, WSAuthorEditorPage arg1) {
    // Empty
  }
  
  /**
   * Customize the contextual menu of Oxygen's Text Page.
   *  
   * @see com.oxygenxml.editor.editors.ActionBarContributorCustomizer#customizeTextPopUpMenu(org.eclipse.jface.action.IMenuManager, ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage)
   */
  @Override
  public void customizeTextPopUpMenu(IMenuManager menuManager, WSTextEditorPage wsTextPage) {
    if (wsTextPage != null) {
      Object textComponent = wsTextPage.getTextComponent();
      if (textComponent instanceof StyledText) {
        StyledText styledText = (StyledText) textComponent;
        ReplaceText replaceText = new ReplaceText(styledText);
        menuManager.add(replaceText);
      }
    }
  }
  
  /**
   *  Add actions in contextual menu of Outline view, when Author page is the current page.
   * 
   * @see com.oxygenxml.editor.editors.ActionBarContributorCustomizer#customizeAuthorOutlinePopUpMenu(org.eclipse.jface.action.IMenuManager, ro.sync.ecss.extensions.api.AuthorAccess)
   */
  @Override
  public void customizeAuthorOutlinePopUpMenu(IMenuManager menuManager, AuthorAccess authorAccess) {
    CopyAttributes copyAttributes = new CopyAttributes(authorAccess);
    menuManager.add(copyAttributes);
  }
  
  /**
   * @see com.oxygenxml.editor.editors.ActionBarContributorCustomizer#customizeAuthorPageExtensionToolbar(IToolBarManager, String, AuthorAccess)
   */
  @Override
  public void customizeAuthorPageExtensionToolbar(IToolBarManager toolbarManager, String toolbarID, AuthorAccess authorAccess) {
    Action lockUnlockPage = new Action("Lock/Unlock page", IAction.AS_CHECK_BOX) {
      @Override
      public void run() {
        WSEditor editor = PluginWorkspaceProvider.getPluginWorkspace().getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
        if(editor != null) {
          WSEditorPage currentPage = editor.getCurrentPage();
          boolean editable = currentPage.isEditable();
          if (editable) {
            currentPage.setReadOnly("Locked by action");
          } else {
            currentPage.setEditable(true);
          }
        }
      }
    };
    
    // Install icon.
    URL image = SamplePlugin.class.getClassLoader().getResource("/images/Lock16.gif");
    if (image != null) {
      lockUnlockPage.setImageDescriptor(ImageDescriptor.createFromURL(image));
    }
    
    toolbarManager.add(lockUnlockPage);
  }
  
}
