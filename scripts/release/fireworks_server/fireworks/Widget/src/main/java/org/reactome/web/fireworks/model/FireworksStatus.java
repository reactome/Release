package org.reactome.web.fireworks.model;

import org.reactome.web.fireworks.util.Coordinate;
import uk.ac.ebi.pwp.structures.quadtree.model.Box;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksStatus {
    /**
     * Related to zoom level
     */
    private double factor;

    /**
     *
     */
    private Coordinate offset;

    public FireworksStatus(double factor, Coordinate offset) {
        this.factor = factor;
        this.offset = offset;
    }

    /**
     * Calculates the distance from the current offset to the target
     *
     * @param target coordinate related to the model
     * @return
     */
    public Coordinate getDistance(Coordinate target){
        return offset.minus(target).multiply(factor);
    }

    public double getFactor() {
        return factor;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Coordinate getCanvasCoordinate(Coordinate coordinate){
        return coordinate.multiply(this.factor).minus(this.offset);
    }

    /**
     *
     * @param coordinate related to the position in the canvas
     * @return
     */
    public Coordinate getModelCoordinate(Coordinate coordinate){
        return offset.add(coordinate.divide(factor));
    }

    public Coordinate getOffset() {
        return offset;
    }

    public Box getVisibleModelArea(double width, double height){
        double x = this.offset.getX();
        double y = this.offset.getY();
        double w = x + width / factor;
        double h = y + height / factor;
        return new Box(x, y, w, h);
    }

    /**
     *
     * @param delta related the model coordinate
     * @return
     */
    public Coordinate translate(Coordinate delta){
        return this.offset = this.offset.minus(delta.divide(this.factor));
    }

    @Override
    public String toString() {
        return "FireworksStatus{" +
                "factor=" + factor +
                ", offset=" + offset +
                '}';
    }
}
