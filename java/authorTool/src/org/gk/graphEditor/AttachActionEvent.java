/*
 * Created on Jan 15, 2010
 *
 */
package org.gk.graphEditor;

import org.gk.render.ConnectWidget;
import org.gk.render.Renderable;

/**
 * This event is used for attach.
 * @author wgm
 *
 */
public class AttachActionEvent extends GraphEditorActionEvent {
    private int role;
    private Renderable attached;
    private ConnectWidget connectWidget;
    
    public AttachActionEvent(Object source) {
        super(source);
        setID(REACTION_ATTACH);
    }
    
    public void setAttached(Renderable r) {
        this.attached = r;
    }
    
    public Renderable getAttached() {
        return attached;
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
    
    public void setConnectWidget(ConnectWidget widget) {
        this.connectWidget = widget;
    }
    
    public ConnectWidget getConnectWidget() {
        return this.connectWidget;
    }
}
