/*
 * Created on Feb 4, 2013
 *
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.apache.batik.ext.swing.GridBagConstants;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;

/**
 * A simple class that is used to do two instances merging.
 * @author gwu
 *
 */
public class InstanceMerger {
    
    public InstanceMerger() {
    }
    
    public void mergeInstances(List<GKInstance> instances,
                               final JFrame parentFrame) {
        if (instances.size() != 2)
            return;
        // Merge two instances
        GKInstance instance1 = (GKInstance) instances.get(0);
        GKInstance instance2 = (GKInstance) instances.get(1);
        InstanceMergingPane mergingPane = new InstanceMergingPane();
        if (!mergingPane.isMergable(instance1, instance2))
            return;
        // Give a warning for references in DBs
        String message = "Please make sure all instances referring to these two merging ones have\n" +
                "been checked out into your local project. Otherwise, instances in the\n" +
                "database may lose reference to the instance that is merged away after\n" +
                "committing to the database. Are you sure you want to continue merging?";
        int reply = JOptionPane.showConfirmDialog(parentFrame, 
                                                  message, 
                                                  "Warning: Merging Instances", 
                                                  JOptionPane.YES_NO_OPTION);
        if (reply != JOptionPane.YES_OPTION)
            return;
        mergingPane.setInstances(instance1, 
                                 instance2);
        String title = "Merge Two Instances in Class \"" + instance1.getSchemClass().getName() + "\"";
        JDialog dialog = GKApplicationUtilities.createDialog(parentFrame, title);
        //dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.getContentPane().add(mergingPane, BorderLayout.CENTER);
        dialog.setModal(true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(mergingPane);
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!mergingPane.isOKClicked())
            return;
        GKInstance merged = mergingPane.getMerged();
        // Find which instance is removed based on DB_ID
        GKInstance deletingInstance = null;
        GKInstance keptInstance = null;
        // Note: the returned DB_ID is a Long object. Don't use
        // "=" to check if they are the same!
        if (merged.getDBID().equals(instance1.getDBID())) {
            deletingInstance = instance2;
            keptInstance = instance1;
        }
        else {
            deletingInstance = instance1;
            keptInstance = instance2;
        }
        copyValuesFromMergedToKept(merged, 
                                   keptInstance);
        AttributeEditManager.getManager().attributeEdit(keptInstance);
        XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        adaptor.markAsDirty(keptInstance);
        try {
            replaceReferrersForDeletedInstance(deletingInstance,
                                               keptInstance,
                                               adaptor);
            final GKInstance kept = keptInstance;
            // In order to get back the _Deleted instance if it is created
            PropertyChangeListener propListener = new PropertyChangeListener() {
                
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("addNewInstance")) {
                        List<GKInstance> list = (List<GKInstance>) evt.getNewValue();
                        if (list == null || list.size() == 0)
                            return;
                        GKInstance _deleted = list.get(0);
                        if (_deleted.getSchemClass().isa(ReactomeJavaConstants._Deleted)) {
                            // Want to make some editing to this instance
                            modifyDeletedInstance(_deleted,
                                                  kept,
                                                  parentFrame);
                        }
                    }
                }
            };
            adaptor.addInstanceListener(propListener);
            // Deleting the deletingInstance
            //                adaptor.deleteInstance(deletingInstance);
            InstanceDeletion deletion = new InstanceDeletion();
            List<GKInstance> instanceList = Collections.singletonList(deletingInstance);
            deletion.delete(instanceList, 
                            parentFrame,
                            false);
            adaptor.removeInstanceListener(propListener);
            // Have to make sure default InstanceEdit is correct
            if (deletingInstance.getSchemClass().getName().equals("Person")) {
                GKInstance defaultInstanceEdit = SynchronizationManager.getManager().getDefaultInstanceEdit(parentFrame);
                if (defaultInstanceEdit != null) {
                    GKInstance instance = (GKInstance) defaultInstanceEdit.getAttributeValue("author");
                    if (instance == deletingInstance) {
                        defaultInstanceEdit.removeAttributeValueNoCheck("author", instance);
                        defaultInstanceEdit.addAttributeValue("author", keptInstance);
                    }
                }
            }
            showInformationDialog(keptInstance,
                                  deletingInstance,
                                  parentFrame);
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame,
                                          "Error in handling merged away instance: " + e.getMessage(),
                                          "Error in Instance Editing", 
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showInformationDialog(GKInstance keptInstance,
                                       GKInstance deletedInstance,
                                       JFrame parentFrame) {
        String message = "\"" + keptInstance + "\" and \"" + deletedInstance + "\" " +
        		         "have been merged. \"" + deletedInstance + "\" has been deleted. " + 
        		         "A _Deleted instance has been created. Please don't forget to " +
        		         "commit it into the database.";
        final JDialog dialog = GKApplicationUtilities.createDialog(parentFrame,
                                                             "Instances Merged");
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getCancelBtn().setVisible(false);
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        
        dialog.getRootPane().setDefaultButton(controlPane.getOKBtn());
        dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        JPanel centerPane = new JPanel();
        Border inborder = BorderFactory.createEmptyBorder(8, 8, 8, 8);
        Border outBorder = BorderFactory.createEtchedBorder();
        centerPane.setBorder(BorderFactory.createCompoundBorder(outBorder, inborder));
        centerPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstants.HORIZONTAL;
        constraints.weightx = 1.0d;
        JTextArea ta = new JTextArea();
        ta.setText(message);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBackground(dialog.getContentPane().getBackground());
        centerPane.add(ta, constraints);
        dialog.getContentPane().add(centerPane, BorderLayout.CENTER);
        
        dialog.setModal(true);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setSize(400, 300);
        dialog.setVisible(true);
        
    }
    
