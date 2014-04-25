/*
 * Created on Nov 9, 2011
 *
 */
package org.gk.scripts;

import java.awt.Point;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;
import org.gk.util.FileUtilities;
import org.junit.Test;

//import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
//import uk.ac.ebi.kraken.interfaces.uniprot.features.ChainFeature;
//import uk.ac.ebi.kraken.interfaces.uniprot.features.FeatureLocation;
//import uk.ac.ebi.kraken.interfaces.uniprot.features.FeatureType;
//import uk.ac.ebi.kraken.uuw.services.remoting.EntryRetrievalService;
//import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;

/**
 * A list of scripts related to attribute value checking.
 * @author gwu
 *
 */
@SuppressWarnings("unchecked")
public class AttributesChecker {
    
    public AttributesChecker() {
    }
    
    public static void main(String[] args) {
        try {
            new AttributesChecker().fixEWASDuplicatedNames();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Update _displayNames for EntityFunctionalStatus instances
     * @throws Exception
     */
    @Test
    public void checkEntityFunctionalStatusName() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomecurator.oicr.on.ca", 
                                            "gk_central", 
                                            "authortool", 
                                            "***REMOVED***");
        Collection<GKInstance> efs = dba.fetchInstancesByClass(ReactomeJavaConstants.EntityFunctionalStatus);
        System.out.println("Total EntityFunctionalStatus: " + efs.size());
        System.out.println("DB_ID\tCurrent\tCorrect");
        List<GKInstance> toBeChanged = new ArrayList<GKInstance>();
        for (GKInstance inst : efs) {
            String oldName = inst.getDisplayName();
            String newName = InstanceDisplayNameGenerator.generateDisplayName(inst);
            if (oldName.equals(newName))
                continue;
            System.out.println(inst.getDBID() + "\t" + oldName + "\t" + newName);
            inst.setDisplayName(newName);
            toBeChanged.add(inst);
        }
        System.out.println("toBeChanged: " + toBeChanged.size());
        ScriptUtilities.updateInstanceNames(dba, toBeChanged);
    }
    
    /**
     * This method is used to check if there is any name duplication in EWASes in a database.
     * @throws Exception
     */
    @Test
    @SuppressWarnings("unchecked")
    public void fixEWASDuplicatedNames() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomecurator.oicr.on.ca", 
                                            "gk_central", 
                                            "authortool", 
                                            "***REMOVED***");
//        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
//                                            "gk_central_050313", 
//                                            "root", 
//                                            "macmysql01");
        List<Long> dbIds = fetchIDsForNameDuplicatedEWASes(dba);
        List<GKInstance> toBeUpdated = new ArrayList<GKInstance>();
        int total = 0;
        for (Long dbId : dbIds) {
//            if (!dbId.equals(888582L))
//                continue;
            GKInstance inst = dba.fetchInstance(dbId);
            List<String> names = inst.getAttributeValuesList(ReactomeJavaConstants.name);
            Set<String> nameSet = new HashSet<String>(names);
            if (nameSet.size() == names.size())
                continue;
            // Delete names that have been duplicated
            while (names.size() > nameSet.size()) {
                // Delete from list
                int index = -1;
                for (int i = 0; i < names.size() - 1; i++) {
                    String current = names.get(i);
                    boolean hasFound = false;
                    for (int j = i + 1; j < names.size(); j++) {
                        String next = names.get(j);
                        if (next.equals(current)) {
                            index = j;
                            hasFound = true;
                            break;
                        }
                    }
                    if (hasFound)
                        break;
                }
                if (index >= 0)
                    names.remove(index);
            }
            inst.setAttributeValue(ReactomeJavaConstants.name, names);
//            String oldDisplayName = inst.getDisplayName();
//            String newDisplayName = InstanceDisplayNameGenerator.generateDisplayName(inst);
//            if (!oldDisplayName.equals(newDisplayName)) {
//                throw new IllegalStateException(inst + " needs to change its _dispalyName!");
//            }
//            System.out.println(inst);
            total ++;
            toBeUpdated.add(inst);
        }
//        ScriptUtilities.updateInstanceNames(dba, toBeUpdated);
        System.out.println("Total updated: " + total);
