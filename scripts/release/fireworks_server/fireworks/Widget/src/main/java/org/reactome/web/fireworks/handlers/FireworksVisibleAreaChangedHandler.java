package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.FireworksVisibleAreaChangedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface FireworksVisibleAreaChangedHandler extends EventHandler {

    void onFireworksVisibleAreaChanged(FireworksVisibleAreaChangedEvent event);

}
