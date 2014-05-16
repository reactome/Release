/*
 * Created on Dec 8, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.TextDialog;

/**
 * This customized JPanel is used to compare two GKinstance objects.
 * @author wugm
 */
public class InstanceComparisonPane extends JPanel {
	public static final int DIDNT_SAVE = (-1);
	public static final int SAVE_AS_NEW = 0;
	public static final int OVERWRITE_FIRST = 1;
	public static final int OVERWRITE_SECOND = 2;
	// Option after merging
	private int mergeOption = DIDNT_SAVE; // Nothing
	// Dispaying table
	private AttributeTable table;
	// Buttons for showing
	private JButton showAllBtn = null;
	private JButton showDiffBtn = null;
	protected JButton mergeBtn = null;
	protected JButton saveMergingBtn = null;
	private JPanel pane = null;
	private SaveOptionDialog saveDialog = null;
	// For merging display
	protected AttributePane attPane = null;
	
	private ActionListener saveDialogActionListener = null;

	private boolean closeAfterSaving = false;
	// Fine-tune the appearance of the save dialog
	boolean saveDialogSaveAsNewBtnFirst;
	boolean saveDialogHideUnusedButtons;
	String saveDialogSaveAsNewBtnTitle;
	String saveDialogReplaceFirstBtnTitle;
	String saveDialogReplaceSecondBtnTitle;

	public InstanceComparisonPane() {
		init();
		
		// Get default fine-tuning params for save dialog
		saveDialog = new SaveOptionDialog();
		saveDialogSaveAsNewBtnFirst = saveDialog.isSaveAsNewBtnFirst();
		saveDialogHideUnusedButtons = saveDialog.isHideUnusedButtons();
		saveDialogSaveAsNewBtnTitle = saveDialog.getSaveAsNewBtnTitle();
		saveDialogReplaceFirstBtnTitle = saveDialog.getReplaceFirstBtnTitle();
		saveDialogReplaceSecondBtnTitle = saveDialog.getReplaceSecondBtnTitle();
	}
	
	/**
	 * @param saveDialogHideUnusedButtons The saveDialogHideUnusedButtons to set.
	 */
	public void setSaveDialogHideUnusedButtons(
			boolean saveDialogHideUnusedButtons) {
		this.saveDialogHideUnusedButtons = saveDialogHideUnusedButtons;
	}
	/**
	 * @param saveDialogReplaceFirstBtnTitle The saveDialogReplaceFirstBtnTitle to set.
	 */
	public void setSaveDialogReplaceFirstBtnTitle(
			String saveDialogReplaceFirstBtnTitle) {
		this.saveDialogReplaceFirstBtnTitle = saveDialogReplaceFirstBtnTitle;
	}
	/**
	 * @param saveDialogReplaceSecondBtnTitle The saveDialogReplaceSecondBtnTitle to set.
	 */
	public void setSaveDialogReplaceSecondBtnTitle(
			String saveDialogReplaceSecondBtnTitle) {
		this.saveDialogReplaceSecondBtnTitle = saveDialogReplaceSecondBtnTitle;
	}
	/**
	 * @param saveDialogSaveAsNewBtnFirst The saveDialogSaveAsNewBtnFirst to set.
	 */
	public void setSaveDialogSaveAsNewBtnFirst(
			boolean saveDialogSaveAsNewBtnFirst) {
		this.saveDialogSaveAsNewBtnFirst = saveDialogSaveAsNewBtnFirst;
	}
	/**
	 * @param saveDialogSaveAsNewBtnTitle The saveDialogSaveAsNewBtnTitle to set.
	 */
	public void setSaveDialogSaveAsNewBtnTitle(
			String saveDialogSaveAsNewBtnTitle) {
		this.saveDialogSaveAsNewBtnTitle = saveDialogSaveAsNewBtnTitle;
	}
	/**
	 * @param closeAfterSaving The closeAfterSaving to set.
	 */
	public void setCloseAfterSaving(boolean closeAfterSaving) {
		this.closeAfterSaving = closeAfterSaving;
	}

	public InstanceComparisonPane(GKInstance instance1, GKInstance instance2) {
		this();
		setInstances(instance1, instance2);
	}
	
	public void setInstances(GKInstance instance1, GKInstance instance2) {
		ComparisonTableModel model = (ComparisonTableModel) table.getModel();
		model.setInstances(instance1, instance2);
		// Just enable diff btn
		showDiffBtn.setEnabled(true);
		showAllBtn.setEnabled(false);
		saveMergingBtn.setVisible(false);
	}
	
	/**
	 * An overloaded method to set the column titles.
	 */
	public void setInstances(GKInstance instance1, GKInstance instance2,
	                         String header1, String header2) {
		ComparisonTableModel model = (ComparisonTableModel)table.getModel();
		model.setInstances(instance1, instance2, header1, header2);
		// Just enable diff btn
		showDiffBtn.setEnabled(true);
		showAllBtn.setEnabled(false);
		saveMergingBtn.setVisible(false);
	}
	
	public GKInstance getFirstInstance() {
	    ComparisonTableModel model = (ComparisonTableModel) table.getModel();
	    return model.instance1;
	}
	
	public GKInstance getSecondInstance() {
	    ComparisonTableModel model = (ComparisonTableModel) table.getModel();
	    return model.instance2;
	}
	
