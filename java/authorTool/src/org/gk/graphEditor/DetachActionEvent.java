/*
 * Created on Jun 30, 2004
 */
package org.gk.graphEditor;

import org.gk.render.Renderable;

/**
 * This event is used to catch a detach event from dragging a reaction.
 * @author wugm
 */
public class DetachActionEvent extends GraphEditorActionEvent {
    private int role;
    private Renderable detached;
	
	public DetachActionEvent(Object source) {
		super(source);
		setID(REACTION_DETACH);
	}
	
	/**
	 * One of INPUT, OUTPUT, HELPER, ACITVATOR, INHIBITOR in HyperEdge.
	 * @param role
	 */
	public void setRole(int role) {
	    this.role = role;
	}
	
	public int getRole() {
	    return this.role;
	}
	
	/**
	 * Set the Renderable that is detached from this Detach event.
	 * @param r
	 */
	public void setDetached(Renderable r) {
	    this.detached = r;
	}
	
	public Renderable getDetached() {
	    return this.detached;
	}
}