    private void modifyDeletedInstance(GKInstance _deleted,
                                       GKInstance keptInstance,
                                       JFrame parentFrame) {
        try {
            _deleted.setAttributeValue(ReactomeJavaConstants.replacementInstances,
                                       keptInstance);
            XMLFileAdaptor fileAdpator = PersistenceManager.getManager().getActiveFileAdaptor();
            Collection<?> c = fileAdpator.fetchInstanceByAttribute(ReactomeJavaConstants.DeletedControlledVocabulary,
                                                                   ReactomeJavaConstants._displayName,
                                                                   "=",
                                                                   "Merged");
            if (c == null || c.size() == 0)
                return;
            GKInstance cv = (GKInstance) c.iterator().next();
            _deleted.setAttributeValue(ReactomeJavaConstants.reason, cv);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame,
                                          "Error in editing _Deleted instance: " + e.getMessage(),
                                          "Error in Instance Editing",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void replaceReferrersForDeletedInstance(GKInstance deletingInstance,
                                                    GKInstance keptInstance,
                                                    XMLFileAdaptor adaptor) throws Exception {
        // Change the referrers to deleting instance to instance1
        java.util.List referrers = adaptor.getReferers(deletingInstance);
        if (referrers != null && referrers.size() > 0) {
            for (Iterator it = referrers.iterator(); it.hasNext();){
                GKInstance referrer = (GKInstance) it.next();
                // Find the referrer and replace it with keptInstance
                InstanceUtilities.replaceReference(referrer, deletingInstance, keptInstance);
                AttributeEditManager.getManager().attributeEdit(referrer);
                adaptor.markAsDirty(referrer);
            }
        }
    }

    private void copyValuesFromMergedToKept(GKInstance merged,
                                            GKInstance keptInstance) {
        // Replace the instance1 with the merged instance
        for (Iterator it = keptInstance.getSchemaAttributes().iterator(); it.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute) it.next();
            try {
                java.util.List values = merged.getAttributeValuesList(att);
                keptInstance.setAttributeValueNoCheck(att, values);
            }
            catch (Exception e) {
                System.err.println("CuratorActionCollection.mergeInstances(): " + e);
                e.printStackTrace();
            }
        }
    }
    
}
