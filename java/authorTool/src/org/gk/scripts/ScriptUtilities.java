/*
 * Created on Apr 25, 2012
 *
 */
package org.gk.scripts;

import java.sql.SQLException;
import java.util.List;

import org.gk.database.DefaultInstanceEditHelper;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;

/**
 * @author gwu
 *
 */
public class ScriptUtilities {
    
    /**
     * A method to update instance name, _displayName, and adding an IE
     * to the updated instance's modified slot.
     * @param dba
     * @param toBeUpdated
     * @throws SQLException
     * @throws Exception
     */
    public static void updateInstanceNames(MySQLAdaptor dba, List<GKInstance> toBeUpdated) throws SQLException, Exception {
        // Update instances to the database
        try {
            dba.startTransaction();
            Long defaultPersonId = 140537L; // For Guanming Wu at CSHL
            GKInstance newIE = ScriptUtilities.createDefaultIE(dba, defaultPersonId, true);
            int count = 0;
            for (GKInstance instance : toBeUpdated) {
                System.out.println(count + ": " + instance);
                // Have to call this first to get the list
                instance.getAttributeValue(ReactomeJavaConstants.modified);
                instance.addAttributeValue(ReactomeJavaConstants.modified, newIE);
                dba.updateInstanceAttribute(instance, ReactomeJavaConstants.modified);
                if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.name))
                    dba.updateInstanceAttribute(instance, ReactomeJavaConstants.name);
                dba.updateInstanceAttribute(instance, ReactomeJavaConstants._displayName);
                count ++;
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
            throw e;
        }
    }
    
    public static GKInstance createDefaultIE(MySQLAdaptor dba,
                                             Long defaultPersonId,
                                             boolean needStore) throws Exception {
        GKInstance defaultPerson = dba.fetchInstance(defaultPersonId);
        DefaultInstanceEditHelper ieHelper = new DefaultInstanceEditHelper();
        GKInstance newIE = ieHelper.createDefaultInstanceEdit(defaultPerson);
        newIE.addAttributeValue(ReactomeJavaConstants.dateTime,  
                                GKApplicationUtilities.getDateTime());
        InstanceDisplayNameGenerator.setDisplayName(newIE);
        if (needStore)
            dba.storeInstance(newIE);
        return newIE;
    }
    
    /**
     * Get the acutal author for an instance.
     * @param inst
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static GKInstance getAuthor(GKInstance inst) throws Exception {
        List<GKInstance> list = inst.getAttributeValuesList(ReactomeJavaConstants.modified);
        if (list != null) {
            for (GKInstance ie : list) {
                if (ie.getDisplayName().startsWith("Wu, G")) { // Escape Guanming Wu
                    continue;
                }
                return ie;
            }
        }
        return (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.created);
    }
    
}
