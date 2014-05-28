/*
 * Created on Jul 7, 2008
 *
 */
package org.gk.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

public class DefaultProcessNodeRenderer extends DefaultReactionNodeRenderer {

    /** Creates a new instance of DefaultPathwayRenderer */
    public DefaultProcessNodeRenderer() {
    }
    
    protected void renderShapes(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Draw three overlapping rectangles
        Stroke oldStroke = g2.getStroke();
        if (isSelected || isHighlited) {
            g2.setStroke(SELECTION_STROKE);
        }
        Rectangle bounds = node.getBounds();
        //drawRectangle(bounds, g2, false);
        Rectangle rect = new Rectangle();
        rect.x = bounds.x + RECTANGLE_DIST;
        rect.y = bounds.y + RECTANGLE_DIST;
        rect.width = bounds.width - 2 * RECTANGLE_DIST;
        rect.height = bounds.height - 2 * RECTANGLE_DIST;
        //drawRectangle(rect, g2, true);
        Color bg = background;
        if (bg == null)
            bg = DEFAULT_BACKGROUND;
        g2.setPaint(bg);
        g2.fill(bounds);
        if (isSelected) {
            g2.setPaint(SELECTION_WIDGET_COLOR);
        }
        else if (isHighlited)
            g2.setPaint(HIGHLIGHTED_COLOR);
        else if (node.lineColor != null)
            g2.setPaint(node.lineColor);
        else
            g2.setPaint(DEFAULT_OUTLINE_COLOR);
        g2.draw(bounds);
        // Draw another rectangle
        bg = bg.brighter();
        g2.setPaint(bg);
        g2.fill(rect);
        if (isSelected) {
            g2.setPaint(SELECTION_WIDGET_COLOR);
        }
        else if (isHighlited)
            g2.setPaint(HIGHLIGHTED_COLOR);
        else if (node.lineColor != null)
            g2.setPaint(node.lineColor);
        else
            g2.setPaint(DEFAULT_OUTLINE_COLOR);
        g2.draw(rect);
        if (isSelected || isHighlited)
            g2.setStroke(oldStroke);
    }
    

}
