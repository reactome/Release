/*
 * Created on Apr 28, 2005
 */
package org.gk.scripts;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * This class is used to extract lists of ReferencePeptideSequences contained in Pathways. 
 * The program works like the following:
 * 1. All human Pathways are fetched.
 * 2. All PhysicalEntities in Pathways are extracted by (Only SequenceEntities are taken):
 *     1). Values in input, output slots for Pathways.
 *     2). hasComponent slot are checked 
 *     3). If a value in hasComponent list is a Reaction, get input, output, catalystActivities
 *     4). If a value in hasComponent list is a pathway, go to 2.
 *     5). Check Regulation if its regulatedEntity is the checked one. This should be applied to
 * reaction too. If true, add the regulator.
 * 3. Extract all subunits for Complex
 * 4. Map PhysicalEntity to ReferencePeptideSequence.
 * 5. Output the results.
 * @author wgm
 */
public class GeneInPathwayExtracter {
    private MySQLAdaptor dba;
    private Map pathwayMap;
    private Map pathwayEntitiesMap;
    private Map reactionMap; // To catch reaction result
    private Map complexMap;
    private Map entitySetMap;
    private Collection regulations;
    
    public GeneInPathwayExtracter() {        
        pathwayMap = new HashMap();
        reactionMap = new HashMap();
        complexMap = new HashMap();
        entitySetMap = new HashMap();
        pathwayEntitiesMap = new HashMap();
    }
    
    public void extractPathwaysInHuman() throws Exception {
        if (dba == null)
            throw new IllegalStateException("GeneInPathwayExtracter.extract(): " +
                                            "No database connection specified");
        // Load instances to be used
        Long humanTaxonID = new Long(48887);
        extract(humanTaxonID);
    }
    