	private void init() {
		setLayout(new BorderLayout());
		table = new AttributeTable();
		ComparisonTableModel model = new ComparisonTableModel();
         table.setModel(model);
		table.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					viewSelectedCell();
				}
			}
		});
		JPanel centerPane = new JPanel();
		centerPane.setBorder(BorderFactory.createRaisedBevelBorder());
		centerPane.setLayout(new BorderLayout());
		pane = new JPanel();
		pane.setLayout(new BorderLayout());
		pane.add(new JScrollPane(table));
		centerPane.add(pane, BorderLayout.CENTER);
		// Some Buttons
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doBtnAction(e);
			}
		};
		showAllBtn = new JButton("Show All Attributes");
		showAllBtn.setActionCommand("showAll");
		showAllBtn.addActionListener(l);
		showDiffBtn = new JButton("Show Diff Attributes");
		showDiffBtn.setActionCommand("showDiff");
		showDiffBtn.addActionListener(l);
		mergeBtn = new JButton("Merge");
		mergeBtn.setActionCommand("merge");
		mergeBtn.addActionListener(l);
		saveMergingBtn = new JButton("Save Merged");
		saveMergingBtn.setActionCommand("save");
		saveMergingBtn.addActionListener(l);
		JPanel btnPane = new JPanel();
		btnPane.add(showAllBtn);
		btnPane.add(showDiffBtn);
		btnPane.add(mergeBtn);
		btnPane.add(saveMergingBtn);
		centerPane.add(btnPane, BorderLayout.SOUTH);
		add(centerPane, BorderLayout.CENTER);
		// Control Panel
		JPanel southPane = createControlPane();
		add(southPane, BorderLayout.SOUTH);
	}
	
	protected JPanel createControlPane() {
		JPanel southPane = new JPanel();
		southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton closeBtn = new JButton("Close");
		closeBtn.setMnemonic('C');
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		southPane.add(closeBtn);	
		return southPane;	
	}
	
	public void doBtnAction(ActionEvent e) {
		String cmd = e.getActionCommand();
		ComparisonTableModel model = (ComparisonTableModel) table.getModel();
		if (cmd.equals("showAll")) {
			model.showAllAttributes();
			showAllBtn.setEnabled(false);
			showDiffBtn.setEnabled(true);
		}
		else if (cmd.equals("showDiff")) {
			model.showDiffAttributes();
			showDiffBtn.setEnabled(false);
			showAllBtn.setEnabled(true);
		}
		else if (cmd.equals("merge")) {
			String text = mergeBtn.getText();
			if (text.equals("Merge"))
				merge();
			else
				closeMerging();
		}
		else if (cmd.equals("save")) {
			saveMerging();
		}
	}
	
	public void hideMergeBtn() {
		mergeBtn.setVisible(false);
	}
	
	private void closeMerging() {
		mergeBtn.setText("Merge");
		saveMergingBtn.setVisible(false);
		pane.removeAll();
		pane.add(new JScrollPane(table), BorderLayout.CENTER);
		pane.invalidate();
		pane.getParent().validate();
		attPane = null; // Mark for gc
	}
	
	/**
	 * Check if two GKInstances can be merged.
	 * @param instance1
	 * @param instance2
	 * @return true for mergable.
	 */
	public boolean isMergable(GKInstance instance1, GKInstance instance2) {
	    // Only two instances from the same model can be merged
	    if (!instance1.getSchemClass().getName().equals(instance2.getSchemClass().getName())) {
		    JOptionPane.showMessageDialog(this,
		                                  "These two instances are not from the same schema class, and cannot be merged.",
		                                  "Error in Merging",
		                                  JOptionPane.ERROR_MESSAGE);
		    return false;
		}
		// Only non-shell instances can be merged
	    if (instance1.isShell() || instance2.isShell()) {
	        boolean isBoth = (instance1.isShell() && instance2.isShell()) ? true : false;
	        String msg = null;
	        if (isBoth)
	            msg = "Both instances are shell instances. Shell instances cannot be merged. Please download them first.";
	        else
	            msg = "One of merging instances is a shell instance. A shell instance cannot be merged. Please download it first.";
	        JOptionPane.showMessageDialog(this,
	                                      msg,
	                                      "Error in Merging",
	                                      JOptionPane.ERROR_MESSAGE);
	        return false;
	    }
	    // Check if any shell instances are used in inversiable slots. If a shell instance is used in
	    // inversiable slot, merging cannot occur since a shell instance might be changed. However,
	    // a shell instance is not editable.
	    // Take the local instance since only local instance can be changed.
	    GKSchemaClass schemaCls = null;
	    if (instance1.getDbAdaptor() instanceof XMLFileAdaptor)
	        schemaCls = (GKSchemaClass) instance1.getSchemClass();
	    else
	        schemaCls = (GKSchemaClass) instance2.getSchemClass();
	    GKSchemaAttribute att = null;
	    Set shellInstances = new HashSet();
	    try {
            grebShellInstances(instance1, schemaCls, shellInstances);
            grebShellInstances(instance2, schemaCls, shellInstances);
        }
        catch (Exception e) {
            System.err.println("InstanceComparisonPane.isMergable(): " + e);
            e.printStackTrace();
        }
        if (shellInstances.size() > 0) {
            if (!checkOutShellInstances(shellInstances))
                return false;
        }
	    return true;
	}
	
	private boolean checkOutShellInstances(Set shellInstances) {
        // Get the list of DB instances
        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(this);
        String msg = null;
        if (dba == null) {
            msg = "Shell instances are used in inversible slots. They might need to be modified and\n" +
            	  "should be checkout first. However, a database connection cannot be established.";
            JOptionPane.showMessageDialog(this,
                                          msg,
                                          "Error in Database Connection",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
                                          
        }
	    if (shellInstances.size() == 1) {
            msg = "A shell instance is used in an inversible slot. It might need to be modified. " + 
                  "Please check it out first before merging by clicking OK button.";
        }
        else
            msg = "Shell instances are used in inversible slots. They might need to be modified. " +
                  "Please check them out first before merging by clicking OK button.";
        Window window = (Window) SwingUtilities.getRoot(this);
        InstanceListDialog dialog = null;
        if (window instanceof JDialog) {
            dialog = new InstanceListDialog((JDialog)window, "Check Out Shell Instances?", true);
        }
        else if (window instanceof JFrame) {
            dialog = new InstanceListDialog((JFrame)window, "Check Out Shell Instances?", true);
        }
        else
            dialog = new InstanceListDialog("Check Out Shell Instances", true);
        dialog.setSubTitle(msg);
        // Need to sort
        List instances = new ArrayList(shellInstances.size());
        GKInstance dbInstance = null;
        GKInstance shellInstance = null;
        try {
            for (Iterator it = shellInstances.iterator(); it.hasNext();) {
                shellInstance = (GKInstance) it.next();
                dbInstance = dba.fetchInstance(shellInstance.getSchemClass().getName(),
                                               shellInstance.getDBID());
                if (dbInstance == null) {
                    JOptionPane.showMessageDialog(this,
                                                  "Instance \"" + shellInstance + "\" has been deleted in the database.\n" +
                                                  "Please delete it in the local project before merging.",
                                                  "Error in Merging",
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                instances.add(dbInstance);
            }
        }
        catch(Exception e) {
            System.err.println("InstanceComparisonPane.checkOutShellInstances() 1: " + e);
            e.printStackTrace();
        }
		InstanceUtilities.sortInstances(instances);
		dialog.setDisplayedInstances(instances);
		dialog.setSize(600, 600);
		dialog.setLocationRelativeTo(this);
		dialog.setModal(true);
		dialog.setVisible(true);
		if (dialog.isOKClicked()) {
		    // Check out
		    try {
                SynchronizationManager.getManager().checkOut(instances, window);
                return true;
            }
            catch (Exception e) {
                System.err.println("InstanceComparisonPane.checkOutShellInstances(): " + e);
                e.printStackTrace();
                return false;
            }
		}
		return false;
	}
	
	private void grebShellInstances(GKInstance localInstance,
	                                 GKSchemaClass schemaCls,
	                                 Set shellInstances) throws Exception {
        if (localInstance.getDbAdaptor() instanceof XMLFileAdaptor) {
            GKSchemaAttribute att = null;
            for (Iterator it = schemaCls.getAttributes().iterator(); it.hasNext();) {
                att = (GKSchemaAttribute) it.next();
                if (att.getInverseSchemaAttribute() == null)
                    continue;
                List values = localInstance.getAttributeValuesList(att.getName());
                if (values == null || values.size() == 0)
                    continue;
                for (Iterator it1 = values.iterator(); it1.hasNext();) {
                    GKInstance instance = (GKInstance) it1.next();
                    if (instance.isShell())
                        shellInstances.add(instance);
                }
            }
        }
	}
	
	/**
	 * Make it protected for subclassing.
	 */
	protected void merge() {
	    // Have to make sure two instances are from the same type
		ComparisonTableModel model = (ComparisonTableModel) table.getModel();
		if (!isMergable(model.instance1, model.instance2))
		    return;
		mergeBtn.setText("Close Merging");
		saveMergingBtn.setVisible(true);
		// Add an editable Attribute under the table for merged Instance
		attPane = new AttributePane();
		attPane.setEditable(true);
		attPane.setLocalChangeOnly(true);
		// Have to generate a temp new GKInstance
		GKInstance mergedInstance = new GKInstance();
		mergedInstance.setIsInflated(true);
		XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		mergedInstance.setDbAdaptor(adaptor);
		mergedInstance.setSchemaClass(adaptor.getSchema().getClassByName(model.instance1.getSchemClass().getName()));
		merge(mergedInstance);
		attPane.setInstance(mergedInstance);
		// Reset the title
		attPane.setTitle("Merged Instance");
		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
		                                new JScrollPane(table),
		                                attPane);
		if (pane.getHeight() > 50)
			jsp.setDividerLocation(pane.getHeight() / 2);
		else
			jsp.setDividerLocation(250);
		jsp.setResizeWeight(0.5);
		pane.removeAll();
		pane.add(jsp, BorderLayout.CENTER);
		pane.invalidate();
		pane.getParent().validate();                                
	}
	
	/**
	 * Refactored method for subclassing.
	 * @param merged
	 * @param adaptor
	 */
	protected void replaceFirstInstance(GKInstance merged) {
		ComparisonTableModel model = (ComparisonTableModel) table.getModel();
		GKInstance instance1 = model.instance1;
		overwriteInstance(instance1, merged);
	}
	
	protected void replaceSecondInstance(GKInstance merged) {
		ComparisonTableModel model = (ComparisonTableModel) table.getModel();
		GKInstance instance2 = model.instance2;
		overwriteInstance(instance2, merged);
	}
	
	
	/**
	 * A helper to replace a target Instance with the source Instance.
	 * @param target
	 * @param source
	 */
	private void overwriteInstance(GKInstance target, GKInstance source) {
		ComparisonTableModel model = (ComparisonTableModel) table.getModel();
		XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		// To fire attribute edit event
		AttributeEditEvent editEvent = new AttributeEditEvent(this);
		java.util.List list = new ArrayList(1);
		//java.util.List uneditbleAtts = AttributePane.getUneditableAttNames();
		// Copy all attributes from source to target
		for (Iterator it = target.getSchemClass().getAttributes().iterator(); it.hasNext();) {
			GKSchemaAttribute att = (GKSchemaAttribute) it.next();
			// Have to check reverse attribute
			GKSchemaAttribute inverseAtt = (GKSchemaAttribute) att.getInverseSchemaAttribute();
			String attName = att.getName();
			// Have to keep these original values
			//if (uneditbleAtts.contains(attName))
			//	continue;
			try {
				java.util.List values = source.getAttributeValuesList(attName);
				java.util.List oldValues = null;
				if (inverseAtt != null) { // Check inverse attribute
					                      // Have to use attribute name?
					oldValues = target.getAttributeValuesList(attName);
				}
				// Copy values to target
				target.setAttributeValueNoCheck(attName, values);
				if (oldValues != null && oldValues.size() > 0) {
					for (Iterator it1 = oldValues.iterator(); it1.hasNext();) {
						GKInstance reference = (GKInstance)it1.next();
						if (values.contains(reference))
						    continue; // Don't need to change
						reference.removeAttributeValueNoCheck(inverseAtt.getName(), target);
						adaptor.markAsDirty(reference);
						editEvent.setEditingInstance(reference);
						editEvent.setAttributeName(inverseAtt.getName());
						list.clear();
						list.add(target);
						editEvent.setRemovedInstances(list);
						AttributeEditManager.getManager().attributeEdit(editEvent);
					}
				}
				// Update new inverse attribute
				if (inverseAtt != null && values != null && values.size() > 0) {
				    boolean needFireEvent = false;
					for (Iterator it1 = values.iterator(); it1.hasNext();) {
						GKInstance reference = (GKInstance) it1.next();
						// Need to remove the source from the reference value list.
						// Need to use name. It seems that there is a bug in Schema. So using name is always more safer.
						java.util.List reverseValues = reference.getAttributeValuesListNoCheck(inverseAtt.getName());
						needFireEvent= false;
						if (reverseValues == null || reverseValues.size() == 0) {
							reference.addAttributeValueNoCheck(inverseAtt.getName(), target);
							needFireEvent = true;
						}
						else {
							int index = reverseValues.indexOf(source);
							if (index >= 0) {// Should occur only once
								reverseValues.set(index, target);
								needFireEvent = true;
							}
							else if (!reverseValues.contains(target)) {
								reverseValues.add(target);
								needFireEvent = true;
							}
						}
						if (needFireEvent) {
                            adaptor.markAsDirty(reference);
                            editEvent.setEditingInstance(reference);
                            editEvent.setAttributeName(inverseAtt.getName());
                            list.clear();
                            list.add(target);
                            editEvent.setAddedInstances(list);
                            AttributeEditManager.getManager().attributeEdit(editEvent);
                        }
					}
				}
			}
			catch(Exception e) {
				System.err.println("InstanceComparisonPane.saveMerging(): " + e);
				e.printStackTrace();
			}
		}
		InstanceDisplayNameGenerator.setDisplayName(target);
		// For saving
		adaptor.markAsDirty(target);
		// Have to update the display
		if (showDiffBtn.isEnabled())
			model.showAllAttributes();
		else
			model.showDiffAttributes();
		// Fire the attribute edit event
		AttributeEditManager.getManager().attributeEdit(target);		
	}
	
	protected void saveMerging() {
		// Ask if the user wants to replace the compared GKInstance or want to
		// save as new Instance
		JDialog parentDialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, this);
		ComparisonTableModel model = (ComparisonTableModel) table.getModel();
		boolean firstFromDB = model.instance1.getDbAdaptor() instanceof MySQLAdaptor ? true : false;
		boolean secondFromDB = model.instance2.getDbAdaptor() instanceof MySQLAdaptor ? true : false;
		SaveOptionDialog saveDialog = new SaveOptionDialog(parentDialog, 
                                                           "Saving Option", 
                                                           firstFromDB, 
                                                           secondFromDB,
														   false);
		// Set up fine-tuning
		saveDialog.setSaveAsNewBtnFirst(saveDialogSaveAsNewBtnFirst);
		saveDialog.setHideUnusedButtons(saveDialogHideUnusedButtons);
		saveDialog.setSaveAsNewBtnTitle(saveDialogSaveAsNewBtnTitle);
		saveDialog.setReplaceFirstBtnTitle(saveDialogReplaceFirstBtnTitle);
		saveDialog.setReplaceSecondBtnTitle(saveDialogReplaceSecondBtnTitle);
		saveDialog.init();
		
		saveDialog.setModal(true);
		saveDialog.setVisible(true);	
		if (saveDialog.isOKClicked) {
			mergeOption = saveDialog.getOption();
			GKInstance merged = (GKInstance) attPane.getInstance();
			XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
			switch(mergeOption) {
				case SAVE_AS_NEW :
					// Have to specify a new DB_ID
					Long dbID = adaptor.getNextLocalID();
					merged.setDBID(dbID);
					merged.setIsDirty(true); // A new local instance
					adaptor.addNewInstance(merged);
					break;
				case OVERWRITE_FIRST :
					overwriteInstance(model.instance1, merged);
					break;
				case OVERWRITE_SECOND :
					overwriteInstance(model.instance2, merged);
					break;
			}
			
			// Close comparison pane once instance has been saved
			if (closeAfterSaving)
				dispose();
		}
	}
	
	public GKInstance getMerged() {
		return (GKInstance) attPane.getInstance();
	}
	
	/**
	 * Get the option in save dialog after merging is done.
	 * @return one of these values: SAVE_AS_NEW, OVERWRITE_FIRST, OVERWRITE_SECOND.
	 */
	public int getSaveMergeOption() {
	    return mergeOption;
	}
	
	/**
	 * Merge two Instance Objects into one specified mergedInstance. Here is the rule for 
	 * merging: If two instances have the same attribute values, just copy; otherwise if the
	 * attribute is multiple, merge two attribute values, otherwise, leave the attribute empty.
	 * @param mergedInstance
	 */
	private void merge(GKInstance mergedInstance) {
		ComparisonTableModel model = (ComparisonTableModel) table.getModel();
		GKInstance instance1 = model.instance1;
		GKInstance instance2 = model.instance2;
		GKSchemaClass schemaClass = (GKSchemaClass) mergedInstance.getSchemClass();
		try {
			for (Iterator it = schemaClass.getAttributes().iterator(); it.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute)it.next();
				java.util.List mergedValue = null;
				java.util.List list1 = instance1.getAttributeValuesList(att.getName());
				java.util.List list2 = instance2.getAttributeValuesList(att.getName());
				if (list1 == null)
					list1 = new ArrayList();
				if (list2 == null)
					list2 = new ArrayList();
				if (list1.size() == 0 && list2.size() > 0) {
					if (att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE)
						mergedValue = convertToLocalCopy(list2);
					else
						mergedValue = new ArrayList(list2);
				}
				else if (list1.size() > 0 && list2.size() == 0) {
					if (att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE)
						mergedValue = convertToLocalCopy(list1);
					else
						mergedValue = new ArrayList(list1);
				}
				else if (list1.size() > 0 && list2.size() > 0) {
					if (att.isMultiple()) {
						if (att.isInstanceTypeAttribute())
							mergedValue = convertToLocalCopy(list1);
						else
							mergedValue = new ArrayList(list1);
						// Go through the second list
						boolean found = false;
						for (Iterator it1 = list2.iterator(); it1.hasNext();) {
							Object obj = it1.next();
							if (obj instanceof GKInstance) {
								GKInstance tmp = (GKInstance) obj;
								found = false;
								for (Iterator it2 = mergedValue.iterator(); it2.hasNext();) {
									GKInstance tmp1 = (GKInstance) it2.next();
									if (tmp1.getDBID() != null &&
									    tmp1.getDBID().equals(tmp.getDBID())) {
										found = true;
										break;
									}
								}
								if (!found) {
									GKInstance localCopy = PersistenceManager.getManager().getLocalReference(tmp);
									if (localCopy != null)
										mergedValue.add(localCopy);                                                
								}
							}
							else if (!mergedValue.contains(obj)) {
								mergedValue.add(obj);
							}
						}
					}
					else {
						if (att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE) {
							GKInstance tmp1 = (GKInstance) list1.get(0);
							GKInstance tmp2 = (GKInstance) list2.get(0);
							if (tmp1.getDBID().equals(tmp2.getDBID())) {
								GKInstance localCopy = PersistenceManager.getManager().getLocalReference(tmp1);
								if (localCopy != null) {
									mergedValue = new ArrayList(1);
									mergedValue.add(localCopy);
								}                                 
							}						
						}
						else if (list1.equals(list2)) {
							mergedValue = new ArrayList(list1);
						}
					}
				}
				handleCreatedForMerging(mergedInstance, instance1, instance2);
				mergedInstance.setAttributeValueNoCheck(att, mergedValue);
			}
			InstanceDisplayNameGenerator.setDisplayName(mergedInstance);
		}
		catch (Exception e) {
			System.err.println("InstanceComparisonPane.merge(): " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * A helper method to handle created slot.
	 * @param mergedInst
	 * @param inst1
	 * @param inst2
	 * @throws Exception
	 */
	private void handleCreatedForMerging(GKInstance mergedInst,
	                                     GKInstance inst1,
	                                     GKInstance inst2) throws Exception {
	    GKInstance created = (GKInstance) mergedInst.getAttributeValue(ReactomeJavaConstants.created);
	    if (created != null)
	        return; // This should be handled already
	    GKInstance created1 = (GKInstance) inst1.getAttributeValue(ReactomeJavaConstants.created);
	    if (created1 == null)
	        return;
	    created1 = PersistenceManager.getManager().getLocalReference(created1);
	    GKInstance created2 = (GKInstance) inst2.getAttributeValue(ReactomeJavaConstants.created);
	    if (created2 == null)
	        return;
	    created2 = PersistenceManager.getManager().getLocalReference(created2);
	    if (created1 == null || created2 == null)
	        return;
	    // Use the instance with smaller DB_ID as the created value for the mergedInst
	    GKInstance createdForMerge = null;
	    GKInstance modifiedForMerge = null;
	    if (created1.getDBID() < created2.getDBID()) {
	        createdForMerge = created1;
	        modifiedForMerge = created2;
	    }
	    else {
	        createdForMerge = created2;
	        modifiedForMerge = created1;
	    }
	    mergedInst.setAttributeValue(ReactomeJavaConstants.created, createdForMerge);
	    // Add another one to the list of modified slot.
	    List<?> list = mergedInst.getAttributeValuesList(ReactomeJavaConstants.modified);
	    if (list == null || list.size() == 0) 
	        mergedInst.addAttributeValue(ReactomeJavaConstants.modified, modifiedForMerge);
	    else {
	        List<GKInstance> instList = new ArrayList<GKInstance>();
	        for (Iterator<?> it = list.iterator(); it.hasNext();) {
	            GKInstance inst = (GKInstance) it.next();
	            instList.add(inst);
	        }
	        // Sort the list based on DB_IDs
	        Collections.sort(instList, new Comparator<GKInstance>() {
	           public int compare(GKInstance inst1, GKInstance inst2) {
	               return inst1.getDBID().compareTo(inst2.getDBID());
	           }
	        });
	        // Find a position to insert modifiedForMerge
	        int index = 0;
	        for (int i = 0; i < instList.size(); i++) {
	            GKInstance tmp = instList.get(i);
	            if (tmp.getDBID() > modifiedForMerge.getDBID()) {
	                index = i;
	                break;
	            }
	        }
	        instList.add(index, modifiedForMerge);;
	        mergedInst.setAttributeValue(ReactomeJavaConstants.modified, instList);
	    }
	}
	
	private java.util.List convertToLocalCopy(java.util.List list) {
		java.util.List rtnList = new ArrayList(list.size());
		GKInstance instance = null;
		GKInstance localCopy = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
			instance = (GKInstance) it.next();
			localCopy = PersistenceManager.getManager().getLocalReference(instance);
			if (localCopy != null)
				rtnList.add(localCopy);
		}
		return rtnList;
	}
	
	protected void dispose() {
		Component root = SwingUtilities.getRoot(this);
		if (root instanceof JDialog)
			((JDialog)root).dispose();
		else if (root instanceof JFrame)
			((JFrame)root).dispose();
	}
	
	protected void viewSelectedCell() {
		ComparisonTableModel model = (ComparisonTableModel)table.getModel();
		// Only need to handle a single cell selection.
		int rowCount = table.getSelectedRowCount();
		int colCount = table.getSelectedColumnCount();
		if (rowCount == 1 && colCount == 1) {
			int row = table.getSelectedRow();
			int col = table.getSelectedColumn();
			Object obj = model.getValueAt(row, col);
			if (obj instanceof GKInstance) {
				GKInstance instance = (GKInstance)obj;
				Component root = SwingUtilities.getRoot(this);
				if (instance.isShell()) {
				    if (root instanceof JDialog)
				        FrameManager.getManager().showShellInstance(instance, this, (JDialog)root);
				    else
				        FrameManager.getManager().showShellInstance(instance, this);
				}
				else {
                    if (root instanceof JDialog)
                        FrameManager.getManager().showInstance(instance, (JDialog) root, false);
                    else
                        FrameManager.getManager().showInstance(instance, false);
                }
			}
			else if (col != 0 && obj != null && model.getValueType(row) == SchemaAttribute.STRING_TYPE) {
				GKInstance instance = null;
				if (col == 1)
					instance = model.instance1;
				else
					instance = model.instance2;
				String title = model.getValueAt(row, 0) + " of " + instance.toString();
				Component root = SwingUtilities.getRoot(this);
				TextDialog dialog;
				if (root instanceof JFrame)
				    dialog = new TextDialog((JFrame)root, title, false);
				else if (root instanceof JDialog)
				    dialog = new TextDialog((JDialog)root, title, false);
				else 
				    dialog = new TextDialog(title, false);
				dialog.setText(obj.toString());
				dialog.setVisible(true);
			}
		}
	}
	
	public SaveOptionDialog getSaveDialog() {
		return saveDialog;
	}
	
	public void setSaveDialogActionListener(ActionListener a) {
		saveDialogActionListener = a;
	}
	
	class ComparisonTableModel extends AttributeTableModel {
	    final String clsKey = "SchemaClass";
		private String[] headers = new String[] {"Property Name", "Instance1", "Instance2"};
		private Map valueMap1 = new HashMap();
		private Map valueMap2 = new HashMap();
		private java.util.List keys = new ArrayList();
		private Object[][] values;
		private GKInstance instance1;
		private GKInstance instance2;
		// Cell span
		private CellSpan cellSpan;

		public ComparisonTableModel() {
			cellSpan = new CellSpan();
			values = new Object[1][1];
		}
		
		public CellSpan getCellSpan() {
			return cellSpan;
		}

		public boolean isCellEditable(int row, int col) {
			return false;
		}
		
		public void setInstances(GKInstance instance1, GKInstance instance2) {
			this.instance1 = instance1;
			this.instance2 = instance2;
			if (instance1.getDbAdaptor() == instance2.getDbAdaptor()) {
				headers[1] = instance1.toString();
				headers[2] = instance2.toString();
			}
			else {
				if (instance1.getDbAdaptor() instanceof XMLFileAdaptor)
					headers[1] = "Local Copy";
				else if (instance1.getDbAdaptor() instanceof MySQLAdaptor)
					headers[1] = "DB Copy";
				if (instance2.getDbAdaptor() instanceof XMLFileAdaptor)
					headers[2] = "Local Copy";
				else if (instance2.getDbAdaptor() instanceof MySQLAdaptor)	
					headers[2] = "DB Copy";
			}
			refresh();
		}
		
		public GKInstance getInstance1() {
			return instance1;
		}
		
		/**
		 * An overloaded method to set the titles for the columns.
		 * @param instance1
		 * @param instance2
		 * @param header1
		 * @param header2
		 */
		public void setInstances(GKInstance instance1, GKInstance instance2,
		                        String header1, String header2) {
			this.instance1 = instance1;
			this.instance2 = instance2;
			headers[1] = header1;
			headers[2] = header2;
			refresh();
		}
		
		public void refresh() {
			valueMap1.clear();
			valueMap2.clear();
			try {
			    // Two instances might be not the same typ
			    keys = getKeys();
			    GKSchemaClass cls1 = (GKSchemaClass) instance1.getSchemClass();
			    GKSchemaClass cls2 = (GKSchemaClass) instance2.getSchemClass();
				for (Iterator it = keys.iterator(); it.hasNext();) {
					String attName = (String)it.next();
					if (cls1.isValidAttribute(attName))
					    valueMap1.put(attName, instance1.getAttributeValuesList(attName));
					else
					    valueMap1.put(attName, null);
					if (cls2.isValidAttribute(attName))
					    valueMap2.put(attName, instance2.getAttributeValuesList(attName));
					else
					    valueMap2.put(attName, null);
				}
                // Sort
				Collections.sort(keys);
				// Add class name too at the top of the list
				keys.add(0, clsKey);
				valueMap1.put(clsKey, cls1.getName());
				valueMap2.put(clsKey, cls2.getName());
				setValues(valueMap1, valueMap2, keys);
				cellSpan.initSpans(valueMap1, valueMap2, keys);
				//fireTableDataChanged();
				fireTableStructureChanged();
			}
			catch(Exception e) {
				System.err.println("AttributePane.PropertyPane.refresh(): " + e);
				e.printStackTrace();
			}
		}
		
		protected void showAllAttributes() {
			refresh();
		}
		
		private java.util.List getKeys() {
		    // Two instances might be not the same typ
		    GKSchemaClass cls1 = (GKSchemaClass) instance1.getSchemClass();
		    GKSchemaClass cls2 = (GKSchemaClass) instance2.getSchemClass();
		    Set attNames = new HashSet();
		    for (Iterator it = cls1.getAttributes().iterator(); it.hasNext();) {
		        GKSchemaAttribute att = (GKSchemaAttribute) it.next();
		        attNames.add(att.getName());
		    }
		    for (Iterator it = cls2.getAttributes().iterator(); it.hasNext();) {
		        GKSchemaAttribute att = (GKSchemaAttribute) it.next();
		        attNames.add(att.getName());
		    }
			return new ArrayList(attNames);
		}
		
		protected void showDiffAttributes() {
			// Get the list of all attributes
			valueMap1.clear();
			valueMap2.clear();
			try {
				// Two instances might be not the same
				java.util.List attNames = getKeys();
				GKSchemaClass cls1 = (GKSchemaClass) instance1.getSchemClass();
				GKSchemaClass cls2 = (GKSchemaClass) instance2.getSchemClass();
				if (keys != null)
					keys.clear();
				else
					keys = new ArrayList();
				SchemaAttribute att = null;
				for (Iterator it = attNames.iterator(); it.hasNext();) {
					String attName = (String) it.next();
					// This shouldn't be here since cls1 doesn't necessarily have this attribute
					// att = cls1.getAttribute(attName);
					Object value1 = null;
					if (cls1.isValidAttribute(attName)) {
						value1 = instance1.getAttributeValuesList(attName);
						att = cls1.getAttribute(attName);
					} else
						value1 = null;
					Object value2 = null;
					if (cls2.isValidAttribute(attName)) {
						value2 = instance2.getAttributeValuesList(attName);
						if (att == null) {
							att = cls2.getAttribute(attName);
						}
					} else
						value2 = null;
					if (!InstanceUtilities.compareAttValues(value1, value2, att)) {
						keys.add(attName);
						valueMap1.put(attName, value1);
						valueMap2.put(attName, value2);
					}
				}
				// Have to check schema class name
				Collections.sort(keys);
				if (!cls1.getName().equals(cls2.getName())) {
					keys.add(0, clsKey);
					valueMap1.put(clsKey, cls1.getName());
					valueMap2.put(clsKey, cls2.getName());
				}
				setValues(valueMap1, valueMap2, keys);
				cellSpan.initSpans(valueMap1, valueMap2, keys);
				fireTableDataChanged();
			} catch (Exception e) {
				System.err.println("AttributePane.PropertyPane.refresh(): " + e);
				e.printStackTrace();
			}
		}
		
		private void setValues(Map valueMap1, Map valueMap2, java.util.List keys) {
			int col = 3;
			int row = 0;
			// Calculate the total row number
			for (Iterator it = valueMap1.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				Object value1 = valueMap1.get(key);
				Object value2 = valueMap2.get(key);
				int size = 1;
				if (value1 instanceof Collection) {
					Collection c = (Collection) value1;
					if (c.size() > size)
						size = c.size();
				}
				if (value2 instanceof Collection) {
					Collection c = (Collection) value2;
					if (c.size() > size)
						size = c.size();
				}
				row += size;
			}
			values = new Object[row][col];
			// Set the object values
			row = 0;
			for (Iterator it = keys.iterator(); it.hasNext();) {
				Object key = it.next();
				values[row][0] = key;
				int size = 1;
				// Assign the values for the first Instance
				Object value = valueMap1.get(key);
				if (value instanceof Collection) {
					Collection c = (Collection)value;
					int i = 0;
					for (Iterator it1 = c.iterator(); it1.hasNext();) {
						values[row + i][1] = it1.next();
						i++;
					}
					if (c.size() > size)
						size = c.size();
				}
				else {
					values[row][1] = value;
				}
				// Assign the values for the second Instance
				value = valueMap2.get(key);
				if (value instanceof Collection) {
					Collection c = (Collection)value;
					int i = 0;
					for (Iterator it1 = c.iterator(); it1.hasNext();) {
						values[row + i][2] = it1.next();
						i++;
					}
					if (c.size() > size)
						size = c.size();
				}
				else {
					values[row][2] = value;
				}				
				row += size;
			}
		}		
		
		public int getRowCount() {
			return values.length;
		}

		public int getColumnCount() {
			return 3;
		}

		public String getColumnName(int col) {
			return headers[col];
		}

		public Object getValueAt(int row, int col) {
			if (cellSpan.isVisible(row, col)) {
				return values[row][col];
			}
			return null;
		}
		
		public int getValueType(int row) {
			// Find the key
			String key = null;
			for (int i = row; i >= 0; i --) {
				if (values[i][0] != null) {
					key = values[i][0].toString();
					break;
				}
			}
			GKSchemaClass cls = (GKSchemaClass) instance1.getSchemClass();
			try {
				return cls.getAttribute(key).getTypeAsInt();
			}
			catch(Exception e) {
				System.err.println("InstanceComparisonPane.getValueType(): " + e);
				e.printStackTrace();
			}
			return SchemaAttribute.STRING_TYPE;
		}
		
		public SchemaClass getSchemaClass() {
            if (instance1 == null || instance2 == null)
                return null;
            // Find the common SchemaClass
            List list1 = instance1.getSchemClass().getOrderedAncestors();
            list1 = new ArrayList(list1); // defensive programming: in case list1 is cached by SchemaClass
            list1.add(instance1.getSchemClass());
            List list2 = instance2.getSchemClass().getOrderedAncestors();
            list2 = new ArrayList(list2);
            list2.add(instance2.getSchemClass());
            SchemaClass cls1;
            for (int i = list1.size() - 1; i >= 0; i--) {
                cls1 = (SchemaClass) list1.get(i);
                if (list2.contains(cls1))
                    return cls1;
            }
            return null;
        }
	}
	
	/**
	 * A customized JDialog for the option of saving.
	 */
	class SaveOptionDialog extends JDialog {
		// Options
		private JRadioButton saveAsNewBtn; 
		private JRadioButton replaceFirstBtn;
		private JRadioButton replaceSecondBtn;
		// Close dialog
		private JButton okBtn;
		
		boolean firstIsFromDB;
        boolean secondIsFromDB;
		// Fine-tune the appearance of this dialog
		boolean saveAsNewBtnFirst = true;
		boolean hideUnusedButtons = false;
		String saveAsNewBtnTitle = "Save as New Instance";
		String replaceFirstBtnTitle = "Overwrite First Instance";
		String replaceSecondBtnTitle = "Overwrite Second Instance";

		
		private boolean isOKClicked = false;
		
		SaveOptionDialog() {
		}
		
		SaveOptionDialog(JDialog parentDialog, 
		                 String title, 
		                 boolean firstIsFromDB,
		                 boolean secondIsFromDB) {
			this(parentDialog, title, firstIsFromDB, secondIsFromDB, true);
		}
		
		SaveOptionDialog(JDialog parentDialog, 
                String title, 
                boolean firstIsFromDB,
                boolean secondIsFromDB,
				boolean doInit) {
			super(parentDialog, title);
			this.firstIsFromDB = firstIsFromDB;
			this.secondIsFromDB = secondIsFromDB;
			if (doInit)
				init();
		}

		public void init() {
			// Add the text option
			JTextArea ta = new JTextArea("Do you want to save the merged instance as a new " +
			                             "instance or overwrite one of the comparing instances?");
			ta.setLineWrap(true);
			ta.setEditable(false);
			Border border1 = BorderFactory.createRaisedBevelBorder();
			Border border2 = BorderFactory.createEmptyBorder(2, 2, 2, 2);
			ta.setBorder(BorderFactory.createCompoundBorder(border1, border2));
			ta.setBackground(getContentPane().getBackground());
			ta.setWrapStyleWord(true);
			getContentPane().add(ta, BorderLayout.NORTH);                             
			JPanel optionPane = new JPanel();
			optionPane.setBorder(BorderFactory.createRaisedBevelBorder());
			optionPane.setLayout(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.insets = new Insets(4, 4, 4, 4);
			constraints.anchor = GridBagConstraints.WEST;
			ButtonGroup btnGroup = new ButtonGroup();
			saveAsNewBtn = new JRadioButton(saveAsNewBtnTitle);
			if (saveAsNewBtnFirst)
				saveAsNewBtn.setSelected(true); // Default
			replaceFirstBtn = new JRadioButton(replaceFirstBtnTitle);
			if (!saveAsNewBtnFirst)
				replaceFirstBtn.setSelected(true);
			replaceSecondBtn = new JRadioButton(replaceSecondBtnTitle);
			if (saveAsNewBtnFirst)
				btnGroup.add(saveAsNewBtn);
			btnGroup.add(replaceFirstBtn);
			btnGroup.add(replaceSecondBtn);
			if (!saveAsNewBtnFirst)
				btnGroup.add(saveAsNewBtn);
			optionPane.add(saveAsNewBtn, constraints);
			constraints.gridy = 1;
			optionPane.add(replaceFirstBtn, constraints);
			constraints.gridy = 2;
			optionPane.add(replaceSecondBtn, constraints);
			getContentPane().add(optionPane, BorderLayout.CENTER);
			// OK and cancel
			JPanel southPane = new JPanel();
			southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
			okBtn = new JButton("OK");
			if (saveDialogActionListener!=null)
				okBtn.addActionListener(saveDialogActionListener);
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = true;
					SaveOptionDialog.this.dispose();
				}
			});
			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = false;
					SaveOptionDialog.this.dispose();
				}
			});
			okBtn.setPreferredSize(cancelBtn.getPreferredSize());
			southPane.add(okBtn);
			southPane.add(cancelBtn);
			getContentPane().add(southPane, BorderLayout.SOUTH);
			// Set the Dialog
			setSize(400, 250);
			setLocationRelativeTo(InstanceComparisonPane.this);

			if (hideUnusedButtons) {
				if (firstIsFromDB)
					replaceFirstBtn.setVisible(false);
				if (secondIsFromDB)
					replaceSecondBtn.setVisible(false);
			} else {
				if (firstIsFromDB)
					replaceFirstBtn.setEnabled(false);
				if (secondIsFromDB)
					replaceSecondBtn.setEnabled(false);
			}
}
		
		public int getOption() {
			int option;
			if (replaceFirstBtn.isSelected())
				option = OVERWRITE_FIRST;
			else if (replaceSecondBtn.isSelected())
				option = OVERWRITE_SECOND;
			else // Default
				option = SAVE_AS_NEW;
			return option;
		}
		
		public JButton getOkBtn() {
			return okBtn;
		}
		/**
		 * @param hideUnusedButtons The hideUnusedButtons to set.
		 */
		public void setHideUnusedButtons(boolean hideUnusedButtons) {
			this.hideUnusedButtons = hideUnusedButtons;
		}
		/**
		 * @param replaceFirstBtnTitle The replaceFirstBtnTitle to set.
		 */
		public void setReplaceFirstBtnTitle(String replaceFirstBtnTitle) {
			this.replaceFirstBtnTitle = replaceFirstBtnTitle;
		}
		/**
		 * @param replaceSecondBtnTitle The replaceSecondBtnTitle to set.
		 */
		public void setReplaceSecondBtnTitle(String replaceSecondBtnTitle) {
			this.replaceSecondBtnTitle = replaceSecondBtnTitle;
		}
		/**
		 * @param saveAsNewBtnFirst The saveAsNewBtnFirst to set.
		 */
		public void setSaveAsNewBtnFirst(boolean saveAsNewBtnFirst) {
			this.saveAsNewBtnFirst = saveAsNewBtnFirst;
		}
		/**
		 * @param saveAsNewBtnTitle The saveAsNewBtnTitle to set.
		 */
		public void setSaveAsNewBtnTitle(String saveAsNewBtnTitle) {
			this.saveAsNewBtnTitle = saveAsNewBtnTitle;
		}
		
		/**
		 * @return Returns the hideUnusedButtons.
		 */
		public boolean isHideUnusedButtons() {
			return hideUnusedButtons;
		}
		/**
		 * @return Returns the replaceFirstBtnTitle.
		 */
		public String getReplaceFirstBtnTitle() {
			return replaceFirstBtnTitle;
		}
		/**
		 * @return Returns the replaceSecondBtnTitle.
		 */
		public String getReplaceSecondBtnTitle() {
			return replaceSecondBtnTitle;
		}
		/**
		 * @return Returns the saveAsNewBtnFirst.
		 */
		public boolean isSaveAsNewBtnFirst() {
			return saveAsNewBtnFirst;
		}
		/**
		 * @return Returns the saveAsNewBtnTitle.
		 */
		public String getSaveAsNewBtnTitle() {
			return saveAsNewBtnTitle;
		}
	}
}
