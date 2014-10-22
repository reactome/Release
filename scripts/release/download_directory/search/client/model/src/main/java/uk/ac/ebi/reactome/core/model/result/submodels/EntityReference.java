package uk.ac.ebi.reactome.core.model.result.submodels;

import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;

import java.util.Comparator;

/**
 * Internal Model for Reactome Entries
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class EntityReference implements Comparator<EnrichedEntry> {
    private String name;
    private Long dbId;
    private String species;
    private String compartment;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getCompartment() {
        return compartment;
    }

    public void setCompartment(String compartment) {
        this.compartment = compartment;
    }


    @Override
    public int compare(EnrichedEntry o1, EnrichedEntry o2) {
        if (o1!= null && o2!=null) {
            if (o1.getName()!= null && o1.getName()!=null) {
                return o1.getName().compareTo(o2.getName());
            }
        }
        return 0;
    }
}
