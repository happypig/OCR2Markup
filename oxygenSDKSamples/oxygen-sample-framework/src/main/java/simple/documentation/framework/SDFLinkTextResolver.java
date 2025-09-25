package simple.documentation.framework;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.link.InvalidLinkException;
import ro.sync.ecss.extensions.api.link.LinkTextResolver;
import ro.sync.ecss.extensions.api.node.AttrValue;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;

/**
 * Resolve a link and obtains a text representation. The content of the link represent the name and 
 * the absolute location of the referred file.
 */
public class SDFLinkTextResolver extends LinkTextResolver {

  /**
   * The author access.
   */
  private AuthorAccess authorAccess;
  
  /**
   * Logger for logging.
   */
  private static final Logger logger = LoggerFactory.getLogger(SDFLinkTextResolver.class.getName());


  /**
   * @see ro.sync.ecss.extensions.api.link.LinkTextResolver#resolveReference(ro.sync.ecss.extensions.api.node.AuthorNode)
   */
  @Override
  public String resolveReference(AuthorNode node) throws InvalidLinkException {
    String linkText = null;
    if (node.getType() == AuthorNode.NODE_TYPE_ELEMENT 
        && "link".equals(ro.sync.basic.xml.BasicXmlUtil.getLocalName(node.getName()))) {
      AuthorElement element = (AuthorElement) node;
      AuthorElement[] authorElements = element.getElementsByLocalName("ref");

      if (authorElements != null && authorElements.length > 0) {
        // Get the first 'ref' element from link.
        AuthorElement refElem = authorElements[0];
        AttrValue locationAttribute = refElem.getAttribute("location");
        String locationVal = locationAttribute.getValue();
        URIResolver uriResolver = authorAccess.getXMLUtilAccess().getURIResolver();
        try {
          Source resolve = uriResolver.resolve(locationVal, authorAccess.getEditorAccess().getEditorLocation().toString());
          String systemId = resolve.getSystemId();
          linkText = "[" + locationVal + "] - " + systemId;
        } catch (TransformerException e) {
          logger.warn(e, e);
        }
      }
    }

    return linkText;
  }
  
  
  /**
   * @see ro.sync.ecss.extensions.api.link.LinkTextResolver#activated(ro.sync.ecss.extensions.api.AuthorAccess)
   */
  @Override
  public void activated(AuthorAccess authorAccess) {
    this.authorAccess = authorAccess;
  }
}
