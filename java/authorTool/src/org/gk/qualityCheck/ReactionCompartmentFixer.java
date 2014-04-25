/*
 * Created on Nov 16, 2010
 *
 */
package org.gk.qualityCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.database.DefaultInstanceEditHelper;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.util.ProgressPane;
import org.junit.Test;

public class ReactionCompartmentFixer extends ReactionCompartmentCheck {
    private List<GKInstance> changedInstances;
    
    public ReactionCompartmentFixer() {
    }
    
    /**
     * Fix the compartment setting for a reaction.
     * @param inst
     * @return
     * @throws Exception
     */
    private boolean fixReaction(GKInstance reaction) throws Exception {
        Set<GKInstance> contained = getAllContainedEntities(reaction);
        // Skip checking for shell instances
        if (containShellInstances(contained))
            return true; // Escape it
        // Nothing to check if contained is empty: probably 
        // the instance is just starting to annotation or
        // container is used as a place holder
        if (contained.size() == 0)
            return true; // Another escape
        // Get the compartment setting: compartments should be a list since
        // it is used as a multiple value attribute.
        Set<GKInstance> containedCompartments = new HashSet<GKInstance>();
        for (GKInstance comp : contained) {
            List<?> compartments = comp.getAttributeValuesList(ReactomeJavaConstants.compartment);
            if (compartments != null) {
                for (Iterator<?> it = compartments.iterator(); it.hasNext();)
                    containedCompartments.add((GKInstance)it.next());
            }
        }
        return fixReactionCompartments(reaction, containedCompartments);
    }
    
    List<GKInstance> tooManyReactions = new ArrayList<GKInstance>();
    List<GKInstance> compartmentNotNeighbor = new ArrayList<GKInstance>();
    List<GKInstance> noCompartmentInReaction = new ArrayList<GKInstance>();
    List<GKInstance> singleCompartmentNotContainer = new ArrayList<GKInstance>();
    List<GKInstance> twoCompartmentNotContainer = new ArrayList<GKInstance>();
    List<GKInstance> twoCompartmentOneInReaction = new ArrayList<GKInstance>();
    List<GKInstance> notOneCompartment = new ArrayList<GKInstance>();
    List<GKInstance> singleCompartmentNotContainerForOne = new ArrayList<GKInstance>();
    
    private boolean fixReactionCompartments(GKInstance reaction,
                                            Collection<GKInstance> participantCompartments) throws Exception {
        List<?> rxtCompartments = reaction.getAttributeValuesList(ReactomeJavaConstants.compartment);
        if (rxtCompartments == null)
            rxtCompartments = EMPTY_LIST; // Just for neat coding
        if (rxtCompartments.size() > 2 ||
            participantCompartments.size() > 2) {
            tooManyReactions.add(reaction);
            return false; // Cannot do a fix. Need the curator's attention.
        }
        if (participantCompartments.size() == 2) {
            // Check if these two compartments are adjacent
            Iterator<GKInstance> it = participantCompartments.iterator();
            GKInstance compartment1 = it.next();
            GKInstance compartment2 = it.next();
            Map<Long, List<Long>> neighborMap = getNeighbors();
            List<Long> neighbor = (List<Long>) neighborMap.get(compartment1.getDBID());
            if (neighbor == null ||
                !neighbor.contains(compartment2.getDBID())) {
                compartmentNotNeighbor.add(reaction);
                return false; // These two compartments are not adjacent. A wrong case! Don't do fix. Need curator's attention!
            }
            // Check how many compartments in the reaction
            if (rxtCompartments.size() == 0) {
                noCompartmentInReaction.add(reaction);
                return false; // Nothing is assigned
            }
            if (rxtCompartments.size() == 1) {
                GKInstance reactionCompartment = (GKInstance) rxtCompartments.get(0);
                // Need to check if reactionComartemnt is a container of compartment1 and compartment2
                Set<GKInstance> containers1 = getAllContainers(compartment1);
                Set<GKInstance> containers2 = getAllContainers(compartment2);
                if (!containers1.contains(reactionCompartment) ||
                        !containers2.contains(reactionCompartment)) {
                    singleCompartmentNotContainer.add(reaction);
                    if (participantCompartments.contains(reactionCompartment)) {
                        twoCompartmentOneInReaction.add(reaction);
                        // Just fix this type of reaction
                        fixReactionByUsingEntityCompartments(reaction, participantCompartments);
                    }
                    return false; // To ensure two entity compartments should be contained by 
                                    // one Reaction compartment
                }
                
            }
            else if (rxtCompartments.size() == 2) {
                GKInstance rxtCompt1 = (GKInstance) rxtCompartments.get(0);
                GKInstance rxtCompt2 = (GKInstance) rxtCompartments.get(1);
                if (!checkTwoReactionAndTwoEntityCompartments(compartment1, 
                                                              compartment2, 
                                                              rxtCompt1,
                                                              rxtCompt2)) {
                    twoCompartmentNotContainer.add(reaction);
                    return false; // To ensure two reaction compartment are containers
                                    // for each of two entitycompartment, respectively.
                }
            }
        }
        else if (participantCompartments.size() == 1) {
            GKInstance entityCompt = participantCompartments.iterator().next();
            if (rxtCompartments.size() != 1) {
                notOneCompartment.add(reaction);
                return false; // Reaction can have only one compartment at most
            }
            GKInstance rxtCompt = (GKInstance) rxtCompartments.get(0);
            Set<GKInstance> containers = getAllContainers(entityCompt);
            containers.add(entityCompt); // These two compartments can be the same.
            if (!containers.contains(rxtCompt)) {
                // Some specical cases are allowed
                List<?> allowedCases = getAllowedRxtEntityCompartments();
                String key = rxtCompt.getDBID() + "," + entityCompt.getDBID();
                if (!allowedCases.contains(key)) {
                    singleCompartmentNotContainerForOne.add(reaction);
                    return false; // Reaction compartment should be a container of the
                                  // sole entity compartment (or the same).
                }
            }
        }
        return true; 
    }
    
