package org.reactome.server.analysis.core.model;

import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ExpressionBoundaries {
    Double min;
    Double max;

    public ExpressionBoundaries(Set<AnalysisIdentifier> identifiers){
        for (AnalysisIdentifier identifier : identifiers) {
            for (Double exp : identifier.getExp()) {
                if( exp == null ) continue;
                if( min == null && max == null ){
                    min = exp;
                    max = exp;
                    continue;
                }
                if( exp < min ){
                    min = exp;
                }else if( exp > max ){
                    max = exp;
                }
            }
        }
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }
}
