package org.reactome.web.fireworks.model;

import com.google.gwt.canvas.dom.client.Context2d;
import org.reactome.web.fireworks.analysis.EntityStatistics;
import org.reactome.web.fireworks.analysis.SpeciesFilteredResult;
import org.reactome.web.fireworks.data.RawNode;
import org.reactome.web.fireworks.interfaces.Drawable;
import org.reactome.web.fireworks.profiles.FireworksColours;
import org.reactome.web.fireworks.util.Coordinate;
import uk.ac.ebi.pwp.structures.quadtree.interfaces.QuadTreeBox;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@SuppressWarnings("UnusedDeclaration")
public class Node extends FireworkObject implements Drawable, QuadTreeBox {

    private Long dbId;
    private String stId;

    private String name;
    private double ratio;
    private EntityStatistics statistics;

    private double angle;
    private double currentSize;
    double originalSize;

    String colour;
    List<String> expColours;

    private Coordinate currentPosition;
    Coordinate originalPosition;

    private Set<Edge> edgesFrom = new HashSet<Edge>();
    private Set<Edge> edgesTo = new HashSet<Edge>();

    private Set<Node> children = new HashSet<Node>();
    private Set<Node> parents = new HashSet<Node>();

    public Node(RawNode raw){
        this.dbId = raw.getDbId();
        this.stId = raw.getStId();
        this.name = raw.getName();
        this.ratio = raw.getRatio();
        this.angle = raw.getAngle();
        this.currentSize = this.originalSize = (raw.getRatio() + 0.01) * 15;
        this.currentPosition = this.originalPosition = new Coordinate(raw.getX(), raw.getY());
        initStatistics(); //Colour is set in initStatistics method
    }

    public Edge addChild(Node node){
        if(node==this) return null;
        this.children.add(node);
        node.parents.add(this);
        Edge edge = new Edge(this, node);
        this.edgesFrom.add(edge);
        node.edgesTo.add(edge);
        return edge;
    }

    public boolean isTopLevel(){
        return this.parents.isEmpty();
    }

    @Override
    public void draw(Context2d ctx) {
        ctx.beginPath();
        ctx.arc(this.getX(), this.getY(), this.getSize(), 0, 2 * Math.PI, true);
        ctx.closePath();
        ctx.fill(); ctx.stroke();
    }

    @Override
    public void drawText(Context2d ctx, double fontSize, double space, boolean selected) {
        if(!this.isTopLevel()) {
            ctx.save(); //Needed before the calculations b
            if(selected) ctx.setFont("bold " + ctx.getFont());

            double x = currentPosition.getX();
            double y = currentPosition.getY();
            double angle = this.angle;
            space += getSize();
            if(this.angle > (Math.PI/2) && this.angle < (Math.PI * 3 / 2) ) {
                double width = ctx.measureText(name).getWidth() + space;
                x = x + width * Math.cos(angle);
                y = y + width * Math.sin(angle);
                angle += Math.PI;
            }else{
                x = x + space * Math.cos(angle);
                y = y + space * Math.sin(angle);
            }

            ctx.translate(x, y);
            ctx.rotate(angle);
            ctx.fillText(name, 0, 0);
            ctx.restore();
        }else {  //Text for top level pathways
            List<String> lines = this.getLines();
            double linesSeparation = fontSize * 0.75;
            double vSpace = lines.size() * (fontSize + linesSeparation);
            for (String line : lines) {
                double width = ctx.measureText(line).getWidth();
                double x = currentPosition.getX() - width/2;
                double y = currentPosition.getY() - getSize() - vSpace;
                ctx.fillText(line, x, y);
                vSpace -= (fontSize + linesSeparation);
            }
        }
    }

    @Override
    public void drawThumbnail(Context2d ctx, double factor) {
        ctx.beginPath();
        double x = originalPosition.getX() * factor;
        double y = originalPosition.getY() * factor;
        double size = originalSize * factor;
        ctx.arc(x, y, size, 0, 2 * Math.PI, true);
        ctx.closePath();
        ctx.fill();ctx.stroke();
    }

    public Set<Node> getTopLevelPathways(){
        Set<Node> rtn = new HashSet<Node>();
        if(this.isTopLevel()) rtn.add(this);
        for (Node parent : this.parents) {
            rtn.addAll(parent.getTopLevelPathways());
        }
        return rtn;
    }

