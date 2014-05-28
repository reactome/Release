/*
 * Created on Feb 25, 2007
 *
 */
package org.gk.gkEditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gk.render.Node;
import org.gk.render.Note;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderablePathway;
import org.gk.util.TreeUtilities;

public class ComplexPropertyEditorTree extends RenderableListTree {
    private DefaultTreeModel entityModel;
    
    public ComplexPropertyEditorTree() {
        initTree();
    }
    
    public void setEntityModel(DefaultTreeModel model) {
        this.entityModel = model;
    }
    
    private void reset() {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();
        model.nodeStructureChanged(root);
    }
    
    @Override
    public void refreshNode(Renderable complex) {
        List nodes = searchNodes(complex);
        if (nodes == null || nodes.size() == 0)
            return ;
        // Temp disable selection listener
        TreeSelectionListener[] listeners = getTreeSelectionListeners();
        if (listeners != null) {
            for (TreeSelectionListener l : listeners)
                removeTreeSelectionListener(l);
        }
        // This maybe slow, and should be changed in the future
        open(pathway);
        if (listeners != null) {
            for (TreeSelectionListener l : listeners)
                addTreeSelectionListener(l);
        }
    }

    /**
     * Display RenderableComplex objects from the passed RenderablePathway objects.
     */
    public void open(RenderablePathway process) {
        this.pathway = process;
        // reset first
        reset();
        // Have to get all these list
        List components = process.getComponents();
        if (components == null || components.size() == 0) {
            return;
        }
        // Want to get the toplevel components
        List<RenderableComplex> complexes = new ArrayList<RenderableComplex>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (!(r instanceof RenderableComplex))
                continue;
            if (!(r.getContainer() instanceof RenderableComplex))
                complexes.add((RenderableComplex)r);
        }
        // Sort once so that no sorting is needed after distributed
        RenderUtility.sort(complexes);
        open(complexes);
    }

    /**
     * Open a list of RenderbaleComplex objects in this list.
     * @param complexes
     */
    public void open(List<RenderableComplex> complexes) {
        for (RenderableComplex complex : complexes) {
            insert(complex);
        }
        // add event hierarchy node
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        model.nodeStructureChanged(root);
        TreeUtilities.expandAllNodes(root, this);
    }
    
    private void insertComplex(RenderableComplex complex) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        DefaultMutableTreeNode newNode = insert(complex, root);
        insertComplexComponents(complex, newNode);
        checkComplexComponents(complex);
    }
    
    /**
     * Some nodes are wrapped into the complex and should not be displayed by themselves.
     * @param complex
     */
    private void checkComplexComponents(RenderableComplex complex) {
        Renderable pathway = RenderUtility.getTopMostContainer(complex);
        List components = complex.getComponents();
        if (components == null || components.size() == 0)
            return;
        Renderable r = null;
        for (Iterator it = components.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            if (pathway.getComponentByName(r.getDisplayName()) != null)
                continue;
            // Need to delete from the top entities
            if (r instanceof RenderableComplex)
                deleteComplexComp((RenderableComplex)r); // A width first search is done here
            else if (r instanceof Node)
                cleanUpNode((Node)r);
        }
    }
    
    private void cleanUpNode(Node node) {
        // Just in case: may occurs during opening a complex 
        if (entityModel == null)
            return; 
        DefaultMutableTreeNode entityRoot = (DefaultMutableTreeNode) entityModel.getRoot();
        DefaultMutableTreeNode treeNode = TreeUtilities.searchNode(node, entityRoot);
        if (treeNode == null)
            return; // Should not be possible
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) treeNode.getParent();
        entityModel.removeNodeFromParent(treeNode);
    }
    
    private void deleteComplexComp(Renderable comp) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        // Check only the top level
        int c = root.getChildCount();
        for (int i = 0; i < c; i++) {
            DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) root.getChildAt(i);
            if (tmp.getUserObject() == comp) {
                DefaultTreeModel model = (DefaultTreeModel) getModel();
                model.removeNodeFromParent(tmp);
                break;
            }
        }
    }
    
    private void insertComplexComponents(Renderable container, 
                                         DefaultMutableTreeNode containerNode) {
        List components = container.getComponents();
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable tmp = (Renderable) it.next();
            DefaultMutableTreeNode tmpNode = insert(tmp, containerNode);
            insertComplexComponents(tmp, tmpNode);
        }
    }
    
    public void delete(Renderable r) {
        // Shortcut is regarded as non-displayable. However,
        // it might be wrapped as components of complex.
        //if (!displayable(r))
        //    return;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        DefaultMutableTreeNode treeNode = TreeUtilities.searchNode(r,
                                                                   root);
        if (treeNode == null)
            return;
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        model.removeNodeFromParent(treeNode);
    }
    
    protected boolean displayable(Renderable r) {
        if (r instanceof Note)
            return false;
        return super.displayable(r);
    }

    public void insert(Renderable r) {
        if (!displayable(r))
            return;
        if (r instanceof RenderableComplex) {
            insertComplex((RenderableComplex)r);
        }
    }
}
