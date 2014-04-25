/*
 * Created on Aug 14, 2008
 *
 */
package org.gk.graphEditor;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.gk.render.Renderable;

/**
 * This class is used to handle an undoable insert action.
 * @author wgm
 *
 */
public class UndoableInsertEdit extends UndoableInsertDeleteEdit {
    
    public UndoableInsertEdit(GraphEditorPane graphPane,
                              Renderable r) {
        List<Renderable> objects = new ArrayList<Renderable>();
        objects.add(r);
        init(graphPane, objects);
    }
    
    public UndoableInsertEdit(GraphEditorPane graphPane,
                              List<Renderable> objects) {
        init(graphPane, objects);
    }
    
    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        insert();
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        delete();
    }
    
}
