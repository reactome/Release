/*
 * Created on Dec 13, 2006
 *
 */
package org.gk.graphEditor;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

import org.gk.render.DefaultRenderConstants;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.util.DrawUtilities;

/**
 * This helper class is used to handle activities related to link widgets.
 * @author guanming
 *
 */
class LinkWidgetHandler implements DefaultRenderConstants {
    // For link widgets
    private Rectangle eastWidgetBounds;
    private Rectangle southWidgetBounds;
    private Rectangle westWidgetBounds;
    private Rectangle northWidgetBounds;
    private boolean shouldDrawWidget;
    // target
    private GraphEditorPane editorPane;
    
    LinkWidgetHandler(GraphEditorPane graphPane) {
        editorPane = graphPane;
        init();
    }
    
    private Rectangle createWidgetBounds() {
        Rectangle rect = new Rectangle();
        rect.width = LINK_WIDGET_WIDTH + 2;
        rect.height = LINK_WIDGET_WIDTH + 2;
        return rect;
    }
    
    private void init() {
        eastWidgetBounds = createWidgetBounds();
        southWidgetBounds = createWidgetBounds();
        westWidgetBounds = createWidgetBounds();
        northWidgetBounds = createWidgetBounds();

        editorPane.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                if (!editorPane.isEditable() ||
                    editorPane.isUsedAsDrawingTool()) // In drawing mode, don't create link widgets for nodes
                    return;                           // which are pretty annoying sometimes.
                if (editorPane.isEditing) {
                    if (shouldDrawWidget) {
                        shouldDrawWidget = false;
                        editorPane.repaint(editorPane.getVisibleRect());
                    }
                    return;
                }
                java.util.List selection = editorPane.getSelection();
                Renderable container = editorPane.getRenderable();
                if (selection.size() == 1 && selection.get(0) instanceof Node) {
                    Renderable node = (Renderable) selection.get(0);
                    if (node.getContainer() instanceof RenderableComplex)
                        return; // Complex components
                    if (node.getBounds() == null)
                        return;
                    Rectangle dirtyRect = (Rectangle) node.getBounds().clone();
                    dirtyRect.x -= (DefaultRenderConstants.LINK_WIDGET_WIDTH + 4);
                    dirtyRect.y -= (DefaultRenderConstants.LINK_WIDGET_WIDTH + 4);
                    dirtyRect.width += (2 * DefaultRenderConstants.LINK_WIDGET_WIDTH + 8);
                    dirtyRect.height += (2 * DefaultRenderConstants.LINK_WIDGET_WIDTH + 8);
                    boolean isPicked = dirtyRect.contains(e.getX() / editorPane.getScaleX(), 
                                                          e.getY() / editorPane.getScaleY());
                    if (!shouldDrawWidget && isPicked) {
                        shouldDrawWidget = true;
                        editorPane.repaint(editorPane.getVisibleRect());
                    }   
                    else if (shouldDrawWidget && !isPicked) {
                        shouldDrawWidget = false;
                        editorPane.repaint(editorPane.getVisibleRect());
                    }
                }
            }
        });
    }
    
    void drawLinkWidgets(Graphics2D g2) {
        if (!shouldDrawWidget)
            return;
        java.util.List selection = editorPane.getSelection();
        if (selection == null || selection.size() != 1)
            return;
        if (!(selection.get(0) instanceof Node))
            return;
        Node node = (Node) selection.get(0);
        Rectangle bounds = node.getBounds();
        if (bounds == null)
            return;
        if (!bounds.intersects(g2.getClipBounds()))
            return;
        List widgetPos = node.getLinkWidgetPositions();
        if (widgetPos == null || widgetPos.size() == 0)
            return;
        g2.setPaint(DefaultRenderConstants.LINK_WIDGET_COLOR);
        g2.setStroke(DefaultRenderConstants.DEFAULT_STROKE);
        Point cP = new Point();
        Point p = new Point();
        moveAwayWidgets();
        Point widget = (Point) widgetPos.get(0);
        if (widget != null)
            drawEastWidget(g2, widget, cP, p);
        widget = (Point) widgetPos.get(1);
        if (widget != null)
            drawSouthWidget(g2, widget, cP, p);
        widget = (Point) widgetPos.get(2);
        if (widget != null)
            drawWestWidget(g2, widget, cP, p);
        widget = (Point) widgetPos.get(3);
        if (widget != null)
            drawNorthWidget(g2, widget, cP, p);
    }
    
    private void moveAwayWidgets() {
        // Move out of the visible area
        eastWidgetBounds.x = -LINK_WIDGET_WIDTH;
        eastWidgetBounds.y = -LINK_WIDGET_WIDTH;
        southWidgetBounds.x = -LINK_WIDGET_WIDTH;
        southWidgetBounds.y = -LINK_WIDGET_WIDTH;
        westWidgetBounds.x = -LINK_WIDGET_WIDTH;
        westWidgetBounds.y = -LINK_WIDGET_WIDTH;
        northWidgetBounds.x = -LINK_WIDGET_WIDTH;
        northWidgetBounds.y = -LINK_WIDGET_WIDTH;
    }
    
    private void drawEastWidget(Graphics2D g2,
                                Point widget,
                                Point cP,
                                Point p) {
        // Draw the east widget
        cP.x = widget.x + 2;
        cP.y = widget.y;
        p.x = cP.x + LINK_WIDGET_WIDTH;
        p.y = cP.y;
        eastWidgetBounds.x = cP.x - 1;
        eastWidgetBounds.y = cP.y - LINK_WIDGET_WIDTH / 2;
        g2.draw(eastWidgetBounds);
        g2.drawLine(cP.x, cP.y, p.x, p.y);
        DrawUtilities.drawArrow(p, cP, g2);
    }
    
    private void drawSouthWidget(Graphics2D g2,
                                 Point widget,
                                 Point cP,
                                 Point p) {
        cP.x = widget.x;
        cP.y = widget.y + 2;
        p.x = cP.x;
        p.y = cP.y + LINK_WIDGET_WIDTH;
        southWidgetBounds.x = cP.x - LINK_WIDGET_WIDTH / 2;
        southWidgetBounds.y = cP.y - 1;
        g2.draw(southWidgetBounds);
        g2.drawLine(cP.x, cP.y, p.x, p.y);
        DrawUtilities.drawArrow(p, cP, g2);
    }
    
    private void drawWestWidget(Graphics2D g2,
                                Point widget,
                                Point cP,
                                Point p) {
        cP.x = widget.x - 3;
        cP.y = widget.y;
        p.x = cP.x - LINK_WIDGET_WIDTH;
        p.y = cP.y;
        westWidgetBounds.x = p.x - 1;
        westWidgetBounds.y = cP.y - LINK_WIDGET_WIDTH / 2;
        g2.draw(westWidgetBounds);
        g2.drawLine(cP.x, cP.y, p.x, p.y);
        DrawUtilities.drawArrow(p, cP, g2);  
    }
    
    private void drawNorthWidget(Graphics2D g2,
                                 Point widget,
                                 Point cP,
                                 Point p) {
        // Draw the north widget
        cP.x = widget.x;
        cP.y = widget.y - 3;
        p.x = cP.x;
        p.y = cP.y - LINK_WIDGET_WIDTH;
        northWidgetBounds.x = p.x - LINK_WIDGET_WIDTH / 2;
        northWidgetBounds.y = p.y - 1;
        g2.draw(northWidgetBounds);
        g2.drawLine(cP.x, cP.y, p.x, p.y);
        DrawUtilities.drawArrow(p, cP, g2);
    }
    
    /**
     * Check if the specified Point is contained in a link widget.
     * @param p
     * @return one value of: LINK_WIDGET_NONE, LINK_WIDGET_EAST, LINK_WIDGET_SOUTH,
     * LINK_WIDGET_NORTH, LINK_WIDGET_WEST.
     */
    int isLinkWidgetPicked(int x, int y) {
        if (!shouldDrawWidget)
            return GraphEditorPane.LINK_WIDGET_NONE;
        java.util.List selection = editorPane.getSelection();
        if (selection == null || selection.size() != 1)
            return GraphEditorPane.LINK_WIDGET_NONE;
        if (!(selection.get(0) instanceof Node))
            return GraphEditorPane.LINK_WIDGET_NONE;
        if (eastWidgetBounds.contains(x, y))
            return GraphEditorPane.LINK_WIDGET_EAST;
        if (southWidgetBounds.contains(x, y))
            return GraphEditorPane.LINK_WIDGET_SOUTH;
        if (westWidgetBounds.contains(x, y))
            return GraphEditorPane.LINK_WIDGET_WEST;
        if (northWidgetBounds.contains(x, y))
            return GraphEditorPane.LINK_WIDGET_NORTH;
        return GraphEditorPane.LINK_WIDGET_NONE;
    }
    
    void setShouldDrawLinkWidgets(boolean shouldDraw) {
        shouldDrawWidget = shouldDraw;
    }
}
