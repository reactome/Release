/*
 * Created on Nov 21, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized JDialog to select one or more GKInstance.
 * @author wugm
 */
public class InstanceSelectDialog extends JDialog {
	// A mark
	private boolean isOKClicked = false;
	private SchemaViewPane localView; // Display local instance repository
	private SchemaViewPane dbView; // Display db instance repository
	//private JList instanceList; // The selected Instances
	private JTable instanceTable; // To display the selected instances
	private JButton okBtn;
	// A flag to control if multiple selection is enabled
	private boolean isMultiple;
	private Collection topLevelClasses;
	// A map to remember the selected instances
	private Map selectedInstances;
	
	public InstanceSelectDialog(JFrame parentFrame, String title) {
		super(parentFrame, title);
		init();
		selectedInstances = new HashMap();
		localView.getInstancePane().setSelectedInstances(selectedInstances);
	}
	
	public InstanceSelectDialog(JDialog parentDialog, String title) {
		super(parentDialog, title);
		init();
		selectedInstances = new HashMap();
		localView.getInstancePane().setSelectedInstances(selectedInstances);		
	}
	
	public void setIsMultipleValue(boolean isMultiple) {
		this.isMultiple = isMultiple;
		if (isMultiple)
			localView.getInstancePane().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		else
			localView.getInstancePane().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	
	private void init() {
		JLabel label = new JLabel("Please choose instances from the instance list:");
		label.setBorder(GKApplicationUtilities.getTitleBorder());
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		getContentPane().add(label, BorderLayout.NORTH);
		// Center Pane
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		JTabbedPane jtb = new JTabbedPane();
		localView = new SchemaViewPane();
		localView.setIsViewable(false);
		localView.setEditable(false);
		localView.hideBookmarkView();
		localView.getSchemaPane().setTitle("Allowed Classes");
		jtb.add("Local Schema View", localView);
		contentPane.add(jtb, BorderLayout.CENTER);
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
		Border border1 = BorderFactory.createEtchedBorder();
		Border border2 = BorderFactory.createEmptyBorder(6, 4, 6, 6);
		pane.setBorder(BorderFactory.createCompoundBorder(border1, border2));
		JPanel listPane = new JPanel();
		listPane.setPreferredSize(new Dimension(500, 150));
		listPane.setLayout(new BorderLayout());
		listPane.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		JLabel label1 = new JLabel("Selected Instances: ");
		label1.setHorizontalAlignment(JLabel.LEFT);
		listPane.add(label1, BorderLayout.NORTH);
		// Set up the table for the selected instances
		instanceTable = new JTable();
		instanceTable.getTableHeader().setReorderingAllowed(false);
		instanceTable.setCellSelectionEnabled(false);
		instanceTable.setRowSelectionAllowed(true);
		instanceTable.setColumnSelectionAllowed(false);
		InstanceTableModel model = new InstanceTableModel();
		instanceTable.setModel(model);
		listPane.add(new JScrollPane(instanceTable), BorderLayout.CENTER);
		// Need a way to control column width
		
		pane.add(listPane);
		JPanel btnPane = new JPanel();
		btnPane.setBorder(BorderFactory.createEmptyBorder(4, 24, 4, 24));
		btnPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(8, 8, 8, 8);
		JButton newInstance = new JButton("New Instance");
		newInstance.setMnemonic('N');
		newInstance.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createInstance();
			}
		});
		btnPane.add(newInstance, constraints);
		// Comment out temporarily
		JButton dbBrowser = new JButton("Browse Database");
		dbBrowser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				browseDB();
			}
		});
		dbBrowser.setMnemonic('D');
		newInstance.setPreferredSize(dbBrowser.getPreferredSize());
		constraints.gridy = 1;
		btnPane.add(dbBrowser, constraints);
		pane.add(btnPane);
		contentPane.add(pane, BorderLayout.SOUTH);
		contentPane.setBorder(BorderFactory.createRaisedBevelBorder());
		getContentPane().add(contentPane, BorderLayout.CENTER);
		// OK and Cancel Btns
		okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				isOKClicked = true;
				dispose();
			}
		});
		okBtn.setMnemonic('O');
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isOKClicked = false;
				dispose();
			}
		});
		cancelBtn.setMnemonic('C');
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
		JPanel btnPane1 = new JPanel();
		btnPane1.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		btnPane1.add(okBtn);
		btnPane1.add(cancelBtn);
		getContentPane().add(btnPane1, BorderLayout.SOUTH);
		installListeners();
	}
	
	private void installListeners() {
		// Add listeners
		installSelectionListener(localView);
		instanceTable.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					doListPopup(e);
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doListPopup(e);
			}		    
		});
	}
	
	private void installSelectionListener(SchemaViewPane view) {
		view.getInstancePane().addSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				// Figure out which view is selected
				JTabbedPane jtb = (JTabbedPane) localView.getParent();
				Component comp = jtb.getSelectedComponent();
				SchemaViewPane view = (SchemaViewPane) comp;
				SchemaClass schemaClass = view.getInstancePane().getSchemaClass();
				if (schemaClass == null)
					return;
				if (!isMultiple)
					selectedInstances.clear();
				java.util.List list = view.getInstancePane().getSelection();
				if (list == null || list.size() == 0)
					selectedInstances.remove(schemaClass);
				else
					selectedInstances.put(schemaClass, new ArrayList(list));
				// List all instances in the JList
				java.util.List instances = new ArrayList();
				for (Iterator it = selectedInstances.keySet().iterator(); it.hasNext();) {
					java.util.List tmp = (java.util.List) selectedInstances.get(it.next());
					if (tmp != null && tmp.size() > 0)
						instances.addAll(tmp);
				}
				InstanceTableModel model = (InstanceTableModel) instanceTable.getModel();
				model.setInstances(instances);
			}
		});
	}
	
	private void doListPopup(MouseEvent e) {
		int index = instanceTable.getSelectedRow();
		if (index >= 0) {
			JPopupMenu popup = new JPopupMenu();
			JMenuItem remove = new JMenuItem("Remove");
			remove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int[] selectedRows = instanceTable.getSelectedRows();
					InstanceTableModel model = (InstanceTableModel) instanceTable.getModel();
					model.removeInstances(selectedRows);
				}
			});
			popup.add(remove);
			popup.show(instanceTable, e.getX(), e.getY());
		}
	}
	
	private void createInstance() {
		NewInstanceDialog newDialog = new NewInstanceDialog(this, "Create a New Instance");
		java.util.List schemaClasses = InstanceUtilities.getAllSchemaClasses(topLevelClasses);
		SchemaClass schemaClass = localView.getSchemaPane().getSelectedClass();
		newDialog.setSchemaClasses(schemaClasses, schemaClass);
		newDialog.setModal(true);
		newDialog.setSize(450, 600);
		newDialog.setLocationRelativeTo(this);
		newDialog.setVisible(true);
		if (newDialog.isOKClicked()) {
			GKInstance instance = newDialog.getNewInstance();
			XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
			adaptor.addNewInstance(instance);
			// Need to update the schema tree display
			try {
				long counter = adaptor.getClassInstanceCount(instance.getSchemClass());
				localView.addInstance(instance);
				if (isMultiple)
					localView.addSelection(instance);
				else
					localView.setSelection(instance);
			}
			catch(Exception e) {
				System.err.println("InstanceSelectionDialog.createInstance(): " + e);
				e.printStackTrace();
			}
		}
	}
	
	private void browseDB() {
		MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(this);
		if (dba == null) {
			JOptionPane.showMessageDialog(this,
			                              "Cannot connect to the database.",
			                              "Error",
			                              JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (dbView == null) {
			dbView = new SchemaViewPane();
			dbView.setIsViewable(false);
			dbView.setEditable(false);
			dbView.hideBookmarkView();
			dbView.getSchemaPane().setTitle("Allowed Classes");
			JTabbedPane jtb = (JTabbedPane) localView.getParent();
			jtb.add("Database Repository", dbView);
			jtb.setSelectedComponent(dbView);
			installSelectionListener(dbView);
		}
		// Have to convert local SchemaClasses to DB SchemaClaases
		java.util.List dbClasses = new ArrayList(topLevelClasses.size());
		for (Iterator it = topLevelClasses.iterator(); it.hasNext();) {
			GKSchemaClass localCls = (GKSchemaClass) it.next();
			SchemaClass dbCls = dba.getSchema().getClassByName(localCls.getName());
			dbClasses.add(dbCls);
		}
		dbView.setTopLevelSchemaClasses(dbClasses, dba);
	}
	
	public void setTopLevelSchemaClasses(Collection schemaClasses) {
		this.topLevelClasses = schemaClasses;
		XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		localView.setTopLevelSchemaClasses(schemaClasses, adaptor);
	}
	
	public java.util.List getSelectedInstances() {
	    java.util.List list = new ArrayList();
		InstanceTableModel model = (InstanceTableModel) instanceTable.getModel();
		List instances = model.getInstances();
		for (Iterator it = instances.iterator(); it.hasNext();) {
		    list.add(it.next());
		}
		GKInstance instance = null;
		for (int i = 0; i < list.size(); i++) {
			instance = (GKInstance)list.get(i);
			if (instance.getDbAdaptor() instanceof MySQLAdaptor) {
				GKInstance localRef = PersistenceManager.getManager().getLocalReference(instance);
				// Need the whole thing instead of the shell instance.
				try {
                    SynchronizationManager.getManager().updateFromDB(localRef, instance);
                    list.set(i, localRef);
                }
                catch (Exception e) {
                    System.err.println("InstanceSelectDialog.getSelectedInstance(): " + e);
                    e.printStackTrace();
                }
			}
		}
		// Need to expand this list based on cardinality
		List rtn = new ArrayList(list.size());
		int cardinality = 0;
		for (Iterator it = list.iterator(); it.hasNext();) {
		    instance = (GKInstance) it.next();
		    // Instances are not the same as in InstanceTableModel for
		    // newly downloaded db instances.
		    cardinality = model.getCardinality(instance.getDBID());
		    for (int i = 0; i < cardinality; i++) {
		        rtn.add(instance);
		    }
		}
		return rtn;	
	}
	
	public boolean isOKClicked() {
		return this.isOKClicked;
	}
	
	public void addOKActionListener(ActionListener l) {
		okBtn.addActionListener(l);
	}
	
	public void removeOKActionListener(ActionListener l) {
		okBtn.removeActionListener(l);
	}
	
	class InstanceTableModel extends AbstractTableModel {
	    
	    private String[] headers = new String[]{"Instances", "Cardinality"};
	    //Actual data to be displayed
	    private Map instanceCardinalityMap;
	    // A sorted instance list for ordering
	    private List sortedInstances;
	    
	    public InstanceTableModel() {
	        instanceCardinalityMap = new HashMap();
	        sortedInstances = new ArrayList();
	    }
	    
	    /**
	     * Set the instances to be displayed. Default cardinality is 1 for all instances.
	     * @param instances
	     */
	    public void setInstances(List instances) {
	        sortedInstances.clear();
	        if (instances == null || instances.size() == 0)
	            return;
	        sortedInstances.addAll(instances);
	        InstanceUtilities.sortInstances(sortedInstances);
	        // Default with 1 for new instances
	        // Try to keep the old values
	        Integer one = new Integer(1);
	        for (Iterator it = sortedInstances.iterator(); it.hasNext();) {
	            GKInstance instance = (GKInstance) it.next();
	            if (instanceCardinalityMap.containsKey(instance))
	                continue;
	            instanceCardinalityMap.put(instance, one);
	        }
	        // Remove oboselete instances
	        for (Iterator it = instanceCardinalityMap.keySet().iterator(); it.hasNext();) {
	            GKInstance instance = (GKInstance) it.next();
	            if (!sortedInstances.contains(instance))
	                it.remove();
	        }
	        fireTableDataChanged();
	    }
	    
	    public List getInstances() {
	        return new ArrayList(sortedInstances);
	    }
	    
	    public int getCardinality(Long dbID) {
	        GKInstance instance = null;
	        for (Iterator it = instanceCardinalityMap.keySet().iterator(); it.hasNext();) {
	            instance = (GKInstance) it.next();
	            if (instance.getDBID().equals(dbID))
	                return ((Integer)instanceCardinalityMap.get(instance)).intValue();
	        }
	        return 1; // default value
	    }
	    
	    /**
	     * Remove instances whose indice are in the specified array.
	     * @param rows
	     */
	    public void removeInstances(int[] rows) {
	        GKInstance instance = null;
	        for (int i = 0; i < rows.length; i++) {
	            instance = (GKInstance) sortedInstances.get(rows[i]);
	            sortedInstances.remove(rows[i]);
	            instanceCardinalityMap.remove(instance);
	        }
	        // The table should be small enough. Just a lazy way.
	        fireTableRowsDeleted(0, getRowCount() - 1);
	    }
	    
	    public boolean isCellEditable(int rowIndex, int columnIndex) {
	        if (columnIndex == 0)
	            return false;
	        if (isMultiple)
	            return true;
	        else
	            return false;
        }
	    
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            // Only Cardinality can be modified
            if (isMultiple && columnIndex == 1) {
                GKInstance instance = (GKInstance) sortedInstances.get(rowIndex);
                instanceCardinalityMap.put(instance, new Integer(aValue.toString()));
            }
        }
	    
	    public int getColumnCount() {
	        return headers.length;
	    }
	    
	    public String getColumnName(int col) {
	        return headers[col];
	    }
	    
	    public Object getValueAt(int row, int col) {
	        GKInstance instance = (GKInstance) sortedInstances.get(row);
	        if (col == 0)
	            return instance;
	        else {
	            return (Integer) instanceCardinalityMap.get(instance);
	        }
	    }
	    
	    public int getRowCount() {
	        return sortedInstances.size();
	    }
	    
	    public Class getColumnClass(int col) {
	        if (col == 0)
	            return GKInstance.class;
	        else
	            return Integer.class;
	    }
	}

}
