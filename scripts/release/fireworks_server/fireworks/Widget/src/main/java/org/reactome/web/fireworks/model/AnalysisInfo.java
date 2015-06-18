package org.reactome.web.fireworks.model;

import org.reactome.web.fireworks.analysis.AnalysisType;
import org.reactome.web.fireworks.analysis.ExpressionSummary;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class AnalysisInfo {

    private AnalysisType analysisType;
    private ExpressionSummary expressionSummary;
    private int column;

    public AnalysisInfo() {
        reset();
    }

    public AnalysisType getType() {
        return analysisType;
    }

    public ExpressionSummary getExpressionSummary() {
        return expressionSummary;
    }

    public boolean is(AnalysisType type){
        return this.analysisType.equals(type);
    }

    public void reset(){
        this.analysisType = AnalysisType.NONE;
        this.expressionSummary = null;
        this.column = 0;
    }

    public void setInfo(AnalysisType type, ExpressionSummary expressionSummary){
        this.analysisType = type;
        this.expressionSummary = expressionSummary;
        this.column = 0;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}
