package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.NodeHoverEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface NodeHoverHandler extends EventHandler {

    void onNodeHover(NodeHoverEvent event);

}
