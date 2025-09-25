package dila.eclipse.action;

import java.net.URL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;

import dila.eclipse.SamplePlugin;

/**
 * Action that will use Oxygen API and replace the selected content in current editor
 * with some static content.
 * 
 * @author adrian_sorop
 */
public class ReplaceText extends Action {

  /**
   * The text that will replace the selection when action is executed by user.
   */
  private String replacementText;
  
  /**
   * The component that contains the text to be replaced.
   */
  private StyledText textComponent;
  
  /**
   * Creates the action.
   * 
   * @param textComponent The text component where to replace the text.
   */
  public ReplaceText(StyledText textComponent) {
    this.textComponent = textComponent;
    
    setText("Replace selection");
    // Install icon.
    URL image = SamplePlugin.class.getClassLoader().getResource("/images/ReplaceText16.gif");
    if (image != null) {
      setImageDescriptor(ImageDescriptor.createFromURL(image));
    }
    
    replacementText = "the replacement text";
  }
  
  /**
   * Replace the text selected in current XML editor with the replacement text. 
   */
  @Override
  public void run() {
    Point selection = textComponent.getSelection();
    if (selection.x != selection.y) {
      textComponent.replaceTextRange(selection.x, selection.y - selection.x, replacementText);
    }
  }
  
}
