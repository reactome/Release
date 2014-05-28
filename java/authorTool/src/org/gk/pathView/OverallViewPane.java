/*
 * Created on Mar 15, 2004
 */
package org.gk.pathView;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.undo.UndoableEdit;

/**
 * A whole view of the view box.
 * @author wugm
 */
public class OverallViewPane extends JPanel implements VisualizationConstants {
	
	private Collection edges;
	private Dimension originalSize;
	private Dimension preferredSize;
	private double hToWRatio;
	private JDialog dialog;
	private Rectangle visibleRect;
	// For moving visibleRect
	private Point prevPoint;
	private boolean isDragging;
	private Rectangle dirtyRect;
	// For scalling
	private double scaleX, scaleY;
	
	public OverallViewPane() {
		preferredSize = new Dimension();
		preferredSize.width = 600;
		preferredSize.height = (int) (600 * hToWRatio);
		// For select rect
		visibleRect = new Rectangle();
		dirtyRect = new Rectangle();
		installListeners();
	}
	
	private void installListeners() {
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				Point p = e.getPoint();
				p.x /= scaleX;
				p.y /= scaleY;
				if (visibleRect.contains(p)) {
					prevPoint = p;
					isDragging = true;
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (isDragging) {
					isDragging = false;
					setCursor(Cursor.getDefaultCursor());
					// Make sure the correct zero
					boolean needFireEvent = false;
					if (visibleRect.x < 0) {
						visibleRect.x = 0;
						needFireEvent = true;
					}
					if ((visibleRect.x + visibleRect.width) * scaleX > getWidth()) {
						visibleRect.x = (int)(getWidth() / scaleX) - visibleRect.width;
						needFireEvent = true;
					}
					if (visibleRect.y < 0) {
						visibleRect.y = 0;
						needFireEvent = true;
					}
					if ((visibleRect.y + visibleRect.height) * scaleY > getHeight()) {
						visibleRect.y = (int) (getHeight() / scaleY) - visibleRect.height;
						needFireEvent = true;
					}
					if (needFireEvent) {
						repaint(visibleRect);
						firePropertyChange("visibleRect", null, visibleRect);
					}
				}
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if (isDragging) {
					int x = (int)(e.getX() / scaleX);
					int y = (int)(e.getY() / scaleY);
					int dx = x - prevPoint.x;
					int dy = y - prevPoint.y;
					dirtyRect.x = visibleRect.x;
					dirtyRect.y = visibleRect.y;
					dirtyRect.width = visibleRect.width;
					dirtyRect.height = visibleRect.height;
					visibleRect.translate(dx, dy);
					repaint(dirtyRect.union(visibleRect));
					prevPoint.x = x;
					prevPoint.y = y;
					firePropertyChange("visibleRect", null, visibleRect);
				}
			}			
		});		
	}
	
	public void setWidthToHeightRatio(double r) {
		this.hToWRatio = 1.0 / r;
		preferredSize.height = (int) (preferredSize.width * hToWRatio);
	}
	
	/**
	 * The specified Collection is directly referred in this OverallViewPane 
	 * object to keep the data consistent to the drawn one.
	 * @param c
	 */
	public void setEdges(Collection c) {
		this.edges = c;
	}
	
	/**
	 * A reference to the dimension to the tool is passed and used in this
	 * OverallViewPane object.
	 * @param size
	 */
	public void setOriginalDimension(Dimension size) {
		this.originalSize = size;
	}
	
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setPaint(BACKGROUND_COLOR);
		int w = getWidth();
		int h = getHeight();
		g2.fillRect(0, 0, w, h);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// Get the scale
		scaleX = (double) w / originalSize.width;
		scaleY = (double) h / originalSize.height;
		g2.scale(scaleX, scaleY);
		Color color;
		IEdge edge = null;
		for (Iterator ei = edges.iterator(); ei.hasNext();) {
			edge = (IEdge) ei.next();
			switch (edge.getType()) {
				case IEdge.LINK_EDGE :
					g2.setPaint(CONNECTION_COLOR);
					g2.setStroke(CONNECTION_STROKE);
					g2.draw(connectionShape(edge));
					break;
				case IEdge.REACTION_EDGE :
					if (edge.getColor() != null)
						g2.setPaint(edge.getColor());
					else
						g2.setPaint(DEFAULT_LOADED_REACTION_COLOR);
					g2.setStroke(REACTION_STROKE);
					g2.draw(reactionShape(edge));
					break;
			}
		}
		// Draw visible rectangle
		if (!visibleRect.isEmpty()) {
			g2.setPaint(SELECTION_COLOR);
			g2.draw(visibleRect);
		}
	}
	
	private Shape connectionShape(IEdge edge) {
		GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
		path.moveTo(edge.getHeadX(), edge.getHeadY());
		path.lineTo(edge.getTailX(), edge.getTailY());
		return path;
	}
	
	private Shape reactionShape(IEdge edge) {
		int sx = edge.getTailX();
		int sy = edge.getTailY();
		int tx = edge.getHeadX();
		int ty = edge.getHeadY();
		int dx = tx - sx;
		int dy = ty - sy;
		int d = (int) Math.max(1, Math.sqrt(dx * dx + dy * dy));
		int ax = - (ARROW_SIZE * dx / d);
		int ay = - (ARROW_SIZE * dy / d);
		GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
		path.moveTo(tx + ax + ay / 2, ty + ay - ax / 2);
		path.lineTo(tx, ty);
		path.lineTo(tx + ax - ay / 2, ty + ay + ax / 2);
		path.moveTo(tx, ty);
		path.lineTo(sx, sy);
		return path;
	}
	
	public void showView(JFrame parentFrame) {
		if (dialog == null) {
			dialog = new JDialog(parentFrame, "Overall View");
			dialog.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					firePropertyChange("isVisible", true, false);
				}
			});
			addComponentListener(new ComponentAdapter() {
				boolean isDone = false;
				public void componentResized(ComponentEvent e) {
					if (isDone) { // Prohibit the infinit recycle.
						isDone = false;
						return;
					}
					Dimension size = dialog.getSize();
					preferredSize.width = size.width;
					preferredSize.height = (int) (size.width * hToWRatio);
					dialog.pack();
					isDone = true;
				}
			});
			dialog.getContentPane().add(this, BorderLayout.CENTER);
			dialog.setLocationRelativeTo(parentFrame);
			dialog.pack();
			dialog.setVisible(true);
		}
		else
			dialog.setVisible(true);
	}
	
	public void hideView() {
		if (dialog != null && dialog.isVisible())
			dialog.setVisible(false);
	}
	
	public boolean isVisible() {
		return dialog == null ? false : dialog.isVisible();
	}
	
	public Dimension getPreferredSize() {
		return preferredSize;
	}
	
	public void setVisibleRect(Rectangle rect) {
		visibleRect.x = rect.x;
		visibleRect.y = rect.y;
		visibleRect.width = rect.width;
		visibleRect.height = rect.height;
		repaint();
	}
	
	private void moveVisibleRect() {
		
	}
	
	private void mousePressed(MouseEvent e) {
		
	}
	
	/**
	 * Select a list of reaction GKInstance objects.
	 * @param reactions
	 */
	public void select(Collection reactions) {
		for (Iterator it = edges.iterator(); it.hasNext();) {
			IEdge edge = (IEdge) it.next();
			if (edge.getType() == IEdge.REACTION_EDGE) {
				if (reactions.contains(edge.getUserObject())) 
					edge.setColor(SELECTED_REACTION_COLOR);
				else
					edge.setColor(null);
			}
		}
		repaint();
	}
}
