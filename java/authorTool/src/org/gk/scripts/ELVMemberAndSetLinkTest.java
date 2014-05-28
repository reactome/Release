/*
 * Created on Jul 25, 2011
 *
 */
package org.gk.scripts;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.gkEditor.PopupMenuManager;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.PathwayDiagramGeneratorViaAT;
import org.gk.pathwaylayout.PredictedPathwayDiagramGeneratorFromDB;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.DiagramGKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.EntitySetAndEntitySetLink;
import org.gk.render.EntitySetAndMemberLink;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableRegistry;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.util.FileUtilities;
import org.gk.util.SwingImageCreator;
import org.junit.Test;

/**
 * This class is used to test drawing between EntitySet and its members.
 * @author wgm
 *
 */
public class ELVMemberAndSetLinkTest {
    private MySQLAdaptor dba;
    
    public ELVMemberAndSetLinkTest() {
    }
    
    public MySQLAdaptor getDBA() throws Exception {
        if (dba == null) {
            dba = new MySQLAdaptor("localhost",
                                   "gk_central_071712",
                                   "root",
                                   "macmysql01");
        }
        return dba;
    }
    
    public void setDBA(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Run as java -Xmx2048m dbHost dbName dbUser dbPwd");
            System.exit(1);
        }
        ELVMemberAndSetLinkTest runner = new ELVMemberAndSetLinkTest();
        try {
            MySQLAdaptor dba = new MySQLAdaptor(args[0], 
                                                args[1], 
                                                args[2],
                                                args[3]);
            runner.setDBA(dba);
//            runner.updateDiagramsInDb();
            runner.updateDiagramsForSetSetLinksInDb();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Because of a bug in method updateDbDiagram(), the original values in the 
     * modified slot has been wiped out. This method is used to fix this problem
     * by using a back-up gk_central database.
     * @throws Exception
     */
    @Test
    public void fixModifediAttributeValues() throws Exception {
        String dirName = "/Users/gwu/Documents/wgm/work/reactome/";
        String[] fileNames = new String[] {
                "OutputFromAddingLinksBetweenCandidateSetsAndCandidates.txt",
                "OutputFromAddSetSetLinks.txt"
        };
        Set<Long> dbIds = new HashSet<Long>();
        for (String fileName : fileNames) {
            Set<Long> tmpDbIds = grepPDIds(dirName + fileName);
            dbIds.addAll(tmpDbIds);
        }
        System.out.println("Total PDs: " + dbIds.size());
        // Original database
        MySQLAdaptor srcDBA = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                               "test_gk_central_041112",
                                               "authortool", 
                                               "T001test");
        // Current database
        MySQLAdaptor targetDBA = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                                  "gk_central",
                                                  "authortool", 
                                                  "T001test");
        int count = 0;
        try {
            targetDBA.startTransaction();
            for (Long dbId : dbIds) {
                GKInstance srcPd = srcDBA.fetchInstance(dbId);
                List<?> srcModified = srcPd.getAttributeValuesList(ReactomeJavaConstants.modified);
                GKInstance targetPd = targetDBA.fetchInstance(dbId);
                List<GKInstance> targetModified = targetPd.getAttributeValuesList(ReactomeJavaConstants.modified);
                // Compare these two modified attributes
                if (srcModified != null && srcModified.size() > 0) {
                    System.out.println("Need to be fixed: " + targetPd);
                    count ++;
                    // Need to copy the src modified values into the target modified values
                    for (Object obj : srcModified) {
                        GKInstance srcIE = (GKInstance) obj;
                        GKInstance targetIE = targetDBA.fetchInstance(srcIE.getDBID());
                        if (targetIE == null) {
                            throw new IllegalStateException(targetIE + " cannot be found in the current DB!");
                        }
                        if (targetModified.contains(targetIE)) {
                            throw new IllegalStateException(targetIE + " is in the target modified slot already!");
                        }
                        targetModified.add(targetIE);
                    }
                    // Need to sort it
                    Collections.sort(targetModified, new Comparator<GKInstance>() {
                        public int compare(GKInstance ie1, GKInstance ie2) {
                            return ie1.getDBID().compareTo(ie2.getDBID());
                        }
                    });
                    targetPd.setAttributeValue(ReactomeJavaConstants.modified,
                                               targetModified);
                    targetDBA.updateInstanceAttribute(targetPd,
                                                      ReactomeJavaConstants.modified);
                    System.out.println("Total target modified slots: " + targetModified.size());
                }
            }
            targetDBA.commit();
        }
        catch(Exception e) {
            targetDBA.rollback();
            throw e;
        }
        System.out.println("Total fixes: " + count);
    }
    
    private Set<Long> grepPDIds(String fileName) throws Exception {
        Set<Long> dbIds = new HashSet<Long>();
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        Pattern pattern = Pattern.compile("\\[PathwayDiagram:(\\d+)\\]");
        while ((line = fu.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) { // There should be only one DB_ID
                String value = matcher.group(1);
//                System.out.println("Value: " + value);
                dbIds.add(new Long(value));
            }
        }
        fu.close();
        return dbIds;
    }
    
    /**
     * This method is used to update diagrams in gk_central to add links among EntitySets that 
     * share members.
     * @throws Exception
     */
    public void updateDiagramsForSetSetLinksInDb() throws Exception {
        // Get the database
        MySQLAdaptor dba = getDBA();
        Collection<GKInstance> c = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        List<GKInstance> list = new ArrayList<GKInstance>(c);
        InstanceUtilities.sortInstances(list);
        
        DiagramGKBReader diagramReader = new DiagramGKBReader();
        DiagramGKBWriter diagramWriter = new DiagramGKBWriter();
        Map<Long, List<Renderable>> dbIdToObject = new HashMap<Long, List<Renderable>>();
        Set<GKInstance> entitySets = new HashSet<GKInstance>();
        int count = 0;
        // The following is used for updating
        try {
            dba.startTransaction();
            GKInstance newIE = createDefaultIE(dba);
            System.out.println("Default IE: " + newIE);
            
            for (GKInstance pd : list) {
//                System.out.println("Working on " + pd);
                // Just a quick check
                String xml = (String) pd.getAttributeValue(ReactomeJavaConstants.storedATXML);
                if (xml == null)
                    continue;
                //            System.out.println("Checking " + pd + "...");
                RenderablePathway diagram = diagramReader.openDiagram(pd);
                RenderableRegistry.getRegistry().open(diagram);
                checkInstancesInDiagram(dba, 
                                        diagram,
                                        dbIdToObject,
                                        entitySets);
                int addedLinks = addSetSetLinks(dbIdToObject,
                                           entitySets,
                                           diagram);
                if (addedLinks > 0) {
                    // Need to reset the XML
                    updateDbDiagram(dba, 
                                    diagramWriter,
                                    newIE,
                                    pd,
                                    diagram);
                    count ++;
                    System.out.println(count + ": " + pd + " (" + addedLinks + " lines).\n");
                }
                dbIdToObject.clear();
                entitySets.clear();
            }
            System.out.println("Total Pathway Diagram changed: " + count);
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
            e.printStackTrace();
        }
    }

    private void updateDbDiagram(MySQLAdaptor dba,
                                 DiagramGKBWriter diagramWriter,
                                 GKInstance newIE, 
                                 GKInstance pd,
                                 RenderablePathway diagram) throws Exception, InvalidAttributeException, InvalidAttributeValueException {
        String xml = diagramWriter.generateXMLString(diagram);
        pd.setAttributeValue(ReactomeJavaConstants.storedATXML, xml);
        // have to get all modified attributes first. Otherwise, the original
        // values will be lost
        pd.getAttributeValue(ReactomeJavaConstants.modified);
        pd.addAttributeValue(ReactomeJavaConstants.modified, newIE);
        dba.updateInstanceAttribute(pd, ReactomeJavaConstants.storedATXML);
        dba.updateInstanceAttribute(pd, ReactomeJavaConstants.modified);
    }
    
    /**
     * This method is used to add EntitySetAndMemberLinks directly to PathwayDiagram in 
     * a database.
     * @throws Exception
     */
    @Test
    public void updateDiagramsInDb() throws Exception {
        // Provide a list of escaped PathwayDiagrams:
//        Long dbIds[] = new Long[] {
//                //34: [PathwayDiagram:500171] Diagram of Transport of glucose and other sugars, bile salts and organic acids, metal ions and amine compounds
//                500171L,
//                //35: [PathwayDiagram:499927] Diagram of Transport of inorganic cations/anions and amino acids/oligopeptides
//                499927L,
//                //45: [PathwayDiagram:504073] Diagram of Metabolism of nucleotides
//                504073L
//        };
        // For links between CandidateSet and its candidates, no diagrams should be escaped
        Long dbIds[] = new Long[]{};
        List<Long> escapeList = Arrays.asList(dbIds);
        // Get the database
        MySQLAdaptor dba = getDBA();
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        DiagramGKBReader diagramReader = new DiagramGKBReader();
        DiagramGKBWriter diagramWriter = new DiagramGKBWriter();
        Map<Long, List<Renderable>> dbIdToObject = new HashMap<Long, List<Renderable>>();
        Set<GKInstance> entitySets = new HashSet<GKInstance>();
        int count = 1;
        // The following is used for updating
        try {
            dba.startTransaction();
            // Set up default IE
            GKInstance newIE = createDefaultIE(dba);
            System.out.println("Default IE: " + newIE);
            
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                GKInstance pd = (GKInstance) it.next();
                if (escapeList.contains(pd.getDBID()))
                    continue; // Escape these PathwayDiagram instances.
//                System.out.println("Working on " + pd);
                // Just a quick check
                String xml = (String) pd.getAttributeValue(ReactomeJavaConstants.storedATXML);
                if (xml == null)
                    continue;
                //            System.out.println("Checking " + pd + "...");
                RenderablePathway diagram = diagramReader.openDiagram(pd);
                RenderableRegistry.getRegistry().open(diagram);
                checkInstancesInDiagram(dba, 
                                        diagram,
                                        dbIdToObject,
                                        entitySets);
                boolean isChanged = false;
                for (GKInstance set : entitySets) {
                    List<Renderable> rSets = dbIdToObject.get(set.getDBID());
//                    List<?> members = set.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    // These statements are used to add links between CandidateSet and its candidates
                    if (!set.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate))
                        continue;
                    List<?> members = set.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                    if (members == null || members.size() == 0)
                        continue;
                    for (Renderable rSet : rSets) {
                        for (Object obj : members) {
                            GKInstance member = (GKInstance) obj;
                            List<Renderable> rMembers = dbIdToObject.get(member.getDBID());
                            if (rMembers == null || rMembers.size() == 0)
                                continue;
                            // Need to add links
                            if (!isChanged) {
                                System.out.println(count + ": " + pd);
                                isChanged = true;
                                count++;
                            }
                            for (Renderable rMember : rMembers) {
                                addEntitySetAndMemberLink(diagram, 
                                                          rSet,
                                                          rMember);
                            }
                        }
                    }
                }
                if (isChanged) {
                    updateDbDiagram(dba, diagramWriter, newIE, pd,
                                    diagram);
                }
                dbIdToObject.clear();
                entitySets.clear();
            }
            System.out.println("Total Pathway Diagram changed: " + (count - 1));
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
            e.printStackTrace();
        }
    }

    private GKInstance createDefaultIE(MySQLAdaptor dba) throws Exception,
            InvalidAttributeException, InvalidAttributeValueException {
        Long defaultPersonId = 140537L; // For Guanming Wu at CSHL
        return ScriptUtilities.createDefaultIE(dba, defaultPersonId, true);
    }
    
    @Test
    public void checkDigramsForAddingSetSetLink() throws Exception {
        final PopupMenuManager manager = new PopupMenuManager(null);
        AddLinks addLinks = new AddLinks() {
            @Override
            public int addLinks(Map<Long, List<Renderable>> idToNodes,
                                Set<GKInstance> entitySets,
                                RenderablePathway diagram) throws Exception {
                return addSetSetLinks(idToNodes, 
                                      entitySets,
                                      diagram);
            }
        };
        addLinksForEntitySets(addLinks, true);
    }
    
    private void addEntitySetEntitySetLink(GKInstance set1,
                                           GKInstance set2,
                                           Map<Long, List<Renderable>> idToNodes,
                                           RenderablePathway diagram) {
        List<Renderable> nodes1 = idToNodes.get(set1.getDBID());
        List<Renderable> nodes2 = idToNodes.get(set2.getDBID());
        for (Renderable node1 : nodes1) {
            for (Renderable node2 : nodes2) {
                EntitySetAndEntitySetLink flowLine = new EntitySetAndEntitySetLink();
                flowLine.setEntitySets((Node)node1, (Node)node2);
                diagram.addComponent(flowLine);
                flowLine.setContainer(diagram);
                flowLine.layout();
                System.out.println("Creating set and set link: " + node1.getDisplayName() + 
                                   " - " + node2.getDisplayName());
            }
        }
        
    }
    
    private int addSetSetLinks(Map<Long, List<Renderable>> idToNodes,
                               Set<GKInstance> entitySets,
                               RenderablePathway diagram) throws Exception {
        int count = 0;
        List<GKInstance> list = new ArrayList<GKInstance>(entitySets);
        for (int i = 0; i < list.size() - 1; i++) {
            GKInstance set1 = list.get(i);
            Set<GKInstance> members1 = InstanceUtilities.getContainedInstances(set1, 
                                                                               ReactomeJavaConstants.hasMember,
                                                                               ReactomeJavaConstants.hasCandidate);
            List<Renderable> nodes1 = idToNodes.get(set1.getDBID());
            for (int j = i + 1; j < list.size(); j++) {
                GKInstance set2 = list.get(j);
                // Make sure they are not self contained
                if (InstanceUtilities.isEntitySetAndMember(set1, set2) ||
                    InstanceUtilities.isEntitySetAndMember(set2, set1))
                    continue; // Should be escaped!
                Set<GKInstance> members2 = InstanceUtilities.getContainedInstances(set2, 
                                                                                   ReactomeJavaConstants.hasMember,
                                                                                   ReactomeJavaConstants.hasCandidate);
                // Check if there any sharing
                members2.retainAll(members1);
                filterSharedMembers(members2);
                if (members2.size() > 0) {
                    // There are some sharing
                    count += nodes1.size() * idToNodes.get(set2.getDBID()).size();
                    addEntitySetEntitySetLink(set1, set2, idToNodes, diagram);
//                    System.out.println(set1 + " - " + set2);
                }
            }
        }
        return count;
    }
    
    private void filterSharedMembers(Set<GKInstance> members) {
        for (Iterator<GKInstance> it = members.iterator(); it.hasNext();) {
            GKInstance inst = it.next();
            if (inst.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity))
                it.remove();
        }
    }
    
    /**
     * This method is used to check all PathwayDiagram to see if EntitySet and its members can be 
     * linked together.
     * @throws Exception
     */
    @Test
    public void checkDiagramsForAdding() throws Exception {
        AddLinks addLinks = new AddLinks() {
            @Override
            public int addLinks(Map<Long, List<Renderable>> idToNodes,
                                Set<GKInstance> entitySets,
                                RenderablePathway diagram) throws Exception {
                int count = 0;
                for (GKInstance set : entitySets) {
//                        List<?> members = set.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    // The following three statements are used to check for Candidate.hasCandidate()
                    if (!set.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate))
                        continue;
                    List<?> members = set.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                    if (members == null || members.size() == 0)
                        continue;
                    List<Renderable> setNodes = idToNodes.get(set.getDBID());
                    for (Object obj : members) {
                        GKInstance member = (GKInstance) obj;
                        List<Renderable> rMember = idToNodes.get(member.getDBID());
                        if (rMember != null) {
                            count += setNodes.size(); // If more than one node for a set, multiple lines should be added.
//                                isCheckDone = true;
//                                System.out.println(set + " <- " + member);
//                                break;
                        }
                    }
//                        if (isCheckDone)
//                            break;
                }
                return count;
            }
        };
        addLinksForEntitySets(addLinks, false);
    }
    
    private void addLinksForEntitySets(AddLinks addLinks,
                                       boolean needExportDiagram) throws Exception{
        // Get the database
        MySQLAdaptor dba = getDBA();
        Collection<GKInstance> c = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        DiagramGKBReader diagramReader = new DiagramGKBReader();
        Map<Long, List<Renderable>> dbIdToObject = new HashMap<Long, List<Renderable>>();
        Set<GKInstance> entitySets = new HashSet<GKInstance>();
        int index = 0;
        List<GKInstance> list = new ArrayList<GKInstance>(c);
        InstanceUtilities.sortInstances(list);
        for (Iterator<?> it = list.iterator(); it.hasNext();) {
            GKInstance pd = (GKInstance) it.next();
            // Just a quick check
            String xml = (String) pd.getAttributeValue(ReactomeJavaConstants.storedATXML);
            if (xml == null)
                continue;
//            System.out.println("Checking " + pd + "...");
            RenderablePathway diagram = diagramReader.openDiagram(pd);
            checkInstancesInDiagram(dba, 
                                    diagram,
                                    dbIdToObject,
                                    entitySets);
            boolean isCheckDone = false;
            int count = addLinks.addLinks(dbIdToObject, 
                                          entitySets,
                                          diagram);
            if (count > 0) {
                index ++;
                System.out.println(index + ": " + pd + " (" + count + " lines)\n");
                if (needExportDiagram)
                    exportDiagram(pd, diagram);
//                System.out.println("Count: " + count);
            }
            dbIdToObject.clear();
            entitySets.clear();
        }
        System.out.println("Total Pathway Diagram needs to be changed: " + index);
    }
    
    private void exportDiagram(GKInstance diagramInst,
                               RenderablePathway pathway) throws IOException {
        PredictedPathwayDiagramGeneratorFromDB helper = new PredictedPathwayDiagramGeneratorFromDB();
        PathwayDiagramGeneratorViaAT generator = new PathwayDiagramGeneratorViaAT();
        PathwayEditor editor = new PathwayEditor();
        editor.setRenderable(pathway);
        editor.setHidePrivateNote(true);
        // Just to make the tightNodes() work, have to do an extra paint
        // to make textBounds correct
        generator.paintOnImage(editor);
        editor.tightNodes(true);
        // Output PDF
        String fileName = diagramInst.getDisplayName();
        fileName = fileName.replaceAll("(\\\\|/)", "-");
        // Note: It seems there is a bug in the PDF exporter to set correct FontRenderContext.
        // Have to call PNG export first to make some rectangles correct.
        File pdfFileName = new File("tmp/AddLinks/", fileName + ".pdf");
        SwingImageCreator.exportImageInPDF(editor, pdfFileName);
    }
    
    @Test
    public void testDraw() throws Exception {
        String dirName = "/Users/gwu/Documents/gkteam/Steve/";
        //String fileName = dirName + "ComplementCascade.rtpj";
//        Long pathwayId = 166658L;
//        String fileName = dirName + "EGFRSignaling.rtpj";
//        String destFileName = dirName + "EGFRSignalingWithSetToMemberLinks.rtpj";
//        Long pathwayId = 177929L;
        
//        String fileName = dirName + "FGFRSignaling.rtpj";
//        String destFileName = dirName + "FGFRSignalingWithSetToMemberLinks.rtpj";
//        Long pathwayId = 190236L;
        
//        String fileName = dirName + "GPVI-mediated activation cascade.rtpj";
//        String destFileName = dirName + "GPVI-mediated activation cascadeWithSetToMemberLinks_101711.rtpj";
//        Long pathwayId = 114604L;
        
        String fileName = dirName + "SPhase.rtpj";
        String destFileName = dirName + "SPhase_101711.rtpj";
        Long pathwayId = 69242L;
        
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor(fileName);
        GKInstance pathway = fileAdaptor.fetchInstance(pathwayId);
        RenderablePathway diagram = fileAdaptor.getDiagram(pathway);
        // To make changes, have to register all RenderableObjects and reset next id
        RenderableRegistry.getRegistry().open(diagram);
        // Map is used to map from DB_ID to objects for further links
        Map<Long, List<Renderable>> dbIdToObjects = new HashMap<Long, List<Renderable>>();
        // Get a list of EntitySet instances       
        Set<GKInstance> entitySets = new HashSet<GKInstance>();
        checkInstancesInDiagram(fileAdaptor, 
                                diagram, 
                                dbIdToObjects,
                                entitySets);
        // Create links for renderable objects between EntitySet and its members
        for (GKInstance set : entitySets) {
            List<?> members = set.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (members == null || members.size() == 0)
                continue;
            List<Renderable> rSets = dbIdToObjects.get(set.getDBID());
            for (Renderable rSet : rSets) {
                for (Object obj : members) {
                    GKInstance member = (GKInstance) obj;
                    List<Renderable> rMembers = dbIdToObjects.get(member.getDBID());
                    if (rMembers == null || rMembers.size() == 0)
                        continue;
                    for (Renderable rMember : rMembers) {
                        addEntitySetAndMemberLink(diagram, 
                                                  rSet, 
                                                  rMember);
                    }
                }
            }
        }
        //fileAdaptor.save(dirName + "ComplementCascadeWithSetToMemberLinks.rtpj");
        fileAdaptor.save(destFileName);
    }

    private void addEntitySetAndMemberLink(RenderablePathway diagram,
                                           Renderable rSet, Renderable rMember) {
        EntitySetAndMemberLink flowLine = new EntitySetAndMemberLink();
        flowLine.setEntitySet((Node)rSet);
        flowLine.setMember((Node)rMember);
        diagram.addComponent(flowLine);
        flowLine.setContainer(diagram);
        flowLine.layout();
        System.out.println("Creating set and member link: " + rSet.getDisplayName() + 
                           " <- " + rMember.getDisplayName());
    }                                             

    /**
     * One GKInstance can be displayed multiple times as shortcuts.
     * @param adaptor
     * @param diagram
     * @param dbIdToObjects
     * @param entitySets
     * @throws Exception
     */
    private void checkInstancesInDiagram(PersistenceAdaptor adaptor,
                                         RenderablePathway diagram,
                                         Map<Long, List<Renderable>> dbIdToObjects,
                                         Set<GKInstance> entitySets) throws Exception {
        if (diagram.getComponents() == null)
            return;
        for (Iterator<?> it = diagram.getComponents().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r.getReactomeId() == null)
                continue;
            GKInstance inst = adaptor.fetchInstance(r.getReactomeId());
            // Just in case some object has been deleted
            if (inst == null)
                continue;
            List<Renderable> list = dbIdToObjects.get(inst.getDBID());
            if (list == null) {
                list = new ArrayList<Renderable>();
                dbIdToObjects.put(inst.getDBID(), list);
            }
            list.add(r);
//            System.out.println(inst);
            if (inst.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
//                System.out.println("EntitySet: " + r.getDisplayName());
                entitySets.add(inst);
            }
        }
    }
    
    private interface AddLinks {
        public int addLinks(Map<Long, List<Renderable>> idToNodes,
                            Set<GKInstance> entitySets,
                            RenderablePathway diagram) throws Exception;
                             
    }
    
    /**
     * There is a bug in the curator tool so that a self-link is added for an EntitySet
     * in the ELV after a reaction is DnD to a pathway ELV panel. This method is used to check
     * how many PDs have been effected by this bug.
     */
    @Test
    public void checkWrongSetAndSetLinks() throws Exception {
        MySQLAdaptor dba = getDBA();
        Collection<GKInstance> pds = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        DiagramGKBReader reader = new DiagramGKBReader();
        List<Long> dbIds = new ArrayList<Long>();
        for (GKInstance pd : pds) {
            String xml = (String) pd.getAttributeValue(ReactomeJavaConstants.storedATXML);
            if (xml == null) {
                System.out.println("No XML text: " + pd);
                continue;
            }
            RenderablePathway pathway = reader.openDiagram(pd);
            List<Renderable> components = pathway.getComponents();
            if (components == null || components.size() == 0)
                continue;
            for (Renderable r : components) {
                if (r instanceof EntitySetAndEntitySetLink) {
                    EntitySetAndEntitySetLink link = (EntitySetAndEntitySetLink) r;
                    Node input = link.getInputNode(0);
                    Node output = link.getOutputNode(0);
                    if (input == output) {
                        System.out.println("Effected PD: " + pd);
                        dbIds.add(pd.getDBID());
                        break;
                    }
                }
            }
        }
        System.out.println("Total PDs: " + dbIds.size());
        Collections.sort(dbIds);
        for (Long dbId : dbIds)
            System.out.println(dbId);
    }
    
    @Test
    public void fixWrongSetAndSetLinks() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "db_name",
                                            "db_user",
                                            "db_pwd");
        Long dbIds[] = new Long[] {
                504073L,
                1243100L,
                1273430L,
                1362420L,
                1660554L,
                2032362L,
                2032365L,
                2162142L,
                2193146L,
                2395206L,
                2399662L
        };
        
        DiagramGKBReader reader = new DiagramGKBReader();
        DiagramGKBWriter writer = new DiagramGKBWriter();
        try {
            dba.startTransaction();
            GKInstance defaultIE = createDefaultIE(dba);
            for (Long dbId : dbIds) {
                GKInstance pd = dba.fetchInstance(dbId);
                System.out.println("Processing " + pd);
                RenderablePathway pathway = reader.openDiagram(pd);
                List<Renderable> comps = pathway.getComponents();
                boolean changed = false;
                for (Iterator<Renderable> it = comps.iterator(); it.hasNext();) {
                    Renderable r = it.next();
                    if (r instanceof EntitySetAndEntitySetLink) {
                        EntitySetAndEntitySetLink link = (EntitySetAndEntitySetLink) r;
                        Node input = link.getInputNode(0);
                        Node output = link.getOutputNode(0);
                        if (input == output) {
                            it.remove();
                            changed = true;
                        }
                    }
                }
                if (changed) 
                    updateDbDiagram(dba, writer, defaultIE, pd, pathway);
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
        }
    }
    
}
