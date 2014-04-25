/*
 * Created on Mar 30, 2010
 *
 */
package org.gk.pathwaylayout;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.DiagramGKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.render.*;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.junit.Test;

/**
 * This class is used to generate diagrams for predicted pathways based on human pathway
 * diagrams created manually. This class is different from another class PredictedPathwayDiagramGenerator,
 * which use jgraph data structure and should be deprecated.
 * @author wgm
 *
 */
public class PredictedPathwayDiagramGeneratorFromDB extends DiagramGeneratorFromDB {
    private static final Logger logger = Logger.getLogger(PredictedPathwayDiagramGeneratorFromDB.class);
    
    private GKInstance defaultIE;
    private Long defaultPersonId;
    
    public PredictedPathwayDiagramGeneratorFromDB() {
    }
    
    public void setDefaultPersonId(Long dbId) {
        this.defaultPersonId = dbId;
    }
    
    private GKInstance getDefaultIE(MySQLAdaptor dba) throws Exception {
        if (defaultIE != null)
            return defaultIE;
        // Need to create this defaultIE
        defaultIE = new GKInstance();
        defaultIE.setSchemaClass(dba.getSchema().getClassByName(ReactomeJavaConstants.InstanceEdit));
        defaultIE.setDbAdaptor(dba);
        GKInstance defaultPerson = dba.fetchInstance(defaultPersonId);
        defaultIE.setAttributeValue(ReactomeJavaConstants.author, defaultPerson);
        defaultIE.setAttributeValue(ReactomeJavaConstants.note, 
                                    "Predicted pathway diagrams from human pathways");
        defaultIE.setAttributeValue(ReactomeJavaConstants.dateTime,
                                    GKApplicationUtilities.getDateTime()); // Use one dateTime only
        InstanceDisplayNameGenerator.setDisplayName(defaultIE);
        return defaultIE;
    }
    
    /**
     * Generate a diagram for the passed predicted target pathway from the source diagram for source pathway.
     * @param targetPathway
     * @param sourcePathway
     * @param sourceDiagram
     * @param dba
     * @return
     * @throws Exception
     */
    public GKInstance generatePredictedDiagram(GKInstance targetPathway,
                                               GKInstance sourcePathway,
                                               GKInstance sourceDiagram) throws Exception {
        // Quick check to make sure default person is not null
        if (getDefaultIE(dba) == null)
            throw new IllegalStateException("PredictedPathwayDiagramGeneratorFromDB(): cannot create a default IE!");
        GKInstance targetDiagram = getTargetDiagramInstance(targetPathway, dba);
        RenderablePathway srcRDiagram = new DiagramGKBReader().openDiagram(sourceDiagram);
        RenderablePathway targetRDigram = generatePredictedDiagram(srcRDiagram,
                                                                   targetPathway,
                                                                   dba);
        DiagramGKBWriter writer = new DiagramGKBWriter();
        String xml = writer.generateXMLString(new Project(targetRDigram));
        targetDiagram.setAttributeValue(ReactomeJavaConstants.storedATXML, 
                                        xml);
        // Use width, height from the source
        targetDiagram.setAttributeValue(ReactomeJavaConstants.width, 
                                        sourceDiagram.getAttributeValue(ReactomeJavaConstants.width));
        targetDiagram.setAttributeValue(ReactomeJavaConstants.height, 
                                        sourceDiagram.getAttributeValue(ReactomeJavaConstants.height));
        storePredictedDiagram(targetDiagram,
                              dba);
        return targetDiagram;
    }
    
    private void storePredictedDiagram(GKInstance diagram,
                                       MySQLAdaptor dba) throws Exception {
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        GKInstance defaultIE = getDefaultIE(dba);
        GKInstance created = (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.created);
        if (created == null)
            diagram.setAttributeValue(ReactomeJavaConstants.created,
                                      defaultIE);
        else
            diagram.addAttributeValue(ReactomeJavaConstants.modified,
                                      defaultIE);
        if (dba.supportsTransactions()) {
            if (diagram.getDBID() == null)
                dba.txStoreInstance(diagram);
            else
                dba.txUpdateInstance(diagram);
        }
        else {
            if (diagram.getDBID() == null)
                dba.storeInstance(diagram);
            else
                dba.updateInstance(diagram);
        }
    }
    
