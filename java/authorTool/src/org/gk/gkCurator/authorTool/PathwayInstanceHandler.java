/*
 * Created on Jan 18, 2007
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.FlowLine;
import org.gk.render.Node;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableFactory;
import org.gk.render.RenderablePathway;

public class PathwayInstanceHandler extends InstanceHandler {
    
    protected Renderable convertToRenderable(GKInstance instance) {
        Renderable r = RenderableFactory.generateRenderable(RenderablePathway.class, 
                                                            container);
        return r;
    }

    public void convertProperties(GKInstance instance, 
                                  Renderable renderable,
                                  Map iToRMap) throws Exception {
        RenderablePathway pathway = (RenderablePathway) renderable;
        if (pathway == container)
            return; // Escape it self. This is the topmost.
        // Get the components into pathways
        List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        if (components == null || components.size() == 0) {
            // Should try to link to other pathways
            convertPrecedingProperties(instance, 
                                       (Node)renderable, 
                                       iToRMap);
            return;
        }
        Set pathwayComponents = new HashSet(); // Use a set to avoid the duplications of using 
                                               // same Instances in multiple reactions
        pathwayComponents.addAll(components);
        for (Iterator it = components.iterator(); it.hasNext();) {
            GKInstance comp = (GKInstance) it.next();
            // Want to add reaction participants too
            if (comp.getSchemClass().isa(ReactomeJavaConstants.Reaction)) {
                Set participants = InstanceUtilities.getReactionParticipants(comp);
                if (participants != null)
                    pathwayComponents.addAll(participants);
            }
        }
        for (Iterator it = pathwayComponents.iterator(); it.hasNext();) {
            GKInstance comp = (GKInstance) it.next();
            Renderable r = (Renderable) iToRMap.get(comp);
            if (r == null) {
                System.err.println("Cannot convert: " + comp);
                continue;
            }
            pathway.addComponent(r);
            r.setContainer(pathway); // Relink to pathway
        }
    }

    @Override
    public Renderable simpleConvertToRenderable(GKInstance instance)
            throws Exception {
        Renderable r = RenderableFactory.generateRenderable(ProcessNode.class, 
                                                            container);
        return r;
    }

    private void convertPrecedingEvents(GKInstance instance,
                                        Renderable r,
                                        Map<GKInstance, Renderable> rToRMap) throws Exception {
        if (rToRMap == null)
            return; // Have nothing to do!
        List<GKInstance> precedingEvents = (List<GKInstance>) instance.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
        if (precedingEvents == null || precedingEvents.size() == 0)
            return;
        for (GKInstance event : precedingEvents) {
            Renderable preR = rToRMap.get(event);
            if (preR == null)
                continue;
            addLink(preR, r);
        }
    }
    
    /**
     * Add a FlowLine link between source and target events
     * @param source
     * @param target
     */
    private void addLink(Renderable source, 
                         Renderable target) {
        if (!(source instanceof ProcessNode) ||
            !(target instanceof ProcessNode))
            return;
        ProcessNode sourceNode = (ProcessNode) source;
        ProcessNode targetNode = (ProcessNode) target;
        FlowLine flowLine = (FlowLine) RenderableFactory.generateRenderable(FlowLine.class,
                                                                            container);
        flowLine.addInput(sourceNode);
        flowLine.addOutput(targetNode);
        container.addComponent(flowLine);
    }
    
    
    @Override
    public void simpleConvertProperties(GKInstance instance, 
                                        Renderable r,
                                        Map<GKInstance, Renderable> toRMap) throws Exception {
        if (r instanceof ProcessNode) {
            convertPrecedingEvents(instance, r, toRMap);
            return;
        }
        convertProperties(instance, 
                          r, 
                          toRMap);
    }
    
    
}
