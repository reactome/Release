package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.NodeOpenedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface NodeOpenedHandler extends EventHandler {

    void onNodeOpened(NodeOpenedEvent event);

}
