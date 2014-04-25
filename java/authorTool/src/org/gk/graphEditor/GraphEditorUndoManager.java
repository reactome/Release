/*
 * Created on Aug 13, 2008
 *
 */
package org.gk.graphEditor;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * This customized UndoManager is extended from UndoManager by adding one feature:
 * fire UndoableEditEvent when a new UndoableEdit is edited.
 * @author wgm
 *
 */
public class GraphEditorUndoManager extends UndoManager {
    private List<UndoableEditListener> editListeners;
    
    public GraphEditorUndoManager() {
        super();
        editListeners = new ArrayList<UndoableEditListener>();
    }
    
    public void addUndoableEditListener(UndoableEditListener l) {
        editListeners.add(l);
    }
    
    public void removeUndoableEditListener(UndoableEditListener l) {
        editListeners.remove(l);
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit anEdit) {
        boolean rtn = super.addEdit(anEdit);
        if (rtn)
            fireUndoableEditEvent(anEdit);
        return rtn;
    }

    @Override
    public synchronized void redo() throws CannotRedoException {
        UndoableEdit edit = editToBeRedone();
        super.redo();
        fireUndoableEditEvent(edit);
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        UndoableEdit edit = editToBeUndone();
        super.undo();
        fireUndoableEditEvent(edit);
    }
    
    @Override
    public synchronized void die() {
        super.die();
        fireUndoableEditEvent(null);
    }

    protected void fireUndoableEditEvent(UndoableEdit edit) {
        if (editListeners.size() == 0)
            return;
        UndoableEditEvent event = new UndoableEditEvent(this, edit);
        for (UndoableEditListener l : editListeners)
            l.undoableEditHappened(event);
    }
    
}
