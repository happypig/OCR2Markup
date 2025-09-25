package simple.documentation.framework.extensions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ro.sync.ecss.css.EditorContent;
import ro.sync.ecss.css.LabelContent;
import ro.sync.ecss.css.StaticContent;
import ro.sync.ecss.css.Styles;
import ro.sync.ecss.extensions.api.StylesFilter;
import ro.sync.ecss.extensions.api.editor.InplaceEditorArgumentKeys;
import ro.sync.ecss.extensions.api.node.AuthorNode;

/**
 * 
 * Style Filter for Simple Documentation Framework.
 *
 */
public class SDFStylesFilter implements StylesFilter {

  /**
   * Filter the style of "para", "b" and "i" elements.
   */
  public Styles filter(Styles styles, AuthorNode authorNode) {
    //Use fixed font size of 12 for "para".
    if(authorNode.getName().equals("para")) {
      ro.sync.exml.view.graphics.Font original = styles.getFont();
      ro.sync.exml.view.graphics.Font modified = new ro.sync.exml.view.graphics.Font(
          original.getName(), original.getStyle(), 12);
      styles.setProperty(Styles.KEY_FONT, modified);
    }

    //Use foreground color red for "b"
    if(authorNode.getName().equals("b")) {
      ro.sync.exml.view.graphics.Color red = ro.sync.exml.view.graphics.Color.COLOR_RED;
      styles.setProperty(Styles.KEY_FOREGROUND_COLOR, red);
    }

    //Use left border width of 5 pixels for "i"
    if(authorNode.getName().equals("i")) {
      styles.setProperty(Styles.KEY_BORDER_LEFT_WIDTH, new Integer(5));
    }

    if(authorNode.getType() == AuthorNode.NODE_TYPE_PSEUDO_ELEMENT 
        && "before".equals(authorNode.getName())) {
      authorNode = authorNode.getParent();
      if ("country".equals(authorNode.getName())) {
        // This is the BEFORE pseudo element of the "country" element.
        // Read the supported countries from the configuration file.
        // This will be a comma separated enumeration: France, Spain, Great Britain
        String countries = readCountriesFromFile();
        Map<String, Object> formControlArgs = new HashMap<String, Object>();
        formControlArgs.put(InplaceEditorArgumentKeys.PROPERTY_EDIT_QUALIFIED, "#text");
        formControlArgs.put(InplaceEditorArgumentKeys.PROPERTY_TYPE, InplaceEditorArgumentKeys.TYPE_COMBOBOX);
        formControlArgs.put(InplaceEditorArgumentKeys.PROPERTY_VALUES, countries);
        formControlArgs.put(InplaceEditorArgumentKeys.PROPERTY_EDITABLE, "false");

        // We also add a label in form of the form control.
        Map<String, Object> labelProps = new HashMap<String, Object>();
        labelProps.put("text", "Country: ");
        labelProps.put("styles", "* {width: 100px; color: gray;}");
        StaticContent[] mixedContent = new StaticContent[] {new LabelContent(labelProps), new EditorContent(formControlArgs)};
        styles.setProperty(Styles.KEY_MIXED_CONTENT, mixedContent);
      }
    }
    
    // The previously added form control is the only way the element can be edited.
    if ("country".equals(authorNode.getName())) {
      styles.setProperty(Styles.KEY_VISIBITY, "-oxy-collapse-text");
    }

    return styles;
  }
  
  /**
   * The filter is invoked every time the styles are needed so we will make sure 
   * we load the countries only once.
   */
  private String countries = null;
  
  /**
   * Read the available countries from the configuration file.
   *
   * @return The supported countries, read from the configuration file.
   *         This will be a comma separated enumeration: France, Spain, Great Britain.
   */
  private String readCountriesFromFile() {
    if (countries == null) {
      StringBuilder countriesBuilder = new StringBuilder();
      // Our countries file is located in the framework folder. To compute 
      // the framework location we will use the fact that the JAR this class is in
      // is located inside the framework folder.
      String classLocation = "/" + getClass().getCanonicalName().replace(".", "/") + ".class";
      URL resource = getClass().getResource(classLocation);

      if (resource != null) {
        // The URL for the class looks like this:
        // jar:file:/D:/projects/eXml/frameworks/sdf/sdf.jar!/simple/documentation/framework/extensions/SDFStylesFilter.class
        String jarURL = resource.toString();
        // This is the URL of the JAR form where the class was loaded.
        jarURL = jarURL.substring(jarURL.indexOf(":") + 1, jarURL.indexOf("!/"));

        try {
          // We know the resources are next to the JAR.
          URL resourceFile = new URL(new URL(jarURL), "resources/countries.txt");
          InputStream openStream = resourceFile.openStream();
          try {
            InputStreamReader inputStreamReader = new InputStreamReader(openStream, "UTF8");
            char[] cbuf = new char[1024];
            int length = -1;
            while ((length = inputStreamReader.read(cbuf)) != -1) {
              countriesBuilder.append(cbuf, 0, length);
            }
          } finally {
            openStream.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      
      countries = countriesBuilder.toString();
    }
    
    return countries;
  }

  /**
   * Description.
   */
  public String getDescription() {
    return "Implementation for the Simple Documentation Framework style filter.";
  }
}

