/*
 * Created on Nov 11, 2003
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;

import org.gk.database.*;
import org.gk.database.util.MODReactomeAnalyzer;
import org.gk.database.util.ReferencePeptideSequenceAutoFiller;
import org.gk.elv.EntityLevelView;
import org.gk.elv.InstanceCloneHelper;
import org.gk.gkCurator.authorTool.CuratorToolToAuthorToolConverter;
import org.gk.gkEditor.GKEditorFrame;
import org.gk.gkEditor.GKEditorManager;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathView.GKVisualizationPane;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.FileAdaptor;
import org.gk.persistence.GKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.persistence.XMLFileQueryReader;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.gk.util.AboutGKPane;
import org.gk.util.BrowserLauncher;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.GKFileFilter;
import org.gk.util.ProgressPane;
import org.gk.util.XMLFileFilter;

/**
 * A list of actions for this package.
 * @author wugm
 */
public class CuratorActionCollection {
    private Action dbSchemaViewAction;
    private Action dbEventViewAction;
    private Action saveProjectAction;
    private Action createInstanceAction;
    private Action deleteInstanceAction;
    private Action viewReferersAction;
    private Action compareInstancesAction;
    private Action compareDBInstanceAction;
    private Action updateFromDBAction;
	private Action checkInAction;
	private Action synchronizeDBAction;
	private Action viewSchemaAction;
	private Action cloneInstanceAction;
	private Action helpAction;
	private Action aboutAction;
	private Action viewInstanceAction;
	private Action optionAction;
	private Action visualizationAction;
	private Action openInAuthoringToolAction;
	private Action exportToAuthoringToolAction;
	private Action importFromAuthoringToolAction;
	private Action addBookmarkAction;
	private Action exportAsProtegePrjAction;
	private Action exportAsProtegePinAction;
	private Action switchTypeAction;
	private Action mergeInstanceAction;
	private Action matchInstanceAction;
    private Action openAction;
    private Action newProjectAction;
    private Action saveAsAction;
    private Action importFromVer1PrjAction;
    private Action importExternalPathwayAction;
    private Action reportBugAction;
    private Action requestNewGOTermAction;
    private Action trackGORequestAction;
    private Action searchInstanceAction;
    // Edit a list of GKInstances in one shot
    private Action batchEditAction;
    // Create EWAS from ReferencePeptideSequence
    private Action createEwasFromRefPepSeqAction;
    // create a new project from a MOD database
    private Action importFromMODReactome;
    // Project information
    private Action projectInfoAction;
    // Rebuild project based on an old project from previous schema.
    // The database will be queired from fetch instances
    private Action rebuildProjectAction;
    // A feature to deploy a pathway diagram via a servlet deployed in the server side
    private Action deployDiagramAction;
	// Used to launch the browser for the PSI-MOD ontology browser
    private Action launchPsiModBrowserAction;
    // Used to launch the disease browser from EBI
    private Action launchDiseaseBrowserAction;
    // Used to add hasModifiedResidue attribute
    private Action addHasModfiedResidueAction;
    private Action deepCloneDefinedSet;
    // Parent Frame
	private GKCuratorFrame curatorFrame;
	// Want to keep only one author tool file open
	private GKEditorFrame authorFrame;
	
	public CuratorActionCollection(GKCuratorFrame frame) {
		this.curatorFrame = frame;
        List<Action> actions = new ArrayList<Action>();
        actions.add(createOpenInAuthorToolActionForDB());
        actions.add(createExportToAuthorToolActionForDB());
        actions.add(buildProjectForDBAction());
        FrameManager.getManager().setAdditionalActionsForEventView(actions);
	}
	
	public Action getDeepCloneDefinedSet() {
	    if (deepCloneDefinedSet == null) {
	        deepCloneDefinedSet = new AbstractAction("Clone DefinedSet Deeply") {
	            public void actionPerformed(ActionEvent e) {
	                deepCloneDefinedSet();
	            }
	        };
	    }
	    return deepCloneDefinedSet;
	}
	
	private void deepCloneDefinedSet() {
	    java.util.List selection = getSelection();
	    if (selection != null && selection.size() != 1)
	        return;
	    GKInstance inst = (GKInstance) selection.get(0);
	    EntitySetDeepCloneDialog helper = new EntitySetDeepCloneDialog(curatorFrame);
	    if (!helper.validEntitySet(inst)) // validate first
	        return;
	    helper.setInstance(inst);
	    helper.setModal(true);
	    helper.setVisible(true);
	}
	
	public Action getAddHasModifiedResidueAction() {
	    if (addHasModfiedResidueAction == null) {
	        addHasModfiedResidueAction = new AbstractAction("Add hasModifiedResidue") {
	            public void actionPerformed(ActionEvent e) {
	                addHasModifiedResidue();
	            }
	        };
	    }
	    return addHasModfiedResidueAction;
	}
	
