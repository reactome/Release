/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.gk.persistence.MySQLAdaptor;

/** 
 *  Provides the actions for the buttons and menus of the IDGenerationFrame.
 * @author croft
 */
public class IDGenerationController {
	private IdentifierDatabase identifierDatabase;
	private IDGenerator idGenerator;
	private IDGenerationFrame idGenerationFrame;

    private Action quitAction = null;
    private Action testAction = null;
    private Action runAction = null;
    private Action identifierDatabaseAction = null;
    private Action gk_centralAction = null;
    private Action rollbackAction = null;
    private Action checkVersionNumsAction = null;

	public IdentifierDatabase getIdentifierDatabase() {
		return identifierDatabase;
	}

	public IDGenerationController(IDGenerationFrame idGenerationFrame) {
		this.idGenerationFrame = idGenerationFrame;
		
		identifierDatabase = new IdentifierDatabase();
		idGenerator = null;
	}
	
	private void quit() {
		if (idGenerationFrame!=null)
			idGenerationFrame.exit();
	}
	
    public Action getQuitAction() {
    	if (quitAction == null) {
    		quitAction = new AbstractAction("Quit") {
    			public void actionPerformed(ActionEvent e) {
    				quit();
    			}
    		};
    	}
    	return quitAction;
    }
    
