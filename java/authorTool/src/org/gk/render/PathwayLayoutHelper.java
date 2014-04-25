/*
 * Created on Jan 12, 2007
 *
 */
package org.gk.render;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.util.Graph;
import org.gk.util.GraphLayoutEngine;
import org.gk.util.Vertex;

/**
 * A helper class to layout a RenderablePathway.
 * @author guanming
 *
 */
public class PathwayLayoutHelper {
    // Used as average node size
    private final Dimension EMPTY_SIZE = new Dimension(100, 40);
    // used for size for point
    private final Dimension POINT_SIZE = new Dimension(40, 10);
    private RenderablePathway pathway;
    
    public PathwayLayoutHelper() {
        
    }
    
    public PathwayLayoutHelper(RenderablePathway pathway) {
        setPathway(pathway);
    }
    
    public void setPathway(RenderablePathway pathway) {
        this.pathway = pathway;
    }
    
    /**
     * Layout RenderablePathway automatically.
     * @param type one of constants from GraphLayoutEngine:     
     * HIERARCHICAL_LAYOUT, FORCE_DIRECTED_LAYOUT.
     */
    public void layout(int type) {
        List components = pathway.getComponents();
        // No need to do layout
        if (components == null || components.size() < 2)
            return;
        // Convert RenderablePathway to a generic Graph
        Graph graph = new Graph();
        Map<Node, Vertex> nodeMap = new HashMap<Node, Vertex>(); // Map from Renderables to Vertices
        // Keep the old coordinates
        Map<Node, Point> oldPositions = new HashMap<Node, Point>();
        // Convert Node to graph Vertex first
        List<HyperEdge> hyperEdges = new ArrayList<HyperEdge>();
        // Only layout the first level components
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable renderable = (Renderable) it.next();
            // Some nodes maybe contained by a sub-pathway. So here
            // just check if a container is a complex. Don't layout
            // complex component here
            if (renderable.getContainer() instanceof RenderableComplex)
                continue;
            // Will not handle pathway right now
            if (renderable instanceof RenderablePathway ||
                renderable instanceof Note ||
                renderable instanceof RenderableCompartment)
                continue;
            if (renderable instanceof Node) {
                Node node = (Node) renderable;
                Vertex vertex = convertNodeToVertext(node);
                if (vertex == null)
                    continue; // Pathway used as container
                graph.addVertex(vertex);
                nodeMap.put(node, vertex);
                // Need to make a copy since objects are directly manipulated
                oldPositions.put(node, new Point(node.getPosition()));
            }
            else 
                hyperEdges.add((HyperEdge)renderable);
        }
        // Convert HyperEdges to simple Edges
        int edgeCount = -1;
        int vertextCount = 0;
        for (HyperEdge edge : hyperEdges) {
            edgeCount ++;
            convertHyperEdge(edge, edgeCount, graph, nodeMap);
        }
        // Do layout
//        if (GKApplicationUtilities.isDeployed)
//            new HierarchicalLayout().layout(graph);
//        else
        GraphLayoutEngine.getEngine().layout(graph, type);
        // Don't need to extract the coordinates. The positions changed in layout
        // will be kept since the Point objects are passed to the layout implementation.
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable renderable = (Renderable) it.next();
            if (renderable instanceof Node) {
                validateNodePosition((Node)renderable, 
                                     oldPositions,
                                     nodeMap);
            }
        }
        // Handle complexes
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableComplex &&
                r.getComponents() != null &&
                r.getComponents().size() > 0) {
                ((RenderableComplex)r).layout();
            }
        }
        for (HyperEdge edge : hyperEdges) {
            //List backbonePoints = edge.getBackbonePoints();
            //if (backbonePoints.size() < 3)
            // Layout every edge so that all added bending points will not be left
            // out. This is a simple approach. It will be better to convert adding
            // bending points during converting to graph. Right now, they are not!
            edge.layout();
            // Need to make sure all connecting information is correct
            edge.validateConnectInfo();
            edge.validateBounds();
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
    }
    
    private void validateNodePosition(Node node,
                                      Map<Node, Point> oldPositions,
                                      Map<Node, Vertex> nodeToVertex) {
        if (!nodeToVertex.containsKey(node))
            return;
        Point newPos = node.getPosition();
        Point oldPos = oldPositions.get(node);
        int dx = newPos.x - oldPos.x;
        int dy = newPos.y - oldPos.y;
        node.move(dx, dy);
    }
    
    private void validateComplexBounds(RenderableComplex complex) {
        // Components of Complex are not handled in layout. Have to handle manually here
        Point currentPos = new Point(complex.getPosition());
        complex.setBoundsFromComponents();
        Point oldPos = complex.getPosition();
        int dx = currentPos.x - oldPos.x;
        int dy = currentPos.y - oldPos.y;
        complex.move(dx, dy);
    }
    
    private void convertHyperEdge(HyperEdge edge,
                                  int edgeCount,
                                  Graph graph,
                                  Map nodeMap) {
        java.util.List inputNodes = edge.getInputNodes();
        java.util.List outputNodes = edge.getOutputNodes();
        if (edge instanceof FlowLine) { // Only for flow line
            if (inputNodes.size() == 1 && outputNodes.size() == 1) {
                Vertex v1 = (Vertex)nodeMap.get(inputNodes.get(0));
                Vertex v2 = (Vertex)nodeMap.get(outputNodes.get(0));
                graph.addEdge(v1, v2);
                return ;
            }
            // All flow lines should have no empty end points
        }
        java.util.List helperNodes = new ArrayList();
        helperNodes.addAll(edge.getHelperNodes());
        helperNodes.addAll(edge.getInhibitorNodes());
        helperNodes.addAll(edge.getActivatorNodes());
        int vertextCount = 0;
        // Convert the position
        Point p = edge.getPosition();
        Vertex edgeV = convertPointToVertex(p, 
                                            edgeCount + "_" + vertextCount++);
        graph.addVertex(edgeV);
        Vertex inputV = null;
        if (inputNodes.size() < 2) {
            inputV = edgeV;
        }
        else {
            // Convert input branches
            p = edge.getInputHub();
            // Create an empty node
            inputV = convertPointToVertex(p,
                                          edgeCount + "_" + vertextCount++);
            graph.addVertex(inputV);
            graph.addEdge(inputV, edgeV);
        }
        if (inputNodes.size() > 0) {
            for (Iterator it1 = inputNodes.iterator(); it1.hasNext();) {
                Renderable r = (Renderable) it1.next();
                Vertex v1 = (Vertex) nodeMap.get(r);
                graph.addEdge(v1, inputV);
            }
        }
        Vertex outputV = null;
        if (outputNodes.size() < 2) {
            outputV = edgeV;
        }
        else {
            // Convert output branches
            p = edge.getOutputHub();
            outputV = convertPointToVertex(p, 
                                           edgeCount + "_" + vertextCount++);
            graph.addVertex(outputV);
            graph.addEdge(edgeV, outputV);
        }
        if (outputNodes.size() > 0){
            for (Iterator it1 = outputNodes.iterator(); it1.hasNext();) {
                Vertex v1 = (Vertex) nodeMap.get(it1.next());
                graph.addEdge(outputV, v1);
            }
        }
        // Convert helper branches
        if (helperNodes != null && helperNodes.size() > 0) {
            for (Iterator it1 = helperNodes.iterator(); it1.hasNext();) {
                Vertex v1 = (Vertex) nodeMap.get(it1.next());
                graph.addEdge(v1, edgeV);
            }
        }
    }
    
    private Vertex convertNodeToVertext(Node node) {
        // Escape Pathway as Container
        if (node instanceof RenderablePathway)
            return null;
        Vertex vertex = new Vertex();
        // Name might be the same in case shortcuts. Have to add ids
        vertex.setName(node.getDisplayName() + "[" + node.getID() + "]");
        vertex.setPosition(node.getPosition());
        if (node.getBounds() == null)
            vertex.setSize(EMPTY_SIZE);
        else
            vertex.setSize(node.getBounds().getSize());
        return vertex;
    }
    
    private Vertex convertPointToVertex(Point p, String name) {
        Vertex v = new Vertex();
        v.setName(name);
        v.setPosition(p);
        v.setSize(POINT_SIZE);
        return v;
    }
    
}