    private GKInstance getTargetDiagramInstance(GKInstance targetPathway,
                                                MySQLAdaptor dba) throws Exception {
        // First check if there is a diagram available already
        Collection c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram, 
                                                    ReactomeJavaConstants.representedPathway,
                                                    "=",
                                                    targetPathway);
        if (c != null && c.size() > 0) {
            GKInstance rtn = (GKInstance) c.iterator().next();
            // Have to call this method. Otherwise, original assigned attributes cannot be kept
            // during updating!
            dba.fastLoadInstanceAttributeValues(rtn);
            // In case there is no name set: for some old bug
            if (rtn.getDisplayName() == null) {
                GKInstance species = (GKInstance) targetPathway.getAttributeValue(ReactomeJavaConstants.species);
                rtn.setDisplayName("Diagram of " + targetPathway.getDisplayName() + 
                                   " (" + species.getDisplayName() + ")");
            }
            return rtn;
        }
        GKInstance targetDiagram = new GKInstance();
        targetDiagram.setDbAdaptor(dba);
        targetDiagram.setSchemaClass(dba.getSchema().getClassByName(ReactomeJavaConstants.PathwayDiagram));
        targetDiagram.setAttributeValue(ReactomeJavaConstants.representedPathway,
                                        targetPathway);
        GKInstance species = (GKInstance) targetPathway.getAttributeValue(ReactomeJavaConstants.species);
        targetDiagram.setDisplayName("Diagram of " + targetPathway.getDisplayName() + 
                                     " (" + species.getDisplayName() + ")");
        return targetDiagram;
    }
    
    private RenderablePathway generatePredictedDiagram(RenderablePathway srcRDiagram,
                                                       GKInstance targetPathway,
                                                       MySQLAdaptor dba) throws Exception {
        RenderablePathway predicted = new RenderablePathway();
        GKInstance species = (GKInstance) targetPathway.getAttributeValue(ReactomeJavaConstants.species);
        List components = srcRDiagram.getComponents();
        if (components == null || components.size() == 0) {
            predicted.setReactomeId(targetPathway.getDBID());
            predicted.setHideCompartmentInNode(true);
            return predicted;
        }
        List<HyperEdge> edges = new ArrayList<HyperEdge>();
        Map<Renderable, Renderable> srcToTarget = new HashMap<Renderable, Renderable>();
        // Handle reactions first to avoid any mismapping from this case: one human Entity can be mapped to multiple
        // entities in one other species.
        // These edges cannot be mapped (e.g. reactions for flow), should be mapped later
        List<HyperEdge> unmapped = new ArrayList<HyperEdge>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (!(r instanceof HyperEdge))
                continue;
            HyperEdge edge = (HyperEdge) r;
            // Check if this edge should be used
            GKInstance inferred = null;
            if (isInferrable(edge)) {
                inferred = getInferred(edge.getReactomeId(),
                                       dba,
                                       species);
                if (inferred == null)
                    continue;
            }
            if (inferred == null) {
                unmapped.add(edge);
                continue;
            }
            Map<Renderable, GKInstance> srcNodeToTargetInst = mapReactionParticipants(edge, inferred);
            HyperEdge target = cloneEdge(edge,
                                         inferred,
                                         srcToTarget,
                                         srcNodeToTargetInst);
            target.setReactomeId(inferred.getDBID());
            srcToTarget.put(edge, target);
        }
        for (Renderable r : srcToTarget.values()) {
            predicted.addComponent(r);
        }
        // Handle nodes that have not been mapped (e.g. compartments, pathways, notes).
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (srcToTarget.containsKey(r))
                continue;
            if (r instanceof HyperEdge)
                continue;
            GKInstance inferred = null;
            // Special case for compartments
            if (isInferrable(r)) {
                inferred = getInferred(r.getReactomeId(), 
                                       dba,
                                       species);
                if (inferred == null)
                    continue; // Cannot create an inferred one
            }
            Renderable target = cloneNode(r);
            if (inferred != null)
                target.setReactomeId(inferred.getDBID());
            else
                target.setReactomeId(r.getReactomeId());
            srcToTarget.put(r, target);
            predicted.addComponent(target);
        }
        // Now handle HyperEdges that have not been mapped (e.g. RenderableInteractions).
        for (HyperEdge edge : unmapped) {
            // Check if this edge should be used
            GKInstance inferred = null;
            if (isInferrable(edge)) {
                inferred = getInferred(edge.getReactomeId(),
                                       dba,
                                       species);
                if (inferred == null)
                    continue;
            }
            HyperEdge target = cloneInteraction(edge,
                                         srcToTarget);
            if (inferred != null)
                target.setReactomeId(inferred.getDBID());
            predicted.addComponent(target);
            srcToTarget.put(edge, target);
        }

        // Remove any nodes that have not been connected
        removeUnlinkedNodes(predicted, srcToTarget);
        // Figure out compartments information
        validateCompartments(srcToTarget);
        predicted.setReactomeId(targetPathway.getDBID());
        predicted.setHideCompartmentInNode(true);
        return predicted;
    }
    
    /**
     * Make sure compartments have correct components.
     * @param srcToTarget
     */
    private void validateCompartments(Map<Renderable, Renderable> srcToTarget) {
        for (Renderable src : srcToTarget.keySet()) {
            if (src instanceof RenderableCompartment) {
                RenderableCompartment srcCompart = (RenderableCompartment) src;
                RenderableCompartment targetCompart = (RenderableCompartment) srcToTarget.get(srcCompart);
                if (targetCompart == null)
                    continue;
                List<Renderable> components = srcCompart.getComponents();
                if (components == null || components.size() == 0)
                    continue; // Nothing needs to be done!
                for (Renderable comp : components) {
                    Renderable targetComp = srcToTarget.get(comp);
                    if (targetComp == null)
                        continue;
                    targetCompart.addComponent(targetComp);
                }
            }
        }
    }

    private void removeUnlinkedNodes(RenderablePathway predicted,
                                     Map<Renderable, Renderable> srcToTarget) {
        for (Renderable r : srcToTarget.values()) {
            if (r instanceof HyperEdge ||
                r instanceof Note ||
                r instanceof RenderableCompartment ||
                r instanceof ProcessNode) // Pathways should be kept.
                continue; // These should not be checked
            if (r instanceof Node) {
                Node node = (Node) r;
                List<HyperEdge> connectedEdges = node.getConnectedReactions();
                if (connectedEdges.size() == 0)
                    predicted.getComponents().remove(node);
            }
        }
    }
    
    private boolean isInferrable(Renderable r) {
        if (r instanceof RenderableCompartment ||
            r instanceof RenderableChemical) // Have to make sure EntitySet is correct!!!
            return false;
        if (r.getReactomeId() == null)
            return false;
        return true;
    }
    
    private GKInstance getInferred(Long sourceDbId,
                                   MySQLAdaptor dba,
                                   GKInstance species) throws Exception {
        GKInstance sourceInstance = dba.fetchInstance(sourceDbId);
        // Just in case
        if (sourceInstance == null)
            return null;
        SchemaClass cls = sourceInstance.getSchemClass();
        // The following code cannot work. Not sure why.
        //ReverseAttributeQueryRequest reverseQuery = dba.createReverseAttributeQueryRequest(cls, 
        //                                                                                   cls.getAttribute(ReactomeJavaConstants.inferredFrom),
        //                                                                                   "=",
        //                                                                                   sourceInstance);
        //AttributeQueryRequest query = dba.createAttributeQueryRequest(cls.getAttribute(ReactomeJavaConstants.species),
        //                                                              "=",
        //                                                              species);
        //List<QueryRequest> queries = new ArrayList<QueryRequest>();
        //queries.add(reverseQuery);
        //queries.add(query);
//        Set c = dba.fetchInstance(queries);
        
        Collection referred = sourceInstance.getReferers(ReactomeJavaConstants.inferredFrom);
        if (referred == null || referred.size() == 0)
            return null;
        for (Iterator it = referred.iterator(); it.hasNext();) {
            GKInstance inferred = (GKInstance) it.next();
            GKInstance species1 = (GKInstance) inferred.getAttributeValue(ReactomeJavaConstants.species);
            if (species1 == species)
                return inferred;
        }
        // Check if it is possible the original event in other species has been used for inferring 
        // the human reaction
        List<GKInstance> list = (List<GKInstance>) sourceInstance.getAttributeValuesList(ReactomeJavaConstants.inferredFrom);
        if (list != null && list.size() > 0) {
            for (GKInstance inferredFrom : list) {
                GKInstance species1 = (GKInstance) inferredFrom.getAttributeValue(ReactomeJavaConstants.species);
                if (species1 == species)
                    return inferredFrom;
            }
        }
        return null;
    }
    
    private Map<Renderable, GKInstance> mapReactionParticipants(HyperEdge srcEdge,
                                                                GKInstance target) throws Exception {
        List<Node> srcNodes = srcEdge.getConnectedNodes();
        Set<GKInstance> targetParticipants = InstanceUtilities.getReactionParticipants(target);
        Map<Renderable, GKInstance> srcToTarget = new HashMap<Renderable, GKInstance>();
        Long targetId = null;
        for (GKInstance targetParticipant : targetParticipants) {
            // It may have multiple values
            Collection list = targetParticipant.getAttributeValuesList(ReactomeJavaConstants.inferredFrom);
            if (list == null || list.size() == 0) {
                targetId = targetParticipant.getDBID(); // SimpleEntity or OtherEntity
                for (Node node : srcNodes) {
                    if (targetId.equals(node.getReactomeId())) {
                        srcToTarget.put(node, targetParticipant);
                        break;
                    }
                }
            }
            else {
                // Need to find one only
                for (Iterator it = list.iterator(); it.hasNext();) {
                    GKInstance tmp = (GKInstance) it.next();
                    targetId = tmp.getDBID();
                    for (Node node : srcNodes) {
                        if (targetId.equals(node.getReactomeId())) {
                            srcToTarget.put(node, targetParticipant);
                            break;
                        }
                    }
                    if (srcToTarget.containsValue(targetParticipant))
                        break;
                }
                // I believe there is a bug in the orthologour script for some DefinedSet cannot
                // be mapped. This is a workaround - wgm on 4/20/2010.
                if (!srcToTarget.containsValue(targetParticipant) && 
                    targetParticipant.getSchemClass().isa(ReactomeJavaConstants.DefinedSet)) {
                    Set<GKInstance> inferredFrom = new HashSet<GKInstance>();
                    List members = targetParticipant.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    if (members != null) {
                        for (Iterator it = members.iterator(); it.hasNext();) {
                            GKInstance member = (GKInstance) it.next();
                            list = member.getAttributeValuesList(ReactomeJavaConstants.inferredFrom);
                            if (list != null)
                                inferredFrom.addAll(list);
                        }
                    }
                    for (GKInstance tmp : inferredFrom) {
                        targetId = tmp.getDBID();
                        for (Node node : srcNodes) {
                            if (targetId.equals(node.getReactomeId())) {
                                srcToTarget.put(node, targetParticipant);
                                break;
                            }
                        }
                        if (srcToTarget.containsValue(targetParticipant))
                            break;
                    }
                }
            }
        }
        // In case something cannot be mapped, do some wild mapping since the mapping purpose is to
        // get coordinates from the source. Even though the mapping is not correct, it should be fine
        // as long as roles are correct
        if (srcToTarget.size() < targetParticipants.size()) {
            mapReactionParticipantsBasedOnRoles(srcEdge,
                                                srcToTarget, 
                                                target);
        }
        return srcToTarget;
    }
    
    private void mapReactionParticipantsBasedOnRoles(HyperEdge srcEdge,
                                                     Map<Renderable, GKInstance> srcToTarget,
                                                     GKInstance targetReaction) throws Exception {
        // Check inputs
        List<GKInstance> inputs = (List<GKInstance>) targetReaction.getAttributeValuesList(ReactomeJavaConstants.input);
        mapReactionParticipantsBasedOnRoles(srcToTarget, 
                                            inputs,
                                            srcEdge.getInputNodes());
        // Check outputs
        List<GKInstance> outputs = (List<GKInstance>) targetReaction.getAttributeValuesList(ReactomeJavaConstants.output);
        mapReactionParticipantsBasedOnRoles(srcToTarget, 
                                            outputs,
                                            srcEdge.getOutputNodes());
        // Check Catalysts
        List<GKInstance> catalysts = new ArrayList<GKInstance>();
        List<GKInstance> cas = (List<GKInstance>) targetReaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (cas != null && cas.size() > 0) {
            for (GKInstance ca : cas) {
                GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (catalyst != null)
                    catalysts.add(catalyst);
            }
        }
        mapReactionParticipantsBasedOnRoles(srcToTarget,
                                            catalysts, 
                                            srcEdge.getHelperNodes());
        // Check Activators and Inhibitors
        List<GKInstance> activiators = new ArrayList<GKInstance>();
        List<GKInstance> inhibitors = new ArrayList<GKInstance>();
        Collection<?> regulations = targetReaction.getReferers(ReactomeJavaConstants.regulatedEntity);
        if (regulations != null && regulations.size() > 0) {
            for (Iterator<?> it = regulations.iterator(); it.hasNext();) {
                GKInstance regulation = (GKInstance) it.next();
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator == null)
                    continue;
                // Only take physical entity
                if (regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) {
                    if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation))
                        activiators.add(regulator);
                    else if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation))
                        inhibitors.add(regulator);
                }
            }
        }
        mapReactionParticipantsBasedOnRoles(srcToTarget, activiators, srcEdge.getActivatorNodes());
        mapReactionParticipantsBasedOnRoles(srcToTarget, inhibitors, srcEdge.getInhibitorNodes());
    }

    private void mapReactionParticipantsBasedOnRoles(Map<Renderable, GKInstance> srcToTarget,
                                                     List<GKInstance> inputs,
                                                     List<Node> srcInputs) {
        if (inputs == null || srcInputs == null ||
            inputs.size() == 0 || srcInputs.size() == 0)
            return;
        List<GKInstance> targetCopy = new ArrayList<GKInstance>(inputs);
        targetCopy.removeAll(srcToTarget.values());
        List<Node> srcCopy = new ArrayList<Node>(srcInputs);
        srcCopy.removeAll(srcToTarget.keySet());
        for (int i = 0; i < targetCopy.size(); i++) {
            if (i < srcCopy.size())
                srcToTarget.put(srcCopy.get(i),
                                targetCopy.get(i));
        }
    }
    
    /**
     * Fine tune pathway diagram layout before an image is generated. For example,
     * tweak some display names, etc.
     */
    @Override
    protected void fineTuneDiagram(RenderablePathway rPathway) {
        if (rPathway.getComponents() == null)
            return;
        // Remove name copied notes
        int index = 0;
        for (Iterator it = rPathway.getComponents().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            String name = r.getDisplayName();
            if (name == null) 
                continue;
            index = name.indexOf("(name copied from");
            if (index > 0) {
                name = name.substring(0, index);
                r.setDisplayName(name);
            }
        }
    }

    private Renderable cloneNode(Renderable r) throws InstantiationException, IllegalAccessException {
        // Create another instance from the same class
        Class cls = r.getClass();
        Renderable copy = (Renderable) cls.newInstance();
        RenderUtility.copyRenderInfo(r, copy);
        if (r instanceof Note) {
            ((Note)copy).setPrivate(((Note)r).isPrivate());
        }
        else if (r instanceof RenderableComplex) {
            RenderableComplex complex = (RenderableComplex) r;
            RenderableComplex copyComplex = (RenderableComplex) copy;
            copyComplex.hideComponents(complex.isComponentsHidden());
        }
        copy.setDisplayName(r.getDisplayName());
        return copy;
    }
    
    private HyperEdge cloneInteraction(HyperEdge edge,
                                       Map<Renderable, Renderable> src2Target) throws  InstantiationException, IllegalAccessException {
        HyperEdge copy = (HyperEdge) edge.getClass().newInstance();
        RenderUtility.copyRenderInfo(edge, copy);
        // The following two properties have not been handled in the previous statement
        copy.setLineColor(edge.getLineColor());
        copy.setLineWidth(edge.getLineWidth());
        
        copy.setBackbonePoints(copyPoints(edge.getBackbonePoints()));
        // Make sure position is a point in the backbone
        Point pos = edge.getPosition();
        for (Object p : copy.getBackbonePoints()) {
            if (p.equals(pos)) {
                copy.setPosition((Point)p);
                break;
            }
        }
        copy.setInputPoints(copyBranches(edge.getInputPoints()));
        copy.setOutputPoints(copyBranches(edge.getOutputPoints()));
        // Link to nodes
        List<Node> inputs = edge.getInputNodes();
        for (Node input : inputs) {
            Renderable target = src2Target.get(input);
            if (target != null) 
                copy.addInput((Node)target);
        }
        List<Node> outputs = edge.getOutputNodes();
        for (Node output : outputs) {
            Renderable target = src2Target.get(output);
            if (target != null)
                copy.addOutput((Node)target);
        }
        if (edge instanceof RenderableInteraction) {
            RenderableInteraction i = (RenderableInteraction) edge;
            ((RenderableInteraction)copy).setInteractionType(i.getInteractionType());
        }
        copyStoichiometries(copy, edge);
        return copy;
    }
    
    private Node getTargetNode(Node src,
                               Map<Renderable, Renderable> src2Target,
                               Map<Renderable, GKInstance> srcNodeToTargetInst) throws Exception {
        Renderable target = src2Target.get(src);
        if (target != null) 
            return (Node) target;
        if (srcNodeToTargetInst.containsKey(src)) {
            GKInstance inst = srcNodeToTargetInst.get(src);
            if (inst == null)
                return null;
            target = cloneNode(src);
            target.setReactomeId(inst.getDBID());
            src2Target.put(src, target);
            return (Node) target;
        }
        return null;   
    }
    
    private HyperEdge cloneEdge(HyperEdge edge,
                                GKInstance targetRxt,
                                Map<Renderable, Renderable> src2Target,
                                Map<Renderable, GKInstance> srcNodeToTargetInst) throws  Exception {
//        System.out.println("Clone edges: " + targetRxt);
        HyperEdge copy = (HyperEdge) edge.getClass().newInstance();
        RenderUtility.copyRenderInfo(edge, copy);
        // The following two properties have not been handled in the previous statement
        copy.setLineColor(edge.getLineColor());
        copy.setLineWidth(edge.getLineWidth());
        
        copy.setBackbonePoints(copyPoints(edge.getBackbonePoints()));
        // Make sure position is a point in the backbone
        Point pos = edge.getPosition();
        for (Object p : copy.getBackbonePoints()) {
            if (p.equals(pos)) {
                copy.setPosition((Point)p);
                break;
            }
        }
        copy.setInputPoints(copyBranches(edge.getInputPoints()));
        copy.setOutputPoints(copyBranches(edge.getOutputPoints()));
        // Link to nodes
        List<Node> inputs = edge.getInputNodes();
        for (Node input : inputs) {
            Node target = getTargetNode(input,
                                        src2Target,
                                        srcNodeToTargetInst);
            if (target != null)
                copy.addInput(target);
        }
        List<Node> outputs = edge.getOutputNodes();
        for (Node output : outputs) {
            Node target = getTargetNode(output,
                                        src2Target,
                                        srcNodeToTargetInst);
            if (target != null)
                copy.addOutput(target);
        }
        if (edge instanceof RenderableInteraction) {
            RenderableInteraction i = (RenderableInteraction) edge;
            ((RenderableInteraction)copy).setInteractionType(i.getInteractionType());
        }
        copyStoichiometries(copy, edge);
        // For catalyst
        copy.setHelperPoints(copyBranches(edge.getHelperPoints()));
        List<Node> helpers = edge.getHelperNodes();
        for (Node node : helpers) {
            Node target = getTargetNode(node,
                                        src2Target,
                                        srcNodeToTargetInst);
            if (target != null)
                copy.addHelper(target);
        }
        // Activator
        copy.setActivatorPoints(copyBranches(edge.getActivatorPoints()));
        List<Node> activators = edge.getActivatorNodes();
        for (Node node : activators) {
            Node target = getTargetNode(node,
                                        src2Target,
                                        srcNodeToTargetInst);
            if (target != null)
                copy.addActivator(target);
        }
        // Inhibitors
        copy.setInhibitorPoints(copyBranches(edge.getInhibitorPoints()));
        List<Node> inhibitors = edge.getInhibitorNodes();
        for (Node node : inhibitors) { 
            Node target = getTargetNode(node,
                                        src2Target,
                                        srcNodeToTargetInst);
            if (target != null)
                copy.addInhibitor(target);
        }
        return copy;
    }
    
    private void copyStoichiometries(HyperEdge target,
                                     HyperEdge source) {
        if (!(target instanceof RenderableReaction) ||
            !(source instanceof RenderableReaction))
            return; // Have to be both are RenderableReaction!
        RenderableReaction srcRxt = (RenderableReaction) source;
        RenderableReaction targetRxt = (RenderableReaction) target;
        // Handle input stoichiometries
        List<Node> srcInputs = srcRxt.getInputNodes();
        List<Node> targetInputs = targetRxt.getInputNodes();
        for (int i = 0; i < srcInputs.size(); i++) {
            if (i > targetInputs.size() - 1)
                break; // May be some inputs cannot be mapped
            Node srcNode = srcInputs.get(i);
            int stoi = srcRxt.getInputStoichiometry(srcNode);
            targetRxt.setInputStoichiometry(targetInputs.get(i),
                                            stoi);
        }
        // Handle output stoichiometries
        List<Node> srcOutputs = srcRxt.getOutputNodes();
        List<Node> targetOutputs = targetRxt.getOutputNodes();
        for (int i = 0; i < srcOutputs.size(); i++) {
            if (i > targetOutputs.size() - 1)
                break; // May be some outputs cannot be mapped.
            Node srcNode = srcOutputs.get(i);
            int stoi = srcRxt.getOutputStoichiometry(srcNode);
            targetRxt.setOutputStoichiometry(targetOutputs.get(i),
                                             stoi);
        }
    }
    
    private List<List<Point>> copyBranches(List<List<Point>> branches) {
        if (branches == null)
            return null;
        List<List<Point>> copy = new ArrayList<List<Point>>(branches.size());
        for (List<Point> list : branches) {
            List<Point> listCopy = new ArrayList<Point>(list.size());
            for (Point p : list)
                listCopy.add(new Point(p));
            copy.add(listCopy);
        }
        return copy;
    }
    
    private List<Point> copyPoints(List<Point> points) {
        if (points == null)
            return null;
        List<Point> copy = new ArrayList<Point>(points.size());
        for (Point p : points)
            copy.add(new Point(p));
        return copy;
    }
    
    /**
     * This method should be called to generate diagrams for other species. All human diagrams
     * will be mapped to other species when applicable. Only diagrams instances are created, other
     * instances and static images should be created in other place.
     * @throws Exception
     */
    public void generateDiagramsForOtherSpecies(MySQLAdaptor dba,
                                                Long defaultPerson,
                                                Long srcSpeciesId) throws Exception {
        this.dba = dba;
        setDefaultPersonId(defaultPerson);
        // Get all diagrams in the database
        Collection diagrams = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        if (diagrams == null || diagrams.size() == 0)
            return; // Just in case
        for (Iterator it = diagrams.iterator(); it.hasNext();) {
            GKInstance diagram = (GKInstance) it.next();
//            if (!diagram.getDBID().equals(984845L))
//                continue;
            List<?> pathways = diagram.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
            if (pathways == null || pathways.size() == 0) {
                logger.error(diagram + " has null pathway!");
                continue;
            }
            GKInstance pathway = (GKInstance) pathways.get(0);
            // Have to work from human only in case there is partial work done (e.g. during test)
            GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
            if (species == null) {
                logger.error("Pathway has no species: " + species);
                continue; // Don't work on it
            }
            if (!species.getDBID().equals(srcSpeciesId))
                continue;
            logger.info("Working on pathway " + pathway);
            List orthologousEvents = pathway.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
            if (orthologousEvents == null || orthologousEvents.size() == 0)
                continue;
            for (Iterator it1 = orthologousEvents.iterator(); it1.hasNext();) {
                GKInstance otherPathway = (GKInstance) it1.next();
                GKInstance otherSpecies = (GKInstance) otherPathway.getAttributeValue(ReactomeJavaConstants.species);
                logger.info("Predicting pathway diagram for " + otherPathway);
                GKInstance otherDiagram = generatePredictedDiagram(otherPathway, 
                                                                   pathway, 
                                                                   diagram);
                // Attach other pathways to PathwayDiagram instance if any
                for (int i = 1; i < pathways.size(); i++) {
                    GKInstance pathway1 = (GKInstance) pathways.get(i);
                    // Get the orthologous
                    List<?> orthologusEvents1 = pathway1.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
                    // Find other pathway with the same species
                    for (Object obj : orthologusEvents1) {
                        GKInstance otherPathway1 = (GKInstance) obj;
                        GKInstance otherSpecies1 = (GKInstance) otherPathway1.getAttributeValue(ReactomeJavaConstants.species);
                        if (otherSpecies1 == otherSpecies) {
                            otherDiagram.addAttributeValue(ReactomeJavaConstants.representedPathway,
                                                           otherPathway1);
                            break;
                        }
                    }
                }
                if (pathways.size() > 1) {
                    // Need to update the database
                    dba.updateInstanceAttribute(otherDiagram, 
                                                ReactomeJavaConstants.representedPathway);
                }
            }
        }
    }
    
    @Test
    public void testGeneratePredictedDiagram() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_current_ver40", 
                                            "root", 
                                            "macmysql01");
        setMySQLAdaptor(dba);
        setDefaultPersonId(140537L); // For myself
        // Cell cycle checkpoints
