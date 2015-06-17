package org.reactome.server.analysis.core.model;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class AnalysisReaction {
    private Long dbId;
    private String stId;

    public AnalysisReaction(Long dbId, String stId) {
        this.dbId = dbId;
        this.stId = stId;
    }

    public Long getDbId() {
        return dbId;
    }

    public String getStId() {
        return stId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalysisReaction reaction = (AnalysisReaction) o;

        if (dbId != null ? !dbId.equals(reaction.dbId) : reaction.dbId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dbId != null ? dbId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return stId!=null && !stId.isEmpty() ? stId : dbId.toString();
    }
}
