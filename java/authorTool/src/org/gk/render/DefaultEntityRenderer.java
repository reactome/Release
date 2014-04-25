/*
 * BasicEntityRenderer.java
 *
 * Created on June 12, 2003, 10:15 PM
 */

package org.gk.render;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
/**
 * This Renderer implementation is used to draw an Entity.
 * @author  wgm
 */
public class DefaultEntityRenderer extends AbstractNodeRenderer {
   
    /** Creates a new instance of BasicEntityRenderer */
    public DefaultEntityRenderer() {
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
        g2.fill(bounds);
        Paint oldPaint = g2.getPaint();
        // Draw the outline
        Stroke stroke = g2.getStroke();
        setDrawPaintAndStroke(g2);
        g2.draw(bounds);
        g2.setStroke(stroke);
        g2.setPaint(oldPaint);
    }
}
