/*
 * Created on Feb 26, 2007
 *
 */
package org.gk.gkEditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gk.render.*;
import org.gk.util.TreeUtilities;

public class EntityPropertyEditorTree extends RenderableListTree {

    public EntityPropertyEditorTree() {
        initTree();
    }
    
    private void reset() {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();
        model.nodeStructureChanged(root);
    }

    public void open(RenderablePathway process) {
        // reset first
        reset();
        // add event hierarchy node
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        // Have to get all these list
        List components = process.getComponents();
        if (components == null || components.size() == 0) {
            return;
        }
        // Filter first to avoid a big list sorting
        List<Renderable> list = new ArrayList<Renderable>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (displayable(r))
                list.add(r);
        }
        // Sort once so that no sorting is needed after loaded
        RenderUtility.sort(list);
        for (Renderable r : list) {
            insert(r);
        }
        model.nodeStructureChanged(root);
        TreeUtilities.expandAllNodes(root, this);
    }
    
    private void insertNode(Node node) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        // Need to sort out
        DefaultMutableTreeNode nodeTypeNode = getNodeTypeNode(node, root);
        insert(node, nodeTypeNode);
    }
    
    private DefaultMutableTreeNode getNodeTypeNode(Node node,
                                                   DefaultMutableTreeNode entityNode) {
        // search
        String typeName = getNodeTypeName(node);
        int count = entityNode.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) entityNode.getChildAt(i);
            if (tmp.getUserObject().equals(typeName))
                return tmp;
        }
        DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(typeName);
        insertStringNodeAlphabetically(typeName, typeNode, entityNode);
        return typeNode;
    }
    
    private String getNodeTypeName(Node node) {
        String type = null;
        if (node instanceof RenderableProtein)
            return "Proteins";
        if (node instanceof RenderableChemical)
            return "Compounds";
        if (node instanceof RenderableGene)
            return "Genes";
        if (node instanceof RenderableRNA)
            return "RNAs";
        return "OtherEntities";
    }
    
    public void delete(Renderable r) {
        if (!displayable(r))
            return;
        if (r instanceof Node)
            deleteNode((Node)r);
    }
    
    private void deleteNode(Node node) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        DefaultMutableTreeNode treeNode = TreeUtilities.searchNode(node, root);
        if (treeNode == null)
            return; // Should not be possible
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) treeNode.getParent();
        model.removeNodeFromParent(treeNode);
        if (parentNode.getChildCount() == 0)
            model.removeNodeFromParent(parentNode);
    }
    
    protected boolean displayable(Renderable r) {
        if (r instanceof Note ||
            r instanceof RenderableCompartment ||
            r instanceof RenderablePathway)
            return false;
        // Complex cannot be displayed in this tree
        if (r instanceof RenderableComplex)
            return false;
        return super.displayable(r);
    }

    public void insert(Renderable r) {
        if (!displayable(r))
            return;
        if (r instanceof Node)
            insertNode((Node)r);
    }
}
