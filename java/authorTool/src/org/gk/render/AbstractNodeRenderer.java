/*
 * Created on Dec 19, 2006
 *
 */
package org.gk.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.List;

public abstract class AbstractNodeRenderer implements Renderer, DefaultRenderConstants {
    protected Node node;
    protected transient java.util.List textLayouts;
    protected int boundsBuffer;
    // Default color
    protected Color foreground = Color.black;
    protected Color background;
    // selection status
    protected boolean isSelected;
    protected boolean isHighlited;
    
    /**
     * Render method.
     */
    public void render(Graphics g) {
        if (!node.isVisible() && !node.isSelected())
            return;
        node.validateBounds(g);
        setProperties(node);
        if (node.isMultimerFormable() && node.getMultimerMonomerNumber() > 1) {
            // Draw a shade
            g.translate(MULTIMER_RECT_DIST, MULTIMER_RECT_DIST);
            renderShapes(g);
            g.translate(-MULTIMER_RECT_DIST, -MULTIMER_RECT_DIST);
        }
        renderShapes(g);
        drawResizeWidgets(g);
        drawNodeAttachments(g);
        drawMultimerMonomerNumber(g);
        RenderUtility.drawName(node, (Graphics2D)g);
    }

    protected abstract void renderShapes(Graphics g);
    
    protected void setDrawPaintAndStroke(Graphics2D g2) {
        // Set line color first
        if (isSelected)
            g2.setPaint(SELECTION_WIDGET_COLOR);
        else if (isHighlited)
            g2.setPaint(HIGHLIGHTED_COLOR);
        else if (node.lineColor != null)
            g2.setPaint(node.lineColor);
        else
            g2.setPaint(DEFAULT_OUTLINE_COLOR);
        // Set stroke
        if (isSelected || isHighlited)
            g2.setStroke(SELECTION_STROKE);
        else if (node.getLineWidth() != null) {
            Stroke stroke = null;
            if (node.needDashedBorder)
                stroke = new BasicStroke(node.getLineWidth(), 
                                         BasicStroke.CAP_BUTT,
                                         BasicStroke.JOIN_BEVEL, 
                                         0,
                                         new float[]{6, 6}, 
                                         0);
            else
                stroke = new BasicStroke(node.getLineWidth());
            g2.setStroke(stroke);
        }
        else {
            if (node.needDashedBorder)
                g2.setStroke(THICK_BROKEN_LINE_STROKE);
            else
                g2.setStroke(DEFAULT_STROKE);
        }
    }
    
    protected void drawNodeAttachments(Graphics g) {
        List<NodeAttachment> attachments = node.getNodeAttachments();
        if (attachments != null && attachments.size() > 0)
            for (NodeAttachment attachment : attachments)
                drawNodeAttachment(attachment, g);
    }
    
    protected void drawMultimerMonomerNumber(Graphics g) {
        if (!node.isMultimerFormable() ||
            node.getMultimerMonomerNumber() < 2)
            return;
        RenderableFeature feature = new RenderableFeature();
        feature.setLabel(node.getMultimerMonomerNumber() + "");
        feature.setRelativePosition(0.2, 0.0);
        drawNodeAttachment(feature, g);
    }
        
