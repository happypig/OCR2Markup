package simple.documentation.framework.extensions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorCaretListener;
import ro.sync.ecss.extensions.api.AuthorExtensionStateListener;
import ro.sync.ecss.extensions.api.AuthorListener;
import ro.sync.ecss.extensions.api.AuthorMouseListener;
import ro.sync.ecss.extensions.api.OptionChangedEvent;
import ro.sync.ecss.extensions.api.OptionListener;
import ro.sync.ecss.extensions.api.callouts.AuthorCalloutsController;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight.PersistentHighlightType;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlighter;
import ro.sync.ecss.extensions.api.highlights.ColorHighlightPainter;
import ro.sync.ecss.extensions.api.highlights.HighlightPainter;
import ro.sync.ecss.extensions.api.highlights.PersistentHighlightRenderer;
import ro.sync.ecss.extensions.api.structure.AuthorPopupMenuCustomizer;
import ro.sync.exml.view.graphics.Color;
import ro.sync.exml.workspace.api.Platform;
import ro.sync.exml.workspace.api.editor.page.author.actions.AuthorActionsProvider;
import ro.sync.basic.util.URLUtil;
import simple.documentation.framework.callouts.CalloutsRenderingProvider;
import simple.documentation.framework.callouts.SDFAuthorPersistentHighlightActionsProvider;
import simple.documentation.framework.filters.SDFDocumentFilter;
import simple.documentation.framework.listeners.SDFAuthorCaretListener;
import simple.documentation.framework.listeners.SDFAuthorListener;
import simple.documentation.framework.listeners.SDFAuthorMouseListener;
import simple.documentation.framework.operations.highlight.HighlightProperties;

/**
 * Simple Document Framework state listener used to register custom listeners(caret listener, mouse 
 * listener, document listener and option listener) when the framework is activated. 
 *
 */
public class SDFAuthorExtensionStateListener implements AuthorExtensionStateListener {
  /**
   * The key used to store a custom option. 
   */
  private String customKey = "sdf.custom.option.key";

  /**
   * Custom caret listener to be added on activate and removed on deactivate
   */
  private AuthorCaretListener sdfCaretListener;

  /**
   * Custom mouse listener
   */
  private AuthorMouseListener sdfMouseListener;

  /**
   * Option listener to be added in the option storage.
   */
  private OptionListener sdfOptionListener;

  /**
   * Custom author listener 
   */
  private AuthorListener sdfAuthorDocumentListener;

  /**
   * The access to the author functions.
   */
  private AuthorAccess authorAccess;
  
  /**
   * Map between author name and the corresponding highlight/colors association  
   */
  public static Map<String, Map<PersistentHighlightType, HighlightColors>> authorHighlightColors = null;
  
  /**
   * Defines the color for a highlight
   */
  public static class HighlightColors {
    private Color bgColor;
    private Color decorationColor;
    
    /**
     * @param bgColor The background color.
     * @param decorationColor The color used for decoration
     */
    public HighlightColors(Color bgColor, Color decorationColor) {
      super();
      this.bgColor = bgColor;
      this.decorationColor = decorationColor;
    }
    
    /**
     * @return Returns the bgColor.
     */
    public Color getBgColor() {
      return bgColor;
    }
    
    /**
     * @return Returns the decorationColor.
     */
    public Color getDecorationColor() {
      return decorationColor;
    }
  }
  
  static {
    authorHighlightColors = new HashMap<String, Map<PersistentHighlightType, HighlightColors>>();
    
    // Set colors for Author_1
    Map<PersistentHighlightType, HighlightColors> colorsMap = new HashMap<PersistentHighlightType, HighlightColors>();
    // Set colors for Insert Change highlight 
    colorsMap.put(PersistentHighlightType.CHANGE_INSERT, 
        new HighlightColors(new Color(230, 255, 230), new Color(130, 255, 130)));
    // Set colors for Delete Change highlight
    colorsMap.put(PersistentHighlightType.CHANGE_DELETE, 
        new HighlightColors(new Color(255, 255, 230), new Color(255, 255, 130)));
    authorHighlightColors.put("Author_1", colorsMap);
    
    // Set colors for Author_2
    colorsMap = new HashMap<PersistentHighlightType, HighlightColors>();
    // Set colors for Insert Change highlight 
    colorsMap.put(PersistentHighlightType.CHANGE_INSERT, 
        new HighlightColors(new Color(255, 255, 230), new Color(255, 255, 130)));
    // Set colors for Delete Change highlight
    colorsMap.put(PersistentHighlightType.CHANGE_DELETE, 
        new HighlightColors(new Color(240, 240, 240), new Color(64, 64, 64)));
    authorHighlightColors.put("Author_2", colorsMap);
  }

