/*
 * Created on Jun 28, 2004
 */
package org.gk.database;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.gk.database.AttributePane.PropertyCellEditor;
import org.gk.database.AttributePane.PropertyTableModel;
import org.gk.graphEditor.ArrayListTransferable;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.model.StoichiometryInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.FileUtilities;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TextDialog;
import org.gk.util.TextFileFilter;

/**
 * This is a Controller class for AttributePane.
 * @author wugm
 */
public class AttributePaneController {
	private AttributePane attributePane;
	// Three actions for editing
	private Action viewAction;
	private Action addAction;
	private Action removeAction;
    private Action sortByAlphabetAction;
    private Action sortByCategoryAction;
    // For cut, copy and paste
    private Action copyAction;
    private Action pasteAction;
    private Action cutAction;
    private Action exportAction;
    // Use to set stoichiometry for Complex
    private Action setStoiAction;
    // Use to view referrers
    private Action viewReferrersAction;
    // For call back
    private PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	
	public AttributePaneController(AttributePane attPane) {
		this.attributePane = attPane;
		initActions();
	}
	
	protected void initActions() {
	    setStoiAction = new AbstractAction("Set Stoichiometry") {
	        public void actionPerformed(ActionEvent e) {
	            setStoichiometry();
	        }
	    };
	    viewAction = new AbstractAction("View", GKApplicationUtilities.createImageIcon(getClass(), "Abstract.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            viewSelectedCell();
	        }
	    };
	    viewAction.putValue(Action.SHORT_DESCRIPTION, "View");
	    addAction = new AbstractAction("Add", GKApplicationUtilities.createImageIcon(getClass(), "Add16.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            addValue();
	        }
	    };
	    addAction.putValue(Action.SHORT_DESCRIPTION, "Add");
	    removeAction = new AbstractAction("Remove", GKApplicationUtilities.createImageIcon(getClass(), "Remove16.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            removeSelectedCell();
	        }
	    };
	    removeAction.putValue(Action.SHORT_DESCRIPTION, "Remove");
	    sortByAlphabetAction = new AbstractAction("Sory by Names", 
	                                              GKApplicationUtilities.createImageIcon(getClass(), "AlphabSort.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            attributePane.setGroupAttributesByCategory(false);
	        }
	    };
	    sortByAlphabetAction.putValue(Action.SHORT_DESCRIPTION, "Click to sort properties by names");
	    sortByCategoryAction = new AbstractAction("Group by Categories",
	                                              GKApplicationUtilities.createImageIcon(getClass(), "TypeSort.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            attributePane.setGroupAttributesByCategory(true);
	        }
	    };
	    sortByCategoryAction.putValue(Action.SHORT_DESCRIPTION, "Click to group properties by categories");
	    viewReferrersAction = new AbstractAction("Display Referrers",
	                                             AuthorToolAppletUtilities.createImageIcon("DisplayReferrers.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            viewReferrers();
	        }
	    };
	    copyAction = new AbstractAction("Copy",
	                                    GKApplicationUtilities.createImageIcon(getClass(), "Copy16.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            copy();
	        }
	    };
	    pasteAction = new AbstractAction("Paste",
	                                     GKApplicationUtilities.createImageIcon(getClass(), "Paste16.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            paste();
	        }
	    };
	    cutAction = new AbstractAction("Cut",
	                                   GKApplicationUtilities.createImageIcon(getClass(), "Cut16.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            cut();
	        }
	    };
	    exportAction = new AbstractAction("Export",
	                                      GKApplicationUtilities.createImageIcon(getClass(), "Export16.gif")) {
	        public void actionPerformed(ActionEvent e) {
	            exportSelection();
	        }
	    };
	    // Disabel all actions first
	    viewAction.setEnabled(false);
	    addAction.setEnabled(false);
	    removeAction.setEnabled(false);
	    copyAction.setEnabled(false);
	    pasteAction.setEnabled(false);
	    cutAction.setEnabled(false);
	    exportAction.setEnabled(false);
	}
	
	private void copy() {
	    List<Object> selectedValues = attributePane.getSelectedValues();
	    if (selectedValues == null || selectedValues.size() == 0)
	        return;
	    // Create a transferable 
	    ArrayList<Object> list = new ArrayList<Object>();
	    for (Object obj : selectedValues) {
	        if (obj instanceof GKInstance) {
	            list.add("Instance:" + ((GKInstance)obj).getDBID());
	        }
	        else
	            list.add(obj);
	    }
	    ArrayListTransferable transferable = new ArrayListTransferable(list);
	    DataFlavor dataFlavor = new DataFlavor(AttributePane.class, "AttributePane");
	    transferable.setDataFlavor(dataFlavor);
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents(transferable, null);
	}
	
