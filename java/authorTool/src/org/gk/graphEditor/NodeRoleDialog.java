/*
 * Created on Oct 7, 2003
 */
package org.gk.graphEditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.gk.render.Renderable;


public class NodeRoleDialog extends JDialog {
	private java.util.List nodes;
	private java.util.List roles;
	private String colNames[] = new String[]{"Entity", "Role"};
	// A flag
	private boolean isOKClicked = false;
	
	public NodeRoleDialog(java.util.List nodes, Frame parentFrame) {
		super(parentFrame);
		this.nodes = nodes;
		roles = new ArrayList(nodes.size());
		for (int i = 0; i < nodes.size(); i++) {
			roles.add("Input");
		}
		init();
	}
    
    public NodeRoleDialog(java.util.List nodes, Dialog parentFrame) {
        super(parentFrame);
        this.nodes = nodes;
        roles = new ArrayList(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            roles.add("Input");
        }
        init();
    }
    
    public NodeRoleDialog(java.util.List nodes) {
        this.nodes = nodes;
        roles = new ArrayList(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            roles.add("Input");
        }
        init();
    }
    
    public static NodeRoleDialog generateNodeRoleDialog(List nodes,
                                                        Component parentComp) {
        // Check if a Frame or Dialog can be found
        Component parentContainer = SwingUtilities.getRoot(parentComp);
        if (parentContainer instanceof Frame)
            return new NodeRoleDialog(nodes, (Frame)parentContainer);
        else if (parentContainer instanceof Dialog)
            return new NodeRoleDialog(nodes, (Dialog)parentContainer);
        else
            return new NodeRoleDialog(nodes);
    }
    
    public boolean isOKClicked() {
		return this.isOKClicked;
	}
	
	public java.util.List getRoles() {
		return this.roles;
	}
	
	private void init() {
		// Create a cutomized TableModel.
		TableModel model = new AbstractTableModel() {
			public String getColumnName(int col) {
				return colNames[col];
			}
			public int getColumnCount() {
				return 2;
			}
			public int getRowCount() {
				return nodes.size();
			}
			public Object getValueAt(int row, int col) {
				if (col == 0) {
					Renderable node = (Renderable) nodes.get(row);
					return node.getDisplayName();
				}
				if (col == 1) {
					return roles.get(row);
				}
				return null;
			}
			public boolean isCellEditable(int row, int col) {
				if (col == 1)
					return true;
				return false;
			}
			public void setValueAt(Object value, int row, int col) {
				if (col == 0)
					return;
				roles.set(row, value);
			}
		};
		// A JComboBox TableCellEditor
		JComboBox roleBox = new JComboBox(new String[]{"Input", "Output", "Catalyst",
		                                  "Inhibitor", "Activator"});
		DefaultCellEditor cellEditor = new DefaultCellEditor(roleBox);
		JTable table = new JTable(model);
		table.getColumnModel().getColumn(1).setCellEditor(cellEditor);
		getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
		// OK and Cancel buttons
		JPanel controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isOKClicked = true;
				dispose();
			}
		});
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
		okBtn.setDefaultCapable(true);
		getRootPane().setDefaultButton(okBtn);
		controlPane.add(okBtn);
		controlPane.add(cancelBtn);
		getContentPane().add(controlPane, BorderLayout.SOUTH);
	}
}