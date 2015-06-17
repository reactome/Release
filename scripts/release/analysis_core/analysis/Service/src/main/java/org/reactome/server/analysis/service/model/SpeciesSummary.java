package org.reactome.server.analysis.service.model;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SpeciesSummary {
    private Long dbId;
    private String name;

    public SpeciesSummary(Long dbId, String name) {
        this.dbId = dbId;
        this.name = name;
    }

    public Long getDbId() {
        return dbId;
    }

    public String getName() {
        return name;
    }
}
