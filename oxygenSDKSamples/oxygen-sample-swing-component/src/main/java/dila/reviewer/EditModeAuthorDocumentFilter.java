package dila.reviewer;

import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import ro.sync.ecss.extensions.api.AuthorDocumentController;
import ro.sync.ecss.extensions.api.AuthorDocumentFilter;
import ro.sync.ecss.extensions.api.AuthorDocumentFilterBypass;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.node.AuthorDocumentFragment;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;

/**
 * Document filter that blocks editing of the document according to the configured edit mode.
 * 
 * The editing mode can be one of the:
 * <li> NONE - the document is read-only. </li>
 * <li> TEXT - only the text nodes can be edited but the structure of the document not. </li>
 * <li> STRUCTURE - full editing of the document is allowed. </li>
 * 
 * @see EditMode
 * 
 * @author cristi_talau
 */
final class EditModeAuthorDocumentFilter extends AuthorDocumentFilter {

	/**
	 * The edit mode of the document in the Author page.
	 */
	public static enum EditMode {

		/**
		 * No editing is allowed.
		 */
		NONE {
			public boolean allows(EditMode action) {
				return false;
			}

			public String toString() {
				return "Read only";
			}
		},

		/**
		 * Only text nodes can be edited, but the structure remains unchanged.
		 */
		TEXT {
			public boolean allows(EditMode action) {
				return action == EditMode.TEXT;
			}

			public String toString() {
				return "Text editing";
			}
		},

		/**
		 * Full editing allowed.
		 */
		STRUCTURE {
			public boolean allows(EditMode action) {
				return true;
			}

			public String toString() {
				return "Full editing";
			}
		};

		/**
		 * The editing is more permissive than the <code>desiredEditMode</code>
		 * 
		 * @param desiredEditMode
		 *            the editing that we want to compare with the current one.
		 * @return true if the desired editing mode is the current one.
		 */
		public abstract boolean allows(EditMode desiredEditMode);
	}

	/**
	 * The controller of the document that we filter.
	 */
	private final AuthorDocumentController controller;

	/**
	 * The current editing mode.
	 */
	private EditMode editMode;

