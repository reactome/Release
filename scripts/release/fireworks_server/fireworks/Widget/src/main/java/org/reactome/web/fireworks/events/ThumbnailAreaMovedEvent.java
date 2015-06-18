package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.ThumbnailAreaMovedHandler;
import org.reactome.web.fireworks.util.Coordinate;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ThumbnailAreaMovedEvent extends GwtEvent<ThumbnailAreaMovedHandler> {
    public static Type<ThumbnailAreaMovedHandler> TYPE = new Type<ThumbnailAreaMovedHandler>();

    private Coordinate coordinate;

    public ThumbnailAreaMovedEvent(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    @Override
    public Type<ThumbnailAreaMovedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ThumbnailAreaMovedHandler handler) {
        handler.onThumbnailAreaMoved(this);
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public String toString() {
        return "ThumbnailAreaMovedEvent{" +
                "coordinate=" + coordinate +
                '}';
    }
}
