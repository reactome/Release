/*
 * Created on Jul 8, 2003
 */
package org.gk.graphEditor;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * A specifial SelectAction for ComplexEditor.
 * @author wgm
 */
public class ComplexEditorSelectAction extends SelectAction {

	public ComplexEditorSelectAction(GraphEditorPane graphPane) {
		super(graphPane);
	}

	public void doAction(MouseEvent e) {
		ComplexGraphEditor editor = (ComplexGraphEditor)editorPane;
		if (e.getID() == MouseEvent.MOUSE_PRESSED) { // Do a single selection
			pressPoint = e.getPoint();
			Rectangle bounds = editor.getComplexBounds();
			if (bounds.contains(e.getX(), e.getY())) {
				if (!editor.isComplexSelected()) {
					editor.setIsComplexSelected(true);
					editor.repaint(editor.getVisibleRect());
				}
			}
			else if (editor.isComplexSelected()) {
				editor.setIsComplexSelected(false);
				editor.repaint(editor.getVisibleRect());
			}
		}
		else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
			if (editor.isComplexSelected()) {
				editor.currentAction = editorPane.dragAction;
				editor.dragAction.prevPoint = pressPoint;
				editor.dragAction.doAction(e);
			}
		}
	}
}
