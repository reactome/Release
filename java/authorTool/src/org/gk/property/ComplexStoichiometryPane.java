/*
 * Created on May 26, 2004
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.Shortcut;

/**
 * 
 * @author wugm
 */
public class ComplexStoichiometryPane extends RenderablePropertyPane{

	private JLabel nameLabel;
	private JTable compositionTable;

	public ComplexStoichiometryPane() {
		init();
	}

	private void init() {
		setLayout(new BorderLayout());
		compositionTable = new JTable(new CompositionTableModel());
		add(new JScrollPane(compositionTable), BorderLayout.CENTER);
		JPanel northPane = new JPanel();
		northPane.setLayout(new BorderLayout());
		// Add name label
		nameLabel = new JLabel("Complex Composition");
		nameLabel.setHorizontalAlignment(JLabel.LEFT);
		nameLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		northPane.add(nameLabel, BorderLayout.WEST);
		add(northPane, BorderLayout.NORTH);
		compositionTable.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int col = compositionTable.columnAtPoint(e.getPoint());
					if (col != 1) {
						JOptionPane.showMessageDialog(compositionTable,
						                              "Click the cell for stoichiometry to set a value.",
						                              "Set Stoichiometry",
						                              JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}
		});
	}

	public void setRenderable(Renderable renderable) {
		this.r = renderable;
		// Extract subunits
		RenderableComplex complex = (RenderableComplex)renderable;
		CompositionTableModel model = (CompositionTableModel)compositionTable.getModel();
		model.setStoichiometries(complex.getStoichiometries());
	}

	public Map getStoichiometries() {
		CompositionTableModel model = (CompositionTableModel)compositionTable.getModel();
		return model.subunitMap;
	}

	public void refresh() {
		CompositionTableModel model = (CompositionTableModel)compositionTable.getModel();
		RenderableComplex complex = (RenderableComplex)r;
		model.setStoichiometries(complex.getStoichiometries());
	}

	/**
	 * Call this method to stop table editing. This method should be called
	 * when OK is clicked in a property dialog in case the editing is not finished.
	 */
	public void stopEditing() {
		DefaultCellEditor editor = (DefaultCellEditor)compositionTable.getCellEditor();
		if (editor != null)
			editor.stopCellEditing();
	}

	/**
	 * A helper to set the stoichiometry for a specified Renderable.
	 */
	private void setStoichiometry(Renderable renderable, int stoichiometry) {
		RenderableComplex complex = (RenderableComplex)this.r;
		complex.setStoichiometry(renderable, stoichiometry);
		fireRenderablePropertyChange(renderable,
		                             "stoichiometry",
		                             null,
		                             new Integer(stoichiometry));
	}

	/**
	 * Get a list of selected Renderables in the table.
	 * @return a list of Renderable objects.
	 */
	public java.util.List getSelection() {
		java.util.List list = new ArrayList();
		int[] rows = compositionTable.getSelectedRows();
		CompositionTableModel model = (CompositionTableModel)compositionTable.getModel();
		java.util.List tableNodes = model.nodes;
		if (rows != null) {
			for (int i = 0; i < rows.length; i++) {
				if (rows[i] > tableNodes.size() - 1)
					continue;
				Renderable node = (Renderable)tableNodes.get(rows[i]);
				list.add(node);
			}
		}
		return list;
	}

	/**
	 * Set the selection in the table.
	 * @param nodes a list of Renderable objects that should be selected in the table.
	 */
	public void setSelection(java.util.List nodes) {
		compositionTable.clearSelection();
		if (nodes != null) {
			CompositionTableModel model = (CompositionTableModel)compositionTable.getModel();
			for (int i = 0; i < model.nodes.size(); i++) {
				Renderable node = (Renderable)model.nodes.get(i);
				if (node == null)
					continue;
				if (node instanceof Shortcut)
					node = (Renderable) ((Shortcut)node).getTarget();
				// Check if node is selected
				for (Iterator it = nodes.iterator(); it.hasNext();) {
					Renderable node1 = (Renderable)it.next();
					if (node1 instanceof Shortcut)
						node1 = ((Shortcut)node1).getTarget();
					if (node1 == node) {
						compositionTable.addRowSelectionInterval(i, i);
						break;
					}
				}
			}
		}
	}

	public void addTableSelectionListener(ListSelectionListener l) {
		compositionTable.getSelectionModel().addListSelectionListener(l);
	}

	public void removeTableSelectionListener(ListSelectionListener l) {
		compositionTable.getSelectionModel().removeListSelectionListener(l);
	}

	class CompositionTableModel extends AbstractTableModel {

		private String[] headers = { "Subunit", "Stoichiometry" };
		// Keys: Node objects (Should not be targets)
		// Values: Integer objects for stoichiometries
		private Map subunitMap = new HashMap();
		// To keep the order
		private java.util.List nodes = new ArrayList();
		// For sorting
		private Comparator nodeSorter = new Comparator() {
			public int compare(Object obj1, Object obj2) {
				Renderable node1 = (Renderable)obj1;
				Renderable node2 = (Renderable)obj2;
				return node1.getDisplayName().compareTo(node2.getDisplayName());
			}
		};

		CompositionTableModel() {
		}

		public void setStoichiometries(Map stoichiometries) {
			subunitMap.clear();
			nodes.clear();
			if (stoichiometries == null) {
				fireTableDataChanged();
				return;
			}
			for (Iterator it = stoichiometries.keySet().iterator(); it.hasNext();) {
				Renderable node = (Renderable)it.next();
				if (node instanceof Shortcut)
					node = (Renderable)((Shortcut)node).getTarget();
				nodes.add(node);
				subunitMap.put(node, stoichiometries.get(node));
			}
			Collections.sort(nodes, nodeSorter);
			fireTableDataChanged();
		}

		public int getRowCount() {
			if (nodes.size() < 3)
				return 3;
			return nodes.size();
		}

		public int getColumnCount() {
			return headers.length;
		}

		public String getColumnName(int col) {
			return headers[col];
		}

		public Class getColumnClass(int col) {
			if (col == 1)
				return Integer.class;
			else
				return String.class;
		}

		public Object getValueAt(int row, int col) {
			if (row > nodes.size() - 1)
				return null;
			Renderable node = (Renderable)nodes.get(row);
			if (node == null)
				return null;
			Integer stoi = (Integer)subunitMap.get(node);
			switch (col) {
				case 0 :
					return node.getDisplayName();
				//case 1 :
				//	return node.getAttributeValue("DB_ID");
				case 1 :
					return stoi;
			}
			return null;
		}

		public void setValueAt(Object value, int row, int col) {
			if (col != 1 || row > nodes.size() - 1)
				return;
			Renderable node = (Renderable)nodes.get(row);
			subunitMap.put(node, value);
			setStoichiometry(node, ((Integer)value).intValue());
		}

		public boolean isCellEditable(int row, int col) {
			if (row > nodes.size() - 1)
				return false;
			if (col == 1)
				return true;
			return false;
		}

		public void insert(Renderable node) {
			// Check if node is a Shortcut. If true, find a same name node
			if (node instanceof Shortcut)
				node = (Renderable)((Shortcut)node).getTarget();
			Integer value= (Integer) subunitMap.get(node);
			if (value != null) {
				subunitMap.put(node, new Integer(value.intValue() + 1));
				int index = nodes.indexOf(node);
				fireTableRowsUpdated(index, index);
			}
			else {
				int i = 0;
				Renderable tmp = null;
				for (i = 0; i < nodes.size(); i++) {
					tmp = (Renderable)nodes.get(i);
					if (tmp.getDisplayName().compareTo(node.getDisplayName()) > 0) {
						break;
					}
				}
				nodes.add(i, node);
				subunitMap.put(node, new Integer(1));
				fireTableRowsInserted(i, i);
				compositionTable.setRowSelectionInterval(i, i);
			}
		}

		public void remove(Renderable node) {
			if (node instanceof Shortcut)
				node = (Renderable) ((Shortcut)node).getTarget();
			Integer value = (Integer) subunitMap.get(node);
			if (value != null) {
				int stoi = value.intValue() - 1;
				if (stoi == 0) {
					nodes.remove(node);
					int index = nodes.indexOf(node);
					subunitMap.remove(node);
					fireTableRowsDeleted(index, index);
				}
				else {
					subunitMap.put(node, new Integer(stoi));
					fireTableCellUpdated(nodes.indexOf(node), 2);
				}
			}
		}

		public java.util.List remove(int[] rows) {
			java.util.List list = new ArrayList();
			int minRow = Integer.MAX_VALUE;
			int maxRow = Integer.MIN_VALUE;
			for (int i = 0; i < rows.length; i++) {
				if (rows[i] < nodes.size()) {
					Object obj = nodes.get(rows[i]);
					list.add(obj);
					subunitMap.remove(obj);
				}
				if (rows[i] > maxRow)
					maxRow = rows[i];
				if (rows[i] < minRow)
					minRow = rows[i];
			}
			nodes.removeAll(list);
			fireTableRowsDeleted(minRow, maxRow);
			return list;
		}
	}

}
