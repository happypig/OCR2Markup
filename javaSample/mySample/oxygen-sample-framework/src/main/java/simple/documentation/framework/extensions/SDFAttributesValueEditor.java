package simple.documentation.framework.extensions;

import java.io.File;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.CustomAttributeValueEditor;
import ro.sync.ecss.extensions.api.EditedAttribute;
import simple.documentation.framework.AuthorAccessProvider;

/**
 * Simple Document Framework attributes value editor.
 */
public class SDFAttributesValueEditor extends CustomAttributeValueEditor {
  /**
   * Provides access to Author functions and components.
   */
  private final AuthorAccessProvider authorAccessProvider;

  /**
   * Constructor.
   * 
   * @param authorAccessProvider Provides access to Author functions and components.
   */
  public SDFAttributesValueEditor(AuthorAccessProvider authorAccessProvider) {
    this.authorAccessProvider = authorAccessProvider;
  }

  /**
   * Get the value for the current attribute.
   * 
   * @param attr The Edited attribute information.
   * @param parentComponent The parent component/composite.
   */
  public String getAttributeValue(EditedAttribute attr, Object parentComponent) {
    String refValue = null;
    AuthorAccess authorAccess = authorAccessProvider.getAuthorAccess();
    if (authorAccess != null) {
      File refFile = authorAccess.getWorkspaceAccess().chooseFile(
          // Title
          "Choose reference file", 
          // Extensions
          null, 
          // Filter description
          null,
          // Open for save
          false);
      if (refFile != null) {
        refValue = refFile.getAbsolutePath();
      }
    }
    return refValue;
  }

  /**
   * Description of the attribute value editor.
   */
  @Override
  public String getDescription() {
    return "Reference attribute value editor";
  }

  /**
   * Filters the attributes that it handles.
   */
  @Override
  public boolean shouldHandleAttribute(EditedAttribute attribute) {
    return attribute.getAttributeQName().endsWith("ref");
  }
}