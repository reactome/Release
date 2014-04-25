/*
 * Created on Nov 19, 2008
 *
 */
package org.gk.elv;

import java.awt.BorderLayout;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.gk.database.AttributeEditEvent;
import org.gk.model.GKInstance;
import org.gk.util.TreeUtilities;

public abstract class InstanceTreePane extends JPanel {
    protected JTree tree;
    
    public InstanceTreePane() {
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        // Set up the tree
        tree = new JTree();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        EntityInstanceCellRenderer renderer = new EntityInstanceCellRenderer();
        tree.setCellRenderer(renderer);
        // Add tree to this JPanel.
        JScrollPane jsp = new JScrollPane(tree);
        add(jsp, BorderLayout.CENTER);
        // To handle DnD
        tree.setDragEnabled(true);
    }
    
    public JTree getTree() {
        return this.tree;
    }
    
    /**
     * Get the selected GKInstance only.
     * @return
     */
    public List<GKInstance> getSelectedInstances() {
        List<GKInstance> list = new ArrayList<GKInstance>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                Object userObj = treeNode.getUserObject();
                if (userObj instanceof GKInstance)
                    list.add((GKInstance)userObj);
                // The selected object may be a SchemaClass
            }
        }
        return list;
    }
    
    public void setSelectedInstances(List<GKInstance> selection) {
        tree.getSelectionModel().clearSelection();
        int firstRow = Integer.MAX_VALUE;
        for (GKInstance instance : selection) {
            List nodeList = TreeUtilities.searchNodes(instance, tree);
            if (nodeList == null || nodeList.size() == 0)
                continue;
            for (Iterator it1 = nodeList.iterator(); it1.hasNext();) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) it1.next();
                TreePath path = new TreePath(node.getPath());
                tree.addSelectionPath(path);
                int row = tree.getRowForPath(path);
                if (row < firstRow) {
                    firstRow = row;
                }
            }
        }
        // make sure the first path is visible
        if (firstRow < Integer.MAX_VALUE)
            tree.scrollRowToVisible(firstRow);
    }
    
    /**
     * Delete an instance from the list.
     * @param instance
     */
    public void deleteInstance(GKInstance instance) {
        if (!isDisplayable(instance))
            return;
        List treeNodes = TreeUtilities.searchNodes(instance, tree);
        if (treeNodes == null || treeNodes.size() == 0)
            return;
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        for (Iterator it = treeNodes.iterator(); it.hasNext();) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
            model.removeNodeFromParent(treeNode);
        }
    }
    
    /**
     * Add a new GKInstance to this list view.
     * @param instance
     */
    public abstract void addInstance(GKInstance instance); 
    
    public void updateInstance(AttributeEditEvent editEvent) {
        GKInstance instance = editEvent.getEditingInstance();
        updateInstance(instance);
    }
    
    public void updateInstance(GKInstance instance) {
        if (!isDisplayable(instance))
            return;
        List treeNodes = TreeUtilities.searchNodes(instance, tree);
        if (treeNodes == null || treeNodes.size() == 0)
            return;
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        for (Iterator it = treeNodes.iterator(); it.hasNext();) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
            model.nodeChanged(treeNode);
            // Reinsert in case display name has been changed
            reinsert(treeNode, model);
        }
    }
    
    /**
     * Re-insert an Instance tree node in case the display for the wrapped GKInstance
     * has been changed because of an attribute editing.
     * @param treeNode
     * @param model
     */
    protected void reinsert(DefaultMutableTreeNode treeNode,
                            DefaultTreeModel model) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) treeNode.getParent();
        model.removeNodeFromParent(treeNode);
        insertInstanceNodeAlphabetically(parent, treeNode);
    }

    protected void insertInstanceNodeAlphabetically(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode instanceNode) {
        GKInstance instance = (GKInstance) instanceNode.getUserObject();
        // Need to insert instance into clsNode
        int index = 0;
        if (instance.getDisplayName() == null) {
            index = parentNode.getChildCount(); // Add to the end of the list
        }
        else if (parentNode.getChildCount() > 0) {
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                GKInstance nodeInst = (GKInstance) node.getUserObject();
                if (nodeInst.getDisplayName().compareTo(instance.getDisplayName()) > 0) {
                    index = i;
                    break;
                }
            }
        }
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.insertNodeInto(instanceNode, parentNode, index);
        // Expand the node if it is not
        TreeNode[] pathNodes = model.getPathToRoot(parentNode);
        TreePath treePath = new TreePath(pathNodes);
        if (!tree.isExpanded(treePath))
            tree.expandPath(treePath);
    }
    
    public void addTreeMouseListener(MouseListener listener) {
        tree.addMouseListener(listener);
    }
    
    public abstract boolean isDisplayable(GKInstance instance);
    
}
