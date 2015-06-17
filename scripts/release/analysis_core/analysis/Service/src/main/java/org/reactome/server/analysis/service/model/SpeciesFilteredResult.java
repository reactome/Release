package org.reactome.server.analysis.service.model;

import java.util.List;

/**
 * This class is a way to wrap the analysis type into the response for the result filtered by species.
 * It is needed for the Fireworks project when working as stand-alone project completely independent
 * of the PathwayBrowser. By having this property in the result we avoid a previous query to retrieve
 * the type of analysis.
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class SpeciesFilteredResult {

    private String type;
    private ExpressionSummary expressionSummary;
    private List<PathwayBase> pathways;

    public SpeciesFilteredResult(String type, ExpressionSummary expressionSummary, List<PathwayBase> pathways) {
        this.type = type;
        this.expressionSummary = expressionSummary;
        this.pathways = pathways;
    }

    public String getType() {
        return type;
    }

    public ExpressionSummary getExpressionSummary() {
        return expressionSummary;
    }

    public List<PathwayBase> getPathways() {
        return pathways;
    }
}
