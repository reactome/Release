/*
 * Created on Dec 8, 2008
 *
 */
package org.gk.elv;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.gk.database.AttributeEditConfig;
import org.gk.database.AttributeEditEvent;
import org.gk.database.AttributeEditManager;
import org.gk.gkCurator.authorTool.ReactionInstanceHandler;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableReaction;
import org.gk.schema.InvalidAttributeException;

/**
 * This helper class is used to handle editing in the entity-level view for reactions.
 * @author wgm
 *
 */
@SuppressWarnings("unchecked")
public class ElvReactionEditHelper extends ElvInstanceEditHandler {
    private ReactionInstanceHandler reactionHandler;
    
    public ElvReactionEditHelper() {
        reactionHandler = new ReactionInstanceHandler();
    }
    
    public void validateReactionInDiagram(RenderableReaction reaction) {
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        GKInstance instance = fileAdaptor.fetchInstance(reaction.getReactomeId());
        if (instance == null)
            return;
        try {
            validateDisplayedInputs(reaction, instance);
            validateDisplayedOutputs(reaction, instance);
            validateDisplayedCatalysts(reaction, instance);
            validateDisplayedRegulation(reaction, instance, ReactomeJavaConstants.PositiveRegulation);
            validateDisplayedRegulation(reaction, instance, ReactomeJavaConstants.NegativeRegulation);
        }
        catch(Exception e) {
            System.err.println("ElvReactionEditHelper.validte(): " + e);
            e.printStackTrace();
        }
    }
    
    private void validateDisplayedInputs(RenderableReaction reaction,
                                         GKInstance instance) throws Exception {
        Map<Renderable, Integer> stoiMap = reaction.getInputStoichiometries();
        List inputs = instance.getAttributeValuesList(ReactomeJavaConstants.input);
        Map<GKInstance, Integer> instanceStoiMap = reactionHandler.generateStoiMap(inputs);
        if (!isSameStoiMap(stoiMap, instanceStoiMap)) {
            // remove all linked nodes first
            for (Renderable r : stoiMap.keySet()) {
                reaction.remove(r, HyperEdge.INPUT);
            }
            handleInputs(instance, reaction);
        }
    }
    
    private void validateDisplayedOutputs(RenderableReaction reaction,
                                GKInstance instance) throws Exception {
        Map<Renderable, Integer> stoiMap = reaction.getOutputStoichiometries();
        List inputs = instance.getAttributeValuesList(ReactomeJavaConstants.output);
        Map<GKInstance, Integer> instanceStoiMap = reactionHandler.generateStoiMap(inputs);
        if (!isSameStoiMap(stoiMap, instanceStoiMap)) {
            for (Renderable r : stoiMap.keySet())
                reaction.remove(r, HyperEdge.OUTPUT);
            handleOutputs(instance, reaction);
        }
    }
    
    private boolean isSameStoiMap(Map<Renderable, Integer> rMap,
                                  Map<GKInstance, Integer> iMap) {
        if (rMap.size() != iMap.size())
            return false;
        // Check the actual contents
        Map<GKInstance, Integer> oldIMap = new HashMap<GKInstance, Integer>();
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        for (Renderable r : rMap.keySet()) {
            GKInstance gI = fileAdaptor.fetchInstance(r.getReactomeId());
            if (gI != null)
                oldIMap.put(gI, rMap.get(r));
        }
        if (oldIMap.size() != iMap.size())
            return false;
        // Need to compare one by one
        for (GKInstance old : oldIMap.keySet()) {
            if (!iMap.containsKey(old))
                return false;
            Integer oldStoi = oldIMap.get(old);
            Integer newStoi = iMap.get(old);
            if (oldStoi != newStoi)
                return false;
        }
        // Since oldIMap and iMap have the same size, the above comparison
        // should suffice. No need to check iMap.
        return true;
    }
    
