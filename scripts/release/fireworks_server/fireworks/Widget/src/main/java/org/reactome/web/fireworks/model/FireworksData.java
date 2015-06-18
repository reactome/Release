package org.reactome.web.fireworks.model;

import org.reactome.web.fireworks.analysis.PathwayBase;
import org.reactome.web.fireworks.analysis.SpeciesFilteredResult;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksData {

    private Graph graph;
    private Map<Long, Node> id2Node;
    private Map<String, Node> stId2Node;

    private SpeciesFilteredResult analysisResult;

    public FireworksData(Graph graph) {
        this.graph = graph;
        this.id2Node = new HashMap<Long, Node>();
        this.stId2Node = new HashMap<String, Node>();
        for (Node node : graph.getNodes()) {
            this.id2Node.put(node.getDbId(), node);
            this.stId2Node.put(node.getStId(), node);
        }
    }

    public Node getNode(Long identifier){
        if(identifier==null) return null;
        return this.id2Node.get(identifier);
    }

    public Node getNode(String identifier){
        if(identifier==null) return null;
        return this.stId2Node.get(identifier);
    }

    public Long getSpeciesId(){
        return this.graph.getSpeciesId();
    }

    public void resetPathwaysAnalysisResult(){
        this.analysisResult = null;
        for (Node node : this.graph.getNodes()) {
            node.initStatistics();
        }
    }

    public void setPathwaysAnalysisResult(SpeciesFilteredResult result) {
        this.analysisResult = result;

        for (Node node : this.graph.getNodes()) {
            node.setFadeoutColour();
        }
        for (Edge edge : this.graph.edges) {
            edge.setFadeoutColour();
        }
        for (PathwayBase pathway : result.getPathways()) {
            Node node = id2Node.get(pathway.getDbId());
            if(node!=null){
                node.setAnalysisResultData(result, pathway.getEntities());
            }
        }
    }

    public void updateColours(){
        if(this.analysisResult!=null){
            setPathwaysAnalysisResult(this.analysisResult);
        }
    }
}
