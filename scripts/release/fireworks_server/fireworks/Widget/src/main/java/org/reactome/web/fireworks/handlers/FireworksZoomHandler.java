package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.FireworksZoomEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface FireworksZoomHandler extends EventHandler {

    void onFireworksZoomChanged(FireworksZoomEvent event);

}