    public void insertReactionInFull(PathwayEditor pathwayEditor,
                                     GKInstance reaction) throws Exception {
        // Check if this reaction has been inserted already
        RenderableReaction r = getDisplayedReaction(reaction);
        if (r != null) {
            // It has been inserted already
            JOptionPane.showMessageDialog(pathwayEditor,
                                          "Reaction, " + reaction + ", is in the diagram already!",
                                          "Reaction in Diagram",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Renderable converted = reactionHandler.simpleConvert(reaction);
        if (!(converted instanceof RenderableReaction))
            return; // It cannot be displayed in the ELV.
        r = (RenderableReaction) converted;
        pathwayEditor.insertEdge((HyperEdge)r, true);
        handleInputs(reaction, r);
        handleOutputs(reaction, r);
        // Get the catalyst
        List cas = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (cas != null) {
            for (Iterator it = cas.iterator(); it.hasNext();) {
                GKInstance ca = (GKInstance) it.next();
                GKInstance pe = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (pe == null)
                    continue;
                Renderable rPe = getRenderableForPE(pe, r.getPosition());
                if (rPe != null)
                    r.addHelper((Node)rPe);
            }
        }
        // Add inhibitors and activators
        List<GKInstance> positiveRegulations = getRegulation(reaction, ReactomeJavaConstants.PositiveRegulation);
        if (positiveRegulations.size() > 0) {
            for (GKInstance pR : positiveRegulations) {
                GKInstance regulator = (GKInstance) pR.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) {
                    Renderable rRegulator = getRenderableForPE(regulator, r.getPosition());
                    if (rRegulator != null)
                        r.addActivator((Node)rRegulator);
                }
            }
        }
        List<GKInstance> negativeRegulations = getRegulation(reaction, ReactomeJavaConstants.NegativeRegulation);
        if (negativeRegulations.size() > 0) {
            for (GKInstance nR : negativeRegulations) {
                GKInstance regulator = (GKInstance) nR.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) {
                    Renderable rRegulator = getRenderableForPE(regulator, r.getPosition());
                    if (rRegulator != null)
                        r.addInhibitor((Node)rRegulator);
                }
            }
        }
        // Want to do a layout
        r.layout();
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
    }

    private void handleOutputs(GKInstance reaction, RenderableReaction r)
            throws InvalidAttributeException, Exception {
        List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        if (outputs != null && outputs.size() > 0) {
            Map<GKInstance, Integer> stoiMap = reactionHandler.generateStoiMap(outputs);
            for (GKInstance i : stoiMap.keySet()) {
                Renderable rI = getRenderableForPE(i, r.getPosition());
                if (rI != null) {
                    r.addOutput((Node)rI);
                    Integer stoi = stoiMap.get(i);
                    if (stoi > 1)
                        r.setOutputStoichiometry(rI, stoi);
                }
            }
        }
    }

    private void handleInputs(GKInstance reaction, RenderableReaction r)
            throws InvalidAttributeException, Exception {
        List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        if (inputs != null && inputs.size() > 0) {
            Map<GKInstance, Integer> stoiMap = reactionHandler.generateStoiMap(inputs);
            for (GKInstance i : stoiMap.keySet()) {
                Renderable rI = getRenderableForPE(i, r.getPosition());
                if (rI != null) {
                    r.addInput((Node)rI);
                    Integer stoi = stoiMap.get(i);
                    if (stoi > 1)
                       r.setInputStoichiometry(rI, stoi); 
                }
            }
        }
    }

    private Renderable getRenderableForPE(GKInstance pe,
                                          Point defaultPos) {
        if (pe == null)
            return null;
        boolean createNew = AttributeEditConfig.getConfig().isMultipleCopyEntity(pe);
        Renderable rPe = null;
        if (createNew) {
            rPe = zoomableEditor.insertInstance(pe);
            if (defaultPos != null) {
                int x = (int) ((Math.random() - 0.5) * 100) + defaultPos.x;
                if (x < 0)
                    x = 0;
                int y = (int) ((Math.random() - 0.5) * 100) + defaultPos.y;
                if ( y < 0)
                    y = 0;
                rPe.setPosition(x, y);
            }
        }
        else
            rPe = zoomableEditor.getFreeFormObject(pe);
        if (rPe == null)
            rPe = zoomableEditor.insertInstance(pe);
        return rPe;
    }
    
