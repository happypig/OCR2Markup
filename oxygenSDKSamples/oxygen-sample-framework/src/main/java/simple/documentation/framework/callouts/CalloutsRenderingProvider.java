package simple.documentation.framework.callouts;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.text.BadLocationException;

import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.callouts.AuthorCalloutRenderingInformation;
import ro.sync.ecss.extensions.api.callouts.CalloutsRenderingInformationProvider;
import ro.sync.ecss.extensions.api.highlights.AuthorPersistentHighlight;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.exml.view.graphics.Color;


/**
 * Callouts rendering provider.
 * 
 * @author marius
 */
public class CalloutsRenderingProvider extends CalloutsRenderingInformationProvider {
  /**
   * The author access.
   */
  private final AuthorAccess authorAccess;

  /**
   * Constructor.
   * 
   * @param authorAccess The author access.
   */
  public CalloutsRenderingProvider(AuthorAccess authorAccess) {
    this.authorAccess = authorAccess;
  }

  @Override
  public boolean shouldRenderAsCallout(AuthorPersistentHighlight persistentHighlight) {
    return true;
  }

  @Override
  public AuthorCalloutRenderingInformation getCalloutRenderingInformation(
      final AuthorPersistentHighlight hl) {
    return new AuthorCalloutRenderingInformation() {

      @Override
      public long getTimestamp() {
        return -1;
      }

      @Override
      public String getContentFromTarget(int arg0) {
        try {
          AuthorDocumentController documentController = authorAccess.getDocumentController();
          AuthorNode nodeAtOffset = documentController.getNodeAtOffset(hl.getEndOffset());
          int startOffset = hl.getStartOffset() - nodeAtOffset.getStartOffset();
          int endOffset = hl.getEndOffset() - nodeAtOffset.getEndOffset();
          String textContent = nodeAtOffset.getTextContent();
          return textContent.substring(startOffset, endOffset);
        } catch (BadLocationException e) {
          e.printStackTrace();
          return null;
        }
      }

      @Override
      public String getComment(int arg0) {
        return hl.getClonedProperties().get(AuthorPersistentHighlight.COMMENT_ATTRIBUTE);
      }

      @Override
      public Color getColor() {
        return Color.COLOR_DARK_YELLOW;
      }

      @Override
      public String getCalloutType() {
        return "Paragraph review";
      }

      @Override
      public String getAuthor() {
        return "para_reviewer";
      }

      @Override
      public Map<String, String> getAdditionalData() {
        LinkedHashMap<String,String> clonedProperties = hl.getClonedProperties();
        clonedProperties.put("Level", "Superior");
        return clonedProperties;
      }
    };
  }
}
