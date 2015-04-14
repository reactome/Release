package org.reactome.server.models2pathways.biomodels.model;

import org.reactome.server.models2pathways.core.model.Namespace;

/**
 * Stores one annotation (without any qualifier)
 *
 * @author Camille Laibe
 * @version 20140703
 */
public class Annotation {
    private Namespace namespace;   // data collection namespace
    private String entityId;
    private String uri;   // full URI, as extracted from the sbml file

    public Annotation(Namespace namespace, String entityId, String uri) {
        this.namespace = namespace;
        this.entityId = entityId;
        this.uri = uri;
    }

    public Annotation(Namespace namespace, String entityId) {
        this.namespace = namespace;
        this.entityId = entityId;
    }


    public Namespace getNamespace() {
        return namespace;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Annotation that = (Annotation) o;

        return !(entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) && !(namespace != null ? !namespace.equals(that.namespace) : that.namespace != null);

    }

    @Override
    public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                "namespace='" + namespace + '\'' +
                ", entityId='" + entityId + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}
