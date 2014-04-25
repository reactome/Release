/*
 * Created on Nov 16, 2010
 *
 */
package org.gk.scripts;

import java.util.Iterator;

import org.gk.database.util.MODReactomeAnalyzer;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.junit.Test;

/**
 * This class is used to help to generate a curator tool project from a test rice 
 * reactome database. The generated curator tool project is used to merge edits to
 * another newly created rice reactome database without any manual edits.
 * Here are logic here this new project is created:
 * 1). Find all manually created IEs using sql query by provided last non-manual created IE.
 * 2). Find all new instances created by the above IEs
 * 3). Find all modified instances create by the above IEs. Two cases in the modified instances:
 *   i>. modified instances are dumped from the rice_reactome project: instances have
 *    DB_IDs > max_ids from original gk_central
 *   ii> modified instances are from the original Reactome gk_central database: instances have 
 *   DB_IDs <= max_id from original gk_central
 * 4). Check out instances from both 2 and 3. Flip instances from 2) and 3.i to negative DB_IDs 
 * to mark them as new instances. Keep original DB_IDs for instances from 3.ii. 
 * 5). The new project should work against a new updated rice_reactome_database by manual 
 * checking instances.
 * @author wgm
 *
 */
public class RiceReactomeNewProjectCreator extends MODReactomeAnalyzer {
    // The following two DB_IDs should be found from databases
    // The max DB_ID before a rice_reactome database is generated.
    // This should be the maximum DB_ID from a base gk_central database.
    private Long maxOriginalDbId;
    // The DB_ID for the latest InstanceEdit instance before any manual editing.
    // This value should be got from a newly created rice reactome db before
    // any manual editing committed.
    private Long maxDumpIEDbId;
    
    public RiceReactomeNewProjectCreator() {
        needRecursive = false;
    }
    
    public void setMaxOrignalDbId(Long dbId) {
        this.maxOriginalDbId = dbId;
    }
    
    public void setMaxDumpIEDbId(Long dbId) {
        this.maxDumpIEDbId = dbId;
    }

    @Override
    protected GKInstance fetchLastInstanceEdit() throws Exception {
        return modDba.fetchInstance(maxDumpIEDbId);
    }

    @Override
    protected void markInstances(XMLFileAdaptor fileAdaptor) {
        // Change all ids of new MOD instances as negative as labels for new instances
        for (Iterator<?> it = newMODInstances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            flipInstance(fileAdaptor, instance);
        }
        // Set dirty flags for all modified instances
        for (Iterator<?> it = changedMODInstances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            if (instance.getDBID() > maxOriginalDbId) {
                // This should be a new Instance from the rice database
                flipInstance(fileAdaptor, instance);
            }
            else {
                // This should be an edit from the original gk_central
                GKInstance local = fileAdaptor.fetchInstance(instance.getDBID());
                fileAdaptor.markAsDirty(local);
            }
        }
    }

    /**
     * Flip a GKInstance as a new GKInstance.
     * @param fileAdaptor
     * @param instance
     */
    private void flipInstance(XMLFileAdaptor fileAdaptor, GKInstance instance) {
        Long dbId = instance.getDBID();
        GKInstance local = fileAdaptor.fetchInstance(dbId);
        local.setDBID(new Long(-dbId.longValue()));
        fileAdaptor.dbIDUpdated(dbId, local);
        // Set the dirty flag
        fileAdaptor.markAsDirty(local);
    }
    
    /**
     * Use this method to generate a Curator Tool project from a specified database.
     * All object properties should  be specified before running.
     * @throws Exception
     */
    @Test
    public void runGenerateProject() throws Exception {
        MySQLAdaptor dbaAdaptor = new MySQLAdaptor("localhost",
                                                   "test_rice_reactome_v2_111610", 
                                                   "root", 
                                                   "macmysql01");
        setModReactome(dbaAdaptor);
        setMaxOrignalDbId(981542L);
        setMaxDumpIEDbId(981538L);
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        PersistenceManager.getManager().setActiveMySQLAdaptor(dbaAdaptor);
        checkMODInstances();
        checkOutAsNewProject();
        String fileName = "/Users/wgm/Documents/gkteam/Liya/NewInTestRiceReactomeV2.rtpj";
        fileAdaptor.save(fileName);
    }
    
}
