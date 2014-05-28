/*
 * Created on Dec 19, 2006
 *
 */
package org.gk.render;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

public class RenderableCompartment extends ContainerNode {
    private final int DEFAULT_WIDTH = 250;
    static final int RESIZE_WIDGET_WIDTH = 4;
    // Draw the inner rectangle if any
    Rectangle insets;
    
    public RenderableCompartment() {
        super();
        bounds = new Rectangle();
        bounds.width = DEFAULT_WIDTH;
        bounds.height = DEFAULT_WIDTH;
        boundsBuffer = DefaultRenderConstants.RECTANGLE_DIST;
        minWidth = 3 * (DefaultRenderConstants.RECTANGLE_DIST + 
                        DefaultRenderConstants.SELECTION_WIDGET_WIDTH);
        //TODO: This should be removed
        backgroundColor = DefaultRenderConstants.COMPARTMENT_COLOR;
        lineColor = DefaultRenderConstants.COMPARTMENT_OUTLINE_COLOR;
        // Use an empty ConnectInfo to avoid null exception
        connectInfo = new NodeConnectInfo(); 
        // We dont' want to have any linkwdigets, so we
        // just assign null to these widgets
        linkWidgetPositions = new ArrayList(4);
        for (int i = 0; i < 4; i ++)
            linkWidgetPositions.add(null);
        isLinkable = false;
        isTransferrable = false;
    }
    
    @Override
    public void setDisplayName(String name) {
        super.setDisplayName(name);
        if (isInsetsNeeded()) {
            if (insets == null) {
                insets = new Rectangle();
                // default value
                insets.x = bounds.x + DefaultRenderConstants.RECTANGLE_DIST;
                insets.y = bounds.y + DefaultRenderConstants.RECTANGLE_DIST;
                insets.width = bounds.width - 2 * DefaultRenderConstants.RECTANGLE_DIST;
                insets.height = bounds.height - 2 * DefaultRenderConstants.RECTANGLE_DIST;
            }
        }
        else
            insets = null;
    }
    
    

    @Override
    protected void ensureTextInBounds(boolean needValidate) {
        if (textBounds == null)
            return;
        if (insets == null || insets.isEmpty())
            ensureTextInBounds(bounds);
        else
            ensureTextInBounds(insets);
        // Call this method to make sure text can be wrapped around if the width
        // is changed.
        if (needValidate)
            invalidateTextBounds();
    }

    @Override
    public void setPosition(Point p) {
        setPosition(p.x, p.y);
    }

    public String getType() {
        return "Compartment";
    }
    
    public boolean isInsetsNeeded() {
        return getDisplayName() != null && !getDisplayName().endsWith("membrane");
    }
    
    public Rectangle getInsets() {
        return insets;
    }
    
    public void setInsets(Rectangle insets) {
        this.insets = insets;
    }

    /**
     * This method is used to test if this compartment can be assigned to
     * the passed Renderable object. The checking is based on bounds. If the bounds
     * of the passed object is touched or contained by this compartment bounds,
     * true will be returned. Otherwise, false will be returned.
     * @param r
     * @return
     */
    public boolean isAssignable(Renderable r) {
        if (r == this)
            return false;
        // Don't assign a compartment to a pathway
        if (r instanceof RenderablePathway)
            return false;
        // If a Renderable object is a subunit of a Renderable complex,
        // it should not be assigned to this compartment.
        //TODO: Need to check a case: a complex component has different component setting
        // from its container complex.
        if (r.getContainer() instanceof RenderableComplex)
            return false;
        Rectangle rBounds = r.getBounds();
        // If r is another compartment, r should be contained fully
        if (r instanceof RenderableCompartment) {
            return bounds.contains(rBounds);
        }
        // Use position for hyper edge. Otherwise, the behavior is a little strange since the bounds
        // for a reaction usually is too big.
        if (r instanceof HyperEdge || rBounds == null) {
            Point p = r.getPosition();
            if (p != null)
                return bounds.contains(p);
            else
                return false;
        }
        return bounds.intersects(rBounds);
    }
    