    /**
     * A helper method to make sure reaction's inputs, outputs, catalysts and modifiers
     * are still correct after a detach or attach event.
     * @param edge
     */
    public void validateReactionAttributes(HyperEdge edge) throws Exception {
        if (!(edge instanceof RenderableReaction))
            return;
        RenderableReaction reaction = (RenderableReaction) edge;
        GKInstance reactionInstance = zoomableEditor.getXMLFileAdaptor().fetchInstance(edge.getReactomeId());
        if (reactionInstance == null)
            return;
        List inputs = reaction.getInputNodes();
        List<GKInstance> list = convertToInstances(inputs);
        reactionInstance.setAttributeValue(ReactomeJavaConstants.input, list);
        AttributeEditManager.getManager().attributeEdit(reactionInstance, ReactomeJavaConstants.input);
        List outputs = reaction.getOutputNodes();
        list = convertToInstances(outputs);
        reactionInstance.setAttributeValue(ReactomeJavaConstants.output, list);
        AttributeEditManager.getManager().attributeEdit(reactionInstance, ReactomeJavaConstants.output);
        validateReactionCatalysts(reaction, reactionInstance);
        validateRegulations(reaction, 
                            reactionInstance); 
    }
    
    private List<GKInstance> convertToInstances(List<Node> renderables) {
        if (renderables == null || renderables.size() == 0)
            return new ArrayList<GKInstance>();
        List<GKInstance> list = new ArrayList<GKInstance>(renderables.size());
        for (Renderable r : renderables) {
            GKInstance instance = zoomableEditor.getXMLFileAdaptor().fetchInstance(r.getReactomeId());
            if (instance != null)
                list.add(instance);
        }
        return list;
    }
    
    private List<GKInstance> getRegulation(GKInstance reaction,
                                           String clsName) throws Exception {
        List<GKInstance> list = new ArrayList<GKInstance>();
        Collection instances = zoomableEditor.getXMLFileAdaptor().fetchInstancesByClass(clsName);
        for (Iterator it = instances.iterator(); it.hasNext();) {
            GKInstance regulation = (GKInstance) it.next();
            GKInstance regulatedEntity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
            if (regulatedEntity == reaction)
                list.add(regulation);
        }
        return list;
    }
    
    private void validateRegulations(RenderableReaction reaction,
                                     GKInstance reactionInstance) throws Exception {
        List<GKInstance> positiveRegulation = getRegulation(reactionInstance, ReactomeJavaConstants.PositiveRegulation);
        List<Node> activators = reaction.getActivatorNodes();
        validateRegulations(activators, 
                            positiveRegulation,
                            reactionInstance, 
                            ReactomeJavaConstants.PositiveRegulation);
        List<GKInstance> negativeRegulation = getRegulation(reactionInstance, ReactomeJavaConstants.NegativeRegulation);
        List<Node> inhibitors = reaction.getInhibitorNodes();
        validateRegulations(inhibitors, 
                            negativeRegulation,
                            reactionInstance, 
                            ReactomeJavaConstants.NegativeRegulation);
    }
    
    private void validateRegulations(List<Node> newNodes,
                                     List<GKInstance> oldRegulations,
                                     GKInstance reaction,
                                     String clsName) throws Exception {
        if (newNodes.size() == 0 &&
            oldRegulations.size() == 0)
            return;
        List<GKInstance> newRegulators = convertToInstances(newNodes);
        // validate the old regulation
        for (GKInstance regulation : oldRegulations) {
            GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
            if (newRegulators.contains(regulator)) {
                newRegulators.remove(regulator);
            }
            else {
                regulation.setAttributeValue(ReactomeJavaConstants.regulatedEntity, null);
                AttributeEditManager.getManager().attributeEdit(regulation, 
                                                                ReactomeJavaConstants.regulatedEntity);

                // TODO: question: should this regulation be deleted?
            }
        }
        // Check if there is any new regulator left
        for (GKInstance newRegulator : newRegulators) {
            GKInstance newRegulation = zoomableEditor.getXMLFileAdaptor().createNewInstance(clsName);
            newRegulation.setAttributeValue(ReactomeJavaConstants.regulatedEntity, reaction);
            newRegulation.setAttributeValue(ReactomeJavaConstants.regulator, newRegulator);
            InstanceDisplayNameGenerator.setDisplayName(newRegulation);
        }
    }
    
