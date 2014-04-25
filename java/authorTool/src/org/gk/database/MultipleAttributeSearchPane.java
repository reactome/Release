/*
 * Created on Sep 6, 2010
 *
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.gk.util.DialogControlPane;

/**
 * This class implements a search panel for multiple attributes. The functions implemented are
 * based on the web page, "Advanced Search".
 * @author wgm
 *
 */
public class MultipleAttributeSearchPane extends SearchPane {
    // Track the positions of dialog
    private Rectangle dialogBounds;
    // list of attributes
    private List<JComboBox> attributeBoxes;
    private List<JComboBox> operatorBoxes;
    private List<JTextField> valueFields;
    
    public MultipleAttributeSearchPane() {
    }
    
    protected void init() {
        setLayout(new BorderLayout());
        // Use two panel
        JPanel propertyPane = new JPanel();
        propertyPane.setLayout(new GridBagLayout());
        Border border = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                                                           BorderFactory.createEmptyBorder(8, 8, 8, 8));
        propertyPane.setBorder(border);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        classLabel = new JLabel("Choose a class:");
        propertyPane.add(classLabel, constraints);
        classBox = new JComboBox();
        classBox.setRenderer(new SchemaClassListCellRenderer());
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 0.5;
        propertyPane.add(classBox, constraints);
        JLabel attributeLabel = new JLabel("Choose one or more attributes: all criteria will be met for multiple attribute search.");
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 3;
        constraints.weightx = 0.0;
        propertyPane.add(attributeLabel, constraints);
        attributeBoxes = new ArrayList<JComboBox>();
        operatorBoxes = new ArrayList<JComboBox>();
        valueFields = new ArrayList<JTextField>();
        // Add 4 possible attribute selections
        for (int i = 0; i < 4; i++) {
            JComboBox attributeBox = new JComboBox();
            attributeBoxes.add(attributeBox);
            attributeBox.setRenderer(new AttributeListCellRenderer());
            constraints.gridx = 0;
            constraints.gridy = 2 + i;
            constraints.gridwidth = 1;
            constraints.weightx = 0.0;
            propertyPane.add(attributeBox, constraints);
            JComboBox operatorBox = createOperatorBox();
            operatorBoxes.add(operatorBox);
            constraints.gridx = 1;
            constraints.weightx = 0.0;
            propertyPane.add(operatorBox, constraints);
            constraints.gridx = 2;
            constraints.weightx = 0.8;
            constraints.fill = GridBagConstraints.BOTH;
            JTextField valueField = new JTextField();
            valueFields.add(valueField);
            propertyPane.add(valueField, constraints);
        }
        add(propertyPane, BorderLayout.CENTER);
        // add search button
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.setBorder(BorderFactory.createEtchedBorder());
        add(controlPane, BorderLayout.SOUTH);
        JButton okBtn = controlPane.getOKBtn();
        okBtn.setText("Search");
        JButton cancelBtn = controlPane.getCancelBtn();
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hideSearch();
            }
        });
        cancelBtn.addActionListener(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                hideSearch();
            }
        });
        installListeners();
        searchBtn = okBtn;
    }
    
    private void installListeners() {
        classBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                GKSchemaClass schemaClass = (GKSchemaClass)classBox.getSelectedItem();
                setAttribute(schemaClass);
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public List<GKInstance> search(PersistenceAdaptor adaptor) throws Exception {
        List<GKSchemaAttribute> attributes = getAttributes();
        List<String> operators = getOperators();
        List<String> values = getValues();
        SchemaClass cls = getSchemaClass();
        List<GKInstance> rtn = null;
        for (int i = 0; i < attributes.size(); i++) {
            GKSchemaAttribute attribute = attributes.get(i);
            if (attribute == null)
                continue;
            String operator = operators.get(i);
            String value = values.get(i);
            Collection instances = doSearch(adaptor,
                                            attribute,
                                            operator, 
                                            value,
                                            cls);
            if (rtn == null)
                rtn = new ArrayList<GKInstance>(instances);
            else
                rtn.retainAll(instances);
        }
        return rtn;
    }
    
    /**
     * Check if a search can be done based on current selection.
     * @return
     */
    public boolean isSearchable() {
        List<GKSchemaAttribute> attributes = getAttributes();
        // Check if any attribute has been selected
        boolean noSearch = true;
        for (GKSchemaAttribute attribute : attributes) {
            if (attribute != null) {
                noSearch = false;
                break;
            }
        };
        return !noSearch;
    }
    
    @Override
    protected void setAttributes(List<GKSchemaAttribute> list) {
        // add a null as an empty value
        list.add(0, null);
        for (JComboBox attributeBox : attributeBoxes)
            setAttributes(list, attributeBox);
    }

    private void hideSearch() {
        JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, this);
        dialog.dispose();
    }
    
    public List<GKSchemaAttribute> getAttributes() {
        List<GKSchemaAttribute> rtn = new ArrayList<GKSchemaAttribute>();
        for (JComboBox box : attributeBoxes) {
            if (box.getSelectedItem() instanceof String) // For an empty String
                rtn.add(null);
            else
                rtn.add((GKSchemaAttribute)box.getSelectedItem());
        }
        return rtn;
    }
    
    public List<String> getOperators() {
        List<String> rtn = new ArrayList<String>();
        for (JComboBox box : operatorBoxes) {
            String operator = getOperator(box);
            rtn.add(operator);
        }
        return rtn;
    }
    
    public List<String> getValues() {
        List<String> rtn = new ArrayList<String>();
        int index = 0;
        for (JTextField field : valueFields) {
            String value = getValue(field,
                                    operatorBoxes.get(index));
            rtn.add(value);
            index ++;
        }
        return rtn;
    }
    
    public void showSearch(JFrame parent) {
        final JDialog dialog = new JDialog(parent, 
                                    "Search Instances");
        // To keep the coordinate and size
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                dialogBounds = new Rectangle(dialog.getBounds());
            }
        });
        if (dialogBounds == null) {
            dialog.setSize(650, 450);
            dialog.setLocationRelativeTo(parent);
        }
        else {
            dialog.setBounds(new Rectangle(dialogBounds));
        }
        dialog.setModal(true);
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }
}
