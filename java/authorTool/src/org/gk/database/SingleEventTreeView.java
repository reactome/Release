/*
 * Created on Jun 4, 2004
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import org.gk.model.GKInstance;
import org.gk.util.TreeUtilities;

/**
 * A tree of a single Event object. To display the tree view correctly, slots values for
 * "componentOf" and "instanceOf" should be filled.
 * @author wugm
 */
public class SingleEventTreeView extends JPanel {
	protected JTree tree;
	private JLabel titleLabel;
	private GKInstance event;
	// NodeToIconMap
	protected Map node2IconMap;

	public SingleEventTreeView() {
		init();
	}
	
	public SingleEventTreeView(GKInstance event) {
		this();
		setEvent(event);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		titleLabel = new JLabel("Tree View");
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		add(titleLabel, BorderLayout.NORTH);
		tree = new JTree() {
			protected void processMouseEvent(MouseEvent e) {
			}
		};
		node2IconMap = new HashMap();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		DefaultTreeModel model = new DefaultTreeModel(root);
		tree.setModel(model);
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		// Set Renderer after the above codes in case a null exception is thrown.
		tree.setCellRenderer(createTreeCellRenderer());
		add(new JScrollPane(tree), BorderLayout.CENTER);
	}
	
	/**
	 * Refactored method for subclassing purpose.
	 * @return
	 */
	protected TreeCellRenderer createTreeCellRenderer() {
		EventCellRenderer renderer = new EventCellRenderer();
		renderer.setNode2IconMap(node2IconMap);
		return renderer;		
	}
	
	/**
	 * Set the event object to be displayed. The slots "componentOf" and "instanceOf"
	 * should be filled already before event is passed.
	 * @param event
	 */
	public void setEvent(GKInstance event) {
		this.event = event;
		if (event == null) {
			clear();
			return;
		}
		// Create a new tree display
		Set topNodes = new HashSet();
		Map nodeMap = new HashMap();
		buildNodes(event, topNodes, nodeMap);
		buildTree(topNodes);
		titleLabel.setText("Tree View for " + event.toString());
	}
	
	private void clear() {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.removeAllChildren();
		model.nodeStructureChanged(root);	
		titleLabel.setText("Tree View");	
	}
	
	private void buildTree(Collection topNodes) {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.removeAllChildren();
		for (Iterator it = topNodes.iterator(); it.hasNext();) {
			InstanceNode node = (InstanceNode) it.next();
			DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node.getInstance());
			root.add(treeNode);
			buildTree(treeNode, node);
		}
		model.nodeStructureChanged(root);
		// expand
		TreeUtilities.expandAllNodes(root, tree);
		// Want to select all end nodes
		for (int i = 0; i < tree.getRowCount(); i++) {
			TreePath path = tree.getPathForRow(i);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			if (node.getChildCount() == 0)
				tree.addSelectionPath(path);
		}
	}
	
	private void buildTree(DefaultMutableTreeNode treeNode, InstanceNode node) {
		Collection children = node.getChildren();
		if (children == null || children.size() == 0)
			return;
		for (Iterator it = children.iterator(); it.hasNext();) {
			InstanceNode childNode = (InstanceNode) it.next();
			DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(childNode.getInstance());
			treeNode.add(childTreeNode);
			Icon typeIcon = node.getChildIcon(childNode);
			node2IconMap.put(childTreeNode, typeIcon);
			buildTree(childTreeNode, childNode);
		}
	}
	
	private void buildNodes(GKInstance eventInstance, Collection topNodes, Map nodeMap) {
		try {
			InstanceNode eventNode = (InstanceNode) nodeMap.get(eventInstance);
			if (eventNode == null) {
				eventNode = new InstanceNode(eventInstance);
				nodeMap.put(eventInstance, eventNode);
			}
			String[] reverseAttNames = EventTreeBuildHelper.getReverseTreeAttributeNames();
			Icon[] icons = EventTreeBuildHelper.getTreeIcons();
			List parents = null;
			boolean isTopNode = true;
			for (int i = 0; i < reverseAttNames.length; i++) {
			    parents = eventInstance.getAttributeValuesListNoCheck(reverseAttNames[i]);
			    if (parents == null || parents.size() == 0)
			        continue;
			    isTopNode = false;
			    for (Iterator it = parents.iterator(); it.hasNext();) {
			        GKInstance parent = (GKInstance) it.next();
			        if (parent == null)
			            continue; // Just in case
			        InstanceNode parentNode = (InstanceNode) nodeMap.get(parent);
					if (parentNode == null) {
						parentNode = new InstanceNode(parent);
						nodeMap.put(parent, parentNode);
					}
					buildNodes(parent, topNodes, nodeMap);
					parentNode.addChild(eventNode, icons[i]);
			    }
			}
			if (isTopNode)
				topNodes.add(eventNode);
		}
		catch(Exception e) {
			System.err.println("SingleEventTreeView.setEvent(): " + e);
			e.printStackTrace();
		}
	}
	
	public GKInstance getEvent() {
		return this.event;
	}
	
	class InstanceNode {
		GKInstance instance;
		Map children;
		
		InstanceNode(GKInstance instance) {
			this.instance = instance;
		}
		
		public void addChild(InstanceNode child, Icon typeIcon) {
			if (children == null) {
				children = new HashMap();
			}
			if (!children.containsKey(child)) {
				children.put(child, typeIcon);
			}
		}
		
		public Collection getChildren() {
			if (children != null)
				return children.keySet();
			return null;
		}
		
		public Icon getChildIcon(InstanceNode node) {
			return (Icon) children.get(node);
		}
		
		public GKInstance getInstance() {
			return this.instance;
		}
	}
}
