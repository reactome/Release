package org.reactome.server.analysis.core.model;

import org.reactome.server.analysis.core.model.identifier.MainIdentifier;
import org.reactome.server.analysis.core.model.resource.MainResource;
import org.reactome.server.analysis.core.util.MapSet;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class PhysicalEntityNode {
    private Long id;
    private SpeciesNode species;

    private Set<PhysicalEntityNode> parents = null;
    private Set<PhysicalEntityNode> children = null;

    private Map<SpeciesNode, PhysicalEntityNode> inferredFrom;
    private Map<SpeciesNode, PhysicalEntityNode> inferredTo;

    //We DO NOT use PathwayNode here because at some point cloning the hierarchies
    //structure will be needed and keeping this separate will help to maintain the
    //links between both structures easy through the pathway location map
    private MapSet<Long, AnalysisReaction> pathwayReactions = null;

    //Next variable will NOT contain value for Complexes and EntitySets because they
    //do not have main resources (members or components are treated separately).
    private MainIdentifier identifier = null;

    public PhysicalEntityNode(Long id, SpeciesNode species) {
        this.id = id;
        this.species = species;
    }

    public PhysicalEntityNode(Long id, SpeciesNode species, MainResource mainResource, String mainIdentifier) {
        this(id, species);
        this.identifier = new MainIdentifier(mainResource, new AnalysisIdentifier(mainIdentifier));
    }

    public void addChild(PhysicalEntityNode child){
        if(this.children==null){
            this.children = new LinkedHashSet<PhysicalEntityNode>();
        }
        this.children.add(child);
        //During the building process we need to keep the graph well formed
        //NOTE: PARENTS need to be removed before the data structure is serialised
        child.addParent(this);
    }

    public void addInferredTo(PhysicalEntityNode inferredTo){
        //We do NOT want cycles at this point
        PhysicalEntityNode aux = inferredTo.getInferredFrom().get(this.species);
        if(aux!=null && aux.equals(this)) return;

        if(this.inferredTo==null){
            this.inferredTo = new HashMap<SpeciesNode, PhysicalEntityNode>();
        }
        this.inferredTo.put(inferredTo.getSpecies(), inferredTo);
    }

    public void addInferredFrom(PhysicalEntityNode inferredFrom){
        //We do NOT want cycles at this point
        PhysicalEntityNode aux = inferredFrom.getInferredTo().get(this.species);
        if(aux!=null && aux.equals(this)) return;

        if(this.inferredFrom==null){
            this.inferredFrom = new HashMap<SpeciesNode, PhysicalEntityNode>();
        }
        this.inferredFrom.put(inferredFrom.getSpecies(), inferredFrom);
    }

    protected void addParent(PhysicalEntityNode parent){
        if(this.parents==null){
            this.parents = new HashSet<PhysicalEntityNode>();
        }
        this.parents.add(parent);
    }

    public void addPathwayReactions(MapSet<Long, AnalysisReaction> pathwayReactions){
        if(this.pathwayReactions==null){
            this.pathwayReactions = new MapSet<Long, AnalysisReaction>();
        }
        this.pathwayReactions.addAll(pathwayReactions);
    }

    public Set<PhysicalEntityNode> getAllNodes(){
        Set<PhysicalEntityNode> rtn = new HashSet<PhysicalEntityNode>();
        rtn.add(this);
        if(this.children!=null){
            for (PhysicalEntityNode child : this.children) {
                rtn.addAll(child.getAllNodes());
            }
        }
        return rtn;
    }

    public Set<PhysicalEntityNode> getChildren() {
        if(children==null){
            return new HashSet<PhysicalEntityNode>();
        }
        return children;
    }

    public PhysicalEntityNode getProjection(SpeciesNode species){
        if(this.species==null) return this; //DO NOT CHANGE THIS AGAIN. If is a SmallMolecule it does NOT have species..
        if(this.species.equals(species)) return this;
        PhysicalEntityNode rtn = null;
        if(inferredFrom!=null ){
            rtn = inferredFrom.get(species);
        }
        if(rtn==null && inferredTo!=null){
            rtn = inferredTo.get(species);
        }
        return rtn;
    }

    public Long getId() {
        return id;
    }

    public MainIdentifier getIdentifier() {
        return identifier;
    }

    public Map<SpeciesNode, PhysicalEntityNode> getInferredFrom() {
        if(inferredFrom==null) return new HashMap<SpeciesNode, PhysicalEntityNode>();
        return inferredFrom;
    }

    public Map<SpeciesNode, PhysicalEntityNode> getInferredTo() {
        if(inferredTo==null) return new HashMap<SpeciesNode, PhysicalEntityNode>();
        return inferredTo;
    }

    public Set<AnalysisReaction> getReactions(Long pathwayId){
        Set<AnalysisReaction> rtn = new HashSet<AnalysisReaction>();
        if(this.pathwayReactions!=null){
            Set<AnalysisReaction> reactions = this.pathwayReactions.getElements(pathwayId);
            if(reactions!=null){
                rtn.addAll(reactions);
            }
        }
        if(this.parents!=null){
            for (PhysicalEntityNode parent : this.parents) {
                rtn.addAll(parent.getReactions(pathwayId));
            }
        }
        return rtn;
    }

    public SpeciesNode getSpecies() {
        return species;
    }

    public Set<Long> getPathwayIds() {
        Set<Long> rtn = new HashSet<Long>();
        if(this.parents!=null){
            for (PhysicalEntityNode parent : this.parents) {
                rtn.addAll(parent.getPathwayIds());
            }
        }
        if(this.pathwayReactions!=null){
            rtn.addAll(this.pathwayReactions.keySet());
        }
        return rtn;
    }

    protected void removeLinkToParent(){
        this.parents = null;
        if(this.children!=null){
            for (PhysicalEntityNode child : this.children) {
                child.removeLinkToParent();
            }
        }
    }

    protected void setLinkToParent(PhysicalEntityNode parent){
        if(parent!=null){
            this.addParent(parent);
        }
        if(this.children!=null){
            for (PhysicalEntityNode child : this.children) {
                child.setLinkToParent(this);
            }
        }
    }

    protected void setOrthologiesCrossLinks(){
        for (SpeciesNode speciesNode : this.getInferredTo().keySet()) {
            if(this.inferredTo.get(speciesNode).inferredFrom==null){
                this.inferredTo.get(speciesNode).inferredFrom = new HashMap<SpeciesNode, PhysicalEntityNode>();
            }
            this.inferredTo.get(speciesNode).inferredFrom.put(this.species, this);
        }
        for (SpeciesNode speciesNode : this.getInferredFrom().keySet()) {
            if(this.inferredFrom.get(speciesNode).inferredTo==null){
                this.inferredFrom.get(speciesNode).inferredTo = new HashMap<SpeciesNode, PhysicalEntityNode>();
            }
            this.inferredFrom.get(speciesNode).inferredTo.put(this.species, this);
        }
        if(this.children!=null){
            for (PhysicalEntityNode child : this.children) {
                child.setOrthologiesCrossLinks();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhysicalEntityNode that = (PhysicalEntityNode) o;

        //noinspection RedundantIfStatement
        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        if(id!=null)
            return id.hashCode();
        else
            return 0;
    }
}
