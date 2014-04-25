/*
 * DefaultPathwayRenderer.java
 *
 * Created on June 23, 2003, 10:43 AM
 */

package org.gk.render;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
/**
 * This Renderer is used to draw a Pathway contained in another Pathway.
 * @author  wgm
 */
public class DefaultPathwayRenderer extends AbstractNodeRenderer {
    
    /** Creates a new instance of DefaultPathwayRenderer */
    public DefaultPathwayRenderer() {
    }
    
    public void render(Graphics g) {
        RenderablePathway pathway = (RenderablePathway) node;
        if (!pathway.isVisible() &&
            !pathway.isSelected())
            return;
        node.validateBounds(g);
        setProperties(node);
        Graphics2D g2 = (Graphics2D) g;
        List components = pathway.getComponents();
        // A RenderablePathway as a Container cannot be resized
        Rectangle rect = node.getBounds();
        Stroke oldStroke = g2.getStroke();
        if (isSelected) {
            g2.setPaint(DEFAULT_PATHWAY_SELECTION_BACKGROUND);
            g2.fill(rect);
            g2.setStroke(SELECTION_STROKE);
            g2.setPaint(SELECTION_WIDGET_COLOR);
            g2.draw(rect);
            g2.setStroke(oldStroke);
        }
        else {
            // Just draw an outline
            g2.setPaint(DEFAULT_OUTLINE_COLOR);
            g2.setStroke(BROKEN_LINE_STROKE);
            g2.draw(rect);
        }
        g2.setStroke(oldStroke);
    }
    
    @Override
    protected void renderShapes(Graphics g) {
    }
}
