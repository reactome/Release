package org.reactome.server.core.models2pathways.reactome.model;


import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class AnalysisResult {
    private AnalysisSummary summary;
    private ExpressionSummary expression;
    private Integer identifiersNotFound;
    private Integer pathwaysFound;
    private List<PathwaySummary> pathways;
    private List<PathwaySummary> reliablePathways;
    private List<ResourceSummary> resourceSummary;

    public AnalysisResult(AnalysisStoredResult storedResult, List<PathwaySummary> pathways) {
        this.summary = storedResult.getSummary();
        this.pathways = pathways;
        this.identifiersNotFound = storedResult.getNotFound().size();
        this.pathwaysFound = storedResult.getPathways().size();
        this.expression = storedResult.getExpressionSummary();
        this.resourceSummary = storedResult.getResourceSummary();
    }

    public AnalysisSummary getSummary() {
        return summary;
    }

    public Integer getPathwaysFound() {
        return pathwaysFound;
    }

    public Integer getIdentifiersNotFound() {
        return identifiersNotFound;
    }

    public List<PathwaySummary> getPathways() {
        return pathways;
    }

    public List<ResourceSummary> getResourceSummary() {
        return resourceSummary;
    }

    public ExpressionSummary getExpression() {
        return expression;
    }

    public List<PathwaySummary> getReliablePathways() {
        return reliablePathways;
    }

    public void setReliablePathways(List<PathwaySummary> reliablePathways) {
        this.reliablePathways = reliablePathways;
    }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "summary=" + summary +
                ", expression=" + expression +
                ", identifiersNotFound=" + identifiersNotFound +
                ", pathwaysFound=" + pathwaysFound +
                ", pathways=" + pathways +
                ", reliablePathways=" + reliablePathways +
                ", resourceSummary=" + resourceSummary +
                '}';
    }
}