/*
 * Created on Aug 15, 2008
 *
 */
package org.gk.graphEditor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.gk.render.HyperEdge;
import org.gk.render.HyperEdgeSelectionInfo;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.SelectionPosition;

/**
 * This class is used to handle undo/redo related to move or drag.
 * @author wgm
 *
 */
public class UndoableMoveEdit extends UndoableGraphEdit {
    private List<Renderable> objects;
    // Have to keep the selection status in case of resizing
    private Map<Node, SelectionPosition> nodeToSelectionPos;
    private Map<Renderable, Renderable> rToContainer;
    // This is used to keep the selection status for HyperEdges
    private Map<HyperEdge, Point> edgeToSelectPoint;
    private int dx;
    private int dy;
    
    public UndoableMoveEdit(GraphEditorPane graphPane,
                            Renderable r,
                            int dx,
                            int dy) {
        this.graphPane = graphPane;
        List<Renderable> list = new ArrayList<Renderable>();
        list.add(r);
        init(list, dx, dy);
    }
    
    public UndoableMoveEdit(GraphEditorPane graphPane,
                            List<Renderable> list,
                            int dx,
                            int dy) {
        this.graphPane = graphPane;
        // Make a copy to avoid any wrong doing
        init(new ArrayList<Renderable>(list),
             dx,
             dy);
    }
    
    private void init(List<Renderable> list,
                      int dx,
                      int dy) {
        this.objects = list;
        this.dx = dx;
        this.dy = dy;
        nodeToSelectionPos = new HashMap<Node, SelectionPosition>();
        // Need to record the selection position
        for (Renderable r : list) {
            if (!(r instanceof Node))
                continue;
            Node node = (Node) r;
            SelectionPosition pos = node.getSelectionPosition();
            nodeToSelectionPos.put(node, pos);
        }
        // Need to record the selection points for edges
        edgeToSelectPoint = new HashMap<HyperEdge, Point>();
        for (Renderable r : list) {
            if (!(r instanceof HyperEdge))
                continue;
            HyperEdge edge = (HyperEdge) r;
            HyperEdgeSelectionInfo info = edge.getSelectionInfo();
            // Want to make a copy in case it is cased
            Point selectPoint = info.getSelectPoint();
            edgeToSelectPoint.put(edge, selectPoint);
        }
    }
    
    public void setContainerInfo(Map<Renderable, Renderable> rToContainer) {
        if (this.rToContainer == null)
            this.rToContainer = new HashMap<Renderable, Renderable>(rToContainer);
        else {
            this.rToContainer.clear();
            this.rToContainer.putAll(rToContainer);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        move(dx, dy);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        move(-dx, -dy);
    }
    
    private void move(int dx, int dy) {
        for (Node node : nodeToSelectionPos.keySet()) {
            SelectionPosition pos = nodeToSelectionPos.get(node);
            node.setSelectionPosition(pos);
        }
        for (HyperEdge edge : edgeToSelectPoint.keySet()) {
            Point p = edgeToSelectPoint.get(edge);
            HyperEdgeSelectionInfo info = edge.getSelectionInfo();
            info.setSelectionPoint(p);
        }
        for (Renderable r : objects) {
            r.move(dx, dy);
        }
        // rToContainer may have different sizes as objects for example
        // from a resized compartment.
        for (Renderable r : rToContainer.keySet()) {
            // Want to handle container relationship directly
            recoverContainerInfo(r, rToContainer);
        }
        graphPane.repaint(graphPane.getVisibleRect());
    }
    
}
