/*
 * Created on Dec 20, 2006
 *
 */
package org.gk.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;


public class DefaultNoteRenderer extends AbstractNodeRenderer {
    
    public DefaultNoteRenderer() {
    }

    protected void renderShapes(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        // Draw the outline when this note is selected
        if (isSelected) {
            Stroke stroke = g2.getStroke();
            g2.setPaint(SELECTION_WIDGET_COLOR);
            g2.setStroke(SELECTION_STROKE);
            g2.draw(node.getBounds());
            g2.setStroke(stroke);
        }
        if (((Note)node).isPrivate()) {
            // Draw something as a hue for private note
            drawPrivateIcon(g2);
        }
    }
    
    private void drawPrivateIcon(Graphics2D g2) {
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(BROKEN_LINE_STROKE);
        g2.draw(node.getBounds());
        g2.setStroke(oldStroke);
        // Add another rectangle
        int x = node.getBounds().x + node.getBounds().width - ROUND_RECT_ARC_WIDTH;
        int y = node.getBounds().y;
        g2.drawRect(x, y, ROUND_RECT_ARC_WIDTH, ROUND_RECT_ARC_WIDTH);
        // Color a triangle
        GeneralPath path = new GeneralPath();
        path.moveTo(x, y);
        x += ROUND_RECT_ARC_WIDTH;
        y += ROUND_RECT_ARC_WIDTH;
        path.lineTo(x, y);
        x -= ROUND_RECT_ARC_WIDTH;
        path.lineTo(x, y);
        path.closePath();
        Color oldColor = g2.getColor();
        g2.setPaint(Color.LIGHT_GRAY);
        g2.fill(path);
        g2.setPaint(oldColor);
    }

}
