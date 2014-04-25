/*
 * Created on Apr 8, 2005
 *
 */
package org.gk.qualityCheck;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.gk.database.*;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.ProgressPane;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * This is an abstract implementation of QualityCheck. It takes care of some common
 * variables for QualityCheck. The functions of a quality check is needed:
 * 1). Check for a list of instances, instances under one selected SchemaClass, or one GKInstance only
 * 2). The offending instances should be listed in a JFrame. If the checked instances are editable, the user
 * should make changes in this window. 
 * 3). The user should be able to check out these offending instances if the checking is done against the 
 * database browser.
 * 4). The user can generate a file output for offending instances for curators to review.
 * @author wgm
 *
 */
public abstract class AbstractQualityCheck implements QualityCheck {
    // The data source to be checked
    protected PersistenceAdaptor dataSource;
    protected Component parentComp;
    protected ProgressPane progressPane;
    // Cached the original JFrame behavior
    private int oldJFrameCloseAction ; // Default
    private WindowListener[] windowListeners;
    // A system-wide properties to be used for some GUIs
    private Properties properties;
    // Used to check if escape if needed
    protected QAEscapeHelper escapeHelper;
    
    protected AbstractQualityCheck() {
        escapeHelper = new QAEscapeHelper();
    }
    
    public abstract void check();
    
    public abstract void check(GKInstance instance);
    
    public abstract void check(List<GKInstance> instances);
   
    public abstract void checkProject(GKInstance event);
    
    public void setSystemProperties(Properties prop) {
        this.properties = prop;
    }
    
    public PersistenceAdaptor getDatasource() {
        return this.dataSource;
    }
    
    public void setDatasource(PersistenceAdaptor dataSource) {
        this.dataSource = dataSource;
    }
    
    public void setParentComponent(Component comp) {
        this.parentComp = comp;
    }

    protected void registerAttributePane(final AttributePane attributePane,
    			                         JFrame parentFrame) {
    	//To synchronize the editing
    	final AttributeEditListener editListener = new AttributeEditListener() {
    		public void attributeEdit(AttributeEditEvent e) {
    			GKInstance instance = (GKInstance) e.getEditingInstance();
    			if (attributePane.getInstance() == instance) {
    				attributePane.refresh();
    			}
    		}
    	};
    	AttributeEditManager.getManager().addAttributeEditListener(editListener);
    	// Have to remove it if display is closed
    	parentFrame.addWindowListener(new WindowAdapter() {
    		// For closing by clicking the up-right corner
    		public void windowClosing(WindowEvent e) {
    			AttributeEditManager.getManager().removeAttributeEditListener(editListener);
    		}
    		// For closing by clicking close button
    		public void windowClosed(WindowEvent e) {
    			AttributeEditManager.getManager().removeAttributeEditListener(editListener);
    		}
    	});
    }
    
