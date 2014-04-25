/*
 * Created on Apr 8, 2005
 *
 */
package org.gk.qualityCheck;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.gk.database.AttributePane;
import org.gk.database.InstanceListPane;
import org.gk.database.SchemaDisplayPane;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * This QualityCheck implementation is used to check if required attributes are filled.
 * This checking should be gone after required attributes are defined in the schema.
 * @author wgm
 *
 */
public class RequiredAttributesCheck extends AbstractQualityCheck {
    // The type used for checking: SchemaAttribute.REQUIRED or MANDATORY
    private int checkedType;
    private String checkedTypeLabel;
    // Used for display checked list
    private InstanceListPane displayedList;
    
    public RequiredAttributesCheck() {
        setCheckedType(SchemaAttribute.REQUIRED);
        setCheckedTypeLabel("required");
    }
    
    public void setCheckedType(int type) {
        this.checkedType = type;
    }
    
    public void setCheckedTypeLabel(String label) {
        this.checkedTypeLabel = label;
    }
    
    
    public void checkProject(final GKInstance event) {
        validateDataSource();
        Thread t = new Thread() {
            public void run() {
                try {
                    initProgressPane("Check " + checkedTypeLabel + " Attributes");
                    Set<GKInstance> instances = loadInstancesInProject(event);
                    if (progressPane.isCancelled())
                        return;
                    Map offendMap = new HashMap();
                    checkInstances(instances, offendMap);
                    if (!progressPane.isCancelled())
                        displayResults(offendMap);
                    hideProgressPane();
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("RequiredAttributesChecker.checkProject(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    public void check() {
        validateDataSource();
        if (!checkIsNeedEscape())
            return;
        // Use a new thread so that the progress can be monitored
        Thread t = new Thread() {
            public void run() {
                try {
                    // Get the Reaction class
                    Schema schema = dataSource.getSchema();
                    Map offendMap = new HashMap();
                    SchemaClass cls = null;
                    // To divide the task, check the first level classes under the root
                    GKSchemaClass rootCls = (GKSchemaClass) ((GKSchema)schema).getRootClass();
                    Collection classes = rootCls.getSubClasses();
                    initProgressPane("Check " + checkedTypeLabel + " Attributes");
                    progressPane.setText("Checking " + checkedTypeLabel + " attributes...");
                    int count = 0;
                    List offendInstances = new ArrayList();
                    int escaped = 0;
                    for (Iterator it = classes.iterator(); it.hasNext() && !progressPane.isCancelled();) {
                        cls = (SchemaClass) it.next();
                        progressPane.setText("Loading " + cls.getName() + " instances...");
                        offendInstances.clear();
                        Collection instances = dataSource.fetchInstancesByClass(cls);
                        escaped += escapeInstancesWithNumber(instances);
                        // In case it is canceled
                        if (progressPane.isCancelled())
                            break;
                        if (instances == null || instances.size() == 0)
                            continue;
                        progressPane.setText("Loading attributes ...");
                        if (dataSource instanceof MySQLAdaptor) {
                            loadAttributes(instances, (MySQLAdaptor)dataSource);
                            if (progressPane.isCancelled())
                                break;
                        }
                        progressPane.setText("Checking " + cls.getName() + " instances...");
                        for (Iterator it1 = instances.iterator(); it1.hasNext() && !progressPane.isCancelled();) {
                            GKInstance instance = (GKInstance) it1.next();
                            if (!checkRequiredAtts(instance))
                                offendInstances.add(instance);
                        }
                        for (Iterator it1 = offendInstances.iterator(); it1.hasNext() && !progressPane.isCancelled();) {
                            GKInstance instance = (GKInstance) it1.next();
                            List list = (List) offendMap.get(instance.getSchemClass());
                            if (list == null) {
                                list = new ArrayList();
                                offendMap.put(instance.getSchemClass(), list);
                            }
                            list.add(instance);
                        }
                        progressPane.setValue(++count);
                    }
                    if (!progressPane.isCancelled()) {
                        hideProgressPane();
                        showEscapeResult(escaped);
                        displayResults(offendMap);
                    }
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("RequiredAttributesChecker.check(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    private void showEscapeResult(int escaped) {
        if (escaped > 0) {
            String message = null;
            if (escaped == 1)
                message = "One instance is";
            else 
                message = escaped + " instatnces are";
            message = message + " in the escaped list, and have not been checked.";
            JOptionPane.showMessageDialog(parentComp,
                                          message,
                                          "Escape QA",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    protected int escapeInstancesWithNumber(Collection<GKInstance> instances) throws Exception {
        if (instances == null || instances.size() == 0 || !escapeHelper.isNeedEscape())
            return 0;
        if(!loadAttributesForQAEscape(instances))
            return 0;
        int escaped = 0;
        // Do escape
        for (Iterator<GKInstance> it = instances.iterator(); it.hasNext();) {
            GKInstance inst = it.next();
            if (escapeHelper.shouldEscape(inst)) {
                it.remove();
                escaped ++;
            }
        }
        return escaped;
    }

    private void loadAttributes(Collection instances, MySQLAdaptor dba) {
        // Sorting instances based on classes
        Map clsMap = new HashMap();
        GKInstance instance = null;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            List list = (List) clsMap.get(instance.getSchemClass());
            if (list == null) {
                list = new ArrayList();
                clsMap.put(instance.getSchemClass(), list);
            }
            list.add(instance);
        }
        // Load attributes based on classes
        try {
            for (Iterator it = clsMap.keySet().iterator(); it.hasNext();) {
                GKSchemaClass cls = (GKSchemaClass) it.next();
                List atts = new ArrayList();
                // Try to get all required attributes
                for (Iterator it1 = cls.getAttributes().iterator(); it1.hasNext();) {
                    SchemaAttribute att = (SchemaAttribute) it1.next();
                    if (att.getCategory() == checkedType)
                        atts.add(att);
                }
                dba.loadInstanceAttributeValues((List)clsMap.get(cls), atts);
            }
        } 
        catch (Exception e) {
            System.err.println("RequiredAttributesCheck.loadAttributes(): " + e);
            e.printStackTrace();
        }
    }
    
    protected InstanceListPane getDisplayedList() {
        return displayedList;
    }
    
    private void displayResults(Map offendMap) {
        if (offendMap.size() == 0) {
            JOptionPane.showMessageDialog(parentComp,
                    "All " + checkedTypeLabel + " attributes are filled for instances!",
                    "Attributes Check Results",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFrame frame = new JFrame("Attributes Check Results");
        ResultDisplayPane displayPane = new ResultDisplayPane();
        displayedList = displayPane.instancePane;
        displayPane.schemaInstanceJSP.setDividerLocation(266);
        displayPane.jspAttributeJSP.setDividerLocation(532);
        displayPane.setDisplayedInstance(offendMap);
        displayPane.titleArea.setText("Total instances whose " + checkedTypeLabel + " attributes have not been filled: " + 
                getCounterFromMap(offendMap));
        List topLevelClses = grepTopLevelClasses(offendMap.keySet());
        displayPane.schemaPane.setTopLevelSchemaClasses(topLevelClses);
        frame.getContentPane().add(displayPane, BorderLayout.CENTER);
        
        // Add a control pane
        CheckOutControlPane controlPane = createControlPane(frame);
        final JButton checkOutBtn = controlPane.getCheckOutBtn();
        displayPane.instancePane.addSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                List selection = getDisplayedList().getSelection();
                if (selection.size() == 0)
                    checkOutBtn.setEnabled(false);
                else
                    checkOutBtn.setEnabled(true);
            }
        });
        
        frame.getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        // To synchronize changes from other places
        if (displayPane.attributePane.isEditable()) {
            registerAttributePane(displayPane.attributePane, frame);
        }
        showResultFrame(frame);
    }
    
    private int getCounterFromMap(Map offendMap) {
        int c = 0;
        List list = null;
        for (Iterator it = offendMap.keySet().iterator(); it.hasNext();) {
            list = (List) offendMap.get(it.next());
            if (list != null)
                c += list.size();
        }
        return c;
    }
    
    
    /**
     * Check if the specified GKInstance has all required attributes filled.
     * @param instance
     * @return
     * @throws Exception
     */
    private boolean checkRequiredAtts(GKInstance instance) throws Exception {
        // Escape shell instances
        if (instance.isShell())
            return true;
        SchemaAttribute att = null;
        for (Iterator it = instance.getSchemClass().getAttributes().iterator(); it.hasNext();) {
            att = (SchemaAttribute) it.next();
            if (att.getCategory() == checkedType) {
                if (instance.getAttributeValue(att) == null)
                    return false;
            }
        }
        return true;
    }
    
    public void check(GKInstance instance) {
        List list = new ArrayList(1);
        list.add(instance);
        check(list);
    }
    
    public void check(final List instances) {
        // Use a new thread so that the progress can be monitored
        Thread t = new Thread() {
            public void run() {
                try {
                    // Get the Reaction class
                    Map offendMap = new HashMap();
                    initProgressPane("Check " + checkedTypeLabel + " Attributes");
                    progressPane.setText("Checking " + checkedType + " attributes...");
                    progressPane.setMaximum(instances.size());
                    checkInstances(instances, offendMap);
                    if (progressPane.isCancelled())
                        return;
                    hideProgressPane();
                    // Display 
                    // Need to get the toplevel class
                    displayResults(offendMap);
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("RequiredAttributesChecker.check(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    private void checkInstances(Collection instances, Map offendMap) throws Exception {
        if (instances == null || instances.size() == 0)
            return;
        progressPane.setMaximum(instances.size());
        progressPane.setValue(0);
        progressPane.setText("Loading attributes ...");
        if (dataSource instanceof MySQLAdaptor) {
            loadAttributes(instances, (MySQLAdaptor)dataSource);
        }
        progressPane.setText("Checking instances...");
        int c = 0;
        for (Iterator it1 = instances.iterator(); it1.hasNext() && !progressPane.isCancelled();) {
            GKInstance instance = (GKInstance) it1.next();
            if (!checkRequiredAtts(instance)) {
                List list = (List) offendMap.get(instance.getSchemClass());
                if (list == null) {
                    list = new ArrayList();
                    offendMap.put(instance.getSchemClass(), list);
                }
                list.add(instance);
            }
            progressPane.setValue(++c);
        }
    }
    
    public void check(final GKSchemaClass cls) {
        validateDataSource();
        if (!checkIsNeedEscape())
            return;
        // Use a new thread so that the progress can be monitored
        Thread t = new Thread() {
            public void run() {
                try {
                    // Get the Reaction class
                    Map offendMap = new HashMap();
                    initProgressPane("Check " + checkedTypeLabel + " Attributes");
                    //progressPane.setText("Checking " + checkedTypeLabel + " attributes for " + cls.getName() + "...");
                    progressPane.setText("Loading instances...");
                    Collection instances = dataSource.fetchInstancesByClass(cls);
                    int escaped = escapeInstancesWithNumber(instances);
                    checkInstances(instances, offendMap);
                    if (!progressPane.isCancelled()) {
                        hideProgressPane();
                        showEscapeResult(escaped);
                        // Display 
                        displayResults(offendMap);
                    }
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("RequiredAttributesChecker.check(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    /**
     * Grep the top-level classes from the listed classes.
     * @param clses
     * @return
     */
    private List grepTopLevelClasses(Collection clses) {
        List topLevelClasses = new ArrayList(clses);
        GKSchemaClass cls = null;
        for (Iterator it = topLevelClasses.iterator(); it.hasNext();) {
            cls = (GKSchemaClass) it.next();
            for (Iterator it1 = topLevelClasses.iterator(); it1.hasNext();) {
                GKSchemaClass tmp = (GKSchemaClass) it1.next();
                if (tmp == cls)
                    continue;
                if (cls.isa(tmp)) {
                    it.remove();
                    break;
                }
            }
        }
        return topLevelClasses;
    }
    
    // Need a customized SchemaViewPane to display the results. This customized Panel is very similar to
    // SchemaViewPane. But since the behaviors in SchemaViewPane is too specific to a PersistenceAdaptor,
    // it is easier to create such a panel from scratch.
    class ResultDisplayPane extends JPanel {
        // For title
        private JTextArea titleArea;
        private SchemaDisplayPane schemaPane;
        private InstanceListPane instancePane;
        private AttributePane attributePane;
        private JSplitPane schemaInstanceJSP;
        private JSplitPane jspAttributeJSP;
        // The actual instances to be displayed
        private Map clsInstanceMap;
        
        private ResultDisplayPane() {
            init();
        }
        
        private void init() {
            setLayout(new BorderLayout());
            titleArea = new JTextArea();
            titleArea.setEditable(false);
            titleArea.setWrapStyleWord(true);
            titleArea.setLineWrap(true);
            titleArea.setBackground(getBackground());
            titleArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            titleArea.setFont(titleArea.getFont().deriveFont(Font.BOLD));
            add(titleArea, BorderLayout.NORTH);
            titleArea.setText("Choose a class to view instances whose " + checkedTypeLabel + " attributes have not been filled:");
            Dimension miniSize = new Dimension(10, 100);
            schemaPane = new SchemaDisplayPane();
            schemaPane.setMinimumSize(miniSize);
            instancePane = new InstanceListPane();
            instancePane.setMinimumSize(miniSize);
            attributePane = new AttributePane();
            attributePane.setMinimumSize(miniSize);
            // Use this way to group required/mandatory attributes together
            // so that it is easy to be viewed by the user.
            attributePane.setGroupAttributesByCategory(true);
            schemaInstanceJSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, schemaPane, instancePane);
            schemaInstanceJSP.setResizeWeight(0.5);
            jspAttributeJSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, schemaInstanceJSP, attributePane);
            jspAttributeJSP.setResizeWeight(0.67);
            add(jspAttributeJSP, BorderLayout.CENTER);
            // Add listeners
            schemaPane.addSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent event) {
                    GKSchemaClass schemaClass = schemaPane.getSelectedClass();
                    displayInstances(schemaClass);
                }
            });
            instancePane.addSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    java.util.List selected = instancePane.getSelection();
                    if (selected.size() == 1) 
                        attributePane.setInstance((Instance)selected.get(0));
                    else
                        attributePane.setInstance(null);
                }
            });
            // No searching is needed
            schemaPane.setSearchPaneVisible(false);
            if (dataSource instanceof XMLFileAdaptor) {
                instancePane.setEditable(true);
                attributePane.setEditable(true);
            }
        }
        
        public void setDisplayedInstance(Map clsInstanceMap) {
            this.clsInstanceMap = clsInstanceMap;
            Map counterMap = new HashMap();
            SchemaClass cls = null;
            List instances = null;
            int counter = 0;
            for (Iterator it = dataSource.getSchema().getClasses().iterator(); it.hasNext();) {
                cls = (SchemaClass) it.next();
                instances = (List) clsInstanceMap.get(cls);
                counter = (instances == null ? 0 : instances.size());
                counterMap.put(cls, new Long(counter));
            }
            schemaPane.setClassCounts(counterMap);
        }
        
        private void displayInstances(GKSchemaClass cls) {
            Set attNames = new HashSet();
            SchemaAttribute att = null;
            for (Iterator it = cls.getAttributes().iterator(); it.hasNext();) {
                att = (SchemaAttribute) it.next();
                if (att.getCategory() == checkedType) {
                    attNames.add(att.getName());
                }
            }
            if (attNames == null || attNames.size() == 0) {
                titleArea.setText("There are no " + checkedTypeLabel + " attributes for class \"" + cls.getName() + "\"");
                return;
            }
            StringBuffer buffer = new StringBuffer();
            buffer.append(upFirst(checkedTypeLabel) + " attributes for class \""); 
            buffer.append(cls.getName());
            buffer.append("\" ");
            buffer.append(" are: ");
            String attName = null;
            for (Iterator it = attNames.iterator(); it.hasNext();) {
                attName = (String) it.next();
                buffer.append(attName);
                if (it.hasNext())
                    buffer.append(", ");
                else
                    buffer.append(".");
            }
            titleArea.setText(buffer.toString());
            // Set the displayed instances in the instance pane
            List list = new ArrayList();
            List tmp = (List) clsInstanceMap.get(cls);
            if (tmp != null)
                list.addAll(tmp);
            InstanceUtilities.sortInstances(list);
            instancePane.setDisplayedInstances(list);
        }
        
        private String upFirst(String name) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }
    
}
