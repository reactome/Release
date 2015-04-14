package org.reactome.server.models2pathways.core.helper;

import org.reactome.server.models2pathways.core.model.Specie;

import java.util.Objects;
import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class SpeciesHelper {

    private static SpeciesHelper speciesHelper = new SpeciesHelper();
    private Set<Specie> species;

    private SpeciesHelper() {
    }

    public static SpeciesHelper getInstance() {
        return speciesHelper;
    }

    /**
     * 
     * @param id BioModel Species Id
     * @return Species Object
     */
    public Specie getSpecieByBioMdSpecieId(Long id) {
        for (Specie specie : species) {
            if (Objects.equals(specie.getBioMdId(), id)) {
                return specie;
            }
        }
        return null;
    }

    public Set<Specie> getSpecies() {
        return species;
    }

    public void setSpecies(Set<Specie> species) {
        this.species = species;
    }

    @Override
    public String toString() {
        return "SpeciesHelper{" +
                "species=" + species +
                '}';
    }
}
