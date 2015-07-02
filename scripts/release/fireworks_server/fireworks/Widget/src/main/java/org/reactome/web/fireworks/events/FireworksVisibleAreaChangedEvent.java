package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.FireworksVisibleAreaChangedHandler;
import org.reactome.web.fireworks.util.Coordinate;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksVisibleAreaChangedEvent extends GwtEvent<FireworksVisibleAreaChangedHandler> {
    public static Type<FireworksVisibleAreaChangedHandler> TYPE = new Type<FireworksVisibleAreaChangedHandler>();

    private Coordinate translation;
    private double width;
    private double height;

    public FireworksVisibleAreaChangedEvent(Coordinate translation, double width, double height) {
        this.translation = translation;
        this.width = width;
        this.height = height;
    }

    @Override
    public Type<FireworksVisibleAreaChangedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FireworksVisibleAreaChangedHandler handler) {
        handler.onFireworksVisibleAreaChanged(this);
    }

    public Coordinate getTranslation() {
        return translation;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }


    @Override
    public String toString() {
        return "FireworksVisibleAreaChangedEvent{" +
                "translation=" + translation +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
