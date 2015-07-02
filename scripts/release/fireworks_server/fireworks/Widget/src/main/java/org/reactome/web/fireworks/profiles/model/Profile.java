package org.reactome.web.fireworks.profiles.model;

import java.io.Serializable;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface Profile extends Serializable {

    public String getName();
    public ProfileColour getNode();
    public ProfileColour getEdge();
    public ProfileColour getThumbnail();

}
