package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.NodeHoverHandler;
import org.reactome.web.fireworks.model.Node;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class NodeHoverEvent extends GwtEvent<NodeHoverHandler> {
    public static Type<NodeHoverHandler> TYPE = new Type<NodeHoverHandler>();

    private Node node;

    public NodeHoverEvent(Node node) {
        this.node = node;
    }

    @Override
    public Type<NodeHoverHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(NodeHoverHandler handler) {
        handler.onNodeHover(this);
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "NodeHoverEvent{" +
                "node=" + node +
                '}';
    }
}
