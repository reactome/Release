package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.ProfileChangedHandler;
import org.reactome.web.fireworks.profiles.model.Profile;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ProfileChangedEvent extends GwtEvent<ProfileChangedHandler> {
    public static Type<ProfileChangedHandler> TYPE = new Type<ProfileChangedHandler>();

    private Profile profile;

    public ProfileChangedEvent(Profile profile) {
        this.profile = profile;
    }

    @Override
    public Type<ProfileChangedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ProfileChangedHandler handler) {
        handler.onProfileChanged(this);
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public String toString() {
        return "ProfileChangedEvent{" +
                "profile=" + profile.getName() +
                '}';
    }
}
