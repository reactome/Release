package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.NodeSelectedResetHandler;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class NodeSelectedResetEvent extends GwtEvent<NodeSelectedResetHandler> {
    public static Type<NodeSelectedResetHandler> TYPE = new Type<NodeSelectedResetHandler>();

    @Override
    public Type<NodeSelectedResetHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(NodeSelectedResetHandler handler) {
        handler.onNodeSelectionReset();
    }

    @Override
    public String toString(){
        return "NodeSelectedResetEvent";
    }
}
