/*
 * Created on Jun 16, 2004
 */
package org.reactome.go;

import javax.swing.tree.TreeCellRenderer;

import org.gk.database.SingleEventTreeView;

/**
 * This customized JPanel is used to display a single GO Biological Process
 * in the DAG context.
 * @author wugm
 */
public class SingleGOBPTreeView extends SingleEventTreeView {

	public SingleGOBPTreeView() {
	}
	
	protected TreeCellRenderer createTreeCellRenderer() {
		GOInstanceRenderer renderer = new GOInstanceRenderer();
		renderer.setNode2IconMap(node2IconMap);
		return renderer;
	}
	
	public void setNeedHilite(boolean hilite) {
		GOInstanceRenderer renderer = (GOInstanceRenderer) tree.getCellRenderer();
		renderer.setNeedHilite(hilite);
	}
}
