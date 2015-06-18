package org.reactome.web.fireworks.profiles.model;

import java.io.Serializable;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface ProfileColour extends Serializable {

    public String getInitial();
    public String getHighlight();
    public String getSelection();
    public String getFadeout();
    public String getHit();

    public ProfileGradient getEnrichment();
    public ProfileGradient getExpression();
}
