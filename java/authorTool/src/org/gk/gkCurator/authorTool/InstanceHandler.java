/*
 * Created on Jan 18, 2007
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.FlowLine;
import org.gk.render.InstanceToRenderableConverter;
import org.gk.render.Node;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableFactory;

public abstract class InstanceHandler {
    protected Renderable container;
    
    protected InstanceHandler() {
    }
    
    public void setContainer(Renderable r) {
        this.container = r;
    }
    
    public Renderable getContainer() {
        return this.container;
    }
    
    public Renderable convert(GKInstance instance,
                              Map iToRMap) throws Exception {
        if (iToRMap != null) {
            Renderable r = (Renderable) iToRMap.get(instance);
            if (r != null)
                return r;
        }
        Renderable r = convertToRenderable(instance);
        // Copy two most important properties
        r.setDisplayName(instance.getDisplayName());
        r.setReactomeId(instance.getDBID());
        if (iToRMap != null)
            iToRMap.put(instance, r);
        if (container != null) {
            container.addComponent(r);
            r.setContainer(container);
        }
        return r;
    }
    
    /**
     * Use this method to convert a GKInstance to Renderable object without considering shortcuts.
     * @param instance
     * @return
     * @throws Exception
     */
    public Renderable simpleConvert(GKInstance instance) throws Exception {
        Renderable r = simpleConvertToRenderable(instance);
        // Copy two most important properties
        r.setDisplayName(instance.getDisplayName());
        r.setReactomeId(instance.getDBID());
        if (container != null) {
            container.addComponent(r);
            r.setContainer(container);
        }
        return r;
    }
    
    public Renderable simpleConvertToRenderable(GKInstance instance) throws Exception {
        return convertToRenderable(instance);
    }
    
    public void simpleConvertProperties(GKInstance instance, 
                                        Renderable r, 
                                        Map<GKInstance, Renderable> iToRMap) throws Exception {
    }
    
    public void convertProperties(GKInstance instance,
                                  Map iToRMap) throws Exception {
        Renderable r = (Renderable) iToRMap.get(instance);
        if (r == null)
            return;
        // This is not handled by the utility method
        r.setPosition(50, 50);
        InstanceToRenderableConverter.getPropertiesForRenderable(instance, r);
        convertProperties(instance, r, iToRMap);
    }
    
    protected abstract Renderable convertToRenderable(GKInstance instance) throws Exception;
    
    protected abstract void convertProperties(GKInstance instance,
                                              Renderable r,
                                              Map iToRMap) throws Exception;

    protected void convertPrecedingProperties(GKInstance instance, 
                                              Node r, 
                                              Map iToRMap) throws Exception {
        // FlowLine will be used to link two ProcessNodes only. If a Pathway has been converted
        // into a RenderablePathway, it should be be linked together.
        if (!(r instanceof ProcessNode))
            return;
        List precedingEvents = instance.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
        if (precedingEvents == null || precedingEvents.size() == 0)
            return;
        for (Iterator it = precedingEvents.iterator(); it.hasNext();) {
            GKInstance pre = (GKInstance) it.next();
            Renderable preR = (Renderable) iToRMap.get(pre);
            // Only link two ProcessNodes together
            if (preR instanceof ProcessNode)
                linkTwoEvents((Node)preR, r);
        }
    }

    private void linkTwoEvents(Node r1, 
                               Node r2) throws Exception {
        FlowLine fl = (FlowLine) RenderableFactory.generateRenderable(FlowLine.class, container);
        fl.addInput(r1);
        fl.addOutput(r2);
        fl.setPosition(50, 50);
        container.addComponent(fl);
    }
}
