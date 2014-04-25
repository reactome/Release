/*
 * Created on Jun 25, 2008
 *
 */
package org.gk.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

/*
 * This class is used to renderer SinkOrSource
 */
public class SourceOrSinkRenderer extends AbstractNodeRenderer {
    
    public SourceOrSinkRenderer() {
        background = Color.white;
    }
    
    /**
     * Render method.
     */
    public void render(Graphics g) {
        node.validateBounds(g);
        setProperties(node);
        drawResizeWidgets(g);
        renderShapes(g);
    }
    
    public void renderShapes(Graphics g) {
        Rectangle bounds = node.getBounds();
        Graphics2D g2 = (Graphics2D) g;
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(DEFAULT_THICK_STROKE);
        Color bgColor = node.getBackgroundColor();
        if (bgColor != null) {
            g2.setPaint(bgColor);
            g2.fillOval(bounds.x,
                       bounds.y, 
                       bounds.width, 
                       bounds.height);
        }
        if (node.isSelected()) {
            g2.setPaint(SELECTION_WIDGET_COLOR);
            g2.setStroke(SELECTION_STROKE);
        }
        else {
            Color fgColor = node.lineColor;
            if (fgColor != null) 
                g2.setPaint(fgColor);
            else
                g2.setPaint(DEFAULT_FOREGROUND);
        }
        g.drawOval(bounds.x, 
                   bounds.y,
                   bounds.width, 
                   bounds.height);
        // Draw a line
        g.drawLine(bounds.x + bounds.width, 
                   bounds.y, 
                   bounds.x,
                   bounds.y + bounds.height);
        g2.setStroke(oldStroke);
    }
    
}
