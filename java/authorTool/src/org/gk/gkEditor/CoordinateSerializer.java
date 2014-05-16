/*
 * Created on Jul 29, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.gk.database.SynchronizationManager;
import org.gk.model.DBIDNotSetException;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.GKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.ConnectWidget;
import org.gk.render.FlowLine;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Note;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderablePathway;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * This class is used to dump coordinates into the backend Reactome database.
 * @author wgm
 *
 */
public class CoordinateSerializer {
    private MySQLAdaptor dbAdaptor;
    private XMLFileAdaptor fileAdaptor;
    // Map from Renderable to Instance to create Edges
    private Map<Renderable, GKInstance> rToIMap;
    // Map from the local newly created instance to downloaded DB instance
    private Map<GKInstance, GKInstance> localToDbMap;
    // For dialog
    private Component parentComp;
    
    public CoordinateSerializer() {
    }
    
    private void resetMap() {
        if (rToIMap == null)
            rToIMap = new HashMap<Renderable, GKInstance>();
        else
            rToIMap.clear();
        if (localToDbMap == null)
            localToDbMap = new HashMap<GKInstance, GKInstance>();
        else
            localToDbMap.clear();
    }
    
    /**
     * This is the entry point to store coordinates for a project.
     * @param project
     * @param width
     * @param height
     * @param comp
     * @throws Exception
     */
    public void storeCoordinates(Project project,
                                 int width,
                                 int height,
                                 Component comp) throws Exception {
        // Need to get the database connection first
        if(!PersistenceManager.getManager().initDatabaseConnection(comp)) {
            throw new IllegalStateException("Cannot connect to the specified database.");
        }
        this.parentComp = comp;
        dbAdaptor = PersistenceManager.getManager().getActiveMySQLAdaptor();
        fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        if (fileAdaptor == null) {
            fileAdaptor = new XMLFileAdaptor();
            PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        }
        resetMap();
        storeCoordinates(project, width, height);
    }
    
    private void storeCoordinates(Project project,
                                  int width,
                                  int height) throws Exception {
        RenderablePathway container = project.getProcess();
        GKInstance pathwayDiagram = creatPathwayDiagram(container, 
                                                        width, 
                                                        height);
        GKBWriter writer = new GKBWriter();
        // Attach XML to this pathwayDiagram
        String xml = writer.generateXMLString(project);
        pathwayDiagram.setAttributeValue(ReactomeJavaConstants.storedATXML,
                                         xml);
        // Go through each displayed objects and save them
        List<Renderable> components = container.getComponents();
        if (components != null) {
        	// Have to hold edges till all nodes have been converted
        	List<HyperEdge> edges = new ArrayList<HyperEdge>();
        	for (Renderable r : components) {
        		if (avoidVertexGeneration(r))
        			continue;
        		if (r instanceof Node)
        			convertNodeToInstance((Node)r, pathwayDiagram);
        		else if (r instanceof HyperEdge)
        			edges.add((HyperEdge)r);
        	}
        	for (HyperEdge edge : edges)
        		convertEdgeToInstance(edge, pathwayDiagram);
        }
        saveToDatabase(pathwayDiagram);
    }
    
    private void saveToDatabase(GKInstance pathwayDiagram) throws Exception {
        List<GKInstance> instances = fetchCoordinateInstances(pathwayDiagram);
        // Need to reset fileAdpator to remove dirty flag
        if (fileAdaptor.getSourceName() != null)
            fileAdaptor.save();
        else {
            String tmpFileName = GKApplicationUtilities.getReactomeDir() + File.separator + "StoredCoordinates.rtpj";
            fileAdaptor.save(tmpFileName);
        }
        SynchronizationManager manager = SynchronizationManager.getManager();
        Window window = null;
        if (parentComp instanceof Window) 
            window = (Window) parentComp;
        else
            window = (Window) SwingUtilities.getAncestorOfClass(Window.class, parentComp);
        List list = manager.commitToDB(instances,
                                       fileAdaptor, 
                                       dbAdaptor, 
                                       true, 
                                       window);
        if (list == null || list.size() == 0)
            throw new IllegalStateException("No coordinates have been saved into the database.");
    }
    
