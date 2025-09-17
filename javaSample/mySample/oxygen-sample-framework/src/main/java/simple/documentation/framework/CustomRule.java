package simple.documentation.framework;

import org.xml.sax.Attributes;

import ro.sync.ecss.extensions.api.DocumentTypeCustomRuleMatcher;

/**
 * Matching rule to the SDF document type.
 *
 */
public class CustomRule implements DocumentTypeCustomRuleMatcher {

  /**
   * Check if the SDF document type should be used for the given document properties.
   */
  public boolean matches(
      String systemID,
      String rootNamespace,
      String rootLocalName,
      String doctypePublicID,
      Attributes rootAttributes) {
    boolean matches = true;
    int attributesCount = rootAttributes.getLength();
    for (int i = 0; i < attributesCount; i++) {
      String localName = rootAttributes.getLocalName(i);
      if ("version".equals(localName)) {
        if ("2.0".equals(rootAttributes.getValue(i))) {
          // Do not match the documents with "2.0" version
          matches = false;
        }
      }
    }
    
    return matches;
  }

  /**
   * Description.
   */
  public String getDescription() {
    return "Checks if the current Document Type Association"
    + " is matching the document.";
  }
}
