/*
 * Created on Nov 10, 2010
 *
 */
package org.gk.elv;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.DiagramGKBWriter;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.xml.sax.InputSource;


/**
 * This helper class is used to encapsulate a sub-pathway drawn in a container into a
 * diagram by its own.
 * @author wgm
 *
 */
public class PathwayEncapsulateHelper {
    
    public PathwayEncapsulateHelper() {
    }
    
    public void encapsulateDiagram(GKInstance subPathway,
                                   EntityLevelView elv) throws Exception {
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        // Need to abort this action if a diagram has been existed for this subPathway
        GKInstance diagramInst = null;
        if (fileAdaptor.getDiagram(subPathway) != null) {
            int reply = JOptionPane.showConfirmDialog(elv, 
                                          "A diagram exists for the selected pathway, \"" + subPathway.getDisplayName() + "\".\n" +
                                          "\"Encapsulate Diagram\" will overwrite the existed diagram. Are you sure you want to continue?",
                                          "Overwrite Exsisted Diagram?",
                                          JOptionPane.OK_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION)
                return;
            RenderablePathway oldDiagram = fileAdaptor.getDiagram(subPathway);
            diagramInst = fileAdaptor.getPathwayDiagramInstance(oldDiagram);
        }
        RenderablePathway pathway = (RenderablePathway) elv.getZoomablePathwayEditor().getPathwayEditor().getRenderable();
        // Create a new RenderablePathway by re-open
        DiagramGKBWriter writer = new DiagramGKBWriter();
        Project project = new Project(pathway);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writer.save(project, bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        DiagramGKBReader reader = new DiagramGKBReader();
        Project newProject = reader.open(new InputSource(bis));
        RenderablePathway newDiagram = newProject.getProcess();
        newDiagram.setReactomeId(subPathway.getDBID());
        cleanUp(newDiagram, subPathway, fileAdaptor);
        // TODO: Make sure the whole diagram should be moved to the top-left corner to avoid
        // any extra space.
        //checkPosition(newDiagram);
        // Need to create a PathwayDiagram for it
        if (diagramInst == null) {
            GKInstance diagramInstance = fileAdaptor.createNewInstance(ReactomeJavaConstants.PathwayDiagram);
            diagramInstance.setAttributeValueNoCheck(ReactomeJavaConstants.representedPathway, subPathway);
            InstanceDisplayNameGenerator.setDisplayName(diagramInstance);
        }
        else {
            fileAdaptor.markAsDirty(diagramInst);
        }
        reader.setDisplayNames(newDiagram, fileAdaptor);
        fileAdaptor.addDiagram(subPathway, newDiagram);
        cleanUpContainerDiagram(pathway,
                                newDiagram,
                                subPathway,
                                elv);
        JOptionPane.showMessageDialog(elv, 
                                      "Please make sure all compartments in the opened diagram are correct, and link the newly\n" +
                                      "created pathway node where applicable. Use \"Open Diagram\" to view the encapsulated\n" +
                                      "diagram. Some fine adjustments may be needed in diagrams.",
                                      "Encapsulating Diagram", 
                                      JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void cleanUpContainerDiagram(RenderablePathway diagram,
                                         RenderablePathway subDiagram,
                                         GKInstance subEvent,
                                         EntityLevelView elv) throws Exception {
        List<?> subComps = subDiagram.getComponents();
        if (subComps == null)
            subComps = new ArrayList<Renderable>(); // For easy programming
        // Get displayed Reactions in subDiagram
        Set<Long> subRxtIds = new HashSet<Long>();
        for (Iterator<?> it = subComps.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableReaction) {
                subRxtIds.add(r.getReactomeId());
            }
        }
        // Remove reactions in diagram
        List<?> comps = diagram.getComponents();
        if (comps == null)
            comps = new ArrayList<Renderable>();
        Set<Node> connectedNodes = new HashSet<Node>();
        for (Iterator<?> it = comps.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r.getReactomeId() == null)
                continue;
            if (subRxtIds.contains(r.getReactomeId())) {
                // It should be a reaction
                RenderableReaction reaction = (RenderableReaction) r;
                connectedNodes.addAll(reaction.getConnectedNodes());
                reaction.clearConnectWidgets();
                removeComponent(it, reaction);
            }
        }
        // Check connected nodes. Remove nodes that are not connected
        Rectangle rect = null;
        for (Node node : connectedNodes) {
            List<HyperEdge> edges = node.getConnectedReactions();
            if (edges == null || edges.size() == 0) {
                removeComponent(comps, node);
            }
            if (rect == null)
                rect = new Rectangle(node.getBounds());
            else
                rect = rect.union(node.getBounds());
        }
        // Under the rectangle, all empty compartments will be deleted
        if (rect !=  null) {
            for (Iterator<?> it = comps.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (!(r instanceof RenderableCompartment))
                    continue;
                // Want to handle compartments related to this sub-pathway only
                if (!rect.intersects(r.getBounds()))
                    continue;
                boolean needToDelete = true;
                List<?> list = r.getComponents();
                if (list != null && list.size() > 0) {
                    for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                        Renderable r1 = (Renderable) it1.next();
                        if (!(r1 instanceof RenderableCompartment)) {
                            needToDelete = false;
                            break;
                        }
                    }
                }
                if (needToDelete) {
                    removeComponent(it, r);
                }
            }
        }
        // Add a ProcessNode for the above reaction
        Renderable inserted = elv.getZoomablePathwayEditor().insertInstance(subEvent);
        List<Renderable> selection = new ArrayList<Renderable>();
        if (inserted != null) {
            // Set the position in the middle of the original sub-pathway
            if (rect != null) {
                int x = (int) rect.getCenterX();
                if (x < 50)
                    x = 50;
                int y = (int) rect.getCenterY();
                if (y < 50)
                    y = 50;
                inserted.setPosition(x, y);
            }
            selection.add(inserted);
        }
        elv.getZoomablePathwayEditor().getPathwayEditor().setSelection(selection);
    }
    
