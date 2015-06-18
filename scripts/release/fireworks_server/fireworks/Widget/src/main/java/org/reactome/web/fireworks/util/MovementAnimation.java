package org.reactome.web.fireworks.util;

import com.google.gwt.animation.client.Animation;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class MovementAnimation extends Animation {

    public interface FireworksZoomAnimationHandler {
        public void zoomToCoordinate(Coordinate model, Coordinate canvas, double factor);
    }

    /**
     * The maximum duration of the animation.
     */
    private static final int MAX_ANIMATION_DURATION = 2000;

    private FireworksZoomAnimationHandler handler;

    private Coordinate currentModelPoint;
    private Coordinate currentCanvasPoint;
    private double currentFactor;

    private Coordinate targetModelPoint;
    private Coordinate targetCanvasPoint;
    private double targetFactor;

    private boolean canceled;

    public MovementAnimation(FireworksZoomAnimationHandler handler, Coordinate modelPoint, Coordinate canvasPoint, double factor) {
        this.handler = handler;
        this.currentModelPoint = modelPoint;
        this.currentCanvasPoint = canvasPoint;
        this.currentFactor = factor;
        this.canceled = false;
    }

    public void moveTo(Coordinate modelPoint, Coordinate canvasPoint, double factor){
        int time = time(distance(this.currentModelPoint, modelPoint));
        this.moveTo(modelPoint, canvasPoint, factor, time);
    }

    public void moveTo(Coordinate modelPoint, Coordinate canvasPoint, double factor, int time){
        this.canceled = false;
        this.targetModelPoint = modelPoint;
        this.targetCanvasPoint = canvasPoint;
        this.targetFactor = factor;
        if(time > 0) { //If no need to move time is zero because distance is zero :)
            run(time); //DO NOT RUN THIS WHEN TIME IS ZERO
        }
    }

    @Override
    protected void onCancel() {
        this.canceled = true;
        super.onCancel();
    }

    @Override
    protected void onComplete() {
        if(!canceled){
            super.onComplete(); //By avoiding the call to "super" if cancelled, a composition of movement is created
        }
    }

    @Override
    protected void onUpdate(double progress) {
        double factor = currentFactor + ((targetFactor - currentFactor) * progress );
        Coordinate m = currentModelPoint.add(targetModelPoint.minus(currentModelPoint).multiply(progress));
        Coordinate c = currentCanvasPoint.add(targetCanvasPoint.minus(currentCanvasPoint).multiply(progress));
        handler.zoomToCoordinate(m, c, factor);
    }

    private int time(double distance){
        int d = (int) Math.ceil(distance) * 10;
        return d > MAX_ANIMATION_DURATION ? MAX_ANIMATION_DURATION : d;
    }

    private double distance(Coordinate a, Coordinate b){
        double dX = b.getX() - a.getX();
        double dY = b.getY() - a.getY();
        return Math.sqrt(dX*dX + dY*dY);
    }
}
