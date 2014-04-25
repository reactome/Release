/*
 * Created on Jun 26, 2003
 */
package org.gk.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * A collection of tree operations.
 * @author wgm
 */
public class TreeUtilities {
	/**
	 * Remove a TreeNode containing the specified obj from a JTree. It is assumed
	 * that all TreeNodes in the specified tree are DefaultMutableTreeNodes and its 
	 * TreeModel should be a DefaultTreeModel.
	 * @param obj the contained 
	 * @param tree the JTree whose TreeNodes are DefaultMutableTreeNodes.
	 */
	public static void removeNode(Object obj, JTree tree) {
		DefaultMutableTreeNode treeNode = searchNode(obj, tree);
		DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		model.removeNodeFromParent(treeNode);
	}
	
	/**
	 * Search a DefaultMutableTreeNode whose userObject is the specified obj.
	 * @param obj the userObject.
	 * @param tree the JTree whose TreeNodes are DefaultMutableTreeNodes.
	 * @return a DefaultMuatbleTreeNode whose userObject is obj.
	 */
	public static DefaultMutableTreeNode searchNode(Object obj, JTree tree) {
		TreeModel model = tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
		if (root.getUserObject() == obj)
			return root;
		return searchNode(obj, root);
	}
	
	/**
	 * A recursive method to search a DefaultMutableTreeNode that displays
	 * an Object obj. 
	 */ 
	public static DefaultMutableTreeNode searchNode(Object obj, 
                                                    DefaultMutableTreeNode parentNode) {
		DefaultMutableTreeNode treeNode = null;
		int size = parentNode.getChildCount();
		// A list of nodes with children
		java.util.List parentNodes = new ArrayList();
		for (int i = 0; i < size; i++) {
			treeNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
			if (treeNode.getUserObject() == obj)
				return treeNode;	
			if (treeNode.getChildCount() > 0)
				parentNodes.add(treeNode);
		}
		for (Iterator it = parentNodes.iterator(); it.hasNext();) {
			treeNode = searchNode(obj, (DefaultMutableTreeNode)it.next());
			if (treeNode != null)
				return treeNode;
		}
		return null;
	}
	
	/**
	 * Search all TreeNodes that use the specified obj as its UserObject.
	 * @param obj
	 * @param tree a JTree whose TreeNode is DefaultMutableTreeNode.
	 * @return a List of DefaultMutableTreeNodes. An empty list will be returned
	 * if no TreeNode is found.
	 */
	public static java.util.List searchNodes(Object obj, JTree tree) {
		java.util.List nodes = new ArrayList();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
		searchNodes(obj, root, nodes);
		return nodes;
	}
	
	public static void searchNodes(Object obj, 
                                   DefaultMutableTreeNode treeNode, 
                                   java.util.List nodes) {
		if (treeNode.getUserObject() == obj)
			nodes.add(treeNode);
		for (int i = 0; i < treeNode.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeNode.getChildAt(i);
			searchNodes(obj, node, nodes);
		}
	}
	
	/**
	 * Expand all tree nodes under the specified DefaultMutableTreeNode object.
	 * @param treeNode
	 * @param tree a JTree with DefaultTreeModel as its model.
	 */
	public static void expandAllNodes(DefaultMutableTreeNode treeNode, JTree tree) {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		Enumeration enum1 = treeNode.breadthFirstEnumeration();
		while (enum1.hasMoreElements()) {
			treeNode = (DefaultMutableTreeNode) enum1.nextElement();
			if (treeNode.getChildCount() > 0) {
				TreePath path = new TreePath(model.getPathToRoot(treeNode));
				tree.expandPath(path);
			}
		}
	}
	
	/**
	 * Collpase all tree nodes under the specified DefaultMutableTreeNode object.
	 * @param treeNode
	 * @param tree a JTree with DefaultTreeModel as its model.
	 */
//	public static void collapseAllNodes(DefaultMutableTreeNode treeNode, JTree tree) {
//		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
//		Enumeration enum = treeNode.breadthFirstEnumeration();
//		Stack stack = new Stack();
//		while (enum.hasMoreElements()) 
//			stack.push(enum.nextElement());
//		while (!stack.isEmpty()) {
//			treeNode = (DefaultMutableTreeNode) stack.pop();
//			if (treeNode.getChildCount() > 0) {
//				TreePath path = new TreePath(model.getPathToRoot(treeNode));
//				tree.collapsePath(path);
//			}
//		}
//	}
	
	public static void collapseAllNodes(DefaultMutableTreeNode treeNode, JTree tree) {
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		collapseAllNodes(treeNode, tree, model);
	}
	
	public static void collapseAllNodes(DefaultMutableTreeNode treeNode, JTree tree, DefaultTreeModel model) {
		int size = treeNode.getChildCount();
		if (size > 0) {
			TreePath path = new TreePath(model.getPathToRoot(treeNode));
			if (tree.isExpanded(path)) {
				for (int i = 0; i < size; i++) {
					DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
					TreePath path1 = new TreePath(model.getPathToRoot(childNode));
					if (!tree.isExpanded(path1))
						continue;
					collapseAllNodes(childNode, tree, model);
					tree.collapsePath(path1);
				}		
				tree.collapsePath(path);
			}
		}
	}
	
	public static Set<Object> grepAllUserObjects(JTree tree) {
	    Set<Object> rtn = new HashSet<Object>();
	    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
	    grepAllUserObjects(root, 
	                       rtn);
	    return rtn;
	}
	
	private static <T> void grepAllUserObjects(DefaultMutableTreeNode treeNode, 
	                                           Set<Object> set) {
	    Object obj = treeNode.getUserObject();
	    if (obj != null)
	        set.add(obj);
	    if (treeNode.getChildCount() == 0)
	        return;
	    for (int i = 0; i < treeNode.getChildCount(); i++) {
	        DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);
	        grepAllUserObjects(child, set);
	    }
	}
}
