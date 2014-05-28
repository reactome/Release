/*
 * Created on Dec 19, 2006
 *
 */
package org.gk.render;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

public class DefaultChemicalRenderer extends AbstractNodeRenderer {
    
    public DefaultChemicalRenderer() {
    }
    
    protected void renderShapes(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (background == null) { 
            g2.setPaint(DEFAULT_BACKGROUND);
        }
        else {
            g2.setPaint(background);
        }
        Rectangle bounds = node.getBounds();
        g2.fillOval(bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height);
        // Draw the outline
        Stroke stroke = g2.getStroke();
        setDrawPaintAndStroke(g2);
        g2.drawOval(bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height);
        g2.setStroke(stroke);
    }
    
    
}
