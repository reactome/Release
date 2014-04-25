/*
 * DefaultReactionRenderer.java
 *
 * Created on June 13, 2003, 10:27 AM
 */

package org.gk.render;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Stroke;
import java.util.Iterator;
import java.util.List;

import org.gk.util.DrawUtilities;
/**
 * This class describes a simple way to draw reactions: use straight line segments.
 * @author  wgm
 */
public class DefaultReactionRenderer implements Renderer, DefaultRenderConstants {
    protected final int CIRCLE_SIZE = 5;
    // Rendering info
    protected HyperEdge reaction;
    
    /** Creates a new instance of DefaultReactionRenderer */
    public DefaultReactionRenderer() {
    }
    
    protected boolean shouldRender() {
        if (reaction == null || 
            (!reaction.isVisible() && !reaction.isSelected()))
            return false;
        return true;
    }
    
    
    /** 
     * Rendering method.
     */
    public void render(Graphics g) {
        if (!shouldRender())
            return;
        Graphics2D g2 = (Graphics2D) g;
        if (reaction.isSelected) {
            g2.setPaint(SELECTION_WIDGET_COLOR);
        }
        else if (reaction.isHighlighted())
            g2.setPaint(HIGHLIGHTED_COLOR);
        else if (reaction.lineColor != null)
        	g2.setPaint(reaction.lineColor);
        else
        	g2.setPaint(DEFAULT_FOREGROUND);
        Stroke oldStroke = g2.getStroke();
        if (reaction.isSelected || reaction.isHighlighted()) {
            g2.setStroke(DEFAULT_LINE_SELECTION_STROKE);
        }
        else {
            Stroke lineStroke = new BasicStroke(reaction.getLineWidth());
            g2.setStroke(lineStroke);
        }
        // These are the control points for a reaction
        Point inputHub, reactionHub, outputHub;
        // Draw the backbone first
        Point prevP, nextP = null; // For drawing line segments
        java.util.List backbonePoints = reaction.getBackbonePoints();
        inputHub = (Point) backbonePoints.get(0);
        outputHub = (Point) backbonePoints.get(backbonePoints.size() - 1);
        reactionHub = reaction.getPosition();
        // This list should have no less than three elements 
        prevP = (Point) backbonePoints.get(0);
        for (int i = 1; i < backbonePoints.size(); i++) {
            nextP = (Point) backbonePoints.get(i);
            g2.drawLine(prevP.x, prevP.y, nextP.x, nextP.y);
            prevP = nextP;
        }
        // Draw the input branches
        if (reaction.getInputPoints() != null) {
            drawBranches(reaction.getInputPoints(), inputHub, g2);        
            if (reaction.isNeedInputArrow())
                drawArrows(reaction.getInputPoints(), inputHub, g2);
        }
        else if (reaction.isNeedInputArrow()) 
            DrawUtilities.drawArrow(inputHub, (Point)backbonePoints.get(1), g2);
        // Draw the output branches
        if (reaction.getOutputPoints() != null) {
            drawBranches(reaction.getOutputPoints(), outputHub, g2);
            if (reaction.isNeedOutputArrow())
                drawArrows(reaction.getOutputPoints(), outputHub, g2);
        }
        else if (reaction.isNeedOutputArrow())
            DrawUtilities.drawArrow(outputHub, (Point) backbonePoints.get(backbonePoints.size() - 2), g2);
        // Draw the helper branches
        if (reaction.getHelperPoints() != null) {
            drawCatalystBranches(reaction.getHelperPoints(),
        	                    reaction.getPosition(),
        	                    g2);
        }
        // Draw inhibitor branches
		if (reaction.getInhibitorPoints() != null && 
		    reaction.getInhibitorPoints().size() > 0) {
			drawInhibitorBranches(reaction.getInhibitorPoints(), 
			                     reaction.getPosition(),
			                     g2);
		}
		// Draw activator branches
		if (reaction.getActivatorPoints() != null &&
		    reaction.getActivatorPoints().size() > 0) {
				drawActivatorBranches(reaction.getActivatorPoints(), 
				                      reaction.getPosition(),
				                      g2);			
		}
		Point typePos = null;
		// Draw types
		if (reaction instanceof RenderableReaction)
		    typePos = drawReactionType(g2);
        // Draw selection widgets
        if (reaction.isSelected()) {
            drawSelectionWidgets(g2);
        }
        drawStoichiometries(g2,
                            typePos);
        g2.setStroke(oldStroke);
    }
    