    public boolean isPicked(Point p) {
        // reset
        selectionPosition = SelectionPosition.NONE;
        // Check contained components first.
        // Have to be aware that components are double checked.
        if (components != null && components.size() > 0) {
            for (Renderable r : components) {
                if (r.canBePicked(p))
                    return false;
            }
        }
        if (isResizeWidgetPicked(p))
            return true;
        // Check if text label is picked
        if (isTextPicked(p)) {
            selectionPosition = SelectionPosition.TEXT;
            return true;
        }
        // Want to check if the bands between the bounds and the boundsbuffer is picked
        // Check the distance between P and the bound
        Rectangle inside = new Rectangle();
        inside.x = bounds.x + boundsBuffer;
        inside.y = bounds.y + boundsBuffer;
        inside.width = bounds.width - 2 * boundsBuffer;
        inside.height = bounds.height - 2 * boundsBuffer;
        if (bounds.contains(p) &&
            !inside.contains(p))
            return true;
        // Check if the insets is clicked
        if (insets != null && !insets.isEmpty()) {
            inside.x = insets.x + boundsBuffer;
            inside.y = insets.y + boundsBuffer;
            inside.width = insets.width - 2 * boundsBuffer;
            inside.height = insets.height - 2 * boundsBuffer;
            if (insets.contains(p) && !inside.contains(p))
                return true;
        }
        return false;
    }
    
