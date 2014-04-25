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
import org.gk.render.InstanceToRenderableConverter;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableFactory;

public class ComplexInstanceHandler extends InstanceHandler {
    
    protected Renderable convertToRenderable(GKInstance instance) {
        Renderable r = RenderableFactory.generateRenderable(RenderableComplex.class, 
                                                            container);
        return r;
    }

    public void convertProperties(GKInstance instance,
                                  Renderable r,
                                  Map iToRMap) throws Exception {
        RenderableComplex complex = (RenderableComplex) r;
        // Extract complex components
        extractComponents(instance, r, iToRMap);
    }
    
    private void extractComponents(GKInstance instance,
                                   Renderable r,
                                   Map iToRMap) throws Exception {
        List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            GKInstance comp = (GKInstance) it.next();
            Renderable rComp = (Renderable) iToRMap.get(comp);
            if (rComp != null) {
                // Create a shortcut
                rComp = (Renderable) rComp.generateShortcut();
                container.addComponent(rComp);
            }
            else {
                rComp = generateComponent(comp);
                container.addComponent(rComp);
                iToRMap.put(comp, rComp);
            }
            // Do an out-loop (?) so that the internal data structure in complex
            // should be right.
            if (rComp instanceof RenderableComplex) {
                extractComponents(comp, rComp, iToRMap);
            }
            // Randomly positions
            rComp.setPosition(60, 60);
            r.addComponent(rComp);
            rComp.setContainer(r);
        }
        // R must be a Complex
        RenderableComplex complex = (RenderableComplex) r;
        complex.hideComponents(true);
        // No need to layout since all components have been hidden
        //complex.layout();
    }
    
    private Renderable generateComponent(GKInstance instance) throws Exception {
        InstanceHandler handler = InstanceHandlerFactory.getFactory().getHandler(instance);
        Renderable comp = handler.convertToRenderable(instance);
        comp.setDisplayName(instance.getDisplayName());
        InstanceToRenderableConverter.getPropertiesForRenderable(instance, comp);
        return comp;
    }
    
    /**
     * Convert the component of the complex
     */
    @Override
    public void simpleConvertProperties(GKInstance instance, 
                                        Renderable r, 
                                        Map<GKInstance, Renderable> iToRMap) throws Exception {
//        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
//            return;
//        List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
//        if (components == null || components.size() == 0)
//            return;
//        for (Iterator it = components.iterator(); it.hasNext();) {
//            GKInstance comp = (GKInstance) it.next();
//            InstanceHandler handler = InstanceHandlerFactory.getFactory().getHandler(comp);
//            Renderable rComp = handler.simpleConvert(comp);
//            r.addComponent(rComp);
//            simpleConvertProperties(comp, rComp);
//        }
        // In case it is displayed as multimer
        if (r instanceof RenderableComplex) {
            RenderableComplex complex = (RenderableComplex) r;
            complex.hideComponents(true);
        }
    }
    
}
