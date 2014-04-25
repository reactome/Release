/*
 * Created on Oct 26, 2012
 *
 */
package org.gk.scripts;

import java.awt.Point;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * This class is used to rename PhysicalEntity instances.
 * @author gwu
 *
 */
@SuppressWarnings("unchecked")
public class PhysicalEntityRename {
    protected final String DIR_NAME = "/Users/gwu/Documents/wgm/work/reactome/Rename/";
//    protected final String DIR_NAME = "";
    private List<String> positionablePrefixes;
    private MySQLAdaptor dba;
    
    public static void main(String[] args) {
        try {
            // Need to get the database connection information
            if (args.length < 4) {
                System.err.println("Usage: java org.gk.scripts.PhysicalEntityRename dbHost dbName dbUser dbPwd");
                System.exit(1);
            }
            MySQLAdaptor dba = new MySQLAdaptor(args[0], 
                                                args[1], 
                                                args[2], 
                                                args[3]);
            PhysicalEntityRename renamer = new PhysicalEntityRename();
            renamer.setDBA(dba);
            renamer.renameEWASesInDb();
//            renamer.fixEWASNamesInDb();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * This method is used to check renamed EWASes in gk_central in order to validate
     * the new names are correct without issues reported by Marija.
     * @throws Exception
     */
    @Test
    public void checkRenamedEWASesInDb() throws Exception {
        MySQLAdaptor dba = getDBA();
        // Read the logging file to get the list of DB_IDs for renamed EWASes
        String fileName = DIR_NAME + "EWASRename_gk_central_050313.txt";
        Map<Long, String> dbIdToNewName = loadDbIdToName(fileName);
        System.out.println("Total Renamed EWASes: " + dbIdToNewName.size());
        List<Long> dbIds = new ArrayList<Long>(dbIdToNewName.keySet());
        Collection<GKInstance> renamedEWASes = dba.fetchInstances(ReactomeJavaConstants.PhysicalEntity, 
                                                                  dbIds);
        System.out.println("In current gk_central: " + renamedEWASes.size());
        if (renamedEWASes.size() < dbIds.size()) {
            // Check what instances have been deleted
            Set<Long> currentIds = new HashSet<Long>();
            for (GKInstance inst : renamedEWASes)
                currentIds.add(inst.getDBID());
            List<Long> copy = new ArrayList<Long>(dbIds);
            copy.removeAll(currentIds);
            System.out.println("\nDeleted instances: " + copy.size());
            for (Long id : copy)
                System.out.println("\t" + id);
        }
        
        // Load attributes for quick performance
        dba.loadInstanceAttributeValues(renamedEWASes, 
                                        new String[]{ReactomeJavaConstants.referenceEntity});
        // Check referenceEntities to make sure they refer to ReferenceGeneProduct instances only
        System.out.println("\nReferenceEntity value is not ReferenceGeneProduct:");
        int total = 0;
        for (GKInstance inst : renamedEWASes) {
            GKInstance refEntity = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct))
                continue;
            if (outputOffendedEWAS(inst, dbIdToNewName))
                total ++;
        }
        System.out.println("Total: " + total);
        // Check if any isoforms are used
        System.out.println("\nReferenceEntity value is an ReferenceIsoform:");
        total = 0;
        for (GKInstance inst : renamedEWASes) {
            GKInstance refEntity = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (!refEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform))
                continue;
            // If the first isoform is used, it should be fine to rename it
            String variantIdentifier = (String) refEntity.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
            if (variantIdentifier.endsWith("-1"))
                continue;
            // If the new name has been fixed, don't count it
            String newName = dbIdToNewName.get(inst.getDBID());
            if (!newName.equals(inst.getDisplayName())) 
                continue;
            // Want to get the isoform id in name
            int index = variantIdentifier.lastIndexOf("-");
            String isoformId = variantIdentifier.substring(index);
            String name = inst.getDisplayName(); 
            index = name.indexOf("[");
            if (index > 0) 
                name = name.substring(0, index).trim() + isoformId + " " + name.substring(index);
            else
                name = name + isoformId;
            GKInstance ie = ScriptUtilities.getAuthor(inst);
            System.out.println("\t" + inst.getDBID() + "\t" + 
                               inst.getDisplayName() + "\t" +
                               name + "\t" + 
                               ie);
            total ++;
        }
        System.out.println("Total: " + total);
        // Check if there is any duplication in the name slot
        dba.loadInstanceAttributeValues(renamedEWASes, new String[]{ReactomeJavaConstants.name});
        System.out.println("\nName duplications:");
        total = 0;
        for (GKInstance inst : renamedEWASes) {
            List<String> names = inst.getAttributeValuesList(ReactomeJavaConstants.name);
            Set<String> nameSet = new HashSet<String>(names);
            if (nameSet.size() == names.size()) 
                continue;
//            // Check if the new name has been duplicated
//            int count = 0;
//            String newName = dbIdToNewName.get(inst.getDBID());
//            for (String name : names) {
//                if (name.equals(newName))
//                    count ++;
//            }
//            if (count > 1) {
                if(outputOffendedEWAS(inst, dbIdToNewName))
                    total ++;
//            }
        }
        System.out.println("Total: " + total);
        // Check if any disease is associated
        System.out.println("\nEWASes are disease entities:");
        total = 0;
        dba.loadInstanceAttributeValues(renamedEWASes, new String[]{ReactomeJavaConstants.disease});
        for (GKInstance inst : renamedEWASes) {
            GKInstance disease = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.disease);
            if (disease == null)
                continue;
            if (outputOffendedEWAS(inst, dbIdToNewName))
                total ++;
        }
        System.out.println("Total: " + total);
        // Check if phosphorylation positions should be ordered based on coordiantes
        System.out.println("\nPhosphorylations ordering based on coordinates:");
        total = 0;
        fileName = DIR_NAME + "EWASRenameForFix_051513.txt";
        Map<Long, String> dbIdToFixName = loadDbIdToName(fileName);
        for (GKInstance inst : renamedEWASes) {
            String newName = dbIdToNewName.get(inst.getDBID());
            String fixName = dbIdToFixName.get(inst.getDBID());
            if (newName.equals(fixName))
                continue;
            if (!newName.equals(inst.getDisplayName())) 
                continue;
            GKInstance ie = ScriptUtilities.getAuthor(inst);
            System.out.println("\t" + inst.getDBID() + "\t" + 
                    inst.getDisplayName() + "\t" +
                    fixName + "\t" + 
                    ie);
            total ++;
        }
        System.out.println("Total: " + total);
    }

    private Map<Long, String> loadDbIdToName(String fileName)
            throws IOException {
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        Map<Long, String> dbIdToNewName = new HashMap<Long, String>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            Long dbId = new Long(tokens[0]);
            // Need a little fix
            String newName = tokens[tokens.length - 1];
            int index = newName.lastIndexOf("[");
            if (index > 0)
                newName = newName.substring(0, index).trim() + " " + newName.substring(index); 
            dbIdToNewName.put(dbId,
                              newName);
        }
        fu.close();
        return dbIdToNewName;
    }
    
    private boolean outputOffendedEWAS(GKInstance inst,
                                       Map<Long, String> dbIdToNewName) throws Exception {
        // If the new name has been fixed, don't count it
        String newName = dbIdToNewName.get(inst.getDBID());
        if (!newName.equals(inst.getDisplayName())) 
            return false;
        GKInstance ie = ScriptUtilities.getAuthor(inst);
        System.out.println("\t" + inst.getDBID() + "\t" + 
                            inst.getDisplayName() + "\t" +
                            ie);
        return true;
    }
    
    public PhysicalEntityRename() {
        positionablePrefixes = new ArrayList<String>();
        positionablePrefixes.add("Ub-");
        positionablePrefixes.add("Me2K-");
        positionablePrefixes.add("Me3K-");
    }
    
    protected MySQLAdaptor getDBA() throws Exception {
        if (dba != null)
            return dba;
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_central_062713",
                                            "root",
                                            "macmysql01");
