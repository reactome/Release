/*
 * Created on Dec 5, 2008
 *
 */
package org.gk.elv;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.gk.graphEditor.ArrayListTransferable;
import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.GraphEditorTransferHandler;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;

/**
 * Subclass to GraphEditorTransferHandler so that it can be used without modification in lower package.
 * @author wgm
 *
 */
public class SimpleInstanceTransferHandler extends GraphEditorTransferHandler {
    
    public SimpleInstanceTransferHandler() {
    }
    
    public boolean importListOfRenderables(ArrayList aList, 
                                           GraphEditorPane graphPane) {
        if (aList == null || aList.size() == 0)
            return true;
        String source = (String) aList.get(0);
        if (source.equals(InstanceTreePane.class.getName())) {
            aList.remove(0);
            return importObjectFromTree(aList, 
                                        (PathwayEditor)graphPane);
        }
        else
            return super.importListOfRenderables(aList, graphPane);
    }    

    private boolean importObjectFromTree(ArrayList reactomeIds,
                                         PathwayEditor pathwayEditor) {
        InstanceZoomablePathwayEditor zoomableEditor = (InstanceZoomablePathwayEditor) SwingUtilities.getAncestorOfClass(InstanceZoomablePathwayEditor.class,
                                                                                                                         pathwayEditor);
        // Track newly added objects for selection
        List<Renderable> list0 = null;
        if (pathwayEditor.getDisplayedObjects() != null)
            list0 = new ArrayList<Renderable>(pathwayEditor.getDisplayedObjects());
        else
            list0 = new ArrayList<Renderable>();
        
        XMLFileAdaptor fileAdaptor = zoomableEditor.getXMLFileAdaptor();
        List<GKInstance> reactions = new ArrayList<GKInstance>();
        List<GKInstance> entities = new ArrayList<GKInstance>();
        for (Iterator it = reactomeIds.iterator(); it.hasNext();) {
            Long id = (Long) it.next();
            GKInstance instance = fileAdaptor.fetchInstance(id);
            if (instance == null)
                continue;
            if (instance.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
                reactions.add(instance);
            else {
                entities.add(instance);
                //zoomableEditor.insertInstance(instance);
            }
        }
        if (entities.size() > 0) {
            // Allow entities be DnD to the drawing diagrams.
//            if (zoomableEditor.isUsedAsDrawingTool()) {
//                JOptionPane.showMessageDialog(zoomableEditor, 
//                                              "ELV is used as a drawing tool only. In this mode, only reactions can be added to diagrams.",
//                                              "Warning!", 
//                                              JOptionPane.WARNING_MESSAGE);
//            }
//            else {
                for (GKInstance entity : entities)
                    zoomableEditor.insertInstance(entity);
//            }
        }
        if (reactions.size() > 0) {
            handleReactions(reactions, 
                            zoomableEditor);
        }
        // To get objects to be selected
        List<Renderable> list1 = null;
        if (pathwayEditor.getDisplayedObjects() != null)
            list1 = new ArrayList<Renderable>(pathwayEditor.getDisplayedObjects());
        else
            list1 = new ArrayList<Renderable>();
        list1.removeAll(list0);
        // Do a layout for reactions
        for (Renderable r : list1) {
            if (r instanceof HyperEdge) {
                ((HyperEdge)r).layout();
            }
        }
        pathwayEditor.setSelection(list1);
        return true;
    }
    
    private void handleReactions(List<GKInstance> reactions,
                                 InstanceZoomablePathwayEditor zoomableEditor) {
        Set<GKInstance> allEvents = new HashSet<GKInstance>();
        try {
            // Make sure inserting reactions should be contained by the displayed pathway
            List<GKInstance> containers = zoomableEditor.getDisplayedPathways();
            for (GKInstance container : containers) {
                allEvents.addAll(InstanceUtilities.getContainedEvents(container));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        List<GKInstance> excluded = new ArrayList<GKInstance>();
        for (GKInstance rxt : reactions) {
            if (allEvents.contains(rxt))
                continue;
            excluded.add(rxt);
        }
        if (excluded.size() > 0) {
            // Give out an information
            StringBuilder builder = new StringBuilder();
            if (excluded.size() == 1)
                builder.append("The following reaction is not contained by the displayed pathway. It cannot be dropped into this diagram:");
            else
                builder.append("The following reactions are not contained by the displayed pathway. They cannot be dropped into this diagram:");
            for (GKInstance inst : excluded)
                builder.append("\n").append(inst.toString());
            JOptionPane.showMessageDialog(zoomableEditor, 
                                          builder.toString(), 
                                          "Cannot Drop Reaction",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
        reactions.removeAll(excluded);
        for (GKInstance rxt : reactions) {
            // Want to link to reaction
            zoomableEditor.insertReactionInFull(rxt);
        }
    }
    
    protected Transferable createTransferable(JComponent c) {
        ArrayList<Object> list = new ArrayList<Object>();
        if (c instanceof PathwayEditor) {
            return super.createTransferable(c);
        }
        else if (c instanceof JTree) {
            list.add(InstanceTreePane.class.getName());
            JTree tree = (JTree) c;
            if (tree.getSelectionCount() > 0) {
                TreePath[] paths = tree.getSelectionPaths();
                for (int i = 0; i < paths.length; i++) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                    if (treeNode.getUserObject() instanceof GKInstance) {
                        GKInstance instance = (GKInstance) treeNode.getUserObject();
                        list.add(instance.getDBID());
                    }
                }
            }
        }
        return new ArrayListTransferable(list);
    }

//    @Override
//    public int getSourceActions(JComponent c) {
//        return COPY;
//    }
    
}
