package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.FireworksLoadedHandler;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksLoadedEvent extends GwtEvent<FireworksLoadedHandler> {
    public static Type<FireworksLoadedHandler> TYPE = new Type<FireworksLoadedHandler>();

    private Long speciesId;

    public FireworksLoadedEvent(Long speciesId) {
        this.speciesId = speciesId;
    }

    @Override
    public Type<FireworksLoadedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FireworksLoadedHandler handler) {
        handler.onFireworksLoaded(this);
    }

    public Long getSpeciesId() {
        return speciesId;
    }

    @Override
    public String toString() {
        return "FireworksLoadedEvent{" +
                "speciesId=" + speciesId +
                '}';
    }
}