	private void exportSelection() {
	    List<Object> selectedValues = attributePane.getSelectedValues();
	    if (selectedValues == null || selectedValues.size() == 0)
	        return; // Nothing to export
	    // Get the file name
	    JFileChooser fileChooser = GKApplicationUtilities.createFileChooser(GKApplicationUtilities.getApplicationProperties());
	    FileFilter txtFilter = new TextFileFilter();
	    fileChooser.addChoosableFileFilter(txtFilter);
	    File file = GKApplicationUtilities.chooseSaveFile(fileChooser, 
	                                                      ".txt", 
	                                                      attributePane);
	    if (file == null)
	        return;
	    FileUtilities fu = new FileUtilities();
	    try {
	        fu.setOutput(file.getAbsolutePath());
	        for (Object obj : selectedValues) {
	            if (obj == null)
	                continue;
	            if (obj instanceof GKInstance) {
	                GKInstance inst = (GKInstance) obj;
	                fu.printLine(inst.getDBID() + "\t" + inst.getSchemClass().getName() + "\t" + inst.getDisplayName());
	            }
	            else
	                fu.printLine(obj.toString());
	        }
	        fu.close();
	        GKApplicationUtilities.storeCurrentDir(fileChooser, 
	                                               GKApplicationUtilities.getApplicationProperties());
	    }
	    catch(IOException e) {
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(attributePane,
	                                      "Cannot export selected values: " + e.getMessage(),
	                                      "Error in Exporting",
	                                      JOptionPane.ERROR_MESSAGE);
	    }
	}
	
	private void paste() {
	    if (!isPasteSupported())
	        return;
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    Transferable data = clipboard.getContents(null);
	    DataFlavor dataFlavor = new DataFlavor(AttributePane.class, "AttributePane");
	    if (data.isDataFlavorSupported(dataFlavor)) {
	        try {
	            ArrayList<Object> values = (ArrayList<Object>) data.getTransferData(dataFlavor);
	            List<Object> copiedValues = new ArrayList<Object>();
	            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	            for (Object obj : values) {
	                if (obj.toString().startsWith("Instance:")) {
	                    //TODO: This may be dangerous if an instance DB_ID has been changed!
	                    String dbIdText = obj.toString().split(":")[1];
	                    GKInstance inst = fileAdaptor.fetchInstance(new Long(dbIdText));
	                    copiedValues.add(inst);
	                }
	                else
	                    copiedValues.add(obj);
	            }
	            GKInstance inst = (GKInstance) attributePane.getInstance();
	            String attributeName = attributePane.getSelectedAttributeName();
	            if (attributeName == null || inst == null || values == null)
	                return;
	            GKSchemaAttribute attribute = (GKSchemaAttribute) inst.getSchemClass().getAttribute(attributeName);
	            // Check if there is any value is invalid. If true, do nothing
	            for (Object obj : copiedValues) {
	                if (!attribute.isValidValue(obj)) {
	                    JOptionPane.showMessageDialog(attributePane, 
	                                                  "Cannot be pasted for the selected attribute: pasting value is not allowed.",
	                                                  "Error in Pasting", 
	                                                  JOptionPane.ERROR_MESSAGE);
	                    return;
	                }
	            }
	            // Check if instance type value is valid
	            if (attribute.isInstanceTypeAttribute()) {
	                AttributeEditValidator validator = attributePane.getEditValidator();
	                if (!validator.validate((GKInstance) attributePane.getInstance(), 
	                                                     attributeName,
	                                                     copiedValues,
	                                                     attributePane))
	                    return;
	            }
	            for (Object obj : copiedValues) {
	                if (attribute.isValidValue(obj)) { // Just in case
	                    inst.addAttributeValue(attribute, obj);
	                }
	            }
	            shuffleRowsToFitNewInstance();
	            AttributeEditEvent event = createAttributeEventForAdding(attributeName);
	            postProcessAttributeEdit(event);
	        }
	        catch(Exception e) {
	            e.printStackTrace();
	        }
	    }
	}

