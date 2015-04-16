package org.reactome.server.models2pathways.core.model;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Specie {
    private Long bioMdId;
    private Long reactId;
    private String name;

    public Specie(Long bioMdId, Long reactId, String name) {
        this.bioMdId = bioMdId;
        this.reactId = reactId;
        this.name = name;
    }

    public Long getBioMdId() {
        return bioMdId;
    }

    public Long getReactId() {
        return reactId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Species{" +
                "bioMdId=" + bioMdId +
                ", reactId=" + reactId +
                ", name='" + name + '\'' +
                '}';
    }
}