    /**
     * Make sure the reaction's ca attributes are the same as in diagram.
     * @param reaction
     */
    private void validateReactionCatalysts(RenderableReaction reaction,
                                           GKInstance reactionInstance) throws Exception {
        // This is much more complicated than inputs and outputs. Keep the original
        // assigned CatalystActivity if its PhysicalEntity is still in the list.
        List<Node> catalysts = reaction.getHelperNodes();
        List oldCas = reactionInstance.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (catalysts == null || catalysts.size() == 0) {
            reactionInstance.setAttributeValue(ReactomeJavaConstants.catalystActivity,
                                               null);
            if (oldCas != null && oldCas.size() > 0)
                AttributeEditManager.getManager().attributeEdit(reactionInstance, 
                                                                ReactomeJavaConstants.catalystActivity);
            return;
        }
        List<GKInstance> pes = convertToInstances(catalysts);
        List<GKInstance> newCas = new ArrayList<GKInstance>();
        if (oldCas == null)
            oldCas = new ArrayList<GKInstance>();
        for (Iterator it = oldCas.iterator(); it.hasNext();) {
            GKInstance oldInstn = (GKInstance) it.next();
            GKInstance oldPe = (GKInstance) oldInstn.getAttributeValue(ReactomeJavaConstants.physicalEntity);
            if (pes.contains(oldPe)) {
                newCas.add(oldInstn);
                pes.remove(oldPe);
            }
        }
        // Check if there is any new pes left
        for (GKInstance newPe : pes) {
            // Create a new CatalystActivity
            GKInstance newCa = zoomableEditor.getXMLFileAdaptor().createNewInstance(ReactomeJavaConstants.CatalystActivity);
            newCa.setAttributeValue(ReactomeJavaConstants.physicalEntity, newPe);
            newCas.add(newCa);
            InstanceDisplayNameGenerator.setDisplayName(newCa);
        }
        reactionInstance.setAttributeValueNoCheck(ReactomeJavaConstants.catalystActivity, 
                                                  newCas);
        AttributeEditManager.getManager().attributeEdit(reactionInstance, 
                                                        ReactomeJavaConstants.catalystActivity);
    }
    
    public void reactionEdit(AttributeEditEvent editEvent,
                             RenderableReaction reaction) {
        GKInstance instance = editEvent.getEditingInstance();
        // Just in case
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
            return;
        String attName = editEvent.getAttributeName();
        if (attName.equals(ReactomeJavaConstants._doRelease)) {
            updateDoNotReleaseEventVisible(instance);
        }
        else {
            validateDisplayedLinkedNodes(reaction,
                                         instance,
                                         attName);
        }
    }
    
    public void regulationEdit(AttributeEditEvent editEvent) {
        GKInstance regulation = editEvent.getEditingInstance();
        int type = editEvent.getEditingType();
        try {
            if (editEvent.getAttributeName().equals(ReactomeJavaConstants.regulatedEntity)) {
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                // Check if it is added or removed
                if (type == AttributeEditEvent.ADDING) {
                    List list = editEvent.getAddedInstances();
                    // Get the first one
                    GKInstance reaction = (GKInstance) list.get(0);
                    validateDisplayedRegulation(reaction, 
                                                regulation, 
                                                regulator,
                                                type);
                }
                else if (type == AttributeEditEvent.REMOVING) {
                    List list = editEvent.getRemovedInstances();
                    GKInstance reaction = (GKInstance) list.get(0);
                    validateDisplayedRegulation(reaction,
                                                regulation, 
                                                regulator,
                                                type);
                }
            }
            else if (editEvent.getAttributeName().equals(ReactomeJavaConstants.regulator)) {
                GKInstance reaction = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
                if (type == AttributeEditEvent.ADDING) {
                    GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                    validateDisplayedRegulation(reaction, regulation, regulator, type);
                }
                else if (type == AttributeEditEvent.REMOVING) {
                    List list = editEvent.getRemovedInstances();
                    GKInstance regulator = (GKInstance) list.get(0);
                    validateDisplayedRegulation(reaction, regulation, regulator, type);
                }
            }
        }
        catch(Exception e) {
            System.err.println("ElvReactionEditHelper.regulationEdit(): " + e);
            e.printStackTrace();
        }
    }
    
