/*
 * Created on Sep 26, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.DefaultRenderConstants;

/**
 * This class is used to provide an overview for a big pathway.
 * @author wgm
 *
 */
public class PathwayOverviewPane extends PathwayEditor {
    
    private Rectangle visibleRect;
    // For moving visibleRect
    private Point prevPoint;
    private boolean isDragging;
    private Rectangle prevRect;
    private Rectangle repaintRect;
    // The parent pathway editor
    private PathwayEditor parentEditor;
    // Control view
    private ZoomablePathwayEditor zoomableEditor;
    // the viewport this overview monitors
    private JViewport viewport;
    // A flag used to synchronize two scroll views
    private boolean isFromOverview;
    // Record these listneres so that they can be removed later on
    // or be checked
    private GraphEditorActionListener selectionListener;
    private GraphEditorActionListener actionListener;
    private ChangeListener viewChangeListener;
    private PropertyChangeListener visibleRectChangeListener;
    
    public PathwayOverviewPane() {
        isEditable = false;
        // For select rect
        prevPoint = new Point();
        repaintRect = new Rectangle();
        visibleRect = new Rectangle();
    }
    
    /**
     * Set the parent editor this overview pane monitors.
     * @param editor
     */
    public void setParentEditor(PathwayEditor editor) {
        this.parentEditor = editor;
        viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, editor);
        installListeners(editor);
    }

    /**
     * Hook up to the monitored PathwayEditor so that this Overview can get
     * the same view.
     * @param editor
     */
    private void installListeners(PathwayEditor editor) {
        if (selectionListener == null) {
            selectionListener = new GraphEditorActionListener() {
                public void graphEditorAction(GraphEditorActionEvent e) {
                    if (e.getID() == GraphEditorActionEvent.SELECTION) {
                        // selection should be repainted too.
                        repaint();
                    }
                }
            };
        }
        editor.getSelectionModel().addGraphEditorActionListener(selectionListener);
        // For GraphEditorActionEvents
        if (actionListener == null) {
            actionListener = new GraphEditorActionListener() {
                public void graphEditorAction(GraphEditorActionEvent e) {   
                    if (e.isRepaintableEvent())
                        repaint();
                }
            };
        }
        editor.addGraphEditorActionListener(actionListener);
    }
    
    /**
     * Call this method to synchronize the scroll views between two displays of 
     * a RenderablePathway.
     * @param zoomableEditor
     */
    public void syncrhonizeScroll(ZoomablePathwayEditor newZoomableEditor) {
        if (this.zoomableEditor != null && viewChangeListener != null)
            zoomableEditor.getPathwayScrollPane().getViewport().addChangeListener(viewChangeListener);
        this.zoomableEditor = newZoomableEditor;
        if (viewChangeListener == null) {
            viewChangeListener = new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (isFromOverview) {
                        isFromOverview = false; // Don't pump the state change event
                        return;
                    }
                    repaint();
                }
            };
        }
        zoomableEditor.getPathwayScrollPane().getViewport().addChangeListener(viewChangeListener);
        if (visibleRectChangeListener == null) {
            visibleRectChangeListener = new PropertyChangeListener() { 
                public void propertyChange(PropertyChangeEvent e) {
                    if (e.getPropertyName().equals("visibleRect")) {
                        Rectangle visibleRect = (Rectangle) e.getNewValue();
                        isFromOverview = true;
                        Point p = new Point(visibleRect.getLocation());
                        PathwayEditor pathwayEditor = zoomableEditor.getPathwayEditor();
                        p.x *= pathwayEditor.getScaleX();
                        p.y *= pathwayEditor.getScaleY();
                        zoomableEditor.getPathwayScrollPane().getViewport().setViewPosition(p);
                    }
                }
            };
        }
        addPropertyChangeListener(visibleRectChangeListener);
    }
    
    private void resetVisibleRect() {
        Rectangle rect = parentEditor.getVisibleRect();
        double scale = parentEditor.getScaleX(); // ScaleX and ScaleY should be the same
        visibleRect.x = (int)(rect.x / scale);
        visibleRect.y = (int)(rect.y / scale);
        visibleRect.width = (int)(rect.width / scale);
        visibleRect.height = (int)(rect.height / scale);
    }
    
    /**
     * Override the super class method to disable dragging
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (isDragging) {
            int x = (int)(e.getX() / scaleX);
            int y = (int)(e.getY() / scaleY);
            int dx = x - prevPoint.x;
            int dy = y - prevPoint.y;
            visibleRect.translate(dx, dy);
            calculateRepaintRect();
            repaint(repaintRect);
            //repaint();
            prevPoint.x = x;
            prevPoint.y = y;
            validateVisibleRect();
            firePropertyChange("visibleRect", null, visibleRect);
        }
    }

    private void calculateRepaintRect() {
        // The clip rectangle is tested against a scaled graphic context.
        prevRect = prevRect.union(visibleRect);
        // Make it a little buffer
        repaintRect.x = prevRect.x;
        repaintRect.y = prevRect.y;
        repaintRect.width = prevRect.width;
        repaintRect.height = prevRect.height;
        repaintRect.x *= scaleX;
        repaintRect.x -= 10;
        repaintRect.y *= scaleY;
        repaintRect.y -= 10;
        repaintRect.width *= scaleX;
        repaintRect.width += 20;
        repaintRect.height *= scaleY;
        repaintRect.height += 20;
    }
    
    public void mouseMoved(MouseEvent e) {
        if (blockAction())
            return;
        Point p = e.getPoint();
        p.x /= scaleX;
        p.y /= scaleY;
        if (visibleRect.contains(p)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        else
            setCursor(Cursor.getDefaultCursor());
    }
    
    public void mousePressed(MouseEvent e) {
        if (blockAction())
            return;
        Point p = e.getPoint();
        p.x /= scaleX;
        p.y /= scaleY;
        if (visibleRect.contains(p)) {
            prevPoint.x = p.x;
            prevPoint.y = p.y;
            prevRect = new Rectangle(visibleRect);
            isDragging = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }
    
    public void mouseReleased(MouseEvent e) {
        if (isDragging) {
            isDragging = false;
            //setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Validate the visible rectangle so that it is fit into the monitored PathwayEditor.
     * @return
     */
    private boolean validateVisibleRect() {
        boolean needFireEvent = false;
        int width = parentEditor.getWidth();
        int height = parentEditor.getHeight();
        double scale = parentEditor.getScaleX();
        if ((visibleRect.x * scale + viewport.getWidth()) > width) {
            visibleRect.x = (int)((width - viewport.getWidth()) / scale);
            needFireEvent = true;
        }
        if (visibleRect.x < 0) {
            visibleRect.x = 0;
            needFireEvent = true;
        }
        if ((visibleRect.y * scale + viewport.getHeight()) > height) {
            visibleRect.y = (int) ((height - viewport.getHeight()) / scale);
            needFireEvent = true;
        }
        if (visibleRect.y < 0) {
            visibleRect.y = 0;
            needFireEvent = true;
        }
        //System.out.println("VisibleRect: " + visibleRect);
        return needFireEvent;
    }
    
    public void zoomToFit() {
        Dimension size = getSize();
        Dimension preferredSize = getPreferredSize();
        double w = preferredSize.width / scaleX; // Scale back to the original dimension
        double h = preferredSize.height / scaleY;
        double scaleX = size.width / w;
        if (scaleX > 1.0)
            scaleX = 1.0;
        double scaleY = size.height / h;
        if (scaleY > 1.0)
            scaleY = 1.0;
        //zoom(scaleX, scaleY);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        //revalidate();
        // Don't call repaint here. It will go to an infinite loop.
    }
    
    // This will slow the drawing. But for a small pathway,
    // it should be OK.
    public void paint(Graphics g) {
        // The preferred size maybe change because of dragging so
        // it should be safe to call zoomToFit for each drawing.
        zoomToFit();
        super.paint(g);
        drawViewport(g);
    }
    
    private boolean blockAction() {
        if (viewport == null || displayedObject == null)
            return true; // This overview pane is not in a JScrollPane. No action should be done.
        // Don't allow any actions if the visible rectangle and the view rectangle
        // are the same: which means that everything is in the view.
        return parentEditor.getSize().equals(viewport.getViewRect().getSize());
    }

    private void drawViewport(Graphics g) {
        // Don't draw this rectangle if all is visible or 
        // nothing to be displayed
        if (viewport == null || displayedObject == null || getDisplayedObjects().size() == 0) 
            return;
        resetVisibleRect();
        if (visibleRect.isEmpty())
            return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(DefaultRenderConstants.SELECTION_WIDGET_BG_COLOR);
        g2.fill(visibleRect);
        // Want to draw at least one-pixel width line
        g2.setPaint(DefaultRenderConstants.SELECTION_WIDGET_COLOR);
        double strokeWidth = 1.0 / Math.max(scaleX, scaleY);
        if (strokeWidth < 1.0)
            strokeWidth = 1.0;
        Stroke stroke = new BasicStroke((float)strokeWidth);
        g2.setStroke(stroke);
        g2.draw(visibleRect);
    }
}
