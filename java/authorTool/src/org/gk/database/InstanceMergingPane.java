/*
 * Created on Jul 22, 2004
 * 
 * A customized InstanceComparisonPane specified for merging two instances.
 */
package org.gk.database;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.gk.model.GKInstance;

public class InstanceMergingPane extends InstanceComparisonPane {
    private boolean isOKClicked = true;

    private JButton okBtn;

    public InstanceMergingPane() {
        super();
    }
    
    public void setInstances(GKInstance instance1, GKInstance instance2) {
        super.setInstances(instance1, instance2);
        merge();
    }
    
    public void setInstances(GKInstance instance1, GKInstance instance2,
                             String title1, String title2) {
        super.setInstances(instance1, instance2, title1, title2);
        merge();
    }

    protected JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        okBtn = new JButton("OK");
        okBtn.setMnemonic('O');
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Have to make sure the used DB_ID is one from two instances
                if(validateDBID()) {
                    isOKClicked = true;
                    dispose();
                }
                else {
                    JOptionPane.showMessageDialog(InstanceMergingPane.this,
                                                  "You have to use a DB_ID from two merging instances.",
                                                  "Error in DB_ID",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        });
        okBtn.setEnabled(false);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isOKClicked = false;
                dispose();
            }
        });
        cancelBtn.setMnemonic('C');
        okBtn.setPreferredSize(cancelBtn.getPreferredSize());
        controlPane.add(okBtn);
        controlPane.add(cancelBtn);
        return controlPane;
    }

    public boolean isOKClicked() {
        return this.isOKClicked;
    }
    
    private boolean validateDBID() {
        Long dbID1 = getFirstInstance().getDBID();
        Long dbID2 = getSecondInstance().getDBID();
        Long selectedDBID = getMerged().getDBID();
        if (selectedDBID != null) {
            if (dbID1 != null && dbID1.equals(selectedDBID))
                return true;
            if (dbID2 != null && dbID2.equals(selectedDBID))
                return true;
            return false;
        }
        else {
            if (dbID1 == null || dbID2 == null)
                return true;
            return false;
        }
    }

    protected void merge() {
        super.merge();
        // Pick the smaller one for DB_ID
        GKInstance merged = getMerged();
        Long dbID1 = getFirstInstance().getDBID();
        Long dbID2 = getSecondInstance().getDBID();
        if (dbID1 == null && dbID2 != null)
            merged.setDBID(dbID2);
        else if (dbID1 != null && dbID2 == null)
            merged.setDBID(dbID2);
        else if (dbID1 != null && dbID2 != null) {
            // Use the larger one in case local instance and db instance merged,
            // so that the db one can be picked.
            if (dbID1.longValue() > dbID2.longValue())
                merged.setDBID(dbID1);
            else
                merged.setDBID(dbID2);
        }
        attPane.refresh();
        // To make all slots editable
        attPane.setIsAllSlotEditable(true);
        // make it invisile.
        mergeBtn.setVisible(false);
        saveMergingBtn.setVisible(false);
        okBtn.setEnabled(true);
    }
}