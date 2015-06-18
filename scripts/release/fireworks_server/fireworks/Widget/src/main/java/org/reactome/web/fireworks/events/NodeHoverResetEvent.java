package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.NodeHoverResetHandler;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class NodeHoverResetEvent extends GwtEvent<NodeHoverResetHandler> {
    public static Type<NodeHoverResetHandler> TYPE = new Type<NodeHoverResetHandler>();

    @Override
    public Type<NodeHoverResetHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(NodeHoverResetHandler handler) {
        handler.onNodeHoverReset();
    }

    @Override
    public String toString(){
        return "NodeHoverResetEvent";
    }
}