	/**
	 * Constructor.
	 * 
	 * @param controller
	 *            the controller of the document that we filter.
	 */
	public EditModeAuthorDocumentFilter(AuthorDocumentController controller) {
		this.controller = controller;
		this.editMode = EditMode.TEXT;
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.delete(
	 *      AuthorDocumentFilterBypass, int, int, boolean)
	 */
	public boolean delete(AuthorDocumentFilterBypass filterBypass, final int startOffset,
			final int endOffset, boolean withBackspace) {
		switch (editMode) {
		case NONE:
			return false;
		case STRUCTURE:
			return filterBypass.delete(startOffset, endOffset, withBackspace);
		case TEXT:
			try {
				// Looks in the content of the document between the given offsets to
				// see whether there is any '\0' markers representing node boundaries.
				//
				// If some markers are present, it means that we are about to
				// delete some structure, so we just don't perform the action.
				boolean containsStructure = false;
				Segment chars = new Segment(new char[1], 0, 1);
				chars.setPartialReturn(true);
				int crt = startOffset;
				int len = endOffset - startOffset + 1;

				// The segment may contain only a part of the given range.
				while (len != 0) {
					controller.getChars(crt, len, chars);
					for (int i = 0; i < chars.count; i++) {
						if (chars.array[chars.offset + i] == 0) {
							containsStructure = true;
							break;
						}
					}
					if (containsStructure) {
						break;
					}
					crt += chars.count;
					len -= chars.count;
				}
				if (!containsStructure) {
					return filterBypass.delete(startOffset, endOffset, withBackspace);
				}
			} catch (BadLocationException e) {
				AuthorComponentReviewerSample.logger.error("Error deleting ", e);
			}
			return false;
		}
		throw new AssertionError();
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.deleteNode(
	 *      AuthorDocumentFilterBypass, AuthorNode)
	 */
	public boolean deleteNode(AuthorDocumentFilterBypass filterBypass, AuthorNode node) {
		if (editMode.allows(EditMode.STRUCTURE)) {
			return filterBypass.deleteNode(node);
		}
		return false;
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.insertFragment(
	 *      AuthorDocumentFilterBypass, int, AuthorDocumentFragment)
	 */
	public void insertFragment(AuthorDocumentFilterBypass filterBypass, int offset,
			AuthorDocumentFragment frag) {
		switch (editMode) {
		case TEXT:
			if (frag.getContentNodes().isEmpty()) {
				filterBypass.insertFragment(offset, frag);
			}
			break;
		case STRUCTURE:
			filterBypass.insertFragment(offset, frag);
			break;
		case NONE:
			break;
		default:
			break;
		}
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.insertNode(
	 *      AuthorDocumentFilterBypass, int, AuthorNode)
	 */
	public boolean insertNode(AuthorDocumentFilterBypass filterBypass, int offset, AuthorNode node) {
		if (editMode.allows(EditMode.STRUCTURE)) {
			return filterBypass.insertNode(offset, node);
		}
		return false;
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.insertText(
	 *      AuthorDocumentFilterBypass, int, String)
	 */
	public void insertText(AuthorDocumentFilterBypass filterBypass, int offset, String toInsert) {
		if (editMode.allows(EditMode.TEXT)) {
			filterBypass.insertText(offset, toInsert);
		}
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.renameElement(
	 *      AuthorDocumentFilterBypass, AuthorElement, String, Object)
	 */
	public void renameElement(AuthorDocumentFilterBypass filterBypass, AuthorElement element,
			String newName, Object infoProvider) {
		if (editMode.allows(EditMode.STRUCTURE)) {
			filterBypass.renameElement(element, newName, infoProvider);
		}
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.split(
	 *      AuthorDocumentFilterBypass, AuthorNode, int)
	 */
	public boolean split(AuthorDocumentFilterBypass filterBypass, AuthorNode toSplit,
			int splitOffset) {
		if (editMode.allows(EditMode.STRUCTURE)) {
			return filterBypass.split(toSplit, splitOffset);
		}
		return false;
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.surroundInFragment(
	 *      AuthorDocumentFilterBypass, AuthorDocumentFragment, int, int)
	 */
	public void surroundInFragment(AuthorDocumentFilterBypass filterBypass,
			AuthorDocumentFragment xmlFragment, int startOffset, int endOffset)
			throws AuthorOperationException {
		if (editMode.allows(EditMode.STRUCTURE)) {
			filterBypass.surroundInFragment(xmlFragment, startOffset, endOffset);
		}
	}

	/**
	 * @see ro.sync.ecss.samples.reviewer.EditModeAuthorDocumentFilter.
	 *      surroundInFragment(AuthorDocumentFilterBypass, String, int, int)
	 */
	public void surroundInFragment(AuthorDocumentFilterBypass filterBypass, String xmlFragment,
			int startOffset, int endOffset) throws AuthorOperationException {
		if (editMode.allows(EditMode.STRUCTURE)) {
			filterBypass.surroundInFragment(xmlFragment, startOffset, endOffset);
		}
	}

	/**
	 * @see ro.sync.ecss.extensions.api.AuthorDocumentFilter.surroundWithNode(
	 *      AuthorDocumentFilterBypass, AuthorNode, int, int, boolean)
	 */
	public void surroundWithNode(AuthorDocumentFilterBypass filterBypass, AuthorNode node,
			int startOffset, int endOffset, boolean leftToRight) {
		if (editMode.allows(EditMode.STRUCTURE)) {
			filterBypass.surroundWithNode(node, startOffset, endOffset, leftToRight);
		}
	}

	/**
	 * Returns the current editing mode.
	 * 
	 * @return the current editing mode.
	 */
	public EditMode getEditMode() {
		return editMode;
	}

	/**
	 * Sets the current editing mode.
	 * 
	 * @param editMode
	 *            the desired editing mode.
	 */
	public void setEditMode(EditMode editMode) {
		this.editMode = editMode;
	}
}