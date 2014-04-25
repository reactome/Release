/*
 * Created on Dec 12, 2008
 *
 */
package org.gk.elv;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.gk.database.AttributeEditManager;
import org.gk.gkCurator.authorTool.InstanceHandler;
import org.gk.gkCurator.authorTool.InstanceHandlerFactory;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.DiagramGKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.Node;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.render.RenderableRegistry;
import org.gk.util.GraphLayoutEngine;

/**
 * This helper class is used to handle pathway diagrams.
 * @author wgm
 *
 */
public class ElvDiagramHandler {
    private ElvReactionEditHelper reactionHelper;
    private ElvEWASEditHandler ewasHelper;
    private ElvComplexEditHandler complexHelper;
    
    public ElvDiagramHandler() {
        reactionHelper = new ElvReactionEditHelper();
        ewasHelper = new ElvEWASEditHandler();
        complexHelper = new ElvComplexEditHandler();
    }
    
    public RenderablePathway setDiagramForDisplay(GKInstance pathway,
                                                  InstanceZoomablePathwayEditor zoomableEditor) {
        reactionHelper.setZoomableEditor(zoomableEditor);
        ewasHelper.setZoomableEditor(zoomableEditor);
        complexHelper.setZoomableEditor(zoomableEditor);
        return getAndDisplayDiagram(pathway, zoomableEditor, zoomableEditor);
    }
    
    private RenderablePathway getAndDisplayDiagram(GKInstance pathway,
                                                   InstanceZoomablePathwayEditor zoomableEditor,
                                                   Component parentComp) {
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        RenderablePathway diagram = null;
        try {
            diagram = fileAdaptor.getDiagram(pathway);
            if (diagram != null) {
                // Need to set the diagram first so that it can be displayed. This is required
                // by ElvReactionEditHelper for display purpose, which is not good at all!!!
                if (zoomableEditor != null)
                    zoomableEditor.setDiagram(diagram);
                validateDiagram(diagram,
                                zoomableEditor, 
                                fileAdaptor);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        if (diagram == null) {
            try {
                //diagram = iRSwitcher.convertPathway(pathway, this);
                diagram = createNewDiagram(pathway, parentComp);
                if (diagram == null)
                    return null;
                fileAdaptor.addDiagram(pathway, diagram);
            }
            catch(Exception e) {
                System.err.println("ElvDiagramHandler.getDiagram(): " + e);
                e.printStackTrace();
                // Just create an empty diagram to avoid corruption of the application
                diagram = createEmptyDiagram(pathway);
            }
            if (zoomableEditor != null) {
                zoomableEditor.setDiagram(diagram);
                // Do a layout so that the edges for pathways are correctly
                // The reason why the following call should be done explicitily is that the bounds
                // for nodes have to be correctly after the above call to display the diagram.
                zoomableEditor.getPathwayEditor().layoutEdges();
            }
        }
        return diagram;
    }
    
    public RenderablePathway getDiagram(GKInstance pathway,
                                        Component parentComp) {
        return getAndDisplayDiagram(pathway, 
                                    null, 
                                    parentComp);
    }
    
    /**
     * This method is used to make sure all displayed objects should be in the local project. Otherwise,
     * objects that are local created will be deleted, and objects existed in DBs will be downloaded in shell.
     * @param diagram
     * @param parentComp
     * @param fileAdaptor
     */
    private void validateDiagram(RenderablePathway diagram,
                                 InstanceZoomablePathwayEditor zoomableEditor,
                                 XMLFileAdaptor fileAdaptor) {
        if (diagram.getComponents() == null ||
            diagram.getComponents().size() == 0)
            return;
        // In case one object has a shortcut
        List<Renderable> localObjects = new ArrayList<Renderable>();
        List<Renderable> dbObjects = new ArrayList<Renderable>();
        for (Iterator it = diagram.getComponents().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r.getReactomeId() == null)
                continue;
            Long dbId = r.getReactomeId();
            if (dbId < 0) {
                GKInstance instance = fileAdaptor.fetchInstance(dbId);
                if (instance == null)
                    localObjects.add(r);
            }
            else {
                GKInstance instance = fileAdaptor.fetchInstance(dbId);
                if (instance == null)
                    dbObjects.add(r);
            }
        }
        handleDeletedObjects(diagram, 
                             localObjects, 
                             zoomableEditor, 
                             false);
        handleDbObjects(diagram, 
                        dbObjects, 
                        zoomableEditor);
        validateDisplayNames(diagram,
                             fileAdaptor);
        validateNodeAttachments(diagram);
        validateComplexes(diagram);
        validateReactions(diagram,
                          zoomableEditor,
                          fileAdaptor);
    }
    
    /**
     * Make sure all display names should be updated in case something has been changed.
     * @param diagram
     * @param fileAdaptor
     */
    private void validateDisplayNames(RenderablePathway diagram, XMLFileAdaptor fileAdaptor) {
        for (Object obj : diagram.getComponents()) {
            Renderable r = (Renderable) obj;
            if (r.getReactomeId() == null)
                continue;
            GKInstance inst = fileAdaptor.fetchInstance(r.getReactomeId());
            if (inst != null)
                r.setDisplayName(inst.getDisplayName());
        }
        if (diagram.getHideCompartmentInNode())
            RenderUtility.hideCompartmentInNodeName(diagram);
    }
    
    private void validateComplexes(RenderablePathway diagram) {
        if (diagram.getComponents() == null || diagram.getComponents().size() == 0)
            return;
        List copy = new ArrayList(diagram.getComponents());
        for (Iterator it = copy.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof RenderableComplex) {
                RenderableComplex complex = (RenderableComplex) obj;
                complexHelper.validateDisplayedComplex(complex);
            }
        }
    }
    
