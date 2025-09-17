package simple.documentation.framework.operations.highlight;

import javax.swing.JFrame;

import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperation;
import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.commons.ui.OKCancelDialog;

/**
 * Operation that allow changing the author.
 */
public class ChangeReviewAuthorOperation implements AuthorOperation {

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#doOperation(ro.sync.ecss.extensions.api.AuthorAccess, ro.sync.ecss.extensions.api.ArgumentsMap)
   */
  public void doOperation(AuthorAccess authorAccess, ArgumentsMap args)
      throws IllegalArgumentException, AuthorOperationException {
    ChangeReviewAuthorDialog commentDlg = new ChangeReviewAuthorDialog(
        (JFrame) authorAccess.getWorkspaceAccess().getParentFrame(), 
        "Change Review Author", 
        new String[] {"Author_1", "Author_2", "Author_3", "Default"});
    // Show the dialog
    commentDlg.showDialog();
    if (commentDlg.getResult() == OKCancelDialog.RESULT_OK) {
      String reviewerAuthorName = commentDlg.getSelectedAuthorName();
      // If the the reviewer author name is set to null, the default author name is used. 
      reviewerAuthorName = "Default".equals(reviewerAuthorName) ? null : reviewerAuthorName;
      // Set the reviewer author name
      authorAccess.getReviewController().setReviewerAuthorName(reviewerAuthorName);
    }
  }

  /**
   * @see ro.sync.ecss.extensions.api.AuthorOperation#getArguments()
   */
  public ArgumentDescriptor[] getArguments() {
    return null;
  }

  /**
   * @see ro.sync.ecss.extensions.api.Extension#getDescription()
   */
  public String getDescription() {
    return "Change review author name";
  }

}
