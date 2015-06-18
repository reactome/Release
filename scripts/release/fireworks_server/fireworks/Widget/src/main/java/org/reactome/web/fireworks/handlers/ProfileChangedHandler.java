package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.ProfileChangedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface ProfileChangedHandler extends EventHandler {

    void onProfileChanged(ProfileChangedEvent event);

}
