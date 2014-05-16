/*
 * Created on Mar 15, 2004
 */
package org.gk.pathView;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.undo.AbstractUndoableEdit;

/**
 * An undoable move edit.
 * @author wugm
 */
public class UndoableMoveEdit extends AbstractUndoableEdit {
	private Map oldPositions;	
	private Map newPositions;
	private JComponent comp;
	// to control undo redo
	private boolean canUndo;

	public UndoableMoveEdit(JComponent comp) {
		oldPositions = new HashMap();
		newPositions = new HashMap();
		this.comp = comp;
	}
	
	public void storeOldPositions(Collection edges) {
		storePositions(edges, oldPositions);
	}
	
	private void storePositions(Collection edges, Map map) {
		IEdge edge = null;
		map.clear();
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			if (edge.getType() == IEdge.REACTION_EDGE) {
				Point tailP = new Point(edge.getTailX(), edge.getTailY());
				Point headP = new Point(edge.getHeadX(), edge.getHeadY());
				map.put(edge, new Point[]{tailP, headP});
			}
		}		
	}
	
	public void storeNewPositions(Collection edges) {
		storePositions(edges, newPositions);
		canUndo = true; // Mark a new info.
	}
	
	public void undo() {
		super.undo();
		revertPositions(oldPositions);
		canUndo = false;
	}
	
	private void revertPositions(Map posMap) {
		IEdge edge = null;
		for (Iterator it = posMap.keySet().iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			Point[] points = (Point[]) posMap.get(edge);
			edge.setTail(points[0].x, points[0].y);
			edge.setHead(points[1].x, points[1].y);
		}
		if (comp != null)
			comp.repaint(comp.getVisibleRect());		
	}
	
	public void redo() {
		super.redo();
		revertPositions(newPositions);
		canUndo = true;
	}
	
	public boolean canUndo() {
		return canUndo;
	}
	
	public boolean canRedo() {
		return !canUndo;
	}

}
