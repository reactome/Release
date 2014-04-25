/*
 * Created on Aug 25, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.UndoableGraphEdit;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableRegistry;

/**
 * This method is used to handle Undo/Redo related to complex formation.
 * @author wgm
 *
 */
public class UndoableFormComplexEdit extends UndoableGraphEdit {
    private List<Renderable> subunits;
    private RenderableComplex complex;
    private Map<Renderable, Point> originalPositions;
    private Map<Renderable, Point> newPositions;
    // Need to recover the original container information
    private Map<Renderable, Renderable> rToContainer;
    
    public UndoableFormComplexEdit(List<Renderable> subunits,
                                   GraphEditorPane editor) {
        this.graphPane = editor;
        this.subunits = new ArrayList<Renderable>(subunits);
        // Extract the original position
        originalPositions = new HashMap<Renderable, Point>();
        storePositions(originalPositions);
        rToContainer = new HashMap<Renderable, Renderable>();
        storeContainerInfo(rToContainer, subunits);
        newPositions = new HashMap<Renderable, Point>();
    }
    
    public void setComplex(RenderableComplex complex) {
        this.complex = complex;
    }
    
    private void storePositions(Map<Renderable, Point> positions) {
        for (Renderable node : subunits) {
            positions.put(node,
                          new Point(node.getPosition()));
        }
    }
    
    public void undo() {
        super.undo();
        if (newPositions.size() == 0)
            storePositions(newPositions);
        graphPane.delete(complex);
        complex.getComponents().clear(); // Just in case
        move(originalPositions);
        for (Renderable r : rToContainer.keySet())
            recoverContainerInfo(r, rToContainer);
        graphPane.repaint(graphPane.getVisibleRect());
    }
    
    private void move(Map<Renderable, Point> positions) {
        for (Renderable node : subunits) {
            Point p = node.getPosition();
            Point oldP = positions.get(node);
            int dx = oldP.x - p.x;
            int dy = oldP.y - p.y;
            node.move(dx, dy);
        }
    }
    
    public void redo() {
        super.redo();
        graphPane.insertNode(complex);
        RenderableRegistry.getRegistry().add(complex);
        for (Renderable r : subunits) {
            complex.addComponent(r);
            r.setContainer(complex);
        }
        move(newPositions);
        graphPane.repaint(graphPane.getVisibleRect());
    }
    
    
}
