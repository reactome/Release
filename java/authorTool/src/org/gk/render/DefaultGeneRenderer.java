/*
 * Created on Dec 12, 2006
 *
 */
package org.gk.render;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;

import org.gk.util.DrawUtilities;

/**
 * There are two parts to display a gene: the text part is the same as the normal RenderableEntity,
 * and the glyph part is for genes only. 
 * @author guanming
 *
 */
public class DefaultGeneRenderer extends AbstractNodeRenderer {
    
    public DefaultGeneRenderer() {
    }

    protected void renderShapes(Graphics g) {
        Rectangle bounds = node.getBounds();
        Rectangle textBounds = node.getTextBounds();
        int x = bounds.x;
        int y = textBounds.y;
        int w = bounds.width;
        int h = textBounds.height;
        Rectangle drawingBounds = new Rectangle(x, y, w, h);
        // Directly draw text: no need to draw background for genes
        Graphics2D g2 = (Graphics2D) g;
        //g2.draw(bounds);
        // Draw the outline
        Stroke stroke = g2.getStroke();
        // Draw bounds when selected
        if (isSelected) {
            g2.setPaint(SELECTION_WIDGET_COLOR);
            g2.setStroke(SELECTION_STROKE);
            g2.draw(drawingBounds);
            g2.setStroke(stroke);
        }
        drawGeneSymbol(g2, drawingBounds);
    }
    
    protected void drawResizeWidgets(Graphics g) {
        if (!node.isSelected())
            return;
        Rectangle bounds = node.getBounds();
        int x = bounds.x;
        int y = bounds.y + bounds.height;
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(SELECTION_WIDGET_COLOR);
        // Don't draw resize widgets at the northern side.
        drawResizeWidgets(x, y, g2);
        x = bounds.x + bounds.width;
        drawResizeWidgets(x, y, g2);
    }
    
    private void drawGeneSymbol(Graphics2D g2,
                                Rectangle bounds) {
        if (isSelected)
            g2.setPaint(SELECTION_WIDGET_COLOR);
        else if (node.lineColor != null)
            g2.setPaint(node.lineColor);
        else
            g2.setPaint(DEFAULT_OUTLINE_COLOR);
        // Draw the horizontal line
        double x1 = bounds.x;
        double y1 = bounds.y;
        double x2 = bounds.x + bounds.width;
        double y2 = y1;
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(GENE_SYMBOL_STROKE);
        g2.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
        // Draw the vertical line
        x1 = x2 - RenderableGene.GENE_SYMBOL_PAD;
        x2 = x1;
        y2 = y1 - RenderableGene.GENE_SYMBOL_WIDTH / 2.0;
        // Looks nice with one pixel offset
        g2.drawLine((int)x1, (int)y1 - 1, (int)x2, (int)y2);
        // another very short horizontal line
        x1 += RenderableGene.GENE_SYMBOL_PAD;
        g2.drawLine((int)x2, (int)y2, (int)x1, (int)y2);
        g2.setStroke(oldStroke);
        // draw the arrow
        DrawUtilities.drawArrow(new Point((int)x1 + DrawUtilities.ARROW_LENGTH, (int)y2), 
                                new Point((int)x2, (int)y2), 
                                g2);
    }
}
