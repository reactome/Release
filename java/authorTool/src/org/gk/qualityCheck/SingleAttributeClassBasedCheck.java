/*
 * Created on Aug 18, 2009
 *
 */
package org.gk.qualityCheck;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;

import org.gk.database.InstanceListAttributePane;
import org.gk.database.InstanceListPane;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;

public abstract class SingleAttributeClassBasedCheck extends ClassBasedQualityCheck {
    
    /**
     * This customized table model is used to display component compartment information
     * for a Complex.
     * @author wgm
     *
     */
    class ComponentTableModel extends ResultTableModel {
        
        protected List<GKInstance> components;
        
        public ComponentTableModel() {
        }
    
        public void setInstance(GKInstance container) {
            try {
                Set<GKInstance> set = getAllContainedEntities(container);
                if (components == null)
                    components = new ArrayList<GKInstance>(set);
                else {
                    components.clear();
                    components.addAll(set);
                }
                fireTableStructureChanged();
            }
            catch(Exception e) {
                System.err.println("ComponentTableModel.setInstance(): " + e);
                e.printStackTrace();
            }
        }
        
        public int getRowCount() {
            return components == null ? 0 : components.size();
        }
    
        public Object getValueAt(int rowIndex, int columnIndex) {
            GKInstance component = (GKInstance) components.get(rowIndex);
            if (columnIndex == 0) {
                String name = component.getDisplayName();
                Long dbId = component.getDBID();
                return name + " [" + dbId + "]";
            }
            try {
                //List values = component.getAttributeValuesList(ReactomeJavaConstants.compartment);
                List values = null;
                if (component.getSchemClass().isValidAttribute(checkAttribute))
                    values = component.getAttributeValuesList(checkAttribute);
                if (values == null || values.size() == 0)
                    return "";
                StringBuffer buffer = new StringBuffer();
                for (Iterator it = values.iterator(); it.hasNext();) {
                    GKInstance compt = (GKInstance) it.next();
                    buffer.append(compt.getDisplayName());
                    if (it.hasNext())
                        buffer.append(", ");
                }
                return buffer.toString();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    protected String checkClsName;
    protected String checkAttribute;
    protected InstanceListPane listPane;
    // Instances in those attributes should be loaded and checked QA
    protected String[] followAttributes;
    
    /**
     * The actual place to do real checking.
     * @param instances
     * @throws Exception
     */
    protected void checkInstances(Collection<GKInstance> instances) throws Exception {
        // Do an escape
        escapeInstances(instances);
        if (instances.size() == 0) // Check instances after escape QAs
            return; // Nothing to be checked 
        if (dataSource instanceof MySQLAdaptor && instances.size() > SIZE_TO_LOAD_ATTS) 
            loadAttributes(instances);
        if (progressPane.isCancelled())
            return;
        List<GKInstance> offended = new ArrayList<GKInstance>();
        progressPane.setIndeterminate(false);
        progressPane.setMinimum(0);
        progressPane.setMaximum(instances.size());
        progressPane.setText("Checking " + checkClsName + " " + checkAttribute + " ...");
        // We need to get all levels PhysilcaEntities contained by a Complex
        // To see if we need to load attribute values first
        int c = 1;
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            if (!checkInstance(instance))
                offended.add(instance);
            progressPane.setValue(c ++);
            if (progressPane.isCancelled())
                return;
        }
        displayResults(offended);
        hideProgressPane();
    }

    protected abstract boolean checkInstance(GKInstance instance) throws Exception;

    public void check() {
        if (!checkIsNeedEscape())
            return;
        // Use a new thread so that the progress can be monitored
        Thread t = new Thread() {
            public void run() {
                try {
                    // Get the Reaction class
                    Schema schema = dataSource.getSchema();
                    SchemaClass cls = schema.getClassByName(checkClsName);
                    initProgressPane("Check " + checkClsName + " " + checkAttribute);
                    progressPane.setText("Fetch " + checkClsName + "...");
                    progressPane.setIndeterminate(true);
                    Collection instances = dataSource.fetchInstancesByClass(cls);
                    checkInstances(instances);
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("SingleAttributeClassBasedCheck.check(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    public void checkProject(final GKInstance event) {
        Thread t = new Thread() {
            public void run() {
                try {
                    initProgressPane("Check " + checkClsName + " " + checkAttribute);
                    Set<GKInstance> instances = loadInstancesInProject(event);
                    Set<GKInstance> filtered = filterInstancesForProject(instances);
                    checkInstances(filtered);
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("SingleAttribuetClassBasedCheck.checkProject(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    protected abstract Set<GKInstance> filterInstancesForProject(Set<GKInstance> instances);
    
    protected Set<GKInstance> filterInstancesForProject(Set<GKInstance> instances,
                                                        String clsName) {
        Set<GKInstance> rtn = new HashSet<GKInstance>();
        for (GKInstance inst : instances) {
            if (inst.getSchemClass().isa(clsName))
                rtn.add(inst);
        }
        return rtn;
    }

    public void check(GKInstance instance) {
        SchemaClass cls = instance.getSchemClass();
        if (cls.getName().equals(checkClsName)) {
            List<GKInstance> list = new ArrayList<GKInstance>();
            list.add(instance);
            check(list);
        }
        else
            showErrorMessage();
    }

    public void check(GKSchemaClass cls) {
        // Only reactions should be checked for imbalance
        if (!cls.isa(checkClsName)) {
            showErrorMessage();
            return;
        }
        check();
    }

    public void check(List<GKInstance> instances) {
        final List<GKInstance> checkable = new ArrayList<GKInstance>();
        for (GKInstance instance : instances) {
            if (instance.getSchemClass().isa(checkClsName))
                checkable.add(instance);
        }
        if (checkable.size() == 0) {
            showErrorMessage();
            return;
        }
        if (checkable.size() < instances.size()) {
            int reply = JOptionPane.showConfirmDialog(parentComp,
                                          "Some of selected instances are not " + checkClsName + ". Only " + checkClsName + 
                                          " instances can be checked for " + checkClsName + " " + checkAttribute + ".\n" +
                                          "Do you want to continue to check those " + checkClsName + " instances?",
                                          "Continue Checking?",
                                          JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.NO_OPTION)
                return;
        }
        // Use a new thread so that the progress can be monitored
        Thread t = new Thread() {
            public void run() {
                try {
                    initProgressPane("Check " + checkClsName + " " + checkAttribute);
                    // If reactions size less than 20, use a simple way to load attributes
                    checkInstances(checkable);
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("SingleAttributeClassCheck.check(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    /**
     * A helper method.
     */
    protected void showErrorMessage() {
        JOptionPane.showMessageDialog(parentComp,
                                      "Only " + checkClsName +  " instances should be checked for " + checkClsName + " " + checkAttribute + ".",
                                      "Error in " + checkAttribute + " check",
                                      JOptionPane.ERROR_MESSAGE);
    }

    @Override
    protected InstanceListPane getDisplayedList() {
        return listPane;
    }

    /**
     * Need to implement a class specific way to load attributes.
     * @param instances
     */
    protected abstract void loadAttributes(Collection<GKInstance> instances) throws Exception;

    private void displayResults(List offendedInstances) throws Exception {
        if (offendedInstances == null || offendedInstances.size() == 0) {
            String msg = null;
            if (dataSource instanceof XMLFileAdaptor) {
                msg = "Checked " + checkClsName +  " have correct " + checkAttribute + " settings.\nNote: Instances containing shell instances are not checked.";
            }
            else
                msg = "Checked " + checkClsName + " have correct " + checkAttribute + " settings.";
            JOptionPane.showMessageDialog(parentComp,
                                          msg,
                                          checkClsName + " " + checkAttribute + " check result",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Check if it is editable
        boolean isEditable = dataSource instanceof MySQLAdaptor ? false : true;
        // Construct a JFrame for results
        final JFrame frame = new JFrame(checkClsName + " " + checkAttribute + " Check Result");
        // Add another pane to display the checking result for the selected GKInstance
        InstanceListAttributePane listAttributePane = new InstanceListAttributePane();
        listPane = listAttributePane.getInstanceListPane();
        listAttributePane.setSubTitle("The following " + checkClsName + " have wrong " + checkAttribute + " settings: " + offendedInstances.size() + " instances");
        InstanceUtilities.sortInstances(offendedInstances);
        listAttributePane.setDisplayedInstances(offendedInstances);
        listAttributePane.hideControlPane();
        // Add a result pane to display the detailed compartment information for a
        // selected wrong complex
        ResultTableModel tableModel = getResultTableModel();
        final ResultPane resultPane = new ResultPane();
        resultPane.setVisible(false);
        resultPane.setTableModel(tableModel);
        final JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        listAttributePane,
                                        resultPane);
        jsp.setResizeWeight(0.75);
        frame.getContentPane().add(jsp,
                                   BorderLayout.CENTER);
        // Need to add a control pane
        CheckOutControlPane controlPane = createControlPane(frame);
        final JButton checkOutBtn = controlPane.getCheckOutBtn();
        ListSelectionListener selectionListner = generateListSelectionListener(resultPane, 
                                                                               jsp, 
                                                                               checkOutBtn, 
                                                                               checkAttribute + " check");
        listPane.addSelectionListener(selectionListner);
    
        frame.getContentPane().add(jsp, BorderLayout.CENTER);
        frame.getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        frame.setSize(650, 525);
        frame.setLocationRelativeTo(parentComp);
        if (isEditable) {
            listAttributePane.setEditable(true);
            registerAttributePane(listAttributePane.getAttributePane(), frame);
        }
        else {
            listAttributePane.setEditable(false);
        }
        // Set image icon
        if (parentComp instanceof JFrame) {
            frame.setIconImage(((JFrame)parentComp).getIconImage());
        }
        frame.setVisible(true);
        frame.toFront();
    }

    /**
     * Different class should have different result table model to show the check result.
     * @return
     * @throws Exception
     */
    protected abstract ResultTableModel getResultTableModel() throws Exception;

    /**
     * This method is used to grep all contained PhysicalEntities from a passed Complex.
     * Entities in different levels will be returned.
     * @param container
     * @return
     * @throws Exception
     */
    protected Set<GKInstance> getAllContainedEntities(GKInstance container) throws Exception {
        Set<GKInstance> components = new HashSet<GKInstance>();
        Set<GKInstance> current = new HashSet<GKInstance>();
        // We don't want to have the container
        for (int i = 0; i < followAttributes.length; i++) {
            if (container.getSchemClass().isValidAttribute(followAttributes[i])) {
                List values = container.getAttributeValuesList(followAttributes[i]);
                if (values != null)
                    current.addAll(values);
            }
        }
        Set next = new HashSet();
        while (current.size() > 0) {
            for (Iterator<?> it = current.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                components.add(instance);
                for (int i = 0; i < followAttributes.length; i++) {
                    if (instance.getSchemClass().isValidAttribute(followAttributes[i])) {
                        List values = instance.getAttributeValuesList(followAttributes[i]);
                        if (values != null)
                            next.addAll(values);
                    }
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
            // To avoid any self-contained wrong case to avoid going into a infinite loop
            current.removeAll(components);
        }
        return components;
    }
    
    /**
     * Override this method to load attribtues if dataSource have been switched.
     * Also, make sure all related instances should be checked out in full so that QA checking results
     * should be the same for a local project for these instances.
     */
    @Override
    protected void checkOutSelectedInstances(JFrame parentFrame,
                                             List<GKInstance> selected) {
        // Check out instances from the active MySQLAdaptor
        Window parentDialog = (Window) SwingUtilities.getRoot(parentFrame);
        try {
            GKInstance instance = (GKInstance) selected.iterator().next();
            if (instance.getDbAdaptor() != dataSource) {
                // The database connection has been switched. Load instance attributes if
                // there is a long instances
                if (selected.size() > SIZE_TO_LOAD_ATTS)
                    loadAttributes(selected);
            }
            Set<GKInstance> checkOutInstances = new HashSet<GKInstance>();
            checkOutInstances.addAll(selected);
            for (GKInstance tmp : selected) {
                Set<GKInstance> contained = getAllContainedEntities(tmp);
                checkOutInstances.addAll(contained);
            }
            // load all values first before checking out
            for (GKInstance tmp : checkOutInstances) {
                MySQLAdaptor dba = (MySQLAdaptor) tmp.getDbAdaptor();
                dba.fastLoadInstanceAttributeValues(tmp);
            }
            // Want to do a full check out: get all participants for reactions
            checkOut(new ArrayList<GKInstance>(checkOutInstances), parentDialog);
        }
        catch(Exception e) {
            System.err.println("SingleAttributeClassBasedCheck.checkOutSelectedInstances(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame,
                                          "Cannot check out instanes: " + e.getMessage(),
                                          "Error in Checking Out",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    protected Set<GKInstance> loadComplexHasComponent(Collection<GKInstance> instances,
                                                      MySQLAdaptor dba) throws Exception {
        // Need to load all complexes in case some complexes are used by complexes for checking
        progressPane.setText("Load Complex hasComponent...");
        loadAttributes(ReactomeJavaConstants.Complex,
                       ReactomeJavaConstants.hasComponent,
                       dba);
        if (progressPane.isCancelled())
            return null;
        // Try to do a smart loading by for the selected instances and their contained instances only
        Set<GKInstance> toBeLoaded = new HashSet<GKInstance>();
        toBeLoaded.addAll(instances);
        for (GKInstance complex : instances) {
            // Just in case
            if (!complex.getSchemClass().isa(ReactomeJavaConstants.Complex))
                continue;
            Set<GKInstance> components = InstanceUtilities.getContainedComponents(complex);
            toBeLoaded.addAll(components);
        }
        return toBeLoaded;
    }

    protected Set<GKInstance> loadEntitySetMembers(Collection<GKInstance> instances,
                                                   MySQLAdaptor dba) throws Exception {
        // Need to load all complexes in case some complexes are used by complexes for checking
        progressPane.setText("Load EntitySet attribute...");
        loadAttributes(instances,
                       ReactomeJavaConstants.EntitySet, 
                       ReactomeJavaConstants.hasMember, 
                       dba);
        loadAttributes(instances, 
                       ReactomeJavaConstants.CandidateSet, 
                       ReactomeJavaConstants.hasCandidate, 
                       dba);
        Set<GKInstance> toBeLoaded = new HashSet<GKInstance>();
        toBeLoaded.addAll(instances);
        for (GKInstance set : instances) {
            if (set.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                List values = set.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                if (values != null)
                    toBeLoaded.addAll(values);
            }
            if (set.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
                List values = set.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                if (values != null)
                    toBeLoaded.addAll(values);
            }
        }
        return toBeLoaded;
    }

}