  /**
   * The SDF Author extension is activated.
   */
  public void activated(final AuthorAccess authorAccess) {
    this.authorAccess = authorAccess;
    sdfOptionListener = new OptionListener(customKey) {
      @Override
      public void optionValueChanged(OptionChangedEvent newValue) {
        // The custom option changed. 
      }
    };

    // Add document filter.
    authorAccess.getDocumentController().setDocumentFilter(new SDFDocumentFilter(authorAccess));

    // Add an option listener.
    authorAccess.getOptionsStorage().addOptionListener(sdfOptionListener);

    // Add author document listeners.
    sdfAuthorDocumentListener = new SDFAuthorListener(authorAccess);
    authorAccess.getDocumentController().addAuthorListener(sdfAuthorDocumentListener);

    if (authorAccess.getWorkspaceAccess().getPlatform() != Platform.WEBAPP) {
      // Add mouse listener.
      sdfMouseListener = new SDFAuthorMouseListener(authorAccess);
      authorAccess.getEditorAccess().addAuthorMouseListener(sdfMouseListener);
      
      // Add custom tooltip
      String tooltip = "[SDF] " + URLUtil.getDescription(authorAccess.getEditorAccess().getEditorLocation());
      authorAccess.getEditorAccess().setEditorTabTooltipText(tooltip);
      
      // Add caret listener.
      sdfCaretListener = new SDFAuthorCaretListener(authorAccess);
      //authorAccess.getEditorAccess().addAuthorCaretListener(sdfCaretListener);
    
      // Use the actions provider to switch to "No Tags" mode.
      AuthorActionsProvider actionsProvider = authorAccess.getEditorAccess().getActionsProvider();
      Map<String, Object> authorCommonActions = actionsProvider.getAuthorCommonActions();
      // Switch to no tags Author/No_tags
      actionsProvider.invokeAction(authorCommonActions.get("Author/No_tags"));
    
      // Set highlights tool tip and painter 
      authorAccess.getEditorAccess().getPersistentHighlighter().setHighlightRenderer(new PersistentHighlightRenderer() {
        public String getTooltip(AuthorPersistentHighlight highlight) {
          // Get highlight properties
          Map<String, String> properties = highlight.getClonedProperties();
          String highlightID = properties.get(HighlightProperties.ID);
          String highlightAuthor = properties.get(HighlightProperties.AUTHOR);
          String highlightComment = properties.get(HighlightProperties.COMMENT);
          
          StringBuilder tooltip = new StringBuilder();
          // Add create date
          if (highlightID != null) {
            try {
              tooltip.append("Id: ").append(highlightID).append("\n");
            } catch (NumberFormatException e) {
              // Wrong date
            }
          }
          // Add author
          if (highlightAuthor != null) {
            tooltip.append("Author: ").append(highlightAuthor).append("\n");;
          }
          // Add comment
          if (highlightComment != null) {
            tooltip.append("Comment: ").append(highlightComment);
          }
          return tooltip.toString();
        }
        
        public HighlightPainter getHighlightPainter(AuthorPersistentHighlight highlight) {
          ColorHighlightPainter painter = new ColorHighlightPainter();
          painter.setBgColor(new Color(0, 0, 255, 20));
          return painter;
        }
      });
      
      // Set reviews tool tip and painters
      authorAccess.getReviewController().setReviewRenderer(new PersistentHighlightRenderer() {
        /**
         * @see ro.sync.ecss.extensions.api.highlights.PersistentHighlightRenderer#getTooltip(ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight)
         */
        public String getTooltip(AuthorPersistentHighlight highlight) {
          String tooltip = "Review Tooltip";
          
          PersistentHighlightType type = highlight.getType();
          if (type == PersistentHighlightType.CHANGE_DELETE) {
            // Delete highlight
            tooltip = "Deleted by " + highlight.getClonedProperties().get("author");
          } else if (type == PersistentHighlightType.CHANGE_INSERT) {
            // Insert highlight
            tooltip = "Inserted by " + highlight.getClonedProperties().get("author");
          } else if (type == PersistentHighlightType.COMMENT) {
            // Comment highlight
            // If a null value is returned, the default tooltip text will be used
            tooltip = null;
          }
          return tooltip;
        }
        
        /**
         * @see ro.sync.ecss.extensions.api.highlights.PersistentHighlightRenderer#getHighlightPainter(ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight)
         */
        public HighlightPainter getHighlightPainter(AuthorPersistentHighlight highlight) {
          HighlightPainter reviewPainter = null;
          Map<String, String> properties = highlight.getClonedProperties();
          String authorName = properties.get("author");
          if (authorName != null) {
            // Get the highlight/colors association for this author name
            Map<PersistentHighlightType, HighlightColors> highlightColorsMap = authorHighlightColors.get(authorName);
            if (highlightColorsMap != null) {
              // Get the associated colors for this type of highlight 
              HighlightColors highlightColors = highlightColorsMap.get(highlight.getType());
              if (highlightColors != null) {
                reviewPainter = new ColorHighlightPainter();
                // Background color
                ((ColorHighlightPainter) reviewPainter).setBgColor(highlightColors.getBgColor());
                // Decoration color
                ((ColorHighlightPainter) reviewPainter).setColor(highlightColors.getDecorationColor());
              }
            }
          }
          
          // If a null value is returned the default highlight painter is used.
          return reviewPainter;
        }
      });
  
      // Get the list with all track changes highlights from the document 
      AuthorPersistentHighlight[] changeHighlights = authorAccess.getReviewController().getChangeHighlights();
      if(changeHighlights.length > 0 && !authorAccess.getReviewController().isTrackingChanges()) {
        // If the document has track changes highlights, then set track changes mode to ON
        authorAccess.getReviewController().toggleTrackChanges();
      }
      
      // Add a popup menu customizer. Group Cut, Copy, Paste and Paste as XML actions in Edit menu.
      authorAccess.getEditorAccess().addPopUpMenuCustomizer(new AuthorPopupMenuCustomizer() {
        // Customize popup menu
        @SuppressWarnings("serial")
        public void customizePopUpMenu(Object popUp, final AuthorAccess authorAccess) {
          JPopupMenu popupMenu = (JPopupMenu) popUp;
          Component[] components = popupMenu.getComponents();
          // The new 'Edit' menu
          JMenu editMenu = new JMenu("Edit");
          boolean shouldAddEditMenu = false;
          for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JMenuItem) {
              JMenuItem menuItem = (JMenuItem) components[i];
              if (menuItem.getAction() != null) {
                Object name = menuItem.getAction().getValue(Action.NAME);
                if ("Cut".equals(name) || "Copy".equals(name) || "Paste".equals(name)|| "Paste as XML".equals(name)) {
                  // Remove edit actions
                  popupMenu.remove(menuItem);
                  // Add edit actions to edit menu
                  editMenu.add(menuItem);
                  shouldAddEditMenu = true;
                }
              }
            }
          }
          if (shouldAddEditMenu) {
            popupMenu.add(editMenu, 0);
          }
          
          final URL selectedUrl;
          try {
            final String selectedText = authorAccess.getEditorAccess().getSelectedText();
            if (selectedText != null) {
              selectedUrl = new URL(selectedText);
              // Open selected url in system application
              popupMenu.add(new JMenuItem(new AbstractAction("Open in system application") {
                public void actionPerformed(ActionEvent e) {
                  authorAccess.getWorkspaceAccess().openInExternalApplication(selectedUrl, true);
                }
              }), 0);
            }
          } catch (MalformedURLException e2) {}
        }
      });
      
