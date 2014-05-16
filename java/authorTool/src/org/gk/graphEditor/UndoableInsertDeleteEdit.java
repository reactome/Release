/*
 * Created on Aug 15, 2008
 *
 */
package org.gk.graphEditor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.gk.render.ConnectWidget;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableRegistry;

public class UndoableInsertDeleteEdit extends UndoableGraphEdit {
    // To main container-contained relationship
    private Map<Renderable, Renderable> rToContainer;
    private Map<HyperEdge, EdgeNodeInformation> edgeNodeInfo;
    private Map<Node, NodeEdgeInformation> nodeEdgeInfo;
    private List<Renderable> objects;
    
    protected void init(GraphEditorPane graphPane,
                      List<Renderable> objects) {
        this.graphPane = graphPane;
        this.objects = objects;
        storeEdgeNodeInfo();
        storeNodeEdgeInfo();
        storeContainerInfo();
    }
    
    private void storeContainerInfo() {
        rToContainer = new HashMap<Renderable, Renderable>();
        storeContainerInfo(rToContainer, objects);
    }
    
    private void storeEdgeNodeInfo() {
        if (edgeNodeInfo == null)
            edgeNodeInfo = new HashMap<HyperEdge, EdgeNodeInformation>();
        for (Renderable r : objects) {
            if (r instanceof HyperEdge) {
                HyperEdge edge = (HyperEdge) r;
                EdgeNodeInformation info = generateEdgeNodeInfo(edge);
                edgeNodeInfo.put(edge,
                                 info);
            }
        }
    }
    
    private void storeNodeEdgeInfo() {
        if (nodeEdgeInfo == null)
            nodeEdgeInfo = new HashMap<Node, NodeEdgeInformation>();
        for (Renderable r : objects) {
            if (r instanceof Node) {
                Node node = (Node) r;
                NodeEdgeInformation info = generateNodeEdgeInfo(node);
                if (info != null)
                    nodeEdgeInfo.put(node,
                                     info);
            }
        }
    }
    
    private NodeEdgeInformation generateNodeEdgeInfo(Node node) {
        NodeEdgeInformation info = new NodeEdgeInformation();
        List list = node.getConnectInfo().getConnectWidgets();
        if (list == null || list.size() == 0)
            return null;
        for (Object obj : list) {
            ConnectWidget widget = (ConnectWidget) obj;
            // Don't need to look at edges in the wrapped list.
            // It should be handled by EdgeNodeInformation.
            if (objects.contains(widget.getEdge()))
                continue;
            List<HyperEdge> edges = null;
            switch (widget.getRole()) {
                case HyperEdge.INPUT :
                    if (info.inputEdges == null) 
                        info.inputEdges = new ArrayList<HyperEdge>();
                    edges = info.inputEdges;
                    break;
                case HyperEdge.OUTPUT :
                    if (info.outputEdges == null)
                        info.outputEdges = new ArrayList<HyperEdge>();
                    edges = info.outputEdges;
                    break;
                case HyperEdge.CATALYST :
                    if (info.catalyzedEdges == null)
                        info.catalyzedEdges = new ArrayList<HyperEdge>();
                    edges = info.catalyzedEdges;
                    break;
                case HyperEdge.INHIBITOR :
                    if (info.inhibitedEdges == null)
                        info.inhibitedEdges = new ArrayList<HyperEdge>();
                    edges = info.inhibitedEdges;
                    break;
                case HyperEdge.ACTIVATOR :
                    if (info.activatedEdges == null)
                        info.activatedEdges = new ArrayList<HyperEdge>();
                    edges = info.activatedEdges;
                    break;
            }
            if (edges != null)
                edges.add(widget.getEdge());
        }
        return info;
    }
    
    private EdgeNodeInformation generateEdgeNodeInfo(HyperEdge edge) {
        EdgeNodeInformation info = new EdgeNodeInformation();
        info.inputs = edge.getInputNodes();
        info.outputs = edge.getOutputNodes();
        info.catalysts = edge.getHelperNodes();
        info.inhibitors = edge.getInhibitorNodes();
        info.activators = edge.getActivatorNodes();
        return info;
    }

    protected void insert() throws CannotRedoException {
        // To make other GUIs correctly, have to insert via
        // GraphEditorPane
        for (Renderable r : objects) {
            if (r instanceof Node) {
                graphPane.insertNode((Node)r);
                linkToShortcuts(r);
            }
            else if (r instanceof HyperEdge) {
                graphPane.insertEdge((HyperEdge)r,
                                     false);
            }
            recoverContainerInfo(r,
                                 rToContainer);
            // Need to handle registration
            RenderableRegistry.getRegistry().add(r);
        }
        recoverEdgeNodeInfo();
        recoverNodeEdgeInfo();
        graphPane.repaint(graphPane.getVisibleRect());
    }

    private void linkToShortcuts(Renderable r) {
        // To link to shortcuts
        Node exist = (Node) RenderableRegistry.getRegistry().getSingleObject(r.getDisplayName());
        if (exist != null) {
           List<Renderable> shortcuts = exist.getShortcuts();
           if (shortcuts == null) {
               shortcuts = new ArrayList<Renderable>();
               shortcuts.add(exist);
               shortcuts.add(r);
               r.setShortcuts(shortcuts);
               exist.setShortcuts(shortcuts);
           }
           else {
               if (shortcuts.size() == 0)
                   shortcuts.add(exist); // Make sure itself is in the list.
               shortcuts.add(r);
               r.setShortcuts(shortcuts);
           }
        }
    }