//        MySQLAdaptor dba = new MySQLAdaptor("reactomecurator.oicr.on.ca", 
//                                            "gk_central",
//                                            "authortool",
//                                            "T001test");
        return dba;
    }
    
    public void setDBA(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    private Set<Long> getEWASDBIDsForChecking() throws IOException {
        String fileName = DIR_NAME + "CompartmentLessEWASNames.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        Set<Long> dbIds = new HashSet<Long>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            dbIds.add(new Long(tokens[0]));
        }
        fu.close();
        return dbIds;
    }
    
    /**
     * Reported by Steve, some instances were missed in renaming.
     * @throws IOException
     */
    @Test
    public void checkMissedInstances() throws Exception {
        Set<Long> checkingIds = getEWASDBIDsForChecking();
        String fileName = DIR_NAME + "unseen_isoforms.txt";
//        String fileName = DIR_NAME + "missing_ids_051613.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        Set<Long> missedIds = new HashSet<Long>();
        String line = null;
        while ((line = fu.readLine()) != null)
            missedIds.add(new Long(line));
        fu.close();
        System.out.println("Total missed: " + missedIds.size());
        System.out.println("Total checked: " + checkingIds.size());
        missedIds.retainAll(checkingIds);
        System.out.println("Missed in checking: " + missedIds.size());
        MySQLAdaptor dba = getDBA();
        for (Long dbId : missedIds) {
            GKInstance inst = dba.fetchInstance(dbId);
            System.out.println(inst);
        }
    }
    
    private Map<String, MODMapper> loadModMapper() throws IOException {
        Map<String, MODMapper> mapper = new HashMap<String, PhysicalEntityRename.MODMapper>();
        String fileName = DIR_NAME + "ptm_lookup.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            MODMapper mapper1 = new MODMapper();
            mapper1.accession = tokens[0];
            String prefix = tokens[1];
            if (!prefix.endsWith("-"))
                prefix += "-";
            mapper1.prefix = prefix;
            if (tokens.length > 2)
                mapper1.letter = tokens[2];
            mapper.put(mapper1.accession, mapper1);
        }
        fu.close();
        return mapper;
    }
    
    private List<Long> loadDBIds(String fileName,
                                 boolean hasHeaders) throws IOException {
        List<Long> dbIds = new ArrayList<Long>();
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        if (hasHeaders) // Escape the first line
            line = fu.readLine();
        int count = 0;
        int emptyLine = 0;
        while ((line = fu.readLine()) != null) {
            count ++;
            line = line.trim();
            if (line.length() == 0) {
                emptyLine ++;
                continue;
            }
            String[] tokens = line.split("\t");
            String id = tokens[0].split(" ")[0].trim();
            dbIds.add(new Long(id));
        }
        fu.close();
//        System.out.println("Returned ids: " + dbIds.size());
//        System.out.println("Total lines: " + count);
//        System.out.println("Empty lines: " + emptyLine);
        return dbIds;
    }
    
    /**
     * About 5,000 human EWASes are missed from the first time renaming effor.
     * This method is used to rename those missed EWASes.
     * @throws Execption
     */
    @Test
    public void renameMissedEWASes() throws Exception {
        List<Long> escapeList1 = loadDBIds(DIR_NAME + "exceptions.txt", false);
        System.out.println("Original escape list: " + escapeList1.size());
        List<Long> escapeList2 = loadDBIds(DIR_NAME + "isoform_derived_ewases.txt", true);
        System.out.println("New escape list: " + escapeList2.size());
        // Load DB_IDs that have been renamed before from a logging file
        List<Long> renamedList = loadDBIds(DIR_NAME + "EWASRename_gk_central_050313.txt", true);
        System.out.println("Total renamed in the first time: " + renamedList.size());
        List<Long> newEscapeList = loadDBIds(DIR_NAME + "exemptions_05242013.txt", true);
        
        Set<Long> escapedIds = new HashSet<Long>();
//        escapedIds.addAll(escapeList1);
//        escapedIds.addAll(escapeList2);
//        escapedIds.addAll(renamedList);
        escapedIds.addAll(newEscapeList);
        System.out.println("Total escaped ids: " + escapedIds.size());
//        if (true)
//            return;
//        for (Long dbId : escapedIds)
//            System.out.println(dbId);
        // Load all human EWASes in the database
        MySQLAdaptor dba = getDBA();
        GKInstance human = dba.fetchInstance(48887L);
        Collection<GKInstance> ewases = dba.fetchInstanceByAttribute(ReactomeJavaConstants.EntityWithAccessionedSequence,
                                                                     ReactomeJavaConstants.species,
                                                                     "=",
                                                                     human);
        System.out.println("Total human ewases: " + ewases.size());
        preloadAttributes(dba, ewases);
        // This is IE used for last-renaming. Any EWASes containing this IE should be
        // escaped during this renaming since it has been renamed last time.
        GKInstance markedIE = dba.fetchInstance(3322333L);
        int lastRenamed = 0;
        for (GKInstance ewas : ewases) {
            List<GKInstance> ies = ewas.getAttributeValuesList(ReactomeJavaConstants.modified);
            if (ies.contains(markedIE))
                lastRenamed ++;
        }
        // It should be 4647 or less if some of them have been deleted!
        System.out.println("Total of renamed EWASes last time in gk_central: " + lastRenamed);
        System.out.println("Deleted: " + (renamedList.size() - lastRenamed));
        // Start to generate new name
        Map<String, MODMapper> modAccToMapper = loadModMapper();
        //Map<String, int[]> uniprotToChain = new AttributesChecker().loadUniProtToChain();
        Map<String, List<Point>> uniprotToChains = new AttributesChecker().loadUniProtToChains();
        int escapedBasedOnList = 0;
        int escapedBasedOnFeatures = 0;
        int escapedOnCorrectName = 0;
        List<GKInstance> toBeUpdated = new ArrayList<GKInstance>();
        FileUtilities fu = new FileUtilities();
//        String fileName = DIR_NAME + "Missed_EWASes_all_052413.txt";
        String fileName = DIR_NAME + "Missed_First_EWASes_052813.txt";
        fu.setOutput(fileName);
        fu.printLine("DB_ID\tStart_Coordinate\tEnd_Coordinate\tOld_Display_Name\tNew_Display_Name\tCurator");
        for (GKInstance ewas : ewases) {
//            if (!ewas.getDBID().equals(3009350L))
//                continue;
            if (escapedIds.contains(ewas.getDBID())) {
                escapedBasedOnList ++;
                continue; 
            }
            if (shouldEscape(ewas)) {
                escapedBasedOnFeatures ++;
                continue;
            }
            String newDisplayName = createNewDisplayName(ewas, 
                                                         modAccToMapper, 
                                                         uniprotToChains);
            // Get new name from newDisplayName by removing the compartment part
            String newName = null;
            int index = newDisplayName.indexOf("[");
            if (index > 0)
                newName = newDisplayName.substring(0, index).trim(); // Don't forget trimming the empty space
            else
                newName = newDisplayName;
            // Check if newName has been in the name list
            List<String> nameList = ewas.getAttributeValuesList(ReactomeJavaConstants.name);
            index = nameList.indexOf(newName);
            if (index == 0) {
                escapedOnCorrectName ++;
                continue; // No need to rename
            }
            if (index > 0) {
                nameList.remove(index);
                nameList.add(0, newName);
            }
            else { // No in the list
                nameList.add(0, newName);
            }
            ewas.setAttributeValue(ReactomeJavaConstants.name, nameList);
            String oldDisplayName = ewas.getDisplayName();
            oldDisplayName = oldDisplayName.replaceAll("\\n", ";");
            InstanceDisplayNameGenerator.setDisplayName(ewas);
            if (!newDisplayName.equals(ewas.getDisplayName()))
                throw new IllegalStateException(ewas + " has two different new _displayNames!");
            // Make sure oldName and new name are not the name
            if (oldDisplayName.equals(ewas.getDisplayName())) {
                throw new IllegalStateException(ewas + " has same old and new _displayName!");
            }
            toBeUpdated.add(ewas);
            Integer startCoord = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.startCoordinate);
            Integer endCoord = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.endCoordinate);
            GKInstance ie = ScriptUtilities.getAuthor(ewas);
            // Reorder author so that it can be sorted in Excel
            String author = ie.getDisplayName() + " [DB_ID: " + ie.getDBID() + "]";
            fu.printLine(ewas.getDBID() + "\t" + 
                    startCoord + "\t" +
                    endCoord + "\t" +
                    oldDisplayName + "\t" + 
                    ewas.getDisplayName() + "\t" + 
                    author);
        }
        fu.close();
        System.out.println("EscapedBasedList: " + escapedBasedOnList);
        System.out.println("EscapedBasedOnFeature: " + escapedBasedOnFeatures);
        System.out.println("EscapedOnCorrectName: " + escapedOnCorrectName);
        System.out.println("Total updated: " + toBeUpdated.size());
    }

    private void preloadAttributes(MySQLAdaptor dba,
                                   Collection<GKInstance> ewases)
            throws Exception, InvalidAttributeException {
        // Load some attribtues to be used
        dba.loadInstanceAttributeValues(ewases,
                                        new String[]{ReactomeJavaConstants._displayName,
                                                     ReactomeJavaConstants.name,
                                                     ReactomeJavaConstants.modified,
                                                     ReactomeJavaConstants.disease,
                                                     ReactomeJavaConstants.startCoordinate,
                                                     ReactomeJavaConstants.endCoordinate,
                                                     ReactomeJavaConstants.hasModifiedResidue,
                                                     ReactomeJavaConstants.compartment, // For DisplayName generation
                                                     ReactomeJavaConstants.modified, // These two attributes for pulling out author information
                                                     ReactomeJavaConstants.created,
                                                     ReactomeJavaConstants.referenceEntity});
        // Want to load all related ReferenceGeneProduct for quick performance
        Set<GKInstance> refEntities = new HashSet<GKInstance>();
        for (GKInstance ewas : ewases) {
            GKInstance refEntity = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refEntity == null)
                continue;
            refEntities.add(refEntity);
        }
        dba.loadInstanceAttributeValues(refEntities, 
                                        new String[]{ReactomeJavaConstants.geneName, 
                                                     ReactomeJavaConstants.identifier,
                                                     ReactomeJavaConstants.variantIdentifier});
        // Modification
        Set<GKInstance> modifications = new HashSet<GKInstance>();
        for (GKInstance ewas : ewases) {
            List<GKInstance> mods = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
            if (mods != null)
                modifications.addAll(mods);
        }
        dba.loadInstanceAttributeValues(modifications,
                                        new String[]{ReactomeJavaConstants.psiMod,
                                                     ReactomeJavaConstants.coordinate});
        // Needs PsiMod identifier
        Set<GKInstance> psiMods = new HashSet<GKInstance>();
        for (GKInstance mod : modifications) {
            if (!mod.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod))
                continue;
            List<GKInstance> psi = mod.getAttributeValuesList(ReactomeJavaConstants.psiMod);
            if (psi != null)
                psiMods.addAll(psi);
        }
        dba.loadInstanceAttributeValues(psiMods,
                                        new String[]{ReactomeJavaConstants.identifier});
    }
    
    /**
     * Check the results using gene names as EWAS instances' display names.
     * @throws Exception
     */
    @Test
    public void checkRenameEWASInstances() throws Exception {
        Set<Long> checkingDbIds = getEWASDBIDsForChecking();
        Map<String, MODMapper> modAccToMapper = loadModMapper();
        MySQLAdaptor dba = getDBA();
        // A list of escapes
        List<Long> escapeIds = loadDBIds(DIR_NAME + "exceptions.txt", false);
        Map<String, List<Point>> uniprotToChains = new AttributesChecker().loadUniProtToChains();
        // For human proteins only
        GKInstance human = dba.fetchInstance(48887L);
        Collection<GKInstance> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.EntityWithAccessionedSequence,
                                                                ReactomeJavaConstants.species, 
                                                                "=",
                                                                human);
        dba.loadInstanceAttributeValues(c, new String[]{ReactomeJavaConstants.name,
                                                        ReactomeJavaConstants.referenceEntity,
                                                        ReactomeJavaConstants.startCoordinate,
                                                        ReactomeJavaConstants.endCoordinate});
