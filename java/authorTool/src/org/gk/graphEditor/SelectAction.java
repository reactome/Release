/*
 * SelectAction.java
 *
 * Created on June 16, 2003, 10:23 PM
 */

package org.gk.graphEditor;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderableComplex;
/**
 * This GraphEditorAction class is used to handle selection action in GraphEditorPane.
 * @author  wgm
 */
public class SelectAction implements GraphEditorAction {
    // Point info
    protected Point pressPoint;
    // Parent Pane
    protected GraphEditorPane editorPane;
    
    /** Creates a new instance of SelectAction */
    public SelectAction(GraphEditorPane editorPane) {
        this.editorPane = editorPane;
        pressPoint = new Point();
    }
    
    /**
     * TODO: There is a bug here: If a HyperEdge has been selected based on a single point,
     * using isPicked(Point) on this HyperEdge may change the selection status (e.g. from a single
     * point to the whole selection). Right now a new method on HyperEdge has been created, canBePicked(Point).
     * However, some more complicated thing here makes it difficult to use this method. Have to
     * think it carefully again.
     */
    public void doAction(java.awt.event.MouseEvent e) {
    	int x = (int) (e.getX() / editorPane.getScaleX());
    	int y = (int) (e.getY() / editorPane.getScaleY());
    	if ((editorPane.linkWidgetAction != null) &&
    	    (editorPane.isLinkWidgetPicked(x, y) != GraphEditorPane.LINK_WIDGET_NONE)) {
    		editorPane.currentAction = editorPane.linkWidgetAction;
    		editorPane.linkWidgetAction.doAction(e);
    		return;
    	}
    	//Use this line to keep selection for popup menu.
    	//if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) > 0)
    	//	return;
        // Do selection here.
        if (e.getID() == MouseEvent.MOUSE_PRESSED) { // Do a single selection
            cleanUpSelection();
            pressPoint.x = x;
            pressPoint.y = y;
            java.util.List list = editorPane.getDisplayedObjects();
            if (list == null || list.size() == 0)
                return;
            boolean isMetaDown = false;
            if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)
                isMetaDown = true;
            else if ((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0)
                isMetaDown = true;
            if (!isMetaDown) {
                // Check if the pressed point is on a selected object
                // This function is used to move a list of selected objects.
                for (Iterator it = editorPane.getSelection().iterator(); it.hasNext();) {
                    Renderable child = (Renderable) it.next();
                    if (child.isPicked(pressPoint)) {
                        if (child instanceof HyperEdge) {
                            editorPane.repaint(editorPane.getVisibleRect());
                        }
                        return;
                    }
                }
            }
            boolean isPicked = false;
          	int size = list.size();
          	// Delay for compartments
          	List<RenderableCompartment> compartments = new ArrayList<RenderableCompartment>();
          	// Do a batch update for performance gaining
          	List<Renderable> removeSelection = new ArrayList<Renderable>();
          	List<Renderable> addSelection = new ArrayList<Renderable>();
          	for (int i = size - 1; i >= 0; i--) {
          		Object obj = list.get(i);
          		if (obj instanceof RenderableCompartment) {
          		    compartments.add((RenderableCompartment)obj);
          		    continue;
          		}
                if (obj instanceof Renderable) {
                    Renderable child = (Renderable) obj;
                    if (child.canBePicked(pressPoint) && !isPicked) {
                        // To control only one selection will be chosen for each click by using isPicked flag
                        if (isMetaDown && child.isSelected()) {
                            child.setIsSelected(false);
//                            editorPane.removeSelected(child);
                            removeSelection.add(child);
                        }
                        else {
                            child.setIsSelected(true);
//                            editorPane.addSelected(child);
                            addSelection.add(child);
                        }
                        isPicked = true;
                    }
                    else if (!isMetaDown && child.isSelected()) {
                    	child.setIsSelected(false);
//                    	editorPane.removeSelected(child);      
                    	removeSelection.add(child);
                    }
                }
            }
          	if (removeSelection.size() > 0)
          	    editorPane.removeSelection(removeSelection);
          	if (addSelection.size() > 0)
          	    editorPane.addSelection(addSelection);
          	for (RenderableCompartment compartment : compartments) {
                if (compartment.canBePicked(pressPoint) && !isPicked) {
                    if (isMetaDown && compartment.isSelected()) {
                        compartment.setIsSelected(false);
                        editorPane.removeSelected(compartment);
                    }
                    else {
                        compartment.setIsSelected(true);
                        editorPane.addSelected(compartment);
                    }
                    isPicked = true;
                }
                else if (!isMetaDown && compartment.isSelected()) {
                    compartment.setIsSelected(false);
                    editorPane.removeSelected(compartment);          
                }
          	}
            // Check if alt is pressed. If alt is pressed, and one Node is selected,
            // go to editing mode directly.
            if ((e.getModifiers() & InputEvent.ALT_MASK) != 0) {
            	java.util.List selection = editorPane.getSelection();
            	if (selection.size() == 1 && (selection.get(0) instanceof Node)) {
            		Node node = (Node) selection.get(0);
            		if (node != editorPane.getEditingNode()) {
            			editorPane.setEditingNode(node);
            		}
            		if (!editorPane.isEditing)
            			editorPane.setIsEditing(true);
            	}
            }
            // Check if it is needed to switch off editing mode.
            if (editorPane.isEditing()) { // Have to check this special case
            	java.util.List selection = editorPane.getSelection();
            	if (selection.size() != 1 ||
            	    selection.get(0) != editorPane.getEditingNode()) {
            		if (editorPane.getEditor().isChanged()) {
            			GraphEditorActionEvent event = new GraphEditorActionEvent(editorPane.getEditingNode());
            			event.setID(GraphEditorActionEvent.NAME_EDITING);
            			editorPane.fireGraphEditorActionEvent(event);
            		}
					editorPane.stopEditing();
            	}
            }
            editorPane.repaint(editorPane.getVisibleRect());
        }
        else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            java.util.List selection = editorPane.getSelection();
            if (isConnectable(selection)) {
                HyperEdge reaction = (HyperEdge)selection.get(0);
                if (reaction.getConnectWidget() != null) {
                    editorPane.currentAction = editorPane.connectAction;
                    editorPane.connectAction.prevPoint = pressPoint;
                    editorPane.connectAction.setConnectWidget(reaction.getConnectWidget());
                    editorPane.connectAction.doAction(e);
                    return;
                }
            }
            if  (selection == null || selection.size() == 0) {
            	editorPane.dragRect.x = x;
            	editorPane.dragRect.y = y;
            	editorPane.dragAction.isForMoving = false;
            }
            else
            	editorPane.dragAction.isForMoving = true;
            editorPane.currentAction = editorPane.dragAction;
            editorPane.dragAction.setStartPosition(pressPoint.x,
                                                   pressPoint.y);
            editorPane.dragAction.doAction(e);
        }
    }
    
    /**
     * A helper method that is used to check if ConnectionAction should be used 
     * @param selection
     * @return
     */
    private boolean isConnectable(List selection) {
        if (!editorPane.isEditable)
            return false;
        // Make sure there is only one selection
        if (selection == null || selection.size() != 1)
            return false;
        Renderable r = (Renderable) selection.get(0);
        // Only HyperEdge can be used for link
        if (!(r instanceof HyperEdge))
            return false;
        // Block this action
        HyperEdge edge = (HyperEdge) r;
        if (edge.getConnectWidget() == null)
            return false;
        if (editorPane.usedAsDrawingTool) {
            if (edge.getConnectWidget().getConnectedNode() == null)
                return false;
        }
        return true;
    }
    
    /**
     * Probably there are some selected objects are contained by displayed 
     * Complex in PathwayEditor. These selected objects should be deselected
     * first. 
     * TODO: This is not a good way. Have to think out a better OOP way.
     */
    private void cleanUpSelection() {
        List selection = editorPane.getSelection();
        List selectionRemoved = new ArrayList();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r.getContainer() instanceof RenderableComplex) {
                r.setIsSelected(false);
                selectionRemoved.add(r);
            }
        }
        editorPane.removeSelection(selectionRemoved);
    }
}
