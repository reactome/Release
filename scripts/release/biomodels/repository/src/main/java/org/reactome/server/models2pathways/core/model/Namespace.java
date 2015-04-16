package org.reactome.server.models2pathways.core.model;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class Namespace {
    private String name;
    private boolean hasTrivialMolecules;

    public Namespace(String name, boolean hasTrivialMolecules) {
        this.name = name;
        this.hasTrivialMolecules = hasTrivialMolecules;
    }

    public String getName() {
        return name;
    }

    public boolean hasTrivialMolecules() {
        return hasTrivialMolecules;
    }

    @Override
    public String toString() {
        return "Namespaces{" +
                "name='" + name + '\'' +
                ", hasTrivialMolecules=" + hasTrivialMolecules +
                '}';
    }
}