//        String fileName = DIR_NAME + "EWASRename_111312.txt";
        //String fileName = DIR_NAME + "EWASRename_120412.txt";
//        String fileName = DIR_NAME + "EWASRename_120612.txt";
//        String fileName = DIR_NAME + "EWASRename_022113.txt";
//        String fileName = DIR_NAME + "EWASRename_031413.txt";
//        String fileName = DIR_NAME + "EWASRename_041613.txt";
//        String fileName = DIR_NAME + "EWASRename_050313.txt";
        String fileName = DIR_NAME + "EWASRenameForFix_051513.txt";
        FileUtilities fu = new FileUtilities();
        fu.setOutput(fileName);
//        System.out.println("DB_ID\tOld_Display_Name\tNew_Display_Name");
        fu.printLine("DB_ID\tStart_Coordinate\tEnd_Coordinate\tOld_Display_Name\tNew_Display_Name");
        for (GKInstance inst : c) {
            if (escapeIds.contains(inst.getDBID()))
                continue;
            if (!checkingDbIds.contains(inst.getDBID()))
                continue;
            if (shouldEscape(inst))
                continue;
            String newDisplayName = createNewDisplayName(inst, modAccToMapper, uniprotToChains);
            fu.printLine(inst.getDBID() + "\t" + 
                         inst.getAttributeValue(ReactomeJavaConstants.startCoordinate) + "\t" + 
                         inst.getAttributeValue(ReactomeJavaConstants.endCoordinate) + "\t" +
                         inst.getDisplayName() + "\t" + 
                         newDisplayName);
        }
        fu.close();
    }
    
    private String createNewDisplayName(GKInstance inst,
                                        Map<String, MODMapper> modAccToMapper,
                                        Map<String, List<Point>> uniprotToChains) throws Exception {
        GKInstance refEntity = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceEntity);
        // geneName should not be null since it has been checked in method "shouldEscape(GKInstance)"
        String geneName = (String) refEntity.getAttributeValue(ReactomeJavaConstants.geneName);
        // We don't need to add gene name into the name list 
