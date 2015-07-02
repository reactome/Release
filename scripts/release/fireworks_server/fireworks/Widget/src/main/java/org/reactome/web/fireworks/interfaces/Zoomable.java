package org.reactome.web.fireworks.interfaces;

import org.reactome.web.fireworks.util.Coordinate;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface Zoomable {

    public void zoom(double factor, Coordinate delta);

}
