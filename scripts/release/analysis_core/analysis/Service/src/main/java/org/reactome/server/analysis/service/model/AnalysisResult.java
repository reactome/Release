package org.reactome.server.analysis.service.model;


import org.reactome.server.analysis.service.result.AnalysisStoredResult;

import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
//@ApiModel(value = "AnalysisResult", description = "Contains general information about the result plus a list with the found pathways", discriminator = "", subTypes = {PathwaySummary.class})
public class AnalysisResult {
//    @ApiModelProperty(value = "Token associated with the query", notes = "Is a good practise to use it with the /token method for future filters of the result", required = true)

    private AnalysisSummary summary;

    private ExpressionSummary expression;

//    @ApiModelProperty(value = "Number of identifiers in the sample without entities associated in the Reactome database", required = true)
    private Integer identifiersNotFound;

    //TODO: Add found identifiers number

//    @ApiModelProperty(value = "Number of pathways found with hits for the given sample", required = true)
    private Integer pathwaysFound;

//    @ApiModelProperty(value = "List of pathways in which the input has been found", required = true) // dataType = "PathwaySummary" ) //, notes = "It may contain a subset of the pathways depending of the filtering options" )
    private List<PathwaySummary> pathways;

    private List<ResourceSummary> resourceSummary;

    public AnalysisResult(AnalysisStoredResult storedResult, List<PathwaySummary> pathways){
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
}
