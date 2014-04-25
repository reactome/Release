/*
 * Created on Jan 18, 2007
 *
 */
package org.gk.gkCurator.authorTool;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.Project;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableRegistry;

public class CuratorToolToAuthorToolConverter {
    
    public CuratorToolToAuthorToolConverter() {
    }
    
    /**
     * This method is used to convert a passed pathway instance to an author tool project.
     * @param instance
     * @param parentComp
     * @return
     * @throws Exception
     */
    public Project convert(GKInstance instance,
                           Component parentComp) throws Exception {
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.Pathway))
            throw new IllegalStateException("CuratorToolToAuthorToolConverter.convert(): " +
                                            "the passed instance must be a pathway instance.");
        // Need to construct the old project first so that RenderableRegistry
        // will not be messed up.
        RenderableRegistry.getRegistry().clear();
        Project newProject = createProjectFromInstance(instance);
        PathwayDiagramHandler diagramHandler = new PathwayDiagramHandler();
        boolean retrieved = diagramHandler.retrieveStoredRenderInfo(instance,
                                                                    newProject,
                                                                    parentComp);
        if (retrieved) {
            RenderableRegistry.getRegistry().clear();
            RenderableRegistry.getRegistry().registerAll(newProject.getProcess());
        }
        return newProject;
    }
    
    /**
     * This method is used to convert a passed pathway instance to an author tool project.
     * If no curator tool coordinates could be found, returns a null project.
     * @param instance
     * @param parentComp
     * @return
     * @throws Exception
     */
    public Project convertNullReturn(GKInstance instance,
                           Component parentComp) throws Exception {
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.Pathway))
            throw new IllegalStateException("CuratorToolToAuthorToolConverter.convert(): " +
                                            "the passed instance must be a pathway instance.");
        // Need to construct the old project first so that RenderableRegistry
        // will not be messed up.
        RenderableRegistry.getRegistry().clear();
        Project newProject = null;
        try {
			newProject = createProjectFromInstance(instance);
			PathwayDiagramHandler diagramHandler = new PathwayDiagramHandler();
			boolean retrieved = diagramHandler.retrieveStoredRenderInfo(instance,
			                                                            newProject,
			                                                            parentComp);
			if (retrieved) {
				System.err.println("Found a saved diagram!");
				
			    RenderableRegistry.getRegistry().clear();
			    RenderableRegistry.getRegistry().registerAll(newProject.getProcess());
			} else
				newProject = null;
		} catch (Exception e) {
			System.err.println("Problem retrieving saved diagram!");
			e.printStackTrace(System.err);
		}
        return newProject;
    }
    
    private Project createProjectFromInstance(GKInstance instance) throws Exception {
        InstanceHandlerFactory factory = InstanceHandlerFactory.getFactory();
        Map iToRMap = new HashMap();
        Set events = getPathwayComponents(instance);
        GKInstance comp = null;
        InstanceHandler handler = null;
        // Need special way for the container
        RenderablePathway pathway = new RenderablePathway();
        // Have to manually add a name
        pathway.setDisplayName(instance.getDisplayName());
        iToRMap.put(instance, pathway);
        // First step: convert to Renderable objects
        for (Iterator it = events.iterator(); it.hasNext();) {
            comp = (GKInstance) it.next();
            handler = factory.getHandler(comp);
            if (handler == null) {
                System.err.println("Null handler for : " +
                                   comp.getSchemClass().getName());
                continue;
            }
            handler.setContainer(pathway);
            handler.convert(comp, iToRMap);
        }
        // Second step: convert properties
        for (Iterator it = events.iterator(); it.hasNext();) {
            comp = (GKInstance) it.next();
            handler = factory.getHandler(comp);
            if (handler == null) {
                continue;
            }
            handler.setContainer(pathway);
            handler.convertProperties(comp, iToRMap);
        }
        // Get the properties for pathway
        handler = factory.getHandler(instance);
        handler.setContainer(pathway);
        handler.convertProperties(instance, iToRMap);
        Project project = new Project();
        project.setProcess(pathway);
        verticalLayout(pathway);
        // Register all Renderable objects in the process.
        RenderableRegistry.getRegistry().registerAll(project.getProcess());
        return project;
    }
    
    /**
     * Do a random layout for converted pathway for easy editing.
     * @param pathway
     */
    private void verticalLayout(RenderablePathway pathway) {
        // Find the width and height
        List components = pathway.getComponents();
        // Count node only
        List<Node> nodes = new ArrayList<Node>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Node &&
                r.getContainer() instanceof RenderablePathway)
                nodes.add((Node)r);
        }
        int y = 0;
        int x = 100;
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof HyperEdge)
                continue;
            //int x = (int) (Math.random() * width);
            Rectangle bounds = r.getBounds();
            if (bounds == null) {
                y += 100;
                r.setPosition(x, y);
            }
            else {
                y += (bounds.height + 10);
                // Do a move
                Point pos = r.getPosition();
                r.move(x - pos.x,
                       y - pos.y);
            }
        }
        // Need to layout edges
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Node)
                continue;
            HyperEdge edge = (HyperEdge) r;
            edge.layout();
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
            getReactionParticipants(objects, instance);
        }
        else if (instance.getSchemClass().isa(ReactomeJavaConstants.Event)) {
            objects.add(instance);
            // Get all contained events
            List components = null;
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
                components = instance.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
                components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember))
                components = instance.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasSpecialisedForm))
                components = instance.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm);
            if (components == null || components.size() == 0)
                return;
            for (Iterator it = components.iterator(); it.hasNext();) {
                GKInstance tmp = (GKInstance) it.next();
                getPathwayComponents(objects, tmp);
            }
        }
    }
    
    private void getReactionParticipants(Set set,
                                        GKInstance reaction) throws Exception {
        Set participants = InstanceUtilities.getReactionParticipants(reaction);
        set.addAll(participants);
    }
}
