package org.reactome.server.analysis.tools.components.filter;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.controller.DatabaseObjectHelper;
import org.reactome.core.model.Species;
import org.reactome.server.analysis.core.model.PathwayHierarchy;
import org.reactome.server.analysis.core.model.PathwayNode;
import org.reactome.server.analysis.core.model.SpeciesNode;
import org.reactome.server.analysis.core.model.SpeciesNodeFactory;
import org.reactome.server.analysis.core.util.MapSet;
import org.reactome.server.analysis.tools.BuilderTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Component
public class PathwayHierarchyBuilder {
    private static Logger logger = Logger.getLogger(PathwayHierarchyBuilder.class.getName());

    @Autowired
    private DatabaseObjectHelper helper;

    //Keeps the set of species identifiers used as main species in reactome
    private Set<Long> mainSpecies;

    private Map<SpeciesNode, PathwayHierarchy> hierarchies = new HashMap<SpeciesNode, PathwayHierarchy>();
    private MapSet<Long, PathwayNode> pathwayLocation = new MapSet<Long, PathwayNode>();

    public void build(MySQLAdaptor dba) {
        //THIS IS NEEDED HERE
        this.helper.setDba(dba);
        this.setMainSpecies();

        List<Species> speciess = this.helper.getSpeciesList();
        int i = 0; int tot = speciess.size();
        for (Species species : speciess) {
//            if(!species.getDbId().equals(48887L)) break;
            if(BuilderTool.VERBOSE) {
                System.out.print("\rCreating the pathway hierarchies >> " + ++i + "/" + tot + " ");
            }
            SpeciesNode speciesNode = SpeciesNodeFactory.getSpeciesNode(species.getDbId(), species.getDisplayName());
            PathwayHierarchy pathwayHierarchy = new PathwayHierarchy(speciesNode);
            this.hierarchies.put(speciesNode, pathwayHierarchy);
            try{
                for (GKInstance instance : this.helper.getFrontPageItems(species.getDisplayName())) {
                    if(!this.isValidSpecies(speciesNode, instance)){
                        continue; //Please read documentation of isValidSpecies method
                    }
                    String stId =  this.getStableIdentifier(instance);
                    boolean hasDiagram = this.getHasDiagram(instance);
                    PathwayNode node = pathwayHierarchy.addFrontpageItem(stId, instance.getDBID(), instance.getDisplayName(), hasDiagram);
                    this.pathwayLocation.add(instance.getDBID(), node);
                    if(BuilderTool.VERBOSE) {
                        System.out.print("."); // Indicates progress
                    }
                    this.fillBranch(speciesNode, node, instance);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        if(BuilderTool.VERBOSE) {
            System.out.println("\rPathway hierarchies created successfully");
        }
    }

    private void fillBranch(SpeciesNode speciesNode, PathwayNode node, GKInstance inst){
        try{
            if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
                List<?> children = inst.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
                for (Object obj : children) {
                    GKInstance instance = (GKInstance) obj;
                    if(!this.isValidSpecies(speciesNode, instance)){
                        continue; //Please read documentation of isValidSpecies method
                    }
                    if(instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)){
                        String stId = this.getStableIdentifier(instance);
                        boolean hasDiagram = this.getHasDiagram(instance);
                        PathwayNode aux = node.addChild(stId, instance.getDBID(), instance.getDisplayName(), hasDiagram);
                        this.pathwayLocation.add(instance.getDBID(), aux);
                        this.fillBranch(speciesNode, aux, instance);
                    }else{
                        //if the pathways has other events than pathways means it is a lower level pathway candidate
                        node.setLowerLevelPathway(true);
                    }
                }
            }
        } catch (Exception e){
            logger.error("Problem filling hierarchy branch for " + speciesNode.getName(), e);
            e.printStackTrace();
        }
    }

    public Map<SpeciesNode, PathwayHierarchy> getHierarchies() {
        return hierarchies;
    }

    public MapSet<Long, PathwayNode> getPathwayLocation() {
        return pathwayLocation;
    }

    private GKInstance getSpecies(GKInstance instance) throws Exception {
        if(instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)){
            return (GKInstance) instance.getAttributeValuesList(ReactomeJavaConstants.species).get(0);
        }
        return null;
    }

    private String getStableIdentifier(GKInstance pathway){
        try {
            GKInstance stId = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
            return (String) stId.getAttributeValue(ReactomeJavaConstants.identifier);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean getHasDiagram(GKInstance pathway){
        try {
            Collection<?> diagrams = pathway.getReferers(ReactomeJavaConstants.representedPathway);
            if (diagrams != null && diagrams.size() > 0) {
                for (Object obj : diagrams) {
                    GKInstance diagram = (GKInstance) obj;
                    if (diagram.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Some oddities have been detected related to species. Those will be fixed at the curation level.
     * In the meantime we consider a invalid species when it is different than the species of the top-level-pathway
     * and equal to any of the other species in the main Reactome species list.
     */
    private boolean isValidSpecies(SpeciesNode speciesNode, GKInstance event) throws Exception {
        GKInstance species = this.getSpecies(event);
        if(speciesNode.getSpeciesID().equals(species.getDBID())){
            return true;
        }else if(this.mainSpecies.contains(species.getDBID())){
            logger.warn(String.format("Event %d (%s) belongs to %s instead of %s",
                    event.getDBID(), event.getDisplayName(), species.getDisplayName(), speciesNode.getName()));
            return false;
        }
        return true;
    }

    protected void prepareToSerialise(){
        if(BuilderTool.VERBOSE) {
            System.out.print("Setting up the resource counters for each Resource/PathwayNode");
        }
        for (SpeciesNode species : this.hierarchies.keySet()) {
            PathwayHierarchy hierarchy = this.hierarchies.get(species);
            hierarchy.setCountersAndCleanUp();
        }
        if(BuilderTool.VERBOSE) {
            System.out.println("\rResource counters set up successfully.");
        }
    }

    private void setMainSpecies(){
        this.mainSpecies = new HashSet<Long>();
        for (Species species : this.helper.getSpeciesList()) {
            this.mainSpecies.add(species.getDbId());
        }
    }
}
