package simple.documentation.framework.callouts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlightActionsProvider;
import ro.sync.exml.workspace.api.editor.page.author.actions.AuthorActionsProvider;

/**
 * Provides actions that are shown on the contextual menu of the callout
 */
public class SDFAuthorPersistentHighlightActionsProvider implements
    AuthorPersistentHighlightActionsProvider {
  /**
   * The list with contextual actions for a custom callout.
   */
  private List<AbstractAction> actions = new ArrayList<AbstractAction>();
  /**
   * The default action.
   */
  private AbstractAction defaultAction;
  /**
   * Constructor.
   * 
   * @param authorAccess The author access.
   */
  public SDFAuthorPersistentHighlightActionsProvider(AuthorAccess authorAccess) {
    AuthorActionsProvider actionsProvider = authorAccess.getEditorAccess().getActionsProvider();
    Map<String, Object> authorCommonActions = actionsProvider.getAuthorCommonActions();
    Set<String> keySet = authorCommonActions.keySet();
    for (String key : keySet) {
      if (key != null) {
        if ("Edit/Add_Comment".equals(key) ||
            "Edit/Edit_Comment".equals(key) ||
            "Edit/Remove_Comments".equals(key)) {
          actions.add((AbstractAction) authorCommonActions.get(key));
          if ("Edit/Edit_Comment".equals(key)) {
            defaultAction = (AbstractAction) authorCommonActions.get(key);
          }
        }
      }
    }
  }
  /**
   * @see ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlightActionsProvider#getDefaultAction(ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight)
   */
  public AbstractAction getDefaultAction(AuthorPersistentHighlight hl) {
    return defaultAction;
  }

  /**
   * @see ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlightActionsProvider#getActions(ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight)
   */
  public List<AbstractAction> getActions(AuthorPersistentHighlight hl) {
    return actions;
  }
}