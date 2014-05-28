/*
 * Created on Jun 16, 2004
 */
package org.reactome.go;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.gk.database.EventCellRenderer;
import org.gk.model.GKInstance;
import org.gk.util.GKApplicationUtilities;

/**
 * 
 * @author wugm
 */
public class AppletEventCellRenderer extends EventCellRenderer {
	private boolean needHilite;
	
	public AppletEventCellRenderer() {
	}
	
	public void setNeedHilite(boolean needHilite) {
		this.needHilite = needHilite;
	}
	
	protected void initIcons() {
		if (AppletHelper.getHelper() == null) {
			pathwayIcon = GKApplicationUtilities.createImageIcon(getClass(), "Pathway.gif");
			reactionIcon = GKApplicationUtilities.createImageIcon(getClass(), "Reaction.gif");
			genericIcon = GKApplicationUtilities.createImageIcon(getClass(), "Generic.gif");
			concreteIcon = GKApplicationUtilities.createImageIcon(getClass(), "Concrete.gif");
		}
		else {
			AppletHelper helper = AppletHelper.getHelper();
			pathwayIcon = helper.getIcon("Pathway.gif");
			reactionIcon = helper.getIcon("Reaction.gif");
			genericIcon = helper.getIcon("Generic.gif");
			concreteIcon = helper.getIcon("Concrete.gif");
		}
	}
	
	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean sel,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
			GKInstance event = (GKInstance)treeNode.getUserObject();
			if (event != null)
				textLabel.setText(event.getDisplayName() + " [" + event.getDBID() + "]");
			if (needHilite && event != null) {
				try {
					GKInstance goInstance = (GKInstance)event.getAttributeValue("goBiologicalProcess");
					if (goInstance != null)
						textLabel.setForeground(Color.blue);
				}
				catch (Exception e) {
				}
			}
			// Have to call this method to make paint correctly.
			return this;
	}
}
