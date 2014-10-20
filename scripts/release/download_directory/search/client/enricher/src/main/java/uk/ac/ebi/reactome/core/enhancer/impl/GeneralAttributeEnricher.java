package uk.ac.ebi.reactome.core.enhancer.impl;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.reactome.core.enhancer.exception.EnricherException;
import uk.ac.ebi.reactome.core.model.result.EnrichedEntry;
import uk.ac.ebi.reactome.core.model.result.submodels.*;

import java.util.*;

/**
 * Queries the MySql database and converts entry to a local object
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class GeneralAttributeEnricher extends Enricher{

    private static final Logger logger = LoggerFactory.getLogger(GeneralAttributeEnricher.class);
    private static final String PUBMED_URL = "http://www.ncbi.nlm.nih.gov/pubmed/";
    private static final String PATHWAY_BROWSER_URL = "/PathwayBrowser/#";

    public void setGeneralAttributes(GKInstance instance, EnrichedEntry enrichedEntry) throws EnricherException {


        try {
            List<String> names = getAttributes(instance, ReactomeJavaConstants.name);
            if (names!= null && !names.isEmpty()) {
                if (names.size() >= 1) {
                    enrichedEntry.setName(names.get(0));
                    if (names.size() > 1) {
                        enrichedEntry.setSynonyms(names.subList(1, names.size() - 1));
                    }
                }else {
                    enrichedEntry.setName(instance.getDisplayName());
                }
            }
            if (hasValue(instance, ReactomeJavaConstants.stableIdentifier)){
                enrichedEntry.setStId((String) ((GKInstance) instance.getAttributeValue(ReactomeJavaConstants.stableIdentifier)).getAttributeValue(ReactomeJavaConstants.identifier));
            }
            enrichedEntry.setSpecies(getAttributeDisplayName(instance,ReactomeJavaConstants.species));
            List<?> summationInstances = instance.getAttributeValuesList(ReactomeJavaConstants.summation);
            List<String> summations = new ArrayList<String>();
            for (Object summationInstance : summationInstances) {
                GKInstance summation = (GKInstance) summationInstance;
                summations.add((String) summation.getAttributeValue(ReactomeJavaConstants.text));
            }
            enrichedEntry.setSummations(summations);
            enrichedEntry.setCompartments(getGoTerms(instance, ReactomeJavaConstants.compartment));
            enrichedEntry.setInferredFrom(getEntityReferences(instance, ReactomeJavaConstants.inferredFrom));
            enrichedEntry.setOrthologousEvents(getEntityReferences(instance, ReactomeJavaConstants.orthologousEvent));
            enrichedEntry.setCrossReferences(getCrossReferences(instance, ReactomeJavaConstants.crossReference, null));
            enrichedEntry.setDiseases(getDiseases(instance));
            enrichedEntry.setLiterature(setLiteratureReferences(instance));

            List<List<EntityReference>> list = new ArrayList<List<EntityReference>>();
            List<EntityReference> path = new ArrayList<EntityReference>();
            GraphNode tree  = new GraphNode();
            tree.setDbId(instance.getDBID());
            tree.setName(instance.getDisplayName());
            if (hasValue(instance, ReactomeJavaConstants.species)) {
                GKInstance species = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
                tree.setSpecies(species.getDisplayName());
                tree.setSpeciesId(species.getDBID());
            }
            tree.setStId(getStableIdentifier(instance));
            recursion(instance, tree);
            Set<GraphNode>leafs = tree.getLeafs();
            Set<TreeNode>trees = generateUrlFromLeaves(leafs);
            enrichedEntry.setLocationsPathwayBrowser(trees);


        } catch (Exception e) {
            logger.error("Error occurred when trying to set general Attributes", e);
            throw new EnricherException("Error occurred when trying to set general Attributes", e);
        }
    }

    /**
     * Returns a list of literature for a given instance
     * @param instance GkInstance
     * @return List of Literature Objects
     * @throws EnricherException
     */
    private List<Literature> setLiteratureReferences(GKInstance instance) throws EnricherException {
        if (hasValues(instance, ReactomeJavaConstants.literatureReference)) {
            List<Literature> literatureList = new ArrayList<Literature>();
            try {
                List<?> literatureInstanceList = instance.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
                for (Object literatureObject : literatureInstanceList) {
                    GKInstance literatureInstance = (GKInstance) literatureObject;
                    Literature literature = new Literature();
                    literature.setTitle(getAttributeString(literatureInstance, ReactomeJavaConstants.title));
                    literature.setJournal(getAttributeString(literatureInstance, ReactomeJavaConstants.journal));
                    literature.setPubMedIdentifier(getAttributeString(literatureInstance, ReactomeJavaConstants.pubMedIdentifier));
                    literature.setYear(getAttributeInteger(literatureInstance, ReactomeJavaConstants.year));
                    if (literature.getPubMedIdentifier() != null) {
                        literature.setUrl(PUBMED_URL + literature.getPubMedIdentifier());
                    }
                    literatureList.add(literature);
                }
                return literatureList;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Returns a list of disease information related to an instance
     * @param instance GkInstance
     * @return List of Disease Objects
     * @throws EnricherException
     */
    private List<Disease> getDiseases(GKInstance instance) throws EnricherException {
        if (hasValues(instance, ReactomeJavaConstants.disease)) {
            try {
                List<Disease> diseases = new ArrayList<Disease>();
                List<?> diseaseInstanceList = instance.getAttributeValuesList(ReactomeJavaConstants.disease);
                for (Object diseaseObject : diseaseInstanceList) {
                    Disease disease = new Disease();
                    GKInstance diseaseInstance = (GKInstance) diseaseObject;
                    disease.setName(getAttributeString(diseaseInstance, ReactomeJavaConstants.name));
                    disease.setSynonyms(getAttributes(diseaseInstance, ReactomeJavaConstants.synonym));
                    disease.setIdentifier(getAttributeString(diseaseInstance, ReactomeJavaConstants.identifier));
                    disease.setDatabase(getDatabase(diseaseInstance, disease.getIdentifier()));
                    diseases.add(disease);
                }
                return diseases;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new EnricherException(e.getMessage() , e);
            }
        }
        return null;
    }


    private Map<Long,GraphNode> nodeMap = new HashMap<Long, GraphNode>();

    private GraphNode getOrCreateTreeNode(GKInstance instance) throws Exception {
        GraphNode node = nodeMap.get(instance.getDBID());
        if (node == null) {
            node = new GraphNode();
            node.setName(instance.getDisplayName());
            node.setDbId(instance.getDBID());
            if (hasValue(instance, ReactomeJavaConstants.species)) {
                GKInstance species = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
                node.setSpecies(species.getDisplayName());
                node.setSpeciesId(species.getDBID());
            }            node.setStId(getStableIdentifier(instance));
            nodeMap.put(node.getDbId(), node);
        }
        return node;
    }

    private void nodeFromReference(GKInstance instance, GraphNode node,String fieldName) throws Exception {
        Collection<?> components = instance.getReferers(fieldName);
        if (components != null && !components.isEmpty()) {
            for (Object entryObject : components) {
                GKInstance entry = (GKInstance) entryObject;
                GraphNode newNode = getOrCreateTreeNode(entry);
                node.addChild(newNode);
                newNode.addParent(node);
                recursion(entry,newNode);
            }
        }
    }

    private void skippNodes(GKInstance instance, GraphNode node, String fieldName) throws Exception {
        Collection<?> regulator = instance.getReferers(fieldName);
        if (regulator != null && !regulator.isEmpty()) {
            for (Object entryObject : regulator) {
                GKInstance entry = (GKInstance) entryObject;
                recursion(entry,node);
            }
        }
    }

    private void nodeFromAttributes(GKInstance instance, GraphNode node, String fieldName) throws Exception {
        if (hasValues(instance, fieldName)) {
            GKInstance regulatedEntityInstance = (GKInstance) instance.getAttributeValue(fieldName);
            if (regulatedEntityInstance != null) {
                GraphNode newNode = getOrCreateTreeNode(regulatedEntityInstance);
                node.addChild(newNode);
                newNode.addParent(node);
                recursion(regulatedEntityInstance,newNode);
            }
        }
    }

    private void recursion(GKInstance instance, GraphNode node)  throws EnricherException {
        try {
            nodeFromReference(instance,node,ReactomeJavaConstants.hasComponent);
            nodeFromReference(instance,node,ReactomeJavaConstants.repeatedUnit);
            nodeFromReference(instance,node,ReactomeJavaConstants.hasCandidate);
            nodeFromReference(instance,node,ReactomeJavaConstants.hasMember);
            nodeFromReference(instance,node,ReactomeJavaConstants.input);
            nodeFromReference(instance,node,ReactomeJavaConstants.output);
            nodeFromReference(instance,node,ReactomeJavaConstants.hasEvent);
            nodeFromReference(instance,node,ReactomeJavaConstants.activeUnit);
            nodeFromReference(instance,node,ReactomeJavaConstants.catalystActivity);
            skippNodes(instance,node,ReactomeJavaConstants.regulator);
            skippNodes(instance,node,ReactomeJavaConstants.physicalEntity);
            nodeFromAttributes(instance, node, ReactomeJavaConstants.regulatedEntity);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EnricherException(e.getMessage() , e);
        }
    }

    private Set<TreeNode> generateUrlFromLeaves(Set<GraphNode>leaves) throws EnricherException {
        Set<TreeNode> topLvlTrees = new HashSet<TreeNode>();
        for (GraphNode leaf : leaves) {
            TreeNode tree = getTreeFromGraphLeaf(leaf, "", "");
            if(tree!=null){
                topLvlTrees.add(tree);
            }else{
                logger.error("Could no process tree for " + leaf.getName());
            }
        }
        return topLvlTrees;
    }
    private TreeNode getTreeFromGraphLeaf(GraphNode leaf, String diagram, String path) {
        TreeNode tree = new TreeNode();
        tree.setName(leaf.getName());
        tree.setSpecies(leaf.getSpecies());

        String id = null;
        try {
            if (hasDiagram(leaf.getDbId())) {
                diagram = String.valueOf(leaf.getDbId());
            } else {
                id = String.valueOf(leaf.getDbId());
            }
        } catch (EnricherException e) {
            logger.error(e.getMessage(), e);
        }
        if(diagram.isEmpty()) return null;

        String url = this.PATHWAY_BROWSER_URL;
        if (leaf.getSpecies() != null && !leaf.getSpecies().contains("Homo sapiens")) {
            url += "SPECIES=" + leaf.getSpeciesId();
        }
        if (url.endsWith("#")) {
            url += "DIAGRAM=" + diagram;
        } else {
            url += "&amp;DIAGRAM=" + diagram;
        }
        if(id!=null && !id.isEmpty()){
            url += "&amp;ID=" + id;
        }
        if(!path.isEmpty()) {
            url += "&amp;PATH=" + path;
        }
        url = url.replaceAll(diagram + "[^"+ diagram + "]*$", "");
        if (!diagram.isEmpty()) {
            tree.setUrl(url);
        }

        if (path.isEmpty()) {
            path += leaf.getDbId();
        } else {
            path += "," + leaf.getDbId();
        }


        if (leaf.getParent()!=null) {

            for (GraphNode node : leaf.getParent()) {
                tree.addChild(getTreeFromGraphLeaf(node,diagram,path));

            }

        }
        return tree;
    }
}