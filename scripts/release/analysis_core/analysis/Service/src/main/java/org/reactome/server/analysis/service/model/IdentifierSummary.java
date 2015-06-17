package org.reactome.server.analysis.service.model;

import org.reactome.server.analysis.core.model.AnalysisIdentifier;

import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class IdentifierSummary {
    private String id;
    private List<Double> exp;

    public IdentifierSummary(AnalysisIdentifier identifier) {
        this.id = identifier.getId();
        this.exp = identifier.getExp();
    }

    public String getId() {
        return id;
    }

    public List<Double> getExp() {
        return exp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentifierSummary that = (IdentifierSummary) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
