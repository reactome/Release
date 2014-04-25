/*
 * Created on Feb 24, 2007
 *
 */
package org.gk.gkEditor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gk.render.HyperEdge;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderableInteraction;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.util.TreeUtilities;

public class EventPropertyEditorTree extends RenderableListTree {
    //  Keep these nodes to speed up searching
    private DefaultMutableTreeNode processNode;
    
    public EventPropertyEditorTree() {
        initTree();
    }
    
    private void reset() {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();
        processNode = null;
    }
    
    public void open(RenderablePathway process) {
        // reset first
        reset();
        // add event hierarchy node
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        processNode = new DefaultMutableTreeNode(process);
        model.insertNodeInto(processNode, root, 0);
        // Have to get all these list
        List components = process.getComponents();
        if (components == null || components.size() == 0) {
            model.nodeStructureChanged(root);
            TreeUtilities.expandAllNodes(root, this);
            return;
        }
        // Filter it first
        List<Renderable> events = new ArrayList<Renderable>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (displayable(r))
                events.add(r);
        }
        // Sort once so that no sorting is needed after distributed
        RenderUtility.sort(events);
        for (Renderable r : events) {
            insert(r);
        }
        model.nodeStructureChanged(root);
        TreeUtilities.expandAllNodes(root, this);
    }
    
    private void insertPathwayComponents(Renderable container,
                                         DefaultMutableTreeNode containerNode) {
        List components = container.getComponents();
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable tmp = (Renderable) it.next();
            if (!displayablePathwayComp(tmp))
                continue;
            DefaultMutableTreeNode tmpNode = insert(tmp, containerNode);
            insertPathwayComponents(tmp, tmpNode);
            // have to remove from processNode
            removePathwayComponent(tmp);
        }        
    }
    
    private boolean displayablePathwayComp(Renderable r) {
        if (!displayable(r))
            return false;
        if (r instanceof RenderablePathway ||
            r instanceof HyperEdge)
            return true;
        return false;
    }
    
    private void removePathwayComponent(Renderable comp) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        int count = processNode.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) processNode.getChildAt(i);
            if (node.getUserObject() == comp) {
                model.removeNodeFromParent(node);
                break;
            }
        }
    }
    
    private void insertEvent(Renderable event) {
        // process node should not be null
        if (processNode == null)
            throw new IllegalStateException("PropertyEditorTree.insertEvent(): process node is null!");
        Renderable container = (Renderable) processNode.getUserObject();
        if (event.getContainer() instanceof RenderablePathway &&
            event.getContainer() != container)
            return; // this event should be inserted somewhere else
        DefaultMutableTreeNode eventNode = insert(event, processNode);
        if (event instanceof RenderablePathway) {
            RenderablePathway pathway = (RenderablePathway) event;
            insertPathwayComponents(pathway, eventNode);
        }
    }
    
    public void delete(Renderable r) {
        if (!displayable(r))
            return;
        if (r instanceof RenderablePathway) 
            deletePathway((RenderablePathway)r);
        else if (r instanceof RenderableReaction) 
            deleteInteraction(r);
        else if (r instanceof RenderableInteraction) 
            deleteInteraction(r);
    }
    
    private void deletePathway(RenderablePathway pathway) {
        List treeNodes = new ArrayList();
        TreeUtilities.searchNodes(pathway,
                                  processNode,
                                  treeNodes);
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        for (Iterator it = treeNodes.iterator(); it.hasNext();) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
            model.removeNodeFromParent(treeNode);
        }
        // Need to add components back
        List components = pathway.getComponents();
        if (components == null)
            return;
        treeNodes.clear();
        // Pathway's container might be null after deleting.
        // Try to get container from components
        Renderable tmp = (Renderable) components.get(0);
        Renderable container = tmp.getContainer();
        if (container == null)
            treeNodes.add(processNode); // Have to add to the top
        else
            // Get tree nodes for container
            TreeUtilities.searchNodes(container,
                                      processNode,
                                      treeNodes);
        for (Iterator it = treeNodes.iterator(); it.hasNext();) {
            DefaultMutableTreeNode containerNode = (DefaultMutableTreeNode) it.next();
            for (Iterator it1 = components.iterator(); it1.hasNext();) {
                Renderable r = (Renderable) it1.next();
                // Have to check: r may be contained by container already.
                if (displayablePathwayComp(r) &&
                        !isPathwayComponentDisplayed(r, containerNode))
                    insert(r, containerNode);
            }
        }
    }
    
    private boolean isPathwayComponentDisplayed(Renderable comp,
                                                DefaultMutableTreeNode pathwayNode) {
        int c = pathwayNode.getChildCount();
        if (c == 0)
            return false;
        for (int i = 0; i < c; i++) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) pathwayNode.getChildAt(i);
            if (treeNode.getUserObject() == comp)
                return true;
        }
        return false;
    }
    
    private void deleteInteraction(Renderable r) {
        List nodes = new ArrayList();
        TreeUtilities.searchNodes(r,
                                  processNode,
                                  nodes);
        if (nodes.size() == 0)
            return; // Should not be possible
        // Need to delete all interactions
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) it.next();
            model.removeNodeFromParent(node);
        }
    }
    
    protected boolean displayable(Renderable r) {
        if (r instanceof RenderablePathway ||
            r instanceof RenderableReaction ||
            r instanceof RenderableInteraction)
            return true;
        return false;
    }

    public void insert(Renderable r) {
        if (!displayable(r))
            return;
        insertEvent(r);
    }
}
