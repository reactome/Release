/*
 * Created on Dec 23, 2003
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.database.AttributeEditManager;
import org.gk.database.FrameManager;
import org.gk.database.InstanceComparer;
import org.gk.database.InstanceComparisonPane;
import org.gk.database.InstanceListPane;
import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.SectionTitlePane;

/**
 * A customized JDialog for syncrhonize the local repository with the database.
 * @author wugm
 */
public class SynchronizationDialog extends JDialog{
	// For map keys
	private final String DELETE_KEY = "delete";
	private final String DELETE_IN_DB_KEY = "deleteInDB";
	private final String NEW_KEY = "new";
	private final String CHANGED_KEY = "changed";
	private final String NEW_CHANGE_IN_DB_KEY = "newChangeInDB";
	private final String NEW_CHANGE_IN_LOCAL_KEY = "newChangeInLocal";
	private final String CONFLICT_CHANGE_KEY = "conflictChange";
	private final String IS_IDENTICAL_KEY = "isIdentical";
    private final String LOCAL_HAS_MORE_IE_KEY = "localHasMoreIE";
	// Actions
	private Action updateFromDBAction;
	private Action commitToDBAction;
	private Action showComparisonAction;
	private Action clearRecordAction;
	
	private boolean isSame = false;
	
	private Map syncMap;
	private Map typeMap;
	private boolean isCancelled;
	// Two adaptors
	private XMLFileAdaptor fileAdaptor;
	private MySQLAdaptor dbAdaptor;
	// The four InstanceListPane
	private InstanceListPane changedList;
	private InstanceListPane newList;
	private InstanceListPane deleteInDBList;
	private InstanceListPane deleteList;
    private InstanceListPane localHasMoreIEList;
    // Use this map to illustrate the update
    private Map listToTitle = new HashMap();
    // A flag that is used to allow commit local instances having
    // unexpected InstanceEdit (e.g. Instances from MOD)
    private boolean isLocalHasUnexpIECommitAllowed = false;
	private JPanel centerPane;
	// The selected class
	private GKSchemaClass selectedCls;
	// EventListeners for InstanceListPane
	private ListSelectionListener selectionListener;
	private MouseListener mouseAdaptor;
	private ListCellRenderer cellRenderer;
	
	public SynchronizationDialog(JFrame parentFrame) {
		super(parentFrame);
		init();
	}

	public SynchronizationDialog(JFrame parentFrame, 
                                 XMLFileAdaptor fileAdaptor, 
                                 MySQLAdaptor dbAdaptor) {
		this(parentFrame);
		setAdaptors(fileAdaptor, dbAdaptor);
	}
	
	private void init() {
		setTitle("Synchronization Results");
		// Add a OK button
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
		// Initialize actions
		initUpdateFromDBAction();
		initCommitToDBAction();
		initShowComparisonAction();
		initClearRecordAction();
		// Add a toolbar
		JToolBar toolbar = new JToolBar();
		Insets insets = new Insets(0, 0, 0, 0);
		JButton updateFromDBBtn = toolbar.add(updateFromDBAction);
        updateFromDBBtn.setText((String)updateFromDBAction.getValue(Action.NAME));
		JButton commitToDBBtn = toolbar.add(commitToDBAction);
        commitToDBBtn.setText((String)commitToDBAction.getValue(Action.NAME));
		JButton showComparisonBtn = toolbar.add(showComparisonAction);
        showComparisonBtn.setText((String)showComparisonAction.getValue(Action.NAME));
		JButton clearRecordBtn = toolbar.add(clearRecordAction);
        clearRecordBtn.setText((String)clearRecordAction.getValue(Action.NAME));
		if (!GKApplicationUtilities.isMac()) {
             updateFromDBBtn.setMargin(insets);
             commitToDBBtn.setMargin(insets);
             showComparisonBtn.setMargin(insets);
             clearRecordBtn.setMargin(insets);
         }
		getContentPane().add(toolbar, BorderLayout.NORTH);
		setSize(600, 700);
		GKApplicationUtilities.center(this);
		setModal(true);
        isLocalHasUnexpIECommitAllowed = SynchronizationManager.getManager().isLocalHasUnexpIECommitAllowed();
	}
    
    public void setSelectedClass(GKSchemaClass cls) {
	    this.selectedCls = cls;
	}
	
	public GKSchemaClass getSelectedClass() {
	    return this.selectedCls;
	}
	
	protected List getSynchronizingClassList(XMLFileAdaptor fileAdaptor) {
	    GKSchemaClass topCls = selectedCls;
	    if (topCls == null)
	        topCls = (GKSchemaClass) ((GKSchema)fileAdaptor.getSchema()).getRootClass();
	    List clsList = new ArrayList();
	    InstanceUtilities.getDescendentClasses(clsList, topCls);
	    return clsList;
	}
	
	protected long getTotalInstanceCount(XMLFileAdaptor fileAdaptor) throws Exception{
	    GKSchemaClass topCls = selectedCls;
	    if (topCls == null)
	        topCls = (GKSchemaClass) ((GKSchema)fileAdaptor.getSchema()).getRootClass();
	    return fileAdaptor.getClassInstanceCount(topCls);
	}
	
	private void initClearRecordAction() {
		clearRecordAction = new AbstractAction("Clear Record",
		                                       GKApplicationUtilities.createImageIcon(getClass(), "ClearRecord.gif")) {
			public void actionPerformed(ActionEvent e) {
				clearDeleteRecord();
			}
		};
		clearRecordAction.putValue(Action.SHORT_DESCRIPTION, "Clear delete record");
		// Disable as default
		clearRecordAction.setEnabled(false);
	}
	
