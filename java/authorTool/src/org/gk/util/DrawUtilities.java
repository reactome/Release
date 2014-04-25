/*
 * DrawUtilities.java
 *
 * Created on June 13, 2003, 11:38 AM
 */

package org.gk.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
/**
 * This class group a list of utilities that can be used in drawing.
 * @author  wgm
 */
public class DrawUtilities {
    // For arrow drawing
    public static final int ARROW_LENGTH = 8; 
    public static final double ARROW_ANGLE = Math.PI / 6;
    
    /**
     * Draw a text in the center of the bounds.
     */
    public static void drawString(String text, Rectangle bounds, Graphics2D g2) {
        FontMetrics metrics = g2.getFontMetrics();
        Rectangle2D textBounds = metrics.getStringBounds(text, g2);
        int x = (int)(bounds.x + bounds.width / 2 - textBounds.getWidth() / 2);
        int y = (int)(bounds.y + bounds.height / 2 - textBounds.getHeight() / 2);
        y += metrics.getAscent();
        g2.drawString(text, x, y);
    }
    
    public static void drawString(String text,
                                  int boundsX,
                                  int boundsY,
                                  int boundsWidth,
                                  int boundsHeight,
                                  Graphics2D g2) {
        FontMetrics metrics = g2.getFontMetrics();
        Rectangle2D textBounds = metrics.getStringBounds(text, g2);
        int x = (int)(boundsX + boundsWidth / 2 - textBounds.getWidth() / 2);
        int y = (int)(boundsY + boundsHeight / 2 - textBounds.getHeight() / 2);
        y += metrics.getAscent();
        g2.drawString(text, x, y);
    }
    
    /**
     * Calculate the origin points that are used to draw a list of TextLayouts.
     * @param textLayouts the drawn TextLayouts.
     * @param bounds the bounds surround the drawn TextLayouts.
     * @param buffer the extra space around the drawn text.
     * @param g2 the graphic context.
     * @return a list of Point2D.Float that are origin points for drawing TextLayouts.
     */
    public static java.util.List getDrawPoints(java.util.List textLayouts, Rectangle bounds, int buffer, Graphics2D g2) {
    	java.util.List points = new ArrayList(textLayouts.size());
		float w = (float)bounds.getWidth();
		TextLayout layout = (TextLayout) textLayouts.get(0);
		float x = (float)(w - layout.getAdvance()) / 2 + bounds.x;
		float y = (float) (layout.getAscent() + buffer) + bounds.y;
		points.add(new Point2D.Float(x, y));
		y += (layout.getDescent() + layout.getLeading());
		for (int i = 1; i < textLayouts.size(); i++) {
			layout = (TextLayout) textLayouts.get(i);
			x = (float) (w - layout.getAdvance()) / 2 + bounds.x;
			y += layout.getAscent();
			points.add(new Point2D.Float(x, y));
			y += (layout.getDescent() + layout.getLeading());
		}
		return points;    	
    }
    
    public static void drawString(java.util.List textLayouts, Rectangle bounds, int buffer, Graphics2D g2) {
    	float w = (float)bounds.getWidth();
    	TextLayout layout = (TextLayout) textLayouts.get(0);
    	float x = (float)(w - layout.getAdvance()) / 2 + bounds.x;
    	float y = (float) (layout.getAscent() + buffer) + bounds.y;
    	layout.draw(g2, x, y);
    	y += (layout.getDescent() + layout.getLeading());
    	for (int i = 1; i < textLayouts.size(); i++) {
    		layout = (TextLayout) textLayouts.get(i);
    		x = (float) (w - layout.getAdvance()) / 2 + bounds.x;
    		y += layout.getAscent();
    		layout.draw(g2, x, y);
    		y += (layout.getDescent() + layout.getLeading());
    	}
    }
	
