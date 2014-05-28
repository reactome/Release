/*
 * Created on Jan 19, 2007
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.InteractionType;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableInteraction;
import org.gk.render.RenderablePathway;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;

public class InteractionHandler extends PathwayHandler {
    
    protected GKInstance convertChanged(Renderable r) throws Exception {
        Long dbId = (Long) getDbId(r);
        if (dbId == null)
            return null; // Just in case. Should not occur.
        GKInstance local = createNewWithID(ReactomeJavaConstants.Reaction, dbId);
        return local;
    }
    
    protected void convertPropertiesForNew(GKInstance iInteraction, 
                                           Renderable interaction, 
                                           Map rToIMap) throws Exception {
        if (iInteraction.getSchemClass().isa(ReactomeJavaConstants.Regulation)) {
            extractComponents(iInteraction, interaction, rToIMap);
            extractNames(interaction, iInteraction);
            extractReference(interaction, iInteraction);
            extractSummation(interaction, iInteraction);
        }
        else 
            super.convertPropertiesForNew(iInteraction, interaction, rToIMap);
    }

    /**
     * Interaction is converted to one Reaction and one Regulation
     */
    public GKInstance createNew(Renderable r) throws Exception {
        RenderableInteraction interaction = (RenderableInteraction) r;
        // The target might be a Pathway. In this case, don't do anything
        GKInstance rtn = null;
        Node output = interaction.getOutputNode(0);
        if (output instanceof RenderablePathway) {
            // Convert to regulation directly
            if (interaction.getInteractionType() == InteractionType.INHIBIT)
                rtn = fileAdaptor.createNewInstance(ReactomeJavaConstants.NegativeRegulation);
            else if (interaction.getInteractionType() == InteractionType.ACTIVATE)
                rtn = fileAdaptor.createNewInstance(ReactomeJavaConstants.PositiveRegulation);
        }
        if (interaction.getInteractionType() == InteractionType.ENCODE) {
            // This will occur between gene and protein only
            rtn = fileAdaptor.createNewInstance(ReactomeJavaConstants.BlackBoxEvent);
        }
        else {
            rtn = fileAdaptor.createNewInstance(ReactomeJavaConstants.Reaction);
        }
        return rtn;
    }

    protected void extractComponents(GKInstance instance, 
                                     Renderable renderable, 
                                     Map rToIMap) throws Exception {
        if (!(renderable instanceof RenderableInteraction))
            return; // Handle Interactions only
        RenderableInteraction interaction = (RenderableInteraction) renderable;
        InteractionType type = interaction.getInteractionType();
        if (type == InteractionType.INTERACT ||
            type == InteractionType.UNKNOWN) {
            convertBindInteraction(interaction, instance, rToIMap);
        }
        else if (type == InteractionType.ENCODE)
            convertEncodeInteraction(interaction, instance, rToIMap);
        // Need to use Regulation for Interaction
        else if (type == InteractionType.INHIBIT ||
                 type == InteractionType.REPRESS)
            convertInhibitInteraction(interaction, instance, rToIMap);
        else if (type == InteractionType.ACTIVATE ||
                 type == InteractionType.ENHANCE)
            convertActivateInteraction(interaction, instance, rToIMap);
    }    
    
    private void convertActivateInteraction(RenderableInteraction interaction,
                                            GKInstance instance,
                                            Map rToIMap) throws Exception {
        if (instance.getSchemClass().isa(ReactomeJavaConstants.Reaction)) {
            GKInstance regulation = fileAdaptor.createNewInstance(ReactomeJavaConstants.PositiveRegulation);
            convertInteraction(regulation,
                               instance,
                               interaction,
                               rToIMap);
        }
        // If output is a Pathway, this Interaction has been converted to Regulation already.
        else if (instance.getSchemClass().isa(ReactomeJavaConstants.Regulation)) {
            Renderable output = interaction.getOutputNode(0);
            GKInstance pathway = (GKInstance) rToIMap.get(output);
            fillUpRegulation(instance, pathway, interaction, rToIMap);
        }
    }
    
    private void convertInhibitInteraction(RenderableInteraction interaction,
                                           GKInstance instance,
                                           Map rToIMap) throws Exception {
        // Create a regulation
        GKInstance regulation = fileAdaptor.createNewInstance(ReactomeJavaConstants.NegativeRegulation);
        convertInteraction(regulation, 
                           instance, 
                           interaction, 
                           rToIMap);
    }
    
    private void convertInteraction(GKInstance regulation,
                                    GKInstance reaction,
                                    RenderableInteraction interaction,
                                    Map rToIMap) throws Exception {
        fillUpRegulation(regulation, reaction, interaction, rToIMap);
        // Output is added as input in the converted reaction
        Node output = interaction.getOutputNode(0);
        GKInstance outputInstance = (GKInstance) rToIMap.get(output);
        reaction.addAttributeValue(ReactomeJavaConstants.input,
                                   outputInstance);
    }

    private void fillUpRegulation(GKInstance regulation, 
                                  GKInstance event, 
                                  RenderableInteraction interaction, 
                                  Map rToIMap) throws InvalidAttributeException, InvalidAttributeValueException {
        Node input = interaction.getInputNode(0);
        GKInstance inputInstance = (GKInstance) rToIMap.get(input);
        regulation.setAttributeValue(ReactomeJavaConstants.regulator,
                                     inputInstance);
        regulation.setAttributeValue(ReactomeJavaConstants.regulatedEntity, 
                                     event);  
        InstanceDisplayNameGenerator.setDisplayName(regulation);
    }
    
    private void convertBindInteraction(RenderableInteraction interaction,
                                        GKInstance instance,
                                        Map rToIMap) throws Exception {
        // Just put input and out as inputs for the converted reaction
        Node input = interaction.getInputNode(0);
        Node output = interaction.getOutputNode(0);
        GKInstance iInstance = (GKInstance) rToIMap.get(input);
        GKInstance oInstance = (GKInstance) rToIMap.get(output);
        if (iInstance != null)
            instance.addAttributeValue(ReactomeJavaConstants.input, iInstance);
        if (oInstance != null)
            instance.addAttributeValue(ReactomeJavaConstants.input, oInstance);
        // Add a note to the defintion
        instance.addAttributeValue(ReactomeJavaConstants.definition,
                                   "Converted from interaction");
    }
    
    private void convertEncodeInteraction(RenderableInteraction interaction,
                                          GKInstance instance,
                                          Map rToIMap) throws Exception {
        // Just put input and out as inputs for the converted reaction
        Node input = interaction.getInputNode(0);
        Node output = interaction.getOutputNode(0);
        GKInstance iInstance = (GKInstance) rToIMap.get(input);
        GKInstance oInstance = (GKInstance) rToIMap.get(output);
        if (iInstance != null)
            instance.addAttributeValue(ReactomeJavaConstants.input, iInstance);
        if (oInstance != null)
            instance.addAttributeValue(ReactomeJavaConstants.output, oInstance);
        // Add a note to the defintion
        instance.addAttributeValue(ReactomeJavaConstants.definition,
                                   "Converted from interaction with type of encode");
    }
}
