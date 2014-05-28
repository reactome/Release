/*
 * Created on Aug 3, 2010
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.InteractionType;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableFactory;
import org.gk.render.RenderableInteraction;

public class InteractionInstanceHandler extends InstanceHandler {
    
    @Override
    protected void convertProperties(GKInstance instance,
                                     Renderable r,
                                     Map iToRMap) throws Exception {
        List<?> interactors = instance.getAttributeValuesList(ReactomeJavaConstants.interactor);
        if (interactors == null || interactors.size() == 0)
            return;
        RenderableInteraction interaction = (RenderableInteraction) r;
        // Can only add two interactors
        GKInstance interactor1 = (GKInstance) interactors.get(0);
        Renderable r1 = (Renderable) iToRMap.get(interactor1);
        if (r1 instanceof Node) {
            interaction.addInput((Node)r1);
        }
        if (interactors.size() > 1) {
            GKInstance interactor2 = (GKInstance) interactors.get(1);
            Renderable r2 = (Renderable) iToRMap.get(interactor2);
            if (r2 instanceof Node)
                interaction.addOutput((Node)r2);
        }
    }
    
    @Override
    public void simpleConvertProperties(GKInstance instance, 
                                        Renderable r,
                                        Map<GKInstance, Renderable> iToRMap)
            throws Exception {
        convertProperties(instance, r, iToRMap);
    }



    @Override
    protected Renderable convertToRenderable(GKInstance instance)
            throws Exception {
        Renderable r = RenderableFactory.generateRenderable(RenderableInteraction.class, 
                                                            container);
        RenderableInteraction interaction = (RenderableInteraction) r;
        String type = (String) instance.getAttributeValue(ReactomeJavaConstants.interactionType);
        InteractionType interactionType = null;
        if (type != null) {
            // Do a little format
            type = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
            interactionType = InteractionType.getType(type);
        }
        if (interactionType == null)
            interactionType = InteractionType.INTERACT;
        interaction.setInteractionType(interactionType);
        return r;
    }
    
}
