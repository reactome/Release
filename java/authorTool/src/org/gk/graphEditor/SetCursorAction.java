/*
 * Created on Sep 8, 2005
 *
 */
package org.gk.graphEditor;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;

import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;

public class SetCursorAction implements GraphEditorAction {
    private GraphEditorPane graphPane;
    
    public SetCursorAction(GraphEditorPane graphPane) {
        this.graphPane = graphPane;
    }
    
    public void doAction(MouseEvent mouseEvent) {
        // Handle Mouse Moved action only
        if (mouseEvent.getID() != MouseEvent.MOUSE_MOVED)
            return;
        //changed the cursor to hand-cursor if a renderable is picked!
        java.util.List comps = graphPane.getDisplayedObjects();
        if (comps == null || comps.size() == 0) {
            graphPane.setCursor(Cursor.getDefaultCursor());
            return;
        }
        int size = comps.size();
        Point p = mouseEvent.getPoint();
        p.x /= graphPane.getScaleX();
        p.y /= graphPane.getScaleY();
        // Check if a LinkWidget is selected
        if (graphPane instanceof PathwayEditor) {
            PathwayEditor pathwayEditor = (PathwayEditor) graphPane;
            if (pathwayEditor.isLinkWidgetPicked(p.x, p.y) != GraphEditorPane.LINK_WIDGET_NONE) {
                graphPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                graphPane.setMouseOveredRenderable(null);
                return;
            }
        }
        Renderable mouseOveredRenderable = graphPane.getMouseOveredRenderable();
        if (mouseOveredRenderable != null) {
            if (mouseOveredRenderable instanceof HyperEdge) {
                HyperEdge edge = (HyperEdge) mouseOveredRenderable;
                if (edge.isPointPicked(p)) {
                    graphPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
                else if (edge.canBePicked(p)) {
                    graphPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }
            }
            else {
                if (mouseOveredRenderable.isPicked(p))
                    return;
            }
        }
        for (int i = size - 1; i >= 0; i--) {
            Renderable renderable = (Renderable) comps.get(i);
            if (renderable instanceof HyperEdge) {
                HyperEdge edge = (HyperEdge) renderable;
                if (edge.isPointPicked(p)) {
                    graphPane.setMouseOveredRenderable(renderable);
                    graphPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
                else if (edge.canBePicked(p)) {
                    graphPane.setMouseOveredRenderable(renderable);
                    graphPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }
                continue; // The problem is from isPicked() will add a new Point to FlowLine.
                          // Escape FlowLine. No need for tooltip for FlowLine.
            }
            else if (renderable.isPicked(p)) {
                graphPane.setMouseOveredRenderable(renderable);
                if (renderable instanceof Node &&
                    ((Node)renderable).isEditing())
                    graphPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                else
                    graphPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                return;
            }
        }
        // Check if any linkable widget is seletable. If true, chang to HAND_CURSOR
        graphPane.setMouseOveredRenderable(null);
        graphPane.setCursor(Cursor.getDefaultCursor());
    }

}