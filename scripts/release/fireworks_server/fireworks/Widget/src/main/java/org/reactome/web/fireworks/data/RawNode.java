package org.reactome.web.fireworks.data;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface RawNode {
    Long getDbId();
    String getStId();
    String getName();
    Double getRatio();
    Double getAngle();
    Double getX();
    Double getY();
}
