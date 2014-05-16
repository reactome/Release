/*
 * Created on Apr 22, 2004
 */
package org.gk.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * A customized JTable for describing the defintion for a GKSchemaClass.
 * @author wugm
 */
public class SchemaClassDefinitionTable extends JTable {
	private SchemaClass schemaClass;

	public SchemaClassDefinitionTable() {
		SchemaClassTableModel model = new SchemaClassTableModel();
		setModel(model);
	}
	
	public SchemaClassDefinitionTable(SchemaClass cls) {
		this();
		setSchemaClass(cls);
	}

	public void setSchemaClass(SchemaClass cls) {
		if (cls == null)
			throw new IllegalArgumentException("SchemaClassDefinitionTableModel.setSchemaClass(): " + 
                                               "the argument should not be null.");
		this.schemaClass = cls;
		SchemaClassTableModel model = (SchemaClassTableModel) getModel();
		model.setSchemaClass(cls);
	}
	
	public SchemaAttribute getSelectedAttribute() {
		int selectedRow = getSelectedRow();
		if (selectedRow > -1) {
			SchemaClassTableModel model = (SchemaClassTableModel) getModel();
			return model.getAttribute(selectedRow); 
		}
		else
			return null;
	}

	class SchemaClassTableModel extends AbstractTableModel {
		// For headers
		private String[] headers = {
                "Name", "Type", "Category", 
                "Allowed Classes", "Attribute Origin",
			    "Cardinality", "Inverse Attribute",
				"Defining Type"};
		private java.util.List sortedAttributes = new ArrayList();
		// For sorting
		private Comparator nameSorter = new Comparator() {
			public int compare(Object obj1, Object obj2) {
				SchemaAttribute att1 = (SchemaAttribute) obj1;
				SchemaAttribute att2 = (SchemaAttribute) obj2;
				return att1.getName().compareTo(att2.getName());
			}
		};
		
		SchemaClassTableModel() {
		}
		
		public String getColumnName(int col) {
			return headers[col];
		}
		
		public void setSchemaClass(SchemaClass cls) {
			// Reset the values
			sortedAttributes.clear();
			for (Iterator it = cls.getAttributes().iterator(); it.hasNext();) {
				sortedAttributes.add(it.next());
			}
			Collections.sort(sortedAttributes, nameSorter);
			fireTableChanged(new TableModelEvent(this));
		}
		
		public int getColumnCount() {
			return headers.length;
		}
		
		public int getRowCount() {
			return sortedAttributes.size();
		}
		
		public SchemaAttribute getAttribute(int row) {
			if (row < 0 || row > sortedAttributes.size() - 1)
				return null;
			return (SchemaAttribute) sortedAttributes.get(row);
		}
		
		public Object getValueAt(int row, int col) {
			if (row < 0 || row > sortedAttributes.size() - 1)
				return null;
			SchemaAttribute att = (SchemaAttribute) sortedAttributes.get(row);
			switch(col) {
				case 0 : // Attribute Name
					return att.getName();
				case 1 : // Attribute Type
					if (att.isInstanceTypeAttribute())
						return "Instance";
					String className = att.getType().getName();
					String packageName = att.getType().getPackage().getName();
					return className.substring(packageName.length() + 1); // 1 for "." 
                 case 2 : // Attribute Category
                     int category = att.getCategory();
                     if (category == 1)
                         return "Mandatory";
                     if (category == 2)
                         return "Required";
                     if (category == 3)
                         return "Optional";
                     if (category == 4)
                         return "No_Manual_Edit";
                     return null;
				case 3 : // Allowed Classes
					if (att.isInstanceTypeAttribute()) {
						StringBuffer buffer = new StringBuffer();
						for (Iterator it = att.getAllowedClasses().iterator(); it.hasNext();) {
							SchemaClass cls = (SchemaClass) it.next();
							buffer.append(cls.getName());
							if (it.hasNext())
								buffer.append("; ");
						}
						return buffer.toString();
					}
					return null;
                case 4 : // Attribute Origin
                    SchemaClass cls = att.getOrigin();
                    if (cls != null)
                        return cls.getName();
                    return null;
				case 5 : // Cardinility
					if (att.isMultiple())
						return "Multiple";
					else
						return "Single";
				case 6 : // Inverse Attribute
					if (att.getInverseSchemaAttribute() != null) {
						SchemaAttribute inverseAtt = att.getInverseSchemaAttribute();
						StringBuffer buffer = new StringBuffer();
						for (Iterator it = inverseAtt.getAllowedClasses().iterator(); it.hasNext();) {
							cls = (SchemaClass) it.next();
							buffer.append(cls.getName() + "." + inverseAtt.getName());
							if (it.hasNext())
								buffer.append("; ");
						}
						return buffer.toString();
					}
					return null;
				case 7 : // Defining type
					int type = att.getDefiningType();
					if (type == SchemaAttribute.NONE_DEFINING)
						return "NONE";
					if (type == SchemaAttribute.ALL_DEFINING)
						return "ALL";
					if (type == SchemaAttribute.ANY_DEFINING)
						return "ANY";
					return null;
			}
			return null;
		}
	}

}
