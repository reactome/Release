/*
 * Created on Dec 19, 2006
 *
 */
package org.gk.render;

import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;

public class RenderableRNA extends Node {
    public static final int LOOP_WIDTH = 16;
    
    public RenderableRNA() {
    }
    
    public String getType() {
        return "RNA";
    }

    @Override
    protected void initBounds(Graphics g) {
        super.initBounds(g);
        // Give it some extra space
        bounds.x -= LOOP_WIDTH;
        bounds.y -= LOOP_WIDTH / 2;
        bounds.width += LOOP_WIDTH * 2;
        bounds.height += LOOP_WIDTH;
    }

    protected void resetLinkWidgetPositions() {
        if (linkWidgetPositions == null) {
            linkWidgetPositions = new ArrayList();
            for (int i = 0; i < 4; i ++)
                linkWidgetPositions.add(new Point());
        }
        if (bounds == null)
            return;
        // East
        Point p = (Point) linkWidgetPositions.get(0);
        p.x = bounds.x + bounds.width;
        p.y = bounds.y + bounds.height / 2;
        // South
        p = (Point) linkWidgetPositions.get(1);
        p.x = bounds.x + bounds.width / 2;
        p.y = bounds.y + bounds.height - LOOP_WIDTH / 2;
        // West
        p = (Point) linkWidgetPositions.get(2);
        p.x = bounds.x;
        p.y = bounds.y + bounds.height / 2;
        // North
        p = (Point) linkWidgetPositions.get(3);
        p.x = bounds.x + bounds.width / 2;
        p.y = bounds.y + LOOP_WIDTH / 2;
    }

    protected void validateConnectWidget(ConnectWidget widget) {
        super.validateConnectWidget(widget);
        // Sink the point at the north and south the the center a little bit
        Point p = widget.getPoint();
        if (p.x > bounds.x && p.x < bounds.getMaxX()) {
            if (p.y < bounds.getMaxY()) // north
                p.y += LOOP_WIDTH / 2;
            else if (p.y > bounds.y)
                p.y -= LOOP_WIDTH / 2;
        }
    }
    
    public Renderable generateShortcut() {
        RenderableRNA shortcut = new RenderableRNA();
        generateShortcut(shortcut);
        return shortcut;
    }
}
