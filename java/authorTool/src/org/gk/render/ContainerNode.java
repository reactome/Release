/*
 * Created on Jan 4, 2007
 *
 */
package org.gk.render;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;

public abstract class ContainerNode extends Node {
    // a flat list of all subunits. This is different from components in that
    // component can have hierarchy, but subunits can't
    // Used to control if components should be drawn. Default is false
    protected boolean hideComponents;
    // A flag to avoid calculate textbounds from bounds
    protected boolean blockTextPositionFromBoundsCall = false;
    protected int textPadding;

    public ContainerNode() {
        textPadding = DefaultRenderConstants.ROUND_RECT_ARC_WIDTH / 4;
    }
    
    public ContainerNode(String displayName) {
        super(displayName);
    }

    public void hideComponents(boolean hide) {
        this.hideComponents = hide;
        // Make sure all contained components is hidden
        if (components != null) {
            for (Iterator it = components.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                r.setIsVisible(!hide);
                if (r instanceof ContainerNode)
                    ((ContainerNode)r).hideComponents(hide);
            }
        }
    }

    public boolean isComponentsHidden() {
        return hideComponents;
    }

    /**
     * This method is used to ensure the text is still encapsulated 
     * by the bounds during resizing.
     */
    protected void ensureTextInBounds(boolean needValidate) {
        if (textBounds == null)
            return;
        ensureTextInBounds(bounds);
        // Call this method to make sure text can be wrapped around if the width
        // is changed.
        if (needValidate)
            invalidateTextBounds();
    }

    protected void ensureTextInBounds(Rectangle bounds) {
        // Check the top-left corner.
        // boundsBuffer has been considered in all textbounds related calculations
        if (textBounds.x < bounds.x + textPadding)
            textBounds.x = bounds.x + textPadding;
        if (textBounds.y < bounds.y + textPadding)
            textBounds.y = bounds.y + textPadding;
        // Check the bottom-right corner
        int diff = textBounds.x + textBounds.width - bounds.x - bounds.width + textPadding;
        if (diff > 0)
            textBounds.x -= diff;
        diff = textBounds.y + textBounds.height - bounds.y - bounds.height + textPadding;
        if (diff > 0)
            textBounds.y -= diff;
    }
    
    public void move(int dx, int dy) {
        if (bounds == null)
            return;
        switch (selectionPosition) {
            // Do resizing
            case NORTH_EAST :
                bounds.width += dx;
                bounds.height -= dy;
                bounds.y += dy;
                validateBoundsInView();
                ensureTextInBounds(true);
                validateMinimumBounds();
                break;
            case SOUTH_EAST :
                bounds.width += dx;
                bounds.height += dy;
                validateBoundsInView();
                ensureTextInBounds(true);
                validateMinimumBounds();
                break;
            case SOUTH_WEST :
                bounds.x += dx;
                bounds.width -= dx;
                bounds.height += dy;
                validateBoundsInView();
                ensureTextInBounds(true);
                validateMinimumBounds();
                break;
            case NORTH_WEST :
                bounds.x += dx;
                bounds.y += dy;
                bounds.width -= dx;
                bounds.height -= dy;
                validateBoundsInView();
                ensureTextInBounds(true);
                validateMinimumBounds();
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
                moveComponents(dx1, dy1);
                break;
        }
        validatePositionFromBounds();
        if (getContainer() instanceof RenderablePathway)
            getContainer().invalidateBounds();
        invalidateConnectWidgets();
        invalidateNodeAttachments();
    }
    
    /**
     * Move contained components. This method should be called during moving.
     * @param dx
     * @param dy
     */
    protected void moveComponents(int dx, int dy) {
        List components = getComponents();
        if (components == null || components.size() == 0)
            return;
        Renderable r = null;
        for (Iterator it = components.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            r.move(dx, dy);
        }
    }

