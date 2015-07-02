package org.reactome.web.fireworks.util;

import com.google.gwt.animation.client.Animation;

/**
 * First centers the node and then apply the zoom until it fits the visible area
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FocusInAnimation extends Animation {
    /**
     * The maximum duration of the animation.
     */
    private static final int MAX_ANIMATION_DURATION = 2000;

    private FocusAnimationHandler handler;

    private Coordinate currentCanvasPoint;
    private double currentFactor;

    private Coordinate targetCanvasPoint;
    private double targetFactor;

    private boolean isZooming = false;

    public FocusInAnimation(FocusAnimationHandler handler, Coordinate canvasPoint, double factor) {
        this.handler = handler;
        this.currentCanvasPoint = canvasPoint;
        this.currentFactor = factor;
    }

    public void moveTo(Coordinate targetCanvasPoint, double factor){
        int time = time(distance(this.currentCanvasPoint, targetCanvasPoint));
        this.moveTo(targetCanvasPoint, factor, time);
    }

    public void moveTo(Coordinate targetCanvasPoint, double factor, int time){
        this.targetCanvasPoint = targetCanvasPoint;
        this.targetFactor = factor;

        if(time > 0) { //If no need to move time is zero because distance is zero :)
            run(time); //DO NOT RUN THIS WHEN TIME IS ZERO
        }
    }

    @Override
    protected void onComplete() {
        if(!isZooming) {
            move(1.0);
            this.isZooming = true;
            run(1000);
        }else{
            super.onComplete(); //By avoiding the call to "super" if cancelled, a composition of movement is created
            zoom(1.0);
            this.handler.focusFinished(null);
        }
    }

    @Override
    protected void onUpdate(double progress) {
        if(!this.isZooming) {
            move(progress);
        }else{
            zoom(progress);
        }
    }

    protected void move(double progress){
        Coordinate delta = this.targetCanvasPoint.minus(this.currentCanvasPoint).multiply(progress);
        this.currentCanvasPoint = this.currentCanvasPoint.add(delta);
        handler.translate(delta.getX(), delta.getY());
    }

    protected void zoom(double progress){
        double deltaFactor = this.targetFactor - this.currentFactor;
        handler.setZoom(this.currentFactor + deltaFactor * progress, targetCanvasPoint);
    }

    private int time(double distance){
        int d = (int) Math.ceil(distance) * 10;
        if(d == 0) d = MAX_ANIMATION_DURATION / 2;
        return d > MAX_ANIMATION_DURATION ? MAX_ANIMATION_DURATION : d;
    }

    private double distance(Coordinate a, Coordinate b){
        double dX = b.getX() - a.getX();
        double dY = b.getY() - a.getY();
        return Math.sqrt(dX*dX + dY*dY);
    }
}
