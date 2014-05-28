/*
 * Created on Jun 11, 2008
 *
 */
package org.gk.render;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;



/**
 * This abstract class is used to describe informaition about Node states (open, close, active) or
 * sequence features (Tyr phosphorylation).
 * @author wgm
 *
 */
public abstract class NodeAttachment implements Serializable{
    // Used to map instance stored in the database
    private Long reactomeId;
    // position related to the bounds of the containing node
    // the reference point is the up-left corner of the bounds
    public double relativeX;
    public double relativeY;
    // Used to check is this node is selected
    private boolean isSelected;
    // bounds
    private Rectangle bounds;
    // A flag indicate that bounds should be recalculated
    private boolean isBoundsWrong = true;
    // Used as a padding between the bounds and text labels
    protected int textPadding = 2;
    // Used to track if two NodeAttachment is the same
    protected int trackId;
    // Check if an attachment is editable
    private boolean isEditable = true;
    
    public NodeAttachment() {
    }
    
    public void setReactomeId(Long id) {
        this.reactomeId = id;
    }
    
    public Long getReactomeId() {
        return this.reactomeId;
    }
    
    public void setIsEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }
    
    public boolean isEditable() {
        return this.isEditable;
    }
    
    public void setIsSelected(boolean selected) {
        this.isSelected = selected;
    }
    
    public boolean isSelected() {
        return this.isSelected;
    }
    
    public Rectangle getBounds() {
        return bounds;
    }
    
    public void invalidateBounds() {
        isBoundsWrong = true;
    }
    
    /**
     * Set the relative position of this NodeAttachment object.
     * The reference point of the containing Node's bounds' up-left
     * corner.
     * @param relativeX
     * @param relativeY
     */
    public void setRelativePosition(double relativeX,
                                    double relativeY) {
        this.relativeX = relativeX;
        this.relativeY = relativeY;
        isBoundsWrong = true;
    }
    
    public double getRelativeX() {
        return this.relativeX;
    }
    
    public double getRelativeY() {
        return this.relativeY;
    }
    
    public void move(int dx,
                     int dy,
                     Rectangle nodeBounds) {
        // Find the original position
        int x = bounds.x + bounds.width / 2;
        int y = bounds.y + bounds.height / 2;
        int dx1 = Math.abs(x - nodeBounds.x);
        int dx2 = Math.abs(x - nodeBounds.x - nodeBounds.width);
        int dy1 = Math.abs(y - nodeBounds.y);
        int dy2 = Math.abs(y - nodeBounds.y - nodeBounds.height);
        if ((dx1 <= 1 || dx2 <= 1) && dy != 0) { // Have to make some changes. Otherwise, it will stuck
            // Try to change the y
            bounds.y += dy;
            if (bounds.getCenterY() < nodeBounds.y)
                bounds.y = (int)(nodeBounds.y - bounds.getHeight() / 2.0); 
            else if (bounds.getCenterY() > nodeBounds.getMaxY())
                bounds.y = (int)(nodeBounds.getMaxY() - bounds.getHeight() / 2.0);
        }
        else if ((dy1 <= 1 || dy2 <= 1) && dx != 0) {
            // Try to change the x
            bounds.x += dx;
            if (bounds.getCenterX() < nodeBounds.x)
                bounds.x = (int)(nodeBounds.x - bounds.getWidth() / 2.0);
            else if (bounds.getCenterX() > nodeBounds.getMaxX()) {
                bounds.x = (int) (nodeBounds.getMaxX() - bounds.getWidth() / 2);
            }
        }
        extractRelativePositionFromBounds(nodeBounds);
    }
    
    private void extractRelativePositionFromBounds(Rectangle nodeBounds) {
        double x = bounds.getCenterX();
        double y = bounds.getCenterY();
        relativeX = (x - nodeBounds.x) / nodeBounds.width;
        relativeY = (y - nodeBounds.y) / nodeBounds.height;
    }
    
    public void validateBounds(Rectangle nodeBounds,
                               Graphics g) {
        if (!isBoundsWrong && bounds != null) 
            return;
        // Calculate the position of the bounds
        int x = (int)(nodeBounds.x + nodeBounds.width * relativeX);
        int y = (int)(nodeBounds.y + nodeBounds.height * relativeY);
        Graphics2D g2 = (Graphics2D) g;
        Font font = g2.getFont();
        font = font.deriveFont(font.getSize2D() - 3.0f);
        FontMetrics metrics = g2.getFontMetrics(font);
        Rectangle2D textBounds = metrics.getStringBounds(getLabel(),
                                                         g2);
        int width = (int)(textBounds.getWidth() + textPadding);
        int height = (int)(textBounds.getHeight() + textPadding);
        // Make sure the width is at least the same as height to make it
        // look nicer
        if (width < height)
            width = height;
        x -= width / 2;
        y -= height / 2;
        if (bounds == null)
            bounds = new Rectangle(x, y, width, height);
        else {
            bounds.x = x;
            bounds.y = y;
            bounds.width = width;
            bounds.height = height;
        }
        isBoundsWrong = false;
    }
    
    /**
     * Check if this NodeAttachment is picked
     * @param p
     * @return
     */
    public boolean isPicked(Point p) {
        if (bounds == null)
            return false;
        return bounds.contains(p);
    }
    
    public void setTrackId(int id) {
        this.trackId = id;
    }
    
    public int getTrackId() {
        return this.trackId;
    }
    
    /**
     * This method is used to duplicate this NodeAttachment object.
     * @return
     */
    public abstract NodeAttachment duplicate();
    
    public abstract void setLabel(String label);

    public abstract String getDescription();

    public abstract void setDescription(String description);

    public abstract String getLabel();
    
}
