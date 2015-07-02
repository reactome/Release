package org.reactome.server.fireworks.output;

import org.reactome.server.fireworks.model.GraphNode;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Node {

    private Long dbId;
    private String stId;
    private String name;
    private Double ratio;
    private Double x;
    private Double y;
    private Double angle;

    public Node(GraphNode node){
        this.dbId = node.getDbId();
        this.stId = node.getStId();
        this.name = node.getName();
        this.ratio = node.getRatio();
        this.x = node.getX();
        this.y = node.getY();
        if(node.getAngle()==null) System.err.println(node.getDbId() + " - " + node.getName());
        this.angle = node.getAngle() % (2 * Math.PI); //Normalisation [0 - 2 PI]
    }

    public Long getDbId() {
        return dbId;
    }

    public String getStId() {
        return stId;
    }

    public String getName() {
        return name;
    }

    public Double getRatio() {
        return ratio;
    }

    public Double getAngle() {
        return angle;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }
}