//        List<String> names = inst.getAttributeValuesList(ReactomeJavaConstants.name);
//        if (names.contains(geneName)) {
//            int index = names.indexOf(geneName);
////            if (index == 0)
////                continue;
//            if (index > 0) {
//                names.remove(geneName);
//                names.add(0, geneName);
//            }
//        }
//        else {
//            names.add(0, geneName);
//        }
        String displayName = inst.getDisplayName();
        displayName = displayName.replaceAll("\\n", ";");
        String newDisplayName = geneName;
        String prefixMod = getPrefixForModication(modAccToMapper, inst);
        newDisplayName = prefixMod + newDisplayName;
        String coordinateText = getCoordinateText(inst, uniprotToChains);
        if (coordinateText.length() > 0) {
            newDisplayName = newDisplayName + coordinateText;
        }
        // Add to name for the time being
        List<String> names = inst.getAttributeValuesList(ReactomeJavaConstants.name);
        names.add(0, newDisplayName);
        newDisplayName = InstanceDisplayNameGenerator.generateDisplayName(inst);
        names.remove(0); // Get back to the original one
        return newDisplayName;
    }
    
    private String getCoordinateText(GKInstance ewas,
                                     Map<String, List<Point>> uniprotToChains) throws Exception {
        Integer start = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.startCoordinate);
        if (start == null)
            start = -1; // As default
        Integer end = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.endCoordinate);
        if (end == null)
            end = -1;
        if (start == -1 && end == -1)
            return "(?-?)";
