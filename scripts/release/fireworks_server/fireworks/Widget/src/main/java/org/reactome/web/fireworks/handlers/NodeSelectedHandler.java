package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.NodeSelectedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface NodeSelectedHandler extends EventHandler {

    void onNodeSelected(NodeSelectedEvent event);

}