	private void addHasModifiedResidue() {
	    // Some simple check
	    java.util.List selection = getSelection();
	    if (selection != null && selection.size() != 1)
	        return;
	    GKInstance inst = (GKInstance) selection.get(0);
	    if (!inst.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
	        return;
	    AddHasModifiedResidueDialog dialog = new AddHasModifiedResidueDialog(curatorFrame, 
	                                                                         "Modification Creation");
	    dialog.setInstance(inst);
	    dialog.setModal(true);
	    dialog.setVisible(true);
	}
	
	public Action getSearchInstanceAction() {
	    if (searchInstanceAction == null) {
	        searchInstanceAction = new AbstractAction("Search Instances",
	                                                  createIcon("Search16.gif")) {
	            public void actionPerformed(ActionEvent e) {
	                searchInstances();
	            }
	        };
	        searchInstanceAction.putValue(Action.SHORT_DESCRIPTION, 
	                                      "Advanced search");
	    }
	    return searchInstanceAction;
	}
	
	private void searchInstances() {
	    JComponent focusedComp = curatorFrame.getFocusedComponent();
	    if (focusedComp instanceof SchemaViewPane) {
	        curatorFrame.getSchemaView().searchInstances();
	    }
	    else if (focusedComp instanceof EventCentricViewPane)
	        curatorFrame.getEventView().searchInstances();
	    else if (focusedComp instanceof EntityLevelView)
	        curatorFrame.getEntityLevelView().searchInstances();
	}
	
	public Action getLaunchPsiModBrowserAction() {
	    if (launchPsiModBrowserAction == null) {
	        launchPsiModBrowserAction = new AbstractAction("Launch PSI-MOD Browser") {
	            public void actionPerformed(ActionEvent e) {
	                String url = "http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MOD";
	                try {
	                    BrowserLauncher.displayURL(url, 
	                                               curatorFrame);
	                }
	                catch(Exception exp) {
	                    JOptionPane.showMessageDialog(curatorFrame,
	                                                  "Cannot launch browser: " + exp.getMessage(),
	                                                  "Error in Launching Browser",
	                                                  JOptionPane.ERROR_MESSAGE);
	                }
	            }
	        };
	    }
	    return launchPsiModBrowserAction;
	}
	
	public Action getLaunchDiseaseBrowserAction() {
	    if (launchDiseaseBrowserAction == null) {
	        launchDiseaseBrowserAction = new AbstractAction("Launch Disease Browser") {
	            public void actionPerformed(ActionEvent e) {
	                String url = "http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=DOID";
	                try {
	                    BrowserLauncher.displayURL(url, 
	                                               curatorFrame);
	                }
	                catch(Exception exp) {
	                    JOptionPane.showMessageDialog(curatorFrame,
	                                                  "Cannot launch browser: " + exp.getMessage(),
	                                                  "Error in Launching Browser",
	                                                  JOptionPane.ERROR_MESSAGE);
	                }
	            }
	        };
	    }
	    return launchDiseaseBrowserAction;
	}
	
	public Action getDeployPathwayDiagramAction() {
	    if (deployDiagramAction == null) {
	        deployDiagramAction = new AbstractAction("Deploy Pathway Diagram") {
	            public void actionPerformed(ActionEvent e) {
	                deployPathwayDiagram();
	            }
	        };
	    }
	    return deployDiagramAction;
	}
	
	private void deployPathwayDiagram() {
	    try {
	        // Get the MySQLAdaptor first to get some connection information
	        final MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(curatorFrame);
	        if (dba == null) {
	            JOptionPane.showMessageDialog(curatorFrame,
	                                          "Cannot connect to the database!",
	                                          "Error in DB Connection",
	                                          JOptionPane.ERROR_MESSAGE);
	            return;
	        }
	        // Get the list of pathway diagram from the connected database
	        Collection diagrams = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
	        // As of March 1, 2010, all diagrams instances can be deployed. However, a warning diagram will appear
	        List<GKInstance> list = new ArrayList<GKInstance>(diagrams);
	        if (diagrams != null && diagrams.size() > 0) {
////	            // Want to make sure only top-level Pathway can be displayed
////	            SchemaAttribute att = dba.getSchema().getClassByName(ReactomeJavaConstants.PathwayDiagram).getAttribute(ReactomeJavaConstants.representedPathway);
////	            dba.loadInstanceAttributeValues(diagrams, att);
////	            Collection c = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
////	            GKInstance frontPage = (GKInstance) c.iterator().next();
////	            List frontPageItem = frontPage.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);
//	            list = new ArrayList<GKInstance>();
//	            for (Iterator it = diagrams.iterator(); it.hasNext();) {
//	                GKInstance pd = (GKInstance) it.next();
//	                GKInstance pathway = (GKInstance) pd.getAttributeValue(ReactomeJavaConstants.representedPathway);
//	                if (frontPageItem.contains(pathway)) {
//	                    list.add(pd);
//	                }
//	            }
	        }
	        if (list == null || list.size() == 0) {
                JOptionPane.showMessageDialog(curatorFrame,
                                              "There are no PathwayDiagram instances in the connected database. Make sure you have checked in\n" +
                                              "your PathwayDiagram instances.",
                                              "No PathwayDiagram Instance",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            InstanceUtilities.sortInstances(list);
	        // Show these instances in a diagram so that the user can choose one
	        InstanceListDialog dialog = new InstanceListDialog(curatorFrame,
	                                                           "Choose a PathwayDiagram",
	                                                           true);
	        dialog.setDisplayedInstances(list);
	        dialog.getInstanceListPane().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	        dialog.setLocationRelativeTo(curatorFrame);
	        dialog.setSubTitle("Please select one PathwayDiagram instance for deployment:");
	        dialog.setSize(800, 400);
	        dialog.setModal(true);
	        dialog.setVisible(true);
	        if (!dialog.isOKClicked()) {
	            return; // Do nothing. Aborted!
	        }
	        List selection = dialog.getInstanceListPane().getSelection();
	        if (selection == null || selection.size() == 0)
	            return;
	        final GKInstance selected = (GKInstance) selection.get(0);
	           // A confirmation diagram
            int reply = JOptionPane.showConfirmDialog(curatorFrame,
                                                      "Are you sure you want to deploy \"" + selected.getDisplayName() + "\"?",
                                                      "Diagram Deploying Confirmation",
                                                      JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                return;
	        // Use a URL to send out these information to the server by using post.
            final String serviceUrl = AttributeEditConfig.getConfig().getPDUrl();
            if (serviceUrl == null) {
                JOptionPane.showMessageDialog(curatorFrame,
                                              "No pathway diagram deployment service URL configured!",
                                              "No Service URL",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Use another thread to make service call
	        Thread t = new Thread() {
	            public void run() {
	                ProgressPane progressPane = new ProgressPane();
	                progressPane.setText("Deploying pathway diagram...");
	                progressPane.setIndeterminate(true);
	                curatorFrame.setGlassPane(progressPane);
	                curatorFrame.getGlassPane().setVisible(true);
	                try {
	                    URL url = new URL(serviceUrl);
	                    URLConnection connection = url.openConnection();
	                    connection.setDoOutput(true);
	                    OutputStream os = connection.getOutputStream();
	                    ObjectOutputStream oos = new ObjectOutputStream(os);
	                    Map<String, Object> info = new HashMap<String, Object>();
	                    info.put("pdId", selected.getDBID());
	                    GKInstance latestIE = InstanceUtilities.getLatestIEFromInstance(selected);
	                    info.put("pdIE", latestIE.getDBID());
	                    // Use char array for some security
	                    info.put("user", dba.getDBUser().toCharArray());
	                    info.put("dbName", dba.getDBName().toCharArray());
	                    oos.writeObject(info);
	                    oos.close();
	                    os.close();
	                    // Now waiting for reply from the server
	                    InputStream is = connection.getInputStream();
	                    // Get the response
	                    BufferedReader bd = new BufferedReader(new InputStreamReader(is));
	                    StringBuilder builder = new StringBuilder();
	                    String line = null;
	                    while ((line = bd.readLine()) != null) {
	                        builder.append(line).append("\n");
	                    }
	                    bd.close();
	                    is.close();
	                    curatorFrame.getGlassPane().setVisible(false);
	                    String message = builder.toString();
	                    if (message.startsWith("The selected pathway diagram has been deployed successfully!")) {
	                        int reply = JOptionPane.showConfirmDialog(curatorFrame, 
	                                                                  message + "Do you want to view the deployed diagram at the following URL?\n" + 
	                                                                  AttributeEditConfig.getConfig().getDevWebELV(), 
	                                                                  "Deploying Pathway Diagram", 
	                                                                  JOptionPane.YES_NO_OPTION);
	                        if (reply == JOptionPane.NO_OPTION)
	                            return;
	                        else {
	                            String webELVUrl = constructWebELVUrl(selected);
	                            BrowserLauncher.displayURL(webELVUrl, 
	                                                       curatorFrame);
	                        }
	                    }
	                    else {
	                        JOptionPane.showMessageDialog(curatorFrame,
	                                                      message,
	                                                      "Deploying Pathway Diagram",
	                                                      JOptionPane.INFORMATION_MESSAGE);
	                    }
	                }
	                catch(Exception e) {
	                    curatorFrame.getGlassPane().setVisible(false);
	                    System.err.println("CuratorActionCollection.deployPathwayDiagram(): " + e);
	                    e.printStackTrace();
	                    JOptionPane.showMessageDialog(curatorFrame,
	                                                  "Error in deploying pathway diagram: " + e,
	                                                  "Error in Deploying Pathway Diagram",
	                                                  JOptionPane.ERROR_MESSAGE);
	                }
	            }
	        };
	        t.start();
	    }
	    catch(Exception e) {
	        JOptionPane.showMessageDialog(curatorFrame,
	                                      "Error in deploying a pathway diagram: " + e, 
	                                      "Error", 
	                                      JOptionPane.ERROR_MESSAGE);
	        System.err.println("CuratorActionCollection.deployPathwayDiagram(): " + e);
	        e.printStackTrace();
	    }
	}
	
	private String constructWebELVUrl(GKInstance diagram) throws Exception {
	    String url = AttributeEditConfig.getConfig().getDevWebELV();
	    GKInstance pathway = (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
	    GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
	    MySQLAdaptor dba = (MySQLAdaptor) diagram.getDbAdaptor();
	    return url + "#DB=" + dba.getDBName() + "&FOCUS_SPECIES_ID=" + species.getDBID() + "&FOCUS_PATHWAY_ID=" + pathway.getDBID();
	}
    
	public Action getRebuildProjectAction() {
	    if (rebuildProjectAction == null) {
	        rebuildProjectAction = new AbstractAction("Rebuild Project") {
	            public void actionPerformed(ActionEvent e) {
	                rebuildProject();
	            }
	        };
	    }
	    return rebuildProjectAction;
	}
	
	private void rebuildProject() {
	    // Need to create a new project
	    if (!curatorFrame.createNewProject(false))
	        return;
	    // Get a file name
	    JFileChooser fileChooser = getRtpjFileChooser();
	    fileChooser.setDialogTitle("Choose a project file using an old schema...");
	    int reply = fileChooser.showOpenDialog(curatorFrame);
	    if (reply == JFileChooser.APPROVE_OPTION) {
	        File file = fileChooser.getSelectedFile();
	        curatorFrame.getSystemProperties().setProperty("currentDir", 
                                                           file.getParent());
	        XMLFileQueryReader reader = new XMLFileQueryReader();
	        try {
	            reader.read(file);
	            List<Long> dbIds = reader.getNonShellDBIds();
	            Long defaultPerson = reader.getDefaultPersonId();
	            checkOutProject(dbIds, defaultPerson);
	        }
	        catch(Exception e) {
	            JOptionPane.showMessageDialog(curatorFrame,
	                                          "Cannot rebuild project from a project with old schema: " + e,
	                                          "Error in Rebuilding",
	                                          JOptionPane.ERROR_MESSAGE);
	            System.err.println("CuratorActionCollection.rebuildProject(): " + e);
	            e.printStackTrace();
	        }
	    }
	}
	
	private void checkOutProject(final List<Long> dbIds,
	                             final Long defaultPerson) throws Exception {
	    final MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(curatorFrame);
	    if (dba == null)
	        return;
	    // Need a progress dialog
	    // Check out database instances using a new thread to display GUIs
	    Thread t = new Thread() {
	        public void run() {
                try {
                    ProgressPane progressPane = new ProgressPane();
                    progressPane.setIndeterminate(true);
                    curatorFrame.setGlassPane(progressPane);
                    progressPane.setText("Query instances in DB...");
                    curatorFrame.getGlassPane().setVisible(true);
                    // Get a list of GKInstances based on dbIds
                    // Used for warning in case instances have been deleted in the database
                    List<Long> deleted = new ArrayList<Long>();
                    // Do a quick fetch
                    Collection fetched = dba.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject, 
                                                                      ReactomeJavaConstants.DB_ID,
                                                                      "=",
                                                                      dbIds);
                    List<GKInstance> dbInstances = new ArrayList<GKInstance>(fetched);
                    // Check if there is any deleted
                    boolean isFound = false;
                    for (Long dbId : dbIds) {
                        isFound = false;
                        for (GKInstance inst : dbInstances) {
                            if (inst.getDBID().equals(dbId)) {
                                isFound = true;
                                break;
                            }
                        }
                        if (!isFound)
                            deleted.add(dbId);
                    }
                    // Generate a warning message
                    if (deleted.size() > 0) {
                        StringBuilder message = new StringBuilder();
                        if (deleted.size() == 1) {
                            message.append("This instance has been deleted in the database: " + deleted.get(0));
                        }
                        else {
                            message.append("The following instances have been deleted in the database: \n");
                            int total = 0;
                            for (Iterator<Long> it = deleted.iterator(); it.hasNext();) {
                                message.append(it.next());
                                total ++;
                                if (it.hasNext()) {
                                    message.append(", ");
                                    if (total % 10 == 0) {
                                        // Ten ids in one line
                                        message.append("\n");
                                    }
                                }
                            }
                        }
                        JOptionPane.showMessageDialog(curatorFrame, 
                                                      message.toString(),
                                                      "Warning: Instance not in DB",
                                                      JOptionPane.WARNING_MESSAGE);
                    }
                    progressPane.setText("Loading attribute values...");
                    dba.loadInstanceAttributeValues(dbInstances);
                    progressPane.setText("Checking out instances...");
                    SynchronizationManager manager = SynchronizationManager.getManager();
	                // Set the default person
	                manager.setDefaultPerson(defaultPerson);
	                manager.checkOut(dbInstances, 
	                                 curatorFrame);
	            }
	            catch(Exception e) {
	                JOptionPane.showMessageDialog(curatorFrame,
	                                              "Error in checking out: " + e,
	                                              "Error in Checking-out",
	                                              JOptionPane.ERROR_MESSAGE);
	                System.err.println("CuratorActionCollection.checkoutProject(): " + e);
	                e.printStackTrace();
	            }
	            curatorFrame.getGlassPane().setVisible(false);
	        }
	    };
	    t.start();
	}
	
    public Action getProjectInfoAction() {
        if (projectInfoAction == null) {
            projectInfoAction = new AbstractAction("Project Info") {
                public void actionPerformed(ActionEvent e) {
                    showProjectInfo();
                }
            };
        }
        return projectInfoAction;
    }
    
    private void showProjectInfo() {
        // Want to display the default project person
        // and an optional project description
        ProjectInfoDialog dialog = new ProjectInfoDialog(curatorFrame);
        GKApplicationUtilities.center(dialog);
        dialog.setModal(true);
        dialog.setVisible(true);
    }
    
    public Action getImportFromMODReactomeAction() {
        if (importFromMODReactome == null) {
            importFromMODReactome = new AbstractAction("MOD Reactome") {
                public void actionPerformed(ActionEvent e) {
                    createNewProjectFromMOD();
                }
            };
        }
        return importFromMODReactome;
    }
    
    private void createNewProjectFromMOD() {
        // Need to create a new Project first
        if(!curatorFrame.createNewProject())
            return; // New Project creation is cancelled
        try {
            // Need to create a new MySQLAdaptor
            DBConnectionPane dbPane = new DBConnectionPane();
            Properties info = new Properties();
            dbPane.setValues(info);
            if (dbPane.showInDialog(curatorFrame)) {
                String dbName = info.getProperty("dbName");
                String dbHost = info.getProperty("dbHost");
                String dbUser = info.getProperty("dbUser");
                String dbPwd = info.getProperty("dbPwd");
                String dbPort = info.getProperty("dbPort");
                MySQLAdaptor dba = PersistenceManager.getManager().getMySQLAdaptor(dbHost, 
                                                                                   dbName,
                                                                                   dbUser, 
                                                                                   dbPwd, 
                                                                                   new Integer(dbPort));
                createNewProjectFromMOD(dba);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(curatorFrame, 
                                          "Error in generating a new project from a MOD Reactome.", 
                                          "Error", 
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void createNewProjectFromMOD(final MySQLAdaptor modDba) throws Exception {
        final CheckOutProgressDialog progressDialog = new CheckOutProgressDialog(curatorFrame,
                                                                                "Creating new project from MOD...");
        progressDialog.disableCancel();
        Thread t = new Thread() {
            public void run() {
                try {
                    yield(); // Make sure progDialog.setVisible is called first.
                    MODReactomeAnalyzer merger = new MODReactomeAnalyzer();
                    merger.setModReactome(modDba);
                    progressDialog.setText("Checking instances in the MOD database...");
                    merger.checkMODInstances();
                    progressDialog.setText("Creating a new project...");
                    merger.checkOutAsNewProject();
                    progressDialog.setText("Rebuilding the event hierarchy...");
                    curatorFrame.getEventView().rebuildTree();
                    progressDialog.setIsDone();
                }
                catch(Exception e) {
                    System.err.println("PantherPathwayImporter.doImport(): " + e);
                    e.printStackTrace();
                    progressDialog.setIsWrong();
                }
            }
        };
        t.start();
        progressDialog.setVisible(true);
    }
    
    public Action getCreateEwasFromRefPepSeqAction() {
        if (createEwasFromRefPepSeqAction == null) {
            createEwasFromRefPepSeqAction = new AbstractAction("Create EWAS from RefGeneProduct") {
                public void actionPerformed(ActionEvent e) {
                    createEwasFromRefPepSeq();
                }
            };
        }
        return createEwasFromRefPepSeqAction;
    }
    
    private void createEwasFromRefPepSeq() {
        java.util.List selection = getSelection(); 
        if (selection == null || selection.size() == 0)
            return; // Nothing to do
        // Get permission for auto-query cooridnates
        int reply = JOptionPane.showConfirmDialog(curatorFrame,
                                                  "The Curator Tool can fetch start and end coordinates for you from the UniProt web site.\n" +
                                                  "Do you want it to do this for you?\n" +
                                                  "Note: not all UniProt entries have coordinates.",
                                                  "Query Coordinates?",
                                                  JOptionPane.YES_NO_OPTION);
        boolean needCoordinates = true;
        if (reply != JOptionPane.YES_OPTION)
            needCoordinates = false;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            if (instance.isShell())
                continue; // Should not occur since this action should be disabled for shell instances
            if (!instance.getSchemClass().isa(ReactomeJavaConstants.ReferencePeptideSequence) &&
                !instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct)) // For new schema as of 2/12/09
                continue; // This action works for ReferencePeptideSequence only
            try {
                GKInstance ewas = fileAdaptor.createNewInstance(ReactomeJavaConstants.EntityWithAccessionedSequence);
                // Copy properties from ReferencePeptideSequence
                InstanceUtilities.copyAttributesFromRefPepSeqToEwas(ewas, instance);
                if (needCoordinates)
                    assignStartAndEndToEWAS(ewas, instance);
            }
            catch(Exception e) {
                System.err.println("CuratorActionCollection.createEwasFromRefPepSeq(): " + e);
                e.printStackTrace();
            }
        }
    }
    
    private void assignStartAndEndToEWAS(GKInstance ewas, GKInstance refPepSeq) throws Exception {
        // Query the coordinates quitely from the uniprot web site
        String identifier = (String) refPepSeq.getAttributeValue(ReactomeJavaConstants.identifier);
        if (identifier == null)
            return; // Cannot do anything
        ReferencePeptideSequenceAutoFiller filler = new ReferencePeptideSequenceAutoFiller();
        int[] startAndEnd = filler.fetchCoordinates(identifier,
                                                    curatorFrame);
        if (startAndEnd == null)
            return;
        if (startAndEnd[0] > -1) // Default is -1
            ewas.setAttributeValue(ReactomeJavaConstants.startCoordinate, new Integer(startAndEnd[0]));
        if (startAndEnd[1] > -1)
            ewas.setAttributeValue(ReactomeJavaConstants.endCoordinate, new Integer(startAndEnd[1]));
    }
    
    public Action getBatchEditAction() {
        if (batchEditAction == null) {
            batchEditAction = new AbstractAction("Edit in Batch",
                    createIcon("EditInBatch.gif")) {
                public void actionPerformed(ActionEvent e) {
                    editInstancesInBatch();
                }
            };
        }
        return batchEditAction;
    }
    
    private void editInstancesInBatch() {
        List selectedInstances = getSelection();
        // Have to have more than one Instance selected
        if (selectedInstances == null || selectedInstances.size() < 2)
            return;
        // Check if any shell instances are selected
        boolean hasShell = false;
        GKInstance instance = null;
        for (Iterator it = selectedInstances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            if (instance.isShell()) {
                hasShell = true;
                break;
            }
        }
        if (hasShell) {
            JOptionPane.showMessageDialog(curatorFrame,
                                          "Shell instance is selected. A shell instance cannot be edited." +
                                          "\nPlease download it first to edit.",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        MultipleInstanceEditDialog attDialog = new MultipleInstanceEditDialog(curatorFrame);
        attDialog.setInstancecs(selectedInstances);
        attDialog.setSize(600, 520);
        attDialog.setLocationRelativeTo(curatorFrame);
        attDialog.setVisible(true);
    }
    
    public Action getRequestNewGOTermAction() {
        if (requestNewGOTermAction == null) {
            requestNewGOTermAction = new AbstractAction("Request New Term") {
                public void actionPerformed(ActionEvent e) {
                    GOTermRequestHelper helper = new GOTermRequestHelper();
                    helper.requestNewTerm(curatorFrame);
                }
            };
        }
        return requestNewGOTermAction;
    }
    
    public Action getTrackGORequestAction() {
        if (trackGORequestAction == null) {
            trackGORequestAction = new AbstractAction("Track Request") {
                public void actionPerformed(ActionEvent e) {
                    GOTermRequestHelper helper = new GOTermRequestHelper();
                    helper.trackRequest(curatorFrame, curatorFrame.getSystemProperties());
                }
            };
        }
        return trackGORequestAction;
    }
	
	public Action getReportBugAction() {
		if (reportBugAction == null) {
			reportBugAction = new AbstractAction("Report Bugs",
					                             createIcon("bug.gif")) {
				public void actionPerformed(ActionEvent e) {
					try {
						BrowserLauncher.displayURL("http://brie8.cshl.edu/bugzilla", curatorFrame);
					}
					catch(Exception e1) {
						System.err.println("CuratorActionCollection.getReportBugAction(): " + e1);
						e1.printStackTrace();
						JOptionPane.showMessageDialog(curatorFrame,
								  	                  "Cannot display the bug report web page.",
													  "Error",
													  JOptionPane.ERROR_MESSAGE);
					}
				}
			};
			reportBugAction.putValue(Action.SHORT_DESCRIPTION, "Report bugs and require new features");
		}
		return reportBugAction;
	}
    
    public Action getImportExternalPathwayAction() {
        if (importExternalPathwayAction == null) {
            importExternalPathwayAction = new AbstractAction("Pathway from Other Databases") {
                public void actionPerformed(ActionEvent e) {
                    PantherPathwayImporter importer = new PantherPathwayImporter();
                    importer.doImport(curatorFrame);
                }
            };
        }
        return importExternalPathwayAction;
    }
    
    public Action getImportFromVer1PrjAction() {
	    if (importFromVer1PrjAction == null) {
	        importFromVer1PrjAction = new AbstractAction("Ver1.0 Project") {
	            public void actionPerformed(ActionEvent e) {
	                importFromVer1Project();
	            }
	        };
	    }
	    return importFromVer1PrjAction;
	}
	
	private void importFromVer1Project() {
	    // Get the folder holding the local repository
	    JFileChooser fileChooser = getRtpjFileChooser();
	    fileChooser.setDialogTitle("Please choose the directory holding the local repository...");
	    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    int reply = fileChooser.showOpenDialog(curatorFrame);
	    if (reply != JFileChooser.APPROVE_OPTION)
	    	return ; // Cancelled by the user.
	    File direcotry = fileChooser.getSelectedFile();
	    try {
	        FileAdaptor fileAdaptor = new FileAdaptor(direcotry.getAbsolutePath());
	        // Convert all instances into a String
	        GKSchema schema = (GKSchema) fileAdaptor.getSchema();
            Collection classes = schema.getClasses();
            GKSchemaClass cls = null;
            GKInstance instance = null;
            StringBuffer buffer = new StringBuffer();
            buffer.append(XMLFileAdaptor.XML_HEADER);
            buffer.append(XMLFileAdaptor.LINE_END);
            buffer.append("<reactome>\n");
            for (Iterator it = classes.iterator(); it.hasNext();) {
                cls = (GKSchemaClass) it.next();
                Collection instances = fileAdaptor.fetchInstancesByClass(cls);
                if (instances == null || instances.size() == 0)
                    continue;
                buffer.append("<");
                buffer.append(cls.getName());
                buffer.append(">\n");
                for (Iterator it1 = instances.iterator(); it1.hasNext();) {
                    instance = (GKInstance) it1.next();
                    fileAdaptor.loadInstanceAttributes(instance);
                    buffer.append(fileAdaptor.convertInstanceToString(instance));
                }
                buffer.append("</");
                buffer.append(cls.getName());
                buffer.append(">\n");
            }
            
            buffer.append("</reactome>\n");
            // Save into a temp file
            File tmpFile = File.createTempFile(direcotry.getName(), ".xml");
            tmpFile.deleteOnExit(); // Mark it to be deleted when the application is gone
            FileWriter writer = new FileWriter(tmpFile);
            BufferedWriter bWriter = new BufferedWriter(writer);
            bWriter.write(buffer.toString());
            bWriter.close();
            writer.close();
            curatorFrame.open(tmpFile);
            // Have to make source null in the XMLFileAdaptor
            XMLFileAdaptor xmlFileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            xmlFileAdaptor.setSource(null, false);
            curatorFrame.setTitle("Untitled - " + GKCuratorFrame.CURATOR_TOOL_NAME);
            getSaveProjectAction().setEnabled(true); // Need to save
	    }
	    catch(Exception e) {
	        System.err.println("CuratorActionCollection.importFromVer1Project(): " + e);
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(curatorFrame,
	                                      "Cannot import ver1.0 project: " + e.toString(),
	                                      "Error in Importing",
	                                      JOptionPane.ERROR_MESSAGE);
	    }
	}
	
	public Action getSaveAsAction() {
		if (saveAsAction == null) {
			saveAsAction = new AbstractAction("Save As...",
			                                  createIcon("SaveAs16.gif")) {
				public void actionPerformed(ActionEvent e) {
					saveAs();
				}
			};
			saveAsAction.putValue(Action.SHORT_DESCRIPTION, "Save the project as another one");
		}
		return saveAsAction;
	}
    
    public Action getNewProjectAction() {
        if (newProjectAction == null) {
            newProjectAction = new AbstractAction("New", createIcon("New16.gif")) {
                public void actionPerformed(ActionEvent e) {
                	curatorFrame.createNewProject();
                }
            };
            newProjectAction.putValue(Action.SHORT_DESCRIPTION, "Create a new project");
        }
        return newProjectAction;
    }
    
    public Action getOpenAction() {
        if (openAction == null) {
            openAction = new AbstractAction("Open", createIcon("Open16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fileChooser = getRtpjFileChooser();
                    fileChooser.setDialogTitle("Please choose a file to open...");
                    int reply = fileChooser.showOpenDialog(curatorFrame);
                    if (reply != JFileChooser.APPROVE_OPTION)
                        return;
                    File selectedFile = fileChooser.getSelectedFile();
                    // Keep track the last directory
                    curatorFrame.getSystemProperties().setProperty("currentDir", 
                                                                   selectedFile.getParent());
                    curatorFrame.open(selectedFile);
                }
            };
            openAction.putValue(Action.SHORT_DESCRIPTION, "Open a local repository file");
        }
        return openAction;
    }
	
	public Action getMatchInstancesAction() {
	    if (matchInstanceAction == null) {
	        matchInstanceAction = new AbstractAction("Match Instance in DB...",
	                                                  createIcon("MatchInstanceInDB.gif")) {
	            public void actionPerformed(ActionEvent e) {
	                java.util.List selection = getSelection();
	                if (selection.size() != 1)
	                    return;
	                GKInstance instance = (GKInstance) selection.get(0);
	                if (instance.getDBID().longValue() < 0)
	                    SynchronizationManager.getManager().matchInstanceInDB(instance, curatorFrame);
	            }
	        };
	        matchInstanceAction.putValue(Action.SHORT_DESCRIPTION, "Search matched instance in the database");
	    }
	    return matchInstanceAction;
	}
	
	public Action getMergeInstanceAction() {
	    if (mergeInstanceAction == null) {
	        mergeInstanceAction = new AbstractAction("Merge Two Instances",
	                                                 createIcon("MergeInstances.gif")) {
	            public void actionPerformed(ActionEvent e) {
	                mergeInstances();
	            }
	        };
	        mergeInstanceAction.putValue(Action.SHORT_DESCRIPTION,
	                                     "Merge two instances"); 
	    }
	    return mergeInstanceAction;
	}
	
	private void mergeInstances() {
		List<GKInstance> selectedInstances = curatorFrame.getSchemaView().getSelection();
		InstanceMerger merger = new InstanceMerger();
		merger.mergeInstances(selectedInstances, curatorFrame);
	}
	
	public Action getSwitchTypeAction() {
		if (switchTypeAction == null) {
			switchTypeAction = new AbstractAction("Switch Type", 
			                                      createIcon("TypeSwitch.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List instances = curatorFrame.getSchemaView().getSelection();
					if (instances == null || instances.size() == 0)
						return;
					// The contents in instances list might changed. Clone the list.
					switchType(new ArrayList(instances));
				}
			};
			switchTypeAction.putValue(Action.SHORT_DESCRIPTION, "Switch class type");
		}
		return switchTypeAction;
	}
	
	private void switchType(final java.util.List instances) {
		// Generate a warning for shell instances. A shell instance cannot be converted.
		java.util.List shellInstances = new ArrayList();
		for (Iterator it = instances.iterator(); it.hasNext();) {
			GKInstance instance = (GKInstance) it.next();
			if (instance.isShell())
				shellInstances.add(instance);
		}
		if (shellInstances.size() > 0) {
			StringBuffer buffer = new StringBuffer();
			if (shellInstances.size() == 1) {
				GKInstance instance = (GKInstance) shellInstances.get(0);
				buffer.append("Instance \"");
				buffer.append(instance.getDisplayName());
				buffer.append("\" is a shell instance. The type of a shell instance cannot be changed.");
			}
			else {
				buffer.append("The following instances are shell instances. The type of a shell instance cannot be changed.\n");
				for (Iterator it = shellInstances.iterator(); it.hasNext();) {
					GKInstance instance = (GKInstance) it.next();
					buffer.append(instance.getDisplayName());
					if (it.hasNext())
						buffer.append("\n");
				}
			}
			JOptionPane.showMessageDialog(curatorFrame,
			                              buffer.toString(),
			                              "Warning",
			                              JOptionPane.WARNING_MESSAGE);
			instances.removeAll(shellInstances);
			if (instances.size() == 0)
				return;
		}
		// Choose new SchemaClass for instances
		JDialog dialog = new JDialog(curatorFrame, "Choose a SchemaClass for Instances");
		final SchemaDisplayPane schemaPane = new SchemaDisplayPane();
		schemaPane.setSchema(PersistenceManager.getManager().getActiveFileAdaptor().getSchema());
		schemaPane.setSearchPaneVisible(false);
		dialog.getContentPane().add(schemaPane, BorderLayout.CENTER);	
		schemaPane.setTitle("Please choose a SchemaClass for instances:");
		// Add a control pane
		JPanel controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
		okBtn.setMnemonic('O');
		cancelBtn.setMnemonic('C');
		okBtn.setDefaultCapable(true);
		dialog.getRootPane().setDefaultButton(okBtn);
		// Add actionListeners to buttons
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton cancelBtn = (JButton) e.getSource();
				JDialog dialog = (JDialog) SwingUtilities.getRoot(cancelBtn);
				dialog.dispose();
			}
		});
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JButton okBtn = (JButton) e.getSource();
				JDialog dialog = (JDialog) SwingUtilities.getRoot(okBtn);
				GKSchemaClass cls = schemaPane.getSelectedClass();
				if (cls.isAbstract()) {
					JOptionPane.showMessageDialog(dialog,
					                              "\"" + cls.getName() + "\" is an abstract class. An abstract class cannot " +					                              "have instances.\nPlease select a non-abstract class.",
					                              "Error",
					                              JOptionPane.ERROR_MESSAGE);
					return;
				}
				dialog.dispose();
				switchType(instances, cls);
			}
		});
		controlPane.add(okBtn);
		controlPane.add(cancelBtn);
		dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
		// Dialog dialog
		dialog.setSize(400, 500);
		dialog.setModal(true);
		dialog.setLocationRelativeTo(curatorFrame);
		dialog.setVisible(true);		
	}
	
	private void switchType(java.util.List instances, GKSchemaClass newCls) {
		GKInstance instance = null;
		XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		// Remove those instances that cannot be switched because they are referred 
		// somewhere.
		java.util.List invalidList = new ArrayList();
		try {
            for (Iterator it = instances.iterator(); it.hasNext();) {
                instance = (GKInstance) it.next();
                List referrers = adaptor.getReferers(instance);
                for (Iterator it1 = referrers.iterator(); it1.hasNext();) {
                    GKInstance referrer = (GKInstance) it1.next();
                    boolean isInValid = false;
                    // Check where the instance is in.
                    for (Iterator it2 = referrer.getSchemaAttributes().iterator(); it2.hasNext();) {
                        GKSchemaAttribute att = (GKSchemaAttribute) it2.next();
                        if (!att.isInstanceTypeAttribute() ||
                            att.isValidValue(referrer))
                            continue;
                        java.util.List values = referrer.getAttributeValuesList(att);
                        if (values == null || values.size() == 0)
                            continue;
                        if (values.contains(instance) &&
                            !att.isValidClass(newCls)) {
                            isInValid = true;
                            break;
                        }
                    }
                    if (isInValid) {
                        invalidList.add(instance);
                        break;
                    }
                }
            }
            if (invalidList.size() > 0) {
                StringBuffer msg = new StringBuffer();
                if (invalidList.size() == 1) {
                    msg.append("The selected instance \"");
                    GKInstance instance1 = (GKInstance) invalidList.get(0);
                    msg.append(instance1.getDisplayName());
                    msg.append("\" is referred by other instances. The selected type is \nnot a valid type for the referrers");
                    msg.append(" and cannot be switched to.");
                }
                else {
                    msg.append("The following instances are referred by other instances. The selected type is not valid for these referrers");
                    msg.append(" and cannot be switched to.\n");
                    for (Iterator it = invalidList.iterator(); it.hasNext();) {
                        GKInstance tmp = (GKInstance) it.next();
                        msg.append(tmp.getDisplayName());
                        if (it.hasNext())
                            msg.append("\n");
                    }
                }
                int reply = JOptionPane.showConfirmDialog(curatorFrame, 
                        								  msg.toString(), 
                        								  "Error in Switching Type",
                        								  JOptionPane.OK_CANCEL_OPTION);
                if (reply == JOptionPane.CANCEL_OPTION)
                    return;
                instances.removeAll(invalidList);
            }
            for (Iterator it = instances.iterator(); it.hasNext();) {
            	instance = (GKInstance) it.next();
            	adaptor.switchType(instance, newCls);
            }
        }
        catch (Exception e) {
            System.err.println("CuratorActionCollection.switchType(): " + e);
            e.printStackTrace();
        }
	}
	
	public Action getExportAsProtegePrgAction() {
		if (exportAsProtegePrjAction == null) {
			exportAsProtegePrjAction = new AbstractAction("Protege Project") {
				public void actionPerformed(ActionEvent e) {
					ProtegeExporter exporter = new ProtegeExporter();
					exporter.exportAsProject(curatorFrame);
				}	
			};
		}
		return exportAsProtegePrjAction;
	}
	
	public Action getExportAsProtegePinAction() {
		if (exportAsProtegePinAction == null) {
			exportAsProtegePinAction = new AbstractAction("Protege Pins File") {
				public void actionPerformed(ActionEvent e) {
					ProtegeExporter exporter = new ProtegeExporter();
					exporter.exportAsPinsFile(curatorFrame);
				}
			};
		}
		return exportAsProtegePinAction;
	}
	
	public Action getAddBookmarkAction() {
		if (addBookmarkAction == null) {
			addBookmarkAction = new AbstractAction("Add Bookmark",
			                                       createIcon("Bookmark.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List selection = getSelection();
					SchemaViewPane schemaPane = curatorFrame.getSchemaView();
					for (Iterator it = selection.iterator(); it.hasNext();) {
						GKInstance instance = (GKInstance) it.next();
						schemaPane.addBookmark(instance);
					}
				}
			};
		}
		return addBookmarkAction;
	}
	
	public Action getImportFromAuthoringToolAction() {
		if (importFromAuthoringToolAction == null) {
			importFromAuthoringToolAction = new AbstractAction("Author Tool File") {
				public void actionPerformed(ActionEvent e) {
                    // Create an empty project first to avoid 
                    // unncessary problems.
                    if(!curatorFrame.createNewProject())
                        return;
					String defaultDir = curatorFrame.getSystemProperties().getProperty("defaultDir");
					ImportAuthoringFileEngine engine = new ImportAuthoringFileEngine();
					File file = engine.importFile(defaultDir, curatorFrame, false);
                    if (file != null)
                        curatorFrame.getSystemProperties().setProperty("defaultDir", 
                                                                       file.getParent());
                    // Need to rebuild the tree
                    curatorFrame.getEventView().rebuildTree();
				}
			};
		}
		return importFromAuthoringToolAction;
	}
    
	public Action getImportFromAuthorTool2Action() {
	    Action importFromAuthorTool2Action = new AbstractAction("Author Tool Ver 2 File") {
	        public void actionPerformed(ActionEvent e) {
	            // Create an empty project first to avoid 
	            // unncessary problems.
	            if(!curatorFrame.createNewProject())
	                return;
	            String defaultDir = curatorFrame.getSystemProperties().getProperty("defaultDir");
	            ImportAuthoringFileEngine engine = new ImportAuthoringFileEngine();
	            File file = engine.importFile(defaultDir, curatorFrame, true);
	            if (file != null)
	                curatorFrame.getSystemProperties().setProperty("defaultDir", 
	                                                               file.getParent());
	            // Need to rebuild the tree
	            curatorFrame.getEventView().rebuildTree();
	        }
	    };
	    return importFromAuthorTool2Action;
	}
	
	public Action getExportToAuthoringToolAction() {
		if (exportToAuthoringToolAction == null) {
			exportToAuthoringToolAction = new AbstractAction("Export to Author Tool File") {
				public void actionPerformed(ActionEvent e) {
                    if (!(curatorFrame.getFocusedComponent() instanceof EventCentricViewPane))
                        return ;
                    java.util.List selection = getSelection();
					exportEventToAuthorToolFile(selection, curatorFrame);
				}
			};
		}
		return exportToAuthoringToolAction;
	}
    
    private Action createExportToAuthorToolActionForDB() {
        Action action = new AbstractAction("Export to Author Tool File") {
            public void actionPerformed(ActionEvent e) {
                JFrame eventFrame = FrameManager.getManager().getEventViewFrame();
                Component[] comps = eventFrame.getContentPane().getComponents();
                for (int i = 0; i < comps.length; i++) {
                    if (comps[i] instanceof EventCentricViewPane) {
                        List list = ((EventCentricViewPane)comps[i]).getSelection();
                        exportEventToAuthorToolFile(list, eventFrame);
                        break;
                    }
                }
            }
        };
        return action;
    }
    
    private Action buildProjectForDBAction() {
        Action action = new AbstractAction("Build Project") {
            public void actionPerformed(ActionEvent e) {
                JFrame eventFrame = FrameManager.getManager().getEventViewFrame();
                Component[] comps = eventFrame.getContentPane().getComponents();
                for (int i = 0; i < comps.length; i++) {
                    if (comps[i] instanceof EventCentricViewPane) {
                        List<?> list = ((EventCentricViewPane)comps[i]).getSelection();
                        if (list.size() == 0)
                            break;
                        Set<GKInstance> events = new HashSet<GKInstance>();
                        for (Iterator<?> it = list.iterator(); it.hasNext();) {
                            GKInstance event = (GKInstance) it.next();
                            events.add(event);
                        }
                        buildProject(events, eventFrame);
                        break;
                    }
                }
            }
        };
        return action;
    }
    
    /**
     * This is a quick way to build a project for a selected event. All events in an event
     * hierarchy rooted at the selected event will be in the project plus any necessary references
     * as defined in the checkOut() method. This method uses an service deployed at the server
     * side.
     * @param event
     */
    private void buildProject(Set<GKInstance> events,
                              final JFrame parentFrame) {
        // Need to create a new project
        if (!curatorFrame.createNewProject(false))
            return;
        // Just in case the focus has been switched
        parentFrame.setState(JFrame.NORMAL);
        parentFrame.toFront();
        // Check the connection information
        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor();
        String dbHost = dba.getDBHost();
        // Check if there is a service available
        String url = AttributeEditConfig.getConfig().getPDUrl();
        // Get the defined host
        if (!url.startsWith("http://" + dbHost)) {
            JOptionPane.showMessageDialog(parentFrame,
                                          "The hosts used for the database and the project build service are not the same.\n" +
                                          "Please use \"Check Out\" instead.",
                                          "Error in Building Project",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        String wsUser = null;
        String wsKey = null;
        try {
            String[] wsInfo = new WSInfoHelper().getWSInfo(parentFrame);
            if (wsInfo == null) {
                JOptionPane.showMessageDialog(parentFrame,
                                              "No connecting information to the server-side program is provided!",
                                              "Error in Building Project",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            wsUser = wsInfo[0];
            wsKey = wsInfo[1];
        }
        catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder builder = new StringBuilder();
        for (Iterator<GKInstance> it = events.iterator(); it.hasNext();) {
            GKInstance event = it.next();
            builder.append(event.getDBID());
            if (it.hasNext())
                builder.append(",");
        }
        url = url + "?action=ProjectGenerator&user=" + wsUser + 
                "&key=" + wsKey + "&dbName=" + dba.getDBName() + 
                "&eventId=" + builder.toString();
        final String tUrl = url;
        Thread t = new Thread() {
            public void run() {
                ProgressPane progressPane = new ProgressPane();
                progressPane.setText("Building project. Please wait...");
                progressPane.setIndeterminate(true);
                parentFrame.setGlassPane(progressPane);
                parentFrame.getGlassPane().setVisible(true);
                try {
                    URL url = new URL(tUrl);
                    InputStream is = url.openStream();
                    // The following is for testing only
//                    InputStreamReader reader = new InputStreamReader(is);
//                    BufferedReader bReader = new BufferedReader(reader);
//                    String line = null;
//                    while ((line = bReader.readLine()) != null) {
//                        System.out.println(line);
//                    }
                    XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
                    fileAdaptor.load(is);
                    curatorFrame.prepareForNewProject(fileAdaptor);
                    // Don't forget to flag it as dirty so that it can be saved.
                    fileAdaptor.markAsDirty();
                }
                catch(Exception e) {
                    JOptionPane.showMessageDialog(parentFrame,
                                                  "Error in building project: please see output for detailed errors.\n" +
                                                  "You may enter wrong user name and/or key.",
                                                  "Error in Building Project",
                                                  JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    // Remove the key so that the user can re-enter
                    GKApplicationUtilities.getApplicationProperties().remove("wsKey");
                }
                parentFrame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
	
	private void exportEventToAuthorToolFile(List selection,
                                             Component parentComp) {
        Project project = convertToAuthorToolProject(selection);
        if (project == null)
            return;
        // Cannot call getRtpjFileChooser, which is used for rtpj only
        JFileChooser fileChooser = new JFileChooser();
		// Set the default directory
		String defaultDir = curatorFrame.getSystemProperties().getProperty("defaultDir");
		if (defaultDir != null) {
			File dir = new File(defaultDir);
			if (dir.exists() && dir.isDirectory())
				fileChooser.setCurrentDirectory(dir);
		}
		GKFileFilter gkFilter = new GKFileFilter();
		fileChooser.addChoosableFileFilter(gkFilter);
		fileChooser.addChoosableFileFilter(new XMLFileFilter());
		fileChooser.setFileFilter(gkFilter);
		fileChooser.setDialogTitle("Choose a file for exporting event...");
		File file = GKApplicationUtilities.chooseSaveFile(fileChooser, parentComp);
		if (file == null)
			return;
		// Recorde the default parent directory
		File parentFile = file.getParentFile();
		Properties prop = curatorFrame.getSystemProperties();
		prop.setProperty("defaultDir", parentFile.getAbsolutePath());
		try {
            GKBWriter writer = new GKBWriter();
			writer.save(project, file.getAbsolutePath());
		}
		catch (Exception e) {
			System.err.println("CuratorActionCollection.exportEventToAuthoringToolFile(): " + e);
			e.printStackTrace();
			JOptionPane.showMessageDialog(
				parentComp,
				"Cannot export the selected event: \n" + e.getMessage(),
				"Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public Action getOpenInAuthoringToolAction() {
		if (openInAuthoringToolAction == null) {
			openInAuthoringToolAction = new AbstractAction("Open in Author Tool") {
				public void actionPerformed(ActionEvent e) {
				    if (!(curatorFrame.getFocusedComponent() instanceof EventCentricViewPane))
                        return ;
                    java.util.List selection = getSelection();
					openEventInAuthorTool(selection);
				}
			};
		}
		return openInAuthoringToolAction;
	}
    
    private Action createOpenInAuthorToolActionForDB() {
        Action action = new AbstractAction("Open in Author Tool") {
            public void actionPerformed(ActionEvent e) {
                JFrame eventFrame = FrameManager.getManager().getEventViewFrame();
                Component[] comps = eventFrame.getContentPane().getComponents();
                for (int i = 0; i < comps.length; i++) {
                    if (comps[i] instanceof EventCentricViewPane) {
                        List list = ((EventCentricViewPane)comps[i]).getSelection();
                        openDbEventsInAuthorTool(list);
                        break;
                    }
                }
            }
        };
        return action;
    }
    
    private void openDbEventsInAuthorTool(final List events) {
        Thread t = new Thread() {
            public void run() {
                JFrame frame = FrameManager.getManager().getEventViewFrame();
                ProgressPane progressPane = new ProgressPane();
                progressPane.setIndeterminate(true);
                frame.setGlassPane(progressPane);
                try {
                    progressPane.setText("Loading Events...");
                    frame.getGlassPane().setVisible(true);
                    Set<GKInstance> reactions = new HashSet<GKInstance>();
                    Set<GKInstance> current = new HashSet<GKInstance>(events);
                    Set<GKInstance> next = new HashSet<GKInstance>();
                    while (current.size() > 0) {
                        for (GKInstance tmp : current) {
                            if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
                                List list = tmp.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
                                if (list != null)
                                    next.addAll(list);
                            }
                            else if (tmp.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                                reactions.add(tmp);
                            }
                        }
                        current.clear();
                        current.addAll(next);
                        next.clear();
                    }
                    // Load inputs
                    MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor();
                    progressPane.setText("Loading Reaction attributes...");
                    dba.loadInstanceAttributeValues(reactions);
                    // Load PhysicaleEntities
                    progressPane.setText("Loading PhysicalEntities...");
                    Set<GKInstance> pes = new HashSet<GKInstance>();
                    for (GKInstance rxt : reactions) {
                        Set set = InstanceUtilities.getReactionParticipants(rxt);
                        pes.addAll(set);
                    }
                    progressPane.setText("Loading PhysicalEntity attributes...");
                    dba.loadInstanceAttributeValues(pes);
                    // Need to load 
                    progressPane.setText("Converting to Author Tool project...");
                    openEventInAuthorTool(events);
                }
                catch(Exception e) {
                    System.err.println("CuratorActionCollection.preloadAttributes(): " + e);
                    e.printStackTrace();
                }
                frame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
	
	private GKInstance createVirtualContainer(List events) {
	    if (events == null || events.size() == 0)
	        return null;
	    GKInstance firstEvent = (GKInstance) events.get(0);
	    if (events.size() == 1)
	        return firstEvent;
	    // Get a name
	    String name = JOptionPane.showInputDialog(curatorFrame, 
	                                              "There are more than one event selected. A container is needed " +
	                                              "\nto hold them. Please input a name for this container.",
	                                              "Input Container Name",
	                                              JOptionPane.INFORMATION_MESSAGE);
	    if (name == null || name.length() == 0)
	        return null;
	    GKInstance container = new GKInstance();
	    container.setDisplayName(name);
	    container.setIsInflated(true);
	    // Use Pathway
	    SchemaClass cls = firstEvent.getDbAdaptor().getSchema().getClassByName(ReactomeJavaConstants.Pathway);
	    container.setSchemaClass(cls);
	    try {
            container.setAttributeValue(ReactomeJavaConstants.hasEvent, 
                                        new ArrayList(events));
            return container;
        }
        catch (Exception e) {
            System.err.println("CuratorActionCollection.createVirtualContainer(): " + e);
            e.printStackTrace();
        }
	    return null;
	}
	
	private void openEventInAuthorTool(List selection) {
	    if (authorFrame == null) 
	        authorFrame = new GKEditorFrame(true);
	    GKEditorManager editorManager = authorFrame.getEditorManager();
	    if (!editorManager.checkOpenedProject())
	        return; // Check if anything should be saved
	    Project project = convertToAuthorToolProject(selection);
	    if (project == null)
	        return;
	    editorManager.open(project);
	    // Don't need to set next id. It should be taken care of already
	    // during initialization of Renderable.
	    authorFrame.enableSaveAction(true); // Enable save
	    authorFrame.setVisible(true);
	    authorFrame.addWindowListener(new WindowAdapter() {
	        public void windowClosed(WindowEvent e) {
	            // Don't keep it so that each time a new
	            // GKEditorFrame can be re-initialized to keep
	            // all setting correct.
	            authorFrame = null;
	        }
	    });
	}
	
    private Project convertToAuthorToolProject(List selection) {
        GKInstance topEvent = createVirtualContainer(selection);
        if (topEvent == null)
            return null;
        // Only pathways can be opened in the authoring tool
        if (topEvent.getSchemClass().isa("Reaction")) {
            JOptionPane.showMessageDialog(curatorFrame,
                                          "Reaction cannot be converted to an author tool project.\n" +
                                          "Please choose a pathway containing this reaction.",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return null;                               
        }
        try {
            CuratorToolToAuthorToolConverter converter = new CuratorToolToAuthorToolConverter();
            Project project = converter.convert(topEvent, curatorFrame);
            return project;
        }
        catch(Exception e) {
            // Don't call e.getMessage(), which may returns null as in NullException.
            String message = e.toString();
            // Don't print too long message. Sometime from SQL exception.
            if (message.length() > 250)
                message = message.substring(0, 250); // Don't use 250 in case the length is 250.
            JOptionPane.showMessageDialog(curatorFrame,
                                          "Cannot convert the selected event to an author tool project: \n" + message,
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
            System.err.println("CuratorActionCollection.convertToAuthorToolProject(): " + e);
            e.printStackTrace();
        }
        return null;
    }
	
	public Action getVisualizationAction() {
		if (visualizationAction == null) {
			visualizationAction = new AbstractAction("Pathway Visualization") {
				JFrame vFrame = null;
				public void actionPerformed(ActionEvent e) {
					if (vFrame != null) {
						vFrame.setState(JFrame.NORMAL);
						vFrame.toFront();
						return;
					}
					final MySQLAdaptor dba = getMySQLAdaptor();
					if (dba == null) {
						JOptionPane.showMessageDialog(curatorFrame, 
											          "Cannot connect to the database.",
											          "Error",
											          JOptionPane.ERROR_MESSAGE);
						return;					          
					}
					Thread t = new Thread() {
						public void run() {
							try {
								GKVisualizationPane att = new GKVisualizationPane(dba);
                                  String coorEnabledProp = curatorFrame.getSystemProperties().getProperty(GKVisualizationPane.COOR_WRITE_ENABLE_KEY);
                                  boolean isEnabled = false;
                                  if (coorEnabledProp != null && coorEnabledProp.equals("true"))
                                      isEnabled = true;
                                  att.setCoordinatesWritable(isEnabled);
								vFrame = att.getFrame();
								GKApplicationUtilities.center(vFrame);
								vFrame.setIconImage(curatorFrame.getIconImage());
								vFrame.addWindowListener(new WindowAdapter() {
									public void windowClosing(WindowEvent e) {
										vFrame = null;
									}
								});
								vFrame.setVisible(true);
							}
							catch (Exception e1) {
								System.err.println("CuratorActionCollection.getVisualizationAction(): " + e1);
								e1.printStackTrace();
								JOptionPane.showMessageDialog(
									curatorFrame,
									"Cannot fetch the top level pathways.",
									"Error",
									JOptionPane.ERROR_MESSAGE);
							}
						}
					};
					t.start();
				}
			};
		}
		return visualizationAction;
	}
	
	public Action getOptionAction() {
		if (optionAction == null) {
			optionAction = new AbstractAction("Options") {
				public void actionPerformed(ActionEvent e) {
					CuratorOptionDialog optionDialog = new CuratorOptionDialog(curatorFrame);
					optionDialog.setVisible(true);
				}
			};
		}
		return optionAction;
	}
	
	public Action getViewInstanceAction() {
		if (viewInstanceAction == null) {
			viewInstanceAction = new AbstractAction("View Instance",
			                                        createIcon("ViewInstance.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List selection = getSelection();
					if (selection.size() == 0)
						return;
					for (Iterator it = selection.iterator(); it.hasNext();) {
						GKInstance instance = (GKInstance) it.next();
						if (instance.isShell()) {
							FrameManager.getManager().showShellInstance(instance, 
							                                            curatorFrame.getFocusedComponent());
						}
						else {
						    //This action can be applied to the local instance only.
							// So it should be true except for a shell instance.
							FrameManager.getManager().showInstance(instance, true);
						}
					}
				}
			};
			viewInstanceAction.putValue(Action.SHORT_DESCRIPTION, "View selected instance(s)");
		}
		return viewInstanceAction;
	}
	
	public Action getAboutAction() {
		if (aboutAction == null) {
			aboutAction = new AbstractAction("About",
											 createIcon("About16.gif")) {
				public void actionPerformed(ActionEvent e) {
					AboutGKPane gkPane = new AboutGKPane();
					gkPane.setApplicationTitle(GKCuratorFrame.CURATOR_TOOL_NAME);
					gkPane.setBuildNumber(GKCuratorFrame.BUILD_NUMBER);
					gkPane.setVersion(GKCuratorFrame.VERSION);
					gkPane.displayInDialog(curatorFrame);
				}
			};
			aboutAction.putValue(Action.SHORT_DESCRIPTION, "About GK");
		}
		return aboutAction;
	}

	public Action getHelpAction() {
		if (helpAction == null) {
			helpAction = new AbstractAction("Curator Guide",
			                                createIcon("Help16.gif")) {
				public void actionPerformed(ActionEvent e) {
					try {
//						AuthorToolAppletUtilities.displayHelp("CuratorToolManul.html",
//                                                              curatorFrame);
					    // As of Jan 31, 2011, the help file points to the wiki curator guide.
					    BrowserLauncher.displayURL("http://wiki.reactome.org/index.php/New_Reactome_Curator_Guide",
					                               curatorFrame);
					}
					catch(Exception e1) {
						System.err.println("CuratorActionCollection.getHelpAction(): " + e1);
						e1.printStackTrace();
						JOptionPane.showMessageDialog(curatorFrame,
													  "Cannot display the help file.",
													  "Error",
													  JOptionPane.ERROR_MESSAGE);
					}
				}
			};
			helpAction.putValue(Action.SHORT_DESCRIPTION, "Show help");
		}
		return helpAction;
	}
	
	public Action getCloneInstanceAction() {
		if (cloneInstanceAction == null) {
			cloneInstanceAction = new AbstractAction("Clone Instance",
			                                         createIcon("CloneInstance.gif")) {
				public void actionPerformed(ActionEvent e) {
					cloneInstances();
				}
			};
			cloneInstanceAction.putValue(Action.SHORT_DESCRIPTION, "Clone selected instance(s)");
		}
		return cloneInstanceAction;
	}
	
	private void cloneInstances() {
	    InstanceCloneHelper cloneHelper = new InstanceCloneHelper();
		java.util.List selection = getSelection(); 
		List<GKInstance> newInstances = cloneHelper.cloneInstances(selection, 
		                                                           curatorFrame.getFocusedComponent());
		if (newInstances != null && newInstances.size() > 0)
		    setSelection(newInstances);
	}
	
	public Action getViewSchemaAction() {
		if (viewSchemaAction == null) {
			viewSchemaAction = new AbstractAction("View Definition",
			                                      createIcon("ViewSchema.gif")) {
				public void actionPerformed(ActionEvent e) {
					// Get the SchemaClass
					SchemaClass schemaClass = curatorFrame.getSchemaView().getSchemaPane().getSelectedClass();
					if (schemaClass == null)
						return;
					SchemaClassDefinitionDialog dialog = new SchemaClassDefinitionDialog(curatorFrame);
					dialog.setSchemaClass(schemaClass);
					dialog.setVisible(true);
				}
			};
		}
		return viewSchemaAction;
	}
	
	public Action getSynchronizeDBAction() {
		if (synchronizeDBAction == null) {
			synchronizeDBAction = new AbstractAction("Synchronize with DB...") {
				public void actionPerformed(ActionEvent e) {
					synchronizeWithDB();
				}
			};
		}
		return synchronizeDBAction;
	}
	
	/**
	 * A helper method to save changes
	 * @param fileAdaptor
	 * @return true for saving the changes, while false for an unsuccessful saving. 
	 * An unsuccessful saving might result from cancelling or throwing an exception.
	 */
	private boolean saveChanges(XMLFileAdaptor fileAdaptor) {
		// Make sure everything is changed
		if (fileAdaptor.isDirty()) {
			int reply = JOptionPane.showConfirmDialog(curatorFrame,
													  "You have to save changes first before doing synchronization.\n" + 
													  "Do you want to save changes and then do synchronization?",
													  "Save Changes?",
													  JOptionPane.OK_CANCEL_OPTION);
			if (reply == JOptionPane.CANCEL_OPTION)
				return false;
			try {
				save();
				return true;
			}
			catch(Exception e) {
				JOptionPane.showMessageDialog(curatorFrame,
											  "Cannot save changes:" + e.getMessage(),
											  "Error in Saving",
											  JOptionPane.ERROR_MESSAGE);
				System.err.println("CuratorActionCollection.synchronizeWithDB(): " + e);
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	private void synchronizeWithDB() {
		XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		// Make sure everything is changed
		if (!saveChanges(fileAdaptor))
			return;
		MySQLAdaptor dbAdaptor = getMySQLAdaptor();
		if (dbAdaptor == null) {
			JOptionPane.showMessageDialog(curatorFrame,
			                              "Cannot connect to the database.",
			                              "Error in DB Connection",
			                              JOptionPane.ERROR_MESSAGE);
			return;                             
		}
		synchronizeWithDB(fileAdaptor, dbAdaptor, null, null);
	}
	
	private void synchronizeWithDB(XMLFileAdaptor fileAdaptor, MySQLAdaptor dbAdaptor, List uncheckableList, String title) {
		SynchronizationDialog syncPane = new SynchronizationDialog(curatorFrame);
		GKSchemaClass selectedCls = curatorFrame.getSchemaView().getSelectedClass();
		syncPane.setSelectedClass(selectedCls);
		syncPane.setAdaptors(fileAdaptor, dbAdaptor, uncheckableList);
		if (title!=null)
			syncPane.setTitle(title);
		if (syncPane.isCancelled())
			return;
		if (syncPane.isSame()) {
			JOptionPane.showMessageDialog(curatorFrame,
			                              "There is no difference between the local repository and the database repository",
			                              "No Difference",
			                              JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		syncPane.setVisible(true);
	}
	
	public Action getCheckInAction() {
		if (checkInAction == null) {
			checkInAction = new AbstractAction("Check In",
			                                   createIcon("CommitToDB.gif")) {
				public void actionPerformed(ActionEvent e) {
					checkIn();
				}
			};
			checkInAction.putValue(Action.SHORT_DESCRIPTION, "Check changes into the database");
		}
		return checkInAction;
	}
	
	private void checkIn() {
		java.util.List selection = getSelection();
		XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		MySQLAdaptor dbAdaptor = getMySQLAdaptor();
		
		checkIn(selection, fileAdaptor, dbAdaptor, true);
	}
		
	public java.util.List checkIn(
			java.util.List selection,
			XMLFileAdaptor fileAdaptor,
			MySQLAdaptor dbAdaptor,
			boolean committedInstancesRtnOnly) {
		if (selection == null || selection.size() == 0)
			return null;
		if (!saveChanges(fileAdaptor))
			return null ;
		if (dbAdaptor == null) {
			JOptionPane.showMessageDialog(curatorFrame,
				  "Cannot connect to the database",
				  "Error in DB Connecting",
				  JOptionPane.ERROR_MESSAGE);
			return null ;			
		}
		// Have to compare the local instances and remote instances first
		// to determine how many instances should be checked-in.
		java.util.List needCheckInList = new ArrayList();
		java.util.List	identicalCopies = new ArrayList();
		java.util.List uncheckableList = new ArrayList();
		java.util.List shellInstances = new ArrayList();
		InstanceComparer comparer = new InstanceComparer();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			GKInstance localCopy = (GKInstance) it.next();
			if (localCopy.isShell())
			    shellInstances.add(localCopy);
			else if (localCopy.getDBID().longValue() < 0)
				needCheckInList.add(localCopy);
			else if (!dbAdaptor.exist(localCopy.getDBID()))
				needCheckInList.add(localCopy);
			else { // Have to compare the difference
				try {
////					GKInstance remoteCopy = dbAdaptor.fetchInstance(localCopy.getSchemClass().getName(),
////					                                                localCopy.getDBID());
					dbAdaptor.setUseCache(false);
					GKInstance remoteCopy = dbAdaptor.fetchInstance(localCopy.getDBID());
					dbAdaptor.setUseCache(true);
                    if (remoteCopy == null) {
                        needCheckInList.add(localCopy);
                    }
                    else {
                        int reply = comparer.compare(localCopy, remoteCopy);
                        if (reply == InstanceComparer.IS_IDENTICAL)
                            identicalCopies.add(localCopy);
                        // Only new change in the local repository can be checked into the database.
                        else if (reply == InstanceComparer.NEW_CHANGE_IN_LOCAL)
                            needCheckInList.add(localCopy);
                        else { // Either new changes in Database or Conflict changes
                            uncheckableList.add(localCopy);
                        }
                    }
				}
				catch(Exception e) {
					System.err.println("CuratorActionCollection.checkIn(): " + e);
					e.printStackTrace();
				}
			}
		}
		if (shellInstances.size() > 0) {
		    // Generate a error message
		    StringBuffer buffer = new StringBuffer();
		    if (shellInstances.size() == 1) {
		        GKInstance instance = (GKInstance) shellInstances.get(0);
		        buffer.append("Shell instance \"" + instance.getDisplayName() + "\" cannot be checked into the database.");
		    }
		    else {
		        buffer.append("The following instances are shell instances. Shell instances cannot be checked into the database.\n");
		        for (Iterator it = shellInstances.iterator(); it.hasNext();) {
		            GKInstance instance = (GKInstance) it.next();
		            buffer.append("    ");
		            buffer.append(instance.getDisplayName());
		            if (it.hasNext())
		                buffer.append("\n");
		        }
		    }
		    JOptionPane.showMessageDialog(curatorFrame,
		                                  buffer.toString(),
		                                  "Error in Checking-in",
		                                  JOptionPane.ERROR_MESSAGE);
		}
		// Display the user info on identical Copies
		if (identicalCopies.size() == 1) {
			GKInstance instance = (GKInstance) identicalCopies.get(0);
			JOptionPane.showMessageDialog(curatorFrame,
			                              "There is no difference between the local and the database copies for the instance \"" + instance.getDisplayName() + "\".\n" +
			                              "This instance cannot be checked into the database.",
			                              "Check-in Information",
			                              JOptionPane.INFORMATION_MESSAGE);
		}
		else if (identicalCopies.size() > 1) {
			showInstanceList(identicalCopies,
						      "Check-in Information",
							  "There is no difference between the local and the database copies for the following instances. " + 
                              "These instances cannot be checked into the database:");
		}
		// To display uncheckable instances
		if (uncheckableList.size() > 0) {
		    GKInstance instance = (GKInstance) uncheckableList.get(0);
			int choice = JOptionPane.showConfirmDialog(curatorFrame,
                            "There are changes in the database.\n" +
							"E.g. for the instance \"" + instance.getDisplayName() + "\".\n" +
                            "These instances cannot be checked into the database.\n" +
							"Would you like to update these instances?",
                            "Check-in errors",
                            JOptionPane.YES_NO_OPTION);		    
			
			if (choice==JOptionPane.YES_OPTION)
				synchronizeWithDB(fileAdaptor, dbAdaptor, uncheckableList, "These instances cannot be checked into the database");
		}
		java.util.List list = SynchronizationManager.getManager().commitToDB(needCheckInList, 
		                                                                     fileAdaptor, 
		                                                                     dbAdaptor, 
		                                                                     committedInstancesRtnOnly,
		                                                                     curatorFrame);
		if (list != null && list.size() > 0) {
			StringBuffer buffer = new StringBuffer();
			if (list.size() == 1) {
				GKInstance instance = (GKInstance) list.get(0);
				String message = new String("Instance \"" + instance.getDisplayName() + "\" has been checked into the database successfully.");
				JOptionPane.showMessageDialog(curatorFrame,
				                              message,
				                              "Check-in Result",
				                              JOptionPane.INFORMATION_MESSAGE);
			}
			else {
				showInstanceList(list, 
                                  "Check-in Results",
								  "The following instances have been checked into the database successfully:");
			}
			// Have to update the GUIs
			GKInstance instance = (GKInstance) curatorFrame.getSchemaView().getAttributePane().getInstance();
			if (list.contains(instance))
				curatorFrame.getSchemaView().getAttributePane().refresh();
			instance = (GKInstance) curatorFrame.getEventView().getAttributePane().getInstance();
			if (list.contains(instance))
				curatorFrame.getEventView().getAttributePane().refresh();
		}
		
		return list;
	}
	
	/**
	 * A helper method to display a list of GKInstance objects.
	 * @param instances
	 * @param title
	 * @param subTitle
	 */
	private void showInstanceList(java.util.List instances,
	                               String title,
	                               String subTitle) {
		InstanceListDialog listDialog = new InstanceListDialog(curatorFrame, title);
		InstanceUtilities.sortInstances(instances);
		listDialog.setDisplayedInstances(instances);
		if (subTitle != null)
			listDialog.setSubTitle(subTitle);
		listDialog.setSize(600, 400);
		GKApplicationUtilities.center(listDialog);
		listDialog.setModal(true);
		listDialog.setVisible(true);
	}
		
	public Action getUpdateFromDBAction() {
		if (updateFromDBAction == null) {
			updateFromDBAction = new AbstractAction("Update from DB",
			                                        createIcon("UpdateFromDB.gif")) {
				public void actionPerformed(ActionEvent e) {
					updateFromDB();
				}
			};
			updateFromDBAction.putValue(Action.SHORT_DESCRIPTION, "Update from the database");
		}
		return updateFromDBAction;
	}
	
	private void updateFromDB() {
		java.util.List selection = getSelection();
		if (selection.size() == 0)
			return;
		// Get the MySQLAdaptor
		MySQLAdaptor dbAdaptor = getMySQLAdaptor();
		if (dbAdaptor == null) {
			JOptionPane.showMessageDialog(curatorFrame,
										  "Cannot connect to the database",
										  "Error in DB Connecting",
										  JOptionPane.ERROR_MESSAGE);
			return;			
		}	
		GKInstance instance = null;
		GKInstance dbInstance = null;
		Map<GKInstance, GKInstance> map = new HashMap<GKInstance, GKInstance>();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			instance = (GKInstance) it.next();
			try {
				dbAdaptor.setUseCache(false);
//				c = dbAdaptor.fetchInstanceByAttribute(instance.getSchemClass().getName(),
//				                                       "DB_ID",
//				                                       "=",
//				                                       instance.getDBID());
				dbInstance = dbAdaptor.fetchInstance(instance.getDBID());
				dbAdaptor.setUseCache(true);
			}
			catch(Exception e) {
				System.err.println("CuratorActionCollection.updateFromDB(): " + e);
				e.printStackTrace();
			}
			if (dbAdaptor != null) {
				// There should be only one
//				dbInstance = (GKInstance) c.iterator().next();
				map.put(instance, dbInstance);
			}
		}
		// Have to compare if the local and db copies are the same
		java.util.List identicalInstances = new ArrayList();
		InstanceComparer comparer = new InstanceComparer();
		try {
            for (Iterator it = map.keySet().iterator(); it.hasNext();) {
            	instance = (GKInstance) it.next();
            	if (instance.isShell())
            	    continue; // Escape the shell
            	dbInstance = (GKInstance) map.get(instance);
            	if (comparer.compare(instance, dbInstance) == InstanceComparer.IS_IDENTICAL) {
            		identicalInstances.add(instance);
            	}
            }
        }
        catch (Exception e) {
            System.err.println("CuratorActionCollection.updateFromDB(): " + e);
            e.printStackTrace();
        }
		if (identicalInstances.size() == 1) {
			instance = (GKInstance) identicalInstances.get(0);
			JOptionPane.showMessageDialog(curatorFrame,
			                              "The local and database copies for the instance \"" + 
			                              instance.getDisplayName() + "\" are the same.",
			                              "Update Info",
			                              JOptionPane.INFORMATION_MESSAGE);
		}
		else if (identicalInstances.size() > 1) {
			showInstanceList(identicalInstances,
			                  "Update Info",
			                  "There is no difference between the local and the database copies for the following instances. ");
		}
		if (identicalInstances.size() > 0) {
			for (Iterator it = identicalInstances.iterator(); it.hasNext();) {
				map.remove(it.next());
			}
		}
		// Need to display update results
		if (map.size() > 0) {
		    XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
			for (Iterator it = map.keySet().iterator(); it.hasNext();) {
				instance = (GKInstance) it.next();
				dbInstance = (GKInstance) map.get(instance);
				if (SynchronizationManager.getManager().updateFromDB(instance, 
				                                                     dbInstance,
				                                                     curatorFrame)) {
                    // Update the view etc.
                    AttributeEditManager.getManager().attributeEdit(instance);
                    fileAdaptor.removeDirtyFlag(instance);
                }
				else
				    it.remove();
			}
			java.util.List list = new ArrayList(map.keySet());
			if (map.size() == 1) {
				instance = (GKInstance) list.get(0);
				JOptionPane.showMessageDialog(curatorFrame,
				                              "Instance \"" + instance.getDisplayName() + "\" is updated successfully.",
				                              "Update Result",
				                              JOptionPane.INFORMATION_MESSAGE);
			}
			else if (map.size() > 1) {
				showInstanceList(list,
								"Update Result", 
				                "These instances are updated successfully:");        
			}
		}
		else {
			JOptionPane.showMessageDialog(curatorFrame,
			                              "No instances are updated.",
			                              "Update Result",
			                              JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	private MySQLAdaptor getMySQLAdaptor() {
		MySQLAdaptor adaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(curatorFrame);
		// Just a case: Most likely pwd is wrong. Remove it so that the user can retry.
		if (adaptor == null) {
		    Properties dbConnectInfo = PersistenceManager.getManager().getDBConnectInfo();
		    dbConnectInfo.remove("dbPwd");
		}
		return adaptor;
	}	
	
	public Action getCompareDBInstanceAction() {
		if (compareDBInstanceAction == null) {
			compareDBInstanceAction = new AbstractAction("Compare Instance in DB...",
			                                             createIcon("ShowComparison.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List selection = getSelection();
					if (selection.size() != 1)
						return;
					GKInstance instance = (GKInstance) selection.get(0);
					MySQLAdaptor adaptor = getMySQLAdaptor();
					if (adaptor == null) {
						JOptionPane.showMessageDialog(curatorFrame,
						                              "Cannot connect to the database",
						                              "Error in DB Connecting",
						                              JOptionPane.ERROR_MESSAGE);
						return;
					}
					GKInstance dbInstance = null;
					try {
						dbInstance = adaptor.fetchInstance(instance.getDBID());
						if (dbInstance == null) {
							JOptionPane.showMessageDialog(curatorFrame,
														  "Cannot find the matched instance. The selected instance \n" + 
														  "might be a new instance or deleted in the database.",
														  "No Instance in the Database",
														  JOptionPane.INFORMATION_MESSAGE);
						}
					}
					catch (Exception e1) {
						System.err.println("CuratorActionCollection.getCompareDBInstanceAction(): " + e1);
						e1.printStackTrace();
					}
					if (dbInstance != null) {
						compareInstances(instance, dbInstance);
					}
				}
			};
			compareDBInstanceAction.putValue(Action.SHORT_DESCRIPTION, "Compare instance in the database");
		}
		return compareDBInstanceAction;
	}
	
	private void compareInstances(GKInstance instance1, GKInstance instance2) {
		if (InstanceUtilities.compare(instance1, instance2)) {
			JOptionPane.showMessageDialog(curatorFrame, 
                                          "There is no difference between the two instances.",
                                          "No Difference",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
		}
		InstanceComparisonPane comparisonPane = new InstanceComparisonPane(instance1, instance2);
		String title = "Comparing Instances in " + instance1.getSchemClass().getName();
		JDialog dialog = new JDialog(curatorFrame, title);
		dialog.getContentPane().add(comparisonPane, BorderLayout.CENTER);
		//dialog.setModal(true);
		dialog.setSize(800, 800);
		GKApplicationUtilities.center(dialog);
		dialog.setVisible(true);
	}
	
	public Action getCompareInstancesAction() {
		if (compareInstancesAction == null) {
			compareInstancesAction = new AbstractAction("Compare Two Instances",
			                                            createIcon("CompareInstances.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List selectedInstances = curatorFrame.getSchemaView().getSelection();
					// Compare only two instances
					if (selectedInstances.size() == 2) {
						GKInstance instance1 = (GKInstance) selectedInstances.get(0);
						GKInstance instance2 = (GKInstance) selectedInstances.get(1);
						compareInstances(instance1, instance2);
					}
				}
			};
			compareInstancesAction.putValue(Action.SHORT_DESCRIPTION, "Compare two selected instances");
		}
		return compareInstancesAction;
	}
	
	public Action getViewReferersAction() {
		if (viewReferersAction == null) {
			viewReferersAction = new AbstractAction("Display Referrers",
			                                        createIcon("DisplayReferrers.gif")) {
				public void actionPerformed(ActionEvent e) {
					displayReferers();
				}
			};
			viewReferersAction.putValue(Action.SHORT_DESCRIPTION, "Display instances referring to the selected one");
		}
		return viewReferersAction;
	}
	
	private void displayReferers() {
		java.util.List selection = getSelection();
		if (selection.size() == 1) {
		    GKInstance instance = (GKInstance) selection.get(0);
		    displayReferrers(instance);
		}
	}

    protected void displayReferrers(GKInstance instance) {
        ReverseAttributePane referrersPane = new ReverseAttributePane();
        referrersPane.displayReferrersWithCallback(instance, curatorFrame.getFocusedComponent());
    }
	
	public Action getDeleteInstanceAction() {
		if (deleteInstanceAction == null) {
			deleteInstanceAction = new AbstractAction("Delete",
			                                          createIcon("DeleteInstance.gif")) {
				public void actionPerformed(ActionEvent e) {
					deleteInstance();
				}
			};
			deleteInstanceAction.putValue(Action.SHORT_DESCRIPTION, "Delete selected instance(s)");
		}
		return deleteInstanceAction;
	}
	
	private java.util.List getSelection() {
		java.util.List selection = null;
		if (curatorFrame.getFocusedComponent() instanceof SchemaViewPane) {
			SchemaViewPane schemaView = curatorFrame.getSchemaView();
			selection = schemaView.getSelection();
		}
		else if (curatorFrame.getFocusedComponent() instanceof EventCentricViewPane) {
			EventCentricViewPane eventView = curatorFrame.getEventView();
			selection = eventView.getSelection();
		}		
		if (selection == null)
			selection = new ArrayList();
		return selection;
	}
	
	private void setSelection(List instances) {
	    if (instances == null || instances.size() == 0)
	        return;
	    if (curatorFrame.getFocusedComponent() instanceof SchemaViewPane) {
	        SchemaViewPane schemaView = curatorFrame.getSchemaView();
	        schemaView.getInstancePane().setSelection(instances);
	    }
	    else if (curatorFrame.getFocusedComponent() instanceof EventCentricViewPane) {
	        EventCentricViewPane eventView = curatorFrame.getEventView();
	        GKInstance firstInstance = (GKInstance) instances.get(0);
	        eventView.setSelectedEvent(firstInstance);
	    }
	}
	
	private void deleteInstance() {
		java.util.List selection = getSelection(); 
		if (selection != null && selection.size() > 0) {
		    InstanceDeletion deletion = new InstanceDeletion();
		    deletion.delete(selection, curatorFrame);
		}
	}
	
	public Action getCreateInstanceAction() {
		if (createInstanceAction == null) {
			createInstanceAction = new AbstractAction("Create Instance",
			                                          createIcon("CreateInstance.gif")) {
				public void actionPerformed(ActionEvent e) {
					createInstance();
				}
			};
			createInstanceAction.putValue(Action.SHORT_DESCRIPTION, "Create instance");
		}
		return createInstanceAction;
	}

	private void createInstance() {
		Schema schema = curatorFrame.getSchemaView().getSchemaPane().getSchema();
		if (schema == null)
			return;
		java.util.List schemaClasses = new ArrayList(schema.getClasses());
		InstanceUtilities.sortSchemaClasses(schemaClasses);
		SchemaClass schemaClass = curatorFrame.getSchemaView().getSchemaPane().getSelectedClass();
		NewInstanceDialog newDialog = new NewInstanceDialog(curatorFrame, "Create a New Instance");
		newDialog.setSchemaClasses(schemaClasses, schemaClass);
		newDialog.setModal(true);
		newDialog.setSize(450, 600);
		newDialog.setLocationRelativeTo(curatorFrame);
		newDialog.setVisible(true);
		if (newDialog.isOKClicked()) {
			GKInstance instance = newDialog.getNewInstance();
			// Persist new instance locally
			XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
			adaptor.addNewInstance(instance);
			curatorFrame.getSchemaView().setSelection(instance);
		}
	}
	
	public Action getSaveProjectAction() {
		if (saveProjectAction == null) {
			saveProjectAction =  new AbstractAction("Save",
			                                        createIcon("Save16.gif")) {
				public void actionPerformed(ActionEvent e) {
					save();
				}
			};
			saveProjectAction.putValue(Action.SHORT_DESCRIPTION, "Save project");
		}
		return saveProjectAction;
	}
	
	public void enableSaveAction(boolean isEnabled) {
	    getSaveProjectAction().setEnabled(isEnabled);
	}
	
	private ImageIcon createIcon(String imgFileName) {
		return GKApplicationUtilities.createImageIcon(getClass(), imgFileName);
	}

	/**
	 * Save a project.
	 */
	protected void save() {
		XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		if (fileAdaptor == null)
			return;
		if (fileAdaptor.getSourceName() == null) {
			saveAs();
			return;
		}
		try {
			fileAdaptor.save();
		}
		catch (Exception e1) {
			System.err.println("curatorActionCollection.getSaveProjectAction(): " + e1);
			e1.printStackTrace();
			JOptionPane.showMessageDialog(curatorFrame, 
			                              "Cannot save the project: " + e1,
			                         	  "Error in Saving",
			                         	  JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
	
	private void saveAs() {
		// Get the file name from the file dialog
		JFileChooser chooser = getRtpjFileChooser();
		chooser.setDialogTitle("Choose a file for saving...");
		File selectedFile = GKApplicationUtilities.chooseSaveFile(chooser, curatorFrame);
		if (selectedFile != null) {
            try {
                XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
                fileAdaptor.save(selectedFile.getAbsolutePath());
                // Set the last selected file path
                // Keep track the last directory
                curatorFrame.getSystemProperties().setProperty("currentDir", 
                                                               selectedFile.getParent());
                curatorFrame.refreshTitle();
                curatorFrame.addRecentProject(selectedFile.getAbsolutePath());
                curatorFrame.saveProperties();
            }
            catch (Exception e) {
                System.err.println("CuratorActionCollection.saveAs(): " + e);
                e.printStackTrace();
                JOptionPane.showMessageDialog(curatorFrame, 
                                             "Cannot save the project as \"" + selectedFile.getAbsolutePath() + "\".", 
                                             "Error in Saving",
                                             JOptionPane.ERROR_MESSAGE);
            }
        }
	}

	public Action getDBEventViewAction() {
		if (dbEventViewAction == null) {
			dbEventViewAction = new AbstractAction("Event View") {
				public void actionPerformed(ActionEvent e) {
					MySQLAdaptor adaptor = getMySQLAdaptor();
                    if (adaptor == null) {
                        JOptionPane.showMessageDialog(curatorFrame,
                                                      "Cannot connect to the database. Please check the connecting information.",
                                                      "Error in Databases Connecting",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }
					FrameManager.getManager().showEventView(adaptor, 
					                                        true, 
					                                        GKDBBrowserPopupManager.CURATOR_TOOL_TYPE);
					JFrame eventFrame = FrameManager.getManager().getEventViewFrame();
					JMenuBar menubar = eventFrame.getJMenuBar();
					if (menubar == null || menubar.getMenuCount() > 1) {
					    return;
					}
					// Add a QA menu
					QAMenuHelper helper = new QAMenuHelper();
					JMenu qaMenu = helper.createQAMenu(FrameManager.getManager().getEventViewPane(),
                                                       PersistenceManager.getManager().getActiveMySQLAdaptor());
                    if (qaMenu != null) {
                        menubar.add(qaMenu);
                        menubar.validate();
                    }
				}
			};
			FrameManager.getManager().setDBEventViewAction(dbEventViewAction);
		}
		return dbEventViewAction;
	}
	

	
	public Action getDBSchemaViewAction() {
		if (dbSchemaViewAction == null) {
			dbSchemaViewAction = new AbstractAction("Schema View") {
				public void actionPerformed(ActionEvent e) {
					MySQLAdaptor adaptor = getMySQLAdaptor();
                    if (adaptor == null) {
                        JOptionPane.showMessageDialog(curatorFrame,
                                                      "Cannot connect to the database. Please check the connecting information.",
                                                      "Error in Databases Connecting",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }
					// Use a short menu, i.e., only File menu
					GKDatabaseBrowser browser = FrameManager.getManager().openBrowser(adaptor, true, GKDBBrowserPopupManager.CURATOR_TOOL_TYPE);
					JMenuBar menubar = browser.getJMenuBar();
					if (menubar.getMenuCount() == 1) {
					    QAMenuHelper helper = new QAMenuHelper();
					    JMenu qaMenu = helper.createQAMenu(browser.getSchemaView(),
					                                       PersistenceManager.getManager().getActiveMySQLAdaptor());
					    if (qaMenu != null) {
					        menubar.add(qaMenu);
					        menubar.validate();
					    }
					}
				}
			};
			FrameManager.getManager().setDBSchemaViewAction(dbSchemaViewAction);
		}
		return dbSchemaViewAction;
	}
	
	/**
	 * Call this method during GUI initialization.
	 */
	public void initializeActionStatus() {
		// Just start
		saveProjectAction.setEnabled(false);
		// Nothing is selected
		checkInAction.setEnabled(false);
		cloneInstanceAction.setEnabled(false);
         compareDBInstanceAction.setEnabled(false);
		compareInstancesAction.setEnabled(false);
		createInstanceAction.setEnabled(false);
		deleteInstanceAction.setEnabled(false);
		updateFromDBAction.setEnabled(false);
		viewInstanceAction.setEnabled(false);
		viewReferersAction.setEnabled(false);
	}
	
	public void validateActions() {
		// No need to check saveProjectAction. It is taken care of by the
		// GKCuratorFrame.
		java.util.List selection = getSelection();
		if (selection.size() == 0) {
			checkInAction.setEnabled(false);
			cloneInstanceAction.setEnabled(false);
            // Have to call this method to initialize the action.
            getCreateEwasFromRefPepSeqAction().setEnabled(false);
             switchTypeAction.setEnabled(false);
             compareDBInstanceAction.setEnabled(false);
			compareInstancesAction.setEnabled(false);
			deleteInstanceAction.setEnabled(false);
			updateFromDBAction.setEnabled(false);
			viewInstanceAction.setEnabled(false);
			viewReferersAction.setEnabled(false);
			addBookmarkAction.setEnabled(false);
			mergeInstanceAction.setEnabled(false);
			matchInstanceAction.setEnabled(false);
             batchEditAction.setEnabled(false);
		}
		else {
		    // checkIn and clone can apply to non-shell instances only
		    boolean hasShell = false;
		    for (Iterator it = selection.iterator(); it.hasNext();) {
		        GKInstance instance = (GKInstance) it.next();
		        if (instance.isShell()) {
		            hasShell = true;
		            break;
		        }
		    }
		    if (hasShell) {
		        checkInAction.setEnabled(false);
		        cloneInstanceAction.setEnabled(false);
                switchTypeAction.setEnabled(false);
                if (createEwasFromRefPepSeqAction != null)
                    createEwasFromRefPepSeqAction.setEnabled(false);
		    }
		    else {
		        checkInAction.setEnabled(true);
		        cloneInstanceAction.setEnabled(true);
                switchTypeAction.setEnabled(true);
                if (createEwasFromRefPepSeqAction != null)
                    createEwasFromRefPepSeqAction.setEnabled(true);
		    }
			deleteInstanceAction.setEnabled(true);
			updateFromDBAction.setEnabled(true);
			viewInstanceAction.setEnabled(true);
			addBookmarkAction.setEnabled(true);
			if (selection.size() == 1) {
                 batchEditAction.setEnabled(false);
				compareDBInstanceAction.setEnabled(true);
				viewReferersAction.setEnabled(true);
				GKInstance instance = (GKInstance) selection.get(0);
				// MatchInstancesAction should work only for
				// newly created instances.
				if (instance.getDBID() != null &&
				    instance.getDBID().longValue() < 0)
				    matchInstanceAction.setEnabled(true);
				else
				    matchInstanceAction.setEnabled(false);
				// A shell instance should have no meanings to compare
				if (instance.isShell())
				    compareDBInstanceAction.setEnabled(false);
				else
				    compareDBInstanceAction.setEnabled(true);
			}
			else {
                batchEditAction.setEnabled(true);
			    matchInstanceAction.setEnabled(false);
				compareDBInstanceAction.setEnabled(false);
				viewReferersAction.setEnabled(false);
			}
			if (selection.size() == 2) {
				compareInstancesAction.setEnabled(true);
				mergeInstanceAction.setEnabled(true);
			}
			else {
				compareInstancesAction.setEnabled(false);
				mergeInstanceAction.setEnabled(false);
			}
		}
		// Create Instance action
		if (curatorFrame.getFocusedComponent() instanceof SchemaViewPane) {
			SchemaViewPane schemaViewPane = (SchemaViewPane) curatorFrame.getFocusedComponent();
			GKSchemaClass selectedClass = schemaViewPane.getSchemaPane().getSelectedClass();
			if (selectedClass != null && !selectedClass.isAbstract())
				createInstanceAction.setEnabled(true);
			else
				createInstanceAction.setEnabled(false);
		}
		else
			createInstanceAction.setEnabled(false);
	}
	
	private JFileChooser getRtpjFileChooser() {
	    JFileChooser fileChooser = GKApplicationUtilities.createFileChooser(curatorFrame.getSystemProperties());
	    // Add Choosable FileFilter
		GKFileFilter fileFilter = new GKFileFilter(GKCuratorFrame.PROJECT_EXT_NAME,
		                                           "Reactome Project Files (*.rtpj)");
		fileChooser.addChoosableFileFilter(fileFilter);
		fileChooser.addChoosableFileFilter(new XMLFileFilter());
		fileChooser.setFileFilter(fileFilter);
	    return fileChooser;
	}
	
}
