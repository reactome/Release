/*
 * Created on Jan 20, 2005
 *
 */
package org.gk.database;

import java.util.ArrayList;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;

/**
 * This utility class is used to compare a local GKInstance and its counterpart 
 * in the database.
 * @author wgm
 *
 */
public class InstanceComparer {
    // Constants definition for different cases
    /**
     * Two instances are the same
     */
    public static final int IS_IDENTICAL = 0;
    /**
     * Changes in the local GKInstance only
     */
    public static final int NEW_CHANGE_IN_LOCAL = 1;
    /**
     * Changes in the db GKInstance only
     */
    public static final int NEW_CHANGE_IN_DB = 2;
    /**
     * Both have changes
     */
    public static final int CONFLICT_CHANGE = 3;
    /**
     * Local instance has more IEs in modified slot. This is usually a
     * wrong case. However, if an instance is pulled from other database,
     * this case may occur.
     */
    public static final int LOCAL_HAS_MORE_IE = 4;
    
    public InstanceComparer() {
    }
    
    /**
     * The comparasion in this method is around modified slot in GKInstance. There are three cases:
     * 1). Both have this slot filled: a).If the last InstanceEdit is the same, the local has no change, 
     * the result should be the same? b).If the last InstanceEdit is the same, the local has changes, 
     * the change is assumed as new. c). If the last InstanceEdit in the db is newer than in the local, 
     * there is no local change, the db change is new. d).If the last InstanceEdit in the db is newer 
     * than in the local, there is local change, the changes should be regards as conflict. It is not 
     * possible the last InstanceEdit in the db later than in the local
     * 2). Both slot are empty: There should be no changes in the db. a). If the local has no change, 
     * both are the same. b). If the local has changes, the change should be new.
     * 3). DB has modified slot but the local doesn't: There are new changes in the db. a). If the local 
     * has no change, the change in the db is new. b). If the local has changes, the changes are conflicting.
     * 4). No such a case: local has modified slot but db doesn't.
         
     * @param localInstance
     * @param dbInstance
     * @return one of constants defined in this class: IS_IDENTICAL, NEW_CHANGE_IN_DB, NEW_CHANGE_IN_LOCAL,
     * CONFLICT_CHANGE.
     * @throws Exception
     */
    public int compare(GKInstance localInstance, 
                       GKInstance dbInstance) throws Exception {
        List localModified = localInstance.getAttributeValuesList("modified");
        if (localModified == null) // To make syntax simple
            localModified = new ArrayList();
        List dbModified = dbInstance.getAttributeValuesList("modified");
        if (dbModified == null)
            dbModified = new ArrayList();
        // Starts with simple case
        // case 2
        if (localModified.size() == 0 && dbModified.size() == 0) {
            if (localInstance.isDirty())
                return NEW_CHANGE_IN_LOCAL;
            else
                return IS_IDENTICAL;
        }
        // case 3
        if (localModified.size() == 0 && dbModified.size() > 0) {
            if (localInstance.isDirty())
                return CONFLICT_CHANGE;
            else
                return NEW_CHANGE_IN_DB;
        }
        if (localModified.size() > dbModified.size()) {
            // Have to make sure all db IEs are in the local.
            // If not, it should be CONFLICT_CHANGE
            for (int i = 0; i < dbModified.size(); i++) {
                GKInstance local = (GKInstance) localModified.get(i);
                GKInstance db = (GKInstance) dbModified.get(i);
                boolean isSame = InstanceUtilities.compareInstanceEdits(local, db);
                if (!isSame)
                    return CONFLICT_CHANGE;
            }
            return LOCAL_HAS_MORE_IE;
        }
        // case 1
        if (localModified.size() > 0 && dbModified.size() > 0) {
            // size of dbModifier should always be greater than local one.
            // The opposite has been checked before
            // Have to make sure all local instances have been covered by
            // db instances
            boolean isIESame = true;
            // local has less IEs
            for (int i = 0; i < localModified.size(); i++) {
                GKInstance local = (GKInstance) localModified.get(i);
                GKInstance db = (GKInstance) dbModified.get(i);
                boolean isSame = InstanceUtilities.compareInstanceEdits(local, db);
                if (!isSame) {
                    isIESame = false;
                    break;
                }
            }
            if (isIESame) {
                if (localModified.size() == dbModified.size()) { // Both of the InstanceEdit are the same
                    if (localInstance.isDirty()) 
                        return NEW_CHANGE_IN_LOCAL; // Case 1.b
                    else
                        return IS_IDENTICAL; // case 1.a
                }
                else { // db has more changes
                    if (localInstance.isDirty())
                        return CONFLICT_CHANGE; // case 1.d
                    else
                        return NEW_CHANGE_IN_DB; // case 1.c
                }
            }
            else { // The local should NOT have later InstanceEdit
                return CONFLICT_CHANGE;
            }
        }
        // Should never be here
        throw new IllegalStateException("Unexpected values in the modifier slot: Instance " + dbInstance.getDBID());
    }
}
