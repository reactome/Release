package org.reactome.web.fireworks.util;

import com.google.gwt.animation.client.Animation;
import org.reactome.web.fireworks.model.Node;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FocusOutAnimation extends Animation {

    private FocusAnimationHandler handler;

    private Coordinate canvasPoint;
    private double currentFactor;

    private Node targetNode;
    private double targetFactor;

    public FocusOutAnimation(FocusAnimationHandler handler, Coordinate canvasPoint, double factor) {
        this.handler = handler;
        this.canvasPoint = canvasPoint;
        this.currentFactor = factor;
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        onUpdate(1.0);
        this.handler.focusFinished(this.targetNode);
    }

    @Override
    protected void onUpdate(double progress) {
        double deltaFactor = this.targetFactor - this.currentFactor;
        handler.setZoom(this.currentFactor + deltaFactor * progress, canvasPoint);
    }

    public void zoomOut(Node node, double factor){
        this.targetNode = node;
        this.targetFactor = factor;
        run(1500);
    }
}
