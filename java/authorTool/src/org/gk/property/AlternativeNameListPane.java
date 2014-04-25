/*
 * Created on Jun 30, 2003
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.util.AuthorToolAppletUtilities;

/**
 * This customized JPanel is used for editing a property with multiple cardinality.
 * @author wgm
 */
public class AlternativeNameListPane extends RenderablePropertyPane {
	// GUIs
	private JLabel propertyNameLabel;
	private JList valueList;
	private JButton addBtn;
	private JButton removeBtn;
	private JButton editBtn;

	public AlternativeNameListPane() {
		init();	
	}
	
	public AlternativeNameListPane(String propertyName) {
		init();
		setPropertyName(propertyName);
	}
	
	/**
     * Use setRenderable(Renderable) instead. 
     * @param values
	 */
    private void setValues(java.util.List values) {
		DefaultListModel model = (DefaultListModel) valueList.getModel();
		model.clear();
		if (values != null) {
			for (Iterator it = values.iterator(); it.hasNext();) {
				model.addElement(it.next());
			}
		}
	}
	
	public java.util.List getValues() {
		java.util.List values = new ArrayList();
		DefaultListModel model = (DefaultListModel)valueList.getModel();
		for (int i = 0; i < model.getSize(); i++) {
			values.add(model.getElementAt(i));
		}
		return values;
	}
	
	private void init() {
		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new BorderLayout());
		JPanel northPane = new JPanel();
		northPane.setLayout(new BorderLayout());
		// Add name label
		propertyNameLabel = new JLabel("Property");
		propertyNameLabel.setHorizontalAlignment(JLabel.LEFT);
		propertyNameLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		northPane.add(propertyNameLabel, BorderLayout.WEST);
		// Add control buttons
		JToolBar toolbar = new JToolBar();
		toolbar.setRollover(true);
		toolbar.setBorderPainted(false); // No border for toolbar
		toolbar.setFloatable(false);
		Dimension btnSize = new Dimension(18, 18);
		addBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Add16.gif"));
		addBtn.setPreferredSize(btnSize);
		addBtn.setActionCommand("add");
		addBtn.setToolTipText("Add new name");
		toolbar.add(addBtn);
		removeBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Remove16.gif"));
		removeBtn.setPreferredSize(btnSize);
		removeBtn.setActionCommand("remove");
		removeBtn.setToolTipText("Remove");
		toolbar.add(removeBtn);
		removeBtn.setEnabled(false);
		editBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Edit16.gif"));
		editBtn.setPreferredSize(btnSize);
		editBtn.setActionCommand("edit");
		editBtn.setToolTipText("Edit");
		toolbar.add(editBtn);
		editBtn.setEnabled(false);
		northPane.add(toolbar, BorderLayout.EAST);
		add(northPane, BorderLayout.NORTH);
		// Add value list
		valueList = new JList();
		valueList.setToolTipText("Use the buttons at the top-right corner for editing");
		valueList.setModel(new DefaultListModel());
		add(new JScrollPane(valueList), BorderLayout.CENTER);
		// add actions
		installListeners();
	}
	
	private void installListeners() {
		// To help the users
		valueList.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					Point p = e.getPoint();
					int row = valueList.locationToIndex(p);
					if (row != -1) 
						editValue();
					else
						addNewValue();
				}
			}
		});
		// Enable/disbale remove and edit based on selection
		valueList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (valueList.getSelectedIndex() == -1) {
					removeBtn.setEnabled(false);
					editBtn.setEnabled(false);
				}
				else {
					removeBtn.setEnabled(true);
					if (valueList.getSelectedIndices().length == 1)
						editBtn.setEnabled(true);
				}
			}
		});
        valueList.getModel().addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {
                doAliasChanged();
            }
            public void intervalAdded(ListDataEvent e) {
                doAliasChanged();
            }
            public void intervalRemoved(ListDataEvent e) {
                doAliasChanged();
            }   
        });
		// Button actions
		// Please note: this is an action for string list.
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();
				if (command.equals("add")) {
					addNewValue();
				}
				else if (command.equals("remove")) {
					DefaultListModel model = (DefaultListModel) valueList.getModel();
					Object[] selectedValues = valueList.getSelectedValues();
					for (int i = 0; i < selectedValues.length; i++) {
						model.removeElement(selectedValues[i]);
					}
				}
				else if (command.equals("edit")) {
					editValue();
				}
			}
		};
		addBtn.addActionListener(l);
		removeBtn.addActionListener(l);
		editBtn.addActionListener(l);
	}
	
	private void addNewValue() {
		DefaultListModel model = (DefaultListModel) valueList.getModel();
		String input = JOptionPane.showInputDialog(AlternativeNameListPane.this,
												   "Input New " + propertyNameLabel.getText());
		if (input != null && input.trim().length() > 0) {
			String newProp = input.trim();
			model.addElement(newProp);                                          
		}
	}
	
	private void editValue() {
		DefaultListModel model = (DefaultListModel) valueList.getModel();
		String propValue = (String) valueList.getSelectedValue();
		int index = valueList.getSelectedIndex();
		String input = JOptionPane.showInputDialog(AlternativeNameListPane.this,
												   "Edit " + propertyNameLabel.getText(),
												   propValue);
		if (input != null) { // Means OK is clicked.
			if (input.trim().length() == 0) 
				model.removeElementAt(index);
			else
				model.setElementAt(input, index);
		}
	}
	
	public void setPropertyName(String propertyName) {
		propertyNameLabel.setText(propertyName);
	}
	
	public JList getValueList() {
		return valueList;
	}
    
	public void setRenderable(Renderable r) {
        super.setRenderable(r);
        duringSetting = true;
        List aliases = (List) r.getAttributeValue(RenderablePropertyNames.ALIAS);
        setValues(aliases);
        duringSetting = false;
    }
	
	private void doAliasChanged() {
        if (duringSetting)
            return;
	    List old = (List) r.getAttributeValue(RenderablePropertyNames.ALIAS);
	    List newList = getValues();
	    r.setAttributeValue(RenderablePropertyNames.ALIAS, newList);
	    fireRenderablePropertyChange(r,
	                                 RenderablePropertyNames.ALIAS,
	                                 old,
	                                 newList);
	}
}
