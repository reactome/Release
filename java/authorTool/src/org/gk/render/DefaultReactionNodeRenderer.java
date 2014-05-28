/*
 * Created on Jul 18, 2003
 */
package org.gk.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;

/**
 * This node is used to display a RenderableReaction as a node.
 * @author wgm
 */
public class DefaultReactionNodeRenderer extends AbstractNodeRenderer {

	public DefaultReactionNodeRenderer() {
	}
	
	protected void renderShapes(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
        Rectangle bounds = node.getBounds();
		// Draw three overlapping rectangles
		drawRectangle(bounds, g2, false);
	}
    
    protected void drawRectangle(Rectangle rect, 
                                 Graphics2D g2,
                                 boolean needBrighter) {
        GeneralPath path = new GeneralPath();
        int w = rect.width / 3;
        int w1 = w / 4;
        int h = rect.height / 3;
        int h1 = h / 4;
        int x = rect.x + w;
        int y = rect.y;
        path.moveTo(x, y);
        x += w;
        path.lineTo(x, y);
        x = rect.x + rect.width;
        y = rect.y + h;
        int x1 = x - w1;
        int y1 = rect.y + h1;
        path.quadTo(x1, y1, x, y);
        y += h;
        path.lineTo(x, y);
        x = rect.x + 2 * w;
        y = rect.y + rect.height;
        y1 = rect.y + rect.height - h1;
        path.quadTo(x1, y1, x, y);
        x = rect.x + w;
        path.lineTo(x, y);
        x = rect.x;
        y = rect.y + 2 * h;
        x1 = rect.x + w1;
        path.quadTo(x1, y1, x, y);
        y = rect.y + h;
        path.lineTo(x, y);
        x = rect.x + w;
        y = rect.y;
        y1 = rect.y + h1;
        path.quadTo(x1, y1, x, y);
        Color bg = background;
        if (bg == null)
            bg = DEFAULT_BACKGROUND;
        if (needBrighter)
            bg = bg.brighter();
        g2.setPaint(bg);
        g2.fill(path);
        Stroke oldStroke = g2.getStroke();
        if (isSelected) {
            g2.setPaint(SELECTION_WIDGET_COLOR);
            g2.setStroke(SELECTION_STROKE);
        }
        else if (node.lineColor != null)
            g2.setPaint(node.lineColor);
        else
            g2.setPaint(DEFAULT_OUTLINE_COLOR);
        g2.draw(path);
        if (isSelected)
            g2.setStroke(oldStroke);
    }
		
}
