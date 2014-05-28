/*
 * Created on Jul 18, 2003
 */
package org.gk.render;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * An Edge class for describing information flow or direction in a pathway.
 * @author wgm
 */
public class FlowLine extends HyperEdge {
    
    public FlowLine() {
        setNeedOutputArrow(true);
        lineWidth = 2.0f;
        // Don't want to change the super constructor. So remove one point here.
        backbonePoints.remove(1);
    }
    
    public String getType() {
        return "Flowline";
    }
    
    public void setBackbonePoints(java.util.List points) {
        this.backbonePoints = points;
    }
    
    /**
     * Use this method to check if some points used in the lines should
     * be removed. If points should be removed, they will be removed in 
     * this method.
     * @return true for some points are removed false for no points removed.
     */
    protected boolean validatePointsForLayout() {
        if (backbonePoints.size() < 3)
            return false; // Should not less than two points.
        Point prevP;
        Point nextP;
        Point p;
        prevP = (Point) backbonePoints.get(0);
        int lastIndex = backbonePoints.size() - 1;
        double distSq;
        java.util.List removePoints = new ArrayList();
        for (int i = 1; i < lastIndex; i++) {
            p = (Point) backbonePoints.get(i);
            nextP = (Point) backbonePoints.get(i + 1);
            // Check if p is in the line segment between prevP and nextP.
            distSq = Line2D.ptSegDistSq(prevP.x, prevP.y, nextP.x, nextP.y, p.x, p.y);
            if (distSq < SENSING_DISTANCE_SQ) { // Remove point p.
                removePoints.add(p);
            }
            prevP = p;
        }
        for (Iterator it = removePoints.iterator(); it.hasNext();) {
            p = (Point) it.next();
            backbonePoints.remove(p);
        }
        if (removePoints.size() > 0) {
            // Need to make sure all control points are correct
            validateWidgetControlPoints();
            return true;
        }
        else
            return false;
    }
    
    /**
     * Use this method to initialize the position info for a brand new RenderableReaction.
     * @param p the position.
     */
    public void initPosition(Point p) {
        Point inputHub = new Point(p.x - 40, p.y);
        setInputHub(inputHub);
        Point outputHub = new Point(p.x + 40, p.y);
        setOutputHub(outputHub);
        needCheckBounds = true;
    } 
    
    /**
     * Do nothing in set position. There is no position needed for FlowLine objects.
     */
    public void setPosition(Point p) {
    }
    
    /**
     * A position for a FlowLine object is the medium point from the list of backbone 
     * points. If the total number of backbone points is even, the middile point is
     * calculated on the fly for the two medium points.
     */
    public Point getPosition() {
        Point p = new Point();
        int size = backbonePoints.size();
        if (size % 2 == 0) {
            int index = size / 2;
            Point p1 = (Point) backbonePoints.get(index);
            Point p2 = (Point) backbonePoints.get(index - 1);
            p.x = (p1.x + p2.x) / 2;
            p.y = (p1.y + p2.y) / 2;
        }
        else {
            int index = size / 2;
            Point tmp = (Point) backbonePoints.get(index);
            p.x = tmp.x;
            p.y = tmp.y;
        }
        return p;
    }
    
    /**
     * Override the super class method to provide a simple implementation for
     * FlowLine objects.
     * @param pos
     */
    public void layout(Point pos) {
        if (pos == null)
            return;
        initPosition(pos);
    }
    
    public void validateBounds() {
        // Reset the bounds
        bounds.width = 5; // Minimum
        bounds.height = 5; // Minimum
        Point p = null;
        // Get all points related to this edge to do checking
        p = (Point) backbonePoints.get(0);
        bounds.x = p.x;
        bounds.y = p.y;
        for (int i = 1; i < backbonePoints.size(); i++) {
            p = (Point) backbonePoints.get(i);
            if (p.x > bounds.width + bounds.x) {
                bounds.width = p.x - bounds.x;
            }
            else if (p.x < bounds.x) {
                bounds.width += (bounds.x - p.x);
                bounds.x = p.x; 
            }
            if (p.y > bounds.height + bounds.y) {
                bounds.height = p.y - bounds.y;
            }
            else if (p.y < bounds.y) {
                bounds.height += (bounds.y - p.y);
                bounds.y = p.y;
            }
        }
        // Give it an extra space
        bounds.x -= 6;
        bounds.y -= 6;
        bounds.width += 12;
        bounds.height += 12;
        needCheckBounds = false;
    }
}
