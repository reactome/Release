/*
 * Created on Oct 16, 2003
 */
package org.gk.database;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * This customized JPanel is used to set up GUIs for searching.
 * @author wgm
 */
public class SearchPane extends JPanel {
	// GUIs
	protected JComboBox classBox;
	private JComboBox attributeBox;
	private JComboBox valueBox;
	private JTextField valueField;
	protected JButton searchBtn;
	protected JButton searchMoreBtn;
	protected JLabel classLabel;

	public SearchPane() {
		init();
	}
	
	protected void init() {
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		classLabel = new JLabel("Choose Class:");
		add(classLabel, constraints);
		classBox = new JComboBox();
		classBox.setRenderer(new SchemaClassListCellRenderer());
		constraints.gridx = 1;
		constraints.gridwidth = 2;
		constraints.weightx = 0.5;
		add(classBox, constraints);
		JLabel attributeLabel = new JLabel("Choose Attribute:");
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		constraints.weightx = 0.0;
		add(attributeLabel, constraints);
		attributeBox = new JComboBox();
		attributeBox.setRenderer(new AttributeListCellRenderer());
		constraints.gridx = 1;
		constraints.gridwidth = 2;
		constraints.weightx = 0.5;
		add(attributeBox, constraints);
		JLabel valueLabel = new JLabel("Attribute Value:");
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridwidth = 1;
		constraints.weightx = 0.0;
		add(valueLabel, constraints);
		valueBox = createOperatorBox();
		constraints.gridx = 1;
		add(valueBox, constraints);
		constraints.gridx = 2;
		constraints.weightx = 0.8;
		constraints.fill = GridBagConstraints.BOTH;
		valueField = new JTextField();
		add(valueField, constraints);
		JPanel btnPane = new JPanel();
		searchBtn = new JButton("Search");
		searchBtn.setMnemonic('S');
		constraints.weightx = 0.0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.gridwidth = 3;
		constraints.anchor = GridBagConstraints.CENTER;
		btnPane.add(searchBtn);
		searchMoreBtn = new JButton("Search More...");
		btnPane.add(searchMoreBtn);
		add(btnPane, constraints);
		installListeners();
	}
	
	protected JComboBox createOperatorBox() {
        JComboBox operatorBox = new JComboBox();
        operatorBox.addItem("Equals");
        operatorBox.addItem("Contains");
        operatorBox.addItem("!=");
        operatorBox.addItem("Use REGEXP");
        operatorBox.addItem("IS NOT NULL");
        operatorBox.addItem("IS NULL");
        return operatorBox;
	}
	
