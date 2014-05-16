/*
 * Created on Dec 14, 2006
 *
 */
package org.gk.graphEditor;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.gk.render.Renderable;
import org.gk.render.RenderableFactory;

/**
 * A customized TreeCellRenderer that can display icons based on Renderable class types.
 * @author guanming
 *
 */
public class TypeTreeCellRenderer extends DefaultTreeCellRenderer {
    private boolean showIsChanged = false;
	
	public TypeTreeCellRenderer() {
		super();
	}
    
    public void setShowIsChanged(boolean shown) {
        this.showIsChanged = shown;
    }
    
    public boolean showIsChanged() {
        return this.showIsChanged;
    }
	
	public Component getTreeCellRendererComponent(
						JTree tree,
						Object value,
						boolean sel,
						boolean expanded,
						boolean leaf,
						int row,
						boolean hasFocus) {
		super.getTreeCellRendererComponent(
						tree, value, sel,
						expanded, leaf, row,
						hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object obj = node.getUserObject();
		if (obj instanceof Renderable) {
            Renderable r = (Renderable) obj;
			setIcon(RenderableFactory.getFactory().getIcon(r));
            if (showIsChanged && r.isChanged())
                setText(">" + r.getDisplayName());
            else
                setText(r.getDisplayName());
        }
		return this;
	}
}