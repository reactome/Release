/*
 * Created on Jan 5, 2004
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.database.SchemaClassDefinitionTable;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized JDialog to display a SchemaClass' definition.
 * @author wugm
 */
public class SchemaClassDefinitionDialog extends JDialog {
	// The displayed SchemaClass
	private SchemaClass schemaClass = null;
	private SchemaClassDefinitionTable table;
	// These two GUIs need to be updated
	private JLabel slotLabel;
	private JTextArea slotArea;
	private JTextArea descArea;

	public SchemaClassDefinitionDialog() {
		init();
	}
	
	public SchemaClassDefinitionDialog(JFrame parentFrame) {
		super(parentFrame);
		init();
	}
	
	private void init() {
		Border emptyBorder = BorderFactory.createEmptyBorder(2, 4, 2, 4);
		// class description
		JLabel clsLabel = new JLabel("Class Description:");
		clsLabel.setBorder(emptyBorder);
		clsLabel.setHorizontalAlignment(SwingUtilities.LEFT);
		Font f = clsLabel.getFont();
		Font boldFont = f.deriveFont(Font.BOLD);
		clsLabel.setFont(boldFont);
		descArea = new JTextArea();
		descArea.setEditable(false);
		descArea.setLineWrap(true);
		descArea.setWrapStyleWord(true);
		JPanel clsDescPane = new JPanel();
		clsDescPane.setPreferredSize(new Dimension(750, 100));
		clsDescPane.setLayout(new BorderLayout());
		clsDescPane.add(clsLabel, BorderLayout.NORTH);
		clsDescPane.add(new JScrollPane(descArea), BorderLayout.CENTER);
		// slot table
		JLabel tableLabel = new JLabel("Slots:");
		tableLabel.setFont(boldFont);
		tableLabel.setHorizontalAlignment(SwingUtilities.LEFT);
		tableLabel.setBorder(emptyBorder);
		table = new SchemaClassDefinitionTable();
		JPanel tablePane = new JPanel();
		tablePane.setLayout(new BorderLayout());
		tablePane.add(tableLabel, BorderLayout.NORTH);
		tablePane.add(new JScrollPane(table), BorderLayout.CENTER);
		// Slot description
		slotLabel = new JLabel("Slot Description:");
		slotLabel.setFont(boldFont);
		slotLabel.setBorder(emptyBorder);
		slotLabel.setHorizontalAlignment(SwingUtilities.LEFT);
		slotArea = new JTextArea();
		slotArea.setEditable(false);
		slotArea.setLineWrap(true);
		slotArea.setWrapStyleWord(true);
		JPanel slotPane = new JPanel();
		slotPane.setPreferredSize(new Dimension(750, 70));
		slotPane.setLayout(new BorderLayout());
		slotPane.add(slotLabel, BorderLayout.NORTH);
		slotPane.add(new JScrollPane(slotArea), BorderLayout.CENTER);
		JSplitPane jsp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
		                                 tablePane,
		                                 slotPane);
		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
		                                clsDescPane,
		                                jsp1);
		getContentPane().add(jsp, BorderLayout.CENTER);
		// To close the dialog
		JPanel southPane = new JPanel();
		southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton closeBtn = new JButton("Close");
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		closeBtn.setMnemonic('C');
		closeBtn.setDefaultCapable(true);
		getRootPane().setDefaultButton(closeBtn);
		southPane.add(closeBtn);
		getContentPane().add(southPane, BorderLayout.SOUTH);
		// Add a table selection listener for the slot
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				SchemaAttribute att = table.getSelectedAttribute();
				if (att != null)
					slotLabel.setText(att.getName() + " Description:");
				else
					slotLabel.setText("Description:");
				//TODO Set slot description from somewhere?
			}
		});
		// Set the size
		setSize(750, 600);
		jsp1.setDividerLocation(350);
		jsp.setDividerLocation(100);
		GKApplicationUtilities.center(this);
		setModal(true);
	}
	
	public void setSchemaClass(SchemaClass schemaClass) {
		this.schemaClass = schemaClass;
		table.setSchemaClass(schemaClass);
		setTitle("SchemaClass: " + schemaClass.getName());
		descArea.setText(MetaDataManager.getManager().getSchemaClassDesc(schemaClass.getName()));
		descArea.setCaretPosition(0);
	}
}
