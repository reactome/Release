package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.FireworksZoomHandler;
import org.reactome.web.fireworks.model.FireworksStatus;
import org.reactome.web.fireworks.util.Coordinate;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksZoomEvent extends GwtEvent<FireworksZoomHandler> {
    public static Type<FireworksZoomHandler> TYPE = new Type<FireworksZoomHandler>();

    private FireworksStatus status;
    private double width;
    private double height;
    private Coordinate coordinate;

    public FireworksZoomEvent(FireworksStatus status, double width, double height, Coordinate coordinate) {
        this.status = status;
        this.width = width;
        this.height = height;
        this.coordinate = coordinate;
    }

    @Override
    public Type<FireworksZoomHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FireworksZoomHandler handler) {
        handler.onFireworksZoomChanged(this);
    }

    public FireworksStatus getStatus() {
        return status;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public String toString() {
        return "FireworksZoomEvent{" +
                "status=" + status +
                ", width=" + width +
                ", height=" + height +
                ", coordinate=" + coordinate +
                '}';
    }
}