    private void drawStoichiometries(Graphics2D g2,
                                     Point typePos) {
        ConnectInfo info = reaction.getConnectInfo();
        List widgets = info.getConnectWidgets();
        if (widgets == null || widgets.size() == 0)
            return;
        for (Object obj : widgets) {
            ConnectWidget widget = (ConnectWidget) obj;
            int stoi = widget.getStoichiometry();
            if (stoi == 1) // Don't draw the default value
                continue;
            Point p1 = widget.getPoint();
            Point p2 = widget.getControlPoint();
            drawStoichiometry(stoi,
                              p1,
                              p2, 
                              typePos,
                              g2);
        }
    }
    
    /**
     * This method is used to draw a catalyst symbols as in SBGN.
     * @param helperPoints
     * @param position
     * @param g2
     */
    private void drawCatalystBranches(List helperPoints,
                                      Point position,
                                      Graphics2D g2) {
        // Draw a circled - in the branches
        int x, y;
        List branch = null;
        Point p;
        double ratio = 0.4;
        double tan, theta;
        double dist = (EDGE_TYPE_WIDGET_WIDTH + EDGE_MODULATION_WIDGET_WIDTH) * 0.6;
        Paint current = g2.getPaint();
        for (Iterator it = helperPoints.iterator(); it.hasNext();) {
            branch = (List) it.next();
            if (branch == null || branch.size() == 0)
                continue; // In case it is wrong -- a defensive way
            Point anchor = anchorPositionInBranch(branch,
                                                  position,
                                                  dist);
            // Draw line
            drawBranchLine(branch, 
                           anchor, 
                           g2);
            // Draw symbol
            x = anchor.x;
            y = anchor.y;
            x -= EDGE_MODULATION_WIDGET_WIDTH / 2;
            y -= EDGE_MODULATION_WIDGET_WIDTH /2;
            g2.setPaint(g2.getBackground());
            g2.fillOval(x, 
                        y, 
                        EDGE_MODULATION_WIDGET_WIDTH, 
                        EDGE_MODULATION_WIDGET_WIDTH);
            //g2.setPaint(DEFAULT_FOREGROUND);
            g2.setPaint(current); // Use any paint used currently
            g2.drawOval(x, 
                        y, 
                        EDGE_MODULATION_WIDGET_WIDTH, 
                        EDGE_MODULATION_WIDGET_WIDTH);
        }   
    }
    
    private Point anchorPositionInBranch(List branch,
                                         Point position,
                                         double dist) {
        Point p = (Point) branch.get(branch.size() - 1);
        // Remember: the y axis is contrary to the ordinary coordinate system
        double tan = (double) (p.y - position.y) / (p.x - position.x);
        double theta = Math.atan(tan);
        if (p.x - position.x < 0)
            theta +=  Math.PI;
        int x = (int)(position.x + dist * Math.cos(theta));
        int y = (int)(position.y + dist * Math.sin(theta));
        return new Point(x, y);
    }
    
    private void drawAssociationType(RenderableReaction rxt,
                                     int x,
                                     int y,
                                     Graphics2D g2) {
        if (rxt.getReactionType() == ReactionType.ASSOCIATION) {
            g2.fillOval(x, 
                        y, 
                        EDGE_TYPE_WIDGET_WIDTH, 
                        EDGE_TYPE_WIDGET_WIDTH);
        }
        else if (rxt.getReactionType() == ReactionType.DISSOCIATION) {
            // Clear the backbround
            Paint oldPaint = g2.getPaint();
            g2.setPaint(g2.getBackground());
            g2.fillOval(x, 
                        y,
                        EDGE_TYPE_WIDGET_WIDTH, 
                        EDGE_TYPE_WIDGET_WIDTH);
            g2.setPaint(oldPaint);
            g2.drawOval(x, 
                        y,
                        EDGE_TYPE_WIDGET_WIDTH, 
                        EDGE_TYPE_WIDGET_WIDTH);
            g2.drawOval(x + 2, 
                        y + 2,
                        EDGE_TYPE_WIDGET_WIDTH - 4, 
                        EDGE_TYPE_WIDGET_WIDTH - 4);
        }
    }
    