	private void clearDeleteRecord() {
		if (deleteList != null) {
			java.util.List list = deleteList.getSelection();
			if (list.size() > 0) {
				try {
					java.util.List dbIDs = new ArrayList(list.size());
					for (Iterator it = list.iterator(); it.hasNext();) {
						GKInstance instance = (GKInstance) it.next();
						dbIDs.add(instance.getDBID());
					}
					fileAdaptor.clearDeleteRecord(dbIDs);	
				}
				catch(IOException e) {
					System.err.println("SynchronizationDialog.clearDeleteRecord(): " + e);
					e.printStackTrace();
				}
				deleteList.deleteInstances(list);
				// Check if deleteList needs to be removed
				if (deleteList.getDisplayedInstances().size() == 0) {
					centerPane.remove(deleteList);
					centerPane.validate();
					centerPane.repaint();
				}
			}
		}
	}

	private void initUpdateFromDBAction() {
		updateFromDBAction = new AbstractAction("Update From DB",
		                                        GKApplicationUtilities.createImageIcon(getClass(), "UpdateFromDB.gif")) {
			public void actionPerformed(ActionEvent e) {
				updateFromDB();
			}
		};
		updateFromDBAction.putValue(Action.SHORT_DESCRIPTION, "Update from the database repository");
		updateFromDBAction.setEnabled(false);
	}
	
	private void updateFromDB() {
	    InstanceListPane touchedList = null;
	    // Find out which instances have been selected in all relevant lists
	    // Update the attribute values for changed list.
	    try {
	        if (changedList != null) {
	            java.util.List list = new ArrayList(changedList.getSelection()); // To support it.remove() 
	            // Use a new ArrayList.
	            if (list != null && list.size() > 0) {
	                for (Iterator it = list.iterator(); it.hasNext();) {
	                    GKInstance instance = (GKInstance)it.next();
	                    // Fetch GKInstance based on its DB_ID since SchemaClass might be changed.
	                    GKInstance localCopy = fileAdaptor.fetchInstance(instance.getDBID());
	                    dbAdaptor.setUseCache(false);
	                    GKInstance dbCopy = dbAdaptor.fetchInstance(instance.getDBID());
	                    dbAdaptor.setUseCache(true);
	                    if(SynchronizationManager.getManager().updateFromDB(localCopy, dbCopy, this)) {
	                        AttributeEditManager.getManager().attributeEdit(localCopy);
	                        fileAdaptor.removeDirtyFlag(localCopy);
	                    }
	                    else
	                        it.remove(); // Remove from the list
	                }
	                // Remove all selected instances
	                changedList.deleteInstances(list);
                    touchedList = changedList;
	            }
	        }
            if (localHasMoreIEList != null && localHasMoreIEList.getSelection().size() > 0) {
                List list = new ArrayList(localHasMoreIEList.getSelection()); 
                for (Iterator it = list.iterator(); it.hasNext();) {
                    GKInstance localCopy = (GKInstance) it.next();
                    dbAdaptor.setUseCache(false);
                    GKInstance dbCopy = dbAdaptor.fetchInstance(localCopy.getDBID());
                    dbAdaptor.setUseCache(true);
                    if(SynchronizationManager.getManager().updateFromDB(localCopy, dbCopy, this)) {
                        AttributeEditManager.getManager().attributeEdit(localCopy);
                        fileAdaptor.removeDirtyFlag(localCopy);
                    }
                    else
                        it.remove(); // Remove from the list, if it it cancelled by the user.
                }
                localHasMoreIEList.deleteInstances(list);
                touchedList = localHasMoreIEList;
            }
	        // Delete the local copy for deleteInDBList.
	        if (deleteInDBList != null) {
	            java.util.List list = deleteInDBList.getSelection();
	            if (list != null && list.size() > 0) {
	                for (Iterator it = list.iterator(); it.hasNext();) {
	                    GKInstance instance = (GKInstance) it.next();
	                    fileAdaptor.deleteInstance(instance);
	                }
	            }
	            deleteInDBList.deleteInstances(list);
                touchedList = deleteInDBList;
	        }
	        // Get another copy for the local repository
	        if (deleteList != null) {
	            java.util.List<?> list = deleteList.getSelection();
	            if (list != null && list.size() > 0) {
	                Map<SchemaClass, Set<GKInstance>> checkOutMap = new HashMap<SchemaClass, Set<GKInstance>>(); // All checked out Instances
	                for (Iterator<?> it = list.iterator(); it.hasNext();) {
	                    GKInstance instance = (GKInstance)it.next();
	                    Map<SchemaClass, List<GKInstance>> schemaMap = InstanceUtilities.listDownloadableInstances(instance);
	                    // Have to merge schemaMap to checkOutMap
	                    for (SchemaClass key : schemaMap.keySet()) {
	                        List<GKInstance> list1 = schemaMap.get(key);
	                        if (list1 != null && list1.size() > 0) {
	                            Set<GKInstance> list2 = checkOutMap.get(key);
	                            if (list2 == null) {
	                                list2 = new HashSet<GKInstance>();
	                                checkOutMap.put(key, list2);
	                            }
	                            // Remove first to avoid any duplication
	                            list2.addAll(list1);
	                        }
	                    }
	                }
	                fileAdaptor.store(checkOutMap);
	                InstanceUtilities.clearShellFlags(checkOutMap);
	                // There are maybe more than selected instances after checking out
	                List<GKInstance> checkedOut = new ArrayList<GKInstance>();
	                for (Set<GKInstance> set : checkOutMap.values()) {
	                    checkedOut.addAll(set);
	                }
	                deleteList.deleteInstances(checkedOut);
	                //deleteList.deleteInstances(list);
	                // Need to clear out these deletion records
	                List<Long> dbIds = new ArrayList<Long>();
                    for (GKInstance inst : checkedOut)
                        dbIds.add(inst.getDBID());
                    fileAdaptor.clearDeleteRecord(dbIds);
                    touchedList = deleteList;
	            }
	        }
	    }
	    catch (Exception e) {
	        System.err.println("SynchronizationDialog.updateFromDB(): " + e);
	        e.printStackTrace();
	    }
	    updateInstanceList(touchedList);
	}
	
