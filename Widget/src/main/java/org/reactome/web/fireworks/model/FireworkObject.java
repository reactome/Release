package org.reactome.web.fireworks.model;

import org.reactome.web.fireworks.interfaces.Movable;
import org.reactome.web.fireworks.interfaces.Zoomable;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class FireworkObject implements Movable, Zoomable {

    public abstract Long getDbId();

    private String getSchemaClass(){
        String[] path = this.getClass().getName().split("\\.");
        return path[path.length-1];
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + this.getDbId() +
                ", schemaClass=" + this.getSchemaClass() +
                '}';
    }
}
