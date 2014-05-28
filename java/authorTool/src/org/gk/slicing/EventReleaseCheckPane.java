/*
 * Created on May 11, 2005
 */
package org.gk.slicing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.gk.database.EventCellRenderer;
import org.gk.database.EventPanePopupManager;
import org.gk.database.HierarchicalEventPane;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TreeUtilities;

/**
 * This customized JPanel is used to do event release by checking an event.
 * 
 * @author wgm
 */

class EventReleaseCheckPane extends JPanel {
    private final String DO_NOT_RELEASE = ReactomeJavaConstants._doNotRelease;    
    private HierarchicalEventPane eventPane;
    private JButton checkBtn;
    private JButton uncheckBtn;
    private JButton resetBtn;
    // To keep the original values for resetting
    private Map originalDnr = new HashMap(); // key: GKInstance value: Boolean or Null    
    // The top level event
    private GKInstance topEvent;
    
    public EventReleaseCheckPane() {
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        JLabel titleLabel = GKApplicationUtilities.createTitleLabel("<html>The following events should be released based on " +
        		"the previous selected top event. Please make necessary changes by checking or unchecking " +
        		"events:</html>");
        add(titleLabel, BorderLayout.NORTH);
        eventPane = new HierarchicalEventPane();
        eventPane.setBorder(BorderFactory.createEtchedBorder());
        add(eventPane, BorderLayout.CENTER);
        ClickableCellRenderer renderer = new ClickableCellRenderer();
        renderer.setNode2IconMap(eventPane.getNode2IconMap());
        final JTree tree = eventPane.getTree();
        tree.setCellRenderer(renderer);
        tree.addMouseListener(createMouseListenerForTree(tree));
        eventPane.setPopupType(EventPanePopupManager.DB_AUTHOR_TOOL_TYPE);
        JPanel controlPane = new JPanel();
        controlPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(controlPane, BorderLayout.SOUTH);
        checkBtn = new JButton("Check");
        checkBtn.setActionCommand("check");
        checkBtn.setEnabled(false);
        uncheckBtn = new JButton("Uncheck");
        uncheckBtn.setActionCommand("uncheck");
        uncheckBtn.setEnabled(false);
        resetBtn = new JButton("Reset");
        resetBtn.setEnabled(false);
        resetBtn.setActionCommand("reset");
        controlPane.add(checkBtn);
        controlPane.add(uncheckBtn);
        controlPane.add(resetBtn);
        eventPane.addSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                GKInstance event = eventPane.getSelectedEvent();
                if (event != null) {
                    checkBtn.setEnabled(true);
                    uncheckBtn.setEnabled(true);
                }
                else {
                    checkBtn.setEnabled(false);
                    uncheckBtn.setEnabled(false);
                }
            }
        });
        ActionListener actions = createCheckActions(tree);
        checkBtn.addActionListener(actions);
        uncheckBtn.addActionListener(actions);
        resetBtn.addActionListener(actions);
    }
    
    private ActionListener createCheckActions(final JTree eventTree) {
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();
                if (command.equals("check")) {
                    check(eventTree, true);
                }
                else if (command.equals("uncheck")) {
                    check(eventTree, false);
                }
                else if (command.equals("reset")) {
                    reset(eventTree);
                }
            }
        };
        return l;
    }
    
    private void reset(JTree eventTree) {
        GKInstance event = null;
        Boolean dnr = null;
        DefaultMutableTreeNode treeNode = null;
        DefaultTreeModel treeModel = (DefaultTreeModel) eventTree.getModel();
        List treeNodes = null;
        try {
            for (Iterator it = originalDnr.keySet().iterator(); it.hasNext();) {
                event = (GKInstance) it.next();
                dnr = (Boolean) originalDnr.get(event);
                event.setAttributeValue(DO_NOT_RELEASE, dnr);
                treeNodes = TreeUtilities.searchNodes(event, eventTree);
                for (Iterator it1 = treeNodes.iterator(); it1.hasNext();) {
                    treeNode = (DefaultMutableTreeNode) it1.next();
                    treeModel.nodeChanged(treeNode);
                }
            }
            originalDnr.clear();
            resetBtn.setEnabled(false);
        }
        catch (Exception e) {
            System.err.println("EventReleaseWizard.reset(): " + e);
            e.printStackTrace();
        }
    }
    
    private void check(JTree eventTree, boolean isForCheck) {
        TreePath[] paths = eventTree.getSelectionPaths();
        if (paths == null || paths.length == 0)
            return;
        DefaultTreeModel treeModel = (DefaultTreeModel) eventTree.getModel();
        GKInstance event = null;
        DefaultMutableTreeNode treeNode = null;
        Boolean dnr = null;
        try {
            if (isForCheck) {
                for (int i = 0; i < paths.length; i++) {
                    treeNode = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                    event = (GKInstance) treeNode.getUserObject();
                    dnr = (Boolean) event.getAttributeValue(DO_NOT_RELEASE);
                    if (dnr != null && !dnr.booleanValue())
                        continue;
                    setDNR(event, dnr, Boolean.FALSE);
                    treeModel.nodeChanged(treeNode);
                }
            }
            else {
                for (int i = 0; i < paths.length; i++) {
                    treeNode = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                    event = (GKInstance) treeNode.getUserObject();
                    dnr = (Boolean) event.getAttributeValue(DO_NOT_RELEASE);
                    if (dnr == null || dnr.booleanValue())
                        continue; // For true
                    setDNR(event, dnr, Boolean.TRUE);
                    treeModel.nodeChanged(treeNode);
                }
            }
        }
        catch (Exception e) {
            System.err.println("EventReleaseWizard.check(): " + e);
            e.printStackTrace();
        }
    }
    
    private void setDNR(GKInstance event, Boolean oldValue, Boolean newValue) throws Exception {
        event.setAttributeValue(DO_NOT_RELEASE, newValue);
        if (!originalDnr.containsKey(event)) { // This should be done once to keep the original value
            originalDnr.put(event, oldValue);
            resetBtn.setEnabled(true);
        }
    }

    private void toggleDNR(DefaultMutableTreeNode treeNode, 
                           DefaultTreeModel treeModel) throws Exception {
        GKInstance event = (GKInstance) treeNode.getUserObject();
        Boolean dnr = (Boolean)event.getAttributeValue(DO_NOT_RELEASE);
        Boolean newValue = null;
        if (dnr == null || dnr.booleanValue())
            newValue = Boolean.FALSE;
        else
            newValue = Boolean.TRUE;
        setDNR(event, dnr, newValue);
        treeModel.nodeChanged(treeNode);
        //Make al contained events have the same _doNotRelease values
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            toggleDNR(childNode, treeModel, newValue);
        }
    }
    
    private void toggleDNR(DefaultMutableTreeNode treeNode, 
                           DefaultTreeModel model, 
                           Boolean newValue) throws Exception {
        GKInstance event = (GKInstance) treeNode.getUserObject();
        Boolean dnr = (Boolean) event.getAttributeValue(ReactomeJavaConstants._doNotRelease);
        if (dnr == null && newValue.booleanValue())
            return;
        if (newValue.equals(dnr))
            return;
        setDNR(event, dnr, newValue);
        model.nodeChanged(treeNode);
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            toggleDNR(childNode, model, newValue);
        }
    }
    
    private MouseListener createMouseListenerForTree(final JTree tree) {
        MouseListener l = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                TreePath path = tree.getPathForLocation(x, y);
                if (path == null)
                    return ; // Nothing selected
                Rectangle rect = tree.getPathBounds(path);
                if (x < (rect.getX() + 16)) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                    GKInstance event = (GKInstance) treeNode.getUserObject();
                    try {
                        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                        toggleDNR(treeNode, model);
                    }
                    catch(Exception e1) {
                        System.err.println("EventReleaseWizard.createMouseListener(): " + e1);
                        e1.printStackTrace();
                    }
                }
            }
        };
        return l;
    }
    
    public void setTopEvent(GKInstance event, MySQLAdaptor dba) {
        if (topEvent == event)
            return;
        this.topEvent = event;
        if (topEvent == null)
            return;
        List topLevelIDs = new ArrayList(1);
        topLevelIDs.add(topEvent.getDBID());
        EventSlicingHelper slicingHelper = new EventSlicingHelper();
        try {
            Map releaseEventMap = slicingHelper.extractAllEvents(topLevelIDs, dba);
            // Need to figure out the top level events
            List topLevelEvents = InstanceUtilities.grepTopLevelEvents(releaseEventMap.values());
            eventPane.setTopLevelEvents(topLevelEvents);
            eventPane.setSelectedEvent(topEvent);
        }
        catch(Exception e) {
            System.err.println("EventReleaseCheckPane.setTopEvent(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Create a new DefaultTreeModel that contains only events to be released.
     * @return
     */
    public List getReleasedTopLevelEvents() {
        Collection releasedEvents = getReleasedEvents();
        try {
            return grepTopLevelEvents(releasedEvents);
        }
        catch (Exception e) {
            System.err.println("EventReleaseCheckPane.getReleaseTopLevelEvents(): " + e);
            e.printStackTrace();
        }
        return new ArrayList();
    }
    
	/**
	 * Grep the top level events from the specified collection of events. An top level
	 * event is an event 
	 * @param events
	 * @return
	 * @throws Exception
	 */
	private List grepTopLevelEvents(Collection events) throws Exception {
	    // Grep all events that are contained by other events
	    Set containedEvents = new HashSet();
	    GKInstance event = null;
	    for (Iterator it = events.iterator(); it.hasNext();) {
	        event = (GKInstance) it.next();
	        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
	            List components = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
	            if (components != null && components.size() > 0) { 
	                for (Iterator it1 = components.iterator(); it1.hasNext();) {
	                    GKInstance tmp = (GKInstance) it1.next();
	                    Boolean dnr = (Boolean) tmp.getAttributeValue(DO_NOT_RELEASE);
	                    if (dnr != null && !dnr.booleanValue())
	                        containedEvents.add(tmp);
	                }
	            }
	        }
	    }
	    List topEvents = new ArrayList(events);
	    topEvents.removeAll(containedEvents);
	    return topEvents;
	}
    
    /**
     * A helper to get all events should be released.
     * @return
     */
    private Collection getReleasedEvents() {
        Set events = new HashSet();
        Collection topEvents = eventPane.getTopLevelEvents();
        Set current = new HashSet();
        current.addAll(topEvents);
        Set next = new HashSet();
        GKInstance event = null;
        List values = null;
        Boolean dnr = null;
        try {
            while (current.size() > 0) {
                for (Iterator it = current.iterator(); it.hasNext();) {
                    event = (GKInstance) it.next();
                    dnr = (Boolean) event.getAttributeValue(DO_NOT_RELEASE);
                    if (dnr != null && !dnr.booleanValue())
                        events.add(event);
                    if (event.getSchemClass().isValidAttribute("hasComponent")) {
                        values = event.getAttributeValuesList("hasComponent");
                        if (values != null)
                            next.addAll(values);
                    }
                    if (event.getSchemClass().isValidAttribute("hasInstance")) {
                        values = event.getAttributeValuesList("hasInstance");
                        if (values != null)
                            next.addAll(values);
                    }
                }
                current.clear();
                current.addAll(next);
                next.clear();
            }
        }
        catch(Exception e) {
            System.err.println("EventReleaseCheckPane.getReleasedEvents(): " + e);
            e.printStackTrace();
        }
        return events;
    }
    
    /**
     * Get the list of GKInstance objects whose release statuses have been changed.
     * @return a list of GKInstance objects.
     */
    public List getChangedEvents() {
        return new ArrayList(originalDnr.keySet());
    }
    
    
    class ClickableCellRenderer extends EventCellRenderer {
        private JLabel dnrLabel;
        private ImageIcon selectedIcon;
        private ImageIcon nonselectedIcon;
        
        public ClickableCellRenderer() {
            super();
            // Have to add clicklable
            Component[] comps = getComponents();
            removeAll();
            dnrLabel = new JLabel();
            add(dnrLabel);
            for (int i = 0; i < comps.length; i++)
                add(comps[i]);
        }
        
        protected void initIcons() {
            super.initIcons();
    		selectedIcon = GKApplicationUtilities.createImageIcon(getClass(), "Selected.png");
    		nonselectedIcon = GKApplicationUtilities.createImageIcon(getClass(), "Unselected.png");
        }
        
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
            GKInstance event = (GKInstance)treeNode.getUserObject();
            if (event != null) {
                // Check _doNotRelease
                try {
                    Boolean doNotRelease = (Boolean) event.getAttributeValue(DO_NOT_RELEASE);
                    if (doNotRelease == null || doNotRelease.booleanValue())
                        dnrLabel.setIcon(nonselectedIcon);
                    else
                        dnrLabel.setIcon(selectedIcon);
                }
                catch(Exception e) {
                    System.err.println("EventReleaseWizard.getTreecellRendererComponent(): " + e);
                    e.printStackTrace();
                }
            }
            super.getTreeCellRendererComponent(tree,
                    value,
                    sel,
                    expanded,
                    leaf,
                    row,
                    hasFocus);
            invalidate();
            return this;
        }
    }
}