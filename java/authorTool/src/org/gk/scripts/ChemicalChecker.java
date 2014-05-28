/*
 * Created on Feb 25, 2013
 *
 */
package org.gk.scripts;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * This class groups methods related to ReferenceMolecule, ReferenceGroup, and SimpleEntity.
 * @author gwu
 *
 */
public class ChemicalChecker {
    
    public ChemicalChecker() {
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void listReactionsForChemicals() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central_022113",
                                            "root", 
                                            "macmysql01");
        // Get all human ReactionlikeEvent
        GKInstance human = dba.fetchInstance(48887L);
        Collection<GKInstance> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReactionlikeEvent,
                                                                ReactomeJavaConstants.species, 
                                                                "=",
                                                                human);
        // Want to check each reaction to see if any chemicals are used
        int count = 0;
        String fileName = "/Users/gwu/Documents/gkteam/bijay/ReactionsToChemicals_022513.txt";
        FileUtilities fu = new FileUtilities();
        fu.setOutput(fileName);
        fu.printLine("Reaction_ID\tReaction_Name\tChemical_ID\tChemical_Class\tChemical_Name");
        for (GKInstance inst : c) {
            Set<GKInstance> participants = InstanceUtilities.getReactionParticipants(inst);
            Set<GKInstance> refEntities = new HashSet<GKInstance>();
            for (GKInstance part : participants) {
                Set<GKInstance> refs = InstanceUtilities.grepReferenceEntitiesForPE(part);
                if (refs == null)
                    continue;
                refEntities.addAll(refs);
            }
            Set<GKInstance> chemicals = new HashSet<GKInstance>();
            for (GKInstance refEntity : refEntities) {
                if (refEntity == null)
                    continue; // This is a little strange!
                if (refEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceGroup) ||
                    refEntity.getSchemClass().isa(ReactomeJavaConstants.ReferenceMolecule)) {
                    chemicals.add(refEntity);
                }
            }
            if (chemicals.size() > 0) {
                count ++;
                for (GKInstance refEntity : chemicals) {
                    fu.printLine(inst.getDBID() + "\t" +
                                 inst.getDisplayName() + "\t" +
                                 refEntity.getDBID() + "\t" +
                                 refEntity.getSchemClass().getName() + "\t" + 
                                 refEntity.getDisplayName());
                }
            }
        }
        System.out.println("Total reactions: " + count);
        fu.close();
    }
    
}
