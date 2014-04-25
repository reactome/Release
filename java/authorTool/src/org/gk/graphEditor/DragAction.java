/*
 * DragAction.java
 *
 * Created on June 16, 2003, 10:11 PM
 */

package org.gk.graphEditor;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
/**
 * This class is used to handle mouse dragging action in the GraphEditorPane.
 * @author  wgm
 */
public class DragAction implements GraphEditorAction {
    // Recording the points
    protected Point prevPoint;
    // Parent editor pane
    protected GraphEditorPane editorPane;
    // A flag to control drag mode
    boolean isForMoving;
    // For moving
    private RenderableMoveHelper moveHelper;
    // For scrolling
    private Rectangle scrollRect = new Rectangle();
    
    /** Creates a new instance of DragAction */
    public DragAction(GraphEditorPane editorPane) {
        this.editorPane = editorPane;
        this.prevPoint = new Point();
        moveHelper = new RenderableMoveHelper();
        moveHelper.setGraphEditorPane(editorPane);
    }
    
    /**
     * This method is used to store the initial states
     * @param x
     * @param y
     */
    void setStartPosition(int x, int y) {
        prevPoint.x = x;
        prevPoint.y = y;
        moveHelper.startMove(x, y);
    }
    
    private void doMouseDrag(int x, int y) {
        if (isForMoving) {
            moveObjects(x, y);
        }
        else { // Do a selection
            // Have to consider the user drags the rectangle from another way around
            editorPane.dragRect.width = x - editorPane.dragRect.x;
            editorPane.dragRect.height = y - editorPane.dragRect.y;
            if (editorPane.dragRect.width < 0 || editorPane.dragRect.height < 0) {
                // Need to create a new Rectangle
                Rectangle rect = new Rectangle();
                rect.width = Math.abs(editorPane.dragRect.width);
                rect.height = Math.abs(editorPane.dragRect.height);
                if (editorPane.dragRect.width < 0)
                    rect.x = editorPane.dragRect.x + editorPane.dragRect.width;
                else
                    rect.x = editorPane.dragRect.x;
                if (editorPane.dragRect.height < 0)
                    rect.y = editorPane.dragRect.y + editorPane.dragRect.height;
                else
                    rect.y = editorPane.dragRect.y;
                editorPane.select(rect);
            }
            else
                editorPane.select(editorPane.dragRect);
        }
        editorPane.setShouldDrawLinkWidgets(false);
        // Don't scale scrollRect       
        // cannot do scroll here. It will make mouse position not correct!
//      scrollRect.x = e.getX();
//      scrollRect.y = e.getY();
//      scrollRect.width = 10; // 30;
//      scrollRect.height = 10; // 20;
//      editorPane.scrollRectToVisible(scrollRect);
        editorPane.repaint(editorPane.getVisibleRect());
        // Revalidate after repaint to make bounds correct
        editorPane.revalidate();
    }
    
    
    private void moveObjects(int x, int y) {
        int dx = x - prevPoint.x;
        int dy = y - prevPoint.y;
        // Do move
        moveHelper.move(dx, dy);
        prevPoint.x = x;
        prevPoint.y = y;
        moveHelper.scrollToVisible((int)(prevPoint.x * editorPane.getScaleX()), 
                                   (int)(prevPoint.y * editorPane.getScaleY()));
    }
    
    public void doAction(MouseEvent e) {
        int x = (int)(e.getX() / editorPane.getScaleX());
        int y = (int)(e.getY() / editorPane.getScaleY());
        if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
            doMouseDrag(x, y);
        }
        else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            doMouseRelease(e);
        }
    }
    
    private void doMouseRelease(MouseEvent e) {
        editorPane.currentAction = editorPane.selectAction; // Reset to default action.
        // Empty dragRect
        if (editorPane.dragRect.width != 0 &&
            editorPane.dragRect.height != 0) {
            editorPane.dragRect.width = 0;
            // Repaint it
            editorPane.repaint(editorPane.getVisibleRect());
        }
        // Do nothing if it is not for moving
        if (!isForMoving)
            return;
        // x, y should be the actual coordinates. 
        int x1 = (int)(e.getX() / editorPane.getScaleX());
        int y1 = (int)(e.getY() / editorPane.getScaleY());
        moveHelper.completeMove(x1, y1);
//      editorPane.revalidate();
////    // Don't scale scrollRect
        scrollRect.x = e.getX();
        scrollRect.y = e.getY();
        scrollRect.width = 30;
        scrollRect.height = 20;
        editorPane.scrollRectToVisible(scrollRect);
        editorPane.repaint(editorPane.getVisibleRect());
    }
}
