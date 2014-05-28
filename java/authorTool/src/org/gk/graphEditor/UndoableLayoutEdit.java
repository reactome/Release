/*
 * Created on Aug 13, 2008
 *
 */
package org.gk.graphEditor;

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;


/**
 * To undoable layout
 * @author wgm
 *
 */
public class UndoableLayoutEdit extends UndoableEdgeLayoutEdit {
    // Old positions for undo
    private Map<Node, Point> oldNodeToPos;
    // New positions for redo
    private Map<Node, Point> newNodeToPos;
    
    public UndoableLayoutEdit(GraphEditorPane graphPane) {
        this.graphPane = graphPane;
        oldNodeToPos = new HashMap<Node, Point>();
        oldEdgeToInfo = new HashMap<HyperEdge, EdgePositionInfo>();
        storeCoordinates(oldNodeToPos,
                         oldEdgeToInfo);
    }
    
    private void storeCoordinates(Map<Node, Point> nodeToPos,
                                  Map<HyperEdge, EdgePositionInfo> edgeToInfo) {
        for (Iterator it = graphPane.getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Node) {
                if (r.getContainer() instanceof RenderableComplex)
                    continue; // Will be handled by complex
                nodeToPos.put((Node)r,
                              new Point(r.getPosition()));
            }
            else if (r instanceof HyperEdge) {
                EdgePositionInfo info = generateEdgeInfo((HyperEdge)r);
                edgeToInfo.put((HyperEdge)r,
                              info);
            }
        }
    }
    
    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        undoOrRedo(newNodeToPos,
                   newEdgeToInfo);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        if (newNodeToPos == null) {// First time undo 
            newNodeToPos = new HashMap<Node, Point>();
            newEdgeToInfo = new HashMap<HyperEdge, EdgePositionInfo>();
            storeCoordinates(newNodeToPos, newEdgeToInfo);
        }
        undoOrRedo(oldNodeToPos,
                   oldEdgeToInfo);
    }

    private void undoOrRedo(Map<Node, Point> nodeToPos,
                            Map<HyperEdge, EdgePositionInfo> edgeToInfo) {
        int dx, dy;
        for (Node r : nodeToPos.keySet()) {
            Point oldPos = nodeToPos.get(r);
            Point newPos = r.getPosition();
            dx = oldPos.x - newPos.x;
            dy = oldPos.y - newPos.y;
            r.move(dx, dy);
        }
        for (HyperEdge edge : edgeToInfo.keySet()) {
            EdgePositionInfo info = edgeToInfo.get(edge);
            recoverEdgeInfo(edge, info);
        }
        graphPane.repaint(graphPane.getVisibleRect());
    }
    
}