    private void validateBoundsForSubunits(Graphics g) {
        List list = getComponents();
        if (list.size() == 0)
            return;
        Renderable r = null;
        for (Iterator it = list.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            if (r instanceof Node)
                ((Node)r).validateBounds(g);
            else if (r instanceof HyperEdge)
                ((HyperEdge)r).validateBounds();
        }
    }

    public void setBoundsFromComponents() {
        List list = getComponents();
        if (list == null || list.size() == 0 || hideComponents)
            return;
        Renderable node = (Renderable) list.get(0);
        Rectangle rect = node.getBounds();
        if (bounds == null)
            bounds = new Rectangle(rect);
        else {
            bounds.x = rect.x;
            bounds.y = rect.y;
            bounds.width = rect.width;
            bounds.height = rect.height;
        }
        for (int i = 1; i < list.size(); i++) {
            node = (Renderable) list.get(i);
            rect = node.getBounds();
            int maxX = bounds.x + bounds.width;
            int maxY = bounds.y + bounds.height;
            if (bounds.x > rect.x) {
                bounds.x = rect.x;
                bounds.width = maxX - bounds.x;
            }
            if (bounds.y > rect.y) {
                bounds.y = rect.y;
                bounds.height = maxY - bounds.y;
            }
            if (bounds.getMaxX() < rect.getMaxX())
                bounds.width = (int) (rect.getMaxX() - bounds.x + 1);
            if (bounds.getMaxY() < rect.getMaxY())
                bounds.height = (int) (rect.getMaxY() - bounds.y + 1);
        }
        if (position == null)
            position = new Point();
        position.x = (int) bounds.getCenterX();
        position.y = (int) bounds.getCenterY();
        // Make it a little shift
        bounds.x -= 1;
        bounds.y -= 1;
        bounds.width += 1;
        bounds.height += 1;
        // Have to make sure text layout is correct
        invalidateTextBounds();
    }

    public Renderable removeComponent(Renderable renderable) {
    	if(components.remove(renderable))
    		return renderable;
    	return null;
    }
        
    /**
     * Check if a Renderable object can be assigned to this Container object.
     * @param r
     * @return
     */
    public abstract boolean isAssignable(Renderable r);
    
    /**
     * Check a list of Renderables to see if their container setting is correct
     * against this ContainerNode.
     * @param renderables
     */
    public void validateContainerSetting(Renderable r) {
        // A hidden complex cannot take anything
        if (r == this || !isVisible)
            return;
        if (isAssignable(r)) {
            if (r.getContainer() != this) {
                // It should not be removed from a pathway container.
                if (r.getContainer() != null &&
                    r.getContainer().getClass() != RenderablePathway.class) {
                    r.getContainer().removeComponent(r);
                }
                r.setContainer(this);
                addComponent(r);
            }
        }
        else if (r.getContainer() == this) {
            r.setContainer(RenderUtility.getTopMostContainer(r.getContainer()));
            removeComponent(r);
        }
    }
    
    /**
     * Check if this Container contains the passed Renderable object.
     * @param r
     * @return
     */
    public boolean contains(Renderable r) {
        Renderable container = r.getContainer();
        while (container != null && 
               !(container instanceof RenderablePathway)) {
            if (container == this)
                return true;
            container = container.getContainer();
        }
        return false;
    }
    
    /**
     * A helper method to check if this complex is picked
     * up by checking a text bounds without boundsbuffer.
     * @param p
     * @return
     */
    protected boolean isTextPicked(Point p) {
        if (textBounds == null)
            return false;
        int x1 = textBounds.x + boundsBuffer;
        int y1 = textBounds.y + boundsBuffer;
        int x2 = textBounds.x + textBounds.width - boundsBuffer;
        int y2 = textBounds.y + textBounds.height - boundsBuffer;
        if (p.x > x1 && p.x < x2 &&
            p.y > y1 && p.y < y2)
            return true;
        return false;
    }
}
