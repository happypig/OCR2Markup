package simple.documentation.framework.extensions;

import java.util.List;

import javax.swing.text.BadLocationException;

import ro.sync.contentcompletion.xml.ContextElement;
import ro.sync.contentcompletion.xml.WhatElementsCanGoHereContext;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.AuthorSchemaAwareEditingHandlerAdapter;
import ro.sync.ecss.extensions.api.AuthorSchemaManager;
import ro.sync.ecss.extensions.api.InvalidEditException;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;

/**
 * Specific editing support for SDF documents. Handles typing and paste events inside section and tables.
 */
public class SDFSchemaAwareEditingHandler extends AuthorSchemaAwareEditingHandlerAdapter {

  private static final String SDF_NAMESPACE = "http://www.oxygenxml.com/sample/documentation";
  /**
   * SDF table element name.
   */
  private static final String SDF_TABLE = "table";
  /**
   * SDF table row name.
   */
  private static final String SDF_TABLE_ROW = "tr";
  /**
   * SDF table cell name.
   */
  private static final String SDF_TABLE_CELL = "td";
  /**
   * SDF section element name.
   */
  private static final String SECTION = "section";
  /**
   * SDF para element name.
   */
  protected static final String PARA = "para";
  /**
   * SDF title element name.
   */
  protected static final String TITLE = "title";

