/*
 * GraphEditorSelectionModel.java
 *
 * Created on June 17, 2003, 10:15 AM
 */

package org.gk.graphEditor;

import java.util.ArrayList;
import java.util.Iterator;

import org.gk.render.Renderable;
/**
 * This class is used to store selected objects for GraphEditorPane.
 * @author  wgm
 */
public class GraphEditorSelectionModel {
    // The list of selected objects
    java.util.List selection = null;
    // For listeners
    private java.util.List listeners;
    private GraphEditorActionEvent selectionEvent;
    
    /** Creates a new instance of GraphEditorSelectionModel */
    public GraphEditorSelectionModel() {
        selection = new ArrayList();
    }
    
    /**
     * Add a single selected object to the model.
     */
    public void addSelected(Object selected) {
    	if (!selection.contains(selected)) {
            Renderable renderable = (Renderable)selected;
            renderable.setIsSelected(true);
            selection.add(selected);
    	}
        fireSelectionChange();
    }
    
    /**
     * Remove an object from this model.
     */
    public void removeSelected(Object selected) {
        selection.remove(selected);
        Renderable renderable = (Renderable) selected;
        renderable.setIsSelected(false);
        fireSelectionChange();
    }
    
    /**
     * Add a list of selected objects to the model.
     */
    public void addSelection(java.util.List selection) {
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Renderable o = (Renderable)it.next();
            if (!this.selection.contains(o)) {
                this.selection.add(o);
                o.setIsSelected(true);
            }
        }
        fireSelectionChange();
    }
    
    /**
     * Remove a list of objects from the model.
     */
    public void removeSelection(java.util.List selection) {
        if (selection == null || selection.size() == 0)
            return;
        Renderable renderable = null;
        for (Iterator it = selection.iterator(); it.hasNext();) {
        	renderable = (Renderable) it.next();
        	renderable.setIsSelected(false);
        }
        this.selection.removeAll(selection);
        fireSelectionChange();
    }
    
    public void removeSelection() {
    	if (selection == null || selection.size() == 0)
    		return;
    	Renderable renderable = null;
    	for (Iterator it = selection.iterator(); it.hasNext();) {
    		renderable = (Renderable) it.next();
    		renderable.setIsSelected(false);
    	}
    	selection.clear();
    	fireSelectionChange();
    }
    
    public java.util.List getSelection() {
        return this.selection;
    }
    
    public void setSelection(java.util.List s) {
        for (Iterator it = this.selection.iterator(); it.hasNext();) {
        	Renderable renderable = (Renderable) it.next();
        	renderable.setIsSelected(false);
        	it.remove();
        }
        if (s != null) {
			for (Iterator it = s.iterator(); it.hasNext();) {
				Renderable renderable = (Renderable)it.next();
				renderable.setIsSelected(true);
				this.selection.add(renderable);
			}
        }
        fireSelectionChange();
    }
    
    // For listeners
    public void addGraphEditorActionListener(GraphEditorActionListener l) {
    	if (listeners == null)
    		listeners = new ArrayList();
    	if (listeners.contains(l)) // Don't add it twice
    	    return;
    	listeners.add(l);
    }
    
    public void removeGraphEditorActionListener(GraphEditorActionListener l) {
    	if (listeners != null)
    		listeners.remove(l);
    }
    
    public void fireSelectionChange() {
    	if (listeners != null) {
    		if (selectionEvent == null) {
    			selectionEvent = new GraphEditorActionEvent(this);
    			selectionEvent.setID(GraphEditorActionEvent.SELECTION);
    		}
    		GraphEditorActionListener l = null;
    		for (Iterator it = listeners.iterator(); it.hasNext();) {
    			l = (GraphEditorActionListener) it.next();
    			l.graphEditorAction(selectionEvent);
    		}
    	}
    }
    
}
