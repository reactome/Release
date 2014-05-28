/*
 * Created on Jan 11, 2007
 *
 */
package org.gk.property;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Renderable;

public class DBInfoSearchPane extends SearchDatabasePane {
    private String dbName;
    private String speciesName;
    private JButton browseBtn;
    
    public DBInfoSearchPane() {
        // Have to initialize these two buttons here
        cancelBtn = new JButton("Cancel");
        okBtn = new JButton("OK");
        okBtn.setDefaultCapable(true);
        okBtn.setPreferredSize(cancelBtn.getPreferredSize());
        browseBtn = new JButton();
    }
    
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
    
    public void setSpeciesName(String name) {
        this.speciesName = name;
        if (speciesName != null && speciesName.length() == 0)
            speciesName = null;
    }
    
    public void smartSearch(Renderable r) {
        try {
            if (dbName.equals("UniProt")) {
                smartSearch(r, 
                            ReactomeJavaConstants.ReferencePeptideSequence,
                            ReactomeJavaConstants.geneName);
            }
            else if (dbName.equals("ChEBI")) {
                smartSearch(r,
                            ReactomeJavaConstants.ReferenceMolecule,
                            ReactomeJavaConstants.name);
            }
        }
        catch(Exception e) {
            System.err.println("DBInfoSearchPane.smartSearch(): " + e);
            e.printStackTrace();
        }
    }
    
    private void smartSearch(Renderable r,
                             String clsName,
                             String attName) throws Exception {
        String name = r.getDisplayName();
        tf.setText(name);
        List instances = new ArrayList();
        // First try to find the perfect match
        Collection c = dbAdaptor.fetchInstanceByAttribute(clsName, 
                                                          attName,
                                                          "=",
                                                          name);
        if (c == null || c.size() == 0) {
            // Try contains
            c = dbAdaptor.fetchInstanceByAttribute(clsName, 
                                                   attName,
                                                   "LIKE",
                                                   "%" + name + "%");
        }
        instances.addAll(c);
        filterSearchResults(instances);
        // Event an empty list should be displayed to make GUI correct
        displayInstances(instances);
        if (instances.size() > 0) {
            // Check the first one
            GKInstance first = (GKInstance) instances.get(0);
            first.setAttributeValueNoCheck("isSelected", Boolean.TRUE);
            instanceList.setSelectedIndex(0);
            okBtn.setEnabled(true); // Have to enable this okBtn
        }
    }

    protected JPanel createSearchPane() {
        JPanel searchPane = new JPanel();
        searchPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 4, 0, 4);
        constraints.anchor = GridBagConstraints.WEST;
        String labelText = "Search " + dbName + " for name";
        // Only use species name for UniProt
        if (speciesName != null && dbName.equals("UniProt"))
            labelText += " in " + speciesName + ":";
        else
            labelText += ":";
        JLabel label = new JLabel(labelText);
        searchPane.add(label, constraints);
        if (tf == null)
            tf = new JTextField();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 1;
        searchPane.add(tf, constraints);
        if (checkbox == null)
            checkbox = new JCheckBox("Match whole name only");
        constraints.gridx = 0;
        constraints.gridy = 2;
        searchPane.add(checkbox, constraints);
        if (searchBtn == null) {
            searchBtn = new JButton("Search DB");
            searchBtn.setEnabled(false);
        }
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.insets = new Insets(2, 8, 2, 4);
        searchPane.add(searchBtn, constraints);
        Border border1 = BorderFactory.createEmptyBorder(4, 0, 4, 0);
        Border border2 = BorderFactory.createEtchedBorder();
        searchPane.setBorder(BorderFactory.createCompoundBorder(border2, border1));
        return searchPane;
    }

//    /**
//     * Override the superclass method to enable single selection.
//     */
//    protected void toggleSelection(int row) {
//        ListModel model = instanceList.getModel();
//        int size = model.getSize();
//        for (int i = 0; i < size; i++) {
//            GKInstance instance = (GKInstance) model.getElementAt(i);
//            if (i != row) {
//                instance.setAttributeValueNoCheck("isSelected", Boolean.FALSE);
//            }
//            else {
//                Boolean isSelected = (Boolean) instance.getAttributeValueNoCheck("isSelected");
//                if (isSelected == null || !isSelected.booleanValue()) {
//                    instance.setAttributeValueNoCheck("isSelected", Boolean.TRUE);
//                    okBtn.setEnabled(true);
//                }
//                else {
//                    instance.setAttributeValueNoCheck("isSelected", Boolean.FALSE);
//                    okBtn.setEnabled(false);
//                }
//            }
//        }
//        instanceList.repaint(instanceList.getVisibleRect());
//    }
    
    protected JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        // Add a new browser button
        controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        browseBtn.setText("Browse " + dbName);
        controlPane.add(browseBtn);
        controlPane.add(okBtn);
        controlPane.add(cancelBtn);
        okBtn.setEnabled(false);
        return controlPane;
    }
    
    public void addBrowseAction(ActionListener l) {
        browseBtn.addActionListener(l);
    }
    
    protected JButton getOKBtn() {
        return okBtn;
    }

    protected void filterSearchResults(List instances) throws Exception {
        if (speciesName == null || !dbName.equals("UniProt"))
            return;
        // Filter based on species names
        GKInstance instance = null;
        GKInstance species = null;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)) {
                species = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
                if (!species.getDisplayName().equals(speciesName))
                    it.remove();
            }
        }
    }

    protected String getAttributeName() {
        if (dbName.equals("UniProt"))
            return ReactomeJavaConstants.geneName;
        if (dbName.equals("ChEBI"))
            return ReactomeJavaConstants.name;
        // Should not reach here
        return ReactomeJavaConstants._displayName;
    }

    protected List getSchemaClassNames() {
        List list = new ArrayList();
        if (dbName.equals("UniProt")) {
            list.add(ReactomeJavaConstants.ReferencePeptideSequence);
            // For new schema class
            list.add(ReactomeJavaConstants.ReferenceGeneProduct);
        }
        else if (dbName.equals("ChEBI"))
            list.add(ReactomeJavaConstants.ReferenceMolecule);
        return list;
    }
    
    public String getAccess() {
        DefaultListModel model = (DefaultListModel) instanceList.getModel();
        int size = model.getSize();
        GKInstance instance = null;
        Boolean isSelected = null;
        try {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < size; i++) {
                instance = (GKInstance) model.getElementAt(i);
                isSelected = (Boolean) instance.getAttributeValueNoCheck("isSelected");
                if (isSelected != null && isSelected.booleanValue()) {
                    String access = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);
                    String variant = null;
                    if (instance.getSchemClass().isValidAttribute("variantIdentifier"))
                        variant = (String) instance.getAttributeValue("variantIdentifier");
                    if (variant != null && variant.length() > 0) {
                        if (buffer.length() == 0)
                            buffer.append(variant); // First time
                        else
                            buffer.append(", ").append(variant);
                    }
                    else {
                        if (buffer.length() == 0)
                            buffer.append(access);
                        else
                            buffer.append(", ").append(access);
                    }
                }
            }
            return buffer.toString();
        }
        catch(Exception e) {
            System.err.println("DBInfoSerachPane.getAccess(): " + e);
            e.printStackTrace();
        }
        return null;
    }
}
