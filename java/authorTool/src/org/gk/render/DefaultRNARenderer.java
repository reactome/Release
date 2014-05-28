/*
 * Created on Dec 19, 2006
 *
 */
package org.gk.render;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;

public class DefaultRNARenderer extends AbstractNodeRenderer {
    
    public DefaultRNARenderer() {
        
    }
    
    protected void renderShapes(Graphics g) {
        // Use a path
        GeneralPath path = new GeneralPath();
        Rectangle bounds = node.getBounds();
        int x = bounds.x + RenderableRNA.LOOP_WIDTH;
        int y = bounds.y + RenderableRNA.LOOP_WIDTH / 2;
        path.moveTo(x, y);
        x = bounds.x + bounds.width - RenderableRNA.LOOP_WIDTH;
        path.lineTo(x, y);
        int x1 = bounds.x + bounds.width;
        int y1 = bounds.y;
        x = bounds.x + bounds.width;
        y = bounds.y + bounds.height / 2;
        path.quadTo(x1, y1, x, y);
        y1 = bounds.y + bounds.height;
        x = bounds.x + bounds.width - RenderableRNA.LOOP_WIDTH;
        y = bounds.y + bounds.height - RenderableRNA.LOOP_WIDTH / 2;
        path.quadTo(x1, y1, x, y);
        x = bounds.x + RenderableRNA.LOOP_WIDTH;
        path.lineTo(x, y);
        x1 = bounds.x;
        y1 = bounds.y + bounds.height;
        x = x1;
        y = bounds.y + bounds.height / 2;
        path.quadTo(x1, y1, x, y);
        y1 = bounds.y;
        x = bounds.x + RenderableRNA.LOOP_WIDTH;
        y = bounds.y + RenderableRNA.LOOP_WIDTH / 2;
        path.quadTo(x1, y1, x, y);
        Graphics2D g2 = (Graphics2D) g;
        if (background == null) { 
            g2.setPaint(DEFAULT_BACKGROUND);
        }
        else {
            g2.setPaint(background);
        }
        g2.fill(path);
        // Draw the outline
        Stroke stroke = g2.getStroke();
        setDrawPaintAndStroke(g2);
        g2.draw(path);
        g2.setStroke(stroke);
    }
    
}
