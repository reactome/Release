/*
 * Created on Jan 18, 2007
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Node;
import org.gk.render.ReactionType;
import org.gk.render.Renderable;
import org.gk.render.RenderableFactory;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.schema.SchemaClass;

public class ReactionInstanceHandler extends InstanceHandler {
    
    protected Renderable convertToRenderable(GKInstance instance) throws Exception {
        // An empty reaction should be converted as Process
        Set participants = InstanceUtilities.getReactionParticipants(instance);
        Renderable r = null;
        if (participants.size() == 0) {
            r = RenderableFactory.generateRenderable(RenderablePathway.class,
                                                     container);
        }
        else {
           r = RenderableFactory.generateRenderable(RenderableReaction.class, 
                                                    container);
           // Use types based on instance class
           RenderableReaction reaction = (RenderableReaction) r;
           SchemaClass cls = instance.getSchemClass();
           if (cls.isa(ReactomeJavaConstants.BlackBoxEvent))
               reaction.setReactionType(ReactionType.OMITTED_PROCESS);
           else if (cls.isa(ReactomeJavaConstants.Polymerisation))
               reaction.setReactionType(ReactionType.ASSOCIATION);
           else if (cls.isa(ReactomeJavaConstants.Depolymerisation))
               reaction.setReactionType(ReactionType.DISSOCIATION);
        }
        return r;
    }
    
    public Map<GKInstance, Integer> generateStoiMap(List instances) {
        Map<GKInstance, Integer> instanceToStoi = new HashMap<GKInstance, Integer>();
        if (instances == null || instances.size() == 0)
            return instanceToStoi;
        for (Iterator it =instances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            Integer stoi = instanceToStoi.get(instance);
            if (stoi == null)
                instanceToStoi.put(instance, 1);
            else {
                instanceToStoi.put(instance, ++stoi);
            }
        }
        return instanceToStoi;
    }
    
    public void convertProperties(GKInstance instance, 
                                  Renderable r,
                                  Map iToRMap) throws Exception {
        if (r instanceof RenderablePathway) {
            // Check if preceding/flowling events needed
            convertPrecedingProperties(instance, (Node)r, iToRMap);
            return; // It may be converted to RenderablePathway
        }
        RenderableReaction reaction = (RenderableReaction) r;
        // Create links for reaction participants
        List inputs = instance.getAttributeValuesList(ReactomeJavaConstants.input);
        if (inputs != null) {
            // Need to consider stoichiometries
            Map<GKInstance, Integer> inputToStoi = generateStoiMap(inputs);
            for (GKInstance input : inputToStoi.keySet()) {
                Integer stoi = inputToStoi.get(input);
                Node inputNode = (Node) iToRMap.get(input);
                reaction.addInput(inputNode);
                if (stoi > 1)
                    reaction.setInputStoichiometry(inputNode, stoi);
            }
        }
        List outputs = instance.getAttributeValuesList(ReactomeJavaConstants.output);
        if (outputs != null) {
            Map<GKInstance, Integer> outputToStoi = generateStoiMap(outputs);
            for (GKInstance output : outputToStoi.keySet()) {
                Integer stoi = outputToStoi.get(output);
                Node outputNode = (Node) iToRMap.get(output);
                reaction.addOutput(outputNode);
                if (stoi > 1)
                    reaction.setOutputStoichiometry(outputNode, stoi);
            }
        }
        List cas = instance.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (cas != null) {
            for (Iterator it = cas.iterator(); it.hasNext();) {
                GKInstance ca = (GKInstance) it.next();
                GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (catalyst == null)
                    continue;
                Node catalystNode = (Node) iToRMap.get(catalyst);
                reaction.addHelper(catalystNode);
            }
        }
        Collection regulations = instance.getReferers(ReactomeJavaConstants.regulatedEntity);
        if (regulations != null) {
            for (Iterator it = regulations.iterator(); it.hasNext();) {
                GKInstance regulation = (GKInstance) it.next();
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator == null)
                    continue;
                Node node = (Node) iToRMap.get(regulator);
                if (node == null) // This may be null if regulator is an Event
                    continue;
                if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation))
                    reaction.addInhibitor(node);
                else
                    reaction.addActivator(node);
            }
        }
    }

    @Override
    public void simpleConvertProperties(GKInstance instance, Renderable r,
                                        Map<GKInstance, Renderable> toRMap)
            throws Exception {
        convertProperties(instance, r, toRMap);
    }
    
    
    
}
