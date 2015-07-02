package org.reactome.server.analysis.service.model;

import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class IdentifierMap {
    private String resource;
    private Set<String> ids;

    public IdentifierMap(String resource, Set<String> ids) {
        this.resource = resource;
        this.ids = ids;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Set<String> getIds() {
        return ids;
    }

    protected boolean addAll(Set<String> maps){
        return this.ids.addAll(maps);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentifierMap that = (IdentifierMap) o;

        if (ids != null ? !ids.equals(that.ids) : that.ids != null) return false;
        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = resource != null ? resource.hashCode() : 0;
        result = 31 * result + (ids != null ? ids.hashCode() : 0);
        return result;
    }
}
