/*
 * Created on May 24, 2010
 *
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaClass;
import org.gk.util.StringUtils;

/**
 * This class is used to do instance deletion.
 * @author wgm
 *
 */
public class InstanceDeletion {
    
    public InstanceDeletion() {
    }
    
    /**
     * Delete a list of GKInstance.
     * @param list
     * @param parentFrame
     */
    public void delete(List<GKInstance> list,
                       JFrame parentFrame) {
        delete(list, parentFrame, true);
    }
    
    public void delete(List<GKInstance> list,
                       JFrame parentFrame,
                       boolean needWarning) {
        if (list == null || list.size() == 0)
            return;
        if (needWarning) {
            int reply = JOptionPane.showConfirmDialog(parentFrame,
                                                      "Are you sure you want to delete the selected instance" + (list.size() == 1 ? "" : "s") + "? " +
                                                              "The slot values in \nother instances referring the deleted instances will be set to null.",
                                                              "Delete Confirmation",
                                                              JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                return;
        }
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        
        // Create a _Deleted instance
        // These instances are used to keep track of
        // deletions.
        try {
            boolean isCreateDeleteInstanceOK = createDeleteInstance(fileAdaptor, 
                                                                    list,
                                                                    parentFrame, 
                                                                    needWarning);
            // Do the actual deletions
            if (isCreateDeleteInstanceOK) {
                for (GKInstance instance : list) {
                    fileAdaptor.deleteInstance(instance);
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame,
                                          "Error in deletion: " + e.getMessage(),
                                          "Error in Deletion",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void delete(GKInstance instance,
                       JFrame parentFrame) {
        List<GKInstance> list = Collections.singletonList(instance);
        delete(list, parentFrame);
    }
    
    /**
     * Given a list of instances that should be deleted, create a new
     * instance of class _Deleted and add it to the fileAdaptor.
     * This is used to track deletions.
     * 
     * N.B. if the user deletes a "_Deleted" instance, no new "_Deleted"
     * instance will be created.  Also, if the instance being deleted
     * was created locally (i.e. is not in the database), then no
     * "_Deleted" instance will be created.
     * 
     * Returns true if everything was OK, false if an error was
     * detected or if the user cancels.
     * 
     * @param dbAdaptor
     * @param instance
     */
    private boolean createDeleteInstance(XMLFileAdaptor fileAdaptor,
                                         List<?> instances,
                                         JFrame parentFrame,
                                         boolean needWarning) throws Exception {
        if (!isDeletedNeeded(instances))
            return true; // Correct handling
        // Make sure DeletedControlledVocabulary existing
        if(!downloadControlledVocabulary(parentFrame))
            return false;
        // Create an instance of class "_Deleted".  It's done
        // in this rather complicated way, rather than using
        // fileAdaptor.createNewInstance, because this latter
        // method inserts two copies of the instance into
        // fileAdaptor's cache.
        SchemaClass schemaClass = fileAdaptor.getSchema().getClassByName(ReactomeJavaConstants._Deleted);
        Long dbID = fileAdaptor.getNextLocalID();
        GKInstance deleted = new GKInstance(schemaClass, dbID, fileAdaptor);
        deleted.setDBID(dbID);
        
        // Add the DB_IDs of all instances to be deleted
        boolean notFirst = false; // A flag in order to control is ", " should be added
        //TODO A bug in the data model: using integer, instead of long for _Deleted's deltedInstanceDB_ID
        // This should be fixed!
        List<Integer> deletedIds = new ArrayList<Integer>();
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance)it.next();
            if (instance.getSchemClass().isa(ReactomeJavaConstants._Deleted))
                continue;
            if (instance.getDBID() < 0)
                continue;
            deletedIds.add(instance.getDBID().intValue());
        }
        // For sure deletedIds should not be null. However, just in case, do this extra check
        if (deletedIds.size() == 0)
            return true;
        Collections.sort(deletedIds);
        String displayName = null;
        if (deletedIds.size() == 1)
            displayName = "Deletion of instance: " + StringUtils.join(", ", deletedIds);
        else
            displayName = "Deletion of instances: " + StringUtils.join(", ", deletedIds);
        deleted.setDisplayName(displayName);
        deleted.setAttributeValue(ReactomeJavaConstants.deletedInstanceDB_ID,
                                  deletedIds);
        if (needWarning) {
            // Let the curator add comment, etc.
            DeletedInstanceDialog deletedInstanceDialog = new DeletedInstanceDialog(parentFrame, deleted);
            deletedInstanceDialog.setModal(true);
            deletedInstanceDialog.setSize(475, 600);
            deletedInstanceDialog.setLocationRelativeTo(parentFrame);
            deletedInstanceDialog.setVisible(true);
            if (!deletedInstanceDialog.isOKClicked)
                return false; // Cancel has been clicked. Abort the whole deletion
            if (deletedInstanceDialog.isDeletedInstanceNeeded)
                fileAdaptor.addNewInstance(deleted);
        }
        else {
            fileAdaptor.addNewInstance(deleted);
        }
        return true;
    }
    
    /**
     * Check if we need a _Deleted instance. If the passed list contains local instances
     * and _Deleted instances only, no need to create a _Deleted instance.
     * @param instances
     * @return
     */
    private boolean isDeletedNeeded(List<?> instances) {
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance)it.next();
            
            if (instance.getSchemClass().isa(ReactomeJavaConstants._Deleted))
                continue;
            if (instance.getDBID() < 0)
                continue;
            
            return true;
        }
        return false;
    }
    
    private boolean downloadControlledVocabulary(JFrame parentFrame) {
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        Collection<?> instances;
        
        // First check to see if any instances of this
        // class are already present - if so, assume
        // that we don't need to check out anything.
        try {
            instances = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DeletedControlledVocabulary);
            if (instances != null && instances.size() > 0) {
                // Some instances already present locally, so don't
                // try to check any out from database.
                return true; 
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        
        MySQLAdaptor dbAdaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(parentFrame);
        if (dbAdaptor == null)
            return false;
        try {
            instances = dbAdaptor.fetchInstancesByClass(ReactomeJavaConstants.DeletedControlledVocabulary);
        } 
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (instances == null || instances.size() == 0)
            return true; // It is possible there is nothing in the database for this class.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<?> instancesList = new ArrayList(instances);
        try {
            SynchronizationManager.getManager().checkOut(instancesList, 
                                                         parentFrame);
            return true;
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * This customized JDialog is used to create a new _Deleted object.
     * @author croft
     */
    private class DeletedInstanceDialog extends JDialog {
        private AttributePane attributePane;
        private boolean isOKClicked;
        private boolean isDeletedInstanceNeeded = false;
        
        public DeletedInstanceDialog(JFrame parentFrame, GKInstance instance) {
            super(parentFrame, "Deletion information");
            attributePane = new AttributePane(instance);
            init();
        }
        
        private void init() {
            // Add instance pane
            attributePane.refresh();
            attributePane.setEditable(true);
            attributePane.setBorder(BorderFactory.createRaisedBevelBorder());
            attributePane.setTitle("Enter information pertaining to deleted instance:");
            getContentPane().add(attributePane, BorderLayout.CENTER);
            // Add buttons
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.Y_AXIS));
            JButton okBtn = new JButton("Delete with _Deleted Instance");
            okBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = true;
                    attributePane.stopEditing();
                    isDeletedInstanceNeeded = true;
                    dispose();
                }
            });
            okBtn.setMnemonic('O');
            okBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            JButton skipBtn = new JButton("Delete without _Deleted Instance");
            skipBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = true;
                    attributePane.stopEditing();
                    isDeletedInstanceNeeded = false;
                    dispose();
                }
            });
            skipBtn.setMnemonic('S');
            skipBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = false; // Just abort the whole deletion process
                    dispose();
                }
            });
            cancelBtn.setMnemonic('C');
            cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            okBtn.setDefaultCapable(true);
            okBtn.setMinimumSize(skipBtn.getPreferredSize());
            cancelBtn.setMinimumSize(skipBtn.getPreferredSize());
            buttonPane.add(Box.createRigidArea(new Dimension(5, 5)));
            buttonPane.add(okBtn);
            buttonPane.add(Box.createRigidArea(new Dimension(5, 5)));
            buttonPane.add(skipBtn);
            buttonPane.add(Box.createRigidArea(new Dimension(5, 5)));
            buttonPane.add(cancelBtn);
            buttonPane.add(Box.createRigidArea(new Dimension(5, 5)));
            getContentPane().add(buttonPane, BorderLayout.NORTH);
        }
        
        public boolean isOKClicked() {
            return this.isOKClicked;
        }
        
        public boolean isDeletedInstanceNeeded() {
            return this.isDeletedInstanceNeeded;
        }
    }
}