    protected void delete() throws CannotUndoException {
        for (Renderable r : objects)
            graphPane.delete(r);
        graphPane.repaint(graphPane.getVisibleRect());
    }
    
    private void recoverEdgeNodeInfo() {
        for (HyperEdge edge : edgeNodeInfo.keySet()) {
            EdgeNodeInformation info = edgeNodeInfo.get(edge);
            recoverEdgeNodeInfo(edge, info);
        }
    }
    
    private void recoverNodeEdgeInfo() {
        for (Node node : nodeEdgeInfo.keySet()) {
            NodeEdgeInformation info = nodeEdgeInfo.get(node);
            recoverNodeEdgeInfo(node, info);
        }
    }
    
    private void recoverEdgeNodeInfo(HyperEdge edge,
                                     EdgeNodeInformation info) {
        if (info.inputs != null) {
            for (Node node : info.inputs)
                edge.addInput(node);
        }
        if (info.outputs != null)
            for (Node node : info.outputs)
                edge.addOutput(node);
        if (info.catalysts != null)
            for (Node node : info.catalysts)
                edge.addHelper(node);
        if (info.activators != null)
            for (Node node : info.activators)
                edge.addActivator(node);
        if (info.inhibitors != null)
            for (Node node : info.inhibitors)
                edge.addInhibitor(node);
    }
    
    private void recoverNodeEdgeInfo(Node node,
                                     NodeEdgeInformation info) {
        if (info.inputEdges != null) {
            for (HyperEdge edge : info.inputEdges)
                edge.addInput(node);
        }
        if (info.outputEdges != null) {
            for (HyperEdge edge : info.outputEdges)
                edge.addOutput(node);
        }
        if (info.catalyzedEdges != null) {
            for (HyperEdge edge : info.catalyzedEdges)
                edge.addHelper(node);
        }
        if (info.inhibitedEdges != null) {
            for (HyperEdge edge : info.inhibitedEdges)
                edge.addInhibitor(node);
        }
        if (info.activatedEdges != null) {
            for (HyperEdge edge : info.activatedEdges)
                edge.addActivator(node);
        }
    }
    
    protected void recoverEdgeInfo(HyperEdge edge, EdgePositionInfo info) {
        recoverPoints(edge.getBackbonePoints(), 
                      info.backbone);
        recoverBranches(edge.getInputPoints(),
                        info.inputs);
        recoverBranches(edge.getOutputPoints(),
                        info.outputs);
        recoverBranches(edge.getHelperPoints(), 
                        info.catalysts);
        recoverBranches(edge.getInhibitorPoints(), 
                        info.inhibitors);
        recoverBranches(edge.getActivatorPoints(),
                        info.activators);
    }

    private void recoverPoints(List points, List<Point> oldPoints) {
        if (points == null || oldPoints == null)
            return;
        for (int i = 0; i < points.size(); i++) {
            // Just in case
            if (i > oldPoints.size() - 1)
                break;
            Point newP = (Point) points.get(i);
            Point oldP = oldPoints.get(i);
            newP.x = oldP.x;
            newP.y = oldP.y;
        }
    }

    private void recoverBranches(List branches, List<List<Point>> oldBranches) {
        if (branches == null || oldBranches == null)
            return;
        for (int i = 0; i < branches.size(); i++) {
            if (i > oldBranches.size() - 1)
                break;
            List points = (List) branches.get(i);
            List<Point> oldPoints = oldBranches.get(i);
            recoverPoints(points, oldPoints);
        }
    }

    protected EdgePositionInfo generateEdgeInfo(HyperEdge edge) {
        EdgePositionInfo info = new EdgePositionInfo();
        List backbone = edge.getBackbonePoints();
        info.backbone = copyPointList(backbone);
        if (edge.getInputPoints() != null)
            info.inputs = copyBranches(edge.getInputPoints());
        if (edge.getOutputPoints() != null)
            info.outputs = copyBranches(edge.getOutputPoints());
        if (edge.getHelperPoints() != null)
            info.catalysts = copyBranches(edge.getHelperPoints());
        if (edge.getActivatorPoints() != null)
            info.activators = copyBranches(edge.getActivatorPoints());
        if (edge.getInhibitorPoints() != null)
            info.inhibitors = copyBranches(edge.getInhibitorPoints());
        return info;
    }

    private List<List<Point>> copyBranches(List branches) {
        List<List<Point>> copy = new ArrayList<List<Point>>(branches.size());
        for (Iterator it = branches.iterator(); it.hasNext();) {
            List points = (List) it.next();
            List<Point> clone = copyPointList(points);
            copy.add(clone);
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

    /**
     * Small data structure to catch linked nodes to edges.
     * After deletion, all link information will be lost.
     */
    class EdgeNodeInformation {
        List<Node> inputs;
        List<Node> outputs;
        List<Node> catalysts;
        List<Node> activators;
        List<Node> inhibitors;
        
        EdgeNodeInformation() {
            
        }
    }
    
    /**
     * This small data structure is used to catch Edges linked to
     * deleted Nodes. These edges should NOT be in the object list.
     */
    class NodeEdgeInformation {
        List<HyperEdge> inputEdges;
        List<HyperEdge> outputEdges;
        List<HyperEdge> catalyzedEdges;
        List<HyperEdge> activatedEdges;
        List<HyperEdge> inhibitedEdges;
        
        NodeEdgeInformation() {
        }
        
    }
    
}
