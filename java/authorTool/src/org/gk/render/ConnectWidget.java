/*
 * AttachWidget.java
 *
 * Created on June 18, 2003, 2:31 PM
 */

package org.gk.render;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
/**
 * This data structure is used to connect a link to a node (entity, complex, pathway).
 * @author  wgm
 */
public class ConnectWidget implements Serializable {
    // Use a little buffer 
    public static final int BUFFER = 3;
    private Point point; // connecting point
    private Point controlPoint; // Another point that decide the line segement with point.
    private int role; // one of input, output, and helper
    private int index; // index of inputs, outputs or helpers.
    private int stoichiometry = 1; // default
    private Node connectedNode; 
    private HyperEdge edge;
    // A flag to set if new connection position should be calculated
    private boolean invalidate;
    // a ratio to keep track fixed position
    private double ratio;
    
    /** Creates a new instance of AttachWidget */
    public ConnectWidget(Point p, 
                         Point controlP, 
                         int role, 
                         int index) {
        this.point = p;
        this.controlPoint = controlP;
        this.role = role;
        this.index = index;
    }
    
    /**
     * Create a ConnectWidget with the same rendering information but not linking to 
     * nodes and edges.
     * @return
     */
    public ConnectWidget shallowCopy() {
        ConnectWidget clone = new ConnectWidget(new Point(point),
                                                new Point(controlPoint), 
                                                role, 
                                                index);
        clone.stoichiometry = stoichiometry;
        clone.connectedNode = connectedNode;
        clone.ratio = ratio;
        return clone;
    }
    
    public Point getPoint() {
        return this.point;
    }
    
    public void setPoint(Point p) {
        this.point = p;
    }
    
    public Point getControlPoint() {
        return this.controlPoint;
    }
    
    public int getRole() {
        return this.role;
    }
    
    public int getIndex() {
        return this.index;
    }
    
    public void setIndex(int index) {
    	this.index = index;
    }
    
    /**
     * Set the connected node. 
     * @param node a Renderable that can be connected to a RenderableReaction.
     */
    public void setConnectedNode(Node node) {
        connectedNode = node;
        invalidate = true;
        // Want to get the ratio to be used for RenderableGene.
        if (connectedNode != null && connectedNode.getBounds() != null) {
            Rectangle bounds = connectedNode.getBounds();
            ratio = (point.x - bounds.x) / (double)bounds.width;
        }
    }
    
    /**
     * Replace the connected Node with a passed Node object.
     * @param node
     */
    public void replaceConnectedNode(Node node) {
        if (connectedNode == null)
            return;
        connectedNode.removeConnectWidget(this);
        node.addConnectWidget(this);
        setConnectedNode(node);
    }
    
    /**
     * The relative position of the link point at the node's bounds.
     * @return
     */
    public double getLinkRatio() {
        return this.ratio;
    }
    
    public Node getConnectedNode() {
        return this.connectedNode;
    }
    
    public void setEdge(HyperEdge reaction) {
        this.edge = reaction;
    }
    
    public HyperEdge getEdge() {
        return this.edge;
    }
    
    public void setControlPoint(Point p) {
        this.controlPoint = p;
    }
    
    /**
     * Mark this ConnectWidget as invalidat. An invalidate ConnectWidget should be validated before its information
     * is used for drawing.
     */
    public void invalidate() {
        invalidate = true;
    }
    
    public boolean isInvalidate() {
    	return invalidate;
    }
    
    /**
     * Use this method to re-calculate the connected position between node and link.
     */
    public void validate() {
        // Do nothing.
        if (!invalidate || connectedNode == null)
            return;
        // Mark reaction bounds is not correct
        if (getEdge() != null)
        	getEdge().invalidateBounds();
        invalidate = false;
        connectedNode.validateConnectWidget(this);
	}

    /**
     * Override the superclass method and let the contained point to determine the identity of ConnectWidget.
     */
    public boolean equals(Object obj) {
        if (obj instanceof ConnectWidget) {
            ConnectWidget another = (ConnectWidget)obj;
            if ((point != null) && (point == another.point))
                return true;
            else
                return super.equals(obj);
        }
        else
            return false;
    }
    
    /**
     * Override superclass method and let the contained point to determine the identity of ConnectWidget.
     */
    public int hashCode() {
        if (point != null)
            return point.hashCode();
        return super.hashCode();
    }
    
    /**
     * Connect the selected node and edge.
     */
    public void connect() {
        if (edge != null)
            edge.addConnectWidget(this);
        if (connectedNode != null)
            connectedNode.addConnectWidget(this);
    }
    
    /**
     * Disconnect the node and edge.
     */
    public void disconnect() {
        if (edge != null)
            edge.removeConnectWidget(this);
        if (connectedNode != null)
            connectedNode.removeConnectWidget(this);
    }
    
    public void setStoichiometry(int stoi) {
    	this.stoichiometry = stoi;
    }
    
    public int getStoichiometry() {
    	return this.stoichiometry;
    }
}
