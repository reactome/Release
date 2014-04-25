/*
 * Created on Dec 12, 2013
 *
 */
package org.gk.render;

import java.awt.Point;

/**
 * @author gwu
 *
 */
public class RenderableEntitySet extends Node {
    
    /**
     * Default constructor.
     */
    public RenderableEntitySet() {
    }
    
    /**
     * @param displayName
     */
    public RenderableEntitySet(String displayName) {
        super(displayName);
    }
    
    
    
    @Override
    public String getType() {
        return "EntitySet";
    }

    @Override
    protected void validateConnectWidget(ConnectWidget widget) {
        super.validateConnectWidget(widget);
        // Shift the point at the north and east side a little bit away from the the center
        Point p = widget.getPoint();
        // The following code is used to draw two same size rectangles with a little shift
//        // North
//        if (p.y < bounds.y && p.x > bounds.x && p.x < bounds.getMaxX()) {
//            p.y -= DefaultRenderConstants.MULTIMER_RECT_DIST;
//        } 
//        // East
//        else if (p.x > bounds.getMaxX() && p.y > bounds.y && p.y < bounds.getMaxY())
//            p.x += DefaultRenderConstants.MULTIMER_RECT_DIST;
        // The following code is used to draw two rectangles: one bigger and one smaller.
        if (p.y < bounds.y)
            p.y -= DefaultRenderConstants.MULTIMER_RECT_DIST;
        else if (p.y > bounds.getMaxY())
            p.y += DefaultRenderConstants.MULTIMER_RECT_DIST;
        if (p.x < bounds.x)
            p.x -= DefaultRenderConstants.MULTIMER_RECT_DIST;
        else if (p.x > bounds.getMaxX())
            p.x += DefaultRenderConstants.MULTIMER_RECT_DIST;
    }
}
