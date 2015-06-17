package org.reactome.server.analysis.core.model;

import org.reactome.server.analysis.core.data.AnalysisDataUtils;
import org.reactome.server.analysis.core.util.MapSet;

import java.io.Serializable;
import java.util.Map;

/**
 * Contains the different data structures for the binary data and also provides
 * methods to initialize the data structure after loading from file and to
 * "prepare" the data structure for serialising
 *
 * PLEASE NOTE
 * The pathway location map is kept separately because at some point splitting
 * or cloning the pathway hierarchies will be needed, so keeping a map will
 * help to perform this task making it easier and faster.
 * Linking from the physical entity graph nodes to the pathway hierarchy is an
 * option that improves the binary time but makes the splitting or cloning
 * tasks MORE difficult and slow
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class DataContainer implements Serializable {
    //A double link hierarchy tree with the pathways for each species
    Map<SpeciesNode, PathwayHierarchy> pathwayHierarchies;
    //A map between pathways identifier and their locations in the pathway hierarchy
    MapSet<Long, PathwayNode> pathwayLocation;

    //A radix-tree with (identifier -> HierarchyNode)
    IdentifiersMap identifiersMap;
    //A double link graph with the representation of the physical entities
    PhysicalEntityGraph physicalEntityGraph;

    public DataContainer(Map<SpeciesNode, PathwayHierarchy> pathwayHierarchies,
                         PhysicalEntityGraph physicalEntityGraph,
                         MapSet<Long, PathwayNode> pathwayLocation,
                         IdentifiersMap identifiersMap) {
        this.pathwayHierarchies = pathwayHierarchies;
        this.physicalEntityGraph = physicalEntityGraph;
        this.pathwayLocation = pathwayLocation;
        this.identifiersMap = identifiersMap;
    }

    /**
     * Returns a clone of the clean version of the hierarchies
     * @return a clone of the clean version of the hierarchies
     */
    public HierarchiesData getHierarchiesData() {
        //The object is not kept by itself because it requires more disk space
        HierarchiesData data = new HierarchiesData(this.pathwayHierarchies, this.pathwayLocation);
        return AnalysisDataUtils.kryoCopy(data);
    }

    public PhysicalEntityGraph getPhysicalEntityGraph() {
        return physicalEntityGraph;
    }

    public IdentifiersMap getIdentifiersMap() {
        return identifiersMap;
    }

    public void initialize(){
        this.physicalEntityGraph.setLinkToParent();
        this.physicalEntityGraph.setOrthologiesCrossLinks();
    }

    /**
     * Avoids cycles when serialising the graph and reduces the space needed in memory
     * @return the DataContainer without graph cycles and size reduced
     */
    public DataContainer prepareToSerialise(){
        this.physicalEntityGraph.prepareToSerialise();
        return this;
    }
}