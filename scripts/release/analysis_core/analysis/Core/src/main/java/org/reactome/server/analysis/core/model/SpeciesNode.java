package org.reactome.server.analysis.core.model;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SpeciesNode {
    private Long speciesID;
    private String name;

    protected SpeciesNode(Long speciesID, String name) {
        this.speciesID = speciesID;
        this.name = name;
    }

    public Long getSpeciesID() {
        return speciesID;
    }

    public String getName() {
        return name;
    }

    public boolean isHuman(){
        return this.speciesID.equals(SpeciesNodeFactory.HUMAN_DB_ID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpeciesNode that = (SpeciesNode) o;

        if (speciesID != null ? !speciesID.equals(that.speciesID) : that.speciesID != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return speciesID != null ? speciesID.hashCode() : 0;
    }
}