    @Override
    protected boolean isResizeWidgetPicked(Point p) {
        boolean rtn = super.isResizeWidgetPicked(p);
        if (rtn)
            return true;
        if (insets == null || insets.isEmpty())
            return false;
        Rectangle resizeWidget = new Rectangle();
        resizeWidget.width = 2 * RESIZE_WIDGET_WIDTH;
        resizeWidget.height = 2 * RESIZE_WIDGET_WIDTH;
        resizeWidget.x = insets.x + insets.width - RESIZE_WIDGET_WIDTH;
        resizeWidget.y = insets.y - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SelectionPosition.IN_NORTH_EAST;
            return true;
        }
        // southeast
        resizeWidget.x = insets.x + insets.width - RESIZE_WIDGET_WIDTH;
        resizeWidget.y = insets.y + insets.height - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SelectionPosition.IN_SOUTH_EAST;
            return true;
        }
        // southwest
        resizeWidget.x = insets.x - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SelectionPosition.IN_SOUTH_WEST;
            return true;
        }
        // northwest
        resizeWidget.y = insets.y - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SelectionPosition.IN_NORTH_WEST;
            return true;
        }
        return false;
    }
    
    @Override
    public void move(int dx, int dy) {
        if (bounds == null)
            return;
        switch (selectionPosition) {
            // Do resizing
            case NORTH_EAST :
                bounds.width += dx;
                bounds.height -= dy;
                bounds.y += dy;
                sanityCheckForMove();
                break;
            case IN_NORTH_EAST :
                // Make sure insets.x, and insets.y should not
                // out of bounds
                if (insets.y + dy < bounds.y)
                    dy = bounds.y - insets.y;
                insets.width += dx;
                insets.height -= dy;
                insets.y += dy;
                sanityCheckForMove();
                break;
            case SOUTH_EAST :
                bounds.width += dx;
                bounds.height += dy;
                sanityCheckForMove();
                break;
            case IN_SOUTH_EAST :
                insets.width += dx;
                insets.height += dy;
                sanityCheckForMove();
                break;
            case SOUTH_WEST :
                bounds.x += dx;
                bounds.width -= dx;
                bounds.height += dy;
                sanityCheckForMove();
                break;
            case IN_SOUTH_WEST :
                if (insets.x + dx < bounds.x)
                    dx = bounds.x - insets.x;
                insets.x += dx;
                insets.width -= dx;
                insets.height += dy;
                sanityCheckForMove();
                break;
            case NORTH_WEST :
                bounds.x += dx;
                bounds.y += dy;
                bounds.width -= dx;
                bounds.height -= dy;
                sanityCheckForMove();
                break;
            case IN_NORTH_WEST :
                if (insets.x + dx < bounds.x)
                    dx = bounds.x - insets.x;
                if (insets.y + dy < bounds.y)
                    dy = bounds.y - insets.y;
                insets.x += dx;
                insets.y += dy;
                insets.width -= dx;
                insets.height -= dy;
                sanityCheckForMove();
                break;    
            case TEXT :
                textBounds.x += dx;
                textBounds.y += dy;
                ensureTextInBounds(false);
                blockTextPositionFromBoundsCall = true;
                break;
                // for node feature
            case FEATURE : case STATE :
                moveNodeAttachment(dx, dy);
                break;
            // Treat node as the default
            default :
                // Need to get correct dx, dy to avoid out of the view
                int dx1 = dx;
                int dy1 = dy;
                if (ensureBoundsInView) {
                    if (bounds.x + dx < pad)
                        dx1 = -bounds.x + pad;
                    if (bounds.y + dy < pad)
                        dy1 = -bounds.y + pad;
                }
                bounds.x += dx1;
                bounds.y += dy1;
                // Make sure text can be moved too
                if (textBounds != null) {
                    textBounds.x += dx1;
                    textBounds.y += dy1;
                }
                if (insets != null) {
                    insets.x += dx1;
                    insets.y += dy1;
                }
                moveComponents(dx1, dy1);
                break;
        }
        validatePositionFromBounds();
        if (getContainer() instanceof RenderablePathway)
            getContainer().invalidateBounds();
        invalidateConnectWidgets();
        invalidateNodeAttachments();
    }

    protected void sanityCheckForMove() {
        validateBoundsInView();
        ensureTextInBounds(true);
        validateMinimumBounds();
        ensureInsetsInBounds();
    }
    
    private void ensureInsetsInBounds() {
        if (bounds == null || insets == null || insets.isEmpty())
            return;
        // Make sure the position of insets is inside bounds
        if (insets.x < bounds.x)
            insets.x = bounds.x;
        else if (insets.x > bounds.x + bounds.width)
            insets.x = bounds.x + bounds.width;
        if (insets.y < bounds.y)
            insets.y = bounds.y;
        else if (insets.y > bounds.y + bounds.height)
            insets.y = bounds.y + bounds.height;
        // Make sure the width and height of insets are not bigger than bounds
        if (insets.width + insets.x > bounds.width + bounds.x)
            insets.width = bounds.width + bounds.x - insets.x;
        if (insets.height + insets.y > bounds.height + bounds.y)
            insets.height = bounds.height + bounds.y - insets.y;
    }

    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        bounds.x = x - bounds.width / 2;
        bounds.y = y - bounds.height / 2;
        if (insets != null) {
            insets.x = x - insets.width / 2;
            insets.y = y - insets.height / 2;
        }
    }

    public void invalidateBounds() {
        // No need
    }
    
    /**
     * To block any actions.
     */
    @Override
    protected void resetLinkWidgetPositions() {
        // Just block it.
    }

    /**
     * Change the default behavior. If the whole bounds of this compartment is covered
     * by the selection rectangle, this compartment will be selected. Otherwise, it is 
     * not even by a touch or its position (the central point) is contained by the selection
     * rectangle.
     */
    @Override
    public void select(Rectangle rect) {
        isSelected = rect.contains(bounds);
    }
    
    @Override
    public void setTextPosition(int x, int y) {
        if (textBounds == null)
            textBounds = new Rectangle();
        textBounds.x = x;
        textBounds.y = y;
        blockTextPositionFromBoundsCall = true;
    }
    
    @Override
    protected void setTextPositionFromBounds() {
        if (blockTextPositionFromBoundsCall)
            return;
        super.setTextPositionFromBounds();
    }
    
}
