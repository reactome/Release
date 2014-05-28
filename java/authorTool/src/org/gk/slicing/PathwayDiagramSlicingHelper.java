/*
 * Created on Mar 17, 2010
 *
 */
package org.gk.slicing;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.DiagramGKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.Project;
import org.gk.render.EntitySetAndEntitySetLink;
import org.gk.render.EntitySetAndMemberLink;
import org.gk.render.FlowLine;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.ProcessNode;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.schema.InvalidAttributeException;
import org.junit.Test;

/**
 * This class is used to handle pathway diagram related activities during slicing.
 * @author wgm
 *
 */
public class PathwayDiagramSlicingHelper {
    // Logger for this class.
    private final static Logger logger = Logger.getLogger(PathwayDiagramSlicingHelper.class);
    protected boolean isInDev = false;
    // Cached for helping diagram processing
    private DiagramGKBReader diagramReader;
    private DiagramGKBWriter diagramWriter;
    
    public PathwayDiagramSlicingHelper() {
        diagramReader = new DiagramGKBReader();
        diagramWriter = new DiagramGKBWriter();
    }
    
    public void removeDoNotReleaseEvents(GKInstance diagramInstance,
                                         MySQLAdaptor dba) throws Exception {
        RenderablePathway diagram = diagramReader.openDiagram(diagramInstance);
        removeDoNotReleaseEvents(diagram, diagramInstance, dba);
    }
    
