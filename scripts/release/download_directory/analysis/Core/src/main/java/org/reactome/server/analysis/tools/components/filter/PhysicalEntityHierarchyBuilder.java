package org.reactome.server.analysis.tools.components.filter;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.reactome.core.controller.ReactomeToRESTfulAPIConverter;
import org.reactome.core.mapper.PhysicalEntityMapper;
import org.reactome.core.model.*;
import org.reactome.server.analysis.core.model.*;
import org.reactome.server.analysis.core.model.resource.MainResource;
import org.reactome.server.analysis.core.model.resource.Resource;
import org.reactome.server.analysis.core.model.resource.ResourceFactory;
import org.reactome.server.analysis.core.util.MapSet;
import org.reactome.server.analysis.core.util.Pair;
import org.reactome.server.analysis.tools.BuilderTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * For a previously loaded set of PhysicalEntities (mapped to the relevant pathways where they are present)
 * this class will decompose all of these and create a graph of the Reactome Physical Entities and a RADIX
 * TREE to map identifiers to Physical Entities.
 *
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Component
public class PhysicalEntityHierarchyBuilder {
    private static Logger logger = Logger.getLogger(PhysicalEntityHierarchyBuilder.class.getName());


    @Autowired
    private ReactomeToRESTfulAPIConverter converter;

    private MySQLAdaptor dba;

    private PhysicalEntityMapper mapper;

    //The buffer is used in building time to avoid querying/decomposition of those previously processed
    private Map<Long, PhysicalEntityNode> physicalEntityBuffer = new HashMap<Long, PhysicalEntityNode>();

    //Will contain the RADIX-TREE with the map (identifiers -> [PhysicalEntityNode])
    private IdentifiersMap identifiersMap;
    //A graph representation of the PhysicalEntities in Reactome
    private PhysicalEntityGraph physicalEntityGraph;

    public PhysicalEntityHierarchyBuilder() {
        this.mapper = new PhysicalEntityMapper();
        this.mapper.setClassName(ReactomeJavaConstants.PhysicalEntity);

        this.identifiersMap = new IdentifiersMap();
        this.physicalEntityGraph = new PhysicalEntityGraph();
    }