    private boolean isPasteSupported() {
        // Make sure it works only for one cell selection
        JTable propTable = attributePane.getPropertyTable();
        if (propTable.getSelectedColumnCount() != 1 ||
            propTable.getSelectedRowCount() != 1)
                return false;
        int row = propTable.getSelectedRow();
        int col = propTable.getSelectedColumn();
        PropertyTableModel model = (PropertyTableModel) propTable.getModel();
        if (!model.isCellEditable(row, col))
            return false;
        // Make sure it cannot paste to a single valued attribute that has a value already
        try {
            String attributeName = attributePane.getSelectedAttributeName();
            GKInstance inst = (GKInstance) attributePane.getInstance();
            GKSchemaAttribute attribute = (GKSchemaAttribute) inst.getSchemClass().getAttribute(attributeName);
            if (!attribute.isMultiple() && inst.getAttributeValue(attribute) != null)
                return false;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    DataFlavor dataFlavor = new DataFlavor(AttributePane.class, "AttributePane");
	    return clipboard.isDataFlavorAvailable(dataFlavor);
    }
	
	private void cut() {
	    if(!isRemoveSupported())
	        return;
	    copy(); // Do copy first
	    removeSelectedCell();
	}

    private boolean isRemoveSupported() {
        JTable propTable = attributePane.getPropertyTable();
	    // Make sure the first names column is not selected
	    int[] cols = propTable.getSelectedColumns();
	    if (cols == null || cols.length == 0)
	        return false;
	    for (int i : cols) {
	        if (i == 0)
	            return false; // Cannot 
	    }
	    int[] rows = propTable.getSelectedRows();
	    if (rows == null || rows.length == 0)
	        return false;
	    PropertyTableModel model = (PropertyTableModel) propTable.getModel();
	    // Check if any uneditable value is selected
	    for (int row : rows) {
	        if (!model.isCellEditable(row, 1))
	            return false;
	    }
	    return true;
    }
	
	private void viewReferrers() {
	    JTable propTable = attributePane.getPropertyTable();
	    int rowCount = propTable.getSelectedRowCount();
	    int colCount = propTable.getSelectedColumnCount();
	    if (rowCount != 1 ||
	            colCount != 1)
	        return;
	    PropertyTableModel model = (PropertyTableModel) propTable.getModel();
	    // Only need to handle a single cell selection.
	    int row = propTable.getSelectedRow();
	    int col = propTable.getSelectedColumn();
	    Object obj = model.getValueAt(row, col);
	    if (obj instanceof GKInstance) {
	        GKInstance instance = (GKInstance) obj;
	        displayReferrers(instance);
	    }
	}

    protected void displayReferrers(GKInstance instance) {
        ReverseAttributePane referrersPane = new ReverseAttributePane();
        referrersPane.displayReferrersWithCallback(instance, attributePane);
    }
	
	public Action getViewAction() {
		return viewAction;
	}
	
	public Action getViewReferrersAction() {
	    return viewReferrersAction;
	}
	
	public Action getAddAction() {
		return addAction;
	}
	
	public Action getCopyAction() {
	    return copyAction;
	}
	
	public Action getExportAction() {
	    return this.exportAction;
	}
	
	public Action getPasteAction() {
	    return pasteAction;
	}
	
	public Action getCutAction() {
	    return cutAction;
	}
	
	public Action getRemoveAction() {
		return removeAction;
	}
	
    public Action getSortByAlphabetAction() {
        return sortByAlphabetAction;
    }
    
    public Action getSortByCategoryAction() {
        return sortByCategoryAction;
    }
    
    protected void validateActions() {
		JTable propTable = attributePane.getPropertyTable();
		GKInstance instance = (GKInstance) attributePane.getInstance();
		// Only work for single selection
		int colCount = propTable.getSelectedColumnCount();
		int rowCount = propTable.getSelectedRowCount();
		if (colCount != 1 || rowCount != 1) {
		    viewAction.setEnabled(false);
		    addAction.setEnabled(false);
//		    removeAction.setEnabled(false);
		    viewReferrersAction.setEnabled(false);
		    pasteAction.setEnabled(false);
		    return;
		}
		PropertyTableModel model = (PropertyTableModel) propTable.getModel();
		int row = propTable.getSelectedRow();
		int col = propTable.getSelectedColumn();
		String attName = model.getKeyAt(row);
		int type = model.getValueType(row);
		// Disbale all actions for these attributes.
		if (attName.equals("DB_ID") || attName.equals("_Protege_ID") ||
			type == SchemaAttribute.BOOLEAN_TYPE ||
			type == SchemaAttribute.FLOAT_TYPE ||
			type == SchemaAttribute.INTEGER_TYPE ||
			type == SchemaAttribute.LONG_TYPE) {
			viewAction.setEnabled(false);
			addAction.setEnabled(false);
			removeAction.setEnabled(false);
			viewReferrersAction.setEnabled(false);
			return;
		}
		SchemaAttribute att = null;
		try {
			att = instance.getSchemClass().getAttribute(attName);
		}
		catch(InvalidAttributeException e) {
			System.err.println("AttributePane.validateActions(): " + e);
			e.printStackTrace();
		}
		if (col == 0) {
			viewAction.setEnabled(false);
			if (att.isMultiple()) {
				addAction.setEnabled(true);
				addAction.putValue(Action.NAME, "Add");
			}
			else {
				addAction.setEnabled(true);
				addAction.putValue(Action.NAME, "Set");
			}
			if (attributePane.isUneditableAttributeName(attName))
			    addAction.setEnabled(false);
			removeAction.setEnabled(false);
			viewReferrersAction.setEnabled(false);
			pasteAction.setEnabled(true);
		}
		else {
		    // For viewAction
		    Object value = model.getValueAt(row, 1);
            if (value == null)
                viewAction.setEnabled(false);
            else
                viewAction.setEnabled(true);
            // For add and remove
            if (model.isCellEditable(row, col)) {
                if (att.isMultiple()) {
                    addAction.setEnabled(true);
                    addAction.putValue(Action.NAME, "Add");
                }
                else {
                    addAction.setEnabled(true);
                    addAction.putValue(Action.NAME, "Set");
                }
                if (value == null)
                    removeAction.setEnabled(false);
                else
                    removeAction.setEnabled(true);
                pasteAction.setEnabled(true);
                if (value == null) {
                    cutAction.setEnabled(false);
                    copyAction.setEnabled(false);
                    exportAction.setEnabled(false);
                }
                else {
                    cutAction.setEnabled(true);
                    copyAction.setEnabled(true);
                    exportAction.setEnabled(true);
                }
            }
            else {
                addAction.setEnabled(false);
                removeAction.setEnabled(false);
                pasteAction.setEnabled(false);
                cutAction.setEnabled(false);
            }
            viewReferrersAction.setEnabled(value instanceof GKInstance);
		}
		// Last check for paste: have to make sure there is something in the clipboad
		if (!isPasteSupported()) // Only check for not support. Other cases should be taken care already.
		    pasteAction.setEnabled(false);
	}
	
    boolean shouldSetStoiActionDisplayed() {
        GKInstance instance = (GKInstance) attributePane.getInstance();
        if (!instance.getSchemClass().isa("Complex"))
            return false;
        JTable propTable = attributePane.getPropertyTable();
        // Only work for single selection
        int colCount = propTable.getSelectedColumnCount();
        int rowCount = propTable.getSelectedRowCount();
        if (colCount != 1 || rowCount != 1) {
            return false;
        }
        PropertyTableModel model = (PropertyTableModel) propTable.getModel();
        int row = propTable.getSelectedRow();
        int col = propTable.getSelectedColumn();
        Object value = model.getValueAt(row, col);
        if (value instanceof StoichiometryInstance)
            return true;
        return false;
    }
    
    boolean shouldStoichiometryInstanceBeUsed(int row, int col) {
        GKInstance instance = (GKInstance) attributePane.getInstance();
        if (!instance.getSchemClass().isa("Complex"))
            return false;
        JTable propTable = attributePane.getPropertyTable();
        PropertyTableModel model = (PropertyTableModel) propTable.getModel();
        String attName = model.getKeyAt(row);
        if (attName.equals(ReactomeJavaConstants.hasComponent))
            return true;
        return false;
    }
    
    /**
     * This method should be called if shouldSetStoiActionDisplayed() method returns true.
     * @see shouldSetStoiActionDisplayed.
     */
    private void setStoichiometry() {
        JTable propTable = attributePane.getPropertyTable();
        GKInstance instance = (GKInstance) attributePane.getInstance();
        int row = propTable.getSelectedRow();
        int col = propTable.getSelectedColumn();
        PropertyTableModel model = (PropertyTableModel) propTable.getModel();
        // The following cast should be validated by shouldSetStoiActionDisplayed()
        StoichiometryInstance stoiInstance = (StoichiometryInstance) model.getValueAt(row, col);
        // Remember the string name
        String attName = model.getKeyAt(row);
        String reply = JOptionPane.showInputDialog(attributePane,
                                                  "Please input a positive integer:",
                                                  "Set Stoichiometry",
                                                  JOptionPane.OK_CANCEL_OPTION);
        if (reply == null)
            return; // Cancelled
        try {
            int stoi = Integer.parseInt(reply);
            int oldStoi = stoiInstance.getStoichiometry();
            if (stoi == oldStoi) // Nothing changed
                return;
            stoiInstance.setStoichiometry(stoi);
            model.fireTableRowsUpdated(row, col);
            // Have to make sure the underneath data structure is correct
            List values = instance.getAttributeValuesList(attName);
            GKInstance wrapper = stoiInstance.getInstance();
            AttributeEditEvent event = new AttributeEditEvent(this, instance, attName);
            event.setEditingComponent(attributePane);
            List<GKInstance> changed = new ArrayList<GKInstance>();
            if (stoi > oldStoi) {
                // Need to add
                int index = values.lastIndexOf(wrapper);
                for (int i = oldStoi; i < stoi; i++) {
                    values.add(index, wrapper);
                    changed.add(wrapper);
                }
                event.setEditingType(AttributeEditEvent.ADDING);
                event.setAddedInstances(changed);
            }
            else if (stoi < oldStoi) {
                // Need to remove
                for (int i = stoi; i < oldStoi; i++) {
                    values.remove(wrapper);
                    changed.add(wrapper);
                }
                event.setEditingType(AttributeEditEvent.REMOVING);
                event.setRemovedInstances(changed);
            }
            postProcessAttributeEdit(event);
        }
        catch(NumberFormatException e) {
            System.err.println("AttributePaneController.setStoichiometry(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(attributePane,
                                          "The input value is not a correct stoichiometry value.",
                                          "Error in Setting Stoichiometry.",
                                          JOptionPane.ERROR_MESSAGE);
        }
        catch(Exception e) {
            System.err.println("AttributePaneController.setStoichiometry(): " + e);
            e.printStackTrace();
        }
    }
    
    Action getSetStoiAction() {
        return this.setStoiAction;
    }
    
	public AttributePane getAttributePane() {
		return attributePane;
	}
	
	public void addValue() {
	    JTable propTable = attributePane.getPropertyTable();
	    // Stop any editing first
	    if (propTable.isEditing()) {
	        propTable.getCellEditor().stopCellEditing();
	    }
	    // Make these variables final to be used in a inner class.
	    PropertyTableModel model = (PropertyTableModel) propTable.getModel();
	    int row = propTable.getSelectedRow();
	    int col = propTable.getSelectedColumn();
	    int type = model.getValueType(row);
	    final String attName = model.getKeyAt(row);
	    Object newValue = null;
	    JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, 
	            attributePane);
	    switch(type) {
	        case SchemaAttribute.INSTANCE_TYPE :
	            addInstanceValue(attName, parentFrame);
	            return;
	        case SchemaAttribute.STRING_TYPE :
	            TextDialog dialog = new TextDialog(parentFrame, 
	                    "Input a New " + attName, 
	                    true);
	            dialog.setVisible(true);
	            if (dialog.isOKClicked()) {
	                String text = dialog.getText().trim();
	                // Remove new line
	                if (text.endsWith("\n"))
	                    text = text.substring(0, text.length() - 1);
	                if (text.length() > 0) {
	                    // Do a validation
	                    AttributeEditValidator validator = attributePane.getEditValidator();
	                    if (!validator.validate((GKInstance)attributePane.getInstance(),
	                                           attName,
	                                           text,
	                                           attributePane))
	                        return;
	                    newValue = text;
	                }
	            }                        		   
	            break;
	        case SchemaAttribute.BOOLEAN_TYPE :
	            break;	
	        case SchemaAttribute.LONG_TYPE :
	            break;
	        case SchemaAttribute.INTEGER_TYPE :
	            break;
	        case SchemaAttribute.FLOAT_TYPE :
	            break;
	        default :
	            System.err.println("AttributePane.addValue(): An unknown data type for attribute.");
	    }
	    addValue(newValue, attName);
        AttributeEditEvent event = createAttributeEventForAdding(attName);
        postProcessAttributeEdit(event);
	}
	
