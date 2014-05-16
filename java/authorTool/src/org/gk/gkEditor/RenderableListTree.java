/*
 * Created on Jan 3, 2007
 *
 */
package org.gk.gkEditor;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.TypeTreeCellRenderer;
import org.gk.render.FlowLine;
import org.gk.render.Renderable;
import org.gk.render.RenderableInteraction;
import org.gk.render.RenderablePathway;
import org.gk.render.Shortcut;
import org.gk.util.TreeUtilities;

public abstract class RenderableListTree extends JTree implements Selectable {
    // Editing pathway
    protected RenderablePathway pathway;
    
    protected void initTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("EMPTY");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        setModel(treeModel);
        setShowsRootHandles(true);
        setRootVisible(false);
        // Set renderer to use customized renderer
        TypeTreeCellRenderer renderer = new TypeTreeCellRenderer();
        // Default is not shown
        //renderer.setShowIsChanged(true);
        setCellRenderer(renderer);
    }
    
    public void showIsChanged(boolean shown) {
        TypeTreeCellRenderer renderer = (TypeTreeCellRenderer) getCellRenderer();
        boolean oldValue = renderer.showIsChanged();
        if (oldValue == shown)
            return;
        renderer.setShowIsChanged(shown);
        // Have to update all tree nodes
        refresh();
    }
    
    private void refresh() {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        List current = new ArrayList();
        List next = new ArrayList();
        current.add(root);
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) it.next();
                model.nodeChanged(node);
                if (node.getChildCount() > 0) {
                    for (int i = 0; i < node.getChildCount(); i++)
                        next.add(node.getChildAt(i));
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
    }
    
    public abstract void open(RenderablePathway process);
    
    public void refresh(Renderable r) {
        // Maybe listed in complexes if r is a node
        List nodes = searchNodes(r);
        if (nodes == null || nodes.size() == 0)
            return;
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
            model.nodeChanged(treeNode);
        }
    }
    
    public void refreshNode(Renderable r) {
    }
    
    protected java.util.List searchNodes(Renderable r) {
        java.util.List nodes = new ArrayList();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) (getModel().getRoot());
        searchNodes(r, root, nodes);
        return nodes;
    }
    
    private void searchNodes(Renderable r, 
                             DefaultMutableTreeNode treeNode, 
                             List nodes) {
        Object userObject = treeNode.getUserObject();
        if (userObject instanceof Renderable) {
            Renderable tmp = (Renderable) userObject;
            String displayName = tmp.getDisplayName();
            if (displayName.equals(r.getDisplayName()))
                nodes.add(treeNode);
        }
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            searchNodes(r, node, nodes);
        }
    }
    
    public abstract void insert(Renderable r);
    
    public void delete(List list) {
        for (Iterator it = list.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            delete(r);
        }
    }
    
    protected boolean displayable(Renderable r) {
        // Shortcut should be listed too as a target can be copied from
        // a complex component
//        if (r instanceof Shortcut)
//            return false; // Don't add shortcuts. Targets should be listed.
        if (r instanceof RenderableInteraction)
            return true;
        if (r instanceof FlowLine) {
            return false; // cannot insert
        }
        return true;
    }
    
    public abstract void delete(Renderable r);
    
    public void setSelection(List selected) {
        clearSelection();
        int firstRow = Integer.MAX_VALUE;
        for (Iterator it = selected.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (!(obj instanceof Renderable))
                continue;
            List nodeList = searchNodes((Renderable)obj);
            if (nodeList == null || nodeList.size() == 0)
                continue;
            for (Iterator it1 = nodeList.iterator(); it1.hasNext();) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) it1.next();
                TreePath path = new TreePath(node.getPath());
                addSelectionPath(path);
                int row = getRowForPath(path);
                if (row < firstRow) {
                    firstRow = row;
                }
            }
        }
        // make sure the first path is visible
        if (firstRow < Integer.MAX_VALUE)
            scrollRowToVisible(firstRow);
    }
    
    /**
     * Override this method so that the tree will not scroll too much.
     * Just want to keep the hierchical visible as much as possible.
     */
    public void scrollRowToVisible(int row) {
        TreePath path = getPathForRow(row);
        if(path != null) {
            makeVisible(path);
            Rectangle bounds = getPathBounds(path);
            if(bounds != null) {
                // Compare to visible rectanle
                Rectangle vis = getVisibleRect();
                if (vis.intersects(bounds))
                    return; // Don't need to scroll 
                scrollRectToVisible(bounds);
                if (accessibleContext != null) {
                    ((AccessibleJTree)accessibleContext).fireVisibleDataPropertyChange();
                }
            }
        }
    }

    public List getSelection() {
        Set rtn = new HashSet();
        TreePath[] paths = getSelectionPaths();
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                Object userObj = node.getUserObject();
                if (userObj instanceof Renderable) {
                    // want to select all 
                    Renderable target = null;
                    if (userObj instanceof Shortcut) {
                        target = ((Shortcut)userObj).getTarget();
                    }
                    else
                        target = (Renderable)userObj;
                    rtn.add(target);
                    if (target.getShortcuts() != null)
                        rtn.addAll(target.getShortcuts());
                }
            }
        }
        return new ArrayList(rtn);
    }

    protected void insertStringNodeAlphabetically(String text, DefaultMutableTreeNode newNode, DefaultMutableTreeNode parentNode) {
        int c = parentNode.getChildCount();
        int index = c;
        DefaultMutableTreeNode node;
        for (int i = 0; i < c; i++) {
            node = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            String name = (String) node.getUserObject();
            if (name.compareTo(text) > 0) {
                index = i;
                break;
            }
        }
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        if (index < c)
            model.insertNodeInto(newNode, parentNode, index);
        else { // This is rather strange: have to use this way to attach a node
               // to the end of another node.
            parentNode.add(newNode);
            model.nodeStructureChanged(parentNode);
        }
    }

    protected DefaultMutableTreeNode insert(Renderable r, 
                                            DefaultMutableTreeNode typeNode) {
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(r);
        String name = r.getDisplayName();
        // find a position
        int c = typeNode.getChildCount();
        int index = c;
        for (int i = 0; i < c; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) typeNode.getChildAt(i);
            Renderable tmp = (Renderable) node.getUserObject();
            if (tmp.getDisplayName() == null)
                throw new IllegalStateException("RenderableListTree.insert(): " + tmp.getID() + " has null name!");
            // Name is unique
            if (tmp.getDisplayName().equals(name))
                return node; // inserted already
            if (tmp.getDisplayName().compareTo(name) > 0) {
                index = i;
                break;
            }
        }
        // Use insert from model to fire node change event
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        model.insertNodeInto(newNode, typeNode, index);
        TreeUtilities.expandAllNodes(typeNode, this);
        return newNode;
    }

    /**
     * Make sure the list is correct after shortcuts have been removed from 
     * the target.
     * @param target
     * @param shortcutName
     */
    public void delinkShortcuts(Renderable target, Renderable  shortcut) {
        List treeNodes = searchNodes(target);
        if (treeNodes != null && treeNodes.size() > 0) {
            // This node has been displayed. Insert the shortcut
            insert(shortcut);
            // Need to update these nodes since the name has been changed
            refresh(target);
        }
        else {
            // Need to insert a new node
            insert(target);
        }
    }
}
