package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.FireworksResizeHandler;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksResizedEvent extends GwtEvent<FireworksResizeHandler> {
    public static Type<FireworksResizeHandler> TYPE = new Type<FireworksResizeHandler>();

    private double width;
    private double height;

    public FireworksResizedEvent(double width, double height) {
//        super(sender);
        this.width = width;
        this.height = height;
    }

    @Override
    public Type<FireworksResizeHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FireworksResizeHandler handler) {
        handler.onFireworksResized(this);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "FireworksResizedEvent{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