	private void installListeners() {
		classBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				GKSchemaClass schemaClass = (GKSchemaClass)classBox.getSelectedItem();
				setAttribute(schemaClass);
			}
		});
		valueBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				int index = valueBox.getSelectedIndex();
				if (index < 4) {
					valueField.setVisible(true);
					valueField.invalidate();
					validate();
				}
				else
					valueField.setVisible(false);
			}
		});
	}
	
	public void setAttribute(GKSchemaClass schemaClass) {
		if (schemaClass == null)
			return;
		List<GKSchemaAttribute> list = new ArrayList<GKSchemaAttribute>();
		for (Iterator it = schemaClass.getAttributes().iterator(); it.hasNext();) {
			GKSchemaAttribute attribute = (GKSchemaAttribute) it.next();
			list.add(attribute);
		}
		Collections.sort(list, new Comparator<GKSchemaAttribute>() {
			public int compare(GKSchemaAttribute attribute1,
			                   GKSchemaAttribute attribute2) {
				return attribute1.getName().compareTo(attribute2.getName());
			}
		});
		setAttributes(list);
	}

	protected void setAttributes(List<GKSchemaAttribute> list) {
	    setAttributes(list, attributeBox);
	}
	
    protected void setAttributes(List<GKSchemaAttribute> list,
                                 JComboBox attributeBox) {
        GKSchemaAttribute selectedAtt = (GKSchemaAttribute) attributeBox.getSelectedItem();
		attributeBox.removeAllItems();
		int selectedIndex = -1;
		int c = 0;
		for (Iterator it = list.iterator(); it.hasNext();) {
			GKSchemaAttribute att1 = (GKSchemaAttribute) it.next();
			if (selectedAtt != null &&
			    att1 != null &&
			    att1.getName().equals(selectedAtt.getName()))
				selectedIndex = c;
			attributeBox.addItem(att1);
			c ++;
		}
		if (selectedIndex != -1)
			attributeBox.setSelectedIndex(selectedIndex);
    }
	
	public void hideClassFields() {
		classLabel.setVisible(false);
		classBox.setVisible(false);
	}
	
	public void setSelectedClass(GKSchemaClass schemaClass) {
		classBox.setSelectedItem(schemaClass);
	}
	
	public void setSchema(GKSchema schema) {
		// Get the old list of all SchemaClass
		java.util.List list = new ArrayList();
		list.add(schema.getRootClass());
		setTopLevelSchemaClasses(list);
	}
	
	public void setTopLevelSchemaClasses(Collection classes) {
		java.util.List list = InstanceUtilities.getAllSchemaClasses(classes);
		setSelectableClasses(list);
	}
	
	public void setSelectableClasses(Collection classes) {
		classBox.removeAllItems();
		// Always put the root class at the top of the list if it is there.
		if (classes.size() > 0) {
		    GKSchemaClass cls = (GKSchemaClass) classes.iterator().next();
		    List list = cls.getOrderedAncestors();
		    // The first class should be the root
		    GKSchemaClass root = (GKSchemaClass) list.get(0);
		    if (classes.contains(root))
		        classBox.addItem(root);
		}
		// The root class is added twice on purpose
		for (Iterator it = classes.iterator(); it.hasNext();) {
			classBox.addItem(it.next());
		}
	}
	
	public GKSchemaClass getSchemaClass() {
		return (GKSchemaClass) classBox.getSelectedItem();
	}
	
	public GKSchemaAttribute getAttribute() {
		return (GKSchemaAttribute) attributeBox.getSelectedItem();
	}
	
	public String getOperator() {
		return getOperator(valueBox);
	}
	
	protected String getOperator(JComboBox operatorBox) {
	    String operator = (String) operatorBox.getSelectedItem();
	    if (operator.equals("Equals"))
	        operator = "=";
	    else if (operator.equals("Contains"))
	        operator = "LIKE";
	    else if (operator.equals("Use REGEXP"))
	        operator = "REGEXP";
	    return operator;
	}
	
	/**
	 * Get the text that input in the attribute value field.
	 * @return
	 */
	public String getText() {
		return getValue(valueField,
		                valueBox);
	}
	
	protected String getValue(JTextField valueField,
	                          JComboBox operatorBox) {
	    String text = valueField.getText().trim();
	    String operator = getOperator(operatorBox);
	    if (operator.equals("LIKE"))
	        text = "%" + text + "%";
	    return text;
	}
	
	public void addSearchActionListener(ActionListener l) {
		searchBtn.addActionListener(l);
		if (valueField != null)
		    valueField.addActionListener(l);
	}
	
	public void addSearchMoreAction(ActionListener l) {
	    searchMoreBtn.addActionListener(l);
	}
	
	public void setSearchButtonVisible(boolean isVisible) {
		searchBtn.setVisible(isVisible);
	}
	
	/**
	 * The actual method for doing search
	 * @param persistenceAdaptor
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public Collection search(PersistenceAdaptor persistenceAdaptor) throws Exception {
	    String operator = getOperator();
	    String value = getText();
	    SchemaAttribute att = getAttribute();
	    SchemaClass cls = getSchemaClass();
	    return doSearch(persistenceAdaptor, att, operator, value, cls);
	}

	@SuppressWarnings("unchecked")
    protected Collection doSearch(PersistenceAdaptor persistenceAdaptor,
                                  SchemaAttribute att,
                                  String operator,
                                  String value, 
                                  SchemaClass cls) throws Exception {
        Collection<GKInstance> c = null;
	    // IS NULL or IS NOT NULL should have nothing to do with value. So it should not to 
	    // pull out the instances
	    if (!(operator.equals("IS NULL")) &&
	        !(operator.equals("IS NOT NULL")) &&
	        (att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE)) {
	        c = new HashSet<GKInstance>();
	        // Search another instance first
	        Collection schemaClasses = att.getAllowedClasses();
	        for (Iterator i = schemaClasses.iterator(); i.hasNext();) {
	            GKSchemaClass class2 = (GKSchemaClass)i.next();
	            Set<GKInstance> attInstances = new HashSet<GKInstance>();
	            // Try to use _displayName only. Use name attribute for "!="
	            // actually generates a bug. For example, to search for ReactionlikeEvent with species != Oryza sativa,
	            // using name will still keep Oryza sativa in the attribute list because there is another name "rice"
	            // in its name attribute list.
//	            if (class2.isValidAttribute(ReactomeJavaConstants.name)) {
//	                Collection<GKInstance> attInstances1 = persistenceAdaptor.fetchInstanceByAttribute(class2.getName(), 
//	                                                                                                   ReactomeJavaConstants.name,
//	                                                                                                   operator, 
//	                                                                                                   value);
//	                if (attInstances1 != null)
//	                    attInstances.addAll(attInstances1);
//	            }
	            // Check _displayName in case of local shell instance
	            // In case for some instances having no name as attributes
	            //if (persistenceAdaptor instanceof XMLFileAdaptor) {
	                Collection<GKInstance> attInstances1 = persistenceAdaptor.fetchInstanceByAttribute(class2.getName(),
	                                                                                                   ReactomeJavaConstants._displayName,
	                                                                                                   operator,
	                                                                                                   value);
	                if (attInstances1 != null)
	                    attInstances.addAll(attInstances1);
	            //}
	            if (attInstances != null && attInstances.size() > 0) {
	                if (persistenceAdaptor instanceof MySQLAdaptor) {
	                    c = persistenceAdaptor.fetchInstanceByAttribute(cls.getName(),
	                                                                    att.getName(),
	                                                                    "=",
	                                                                    attInstances);
//	                    System.out.println("Found instance for " + att.getName() + ": " + c.size());
	                }
	                else {
	                    for (Iterator it = attInstances.iterator(); it.hasNext();) {
	                        GKInstance attInstance = (GKInstance) it.next();
	                        Collection instances1 = persistenceAdaptor.fetchInstanceByAttribute(cls.getName(), 
	                                                                                            att.getName(), 
	                                                                                            "=",
	                                                                                            attInstance);
	                        if (instances1 != null)
	                            c.addAll(instances1);
	                    }
	                }
	            }
	        }
	    }
	    else
	        c = persistenceAdaptor.fetchInstanceByAttribute(cls.getName(), 
	                                                        att.getName(),
	                                                        operator,
	                                                        value);
	    return c;
    }
	
	static class SchemaClassListCellRenderer extends DefaultListCellRenderer {

		SchemaClassListCellRenderer() {
			super();
		}

		public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus) {
			Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			GKSchemaClass gkSchema = (GKSchemaClass)value;
			if (gkSchema != null)
				setText(gkSchema.getName());
			return comp;
		}
	}
	
	class AttributeListCellRenderer extends DefaultListCellRenderer {
		
		AttributeListCellRenderer() {
			super();
		}
		
		public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus) {
				Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				GKSchemaAttribute attribute = (GKSchemaAttribute) value;
				if (attribute == null)
				    setText(" "); // Give it a space so that it can be displayed correct
				else
					setText(attribute.getName());
				return comp;		
		}
	}
}