//        dba = new MySQLAdaptor("reactomecurator.oicr.on.ca", 
//                             "gk_central", 
//                             "authortool", 
//                             "***REMOVED***");
//        List<Long> gkcentralIds = fetchIDsForNameDuplicatedEWASes(dba);
//        gkcentralIds.removeAll(localIds);
//        System.out.println("New DBIDs: " + gkcentralIds.size());
//        for (Long dbId : gkcentralIds)
//            System.out.println(dbId);
    }

    private List<Long> fetchIDsForNameDuplicatedEWASes(MySQLAdaptor dba) throws Exception, InvalidAttributeException {
        // For human proteins only
        GKInstance human = dba.fetchInstance(48887L);
        Collection<GKInstance> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.EntityWithAccessionedSequence,
                                                                ReactomeJavaConstants.species, 
                                                                "=",
                                                                human);
        System.out.println("Total human EWASes: " + c.size());
        dba.loadInstanceAttributeValues(c, new String[]{ReactomeJavaConstants.name});
        int total = 0;
        List<Long> dbIds = new ArrayList<Long>();
        for (GKInstance inst : c) {
            List<String> nameList = inst.getAttributeValuesList(ReactomeJavaConstants.name);
            Set<String> nameSet = new HashSet<String>(nameList);
            if (nameSet.size() < nameList.size()) {
//                System.out.println(inst);
                dbIds.add(inst.getDBID());
                total ++;
            }
        }
        System.out.println("Total EWASes having duplicated names: " + total);
        return dbIds;
    }
    
    /**
     * Check if the same Modification is used multiple times in a EWAS.
     * @throws Exception
     */
    @Test
    public void checkEWASModificatiosn() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomecurator.oicr.on.ca", 
                                            "gk_central", 
                                            "authortool", 
                                            "***REMOVED***");
        Connection connection = dba.getConnection();
        String query = "SELECT DB_ID, hasModifiedResidue FROM EntityWithAccessionedSequence_2_hasModifiedResidue";
        Statement stat = connection.createStatement();
        ResultSet result = stat.executeQuery(query);
        Map<Long, List<Long>> ewasIdToModIds = new HashMap<Long, List<Long>>();
        while (result.next()) {
            Long ewasId = result.getLong(1);
            Long modId = result.getLong(2);
            List<Long> list = ewasIdToModIds.get(ewasId);
            if (list == null) {
                list = new ArrayList<Long>();
                ewasIdToModIds.put(ewasId, list);
            }
            list.add(modId);
        }
        result.close();
        stat.close();
        connection.close();
        int total = 0;
        for (Long ewasId : ewasIdToModIds.keySet()) {
            List<Long> list = ewasIdToModIds.get(ewasId);
            Set<Long> set = new HashSet<Long>(list);
            if (list.size() == set.size())
                continue;
            System.out.println(ewasId);
            total ++;
        }
        System.out.println("Total: " + total);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void updateEWASCoordiantes() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomecurator.oicr.on.ca", 
                                            "gk_central", 
                                            "authortool", 
                                            "***REMOVED***");
        // Homo sapiens
        GKInstance human = dba.fetchInstance(48887L);
        Collection<GKInstance> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.EntityWithAccessionedSequence,
                                                                ReactomeJavaConstants.species,
                                                                "=",
                                                                human);
        System.out.println("Total human EWASes: " + c.size());
        dba.loadInstanceAttributeValues(c, 
                                        new String[]{ReactomeJavaConstants.startCoordinate,
                ReactomeJavaConstants.endCoordinate,
                ReactomeJavaConstants.referenceEntity});
        Map<String, int[]> uniIdToChain = loadUniProtToChain();
        int noChainCase = 0;
        int needChangeCase = 0;
        try {
            dba.startTransaction();
            FileUtilities fu = new FileUtilities();
            fu.setOutput("EWASCoordinateUpdateLogging.txt");
            fu.printLine("DB_ID\tDisplayName\tOld_Start\tOld_End\tNew_Start\tNew_End");
            Long defaultPersonId = 140537L; // For Guanming Wu at CSHL
            GKInstance defaultIE = ScriptUtilities.createDefaultIE(dba, defaultPersonId, true);
            for (GKInstance ewas : c) {
                boolean isChanged = false;
                GKInstance refEntity = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                if (refEntity == null)
                    continue; // Don't need to do anything
                String identifier = (String) refEntity.getAttributeValue(ReactomeJavaConstants.identifier);
                if (identifier == null)
                    continue;
                int[] chain = uniIdToChain.get(identifier);
                if (chain == null) {
                    noChainCase ++;
                    //                System.err.println(identifier + " doesn't have a chain!");
                    continue;
                }
                Integer start = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.startCoordinate);
                Integer end = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.endCoordinate);
                if (start == null)
                    isChanged = true;
                if (end == null)
                    isChanged = true;
                if (start != null && end != null && start == 1 && end == -1)
                    isChanged = true;
                if (start != null && end != null && start == -1 && end == -1)
                    isChanged = true;
                if (isChanged) {
                    ewas.setAttributeValue(ReactomeJavaConstants.startCoordinate,
                                           chain[0]);
                    ewas.setAttributeValue(ReactomeJavaConstants.endCoordinate,
                                           chain[1]);
                    dba.updateInstanceAttribute(ewas, ReactomeJavaConstants.startCoordinate);
                    dba.updateInstanceAttribute(ewas, ReactomeJavaConstants.endCoordinate);
                    List<GKInstance> modified = ewas.getAttributeValuesList(ReactomeJavaConstants.modified);
                    ewas.addAttributeValue(ReactomeJavaConstants.modified, defaultIE);
                    dba.updateInstanceAttribute(ewas, ReactomeJavaConstants.modified);
                    fu.printLine(ewas.getDBID() + "\t" + ewas.getDisplayName() + "\t" + 
                                 start + "\t" + end + "\t" +
                                 chain[0] + "\t" + chain[1]);
                    needChangeCase ++;
                }
            }
            fu.printLine("No chain available: " + noChainCase);
            fu.printLine("Total changed: " + needChangeCase);
            fu.close();
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
            e.printStackTrace();
        }
    }
    
    @Test
    public void checkEWASCoordinates() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_central_021213", 
                                            "root", 
                                            "macmysql01");
        // Homo sapiens
        GKInstance human = dba.fetchInstance(48887L);
        Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.EntityWithAccessionedSequence,
                                                       ReactomeJavaConstants.species,
                                                       "=",
                                                       human);
        System.out.println("Total human EWASes: " + c.size());
        dba.loadInstanceAttributeValues(c, 
                                        new String[]{ReactomeJavaConstants.startCoordinate, ReactomeJavaConstants.endCoordinate});
        int count = 0;
        int defaultCase = 0;
        int endDefaultCase = 0;
        for (Object obj : c) {
            GKInstance ewas = (GKInstance) obj;
            Integer start = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.startCoordinate);
            Integer end = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.endCoordinate);
            if (start == null || start == -1 ||
                end == null || end == -1 || end == 1) {
                if (count < 100)
                    System.out.println(ewas);
                count ++;
            }
            if (start != null && start == 1 && end != null && end == -1)
                defaultCase ++;
            if (end != null && end == -1)
                endDefaultCase ++;
        }
        System.out.println("To be fixed: " + count);
        System.out.println("Default cases: " + defaultCase);
        System.out.println("endCoordiante == -1: " + endDefaultCase);
    }
    
    @Test
    public void checkComplexCompartments() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central_121511",
                                            "root",
                                            "macmysql01");
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.Complex);
        SchemaClass complexCls = dba.getSchema().getClassByName(ReactomeJavaConstants.Complex);
        dba.loadInstanceAttributeValues(c, complexCls.getAttribute(ReactomeJavaConstants.compartment));
        List<GKInstance> list = new ArrayList<GKInstance>();
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            List<?> values = inst.getAttributeValuesList(ReactomeJavaConstants.compartment);
            if (values != null && values.size() > 1)
                list.add(inst);
        }
        InstanceUtilities.sortInstances(list);
        System.out.println("Complexes having more than one compartments: " + list.size());
        for (GKInstance inst : list)
            System.out.println(inst);
    }
    
    @Test
    public void grepReactionsWithMultipleSpecies() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "gk_central",
                                            "authortool",
                                            "***REMOVED***");
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        SchemaClass cls = dba.getSchema().getClassByName(ReactomeJavaConstants.ReactionlikeEvent);
        dba.loadInstanceAttributeValues(c, cls.getAttribute(ReactomeJavaConstants.species));
        // Disease
        GKInstance disease = dba.fetchInstance(1643685L);
        Set<GKInstance> components = InstanceUtilities.grepPathwayEventComponents(disease);
        c.removeAll(components);
        for (Object obj : c) {
            GKInstance inst = (GKInstance) obj;
            List<?> species = inst.getAttributeValuesList(ReactomeJavaConstants.species);
            if (species != null && species.size() > 1)
                System.out.println(inst + "\t" + species.size());
        }
    }
    
    public Map<String, int[]> loadUniProtToChain() throws IOException {
        Map<String, int[]> rtn = new HashMap<String, int[]>();
        FileUtilities fu = new FileUtilities();
        fu.setInput("uniprotToChain.txt");
        String line = null;
        while ((line = fu.readLine()) != null) {
            if (line.contains("?") || line.contains(">") || line.contains("<")) // Don't load these unknown coordinates
                continue;
            String[] tokens = line.split("\t");
            rtn.put(tokens[0],
                    new int[]{Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])});
        }
        fu.close();
        return rtn;
    }
    
    /**
     * Load a map from a uniprot to its chain features. A UniProt entry may have multiple chains.
     * Class Point is used to store coordinate information: x as start and y as end coordinate.
     * @return
     * @throws IOException
     */
    public Map<String, List<Point>> loadUniProtToChains() throws IOException {
        Map<String, List<Point>> rtn = new HashMap<String, List<Point>>();
        FileUtilities fu = new FileUtilities();
        String fileName = "/Users/gwu/datasets/UniProt/release_2013_05/uniprotToChainWithName.txt";
        fu.setInput(fileName);
        String line = null;
        while ((line = fu.readLine()) != null) {
            if (line.contains("?") || line.contains(">") || line.contains("<")) // Don't load these unknown coordinates
                continue;
            String[] tokens = line.split("\t");
            Point p = new Point(new Integer(tokens[1]), new Integer(tokens[2]));
            List<Point> list = rtn.get(tokens[0]);
            if (list == null) {
                list = new ArrayList<Point>();
                rtn.put(tokens[0], list);
            }
            list.add(p);
        }
        fu.close();
        return rtn;
    }
    
    @Test
    public void extraUniprotToChain() throws IOException {
//        String dir = "/Users/gwu/datasets/UniProt/release_2013_02/";
        String dir = "/Users/gwu/datasets/UniProt/release_2013_05/";
        String fileName = dir + "uniprot_sprot_human.dat";
//        String outName = dir + "uniprotToChain.txt";
        String outName = dir + "uniprotToChainWithName.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        fu.setOutput(outName);
        boolean inNew = true;
        String line = null;
        int index = 0;
        String ac = null;
        while ((line = fu.readLine()) != null) {
            if (line.startsWith("//")) {
                ac = null; // Prepare for entering a new entry
            }
            else if (ac == null && line.startsWith("AC")) { // Want the first AC only. Have to use the first check ac == null to avoid multiple AC lines
                index = line.indexOf(" ");
                line = line.substring(index + 1).trim();
                String[] tokens = line.split(";( )?"); // ; or " "
                ac = tokens[0];
            }
            else if (ac != null && line.startsWith("FT   CHAIN")) {
                index = line.indexOf(" ", "FT   CHAIN".length());
                line = line.substring(index + 1).trim();
                String[] tokens = line.split("( )+");
//                fu.printLine(ac + "\t" + tokens[0] + "\t" + tokens[1]);
//                ac = null;
                // Want to get names associated with each chain
                index = line.indexOf(tokens[1]);
                line = line.substring(index + tokens[1].length()).trim();
                if (line.endsWith(".") || line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1);
                }
                fu.printLine(ac + "\t" + tokens[0] + "\t" + tokens[1] + "\t" + line);
            }
        }
        fu.close();
    }
    
//    @Test
//    public void testUniProtJAPI() throws Exception {
//        EntryRetrievalService service = UniProtJAPI.factory.getEntryRetrievalService();
//        UniProtEntry entry = service.getUniProtEntry("O14511");
//        Collection<ChainFeature> chains = entry.getFeatures(FeatureType.CHAIN);
//        System.out.println("Total chains: " + chains.size());
//        for (ChainFeature chain : chains) {
//            FeatureLocation location = chain.getFeatureLocation();
//            System.out.println(chain.getFeatureDescription() + "\t" + location.getStart() + "\t" + location.getEnd());
//        }
//    }
    
}
