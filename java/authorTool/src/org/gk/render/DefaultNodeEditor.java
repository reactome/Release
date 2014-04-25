/*
 * Created on Aug 22, 2003
 */
package org.gk.render;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextHitInfo;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import javax.swing.Timer;

import org.gk.util.DrawUtilities;

/**
 * A default implementation of editor for editing display names of RenderableEntity
 * objects. This is a subclass of DefaultEntityRenderer.
 * @author wgm
 */
public class DefaultNodeEditor implements Editor, DefaultRenderConstants{
	private int caretPosition;
	private int selectStart;
	private int selectEnd;
	private java.util.List points; // The origin points for drawing TextLayouts.
	private boolean isChanged = false;
	// Used to control blinking
    private Timer flasher;
    private boolean isCaretVisible;
    private Component component;
    // To figure out what part should be repaint during blinking
    private double scaleX = 1.0; 
    private double scaleY = 1.0;
    private int blinkingRate = 500;
    // For hight
    private Color selectionColor;
    
    protected Renderable renderable;
    
	public DefaultNodeEditor() {
	    initFlasher();
        //selectionColor = UIManager.getColor("TextField.selectionBackground");
        //if (selectionColor == null)
        //    selectionColor = SELECTION_WIDGET_COLOR;
	    // Used a fixed color. Otherwise, under Windows LF (not xp),
	    // the foreground should be changed to make test visible. Just
	    // make things a little simple.
	    selectionColor = new Color(180, 213, 255);
    }
    
