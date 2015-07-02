package org.reactome.web.fireworks.util;

import org.reactome.web.fireworks.model.Node;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface FocusAnimationHandler {

    public void focusFinished(Node node);

    public void translate(double dX, double dY);

    public void setZoom(double factor, Coordinate point);

}