//        if ((start == -1 || start == 1) && end == -1) // 1 and -1 also means the whole length
//            return "";
        if (start != -1 && end != -1) {
            // Check if the used coordinates are the same as the chain coordinates recorded in
            // UniProt database. If yes, no coordinates should be displayed.
            GKInstance refEntity = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refEntity != null) {
                String identify = (String) refEntity.getAttributeValue(ReactomeJavaConstants.identifier);
                List<Point> chains = uniprotToChains.get(identify);
                if (chains != null) {
                    if (chains.size() > 1) { // If there is more than one chain, always attach coordiantes as suggested by Marija by May 24, 2013
                        return "(" + start + "-" + end + ")";
                    }
                    Point chain = chains.get(0);
                    if (chain.x == start && chain.y == end)
                        return "";
                    //                Integer seqLen = (Integer) refEntity.getAttributeValue(ReactomeJavaConstants.sequenceLength);
                    //                if (seqLen != null && seqLen == (end - start + 1))
                    //                    return "";
                }
            }
            return "(" + start + "-" + end + ")";
        }
        if (start == -1)
            return "(?-" + end + ")";
        if (end == -1)
            return "(" + start + "-?)";
        return "";
    }
    
    private boolean shouldEscape(GKInstance ewas) throws Exception {
        // Don't need to handle any mutated EWASes
        if (ewas.getDisplayName().toLowerCase().contains("mutant")) {
            //            System.out.println(inst);
            //            count ++;
            return true;
        }
        // Skip EWASes having disease attributes
        GKInstance disease = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.disease);
        if (disease != null)
            return true;
        GKInstance refEntity = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.referenceEntity);
        // Nothing can be done if there is no gene name
        if (refEntity == null || !refEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.geneName))
            return true;
        String geneName = (String) refEntity.getAttributeValue(ReactomeJavaConstants.geneName);
        if (geneName == null)
            return true;
        // Escape non-protein
        if (!refEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct))
            return true;
        // Escape non primary ReferenceIsoform (e.g. isoform 2 or higher.
        if (refEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceIsoform)) {
            String variantId = (String) refEntity.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
            if (!variantId.endsWith("-1"))
                return true;
        }
        List<GKInstance> modifications = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        if (modifications == null || modifications.size() == 0)
            return false;
        // Escape GeneticallyModifiedResidue and GroupModification
        for (GKInstance modification : modifications) {
            //if (modification.getSchemClass().isa(ReactomeJavaConstants.GeneticallyModifiedResidue) ||
            //    modification.getSchemClass().isa(ReactomeJavaConstants.GroupModifiedResidue))
            // As of May 17, 2013, only EWASes with modifiedResidue will be subject to renaming!
            if (!modification.getSchemClass().isa(ReactomeJavaConstants.ModifiedResidue))
                return true;
        }
        return false;
    }
    
    private String getPrefixForModication(Map<String, MODMapper> idToMapper,
                                          GKInstance ewas) throws Exception {
        List<GKInstance> modifications = ewas.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
        if (modifications == null || modifications.size() == 0)
            return "";
        final Map<String, List<String>> typeToResidue = new HashMap<String, List<String>>();
        for (GKInstance modification : modifications) {
            if (!modification.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod))
                continue;
            GKInstance psiMod = (GKInstance) modification.getAttributeValue(ReactomeJavaConstants.psiMod);
            if (psiMod == null)
                continue;
            String type = null;
            String identifier = (String) psiMod.getAttributeValue(ReactomeJavaConstants.identifier);
            Integer coordinate = null;
            if (modification.getSchemClass().isValidAttribute(ReactomeJavaConstants.coordinate)) {
                coordinate = (Integer) modification.getAttributeValue(ReactomeJavaConstants.coordinate);
            }
            MODMapper mapper = idToMapper.get("MOD:" + identifier);
            if (mapper == null) {
                type = (String) psiMod.getAttributeValue(ReactomeJavaConstants.name);
            }
            else {
                type = mapper.prefix;
            }
            List<String> list = typeToResidue.get(type);
            if (list == null) {
                list = new ArrayList<String>();
                typeToResidue.put(type, list);
            }
            String residue = "";
            if (mapper != null && mapper.letter != null) {
                residue = mapper.letter;
                if (coordinate != null)
                    residue += coordinate;
            }
            else if (coordinate != null) {
                residue += coordinate;
            }
            list.add(residue);
        }
        // Do a sort so that numerous types will be listed first
        List<String> typeList = new ArrayList<String>(typeToResidue.keySet());
        Collections.sort(typeList, new Comparator<String>() {
            public int compare(String type1, String type2) {
                List<String> residues1 = typeToResidue.get(type1);
                List<String> residues2 = typeToResidue.get(type2);
                int rtn = residues2.size() - residues1.size();
                if (rtn == 0)
                    return type1.compareTo(type2);
                return rtn;
            }
        });
        StringBuilder builder = new StringBuilder();
        for (String type : typeList) {
            List<String> residues = typeToResidue.get(type);
            // No need to show anything for no-p
            if (!type.equals("p-") && !positionablePrefixes.contains(type)) {
                if (residues.size() == 1) {
                    builder.append(type);
                }
                else { // Should greater than 1
                    builder.append(residues.size()).append("x").append(type);
                }
            }
            else { // For phosphorylation
                int emptyCount = 0;
                for (Iterator<String> it = residues.iterator(); it.hasNext();) {
                    String residue = it.next();
                    if (residue.length() == 0) {
                        emptyCount ++;
                        it.remove();
                    }
                }
                String tmp = joinResidules(type,
                                           residues);
                if (emptyCount == 0) {
                    if (tmp.matches("(\\d)+") && residues.size() > 4) // Just simple number
                        builder.append(tmp).append("x").append(type);
                    else
                        builder.append(type).append(tmp);
                }
                else if (residues.size() == 0) { // All are empty
                    if (emptyCount == 1)
                        builder.append(type);
                    else
                        builder.append(emptyCount).append("x").append(type);
                }
                else {
                    if (tmp.matches("(\\d)+")) // Just simple number
                        builder.append(tmp).append("x").append(type);
                    else
                        builder.append(type).append(tmp);
                    builder.append("-");
                    if (emptyCount == 1)
                        builder.append(type);
                    else
                        builder.append(emptyCount).append("x").append(type);
                }
            }
            if (!builder.toString().endsWith("-"))
                builder.append("-"); // For p-
        }
        return builder.toString();
    }
    
    private String joinResiduesForP(List<String> residues) {
//        if (residues.size() > 4) {
//            String aa = residues.get(0).substring(0, 1);
//            // Don't show "x" for p as of March 14, 2013
//            return residues.size() + aa;
//            //                return residues.size() + "x" + aa;
//        }
        // Create a map so that same residues can be merged together (e.g. 6 x Y)
        Map<String, Integer> residueToNumber = new HashMap<String, Integer>();
        for (String residue : residues) {
            Integer number = residueToNumber.get(residue);
            if (number == null) {
                residueToNumber.put(residue, 1);
            }
            else
                residueToNumber.put(residue, ++number);
        }
        List<String> keys = new ArrayList<String>(residueToNumber.keySet());
        // Sort phosphorylation based on coordinates is available
        final Pattern pattern = Pattern.compile("(\\d)+");
        Collections.sort(keys, new Comparator<String>() {
            public int compare(String residue1, String residue2) {
                // Extract coordinate if available
                Matcher matcher = pattern.matcher(residue1);
                String pos1 = null;
                if (matcher.find()) {
                    pos1 = matcher.group(0);
                }
                matcher = pattern.matcher(residue2);
                String pos2 = null;
                if (matcher.find())
                    pos2 = matcher.group(0);
                if (pos1 == null || pos2 == null)
                    return residue1.compareTo(residue2);
                return new Integer(pos1).compareTo(new Integer(pos2));
            }
        });
        // For residues having more than 3, don't show positions
        // This map is used to check for this
        Map<String, Integer> aaToNumber = new HashMap<String, Integer>();
        for (String key : keys) {
            String aa = key.substring(0, 1); // Get the first aa
            Integer number = aaToNumber.get(aa);
            if (number == null)
                aaToNumber.put(aa, 1);
            else
                aaToNumber.put(aa, ++number);
        }
        // Get a list of AAs that should not show positions
        List<String> aaList = new ArrayList<String>();
        for (String aa : aaToNumber.keySet()) {
            Integer number = aaToNumber.get(aa);
            if (number >= 4) {
                aaList.add(aa);
            }
        }
        // Simple sorting
        Collections.sort(aaList);
        StringBuilder builder = new StringBuilder();
        // Want to list AAs without coordiantes first
        for (String aa : aaList) {
            Integer number = aaToNumber.get(aa);
            builder.append(number).append(aa).append(",");
        }
        for (String key : keys) {
            String aa = key.substring(0, 1);
            if (aaList.contains(aa))
                continue; // This should have been listed
            Integer number = residueToNumber.get(key);
            if (number == 1)
                builder.append(key);
            else // Unknown positions
                builder.append(number).append(key); // Don't show "x" for p- as of March 14, 2013
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1); // Remove the last ","
        return builder.toString();
    }
    
    //TODO: limit the number of coordinate to 4 (see Steve's email on Nov 8, 2012) 
    private String joinResidules(String type,
                                 List<String> residues) {
        if (residues.size() == 0)
            return "";
        if (type.equals("p-"))
            return joinResiduesForP(residues);
        if (residues.size() >= 4) {
            return residues.size() + "";
        }
        // Create a map so that same residues can be merged together (e.g. 6 x Y)
        Map<String, Integer> residueToNumber = new HashMap<String, Integer>();
        for (String residue : residues) {
            Integer number = residueToNumber.get(residue);
            if (number == null) {
                residueToNumber.put(residue, 1);
            }
            else
                residueToNumber.put(residue, ++number);
        }
        StringBuilder builder = new StringBuilder();
        List<String> keys = new ArrayList<String>(residueToNumber.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Integer number = residueToNumber.get(key);
            if (number == 1)
                builder.append(key);
            else 
                builder.append(number).append("x").append(key);
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1); // Remove the last ","
        return builder.toString();
    }
    
    /**
     * There is an extra space at the end of new names. Have to run this
     * fix to remove it.
     * @throws Exception
     */
    @Test
    public void fixEWASNamesInDb() throws Exception {
        String fileName = DIR_NAME + "EWASRename_050313.txt";
        MySQLAdaptor dba = getDBA();
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        List<GKInstance> toBeUpdated = new ArrayList<GKInstance>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            Long dbId = new Long(tokens[0]);
            GKInstance instance = dba.fetchInstance(dbId);
            List<String> currentNames = instance.getAttributeValuesList(ReactomeJavaConstants.name);
            String firstName = currentNames.get(0);
            if (firstName.endsWith(" ")) {
                firstName = firstName.trim();
                currentNames.remove(0);
                currentNames.add(0, firstName);
                instance.setAttributeValue(ReactomeJavaConstants.name, 
                                           currentNames);
                InstanceDisplayNameGenerator.setDisplayName(instance);
                toBeUpdated.add(instance);
            }
            else
                System.out.println("No need to fix: " + instance);
        }
        fu.close();
        ScriptUtilities.updateInstanceNames(dba, toBeUpdated);
    }
    
    /**
     * Commit short names generated from method checkRenameEWASInstances() into
     * the database.
     * @throws Exception
     */
    @Test
    public void renameEWASesInDb() throws Exception {
//        String fileName = DIR_NAME + "EWASRename_050313.txt";
        String fileName = DIR_NAME + "Missed_First_EWASes_052813.txt";
        MySQLAdaptor dba = getDBA();
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        List<GKInstance> toBeUpdated = new ArrayList<GKInstance>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            Long dbId = new Long(tokens[0]);
            String newName = tokens[4];
            // Get rid of the compartment name
            int index = newName.indexOf("[");
            if (index > 0)
                newName = newName.substring(0, index).trim();
            GKInstance instance = dba.fetchInstance(dbId);
            // In case it is deleted
            if (instance == null)
                continue;
            List<String> currentNames = instance.getAttributeValuesList(ReactomeJavaConstants.name);
            // A flag to indicate if an update is needed
            boolean needUpdate = false;
            if (currentNames.contains(newName)) {
                // Check if it is the first one
                index = currentNames.indexOf(newName);
                if (index > 0) {
                    // Re-sort it
                    currentNames.remove(index);
                    currentNames.add(0, newName);
                    needUpdate = true;
                }
            }
            else {
                currentNames.add(0, newName);
                needUpdate = true;
            }
            if (needUpdate) {
                instance.setAttributeValue(ReactomeJavaConstants.name,
                                           currentNames);
                String oldDisplayName = instance.getDisplayName();
                InstanceDisplayNameGenerator.setDisplayName(instance);
                // Commit to the database
                System.out.println(instance.getDBID() + "\t" + 
                                   oldDisplayName + "\t" +
                                   instance.getDisplayName());
                toBeUpdated.add(instance);
            }
        }
        fu.close();
        ScriptUtilities.updateInstanceNames(dba, toBeUpdated);
    }
    
    /**
     * Because of a bug in method "renameSmallMolecules", an extra IE has been added for SimpleEntity
     * that should not be changed. Remove this IE if nothing has been updated there.
     * @throws Exception
     */
    @Test
    public void fixSimpleEntityInstances() throws Exception {
        String fileName = "/Users/gwu/Documents/wgm/work/reactome/Rename/SimpleEntityRenamedList_gk_central_102612.txt";
        Set<Long> dbIds = new HashSet<Long>();
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        while ((line = fu.readLine()) != null) {
            if (line.startsWith("Total"))
                break;
            String[] tokens = line.split("\t");
            dbIds.add(new Long(tokens[0]));
        }
        fu.close();
        System.out.println("Total updated instances: " + dbIds.size());
        
        MySQLAdaptor dba = getDBA();
        // My IE
//        GKInstance newIE = dba.fetchInstance(2534102L);
        GKInstance newIE = dba.fetchInstance(2537470L);
        Collection<GKInstance> instances = dba.fetchInstancesByClass(ReactomeJavaConstants.SimpleEntity);
        dba.loadInstanceAttributeValues(instances, new String[]{ReactomeJavaConstants.modified});
        try {
            dba.startTransaction();
            int index = 0;
            for (GKInstance inst : instances) {
                if (dbIds.contains(inst.getDBID()))
                    continue;
                List<GKInstance> modified = inst.getAttributeValuesList(ReactomeJavaConstants.modified);
                if (modified.contains(newIE)) {
                    modified.remove(newIE);
                    dba.updateInstanceAttribute(inst, ReactomeJavaConstants.modified);
                    System.out.println(index + ": " + inst);
                    index ++;
                }
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
            throw e;
        }
    }
    
    @Test
    public void renameSmallMolecules() throws Exception {
        String fileName = "/Users/gwu/Documents/wgm/work/reactome/Rename/small_mol_renaming.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        Map<String, String> nameToShort = new HashMap<String, String>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            if (tokens[1].length() == 0)
                continue;
            // In case they are the same: no need to make any change
            if (tokens[1].equals(tokens[0]))
                continue;
            // The second column is for short names
            String shortName = nameToShort.get(tokens[0]);
            if (shortName != null && !shortName.equals(tokens[1])) {
                String message = tokens[0] + " has different short names!";
                System.err.println(message);
                throw new IllegalStateException(message);
            }
            nameToShort.put(tokens[0], tokens[1]);
        }
        fu.close();
        System.out.println("Total name to short name mappings: " + nameToShort.size());
        MySQLAdaptor dba = getDBA();
        Collection<GKInstance> instances = dba.fetchInstancesByClass(ReactomeJavaConstants.SimpleEntity);
        dba.loadInstanceAttributeValues(instances, new String[]{ReactomeJavaConstants.name});
        int totalChanged = 0;
        int inTheList = 0;
        List<GKInstance> toBeUpdated = new ArrayList<GKInstance>();
        for (GKInstance instance : instances) {
            List<String> names = instance.getAttributeValuesList(ReactomeJavaConstants.name);
            // The first name should be used for _displayName
            String firstName = names.get(0);
            String shortName = nameToShort.get(firstName);
            if (shortName == null)
                continue;
            // Check if the shortName has been in the list already, if true
            // just re-order it.
            if (names.contains(shortName)) {
                names.remove(shortName);
                inTheList ++;
            }
            names.add(0, shortName);
            String oldName = instance.getDisplayName();
            InstanceDisplayNameGenerator.setDisplayName(instance);
            System.out.println(instance.getDBID() + "\t" + 
                               instance.getSchemClass().getName() + "\t" + 
                               oldName + "\t" + 
                               instance.getDisplayName());
            totalChanged ++;
            toBeUpdated.add(instance);
        }
        System.out.println("Total changed instances: " + totalChanged);
        System.out.println("Short name in the name list: " + inTheList);
        
        ScriptUtilities.updateInstanceNames(dba, toBeUpdated);
    }
    
    /**
     * Use this method to check if renaming results from two runs are the same. This method can
     * be used to make sure bug fixes or improvements don't bring in un-expected changes.
     * @throws IOException
     */
    @Test
    public void compareTwoRenamingResults() throws IOException {
        String fileName1 = DIR_NAME + "Missed_EWASes_051713.txt";
        fileName1 = DIR_NAME + "Missed_First_EWASes_052413.txt";
        Map<Long, String> dbIdToNewName1 = loadDbIdToNewName(fileName1);
        List<Long> dbIds1 = new ArrayList<Long>(dbIdToNewName1.keySet());
        String fileName2 = DIR_NAME + "Missed_EWASes_052413.txt";
        fileName2 = DIR_NAME + "Missed_First_EWASes_052813.txt";
        Map<Long, String> dbIdToNewName2 = loadDbIdToNewName(fileName2);
        List<Long> dbIds2 = new ArrayList<Long>(dbIdToNewName2.keySet());
        dbIds1.removeAll(dbIds2);
        System.out.println("Deleted instances: " + dbIds1.size());
        for (Long dbId : dbIds1)
            System.out.println(dbId);
        dbIds2.removeAll(dbIdToNewName1.keySet());
        System.out.println("New instances: " + dbIds2.size());
        for (Long dbId : dbIds2)
            System.out.println(dbId);
        // Check if there is any new or deleted DB_IDs
        int count = 0;
        System.out.println("\nDifferent Names:");
        System.out.println("DB_ID\tNew_Name1\tNew_Name2");
        for (Long dbId : dbIdToNewName2.keySet()) {
            String newName2 = dbIdToNewName2.get(dbId);
            String newName1 = dbIdToNewName1.get(dbId);
            if (newName1 != null && !newName1.equals(newName2)) {
                System.out.println(dbId + "\t" + newName1 + "\t" + newName2);
                count ++;
            }
        }
        System.out.println("Total: " + count);
    }
    
    private Map<Long, String> loadDbIdToNewName(String fileName) throws IOException {
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine(); 
        Map<Long, String> dbIdToNewName = new HashMap<Long, String>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            dbIdToNewName.put(new Long(tokens[0]), tokens[4]);
        }
        return dbIdToNewName;
    }
    
    private class MODMapper {
        private String accession;
        private String prefix;
        private String letter;
        
        /**
         * 
         */
        public MODMapper() {
            
        }
    }
    
}
