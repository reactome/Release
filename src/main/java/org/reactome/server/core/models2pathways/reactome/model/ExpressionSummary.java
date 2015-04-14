package org.reactome.server.core.models2pathways.reactome.model;

import org.reactome.server.analysis.core.model.UserData;

import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ExpressionSummary {
    List<String> columnNames;
    Double min;
    Double max;

    public ExpressionSummary(UserData storedResult) {
        this.columnNames = storedResult.getExpressionColumnNames();
        this.min = storedResult.getExpressionBoundaries().getMin();
        this.max = storedResult.getExpressionBoundaries().getMax();
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "ExpressionSummary{" +
                "columnNames=" + columnNames +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}