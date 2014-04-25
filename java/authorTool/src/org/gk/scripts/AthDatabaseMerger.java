/*
 * Created on Sep 1, 2009
 *
 */
package org.gk.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaClass;
import org.junit.Test;


/**
 * This class is used to merge two databases from Arabidopsin Reactome databases: one is human curated
 * and another is imported from AraCyc and KEGG. Both of these databases are sliced from the same database,
 * but with similar changes afterwards. However, many human curated instances have been added to the curated
 * database. The method presented here is to check each instance in the curated database against the imported
 * database. There are three cases:
 * 1). An instance in the curated DB is same as one in the imported. Do nothing.
 * 2). An instance in the curated DB has beem modified from the imported database. This should be marked as dirty.
 * 3). An instance in the curated DB is completed new. This should be marked as new with a negative DB_ID.
 * @author wgm
 *
 */
public class AthDatabaseMerger {
    private MySQLAdaptor curatedDBA;
    private MySQLAdaptor importedDBA;
    // Three types of instances
    private List<GKInstance> identificalInstances;
    private List<GKInstance> changedInstances;
    private List<GKInstance> newInstances;
    
    public AthDatabaseMerger() {
    }
    
    /**
     * Set up memeber variables.
     * @throws Exception
     */
    private void init() throws Exception {
        curatedDBA = new MySQLAdaptor("localhost",
                                      "test_ath_curated2",
                                      "root",
                                      "macmysql01",
                                      3306);
        importedDBA = new MySQLAdaptor("localhost",
                                       "test_ath_imported_original",
                                       "root",
                                       "macmysql01",
                                       3306);
        // Initialize three sets
        identificalInstances = new ArrayList<GKInstance>();
        changedInstances = new ArrayList<GKInstance>();
        newInstances = new ArrayList<GKInstance>();
    }
    
    /**
     * Check instances in the curated database against instances in the imported database.
     * @throws Exception
     */
    private void checkInstances() throws Exception {
        init();
        Collection curatedInstances = curatedDBA.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
        System.out.println("Total curated instances: " + curatedInstances.size());
        // Compare instances in the curated database vs imported database
        for (Iterator it = curatedInstances.iterator(); it.hasNext();) {
            GKInstance curated = (GKInstance) it.next();
            // Check if an instance with same ID existing in the imported db
            GKInstance imported = importedDBA.fetchInstance(curated.getDBID());
            if (imported == null) {
                newInstances.add(curated);
            }
            else {
                // Compare this with curated
                // A specical case
                boolean isSameClass = compareSchemaClass(curated, imported);
                if (!isSameClass) {
                    // Should treat the curated as new
                    newInstances.add(curated);
                }
                else {
                    boolean isSame = InstanceUtilities.compare(curated, imported);
                    if (isSame)
                        identificalInstances.add(curated);
                    else
                        changedInstances.add(curated);
                }
            }
        }
    }
    
    /**
     * This method is used to compare if two instances have the same class.
     * @param curated
     * @param imported
     * @return
     * @throws Exception
     */
    private boolean compareSchemaClass(GKInstance curated,
                                       GKInstance imported) throws Exception {
        if (curated.getDBID().equals(46408L)) {
            // A special case by changing the schema class in the curated database
            // from Compartment in imported db to EntityCompartment in curated db.
            return true;
        }
        SchemaClass cls1 = curated.getSchemClass();
        SchemaClass cls2 = imported.getSchemClass();
        return cls1.getName().equals(cls2.getName());
    }
    
    /**
     * Create a new curator tool project.
     * @return
     * @throws Exception
     */
    private void checkOutAsNewProject() throws Exception {
        checkInstances();
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        fileAdaptor.reset();
        SynchronizationManager dbManager = SynchronizationManager.getManager();
        // Need to assume that the local schema is the same as in Mod DBA.
        // Check out new instances first
        for (GKInstance instance : newInstances) {
            dbManager.checkOutShallowly(instance);
        }
        // Check out changed instances
        for (GKInstance instance : changedInstances) {
            dbManager.checkOutShallowly(instance);
        }
        // Do a recurse checking out: make sure no shell instances existing in the local project
        List<GKInstance> shellInstances = new ArrayList<GKInstance>();
        while (true) {
            Collection all = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DatabaseObject);
            for (Iterator it = all.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                if (inst.isShell())
                    shellInstances.add(inst);
            }
            if (shellInstances.size() == 0)
                break;
            for (Iterator it = shellInstances.iterator(); it.hasNext();) {
                GKInstance shellInst = (GKInstance) it.next();
                GKInstance dbInst = curatedDBA.fetchInstance(shellInst.getDBID());
                dbManager.updateFromDB(shellInst, dbInst);
            }
            shellInstances.clear();
        }
        // Change all ids of new MOD instances as negative as labels for new instances
        for (GKInstance instance : newInstances) {
            Long dbId = instance.getDBID();
            GKInstance local = fileAdaptor.fetchInstance(dbId);
            local.setDBID(-dbId.longValue());
            fileAdaptor.dbIDUpdated(dbId, local);
            // Set the dirty flag
            fileAdaptor.markAsDirty(local);
        }
        // Set dirty flags for all modified instances
        for (GKInstance instance : changedInstances) {
            GKInstance local = fileAdaptor.fetchInstance(instance.getDBID());
            fileAdaptor.markAsDirty(local);
        }
    }
    
    /**
     * This running method is used to create a curator tool project into a file.
     * @throws Exception
     */
    @Test
    public void generateProjectFromCuratedDB() throws Exception {
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        checkOutAsNewProject();
        String fileName = "/Users/wgm/Documents/gkteam/Esther/AthCuratedProject.rtpj";
        fileAdaptor.save(fileName);
    }
    
}
