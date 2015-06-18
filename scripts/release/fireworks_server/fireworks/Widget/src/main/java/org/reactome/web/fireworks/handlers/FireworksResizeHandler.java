package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.FireworksResizedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface FireworksResizeHandler extends EventHandler {

    void onFireworksResized(FireworksResizedEvent event);

}
