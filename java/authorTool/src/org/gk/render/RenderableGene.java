/*
 * Created on Dec 12, 2006
 *
 */
package org.gk.render;

import static org.gk.render.SelectionPosition.SOUTH_EAST;
import static org.gk.render.SelectionPosition.SOUTH_WEST;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import org.gk.util.DrawUtilities;

public class RenderableGene extends Node {
    // Placing these two constants here is not good since
    // it couples with presentation information. Have to
    // think out a good way later.
    public static final int GENE_SYMBOL_PAD = 4;
    public static final int GENE_SYMBOL_WIDTH = 50;
    // An arrow point
    private Point arrowPoint;
    
    public RenderableGene() {
        arrowPoint = new Point();
    }
    
    public RenderableGene(String displayName) {
        super(displayName);
    }

    public String getType() {
        return "Gene";
    }
    
    public Point getArrowPoint() {
        return this.arrowPoint;
    }
    
    public void setArrowPoint(int x, int y) {
        arrowPoint.x = x;
        arrowPoint.y = y;
    }
    
    protected void resetLinkWidgetPositions() {
        if (linkWidgetPositions == null) {
            linkWidgetPositions = new ArrayList();
            linkWidgetPositions.add(arrowPoint);
            for (int i = 0; i < 3; i++)
                linkWidgetPositions.add(null); // Add place holders
        }
    }

    public Point getLinkPoint() {
        if (linkPoint == null) {
            linkPoint = new Point();
        }
        linkPoint.x = arrowPoint.x + DrawUtilities.ARROW_LENGTH + GENE_SYMBOL_PAD;
        linkPoint.y = arrowPoint.y;
        return linkPoint;
    }
    
    public boolean isPicked(Point p) {
        if (getBounds() == null)
            return false;
        // Check only two resizing widgets at the southern side
        // reset
        selectionPosition = SelectionPosition.NONE;
        Rectangle resizeWidget = new Rectangle();
        resizeWidget.width = 2 * RESIZE_WIDGET_WIDTH;
        resizeWidget.height = 2 * RESIZE_WIDGET_WIDTH;
        // southeast
        resizeWidget.x = bounds.x + bounds.width - RESIZE_WIDGET_WIDTH;
        resizeWidget.y = bounds.y + bounds.height - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SOUTH_EAST;
            return true;
        }
        // southwest
        resizeWidget.x = bounds.x - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SOUTH_WEST;
            return true;
        }
        // Check with text bounds
        int minY = bounds.y + GENE_SYMBOL_WIDTH / 2 - boundsBuffer;
        if ((p.x > bounds.x && p.x < bounds.getMaxX()) &&
            (p.y > minY && p.y < bounds.getMaxY())) {
            selectionPosition = SelectionPosition.NODE;
            return true;
        }
        // Check with arrow bounds
        int minX = arrowPoint.x - boundsBuffer;
        minY = arrowPoint.y - boundsBuffer;
        int maxX = bounds.x  + bounds.width + DrawUtilities.ARROW_LENGTH;
        int maxY = bounds.y + GENE_SYMBOL_WIDTH;
        if (p.x > minX && p.x < maxX &&
            p.y > minY && p.y < maxY) {
            selectionPosition = SelectionPosition.NODE;
            return true;
        }
        return false;
    }
    
    public void validateBounds(Graphics g) {
        if (!needCheckBounds && !needCheckTextBounds)
            return;
        super.validateBounds(g);
        // reset position to point the arrow point
        int x = bounds.x + bounds.width - RenderableGene.GENE_SYMBOL_PAD;
        int y = textBounds.y - RenderableGene.GENE_SYMBOL_WIDTH / 2;
        setArrowPoint(x, y);
    }
    
    /**
     * Limit the super class behavior so that only width can be changed for resizing.
     */
    public void move(int dx, int dy) {
        switch (selectionPosition) {
            case SOUTH_EAST :
                bounds.width += dx;
                duringMoving = false;
                break;
            case SOUTH_WEST :
                bounds.x += dx;
                bounds.width -= dx;
                duringMoving = false;
                break;
            default :
                bounds.x += dx;
                bounds.y += dy;
                duringMoving = true;
                break;
        }
        validateBoundsInView();
        validatePositionFromBounds();
        // Cannot move widgets to catch up the node's moving.
        // calculation is not fast enough.
        invalidateConnectWidgets();
        // Call this method instead just setting the flag
        //invalidateBounds();
        invalidateTextBounds();
    }
    
    protected void initBounds(Graphics g) {
        super.initBounds(g);
        bounds.height += (RenderableGene.GENE_SYMBOL_WIDTH / 2+ 4);
        bounds.y -= (RenderableGene.GENE_SYMBOL_WIDTH / 2 + 2);
        if (bounds.width < RenderableGene.GENE_SYMBOL_WIDTH)
            bounds.width = RenderableGene.GENE_SYMBOL_WIDTH;
    }
    
    /**
     * Override superclass method to provide a behavior for gene.
     */
    protected void setTextPositionFromBounds() {
        if (textBounds == null) 
            textBounds = new Rectangle(bounds);
        textBounds.x = bounds.x + (bounds.width - textBounds.width) / 2;
        // height should not be resizable for gene
        textBounds.y = bounds.y + RenderableGene.GENE_SYMBOL_WIDTH / 2 + 2;
    }
    
    protected void validateConnectWidget(ConnectWidget widget) {
        Point point = widget.getPoint();
        if (widget.getRole() == HyperEdge.INPUT) {
            // Should have fixed position
            Point p = getLinkPoint();
            point.x = p.x + 1;
            point.y = p.y;
            return;
        }
        // Always in the north
        Rectangle bounds = getBounds();
        int fixedY = bounds.y + RenderableGene.GENE_SYMBOL_WIDTH / 2 - 2;
        if (point.y != fixedY)
            point.y = fixedY;
        double ratio = widget.getLinkRatio();
        // Don't let x goes out of the bounds
        if (ratio > 0.0) {
            point.x = (int)(bounds.x + bounds.width * ratio);
        }
        else if (point.x < bounds.x)
            point.x = bounds.x;
        else if (point.x > bounds.x + bounds.width)
            point.x = bounds.x + bounds.width;
    }
    
    public Renderable generateShortcut() {
        RenderableGene shortcut = new RenderableGene();
        generateShortcut(shortcut);
        return shortcut;
    }
    
}