	private void test() {
		List schemaClasses = getSchemaClassesIfOk();
		if (schemaClasses!=null) {
	    	IdentifierDatabase.setDba(IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.IDENTIFIER_MANAGER));
			MySQLAdaptor previousDba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.PREVIOUS_MANAGER);
			MySQLAdaptor currentDba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.CURRENT_MANAGER);
			MySQLAdaptor gk_centraldba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.GK_CENTRAL_MANAGER);			
			idGenerator = new IDGenerator(previousDba, currentDba, gk_centraldba, identifierDatabase);
			idGenerator.testIDs(schemaClasses);
			idGenerationFrame.getIdGenerationPane().getTestResultsPane().setTitleLabelTest();
			displayTestResults();
		}
	}
	
	public Action getTestAction() {
    	if (testAction == null) {
    		testAction = new AbstractAction("Test") {
    			public void actionPerformed(ActionEvent e) {
    				test();
    			}
    		};
    	}
    	return testAction;
	}
	
	private void run() {
		int reply = JOptionPane.showConfirmDialog(idGenerationFrame,
				"Run will modify live databases. " +
				"Are you really sure you want to do this?",
				"Run confirmation",                                                                          JOptionPane.YES_NO_OPTION);
		if (reply == JOptionPane.NO_OPTION)
			return;
		
		List schemaClasses = getSchemaClassesIfOk();
		if (schemaClasses!=null) {
	    	IdentifierDatabase.setDba(IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.IDENTIFIER_MANAGER));
			MySQLAdaptor previousDba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.PREVIOUS_MANAGER);
			MySQLAdaptor currentDba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.CURRENT_MANAGER);
			MySQLAdaptor gk_centraldba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.GK_CENTRAL_MANAGER);			
			idGenerator = new IDGenerator(previousDba, currentDba, gk_centraldba, identifierDatabase);
			idGenerator.generateIDs(schemaClasses);
			idGenerationFrame.getIdGenerationPane().getTestResultsPane().setTitleLabelRun();
			displayTestResults();
		}
	}
	
	public Action getRunAction() {
    	if (runAction == null) {
    		runAction = new AbstractAction("Run") {
    			public void actionPerformed(ActionEvent e) {
    				run();
    			}
    		};
    	}
    	return runAction;
	}
	
	private void rollback() {
		int reply = JOptionPane.showConfirmDialog(idGenerationFrame,
				"Rollback will irreversibly destroy stable ID information " +
				"for the selected release.  Are you really sure you want to do this?",
				"Rollback confirmation",                                                                          JOptionPane.YES_NO_OPTION);
		if (reply == JOptionPane.NO_OPTION)
			return;
		
		List schemaClasses = getSchemaClassesIfOk();
    	IdentifierDatabase.setDba(IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.IDENTIFIER_MANAGER));
		MySQLAdaptor previousDba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.PREVIOUS_MANAGER);
		MySQLAdaptor currentDba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.CURRENT_MANAGER);
		MySQLAdaptor gk_centraldba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.GK_CENTRAL_MANAGER);			
		idGenerator = new IDGenerator(previousDba, currentDba, gk_centraldba, identifierDatabase);
		idGenerator.rollbackIDs(schemaClasses);
	}
	
	public Action getRollbackAction() {
    	if (rollbackAction == null) {
    		rollbackAction = new AbstractAction("Rollback") {
    			public void actionPerformed(ActionEvent e) {
    				rollback();
    			}
    		};
    	}
    	return rollbackAction;
	}
	
	private void checkVersionNums() {
//		IdentifierDatabase.correctVersionNums(true);
	}
	
	public Action getCheckVersionNumsAction() {
    	if (checkVersionNumsAction == null) {
    		checkVersionNumsAction = new AbstractAction("Check ver nums") {
    			public void actionPerformed(ActionEvent e) {
    				checkVersionNums();
    			}
    		};
    	}
    	return checkVersionNumsAction;
	}
	
	private void identifierDb() {
		// Get database connection info from user
		IDGenerationDBConnectionPane dbConnectionPane = new IDGenerationDBConnectionPane();
		dbConnectionPane.setValues(IDGenerationPersistenceManagers.IDENTIFIER_MANAGER, SystemProperties.retrieveSystemProperties());
		if (dbConnectionPane.showInDialog(idGenerationFrame)) {
			dbConnectionPane.commit();
			idGenerationFrame.getIdGenerationPane().getReleasesPane().refreshTable();
			
			// Check to see if the refresh is OK
			if (IdentifierDatabase.getDba()==null)
	            JOptionPane.showMessageDialog(idGenerationFrame,
	                    "Could not get a database connection - maybe there is something wrong with the database parameters you entered?",
	                    "No database connection",
	                    JOptionPane.WARNING_MESSAGE);
		}
	}
	
	public Action getIdentifierDatabaseAction() {
    	if (identifierDatabaseAction == null) {
    		identifierDatabaseAction = new AbstractAction("Set identifier database") {
    			public void actionPerformed(ActionEvent e) {
    				identifierDb();
    			}
    		};
    	}
    	return identifierDatabaseAction;
	}
	
	private void gk_central() {
		// Get database connection info from user
		IDGenerationDBConnectionPane dbConnectionPane = new IDGenerationDBConnectionPane();
		dbConnectionPane.setValues(IDGenerationPersistenceManagers.GK_CENTRAL_MANAGER, SystemProperties.retrieveSystemProperties());
		if (dbConnectionPane.showInDialog(idGenerationFrame)) {
			dbConnectionPane.commit();
			idGenerationFrame.getIdGenerationPane().getReleasesPane().refreshTable();
		}
	}
	
	public Action getgk_centralAction() {
    	if (gk_centralAction == null) {
    		gk_centralAction = new AbstractAction("Set gk_central") {
    			public void actionPerformed(ActionEvent e) {
    				gk_central();
    			}
    		};
    	}
    	return gk_centralAction;
	}
	
	/**
	 * Before running, first check that the user has selected the
	 * appropriate stuff.  If so, return a list of selected
	 * schema classes, otherwise return null.
	 */ 
	public List getSchemaClassesIfOk() {
		IncludeInstancesPane includeInstancesPane = idGenerationFrame.getIdGenerationPane().getIncludeInstancePane();
		List schemaClasses = includeInstancesPane.getSelectedRootClasses();
		if (schemaClasses==null) {
            JOptionPane.showMessageDialog(idGenerationFrame,
                    "No schema available - you may need to set the database parameters for the selected release.",
                    "Schema not available",
                    JOptionPane.ERROR_MESSAGE);
			return null;
		}
		if (schemaClasses.size()<1) {
            JOptionPane.showMessageDialog(idGenerationFrame,
                    "You must select at least one instance class.",
                    "Class not selected",
                    JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		ReleasesPane releasesPane = idGenerationFrame.getIdGenerationPane().getReleasesPane();
		if (!releasesPane.isCurrentReleaseSelected()) {
            JOptionPane.showMessageDialog(idGenerationFrame,
                    "You must select a current release.",
                    "Current not selected",
                    JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		String releaseCountString = idGenerationFrame.getIdGenerationPane().getReleasesPane().getController().getModel().getLastNonNullReleaseNum();
		MySQLAdaptor previousDba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.PREVIOUS_MANAGER);
		String previousReleaseNumString = identifierDatabase.getReleaseNumFromReleaseDba(previousDba);
		MySQLAdaptor currentDba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(IDGenerationPersistenceManagers.CURRENT_MANAGER);
		String currentReleaseNumString = identifierDatabase.getReleaseNumFromReleaseDba(currentDba);
		int previousReleaseNum = Integer.MIN_VALUE;
		if (previousReleaseNumString!=null && !previousReleaseNumString.equals(""))
			previousReleaseNum = (new Integer(previousReleaseNumString)).intValue();
		int currentReleaseNum = Integer.MIN_VALUE;
		if (currentReleaseNumString!=null && !currentReleaseNumString.equals(""))
			currentReleaseNum = (new Integer(currentReleaseNumString)).intValue();
		int releaseCount = Integer.MIN_VALUE;
		if (releaseCountString!=null && !releaseCountString.equals(""))
			releaseCount = (new Integer(releaseCountString)).intValue();
		int reply;
		
		if (previousReleaseNumString==null || previousReleaseNumString.equals("")) {
			reply = JOptionPane.showConfirmDialog(idGenerationFrame,
					"You have not specified a previous release - the current release\n" +
					"will be treated as a first release, i.e. completely new stable IDs\n" +
					"will be generated.  Are you sure you want to do this?",
					"Missing previous",                                                                          JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.NO_OPTION)
				return null;
		}
		if (previousReleaseNum==currentReleaseNum) {
			reply = JOptionPane.showConfirmDialog(idGenerationFrame,
					"Previous and current release numbers are identical!\n" +
					"That doesn't make sense.\n" +
					"Are you sure you want to go ahead with this?",
					"Identical releases",                                                                          JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.NO_OPTION)
				return null;
		}
		if (previousReleaseNum>currentReleaseNum) {
			reply = JOptionPane.showConfirmDialog(idGenerationFrame,
					"Previous release number is greater than current release number -\n" +
					"that is rather unusual!!\n" +
					"Is this really what you want to do?",
					"Previous bigger",                                                                          JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.NO_OPTION)
				return null;
		}
		if (currentReleaseNum != releaseCount) {
			reply = JOptionPane.showConfirmDialog(idGenerationFrame,
					"The current release (" + currentReleaseNum + ") is not the last in the release list (" + releaseCount + ") -\n" +
					"that is rather unusual!!\n" +
					"Is this really what you want to do?",
					"Current not last",                                                                          JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.NO_OPTION)
				return null;
		}
		
		return schemaClasses;
	}
	
	/**
	 * Extract the test results from idGenerator and pass them onto
	 * the test results panel.
	 * 
	 */
	private void displayTestResults() {
		if (idGenerator!=null) {
			IDGeneratorTests tests = idGenerator.getTests();
			idGenerationFrame.getIdGenerationPane().getTestResultsPane().setTests(tests);
		}
	}
}