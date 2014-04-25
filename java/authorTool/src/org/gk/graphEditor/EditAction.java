/*
 * Created on Aug 25, 2003
 */
package org.gk.graphEditor;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import org.gk.render.Editor;

/**
 * For editing name.
 * @author wgm
 */
public class EditAction implements GraphEditorAction {
	private GraphEditorPane graphPane;
	// Selection start for dragging
	private int index0;

	public EditAction(GraphEditorPane editor) {
		this.graphPane = editor;
	}
	
	public void doAction(MouseEvent e) {
		// Handle mouse pressing or dragging only
		if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			Editor editor = graphPane.editor;
            // To avoid round-off error, add 0.5
            int x = (int)(e.getX() / graphPane.getScaleX() + 0.5);
            int y = (int)(e.getY() / graphPane.getScaleY() + 0.5);
			editor.setCaretPosition(x, y);
			int i = editor.getCaretPosition();
			if ((e.getModifiers() & InputEvent.SHIFT_MASK) > 0) {
				// Do selection
				if (i > editor.getSelectionEnd()) {
					editor.setSelectionEnd(i);
				}
				else {
					int end = editor.getSelectionStart();
					editor.setSelectionStart(i);
					editor.setSelectionEnd(end);
				}
			}
			else
				editor.clearSelection();
			index0 = editor.getCaretPosition();
			graphPane.repaint(graphPane.getEditingNode().getBounds());
		}
		else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
			Editor editor = graphPane.editor;
//           To avoid round-off error, add 0.5
            int x = (int)(e.getX() / graphPane.getScaleX() + 0.5);
            int y = (int)(e.getY() / graphPane.getScaleY() + 0.5);
            editor.setCaretPosition(x, y);
			int index1 = editor.getCaretPosition();
			if (index0 < index1) {
				editor.setSelectionStart(index0);
				editor.setSelectionEnd(index1);
			}
			else {
				editor.setSelectionStart(index1);
				editor.setSelectionEnd(index0);
			}
			graphPane.repaint(graphPane.getEditingNode().getBounds());
		}
	}
}
