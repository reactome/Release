/*
 * Created on Aug 20, 2004
 */
package org.reactome.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.MySQLAdaptor.AttributeQueryRequest;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.AuthorToolAppletUtilities;
import org.junit.Test;

/**
 * 
 * @author wugm
 */
public class MySQLAdaptorTest {
    private MySQLAdaptor adaptor = null;
    
    public MySQLAdaptorTest() {
    }
    
    public void generateLocalSchema() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca", 
                                            "test_gk_central_new_schema_041212",
                                            "authortool",
                "T001test");
        Schema schema = dba.getSchema();
        AuthorToolAppletUtilities.saveLocalSchema(schema);
    }
    
    public void checkSchema() throws Exception {
        GKSchema schema = (GKSchema) adaptor.getSchema();
        Collection<?> attributes = schema.getAttributes();
        for (Object obj : attributes) {
            GKSchemaAttribute att = (GKSchemaAttribute) obj;
            System.out.println(att.getName());
        }
        String attName = "functionalStatusType";
        SchemaAttribute att1 = schema.getAttributeByName(attName);
        SchemaAttribute att2 = schema.getClassByName("FunctionalStatus").getAttribute(attName);
        System.out.println(att1 == att2);
    }
    
    public void testGetReleaseNumber() throws Exception {
        Integer releaseNumber = adaptor.getReleaseNumber();
        System.out.println("Release number in " + adaptor.getDBName() + ": " + releaseNumber);
    }
    
    public void testReferrers() throws Exception {
        SchemaClass cls = adaptor.getSchema().getClassByName(ReactomeJavaConstants.ReferenceDatabase);
        Collection c = cls.getReferers();
        for (Iterator it = c.iterator(); it.hasNext();) {
            SchemaAttribute att = (SchemaAttribute) it.next();
            System.out.println("Attribute in Referres: " + att.toString());
            Collection c1 = ((GKSchemaClass)cls).getReferersByName(att.getName());
            for (Iterator it1 = c1.iterator(); it1.hasNext();)
                System.out.println("\t" + it1.next());
        }
    }
    
    @Test
    public void queryExamples() throws Exception {
    	// Initialize a MySQLAdaptor, which is used to connect to a Reactome database
    	MySQLAdaptor dba = new MySQLAdaptor("localhost",
    								        "gk_current_ver45",
    								        "root",
    								        "macmysql01");
    	// Get instance of Homo sapien. Its DB_ID is 48887
    	GKInstance homoSapiens = dba.fetchInstance(48887L);
    	System.out.println("Homo sapiend: " + homoSapiens);
    	// Query Pathways in Homo sapien: i.e. pathways whose species attribute should be
    	// Homo sapiens
    	Collection<?> pathways = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway,
    													      ReactomeJavaConstants.species,
    													      "=",
    													      homoSapiens);
    	System.out.println("Total human pathways: " + pathways.size());
    	// Get the first human pathway and check its attribute
    	GKInstance firstPathway = (GKInstance) pathways.iterator().next();
    	System.out.println("First human pathway: " + firstPathway.getDisplayName() + " with DB_ID " + firstPathway.getDBID());
    	// Get how many sub-pathways this firstPathway has
    	List<?> hasEvent = firstPathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
    	System.out.println("\thasEvent: " + hasEvent.size());
    	for (Object obj : hasEvent) {
    		GKInstance subEvent = (GKInstance) obj;
    		System.out.println("\t" + subEvent); // GKInstance.toString should print out an instance's _displayName, DB_ID and schemaClass.
    	}
    }
    
    public void testQuery() {
        try {
            Collection collection = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.LiteratureReference,
                                                                     ReactomeJavaConstants.pages,
                                                                     "like",
                    "133-%");
            System.out.println("Query for literature based on startPage:\n" + collection);
            collection = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.LiteratureReference,
                                                          ReactomeJavaConstants.pages,
                                                          "like",
                    "%-63");
            System.out.println("Query for literature based on endPage:");
            for (Iterator it = collection.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                System.out.println(instance.toString());
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private String displayNameWithSpecies(GKInstance pathway) {
        return pathway.getDisplayName();
    }
    
    public void checkReactionQuery() throws Exception {
        String riceName = "Oryza sativa";
        Collection<?> c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species,
                                                           ReactomeJavaConstants._displayName, 
                                                           "=",
                                                           riceName);
        GKInstance rice = (GKInstance) c.iterator().next();
        System.out.println("Rice: " + rice);
        c = adaptor.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        System.out.println("Total reactions: " + c.size());
        c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReactionlikeEvent,
                                             ReactomeJavaConstants.species,
                                             "=",
                                             rice);
        System.out.println("Total reactions in rice: " + c.size());
        c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReactionlikeEvent,
                                             ReactomeJavaConstants.species,
                                             "!=",
                                             rice);
        System.out.println("Total reactions not in rice: " + c.size());
        c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Species,
                                             ReactomeJavaConstants.name, 
                                             "!=",
                                             riceName);
        System.out.println("Total species not rice: " + c.size());
        c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReactionlikeEvent,
                                             ReactomeJavaConstants.species,
                                             "=", 
                                             c);
        System.out.println("Based on the above query: " + c.size());
        GKInstance inst = (GKInstance) c.iterator().next();
        GKInstance species = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.species);
        System.out.println("Species in the first reaction: " + species);
    }
    
    /**
     * Test for Willem
     * @return
     */
    public ArrayList getPathwaysFromDatabaseUnitTest(){
        ArrayList returnList =  new ArrayList();
        ArrayList pathwayTree = new ArrayList();
        Hashtable pathway2Parent = new Hashtable();
        Hashtable pathway2Values = new Hashtable();
        ArrayList pathwayInstances = new ArrayList();
        
        try{
            // Get Pathways that have no hasComponent references
            Collection pathways = adaptor.fetchInstancesByClass("Pathway");
            for(Iterator pi = pathways.iterator(); pi.hasNext();){
                GKInstance pathway = (GKInstance)pi.next();
                Collection components = pathway.getReferers("hasComponent");
                if(components != null){
                    pathwayInstances.add(pathway);
                    ArrayList pathwayNames = new ArrayList();
                    pathwayNames.add(displayNameWithSpecies(pathway));
                    pathwayTree.add(pathwayNames);
                }
            }
            // get the rest of the pathways as well
            String[] attributeNames = new String[]{"hasComponent"};
            adaptor.loadInstanceAttributeValues(pathwayInstances, attributeNames);
            for(int pi = 0; pi < pathwayInstances.size(); pi++){
                GKInstance pathway = (GKInstance)pathwayInstances.get(pi);
                Collection components =
                        pathway.getAttributeValuesList("hasComponent");
                if(components.isEmpty() == false){
                    for(Iterator ci = components.iterator(); ci.hasNext();){
                        GKInstance component = (GKInstance)ci.next();
                        
                        if(component.getSchemClass().getName().equalsIgnoreCase("Pathway")){
                            // This component is a pathway
                            ArrayList componentsList = new ArrayList();
                            componentsList.add(component);
                            
                            adaptor.loadInstanceAttributeValues((Collection)componentsList,
                                                                attributeNames);
                            pathwayInstances.addAll(componentsList);
                            
                            pathway2Parent.put(displayNameWithSpecies(component),
                                               displayNameWithSpecies(pathway));
                            // add new entry to to pathwayTree list
                            ArrayList pathwayNames = new ArrayList();
                            pathwayNames.add(displayNameWithSpecies(component));
                            pathwayNames.add(displayNameWithSpecies(pathway));
                            pathwayTree.add(pathwayNames);
                        }
                    }
                }
            }
            // For each GKInstance in pathwayInstances get the amount
            //of reactions in the pathway
            // add this amount to pathway2Values and then the 0th
            //value. For this pathway, but also for it's parents
            for(Iterator ii = pathwayInstances.iterator(); ii.hasNext();){
                GKInstance pathway = (GKInstance)ii.next();
                int reactionCounter = 0;
                int reactionsMapped = 0;
                Collection components =
                        pathway.getAttributeValuesList("hasComponent");
                if(components.isEmpty() == false){
                    for(Iterator ci = components.iterator(); ci.hasNext();){
                        GKInstance component = (GKInstance)ci.next();
                        
                        if(component.getSchemClass().getName().equalsIgnoreCase("Reaction")){
                            // This component is a reaction
                            reactionCounter++;
                        }
                    }
                }
                
                ArrayList parentPathways = new ArrayList();
                parentPathways.add(displayNameWithSpecies(pathway));
                for(int it = 0; it < parentPathways.size(); it++){
                    String pathwayString = (String)parentPathways.get(it);
                    if (pathway2Parent.containsKey(pathwayString)){
                        String parent =
                                (String)pathway2Parent.get(pathwayString);
                        parentPathways.add(parent);
                    }
                    if(pathway2Values.containsKey(pathwayString)){
                        ArrayList values =
                                (ArrayList)pathway2Values.get(pathwayString);
                        int amountReaction =
                                Integer.parseInt((String)values.get(0));
                        int total = amountReaction + reactionCounter;
                        ArrayList valuesToAdd =  new ArrayList();
                        valuesToAdd.add(0, String.valueOf(total));
                        valuesToAdd.add(String.valueOf(reactionsMapped));
                        //pathway2Values.remove(pathwayString);
                        pathway2Values.put(pathwayString, valuesToAdd);
                    }
                    else{
                        ArrayList values = new ArrayList();
                        values.add(String.valueOf(reactionCounter));
                        values.add(String.valueOf(reactionsMapped));
                        pathway2Values.put(pathwayString, values);
                    }
                }
            }
        } catch(Exception e){
            System.out.println("6: Error fetching pathways: " + e);
        }
        
        // return pathwayTree, pathway2Parent and pathway2Values.
        returnList.add(pathwayTree);
        returnList.add(pathway2Parent);
        returnList.add(pathway2Values);
        
        return returnList;
    }
    
    @Test
    public void testReverseQuery() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca", 
                                            "test_reactome_41", 
                                            "authortool", 
                                            "T001test");
        List<AttributeQueryRequest> queries = new ArrayList<MySQLAdaptor.AttributeQueryRequest>();
        
        // Get ReferenceGeneProduct for the specified gene id
        Collection<GKInstance> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                                ReactomeJavaConstants.otherIdentifier,
                                                                "=", 
                                                                "EntrezGene:24153");
        assert(c.size() == 1);
        GKInstance rgp = c.iterator().next();
        // Get PhysicalEntity using the returned ReferenceGeneProduct
        c = rgp.getReferers(ReactomeJavaConstants.referenceEntity);
        // Multiple PhysicalEntity instances may be returned
        System.out.println("PhysicalEntity: " + c.size());
        for (GKInstance pe : c)
            System.out.println(pe);
        // Get Event that use returned PhysicalEntitiy instances
        Set<GKInstance> events = new HashSet<GKInstance>();
        String[] attributeNames = new String[] {
                ReactomeJavaConstants.hasComponent,
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.hasCandidate,
                ReactomeJavaConstants.repeatedUnit,
                ReactomeJavaConstants.input,
                ReactomeJavaConstants.output,
                ReactomeJavaConstants.physicalEntity,
                ReactomeJavaConstants.catalystActivity,
                ReactomeJavaConstants.regulator
        };
        Set<GKInstance> current = new HashSet<GKInstance>();
        Set<GKInstance> next = new HashSet<GKInstance>();
        for (GKInstance pe : c) {
            current.clear();
            next.clear();
            current.add(pe);
            while (current.size() > 0) {
                for (GKInstance inst : current) {
                    for (String attName : attributeNames) {
                        Collection<GKInstance> tmp = inst.getReferers(attName);
                        if (tmp == null || tmp.size() == 0)
                            continue;
                        for (GKInstance referrer : tmp) {
                            if (referrer.getSchemClass().isa(ReactomeJavaConstants.Event))
                                events.add(referrer);
                            else
                                next.add(referrer);
                        }
                    }
                }
                current.clear();
                current.addAll(next);
                next.clear();
            }
        }
        System.out.println("Events: " + events.size());
        List<GKInstance> instanceList = new ArrayList<GKInstance>(events);
        InstanceUtilities.sortInstances(instanceList);
        for (GKInstance event : instanceList)
            System.out.println(event);
    }
}
