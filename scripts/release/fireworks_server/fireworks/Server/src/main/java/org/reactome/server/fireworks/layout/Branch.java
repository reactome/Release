package org.reactome.server.fireworks.layout;

import org.reactome.server.fireworks.model.GraphNode;

import java.util.Collections;
import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
class Branch {
    static final double RADIUS_DELTA = 12d;

    double minRadius;
    double minAngle;
    double maxAngle;
    double angle;

    Burst burst;

    List<GraphNode> nodes;
    double length;
    double radius;

    Branch(Burst burst, double minRadius, double minAngle, double maxAngle, List<GraphNode> nodes) {
        this.burst = burst;
        this.minRadius = minRadius;
        this.minAngle = minAngle;
        this.maxAngle = maxAngle;
        this.angle = Math.abs(maxAngle - minAngle);
        this.nodes = nodes;
        if(burst.getDirection().equals(Direction.ANTICLOCKWISE)){
            Collections.reverse(this.nodes);
        }
        this.setLength();
        this.setRadius();
    }

    double getRadius(){
        return this.radius;
    }

    void setNodesPosition(){
        double lAvailable = angle * this.radius;                    // first we calculate the space we have
        double angle = minAngle;
        for (GraphNode node : nodes) {
            if(!node.hasLayoutData() || node.getRadius() > this.radius){
                double p = length / node.getSize();                 // p is the proportion factor
                double size = lAvailable / p;                       // a normalised size depending on the factor
                double minAngle = angle;
                double nodeAngle = angle + (size/2d) / this.radius; // the angle where the node has to be drawn
                double x = this.burst.getCenterX() + this.radius * Math.cos(nodeAngle);
                double y = this.burst.getCenterY() + this.radius * Math.sin(nodeAngle);
                angle += size/this.radius;

                node.setLayoutParameters(x, y, this.radius, nodeAngle, minAngle, angle);
            }
        }
    }

    private void setLength(){
        length = 0;
        for (GraphNode node : nodes) {
            length += node.getSize();
        }
    }

    private void setRadius(){
        this.radius = length / angle;
        if(this.radius < (minRadius + RADIUS_DELTA)){
            this.radius = minRadius + RADIUS_DELTA;
        }
    }
}