    private Point getReactionTypePosition() {
        Point pos = reaction.getPosition();
        // Make it looking nicer
        // Find two adjacent points
        List backbone = reaction.getBackbonePoints();
        Point p1 = null;
        Point p2 = null;
        for (int i = 0; i < backbone.size(); i++) {
            Point p = (Point) backbone.get(i);
            if (p == pos) {
                p1 = (Point) backbone.get(i - 1 < 0 ? 0 : i - 1);
                p2 = (Point) backbone.get(i + 1 == backbone.size() ? i : i + 1);
            }
        }
        // There are only two points in the backbone
        if (p1 == pos || p2 == pos) {
            // The end point is used as the position
            // pos should be the middle point of two end points
            pos = new Point((p1.x + p2.x) / 2,
                            (p1.y +  p2.y) / 2);
        }
        return pos;
    }
    
    private Point drawReactionType(Graphics2D g2) {
        Point pos = reaction.getPosition();
        // Make it looking nicer
        // Find two adjacent points
        List backbone = reaction.getBackbonePoints();
        Point p1 = null;
        Point p2 = null;
        for (int i = 0; i < backbone.size(); i++) {
            Point p = (Point) backbone.get(i);
            if (p == pos) {
                p1 = (Point) backbone.get(i - 1 < 0 ? 0 : i - 1);
                p2 = (Point) backbone.get(i + 1 == backbone.size() ? i : i + 1);
            }
        }
        // There are only two points in the backbone
        if (p1 == pos || p2 == pos) {
            // The end point is used as the position
            // pos should be the middle point of two end points
            pos = new Point((p1.x + p2.x) / 2,
                            (p1.y +  p2.y) / 2);
        }
        int x = pos.x - EDGE_TYPE_WIDGET_WIDTH / 2;
        int y = pos.y - EDGE_TYPE_WIDGET_WIDTH / 2;
        Stroke oldStroke = g2.getStroke();
        Paint oldPaint = g2.getPaint();
        if (reaction.isSelected()) {
            g2.setStroke(SELECTION_STROKE);
            g2.setPaint(SELECTION_WIDGET_COLOR);
        }
        RenderableReaction rxt = (RenderableReaction) reaction;
        if (rxt.getReactionType() == ReactionType.ASSOCIATION ||
            rxt.getReactionType() == ReactionType.DISSOCIATION) {
            drawAssociationType(rxt, x, y, g2);
        }
        else {
            double theta;
            // Check the following two special cases to avoid NaN
            // problem: if the theta is a NaN, g2.rotate() will hang
            // and other reactions cannot be drawn correctly.
            if (p2.y - p1.y == 0)
                theta = 0.0;
            else if (p2.x - p1.x == 0)
                theta = Math.PI / 2.0;
            else {
                double tan = (double) (p2.y - p1.y) / (p2.x - p1.x);
                theta = Math.atan(tan);
            }
            g2.rotate(theta, pos.x, pos.y);
            g2.clearRect(x, 
                         y, 
                         EDGE_TYPE_WIDGET_WIDTH, 
                         EDGE_TYPE_WIDGET_WIDTH);
            g2.drawRect(x, 
                        y, 
                        EDGE_TYPE_WIDGET_WIDTH, 
                        EDGE_TYPE_WIDGET_WIDTH);
            if (rxt.getReactionType() == ReactionType.OMITTED_PROCESS) {
                int pad = 3;
                int x1 = x + pad;
                int y1 = y + pad;
                int x2 = x + EDGE_TYPE_WIDGET_WIDTH / 2;
                int y2 = y + EDGE_TYPE_WIDGET_WIDTH - pad;
                g2.drawLine(x1, y1, x2, y2);
                x1 = x + EDGE_TYPE_WIDGET_WIDTH / 2;
                x2 = x + EDGE_TYPE_WIDGET_WIDTH - pad;
                g2.drawLine(x1, y1, x2, y2);
            }
            else if (rxt.getReactionType() == ReactionType.UNCERTAIN_PROCESS) {
                // Draw a question mark
                Font oldFont = g2.getFont();
                g2.setFont(WIDGET_FONT);
                int x1 = x + 4;
                int y1 = y + EDGE_TYPE_WIDGET_WIDTH - 3;
                g2.drawString("?", x1, y1);
                g2.setFont(oldFont);
            }
            g2.rotate(-theta, pos.x, pos.y);
        }
        if (reaction.isSelected()) {
            g2.setStroke(oldStroke);
            g2.setPaint(oldPaint);
        }
        return pos;
    }
    
