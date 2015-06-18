package org.reactome.web.fireworks.interfaces;

import org.reactome.web.fireworks.util.Coordinate;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface Movable {

    public void move(Coordinate delta);

//    public void setTranslation(Coordinate delta, double factor);

}