    private List<GKInstance> fetchCoordinateInstances(GKInstance pathwayDiagram) throws Exception {
        // Need to search if this pathway has been in the database
        GKInstance pathway = (GKInstance) pathwayDiagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
        // Get the database version of pathway diagram
        // Only DB_ID from pathway will be used for db query so a local instance can be used
        // for the following query.
        Collection found = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram, 
                                                              ReactomeJavaConstants.representedPathway, 
                                                              "=",
                                                              pathway);
        if (found == null || found.size() == 0) {
            // This is a new saving
            List<GKInstance> toBeSaved = getInstancesToBeSaved(pathwayDiagram);
            return toBeSaved;
        }
        // This pathway has been drawn before. Need to replace information in the GKInstance
        // with the current information.
        GKInstance dbPathwayDiagram = (GKInstance) found.iterator().next();
        switchNewPathwayDiagramToDb(pathwayDiagram, 
                                    dbPathwayDiagram);
        // Get all instances represented for this pathway diagram
        found = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Vertex,
                                                   ReactomeJavaConstants.pathwayDiagram,
                                                   "=",
                                                   dbPathwayDiagram);
        // For performance gain
        dbAdaptor.loadInstanceAttributeValues(found);
        Collection local = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Vertex,
                                                                ReactomeJavaConstants.pathwayDiagram,
                                                                "=",
                                                                pathwayDiagram);
        matchAndSwitchNewVertexToDb(local, found);
        // Need to handle Edge instances
        Collection dbEdges = dbAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Edge, 
                                                                ReactomeJavaConstants.pathwayDiagram,
                                                                "=",
                                                                dbPathwayDiagram);
        dbAdaptor.loadInstanceAttributeValues(dbEdges);
        Collection localEdges = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Edge, 
                                                                     ReactomeJavaConstants.pathwayDiagram,
                                                                     "=",
                                                                     pathwayDiagram);
        handleEdges(localEdges, dbEdges);
        cleanUpLocalInstances(pathwayDiagram);
        // Collect all instances that should be saved
        GKInstance dbToLocal = localToDbMap.get(pathwayDiagram);
        List<GKInstance> toBeSaved = getInstancesToBeSaved(dbToLocal);
        return toBeSaved;
    }
    
    private List<GKInstance> getInstancesToBeSaved(GKInstance pathwayDiagram) throws Exception {
        // Collect all instances that should be saved
        List<GKInstance> toBeSaved = new ArrayList<GKInstance>();
        toBeSaved.add(pathwayDiagram);
        Collection list = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Vertex,
                                                               ReactomeJavaConstants.pathwayDiagram,
                                                               "=",
                                                               pathwayDiagram);
        if (list != null)
            toBeSaved.addAll(list);
        list = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Edge, 
                                                    ReactomeJavaConstants.pathwayDiagram,
                                                    "=", 
                                                    pathwayDiagram);
        if (list != null)
            toBeSaved.addAll(list);
        return toBeSaved;
    }
    
    private void cleanUpLocalInstances(GKInstance localPathwayDiagram) throws Exception {
        // Need to replace any new Vertex with localPathwayDiagram so that the local
        // PathwayDiagram can be deleted
        List<GKInstance> toBeReplaced = new ArrayList<GKInstance>();
        Collection list = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Vertex, 
                                                               ReactomeJavaConstants.pathwayDiagram,
                                                               "=", 
                                                               localPathwayDiagram);
        if (list != null)
            toBeReplaced.addAll(list);
        list = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.Edge,
                                                    ReactomeJavaConstants.pathwayDiagram, 
                                                    "=", 
                                                    localPathwayDiagram);
        if (list != null)
            toBeReplaced.addAll(list);
        if (toBeReplaced.size() > 0) {
            GKInstance dbToLocal = localToDbMap.get(localPathwayDiagram);
            for (GKInstance instance : toBeReplaced) {
                instance.setAttributeValue(ReactomeJavaConstants.pathwayDiagram, 
                                           dbToLocal);
            }
        }
        for (GKInstance local : localToDbMap.keySet()) 
            fileAdaptor.deleteInstance(local);
    }
    
    private void handleEdges(Collection localEdges,
                             Collection dbEdges) throws Exception {
        Map<String, GKInstance> dbEdgeIndex = indexEdges(dbEdges);
        // Replace local reference in edges with db ones
        for (Iterator it = localEdges.iterator(); it.hasNext();) {
            GKInstance edge = (GKInstance) it.next();
            GKInstance sourceVertex = (GKInstance) edge.getAttributeValue(ReactomeJavaConstants.sourceVertex);
            GKInstance dbSource = localToDbMap.get(sourceVertex);
            if (dbSource != null)
                edge.setAttributeValue(ReactomeJavaConstants.sourceVertex, dbSource);
            GKInstance targetVertex = (GKInstance) edge.getAttributeValue(ReactomeJavaConstants.targetVertex);
            GKInstance dbTarget = localToDbMap.get(targetVertex);
            if (dbTarget != null)
                edge.setAttributeValue(ReactomeJavaConstants.targetVertex, dbTarget);
            // Check if local Edge can be replaced by db edge
            if (dbSource != null && dbTarget != null) {
                String key = dbSource.getDBID() + "-" + dbTarget.getDBID();
                GKInstance dbEdge = dbEdgeIndex.get(key);
                if (dbEdge == null)
                    continue; // For some old database schema
                switchNewLocalToDb(edge, 
                                   dbEdge, 
                                   ReactomeJavaConstants.edgeType,
                                   ReactomeJavaConstants.pointCoordinates);
            }
        }
    }
    
    private Map<String, GKInstance> indexEdges(Collection edges) throws Exception {
        Map<String, GKInstance> keyToEdge = new HashMap<String, GKInstance>();
        for (Iterator it = edges.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            GKInstance source = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.sourceVertex);
            GKInstance target = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.targetVertex);
            keyToEdge.put(source.getDBID() + "-" + target.getDBID(),
                          instance);
        }
        return keyToEdge;
    }
    
    private void matchAndSwitchNewVertexToDb(Collection localInstances,
                                             Collection dbInstances) throws Exception {
        if (dbInstances == null || dbInstances.size() == 0)
            return;
        // Need to map new instance to dbInstance
        // For performance reason, use a map
        Map<Long, GKInstance> localRepToInst = new HashMap<Long, GKInstance>();
        for (Iterator it = localInstances.iterator(); it.hasNext();) {
            GKInstance local = (GKInstance) it.next();
            GKInstance rep = (GKInstance) local.getAttributeValue(ReactomeJavaConstants.representedInstance);
            localRepToInst.put(rep.getDBID(), local);
        }
        Map<GKInstance, GKInstance> localToDbVertex = new HashMap<GKInstance, GKInstance>();
        for (Iterator it = dbInstances.iterator(); it.hasNext();) {
            GKInstance db = (GKInstance) it.next();
            GKInstance dbRep = (GKInstance) db.getAttributeValue(ReactomeJavaConstants.representedInstance);
            GKInstance local = (GKInstance) localRepToInst.get(dbRep.getDBID());
            if (db.getSchemClass().isa(ReactomeJavaConstants.EntityVertex))
                switchNewEntityVertexToDb(local, db);
            else if (db.getSchemClass().isa(ReactomeJavaConstants.ReactionVertex)) 
                switchNewReactionVertexToDb(local, db);
        }
    }
    
    private void switchNewReactionVertexToDb(GKInstance local,
                                             GKInstance dbInstance) throws Exception {
        switchNewLocalToDb(local, 
                           dbInstance,
                           ReactomeJavaConstants.x,
                           ReactomeJavaConstants.y,
                           ReactomeJavaConstants.pointCoordinates);
    }
    
    private void switchNewEntityVertexToDb(GKInstance local,
                                           GKInstance dbInstance) throws Exception {
        switchNewLocalToDb(local, 
                           dbInstance,
                           ReactomeJavaConstants.x,
                           ReactomeJavaConstants.y,
                           ReactomeJavaConstants.width,
                           ReactomeJavaConstants.height);
    }
    
    private void switchNewPathwayDiagramToDb(GKInstance local,
                                             GKInstance dbInstance) throws Exception {
        switchNewLocalToDb(local, 
                           dbInstance, 
                           ReactomeJavaConstants.width,
                           ReactomeJavaConstants.height,
                           ReactomeJavaConstants.storedATXML);
    }
    
    private void switchNewLocalToDb(GKInstance local,
                                    GKInstance dbInstance,
                                    String... attNames) throws Exception {
        // We have a fully loaded database instance
        GKInstance dbToLocal = SynchronizationManager.getManager().checkOutShallowly(dbInstance);
        // Need to copy local information 
        for (String tmp : attNames) {
            Object value = local.getAttributeValue(tmp);
            dbToLocal.setAttributeValue(tmp, value);
        }
        dbToLocal.setIsDirty(true);
        localToDbMap.put(local, dbToLocal);
    }
    
    /**
     * This helper method is used to convert a Node to a GKInstance to store
     * coordinates.
     * @param node
     */
    private void convertNodeToInstance(Node node,
                                       GKInstance pathwayDiagram) throws Exception {
        GKInstance instance = createInstance(node, 
                                             ReactomeJavaConstants.EntityVertex,
                                             pathwayDiagram);
        // Need to get coordinates and width and size
        Rectangle bounds = node.getBounds();
        instance.setAttributeValue(ReactomeJavaConstants.x, bounds.x);
        instance.setAttributeValue(ReactomeJavaConstants.y, bounds.y);
        instance.setAttributeValue(ReactomeJavaConstants.width, bounds.width);
        instance.setAttributeValue(ReactomeJavaConstants.height, bounds.height);
    }
    
    public GKInstance convertNodeToInstance(Node node,
                                            GKInstance pathwayDiagram,
                                            MySQLAdaptor dba) throws Exception {
        String cls = null;
        if (node instanceof ProcessNode)
            cls = ReactomeJavaConstants.PathwayVertex;
        else
            cls = ReactomeJavaConstants.EntityVertex;
        GKInstance instance = createInstance(node, 
                                             cls,
                                             pathwayDiagram,
                                             dba);
        // Need to get coordinates and width and size
        Rectangle bounds = node.getBounds();
        instance.setAttributeValue(ReactomeJavaConstants.x, bounds.x);
        instance.setAttributeValue(ReactomeJavaConstants.y, bounds.y);
        instance.setAttributeValue(ReactomeJavaConstants.width, bounds.width);
        instance.setAttributeValue(ReactomeJavaConstants.height, bounds.height);
        return instance;
    }

    private GKInstance createInstance(Renderable r, 
                                      String clsName,
                                      GKInstance pathwayDiagram) throws Exception {
        GKInstance instance = createInstance(clsName);
        rToIMap.put(r, instance);
        GKInstance wrapped = getWrappedDbInstance(r, dbAdaptor);
        instance.setAttributeValue(ReactomeJavaConstants.representedInstance, wrapped);
        instance.setAttributeValue(ReactomeJavaConstants.pathwayDiagram, pathwayDiagram);
        return instance;
    }
    
    private GKInstance createInstance(Renderable r,
                                      String clsName,
                                      GKInstance pathwayDiagram,
                                      MySQLAdaptor dba) throws Exception {
        GKInstance instance = createInstance(clsName, dba);
        GKInstance wrapped = getWrappedDbInstance(r, dba);
        instance.setAttributeValue(ReactomeJavaConstants.representedInstance, wrapped);
        instance.setAttributeValue(ReactomeJavaConstants.pathwayDiagram, pathwayDiagram);
        return instance;
    }
    
    /**
     * This helper method is used to convert a HyperEdge to GKInstances to store
     * coordinates for a HyperEdge. There are maybe several different kinds of
     * GKInstances generated.
     * @param edge
     */
    private void convertEdgeToInstance(HyperEdge edge,
                                       GKInstance pathwayDiagram) throws Exception {
        // This type of edge will not be processed
        if (edge instanceof FlowLine && 
            edge.getReactomeId() == null)
            return;
        GKInstance instance = createInstance(edge,
                                             ReactomeJavaConstants.ReactionVertex,
                                             pathwayDiagram);
        Point position = edge.getPosition();
        instance.setAttributeValue(ReactomeJavaConstants.x, position.x);
        instance.setAttributeValue(ReactomeJavaConstants.y, position.y);
        // Generate value for backbone points
        List backbone = edge.getBackbonePoints();
        String pointString = convertPointsToString(backbone);
        instance.setAttributeValue(ReactomeJavaConstants.pointCoordinates, 
                                   pointString);
        // Need to figure out all edges
        convertReactionToEdges(edge,
                               instance,
                               pathwayDiagram);
    }
    
    public List<GKInstance> convertEdgeToInstances(HyperEdge edge,
                                                   GKInstance pathwayDiagram,
                                                   MySQLAdaptor dba,
                                                   Map<Renderable, GKInstance> rToIMap) throws Exception {
        // This type of edge will not be processed
        if (edge instanceof FlowLine || 
            edge.getReactomeId() == null)
            return null;
        List<GKInstance> rtn = new ArrayList<GKInstance>();
        GKInstance instance = createInstance(edge,
                                             ReactomeJavaConstants.ReactionVertex,
                                             pathwayDiagram,
                                             dba);
        rtn.add(instance);
        Point position = edge.getPosition();
        instance.setAttributeValue(ReactomeJavaConstants.x, position.x);
        instance.setAttributeValue(ReactomeJavaConstants.y, position.y);
        // Generate value for backbone points
        List backbone = edge.getBackbonePoints();
        String pointString = convertPointsToString(backbone);
        instance.setAttributeValue(ReactomeJavaConstants.pointCoordinates, 
                                   pointString);
        // Need to figure out all edges
        convertReactionToEdges(edge,
                               instance,
                               pathwayDiagram,
                               dba,
                               rToIMap,
                               rtn);
        return rtn;
    }
    
    private String convertPointsToString(List points) {
        StringBuilder builder = new StringBuilder();
        for (Iterator it = points.iterator(); it.hasNext();) {
            Point p = (Point) it.next();
            builder.append(p.x).append(" ").append(p.y);
            if (it.hasNext())
                builder.append(", ");
        }
        return builder.toString();
    }
    
    private void convertReactionToEdges(HyperEdge reaction,
                                        GKInstance reactionVertex,
                                        GKInstance pathwayDiagram) throws Exception {
        convertReactionToEdges(reaction,
                               reactionVertex, 
                               pathwayDiagram, 
                               null,
                               rToIMap,
                               null);
    }
    
    private void convertReactionToEdges(HyperEdge reaction,
                                        GKInstance reactionVertex,
                                        GKInstance pathwayDiagram,
                                        MySQLAdaptor dba,
                                        Map<Renderable, GKInstance> rToIMap,
                                        List<GKInstance> list) throws Exception {
        List widgets = reaction.getConnectInfo().getConnectWidgets();
        // Just in case
        if (widgets == null || widgets.size() == 0)
            return;
        for (Iterator it = widgets.iterator(); it.hasNext();) {
            ConnectWidget widget = (ConnectWidget) it.next();
            Node node = widget.getConnectedNode();
            if (node == null)
                continue;
            GKInstance nodeVertex = rToIMap.get(node);
            int role = widget.getRole();
            List points = null;
            List branches = reaction.getBranchFromType(role);
            if (branches != null && branches.size() > 0) {
                points = (List) branches.get(widget.getIndex());
            }
            if (role == HyperEdge.OUTPUT) {
                // Should use reaction as the source
                GKInstance edgeInstance = createEdgeToReaction(reactionVertex, 
                                                               nodeVertex,
                                                               role,
                                                               points, 
                                                               pathwayDiagram,
                                                               dba);
                if (list != null)
                    list.add(edgeInstance);
            }
            else {
                // Use reaction as the target
                GKInstance edgeInstance = createEdgeToReaction(nodeVertex,
                                                               reactionVertex,
                                                               role, 
                                                               points, 
                                                               pathwayDiagram,
                                                               dba);
                if (list != null)
                    list.add(edgeInstance);
            }
        }
    }

    private GKInstance createEdgeToReaction(GKInstance sourceVertex, 
                                            GKInstance targetVertex,
                                            int type,
                                            List points,
                                            GKInstance pathwayDiagram,
                                            MySQLAdaptor dba)  throws Exception {
        GKInstance edgeInstance = null;
        if (dba != null) 
            edgeInstance = createInstance(ReactomeJavaConstants.Edge, 
                                         dba);
        else // For local instance
            edgeInstance = createInstance(ReactomeJavaConstants.Edge);
        edgeInstance.setAttributeValue(ReactomeJavaConstants.sourceVertex,
                                       sourceVertex);
        edgeInstance.setAttributeValue(ReactomeJavaConstants.targetVertex,
                                       targetVertex);
        edgeInstance.setAttributeValue(ReactomeJavaConstants.pathwayDiagram, 
                                       pathwayDiagram);
        edgeInstance.setAttributeValue(ReactomeJavaConstants.edgeType, 
                                       type);
        if (points != null && points.size() > 0) {
            String pointString = convertPointsToString(points);
            edgeInstance.setAttributeValue(ReactomeJavaConstants.pointCoordinates, 
                                           pointString);
        }
        return edgeInstance;
    }
    
    public boolean avoidVertexGeneration(Renderable r) {
        // Don't output compartments, notes and pathways
        if (r instanceof RenderableCompartment ||
            r instanceof Note ||
            r instanceof RenderablePathway)
            return true;
        // Don't output a hidden object
        if (!r.isVisible())
            return true;
        return false;
    }

    private GKInstance creatPathwayDiagram(RenderablePathway container, 
                                           int width,
                                           int height) throws Exception {
        GKInstance pathwayDiagram = createInstance(ReactomeJavaConstants.PathwayDiagram);
        // Need to get the correct pathway
        GKInstance dbInstance = getWrappedDbInstance(container, dbAdaptor);
        pathwayDiagram.setAttributeValue(ReactomeJavaConstants.representedPathway, 
                                         dbInstance);
        pathwayDiagram.setAttributeValue(ReactomeJavaConstants.width,
                                         width);
        pathwayDiagram.setAttributeValue(ReactomeJavaConstants.height,
                                         height);
        return pathwayDiagram;
    }
    
    private GKInstance createInstance(String clsName) {
        GKInstance instance = fileAdaptor.createNewInstance(clsName);
        return instance;
    }
    
    /**
     * Create a DB Instance. 
     * Note: DB_ID has not been assigned.
     * @param clsName
     * @param dba
     * @return
     */
    private GKInstance createInstance(String clsName,
                                      MySQLAdaptor dba) {
        SchemaClass cls = dba.getSchema().getClassByName(clsName);
        GKInstance instance = new GKInstance(cls);
        instance.setDbAdaptor(dba);
        return instance;
    }
    
    private GKInstance getWrappedDbInstance(Renderable r,
                                            MySQLAdaptor dba) throws Exception {
        Long dbId = r.getReactomeId();
        if (dbId == null)
            throw new DBIDNotSetException(r.getType() + ": " + r.getDisplayName() + " doesn't have DB_ID.");
        GKInstance dbInstance = dba.fetchInstance(dbId);
        if (dbInstance == null) {
            //throw new InstanceNotFoundException(dbId);
            // This is just a temporary solution to make the deployment work. There should be a way
            // to check if a diagram contains an instance that has been deleted.
            return null;
        }
        if (dba != dbAdaptor)
            return dbInstance; // called from outside.
        GKInstance localInstance = PersistenceManager.getManager().getLocalReference(dbInstance);
        return localInstance;
    }
}