    private void clearDBACache(){
        try {
            dba.refresh(); //Cleans the mysql adaptor internal cache
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void build(MySQLAdaptor dba, EntityPathwayReactionMap physicalEntitiesPathways){
        this.dba = dba;
        this.clearDBACache();
        int i=0; int tot = physicalEntitiesPathways.keySet().size();
        for (Long physicalEntityId : physicalEntitiesPathways.keySet()) {
            if(BuilderTool.VERBOSE) {
                System.out.print("\rPhysicalEntity -> " + physicalEntityId + " >> " + (++i) + "/" + tot);
            }
            PhysicalEntityNode node = this.process(physicalEntityId);
            if(node!=null){
                MapSet<Long, AnalysisReaction> pathwaysReactions = physicalEntitiesPathways.getPathwaysReactions(physicalEntityId);
                node.addPathwayReactions(pathwaysReactions);
                this.physicalEntityGraph.addRoot(node);
            }
        }
        if(BuilderTool.VERBOSE) {
            System.out.println("\rPhysicalEntity processed");
        }
    }

    public IdentifiersMap getIdentifiersMap() {
        return this.identifiersMap;
    }

    public PhysicalEntityGraph getPhysicalEntityGraph() {
        return physicalEntityGraph;
    }

//    public Map<Long, PhysicalEntityNode> getPhysicalEntityBuffer() {
//        return physicalEntityBuffer;
//    }

    @SuppressWarnings("ConstantConditions")
    private PhysicalEntityNode process(Long physicalEntityId){
        if(physicalEntityId==null){
            return null; //Just in case
        }
        //When organically growing, we do not want previous processed entities to be processed again
        //This a recursive algorithm STOP CONDITION (DO NOT REMOVE)
        PhysicalEntityNode aux = physicalEntityBuffer.get(physicalEntityId);
        if(aux!=null){
            return aux;
        }

        //1st -> fetch the GKInstance from the database
        GKInstance instance;
        try {
            instance = this.dba.fetchInstance(physicalEntityId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        //2nd -> create the corresponding PhysicalEntityObject
        PhysicalEntity physicalEntity;
        try {
            physicalEntity = (PhysicalEntity) this.converter.convert(instance);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        //3rd -> use the mapper to fill the cross-references that will be used later on
        try {
            this.mapper.fillCrossReferences(instance, physicalEntity, this.converter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //4th -> figure out the species of the physical entity
        SpeciesNode species = null;
        if(instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)){
            try {
                GKInstance s = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
                species = SpeciesNodeFactory.getSpeciesNode(s.getDBID(), s.getDisplayName());
            } catch (Exception e) {
                try {
                    List<?> ss = instance.getAttributeValuesList(ReactomeJavaConstants.species);
                    if(ss == null && !ss.isEmpty()){
                        GKInstance s = (GKInstance) ss.get(0);
                        species = SpeciesNodeFactory.getSpeciesNode(s.getDBID(), s.getDisplayName());
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
        //5th -> set the node for the main identifier
        PhysicalEntityNode node = this.getPhysicalEntityNode(physicalEntityId, species, physicalEntity.getCrossReference());
        if(node!=null){
            MapSet<Resource, String> resourceIdentifiers = this.getResourceIdentifiers(physicalEntity);
            for (Resource resource : resourceIdentifiers.keySet()) {
                for (String identifier : resourceIdentifiers.getElements(resource)) {
                    //create the corresponding entries in the RADIX TREE for all the (identifier, resource, node)
                    this.identifiersMap.add(identifier, resource, node);
                }
            }
        }else{
            if(physicalEntity instanceof SimpleEntity || physicalEntity instanceof EntityWithAccessionedSequence){
                logger.warn("No main resource and identifier found for physical entity " + physicalEntityId);
            }
            node = new PhysicalEntityNode(physicalEntityId, species);
            for (Long containedEntity : this.getContainedPhysicalEntities(physicalEntity)) {
                PhysicalEntityNode child = this.process(containedEntity);
                if(child!=null){
                    node.addChild(child);
                }
            }
        }
        // To avoid future decompositions and cycles
        this.physicalEntityBuffer.put(node.getId(), node);
//        try {
//            this.setOrthologous(node, physicalEntity.getInferredTo());
//        } catch (Exception e) {
//            logger.error(String.format("Could not set the orthology for %d", node.getId()), e);
//        }
        return node;
    }

    private Set<Long> getContainedPhysicalEntities(PhysicalEntity physicalEntity){
        Set<PhysicalEntity> contained = new HashSet<PhysicalEntity>();
        if(physicalEntity instanceof Complex){
            Complex complex = (Complex) physicalEntity;
            contained.addAll(complex.getHasComponent());
        }else if(physicalEntity instanceof EntitySet){
            EntitySet entitySet = (EntitySet) physicalEntity;
            if(entitySet.getHasMember()!=null){
                contained.addAll(entitySet.getHasMember());
            }
            if(physicalEntity instanceof CandidateSet){
                CandidateSet candidateSet = (CandidateSet) physicalEntity;
                if(candidateSet.getHasCandidate()!=null){
                    contained.addAll(candidateSet.getHasCandidate());
                }
            }
        }else if(physicalEntity instanceof Polymer){
            Polymer polymer = (Polymer) physicalEntity;
            if(polymer.getRepeatedUnit()!=null){
                //Aux contains only the DIFFERENT repeated units (just in case)
                contained.addAll(polymer.getRepeatedUnit());
            }
        }

        Set<Long> rtn = new HashSet<Long>();
        for (PhysicalEntity entity : contained) {
            rtn.add(entity.getDbId());
        }
        return rtn;
    }

    private MapSet<Resource, String> getResourceIdentifiers(PhysicalEntity physicalEntity){
        MapSet<Resource, String> rtn = new MapSet<Resource, String>();
        if(physicalEntity.getCrossReference()!=null){
            for (DatabaseIdentifier databaseIdentifier : physicalEntity.getCrossReference()) {
                for (Pair<Resource, String> resourceIdentifier : this.getIdentifier(databaseIdentifier)) {
                    rtn.add(resourceIdentifier.getFst(), resourceIdentifier.getSnd());
                }
            }
        }
        return rtn;
    }

    private PhysicalEntityNode getPhysicalEntityNode(Long physicalEntityId, SpeciesNode species, List<DatabaseIdentifier> identifiers){
        PhysicalEntityNode aux = null;
        if(identifiers != null) {
            for (DatabaseIdentifier identifier : identifiers) {
                for (Pair<Resource, String> resourceIdentifier : this.getIdentifier(identifier)) {
                    Resource resource = resourceIdentifier.getFst();
                    if(resource instanceof MainResource){
                        MainResource mainResource = (MainResource) resource;
                        String mainIdentifier = resourceIdentifier.getSnd();
                        if(mainResource.isAuxMainResource() && aux==null){
                            aux = new PhysicalEntityNode(physicalEntityId, species, mainResource, mainIdentifier);
                        }else{
                            return new PhysicalEntityNode(physicalEntityId, species, mainResource, mainIdentifier);
                        }
                    }
                }
            }
        }
        return aux;
    }

    /**
     * Returns a list of pairs (resource, identifier) contained in the display name
     * @param databaseIdentifier the database identifier object containing the information
     * @return a list of pairs (resource, identifier) contained in the display name
     */
    private List<Pair<Resource, String>> getIdentifier(DatabaseIdentifier databaseIdentifier){
        List<Pair<Resource, String>> rtn = new LinkedList<Pair<Resource, String>>();
        try {
            Resource resource = null;
            GKInstance instance = dba.fetchInstance(databaseIdentifier.getDbId());
            if(instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceDatabase)){
                GKInstance referenceDatabase = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                String r = referenceDatabase.getDisplayName();
                if(r!=null){
                    resource = ResourceFactory.getResource(r);
                }
            }

            if(resource!=null){
                //If the variant identifier exists we use it as the identifier
                if(instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier)){
                    String variantIdentifier = (String) instance.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
                    if(variantIdentifier!=null){
                        rtn.add(new Pair<Resource, String>(resource, variantIdentifier));
                    }
                }
                if(rtn.isEmpty()){ //If rtn is empty is because the variant identifier does NOT exist
                    if(instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.identifier)){
                        String mainIdentifier = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);
                        rtn.add(new Pair<Resource, String>(resource, mainIdentifier));
                    }
                }
                //From now on, resource will have "#" to distinguish from the main pair above
                String rscAux = "#" + resource.getName(); //fake resource
                resource = ResourceFactory.getResource(rscAux);
                List<String> aux = new LinkedList<String>();
                if(instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.otherIdentifier)){
                    for (Object identifier : instance.getAttributeValuesList(ReactomeJavaConstants.otherIdentifier)) {
                        aux.add((String) identifier);
                    }
                }
                if(instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.geneName)){
                    for (Object gene : instance.getAttributeValuesList(ReactomeJavaConstants.geneName)) {
                        aux.add((String) gene);
                    }
                }
                for (String identifier : aux) {
                    rtn.add(new Pair<Resource, String>(resource, identifier));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rtn;
    }

    @SuppressWarnings("ConstantConditions")
    public void setOrthologous(){
        Set<PhysicalEntityNode> nodes = getPhysicalEntityGraph().getAllNodes();
        int i=0; int tot = nodes.size();
        for (PhysicalEntityNode node : nodes) {
            if(BuilderTool.VERBOSE) {
                System.out.print("\rOrthology for PhysicalEntity -> " + node.getId() + " >> " + (++i) + "/" + tot);
            }
            GKInstance pe = null;
            try {
                pe = dba.fetchInstance(node.getId());
            } catch (Exception e1) {
                logger.error(String.format("%d entity could not be retrieved", node.getId()));
            }

            List<?> inferredFrom = getAttributeValuesList(pe, ReactomeJavaConstants.inferredFrom);
            Set<PhysicalEntityNode> inferredFromNodes = getOrthologyNodes(node, inferredFrom);
            for (PhysicalEntityNode inferredFromNode : inferredFromNodes) {
                node.addInferredFrom(inferredFromNode);
            }

            List<?> inferredTo = getAttributeValuesList(pe, ReactomeJavaConstants.inferredTo);
            Set<PhysicalEntityNode> inferredToNodes = getOrthologyNodes(node, inferredTo);
            for (PhysicalEntityNode inferredToNode : inferredToNodes) {
                node.addInferredTo(inferredToNode);
            }
        }
        if(BuilderTool.VERBOSE) {
            System.out.println("\rOrthologies processed");
        }
    }

    private List<?> getAttributeValuesList(GKInstance instance, String attr){
        List<?> list = null;
        if(instance.getSchemClass().isValidAttribute(attr)){
            try {
                list = instance.getAttributeValuesList(attr);
            } catch (Exception e1) {
                logger.error(String.format("'%s' entities error for %d", attr, instance.getDBID()));
            }
        }
        return list;
    }

    private Set<PhysicalEntityNode> getOrthologyNodes(PhysicalEntityNode node, List<?> orth){
        Set<PhysicalEntityNode> rtn = new HashSet<PhysicalEntityNode>();
        if(orth==null) return rtn;
        for (Object obj : orth) {
            Long id = ((GKInstance) obj).getDBID();
            PhysicalEntityNode aux = physicalEntityBuffer.get(id);
            if(aux!=null){
                rtn.add(aux);
            }else{
                logger.warn(String.format("There is not node for %d (orthology from %d)", id, node.getId()));
            }
        }
        return rtn;
    }
}