      // Add a provider for callouts rendering
      CalloutsRenderingProvider calloutsRenderingProvider = new CalloutsRenderingProvider(authorAccess);
      AuthorCalloutsController authorCalloutsController = 
        authorAccess.getReviewController().getAuthorCalloutsController();
      authorCalloutsController.setCalloutsRenderingInformationProvider(calloutsRenderingProvider);
      
      // Show insertions callouts
      authorCalloutsController.setShowInsertionsCallouts(true);
      
      // Add an actions provider for actions that are shown on the contextual menu of the
      // custom callout
      AuthorPersistentHighlighter highlighter = authorAccess.getEditorAccess().getPersistentHighlighter();
      SDFAuthorPersistentHighlightActionsProvider highlightActionsProvider = 
        new SDFAuthorPersistentHighlightActionsProvider(authorAccess);
      highlighter.setHighlightsActionsProvider(highlightActionsProvider);
    }
    
    // Other custom initializations...
  }

  /**
   * The SDF Author extension is deactivated.
   */
  public void deactivated(AuthorAccess authorAccess) {

    // Remove the option listener.
    authorAccess.getOptionsStorage().removeOptionListener(sdfOptionListener);

    // Remove document listeners.
    authorAccess.getDocumentController().removeAuthorListener(sdfAuthorDocumentListener);

    if (authorAccess.getWorkspaceAccess().getPlatform() != Platform.WEBAPP) {
      // Remove mouse listener.
      authorAccess.getEditorAccess().removeAuthorMouseListener(sdfMouseListener);
      
      // Remove caret listener.
      authorAccess.getEditorAccess().removeAuthorCaretListener(sdfCaretListener);
    }

    // Other actions...
  }

  public String getDescription() {
    return "Simple Document Framework state listener";
  }
  
  /**
   * Returns the Author access.
   * 
   * @return The access to Author functions and components.
   */
  public AuthorAccess getAuthorAccess() {
    return authorAccess;
  }
}
