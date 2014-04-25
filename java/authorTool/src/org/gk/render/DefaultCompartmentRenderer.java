/*
 * Created on Dec 19, 2006
 *
 */
package org.gk.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;

public class DefaultCompartmentRenderer extends AbstractNodeRenderer {
    private Rectangle insets;
    
    public DefaultCompartmentRenderer() {
        insets = new Rectangle();
    }
    
    private boolean isInsetsNeeded() {
        if (node instanceof RenderableCompartment)
            return ((RenderableCompartment)node).isInsetsNeeded();
        return node.getDisplayName() != null && !node.getDisplayName().endsWith("membrane");
    }
    
    
    
    @Override
    protected void drawResizeWidgets(Graphics g) {
        if (!node.isSelected())
            return;
        drawResizeWidgets(g, node.getBounds());
        if (node instanceof RenderableCompartment) {
            RenderableCompartment comp = (RenderableCompartment) node;
            Rectangle insets = comp.getInsets();
            if (insets != null)
                drawResizeWidgets(g, insets);
        }
    }

    protected void renderShapes(Graphics g) {
        // This must be true
        RenderableCompartment comp = (RenderableCompartment) node;
        Graphics2D g2 = (Graphics2D) g;
        Rectangle bounds = node.getBounds();
        boolean isInsetsNeeded = isInsetsNeeded();
        if (isInsetsNeeded) {
            // For old format compatibility
            if (comp.getInsets() == null || comp.getInsets().isEmpty()) {
                insets.x = bounds.x + RECTANGLE_DIST;
                insets.y = bounds.y + RECTANGLE_DIST;
                insets.width = bounds.width - 2 * RECTANGLE_DIST;
                insets.height = bounds.height - 2 * RECTANGLE_DIST;
            }
            else {
                Rectangle original = comp.getInsets();
                insets.x = original.x;
                insets.y = original.y;
                insets.width = original.width;
                insets.height = original.height;
            }
        }
        // Draw background if any
        if (node.getBackgroundColor() != null) {
            //g2.setPaint(node.getBackgroundColor());
            Color bg = node.getBackgroundColor();
            Color color = new Color(bg.getRed(),
                                    bg.getGreen(),
                                    bg.getBlue(),
                                    150); // 75% 
            g2.setPaint(color);
            g2.fillRoundRect(bounds.x, 
                             bounds.y, 
                             bounds.width,
                             bounds.height,
                             2 * ROUND_RECT_ARC_WIDTH, 
                             2 * ROUND_RECT_ARC_WIDTH);
            if (isInsetsNeeded) {
                bg = PANEL_BACKGROUND;
                color = new Color(bg.getRed(),
                                  bg.getGreen(),
                                  bg.getBlue(),
                                  150); // 50% 
                g2.setPaint(color);
                //g2.setPaint(PANEL_BACKGROUND);
                g2.fillRoundRect(insets.x, 
                                 insets.y,
                                 insets.width,
                                 insets.height,
                                 2 * ROUND_RECT_ARC_WIDTH, 
                                 2 * ROUND_RECT_ARC_WIDTH);
            }
        }
        Stroke oldStroke = g2.getStroke();
        Paint oldPaint = g2.getPaint();
        if (isSelected) {
            // Draw the outline
            g2.setPaint(SELECTION_WIDGET_COLOR);
            g2.setStroke(SELECTION_STROKE);
        }
        else if (isHighlited) {
            g2.setPaint(HIGHLIGHTED_COLOR);
            g2.setStroke(DEFAULT_THICK_STROKE);
        }
        else if (node.lineColor != null) {
            g2.setPaint(node.lineColor);
            g2.setStroke(DEFAULT_THICK_STROKE);
        }
        else {
            g2.setPaint(COMPARTMENT_OUTLINE_COLOR);
            g2.setStroke(DEFAULT_THICK_STROKE);
        }
        g2.drawRoundRect(bounds.x,
                         bounds.y,
                         bounds.width,
                         bounds.height,
                         2 * ROUND_RECT_ARC_WIDTH,
                         2 * ROUND_RECT_ARC_WIDTH);
        if (isInsetsNeeded) {
            g2.drawRoundRect(insets.x, 
                             insets.y,
                             insets.width,
                             insets.height,
                             2 * ROUND_RECT_ARC_WIDTH, 
                             2 * ROUND_RECT_ARC_WIDTH);   
        }
        g2.setStroke(oldStroke);
        g2.setPaint(oldPaint);
    }
}
