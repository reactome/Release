package org.reactome.server.analysis.service.model;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ResourceSummary implements Comparable<ResourceSummary> {
    String resource;
    Integer pathways;

    public ResourceSummary(String resource, Integer pathways) {
        this.resource = resource;
        this.pathways = pathways;
    }

    public String getResource() {
        return resource;
    }

    public Integer getPathways() {
        return pathways;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceSummary that = (ResourceSummary) o;

        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return resource != null ? resource.hashCode() : 0;
    }

    @Override
    public int compareTo(ResourceSummary o) {
        return pathways.compareTo(o.pathways);
    }
}
