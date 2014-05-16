/*
 * Created on Jun 16, 2004
 */
package org.reactome.go;

import java.awt.Color;
import java.awt.Component;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.gk.model.GKInstance;

/**
 * A customized TreeCellRenderer for displaying GO_BiologicalProcess 
 * GKInstance objects.
 * @author wugm
 */

class GOInstanceRenderer extends DefaultTreeCellRenderer {
	private Map node2IconMap;
	private boolean needHilite;
	
	public GOInstanceRenderer() {
		super();
	}
	
	public void setNode2IconMap(Map map) {
		this.node2IconMap = map;
	}
	
	public void setNeedHilite(boolean hilite) {
		this.needHilite = hilite;
	}
		
	public Component getTreeCellRendererComponent(JTree tree,
										 Object value,
										 boolean sel,
										 boolean expanded,
										 boolean leaf,
										 int row,
										 boolean hasFocus) {
		Component comp = super.getTreeCellRendererComponent(tree, 
										   value, 
										   sel, 
										   expanded, 
										   leaf, 
										   row, 
										   hasFocus);
		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
		Object userObject = treeNode.getUserObject();
		if (userObject instanceof GKInstance) {
			GKInstance instance = (GKInstance) userObject;
			// Get GO:ID
			try {
			    String accession = (String) instance.getAttributeValue("accession");
			    if (accession != null)
			        setText(instance.getDisplayName() + " [GO:" + accession + "]");
			}
			catch(Exception e) {
				setText(instance.getDisplayName());
				e.printStackTrace();
			}
			setIcon((ImageIcon)node2IconMap.get(treeNode));
			if (needHilite) {
				try {
					GKInstance event = (GKInstance) instance.getAttributeValueNoCheck("event");
					if (event != null)
						setForeground(Color.blue);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		else if (userObject != null) {
			setText(userObject.toString());
			setIcon(null);
		}
		return comp;
	}
}