	private void initCommitToDBAction() {
		commitToDBAction = new AbstractAction("Commit to DB", 
		                                      GKApplicationUtilities.createImageIcon(getClass(), "CommitToDB.gif")) {
			public void actionPerformed(ActionEvent e) {
				commitToDB();
			}
		};
		commitToDBAction.putValue(Action.SHORT_DESCRIPTION, "Commit to the database repository");
		commitToDBAction.setEnabled(false);
	}

    private void commitToDB() {
        // Find which InstanceListPane should be used
        InstanceListPane instanceList = null;
        InstanceListPane[] panes = new InstanceListPane[] {
                changedList,
                newList,
                deleteInDBList,
                localHasMoreIEList,
                deleteList
        };
        for (int i = 0; i < panes.length; i++) {
            if (panes[i] == null)
                continue;
            if (panes[i].getSelection().size() > 0) {
                instanceList = panes[i];
                break;
            }
        }
        if (instanceList == null)
            return;
        List selection = instanceList.getSelection();
        List commitList = null;
        if (instanceList != deleteList) {
            // Make sure any user changes have been saved before doing
            // a commit
            saveChanges(fileAdaptor);
            // Now do the commit
            commitList = SynchronizationManager.getManager().commitToDB(selection,
                                                                        fileAdaptor,
                                                                        dbAdaptor,
                                                                        false,
                                                                        this);
        }
        else {
            commitList = SynchronizationManager.getManager().deleteInstancesInDB(dbAdaptor,
                                                                                 fileAdaptor,
                                                                                 selection,
                                                                                 this);
        }
        if (commitList != null && commitList.size() > 0) {
            instanceList.deleteInstances(commitList);
        }
        updateInstanceList(instanceList);
	}

    /**
     * A helper method to update GUIs related the passed InstanceListPane:
     * delete, or update the assoicated title.
     * @param instanceList
     */
    private void updateInstanceList(InstanceListPane instanceList) {
        if (instanceList == null)
            return; // Just in case
        if (instanceList.getDisplayedInstances().size() == 0) {
            removeInstanceList(instanceList);
            // Have to null the original reference
            if (instanceList == changedList)
                changedList = null;
            else if (instanceList == newList)
                newList = null;
            else if (instanceList == deleteInDBList)
                deleteInDBList = null;
            else if (instanceList == localHasMoreIEList)
                localHasMoreIEList = null;
            else if (instanceList == deleteList)
                deleteList = null;
        }
        else {
            // Update label
            SectionTitlePane titlePane = (SectionTitlePane) listToTitle.get(instanceList);
            String title = titlePane.getTitle();
            int index = title.lastIndexOf(":");
            title = title.substring(0, index) + ": " + instanceList.getDisplayedInstances().size();
            titlePane.setTitle(title);
        }
    }
    
    private void removeInstanceList(InstanceListPane listPane) {
        SectionTitlePane titlePane = (SectionTitlePane) listToTitle.get(listPane);
        centerPane.remove(titlePane);
        centerPane.remove(listPane);
        centerPane.validate();
        centerPane.repaint();
        listToTitle.remove(listPane);
    }
	