    /**
     * A simple fix by copying comparatments from reaction participants.
     * @param reaction
     * @param entityCompartments
     * @throws Exception
     */
    private void fixReactionByUsingEntityCompartments(GKInstance reaction,
                                                      Collection<GKInstance> entityCompartments) throws Exception {
        reaction.setAttributeValue(ReactomeJavaConstants.compartment,
                                   new ArrayList<GKInstance>(entityCompartments));
        if (changedInstances == null)
            changedInstances = new ArrayList<GKInstance>();
        changedInstances.add(reaction);
    }
                           
    /**
     * Commit any changed instances into the database.
     * @param personId
     * @param dba
     * @throws Exception
     */
    private void commitChanges(Long personId,
                               MySQLAdaptor dba) throws Exception {
        if (changedInstances == null || changedInstances.size() == 0)
            return; // Nothing to be changed.
        // Prepare a local environment
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        DefaultInstanceEditHelper ieHelper = new DefaultInstanceEditHelper();
        ieHelper.setDefaultPerson(personId);
        GKInstance ie = ieHelper.getDefaultInstanceEdit(null);
        // Call to load all modified slot 
        for (GKInstance inst : changedInstances) {
            inst.getAttributeValue(ReactomeJavaConstants.modified);
        }
        // An InstanceEdit has been cloned in the following call
        ie = ieHelper.attachDefaultIEToDBInstances(changedInstances, ie);
        // First commit ie
        try {
            dba.startTransaction();
            dba.storeInstance(ie);
            for (GKInstance inst : changedInstances) {
                // Update the compartment value
                dba.updateInstanceAttribute(inst, ReactomeJavaConstants.compartment);
                // Update IEs
                dba.updateInstanceAttribute(inst, ReactomeJavaConstants.modified);
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
            throw e; // Re-thrown exception
        }
    }
    
    /**
     * Create a main method to that this method can be used in the server side.
     * @param args
     */
    public static void main(String[] args) {
        ReactionCompartmentFixer fixer = new ReactionCompartmentFixer();
        try {
            fixer.fixModifiedSlotForReactions();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        //        if (args.length < 5) {
        //            System.out.println("Usage: java org.gk.quality.ReactionCompartmentFixer dbHost dbName dbUser dbPwd personId");
        //            System.exit(1);
        //        }
        //        try {
        //            MySQLAdaptor dba = new MySQLAdaptor(args[0], 
        //                                                args[1],
        //                                                args[2],
        //                                                args[3]);
        //            ReactionCompartmentFixer fixer = new ReactionCompartmentFixer();
        //            fixer.setDatasource(dba);
        //            fixer.progressPane = new ProgressPane();
        //            Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.Reaction);
        //            fixer.loadAttributes(c);
        //            int total = 0;
        //            for (Iterator<?> it = c.iterator(); it.hasNext();) {
        //                GKInstance inst = (GKInstance) it.next();
        //                boolean passed = fixer.checkInstance(inst);
        //                if (!passed) {
        //                    //                    System.out.println(inst);
        //                    boolean isFixed = fixer.fixReaction(inst);
        //                    total ++;
        //                }
        //            }
        //            System.out.println("## Total offended reactions: " + total);
        //            Long personId = new Long(args[4]);
        //            System.out.println("Starting fix...");
        //            long time1 = System.currentTimeMillis();
        //            fixer.commitChanges(personId, dba);
        //            long time2 = System.currentTimeMillis();
        //            System.out.println("Total time for committing: " + (time2 - time1));
        //            System.out.println("Total fixed instances: " + 
        //                               (fixer.changedInstances == null ? "0 " : fixer.changedInstances.size()));
        //        }
        //        catch(Exception e) {
        //            e.printStackTrace();
        //        }
    }
    
    @Test
    public void testCheckReactions() throws Exception {
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "gk_central_110410",
//                                            "root",
//                                            "macmysql01");
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "test_gk_central_110410",
                                            "authortool",
                                            "T001test");
        this.dataSource = dba;
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.Reaction);
        progressPane = new ProgressPane();
        loadAttributes(c);
        int total = 0;
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            boolean passed = checkInstance(inst);
            if (!passed) {
//                System.out.println(inst);
                boolean isFixed = fixReaction(inst);
                total ++;
            }
        }
        System.out.println("## Total offended reactions: " + total);
//        List<GKInstance> tooManyReactions = new ArrayList<GKInstance>();
        System.out.println("\n## Three or more entity compartments or three or more reaction compartments: " + tooManyReactions.size());
        printList(tooManyReactions);
//        List<GKInstance> compartmentNotNeighbor = new ArrayList<GKInstance>();
        System.out.println("\n## Two entity compartments are not neighbor: " + compartmentNotNeighbor.size());
        printList(compartmentNotNeighbor);
//        List<GKInstance> noCompartmentInReaction = new ArrayList<GKInstance>();
        System.out.println("\n## Two entity compartments, but no reaction compartemnt assigned: " + noCompartmentInReaction.size());
//        List<GKInstance> singleCompartmentNotContainer = new ArrayList<GKInstance>();
        System.out.println("\n## Two entity compartments, and one reaction compartment. But reaction compartment cannot enclose two entity compartments: "  + singleCompartmentNotContainer.size());
        printList(singleCompartmentNotContainer);
        System.out.println("\n## Two entity compartments, and one reaction compartment. Reaction compartment is one of entity compartments: " + twoCompartmentOneInReaction.size());
        printList(twoCompartmentOneInReaction);
        //        List<GKInstance> twoCompartmentNotContainer = new ArrayList<GKInstance>();
        System.out.println("\n## Two entity compartemnts, and two reaction compartments. But two reaction compartments cannot enclose two entity compartments: " + twoCompartmentNotContainer.size());
        printList(twoCompartmentNotContainer);
        //        List<GKInstance> notOneCompartment = new ArrayList<GKInstance>();
        System.out.println("\n## One entity compartment, but not one reaction compartemnt(0 or > 1): " + notOneCompartment.size());
        printList(notOneCompartment);
//        List<GKInstance> singleCompartmentNotContainerForOne = new ArrayList<GKInstance>();
        System.out.println("\n## One entity compartment, and one reaction compartment. But reaction compartment cannot enclose entity compartment: " + singleCompartmentNotContainerForOne.size());
        printList(singleCompartmentNotContainerForOne);
        // Commit to the database now
        // For Guanming Wu
        Long personId = 140537L;
        System.out.println("Starting fix...");
        long time1 = System.currentTimeMillis();
        commitChanges(personId, dba);
        long time2 = System.currentTimeMillis();
        System.out.println("Total time for committing: " + (time2 - time1));
    }
    
    private void printList(List<GKInstance> list) {
        for (GKInstance inst  : list)
            System.out.println(inst);
    }
    
    @Test
    public void fixModifiedSlotForReactions() throws Exception {
        MySQLAdaptor targetDBA = new MySQLAdaptor("localhost",
                                                  "gk_central",
                                                  "wgm",
                                                  "zhe10jiang23");
        MySQLAdaptor sourceDBA = new MySQLAdaptor("localhost",
                                                  "test_gk_central_111910_before_fix",
                                                  "wgm",
                                                  "zhe10jiang23");
        GKInstance ie = targetDBA.fetchInstance(1043153L);
        Collection<?> c = ie.getReferers(ReactomeJavaConstants.modified);
        System.out.println("Total instances: " + c.size());
        List<GKInstance> changed = new ArrayList<GKInstance>();
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            List<?> targetValue = inst.getAttributeValuesList(ReactomeJavaConstants.modified);
            GKInstance srcInst = sourceDBA.fetchInstance(inst.getDBID());
            List<?> sourceValue = srcInst.getAttributeValuesList(ReactomeJavaConstants.modified);
            if (targetValue.size() > 1) {
                System.out.println("Cannot fix: " + inst);
                continue;
            }
            List<GKInstance> newValues = new ArrayList<GKInstance>();
            if (sourceValue != null) {
                for (Iterator<?> it1 = sourceValue.iterator(); it1.hasNext();) {
                    GKInstance ie1 = (GKInstance) it1.next();
                    newValues.add(targetDBA.fetchInstance(ie1.getDBID()));
                }
            }
            for (Iterator<?> it1 = targetValue.iterator(); it1.hasNext();) {
                GKInstance ie1 = (GKInstance) it1.next();
                newValues.add(ie1);
            }
            inst.setAttributeValue(ReactomeJavaConstants.modified, 
                                   newValues);
            changed.add(inst);
        }
        System.out.println("Total changed instances to be stored into db: " + changed.size());
        try {
            targetDBA.startTransaction();
            for (GKInstance inst : changed) {
                targetDBA.updateInstanceAttribute(inst, ReactomeJavaConstants.modified);
            }
            targetDBA.commit();
        }
        catch(Exception e) {
            targetDBA.rollback();
            throw e;
        }
    }
    
}