    private void validateNodeAttachments(RenderablePathway diagram) {
        if (diagram.getComponents() == null || diagram.getComponents().size() == 0)
            return;
        for (Iterator it = diagram.getComponents().iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof Node) {
                Node node = (Node) obj;
                ewasHelper.validateDisplayedNodeAttachments(node);
            }
        }
    }
    
    private void validateReactions(RenderablePathway diagram,
                                   InstanceZoomablePathwayEditor zoomableEditor,
                                   XMLFileAdaptor fileAdaptor) {
        if (diagram.getComponents() == null || diagram.getComponents().size() == 0)
            return;
        try {
            // Make sure all displayed reactions are still contained by the top level pathway
            List<GKInstance> pathways = fileAdaptor.getRepresentedPathwaysInDiagram(diagram);
            Set<GKInstance> allContained = InstanceUtilities.getContainedEvents(pathways);
            List<Renderable> deleted = new ArrayList<Renderable>();
            for (Iterator<?> it = diagram.getComponents().iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof RenderableReaction ||
                    obj instanceof RenderablePathway) {
                    Renderable event = (Renderable) obj;
                    GKInstance inst = fileAdaptor.fetchInstance(event.getReactomeId());
                    if (!allContained.contains(inst))
                        deleted.add(event);
                }
            }
            if (deleted.size() > 0) {
                zoomableEditor.disableExitenceCheck(true);
                for (Renderable r : deleted)
                    zoomableEditor.getPathwayEditor().delete(r);
                zoomableEditor.disableExitenceCheck(false);
            }
        }
        catch(Exception e) {
            System.err.println("ElvDiagramHandler.validateReactions(): " + e);
            e.printStackTrace();
        }
        // To avoid ConcurrentModificationException
        List copy = new ArrayList(diagram.getComponents());
        for (Iterator it = copy.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof RenderableReaction) {
                RenderableReaction reaction = (RenderableReaction) obj;
                reactionHelper.validateReactionInDiagram(reaction);
            }
        }
    }
    
    private void handleDbObjects(RenderablePathway pathway,
                                 List<Renderable> dbObjects,
                                 Component parentComp) {
        if (dbObjects.size() == 0)
            return;
        String message = "One or more objects in the pathway diagram are not in the local project.\nThe database will be checked.";
        JOptionPane.showMessageDialog(parentComp,
                                      message,
                                      "Check Diagram",
                                      JOptionPane.INFORMATION_MESSAGE);
        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(parentComp);
        if (dba == null) {
            // Show error message
            JOptionPane.showMessageDialog(parentComp,
                                          "Database cannot be connected. Please use the diagram cautiously!",
                                          "Database Connection Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Sort based on DB_ID
        Map<Long, List<Renderable>> idToObjects = new HashMap<Long, List<Renderable>>();
        for (Renderable r : dbObjects) {
            Long dbId = r.getReactomeId();
            List<Renderable> list = idToObjects.get(dbId);
            if (list == null) {
                list = new ArrayList<Renderable>();
                idToObjects.put(dbId, list);
            }
            list.add(r);
        }
        try {
            List<Long> deleted = new ArrayList<Long>();
            for (Long dbId : idToObjects.keySet()) {
                GKInstance dbInstance = dba.fetchInstance(dbId);
                if (dbInstance == null) {
                    deleted.add(dbId);
                }
                else {
                    // Want to download these instances
                    GKInstance local = PersistenceManager.getManager().getLocalReference(dbInstance);
                    // Force to update in the entity level view
                    AttributeEditManager.getManager().attributeEdit(local, 
                                                                    ReactomeJavaConstants._displayName);
                    // Avoid isDirty flag
                    local.setIsDirty(false);
                }
            }
            // Check if there is any deleted DB instance
            if (deleted.size() > 0) {
                List<Renderable> totalDeleted = new ArrayList<Renderable>();
                for (Long dbId : deleted) {
                    List<Renderable> list = idToObjects.get(dbId);
                    totalDeleted.addAll(list);
                }
                handleDeletedObjects(pathway, 
                                     totalDeleted, 
                                     parentComp,
                                     true);
            }
        }
        catch(Exception e) {
            System.err.println("ElvDiagramHandler.handleDbObjects(): " + e);
            e.printStackTrace();
        }
    }

    private void handleDeletedObjects(RenderablePathway pathway,
                                      List<Renderable> localObjects,
                                      Component parentComp,
                                      boolean isForDB) {
        if (localObjects.size() == 0)
            return;
        // Create a set of name
        Set<String> names = new HashSet<String>();
        for (Renderable r : localObjects) {
            if (r.getDisplayName() == null)
                names.add(r.getReactomeId() + "");
            else
                names.add(r.getDisplayName());
        }
        // Local objects
        StringBuilder builder = new StringBuilder();
        if (names.size() == 1) {
            builder.append("The following " + 
                           (isForDB ? "db" : "local") + 
                           " object has been deleted. It will be removed from the digram:\n");
        }
        else
            builder.append("The following " +
                           (isForDB ? "db" : "local") + 
                           " objects have been deleted. They will be removed from the diagram:\n");
        for (Iterator<String> it = names.iterator(); it.hasNext();) {
            builder.append(it.next());
            if (it.hasNext())
                builder.append("\n");
        }
        JOptionPane.showMessageDialog(parentComp, 
                                      builder.toString(),
                                      "Check Diagram",
                                      JOptionPane.INFORMATION_MESSAGE);
        // Delete these Renderable objects
        for (Renderable r : localObjects) {
            deleteObjectFromDiagram(pathway, 
                                    r);
        }
    }

    private void deleteObjectFromDiagram(RenderablePathway pathway, Renderable r) {
        r.clearConnectWidgets();
        pathway.removeComponent(r);
        r.setContainer(null);
    }

    private RenderablePathway createEmptyDiagram(GKInstance pathway) {
        RenderablePathway diagram;
        diagram = new RenderablePathway();
        diagram.setReactomeId(pathway.getDBID());
        diagram.setDisplayName(pathway.getDisplayName());
        XMLFileAdaptor fileAdpator = PersistenceManager.getManager().getActiveFileAdaptor();
        GKInstance diagramInstance = fileAdpator.createNewInstance(ReactomeJavaConstants.PathwayDiagram);
        diagramInstance.setAttributeValueNoCheck(ReactomeJavaConstants.representedPathway, pathway);
        InstanceDisplayNameGenerator.setDisplayName(diagramInstance);
        return diagram;
    }
    
    /**
     * This method is used to create a complex new pathway diagram. In this new diagram, pathways
     * are represented as ProcessNodes, but reactions have been represented as RendearbleReactions.
     * @param pathway
     * @return
     */
    private RenderablePathway createNewDiagram(GKInstance instance,
                                               Component parentComp) throws Exception {
        RenderablePathway pathway = null;
        // Ask if the user wants to create an auto-generated diagram
        int reply = JOptionPane.showConfirmDialog(parentComp, 
                                                  "No diagram has been created for the selected pathway.\n" + 
                                                  "Do you want the tool to generate one automatically?", 
                                                  "Auto-Generate Diagram?",
                                                  JOptionPane.YES_NO_CANCEL_OPTION);
        if (reply == JOptionPane.CANCEL_OPTION)
            return null;
        pathway = createEmptyDiagram(instance);
        if (reply == JOptionPane.NO_OPTION) {
            // Need special way for the container
            return pathway;
        }
        createNewDiagram(instance, pathway);
        return pathway;
    }

    /**
     * Create a RenderablePathway by converting objects contained by passed pathway GKInstance into
     * a RenderablePathway.
     * @param instance
     * @param pathway
     * @return
     * @throws Exception
     */
    public void createNewDiagram(GKInstance instance,
                                 RenderablePathway pathway) throws Exception {
        InstanceHandlerFactory factory = InstanceHandlerFactory.getFactory();
        Set<GKInstance> events = getPathwayComponents(instance);
        GKInstance comp = null;
        InstanceHandler handler = null;
        Map<GKInstance, Renderable> iToRMap = new HashMap<GKInstance, Renderable>();
        iToRMap.put(instance, pathway);
        // First step: convert to Renderable objects
        for (Iterator it = events.iterator(); it.hasNext();) {
            comp = (GKInstance) it.next();
            handler = factory.getHandler(comp);
            if (handler == null) {
                System.err.println("ElvDiagramHandler.createNewDiagram(): Null handler for : " +
                                   comp.getSchemClass().getName());
                continue;
            }
            handler.setContainer(pathway);
            Renderable r = handler.simpleConvert(comp);
            iToRMap.put(comp, r);
            handler.setContainer(null);
        }
        // Second step: convert properties
        for (Iterator it = events.iterator(); it.hasNext();) {
            comp = (GKInstance) it.next();
            handler = factory.getHandler(comp);
            if (handler == null) {
                continue;
            }
            handler.setContainer(pathway);
            Renderable r = iToRMap.get(comp);
            handler.simpleConvertProperties(comp, 
                                            r, 
                                            iToRMap);
            handler.setContainer(null);
        }
        handler = InstanceHandlerFactory.getFactory().getHandler(instance);
        // Get the properties for pathway
        handler.setContainer(pathway);
        handler.simpleConvertProperties(instance, 
                                        pathway, 
                                        iToRMap);
        RenderableRegistry.getRegistry().clear();
        // Register all Renderable objects in the process.
        RenderableRegistry.getRegistry().registerAll(pathway);
        RenderableRegistry.getRegistry().resetNextIdFromPathway(pathway);
        // Do a random layout
        layout(pathway);
    }
    
    private void layout(RenderablePathway pathway) {
        // Find the width and height
        List components = pathway.getComponents();
        if (components == null || components.size() == 0)
            return;
        // Count node only
        int x1 = 100 + Node.getNodeWidth() / 2;
        int y1 = 100 + 50 / 2;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Node &&
                r.getContainer() instanceof RenderablePathway) {
                r.setPosition(100, 100);
                r.setBounds(new Rectangle(x1, 
                                          y1, 
                                          Node.getNodeWidth(), 
                                          50));
            }
        }
//        // Need to layout edges
//        for (Iterator it = components.iterator(); it.hasNext();) {
//            Renderable r = (Renderable) it.next();
//            if (r instanceof Node)
//                continue;
//            HyperEdge edge = (HyperEdge) r;
//            edge.layout();
//        }
        // Since the bounds for nodes are not correct, the positions for edges
        // most likely not correct and should be laid out lately.
        pathway.layout(GraphLayoutEngine.HIERARCHICAL_LAYOUT);
        // The following is used to make the bounds correct
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Node &&
                r.getContainer() instanceof RenderablePathway) {
                r.setBounds(null);
            }
        }
    }
    
    /**
     * This helper method is used to get all events contained by the specified instance.
     * This method will flatten all contained events and other displayable entities.
     * @return
     */
    private Set<GKInstance> getPathwayComponents(GKInstance instance) throws Exception {
        Set<GKInstance> objects = new HashSet<GKInstance>();
        List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        if (components == null || components.size() == 0)
            return objects;
        for (Iterator it = components.iterator(); it.hasNext();) {
            GKInstance event = (GKInstance) it.next();
            getPathwayComponents(objects, event);
        }
        return objects;
    }
    
    private void getPathwayComponents(Set<GKInstance> objects,
                                      GKInstance instance) throws Exception {
        if (instance.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
            objects.add(instance);
            Set participants = InstanceUtilities.getReactionParticipants(instance);
            objects.addAll(participants);
        }
        else if (instance.getSchemClass().isa(ReactomeJavaConstants.Interaction)) {
            objects.add(instance);
            List interactors = instance.getAttributeValuesList(ReactomeJavaConstants.interactor);
            objects.addAll(interactors);
        }
        else if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
            objects.add(instance);
            // Just want to have the first level pathway. Don't go through
        }
    }
    
    /**
     * Clone a pathway diagram.
     * @param diagram
     * @throws Exception
     */
    public RenderablePathway cloneDiagram(RenderablePathway diagram,
                                          XMLFileAdaptor fileAdaptor) throws Exception {
        // Use reader/writer to do a cloning.
        DiagramGKBWriter writer = new DiagramGKBWriter();
        Project project = new Project();
        project.setProcess(diagram);
        String xml = writer.generateXMLString(project);
        DiagramGKBReader reader = new DiagramGKBReader();
        RenderablePathway clone = reader.openDiagram(xml);
        reader.setDisplayNames(clone, 
                               fileAdaptor);
        return clone;
    }
    
}
