package org.reactome.server.core.models2pathways.reactome.model;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
//@ApiModel(value = "ReactionStatistics", description = "Statistics for a reaction type")
public class ReactionStatistics extends Statistics {

    public ReactionStatistics(String resource, Integer total, Integer found, Double ratio) {
        super(resource, total, found, ratio);
    }
}
