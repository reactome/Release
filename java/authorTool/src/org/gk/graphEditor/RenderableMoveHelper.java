/*
 * Created on Oct 10, 2008
 *
 */
package org.gk.graphEditor;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderableComplex;

/**
 * This helper class is refactored from DragAction, used to manage moving
 * related stuff.
 * @author wgm
 *
 */
public class RenderableMoveHelper {
    private GraphEditorPane editorPane;
    // To control complex and compartment editing
    // Keep track the picked compartment
    private List<RenderableCompartment> pickedCompartments;
    // Keep track renderables that can be picked up by moving compartments
    private List<Renderable> pickedCompartmentComps;
    // Keep track the picked complex
    private List<RenderableComplex> pickedComplexes;
    // track nodes that can be picked by moving complexes
    private List<Node> pickedComplexComps;
    // For undo
    // Original coordinate
    private int x0, y0;
    private Map<Renderable, Renderable> rToContainer;
    // A helper rectangle to control visible area
    private Rectangle visibleRectangle = new Rectangle(0, 0, 10, 10);
    
    public RenderableMoveHelper() {
        pickedCompartments = new ArrayList<RenderableCompartment>();
        pickedCompartmentComps = new ArrayList<Renderable>();
        pickedComplexes = new ArrayList<RenderableComplex>();
        pickedComplexComps = new ArrayList<Node>();
    }
    
    public void setGraphEditorPane(GraphEditorPane editor) {
        this.editorPane = editor;
    }
    
    public void move(int dx, int dy) {
        // Do move
        cleanUpPickedComplexes();
        cleanUpPickedCompartments();
        List selection = editorPane.getSelection();
        if (selection != null && selection.size() > 0) {
            for (Iterator it = selection.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r.getContainer() instanceof RenderableCompartment &&
                    selection.contains(r.getContainer()))
                    continue; // Will be handled by the compartment moving
                r.move(dx, dy);
                RenderableCompartment compartment = editorPane.pickUpCompartment(r);
                if (compartment != null) {
                    pickedCompartments.add(compartment);
                    compartment.setIsHighlighted(true);
                }
                if (r instanceof Node) {
                    Node node = (Node) r;
                    RenderableComplex complex = editorPane.pickUpComplex(node);
                    if (complex != null) {
                        complex.setIsHighlighted(true);
                        pickedComplexes.add(complex);
                    }
                }
                if (r instanceof RenderableComplex) {
                    RenderableComplex complex = (RenderableComplex) r;
                    List<Node> complexNodes = editorPane.pickUpComplexComponents(complex);
                    if (complexNodes != null) {
                        for (Node node : complexNodes) {
                            node.setIsHighlighted(true);
                            pickedComplexComps.add(node);
                        }
                    }
                }
                else if (r instanceof RenderableCompartment) {
                    compartment = (RenderableCompartment) r;
                    List<Renderable> list = editorPane.pickUpCompartmentComponents(compartment);
                    if (list != null) {
                        for (Renderable tmp : list) {
                            tmp.setIsHighlighted(true);
                            pickedCompartmentComps.add(tmp);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Scroll the editorPane so that the passed point to be visible.
     * @param x
     * @param y
     */
    public void scrollToVisible(int x, int y) {
        visibleRectangle.x = x;
        visibleRectangle.y = y;
        editorPane.scrollRectToVisible(visibleRectangle);
    }
    
    public void completeMove(int endX, 
                             int endY) {
        // Generate a GraphEditorActionEvent for moving
        // Validate the points in the HyperEdge
        List selection = editorPane.getSelection();
        // In case it is changed somewhere
        List copy = new ArrayList(selection);
        for (Iterator it = copy.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            // Check RenderableComplex first. If a Node can be contained by a RenderableComplex
            // It should not be contained by RenderableCompartment. The compartment of this Node
            // should be set by the complex.
            editorPane.validateComplexSetting(r);
            editorPane.validateCompartmentSetting(r);
        }
        cleanUpPickedComplexes();
        cleanUpPickedCompartments();
        GraphEditorActionEvent event = new GraphEditorActionEvent(editorPane, 
                                                                  GraphEditorActionEvent.MOVING);
        editorPane.fireGraphEditorActionEvent(event);
        // x, y should be the actual coordinates. 
        int dx = endX - x0;
        int dy = endY - y0;
        UndoableMoveEdit edit = new UndoableMoveEdit(editorPane,
                                                     selection,
                                                     dx,
                                                     dy);
        edit.setContainerInfo(rToContainer);
        editorPane.addUndoableEdit(edit);
    }
    
    private void cleanUpPickedComplexes() {
        // Need to empty previously complex
        for (RenderableComplex complex : pickedComplexes) {
            complex.setIsHighlighted(false);
        }
        pickedComplexes.clear();
        for (Node node : pickedComplexComps) 
            node.setIsHighlighted(false);
        pickedComplexComps.clear();
    }
    
    private void cleanUpPickedCompartments() {
        for (RenderableCompartment compartment : pickedCompartments)
            compartment.setIsHighlighted(false);
        pickedCompartments.clear();
        for (Renderable r : pickedCompartmentComps)
            r.setIsHighlighted(false);
        pickedCompartmentComps.clear();
    }
    
    public void startMove(int originalX,
                          int originalY) {
        x0 = originalX;
        y0 = originalY;
        grepContainerInfo();
    }
    
    /**
     * Fill up container information to a passed map.
     * @param rToContainer
     */
    private void grepContainerInfo() {
        if (rToContainer == null)
            rToContainer = new HashMap<Renderable, Renderable>();
        else
            rToContainer.clear();
        for (Iterator it = editorPane.getSelection().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            rToContainer.put(r, r.getContainer());
        }
        // Try to extract more information just in case of resizing
        Set<Renderable> keys = new HashSet<Renderable>(rToContainer.keySet());
        for (Renderable r : keys) {
            List<Renderable> comps = r.getComponents();
            if (comps == null || comps.size() == 0)
                continue;
            for (Renderable comp : comps) {
                rToContainer.put(comp, r);
            }
        }
    }
    
}