    private void drawStoichiometry(Integer value,
                               Point p1,
                               Point p2,
                               Point typePos,
                               Graphics2D g2) {
        double tan = (double) (p2.y - p1.y) / (p2.x - p1.x);
        double theta = Math.atan(tan);
        int x0 = (p1.x + p2.x) / 2;
        int y0 = (p1.y + p2.y) / 2;
        if (typePos != null) {
            // A special case check: one input, one output, two points of backbone
            if (Math.abs(x0 - typePos.x) < EDGE_TYPE_WIDGET_WIDTH &&
                Math.abs(y0 - typePos.y) < EDGE_TYPE_WIDGET_WIDTH) {
                // P1 should be the point on the Node.
                x0 = (p1.x + typePos.x) / 2;
                y0 = (p1.y + typePos.y) / 2;
            }
        }
        g2.rotate(theta, x0, y0);
        int x = x0 - EDGE_TYPE_WIDGET_WIDTH / 2;
        int y = y0 - EDGE_TYPE_WIDGET_WIDTH / 2;
        g2.clearRect(x, 
                     y, 
                     EDGE_TYPE_WIDGET_WIDTH, 
                     EDGE_TYPE_WIDGET_WIDTH);
        g2.drawRect(x, 
                    y, 
                    EDGE_TYPE_WIDGET_WIDTH, 
                    EDGE_TYPE_WIDGET_WIDTH);
        // Draw a question mark
        Font oldFont = g2.getFont();
        g2.setFont(WIDGET_FONT);
        //int x1 = x + 4;
        //int y1 = y + EDGE_TYPE_WIDGET_WIDTH - 3;
        //g2.drawString(value.toString(), x1, y1);
        DrawUtilities.drawString(value.toString(), 
                                 x,
                                 y,
                                 EDGE_TYPE_WIDGET_WIDTH,
                                 EDGE_TYPE_WIDGET_WIDTH,
                                 g2);
        g2.setFont(oldFont);
        g2.rotate(-theta, x0, y0);
    }
    
    private void drawActivatorBranches(java.util.List branches, 
                                       Point hub, 
                                       Graphics2D g2) {
		// Draw a circled - in the branches
		int x, y;
		java.util.List branch = null;
		Point p;
		double ratio = 0.4;
		double dist = EDGE_TYPE_WIDGET_WIDTH * 0.75;
		for (Iterator it = branches.iterator(); it.hasNext();) {
			branch = (java.util.List) it.next();
			if (branch == null || branch.size() == 0)
				continue; // In case it is wrong -- a defensive way
			p = (Point) branch.get(branch.size() - 1);
			Point anchor = anchorPositionInBranch(branch, 
			                                      hub, 
			                                      dist);
			drawBranchLine(branch, 
			               anchor, 
			               g2);
			DrawUtilities.drawHollowArrow(anchor,
			                              p,
			                              g2);
		}	
    }
    
