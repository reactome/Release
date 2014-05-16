/*
 * Created on Oct 17, 2011
 *
 */
package org.gk.render;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * @author gwu
 *
 */
public class DefaultEntitySetAndMemberLinkRenderer extends DefaultFlowLineRenderer {
    private boolean hideOutput;
    private float[] dashPattern;
    
    public DefaultEntitySetAndMemberLinkRenderer() {
        dashPattern = new float[]{5.0f, 5.0f}; // Default dash pattern
    }
    
    public void setDashPattern(float[] pattern) {
        this.dashPattern = pattern;
    }
    
    public float[] getDashPattern() {
        return this.dashPattern;
    }
    
    public boolean isHideOutput() {
        return hideOutput;
    }

    public void setHideOutput(boolean hideOutput) {
        this.hideOutput = hideOutput;
    }
    

    @Override
    protected boolean shouldRender() {
        boolean rtn = super.shouldRender();
        if (!rtn)
            return false;
        // This is a workaround to fix a bug in the pathway diagram
        Node input = reaction.getInputNode(0);
        Node output = reaction.getOutputNode(0);
        if (input == output)
            return false;
        return true;
    }

    public void render(Graphics g) {
        if (!shouldRender())
            return;
        FlowLine link = (FlowLine) reaction;
        Graphics2D g2 = (Graphics2D) g;
        setLineColor(link, 
                     g2);
        Stroke oldStroke = g2.getStroke();
        Stroke lineStroke = new BasicStroke(link.getLineWidth(),
                                            BasicStroke.CAP_SQUARE,
                                            BasicStroke.JOIN_MITER,
                                            10.0f,
                                            dashPattern,
                                            0.0f);
        g2.setStroke(lineStroke);
        // Only the backbone needs to be drawn
        Point prevP, nextP = null; // For drawing line segments
        List<Point> backbonePoints = link.getBackbonePoints();
        // This list should have no less than three elements 
        prevP = (Point) backbonePoints.get(0);
        for (int i = 1; i < backbonePoints.size(); i++) {
            nextP = (Point) backbonePoints.get(i);
            g2.drawLine(prevP.x, prevP.y, nextP.x, nextP.y);
            prevP = nextP;
        }
        if (!hideOutput) {
            // Last point should be for output
            Point outputHub = backbonePoints.get(backbonePoints.size() - 1);
            float x = outputHub.x;
            float y = outputHub.y;
            x -= EDGE_MODULATION_WIDGET_WIDTH / 3.0f;
            y -= EDGE_MODULATION_WIDGET_WIDTH / 3.0f;
            g2.fill(new Ellipse2D.Float(x, 
                                        y, 
                                        EDGE_MODULATION_WIDGET_WIDTH / 1.5f, 
                                        EDGE_MODULATION_WIDGET_WIDTH / 1.5f));
        }
        g2.setStroke(oldStroke);
        if (link.isSelected())
            drawSelectionWidgets(g2);
    }
}
