package org.reactome.web.fireworks.analysis;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface PathwayBase {
    String getStId();

    Long getDbId();

    EntityStatistics getEntities();

}
