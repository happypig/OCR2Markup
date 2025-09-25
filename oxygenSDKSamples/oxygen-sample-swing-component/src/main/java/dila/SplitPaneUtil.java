package dila;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class SplitPaneUtil {
	/**
	 * Clears the unnecessary divider and split pane borders. In this way the UI is smoother.
	 * 
	 * @param splitPane The split pane.
	 */
	public static void minimizeUI(JSplitPane splitPane) {
		splitPane.setBorder(BorderFactory.createEmptyBorder());
		splitPane.setDividerSize(4);
		
		SplitPaneUI ui = splitPane.getUI();
		if (ui instanceof BasicSplitPaneUI){
			((BasicSplitPaneUI)ui).getDivider().setBorder(null);
		}
	}
}