	/**
	 * A helper method to save changes
	 * @param fileAdaptor
	 * @param curatorFrame
	 * @return true for saving the changes, while false for an unsuccessful saving. 
	 * An unsuccessful saving might result from cancelling or throwing an exception.
	 */
	private boolean saveChanges(XMLFileAdaptor fileAdaptor) {
		// Make sure everything is changed
		if (fileAdaptor.isDirty()) {
			int reply = JOptionPane.showConfirmDialog(this,
													  "You have to save changes first before doing synchronization.\n" + 
													  "Do you want to save changes and then do synchronization?",
													  "Save Changes?",
													  JOptionPane.OK_CANCEL_OPTION);
			if (reply == JOptionPane.CANCEL_OPTION)
				return false;
			try {
				fileAdaptor.save();
				return true;
			}
			catch(Exception e) {
				JOptionPane.showMessageDialog(this,
											  "Cannot save changes:" + e.getMessage(),
											  "Error in Saving",
											  JOptionPane.ERROR_MESSAGE);
				System.err.println("SynchronizationDialog.saveChanges(): " + e);
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	private void initShowComparisonAction() {
		showComparisonAction = new AbstractAction("Show Comparison",
		                                          GKApplicationUtilities.createImageIcon(getClass(), "ShowComparison.gif")) {
			public void actionPerformed(ActionEvent e) {
				showComparison();
			}
		};
		showComparisonAction.putValue(Action.SHORT_DESCRIPTION, "Compare an instance in the local and database repositories");
		showComparisonAction.setEnabled(false);
	}
	
	private void handleMergeResult(GKInstance localCopy,
	                                GKInstance dbCopy,
	                                InstanceComparisonPane comparisonPane) {
        if (comparisonPane.getSaveMergeOption() == InstanceComparisonPane.OVERWRITE_FIRST) {
            // Need to refresh the selected instance
            InstanceComparer comparer = new InstanceComparer();
            try {
                 int result = comparer.compare(localCopy, dbCopy);
                 if (result != InstanceComparer.IS_IDENTICAL) {
                     typeMap.put(localCopy, mapCompareResultToString(result));
                 }
                 else {
                     typeMap.remove(localCopy);
                     changedList.deleteInstance(localCopy);
                 }
                 changedList.repaint();
             }
             catch (Exception e1) {
                 System.err.println("SynchronizationDialog.handleMergeResult(): " + e1);
                 e1.printStackTrace();
             }
        }
        else if (comparisonPane.getSaveMergeOption() == InstanceComparisonPane.SAVE_AS_NEW) {
            // Need to add a new instance to the local new instance
            GKInstance newInstance = comparisonPane.getMerged();
            List newInstances = (List) syncMap.get(NEW_KEY);
            if (newInstances == null) {
                newInstances = new ArrayList();
                syncMap.put(NEW_KEY, newInstances);
            }
            newInstances.add(newInstance);
            typeMap.put(newInstance, NEW_KEY);
            if (newList == null) {
                // Get some information from changed
                newList = initInstanceListPane(newInstances,
                                               "Instances created locally: " + newInstances.size());
                centerPane.validate();
            }
            else {
                // Cannot call this method because it is used for one schema class only
                //newList.addInstance(newInstance);
                newList.setTitle("Instances created locally: " + newInstances.size());
                newList.setDisplayedInstances(newInstances);
                newList.repaint();
            }
        }
	}
	
	private void showComparison() {
	    // Find the instance that can be used for comparision. This instance
	    // should be chosen from changedList or localHasUnexpInstance
	    GKInstance instance = null;
	    if (changedList != null) {// Check changedList first
	        List selection = changedList.getSelection();
	        if (selection.size() > 0)
	            instance = (GKInstance) selection.get(0);
	    }
	    if (instance == null && localHasMoreIEList != null) {
	        List selection = localHasMoreIEList.getSelection();
	        if (selection.size() > 0)
	            instance = (GKInstance) selection.get(0);
	    }
	    if (instance == null)
	        return;
	    final InstanceComparisonPane comparisonPane = new InstanceComparisonPane();
	    // Fine tune comparison pane for making comparisons with
	    // database
	    comparisonPane.setSaveDialogHideUnusedButtons(true);
	    comparisonPane.setSaveDialogSaveAsNewBtnFirst(false);
	    comparisonPane.setSaveDialogSaveAsNewBtnTitle("Create new local instance and put merge into that");
	    comparisonPane.setSaveDialogReplaceFirstBtnTitle("Overwrite existing instance with merge (recommended)");
	    comparisonPane.setCloseAfterSaving(true);
	    try {
	        String clsName = instance.getSchemClass().getName();
	        Long instanceId = instance.getDBID();
	        final GKInstance localCopy = fileAdaptor.fetchInstance(clsName, instanceId);
	        dbAdaptor.setUseCache(false);
	        //final GKInstance dbCopy = dbAdaptor.fetchInstance(clsName, instanceId);
	        // The class type might be changed locally or remotely. So not clsName should be used
	        // to fetch database instance. Otherwise, null exception will be thrown. The same thing 
	        // is done also in creating synchronization result.
	        final GKInstance dbCopy = dbAdaptor.fetchInstance(instanceId);
	        dbAdaptor.setUseCache(true);
	        comparisonPane.setInstances(localCopy, dbCopy);
	        String title = "Comparing Instances \"" + instance.getDisplayName()
	        + "\" in the local and DB repositories";
	        JDialog parentDialog = (JDialog) SwingUtilities.getAncestorOfClass(
	                                                                           JDialog.class, centerPane);
	        final JDialog dialog = new JDialog(parentDialog, title);
	        // Try to update the list automatically
	        dialog.addWindowListener(new WindowAdapter() {
	            public void windowClosed(WindowEvent e) {
	                handleMergeResult(localCopy, dbCopy, comparisonPane);
	                // To prevent double calling. It is called again when parentDialog is closed.
	                dialog.removeWindowListener(this);
	            }
	        });
	        dialog.getContentPane().add(comparisonPane, BorderLayout.CENTER);
	        dialog.setModal(true);
	        dialog.setSize(800, 600);
	        GKApplicationUtilities.center(dialog);
	        dialog.setVisible(true);
	        
	        // Update synchronization dialog panels depending on user
	        // choice.
	        if (comparisonPane.getSaveMergeOption() == InstanceComparisonPane.SAVE_AS_NEW) {
	            GKInstance merged = comparisonPane.getMerged();
	            
	            if (newList==null) {
	                List list = new ArrayList();
	                list.add(merged);
	                newList = initInstanceListPane(list,
	                                               "Instances created locally: " + list.size());
	            } else
	                newList.addInstance(merged);
	        } else if (comparisonPane.getSaveMergeOption() == InstanceComparisonPane.OVERWRITE_FIRST) {
	            dbAdaptor.setUseCache(false);
	            GKInstance remoteCopy = dbAdaptor.fetchInstance(instance.getDBID());
	            dbAdaptor.setUseCache(true);
	            if (remoteCopy != null) {
	                InstanceComparer comparer = new InstanceComparer();
	                int reply = comparer.compare(instance, remoteCopy);
	                if (reply == InstanceComparer.LOCAL_HAS_MORE_IE) {
	                    if (localHasMoreIEList == null) {
	                        List list = new ArrayList();
	                        list.add(instance);
	                        localHasMoreIEList = initInstanceListPane(list, 
	                                                                   "Local instances having unexpected InstanceEdits: " + list.size());
	                    }
	                    else
	                        localHasMoreIEList.addInstance(instance);
	                }
	                else if (reply == InstanceComparer.IS_IDENTICAL) {
	                    // Merge has made local and database copies
	                    // identical - don't need to check in anymore
	                    changedList.deleteInstance(instance);
	                } else if (reply != InstanceComparer.NEW_CHANGE_IN_LOCAL) {
	                    // Merge has produced a conflict
	                    typeMap.put(mapCompareResultToString(reply), instance);
	                } else
	                    // The user should now be allowed to commit this
	                    // instance, because it will now have changed.
	                    commitToDBAction.setEnabled(true);
	                
	                JOptionPane.showMessageDialog(parentDialog,
	                                              "Merge successful, you will need to check the merged instance into the database.\n\n",
	                                              "Merge OK",
	                                              JOptionPane.INFORMATION_MESSAGE);
	            }
	        }
	        
	    }
	    catch (Exception e1) {
	        System.err.println("Synchronization.createDisplayMapPane(): " + e1);
	        e1.printStackTrace();
	        JOptionPane.showMessageDialog(this,
	                                      "Error in comparing: " + instance.toString(),
	                                      "Error in Comparing",
	                                      JOptionPane.ERROR_MESSAGE);
	    }
	}
	
	/**
	 * Check if the synchronization is cancelled by the user.
	 * @return
	 */
	public boolean isCancelled() {
		return this.isCancelled;
	}
	
	/**
	 * Check if the local repository is the same as the database repository.
	 * @return
	 */
	public boolean isSame() {
		return isSame;
	}
	
	public void setAdaptors(XMLFileAdaptor fileAdaptor,
            MySQLAdaptor dbAdaptor) {
		setAdaptors(fileAdaptor, dbAdaptor, null);
	}
	
	/**
	 * Sets up lists of instances that are different between database
	 * and local instances.  If uncheckableList is null, then the
	 * "difference" between database and local will be used to
	 * generate these lists.  If uncheckableList contains something,
	 * then the contents of the list (assumed to be of type GKInstance)
	 * will be taken instead.
	 * 
	 * @param fileAdaptor
	 * @param dbAdaptor
	 * @param uncheckableList
	 */
	public void setAdaptors(XMLFileAdaptor fileAdaptor,
	                        MySQLAdaptor dbAdaptor,
							List uncheckableList) {
		try {
			this.fileAdaptor = fileAdaptor;
			this.dbAdaptor = dbAdaptor;
			typeMap = new HashMap();
			if (uncheckableList!=null) {
				// Create a "fake" syncMap if a list of instances
				// already exists.
				syncMap = new HashMap();
				syncMap.put(CHANGED_KEY, uncheckableList);
				
				// Create the corresponding "fake" type map.  This
				// is needed so that the correct actions are assigned
				// to these instances, e.g. "update from db".
				GKInstance uncheckableInstance;
				for (Iterator it = uncheckableList.iterator(); it.hasNext();) {
					uncheckableInstance = (GKInstance)it.next();
					typeMap.put(uncheckableInstance, NEW_CHANGE_IN_DB_KEY);
				}
			} else // The usual case
				// Create a syncMap based on the discrepancies
				// between local and database instances.
				syncMap = synchronize(fileAdaptor, dbAdaptor);
			if (syncMap.size() == 0)
				isSame = true;
			else {
				isSame = false;
				JPanel mapPane = createDisplayMapPane();
				getContentPane().add(mapPane, BorderLayout.CENTER);
				setTitle("Synchronization Results vs. " + 
				                dbAdaptor.getDBName() + 
				                "@" + 
				                dbAdaptor.getDBHost());
			}
		}
		catch(Exception e) {
			System.err.println("SynchronizationPane.setAdaptors(): " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Synchronize a local instance repository to the db repository. Calling this method may modify
	 * deleteMap Map in the FileAdaptor object.
	 * @param fileAdaptor the PersistenceAdaptor for the local instance repository.
	 * @param dbAdaptor the PersistenceAdaptor for the database instance repository.
	 * @return Key: delete, deleteInDB, new and/or changed; Values: A list of instances
	 * 
	 * Sideeffect: changes typeMap.
	 */
	private Map synchronize(final XMLFileAdaptor fileAdaptor, 
                            final MySQLAdaptor dbAdaptor) {
		// Need another thread to display the progress
		final ProgressDialog progDialog = new ProgressDialog((JFrame)getOwner());
		progDialog.setSize(300, 180);
		//GKApplicationUtilities.center(progDialog);
		progDialog.setLocationRelativeTo(progDialog.getOwner());
		progDialog.setModal(true);
		final List syncClassList = getSynchronizingClassList(fileAdaptor);
		// Get the total instance counter
		Schema schema = fileAdaptor.getSchema();
		SchemaClass cls = null;
		long total = 0;
		try {
		    total = getTotalInstanceCount(fileAdaptor);
		}
		catch (Exception e) {
			System.err.println("SyncrhonizationEngine.synchronize(): " + e);
			e.printStackTrace();
		}
		progDialog.totalBar.setMaximum((int)total);
		progDialog.totalBar.setMinimum(0);
		// Let the synchronizing work in another thread.
		final Map map = new HashMap();
		Thread t = new Thread() {
			public void run() {
			    java.util.List newInstances = new ArrayList();
				java.util.List changedInstances = new ArrayList();
				java.util.List deletedInDBInstances = new ArrayList();
                java.util.List localHasMoreIEInstances = new ArrayList();
				Schema schema = fileAdaptor.getSchema();
				SchemaClass schemaClass = null;
				Collection instances = null;
				GKInstance instance = null;
				GKInstance dbCopy = null;
				int index = 0;
				int total = 0;
				// For comparsing
				InstanceComparer comparer = new InstanceComparer();
				try {
					for (Iterator it = syncClassList.iterator(); it.hasNext();) {
						if (isCancelled) 
							break;
						schemaClass = (SchemaClass)it.next();
						instances = fileAdaptor.fetchInstancesByClass(schemaClass, false);
						if (instances == null || instances.size() == 0)
							continue;
						progDialog.clsLabel.setText("Scan class " + schemaClass.getName() + "...");
						progDialog.clsBar.setMinimum(0);
						progDialog.clsBar.setMaximum(instances.size());
						List existedIDs = new ArrayList(instances.size());
						for (Iterator it1 = instances.iterator(); it1.hasNext();) {
						    instance = (GKInstance) it1.next();
						    if (instance.getDBID().longValue() < 0) {
						        newInstances.add(instance);
						        typeMap.put(instance, NEW_KEY);
						    }
						    else // Shell instances should not be skipped since a shell instance can still
						        // be deleted locally so existence checking is needed.
						        existedIDs.add(instance.getDBID());
						}
						if (existedIDs.size() == 0)
						    continue;
						Collection existences = dbAdaptor.fetchInstances(schemaClass.getName(), existedIDs);
						if (existences.size() > 0) {
                            // Load all modified attribute
                            SchemaAttribute att = schemaClass.getAttribute("modified");
                            // Load all atributes in a batch
                            dbAdaptor.loadInstanceAttributeValues(existences, att);
                        }
						index = 0;
						for (Iterator it1 = instances.iterator(); it1.hasNext();) {
						    if (isCancelled)
						        break;
						    instance = (GKInstance) it1.next();
						    if (instance.getDBID().longValue() < 0) { // Handled by the previous loop
						        continue;
						    }
						    // The schema class might be changed. Use the
                            // top-leve class to fetch instance
                            dbCopy = dbAdaptor.fetchInstance(instance.getDBID());
                            if (dbCopy == null) {
                                deletedInDBInstances.add(instance);
                                typeMap.put(instance, DELETE_IN_DB_KEY);
                            }
                            //Don't care a shell instance
                            else {
                                if (!instance.isShell()) {
                                    int tmp = comparer.compare(instance, dbCopy);
                                    String typeKey = mapCompareResultToString(tmp);
                                    if (typeKey == LOCAL_HAS_MORE_IE_KEY) {
                                        localHasMoreIEInstances.add(instance);
                                        typeMap.put(instance, typeKey);
                                    }
                                    else if (typeKey != IS_IDENTICAL_KEY) {
                                        changedInstances.add(instance);
                                        typeMap.put(instance, typeKey);
                                    }
                                }
                            }
                            index++;
                            total++;
                            progDialog.clsBar.setValue(index);
                            progDialog.totalBar.setValue(total);
                        }
                    }
					if (deletedInDBInstances.size() > 0)
						map.put(DELETE_IN_DB_KEY, deletedInDBInstances);
					if (newInstances.size() > 0)
						map.put(NEW_KEY, newInstances);
					if (changedInstances.size() > 0)
						map.put(CHANGED_KEY, changedInstances);
                    if (localHasMoreIEInstances.size() > 0)
                        map.put(LOCAL_HAS_MORE_IE_KEY, localHasMoreIEInstances);
					// Check the delete instances
					Map deleteMap = getLocalDeleteMap(fileAdaptor, syncClassList);
					if (deleteMap != null && deleteMap.size() > 0) {
                        java.util.List deleteInstances = new ArrayList();
                        List clearingIDs = new ArrayList();
                        for (Iterator it = deleteMap.keySet().iterator(); it.hasNext();) {
                            Long dbID = (Long) it.next();
                            String className = (String) deleteMap.get(dbID);
                            dbCopy = dbAdaptor.fetchInstance(className, dbID);
                            if (dbCopy != null) {
                                deleteInstances.add(dbCopy);
                                typeMap.put(dbCopy, DELETE_KEY);
                            }
                            else
                                clearingIDs.add(dbID);
                        }
                        if (deleteInstances.size() > 0)
                            map.put(DELETE_KEY, deleteInstances);
                        fileAdaptor.clearDeleteRecord(clearingIDs);
					}
					progDialog.dispose();
				}
				catch (Exception e) {
					System.err.println("SynchronizationEngine.synchronize() 1: " + e);
					e.printStackTrace();
					JOptionPane.showMessageDialog(progDialog,
					                              "Error in synchronizing: \n" + e,
					                              "Error in Synchronizing",
					                              JOptionPane.ERROR_MESSAGE);
					// Have to set this first to behavior correctly since thread issue.
					isCancelled = true;
					progDialog.dispose();
				}
			}
		};
		t.start();
		progDialog.setVisible(true);
		return map;
	}
	
	
	private String mapCompareResultToString(int result) {
        switch (result) {
        	case InstanceComparer.NEW_CHANGE_IN_DB:
        	    return NEW_CHANGE_IN_DB_KEY;
        	case InstanceComparer.NEW_CHANGE_IN_LOCAL:
        	    return NEW_CHANGE_IN_LOCAL_KEY;
        	case InstanceComparer.CONFLICT_CHANGE:
        	    return CONFLICT_CHANGE_KEY;
            case InstanceComparer.LOCAL_HAS_MORE_IE :
                return LOCAL_HAS_MORE_IE_KEY;
        	default:
        	    return IS_IDENTICAL_KEY;
        }
	}
	
	private Map getLocalDeleteMap(XMLFileAdaptor fileAdaptor, 
                                  List synClassList) {
		Map rtnMap = new HashMap();
	    Map deleteMap = fileAdaptor.getDeleteMap();
		if (deleteMap != null && deleteMap.size() > 0) {
            List synClassName = new ArrayList(synClassList.size());
            for (Iterator it = synClassList.iterator(); it.hasNext();) {
                GKSchemaClass cls = (GKSchemaClass) it.next();
                synClassName.add(cls.getName());
            }
		    for (Iterator it = deleteMap.keySet().iterator(); it.hasNext();) {
                Long dbID = (Long) it.next();
                String className = (String) deleteMap.get(dbID);
                if (synClassName.contains(className))
                    rtnMap.put(dbID, className);
            }
        }	    
		return rtnMap;
	}
	
	private void doPopup(MouseEvent e) {
		// Check if there is any thing is selected
		boolean isSelected = false;
		if (changedList != null) {
			if (changedList.getSelection().size() > 0)
				isSelected = true;
		}
		if (!isSelected && newList != null) {
			if (newList.getSelection().size() > 0)
				isSelected = true;
		}
		if (!isSelected && deleteInDBList != null) {
			if (deleteInDBList.getSelection().size() > 0)
				isSelected = true;
		}
		if (!isSelected && deleteList != null)
			if (deleteList.getSelection().size() > 0)
				isSelected = true;
        if (!isSelected && localHasMoreIEList != null) {
            if (localHasMoreIEList.getSelection().size() > 0)
                isSelected = true;
        }
		if (!isSelected)
			return;
		JPopupMenu popup = new JPopupMenu();
		popup.add(updateFromDBAction);
		popup.add(commitToDBAction);
		popup.add(showComparisonAction);
		popup.add(clearRecordAction);
		JComponent comp = (JComponent) e.getSource();
		popup.show(comp, e.getX(), e.getY());
	}
	
	private void validateActions() {
		boolean isChangedSelected = false;
		if (changedList != null && changedList.getSelection().size() > 0)
			isChangedSelected = true;
		boolean isNewSelected = false;
		if (newList != null && newList.getSelection().size() > 0)
			isNewSelected = true;
		boolean isDeleteInDBSelected = false;
		if (deleteInDBList != null &&
		    deleteInDBList.getSelection().size() > 0)
		    isDeleteInDBSelected = true;
		boolean isDeleteSelected = false;
		if (deleteList != null && deleteList.getSelection().size() > 0)
			isDeleteSelected = true;
        boolean isLocalHasUnExpSelected = false;
        if (localHasMoreIEList != null && localHasMoreIEList.getSelection().size() > 0)
            isLocalHasUnExpSelected = true;
		if (!isChangedSelected && !isNewSelected && 
		    !isDeleteInDBSelected && !isDeleteSelected && !isLocalHasUnExpSelected) {
			updateFromDBAction.setEnabled(false);
			commitToDBAction.setEnabled(false);
			showComparisonAction.setEnabled(false);
			clearRecordAction.setEnabled(false);
			return;
		}
		// UpdateFromDBAction
		if (!isNewSelected) 
			updateFromDBAction.setEnabled(true);
		else
			updateFromDBAction.setEnabled(false);
		// CommitToDBAction
		if (isChangedSelected) {
		    // detailed cases
		    List changed = changedList.getSelection();
		    boolean isLocalChangeSelected = false;
		    boolean isDBChangeSelected = false;
		    boolean isConflictSelected = false;
		    for (Iterator it = changed.iterator(); it.hasNext();) {
		        GKInstance instance = (GKInstance) it.next();
		        String type = (String) typeMap.get(instance);
		        if (type == NEW_CHANGE_IN_LOCAL_KEY)
		            isLocalChangeSelected = true;
		        else if (type == NEW_CHANGE_IN_DB_KEY)
		            isDBChangeSelected = true;
		        else if (type == CONFLICT_CHANGE_KEY ||
                         type == LOCAL_HAS_MORE_IE_KEY)
		            isConflictSelected = true;
		    }
		    if (isDBChangeSelected || isConflictSelected)
		        commitToDBAction.setEnabled(false);
		    else
		        commitToDBAction.setEnabled(true);
		}
		else if (isLocalHasUnExpSelected) {
            // Add a key in the config so that only some people can
            // commit this strange InstanceEdit
            if (isLocalHasUnexpIECommitAllowed)
                commitToDBAction.setEnabled(true);
            else
                commitToDBAction.setEnabled(false);
        }
        else
            commitToDBAction.setEnabled(true);
		// showComparisonAction
        if (isChangedSelected || isLocalHasUnExpSelected)
			showComparisonAction.setEnabled(true);
		else
			showComparisonAction.setEnabled(false);
		// ClearRecordAction
		if (isDeleteSelected) // Single selection enforced
			clearRecordAction.setEnabled(true);
		else
			clearRecordAction.setEnabled(false);
	}
    
    private InstanceListPane figureOutInstanceListPane(JList list) {
        Component parent = list.getParent();
        while (true) {
            if (parent instanceof InstanceListPane) 
                break;
            parent = parent.getParent();
        }
        return (InstanceListPane) parent;
    }
    
    private void ensureSingleListSelection(ListSelectionEvent e) {
        JList list = (JList) e.getSource();
        // Need to find which InstanceListPane 
        InstanceListPane instanceList = figureOutInstanceListPane(list);
        // Need to clear selection in other place
        InstanceListPane[] lists = new InstanceListPane[] {
                changedList,
                newList,
                deleteInDBList,
                deleteList,
                localHasMoreIEList
        };
        // Remove the current selection listener to avoid bunching back
        for (int i = 0; i < lists.length; i++) {
            if (lists[i] == null)
                continue;
            lists[i].removeSelectionListener(selectionListener);
        }
        for (int i = 0; i < lists.length; i++) {
            if (lists[i] == null)
                continue;
            if (lists[i] == instanceList)
                continue;
            lists[i].clearSelection();
        }
        // Add the selection listener back
        for (int i = 0; i < lists.length; i++) {
            if (lists[i] == null)
                continue;
            lists[i].addSelectionListener(selectionListener);
        }
    }
	
	private JPanel createDisplayMapPane() {
		centerPane = new JPanel();
        centerPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		cellRenderer = new SyncListCellRenderer();
		selectionListener = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
                ensureSingleListSelection(e);
				validateActions();
			}
		};
		centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.Y_AXIS));
		java.util.List list = (java.util.List) syncMap.get(CHANGED_KEY);
		if (list != null && list.size() > 0) {
			changedList = initInstanceListPane(list, 
                                               "Instances different between the local and the db repositories: " + list.size());
		}
		list = (java.util.List) syncMap.get(NEW_KEY);
		if (list != null && list.size() > 0) {
			// Double click to show the Instance in the local repository
		    newList = initInstanceListPane(list,
		                                   "Instances created locally: " + list.size());
		}
		list = (java.util.List) syncMap.get(DELETE_IN_DB_KEY);
		if (list != null && list.size() > 0) {
			// Double click to show the Instance in the local repository
		    deleteInDBList = initInstanceListPane(list,
		                    "Instances deleted in the database: " + list.size());
		}
		list = (java.util.List) syncMap.get(DELETE_KEY);
		if (list != null && list.size() > 0) {
			// Double click to show the Instance in the db repository
		    deleteList = initInstanceListPane(list,
		                    "Instances deleted locally: " + list.size());
		}
        list = (java.util.List) syncMap.get(LOCAL_HAS_MORE_IE_KEY);
        if (list != null && list.size() > 0) {
            localHasMoreIEList = initInstanceListPane(list, 
                                                       "Local instances having unexpected InstanceEdits: " + list.size());
        }
		return centerPane;
	}
	
	private InstanceListPane initInstanceListPane(List instances, 
	                                              String title) {
		InstanceListPane instanceList = new InstanceListPane();
		instanceList.getInstanceList().addListSelectionListener(selectionListener);
		instanceList.setIsViewable(false); 
		if (mouseAdaptor == null) {
			// To view instances
			mouseAdaptor = new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger())
						doPopup(e);
				}
					
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger())
						doPopup(e);
				}
					
