package org.reactome.server.analysis.core.model;

/**
 * This is a almost useless class only used to filtered the hit pathways per species
 * because the equals and hascode can NOT be implemented at the level of PathwayNode
 * object for obvious reasons (just in case is because a pathway can be in several
 * location and we want to take all of them into account while analysing)
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
class SpeciesPathway {

    Long speciesId;
    Long pathwayId;

    public SpeciesPathway(PathwayNode node) {
        this.speciesId = node.getSpecies().getSpeciesID();
        this.pathwayId = node.getPathwayId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpeciesPathway that = (SpeciesPathway) o;

        if (pathwayId != null ? !pathwayId.equals(that.pathwayId) : that.pathwayId != null) return false;
        if (speciesId != null ? !speciesId.equals(that.speciesId) : that.speciesId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = speciesId != null ? speciesId.hashCode() : 0;
        result = 31 * result + (pathwayId != null ? pathwayId.hashCode() : 0);
        return result;
    }
}
