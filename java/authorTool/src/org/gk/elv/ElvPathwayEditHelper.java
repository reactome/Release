/*
 * Created on Dec 30, 2008
 *
 */
package org.gk.elv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gk.database.AttributeEditEvent;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.FlowLine;
import org.gk.render.Node;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;

public class ElvPathwayEditHelper extends ElvInstanceEditHandler {
    
    public ElvPathwayEditHelper() {
        
    }
    
    public void pathwayEdit(AttributeEditEvent edit) {
        GKInstance editingInstance = edit.getEditingInstance();
        String attributeName = edit.getAttributeName();
        if (attributeName.equals(ReactomeJavaConstants.hasEvent))
            hasEventChanged(editingInstance,
                            edit);
        else if (attributeName.equals(ReactomeJavaConstants.precedingEvent))
            precedingEventChanged(editingInstance,
                                  edit);
        else if (attributeName.equals(ReactomeJavaConstants._doRelease))
            updateDoNotReleaseEventVisible(editingInstance);
    }
    
    private void precedingEventChanged(GKInstance pathway,
                                       AttributeEditEvent edit) {
        // Check if a related instance are in the display
        List<Renderable> pathwayNodes = zoomableEditor.searchConvertedRenderables(pathway);
        if (pathwayNodes == null || pathwayNodes.size() == 0)
            return;
        // Just pick up one pathway node
        Node pathwayNode = (Node) pathwayNodes.get(0);
        if (edit.getEditingType() == AttributeEditEvent.ADDING) {
            // Check if any added events are in display
            List<GKInstance> added = edit.getAddedInstances();
            for (GKInstance i : added) {
                if (!i.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                    continue; // Link for pathways only
                List<Renderable> list = zoomableEditor.searchConvertedRenderables(i);
                if (list == null || list.size() == 0)
                    continue;
                Node addedNode = (Node) list.get(0);
                createLink(addedNode, pathwayNode);
            }
        }
        else if (edit.getEditingType() == AttributeEditEvent.REMOVING) {
            // Check if any added events are in display
            List<GKInstance> removed = edit.getRemovedInstances();
            for (GKInstance i : removed) {
                if (!i.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                    continue; // Link for pathways only
                // Delete flow lines between this two pathways
                removeLink(i, pathway);
            }
        }
    }
    
    private void removeLink(GKInstance source,
                            GKInstance target) {
        PathwayEditor editor = zoomableEditor.getPathwayEditor();
        List<FlowLine> toBeDeleted = new ArrayList<FlowLine>();
        for (Iterator it = editor.getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof FlowLine) {
                FlowLine flowLine = (FlowLine) r;
                Node input = flowLine.getInputNode(0);
                Node output = flowLine.getOutputNode(0);
                if (input != null && source.getDBID().equals(input.getReactomeId()) &&
                    output != null && target.getDBID().equals(output.getReactomeId())) {
                    toBeDeleted.add(flowLine);
                }
            }
        }
        for (FlowLine line : toBeDeleted)
            editor.delete(line);
    }
    
    private void createLink(Node source,
                            Node target) {
        FlowLine flowLine = new FlowLine();
        flowLine.addInput(source);
        flowLine.addOutput(target);
        flowLine.layout();
        PathwayEditor editor = zoomableEditor.getPathwayEditor();
        editor.insertEdge(flowLine, false);
        editor.repaint(editor.getVisibleRect());
    }
    
    private void hasEventChanged(GKInstance pathway,
                                 AttributeEditEvent edit) {
        if (edit.getEditingType() == AttributeEditEvent.REMOVING) {
            // Check the displayed top-level pathway to see if the removed instance should be
            // deleted from the diagram
            try {
                List<GKInstance> topMosts = zoomableEditor.getDisplayedPathways();
                if (topMosts == null || topMosts.size() == 0)
                    return;
                Set<GKInstance> allEvents = InstanceUtilities.getContainedEvents(topMosts);
                allEvents.addAll(topMosts);
                if (allEvents.contains(edit.getEditingInstance())) {
                    // Need to check if the removed instance is still used by the top-most event
                    List<GKInstance> removed = edit.getRemovedInstances();
                    for (GKInstance tmp : removed) {
                        if (allEvents.contains(tmp))
                            continue;
                        // Delete it
                        zoomableEditor.deleteInstance(tmp);
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void postInsert(GKInstance instance, 
                           Renderable renderable) throws Exception {
        if (!(renderable instanceof ProcessNode))
            return;
        ProcessNode pathwayNode = (ProcessNode) renderable;
        // Get any processNodes that may be linked to this node
        List<ProcessNode> pathwayNodes = new ArrayList<ProcessNode>();
        for (Iterator it = zoomableEditor.getPathwayEditor().getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof ProcessNode) {
                pathwayNodes.add((ProcessNode)r);
            }
        }
        // Remove itself from the list
        pathwayNodes.remove(renderable);
        if (pathwayNodes.size() == 0)
            return; // Nothing else displayed.
        // First search if any precedingEvent has been displayed in the diagram
        List precedingEvents = instance.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
        if (precedingEvents != null && precedingEvents.size() > 0) {
            for (Iterator it = precedingEvents.iterator(); it.hasNext();) {
                GKInstance precedingEvent = (GKInstance) it.next();
                // Find node for this precedingEvent
                for (ProcessNode node : pathwayNodes) {
                    if (node.getReactomeId() != null && node.getReactomeId().equals(precedingEvent.getDBID())) {
                        createLink(node, pathwayNode);
                    }
                }
            }
        }
        // Second search for followingEvent
        XMLFileAdaptor fileAdaptor = zoomableEditor.getXMLFileAdaptor();
        for (ProcessNode node : pathwayNodes) {
            if (node.getReactomeId() == null)
                continue;
            GKInstance nodeInst = fileAdaptor.fetchInstance(node.getReactomeId());
            if (nodeInst == null)
                continue; // This should not occur
            List list = nodeInst.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
            if (list != null && list.contains(instance))
                createLink(pathwayNode, node);
        }
    }
    
    
}