    /**
     * Extract all genes in pathways in a species whose DB_ID is specied. If speciesId is null, pathways in all
     * species will be queried.
     * @param speciesId
     * @throws Exception
     */
    public void extract(Long speciesId) throws Exception {
        Collection pathways = null;
        Collection reactions = null;
        Collection complexes = null;
        Collection entitySets = null;
        if (speciesId != null) {
            pathways = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway,
                                                    ReactomeJavaConstants.species,
                                                    "=",
                                                    speciesId);
            reactions = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Reaction, 
                                                     ReactomeJavaConstants.species,
                                                     "=",
                                                     speciesId);
            complexes = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Complex,
                                                     ReactomeJavaConstants.species,
                                                     "=",
                                                     speciesId);
            entitySets = dba.fetchInstanceByAttribute(ReactomeJavaConstants.EntitySet,
                                                      ReactomeJavaConstants.species,
                                                      "=",
                                                      speciesId);
        }
        else {
            pathways = dba.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
            reactions = dba.fetchInstancesByClass(ReactomeJavaConstants.Reaction);
            complexes = dba.fetchInstancesByClass(ReactomeJavaConstants.Complex);
            entitySets = dba.fetchInstancesByClass(ReactomeJavaConstants.EntitySet);
        }
        Collection catalystActivities = dba.fetchInstancesByClass(ReactomeJavaConstants.CatalystActivity);
        regulations = dba.fetchInstancesByClass(ReactomeJavaConstants.Regulation);
        Collection sequenceEntities = dba.fetchInstancesByClass(ReactomeJavaConstants.EntityWithAccessionedSequence);
        // Load attributes to be used
        Schema schema = dba.getSchema();
        SchemaClass pathwayCls = schema.getClassByName(ReactomeJavaConstants.Pathway);
        SchemaAttribute att = pathwayCls.getAttribute(ReactomeJavaConstants.hasComponent);
        dba.loadInstanceAttributeValues(pathways, att);
        att = pathwayCls.getAttribute(ReactomeJavaConstants.input);
        dba.loadInstanceAttributeValues(pathways, att);
        att = pathwayCls.getAttribute(ReactomeJavaConstants.output);
        dba.loadInstanceAttributeValues(pathways, att);
        // Load inputs, outputs and catalyistActivies for Reactions
        SchemaClass reactionCls = schema.getClassByName(ReactomeJavaConstants.Reaction);
        att = reactionCls.getAttribute(ReactomeJavaConstants.input);
        dba.loadInstanceAttributeValues(reactions, att);
        att = reactionCls.getAttribute(ReactomeJavaConstants.output);
        dba.loadInstanceAttributeValues(reactions, att);
        att = reactionCls.getAttribute(ReactomeJavaConstants.catalystActivity);
        dba.loadInstanceAttributeValues(reactions, att);
        // load physicalEntity for CatalystActivies
        SchemaClass caCls = schema.getClassByName(ReactomeJavaConstants.CatalystActivity);
        att = caCls.getAttribute(ReactomeJavaConstants.physicalEntity);
        dba.loadInstanceAttributeValues(catalystActivities, att);
        // load hasComponents for complexes
        SchemaClass complexCls = schema.getClassByName(ReactomeJavaConstants.Complex);
        att = complexCls.getAttribute(ReactomeJavaConstants.hasComponent);
        dba.loadInstanceAttributeValues(complexes, att);
        // load hasMember for EntitySet
        SchemaClass entitySetCls = schema.getClassByName(ReactomeJavaConstants.EntitySet);
        att = entitySetCls.getAttribute(ReactomeJavaConstants.hasMember);
        dba.loadInstanceAttributeValues(entitySets, att);
        // load regulator, regulatedEntity for Regulations
        SchemaClass regulationCls = schema.getClassByName(ReactomeJavaConstants.Regulation);
        att = regulationCls.getAttribute(ReactomeJavaConstants.regulator);
        dba.loadInstanceAttributeValues(regulations, att);
        att = regulationCls.getAttribute(ReactomeJavaConstants.regulatedEntity);
        dba.loadInstanceAttributeValues(regulations, att);
        // load referenceEntity for SequenceEntities
        SchemaClass sequenceEntityCls = schema.getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
        att = sequenceEntityCls.getAttribute(ReactomeJavaConstants.referenceEntity);
        dba.loadInstanceAttributeValues(sequenceEntities, att);
        // Do a clean up for regulation: only regulator is SequenceEntity or Complex
        // are needed
        cleanUpRegulations(regulations);
        // Get SequenceEntities for Complex
        GKInstance complex = null;
        for (Iterator it = complexes.iterator(); it.hasNext();) {
            complex = (GKInstance) it.next();
            extractComplex(complex);
        }
        // Get SequenceEntities for EntitySet
        GKInstance entitySet = null;
        for (Iterator it = entitySets.iterator(); it.hasNext();) {
            entitySet = (GKInstance) it.next();
            extractEntitySet(entitySet);
        }
        // Get SequenceEntities and Complexes for Pathways
        GKInstance pathway = null;
        for (Iterator it = pathways.iterator(); it.hasNext();) {
            pathway = (GKInstance) it.next();
            extractPathway(pathway);
        }
        // Convert Complexes to SequenceEntities
        for (Iterator it = pathwayMap.keySet().iterator(); it.hasNext();) {
            pathway = (GKInstance) it.next();
            Set entities = (Set) pathwayMap.get(pathway);
            Set newSet = new HashSet();
            for (Iterator it1 = entities.iterator(); it1.hasNext();) {
                GKInstance entity = (GKInstance) it1.next();
                if (entity.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                    Set complexSet = (Set) complexMap.get(entity);
                    if (complexSet != null && complexSet.size() > 0)
                        newSet.addAll(complexSet);
                }
                else if (entity.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                    Set entitySetEntities = (Set) entitySetMap.get(entity);
                    if (entitySetEntities != null && entitySetEntities.size() > 0)
                        newSet.addAll(entitySetEntities);
                }
                else 
                    newSet.add(entity);
            }
            if (newSet.size() > 0)
                pathwayEntitiesMap.put(pathway, newSet);
        }
    }
    
    private void extractEntitySet(GKInstance entitySet) throws Exception {
        if (entitySetMap.containsKey(entitySet))
            return;
        Set setEntities = new HashSet();
        List members = entitySet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
        for (Iterator it = members.iterator(); it.hasNext();) {
            GKInstance member = (GKInstance) it.next();
            if (member.getSchemClass().isa(ReactomeJavaConstants.Complex))
                extractComplex(member, setEntities);
            else if (member.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                extractEntitySet(member, setEntities);
            else if (member.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
                setEntities.add(member);
        }
        entitySetMap.put(entitySet, setEntities);
    }
    
    private void extractEntitySet(GKInstance entitySet, Set setEntities) throws Exception {
        if (!entitySetMap.containsKey(entitySet)) 
            extractEntitySet(entitySet);
        Set entities = (Set) entitySetMap.get(entitySet);
        setEntities.addAll(entities);
    }
    
    private void extractComplex(GKInstance complex) throws Exception {
        if (complexMap.containsKey(complex))
            return;
        Set complexEntities = new HashSet();
        List subunits = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        for (Iterator it = subunits.iterator(); it.hasNext();) {
            GKInstance subunit = (GKInstance) it.next();
            if (subunit.getSchemClass().isa(ReactomeJavaConstants.Complex))
                extractComplex(subunit, complexEntities);
            else if (subunit.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                extractEntitySet(subunit, complexEntities);
            else if (subunit.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
                complexEntities.add(subunit);
        }
        complexMap.put(complex, complexEntities);
    }
    
    private void extractComplex(GKInstance complex, Set containedComplexEntities) throws Exception {
        if (!complexMap.containsKey(complex)) 
            extractComplex(complex);
        Set entities = (Set) complexMap.get(complex);
        containedComplexEntities.addAll(entities);
    }
    
    private void extractPathway(GKInstance pathway) throws Exception {
        if (pathwayMap.containsKey(pathway)) {
           return; 
        }
        Set pathwayEntities = new HashSet();
        List inputs = pathway.getAttributeValuesList(ReactomeJavaConstants.input);
        extractPathwayEntities(inputs, pathwayEntities);
        List outputs = pathway.getAttributeValuesList(ReactomeJavaConstants.output);
        extractPathwayEntities(outputs, pathwayEntities);
        // Need to check "hasComponent" now
        List hasComponents = pathway.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        for (Iterator it = hasComponents.iterator(); it.hasNext();) {
            GKInstance event = (GKInstance) it.next();
            if (event.getSchemClass().isa(ReactomeJavaConstants.Reaction))
                extractReaction(event, pathwayEntities);
            else
                extractPathway(event, pathwayEntities);
        }
        GKInstance regulator = checkRegulation(pathway);
        if (regulator != null)
            pathwayEntities.add(regulator);
        pathwayMap.put(pathway, pathwayEntities);
    }
    
    /**
     * Check regulation to see if regulator should be in the event list
     */
    private GKInstance checkRegulation(GKInstance event) throws Exception {
        GKInstance regulation = null;
        GKInstance regulatedEntity = null;
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            regulatedEntity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
            if (regulatedEntity == event) {
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                return regulator;
            }
        }
        return null;
    }
    
    private void cleanUpRegulations(Collection regulations) throws Exception {
        GKInstance regulation = null;
        GKInstance regulator = null;
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
            if (regulator == null) {
                it.remove();
                continue;
            }
            if (!regulator.getSchemClass().isa(ReactomeJavaConstants.Complex) &&
                !regulator.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
                it.remove();
                continue;
            }
        }
    }
    
    private void extractReaction(GKInstance reaction, Set containerPathwayEntities) throws Exception {
        Set entities = (Set) reactionMap.get(reaction);
        if (entities != null) {
            containerPathwayEntities.addAll(entities);
            return;
        }
        Set reactionEntities = new HashSet();
        List values = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        extractPathwayEntities(values, reactionEntities);
        values = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        extractPathwayEntities(values, reactionEntities);
        values = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (values != null && values.size() > 0) {
            GKInstance tmp = null;
            for (Iterator it = values.iterator(); it.hasNext();) {
                tmp = (GKInstance) it.next();
                GKInstance regulator = checkRegulation(tmp);
                if (regulator != null)
                    reactionEntities.add(regulator);
                // Need to check physicalEntity
                GKInstance entity = (GKInstance) tmp.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (entity == null)
                    continue;
                if (entity.getSchemClass().isa(ReactomeJavaConstants.Complex))
                    reactionEntities.add(entity);
                else if (entity.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                    reactionEntities.add(entity);
                else if (entity.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
                    reactionEntities.add(entity);
            }
        }
        GKInstance regulator = checkRegulation(reaction);
        if (regulator != null)
            reactionEntities.add(regulator);
        reactionMap.put(reaction, reactionEntities);
        containerPathwayEntities.addAll(reactionEntities);
    }
    
    private void extractPathway(GKInstance pathway, Set containerPathwayEntities) throws Exception {
        if (!pathwayMap.containsKey(pathway))
            extractPathway(pathway);
        Set entities = (Set) pathwayMap.get(pathway);
        containerPathwayEntities.addAll(entities);
    }
    
    /**
     * Either SequenceEntities or Complexes should be pulled out.
     * @param entities
     * @param pathwayEntities
     */
    private void extractPathwayEntities(List entities, Set pathwayEntities) {
        if (entities == null || entities.size() == 0)
            return;
        GKInstance entity = null;
        for (Iterator it = entities.iterator(); it.hasNext();) {
            entity = (GKInstance) it.next();
            if (entity.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
                pathwayEntities.add(entity);
            else if (entity.getSchemClass().isa(ReactomeJavaConstants.Complex))
                pathwayEntities.add(entity);
            else if (entity.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                pathwayEntities.add(entity);
        }
    }
    
    public void output(String fileName) throws Exception {
        GKInstance pathway = null;
        Set entities = null;
        FileWriter fileWriter = new FileWriter(fileName);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        StringBuffer buffer = new StringBuffer();
        GKInstance species = null;
        for (Iterator it = pathwayEntitiesMap.keySet().iterator(); it.hasNext();) {
            pathway = (GKInstance) it.next();
            entities = (Set) pathwayEntitiesMap.get(pathway);
            buffer.setLength(0); // reset
            species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
            buffer.append(species.getDisplayName());
            buffer.append("\t");
            buffer.append(pathway.getDisplayName());
            buffer.append("[");
            buffer.append(pathway.getDBID());
            buffer.append("]");
            buffer.append("\t");
            for (Iterator it1 = entities.iterator(); it1.hasNext();) {
                GKInstance entity = (GKInstance) it1.next();
                GKInstance referenceEntity = (GKInstance) entity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                if (referenceEntity == null)
                    continue;
                String displayName = referenceEntity.getDisplayName();
                //buffer.append(getUniProtID(displayName));
                buffer.append(displayName);
                buffer.append("[");
                buffer.append(referenceEntity.getDBID());
                buffer.append("]");
                if (it1.hasNext())
                    buffer.append("\t");
            }
            printWriter.println(buffer.toString());
        }
        printWriter.close();
        fileWriter.close();
    }
    
//    private String getUniProtID(String displayName) {
//        int index = displayName.indexOf(" ");
//        if (index < 0) 
//            index = displayName.length();
//        int index1 = displayName.indexOf(":");
//        if (index1 < 0) {
//            index1 = -1;
//            System.out.println("DisplayName has not colon: " + displayName);
//        }
//        System.out.println("DisplayName: " + displayName);
//        return displayName.substring(index1 + 1, index);
//    }
    
    public static void main(String[] args) {
        try {
            MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                                "gk_current_ver17",
                                                "root",
                                                "macmysql01",
                                                3306);
            GeneInPathwayExtracter extracter = new GeneInPathwayExtracter();
            extracter.dba = dba;
            // This is for mouse
            Long mouseId = new Long(48892L);
            //extracter.extract(mouseId);
            //extracter.output("MouseGenesInPathwaysVer17.txt");
            extracter.extract(null);
            extracter.output("GeneInPathwayForAll.txt");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
