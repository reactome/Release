/*
 * Created on Jul 8, 2003
 */
package org.gk.graphEditor;

import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * A specified DragAction for ComplexEditor.
 * @author wgm
 */
public class ComplexEditorDragAction extends DragAction {

	public ComplexEditorDragAction(GraphEditorPane graphPane) {
		super(graphPane);
	}
	
	public void doAction(MouseEvent e) {
		ComplexGraphEditor editor = (ComplexGraphEditor) editorPane;
		if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
			Point nextP = e.getPoint();
			int dx = nextP.x - prevPoint.x;
			int dy = nextP.y - prevPoint.y;
			editor.moveComplex(dx, dy);
			prevPoint = nextP;
			editorPane.repaint(editorPane.getVisibleRect());
		}
		else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			editorPane.currentAction = editorPane.selectAction; // Reset to default action.
		}	
	}
}