    private void validateDisplayedRegulation(RenderableReaction reaction,
                                             GKInstance reactionInstance,
                                             String clsName) throws Exception {
        List<GKInstance> regulations = getRegulation(reactionInstance, clsName);
        Set<GKInstance> regulators = new HashSet<GKInstance>();
        for (GKInstance regulation : regulations) {
            GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
            if (regulator != null && regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                regulators.add(regulator);
        }
        List<Node> linkedNodes = null;
        if (clsName.equals(ReactomeJavaConstants.PositiveRegulation))
            linkedNodes = reaction.getActivatorNodes();
        else if (clsName.equals(ReactomeJavaConstants.NegativeRegulation))
            linkedNodes = reaction.getInhibitorNodes();
        if (linkedNodes == null) 
            linkedNodes = new ArrayList<Node>();
        if (regulators.size() == 0 && linkedNodes.size() == 0)
            return;
        // Make sure all positive regulator is there
        List<Node> deleted = new ArrayList<Node>();
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        for (Node node : linkedNodes) {
            GKInstance instance = fileAdaptor.fetchInstance(node.getReactomeId());
            if (!regulators.remove(instance)) 
                deleted.add(node);
        }
        // Delete these extra
        for (Node node : deleted) {
            if (clsName.equals(ReactomeJavaConstants.PositiveRegulation))
                reaction.remove(node, HyperEdge.ACTIVATOR);
            else if (clsName.equals(ReactomeJavaConstants.NegativeRegulation))
                reaction.remove(node, HyperEdge.INHIBITOR);
        }
        // These should be added
        for (GKInstance regulator : regulators) {
            Node node = (Node) getRenderableForPE(regulator, reaction.getPosition());
            if (node != null) {
                if (clsName.equals(ReactomeJavaConstants.PositiveRegulation))
                    reaction.addActivator(node);
                else if (clsName.equals(ReactomeJavaConstants.NegativeRegulation))
                    reaction.addInhibitor(node);
            }
        }
    }
    
    protected void validateDisplayedRegulation(GKInstance reaction,
                                               GKInstance regulation,
                                               GKInstance regulator,
                                               int type) {
        if (reaction == null)
            return;
        RenderableReaction rReaction = getDisplayedReaction(reaction);
        if (rReaction == null)
            return;
        boolean needRepaint = false;
        if (type == AttributeEditEvent.ADDING) {
            Node node = (Node) getRenderableForPE(regulator, rReaction.getPosition());
            if (node != null) {
                if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation)) {
                    rReaction.addActivator(node);
                    needRepaint = true;
                }
                else if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation)) {
                    rReaction.addInhibitor(node);
                    needRepaint = true;
                }
            }
        }
        else if (type == AttributeEditEvent.REMOVING) {
            List<Node> linkedNodes = null;
            boolean isInhibitors = false;
            if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation)) {
                linkedNodes = rReaction.getActivatorNodes();
                isInhibitors = false;
            }
            else if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation)) {
                linkedNodes = rReaction.getInhibitorNodes();
                isInhibitors = true;
            }
            if (linkedNodes != null && linkedNodes.size() > 0) {
                for (Node node : linkedNodes) {
                    if (node.getReactomeId().equals(regulator.getDBID())) {
                        if (isInhibitors)
                            rReaction.remove(node, HyperEdge.INHIBITOR);
                        else
                            rReaction.remove(node, HyperEdge.ACTIVATOR);
                        needRepaint = true;
                        break;
                    }
                }
            }
        }
        if (needRepaint) {
            PathwayEditor editor = zoomableEditor.getPathwayEditor();
            editor.repaint(editor.getVisibleRect());
        }
    }
    
    private RenderableReaction getDisplayedReaction(GKInstance reaction) {
        if (reaction == null)
            return null;
        Long id = reaction.getDBID();
        for (Iterator it = zoomableEditor.getPathwayEditor().getDisplayedObjects().iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof RenderableReaction) {
                RenderableReaction rxt = (RenderableReaction) obj;
                if (rxt.getReactomeId().equals(id))
                    return rxt;
            }
        }
        return null;
    }
    
    public void catalystActivityEdit(GKInstance ca) {
        try {
            // Need to find reaction this ca is worked before
            Map map = zoomableEditor.getXMLFileAdaptor().getReferrersMap(ca);
            List reactions = (List) map.get(ReactomeJavaConstants.catalystActivity);
            if (reactions != null && reactions.size() > 0) {
                Map<RenderableReaction, GKInstance> displayedRxt2Instance = new HashMap<RenderableReaction, GKInstance>();
                for (Iterator it = reactions.iterator(); it.hasNext();) {
                    GKInstance reaction = (GKInstance) it.next();
                    RenderableReaction rReaction = getDisplayedReaction(reaction);
                    if (rReaction == null)
                        continue;
                    displayedRxt2Instance.put(rReaction, reaction);
                }
                // In this case, the catalyst should be validated first.
                if (displayedRxt2Instance.size() > 0) {
                    GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                    Node node = null;
                    if (catalyst != null)
                        node = (Node) getRenderableForPE(catalyst, null);        
                    for (RenderableReaction rReaction : displayedRxt2Instance.keySet()) {
                        GKInstance reaction = displayedRxt2Instance.get(rReaction);
                        // If this ca has removed its original PE, there is no way to know which PE
                        // it is. So do a wholesale validation.
                        validateDisplayedCatalysts(rReaction, 
                                                   reaction);
                    }
                }
            }
        }
        catch(Exception e) {
            System.err.println("ElvReactionEditHelper.catalystActivityEdit(): " + e);
            e.printStackTrace();
        }
    }
    
    private void validateDisplayedCatalysts(RenderableReaction reaction,
                                            GKInstance instance) throws Exception {
        List list = instance.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        // Use set in case there are two different cas
        Set<GKInstance> pes = new HashSet<GKInstance>();
        if (list != null) {
            for (Iterator it = list.iterator(); it.hasNext();) {
                GKInstance ca = (GKInstance) it.next();
                GKInstance pe = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (pe != null)
                    pes.add(pe);
            }
        }
        // Need to get the actual PEs from CAs
        List<Node> nodes = reaction.getHelperNodes();
        for (Iterator<Node> it = nodes.iterator(); it.hasNext();) {
            Node node = it.next();
            Long reactomeId = node.getReactomeId();
            GKInstance inst = zoomableEditor.getXMLFileAdaptor().fetchInstance(reactomeId);
            if(pes.remove(inst)) {
                it.remove();
            }
        }
        // Just in case if nothing changed to avoid an infinity loop
        int preSize = nodes.size() + 1;
        while (nodes.size() > 0 &&
                nodes.size() < preSize) {
            List<Node> helperNodes = reaction.getHelperNodes();
            preSize = nodes.size();
            for (int i = 0; i < helperNodes.size(); i++) {
                Node helper = helperNodes.get(i);
                if (nodes.contains(helper)) {
                    reaction.removeHelper(i);
                    nodes.remove(helper);
                    break;
                }
            }
        }
        for (GKInstance inst : pes) {
            Node node = (Node) getRenderableForPE(inst, reaction.getPosition());
            reaction.addHelper(node);
        }
        PathwayEditor pathwayEditor = zoomableEditor.getPathwayEditor();
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
    }

    private void validateDisplayedLinkedNodes(RenderableReaction reaction,
                                              GKInstance instance,
                                              String attName) {
        if (attName.equals(ReactomeJavaConstants.catalystActivity)) {
            try {
                validateDisplayedCatalysts(reaction, instance);
            }
            catch(Exception e) {
                System.err.println("ElvReactionEditHelper.validateDisplayedLinkedNodes(): " + e);
                e.printStackTrace();
            }
            return;
        }
        if (!attName.equals(ReactomeJavaConstants.input) &&
            !attName.equals(ReactomeJavaConstants.output))
            return; // Nothing will be done in this method
        try {
            List<Node> nodes = null;
            if (attName.equals(ReactomeJavaConstants.input)) {
                nodes = reaction.getInputNodes();
                for (Node node : nodes)
                    reaction.remove(node, HyperEdge.INPUT);
                handleInputs(instance, reaction);
            }
            else if (attName.equals(ReactomeJavaConstants.output)) {
                nodes = reaction.getOutputNodes();
                for (Node node : nodes)
                    reaction.remove(node, HyperEdge.OUTPUT);
                handleOutputs(instance, reaction);
            }
            PathwayEditor pathwayEditor = zoomableEditor.getPathwayEditor();
            pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        }
        catch(Exception e) {
            System.err.println("ElvReactionEditHelper.validateDisplayedLinkedNodes(): " + e);
            e.printStackTrace();
        }
    }
    
}
