package org.reactome.web.fireworks.analysis;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface Statistics {

    String getResource();

    Integer getTotal();

    Integer getFound();

    Double getRatio();
}
