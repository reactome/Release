/*
 * Created on Aug 28, 2007
 *
 */
package org.gk.database.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;

/**
 * This class is used to merge a MOD reactome database into gk_central. For example,
 * the fly Reactome into gk_central so that MOD curators can work against gk_central
 * directly.
 * @author guanming
 */
@SuppressWarnings("unchecked")
public class MODReactomeAnalyzer {
    // External MOD Reactome database
    protected MySQLAdaptor modDba;
    // These lists are used to hold instances
    protected Set newMODInstances;
    protected Set changedMODInstances;
    protected boolean needRecursive = true;
    
    public MODReactomeAnalyzer() {
        newMODInstances = new HashSet();
        changedMODInstances = new HashSet();
    }
    
    public void setModReactome(MySQLAdaptor dba) {
        this.modDba = dba;
    }
    
    public Set getNewMODInstances() {
        return newMODInstances;
    }
    
    public Set getChangedMODInstances() {
        return changedMODInstances;
    }
    
    /**
     * List new instances from MOD Reactome. A new instance in the MOD Reactome is
     * an Instance whose created InstanceEdit or modified InstanceEdits are not in
     * gk_central. A modified instance in the MOD Reactome is an Instance whose create
     * InstanceEdit is in gk_central, but some of its modified InstanceEdits are not.
     * @throws Exception
     */
    public void checkMODInstances() throws Exception {
        GKInstance lastIE = fetchLastInstanceEdit();
        // Get all new IEs
        Collection c = modDba.fetchInstanceByAttribute(ReactomeJavaConstants.InstanceEdit, 
                                            ReactomeJavaConstants.DB_ID, 
                                            ">", 
                                            lastIE.getDBID());
        // This set is used to grep any IEs created by Esther, by used by MOD
        Set estherIEs = new HashSet();
        // Intances related to these 
        for (Iterator it = c.iterator(); it.hasNext();) {
            GKInstance ie = (GKInstance) it.next();
            // Maybe a script has been used to change the schem. Remove them:
            String note = (String) ie.getAttributeValue(ReactomeJavaConstants.note);
            if (note != null) {
                // Have to ask Esther to mark these InstanceEdits
                if (note.startsWith("schema_change: assignment") ||
                    note.startsWith("switched to BlackBoxEvent") ||
                    note.startsWith("schema change - merging Pathway and ConceptualEvent")) {
                    // Though these IEs should not be used to pull out referrers. But these should
                    // be treated as new Instances in case they are referred by other instances. 
                    // If not, their DB_IDs may be used somewhere in gk_central
                    //newMODInstances.add(ie);
                    estherIEs.add(ie);
                    continue;
                }
            }
            // If these instances are used in created slots. New instances
            Collection referrers = ie.getReferers(ReactomeJavaConstants.created);
            if (referrers != null)
                newMODInstances.addAll(referrers);
            referrers = ie.getReferers(ReactomeJavaConstants.modified);
            if (referrers != null)
                changedMODInstances.addAll(referrers);
            // Dont't forget this InstanceEdit
            newMODInstances.add(ie);
        }
        // New MOD instances may be in changedMOD instances
        changedMODInstances.removeAll(newMODInstances);
        // Check Esther's IEs. Only used IEs will be treated as new IEs
        for (Iterator it = estherIEs.iterator(); it.hasNext();) {
            GKInstance ie = (GKInstance) it.next();
            Collection referrers = new HashSet();
            c = ie.getReferers(ReactomeJavaConstants.modified);
            if (c != null)
                referrers.addAll(c);
            c = ie.getReferers(ReactomeJavaConstants.created);
            if (c != null)
                referrers.addAll(c);
            if (referrers.size() == 0)
                continue;
            // Check if referrers is in the two MOD sets
            for (Iterator it1 = referrers.iterator(); it1.hasNext();) {
                GKInstance referrer = (GKInstance) it1.next();
                if (changedMODInstances.contains(referrer)) {
                    newMODInstances.add(ie);
                    break;
                }
                if (newMODInstances.contains(referrer)) {
                    newMODInstances.add(ie);
                    break;
                }
            }
        }
    }

    protected GKInstance fetchLastInstanceEdit() throws Exception {
        // This is a shortcut to get instances in different categories.
        // We will try to get referrers for new InstanceEdit instances in MOD. If 
        // instances in MOD use these new InstanceEdits, these instances are either
        // new or modified. If new instances are used in created slot, instances should
        // be treated as new. Otherwise, instances should be treated as modified.
        // The last InstanceEdit that is created by Esther's script actually can be retrived.
        // Get InstanceEdits based on note, inferred events based on orthomcl. These
        // InstanceEdit should be created by Esther's script, and should be the last 
        // InstanceEdit.
        Collection c = modDba.fetchInstanceByAttribute(ReactomeJavaConstants.InstanceEdit,
                                                       ReactomeJavaConstants.note,
                                                       "=",
                                                       "inferred events based on orthomcl");
        GKInstance lastIE = null;
        for (Iterator it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            if (lastIE == null)
                lastIE = inst;
            else {
                Long lastId = lastIE.getDBID();
                Long newId = inst.getDBID();
                if (newId.longValue() > lastId.longValue()) {
                    lastId = newId;
                }
            }
        }
        return lastIE;
    }
    
    /**
     * Create a new curator tool project from newly created MOD instances and
     * modified instances. This method should be called after checkMODInstance.
     * Otherwise, nothing will be checked out.
     * @return
     * @throws Exception
     */
    public void checkOutAsNewProject() throws Exception {
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        fileAdaptor.reset();
        SynchronizationManager dbManager = SynchronizationManager.getManager();
        // Need to assume that the local schema is the same as in Mod DBA.
        // Check out new instances first
        for (Iterator it = newMODInstances.iterator(); it.hasNext();) {
            GKInstance newMod = (GKInstance) it.next();
            dbManager.checkOutShallowly(newMod);
        }
        // Check out changed instances
        for (Iterator it = changedMODInstances.iterator(); it.hasNext();) {
            GKInstance changedMod = (GKInstance) it.next();
            dbManager.checkOutShallowly(changedMod);
        }
        // Do a recurse checking out: make sure no shell instances existing in the local project
        while (true) {
            List shellInstances = new ArrayList();
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
                GKInstance dbInst = modDba.fetchInstance(shellInst.getDBID());
                dbManager.updateFromDB(shellInst, dbInst);
            }
            if (!needRecursive)
                break; // Just run once
        }
        markInstances(fileAdaptor);
    }

    /**
     * Flip positive DB_IDs to negative DB_IDs if needed. All instances should be marked as dirty.
     * @param fileAdaptor
     */
    protected void markInstances(XMLFileAdaptor fileAdaptor) {
        // Change all ids of new MOD instances as negative as labels for new instances
        for (Iterator it = newMODInstances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            Long dbId = instance.getDBID();
            GKInstance local = fileAdaptor.fetchInstance(dbId);
            local.setDBID(new Long(-dbId.longValue()));
            fileAdaptor.dbIDUpdated(dbId, local);
            // Set the dirty flag
            fileAdaptor.markAsDirty(local);
        }
        // Set dirty flags for all modified instances
        for (Iterator it = changedMODInstances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            GKInstance local = fileAdaptor.fetchInstance(instance.getDBID());
            fileAdaptor.markAsDirty(local);
        }
    }
    
}