				// Use mouseClicked method. See InstanceListPane.init().
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 &&
                        e.getClickCount() == 2) {
						JList list = (JList) e.getSource();
						if (list.getSelectedValues().length != 1)
							return;
                        InstanceListPane instanceList = figureOutInstanceListPane(list);
                        if (instanceList == changedList ||
                            instanceList == localHasMoreIEList) {
                            showComparison();
                        }
                        else {
                            GKInstance instance = (GKInstance) list.getSelectedValue();
                            JDialog parentDialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, centerPane);
                            FrameManager.getManager().showInstance(instance, parentDialog);
                        }
					}
				}
			};
		}
		instanceList.getInstanceList().addMouseListener(mouseAdaptor);
		InstanceUtilities.sortInstances(instances);
		instanceList.setDisplayedInstances(instances);
		//instanceList.setTitle(title);
		instanceList.setListCellRenderer(cellRenderer);
		instanceList.hideTitle();
        SectionTitlePane titlePane = new SectionTitlePane(title);
        titlePane.setSectionPane(instanceList);
        centerPane.add(titlePane);
		centerPane.add(instanceList);
        listToTitle.put(instanceList, titlePane);
		return instanceList;
	}
	
	class SyncListCellRenderer extends DefaultListCellRenderer {
		
		private Icon icon;
		private Icon deleteInDBIcon;
		private Icon deleteIcon;
		private Icon newInstanceIcon;
		private Icon newChangeIcon;
		private Icon dbChangeIcon;
		private Icon conflictIcon;
		
		public SyncListCellRenderer() {
			super();
			icon = GKApplicationUtilities.createImageIcon(getClass(), "Instance.gif");
			deleteInDBIcon = GKApplicationUtilities.createImageIcon(getClass(), "InstanceDeleteInDB.png");
			deleteIcon = GKApplicationUtilities.createImageIcon(getClass(), "InstanceDelete.png");
			newInstanceIcon = GKApplicationUtilities.createImageIcon(getClass(), "InstanceNew.png");
			newChangeIcon = GKApplicationUtilities.createImageIcon(getClass(), "InstanceNewChange.png");
			dbChangeIcon = GKApplicationUtilities.createImageIcon(getClass(), "InstanceNewChangeInDB.png");
			conflictIcon = GKApplicationUtilities.createImageIcon(getClass(), "InstanceChangeConflict.png");
		}
		
		public Component getListCellRendererComponent(JList list, 
		                                               Object value,
		                                               int index, 
		                                               boolean isSelected, 
		                                               boolean hasFocus) {
			Component comp = super.getListCellRendererComponent(list, 
			                                                     value,
			                                                     index, 
			                                                     isSelected, 
			                                                     hasFocus());
			if (!(value instanceof GKInstance))
			    return comp;
			String text = null;
            GKInstance instance = (GKInstance) value;
            text = instance.getDisplayName();
            if (text == null || (text != null && text.length() == 0)) {
                text = instance.getExtendedDisplayName();
            }
            String type = (String) typeMap.get(instance);
            if (type == DELETE_IN_DB_KEY)
                setIcon(deleteInDBIcon);
            else if (type == DELETE_KEY)
                setIcon(deleteIcon);
            else if (type == NEW_KEY)
                setIcon(newInstanceIcon);
            else if (type == NEW_CHANGE_IN_DB_KEY)
                setIcon(dbChangeIcon);
            else if (type == NEW_CHANGE_IN_LOCAL_KEY)
                setIcon(newChangeIcon);
            else if (type == CONFLICT_CHANGE_KEY ||
                     type == LOCAL_HAS_MORE_IE_KEY)
                setIcon(conflictIcon);
            else
                setIcon(icon);
            if (text == null) {
                text = "";
                setIcon(null);
            }
            setText(text);
            if (text.length() > 0)
                setToolTipText(text);
            else
                setToolTipText(null);
            return comp;
		}	    
	}
	
	class ProgressDialog extends AbstractProgressDialog {		
		public ProgressDialog(JFrame parentFrame) {
			super(parentFrame, "Synchronizing with the DB repository...");
		}
		
		public void cancel(boolean ic) {
			isCancelled = ic;
		}
	}
}
