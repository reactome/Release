/*
 * Created on Feb 21, 2008
 *
 */
package org.gk.gkCurator;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.gk.database.FrameManager;
import org.gk.database.InstanceSelectDialog;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.util.GKApplicationUtilities;

/**
 * This is a helper class for choosing a default person for a project.
 * @author wgm
 *
 */
public class PersonChoosingHelper {
    
    public PersonChoosingHelper() {
        
    }
    
    public GKInstance choosePerson(JDialog parentDialog) {
        String title = "Choose a Person instance for the default InstanceEdit:";
        InstanceSelectDialog dialog = new InstanceSelectDialog(parentDialog, title);
        List cls = new ArrayList(1);
        cls.add(PersistenceManager.getManager().getActiveFileAdaptor().getSchema().getClassByName("Person"));
        dialog.setTopLevelSchemaClasses(cls);
        dialog.setIsMultipleValue(false);
        dialog.setModal(true);
        dialog.setSize(1000, 700);
        GKApplicationUtilities.center(dialog);
        dialog.setVisible(true);
        if (dialog.isOKClicked()) {
            List list = dialog.getSelectedInstances();
            if (list != null && list.size() > 0) {
                GKInstance person = (GKInstance) list.get(0);
                return person;
            }
        }
        return null;
    }
    
    public void viewPerson(Long dbId,
                           JDialog parentDialog,
                           boolean skipLocalChecking) {
        GKInstance person = null;
        if (!skipLocalChecking) {
            // Need to check local project first
            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            person = fileAdaptor.fetchInstance(dbId);
            if (person != null) {
                FrameManager.getManager().showInstance(person, 
                                                       parentDialog, 
                                                       true);
                return;
            }
        }
        // Need to go do the database
        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(parentDialog);
        if (dba == null)
            return;
        try {
            person = dba.fetchInstance(dbId);
            if (person == null) {
                JOptionPane.showMessageDialog(parentDialog,
                                              "Person " + dbId + " has been deleted in the database!",
                                              "Error in Fetching Person",
                                              JOptionPane.ERROR_MESSAGE);
            }
            else {
                FrameManager.getManager().showInstance(person, 
                                                       parentDialog, 
                                                       false);
            }
        }
        catch(Exception e) {
            System.err.println("PersonChooseDialog.view(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentDialog, 
                                          "Error in Fetching Person",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
}
