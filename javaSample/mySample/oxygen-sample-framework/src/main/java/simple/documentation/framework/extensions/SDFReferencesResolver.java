package simple.documentation.framework.extensions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.sax.SAXSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorReferenceResolver;
import ro.sync.ecss.extensions.api.node.AttrValue;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;

/**
 * References resolver for Simple Documentation Framework.
 *
 */
public class SDFReferencesResolver implements AuthorReferenceResolver {
  /**
   * Logger for logging.
   */
  private static Logger logger = LoggerFactory.getLogger(SDFReferencesResolver.class.getName());

  /**
   * Verifies if the handler considers the node to have references.
   *
   * @param node The node to be analyzed.
   * @return <code>true</code> if it is has references.
   */
  public boolean hasReferences(AuthorNode node) {
    boolean hasReferences = false;
    if (node.getType() == AuthorNode.NODE_TYPE_ELEMENT) {
      AuthorElement element = (AuthorElement) node;
      if ("ref".equals(element.getLocalName())) {
        AttrValue attrValue = element.getAttribute("location");
        hasReferences = attrValue != null;
      }
    }
    return hasReferences;
  }

  /**
   * Returns the name of the node that contains the expanded referred content.
   *
   * @param node The node that contains references.
   * @return The display name of the node.
   */
  public String getDisplayName(AuthorNode node) {
    String displayName = "ref-fragment";
    if (node.getType() == AuthorNode.NODE_TYPE_ELEMENT) {
      AuthorElement element = (AuthorElement) node;
      if ("ref".equals(element.getLocalName())) {
        AttrValue attrValue = element.getAttribute("location");
        if (attrValue != null) {
          displayName = attrValue.getValue();
        }
      }
    }
    return displayName;
  }

  /**
   * Resolve the references of the node.
   *
   * The returning SAXSource will be used for creating the referred content
   * using the parser and source inside it.
   *
   * @param node The clone of the node.
   * @param systemID The system ID of the node with references.
   * @param authorAccess The author access implementation.
   * @param entityResolver The entity resolver that can be used to resolve:
   *
   * <ul>
   * <li>Resources that are already opened in editor.
   * For this case the InputSource will contains the editor content.</li>
   * <li>Resources resolved through XML catalog.</li>
   * </ul>
   *
   * @return The SAX source including the parser and the parser's input source.
   */
  public SAXSource resolveReference(
      AuthorNode node,
      String systemID,
      AuthorAccess authorAccess,
      EntityResolver entityResolver) {
    SAXSource saxSource = null;
    if (node.getType() == AuthorNode.NODE_TYPE_ELEMENT) {
      AuthorElement element = (AuthorElement) node;
      if ("ref".equals(element.getLocalName())) {
        AttrValue attrValue = element.getAttribute("location");
        if (attrValue != null){
          String attrStringVal = attrValue.getValue();
          if (attrStringVal.length() > 0) {
            try {
              URL absoluteUrl = new URL(new URL(systemID),
                  authorAccess.getUtilAccess().correctURL(attrStringVal));
              InputSource inputSource = entityResolver.resolveEntity(null,
                  absoluteUrl.toString());
              if(inputSource == null) {
                inputSource = new InputSource(absoluteUrl.toString());
              }
              XMLReader xmlReader = authorAccess.getXMLUtilAccess().newNonValidatingXMLReader();
              xmlReader.setEntityResolver(entityResolver);
              saxSource = new SAXSource(xmlReader, inputSource);
            } catch (MalformedURLException e) {
              logger.error(e, e);
            } catch (SAXException e) {
              logger.error(e, e);
            } catch (IOException e) {
              logger.error(e, e);
            }
          }
        }
      }
    }
    return saxSource;
  }

  /**
   * Get an unique identifier for the node reference.
   *
   * The unique identifier is used to avoid resolving the references
   * recursively.
   *
   * @param node The node that has reference.
   * @return An unique identifier for the reference node.
   */
  public String getReferenceUniqueID(AuthorNode node) {
    String id = null;
    if (node.getType() == AuthorNode.NODE_TYPE_ELEMENT) {
      AuthorElement element = (AuthorElement) node;
      if ("ref".equals(element.getLocalName())) {
        AttrValue attrValue = element.getAttribute("location");
        if (attrValue != null) {
          id = attrValue.getValue();
        }
      }
    }
    return id;
  }

  /**
   * Return the systemID of the referred content.
   *
   * @param node The reference node.
   * @param authorAccess The author access.
   *
   * @return The systemID of the referred content.
   */
  public String getReferenceSystemID(AuthorNode node,
      AuthorAccess authorAccess) {
    String systemID = null;
    if (node.getType() == AuthorNode.NODE_TYPE_ELEMENT) {
      AuthorElement element = (AuthorElement) node;
      if ("ref".equals(element.getLocalName())) {
        AttrValue attrValue = element.getAttribute("location");
        if (attrValue != null) {
          String attrStringVal = attrValue.getValue();
          try {
            URL absoluteUrl = new URL(node.getXMLBaseURL(),
                authorAccess.getUtilAccess().correctURL(attrStringVal));
            systemID = absoluteUrl.toString();
          } catch (MalformedURLException e) {
            logger.error(e, e);
          }
        }
      }
    }
    return systemID;
  }

  /**
   * Verifies if the references of the given node must be refreshed
   * when the attribute with the specified name has changed.
   *
   * @param node The node with the references.
   * @param attributeName The name of the changed attribute.
   * @return <code>true</code> if the references must be refreshed.
   */
  public boolean isReferenceChanged(AuthorNode node, String attributeName) {
    return "location".equals(attributeName);
  }

  /**
   * @return The description of the author extension.
   */
  public String getDescription() {
    return "Resolves the 'ref' references";
  }
}
