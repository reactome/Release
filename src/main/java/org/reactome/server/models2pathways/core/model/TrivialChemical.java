package org.reactome.server.models2pathways.core.model;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class TrivialChemical {
    private Long id;
    private String name;

    public TrivialChemical(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TrivialMolecules{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
