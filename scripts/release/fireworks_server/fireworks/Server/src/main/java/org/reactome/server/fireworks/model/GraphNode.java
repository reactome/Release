package org.reactome.server.fireworks.model;

import org.reactome.server.analysis.core.model.PathwayNode;
import org.reactome.server.fireworks.output.Edge;
import org.reactome.server.fireworks.output.Node;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class GraphNode implements Comparable<GraphNode> {

    private Long dbId;
    private String stId;
    private String name;
    private Double ratio;
    private Double angle;

    private boolean layoutData = false;
    private int level = 0;

    private double x;
    private double y;
    private double radius;
    private double minAngle;
    private double maxAngle;

    public Set<GraphNode> children;
    private Set<GraphNode> parents;

    public GraphNode(Long speciesId, String name) {
        this.dbId = speciesId;
        this.stId = "TLP";
        this.ratio = 0.5;
        this.name = name;
        this.children = new HashSet<GraphNode>();
        this.parents = new HashSet<GraphNode>();
    }

    public GraphNode(PathwayNode node) {
        this.children = new HashSet<GraphNode>();
        this.parents = new HashSet<GraphNode>();
        this.dbId = node.getPathwayId();
        this.stId = node.getStId();
        this.ratio = node.getPathwayNodeData().getEntitiesRatio();
        this.name = node.getName();
    }

    public void addChild(GraphNode child){
        child.setLevel(this.level + 1);
        this.children.add(child);
        child.addParent(this);
    }

    protected void addParent(GraphNode parent){
        this.parents.add(parent);
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

    public Double getRatio(){
        return ratio;
    }

    public Double getSize() {
        return (ratio + 0.01) * 15;
    }

    public Double getAngle() {
        return angle;
    }

    public double getMinAngle() {
        return minAngle;
    }

    public double getMaxAngle() {
        return maxAngle;
    }

    public double getRadius() {
        return radius;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public List<GraphNode> getChildren() {
        List<GraphNode> rtn = new LinkedList<GraphNode>();
        for (GraphNode child : this.children) {
            rtn.add(child);
        }
        Collections.sort(rtn);
        return rtn;
    }

    public List<Edge> getEdges(){
        List<Edge> rtn = new LinkedList<Edge>();
        for (GraphNode child : this.getChildren()) {
            if(!this.dbId.equals(-1L) && this.layoutData) {
                rtn.add(new Edge(this.getDbId(), child.getDbId()));
            }
            rtn.addAll(child.getEdges());
        }
        return rtn;
    }

    public List<Node> getNodes(){
        List<Node> rtn = new LinkedList<Node>();
        if(!this.dbId.equals(-1L) && this.layoutData) {
            rtn.add(new Node(this));
        }
        for (GraphNode child : this.children) {
            rtn.addAll(child.getNodes());
        }
        return rtn;
    }

    @SuppressWarnings("UnusedDeclaration")
    public List<GraphNode> getParents() {
        List<GraphNode> rtn = new LinkedList<GraphNode>();
        for (GraphNode parent : this.parents) {
            rtn.add(parent);
        }
        Collections.sort(rtn);
        return rtn;
    }

    @Override @SuppressWarnings("NullableProblems")
    public int compareTo(GraphNode o) {
        return this.name.compareTo(o.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphNode graphNode = (GraphNode) o;

        //noinspection RedundantIfStatement
        if (dbId != null ? !dbId.equals(graphNode.dbId) : graphNode.dbId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dbId != null ? dbId.hashCode() : 0;
    }

    public boolean hasLayoutData() {
        return layoutData;
    }

    protected void setLevel(int level){
        if(this.level<level){
            this.level = level;
        }
    }

    public void setLayoutParameters(double x, double y, double radius, double angle, double minAngle, double maxAngle){
        this.layoutData = true;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.angle = angle;
        this.minAngle = minAngle;
        if(Math.abs(maxAngle - minAngle) > Math.PI){
            this.maxAngle = minAngle + Math.PI;
        }else{
            this.maxAngle = maxAngle;
        }
    }

    public void setLayoutParameters(GraphNode eq){
        this.layoutData = true;
        this.x = eq.x;
        this.y = eq.y;
        this.radius = eq.radius;
        this.angle = eq.angle;
        this.minAngle = eq.minAngle;
        this.maxAngle = eq.maxAngle;
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "dbId=" + dbId +
                ", name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
