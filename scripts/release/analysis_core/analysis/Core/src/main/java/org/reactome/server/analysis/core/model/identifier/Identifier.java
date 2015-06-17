package org.reactome.server.analysis.core.model.identifier;

import org.reactome.server.analysis.core.model.AnalysisIdentifier;
import org.reactome.server.analysis.core.model.resource.Resource;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class Identifier<R extends Resource> {
    R resource;
    AnalysisIdentifier value;

    Identifier(R resource, AnalysisIdentifier value) {
        this.resource = resource;
        this.value = value;
    }

    public R getResource() {
        return resource;
    }

    public AnalysisIdentifier getValue() {
        return value;
    }

    public boolean is(String name){
        return resource.is(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Identifier that = (Identifier) o;

        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        //noinspection RedundantIfStatement
        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = resource != null ? resource.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