//        Long srcDbId = 69620L;
//        Long targetDbId = 562759L; // Mouse
        // Metabolism of carbohydrates
//        Long srcDbId = 71387L;
//        Long targetDbId = 562514L;
        // Gap junction trafficking
//        Long srcDbId = 74160L;
//        Long targetDbId = 1184067L;
        // Test inferredFrom value
        Long srcDbId = 166520L;
        Long targetDbId = 2295155L;
        GKInstance srcPathway = dba.fetchInstance(srcDbId);
        Collection c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram,
                                                    ReactomeJavaConstants.representedPathway,
                                                    "=", 
                                                    srcPathway);
        GKInstance srcDiagram = (GKInstance) c.iterator().next();
        GKInstance targetPathway = dba.fetchInstance(targetDbId);
        long time1 = System.currentTimeMillis();
        GKInstance predictedDiagram = generatePredictedDiagram(targetPathway, 
                                                               srcPathway, 
                                                               srcDiagram);
        long time2 = System.currentTimeMillis();
        System.out.println("Time to predict: " + (time2 - time1));
        File tmpDir = new File("tmp");
        createImageFiles(predictedDiagram, targetPathway, tmpDir);
//        generateELVInstancesAndFiles(targetPathway,
//                                     predictedDiagram, 
//                                     tmpDir);
        long time3 = System.currentTimeMillis();
        System.out.println("Time for output: " + (time3 - time2));
    }
    
    @Test
    public void testGenerateDiagramsForOtherSpecies() throws Exception {
        PropertyConfigurator.configure("resources/log4j.properties");
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_current_ver32", 
                                            "root", 
                                            "macmysql01");
        Long defaultPerson = 140537L; // For myself
        long time1 = System.currentTimeMillis();
        generateDiagramsForOtherSpecies(dba, 
                                        defaultPerson,
                                        48887L);
        long time2 = System.currentTimeMillis();
        System.out.println("Total time: " + (time2 - time1));
    }
    
    @Test
    public void testGenerateImageFiles() throws Exception {
        PropertyConfigurator.configure("resources/log4j.properties");
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca", 
                                            "test_reactome_34_brie8_diagrams",
                                            "authortool", 
                                            "T001test");
        PredictedPathwayDiagramGeneratorFromDB generator = new PredictedPathwayDiagramGeneratorFromDB();
        generator.setMySQLAdaptor(dba);
        generator.setImageBaseDir("tmp");
        generator.setNeedInfo(false);
        generator.setDefaultPersonId(140537L);
        List<Long> dbIds = new ArrayList<Long>();
        dbIds.add(1052232L);
        for (Long dbId : dbIds) {
            generator.generateImages(dbId);
        }
    }
    
    /**
     * Main method that can be invoked from a command line. The main method expects the following arguments in order:
     * dbHost dbName dbUser dbPwd dbPort imageBaseDir. Two things have been done here: 1). Predicted diagrams for other
     * species based on human manual diagrams; 2). Generate needed files and instances for both predicted and manual 
     * diagrams to serve the web application.
     * imagebaseDir: the top directory name for all static files used by Web ELV
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("Usage: java -Xmx1024m PredictedPathwayDiagramGeneratorFromDB dbHost dbName dbUser dbPwd dbPort "
                    + "dbIdForDefaultPerson {needStaticFiles(optional, true or false, default false, for old web site) imageBaseDir}\n");
            System.exit(1);
        }
        try {
            // Set the logging
            PropertyConfigurator.configure("resources/log4j.properties");
            //logger.info("Log4j is configured!");
            MySQLAdaptor dba = new MySQLAdaptor(args[0],
                                                args[1],
                                                args[2],
                                                args[3],
                                                Integer.parseInt(args[4]));
            PredictedPathwayDiagramGeneratorFromDB generator = new PredictedPathwayDiagramGeneratorFromDB();
            generator.setMySQLAdaptor(dba);
            logger.info("Used database: " + dba.getDBName());
            Long defaultPerson = new Long(args[5]);
            logger.info("Default person: " + defaultPerson);
            // Predicted pathway diagrams
            logger.info("Starting generating diagrams for other species...");
            generator.generateDiagramsForOtherSpecies(dba, 
                                                      defaultPerson,
                                                      48887L);
            if (args.length > 7 && args[6].equalsIgnoreCase("true")) {
                generator.setImageBaseDir(args[7]);
                logger.info("Image base dir: " + generator.getImageBaseDir());
                generator.setNeedInfo(false); // Don't need information stored.
                List<Long> dbIds = generator.getPathwayIDsForDiagrams(dba);
                logger.info("Starting generate images...");
                logger.info("Total pathway ids used for generating images: " + dbIds.size());
                for (Long dbId : dbIds) {
                    generator.generateImages(dbId);
                }
            }
            logger.info("Finished the whole process!");
        }
        catch(Exception e) {
            logger.error("Error in main(): ", e);
            e.printStackTrace();
        }
    }
                             
}
