/*
 * Created on Aug 1, 2008
 *
 */
package org.gk.gkCurator.authorTool;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.GKBReader;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.render.*;
import org.xml.sax.InputSource;

/**
 * This class is used to handle pathway diagram related stuff.
 * @author wgm
 *
 */
public class PathwayDiagramHandler {
    private RenderablePathway topProcess;
    
    public PathwayDiagramHandler() {
    }
    
    /**
     * This method should be called to retrieve the stored diagram for a passed
     * pathway instance or its contained sub-pathway if nothing is done for the
     * passed pathway.
     * @param pathway
     * @param newProject
     * @param parentComp
     * @return
     * @throws Exception
     */
    public boolean retrieveStoredRenderInfo(GKInstance pathway,
                                            Project newProject,
                                            Component parentComp) throws Exception {
        Map<GKInstance, String> pathwayToXML = loadPathwayToXML(pathway, parentComp);
        if (pathwayToXML.size() == 0)
            return false;
        topProcess = newProject.getProcess();
        Map<Long, RenderablePathway> dbIdToR = indexPathways(newProject.getProcess());
        for (GKInstance tmp : pathwayToXML.keySet()) {
            String xml = pathwayToXML.get(tmp);
            RenderablePathway oldPathway = createProcessBasedOnXML(xml);
            RenderablePathway newPathway = dbIdToR.get(tmp.getDBID());
            extractRenderInfoFromOldProject(oldPathway, 
                                            newPathway);
        }
        return true;
    }
    