    private void drawInhibitorBranches(java.util.List branches,
                                       Point hub, 
                                       Graphics2D g2) {
    	// Draw a circled - in the branches
    	int x, y;
    	java.util.List branch = null;
    	Point p;
    	double ratio = 0.4;
    	double dist = EDGE_TYPE_WIDGET_WIDTH * 0.75;
    	for (Iterator it = branches.iterator(); it.hasNext();) {
    		branch = (java.util.List) it.next();
    		if (branch == null || branch.size() == 0)
    			continue; // In case it is wrong -- a defensive way
    		p = (Point) branch.get(branch.size() - 1);
    		Point anchor = anchorPositionInBranch(branch, 
                                                  hub,
                                                  dist);
            drawBranchLine(branch, 
                           anchor, 
                           g2);
    		// The the angle of the line segment
            double alpha = Math.atan((double)(anchor.y - p.y) / (anchor.x - p.x));
            if (p.x > anchor.x)
                alpha += Math.PI;
            double angle = Math.PI / 2.0 - alpha;
            float x1 = (float)(anchor.x - EDGE_INHIBITION_WIDGET_WIDTH / 2.0 * Math.cos(angle));
            float y1 = (float)(anchor.y + EDGE_INHIBITION_WIDGET_WIDTH / 2.0 * Math.sin(angle));
            angle = Math.PI / 2.0 + alpha;
            float x2 = (float)(anchor.x - EDGE_INHIBITION_WIDGET_WIDTH / 2.0 * Math.cos(angle));
            float y2 = (float)(anchor.y - EDGE_INHIBITION_WIDGET_WIDTH / 2.0 * Math.sin(angle));
            g2.drawLine((int)x1, (int)y1, 
                        (int)x2, (int)y2);
    	}	
    }
    
    private void drawBranches(List branches, 
                              Point hub, 
                              Graphics2D g2) {
        List branch = null;
        Point prevP, nextP;
        for (Iterator it = branches.iterator(); it.hasNext();) {
            branch = (java.util.List) it.next();
            drawBranchLine(branch, 
                           hub, 
                           g2);
        }
    }
    
    private void drawBranchLine(List branch,
                                Point hub,
                                Graphics2D g2) {
        if (branch == null || branch.size() == 0)
            return;
        Point prevP = (Point) branch.get(0);
        Point nextP;
        for (int i = 1; i < branch.size(); i++) {
            nextP = (Point) branch.get(i);
            g2.drawLine(prevP.x, prevP.y, nextP.x, nextP.y);
            prevP = nextP;
        }
        g2.drawLine(prevP.x, prevP.y, hub.x, hub.y);
    }
    
    private void drawArrows(java.util.List branches, Point hub, Graphics2D g2) {
        java.util.List branch = null;
        for (Iterator it = branches.iterator(); it.hasNext();) {
            branch = (java.util.List) it.next();
            if (branch == null || branch.size() == 0)
                continue;
            if (branch.size() == 1) 
                DrawUtilities.drawArrow((Point)branch.get(0), hub, g2);
            else
                DrawUtilities.drawArrow((Point)branch.get(0), (Point)branch.get(1), g2);
        }
    }

    protected void drawSelectionWidgets(Graphics2D g2) {
        Point p = null;
        g2.setPaint(DefaultRenderConstants.SELECTION_WIDGET_COLOR);
        int widgetWidth = DefaultRenderConstants.EDGE_SELECTION_WIDGET_WIDTH;
        int offset = widgetWidth / 2;
        // Draw the selected Point only
//        if (reaction.getSelectedPoint() != null) {
//            p = reaction.getSelectedPoint();
//            g2.fillRect(p.x - offset, p.y - offset, widgetWidth, widgetWidth);
//        }
//        else {
        // as of Jan 13, 2010, always draw all points to avoid accidental delinks.
            for (Iterator it = reaction.getAllPoints().iterator(); it.hasNext();) {
                p = (Point) it.next();
                g2.fillRect(p.x - offset, p.y - offset, widgetWidth, widgetWidth);
//            }
        }
    }
    
    public void setRenderable(Renderable r) {
        this.reaction = (HyperEdge) r;
    }
}
