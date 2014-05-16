/*
 * Created on Sep 23, 2006
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.database.AttributeEditConfig;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableReaction;
import org.gk.render.Shortcut;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;

public class ReactionHandler extends PathwayHandler {
    
    protected GKInstance convertChanged(Renderable r) throws Exception {
        Long dbId = (Long) getDbId(r);
        if (dbId == null)
            return null; // Just in case. Should not occur.
        GKInstance local = createNewWithID(ReactomeJavaConstants.Reaction,
                                           dbId);
        return local;
    }
    
    public GKInstance createNew(Renderable r) throws Exception {
        GKInstance reaction = fileAdaptor.createNewInstance(ReactomeJavaConstants.Reaction);
        return reaction;
    }
    
    private List getConverted(List renderables,
                              Map rToIMap) {
        if (renderables == null || renderables.size() == 0)
            return null;
        List list = new ArrayList(renderables.size());
        for (Iterator it = renderables.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Shortcut)
                r = ((Shortcut)r).getTarget();
            GKInstance converted = (GKInstance) rToIMap.get(r);
            list.add(converted);
        }
        return list;
    }
    
    private void extractOutputs(RenderableReaction reaction,
                                GKInstance iReaction,
                                Map rToIMap) throws Exception {
        List outputs = reaction.getOutputNodes();
        if (outputs == null || outputs.size() == 0) {
            iReaction.setAttributeValue(ReactomeJavaConstants.output, null);
            return;
        }
        List list = new ArrayList();
        for (Iterator it = outputs.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Shortcut)
                r = ((Shortcut)r).getTarget();
            int stoi = reaction.getInputStoichiometry(r);
            GKInstance output = (GKInstance) rToIMap.get(r);
            if (stoi == 0 || stoi == 1)
                list.add(output);
            else {
                for (int i = 0; i < stoi; i++)
                    list.add(output);
            }
        }
        iReaction.setAttributeValue(ReactomeJavaConstants.output, list);
    }
    
    private void extractInputs(RenderableReaction reaction,
                               GKInstance iReaction,
                               Map rToIMap) throws Exception {
        List inputs = reaction.getInputNodes();
        if (inputs == null || inputs.size() == 0) {
            iReaction.setAttributeValue(ReactomeJavaConstants.input, null);
            return;
        }
        List list = new ArrayList();
        for (Iterator it = inputs.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Shortcut)
                r = ((Shortcut)r).getTarget();
            int stoi = reaction.getInputStoichiometry(r);
            GKInstance input = (GKInstance) rToIMap.get(r);
            if (stoi == 0 || stoi == 1)
                list.add(input);
            else {
                for (int i = 0; i < stoi; i++)
                    list.add(input);
            }
        }
        iReaction.setAttributeValue(ReactomeJavaConstants.input, list);
    }

    protected void extractComponents(GKInstance iReaction,
                                     Renderable rReaction,
                                     Map rToIMap) throws Exception {
        RenderableReaction reaction = null;
        if (rReaction instanceof ReactionNode)
            reaction = ((ReactionNode)rReaction).getReaction();
        else
            reaction = (RenderableReaction) rReaction;
        // inputs
        extractInputs(reaction, iReaction, rToIMap);
        // outputs
        extractOutputs(reaction, iReaction, rToIMap);
        // helpers
        List helpers = reaction.getHelperNodes();
        extractCatalysts(helpers, iReaction, rToIMap);
        // Inhibitors
        List inhibitors = reaction.getInhibitorNodes();
        extractRegulations(inhibitors, 
                           ReactomeJavaConstants.NegativeRegulation,
                           iReaction,
                           rToIMap);
        // Activators
        List activators = reaction.getActivatorNodes();
        extractRegulations(activators, 
                           ReactomeJavaConstants.PositiveRegulation,
                           iReaction, 
                           rToIMap);
    }
    
    private void extractRegulations(List regulators, 
                                    String type,
                                    GKInstance iReaction, 
                                    Map rToIMap) throws Exception {
        if (regulators == null || regulators.size() == 0) {
            return;
        }
        // inhibitors or activators
        List regulatorInstances = getConverted(regulators, rToIMap);
        for (Iterator it = regulatorInstances.iterator(); it.hasNext();) {
            GKInstance pe = (GKInstance) it.next();
            GKInstance regulation = fileAdaptor.createNewInstance(type);
            regulation.setAttributeValue(ReactomeJavaConstants.regulator, pe);
            regulation.setAttributeValue(ReactomeJavaConstants.regulatedEntity,
                                         iReaction);
            InstanceDisplayNameGenerator.setDisplayName(regulation);
        }   
    }

    private void extractCatalysts(List helpers,
                                  GKInstance iReaction, 
                                  Map rToIMap) throws InvalidAttributeException, InvalidAttributeValueException {
        if (helpers == null || helpers.size() == 0) {
            iReaction.setAttributeValue(ReactomeJavaConstants.catalystActivity, null);
            return;
        }
        List catalysts = getConverted(helpers, rToIMap);
        List caList = new ArrayList(helpers.size());
        for (Iterator it = catalysts.iterator(); it.hasNext();) {
            GKInstance catalyst = (GKInstance) it.next();
            GKInstance ca = fileAdaptor.createNewInstance(ReactomeJavaConstants.CatalystActivity);
            ca.setAttributeValue(ReactomeJavaConstants.physicalEntity, 
                                 catalyst);
            InstanceDisplayNameGenerator.setDisplayName(ca);
            caList.add(ca);
        }
        // In the current schema, reaction can have only one catalystActivity
        if (caList.size() == 0)
            return;
        GKInstance ca = (GKInstance) caList.get(0);
        iReaction.setAttributeValue(ReactomeJavaConstants.catalystActivity,
                                    ca);
    }
    
    protected void convertPropertiesForNew(GKInstance converted, 
                                           Renderable r,
                                           Map rToIMap) throws Exception {
        super.convertPropertiesForNew(converted, r, rToIMap);
        // Only for Reaction
        handleIsReversible(converted, r);
    }
    
    private void handleIsReversible(GKInstance iReaction,
                                    Renderable rReaction) throws Exception {
        String isReversible = (String) rReaction.getAttributeValue(RenderablePropertyNames.IS_REVERSIBLE);
        if (isReversible == null || isReversible.equals("false"))
            return;
        // Need to create a new Reaction
        GKInstance reversibleReaction = createReversibleReaction(iReaction);
        // This is a inverse attribute
        iReaction.setAttributeValue(ReactomeJavaConstants.reverseReaction, reversibleReaction);
        reversibleReaction.setAttributeValue(ReactomeJavaConstants.reverseReaction, iReaction);
    }
    
    private GKInstance createReversibleReaction(GKInstance reaction) throws Exception {
        // Copy all attribute values except the uneditable values
        java.util.List uneditableAttNames = AttributeEditConfig.getConfig().getUneditableAttNames();
        GKSchemaClass schemaCls = (GKSchemaClass) reaction.getSchemClass();
        GKInstance reversibleReaction = fileAdaptor.createNewInstance(ReactomeJavaConstants.Reaction);
        for (Iterator it = schemaCls.getAttributes().iterator(); it.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute) it.next();
            String attName = att.getName();
            if (uneditableAttNames.contains(attName))
                continue;
            java.util.List values = reaction.getAttributeValuesList(attName);
            if (attName.equals(ReactomeJavaConstants.input)) {
                reversibleReaction.setAttributeValueNoCheck(ReactomeJavaConstants.output, 
                                                            values == null ? null : new ArrayList(values));
            }
            else if (attName.equals(ReactomeJavaConstants.output)) {
                reversibleReaction.setAttributeValueNoCheck(ReactomeJavaConstants.input, 
                                                            values == null ? null : new ArrayList(values));
            }
            else if (attName.equals(ReactomeJavaConstants.name)) {
                if (values == null || values.size() == 0)
                    reversibleReaction.setAttributeValueNoCheck(attName, null);
                else {
                    java.util.List names = new ArrayList(values.size());
                    for (Iterator it1 = values.iterator(); it1.hasNext();) {
                        String name = (String) it1.next();
                        names.add(name + " [reversible]");
                    }
                    reversibleReaction.setAttributeValueNoCheck(attName, names);
                }
            }
            else if (values != null && values.size() > 0){
                reversibleReaction.setAttributeValueNoCheck(attName, new ArrayList(values));
            }
        }
        InstanceDisplayNameGenerator.setDisplayName(reversibleReaction);
        return reversibleReaction;
    }
    
}