	public void reorder(String attName, int valueIndex, Object anchorValue) {
		try {
			GKInstance instance = (GKInstance) attributePane.getInstance();
			java.util.List list = instance.getAttributeValuesList(attName);
			int index = list.indexOf(anchorValue);
			Object value = list.remove(valueIndex);
			list.add(index, value);
			// To fire adding events
			AttributeEditEvent event = new AttributeEditEvent(attributePane, instance, attName);
			event.setEditingType(AttributeEditEvent.REORDER);
			event.setEditingComponent(attributePane);
			postProcessAttributeEdit(event);
		}
		catch(Exception e) {
			System.err.println("AttributePaneController.reorder(): " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Insert an Instance type value into the specified attribute at the specified index.
	 * @param att the attribute to be inserted
	 * @param instance the inserting value
	 * @param index the inserting position
	 * @return true for a successful inserting false for an unccessful inserting.
	 */
	public boolean insertValue(SchemaAttribute att, GKInstance instance, int index) {
	    if (!att.isValidValue(instance)) // Block an invalid value
	        return false;
	    try {
	        GKInstance editingInstance = (GKInstance) attributePane.getInstance();
	        java.util.List list = editingInstance.getAttributeValuesList(att);
	        if (list == null || list.size() == 0)
	            editingInstance.addAttributeValue(att, instance); // Should be the first one
	                                                              // don't need account for index
	        else {
	            list.add(index, instance);
	        }
			// To fire adding events
			AttributeEditEvent event = new AttributeEditEvent(attributePane, editingInstance, att.getName());
			event.setEditingType(AttributeEditEvent.ADDING);
			event.setEditingComponent(attributePane);
			postProcessAttributeEdit(event);
			return true;
	    }
	    catch(Exception e) {
	        System.err.println("AttributePaneController.insertValue(): " + e);
	        e.printStackTrace();
	        return false;
	    }
	}
	
	/**
	 * Adds a new value to a multi-value slot.
	 * 
	 * @param newValue
	 * @param attName
	 */
	private void addValue(Object newValue, String attName) {
	    if (newValue != null) {
	        try {
	            Instance instance = attributePane.getInstance();
	            SchemaAttribute att = instance.getSchemClass().getAttribute(attName);
	            if (newValue instanceof java.util.List) {
	                java.util.List list = (java.util.List) newValue;
	                for (Iterator it = list.iterator(); it.hasNext();)
	                    instance.addAttributeValue(att, it.next());
	            }
	            else
	                instance.addAttributeValue(att, newValue);
	            shuffleRowsToFitNewInstance();
	        } 
	        catch(Exception e) {
	            System.err.println("AttributePane.addValue(): " + e);
	            e.printStackTrace();
	        }
	    }		
	}
	
	/**
	 * Creates an empty slot.
	 *  
	 * @param attName
	 */
	public void addEmptyValue(String attName) {
		try {
			PropertyTableModel model = (PropertyTableModel) attributePane.getPropertyTable().getModel();
			model.setEmptyCell(true, attName);
			shuffleRowsToFitNewInstance();
		} catch(Exception e) {
			System.err.println("AttributePane.addEmptyValue(): " + e);
			e.printStackTrace();
		}
	}
	
	private void shuffleRowsToFitNewInstance() {
        AttributeTable propTable = (AttributeTable)attributePane.getPropertyTable();
        PropertyTableModel model = (PropertyTableModel) propTable.getModel();
        int row = propTable.getSelectedRow();
        int col = propTable.getSelectedColumn();
        int row0 = model.getRowCount();
        model.refresh();
        int row1 = model.getRowCount();
        if (col == 0) {
            propTable.setColumnSelectionInterval(0, 0);
            propTable.setRowSelectionInterval(row, row);
        }
        else if (col == 1) {
            propTable.setColumnSelectionInterval(1, 1);
            if (row1 > row0) { // More values are added
                int diff = row1 - row0;
                int lastRow = model.getLastRowForAttributeAt(row);
                propTable.setRowSelectionInterval(lastRow - diff + 1, lastRow);
            }
            else {
                propTable.setRowSelectionInterval(row, row);
            }
        }
        
		// Use information that the attribute table model
		// knows about the attributes to color code the
		// rows in the table.
		propTable.setGrayRows(model.getDefiningRows());			
   }

	private SchemaAttribute attributeFromName(String attName) {
		SchemaAttribute att = null;
		GKInstance instance = (GKInstance)attributePane.getInstance();
		try {
			att = instance.getSchemClass().getAttribute(attName);
		}
		catch(InvalidAttributeException e) {
			System.err.println("AttributePaneController.attributeFromName(): " + e);
			e.printStackTrace();
		}
		
		return att;
	}
	
	public boolean isMultipleAttribute(String attName) {
		SchemaAttribute att = attributeFromName(attName);
		if (att==null)
			return false; // this is a bit arbitrary
		return att.isMultiple();
	}
	
	protected void addInstanceValue(String attName, JFrame parentFrame) {
		SchemaAttribute att = attributeFromName(attName);
		if (att.getAllowedClasses() == null || att.getAllowedClasses().size() == 0) {
			JOptionPane.showMessageDialog(attributePane,
									"There is no allowed class for attribute "+attName,
									"No Allowed Classes",
									JOptionPane.ERROR_MESSAGE);
			return;
		}
		addInstanceValue(att.getAllowedClasses(), att, parentFrame);
	}
	
    private AttributeEditEvent createAttributeEventForAdding(String attName) {
        GKInstance instance = (GKInstance) attributePane.getInstance();
        AttributeEditEvent event = new AttributeEditEvent(attributePane, instance, attName);
        event.setEditingComponent(attributePane);
        // Set the type based on the values
        try {
            SchemaAttribute att = instance.getSchemClass().getAttribute(attName);
            if (att.isMultiple())
                event.setEditingType(AttributeEditEvent.ADDING);
            else {
                java.util.List values = instance.getAttributeValuesList(att);
                if (values == null || values.size() == 0)
                    event.setEditingType(AttributeEditEvent.ADDING);
                else {
                    event.setEditingType(AttributeEditEvent.UPDATING);
                    event.setRemovedInstances(values);
                }
            }
        }
        catch (Exception e) {
            System.err.println("AttributePaneController.createAttributeEventForAdding(): " + e);
            e.printStackTrace();
        }
        return event;
    }
	
	protected void addInstanceValue(Collection topLevelClasses, SchemaAttribute att, JFrame parentFrame) {
	    GKInstance instance = (GKInstance) attributePane.getInstance(); 
	    String attName = att.getName();
		InstanceSelectDialog dialog1 = new InstanceSelectDialog(parentFrame,
																"Select Instance for " + attName);
		dialog1.setTopLevelSchemaClasses(topLevelClasses);
		dialog1.setIsMultipleValue(att.isMultiple());
		dialog1.setModal(true);
		dialog1.setSize(1000, 700);
		GKApplicationUtilities.center(dialog1);
		dialog1.setVisible(true); 
		if (dialog1.isOKClicked()) {
			java.util.List instances = dialog1.getSelectedInstances();
			if (instances == null || instances.size() == 0)
				return;
			// Need to validate if the new values are correct
			AttributeEditValidator validator = attributePane.getEditValidator();
			if (!validator.validate((GKInstance) attributePane.getInstance(), 
			                                     att.getName(),
			                                     instances,
			                                     attributePane))
			    return;
			handleInverseAttribute(att, instances);
			// To fire adding events
			AttributeEditEvent event = createAttributeEventForAdding(attName);
			// In case anything is changed
			attributePane.setInstance(instance);

			// Have to call this method after event settings
			addValue(instances, attName);
			event.setAddedInstances(instances);

			// Put in an extra empty slot, to allow the user to
			// add extra instances using a combo box.  This is
			// only relevant to slots that can accept multiple
			// values.
			if (isMultipleAttribute(attName))
				addEmptyValue(attName);
			
			// Keep the displayed instance reference. The next line call might
			// null displayed instance
			postProcessAttributeEdit(event);
			attributePane.setInstance((GKInstance)instance);
		}
	}
	
	protected void handleInverseAttribute(SchemaAttribute att, java.util.List instances) {
		// Have to check if inverseAttribute exists    
		if (att.getInverseSchemaAttribute() != null) {
			try {
				AttributeEditEvent event = new AttributeEditEvent(attributePane);
				String inverseAttName = att.getInverseSchemaAttribute().getName();
				// Set the inverse attribute value for the selected instances
				event.setAttributeName(inverseAttName);
				java.util.List tmp = new ArrayList(1);
				GKInstance instance = (GKInstance) attributePane.getInstance(); 
				tmp.add(instance);
				event.setAddedInstances(tmp);
				for (Iterator it = instances.iterator(); it.hasNext();) {
					GKInstance inverseInstance = (GKInstance)it.next();
					// Have to download the inverseInstance if it is a shell instance
					if (inverseInstance.isShell()) {
						int reply = JOptionPane.showConfirmDialog(attributePane,
													  "\"" + inverseInstance.getDisplayName() + "\" is a shell instance. " +
													  "It should be downloaded first to be used for this type of slots. " +													  "\nDo you want to download it now? Selecting NO will not add this instance to the slot.",
													  "Downloading Shell Instance",
													  JOptionPane.YES_NO_OPTION);
						if (reply == JOptionPane.NO_OPTION) {
							it.remove();
							continue;
						}
						else {
							MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(attributePane);
							if (dba == null) {
								JOptionPane.showMessageDialog(attributePane,
															  "Cannot connect to the database. \"" + inverseInstance.getDisplayName() + "\"" +
															  "\nwill not be added to the slot.",
															  "Error",
															  JOptionPane.ERROR_MESSAGE);
								it.remove();
								continue;
							}
							GKInstance dbCopy = dba.fetchInstance(inverseInstance.getSchemClass().getName(),
																  inverseInstance.getDBID());
							if (dbCopy == null) {
								JOptionPane.showMessageDialog(attributePane,
															  "Cannot download shell instance \"" + inverseInstance.getDisplayName() + "\"." +
															  "\nIt will not be added to the slot.",
															  "Error",
															  JOptionPane.ERROR_MESSAGE);
								it.remove();
								continue;
							}
							SynchronizationManager.getManager().updateFromDB(inverseInstance, dbCopy);
							AttributeEditManager.getManager().attributeEdit(inverseInstance);
						}
					}
					if (att.getInverseSchemaAttribute().isMultiple()) {
						event.setEditingType(AttributeEditEvent.ADDING);
					}
					else {
						java.util.List values = inverseInstance.getAttributeValuesList(inverseAttName);
						if (values == null || values.size() == 0)
							event.setEditingType(AttributeEditEvent.ADDING);
						else {
							event.setEditingType(AttributeEditEvent.UPDATING);
							event.setRemovedInstances(values);
						}
					}
					// Have to use attribute name. InverseAttribute cannot work here.
					// Probably this is a bug!
					if (inverseInstance.getSchemClass().isValidAttribute(inverseAttName)) {
						inverseInstance.addAttributeValue(inverseAttName, instance);
						event.setEditingInstance(inverseInstance);
						postProcessAttributeEdit(event);
					}
				}
			}
			catch (Exception e) {
				System.err.println("AttributePaneController.handleInverseAttribute(): " + e);
				e.printStackTrace();
			}
		}
	}
	
	public void removeValue(Object value, String attName) {
		GKInstance instance = (GKInstance)attributePane.getInstance();
		try {
			instance.removeAttributeValueNoCheck(attName, value);
			JTable propTable = attributePane.getPropertyTable();
			PropertyTableModel model = (PropertyTableModel)propTable.getModel();
			int selectedCol = propTable.getSelectedColumn();
			int selectedRow = propTable.getSelectedRow();
			model.refresh();
			if (selectedCol > -1)
				propTable.setColumnSelectionInterval(selectedCol, selectedCol);
			if (selectedRow >= propTable.getRowCount())
				selectedRow = propTable.getRowCount() - 1;
			if (selectedRow > -1)
				propTable.setRowSelectionInterval(selectedRow, selectedRow);
			finishRemoving(value, attName);
		}
		catch (Exception e) {
			System.err.println("AttributePaneController.removeValue(): " + e);
			e.printStackTrace();
		}
	}
	
	private void finishRemoving(Object removed, String attName) {
		//validateActions();
		GKInstance instance = (GKInstance)attributePane.getInstance();
		AttributeEditEvent event = new AttributeEditEvent(this, instance, attName);
		event.setEditingComponent(attributePane);
		boolean isInstance = removed instanceof GKInstance ? true : false;
		if (isInstance) {
			event.setEditingType(AttributeEditEvent.REMOVING);
			java.util.List list = new ArrayList(1);
			// Check for inverseAttributes
			GKInstance inverseInstance = (GKInstance)removed;
			try {
				SchemaAttribute att = instance.getSchemClass().getAttribute(attName);
				if (att.getInverseSchemaAttribute() != null) {
					String inverseAttName = att.getInverseSchemaAttribute().getName();
					if (inverseInstance.getSchemClass().isValidAttribute(inverseAttName)) {
						java.util.List values = inverseInstance.getAttributeValuesList(inverseAttName);
						values.remove(instance);
						if (values.size() == 0) // null it
							inverseInstance.setAttributeValueNoCheck(inverseAttName, null);
						event.setAttributeName(inverseAttName);
						event.setEditingInstance(inverseInstance);
						list.add(instance);
						event.setRemovedInstances(list);
						postProcessAttributeEdit(event);
					}
				}
			}
			catch (Exception e) {
				System.err.println("AttributePane.removeValue(): " + e);
				e.printStackTrace();
			}
			list.clear();
			list.add(removed);
			event.setRemovedInstances(list);
			event.setEditingInstance(instance);
			event.setAttributeName(attName);
		}
		// Should call it at the last step
		postProcessAttributeEdit(event);
	}
	
	protected void removeSelectedCell() {
	    if (!isRemoveSupported())
	        return;
	    JTable propTable = attributePane.getPropertyTable();
	    int[] rows = propTable.getSelectedRows();
	    int col = propTable.getSelectedColumn();
	    PropertyTableModel model = (PropertyTableModel) propTable.getModel();
	    for (int i = 0; i < rows.length; i++) {
	        int row = rows[i]; 
	        row -= i; // row index will decrease one after a deletion
	        // Remember the string name
	        String attName = model.getKeyAt(row);
	        Object removed = model.removeValueAt(row);
	        if (col > -1)
	            propTable.setColumnSelectionInterval(col, col);
	        if (row >= propTable.getRowCount())
	            row = propTable.getRowCount() - 1;
	        if (row > -1)
	            propTable.setRowSelectionInterval(row, row);
	        finishRemoving(removed, attName);
	    }
	}
	
	protected void viewSelectedCell() {
		JTable propTable = attributePane.getPropertyTable();
		GKInstance instance = (GKInstance) attributePane.getInstance();
		boolean isEditable = attributePane.isEditable();
		PropertyTableModel model = (PropertyTableModel) propTable.getModel();
		// Only need to handle a single cell selection.
		int rowCount = propTable.getSelectedRowCount();
		int colCount = propTable.getSelectedColumnCount();
		if (rowCount == 1 && colCount == 1) {
			int row = propTable.getSelectedRow();
			int col = propTable.getSelectedColumn();
			Object obj = model.getValueAt(row, col);
			Component root = SwingUtilities.getRoot(attributePane);
			if (obj instanceof GKInstance) {
                GKInstance instance1 = null;
                if (obj instanceof StoichiometryInstance)
                    instance1 = ((StoichiometryInstance)obj).getInstance();
                else
                    instance1 = (GKInstance)obj;
				if (instance1.isShell()) { 
				    if (root instanceof JDialog)
				        FrameManager.getManager().showShellInstance(instance1, attributePane, (JDialog)root);
				    else
				        FrameManager.getManager().showShellInstance(instance1, attributePane);
				}
				else {
					if (root instanceof JDialog)
						FrameManager.getManager().showInstance(instance1, (JDialog)root, isEditable);
					else
						FrameManager.getManager().showInstance(instance1, isEditable);
				}
			}
			else if (col == 1 && obj != null && 
					 model.getValueType(row) == SchemaAttribute.STRING_TYPE) {
				String title = model.getValueAt(row, 0) + " of " + instance.toString();
				TextDialog dialog = null;
				if (root instanceof JFrame)
					dialog = new TextDialog((JFrame)root, title, !isEditable ? false : model.isCellEditable(row, col));
				else if (root instanceof JDialog)
					dialog = new TextDialog((JDialog)root, title, !isEditable ? false : model.isCellEditable(row, col));
				else
					dialog = new TextDialog(title, !isEditable ? false : model.isCellEditable(row, col));
				String value = obj.toString();
				dialog.setText(value);
				dialog.setVisible(true);
				if (isEditable && dialog.isOKClicked() && dialog.isChanged()) {
					String text = dialog.getText();
					if (propTable.isEditing()) {
						PropertyCellEditor editor = (PropertyCellEditor) propTable.getCellEditor();
						JTextField tf = (JTextField) editor.getCurrentComponent();
						//text = InstanceUtilities.encodeLineSeparators(text);
						tf.setText(text);
					}
					else {
					    model.setValueAt(text, row, col);
					}
				}
			}
		}		
	}
	
	/**
	 * Some post processing actions are needed to make editing correct.
	 * @param e
	 */
	protected void postProcessAttributeEdit(AttributeEditEvent e) {
	    processAttribute(e);
	    propagateAttributeEdit(e);
	    validateDisplayName();
		if (!attributePane.localChangeOnly)
			AttributeEditManager.getManager().attributeEdit(e);
	}
	
	/**
	 * Some changes to an GKInstance object can be propagated to its descendents
	 * or other instances that are related to this GKInstance. For example, a compartment
	 * assignment can be propagated to input or output for a Reaction instance.
	 * @param e
	 */
	private void propagateAttributeEdit(AttributeEditEvent e) {
	    GKInstance instance = (GKInstance) e.getEditingInstance();
	    if (instance == null)
	        return;
	    GKSchemaClass cls = (GKSchemaClass) instance.getSchemClass();
	    String attName = e.getAttributeName();
	    try {
            GKSchemaAttribute att = (GKSchemaAttribute) cls.getAttribute(attName);
            AttributeEditPropagater propagater = new AttributeEditPropagater(this);
            propagater.propagate(att, instance);
        }
        catch (Exception e1) {
            System.err.println("AttributePaneController.propageAttributeEdit(): " + e1);
            e1.printStackTrace();
        }
	}
	
	private void processAttribute(AttributeEditEvent e) {
	    GKInstance instance = (GKInstance) e.getEditingInstance();
	    if (instance == null)
	        return ;
        AttributeAutoFiller filler = AttributeEditConfig.
                                        getConfig().getAttributeAutoFiller(instance.getSchemClass().getName(),
                                                                           e.getAttributeName());
        if (filler == null)
            return; // Do nothing since no filler is used.
        filler.setPersistenceAdaptor(PersistenceManager.getManager().getActiveFileAdaptor());
        filler.setParentComponent(attributePane);
        if (filler.getApprove(attributePane, instance)) {
            try {
                // In case it is a long process. Probably it will not work?
                attributePane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                filler.process(instance, attributePane);
                JTable propTable = attributePane.getPropertyTable();
                PropertyTableModel model = (PropertyTableModel) propTable.getModel();
                model.refresh();
                List autoCreatedInstances = filler.getAutoCreatedInstances();
                if (autoCreatedInstances != null && autoCreatedInstances.size() > 0) {
                    propertySupport.firePropertyChange("autoCreatedInstances", null, autoCreatedInstances);
                }
            }
            catch(Exception exp) {
                System.err.println("AttributePaneController.processAttribute(): "  + exp);
                exp.printStackTrace();
            }
            finally {
                attributePane.setCursor(Cursor.getDefaultCursor());
            }
        }
	}
	
	private void validateDisplayName() {
		GKInstance instance = (GKInstance) attributePane.getInstance();
		boolean displayNameIsChanged = AttributeEditManager.getManager().validateDisplayName(instance);
        if (displayNameIsChanged) 
            attributePane.refresh();
	}	
	
	public void downloadShellInstance(GKInstance shellInstance) {
		// Get the mysqlAdaptor
		MySQLAdaptor adaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(attributePane);
		if (adaptor == null) {
			JOptionPane.showMessageDialog(
				attributePane,
				"Cannot connect to the database",
				"Error in DB Connecting",
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			GKInstance dbInstance = adaptor.fetchInstance(shellInstance.getSchemClass().getName(),
			                                              shellInstance.getDBID());
			if (dbInstance == null) {
				JOptionPane.showMessageDialog(attributePane,
				                              "Cannot find the instance in the database. " +				                              "The instance \nmight be deleted in the database.",
				                              "Error in Downloading",
				                              JOptionPane.ERROR_MESSAGE);
				return;
			}
			SynchronizationManager.getManager().updateFromDB(shellInstance, dbInstance);
			attributePane.setEditable(true); // Remove the lock.
			// Update the view etc.
			AttributeEditManager.getManager().attributeEdit(shellInstance);
            // The above call will add dirty flag to shellInstance. Have to remove it explicity.
			XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
			fileAdaptor.removeDirtyFlag(shellInstance);
		}
		catch(Exception e) {
			System.err.println("AttributePaneController.downloadShellInstance(): " + e);
			e.printStackTrace();
		}
	}	
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertySupport.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertySupport.removePropertyChangeListener(l);
    }
}