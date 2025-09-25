package dila;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ro.sync.exml.view.graphics.Font;

/**
 * Container for an author component additional view.
 */
@SuppressWarnings("serial")
public class AuthorComponentAdditionalView extends JPanel {
	
	/**
	 * Constructor.
	 * 
	 * @param title The view title.
	 * @param additionalViewComp Additional view component.
	 */
	public AuthorComponentAdditionalView(String title, JComponent additionalViewComp) {
		setLayout(new BorderLayout());
		
		//Title of the view
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
		add(titleLabel, BorderLayout.NORTH);
		
		//The view component
		add(additionalViewComp, BorderLayout.CENTER);
		
		setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
		setPreferredSize(new Dimension(200, getPreferredSize().height));
	}
}
