/*
 * Created on Aug 15, 2008
 *
 */
package org.gk.graphEditor;

import java.util.List;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.gk.render.Renderable;

/**
 * This class is used to handle undo/redo related to deletion of objects.
 * @author wgm
 *
 */
public class UndoableDeleteEdit extends UndoableInsertDeleteEdit {
    
    public UndoableDeleteEdit(GraphEditorPane graphPane,
                              List<Renderable> objects) {
        init(graphPane, objects);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        delete();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        insert();
    }
    
}
