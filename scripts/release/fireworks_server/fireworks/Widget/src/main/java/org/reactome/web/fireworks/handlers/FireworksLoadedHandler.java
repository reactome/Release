package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.FireworksLoadedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface FireworksLoadedHandler extends EventHandler {

    void onFireworksLoaded(FireworksLoadedEvent event);

}
