/*
 * Created on Jan 20, 2006
 *
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;

import org.apache.batik.ext.swing.GridBagConstants;
import org.gk.database.AttributeEditConfig;
import org.gk.database.AttributeEditManager;
import org.gk.database.InstanceListPane;
import org.gk.database.InstanceSelectDialog;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TextDialog;

/**
 * This customized JFrame is used to edit a list of GKInstance objects in a batch mode.
 * @author guanming
 *
 */
public class MultipleInstanceEditDialog extends JDialog {
    // To display the list
    private InstanceListPane listPane;
    private DialogControlPane controlPane;
    // To display the list of editable attributes
    private JComboBox attributeList;
    // To choose an action
    private JRadioButton addBtn;
    private JRadioButton setBtn;
    private JRadioButton removeBtn;
    // Action text
    private JTextArea applyText;
    private JLabel applyLabel;
    // instances list to be edited
    private List instances;
    // Need to cache the schema class
    private SchemaClass commonCls;
    // Cache the action results
    private JRadioButton actionBtn;
    private List actionValues;
    
    public MultipleInstanceEditDialog() {
        init();
    }
    
    public MultipleInstanceEditDialog(JFrame parentFrame) {
        super(parentFrame);
        init();
    }
    
    private void init() {
        setTitle("Edit Instances in the Batch Mode");
        controlPane = new DialogControlPane();   
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                commit();
            }
        });
        listPane = new InstanceListPane();
        // ContentPane
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        contentPane.add(listPane);
        // To display attributes
        JPanel attListPane = new JPanel();
        Border emptyBorder = BorderFactory.createEmptyBorder(2, 4, 2, 4);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border compoundBorder = BorderFactory.createCompoundBorder(etchedBorder, emptyBorder);
        attListPane.setBorder(compoundBorder);
        attListPane.setLayout(new BoxLayout(attListPane, BoxLayout.X_AXIS));
        JLabel attLabel = new JLabel("Choose an Attribute:");
        Font labelFont = attLabel.getFont().deriveFont(Font.BOLD);
        attLabel.setFont(labelFont);
        attributeList = new JComboBox();
        attListPane.add(attLabel);
        attListPane.add(attributeList);
        contentPane.add(attListPane);
        // To display an action
        JPanel actionPane = new JPanel();
        actionPane.setBorder(compoundBorder);
        actionPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstants.HORIZONTAL;
        constraints.weightx = 0.1;
        constraints.anchor = GridBagConstants.WEST;
        constraints.insets = new Insets(2, 2, 2, 2);
        JLabel actionLabel = new JLabel("Choose an Action:");
        actionLabel.setFont(labelFont);
        actionPane.add(actionLabel, constraints);
        addBtn = new JRadioButton("Add");
        setBtn = new JRadioButton("Set");
        removeBtn = new JRadioButton("Remove");
        // Default disabled
        addBtn.setEnabled(false);
        setBtn.setEnabled(false);
        removeBtn.setEnabled(false);
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(addBtn);
        btnGroup.add(setBtn);
        btnGroup.add(removeBtn);
        constraints.weightx = 0.9;
        constraints.gridx = 1;
        actionPane.add(addBtn, constraints);
        constraints.gridy = 1;
        actionPane.add(setBtn, constraints);
        constraints.gridy = 2;
        actionPane.add(removeBtn, constraints);
        contentPane.add(actionPane);
        // Apply pane
        JPanel applyPane = new JPanel();
        applyPane.setBorder(compoundBorder);
        applyPane.setLayout(new GridBagLayout());
        constraints.gridx = 0;
        constraints.gridy = 0;
        applyLabel = new JLabel("Apply:");
        applyLabel.setFont(labelFont);
        applyText = new JTextArea();
        applyText.setBackground(contentPane.getBackground());
        applyText.setLineWrap(true);
        applyText.setEditable(false);
        applyText.setWrapStyleWord(true);
        applyText.setFont(labelFont);
        applyText.setMinimumSize(new Dimension(500, 20));
        constraints.weightx = 0.1;
        applyPane.add(applyLabel, constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.9;
        constraints.gridheight = 4;
        constraints.fill = GridBagConstants.BOTH;
        applyPane.add(applyText, constraints);
        contentPane.add(applyPane);
        applyLabel.setEnabled(false);
        
        getContentPane().add(contentPane, BorderLayout.CENTER);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        controlPane.getOKBtn().setEnabled(false);
        installListeners();
    }
    
    private void installListeners() {
        // Based on the selected attribute to enable actions
        attributeList.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                String attName = (String) attributeList.getSelectedItem();
                if (attName == null || commonCls == null)
                    return; // Do nothing. It might in the initialization state.
                try {
                    SchemaAttribute attribute = commonCls.getAttribute(attName);
                    if (attribute.isMultiple()) {
                        addBtn.setEnabled(true);
                        removeBtn.setEnabled(true);
                        setBtn.setEnabled(true);
                    }
                    else {
                        addBtn.setEnabled(false);
                        setBtn.setEnabled(true);
                        removeBtn.setEnabled(true);
                    }
                }
                catch(InvalidAttributeException exp) {
                    System.err.println("MultupeInstanceEditDialog.installListeners(): " + exp);
                    exp.printStackTrace();
                }
            }
        });
        // Add a value
        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionBtn = addBtn;
                doAddOrSetAction();
            }
        });
        // Set a value
        setBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionBtn = setBtn;
                doAddOrSetAction();
            }
        });
        // Remove a value
        removeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionBtn = removeBtn;
                doRemoveAction();
            }
        });
    }
    
    /**
     * A helper method to remove a value.
     *
     */
    private void doRemoveAction() {
        // Get the selected attribute
        SchemaAttribute selectedAtt = getSelectedAttribute();
        if (selectedAtt == null)
            return; // Just in case
        // Get the set of all values
        Set values = new HashSet();
        GKInstance instance = null;
        String attName = selectedAtt.getName();
        try {
            for (Iterator it = instances.iterator(); it.hasNext();) {
                instance = (GKInstance) it.next();
                List list = instance.getAttributeValuesList(attName);
                if (list != null && list.size() > 0)
                    values.addAll(list);
            }
        }
        catch(Exception e) {
            System.err.println("MultipleInstanceEditDialog.doRemoveAction(): " + e);
            e.printStackTrace();
        }
        if (values.size() == 0) {
            JOptionPane.showMessageDialog(this,
                                          "No values in the selected attribute.",
                                          "Empty Value",
                                          JOptionPane.INFORMATION_MESSAGE);
            return ;
        }
        // Use a JList to display
        ValueListDialog dialog = new ValueListDialog(this, "Remove Attribute Value");
        dialog.setAttributeValues(values, selectedAtt);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setModal(true);
        dialog.setVisible(true);
        if (dialog.isOKClicked) {
            if (actionValues != null)
                actionValues.clear();
            else
                actionValues = new ArrayList();
            actionValues.addAll(dialog.getSelectedValues());
        }
        else
            actionValues = null; // Empty
        setApply();
    }
    
    private SchemaAttribute getSelectedAttribute() {
        SchemaAttribute selectedAtt = null;
        try {
            selectedAtt = commonCls.getAttribute((String)attributeList.getSelectedItem());
        }
        catch(InvalidAttributeException e) {
            // No exception should be thrown
            e.printStackTrace();
        }
        return selectedAtt;
    }
    
    /**
     * A helper to choose one or more instances
     * @return
     */
    private void doAddOrSetAction() {
        String dialogTitle = null;
        if (actionBtn == addBtn)
            dialogTitle = "Add Attribute Value";
        else
            dialogTitle = "Set Attribute Value";
        // Get the allowed Schema
        SchemaAttribute selectedAtt = getSelectedAttribute();
        if (selectedAtt == null) // Just in case
            return; // Nothing to do
        if (selectedAtt.isInstanceTypeAttribute()) {
            InstanceSelectDialog selectDialog = new InstanceSelectDialog(this, dialogTitle);
            selectDialog.setTopLevelSchemaClasses(selectedAtt.getAllowedClasses());
            selectDialog.setModal(true);
            selectDialog.setSize(950, 650);
            GKApplicationUtilities.center(selectDialog);
            selectDialog.setVisible(true);
            if (selectDialog.isOKClicked()) 
                actionValues = selectDialog.getSelectedInstances();
            else 
                actionValues = null;   
        }
        else { // Use a text window
            TextDialog textDialog = new TextDialog(this, dialogTitle, true);
            textDialog.setModal(true);
            textDialog.setSize(400, 300);
            textDialog.setLocationRelativeTo(this);
            textDialog.setVisible(true);
            if (textDialog.isOKClicked()) {
                String text = textDialog.getText();
                Object attValue = generateAttributeValueFromText(text, selectedAtt);
                if (attValue != null) {
                    if (actionValues != null)
                        actionValues.clear();
                    else
                        actionValues = new ArrayList();
                    actionValues.add(attValue);
                }
            }
            else
                actionValues = null;
        }
        setApply();
    }
    
    private void setApply() {
        if (actionValues != null && actionValues.size() > 0) {
            applyLabel.setEnabled(true);
            applyText.setText(generateApplyText());
            controlPane.getOKBtn().setEnabled(true);
        }
        else {
            applyLabel.setEnabled(false);
            applyText.setText("");
            controlPane.getOKBtn().setEnabled(false);
        }        
    }
    
    private Object generateAttributeValueFromText(String text, SchemaAttribute att) {
        if (att.getTypeAsInt() == SchemaAttribute.BOOLEAN_TYPE) {
            Boolean attValue = null;
            if (text.equals("true"))
                attValue = Boolean.TRUE;
            else if (text.equals("false"))
                attValue = Boolean.FALSE;
            else {
                JOptionPane.showMessageDialog(this, 
                                        "Please use \"true\" or \"false\" for a boolean attribute.",
                                        "Attribute Value Error",
                                        JOptionPane.ERROR_MESSAGE);
            }
            return attValue;
        }
        else if (att.getTypeAsInt() == SchemaAttribute.INTEGER_TYPE) {
            try {
                Integer attValue = new Integer(text);
                return attValue;
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                        "Please input an integer for attribute \"" + att.getName() + "\".",
                        "Attribute Value Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (att.getTypeAsInt() == SchemaAttribute.LONG_TYPE) {
            try {
                Long attValue = new Long(text);
                return attValue;
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                        "Please input an integer for attribute \"" + att.getName() + "\".",
                        "Attribute Value Error",
                        JOptionPane.ERROR_MESSAGE);
            }            
        }
        else if (att.getTypeAsInt() == SchemaAttribute.FLOAT_TYPE) {
            try {
                Float attValue = new Float(text);
                return attValue;
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this, 
                        "Please input an float number for attribute \"" + att.getName() + "\".",
                        "Attribute Value Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        else if (att.getTypeAsInt() == SchemaAttribute.STRING_TYPE)
            return text;
        return null;
    }
    
    private String generateApplyText() {
        StringBuffer buffer = new StringBuffer();
        if (actionBtn == addBtn) {
            buffer.append("Add ");
        }
        else if (actionBtn == setBtn) {
            buffer.append("Set ");
        }
        else if (actionBtn == removeBtn) {
            buffer.append("Remove ");
        }
        if (actionValues != null && actionValues.size() > 0) {
            buffer.append("\"");
            for (Iterator it = actionValues.iterator(); it.hasNext();) {
                buffer.append(it.next().toString());
                if (it.hasNext())
                    buffer.append(", ");
            }
            buffer.append("\"");
        }
        String attName = (String) attributeList.getSelectedItem();
        if (actionBtn == removeBtn)
            buffer.append(" from ");
        else
            buffer.append(" to ");
        buffer.append(attName).append(".");
        return buffer.toString();
    }
    
    private void commit() {
        if (actionValues == null || actionValues.size() == 0)
            return;
        String attName = (String) attributeList.getSelectedItem();
        try {
            GKInstance instance = null;
            boolean isChanged = false;
            AttributeEditManager editManager = AttributeEditManager.getManager();
            //Add avalue
            if (actionBtn == addBtn) {
                for (Iterator it = instances.iterator(); it.hasNext();) {
                    instance = (GKInstance) it.next();
                    List attValues = instance.getAttributeValuesList(attName);
                    isChanged = false;
                    for (Iterator it1 = actionValues.iterator(); it1.hasNext();) {
                        Object value = it1.next();
                        if (attValues == null || !attValues.contains(value)) {
                            instance.addAttributeValue(attName, value);
                            isChanged = true;
                        }
                    }
                    if (isChanged) {
                        editManager.validateDisplayName(instance);
                        editManager.attributeEdit(instance, attName);
                    }
                }
            }
            // Set value
            else if (actionBtn == setBtn) {
                SchemaAttribute att = null;
                for (Iterator it = instances.iterator(); it.hasNext();) {
                    instance = (GKInstance) it.next();
                    att = instance.getSchemClass().getAttribute(attName);
                    if (att.isMultiple())
                        instance.setAttributeValue(att, actionValues);
                    else {
                        Object attValue = actionValues.get(0);
                        instance.setAttributeValue(att, attValue);
                    }
                    // _displayName might change
                    editManager.validateDisplayName(instance);
                    editManager.attributeEdit(instance, attName);
                }
            }
            // Remove value
            else if (actionBtn == removeBtn) {
                for (Iterator it = instances.iterator(); it.hasNext();) {
                    instance = (GKInstance) it.next();
                    List valueList = instance.getAttributeValuesList(attName);
                    if (valueList == null || valueList.size() == 0)
                        continue; // Nothing to do
                    isChanged = valueList.removeAll(actionValues);
                    if (isChanged) {
                        editManager.validateDisplayName(instance);
                        editManager.attributeEdit(instance, attName);
                    }
                }
            }
        }
        catch(Exception e) {
            System.err.println("MultipleInstanceEditDialog.commit(): " + e);
            e.printStackTrace();
        }
    }
    
    public void setInstancecs(List instances) {
        this.instances = instances;
        listPane.setDisplayedInstances(instances);
        // Want to control the title
        listPane.setTitle("Editing Instances");
        commonCls = getCommonAncestor(instances);
        List unedtiableNames = AttributeEditConfig.getConfig().getUneditableAttNames();
        List attNames = new ArrayList();
        for (Iterator it = commonCls.getAttributes().iterator(); it.hasNext();) {
            SchemaAttribute att = (SchemaAttribute) it.next();
            if (unedtiableNames.contains(att.getName()))
                continue;
            attNames.add(att.getName());
        }
        Collections.sort(attNames);
        for (Iterator it = attNames.iterator(); it.hasNext();) {
            attributeList.addItem(it.next());
        }
    }
    
    /**
     * A helper method to get the common ancestor SchemaClass for a list of GKInstance objects.
     * @param instances
     * @return
     */
    private SchemaClass getCommonAncestor(List instances) {
        Set schemaClasses = new HashSet();
        GKInstance tmp = null;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            tmp = (GKInstance) it.next();
            schemaClasses.add(tmp.getSchemClass());
        }
        // Common SchemaClass
        SchemaClass schemaCls = null;
        if (schemaClasses.size() == 1) {
            schemaCls = (SchemaClass) schemaClasses.iterator().next();
        }
        else { // More than one
            int size = schemaClasses.size();
            List clsList = new ArrayList(schemaClasses);
            SchemaClass cls1 = (SchemaClass)clsList.get(0);
            List list1 = cls1.getOrderedAncestors();
            List endList = new ArrayList(list1);
            for (int i = 1; i < size; i++) {
                cls1 = (SchemaClass) clsList.get(i);
                list1 = cls1.getOrderedAncestors();
                endList.retainAll(list1);
            }
            schemaCls = (SchemaClass) endList.get(endList.size() - 1);
        }
        return schemaCls;
    }
    
    /**
     * An inner, customized JDialog to display a list of attribute value to be removed.
     * @author guanming
     *
     */
    private class ValueListDialog extends JDialog {
        private boolean isOKClicked;
        private JList list;
        
        public ValueListDialog(JDialog parentDialog, String title) {
            super(parentDialog, title);
            init();
        }
        
        private void init() {
            JDialog listDialog = new JDialog(this, "Remove an Attribute Value");
            JPanel contentPane = new JPanel(); // Use a JPanel so that a customized Border can be used
            contentPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            contentPane.setLayout(new BorderLayout());
            JLabel label = new JLabel("Please Choose a Value to be removed:");
            label.setBorder(GKApplicationUtilities.getTitleBorder());    
            contentPane.add(label, BorderLayout.NORTH);
            list = new JList();
            DefaultListModel model = new DefaultListModel();
            list.setModel(model);
            contentPane.add(new JScrollPane(list), BorderLayout.CENTER);
            DialogControlPane controlPane = new DialogControlPane();
            contentPane.add(controlPane, BorderLayout.SOUTH);
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = true;
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = false;
                    dispose();
                }
            });
            getContentPane().add(contentPane, BorderLayout.CENTER);
        }
        
        public List getSelectedValues() {
            Object[] selected = list.getSelectedValues();
            if (selected == null || selected.length == 0)
                return null;
            return Arrays.asList(selected);
        }
        
        public void setAttributeValues(Set values, SchemaAttribute selectedAtt) {
            List valueList = new ArrayList(values);
            if (selectedAtt.isInstanceTypeAttribute())
                InstanceUtilities.sortInstances(valueList);
            else
                Collections.sort(valueList);
            DefaultListModel listModel = (DefaultListModel) list.getModel();
            for (Iterator it = valueList.iterator(); it.hasNext();)
                listModel.addElement(it.next());
        }
    }
}
