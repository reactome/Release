/*
 * Created on Jun 16, 2004
 */
package org.reactome.go;

import javax.swing.tree.TreeCellRenderer;

import org.gk.database.SingleEventTreeView;

/**
 * 
 * @author wugm
 */
public class AppletSingleEventView extends SingleEventTreeView {
	
	public AppletSingleEventView() {
	}

	protected TreeCellRenderer createTreeCellRenderer() {
		AppletEventCellRenderer renderer = new AppletEventCellRenderer();
		renderer.setNode2IconMap(node2IconMap);
		return renderer;
	}
	
	public void setHiliteGOEvents(boolean hilite) {
		AppletEventCellRenderer renderer = (AppletEventCellRenderer) tree.getCellRenderer();
		renderer.setNeedHilite(hilite);
	}
}
