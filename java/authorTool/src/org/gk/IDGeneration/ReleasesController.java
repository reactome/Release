/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/** 
 *  Reacts to button clicks, etc. in the ReleasePane and carries out
 *  the requisite actions.
 * @author croft
 */
public class ReleasesController {
	private ReleasesPane releasesPane;
	private ReleasesModel model;
	private IdentifierDatabase identifierDatabase = null;
    private Action addReleaseAction = null;
    private Action editReleaseAction = null;
    private Action dbSliceParamsReleaseAction = null;
    private Action dbReleaseParamsReleaseAction = null;
    private Action dbCopyParamsReleaseAction = null;
    private Action deleteReleaseAction = null;
    private Action previousReleaseAction = null;
    private Action currentReleaseAction = null;
    private Action previousClearReleaseAction = null;
    private Action currentClearReleaseAction = null;
	
	public ReleasesController(ReleasesPane releasesPane) {
		this.releasesPane = releasesPane;
		refreshModel();
    	IdentifierDatabase.setDba(IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.IDENTIFIER_MANAGER));
	}
	
	/** 
	 *  Adds a new release to the identifier database.  This is done by
	 *  popping up an AttributePane in edit mode.  The user would be
	 *  expected to enter at leaset the release number, a date would be
	 *  nice too.
	 */
	private void addRelease() {
		GKInstance release = IdentifierDatabase.createBlankRelease();
		// Put the current date/time into this parameter,
		// just to make sure that something half sensible
		// is there.  The user can always change it by hand
		// in the SQL if it needs a different value.
		String currentReleaseDateTime = GKApplicationUtilities.getDateTime();
		try {
			release.setAttributeValue("dateTime", currentReleaseDateTime);
		} catch (InvalidAttributeException e) {
			JOptionPane.showMessageDialog(releasesPane,
                    "Unknown attribute dateTime for release",
                    "Unknown attribute",
                    JOptionPane.ERROR_MESSAGE);
		} catch (InvalidAttributeValueException e) {
			JOptionPane.showMessageDialog(releasesPane,
                    "Invalid value: " +  currentReleaseDateTime + " for attribute dateTime for release",
                    "Invalid attribute value",
                    JOptionPane.ERROR_MESSAGE);
		}
		model.storeRelease(release);
	}
	
	public Action getAddReleaseAction() {
    	if (addReleaseAction == null) {
    		addReleaseAction = new AbstractAction("Add") {
    			public void actionPerformed(ActionEvent e) {
    				addRelease();
    			}
    		};
    	}
    	return addReleaseAction;
	}
	
	/** 
	 *  Changes database parameters for the currently selected release in the identifier database.
	 *  This is done by popping up a DBConnectionPane
	 */
	private void sliceDbParamsRelease() {
		JTable table = releasesPane.getReleasesTable();
		int seletctedRow = table.getSelectedRow();
		GKInstance release = model.getReleaseByIndex(seletctedRow);
		
		// Nothing selected
		if (release==null)
			return;
		
		// Get database connection info from user
		ReleasesDBConnectionPane dbConnectionPane = new ReleasesDBConnectionPane();
		dbConnectionPane.setValues(release);
		if (dbConnectionPane.showInDialog(releasesPane)) {
			dbConnectionPane.commit();
			model.updateRelease(release);
		}
	}
	
	public Action getSliceDbParamsReleaseAction() {
    	if (dbSliceParamsReleaseAction == null) {
    		dbSliceParamsReleaseAction = new AbstractAction("Slice") {
    			public void actionPerformed(ActionEvent e) {
    				sliceDbParamsRelease();
    			}
    		};
    	}
    	return dbSliceParamsReleaseAction;
	}
	
	/** 
	 *  Changes database parameters for the currently selected release in the identifier database.
	 *  This is done by popping up a DBConnectionPane
	 */
	private void releaseDbParamsRelease() {
		JTable table = releasesPane.getReleasesTable();
		int seletctedRow = table.getSelectedRow();
		GKInstance release = model.getReleaseByIndex(seletctedRow);
		
		// Nothing selected
		if (release==null)
			return;
		
		// Get database connection info from user
		ReleasesDBConnectionPane dbConnectionPane = new ReleasesDBConnectionPane();
		dbConnectionPane.setDbParamsAttributeName(ReleasesDBConnectionPane.RELEASE);
		dbConnectionPane.setValues(release);
		if (dbConnectionPane.showInDialog(releasesPane)) {
			dbConnectionPane.commit();
			model.updateRelease(release);
		}
	}
	
	public Action getReleaseDbParamsReleaseAction() {
    	if (dbReleaseParamsReleaseAction == null) {
    		dbReleaseParamsReleaseAction = new AbstractAction("Release") {
    			public void actionPerformed(ActionEvent e) {
    				releaseDbParamsRelease();
    			}
    		};
    	}
    	return dbReleaseParamsReleaseAction;
	}
	
	/** 
	 *  Copies database parameters for the currently selected release from slice
	 *  to release.  This is a convenience method. 
	 */
	private void copyDbParamsRelease() {
		JTable table = releasesPane.getReleasesTable();
		int seletctedRow = table.getSelectedRow();
		GKInstance release = model.getReleaseByIndex(seletctedRow);
		
		// Nothing selected
		if (release==null)
			return;
		
		IdentifierDatabase.copySliceToReleaseDbParams(release);
		
		model.updateRelease(release);
	}
	
	public Action getCopyDbParamsReleaseAction() {
    	if (dbCopyParamsReleaseAction == null) {
    		dbCopyParamsReleaseAction = new AbstractAction("S->R") {
    			public void actionPerformed(ActionEvent e) {
    				copyDbParamsRelease();
    			}
    		};
    	}
    	return dbCopyParamsReleaseAction;
	}
	
	/** 
	 *  Edits the currently selected release in the identifier database.
	 *  This is done by popping up an AttributePane in edit mode.
	 */
	private void editRelease() {
		JTable table = releasesPane.getReleasesTable();
		int seletctedRow = table.getSelectedRow();
		GKInstance release = model.getReleaseByIndex(seletctedRow);
		editRelease(release);
	}
	
	/** 
	 *  Edits the supplied release in the identifier database.
	 *  This is done by popping up an AttributePane in edit mode.
	 */
	private void editRelease(GKInstance release) {
		// Nothing selected
		if (release==null)
			return;
		
		AttributeDialog attributeDialog = new AttributeDialog(releasesPane.getIDGenerationPane().getFrame(), release);
		
		attributeDialog.setVisible(true);
		
		if (attributeDialog.isOk())
			model.updateRelease(release);
	}
	
	public Action getEditReleaseAction() {
    	if (editReleaseAction == null) {
    		editReleaseAction = new AbstractAction("Edit") {
    			public void actionPerformed(ActionEvent e) {
    				editRelease();
    			}
    		};
    	}
    	return editReleaseAction;
	}
	
	/** 
	 *  Deletes the currently selected release in the identifier database,
	 *  also deletes all references to this release.
	 */
	private void deleteRelease() {
		JTable table = releasesPane.getReleasesTable();
		GKInstance release = model.getReleaseByIndex(table.getSelectedRow());
		deleteRelease(release);
	}
	
	/** 
	 *  Deletes the supplied release in the identifier database,
	 *  also deletes all references to this release.
	 */
	private void deleteRelease(GKInstance release) {
		if (release==null)
			return;
		
		model.deleteRelease(release);
	}
	
	public Action getDeleteReleaseAction() {
    	if (deleteReleaseAction == null) {
    		deleteReleaseAction = new AbstractAction("Del") {
    			public void actionPerformed(ActionEvent e) {
    				deleteRelease();
    			}
    		};
    	}
    	return deleteReleaseAction;
	}
	
	private void currentRelease() {
		int selectedIndex = releasesPane.getReleasesTable().getSelectedRow();
		String releaseNum = model.getReleaseNum(selectedIndex);
		releasesPane.getCurrentReleasePanel().setRelease(releaseNum);

		stashRelease(IDGenerationPersistenceManagers.CURRENT_MANAGER, releaseNum);
		
		updateSchemaDisplayPane();
	}
	
	/** If a new current release has been selected, update
	 * the schema panel to reflect the (possibly) different
	 * database schema.
	 **/
	public void updateSchemaDisplayPane() {
		MySQLAdaptor dba = null;
		try {
			dba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.CURRENT_MANAGER);
		} catch (Exception e) {
            JOptionPane.showMessageDialog(releasesPane,
                    "Could not access database",
                    "Database problem",
                    JOptionPane.ERROR_MESSAGE);
		}
		Schema schema = null;
		if (dba!=null)
			schema = dba.getSchema();
		IncludeInstancesPane includeInstancesPane = releasesPane.getIDGenerationPane().getIncludeInstancePane();
		// Blank out schema panel, if no schema is available, otherwise
		// display new schema.
		if (schema==null)
			includeInstancesPane.clear();
		else
			includeInstancesPane.setSchema(schema);
	}
	
	public Action getCurrentReleaseAction() {
    	if (currentReleaseAction == null) {
    		currentReleaseAction = new AbstractAction() {
    			public void actionPerformed(ActionEvent e) {
    				currentRelease();
    			}
    		};
    	}
    	return currentReleaseAction;
	}
	
	private void previousRelease() {
		int selectedIndex = releasesPane.getReleasesTable().getSelectedRow();
		String releaseNum = model.getReleaseNum(selectedIndex);
		releasesPane.getPreviousReleasePanel().setRelease(releaseNum);

		stashRelease(IDGenerationPersistenceManagers.PREVIOUS_MANAGER, releaseNum);
	}
	
	public Action getPreviousReleaseAction() {
    	if (previousReleaseAction == null) {
    		previousReleaseAction = new AbstractAction() {
    			public void actionPerformed(ActionEvent e) {
    				previousRelease();
    			}
    		};
    	}
    	return previousReleaseAction;
	}
	
	private void currentClearRelease() {
		stashRelease(IDGenerationPersistenceManagers.CURRENT_MANAGER, "");
		
		updateSchemaDisplayPane();
	}
	
	public Action getCurrentClearReleaseAction() {
    	if (currentClearReleaseAction == null) {
    		currentClearReleaseAction = new AbstractAction() {
    			public void actionPerformed(ActionEvent e) {
    				currentClearRelease();
    			}
    		};
    	}
    	return currentClearReleaseAction;
	}
	
	private void previousClearRelease() {
		stashRelease(IDGenerationPersistenceManagers.PREVIOUS_MANAGER, "");
	}
	
	public Action getPreviousClearReleaseAction() {
    	if (previousClearReleaseAction == null) {
    		previousClearReleaseAction = new AbstractAction() {
    			public void actionPerformed(ActionEvent e) {
    				previousClearRelease();
    			}
    		};
    	}
    	return previousClearReleaseAction;
	}
	
	// Transfer information from the selected release to the appropriate
	// persistence manager.
	private void stashRelease(String managerName, String releaseNum) {
		int selectedIndex = releasesPane.getReleasesTable().getSelectedRow();
		IDGenerationPersistenceManager manager = IDGenerationPersistenceManagers.getManager().getManager(managerName);
		
		// Stash the database connection info for the release
		try {
			GKInstance release = model.getReleaseByIndex(selectedIndex);
			if (release==null) {
				System.err.println("ReleasesController.currentRelease: WARNING - retrieved release is null for selectedIndex=" + selectedIndex);
				return;
			}
			GKInstance dbParams = (GKInstance)release.getAttributeValue("sliceDbParams");
			if (dbParams==null || releaseNum.equals(""))
				manager.unsetDBConnectInfo();
			else {
				String dbHost = (String)dbParams.getAttributeValue("host");
				String dbName = (String)dbParams.getAttributeValue("dbName");
				String dbPort = (String)dbParams.getAttributeValue("port");
				String dbUser = (String)dbParams.getAttributeValue("user");
				String dbPwd = (String)dbParams.getAttributeValue("pwd");
				manager.setDBConnectInfo(dbHost, dbName, dbPort, dbUser, dbPwd);
			}
		} catch (Exception e) {
			System.err.println("ReleasesController.currentRelease: something went wrong while trying to stash database connection info");
			e.printStackTrace();
		}
		
		// Add to the system properties, so that we remember what
		// the user selected the next time we start
		Properties systemProperties = SystemProperties.retrieveSystemProperties();
		if (releaseNum.equals(""))
			systemProperties.remove(managerName + ".releaseNum");
		else
			systemProperties.put(managerName + ".releaseNum", releaseNum);
	}
	
	public void refreshModel() {
		try {
			if (identifierDatabase==null) {
				
				
				// Do this to help debugging
				IDGenerationPane gloopyIDGenerationPane = releasesPane.getIDGenerationPane();
				IDGenerationFrame gloopyIDGenerationFrame = gloopyIDGenerationPane.getFrame();
				IDGenerationController gloopyIDGenerationController = gloopyIDGenerationFrame.getController();
				IdentifierDatabase gloopyIdentifierDatabase = gloopyIDGenerationController.getIdentifierDatabase();
				
				
				identifierDatabase = releasesPane.getIDGenerationPane().getFrame().getController().getIdentifierDatabase();
			}
		} catch (NullPointerException e) {
			System.err.println("ReleasesController.refreshModel: exception while trying to get an identifier database!");
			e.printStackTrace();
		}
		
		// This is a bit of a random hack, I don't know if it
		// is really OK.
		if (identifierDatabase==null)
			identifierDatabase = new IdentifierDatabase();

		model = new ReleasesModel(identifierDatabase);
	}
	
	public ReleasesModel getModel() {
		return model;
	}
	
	private class AttributeDialog extends JDialog {
		private GKInstance instance;
		private boolean ok = false;
		
		public AttributeDialog(JFrame parentFrame, GKInstance instance) {
			super(parentFrame);
			this.instance = instance;
			init();
		}

		private void init() {
			String title = "Properties: " + instance.toString();
			setTitle(title);
			
			// Add attribute pane
			SimpleAttributePane propPane = new SimpleAttributePane(instance);
			propPane.setEditable(true);
			getContentPane().add(propPane, BorderLayout.CENTER);

			// Add buttons
			JPanel southPane = new JPanel();
			southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
			JButton okBtn = new JButton("OK");
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ok = true;
					dispose();
				}
			});
			southPane.add(okBtn);
			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ok = false;
					dispose();
				}
			});
			southPane.add(cancelBtn);
			getContentPane().add(southPane, BorderLayout.SOUTH);
			setSize(450, 500);
			GKApplicationUtilities.center(this);
			setModal(true);
		}

		public boolean isOk() {
			return ok;
		}
	}
	
}