  /**
   * @see ro.sync.ecss.extensions.api.AuthorSchemaAwareEditingHandler#handleDelete(int, int, ro.sync.ecss.extensions.api.AuthorAccess, boolean)
   */
  @Override
  public boolean handleDelete(int offset, int deleteType, AuthorAccess authorAccess, boolean wordLevel) 
  throws InvalidEditException {
    // Not handled.
    return false;
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorSchemaAwareEditingHandler#handleDeleteElementTags(ro.sync.ecss.extensions.api.node.AuthorNode, ro.sync.ecss.extensions.api.AuthorAccess)
   */
  @Override
  public boolean handleDeleteElementTags(AuthorNode nodeToUnwrap, AuthorAccess authorAccess) 
  throws InvalidEditException {
    // Not handled.
    return false;
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorSchemaAwareEditingHandler#handleDeleteSelection(int, int, int, ro.sync.ecss.extensions.api.AuthorAccess)
   */
  @Override
  public boolean handleDeleteSelection(int selectionStart, int selectionEnd, int generatedByActionId, 
      AuthorAccess authorAccess) throws InvalidEditException {
    // Not handled.
    return false;
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorSchemaAwareEditingHandler#handleJoinElements(ro.sync.ecss.extensions.api.node.AuthorNode, java.util.List, ro.sync.ecss.extensions.api.AuthorAccess)
   */
  @Override
  public boolean handleJoinElements(AuthorNode targetNode, List<AuthorNode> nodesToJoin, AuthorAccess authorAccess) 
  throws InvalidEditException {
    // Not handled.
    return false;
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorSchemaAwareEditingHandler#handlePasteFragment(int, ro.sync.ecss.extensions.api.node.AuthorDocumentFragment[], int, ro.sync.ecss.extensions.api.AuthorAccess)
   */
  @Override
  public boolean handlePasteFragment(int offset, AuthorDocumentFragment[] fragmentsToInsert, int actionId, 
      AuthorAccess authorAccess) throws InvalidEditException {
    boolean handleInsertionEvent = false;
    AuthorSchemaManager authorSchemaManager = authorAccess.getDocumentController().getAuthorSchemaManager();
    if (!authorSchemaManager.isLearnSchema() && 
        !authorSchemaManager.hasLoadingErrors() &&
        authorSchemaManager.getAuthorSchemaAwareOptions().isEnableSmartPaste()) {
      handleInsertionEvent = handleInsertionEvent(offset, fragmentsToInsert, authorAccess);
    }
    return handleInsertionEvent;
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorSchemaAwareEditingHandler#handleTyping(int, char, ro.sync.ecss.extensions.api.AuthorAccess)
   */
  @Override
  public boolean handleTyping(int offset, char ch, AuthorAccess authorAccess)
  throws InvalidEditException {
    boolean handleTyping = false;
    AuthorSchemaManager authorSchemaManager = authorAccess.getDocumentController().getAuthorSchemaManager();
    if (!authorSchemaManager.isLearnSchema() && 
        !authorSchemaManager.hasLoadingErrors() &&
        authorSchemaManager.getAuthorSchemaAwareOptions().isEnableSmartTyping()) {
      try {
        AuthorDocumentFragment characterFragment = 
          authorAccess.getDocumentController().createNewDocumentTextFragment(String.valueOf(ch));    
        handleTyping = handleInsertionEvent(offset, new AuthorDocumentFragment[] {characterFragment}, authorAccess);
      } catch (AuthorOperationException e) {
        throw new InvalidEditException(e.getMessage(), "Invalid typing event: " + e.getMessage(), e, false);
      }
    }
    return handleTyping;    
  }

  /**
   * Handle an insertion event (either typing or paste).
   * 
   * @param offset Offset where the insertion event occurred.
   * @param fragmentsToInsert Fragments that must be inserted at the given offset. 
   * @param authorAccess Author access.
   * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
   * 
   * @throws InvalidEditException The event was rejected because it is invalid.
   */
  private boolean handleInsertionEvent(
      int offset, 
      AuthorDocumentFragment[] fragmentsToInsert, 
      AuthorAccess authorAccess) throws InvalidEditException {
    AuthorSchemaManager authorSchemaManager = authorAccess.getDocumentController().getAuthorSchemaManager();
    boolean handleEvent = false;
    try {
      AuthorNode nodeAtInsertionOffset = authorAccess.getDocumentController().getNodeAtOffset(offset);
      if (isElementWithNameAndNamespace(nodeAtInsertionOffset, SDF_TABLE)) {
        // Check if the fragment is allowed as it is.
        boolean canInsertFragments = authorSchemaManager.canInsertDocumentFragments(
            fragmentsToInsert, 
            offset, 
            AuthorSchemaManager.VALIDATION_MODE_STRICT_FIRST_CHILD_LAX_OTHERS);
        if (!canInsertFragments) {
          handleEvent = handleInvalidInsertionEventInTable(
              offset, 
              fragmentsToInsert, 
              authorAccess,
              authorSchemaManager);
        }
      } else if(isElementWithNameAndNamespace(nodeAtInsertionOffset, SECTION)) {
        // Check if the fragment is allowed as it is.
        boolean canInsertFragments = authorSchemaManager.canInsertDocumentFragments(
            fragmentsToInsert, 
            offset, 
            AuthorSchemaManager.VALIDATION_MODE_STRICT_FIRST_CHILD_LAX_OTHERS);
        if (!canInsertFragments) {
          // Insertion in 'section' element
          handleEvent = handleInvalidInsertionEventInSect(
              offset, 
              fragmentsToInsert, 
              authorAccess,
              authorSchemaManager);
        }
      } 
    } catch (BadLocationException e) {
      throw new InvalidEditException(e.getMessage(), "Invalid typing event: " + e.getMessage(), e, false);
    } catch (AuthorOperationException e) {
      throw new InvalidEditException(e.getMessage(), "Invalid typing event: " + e.getMessage(), e, false);
    }
    return handleEvent;    
  }

  /**
   * @return <code>true</code> if the given node is an element with the given local name and from the SDF namespace.
   */
  protected boolean isElementWithNameAndNamespace(AuthorNode node, String elementLocalName) {
    boolean result = false;
    if(node.getType() == AuthorNode.NODE_TYPE_ELEMENT) {
      AuthorElement element = (AuthorElement) node;
      result = elementLocalName.equals(element.getLocalName()) && element.getNamespace().equals(SDF_NAMESPACE);
    }
    return result;
  }

  /**
   * Try to handle invalid insertion events in a SDF 'table'. 
   * A row element will be inserted with a new cell in which the fragments will be inserted.
   * 
   * @param offset Offset where the insertion event occurred.
   * @param fragmentsToInsert Fragments that must be inserted at the given offset. 
   * @param authorAccess Author access.
   * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
   */
  private boolean handleInvalidInsertionEventInTable(
      int offset,
      AuthorDocumentFragment[] fragmentsToInsert, 
      AuthorAccess authorAccess,
      AuthorSchemaManager authorSchemaManager) throws BadLocationException, AuthorOperationException {
    boolean handleEvent = false;
    // Typing/paste inside a SDF table. We will try to wrap the fragment into a new cell and insert it inside a new row.
    WhatElementsCanGoHereContext context = authorSchemaManager.createWhatElementsCanGoHereContext(offset);
    StringBuilder xmlFragment = new StringBuilder("<");
    xmlFragment.append(SDF_TABLE_ROW);
    if (SDF_NAMESPACE != null && SDF_NAMESPACE.length() != 0) {
      xmlFragment.append(" xmlns=\"").append(SDF_NAMESPACE).append("\"");
    }
    xmlFragment.append("/>");

    // Check if a row can be inserted at the current offset.
    boolean canInsertRow = authorSchemaManager.canInsertDocumentFragments(
        new AuthorDocumentFragment[] {authorAccess.getDocumentController().createNewDocumentFragmentInContext(xmlFragment.toString(), offset)},
        context,
        AuthorSchemaManager.VALIDATION_MODE_STRICT_FIRST_CHILD_LAX_OTHERS);

    // Derive the context by adding a new row element with a cell.
    if (canInsertRow) {
      pushContextElement(context, SDF_TABLE_ROW);
      pushContextElement(context, SDF_TABLE_CELL);

      // Test if fragments can be inserted in the new context.
      if (authorSchemaManager.canInsertDocumentFragments(
          fragmentsToInsert, 
          context, 
          AuthorSchemaManager.VALIDATION_MODE_STRICT_FIRST_CHILD_LAX_OTHERS)) {

        // Insert a new row with a cell.
        xmlFragment = new StringBuilder("<");
        xmlFragment.append(SDF_TABLE_ROW);

        if (SDF_NAMESPACE != null && SDF_NAMESPACE.length() != 0) {
          xmlFragment.append(" xmlns=\"").append(SDF_NAMESPACE).append("\"");
        }
        xmlFragment.append("><");
        xmlFragment.append(SDF_TABLE_CELL);
        xmlFragment.append("/></");
        xmlFragment.append(SDF_TABLE_ROW);
        xmlFragment.append(">");
        authorAccess.getDocumentController().insertXMLFragment(xmlFragment.toString(), offset);

        // Get the newly inserted cell.
        AuthorNode newCell = authorAccess.getDocumentController().getNodeAtOffset(offset + 2);            
        for (int i = 0; i < fragmentsToInsert.length; i++) { 
          authorAccess.getDocumentController().insertFragment(newCell.getEndOffset(), fragmentsToInsert[i]);
        }

        handleEvent = true;
      } 
    }
    return handleEvent;
  }

  /**
   * Derive the given context by adding the specified element.
   */
  protected void pushContextElement(WhatElementsCanGoHereContext context, String elementName) {
    ContextElement contextElement = new ContextElement();
    contextElement.setQName(elementName);
    contextElement.setNamespace(SDF_NAMESPACE);
    context.pushContextElement(contextElement, null);
  }

  /**
   * Try to handle invalid insertion events in 'section'. 
   * The solution is to insert the <code>fragmentsToInsert</code> into a 'title' element if the sect element is empty or
   * into a 'para' element if the sect already contains a 'title'.  
   * 
   * @param offset Offset where the insertion event occurred.
   * @param fragmentsToInsert Fragments that must be inserted at the given offset. 
   * @param authorAccess Author access.
   * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
   */
  private boolean handleInvalidInsertionEventInSect(int offset, AuthorDocumentFragment[] fragmentsToInsert, AuthorAccess authorAccess,
      AuthorSchemaManager authorSchemaManager) throws BadLocationException, AuthorOperationException {
    boolean handleEvent = false;
    // Typing/paste inside an section. 
    AuthorElement sectionElement = (AuthorElement) authorAccess.getDocumentController().getNodeAtOffset(offset);

    if (sectionElement.getStartOffset() + 1 == sectionElement.getEndOffset()) {
      // Empty section element
      WhatElementsCanGoHereContext context = authorSchemaManager.createWhatElementsCanGoHereContext(offset);
      // Derive the context by adding a title.
      pushContextElement(context, TITLE);

      // Test if fragments can be inserted in 'title' element
      if (authorSchemaManager.canInsertDocumentFragments(
          fragmentsToInsert, 
          context, 
          AuthorSchemaManager.VALIDATION_MODE_STRICT_FIRST_CHILD_LAX_OTHERS)) {
        // Create a title structure and insert fragments inside
        StringBuilder xmlFragment = new StringBuilder("<").append(TITLE);
        if (SDF_NAMESPACE != null && SDF_NAMESPACE.length() != 0) {
          xmlFragment.append(" xmlns=\"").append(SDF_NAMESPACE).append("\"");
        }
        xmlFragment.append(">").append("</").append(TITLE).append(">");
        // Insert title
        authorAccess.getDocumentController().insertXMLFragment(xmlFragment.toString(), offset);

        // Insert fragments
        AuthorNode newParaNode = authorAccess.getDocumentController().getNodeAtOffset(offset + 1);            
        for (int i = 0; i < fragmentsToInsert.length; i++) { 
          authorAccess.getDocumentController().insertFragment(newParaNode.getEndOffset(), fragmentsToInsert[i]);
        }
        handleEvent = true;
      } 
    } else {
      // Check if there is just a title.
      List<AuthorNode> contentNodes = sectionElement.getContentNodes();
      if (contentNodes.size() == 1) {
        AuthorNode child = contentNodes.get(0);
        boolean isTitleChild = isElementWithNameAndNamespace(child, TITLE);
        if (isTitleChild && child.getEndOffset() < offset) {
          // We are after the title.

          // Empty sect element
          WhatElementsCanGoHereContext context = authorSchemaManager.createWhatElementsCanGoHereContext(offset);
          // Derive the context by adding a para
          pushContextElement(context, PARA);

          // Test if fragments can be inserted in 'para' element
          if (authorSchemaManager.canInsertDocumentFragments(
              fragmentsToInsert, 
              context, 
              AuthorSchemaManager.VALIDATION_MODE_STRICT_FIRST_CHILD_LAX_OTHERS)) {
            // Create a para structure and insert fragments inside
            StringBuilder xmlFragment = new StringBuilder("<").append(PARA);
            if (SDF_NAMESPACE != null && SDF_NAMESPACE.length() != 0) {
              xmlFragment.append(" xmlns=\"").append(SDF_NAMESPACE).append("\"");
            }
            xmlFragment.append(">").append("</").append(PARA).append(">");
            // Insert para
            authorAccess.getDocumentController().insertXMLFragment(xmlFragment.toString(), offset);

            // Insert fragments
            AuthorNode newParaNode = authorAccess.getDocumentController().getNodeAtOffset(offset + 1);            
            for (int i = 0; i < fragmentsToInsert.length; i++) { 
              authorAccess.getDocumentController().insertFragment(newParaNode.getEndOffset(), fragmentsToInsert[i]);
            }
            handleEvent = true;
          } 
        }
      }
    }
    return handleEvent;
  }
}
