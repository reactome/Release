package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.controls.ControlAction;
import org.reactome.web.fireworks.handlers.ControlActionHandler;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ControlActionEvent extends GwtEvent<ControlActionHandler> {
    public static Type<ControlActionHandler> TYPE = new Type<ControlActionHandler>();

    ControlAction action;

    public ControlActionEvent(ControlAction action) {
        this.action = action;
    }

    @Override
    public Type<ControlActionHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ControlActionHandler handler) {
        handler.onControlAction(this);
    }

    public ControlAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "ControlActionEvent{" +
                "action=" + action +
                '}';
    }
}
