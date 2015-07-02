package org.reactome.web.fireworks.profiles.model;

import java.io.Serializable;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface ProfileGradient extends Serializable {

    public String getMin();
    public String getStop();
    public String getMax();
}
