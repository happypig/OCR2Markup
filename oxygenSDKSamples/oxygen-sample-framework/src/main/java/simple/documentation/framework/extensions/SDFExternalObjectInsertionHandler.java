package simple.documentation.framework.extensions;

import java.net.URL;
import java.util.List;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorExternalObjectInsertionHandler;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.basic.util.URLUtil;

/**
 * Accepts insertion of images and XML files from external sources.
 */
public class SDFExternalObjectInsertionHandler extends AuthorExternalObjectInsertionHandler {
  /**
   * @see ro.sync.ecss.extensions.api.AuthorExternalObjectInsertionHandler#insertURLs(ro.sync.ecss.extensions.api.AuthorAccess, java.util.List, int)
   */
  @Override
  public void insertURLs(AuthorAccess authorAccess, List<URL> urls, int source)
      throws AuthorOperationException {
    for (URL url : urls) {
      String xmlFragment = null;
      if (authorAccess.getUtilAccess().isSupportedImageURL(url)) {
        // Insert an image element
         xmlFragment = "<image href=\"" + url.toString() + "\"/>";
      } else {
        // Insert 
        xmlFragment = "<ref location=\"" + url.toString() + "\"/>";
      }
      if (xmlFragment != null) {
        // Create am image fragment
        AuthorDocumentFragment imageFragment = authorAccess.getDocumentController().createNewDocumentFragmentInContext(
            xmlFragment, authorAccess.getEditorAccess().getCaretOffset());
        // Insert the fragment
        authorAccess.getDocumentController().insertFragment(authorAccess.getEditorAccess().getCaretOffset(), imageFragment);
      }
    }
  }
  
  /**
   * @see ro.sync.ecss.extensions.api.AuthorExternalObjectInsertionHandler#acceptSource(ro.sync.ecss.extensions.api.AuthorAccess, int)
   */
  @Override
  public boolean acceptSource(AuthorAccess authorAccess, int source) {
    if (source == DND_EXTERNAL) {
      // Accept only from external source
      return true;
    }
    // For urls from other sources (Dita Maps Manager, Project) a new tab will be open.
    return false;
  }
  
  /**
   * @see ro.sync.ecss.extensions.api.AuthorExternalObjectInsertionHandler#acceptURLs(ro.sync.ecss.extensions.api.AuthorAccess, java.util.List, int)
   */
  @Override
  public boolean acceptURLs(AuthorAccess authorAccess, List<URL> urls, int source) {
    boolean accept = acceptSource(authorAccess, source);
    if (accept) {
      for (int i = 0; i < urls.size(); i++) {
        // Accept only XML and image files
        if (!authorAccess.getUtilAccess().isSupportedImageURL(urls.get(i)) &&
            !"xml".equalsIgnoreCase(URLUtil.getExtension(urls.get(i).toString()))) {
          accept = false;
          break;
        }
      }
    } 
    return accept;
  }
}
