package org.reactome.server.analysis.core.model;

import org.reactome.server.analysis.core.model.identifier.Identifier;
import org.reactome.server.analysis.core.model.identifier.MainIdentifier;
import org.reactome.server.analysis.core.model.resource.MainResource;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class PathwayNode implements Serializable, Comparable<PathwayNode> {
    private String stId;
    private Long pathwayId;
    private String name;
    private boolean hasDiagram;
    private boolean isLowerLevelPathway = false;

    private PathwayNode parent;
    private Set<PathwayNode> children;

    private PathwayNodeData data;

    public PathwayNode(String stId, Long pathwayId, String name, boolean hasDiagram) {
        this(null, stId, pathwayId, name, hasDiagram);
    }

    protected PathwayNode(PathwayNode parent, String stID, Long pathwayId, String name, boolean hasDiagram) {
        this.parent = parent;
        this.stId = stID;
        this.pathwayId = pathwayId;
        this.name = name;
        this.hasDiagram = hasDiagram;
        this.children = new HashSet<PathwayNode>();
        this.data = new PathwayNodeData();
    }

    public PathwayNode addChild(String stId, Long pathwayId, String name, boolean hasDiagram){
        PathwayNode node = new PathwayNode(this, stId, pathwayId, name, hasDiagram);
        this.children.add(node);
        return node;
    }

    public Set<PathwayNode> getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    public PathwayNode getDiagram(){
        if(this.hasDiagram){
            return this;
        }
        if(parent != null){
            return parent.getDiagram();
        }
        return null;
    }

    protected Set<PathwayNode> getHitNodes(){
        Set<PathwayNode> rtn = new HashSet<PathwayNode>();
        if(this.data.hasResult()){
            rtn.add(this);
            for (PathwayNode node : children) {
                rtn.addAll(node.getHitNodes());
            }
        }
        return rtn;
    }

    public boolean isLowerLevelPathway() {
        return isLowerLevelPathway;
    }

    public Long getPathwayId() {
        return pathwayId;
    }

    public PathwayNode getParent() {
        return parent;
    }

    public PathwayNodeData getPathwayNodeData() {
        return data;
    }

    public SpeciesNode getSpecies(){
        if(this.parent==null){
            PathwayRoot root = (PathwayRoot) this;
            return root.getSpecies();
        }else{
            return parent.getSpecies();
        }
    }

    public String getStId() {
        return stId;
    }

    public void setLowerLevelPathway(boolean isLowerLevelPathway) {
        this.isLowerLevelPathway = isLowerLevelPathway;
    }

    protected void setCounters(PathwayNodeData speciesData){
        this.data.setCounters(speciesData);
        for (PathwayNode child : this.children) {
            child.setCounters(speciesData);
        }
    }

    public void setResultStatistics(Map<MainResource, Integer> sampleSizePerResource, Integer notFound){
        for (PathwayNode child : this.children) {
            child.setResultStatistics(sampleSizePerResource, notFound);
        }
        this.data.setResultStatistics(sampleSizePerResource, notFound);
    }

    public void process(MainIdentifier mainIdentifier, Set<AnalysisReaction> reactions){
        this.process(mainIdentifier, mainIdentifier, reactions);
    }

    public void process(Identifier identifier, MainIdentifier mainIdentifier, Set<AnalysisReaction> reactions){
        this.data.addMapping(identifier, mainIdentifier);
        this.data.addReactions(mainIdentifier.getResource(), reactions);
        if(this.parent!=null){
            this.parent.process(identifier, mainIdentifier, reactions);
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(PathwayNode o) {
        int rtn = this.data.getEntitiesPValue().compareTo(o.data.getEntitiesPValue());
        if(rtn==0){
            Double oScore = o.data.getScore(); Double thisScore = this.data.getScore();
            rtn = oScore.compareTo(thisScore);
            if(rtn==0){
                rtn = o.data.getEntitiesFound().compareTo(this.data.getEntitiesFound());
            }
        }
        return rtn;
    }
}