    protected void initProgressPane(String title) {
        progressPane = new ProgressPane();
        progressPane.setTitle(title);
        progressPane.setIndeterminate(true);
        progressPane.enableCancelAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hideProgressPane();
            }
        });
        final JFrame parentFrame = (JFrame) SwingUtilities.getRoot(parentComp);
        parentFrame.setGlassPane(progressPane);
        parentFrame.getGlassPane().setVisible(true);
        oldJFrameCloseAction = parentFrame.getDefaultCloseOperation();
        parentFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // Need to remove all WindowsListeners. Otherwise, the above statement cannot work
        windowListeners = parentFrame.getWindowListeners();
        if (windowListeners != null && windowListeners.length > 0) {
            for (int i = 0; i < windowListeners.length; i++)
                parentFrame.removeWindowListener(windowListeners[i]);
        }
    }

    protected void hideProgressPane() {
        JFrame parentFrame = (JFrame) SwingUtilities.getRoot(parentComp);
        parentFrame.getGlassPane().setVisible(false);
        if (oldJFrameCloseAction != -1)
            parentFrame.setDefaultCloseOperation(oldJFrameCloseAction);
        if (windowListeners != null && windowListeners.length > 0) {
            for (int i = 0; i < windowListeners.length; i++)
                parentFrame.addWindowListener(windowListeners[i]);
        }
    }
    
    protected Action createCheckOutAction(final JFrame frame) {
        Action checkOutAction = new AbstractAction("Check Out") {
            public void actionPerformed(ActionEvent e) {
                checkOutSelectedInstances(frame);
            }
        };
        return checkOutAction;
    }
    
    protected void checkOutSelectedInstances(JFrame frame) {
        List selected = getDisplayedList().getSelection();
        if (selected == null || selected.size() == 0)
            return;
        // First need to ask if the user wants to switch to gk_central database if the current
        // active MySQLAdaptor is not.
        MySQLAdaptor current = PersistenceManager.getManager().getActiveMySQLAdaptor();
        String[] centralDbInfo = getCentralDatabaseInfo();
        if (centralDbInfo != null) {
            String currentHost = current.getDBHost();
            String currentDb = current.getDBName();
            String centralHost = centralDbInfo[1];
            String centralDb = centralDbInfo[0];
            // Make sure both host name use a short form (e.g. brie8 instead of brie8.cshl.edu)
            if (currentHost.indexOf(".") < -1 ||
                centralHost.indexOf(".") < -1) {
                // Get the short name only
                int index = currentHost.indexOf(".");
                if (index > 0)
                    currentHost = currentHost.substring(0, index);
                index = centralHost.indexOf(".");
                if (index > 0)
                    centralHost = centralHost.substring(0, index);
            }
            // Make sure both dbName and dbHost are the same
            if (!currentDb.equals(centralDb) ||
                !currentHost.equals(centralHost)) {
                // Check if database switch is needed
                String message = "You have not used the central Reactome database for your repository.\n" + 
                                 "Do you want to switch to the central database for checking out?\n" +
                                 "If you switch the connection, opened db schema and event browsers will be closed.";
                int reply = JOptionPane.showConfirmDialog(frame,
                                                          message,
                                                          "Switch to Central Database?",
                                                          JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION) {
                    Properties prop = PersistenceManager.getManager().getDBConnectInfo();
                    prop.put("dbHost", centralHost);
                    prop.put("dbName", centralDb);
                    prop.remove("dbPwd"); // To force the connection dialog to appear
                    // Force to null
                    PersistenceManager.getManager().setActiveMySQLAdaptor(null);
                    MySQLAdaptor centralDBA = PersistenceManager.getManager().getActiveMySQLAdaptor(frame);
                    // If the user wants to switch, try to popup the connecting dialog information for confirmation
                    if (centralDBA == null) {// canceled 
                        PersistenceManager.getManager().setActiveMySQLAdaptor(current);
                        return; 
                    }
                    FrameManager.getManager().closeBrowser();
                    SynchronizationManager.getManager().refresh();
                    // Create a new list based on DB_IDs
                    // Get DB_IDs from the list if the database has been switched.
                    // Load instances from the new database
                    List dbIds = new ArrayList();
                    for (Iterator it = selected.iterator(); it.hasNext();) {
                        GKInstance tmp = (GKInstance) it.next();
                        dbIds.add(tmp.getDBID());
                    }
                    try {
                        Collection list = centralDBA.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject, 
                                                                              ReactomeJavaConstants.DB_ID, 
                                                                              "=",
                                                                              dbIds);
                        selected = new ArrayList(list);
                    }
                    catch(Exception e) {
                        System.err.println("AbstractQualityChecker.checkOutSelectedInstances(): " + e);
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(frame,
                                                      "Cannot load instances from the central database: " + e,
                                                      "Error in Loading Instances",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
        checkOutSelectedInstances(frame, 
                                  selected);
    }

    protected void checkOutSelectedInstances(JFrame frame,
                                             List<GKInstance> selected) {
        // Check out instances from the active MySQLAdaptor
        Window parentDialog = (Window) SwingUtilities.getRoot(frame);
        try {
            checkOut(selected, parentDialog);
        }
        catch(Exception e) {
            System.err.println("AbstractQualityChecker.checkOutSelectedInstances() 1: " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Cannot check out instanes from the central database: " + e.getMessage(),
                                          "Error in Checking Out",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    protected void checkOut(List instances,
                            Window parentDialog) throws Exception {
        // Need to sort based on schema classes
        Map sorted = new HashMap();
        for (Iterator it = instances.iterator(); it.hasNext();) {
            GKInstance tmp = (GKInstance) it.next();
            List list = (List) sorted.get(tmp.getSchemClass());
            if (list == null) {
                list = new ArrayList();
                sorted.put(tmp.getSchemClass(), list);
            }
            list.add(tmp);
        }
        for (Iterator it = sorted.keySet().iterator(); it.hasNext();) {
            SchemaClass cls = (SchemaClass) it.next();
            List list = (List) sorted.get(cls);
            SynchronizationManager.getManager().checkOut(list, parentDialog);
        }
    }
    
    private String[] getCentralDatabaseInfo() {
        String dbName = null;
        String dbHost = null;
        try {
            InputStream metaConfig = GKApplicationUtilities.getConfig("curator.xml");
            if (metaConfig == null)
                return null;
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(metaConfig);
            Element elm = (Element) XPath.selectSingleNode(doc.getRootElement(), 
                                                           "central_db");
            if (elm == null)
                return null;
            dbName = elm.getAttributeValue("dbName");
            dbHost = elm.getAttributeValue("dbHost");
            return new String[]{dbName, dbHost};
        }
        catch (IOException e) {
            // Don't do anything if there is an exception.
            e.printStackTrace();
        }
        catch (JDOMException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    protected abstract InstanceListPane getDisplayedList();
    
    protected CheckOutControlPane createControlPane(final JFrame frame) {
        return new CheckOutControlPane(frame);
    }

    protected void showResultFrame(final JFrame frame) {
        frame.setSize(700, 525);
        frame.setLocationRelativeTo(parentComp);
        // Set image icon
        if (parentComp instanceof JFrame) {
            frame.setIconImage(((JFrame)parentComp).getIconImage());
        }
        frame.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    public static void doCheck(String checkerClsName,
    		                   PersistenceAdaptor dataSource,
							   SchemaViewPane schemaView,
							   EventCentricViewPane eventView,
							   Properties properties) {
		try {
			QualityCheck checker = (QualityCheck) Class.forName(checkerClsName).newInstance();
			checker.setDatasource(dataSource);
			// Only database can escape a set of instances.
			checker.setIsInstancesEscapeNeeded(dataSource instanceof MySQLAdaptor);
			checker.setParentComponent(schemaView != null ? schemaView : eventView);
			checker.setSystemProperties(properties);
			if (schemaView != null) {
			    // If there are some instances selected in InstanceListPane, check these
			    List selectedInstances = schemaView.getSelection();
			    if (selectedInstances != null && selectedInstances.size() > 0) {
			        checker.check(selectedInstances);
			        return;
			    }
			    // else if there is a SchemaClass selected in SchemaDisplayPane, check that class
			    GKSchemaClass cls = schemaView.getSelectedClass();
			    if (cls != null) {
			        checker.check(cls);
			        return;
			    }
			    // else check the whole repository
			    checker.check();
			}
			else if (eventView != null) {
			    List<GKInstance> selection = eventView.getSelection();
			    if (selection == null || selection.size() == 0) {
			        JOptionPane.showMessageDialog(eventView, 
			                                      "Please choose an event for QA check!",
			                                      "Project QA Check",
			                                      JOptionPane.INFORMATION_MESSAGE);
			        return;
			    }
			    if (selection.size() > 1) {
			        JOptionPane.showMessageDialog(eventView,
			                                      "Please choose only one event for QA check!",
			                                      "Project QA Check",
			                                      JOptionPane.INFORMATION_MESSAGE);
			        return;
			    }
			    GKInstance event = selection.get(0);
			    checker.checkProject(event);
			}
		}
		catch (Exception e1) {
			System.err.println("AbstractQualityCheck.createAction(): " + e1);
			e1.printStackTrace();
		}
    }
    
    public void setIsInstancesEscapeNeeded(boolean isNeeded) {
        escapeHelper.setNeedEscapePermissioin(isNeeded);
    }

    /**
     * A helper method to create an action for dump to file button.
     * @return
     */
    protected Action createDumpToFileAction() {
        Action action = new AbstractAction("Dump to File") {
            public void actionPerformed(ActionEvent e) {
                InstanceListPane listPane = getDisplayedList();
                List instances = listPane.getDisplayedInstances();
                if (instances == null || instances.size() == 0)
                    return;
                JFileChooser fileChooser = GKApplicationUtilities.createFileChooser(properties);
                FileFilter txtFilter = new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        String fileName = f.getName();
                        return fileName.endsWith(".txt");
                    }
                    @Override
                    public String getDescription() {
                        return "Text File (.txt)";
                    }                    
                };
                fileChooser.addChoosableFileFilter(txtFilter);
                JButton btn = (JButton) e.getSource();
                File file = GKApplicationUtilities.chooseSaveFile(fileChooser, 
                                                                  ".txt", 
                                                                  btn == null ? parentComp : btn);
                if (file == null)
                    return;
                dumpInstancesToFile(file, instances);
            }
        };
        return action;
    }
    
    /**
     * Helper method to dump some essential information (DB_ID, class, and _displayName)
     * into a simple text file.
     * @param file
     * @param instances
     */
    private void dumpInstancesToFile(File file,
                                     List instances) {
        try {
            FileWriter writer = new FileWriter(file);
            PrintWriter printWriter = new PrintWriter(writer);
            // Generate a simple text file
            // Output format is: DB_ID\tClass\t_displayName
            for (Object obj : instances) {
                GKInstance instance = (GKInstance) obj;
                printWriter.println(instance.getDBID() + "\t" + 
                                    instance.getSchemClass().getName() + "\t" + 
                                    instance.getDisplayName());
            }
            printWriter.close();
            writer.close();
        }
        catch(IOException e) {
            System.err.println("AbstractQualityCheck.dumpInstancesToFile(): " + e);
            e.printStackTrace();
        }
    }
    
    protected void validateDataSource() {
        if (dataSource == null)
            throw new IllegalStateException("validateDataSource(): dataSource has not specified.");
    }

    protected Set<GKInstance> loadInstancesInProject(GKInstance event) throws Exception {
        progressPane.setText("Load contained events...");
        Set<GKInstance> allEvents = InstanceUtilities.getContainedEvents(event);
        allEvents.add(event);
        progressPane.setText("Load attributes for events...");
        EventCheckOutHandler checkOutHandler = new EventCheckOutHandler();
        Map<SchemaClass, Set<GKInstance>> clsToInstances = checkOutHandler.pullInstances(allEvents);
        Set<GKInstance> instances = new HashSet<GKInstance>();
        for (Set<GKInstance> set : clsToInstances.values())
            instances.addAll(set);
        return instances;
    }
    
    protected boolean checkIsNeedEscape() {
        return escapeHelper.checkIfEscapeNeeded(parentComp);
    }
    
    /**
     * Return false if loading is cancelled.
     * @param instances
     * @return
     * @throws Exception
     */
    protected boolean loadAttributesForQAEscape(Collection<GKInstance> instances) throws Exception {
        if (escapeHelper.isNeedEscape() && dataSource instanceof MySQLAdaptor) {
            // Load instances attribute
            progressPane.setText("Load created and modified values...");
            MySQLAdaptor dba = (MySQLAdaptor) dataSource;
            SchemaClass dbcls = dba.getSchema().getClassByName(ReactomeJavaConstants.DatabaseObject);
            dba.loadInstanceAttributeValues(instances, 
                                            dbcls.getAttribute(ReactomeJavaConstants.created));
            dba.loadInstanceAttributeValues(instances,
                                            dbcls.getAttribute(ReactomeJavaConstants.modified));
            // Want to load all dateTime for instances
            Set ies = new HashSet();
            for (GKInstance inst : instances) {
                List values = inst.getAttributeValuesList(ReactomeJavaConstants.created);
                if (values != null)
                    ies.addAll(values);
                values = inst.getAttributeValuesList(ReactomeJavaConstants.modified);
                if (values != null)
                    ies.addAll(values);
            }
            dbcls = dba.getSchema().getClassByName(ReactomeJavaConstants.InstanceEdit);
            dba.loadInstanceAttributeValues(ies, dbcls.getAttribute(ReactomeJavaConstants.dateTime));
            return !progressPane.isCancelled(); // Return false if it is canceled
        }
        return true;
    }

    protected void escapeInstances(Collection<GKInstance> instances) throws Exception {
        if (instances == null || instances.size() == 0 || !escapeHelper.isNeedEscape()) 
            return;
        if(!loadAttributesForQAEscape(instances))
            return; // Don't do anything
        int escaped = 0;
        // Do escape
        for (Iterator<GKInstance> it = instances.iterator(); it.hasNext();) {
            GKInstance inst = it.next();
            if (escapeHelper.shouldEscape(inst)) {
                it.remove();
                escaped ++;
            }
        }
        // Show a message
        if (escaped > 0) {
            String message;
            if (instances.size() == 0)
                message = "All instances are in the escaped list. No instance will be checked.";
            else
                message = (escaped == 1 ? "One instance is" : (escaped + " instances are")) + 
                             " in the escaped list, and will not be checked.";
            JOptionPane.showMessageDialog(parentComp,
                                          message,
                                          "Escape QA",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * This customized JPanel is used to create several buttons related to check-out,
     * close and dump a report file.
     * @author wgm
     *
     */
    class CheckOutControlPane extends JPanel {
        
        private JButton checkOutBtn;
        private JButton closeBtn;
        private JButton dumpToFileBtn;
        
        public CheckOutControlPane(JFrame parentFrame) {
            init(parentFrame);
        }
        
        public JButton getCheckOutBtn() {
            return this.checkOutBtn;
        }
        
        private void init(final JFrame parentFrame) {
            setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            checkOutBtn = new JButton("Check Out");
            checkOutBtn.setAction(createCheckOutAction(parentFrame));
            checkOutBtn.setToolTipText("Check out selected instances to local project");
            dumpToFileBtn = new JButton("Dump to File");
            dumpToFileBtn.setAction(createDumpToFileAction());
            dumpToFileBtn.setToolTipText("Dump DB_IDs and _displayNames of offended instances to a local file");
            closeBtn = new JButton("Close");
            closeBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    parentFrame.dispose();
                }
            });
            add(checkOutBtn);
            checkOutBtn.setEnabled(false); // Default should be disabled
            // Don't need checkout for a local project
            checkOutBtn.setVisible(dataSource instanceof MySQLAdaptor);
            add(dumpToFileBtn);
            add(closeBtn);
        }
        
        public void setCheckOutVisiable(boolean isVisible) {
            checkOutBtn.setVisible(isVisible);
        }
        
    }
}
