package dila;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;


/**
 * FlowLayout implementation that allows the toolbar items to wrap when the application
 * is resized. 
 */
@SuppressWarnings("serial")
public class WrapToolbarLayout extends FlowLayout {
	/**
	 * Constructor.
	 * 
	 * @param align The alignment value.
	 */
	public WrapToolbarLayout(int align) {
		super(align);
	}

	
	/**
	 * @see java.awt.FlowLayout#minimumLayoutSize(java.awt.Container)
	 */
	public Dimension minimumLayoutSize(Container target) {
		return computeMinSize(target);
	}

	/**
	 * @see java.awt.FlowLayout#preferredLayoutSize(java.awt.Container)
	 */
	public Dimension preferredLayoutSize(Container target) {
		return computeSize(target);
	}

	/**
	 * Computes the size of the given container, size which will be used as preferred size.
	 * 
	 * @param target	The target container.
	 * 
	 * @return A size which will be used as preferred size.
	 */
	private Dimension computeSize(Container target) {
		synchronized (target.getTreeLock()) {
			int hgap = getHgap();
			int vgap = getVgap();
			int w = target.getWidth();

			// Container does not have a size, so use a FlowLayout on one row
			if (w == 0) {
				w = Integer.MAX_VALUE;
			}

			Insets insets = target.getInsets();
			if (insets == null) {
				insets = new Insets(0, 0, 0, 0);
			}
			
			int reqdWidth = 0;
			int maxwidth = w - (insets.left + insets.right + hgap * 2);
			// Obtain the number of components
			int n = target.getComponentCount();
			int x = 0;
			// Add vgap and top inset
			int y = insets.top + vgap;
			int rowHeight = 0;
			// Iterate through the components
			for (int i = 0; i < n; i++) {
				Component c = target.getComponent(i);
				// Component is visible, it will contribute to the layout
				if (c.isVisible()) {
					Dimension d = c.getPreferredSize();
					if ((x == 0) || ((x + d.width) <= maxwidth)) {
						// Can be added in the first row
						if (x > 0) {
							x += hgap;
						}
						x += d.width;
						// Compute also row height
						rowHeight = Math.max(rowHeight, d.height);
					} else {
						// New row
						x = d.width;
						y += vgap + rowHeight;
						rowHeight = d.height;
					}
					
					// Update the width
					reqdWidth = Math.max(reqdWidth, x);
				}
			}
			// Add the last row height
			y += rowHeight;
			// Add bottom insets
			y += insets.bottom;
			int finalWidth = reqdWidth + insets.left + insets.right;
			
			return new Dimension(finalWidth, y);
		}
	}

	/**
	 * Computes the minimum size of the given container.
	 * 	
	 * @param target The container.
	 * 
	 * @return The minimum dimension that the given container can have.
	 */
	private Dimension computeMinSize(Container target) {
		synchronized (target.getTreeLock()) {
			int minx = Integer.MAX_VALUE;
			int miny = Integer.MIN_VALUE;
			boolean found_one = false;
			
			// Number of components
			int n = target.getComponentCount();

			// Iterate through the components and determin the minimum dimension
			for (int i = 0; i < n; i++) {
				Component c = target.getComponent(i);
				// Current component is visible, so use it to layout
				if (c.isVisible()) {
					found_one = true;
					Dimension d = c.getPreferredSize();
					// Minimum width
					minx = Math.min(minx, d.width);
					// Minimum height
					miny = Math.min(miny, d.height);
				}
			}
			
			// The container contains at least one visible component
			if (found_one) {
				return new Dimension(minx, miny);
			}
			
			// No visible component was found
			return new Dimension(0, 0);
		}
	}
}
