/*
 * Created on Jun 24, 2003
 */
package org.gk.graphEditor;

import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.gk.render.*;
import org.gk.util.TreeUtilities;

/**
 * This customized JTree is used to display a GK Pathway in a tree.
 * @author wgm
 */
public class ProcessTree extends JTree {
	private RenderablePathway pathway;
	private boolean isSorted;
	// To control reaction display
	private boolean pathwayOnly = false;
	
	public ProcessTree() {
		init();
	}
	
	private void init() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("EMPTY");
		DefaultTreeModel treeModel = new DefaultTreeModel(root);
		setModel(treeModel);
		setShowsRootHandles(true);
		setRootVisible(false);
		// Set renderer to use customized renderer
		setCellRenderer(new TypeTreeCellRenderer());
		// Disable double clicking to expand tree
		setToggleClickCount(3);
        // As of version 2.2, build 26, DnD in ProcessTree is disabled to
        // enable single click selection to open properties pane.
		// To enable DnD
		//setDragEnabled(true);
		//setTransferHandler(new RenderableTransferHandler());
	}
	
	protected void insertToModelAlphabetically(DefaultMutableTreeNode childNode,
	                                  DefaultMutableTreeNode parentNode) {
		DefaultTreeModel model = (DefaultTreeModel) treeModel;
		if (!isSorted) {
			int size = parentNode.getChildCount();
			model.insertNodeInto(childNode, parentNode, size);
			return;
		}
		DefaultMutableTreeNode treeNode = null;
		int size = parentNode.getChildCount();
		boolean isDone = false;
		Renderable insertRenderable = (Renderable) childNode.getUserObject();
		Renderable renderable = null;
		for (int i = 0; i < size; i++) {
			treeNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
			renderable = (Renderable) treeNode.getUserObject();
			if (renderable.getDisplayName() == null ||
			    insertRenderable.getDisplayName() == null)
			    System.out.println();
			if (renderable.getDisplayName().compareTo(insertRenderable.getDisplayName()) > 0) {
				model.insertNodeInto(childNode, parentNode, i);
				isDone = true;
				break;
			}
		}
		if (!isDone)
			model.insertNodeInto(childNode, parentNode, size);
	}
	
	protected int insertToNodeAlphabetically(DefaultMutableTreeNode childNode,
											DefaultMutableTreeNode parentNode) {
		if (!isSorted) {
			parentNode.add(childNode);
			return parentNode.getChildCount() - 1;
		}
		DefaultMutableTreeNode treeNode = null;
		int size = parentNode.getChildCount();
		Renderable insertRenderable = (Renderable)childNode.getUserObject();
		Renderable renderable = null;
		for (int i = 0; i < size; i++) {
			treeNode = (DefaultMutableTreeNode)parentNode.getChildAt(i);
			renderable = (Renderable)treeNode.getUserObject();
			if (renderable.getDisplayName().compareTo(insertRenderable.getDisplayName()) > 0) {
				parentNode.insert(childNode, i);
				return i;
			}
		}
		parentNode.insert(childNode, size);
		return size;
	}
	
	public void insert(Renderable parent, java.util.List children) {
		if (children == null || children.size() == 0)
			return;
		Renderable child = null;
		for (Iterator it = children.iterator(); it.hasNext();) {
			child = (Renderable) it.next();
			insert(parent, child);
		}
	}
	
	public void insert(Renderable parent, Renderable child) {
		if (child instanceof FlowLine)
			return; // FlowLine cannot be added to tree.
		// Check if parent contains a Renderable with the same name as child.
		// If so, replace the UserObject by the child.
		java.util.List parentTreeNodes = searchNodes(parent);
		if (parentTreeNodes.size() == 0)
			return;
		boolean needAdd = false;
		for (Iterator it = parentTreeNodes.iterator(); it.hasNext();) {
			DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)it.next();
			needAdd = true;
			for (int i = 0; i < parentNode.getChildCount(); i++) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)parentNode.getChildAt(i);
				Renderable renderable = (Renderable)treeNode.getUserObject();
				if (renderable.getDisplayName().equals(child.getDisplayName())) {
					treeNode.setUserObject(child);
					needAdd = false;
					break;
				}
			}
			if (!needAdd)
				continue;
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(child);
			DefaultTreeModel model = (DefaultTreeModel)treeModel;
			insertToModelAlphabetically(newNode, parentNode);
			// Have to check if any children are in the child list.
			insertChildren(newNode, child);
			if (parentNode.getChildCount() == 1) { // A bug in tree
				TreePath path = new TreePath(model.getPathToRoot(parentNode));
				expandPath(path);
			}
		}
	}
	
	/**
	 * A recursive method to insert components to parent node.
	 * @param objs
	 */
	protected void insertChildren(DefaultMutableTreeNode parentNode, Renderable parent) {
		if (parent.getComponents() != null) {
			Map added = new HashMap();
			DefaultTreeModel model = (DefaultTreeModel) treeModel;
			int[] index = new int[1];
			for (Iterator it = parent.getComponents().iterator(); it.hasNext();) {
				Renderable r = (Renderable) it.next();
				if (r instanceof FlowLine)
					continue; // Don't add flow lines
				if (added.containsKey(r.getDisplayName()))
					continue;
				// Don't add RenderableReaction to ReactionNode
				if (r instanceof RenderableReaction && parent instanceof ReactionNode)
					continue;
				if (pathwayOnly && r instanceof ReactionNode)
					continue;
				DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(r);
				insertToModelAlphabetically(childNode, parentNode);
				added.put(r.getDisplayName(), r);
				insertChildren(childNode, r);
			}
			// Expand a no reaction node
			if ((parent instanceof RenderablePathway) && 
			    (parentNode.getChildCount() > 0)) {
				TreePath path = new TreePath(model.getPathToRoot(parentNode));
				expandPath(path);
			}
		}
	}
	
	public void remove(java.util.List objs) {
		if (objs == null || objs.size() == 0)
			return;
		for (Iterator it = objs.iterator(); it.hasNext();) {
			Renderable renderable = (Renderable) it.next();
			remove(renderable);
		}
	}
	
	/**
	 * Use this method to delete a Renderable object from the view and take care of the data structure.
	 * @param renderable
	 */
	public void delete(Renderable renderable) {
		DefaultMutableTreeNode node = TreeUtilities.searchNode(renderable, this);
		if (node == null)
			return;
		// Do removing
		DefaultTreeModel model = (DefaultTreeModel)getModel();
		Renderable container = renderable.getContainer();
		if (container == null)
			model.removeNodeFromParent(node);
		else {
			java.util.List parentNodes = searchNodes(container);
			for (Iterator it = parentNodes.iterator(); it.hasNext();) {
				DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)it.next();
				// Search for the node that should be removed
				for (int i = 0; i < parentNode.getChildCount(); i++) {
					DefaultMutableTreeNode node1 = (DefaultMutableTreeNode)parentNode.getChildAt(i);
					if (node1.getUserObject() == renderable) {
						model.removeNodeFromParent(node1);
						break;
					}
				}
			}
		}
		// Take care of data strucutre of renderable here.
		// Get the deleted objects
		java.util.List list = new ArrayList();
		//list.add(renderable);
		Renderable target = null;
		if (renderable instanceof Shortcut)
			target = ((Shortcut)renderable).getTarget();
		else
			target = renderable;
		if (container != null) {
			for (Iterator it = container.getComponents().iterator(); it.hasNext();) {
				Object obj = it.next();
				if (obj == target)
					list.add(obj);
				else if (obj instanceof Shortcut) {
					if (((Shortcut)obj).getTarget() == target)
						list.add(obj);
				}
			}
			// Remove shortcut first
			for (Iterator it = list.iterator(); it.hasNext();) {
				Renderable r = (Renderable) it.next();
				if (r instanceof Shortcut) {
					target.removeShortcut(r);
					r.clearConnectWidgets();
					container.removeComponent(r);
					it.remove();
				}
			}
			// Remove target
			for (Iterator it = list.iterator(); it.hasNext();) {
				Renderable r = (Renderable)it.next();
				r.clearConnectWidgets();
				container.removeComponent(r);
				it.remove();
			}
		}
		// Let the registry to take care of all dirty stuff.
		RenderableRegistry.getRegistry().remove(renderable, true);
	}
	
	/**
	 * Use this method if you only want to remove the renderable from the tree view.
	 * However, the view might not be changed if there is still another same name
	 * renderable is contained by renderable's container. This same name Renderable 
	 * might be the target or a Shortcut.
	 * @param renderable
	 */
	public void remove(Renderable renderable) {
		DefaultMutableTreeNode node = TreeUtilities.searchNode(renderable, this);
		if (node == null)
			return;
		DefaultTreeModel model = (DefaultTreeModel)getModel();
		Renderable container = renderable.getContainer();
		if (container == null)
			model.removeNodeFromParent(node);
		else {
			java.util.List parentNodes = searchNodes(container);
			for (Iterator it = parentNodes.iterator(); it.hasNext();) {
				DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode)it.next();
				DefaultMutableTreeNode childNode = null;
				for (int i = 0; i < parentNode.getChildCount(); i++) {
					DefaultMutableTreeNode node1 = (DefaultMutableTreeNode)parentNode.getChildAt(i);
					if (node1.getUserObject() == renderable) {
						childNode = node1;
						break;
					}
				}
				// Search if there is any Renderable object with the same name
				Renderable anotherNode = null;
				for (Iterator it1 = container.getComponents().iterator(); it1.hasNext();) {
					Renderable r = (Renderable) it1.next();
					if (r.getDisplayName() == null)
						continue; // It might be a flowline
					if (r.getDisplayName().equals(renderable.getDisplayName())) {
						anotherNode = r;
						break;
					}
				}
				if (anotherNode != null)
					childNode.setUserObject(anotherNode);
				else
					model.removeNodeFromParent(childNode);
			}
		}
	}
	
	public void addProcess(RenderablePathway pathway) {
		this.pathway = pathway;
		// Create a new root
		DefaultMutableTreeNode processNode = new DefaultMutableTreeNode(pathway);
		// attach this process node
		DefaultTreeModel model = (DefaultTreeModel)getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		model.insertNodeInto(processNode, root, root.getChildCount());
		// Popup the root
		popupTreeRoot(processNode, pathway);
		// Force expand
		if (root.getChildCount() == 1) {
			setRootVisible(true);
			expandRow(0);
			setRootVisible(false);
		}
	}
	
	private void popupTreeRoot(DefaultMutableTreeNode parentNode, RenderablePathway pathway) {
		java.util.List list = pathway.getComponents();
		if (list == null)
			return;
		for (Iterator it = list.iterator(); it.hasNext();) {
			Object obj = it.next();
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(it.next());
			if (obj instanceof RenderablePathway)
				popupTreeRoot(node, (RenderablePathway)obj);
		}
	}
	
	public RenderablePathway getProcess() {
		return this.pathway;
	}
	
	public void open(RenderablePathway process) {
		this.pathway = process;
		DefaultTreeModel model = (DefaultTreeModel) treeModel;
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		DefaultMutableTreeNode processNode = new DefaultMutableTreeNode(process);
		root.add(processNode);
		//// Shortcuts should be handled at the last step
		//java.util.List shortcuts = new ArrayList();
		open(process, processNode);
//		if (shortcuts.size() > 0) {
//			Renderable renderable = null;
//			Renderable container = null;
//			for (Iterator it = shortcuts.iterator(); it.hasNext();) {
//				renderable = (Renderable) it.next();
//				container = renderable.getContainer();
//				insert(container, renderable);
//			}
//		}
		model.nodeStructureChanged(root);
		expandToReactions(processNode);
	}
	
	private void expandToReactions(DefaultMutableTreeNode processNode) {
		java.util.List current = new ArrayList();
		current.add(processNode);
		java.util.List next = new ArrayList();
		DefaultMutableTreeNode treeNode = null;
		TreePath treePath = null;
		DefaultTreeModel model = (DefaultTreeModel) getModel();
		while (current.size() > 0) {
			for (Iterator it = current.iterator(); it.hasNext();) {
				treeNode = (DefaultMutableTreeNode) it.next();
				if (treeNode.getUserObject() instanceof ReactionNode ||
				    treeNode.getUserObject() instanceof RenderableReaction)
				    continue; // Don't expand reaction
				treePath = new TreePath(model.getPathToRoot(treeNode));
				expandPath(treePath);
				for (int i = 0; i < treeNode.getChildCount(); i++) {
					next.add(treeNode.getChildAt(i));
				}
			}
			current.clear();
			current.addAll(next);
			next.clear();
		}	
	}
	
	protected void open(Renderable container, 
	                    DefaultMutableTreeNode parentNode) {
		java.util.List components = container.getComponents();
		if (components != null) {
			// To control unique
			Map controlMap = new HashMap();
			for (Iterator it = components.iterator(); it.hasNext();) {
				Renderable renderable = (Renderable) it.next();
				if (controlMap.containsKey(renderable.getDisplayName()))
					continue;
				controlMap.put(renderable.getDisplayName(), renderable);
				// Escape flow line
				if (renderable instanceof FlowLine)
					continue;
				// Escape a RenderableReaction under ReactionNode
				if (renderable instanceof RenderableReaction &&
				    container instanceof ReactionNode)
				    continue;
				if (pathwayOnly && renderable instanceof ReactionNode)
					continue;
				// Escape shortcut temporarily
//				if (renderable instanceof Shortcut) {
//					shortcuts.add(renderable);
//					continue;
//				}
				DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(renderable);
				insertToNodeAlphabetically(treeNode, parentNode);
				if (renderable instanceof RenderableEntity ||
					renderable instanceof RenderableReaction)
					continue;
				open(renderable, treeNode);		
			}
		}
	}
	
	/**
	 * Refresh a TreeNode corresponding to a specified Renderable object. This method
	 * should be called if the property for tree display is changed (e.g. displayName).
	 * However, the child nodes will not be taken care of here.
	 * @param renderable
	 */
	public void refresh(Renderable renderable) {
		//Find all TreeNode for renderable and refresh it.
		if (renderable instanceof Shortcut)
			renderable = ((Shortcut)renderable).getTarget();
		java.util.List nodes = searchNodes(renderable);
		if (nodes.size() > 0) {
			DefaultTreeModel model = (DefaultTreeModel) getModel();
			for (Iterator it = nodes.iterator(); it.hasNext();) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
				if (isSorted) {
					DefaultMutableTreeNode parent = (DefaultMutableTreeNode) treeNode.getParent();
					TreePath path = new TreePath(model.getPathToRoot(treeNode));
					boolean isSelected = isPathSelected(path);
					// Remember the expanded state
					java.util.List expandedPaths = new ArrayList();
					getExpandedPaths(expandedPaths, treeNode, model);
					model.removeNodeFromParent(treeNode);
					insertToModelAlphabetically(treeNode, parent);
					for (Iterator it1 = expandedPaths.iterator(); it1.hasNext();) {
						TreePath path1 = (TreePath) it1.next();
						expandPath(path1);
					}
					if (isSelected)
						addSelectionPath(path);
				}
				else 
					model.nodeChanged(treeNode);
			}
		}
	}
	
	/**
	 * Update a branch rooted at the specified Renderable object. Call this method if
	 * the components of a Renderable object is changed.
	 * @param r
	 */
	public void updateChildrenRemove(Renderable r) {
	    java.util.List nodes = searchNodes(r);
	    if (nodes.size() == 0)
	        return;
	    DefaultMutableTreeNode treeNode;
	    DefaultTreeModel model = (DefaultTreeModel) treeModel;
	    java.util.List components = r.getComponents();
	    if (components == null)
	        components = new ArrayList();
	    // Check removed children
	    java.util.List removed = new ArrayList();
	    for (Iterator it = nodes.iterator(); it.hasNext();) {
	        treeNode = (DefaultMutableTreeNode) it.next();
	        removed = new ArrayList();
	        for (int i = 0; i < treeNode.getChildCount(); i++) {
	            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
	            if (!components.contains(childNode.getUserObject()))
	                removed.add(childNode);
	        }
	        for (Iterator it1 = removed.iterator(); it1.hasNext();) {
	            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) it1.next();
	            model.removeNodeFromParent(childNode);
	        }
	    }
	}
	
	/**
	 * Switch the type from ReactionNode to RenderablePathway or vice versa. The
	 * data structure in both source and target are not taken care of here. This method
	 * is trying to make display correct.
	 * @param source
	 * @param target
	 */
	public void switchType(Renderable source, Renderable target) {
		// Point to targets
		if (source instanceof Shortcut)
			source = ((Shortcut)source).getTarget();
		if (target instanceof Shortcut)
			target = ((Shortcut)target).getTarget();
		java.util.List nodes = searchNodes(source);
		if (nodes.size() > 0) {
			DefaultTreeModel model = (DefaultTreeModel) getModel();
			DefaultMutableTreeNode treeNode = null;
			DefaultMutableTreeNode parentNode = null;
			TreePath treePath;
			for (Iterator it = nodes.iterator(); it.hasNext();) {
				treeNode = (DefaultMutableTreeNode) it.next();
				treePath = new TreePath(model.getPathToRoot(treeNode));
				boolean isSelected = isPathSelected(treePath);
				parentNode = (DefaultMutableTreeNode) treeNode.getParent();
				int index = parentNode.getIndex(treeNode);
				// Find a Renderable object matched to target
				Renderable container = (Renderable) parentNode.getUserObject();
				Renderable matchedNode = RenderUtility.getComponentByName(container, target.getDisplayName());
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(matchedNode);
				model.removeNodeFromParent(treeNode);
				model.insertNodeInto(newNode, parentNode, index);
				if (isSelected) {
					treePath = new TreePath(model.getPathToRoot(newNode));
					addSelectionPath(treePath);
				}
			}
		}		
	}
	
	private void getExpandedPaths(java.util.List expandedPaths, 
	                              DefaultMutableTreeNode treeNode,
	                              DefaultTreeModel model) {
		TreePath path = new TreePath(model.getPathToRoot(treeNode));
		if (isExpanded(path)) {
			expandedPaths.add(path);
			for (int i = 0; i < treeNode.getChildCount(); i++) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
				getExpandedPaths(expandedPaths, childNode, model);
			}
		}
	}
	
	public void setSelected(Renderable renderable) {
		if (renderable == null) {
			clearSelection();
			return;
		}
		// Search the treeNode
		if (renderable instanceof Shortcut) 
			renderable = ((Shortcut)renderable).getTarget();
		DefaultTreeModel model = (DefaultTreeModel) treeModel;
		java.util.List treeNodes = searchNodes(renderable);
		if (treeNodes.size() > 0) {
			TreePath paths[] = new TreePath[treeNodes.size()];
			boolean needScroll = true;
			Rectangle pathBounds = null;
			Rectangle visibleRect = getVisibleRect();
			for (int i = 0; i < treeNodes.size(); i++) {
				TreeNode[] path = model.getPathToRoot((TreeNode)treeNodes.get(i));
				paths[i] = new TreePath(path);
				pathBounds = getPathBounds(paths[i]);
				if (pathBounds != null && visibleRect.intersects(pathBounds))
					needScroll = false;
			}
			setSelectionPaths(paths);
			if (needScroll)
				scrollPathToVisible(paths[0]); // Make sure the first in the array is visible
				                               // This might be not good.
		}
	}
	
	/**
	 * A shortcut Renderable object can be changed to its target Renderable object because 
	 * the target is deleted. Call this method to make necessary display change.
	 * @param shortcut
	 * @param target
	 */
	public void updateShortcutToTarget(Renderable shortcut, Renderable target) {
		// Have to change the userObject in the treeNode
		java.util.List treeNodes = TreeUtilities.searchNodes(shortcut, this);
		if (treeNodes.size() > 0) {
			for (Iterator it = treeNodes.iterator(); it.hasNext();) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
				treeNode.setUserObject(target);
			}
		}
	}
	
	/**
	 * Search Renderable objects with the specified name
	 * @param name the name to be searched.
	 */
	public java.util.List search(String name) {
		Renderable renderable = RenderableRegistry.getRegistry().getSingleObject(name);
		java.util.List list = searchNodes(renderable);
		if (list != null && list.size() > 0) {
			// Highlight the found path
			clearSelection();
			TreePath path = null;
			DefaultMutableTreeNode treeNode = null;
			DefaultTreeModel model = (DefaultTreeModel)treeModel;
			int firstRow = Integer.MAX_VALUE;
			int row = 0;
			for (Iterator it = list.iterator(); it.hasNext();) {
				treeNode = (DefaultMutableTreeNode)it.next();
				path = new TreePath(model.getPathToRoot(treeNode));
				row = getRowForPath(path);
				if (row < firstRow)
					firstRow = row;
				expandPath(path);
				addSelectionPath(path);
			}
			// Make sure first selection is visible
			scrollRowToVisible(firstRow);
		}
		return list;
	}
	
	public java.util.List search(String key, 
	                             boolean isWholeNameOnly, 
	                             boolean isCaseSensitive) {
		// Get the list of all Renderables matched
		java.util.List renderables = new ArrayList();
		Collection all = RenderableRegistry.getRegistry().getAllRenderables();
		if (all != null) {
			Renderable r = null;
			if (isWholeNameOnly && isCaseSensitive) {
				for (Iterator it = all.iterator(); it.hasNext();) {
					r = (Renderable) it.next();
					if (r.getDisplayName().equals(key))
						renderables.add(r);
				}
			}
			else if (isWholeNameOnly) {
				for (Iterator it = all.iterator(); it.hasNext();) {
					r = (Renderable) it.next();
					if (r.getDisplayName().equalsIgnoreCase(key))
						renderables.add(r);
				}
			}
			else if (isCaseSensitive) {
				int index;
				for (Iterator it = all.iterator(); it.hasNext();) {
					r = (Renderable) it.next();
					index = r.getDisplayName().indexOf(key);
					if (index > -1)
						renderables.add(r);
				}
			}
			else {
				String lowKey = key.toLowerCase();
				int index;
				for (Iterator it = all.iterator(); it.hasNext();) {
					r = (Renderable) it.next();
					index = r.getDisplayName().toLowerCase().indexOf(lowKey);
					if (index > -1)
						renderables.add(r);
				}
			}
		}
		// Need to highlight them in the tree
		if (renderables.size() > 0) {
			java.util.List treeNodes = new ArrayList();
			Renderable r = null;
			for (Iterator it = renderables.iterator(); it.hasNext();) {
				r = (Renderable) it.next();
				java.util.List list = searchNodes(r);
				if (list != null)
					treeNodes.addAll(list);
			}
			if (treeNodes.size() > 0) {
				// Highlight the found path
				clearSelection();
				TreePath path = null;
				DefaultMutableTreeNode treeNode = null;
				DefaultTreeModel model = (DefaultTreeModel) treeModel;
				int firstRow = Integer.MAX_VALUE;
				int row = 0;
				for (Iterator it = treeNodes.iterator(); it.hasNext();) {
					treeNode = (DefaultMutableTreeNode)it.next();
					path = new TreePath(model.getPathToRoot(treeNode));
					row = getRowForPath(path);
					if (row < firstRow)
						firstRow = row;
					expandPath(path);
					addSelectionPath(path);
				}
				// Make sure first selection is visible
				scrollRowToVisible(firstRow);
			}
		}
		return renderables;
	}
	
	/**
	 * Search TreeNodes that display the specified Renderable object.
	 * @param renderable
	 * @return
	 */
	private java.util.List searchNodes(Renderable renderable) {
		java.util.List treeNodes = new ArrayList();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
		for (int i = 0; i < root.getChildCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)root.getChildAt(i);
			searchNodes(renderable, treeNodes, node);
		}
		return treeNodes;
	}
	
	/**
	 * A recursive method to select tree paths.
	 * @return
	 */
	private void searchNodes(Renderable renderable, 
	                         java.util.List treeNodes, 
	                         DefaultMutableTreeNode parentNode) {
	    Renderable tmp = (Renderable) parentNode.getUserObject();
		if (tmp instanceof Shortcut) {
			if (((Shortcut)tmp).getTarget() == renderable)
				treeNodes.add(parentNode);
		}
		else if (tmp == renderable)
			treeNodes.add(parentNode);
		if (parentNode.getChildCount() > 0) {
			for (int i = 0; i < parentNode.getChildCount(); i++) {
				DefaultMutableTreeNode tmp1 = (DefaultMutableTreeNode) parentNode.getChildAt(i);
				searchNodes(renderable, treeNodes, tmp1);
			}	
		}
	}
	
	/**
	 * Get a list of selected Renderable objects.
	 * @return a list of Renderable objects.
	 */
	public java.util.List getSelected() {
		java.util.List selection = new ArrayList();
		TreePath[] paths = getSelectionPaths();
		if (paths != null && paths.length > 0) {
			for (int i = 0; i < paths.length; i++) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
				selection.add(treeNode.getUserObject());
			}
		}
		return selection;
	}
	
	public void addDoubleClickAction(MouseListener doubleAction) {
		addMouseListener(doubleAction);
	}
	
	private void sort() {
		// To sort
		DefaultTreeModel model = (DefaultTreeModel) treeModel;
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		if (root.getChildCount() == 0)
			return;
		// Escape the root
		DefaultMutableTreeNode processNode = (DefaultMutableTreeNode) root.getFirstChild();
		sort(processNode, model);
		// Expand all
		for (int i = 0; i < getRowCount(); i++)
			expandRow(i);
	}
	
	private void sort(DefaultMutableTreeNode treeNode, DefaultTreeModel model) {
		int size = treeNode.getChildCount();
		if (size > 0) {
			java.util.List list = new ArrayList(size);
			int[] indices = new int[size];
			for (int i = 0; i < size; i++) {
				list.add(treeNode.getChildAt(i));
				indices[i] = i;
			}
			for (Iterator it = list.iterator(); it.hasNext();) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) it.next();
				model.removeNodeFromParent(childNode);
			}
			for (Iterator it = list.iterator(); it.hasNext();) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) it.next();
				insertToNodeAlphabetically(childNode, treeNode);
			}
			model.nodesWereInserted(treeNode, indices);
			// Recursively to children
			for (Iterator it = list.iterator(); it.hasNext();) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) it.next();
				sort(childNode, model);
			}				
		}
	}
	
	private void unsort() {
		// To sort
		DefaultTreeModel model = (DefaultTreeModel) treeModel;
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		if (root.getChildCount() == 0)
			return;
		// Root should be escaped
		DefaultMutableTreeNode processNode = (DefaultMutableTreeNode) root.getFirstChild();
		unsort(processNode, model);
		// Expand all
		for (int i = 0; i < getRowCount(); i++)
			expandRow(i);
	}
	
	private void unsort(DefaultMutableTreeNode treeNode, DefaultTreeModel model) {
		int size = treeNode.getChildCount();
		if (size > 0) {
			java.util.List list = new ArrayList(size);
			int[] indices = new int[size];
			for (int i = 0; i < size; i++) {
				list.add(treeNode.getChildAt(i));
				indices[i] = i;
			}
			for (Iterator it = list.iterator(); it.hasNext();) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) it.next();
				model.removeNodeFromParent(childNode);
			}
			Renderable r = (Renderable) treeNode.getUserObject();
			if (r.getComponents() == null)
				return;
			for (Iterator it = r.getComponents().iterator(); it.hasNext();) {
				Renderable r1 = (Renderable) it.next();
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) it1.next();
					if (node.getUserObject() == r1) {
						treeNode.add(node);
						it1.remove();
						break;
					}
				}
			}
			model.nodesWereInserted(treeNode, indices);
			// Recursively to children
			for (int i = 0; i < size; i++) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
				unsort(childNode, model);
			}
		}
	}
	
	public void setIsSorted(boolean isSorted) {
		if (this.isSorted != isSorted) {
			this.isSorted = isSorted;
			if (isSorted)
				sort();
			else
				unsort();
		}
	}
	
	public void setPathwayOnly(boolean pathwayOnly) {
		this.pathwayOnly = pathwayOnly;
	}
	
	class RenderableTransferHandler extends TransferHandler {
		private DataFlavor serialArrayListFlavor;
		//private java.util.List transferredData;
		
		public RenderableTransferHandler() {
			serialArrayListFlavor = new DataFlavor(ArrayList.class,
			                                  "ArrayList");
		}
		
		private boolean hasSerialArrayListFlavor(DataFlavor[] flavors) {
			if (serialArrayListFlavor == null) {
				return false;
			}

			for (int i = 0; i < flavors.length; i++) {
				if (flavors[i].equals(serialArrayListFlavor)) {
					return true;
				}
			}
			return false;
		}
	
		public boolean canImport(JComponent c, DataFlavor[] flavors) {
			if (hasSerialArrayListFlavor(flavors)) { 
				return true; 
			}
			return false;
		}

		public boolean importData(JComponent c, Transferable t) {
			ArrayList aList = null;
			if (!canImport(c, t.getTransferDataFlavors())) {
				return false;
			}
			try {
				if (hasSerialArrayListFlavor(t.getTransferDataFlavors())) {
					aList = (ArrayList)t.getTransferData(serialArrayListFlavor);
				} 
				else {
					return false;
				}
			} catch (UnsupportedFlavorException ufe) {
				System.err.println("importData: unsupported data flavor");
				return false;
			} catch (IOException ioe) {
				System.err.println("importData: I/O exception");
				return false;
			}
			if (aList != null) {
				TreePath path = getSelectionPath();
				if (path == null)
					return false;
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
				Renderable container = (Renderable) treeNode.getUserObject();
				// Get rid of the mark (processTree)
				aList.remove(0);
				// Make sure container will not be inserted to itself
				if (!circularRefValidate(container, aList))
					return false;
				if (container instanceof RenderablePathway) 
					return importToPathway(container, aList);
				else if (container instanceof ReactionNode)
					return importToReaction((ReactionNode)container, aList);
				else if (container instanceof RenderableComplex)
					return importToComplex(container, aList);
				else if (container instanceof RenderableEntity)
					return importToEntity(container, aList);
			}
			return true;
		}
        
        private boolean circularRefValidate(Renderable container, List list) {
            Renderable r = null;
            for (Iterator it = list.iterator(); it.hasNext();) {
                r = (Renderable) it.next();
                if (!circularRefValidate(container, r))
                    return false;
            }
            return true;
        }
        
        private boolean circularRefValidate(Renderable container, Renderable contained) {
            String circularRefName = RenderUtility.searchCircularRef(container, contained);
            if (circularRefName != null) {
                    JOptionPane.showMessageDialog(ProcessTree.this.getParent(),
                            "Circular reference for \"" + circularRefName + "\" can be created.\n" +
                            "This drag-and-drop cannot be allowed.",
                            "Error in Drag-And-Drop",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
            }
            return true;
        }
		
		private boolean importToPathway(Renderable pathway, java.util.List list) {
			// Check if an error will occur
			Renderable r;
			for (Iterator it = list.iterator(); it.hasNext();) {
				r = (Renderable) it.next();
				if (!(r instanceof RenderablePathway ||
				      r instanceof ReactionNode)) {
					JOptionPane.showMessageDialog(ProcessTree.this.getParent(),
					                              "A pathway can only contain pathways or reactions. Complexes or \n" +
					                              "entities cannot be drag-and-dropped into a pathway.",
					                              "Error in Drag-and-Drop",
					                              JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			java.util.List added = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				r = (Renderable) it.next();
				if (r instanceof RenderablePathway ||
					r instanceof ReactionNode) {
					// Need to use the original Renderable object
					r = (Renderable) RenderableRegistry.getRegistry().getSingleObject(r.getDisplayName());
					Renderable shortcut = (Renderable) r.generateShortcut();
					pathway.addComponent(shortcut);
					shortcut.setContainer(pathway);
					//TODO: Is it necessary to add FlowLines automatically for two nodes that
					// are linked originally?
					added.add(shortcut);
				}
			}
			if (added.size() > 0)
				firePropertyChange("dndAddEntities", pathway, added);			
			return true;
		}
		
		private boolean importToReaction(ReactionNode reaction, java.util.List list) {
			// Check if an error message needs to be displayed.
			Renderable r;
			// Escape the first one
			for (Iterator it = list.iterator(); it.hasNext();) {
				r = (Renderable)it.next();
				if (!(r instanceof RenderableComplex || r instanceof RenderableEntity)) {
					JOptionPane.showMessageDialog(
						ProcessTree.this.getParent(),
						"A reaction can only contain entity or complex instances. "
					     + "Pathways or Reactions \ncannot be drag-and-dropped into a reaction.",
						"Error in Drag-And-Drop",
						JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			java.util.List added = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				r = (Renderable)it.next();
				if (r instanceof RenderableComplex || r instanceof RenderableEntity) {
					// Need to use the original Renderable object
					r = (Renderable)RenderableRegistry.getRegistry().getSingleObject(r.getDisplayName());
					Renderable shortcut = (Renderable)r.generateShortcut();
					added.add(shortcut);
				}
			}
			// Use a reaction dialog for assign roles to reactions
			// Use the role table for the entities
			JFrame parentFrame = (JFrame)SwingUtilities.getRoot(ProcessTree.this);
			NodeRoleDialog dialog = new NodeRoleDialog(added, parentFrame);
			dialog.setTitle("Assign Roles to Nodes");
			dialog.setLocationRelativeTo(parentFrame);
			dialog.setModal(true);
			dialog.setSize(300, 200);
			dialog.setVisible(true);
			if (dialog.isOKClicked()) {
				java.util.List roles = dialog.getRoles();
				String role = null;
				Node node = null;
				// Use index: the nodes for inserting have been different from
				// ones in NodeRoleDialog.
				RenderableReaction reactionEdge = reaction.getReaction();
				boolean needAdding = true;
				for (int i = 0; i < added.size(); i++) { 
					node = (Node)added.get(i);
					role = (String)roles.get(i);
					needAdding = true;
					if (role.equals("Input")) {
						java.util.List inputs = reactionEdge.getInputNodes();
						Renderable input = (Renderable) RenderUtility.searchNode(inputs, node.getDisplayName());
						if (input == null) 
							reactionEdge.addInput(node);
						else {
							needAdding = false;
							int stoi = reactionEdge.getInputStoichiometry(input);
							reactionEdge.setInputStoichiometry(input, stoi + 1);
							input.invalidateConnectWidgets();
						}
					}
					else if (role.equals("Output")) {
						java.util.List outputs = reactionEdge.getOutputNodes();
						Renderable output = RenderUtility.searchNode(outputs, node.getDisplayName());
						if (output == null)
							reactionEdge.addOutput(node);
						else {
							needAdding = false;
							int stoi = reactionEdge.getOutputStoichiometry(output);
							reactionEdge.setOutputStoichiometry(output, stoi + 1);
							output.invalidateConnectWidgets();
						}
					}
					else if (role.equals("Catalyst"))
						reactionEdge.addHelper(node);
					else if (role.equals("Inhibitor"))
						reactionEdge.addInhibitor(node);
					else if (role.equals("Activator"))
						reactionEdge.addActivator(node);
					if (needAdding) {
						reaction.addComponent(node);
						node.setContainer(reaction);
					}
				}
			}
			if (added.size() > 0)
				firePropertyChange("dndAddEntities", reaction, added);
			return true;
		}
		
		private boolean importToComplex(Renderable complex, java.util.List list) {
			// Check if an error message needs to be displayed.
			Renderable r;
			// Escape the first one
			for (Iterator it = list.iterator(); it.hasNext();) {
				r = (Renderable) it.next();
				if (!(r instanceof RenderableComplex ||
				      r instanceof RenderableEntity)) {
					JOptionPane.showMessageDialog(ProcessTree.this.getParent(),
					                              "A complex can only contain entity or complex instances. " + 
					                              "Pathways or Reactions \ncannot be drag-and-dropped into a complex.",
					                              "Error in Drag-And-Drop",
					                              JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
			java.util.List added = new ArrayList();
			for (Iterator it = list.iterator(); it.hasNext();) {
				r = (Renderable) it.next();
				if (r instanceof RenderableComplex ||
				    r instanceof RenderableEntity) {
				    // Need to use the original Renderable object
				    r = (Renderable) RenderableRegistry.getRegistry().getSingleObject(r.getDisplayName());
					Renderable shortcut = (Renderable) r.generateShortcut();
					complex.addComponent(shortcut);
					shortcut.setContainer(complex);
					added.add(shortcut);
				}
			}
			if (added.size() > 0)
				firePropertyChange("dndAddEntities", complex, added);
			return true;
		}
		
		private boolean importToEntity(Renderable container, java.util.List list) { 
			JOptionPane.showMessageDialog(ProcessTree.this.getParent(),
			                              "An entity cannot contain any instances.",
			                              "Error in Drag-And-Drop",
			                              JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		protected Transferable createTransferable(JComponent c) {
			ProcessTree tree = (ProcessTree) c;
			TreePath[] paths = tree.getSelectionPaths();
			//transferredData = new ArrayList();
			if (paths != null && paths.length > 0) {
				ArrayList list = new ArrayList(paths.length);
				DefaultMutableTreeNode node = null;
				Renderable renderable = null;
				for (int i = 0; i < paths.length; i++) {
					node = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
					renderable = (Renderable) node.getUserObject();
					if (renderable.getContainer() != null)
						list.add(node.getUserObject());
				}
				if (list.size() > 0) {
					//transferredData.addAll(list);
					list.add(0, "processTree");
					return new ArrayListTransferable(list);
				}
			}
			return null;
		}
		
		/**
		 * Only copy is allowed for DnD.
		 */
		public int getSourceActions(JComponent c) {
			//return COPY_OR_MOVE;
			return COPY; // Support copy only for the time being.
		}
		
//		protected void exportDone(JComponent source, Transferable data, int action) {
//			if (action == MOVE) {
//				if (transferredData == null || transferredData.size() == 0)
//					return;
//				Renderable r = null;
//				for (Iterator it = transferredData.iterator(); it.hasNext();) {
//					r = (Renderable) it.next();
//					delete(r);
//				}
//			}
//		}
	}
}
