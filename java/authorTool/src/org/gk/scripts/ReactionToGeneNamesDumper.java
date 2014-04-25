/*
 * Created on Aug 22, 2012
 *
 */
package org.gk.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;
import org.gk.util.StringUtils;
import org.junit.Test;

/**
 * This class is used to dump human ReactionLikeEvent to gene names.
 * @author gwu
 *
 */
public class ReactionToGeneNamesDumper {
    
    public ReactionToGeneNamesDumper() {
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void dump() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_current_ver41",
                                            "root", 
                                            "macmysql01");
        GKInstance human = dba.fetchInstance(48887L);
        Collection<GKInstance> reactions = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReactionlikeEvent, 
                                                                        ReactomeJavaConstants.species, 
                                                                        "=",
                                                                        human);
        System.out.println("Total reactions: " + reactions.size());
        String fileName = "tmp/HumanReactionsToGenes.txt";
        FileUtilities fu = new FileUtilities();
        fu.setOutput(fileName);
        fu.printLine("DB_ID\tName\tType\tNumberOfGenes\tGenes");
        for (GKInstance rxt : reactions) {
            List<String> genes = getGenesInReaction(rxt);
            fu.printLine(rxt.getDBID() + "\t" +
                    rxt.getDisplayName() + "\t" + 
                    rxt.getSchemClass().getName() + "\t" + 
                    genes.size() + "\t" +
                    StringUtils.join(",", genes));
        }
        fu.close();
    }
    
    private List<String> getGenesInReaction(GKInstance reaction) throws Exception {
        Set<GKInstance> refEntities = InstanceUtilities.grepRefPepSeqsFromPathway(reaction);
        Set<String> geneNames = new HashSet<String>();
        for (GKInstance refEntity : refEntities) {
            if (refEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.geneName)) {
                String geneName = (String) refEntity.getAttributeValue(ReactomeJavaConstants.geneName);
                if (geneName != null)
                    geneNames.add(geneName);
            }
        }
        List<String> list = new ArrayList<String>(geneNames);
        Collections.sort(list);
        return list;
    }
    
}