    /**
     * A helper method is used to draw RenderaleFeature
     * @param attachment
     * @param g
     */
    protected void drawNodeAttachment(NodeAttachment attachment,
                                      Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        g2.setPaint(PANEL_BACKGROUND);
        g2.setStroke(DEFAULT_STROKE);
        // Need to calculate the size of the text
        // Use a smaller font
        attachment.validateBounds(node.getBounds(), g);
        Rectangle featureBounds = attachment.getBounds();
        // Should set the font after the above checking.
        Font oldFont = g2.getFont();
        Font font = oldFont.deriveFont(oldFont.getSize2D() - 3.0f);
        g2.setFont(font);
        if (attachment instanceof RenderableState)
            g2.fillOval(featureBounds.x,
                        featureBounds.y,
                        featureBounds.width,
                        featureBounds.height);
        else
            g2.fill(featureBounds);
        if (attachment.isSelected()) {
            g2.setPaint(SELECTION_WIDGET_COLOR);
            g2.setStroke(SELECTION_STROKE);
        }
        else
            g2.setPaint(DEFAULT_FOREGROUND);
        if (attachment instanceof RenderableState) {
            g2.drawOval(featureBounds.x,
                        featureBounds.y,
                        featureBounds.width,
                        featureBounds.height);
        }
        else
            g2.draw(featureBounds);
        FontMetrics metrics = g2.getFontMetrics(font);
        Rectangle2D txtBounds = metrics.getStringBounds(attachment.getLabel(), 
                                                        g2);
        int x = (int) (featureBounds.x + (featureBounds.width - txtBounds.getWidth()) / 2);
        int y = (int) (featureBounds.y + (featureBounds.height - txtBounds.getHeight()) / 2);
        y += metrics.getAscent();
        g2.drawString(attachment.getLabel(), 
                      x, 
                      y + 1);
        // Need to draw description if any
        // As of Jan 12, 2010, don't draw description, which interfere the main text rendering for nodes.
//        if (attachment instanceof RenderableFeature) {
//            String desc = attachment.getDescription();
//            if (desc != null) {
//                // Need to find the position of x, y
//                Rectangle2D descBounds = metrics.getStringBounds(desc, g2);
//                drawFeatureDescription(desc, 
//                                       featureBounds, 
//                                       descBounds, 
//                                       x,
//                                       y + 1,
//                                       g2);
//            }
//        }
        g2.setFont(oldFont);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
    }
    
    private void drawFeatureDescription(String desc,
                                        Rectangle featureBounds,
                                        Rectangle2D descBounds,
                                        int labelX,
                                        int labelY,
                                        Graphics2D g2) {
        // Need to calculate x, y for description.
        // Use double to do accurate calculation
        double featureX = featureBounds.getCenterX();
        double featureY = featureBounds.getCenterY();
        double nodeX = node.getBounds().getX();
        double nodeY = node.getBounds().getY();
        double nodeMaxX = node.getBounds().getMaxX();
        double nodeMaxY = node.getBounds().getMaxY();
        int x = labelX, y = labelY; // Should not occur
        if (Math.abs(featureX - nodeX) <= 1.0) {
            // West
            x = featureBounds.x + featureBounds.width + 2;
            y = labelY;
        }
        else if (Math.abs(featureX - nodeMaxX) <= 1.0) {
            // East
            x = (int)(featureBounds.x - descBounds.getWidth() - 2);
            y = labelY;
        }
        else if (Math.abs(featureY - nodeY) <= 1.0) {
            // North
            x = (int) (featureBounds.x - (descBounds.getWidth() - featureBounds.width) / 2);
            y = (int) (labelY + descBounds.getHeight() + 2);
        }
        else if (Math.abs(featureY - nodeMaxY) <= 1.0) {
            x = (int) (featureBounds.x - (descBounds.getWidth() - featureBounds.width) / 2);
            y = (int) (labelY - featureBounds.getHeight());
        }
        g2.drawString(desc, x, y);
    }
    
    protected void drawResizeWidgets(Graphics g) {
        if (!node.isSelected())
            return;
        Rectangle bounds = node.getBounds();
        drawResizeWidgets(g, bounds);
    }

    protected void drawResizeWidgets(Graphics g, Rectangle bounds) {
        int x = bounds.x;
        int y = bounds.y;
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(SELECTION_WIDGET_COLOR);
        drawResizeWidgets(x, y, g2);
        x = bounds.x + bounds.width;
        drawResizeWidgets(x, y, g2);
        y = bounds.y + bounds.height;
        drawResizeWidgets(x, y, g2);
        x = bounds.x;
        drawResizeWidgets(x, y, g2);
    }
    
    protected void drawResizeWidgets(int x, int y, Graphics2D g2) {
        g2.fillRect(x - Node.RESIZE_WIDGET_WIDTH,
                    y - Node.RESIZE_WIDGET_WIDTH,
                    2 * Node.RESIZE_WIDGET_WIDTH,
                    2 * Node.RESIZE_WIDGET_WIDTH);
    }
    
    public void setRenderable(Renderable r) {
        node = (Node) r;
    }
    
    /**
     * Prepare this Renderer with infomation in Renderable.
     */
    protected void setProperties(Renderable renderable) {
        Node node = (Node) renderable;
        isSelected = node.isSelected();
        isHighlited = node.isHighlighted();
        background = renderable.getBackgroundColor();
        foreground = renderable.getForegroundColor();
        textLayouts = node.getTextLayouts();
        boundsBuffer = node.boundsBuffer;
    }  
}