	public static void drawResizeWidgets(Rectangle bounds, int widgetWidth, Color color, Graphics2D g2) {
		int offset = widgetWidth / 2;
		g2.setPaint(color);
		// north-west
		int x = bounds.x - offset;
		int y = bounds.y - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// north
		x = bounds.x + bounds.width / 2 - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// north-east
		x = bounds.x + bounds.width - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// east
		y = bounds.y + bounds.height / 2 - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// south-east
		y = bounds.y + bounds.height - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// south
		x = bounds.x + bounds.width / 2 - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// south-west
		x = bounds.x - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// west
		y = bounds.y + bounds.height / 2 - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
	} 
   
    
    /**
     * Calculate the start point for drawing a TextLayout. 
     * @param textLayout
     * @param bounds the constraints of rendering.
     * @param p the point's x y values will be reset if p is not null.
     * @return a new Point object will be returned if p is null. Otherwise,
     * the passed Point object will be returned.
     */
    public static Point getDrawPoint(TextLayout textLayout, Rectangle bounds, Point p) {
		Rectangle2D textBounds = textLayout.getBounds();
		int x = (int)(bounds.x + bounds.width / 2 - textBounds.getWidth() / 2);
		int y = (int)(bounds.y + bounds.height / 2 - textBounds.getHeight() / 2);
		y += textLayout.getAscent();
		if (p == null)
			return new Point(x, y);    	
		else {
			p.x = x;
			p.y = y;
			return p;
		}
    }
    
    public static void validateStringBounds(String text, Point position, Rectangle bounds, int buffer, Graphics g) {
        Font font = g.getFont();
        Rectangle2D stringBounds = font.getStringBounds(text, ((Graphics2D)g).getFontRenderContext());
        if (position == null) {
            bounds.x = 0;
            bounds.y = 0;
            bounds.width = (int)(stringBounds.getWidth() + 2 * buffer);
            bounds.height = (int)(stringBounds.getHeight() + 2 * buffer);
        }
        else {
            bounds.x = (int)(position.x - stringBounds.getWidth() / 2 - buffer);
            bounds.y = (int)(position.y - stringBounds.getHeight() / 2 - buffer);
            bounds.width = (int)(stringBounds.getWidth() + 2 * buffer);
            bounds.height = (int)(stringBounds.getHeight() + 2 * buffer);
        }
    }
    
    /**
     * Draw an arrow around a line segement.
     * @param position the arrow position.
     * @param controlPoint another position that the arrow points away.
     * @param g2 graphic context
     */
    public static void drawArrow(Point position,
                                 Point controlPoint,
                                 Graphics2D g2) {
        GeneralPath path = createArrowPath(position, 
                                           controlPoint);
        g2.draw(path);
        g2.fill(path);
    }
    
    /**
     * Draw an hollow arrow.
     * @param position
     * @param controlPoint
     * @param g2
     */
    public static void drawHollowArrow(Point position,
                                       Point controlPoint,
                                       Graphics2D g2) {
        // The the angle of the line segment
        GeneralPath path = createArrowPath(position, 
                                           controlPoint);
        Paint oldPaint = g2.getPaint();
        g2.setPaint(g2.getBackground());
        g2.fill(path);
        g2.setPaint(oldPaint);
        g2.draw(path);
    }
    
    private static GeneralPath createArrowPath(Point position,
                                               Point controlPoint) {
        // The the angle of the line segment
        double alpha = Math.atan((double)(position.y - controlPoint.y) / (position.x - controlPoint.x));
        if (controlPoint.x > position.x)
            alpha += Math.PI;
        double angle = ARROW_ANGLE - alpha;
        GeneralPath path = new GeneralPath();
        float x1 = (float)(position.x - ARROW_LENGTH * Math.cos(angle));
        float y1 = (float)(position.y + ARROW_LENGTH * Math.sin(angle));
        path.moveTo(x1, y1);
        path.lineTo(position.x, position.y);
        angle = ARROW_ANGLE + alpha;
        float x2 = (float)(position.x - ARROW_LENGTH * Math.cos(angle));
        float y2 = (float)(position.y - ARROW_LENGTH * Math.sin(angle));
        path.lineTo(x2, y2);
        path.closePath();
        return path;
    }
 }
