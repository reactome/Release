package org.reactome.server.analysis.service.model;

import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
//@ApiModel(value = "EntityStatistics", description = "Statistics for an entity type")
public class EntityStatistics extends Statistics {
    private Double pValue;
    private Double fdr;
    private List<Double> exp = null;

    public EntityStatistics(String resource, Integer total, Integer found, Double ratio, Double pValue, Double fdr, List<Double> exp) {
        this(resource, total, found, ratio, pValue, fdr);
        this.exp = exp;
    }
    public EntityStatistics(String resource, Integer total, Integer found, Double ratio, Double pValue, Double fdr) {
        super(resource, total, found, ratio);
        this.pValue = pValue;
        this.fdr = fdr;
    }

    public Double getpValue() {
        return pValue;
    }

    public Double getFdr() {
        return fdr;
    }

    public List<Double> getExp() {
        return exp;
    }
}