    /**
     * This method is used to remove doNotRelease events and its linked objects.
     * @param diagram
     * @param diagramInstance
     * @throws Exception
     */
    public void removeDoNotReleaseEvents(RenderablePathway diagram,
                                          GKInstance diagramInstance,
                                          MySQLAdaptor dba) throws Exception {
        List<Renderable> components = diagram.getComponents();
        if (components == null || components.size() == 0)
            return;
        Set<Renderable> toBeRemoved = new HashSet<Renderable>();
        for (Renderable r : components) {
            if (r.getReactomeId() == null)
                continue;
            GKInstance instance = dba.fetchInstance(r.getReactomeId());
            if (instance == null) {
                logger.warn(r.getReactomeId() + " in " +  diagramInstance.getDisplayName() + " is not in the slice databasae!");
                continue;
            }
            if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants._doRelease)) {
                continue;
            }
            Boolean doRelease = (Boolean) instance.getAttributeValue(ReactomeJavaConstants._doRelease);
            if (doRelease != null && doRelease.booleanValue())
                continue;
            processDoNotReleaseEvent(r, 
                                     toBeRemoved,
                                     dba);
        }
        // Sanity check: some nodes may be linked to doNotRelease ProcessNode via FlowLines. These nodes should
        // be removed too.
        List<Node> more = new ArrayList<Node>();
        for (Renderable r : toBeRemoved) {
            if (r instanceof FlowLine) {
                FlowLine line = (FlowLine) r;
                List<Node> nodes = line.getConnectedNodes();
                for (Node node : nodes) {
                    if (toBeRemoved.contains(node))
                        continue;
                    // If it is a ProcessNode, it should be kept since it should be
                    // tested before
                    if (node instanceof ProcessNode)
                        continue;
                    List<HyperEdge> edges = node.getConnectedReactions();
                    if (isToBeRemoved(edges, toBeRemoved))
                        more.add(node);
                }
            }
        }
        toBeRemoved.addAll(more);
        if (toBeRemoved.size() == 0)
            return; // Nothing has been done
        // Clear-up EntitySetAndMember links if any
        List<FlowLine> linksToBeRemoved = new ArrayList<FlowLine>();
        for (Renderable r : components) {
            if (r instanceof EntitySetAndMemberLink ||
                r instanceof EntitySetAndEntitySetLink) {
                FlowLine link = (FlowLine) r;
                Node input = link.getInputNode(0);
                Node output = link.getOutputNode(0);
                if (input == null || output == null ||
                    toBeRemoved.contains(input) || toBeRemoved.contains(output))
                    linksToBeRemoved.add(link);
            }
        }
        toBeRemoved.addAll(linksToBeRemoved);
        // Remove all links 
        for (Renderable r : toBeRemoved)
            r.clearConnectWidgets();
        diagram.getComponents().removeAll(toBeRemoved);
        // Make changes to the diagram instance
        // Dimension may be changed because of the above removable
        Dimension size = RenderUtility.getDimension(diagram);
        // Make sure these changes should never be written back to the sourceDBA during slicing
        diagramInstance.setAttributeValue(ReactomeJavaConstants.width, 
                                          size.width);
        diagramInstance.setAttributeValue(ReactomeJavaConstants.height, 
                                          size.height);
        Project project = new Project(diagram);
        String xml = diagramWriter.generateXMLString(project);
        diagramInstance.setAttributeValue(ReactomeJavaConstants.storedATXML, 
                                          xml);
    }
    
    private boolean isToBeRemoved(List<HyperEdge> edges,
                                  Set<Renderable> toBeRemoved) {
        for (HyperEdge edge : edges) {
            if (!toBeRemoved.contains(edge))
                return false;
        }
        return true;
    }
    
    /**
     * A helper method to grep objects should be removed.
     * @param r
     * @param toBeRemoved
     */
    private void processDoNotReleaseEvent(Renderable r,
                                          Set<Renderable> toBeRemoved,
                                          MySQLAdaptor dba) throws Exception {
        toBeRemoved.add(r);
        if (r instanceof ProcessNode) {
            List<HyperEdge> edges = ((ProcessNode)r).getConnectedReactions();
            for (HyperEdge edge : edges) {
                if (edge instanceof FlowLine) {
                    toBeRemoved.add(edge);
                }
            }
        }
        else if (r instanceof HyperEdge) {
            HyperEdge edge = (HyperEdge) r;
            // Want to check linked nodes
            List<Node> linked = edge.getConnectedNodes();
            for (Node node : linked) {
                // Check if node should be removed. If a node is linked to doNotRelease events only,
                // this node should be removed. Otherwise, keep it.
                List<HyperEdge> connectedReactions = node.getConnectedReactions();
                boolean needRemove = true;
                for (HyperEdge connectedEdge : connectedReactions) {
                    if (connectedEdge instanceof EntitySetAndMemberLink ||
                        connectedEdge instanceof EntitySetAndEntitySetLink)
                        continue; // Should not be considered a valid criteria to keep a Node.
                    // Special cases
                    if (connectedEdge.getReactomeId() == null) {
                        needRemove = false;
                        break;
                    }
                    GKInstance inst = dba.fetchInstance(connectedEdge.getReactomeId());
                    if (inst == null) {
                        needRemove = false;
                        break; // This has been linked to some FlowLines
                    }
                    Boolean doRelease = (Boolean) inst.getAttributeValue(ReactomeJavaConstants._doRelease);
                    if (doRelease != null && doRelease.booleanValue()) {
                        needRemove = false;
                        break; // Nothing to worry
                    }
                }
                if (needRemove)
                    toBeRemoved.add(node);
            }
        }
    }
    
    /**
     * This method is used to load contained sub-pathway diagrams for a passed diagram.
     * @param diagram
     * @return
     * @throws Exception
     */
    public Set<GKInstance> loadContainedSubPathways(GKInstance diagram,
                                                    MySQLAdaptor dba) throws Exception {
        Set<GKInstance> subDiagrams = new HashSet<GKInstance>();
        // Read the diagram
        loadContainedSubPathways(diagram, 
                                 dba,
                                 subDiagrams);
        return subDiagrams;
    }

    private void loadContainedSubPathways(GKInstance diagram, 
                                          MySQLAdaptor dba,
                                          Set<GKInstance> subDiagrams) throws InvalidAttributeException, Exception {
        String xml = (String) diagram.getAttributeValue(ReactomeJavaConstants.storedATXML);
        RenderablePathway rDiagram = diagramReader.openDiagram(xml);
        //removeDoNotReleaseEvents(rDiagram, diagram, dba);
        List components = rDiagram.getComponents();
        if (components != null) {
            for (Iterator it = components.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                Long reactomeId = r.getReactomeId();
                if (reactomeId == null)
                    continue; // In case for notes
                GKInstance instance = dba.fetchInstance(reactomeId);
                if (instance == null) { // Want to make it work in is in dev
                    logger.warn(diagram.getDisplayName() + " has an object not in the database: " + reactomeId); // Should never occur
                    continue;
                    //throw new IllegalStateException(diagram.getDisplayName() + " has an object not in the database: " + reactomeId); // Should never occur
                }
                if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                    // Check if this is a releasable pathway
                    Boolean doRelease = (Boolean) instance.getAttributeValue(ReactomeJavaConstants._doRelease);
                    if (doRelease == null || !doRelease)
                        continue; // It is a _doRelease false pathway
                    GKInstance subDiagram = fetchDiagramForPathway(instance, dba);
                    if (subDiagram == null) {
                        //if (isInDev)
                        //    continue;
                        logger.warn("\"" + diagram.getDisplayName() + "\" has a pathway node has no diagram associated: " + reactomeId);
                        continue; 
                        //throw new IllegalStateException("\"" + diagram.getDisplayName() + "\" has a pathway node has no diagram associated: " + reactomeId);
                    }
                    if (!subDiagrams.contains(subDiagram)) {
                        subDiagrams.add(subDiagram);
                        loadContainedSubPathways(subDiagram,
                                                 dba,
                                                 subDiagrams);
                    }
                }
            }
        }
    }
    
    protected GKInstance fetchDiagramForPathway(GKInstance pathway,
                                                MySQLAdaptor dba) throws Exception {
        Collection collection = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram,
                                                             ReactomeJavaConstants.representedPathway,
                                                             "=",
                                                             pathway);    
        if (collection == null || collection.size() == 0)
            return null;
        // Pick up the first one only
        return (GKInstance) collection.iterator().next();
    }
    
    /**
     * This method is used to test method for removing do not release.
     */
    @Test
    public void testRemoveDoNotReleaseEvents() throws Exception {
        MySQLAdaptor targetDBA = new MySQLAdaptor("localhost", 
                                                  "gk_central_091112",
                                                  "root",
                                                  "macmysql01");
        MySQLAdaptor sourceDBA = new MySQLAdaptor("localhost",
                                                  "gk_central_091112", 
                                                  "root", 
                                                  "macmysql01");
        // Diagram wanted to be processed
//        Long dbId = 451075L;
        Long dbId = 2029401L;
        GKInstance sourceInst = sourceDBA.fetchInstance(dbId);
        RenderablePathway pathway = diagramReader.openDiagram(sourceInst);
        removeDoNotReleaseEvents(pathway, sourceInst, sourceDBA);
        String xml = (String) sourceInst.getAttributeValue(ReactomeJavaConstants.storedATXML);
        System.out.println(xml);
        
        // Do an update in the target DBA
//        sourceInst.setDbAdaptor(targetDBA);
//        SchemaClass cls = targetDBA.getSchema().getClassByName(ReactomeJavaConstants.PathwayDiagram);
//        SchemaAttribute att = cls.getAttribute(ReactomeJavaConstants.storedATXML);
//        targetDBA.updateInstanceAttribute(sourceInst, att);
    }
    
}
