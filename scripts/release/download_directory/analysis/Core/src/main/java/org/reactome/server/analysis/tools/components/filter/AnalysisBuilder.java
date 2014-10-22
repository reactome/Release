package org.reactome.server.analysis.tools.components.filter;

import org.gk.persistence.MySQLAdaptor;
import org.reactome.server.analysis.core.data.AnalysisDataUtils;
import org.reactome.server.analysis.core.model.*;
import org.reactome.server.analysis.core.model.identifier.MainIdentifier;
import org.reactome.server.analysis.core.util.MapSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@SuppressWarnings("UnusedDeclaration")
@Component
public class AnalysisBuilder {
    @Autowired
    private PathwayHierarchyBuilder pathwaysBuilder;
    @Autowired
    private ReactionLikeEventBuilder rleBuilder;
    @Autowired
    private PhysicalEntityHierarchyBuilder peBuilder;

    public void build(MySQLAdaptor dba, String fileName){
        this.pathwaysBuilder.build(dba);
        this.rleBuilder.build(dba, this.pathwaysBuilder.getPathwayLocation());
        this.peBuilder.build(dba, rleBuilder.getEntityPathwayReaction());
        this.peBuilder.setOrthologous();
        //Pre-calculates the counters for each MainResource/PathwayNode
        this.calculateNumbersInHierarchyNodesForMainResources();

        DataContainer container = new DataContainer(getHierarchies(),
                                                    getPhysicalEntityGraph(),
                                                    getPathwayLocation(),
                                                    getIdentifierMap());
        AnalysisDataUtils.kryoSerialisation(container, fileName);
    }

    private void calculateNumbersInHierarchyNodesForMainResources(){
        MapSet<Long, PathwayNode> pathwayLocation = getPathwayLocation();
        IdentifiersMap identifiersMap = this.getIdentifierMap();

        for (PhysicalEntityNode physicalEntityNode : getPhysicalEntityGraph().getAllNodes()) {
            MainIdentifier mainIdentifier = physicalEntityNode.getIdentifier();
            if(mainIdentifier!=null){
                for (Long pathwayId : physicalEntityNode.getPathwayIds()) {
                    Set<PathwayNode> pNodes = pathwayLocation.getElements(pathwayId);
                    if(pNodes==null) continue;
                    for (PathwayNode pathwayNode : pNodes) {
                        Set<AnalysisReaction> reactions = physicalEntityNode.getReactions(pathwayId);
                        pathwayNode.process(mainIdentifier, reactions);
                    }
                }
            }
        }
        pathwaysBuilder.prepareToSerialise();
    }

    public Map<SpeciesNode, PathwayHierarchy> getHierarchies() {
        return pathwaysBuilder.getHierarchies();
    }

    public IdentifiersMap getIdentifierMap(){
        return peBuilder.getIdentifiersMap();
    }

    public MapSet<Long, PathwayNode> getPathwayLocation(){
        return pathwaysBuilder.getPathwayLocation();
    }

    public PhysicalEntityGraph getPhysicalEntityGraph() {
        return peBuilder.getPhysicalEntityGraph();
    }
}
