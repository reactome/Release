/*
 * Created on Sep 19, 2008
 *
 */
package org.reactome.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.gkCurator.authorTool.CuratorToolToAuthorToolConverter;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.GKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.schema.InvalidAttributeException;
import org.junit.Test;

/**
 * This class is used to get coordinates from the pathway_diagram database generated
 * by Imre to see if we can use these coordinates to speed up the layout.
 * @author wgm
 *
 */
public class AuthorToolProjectTest {
    private MySQLAdaptor dba;
    
    public AuthorToolProjectTest() {
    }
    
    public MySQLAdaptor getDBA() throws Exception {
        if (dba == null) {
            dba = new MySQLAdaptor("localhost",
                                   "reactome_25_pathway_diagram",
                                   "root",
                                   "macmysql01",
                                   3306);
        }
        return dba;
    }
    
    @Test
    public void exportPathwayIntoATProject() throws Exception {
        MySQLAdaptor dba = getDBA();
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        // This is used to test human apoptosis
        Long dbId = 109581L;
        GKInstance pathway = dba.fetchInstance(dbId);
        Collection collection = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram, 
                                                             ReactomeJavaConstants.representedPathway,
                                                             "=", 
                                                             pathway);
        GKInstance pathwayDiagram = (GKInstance) collection.iterator().next();
        // Need to get vertex
        // Create a map to speed up search
        Map<Long, GKInstance> idToVertex = loadVertex(pathwayDiagram, dba);
        CuratorToolToAuthorToolConverter converter = new CuratorToolToAuthorToolConverter();
        Project project = converter.convert(pathway, null);
        // Just a quick check
        System.out.println("Project name: " + project.getProcess().getDisplayName());
        // This is used to copy the coordinates from vertex
        List<Node> notMapped = extractCoordinates(idToVertex, project);
        // Want to delete Nodes that don't have any coordinates
        deleteNodes(project, notMapped);
        // Output the converted project
        String fileName = "tmp/Apoptosis.gkb";
        GKBWriter writer = new GKBWriter();
        writer.save(project, fileName);
    }
    
    private void deleteNodes(Project project,
                             List<Node> toBeDeleted) {
        // Borrow the PathwayEditor to do deletion
        PathwayEditor editor = new PathwayEditor();
        editor.setRenderable(project.getProcess());
        editor.setSelection(toBeDeleted);
        editor.deleteSelection();
    }

    private List<Node> extractCoordinates(Map<Long, GKInstance> idToVertex,
                                    Project project)
            throws InvalidAttributeException, Exception {
        List components = project.getProcess().getComponents();
        List<HyperEdge> hyperEdges = new ArrayList<HyperEdge>();
        List<Node> notMapped = new ArrayList<Node>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof HyperEdge) {
                hyperEdges.add((HyperEdge)r);
                continue;
            }
            Long rId = r.getReactomeId();
            GKInstance vertex = idToVertex.get(rId);
            if (vertex == null) {
                notMapped.add((Node)r);
                continue;
            }
            // Extract coordinate
            int x0 = (Integer) vertex.getAttributeValue(ReactomeJavaConstants.x);
            int y0 = (Integer) vertex.getAttributeValue(ReactomeJavaConstants.y);
            if (r.getBounds() == null)
                r.setPosition(x0, y0);
            else {
                int x1 = r.getPosition().x;
                int y1 = r.getPosition().y;
                int dx = x0 - x1;
                int dy = y0 - y1;
                r.move(dx, dy);
            }
        }
        // The following code is copied from org.gk.render.PahtwayLayoutHelper
        for (HyperEdge edge : hyperEdges) {
            //List backbonePoints = edge.getBackbonePoints();
            //if (backbonePoints.size() < 3)
            // Layout every edge so that all added bending points will not be left
            // out. This is a simple approach. It will be better to convert adding
            // bending points during converting to graph. Right now, they are not!
                edge.layout();
        }
        // Do a sanity check for edges
        for (Iterator it = hyperEdges.iterator(); it.hasNext();) {
            HyperEdge edge = (HyperEdge) it.next();
            List inputs = edge.getInputNodes();
            if (inputs == null || inputs.size() == 0) {
                edge.setInputHub(edge.getPosition());
            }
            List outputs = edge.getOutputNodes();
            if (outputs == null || outputs.size() == 0) {
                edge.setOutputHub(edge.getPosition());
            }
        }
        return notMapped;
    }
    
    private Map<Long, GKInstance> loadVertex(GKInstance pathwayDiagram,
                                             MySQLAdaptor dba) throws Exception {
        // This is used to test human apoptosis
        // Need to get vertex
        Collection collection = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Vertex, 
                                                  ReactomeJavaConstants.pathwayDiagram,
                                                  "=", 
                                                  pathwayDiagram);
        System.out.println("Total vertex: " + collection.size());
        dba.loadInstanceAttributeValues(collection);
        // Create a map to speed up search
        Map<Long, GKInstance> idToVertex = new HashMap<Long, GKInstance>();
        for (Iterator it = collection.iterator(); it.hasNext();) {
            GKInstance vertex = (GKInstance) it.next();
            GKInstance represented = (GKInstance) vertex.getAttributeValue(ReactomeJavaConstants.representedInstance);
            idToVertex.put(represented.getDBID(),
                           vertex);
        }
        return idToVertex;
    }
    
}
