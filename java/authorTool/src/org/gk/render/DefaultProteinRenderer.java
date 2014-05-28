/*
 * Created on Dec 19, 2006
 *
 */
package org.gk.render;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

public class DefaultProteinRenderer extends AbstractNodeRenderer {
    
    public DefaultProteinRenderer() {
        
    }
    
    protected void renderShapes(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Rectangle bounds = node.getBounds();
        renderShapes(bounds, g2);
    }
    
    protected void renderShapes(Rectangle bounds,
                                Graphics2D g2) {
        if (background == null) { 
            g2.setPaint(DEFAULT_BACKGROUND);
        }
        else {
            g2.setPaint(background);
        }
        g2.fillRoundRect(bounds.x,
                         bounds.y,
                         bounds.width,
                         bounds.height,
                         ROUND_RECT_ARC_WIDTH,
                         ROUND_RECT_ARC_WIDTH);
        // Draw the outline
        Stroke stroke = g2.getStroke();
        setDrawPaintAndStroke(g2);
        g2.drawRoundRect(bounds.x,
                         bounds.y,
                         bounds.width,
                         bounds.height,
                         ROUND_RECT_ARC_WIDTH,
                         ROUND_RECT_ARC_WIDTH);
        g2.setStroke(stroke);
    }
    
}
