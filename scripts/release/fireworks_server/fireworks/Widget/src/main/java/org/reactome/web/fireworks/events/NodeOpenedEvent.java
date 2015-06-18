package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.NodeOpenedHandler;
import org.reactome.web.fireworks.model.Node;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class NodeOpenedEvent extends GwtEvent<NodeOpenedHandler> {

    public static Type<NodeOpenedHandler> TYPE = new Type<NodeOpenedHandler>();

    private Node node;

    public NodeOpenedEvent(Node node) {
        this.node = node;
    }

    @Override
    public Type<NodeOpenedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(NodeOpenedHandler handler) {
        handler.onNodeOpened(this);
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "NodeOpenedEvent{" +
                "node=" + node +
                '}';
    }
}