    private Map<Long, RenderablePathway> indexPathways(RenderablePathway process) {
        Map<Long, RenderablePathway> idToPathway = new HashMap<Long, RenderablePathway>();
        Long dbId = process.getReactomeId();
        if (dbId != null)
            idToPathway.put(new Long(dbId), process);
        List components = process.getComponents();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderablePathway) {
                dbId = r.getReactomeId();
                if (dbId != null)
                    idToPathway.put(new Long(dbId),
                                    (RenderablePathway)r);
            }
        }
        return idToPathway;
    }
    
    /**
     * Load stored XML for the passed pathway. If no XML is found for the passsed pathway,
     * its contained pathways will be checked until one is found. 
     * @param pathway
     * @param parentComp
     * @return
     * @throws Exception
     */
    private Map<GKInstance, String> loadPathwayToXML(GKInstance pathway,
                                                    Component parentComp) throws Exception {
        Map<GKInstance, String> pathwayToXML = new HashMap<GKInstance, String>();
        // Need to get the database adaptor first. 
        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(parentComp);
        if (dba == null) {
            // Show an error message
            JOptionPane.showMessageDialog(parentComp, 
                                          "Cannot connection to the database. The diagram will be auto-generated.", 
                                          "Connection Error", 
                                          JOptionPane.ERROR_MESSAGE);
            return pathwayToXML;
        }
        loadPathwayToXML(pathway, parentComp, pathwayToXML);
        return pathwayToXML;
    }

    /**
     * A recursive method to load the stored author tool file for the pathway.
     * @param pathway
     * @param parentComp
     * @param pathwayToXML
     * @throws Exception
     */
    private void loadPathwayToXML(GKInstance pathway, 
                                  Component parentComp,
                                  Map<GKInstance, String> pathwayToXML) throws Exception {
        String xml = loadStoredXMLFile(pathway, parentComp);
        if (xml != null && xml.length() > 0) {
            pathwayToXML.put(pathway, xml);
            return;
        }
        List components = pathway.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            GKInstance comp = (GKInstance) it.next();
            if (comp.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                loadPathwayToXML(comp, 
                                 parentComp,
                                 pathwayToXML);
        }
    }
    
    private String loadStoredXMLFile(GKInstance pathway,
                                     Component parentComp) throws Exception {
        if (pathway.getDBID() <= 0)
            return null; // A local new pathway. Nothing should be stored for it.
        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(parentComp);
        if (!dba.getSchema().isValidClass(ReactomeJavaConstants.PathwayDiagram))
            return null;
        // Load PathwayDiagram for this pathway
        Collection instances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram, 
                                                           ReactomeJavaConstants.representedPathway, 
                                                           "=", 
                                                           pathway);
        if (instances == null || instances.size() == 0)
            return null; // Nothing in the database yet
        // There should be only one
        GKInstance pathwayDiagram = (GKInstance) instances.iterator().next();
        if (pathwayDiagram.getSchemClass().isValidAttribute(ReactomeJavaConstants.storedATXML)) {
            String xml = (String) pathwayDiagram.getAttributeValue(ReactomeJavaConstants.storedATXML);
            return xml;
        }
        return null;
    }
    
    private RenderablePathway createProcessBasedOnXML(String xml) throws Exception {
        RenderableRegistry.getRegistry().clear();
        GKBReader reader = new GKBReader();
        StringReader stringReader = new StringReader(xml);
        Project project = reader.open(new InputSource(stringReader));
        return project.getProcess();
    }
        
    /**
     * This method is used to copy the stored coordinates to the newly created project.
     * By copying coordinates to newly created project, we can ignore any changes in the
     * new project (add, delete, edit).
     * @param oldProject
     * @param newProject
     */
    private void extractRenderInfoFromOldProject(RenderablePathway oldPathway,
                                                 RenderablePathway newPathway) {
        Map<String, Renderable> oldIdToR = indexRenderables(oldPathway);
        Map<String, Renderable> newIdToR = indexRenderables(newPathway);
        // Make copy rendering information from old to new based on unique ids
        for (String newId : newIdToR.keySet()) {
            Renderable oldR = oldIdToR.get(newId);
            if (oldR == null)
                continue;
            Renderable newR = newIdToR.get(newId);
            // Cannot use the method in the RenderUtility. Need to have a light weight method
            // here
            if (newR instanceof Node && oldR instanceof Node)
                switchNodeRenderInfo((Node)oldR, 
                                     (Node)newR);
        }
        // Switch edge information
        for (String newId : newIdToR.keySet()) {
            Renderable oldR = oldIdToR.get(newId);
            if (oldR == null)
                continue;
            Renderable newR = newIdToR.get(newId);
            if (newR instanceof HyperEdge && oldR instanceof HyperEdge)
                switchEdgeRenderInfo((HyperEdge)oldR,
                                     (HyperEdge)newR);
        }
        // Some complex components may be new. After copying, their bounds maybe not correct
        // They are still use the old bounds. Check this case
        for (String newId : newIdToR.keySet()) {
            Renderable newR = newIdToR.get(newId);
            if (!(newR instanceof RenderableComplex) ||
                newR.getContainer() instanceof RenderableComplex)
                continue;
            RenderableComplex complex = (RenderableComplex) newR;
            // For hidden components only
            if (complex.getBounds() != null &&
                complex.isComponentsHidden())
                complex.copyBoundsToComponents();
            else
                complex.invalidateBounds();
        }
        // We need to copy notes and compartments information
        Map<Renderable, String> oldRToId = switchKeyValue(oldIdToR);
        for (String oldId : oldIdToR.keySet()) {
            Renderable oldR = oldIdToR.get(oldId);
            if (oldR instanceof RenderableCompartment) {
                copyCompartment((RenderableCompartment)oldR,
                                oldRToId,
                                newIdToR,
                                oldPathway,
                                newPathway);
            }
            else if (oldR instanceof Note) {
                // Just make a copy
                newPathway.addComponent(oldR);
                oldR.setContainer(newPathway);
                oldR.setID(RenderableRegistry.getRegistry().nextId());
                RenderableRegistry.getRegistry().add(oldR);
            }
        }
    }
    
    private void copyCompartment(RenderableCompartment compartment,
                                 Map<Renderable, String> oldRToId,
                                 Map<String, Renderable> newIdToR,
                                 RenderablePathway oldProcess,
                                 RenderablePathway newProcess) {
        topProcess.addComponent(compartment);
        if (compartment.getContainer() == oldProcess)
            compartment.setContainer(topProcess);
        topProcess.resetNextID();
        compartment.setID(topProcess.generateUniqueID());
        // Registry should be handled later. No use to handle here since it will be
        // removed.
        // Need to make sure its components are correct
        List components = compartment.getComponents();
        if (components == null)
            return;
        List newComps = new ArrayList();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable oldComp = (Renderable) it.next();
            if (oldComp instanceof RenderableCompartment)
                continue; // The contained compartment should be still there to
                          // keep the hierarchy relationship
            String id = oldRToId.get(oldComp);
            Renderable newComp = newIdToR.get(id);
            if (newComp != null) {
                newComps.add(newComp);
                newComp.setContainer(compartment);
            }
            it.remove();
        }
        components.addAll(newComps);
    }
    
    private Map<Renderable, String> switchKeyValue(Map<String, Renderable> idToR) {
        Map<Renderable, String> rToId = new HashMap<Renderable, String>();
        for (String id : idToR.keySet()) {
            Renderable r = idToR.get(id);
            rToId.put(r, id);
        }
        return rToId;
    }
    
    private void switchEdgeRenderInfo(HyperEdge source,
                                      HyperEdge target) {
        // Color information
        target.setForegroundColor(source.getForegroundColor());
        target.setBackgroundColor(source.getBackgroundColor());
        target.setLineWidth(source.getLineWidth());
        if (source instanceof RenderableReaction && target instanceof RenderableReaction) {
            RenderableReaction sourceRxt = (RenderableReaction) source;
            RenderableReaction targetRxt = (RenderableReaction) target;
            targetRxt.setReactionType(sourceRxt.getReactionType());
        }
        // Copy positions of edges are much more involved than nodes
        // Have to copy position first to make position correct
        Point oldPos = source.getPosition();
        target.setPosition(new Point(oldPos));
        target.setBackbonePoints(copyPointList(source.getBackbonePoints()));
        copyInputBranch(source, target);
        copyOutputBranch(source, target);
        copyHelperBranch(source, target);
        copyInhibitorBranch(source, target);
        copyActivatorBranch(source, target);
        target.validateWidgetControlPoints();
        validateWidgetPosition(target);
    }
    
    private void copyActivatorBranch(HyperEdge source,
                                     HyperEdge target) {
        List sourcePoints = source.getActivatorPoints();
        if (sourcePoints == null)
            return;
        List<Node> sourceNodes = source.getActivatorNodes();
        List targetPoints = target.getActivatorPoints();
        List<Node> targetNodes = target.getActivatorNodes();
        copyBranch(sourcePoints, 
                   sourceNodes, 
                   targetPoints,
                   targetNodes);
    }
    
    private void copyInhibitorBranch(HyperEdge source,
                                     HyperEdge target) {
        List sourcePoints = source.getInhibitorPoints();
        if (sourcePoints == null)
            return;
        List<Node> sourceNodes = source.getInhibitorNodes();
        List targetPoints = target.getInhibitorPoints();
        List<Node> targetNodes = target.getInhibitorNodes();
        copyBranch(sourcePoints, 
                   sourceNodes, 
                   targetPoints,
                   targetNodes);
    }
    
    private void copyHelperBranch(HyperEdge source,
                                  HyperEdge target) {
        List sourcePoints = source.getHelperPoints();
        if (sourcePoints == null)
            return;
        List<Node> sourceNodes = source.getHelperNodes();
        List targetPoints = target.getHelperPoints();
        List<Node> targetNodes = target.getHelperNodes();
        copyBranch(sourcePoints, 
                   sourceNodes, 
                   targetPoints,
                   targetNodes);
    }
    
    private void copyOutputBranch(HyperEdge source,
                                  HyperEdge target) {
        List sourcePoints = source.getOutputPoints();
        if (sourcePoints == null)
            return;
        List<Node> sourceNodes = source.getOutputNodes();
        List targetPoints = target.getOutputPoints();
        List<Node> targetNodes = target.getOutputNodes();
        copyBranch(sourcePoints, 
                   sourceNodes, 
                   targetPoints,
                   targetNodes);
    }
    
    private void copyInputBranch(HyperEdge source,
                                 HyperEdge target) {
        List sourceInputPoints = source.getInputPoints();
        if (sourceInputPoints == null)
            return;
        List<Node> sourceInputs = source.getInputNodes();
        List targetInputPoints = target.getInputPoints();
        List<Node> targetInputs = target.getInputNodes();
        copyBranch(sourceInputPoints, 
                   sourceInputs, 
                   targetInputPoints,
                   targetInputs);
    }

    private void copyBranch(List sourcePoints, 
                            List<Node> sourceNodes,
                            List targetPoints,
                            List<Node> targetNodes) {
        for (int i = 0; i < targetNodes.size(); i++) {
            Node input = targetNodes.get(i);
            // Need to find the index from the source
            int sourceIndex = indexOfNode(sourceNodes, input);
            if (sourceIndex == -1)
                continue;
            List sourceList = (List) sourcePoints.get(sourceIndex);
            List copy = copyPointList(sourceList);
            targetPoints.set(i, copy);
        }
    }
    
    private int indexOfNode(List<Node> nodes, Node target) {
        // Check based on reactome id
        for (int i = 0; i < nodes.size(); i++) {
            Node source = nodes.get(i);
            if (source.getReactomeId() == target.getReactomeId())
                return i;
        }
        return -1;
    }
    
    private void validateWidgetPosition(HyperEdge edge) {
        List widgets = edge.getConnectInfo().getConnectWidgets();
        if (widgets == null)
            return;
        for (Iterator it = widgets.iterator(); it.hasNext();) {
            ConnectWidget widget = (ConnectWidget) it.next();
            int role = widget.getRole();
            int index = widget.getIndex();
            Point point = getWidgetPoint(edge, index, role);
            if (point != null)
                widget.setPoint(point);
            // Otherwise, take the default value.
            //TODO: need to consider cases like connection changed.
            // Right now, the connected nodes will be delinked.
        }
    }
    
    private Point getWidgetPoint(HyperEdge edge,
                                 int index, 
                                 int role) {
        Point point = null;
        List branch = null;
        switch (role) {
            case HyperEdge.INPUT :
                branch = edge.getInputPoints();
                if (branch == null) 
                    point = (Point) edge.getBackbonePoints().get(0);
                break;
            case HyperEdge.OUTPUT :
                branch = edge.getOutputPoints();
                if (branch == null) {
                    List backbone = edge.getBackbonePoints();
                    point = (Point) backbone.get(backbone.size() - 1);
                }
                break;
            case HyperEdge.CATALYST :
                branch = edge.getHelperPoints();
                break;
            case HyperEdge.ACTIVATOR :
                branch = edge.getActivatorPoints();
                break;
            case HyperEdge.INHIBITOR :
                branch = edge.getInhibitorPoints();
                break;
        }
        if (point != null)
            return point;
        if (branch != null && index < branch.size()) {
            List points = (List) branch.get(index);
            point = (Point) points.get(0);
        }
        return point;
    }
    
    private List copyBranch(List branch) {
        if (branch == null)
            return null;
        List copy = new ArrayList();
        for (Iterator it = branch.iterator(); it.hasNext();) {
            List points = (List) it.next();
            List pointsCopy = copyPointList(points);
            copy.add(pointsCopy);
        }
        return copy;
    }
    
    private List<Point> copyPointList(List points) {
        List<Point> copy = new ArrayList<Point>(points.size());
        for (Iterator it = points.iterator(); it.hasNext();) {
            Point p = (Point) it.next();
            copy.add(new Point(p));
        }
        return copy;
    }
    
    private void switchNodeRenderInfo(Node source,
                                      Node target) {
        Point sourceP = source.getPosition();
        target.setPosition(new Point(sourceP));
        target.setBounds(new Rectangle(source.getBounds()));
        Rectangle textBounds = source.getTextBounds();
        if (textBounds != null) // Some nodes may don't have text bounds
            target.setTextPosition(textBounds.x, textBounds.y);
        target.setForegroundColor(source.getForegroundColor());
        target.setBackgroundColor(source.getBackgroundColor());
        // In case displayName has been changed
        String oldName = target.getDisplayName();
        target.setDisplayName(source.getDisplayName());
        RenderableRegistry.getRegistry().changeName(target, oldName);
        // Have to move all connecting info from source to target
        target.setNodeAttachmentsLocally(source.getNodeAttachments());
    }
    
    private Map<String, Renderable> indexRenderables(RenderablePathway process) {
        Map<String, Renderable> idToR = new HashMap<String, Renderable>();
        List components = process.getComponents();
        if (components == null)
            return idToR;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            String id = generateId(r, 
                                   idToR);
            idToR.put(id, r);
        }
        return idToR;
    }
    
    /**
     * Generate a customized id so that it can be compared with
     * Renderable objects in the new Project. This id is built based on
     * the container chain.
     * @param r
     * @return
     */
    private String generateId(Renderable r,
                              Map<String, Renderable> idToR) {
        StringBuilder idBuilder = new StringBuilder();
        Long dbId = r.getReactomeId();
        idBuilder.append(dbId);
        Renderable container = r;
        do {
            container = container.getContainer();
            // Skip compartment since it is not converted 
            if (container instanceof RenderableComplex) {
                dbId = container.getReactomeId();
                idBuilder.append(".").append(dbId);
            }
        }
        while (container instanceof RenderableComplex);
        String id = idBuilder.toString();
        // Some complex has multiple same components
        int c = -1;
        String tmpId = id;
        while (idToR.containsKey(tmpId)) {
            c --; // Use negative value to avoid conflict
            tmpId = id + "." + c;
        }
        return tmpId;
    }
    
}
