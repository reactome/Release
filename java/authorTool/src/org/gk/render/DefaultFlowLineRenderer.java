/*
 * Created on Dec 11, 2006
 *
 */
package org.gk.render;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;

import org.gk.util.DrawUtilities;


/**
 * This Renderer is used to draw Interaction objects.
 * @author guanming
 *
 */
public class DefaultFlowLineRenderer extends DefaultReactionRenderer {
    
    public DefaultFlowLineRenderer() {
    }

    public void render(Graphics g) {
        if (!shouldRender())
            return;
        FlowLine flowLine = (FlowLine) reaction;
        if (flowLine == null)
            return;
        Graphics2D g2 = (Graphics2D) g;
        setLineColor(flowLine, 
                     g2);
        Stroke oldStroke = g2.getStroke();
        Stroke lineStroke = new BasicStroke(flowLine.getLineWidth());
        g2.setStroke(lineStroke);
        // These are the control points for a reaction
        Point inputHub, reactionHub, outputHub;
        // Only the backbone needs to be drawn
        Point prevP, nextP = null; // For drawing line segments
        List<Point> backbonePoints = flowLine.getBackbonePoints();
        inputHub = (Point) backbonePoints.get(0);
        outputHub = (Point) backbonePoints.get(backbonePoints.size() - 1);
        reactionHub = flowLine.getPosition();
        // This list should have no less than three elements 
        prevP = (Point) backbonePoints.get(0);
        for (int i = 1; i < backbonePoints.size(); i++) {
            nextP = (Point) backbonePoints.get(i);
            g2.drawLine(prevP.x, prevP.y, nextP.x, nextP.y);
            prevP = nextP;
        }
        g2.setStroke(oldStroke);
        if (flowLine instanceof RenderableInteraction) {
            InteractionType type = ((RenderableInteraction)flowLine).getInteractionType();
            drawInteractionType(outputHub, type, backbonePoints, g2);
        }
        else {
            drawInteractionType(outputHub, null, backbonePoints, g2);
        }
        if (flowLine.isSelected())
            drawSelectionWidgets(g2);
    }

    protected void setLineColor(FlowLine flowLine, Graphics2D g2) {
        if (flowLine.isHighlighted())
            g2.setPaint(HIGHLIGHTED_COLOR);
        else if (flowLine.getLineColor() == null)
            g2.setPaint(DEFAULT_FOREGROUND);
        else
            g2.setPaint(flowLine.getLineColor());
    }
    
    private void drawInteractionType(Point outputHub,
                                     InteractionType type,
                                     List<Point> backbonePoints,
                                     Graphics2D g2) {
        Point secondPoint = (Point) backbonePoints.get(backbonePoints.size() - 2);
        // Use arrow for activate. Arrows seem more popular than plus.
        if ((type == null && reaction.isNeedOutputArrow()) ||
            type == InteractionType.ENCODE) {
            // Draw an arrow: used as the classic flow line
            DrawUtilities.drawArrow(outputHub, 
                                    secondPoint, 
                                    g2);
        }
        else if (type == InteractionType.INTERACT) {
            // Do nothing
        }
        else if (type == InteractionType.ACTIVATE) {
            drawActivatorSymbol(outputHub, 
                                g2);
        }
        else if (type == InteractionType.INHIBIT) {
            drawInhibitorSymbol(outputHub, secondPoint, g2);
        }
    }
    
    private void drawActivatorSymbol(Point outputHub, 
                                     Graphics2D g2) {
        // Have to find which direction 
        Node node = (Node) reaction.getOutputNode(0);
        if (node == null)
            return;
        List<Point> backbonePoints = reaction.getBackbonePoints();
        Point controlP = (Point) backbonePoints.get(backbonePoints.size() - 2);
        DrawUtilities.drawHollowArrow(outputHub, 
                                      controlP,
                                      g2);
    }

    private void drawInhibitorSymbol(Point outputHub, 
                                     Point controlPoint,
                                     Graphics2D g2) {
        // Have to find which direction outputHub is
        Node node = (Node) reaction.getOutputNode(0);
        if (node == null) {
            DrawUtilities.drawArrow(outputHub, controlPoint, g2);
            return; // might be from detaching
        }
        Rectangle bounds = node.getBounds();
        int y = outputHub.y;
        if ((node instanceof RenderableGene) || // Always draw a vertical line for genes
            (y < bounds.getY()) || 
            (y > bounds.getMaxY())) {
            // use horizontal line
            int x1 = outputHub.x - CIRCLE_SIZE;
            int x2 = outputHub.x + CIRCLE_SIZE;
            g2.drawLine(x1, outputHub.y, x2, outputHub.y);
        }
        else {
            // Draw vertical line
            int y1 = outputHub.y - CIRCLE_SIZE;
            int y2 = outputHub.y + CIRCLE_SIZE;
            g2.drawLine(outputHub.x, y1,
                        outputHub.x, y2);
        }
    }   
}
