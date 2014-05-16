/*
 * Created on 27-Sep-2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gk.pathView;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.gk.database.EventCellRenderer;
import org.gk.database.EventTreeBuildHelper;
import org.gk.database.FrameManager;
import org.gk.database.SingleEventTreeView;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaClass;
import org.gk.util.TreeUtilities;
/**
 * @author vastrik
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class PathwayBrowserPanel extends JPanel {
	
	private JTree eventTree;
	private JLabel dbLabel;
	private Map node2icon = new HashMap();
	private TreeSelectionListener treeSelectionListener;
	private MySQLAdaptor dba;
	// For display selected instance
	private SingleEventTreeView treeView;
	private JSplitPane jsp;
	
	public PathwayBrowserPanel() {
		init();
	}
	
	public MySQLAdaptor getMySQLAdaptor() {
		return this.dba;
	}
	
	public void setMySQLAdaptor(MySQLAdaptor dba) {
		setMySQLAdaptor(dba, false);
	}
	
	public void setMySQLAdaptor(MySQLAdaptor adaptor, boolean updateTree) {
		this.dba = adaptor;
		dbLabel.setText(dba.toString());
		if (!updateTree)
			return;
		try {
			dba.refresh(); // In case the tree is loaded before.
			EventTreeBuildHelper treeHelper = new EventTreeBuildHelper(dba);
			Collection topEvents = treeHelper.getTopLevelEvents();
			// Only "homo sapiens" is needed
//			Collection spc = dba.fetchInstanceByAttribute("Species", "name", "=", "Homo sapiens");
//			GKInstance human = (GKInstance) spc.iterator().next();
//			// Get the name for the taxon. 
//			SchemaAttribute taxonAtt = null;
			SchemaClass eventCls = dba.getSchema().getClassByName("Event");
//			if (eventCls.isValidAttribute("taxon"))
//			    taxonAtt = eventCls.getAttribute("taxon");
//			else if (eventCls.isValidAttribute("species"))
//			    taxonAtt = eventCls.getAttribute("species");
//			dba.loadInstanceAttributeValues(topEvents, taxonAtt);
//			List humanTopEvents = new ArrayList();
//			for (Iterator it = topEvents.iterator(); it.hasNext();) {
//			    GKInstance event = (GKInstance) it.next();
//			    GKInstance taxon = (GKInstance) event.getAttributeValue(taxonAtt);
//			    if (taxon == null)
//			        continue;
//			    if (taxon.getDBID().equals(human.getDBID()))
//			        humanTopEvents.add(event);
//			}
			Collection events = dba.fetchInstancesByClass("Event");
			treeHelper.loadAttribtues(events);
			//dba.loadInstanceAttributeValues(events, dba.getSchema().getClassByName("GenericEvent").getAttribute("hasInstance"));
			//dba.loadInstanceAttributeValues(events, dba.getSchema().getClassByName("Pathway").getAttribute("hasComponent"));
			dba.loadInstanceAttributeValues(events, eventCls.getAttribute("precedingEvent"));
			// All events have already been loaded. There is no need to call this method again.
			//dba.loadInstanceReverseAttributeValues(events, dba.getSchema().getClassByName("Event").getAttribute("precedingEvent"));
			EventTreeBuildHelper.cacheReverseAttributes(events);
             buildTree(topEvents);
			//buildTree(humanTopEvents);
		}
		catch (Exception e) {
			System.err.println("PathwayBrowserPanel.setMySQLAdaptor(): " + e);
			e.printStackTrace();
		}
	}
	
	private void init() {
		setLayout(new BorderLayout());
		JPanel browserPane = new JPanel();
		browserPane.setLayout(new BorderLayout());
		dbLabel = new JLabel();
		dbLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); 
		browserPane.add(dbLabel, BorderLayout.NORTH);
		eventTree = new JTree();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		DefaultTreeModel model = new DefaultTreeModel(root);
		eventTree.setModel(model);
		eventTree.setShowsRootHandles(true);
		EventCellRenderer renderer = new EventCellRenderer();
		renderer.setNode2IconMap(node2icon);
		eventTree.setCellRenderer(renderer);
		eventTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		browserPane.add(new JScrollPane(eventTree), BorderLayout.CENTER);
		// To see the contents of the event
		eventTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					doTreePopup(e);
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doTreePopup(e);
			}
		});
		// To display tree view
		treeView = new SingleEventTreeView();
		jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, browserPane, treeView);
		jsp.setResizeWeight(0.5);
		jsp.setOneTouchExpandable(true);
		add(jsp, BorderLayout.CENTER);
		// To synchronize selection
		eventTree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) eventTree.getLastSelectedPathComponent();
				if (treeNode == null)
					treeView.setEvent(null);
				else
					treeView.setEvent((GKInstance)treeNode.getUserObject());
			}
		});
		treeView.setMinimumSize(new Dimension(20, 20));
	}
	
	public void setSplitPaneOrientation(int orientation) {
    if (orientation != jsp.getOrientation()) {
      jsp.setOrientation(orientation);
      // To control the divider position
      if (orientation == JSplitPane.VERTICAL_SPLIT) {
          double pos = jsp.getDividerLocation() / (double) getWidth();
          jsp.setDividerLocation((int)(pos * getHeight()));
      }
      else {
        double pos = jsp.getDividerLocation() / (double) getHeight();
        jsp.setDividerLocation((int)(pos * getWidth()));
      }
    }
	}
	
	private void doTreePopup(MouseEvent e) {
		TreePath[] paths = eventTree.getSelectionPaths();
		if (paths == null || paths.length != 1)
			return;
		JPopupMenu popup = new JPopupMenu();
		JMenuItem viewItem = new JMenuItem("View Event");
		viewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showSelectedEvent();
			}
		});
		popup.add(viewItem);
		popup.show(eventTree, e.getX(), e.getY());		
	}
	
	private void showSelectedEvent() {
		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) eventTree.getLastSelectedPathComponent();
		GKInstance event = (GKInstance) treeNode.getUserObject();
		FrameManager.getManager().showInstance(event);
	}
	
	public void buildTree(GKInstance event) throws InvalidAttributeException, Exception {
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(event);
		root.add(node);
		EventTreeBuildHelper.buildTree(node, event, node2icon);
	}

	public void buildTree(Collection events) throws InvalidAttributeException, Exception {
		List eventList = new ArrayList();
		eventList.addAll(events);
		InstanceUtilities.sortInstances(eventList);
		for (Iterator i = eventList.iterator(); i.hasNext();) {
			GKInstance event = (GKInstance) i.next();
			buildTree(event);
		}
		eventTree.expandRow(0);
		eventTree.setRootVisible(false);
	}
	
	public JTree getEventTree() {
		return this.eventTree;
	}
	
	public GKInstance getSelectedInstance() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) eventTree.getLastSelectedPathComponent();
		if (node == null)
			return null;
		return (GKInstance) node.getUserObject();
	}
	
	public Collection getSelectedEvents() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) eventTree.getLastSelectedPathComponent();
		if (node == null)
			return null;
		Collection events = new HashSet();
		events.add(node.getUserObject());
		for (Enumeration ne = node.breadthFirstEnumeration(); ne.hasMoreElements();) {
			DefaultMutableTreeNode descendant = (DefaultMutableTreeNode) ne.nextElement();
			events.add(descendant.getUserObject());
		}
		return events;
	}
	
	public void addSelectionListener(TreeSelectionListener listener) {
		treeSelectionListener = listener;
		eventTree.getSelectionModel().addTreeSelectionListener(listener);
	}
	
	public void removeSelectionListener(TreeSelectionListener listener) {
		eventTree.getSelectionModel().removeTreeSelectionListener(listener);
	}
	
	public void highliteNodes(Collection events) {
		eventTree.clearSelection();
		//collpaseAll();
		// Do selection now
		GKInstance event;
		for (Iterator it = events.iterator(); it.hasNext();) {
			event = (GKInstance) it.next();
			select(event);
		}
	}
	
	private void collpaseAll() {
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		Stack stack = new Stack();
		int size = root.getChildCount();
		DefaultMutableTreeNode treeNode;
		TreePath path = null;
		for (int i = 0; i < size; i++) { // To escape collapsing the root
			treeNode = (DefaultMutableTreeNode) root.getChildAt(i);
			path = new TreePath(model.getPathToRoot(treeNode));
			if (eventTree.isExpanded(path))
				TreeUtilities.collapseAllNodes(treeNode, eventTree);
		}
	}
	
	public void select(GKInstance event) {
		if (event == null)
			return;
		java.util.List pathList = getPaths(event);
		// Search the tree node based on paths
		java.util.List path;
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		for (Iterator it = pathList.iterator(); it.hasNext();) {
			path = (java.util.List) it.next();
			TreePath treePath = searchTreePath(path, root, model);
			if (treePath != null) {
				eventTree.addSelectionPath(treePath);
			}
		}
		eventTree.scrollRowToVisible(eventTree.getMinSelectionRow());		
	}
	
  /**
   * Get the list of events under the specified event in the tree, including event.
   * @param event
   * @return
   */
  public Collection getEventsUnder(GKInstance event) {
    Set events = new HashSet();
    DefaultMutableTreeNode node = TreeUtilities.searchNode(event, eventTree);
    if (node == null)
      return events;
    DefaultMutableTreeNode tmp = null;
    for (Enumeration enum1 = node.breadthFirstEnumeration(); enum1.hasMoreElements();) {
      tmp = (DefaultMutableTreeNode) enum1.nextElement();
      events.add(tmp.getUserObject());
    }
    return events;
  }
  
	private TreePath searchTreePath(java.util.List path, DefaultMutableTreeNode root, DefaultTreeModel model) {
		GKInstance event;
		DefaultMutableTreeNode anchor = root;
		boolean isFound = false;
		for (Iterator it = path.iterator(); it.hasNext();) {
			event = (GKInstance) it.next();
			isFound = false;
			for (int i = 0; i < anchor.getChildCount(); i++) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) anchor.getChildAt(i);
				if (treeNode.getUserObject() == event) {
					anchor = treeNode;
					isFound = true;
					break;
				}
			}
			if (!isFound)
				return null;
		}
		return new TreePath(model.getPathToRoot(anchor));
	}
	
	private java.util.List getPaths(GKInstance event) {
		java.util.List pathList = new ArrayList();
		java.util.List firstPath = new ArrayList();
		pathList.add(firstPath);
		firstPath.add(0, event);
		getPaths(event, firstPath, pathList);
		return pathList;
	}
	
	private void getPaths(GKInstance startEvent, java.util.List firstPath, java.util.List pathList) {
		java.util.List parents = new ArrayList(); 
		String[] reverseAttNames = EventTreeBuildHelper.getReverseTreeAttributeNames();
		for (int i = 0; i < reverseAttNames.length; i++) {
		    java.util.List list = startEvent.getAttributeValuesListNoCheck(reverseAttNames[i]);
		    if (list != null)
		        parents.addAll(list);
		}
		if (parents.size() > 0) {
			GKInstance parent;
			// Start from the second index
			if (parents.size() > 1) {
				for (int i = 1; i < parents.size(); i++) {
					// Copy the path
					java.util.List path1 = new ArrayList(firstPath);
					pathList.add(path1);
					parent = (GKInstance) parents.get(i);
					path1.add(0, parent);
					getPaths(parent, path1, pathList);
				}
			}
			parent = (GKInstance) parents.get(0);
			firstPath.add(0, parent);
			getPaths(parent, firstPath, pathList);	
		}		
	}
	
	public void search(String name) {
		eventTree.clearSelection();
		//collpaseAll();
		GKInstance event = null;
		try {
			Collection events = dba.fetchInstanceByAttribute("Event", "_displayName", "=", name);
			if (events != null && events.size() > 0)
				event = (GKInstance) events.iterator().next();
		}
		catch(Exception e) {
			System.err.println("PathwayBrowserPanel.search(): " + e);
			e.printStackTrace();
		}
		select(event);
	}
}
