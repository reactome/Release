package org.reactome.server.models2pathways.biomodels.model;

import org.reactome.server.models2pathways.core.model.Specie;

import java.util.Set;

/**
 * @author Maximilian Koch <mkoch@ebi.ac.uk>
 */
public class BioModel {
    private String name;
    private String bioMdId;
    private Specie specie;
    private Set<Annotation> annotations;

    /**
     * SBMLModel represents given information from the given
     * SBMLModel/XML and the BioModel-Webservice
     *
     * @param name Name of BioModel
     * @param bioMdId BioModel Id
     * @param specie Specie of BioModel
     * @param annotations Set of annotations of BioModel
     */
    public BioModel(String name, String bioMdId, Specie specie, Set<Annotation> annotations) {
        this.name = name;
        this.bioMdId = bioMdId;
        this.specie = specie;
        this.annotations = annotations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBioMdId() {
        return bioMdId;
    }

    public Specie getSpecie() {
        return specie;
    }

    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return "BioModel{" +
                "name='" + name + '\'' +
                ", bioMdId='" + bioMdId + '\'' +
                ", specie=" + specie +
                ", annotations=" + annotations +
                '}';
    }
}