    private void cleanUp(RenderablePathway diagram,
                         GKInstance pathway,
                         XMLFileAdaptor fileAdaptor) throws Exception {
        // Remove objects that should not be kept
        List<?> components = diagram.getComponents();
        if (components == null || components.size() == 0)
            return; // Nothing needs to be done
        // Get all instances that should be kept
        Set<Long> kept = new HashSet<Long>();
        Set<GKInstance> contained = InstanceUtilities.getContainedEvents(pathway);
        for (GKInstance inst : contained) {
            kept.add(inst.getDBID());
            // Get all related instances
            if (inst.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                Set<GKInstance> participants = InstanceUtilities.getReactionParticipants(inst);
                for (GKInstance part : participants)
                    kept.add(part.getDBID());
            }
        }
        for (Iterator<?> it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableCompartment || // Leave compartments for the time being
                kept.contains(r.getReactomeId()))
                continue;
            removeComponent(it, r);
            if (r instanceof HyperEdge) {
                HyperEdge edge = (HyperEdge) r;
                edge.clearConnectWidgets();
            }
        }
        // Do a cleaning to remove any Entities that should be removed: duplicated entities may be used
        for (Iterator<?> it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (!(r instanceof Node) || 
                 (r instanceof ProcessNode) || 
                 (r instanceof RenderableCompartment))
                continue;
            Node node = (Node) r;
            List<HyperEdge> reactions = node.getConnectedReactions();
            if (reactions == null || reactions.size() == 0)
                removeComponent(it, r);
        }
        // Check for compartments now
        for (Iterator<?> it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (!(r instanceof RenderableCompartment))
                continue;
            RenderableCompartment compart = (RenderableCompartment) r;
            List<?> list = compart.getComponents();
            boolean needToRemove = true;
            for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                Renderable r1 = (Renderable) it1.next();
                if (kept.contains(r1.getReactomeId())) {
                    needToRemove = false;
                    break;
                }
            }
            if (needToRemove)
                removeComponent(it, compart);
        }
    }
    
    private void removeComponent(List<?> components, Renderable r) {
        components.remove(r);
        Renderable container = r.getContainer();
        if (container != null) {
            r.setContainer(null);
            container.removeComponent(r);
        }
    }

    private void removeComponent(Iterator<?> it, Renderable r) {
        it.remove();
        Renderable container = r.getContainer();
        if (container != null) {
            r.setContainer(null);
            container.removeComponent(r);
        }
    }
    
}
