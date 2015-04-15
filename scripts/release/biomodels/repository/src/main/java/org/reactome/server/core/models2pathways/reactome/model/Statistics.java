package org.reactome.server.core.models2pathways.reactome.model;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class Statistics {
    private String resource;
    private Integer total;
    private Integer found;
    private Double ratio;

    public Statistics(String resource, Integer total, Integer found, Double ratio) {
        this.resource = resource;
        this.total = total;
        this.found = found;
        this.ratio = ratio;
    }

    public String getResource() {
        return resource;
    }

    public Integer getTotal() {
        return total;
    }

    public Integer getFound() {
        return found;
    }

    public Double getRatio() {
        return ratio;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "resource='" + resource + '\'' +
                ", total=" + total +
                ", found=" + found +
                ", ratio=" + ratio +
                '}';
    }
}