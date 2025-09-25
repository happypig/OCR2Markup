package simple.documentation.framework;

import ro.sync.contentcompletion.xml.SchemaManagerFilter;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorExtensionStateListener;
import ro.sync.ecss.extensions.api.AuthorExternalObjectInsertionHandler;
import ro.sync.ecss.extensions.api.AuthorReferenceResolver;
import ro.sync.ecss.extensions.api.AuthorSchemaAwareEditingHandler;
import ro.sync.ecss.extensions.api.AuthorTableCellSpanProvider;
import ro.sync.ecss.extensions.api.AuthorTableColumnWidthProvider;
import ro.sync.ecss.extensions.api.CustomAttributeValueEditor;
import ro.sync.ecss.extensions.api.ExtensionsBundle;
import ro.sync.ecss.extensions.api.StylesFilter;
import ro.sync.ecss.extensions.api.link.ElementLocatorProvider;
import ro.sync.ecss.extensions.api.link.LinkTextResolver;
import ro.sync.ecss.extensions.api.structure.AuthorBreadCrumbCustomizer;
import ro.sync.ecss.extensions.api.structure.AuthorOutlineCustomizer;
import ro.sync.ecss.extensions.commons.DefaultElementLocatorProvider;
import simple.documentation.framework.extensions.SDFAttributesValueEditor;
import simple.documentation.framework.extensions.SDFAuthorBreadCrumbCustomizer;
import simple.documentation.framework.extensions.SDFAuthorExtensionStateListener;
import simple.documentation.framework.extensions.SDFAuthorOutlineCustomizer;
import simple.documentation.framework.extensions.SDFExternalObjectInsertionHandler;
import simple.documentation.framework.extensions.SDFReferencesResolver;
import simple.documentation.framework.extensions.SDFSchemaAwareEditingHandler;
import simple.documentation.framework.extensions.SDFSchemaManagerFilter;
import simple.documentation.framework.extensions.SDFStylesFilter;
import simple.documentation.framework.extensions.TableCellSpanProvider;
import simple.documentation.framework.extensions.TableColumnWidthProvider;

/**
 * Simple Document Framework extension bundle.
 *
 */
public class SDFExtensionsBundle extends ExtensionsBundle {
  /**
   * Simple documentation framework state listener.
   */
  private SDFAuthorExtensionStateListener sdfAuthorExtensionStateListener;

  /**
   * Editor for attributes values.
   */
  @Override
  public CustomAttributeValueEditor createCustomAttributeValueEditor(boolean forEclipsePlugin) {
    return new SDFAttributesValueEditor(new AuthorAccessProvider() {
      
      public AuthorAccess getAuthorAccess() {
        AuthorAccess access = null;
        if (sdfAuthorExtensionStateListener != null) {
          // Get the Author access.
          access = sdfAuthorExtensionStateListener.getAuthorAccess();
        }
        return access;
      }
    });
  }

  /**
   * Simple documentation framework state listener.
   */
  @Override
  public AuthorExtensionStateListener createAuthorExtensionStateListener() {
    sdfAuthorExtensionStateListener = new SDFAuthorExtensionStateListener();
    return sdfAuthorExtensionStateListener;
  }

  /**
   * Filter for content completion proposals from the schema manager.
   */
  @Override
  public SchemaManagerFilter createSchemaManagerFilter() {
    return new SDFSchemaManagerFilter();
  }

  /**
   * Default element locator.
   */
  @Override
  public ElementLocatorProvider createElementLocatorProvider() {
    return new DefaultElementLocatorProvider();
  }

  /**
   * Expand content references.
   */
  @Override
  public AuthorReferenceResolver createAuthorReferenceResolver() {
    return new SDFReferencesResolver();
  }

  /**
   * CSS styles filtering.
   */
  @Override
  public StylesFilter createAuthorStylesFilter() {
    return new SDFStylesFilter();
  }

  /**
   * Provider for table cell span informations.
   */
  @Override
  public AuthorTableCellSpanProvider createAuthorTableCellSpanProvider() {
    return new TableCellSpanProvider();
  }

  /**
   * Table column width provider responsible of handling modifications regarding 
   * table width and column widths.
   */
  @Override
  public AuthorTableColumnWidthProvider createAuthorTableColumnWidthProvider() {
    return new TableColumnWidthProvider();
  }

  /**
   * Editing support for SDF documents responsible of handling typing and paste events inside section and tables.
   */
  @Override
  public AuthorSchemaAwareEditingHandler getAuthorSchemaAwareEditingHandler() {
    return new SDFSchemaAwareEditingHandler();
  }

  /**
   * Author Outline customizer used for custom filtering and nodes rendering in the Outline. 
   */
  @Override
  public AuthorOutlineCustomizer createAuthorOutlineCustomizer() {
    return new SDFAuthorOutlineCustomizer();
  }

  /**
   * Simple Document Framework Author customizer used for custom nodes rendering
   * in the Breadcrumb. 
   */
  @Override
  public AuthorBreadCrumbCustomizer createAuthorBreadCrumbCustomizer() {
    return new SDFAuthorBreadCrumbCustomizer();
  }
  
  /**
   * @see ro.sync.ecss.extensions.api.ExtensionsBundle#createExternalObjectInsertionHandler()
   */
  @Override
  public AuthorExternalObjectInsertionHandler createExternalObjectInsertionHandler() {
    return new SDFExternalObjectInsertionHandler();
  }

  /**
   * The unique identifier of the Document Type.
   * This identifier will be used to store custom SDF options. 
   */
  @Override
  public String getDocumentTypeID() {
    return "Simple.Document.Framework.document.type";
  }

  /**
   * Bundle description.
   */
  public String getDescription() {
    return "A custom extensions bundle used for the Simple Document Framework";
  }
  
  /**
   * @see ro.sync.ecss.extensions.api.ExtensionsBundle#createLinkTextResolver()
   */
  @Override
  public LinkTextResolver createLinkTextResolver() {
    return new SDFLinkTextResolver();
  }
}