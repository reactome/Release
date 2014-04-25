/*
 * Created on Aug 14, 2008
 *
 */
package org.gk.graphEditor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.gk.render.HyperEdge;

/**
 * To handle undo/redo for edge layout.
 * @author wgm
 *
 */
public class UndoableEdgeLayoutEdit extends UndoableGraphEdit {
    
    protected Map<HyperEdge, EdgePositionInfo> oldEdgeToInfo;
    protected Map<HyperEdge, EdgePositionInfo> newEdgeToInfo;
    
    /**
     * This constructor is for subclass.
     */
    protected UndoableEdgeLayoutEdit() {
    }
    
    public UndoableEdgeLayoutEdit(GraphEditorPane graphPane,
                                  List<HyperEdge> edges) {
        super();
        this.graphPane = graphPane;
        oldEdgeToInfo = new HashMap<HyperEdge, EdgePositionInfo>();
        storeCoordinates(oldEdgeToInfo, edges);
    }
    
    private void storeCoordinates(Map<HyperEdge, EdgePositionInfo> edgeInfo,
                                  Collection<HyperEdge> edges) {
        for (HyperEdge edge : edges) {
            EdgePositionInfo info = generateEdgeInfo(edge);
            edgeInfo.put(edge, info);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        undoOrRedo(newEdgeToInfo);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        if (newEdgeToInfo == null) {
            newEdgeToInfo = new HashMap<HyperEdge, EdgePositionInfo>();
            storeCoordinates(newEdgeToInfo, 
                             oldEdgeToInfo.keySet());
        }
        undoOrRedo(oldEdgeToInfo);
    }
    
    private void undoOrRedo(Map<HyperEdge, EdgePositionInfo> edgeInfo) {
        for (HyperEdge edge : edgeInfo.keySet()){
            EdgePositionInfo info = edgeInfo.get(edge);
            recoverEdgeInfo(edge, info);
        }
        graphPane.repaint(graphPane.getVisibleRect());
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
    
}
