/*
 * Created on Jul 1, 2003
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import org.gk.model.Modification;
import org.gk.render.Renderable;
import org.gk.util.AuthorToolAppletUtilities;


/**
 * A customized JPanel for editing modifications in entities.
 * @author wgm
 */
public class ModificationPane extends RenderablePropertyPane {
	private JTable modificationTable;
	// control buttons
	private JButton addRowBtn;
	private JButton removeRowBtn;
	private JButton moveUpBtn;
	private JButton moveDownBtn;
	// A mark
	private boolean isDirty;
	
	public ModificationPane() {
		init();
	}
	
	private void init() {
		// Set up the table
		ModificationTableModel model = new ModificationTableModel();
        modificationTable = new JTable();
		modificationTable.setModel(model);
        model.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                if (!duringSetting)
                    commit();
            }
        });
		// create the control buttons
		addRowBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("AddRow.gif"));
		addRowBtn.setActionCommand("add");
		removeRowBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("RemoveRow.gif"));
		removeRowBtn.setActionCommand("remove");
		moveUpBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("MoveUpRow.gif"));
		moveUpBtn.setActionCommand("moveUp");
		moveDownBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("MoveDownRow.gif"));
		moveDownBtn.setActionCommand("moveDown");
        JToolBar toolbar = new JToolBar();
        toolbar.setRollover(true);
        toolbar.setBorderPainted(false); // No border for toolbar
        toolbar.setFloatable(false);
        Dimension btnSize = new Dimension(20, 20);
        addRowBtn.setPreferredSize(btnSize);
        removeRowBtn.setPreferredSize(btnSize);
        moveUpBtn.setPreferredSize(btnSize);
        moveDownBtn.setPreferredSize(btnSize);
        addRowBtn.setToolTipText("Add row");
        removeRowBtn.setToolTipText("Remove selected row");
        moveUpBtn.setToolTipText("Move selected row up");
        moveDownBtn.setToolTipText("Move selected row down");
		toolbar.add(addRowBtn);
		toolbar.add(removeRowBtn);
		toolbar.add(moveUpBtn);
		toolbar.add(moveDownBtn);
		// Add action listeners
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();
				ModificationTableModel model = (ModificationTableModel)modificationTable.getModel();
				if (command.equals("add")) {
					model.addRow();
					model.fireTableRowsInserted(model.getRowCount(), model.getRowCount());
				}
				else if (command.equals("remove")) {
					int[] selectedRows = modificationTable.getSelectedRows();
					model.removeRows(selectedRows);
					model.fireTableDataChanged();
				}
				else if (command.equals("moveUp")) {
					int selectedRow = modificationTable.getSelectedRow();
					model.moveUp(selectedRow);
					model.fireTableRowsUpdated(selectedRow - 1, selectedRow);
					modificationTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
				}
				else if (command.equals("moveDown")) {
					int selectedRow = modificationTable.getSelectedRow();
					model.moveDown(selectedRow);
					model.fireTableRowsUpdated(selectedRow, selectedRow + 1);
					modificationTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
				}
			}
		};
		addRowBtn.addActionListener(l);
		removeRowBtn.addActionListener(l);
		moveUpBtn.addActionListener(l);
		moveDownBtn.addActionListener(l);
		modificationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				int[] selectedRows = modificationTable.getSelectedRows();
				if (selectedRows.length == 0) {
					removeRowBtn.setEnabled(false);
					moveUpBtn.setEnabled(false);
					moveDownBtn.setEnabled(false);
				}
				else if (selectedRows.length == 1) {
					removeRowBtn.setEnabled(true);
					int selectedRow = selectedRows[0];
					if (selectedRow == 0)
						moveUpBtn.setEnabled(false);
					else
						moveUpBtn.setEnabled(true);
					if (selectedRow == modificationTable.getRowCount() - 1)
						moveDownBtn.setEnabled(false);
					else
						moveDownBtn.setEnabled(true);
				}
				else {
					removeRowBtn.setEnabled(true);
					moveUpBtn.setEnabled(false);
					moveDownBtn.setEnabled(false);
				}
			}
		});
		removeRowBtn.setEnabled(false);
		moveUpBtn.setEnabled(false);
		moveDownBtn.setEnabled(false);
				
		setLayout(new BorderLayout());
        JPanel northPane = new JPanel();
        northPane.setLayout(new BorderLayout());
         JLabel title = new JLabel("Modifications");
         title.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
         add(title, BorderLayout.NORTH);
		add(new JScrollPane(modificationTable), BorderLayout.CENTER);
		northPane.add(title, BorderLayout.WEST);
        northPane.add(toolbar, BorderLayout.EAST);
        add(northPane, BorderLayout.NORTH);
		// Commit editing result when focus is lost
		modificationTable.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				if (modificationTable.isEditing()) {
					TableCellEditor editor = modificationTable.getCellEditor();
					editor.stopCellEditing();
				}
			}
		});
	}
	
	public void setRenderable(Renderable renderable) {
        super.setRenderable(renderable);
        duringSetting = true;
        if (renderable != null) {
            ModificationTableModel model = (ModificationTableModel) modificationTable.getModel();
            model.setModifications((java.util.List)renderable.getAttributeValue("modifications"));
        }
		duringSetting = false;
    }
	
	public java.util.List getModifications() {
		ModificationTableModel model = (ModificationTableModel) modificationTable.getModel();
		java.util.List mdfs = model.getModifications();
		return mdfs;
	}
	
	public void commit() {
		if (r != null && isDirty) {
			ModificationTableModel model = (ModificationTableModel) modificationTable.getModel();
			java.util.List mdfs = model.getModifications();
			if (mdfs == null || mdfs.size() == 0)
				r.setAttributeValue("modifications", null);
			else
				r.setAttributeValue("modifications", new ArrayList(mdfs));
            r.setIsChanged(true);
            // Fire isChanged instead modifications to minimize the overhead in the downstream
            fireRenderablePropertyChange(r, 
                                         "isChanged",
                                         Boolean.FALSE, 
                                         Boolean.TRUE);
		}
	}
	
	class ModificationTableModel extends AbstractTableModel {
		
		private String[] headers = new String[]{"Position", "Residue", "Modification"};
		private java.util.List modifications;
		
		ModificationTableModel() {
			modifications = new ArrayList();
			// Add three empty rows
			for (int i = 0; i < 3; i++)
				modifications.add(null);
		}
		
		public void setModifications(java.util.List newModifications) {
			modifications.clear();
			if (newModifications != null) {
				for (Iterator it = newModifications.iterator(); it.hasNext();) {
					Modification mdf = (Modification) it.next();
					modifications.add(mdf.clone());
				}
			}
			// To keep the minimum row number at 3
			if (modifications.size() < 3) {
				for (int i = modifications.size(); i < 3; i++)
					modifications.add(null);
			}
            fireTableDataChanged();
		}
		
		public java.util.List getModifications() {
            List rtn = new ArrayList();
			for (Iterator it = modifications.iterator(); it.hasNext();) {
				Object obj = it.next();
				if (obj != null)
                    rtn.add(obj);
			}
			if (rtn.size() == 0)
				return null;
			return rtn;
		}
		
		public int getRowCount() {
			return modifications.size();
		}
		
		public int getColumnCount() {
			return headers.length;
		}
		
		public String getColumnName(int col) {
			return headers[col];
		}
		
		public Class getColumnClass(int col) {
			if (col == 0)
				return Integer.class;
			else
				return String.class;
		}
		
		public Object getValueAt(int row, int col) {
			Modification modification = (Modification) modifications.get(row);
			if (modification == null)
				return null;
			switch (col) {
				case 0 :
					if (modification.getCoordinate() == -1)
						return null;
					return new Integer(modification.getCoordinate());
				case 1 :
					return modification.getResidue();
				case 2 :
					return modification.getModification();
				//case 3 :
				//	return modification.getModificationDbID();
			}
			return null; // Should not come here.
		}
		
		public void setValueAt(Object value, int row, int col) {
			if (value == null)
				return;
			Modification modification = (Modification) modifications.get(row);
			if (modification == null) { 
				modification = new Modification();
				modifications.set(row, modification);
			}
			switch (col) {
				case 0 :
					Integer posValue = (Integer) value;
					modification.setCoordinate(posValue.intValue());
					break;
				case 1 :
					modification.setResidue(value.toString());
					break;
				case 2 :
					modification.setModification(value.toString());
					break;
				//case 3 :
				//	modification.setModificationDbID(value.toString());
				//	break;
			}
			isDirty = true;
            fireTableDataChanged();
		}
		
		public boolean isCellEditable(int row, int col) {
			return true;
		}
		
		public void addRow() {
			modifications.add(null);
		}
		
		public void removeRows(int[] rows) {
			if (rows.length == 0)
				return;
			for (int i = 0; i < rows.length; i++)
                modifications.remove(rows[i]);
			isDirty = true;
            fireTableDataChanged();
		}
		
		public void moveUp(int row) {
			if (row <= 0)
				return ;
			Object obj = modifications.get(row - 1);
			modifications.set(row - 1, modifications.get(row));
			modifications.set(row, obj);
			isDirty = true;
            fireTableDataChanged();
		}
		
		public void moveDown(int row) {
			if (row >= modifications.size() - 1) 
				return;
			Object obj = modifications.get(row + 1);
			modifications.set(row + 1, modifications.get(row));
			modifications.set(row, obj);
			isDirty = true;
            fireTableDataChanged();
		}
	}

}
