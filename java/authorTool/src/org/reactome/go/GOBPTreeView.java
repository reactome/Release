/*
 * Created on Jun 15, 2004
 */
package org.reactome.go;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.gk.util.BrowserLauncher;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TreeUtilities;

/**
 * This customized JPanel is used to display GO Biological process in a Tree.
 * @author wugm
 */
public class GOBPTreeView extends JPanel {
	private final String GO_URL = "http://www.ebi.ac.uk/ego/QuickGO?mode=display&entry=";
	
	private JTree tree;
	private JLabel titleLabel;
	private MySQLAdaptor dbAdaptor;
	// Node type map
	private Icon isAIcon;
	private Icon isPartOfIcon;
	private Map node2IconMap;
	
	public GOBPTreeView() {
		init();
	}
	
	public GOBPTreeView(MySQLAdaptor dbAdaptor) {
		init();
		setMySQLAdaptor(dbAdaptor);
	}
	
	public void setMySQLAdaptor(MySQLAdaptor adaptor) {
		this.dbAdaptor = adaptor;
		fetchGOProcesses();
	}
	
	public MySQLAdaptor getMySQLAdaptor() {
		return this.dbAdaptor;
	}
	
	private void init() {
		setLayout(new BorderLayout());
		titleLabel = new JLabel("GO Biological Process");
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		add(titleLabel, BorderLayout.NORTH);
		
		tree = new JTree();
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setToggleClickCount(3);
		node2IconMap = new HashMap();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Processes");
		DefaultTreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		GOInstanceRenderer renderer = new GOInstanceRenderer();
		renderer.setNode2IconMap(node2IconMap);
		tree.setCellRenderer(renderer);
		add(new JScrollPane(tree), BorderLayout.CENTER);
		// Init Icons
		initIcons();
		
		tree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					viewInstance();
				}
			}
		});
	}
	
	private void viewInstance() {
		Collection c = getSelectedInstances();
		if (c.size() > 0) {
			GKInstance instance = (GKInstance) c.iterator().next();
			try {
				String accession = (String)instance.getAttributeValue("accession");
				if (accession != null) {
					String url = GO_URL + "GO:" + accession;
					if (AppletHelper.getHelper() != null)
						AppletHelper.getHelper().showDocument(url, "GO");
					else {
						BrowserLauncher.displayURL(url, this);
					}
				}
			}
			catch(Exception e) {
				System.err.println("GOBPTreeView.viewInstance(): " + e);
				e.printStackTrace();
			}
		}
	}
	
	private void initIcons() {
		if (AppletHelper.getHelper() == null) {
			isAIcon = GKApplicationUtilities.getIsAIcon();
			isPartOfIcon = GKApplicationUtilities.getIsPartOfIcon();
		}
		else {
			isAIcon = AppletHelper.getHelper().getIsAIcon();
			isPartOfIcon = AppletHelper.getHelper().getIsPartOfIcon();
		}
	}
	
	private void fetchGOProcesses() {
		if (dbAdaptor == null)
			return;
		try {
			//dbAdaptor.refresh();
			ArrayList qr = new ArrayList();
			String clsName = "GO_BiologicalProcess";
			qr.add(dbAdaptor.createAttributeQueryRequest(clsName, "instanceOf", "IS NULL", null));
			qr.add(dbAdaptor.createAttributeQueryRequest(clsName, "componentOf", "IS NULL", null));
			Collection topLevelProcesses = dbAdaptor.fetchInstance(qr);
			Collection c = dbAdaptor.fetchInstancesByClass(clsName);
			SchemaClass cls = dbAdaptor.getSchema().getClassByName(clsName);
			dbAdaptor.loadInstanceAttributeValues(c, cls.getAttribute("instanceOf"));
			dbAdaptor.loadInstanceAttributeValues(c, cls.getAttribute("componentOf"));
			dbAdaptor.loadInstanceAttributeValues(c, cls.getAttribute("accession"));
			fillReverseAttributes(c);
			ArrayList list = new ArrayList(topLevelProcesses);
			Collections.sort(list, new Comparator() {
				public int compare(Object obj1, Object obj2) {
					GKInstance instance1 = (GKInstance)obj1;
					GKInstance instance2 = (GKInstance)obj2;
					String dn1 = instance1.getDisplayName();
					if (dn1 == null)
						dn1 = "";
					String dn2 = instance2.getDisplayName();
					if (dn2 == null)
						dn2 = "";
					return dn1.compareTo(dn2);
				}
			});
			java.util.List topLevelBPs = new ArrayList(topLevelProcesses);
			InstanceUtilities.sortInstances(topLevelBPs);
			buildTree(topLevelBPs);
		}
		catch (Exception e) {
			System.err.println("GOBPTreeView.fetchGOProcess(): " + e);
			e.printStackTrace();
		}
	}
	
	private void fillReverseAttributes(Collection c) throws Exception {
		GKInstance instance = null;
		java.util.List values;
		for (Iterator it = c.iterator(); it.hasNext();) {
			instance = (GKInstance) it.next();
			values = instance.getAttributeValuesList("componentOf");
			if (values != null) {
				for (Iterator it1 = values.iterator(); it1.hasNext();) {
					GKInstance parent = (GKInstance)it1.next();
					parent.addAttributeValueNoCheck("hasComponent", instance);
				}
			}
			values = instance.getAttributeValuesList("instanceOf");
			if (values != null) {
				for (Iterator it1 = values.iterator(); it1.hasNext();) {
					GKInstance parent = (GKInstance)it1.next();
					parent.addAttributeValueNoCheck("hasInstance", instance);
				}
			}
		}
	}
	
	private void buildTree(java.util.List topLevelProcesses) throws Exception {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		for (Iterator it = topLevelProcesses.iterator(); it.hasNext();) {
			GKInstance instance = (GKInstance) it.next();
			DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(instance);
			root.add(treeNode);
			buildTree(instance, treeNode);
		}
		model.nodeStructureChanged(root);
		// Want to expand the second level tree nodes
		TreePath path = null;
		DefaultMutableTreeNode treeNode = null;
		for (int i = 0; i < root.getChildCount(); i++) {
			treeNode = (DefaultMutableTreeNode) root.getChildAt(i);
			path = new TreePath(model.getPathToRoot(treeNode));
			tree.expandPath(path);
		}
	}
	
	private void buildTree(GKInstance instance, DefaultMutableTreeNode treeNode) throws Exception {
		Collection values = instance.getAttributeValuesListNoCheck("hasComponent");
		if (values != null && values.size() > 0) {
			for (Iterator it = values.iterator(); it.hasNext();) {
				GKInstance child = (GKInstance) it.next();
				DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
				treeNode.add(childNode);
				node2IconMap.put(childNode, isPartOfIcon);
				buildTree(child, childNode);
			}
		}
		values = instance.getAttributeValuesListNoCheck("hasInstance");
		if (values != null && values.size() > 0) {
			for (Iterator it = values.iterator(); it.hasNext();) {
				GKInstance child = (GKInstance) it.next();
				DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
				treeNode.add(childNode);
				node2IconMap.put(childNode, isAIcon);
				buildTree(child, childNode);
			}
		}
	}
	
	public void addSelectionListener(TreeSelectionListener l) {
		tree.addTreeSelectionListener(l);
	}
	
	public void removeSelectionListener(TreeSelectionListener l) {
		tree.removeTreeSelectionListener(l);
	}
	
	public Collection getSelectedInstances() {
		Set instances = new HashSet();
		TreePath[] paths = tree.getSelectionPaths();
		if (paths != null) {
			for (int i = 0; i < paths.length; i++) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
				GKInstance instance = (GKInstance) treeNode.getUserObject();
				instances.add(instance);
			}
		}
		return instances;
	}
	
	public void setNeedHilite(boolean hilite) {
		GOInstanceRenderer renderer = (GOInstanceRenderer) tree.getCellRenderer();
		renderer.setNeedHilite(hilite);
	}
	
	public void clearSelection() {
		tree.clearSelection();
	}
	
	public void collapseAllNodes() {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		DefaultMutableTreeNode bpRoot = (DefaultMutableTreeNode) root.getFirstChild();
		TreeUtilities.collapseAllNodes(bpRoot, tree);
	}
	
	public void select(Collection instances) {
		java.util.List nodes = new ArrayList();
		for (Iterator it = instances.iterator(); it.hasNext();) {
			Object obj = it.next();
			java.util.List treeNodes = TreeUtilities.searchNodes(obj, tree);
			nodes.addAll(treeNodes);
		}
		if (nodes.size() == 0) {
			JOptionPane.showMessageDialog(this,
										  "No events found.",
										  "Search Result",
										  JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		tree.clearSelection();
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode treeNode = null;
		// The first path should be visiable
		TreePath firstPath = null;
		for (Iterator it = nodes.iterator(); it.hasNext();) {
			treeNode = (DefaultMutableTreeNode) it.next();
			TreePath path = new TreePath(model.getPathToRoot(treeNode));
			if (firstPath == null)
				firstPath = path;
			//tree.expandPath(path);
			tree.addSelectionPath(path);
		}
		if (firstPath != null)
			tree.scrollPathToVisible(firstPath);
	}
}
