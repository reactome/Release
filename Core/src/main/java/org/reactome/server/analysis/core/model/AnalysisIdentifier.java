package org.reactome.server.analysis.core.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class AnalysisIdentifier implements Comparable<AnalysisIdentifier> {

    private String id;
    private List<Double> exp;

    public AnalysisIdentifier(AnalysisIdentifier aux){
        this(aux.getId());
        for (Double ev : aux.getExp()) {
            this.add(ev);
        }
    }

    public AnalysisIdentifier(String id) {
        this.id = id;
        this.exp = new LinkedList<Double>();
    }

    public AnalysisIdentifier(String id, List<Double> exp) {
        this.id = id;
        this.exp = exp;
    }

    public boolean add(Double value){
        return this.exp.add(value);
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

        AnalysisIdentifier that = (AnalysisIdentifier) o;

        //noinspection RedundantIfStatement
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return this.id;
    }

    @Override
    public int compareTo(AnalysisIdentifier o) {
        return this.id.compareTo(o.id);
    }
}