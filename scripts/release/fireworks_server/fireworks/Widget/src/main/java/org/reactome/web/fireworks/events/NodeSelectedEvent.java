package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.NodeSelectedHandler;
import org.reactome.web.fireworks.model.Node;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class NodeSelectedEvent extends GwtEvent<NodeSelectedHandler> {
    public static Type<NodeSelectedHandler> TYPE = new Type<NodeSelectedHandler>();

    private Node node;

    public NodeSelectedEvent(Node node) {
        this.node = node;
    }

    @Override
    public Type<NodeSelectedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(NodeSelectedHandler handler) {
        handler.onNodeSelected(this);
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "NodeSelectedEvent{" +
                "node=" + node +
                '}';
    }
}