    @Override
    public void highlight(Context2d ctx, double auraSize) {
        ctx.beginPath();
        ctx.arc(this.getX(), this.getY(), this.getSize() + auraSize, 0, 2 * Math.PI, true);
        ctx.closePath();
        ctx.fill();ctx.stroke();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        //noinspection RedundantIfStatement
        if (dbId != null ? !dbId.equals(node.dbId) : node.dbId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dbId != null ? dbId.hashCode() : 0;
    }

    public Set<Node> getAncestors(){
        Set<Node> rtn = new HashSet<Node>();
        for (Node parent : this.parents) {
            rtn.add(parent);
            rtn.addAll(parent.getAncestors());
        }
        return rtn;
    }

    public Set<Node> getChildren() {
        return children;
    }

    /**
     * Can be used either for normal visualisation, overrepresentation analysis or species comparison.
     *
     * @return the color associated with this node for normal visualisation, overrepresentation
     *         analysis or species comparison.
     */
    public String getColour() {
        return this.colour;
    }

    @Override
    public Long getDbId() {
        return dbId;
    }

    public Set<Edge> getEdgesFrom() {
        return edgesFrom;
    }

    public Set<Edge> getEdgesTo() {
        return edgesTo;
    }

    public String getName() {
        return name;
    }

    public Set<Node> getParents() {
        return parents;
    }

    public double getRatio() {
        return ratio;
    }

    public EntityStatistics getStatistics() {
        return statistics;
    }

    public double getSize() {
        return currentSize;
    }

    public String getStId() {
        return stId;
    }

    public double getX() {
        return this.currentPosition.getX();
    }

    public double getY() {
        return this.currentPosition.getY();
    }

    public Coordinate getOriginalPosition() {
        return originalPosition;
    }

    public Coordinate getCurrentPosition() {
        return currentPosition;
    }

    public String getExpressionColor(int column){
        if(this.expColours!=null){
            if( column >= 0 && column < this.expColours.size()) {
                return this.expColours.get(column);
            }
        }
        return FireworksColours.PROFILE.getNodeFadeoutColour();
    }

    public void initStatistics(){
        this.statistics = null;
        this.expColours = null;
        this.colour = FireworksColours.PROFILE.getNodeInitialColour();
        for (Edge edge : this.edgesTo) {
            edge.setColour(FireworksColours.PROFILE.getEdgeInitialColour());
            edge.setExpColours(null);
        }
    }

    public void setAnalysisResultData(SpeciesFilteredResult result, EntityStatistics statistics) {
        this.statistics = statistics;
        switch (result.getAnalysisType()){
            case SPECIES_COMPARISON:
            case OVERREPRESENTATION:
                this.colour = FireworksColours.PROFILE.getNodeEnrichmentColour(statistics.getpValue());
                String edgeColour = FireworksColours.PROFILE.getEdgeEnrichmentColour(statistics.getpValue());
                for (Edge edge : this.edgesTo) {
                    edge.setColour(edgeColour);
                }
                break;
            case EXPRESSION:
                List<Double> exp = this.statistics.getExp();
                if(exp!=null){
                    double min = result.getExpressionSummary().getMin();
                    double max = result.getExpressionSummary().getMax();
                    this.expColours = new ArrayList<String>();
                    List<String> edgeExpColours = new ArrayList<String>();
                    for (Double v : exp) {
                        this.expColours.add(FireworksColours.PROFILE.getNodeExpressionColour(statistics.getpValue(), v, min, max));
                        edgeExpColours.add(FireworksColours.PROFILE.getEdgeExpressionColour(statistics.getpValue(), v, min, max));
                    }
                    for (Edge edge : this.edgesTo) {
                        edge.setExpColours(edgeExpColours);
                    }
                }
                break;
            case NONE:
            default:
                //Nothing here
        }
    }

    public void setFadeoutColour(){
        this.colour = FireworksColours.PROFILE.getNodeFadeoutColour();
    }

    // ####################################
    //  QuadTreeBox methods implementation
    // ####################################

    @Override
    public double getMinX() {
        return originalPosition.getX() - originalSize;
    }

    @Override
    public double getMinY() {
        return originalPosition.getY() - originalSize;
    }

    @Override
    public double getMaxX() {
        return originalPosition.getX() + originalSize;
    }

    @Override
    public double getMaxY() {
        return originalPosition.getY() + originalSize;
    }

    // ####################################################################
    //  Translatable method implementation (inherited from FireworkObject)
    // ####################################################################

    @Override
    public void move(Coordinate delta) {
        this.currentPosition = this.currentPosition.add(delta);
    }

    // ###############################################################
    // Zoomable method implementation (inherited from FireworkObject)
    // ###############################################################

    @Override
    public void zoom(double factor, Coordinate delta){
        this.currentSize = this.originalSize * factor;
        this.currentPosition = this.originalPosition.multiply(factor).minus(delta);
    }

    @Override
    public String toString() {
        return "Node{" +
                "stId='" + stId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    private List<String> getLines(){
        List<String> rtn = new LinkedList<String>();
        if(name.length()<=15){ //Using name length behaves better than counting words
            rtn.add(this.name);
            return rtn;
        }
        String[] words = name.split("  *");
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < words.length/2 ; i++) {
            line.append(words[i]).append(" ");
        }
        rtn.add(line.toString().trim());
        line = new StringBuilder();
        for (int i = words.length/2; i < words.length; i++) {
            line.append(words[i]).append(" ");
        }
        rtn.add(line.toString().trim());
        return rtn;
    }
}
