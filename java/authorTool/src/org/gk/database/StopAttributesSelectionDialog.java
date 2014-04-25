/*
 * Created on Nov 14, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.gk.schema.GKSchemaAttribute;

/**
 * A customized JDialog for selecting stop attributes.
 * @author wugm
 */
public class StopAttributesSelectionDialog extends JDialog {
	private JTable selectionTable;

	boolean isOKClicked = false;

	public StopAttributesSelectionDialog(JFrame parentFrame) {
		super(parentFrame);
		init();
	}

	private void init() {
		JLabel label = new JLabel("Choose attibutes as the stop conditions:");
		label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
		label.setHorizontalAlignment(JLabel.LEFT);
		getContentPane().add(label, BorderLayout.NORTH);
		
		JPanel tablePane = new JPanel();
		tablePane.setBorder(BorderFactory.createRaisedBevelBorder());
		tablePane.setLayout(new BorderLayout());
		selectionTable = new JTable();
		selectionTable.setCellSelectionEnabled(true);
		tablePane.add(new JScrollPane(selectionTable), BorderLayout.CENTER);
		JPanel btnPane = new JPanel();
		btnPane.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 8));
		JButton selectAll = new JButton("Select All");
		selectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AttributeSelectionTableModel model = (AttributeSelectionTableModel) selectionTable.getModel();
				model.selectAll();
				model.fireTableDataChanged();
			}
		});
		JButton selectNone = new JButton("Select None");
		selectNone.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AttributeSelectionTableModel model = (AttributeSelectionTableModel) selectionTable.getModel();
				model.selectNone();
				model.fireTableDataChanged();
			}
		});
		btnPane.add(selectAll);
		btnPane.add(selectNone);
		tablePane.add(btnPane, BorderLayout.SOUTH);
		getContentPane().add(tablePane, BorderLayout.CENTER);

		JPanel southPane = new JPanel();
		southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton okBtn = new JButton("OK");
		okBtn.setMnemonic('O');
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isOKClicked = true;
				dispose();
			}
		});
		southPane.add(okBtn);
		okBtn.setDefaultCapable(true);
		getRootPane().setDefaultButton(okBtn);
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.setMnemonic('C');
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isOKClicked = false;
				dispose();
			}
		});
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
		southPane.add(cancelBtn);
		getContentPane().add(southPane, BorderLayout.SOUTH);
	}

	public void setAttributes(Collection attributes) {
		// Sort attributes first
		java.util.List list = new ArrayList(attributes);
		Collections.sort(list, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				GKSchemaAttribute att1 = (GKSchemaAttribute)obj1;
				GKSchemaAttribute att2 = (GKSchemaAttribute)obj2;
				return att1.getName().compareTo(att2.getName());
			}
		});
		AttributeSelectionTableModel model = new AttributeSelectionTableModel(list);
		selectionTable.setModel(model);
	}

	public boolean isOKClicked() {
		return this.isOKClicked;
	}

	public java.util.List getSelectedAttributes() {
		AttributeSelectionTableModel model = (AttributeSelectionTableModel) selectionTable.getModel();
		java.util.List list = new ArrayList();
		for (Iterator it = model.selectionMap.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			Boolean value = (Boolean)model.selectionMap.get(key);
			if (value.booleanValue())
				list.add(key);
		}
		return list;
	}

	class AttributeSelectionTableModel extends AbstractTableModel {
		java.util.List attributes;
		Map selectionMap;
		
		AttributeSelectionTableModel(java.util.List attributes) {
			this.attributes = attributes;
			selectionMap = new HashMap();
			for (Iterator it = attributes.iterator(); it.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute) it.next();
				selectionMap.put(att, Boolean.FALSE);
			}
		}
		
		public String getColumnName(int col) {
			if (col == 0)
				return "Selection";
			else
				return "Attributes";
		}
		
		public int getColumnCount() {
			return 2;
		}
		
		public int getRowCount() {
			return attributes.size();
		}
		
		public Object getValueAt(int row, int col) {
			GKSchemaAttribute att = (GKSchemaAttribute) attributes.get(row);
			if (col == 1)
				return att.getName();
			else
				return selectionMap.get(att); 
		}
		
		public Class getColumnClass(int col) {
			if (col == 0)
				return Boolean.class;
			else
				return String.class;
		}
		
		public boolean isCellEditable(int row, int col) {
			if (col == 0)
				return true;
			return false;	
		}
		
		public void setValueAt(Object value, int row, int col) {
			if (col == 1)
				return;
			GKSchemaAttribute attribute = (GKSchemaAttribute) attributes.get(row);
			selectionMap.put(attribute, value);
		}
		
		public void selectAll() {
			for (Iterator it = selectionMap.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				selectionMap.put(key, Boolean.TRUE);
			}
		}
		
		public void selectNone() {
			for (Iterator it = selectionMap.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				selectionMap.put(key, Boolean.FALSE);
			}
		}
	}
}