    private void initFlasher() {
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isCaretVisible = !isCaretVisible;
                if (component != null && renderable != null) {
                    Rectangle bounds = renderable.getTextBounds();
                    // Have to do a zoom back
                    component.repaint((int)(bounds.x * scaleX),
                                      (int)(bounds.y * scaleY),
                                      (int)(bounds.width * scaleX + 1),
                                      (int)(bounds.height * scaleY + 1));
                }
            }
        };
        flasher = new Timer(blinkingRate,
                            l);
    }
    
    public void setComponent(Component comp) {
        this.component = comp;
    }
    
    public void setScaleFactors(double scaleX, double scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }
    
	public Renderable getRenderable() {
		return this.renderable;
	}
	
    /**
     * This Editor is used for Node only. The passed renderable should be Node or Null.
     */
	public void setRenderable(Renderable renderable) {
        this.renderable = renderable;
	}
	
	protected Rectangle drawBounds(Graphics2D g2) {
		if (renderable.getBackgroundColor() == null) {
			g2.setPaint(DEFAULT_BACKGROUND);
		}
		else {
			g2.setPaint(renderable.getBackgroundColor());
		}
		Rectangle bounds = renderable.getTextBounds();
		g2.fillRoundRect(bounds.x,
                         bounds.y,
                         bounds.width,
                         bounds.height,
                         ROUND_RECT_ARC_WIDTH,
                         ROUND_RECT_ARC_WIDTH);
		// Draw the outline
		Stroke oldStroke = g2.getStroke();
		if (renderable.isSelected()) {
			g2.setStroke(SELECTION_STROKE);
			g2.setPaint(SELECTION_WIDGET_COLOR);
		}
		else
			g2.setPaint(DEFAULT_OUTLINE_COLOR);
		g2.drawRoundRect(bounds.x,
                         bounds.y,
                         bounds.width,
                         bounds.height,
                         ROUND_RECT_ARC_WIDTH,
                         ROUND_RECT_ARC_WIDTH);
		g2.setStroke(oldStroke);
		return bounds;
	}
	
	protected void validateBounds(Graphics g) {
		((Node)renderable).validateBounds(g);
	}
	
	public void render(Graphics g) {
		if (renderable == null)
			return;
        renderable.render(g);
        validateBounds(g);
		Graphics2D g2 = (Graphics2D) g;
        Rectangle bounds = drawBounds(g2);
		// Draw the selection
		Node node = (Node) renderable;
		points = DrawUtilities.getDrawPoints(node.getTextLayouts(),
											 bounds,
											 node.boundsBuffer,
											 g2);
        // Draw the selection background
		if (selectEnd != selectStart) {
			java.util.List layouts = node.getTextLayouts();
			int c1[] = convertLogicalToVisual(layouts, selectStart);
			int c2[] = convertLogicalToVisual(layouts, selectEnd);
			// If start and end are at the same text layout
			if (c1[0] == c2[0]) {
				TextLayout layout = (TextLayout) layouts.get(c1[0]);
				Point2D.Float point = (Point2D.Float) points.get(c1[0]);
				hilite(layout, c1[1], c2[1], point, g2);
			}
			// Otherwise, all text layouts between should be selected.
			else {
				// Draw the highlighter for the first textlayout
				TextLayout layout = (TextLayout) layouts.get(c1[0]);
				Point2D.Float point = (Point2D.Float) points.get(c1[0]);
				hilite(layout, c1[1], layout.getCharacterCount(), point, g2);
				// Draw the hightlighter for the last text layout
				layout = (TextLayout) layouts.get(c2[0]);
				point = (Point2D.Float) points.get(c2[0]);
				hilite(layout, 0, c2[1], point, g2);
				for (int i = c1[0] + 1; i < c2[0]; i++) {
					layout = (TextLayout) layouts.get(i);
					point = (Point2D.Float) points.get(i);
					hilite(layout, 0, layout.getCharacterCount(), point, g2);
				}
			}
		}
        // Have to draw name again here 
        drawName((Node)renderable, g2);
        if (isCaretVisible) {
            // Draw cursor
            int c[] = convertLogicalToVisual(node.getTextLayouts(), caretPosition);
            TextLayout layout = (TextLayout) node.getTextLayouts().get(c[0]);
            Shape caret = layout.getCaretShapes(c[1])[0];
            Point2D.Float point = (Point2D.Float) points.get(c[0]);
            AffineTransform at = AffineTransform.getTranslateInstance(point.x, point.y);
            caret = at.createTransformedShape(caret);
            g2.draw(caret);
        }
        if (!flasher.isRunning())
            flasher.start();
	}
    
    public void drawName(Node node, Graphics2D g2) {
        // Draw the text: Draw the text at the center of the bounding rectangle
        if (node.getForegroundColor() == null)
            g2.setPaint(DEFAULT_FOREGROUND);
        else
            g2.setPaint(node.getForegroundColor());
        Rectangle bounds = node.getTextBounds();
        DrawUtilities.drawString(node.getTextLayouts(), 
                                 bounds, 
                                 node.boundsBuffer, 
                                 g2);
    }
	
	private void hilite(TextLayout layout, int start, int end, Point.Float origin, Graphics2D g2) {
		Shape highlighter = layout.getLogicalHighlightShape(start, end);
		AffineTransform at = AffineTransform.getTranslateInstance(origin.x, origin.y);
		highlighter = at.createTransformedShape(highlighter);
        g2.setPaint(selectionColor);
        g2.fill(highlighter);		
	}
	
	/**
	 * A helper to convert a logical caret position, that is stored in this
	 * class, to visual caret position.
	 * @return two element int array. The first value is the index of TextLayout,
	 * the second value is the caret position at that TextLayout.
	 */
	private int[] convertLogicalToVisual(java.util.List textLayouts, 
	                                         int caretPosition) {
		TextLayout layout = null;
		int prevCount = 0;
		int lineCount = 0;
		int c = 0;
		int pos = 0;
		for (Iterator it = textLayouts.iterator(); it.hasNext();) {
			layout = (TextLayout) it.next();
			lineCount = layout.getCharacterCount();
			// The visual caret should be in this TextLayout.
			if (prevCount + lineCount >= caretPosition) {
				pos = caretPosition - prevCount;
				break;
			}
			prevCount += lineCount;
			c++;
		}		
		return new int[]{c, pos};
	}
	
	private int convertVisualToLogical(java.util.List layouts, int layoutIndex, int visualPosition) {
		TextLayout layout = null;
		int c = 0;
		for (int i = 0; i < layoutIndex; i++) {
			layout = (TextLayout) layouts.get(i);
			c += layout.getCharacterCount();
		}
		layout = (TextLayout) layouts.get(layoutIndex);
		c += visualPosition;
		return c;
	}
	
	public void setCaretPosition(int pos) {
		this.caretPosition = pos;
	}
	
	public void setCaretPosition(int x, int y) {
		Node node = (Node) renderable;
		java.util.List layouts = node.getTextLayouts();
		// In case the old points are used.
		if ((points == null) ||
		    points.size() < layouts.size()) 
		    return;
		TextLayout layout = null;
		Rectangle2D.Float bounds = new Rectangle2D.Float();
		Point.Float point = null;
		for (int i = 0; i < layouts.size(); i++) {
			layout = (TextLayout) layouts.get(i);
			point = (Point.Float) points.get(i);
			bounds.width = layout.getAdvance() + node.boundsBuffer; // An extra for select last character.
			bounds.height = layout.getAscent() + layout.getDescent() + layout.getLeading();
			bounds.x = point.x;
			bounds.y = point.y - layout.getAscent();
			if (bounds.contains(x, y)) {
				TextHitInfo hitInfo = layout.hitTestChar(x - point.x, y - point.y);
				int visualPos  = hitInfo.getCharIndex();
				caretPosition = convertVisualToLogical(layouts, i, visualPos);
				return;
			}
		}
	}
	
	public int getCaretPosition() {
		return this.caretPosition;
	}
	
	public void moveCaretToLeft() {
		caretPosition --;
		if (caretPosition == -1) 
			caretPosition = renderable.getDisplayName().length();
	}
	
	public void moveCaretToRight() {
		caretPosition ++;
		if (caretPosition == renderable.getDisplayName().length() + 1)
			caretPosition = 0;
	}
	
	public boolean moveCaretUp() {
		java.util.List layouts = ((Node)renderable).getTextLayouts();
		int[] c = convertLogicalToVisual(layouts, caretPosition);
		if (c[0] == 0)
			return false;
		int layoutIndex = c[0] - 1;
		TextLayout layout = (TextLayout) layouts.get(layoutIndex);
		int visualPos = c[1];
		if (visualPos > layout.getCharacterCount())
			visualPos = layout.getCharacterCount();
		caretPosition = convertVisualToLogical(layouts, layoutIndex, visualPos);
		return true;
	}
	
	public boolean moveCaretDown() {
		java.util.List layouts = ((Node)renderable).getTextLayouts();
		int[] c = convertLogicalToVisual(layouts, caretPosition);
		if (c[0] == layouts.size() - 1)
			return false;
		int layoutIndex = c[0] + 1;
		TextLayout layout = (TextLayout) layouts.get(layoutIndex);
		int visualPos = c[1];
		if (visualPos > layout.getCharacterCount())
			visualPos = layout.getCharacterCount();
		caretPosition = convertVisualToLogical(layouts, layoutIndex, visualPos);
		return true;
	}
	
	public void setSelectionStart(int start) {
		selectStart = start;
	}
	
	public void setSelectionEnd(int end) {
		selectEnd = end;
	}
	
	public int[] getSelection() {
		int[] selection = new int[2];
		if (selectStart < selectEnd) {
			selection[0] = selectStart;
			selection[1] = selectEnd;
		}
		else {
			selection[0] = selectEnd;
			selection[1] = selectStart;
		}
		return selection;
	}
	
	public int getSelectionStart() {
		return selectStart;
	}
	
	public int getSelectionEnd() {
		return selectEnd;
	}
	
	public void clearSelection() {
		selectStart = selectEnd = 0;
	}
	
	public void setIsChanged(boolean isChanged) {
		this.isChanged = true;
	}
	
	public boolean isChanged() {
		return isChanged;
	}
	
	public void reset() {
		renderable = null;
		if (points != null) {
			points.clear();
			points = null;
		}
		caretPosition = 0;
		selectStart = selectEnd = 0;
		isChanged = false;
        if (flasher != null && flasher.isRunning())
            flasher.stop();
	}
}
