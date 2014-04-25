package org.gk.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * A TabbedPane that shows an close-button whenever the mouse is over the right corner.
 * @author Fritz Ritzberger modified by wgm.
 */

public class CloseableTabbedPane extends JTabbedPane implements MouseListener, MouseMotionListener, ActionListener {
	// For action
	private JButton close;
	private Icon closeIcon = new CloseTabIcon();
	// Icon size
	private Dimension btnSize = new Dimension(16, 16);
	// For close action
	private Action closeAction;

	public CloseableTabbedPane() {
		this(JTabbedPane.TOP);
	}

	public CloseableTabbedPane(int tabPlacement) {
		super(tabPlacement);

		close = new JButton(closeIcon);
		//close.setBorder(BorderFactory.createEmptyBorder());
		close.setPreferredSize(btnSize);
		close.addActionListener(this);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		close.addMouseListener(this);
		close.addMouseMotionListener(this);
	}

	public void mouseEntered(MouseEvent e) {
		if (e.getSource() == this)
			showPopup(e);
	}
	
	public void mouseExited(MouseEvent e) {
		if (e.getSource() == close)
			removeCloseButton();
	}
	
	public void mouseClicked(MouseEvent e) {
	}
	
	public void mousePressed(MouseEvent e) {
	}
	
	public void mouseReleased(MouseEvent e) {
	}
	
	public void mouseMoved(MouseEvent e) {
		if (e.getSource() == this)
			showPopup(e);
		else
			e.consume();
	}
	
	public void mouseDragged(MouseEvent e) {
		//if (e.getSource() == this)
		//	showPopup(e);
		//else
		//	e.consume();
	}

	private void showPopup(MouseEvent e) {
		if (getTabCount() == 0)
			return;
		Rectangle r = (Rectangle)getUI().getTabBounds(this, getSelectedIndex()).clone();
		int deltaX = r.width - btnSize.width - 2; // littleBitLeft
		int deltaY = (r.height - btnSize.height) / 2; // A little upshift.
		r.x += deltaX;
		r.width = btnSize.width;
		r.y += deltaY;
		r.height = btnSize.height;
		
		if (r.contains(e.getPoint())) {
			if (close.isVisible() == false) {
				JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();
				Point p = SwingUtilities.convertPoint(this, r.x, r.y, layeredPane);
				close.setBounds(p.x, p.y, btnSize.width, btnSize.height);
				layeredPane.add(close, JLayeredPane.POPUP_LAYER);
				close.setVisible(true);
			}
		}
		else {
			removeCloseButton();
		}
	}

	private void removeCloseButton() {
		if (close.isVisible()) {
			close.setVisible(false);
			JLayeredPane layeredPane = SwingUtilities.getRootPane(this).getLayeredPane();
			layeredPane.remove(close);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == close && closeAction != null) {
			closeAction.actionPerformed(e);
			removeCloseButton();
		}
	}
	
	public void setCloseAction(Action closeAction) {
		this.closeAction = closeAction;
	}
	
	public Action getCloseAction() {
		return this.closeAction;
	}

	// test main
	public static void main(String[] args) {
		CloseableTabbedPane tp = new CloseableTabbedPane();
		//tp.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		for (int i = 1; i <= 10; i++) {
			tp.addTab("Tab " + i, new JButton("Button " + i));
		}
		JFrame f = new JFrame();
		f.getContentPane().add(tp);
		f.setSize(200, 200);
		f.setVisible(true);
	}
	
	/**
	 * The class which generates the 'X' icon for the tabs. The constructor 
	 * accepts an icon which is extra to the 'X' icon, so you can have tabs 
	 * like in JBuilder. This value is null if no extra icon is required. 
	 * @author Mr_Silly @jdc modified by wgm.
	 * */
	class CloseTabIcon implements Icon {
		private int x_pos;
		private int y_pos;
		private int width;
		private int height;
		//private Icon fileIcon;
		
		public CloseTabIcon() {
			width = 16;
			height = 16;
		}
		public void paintIcon(Component c, Graphics g, int x, int y) {
			x_pos = x;
			y_pos = y;
			Color col = g.getColor();
			g.setColor(Color.black);
			int b = 5;
			g.drawLine(x + b - 1, y + b, x + width - b - 1, y + height - b);
			g.drawLine(x + b , y + b, x + width - b, y + height - b);
			g.drawLine(x + width - b - 1, y + b, x + b - 1, y + height - b);
			g.drawLine(x + width - b, y + b, x + b, y + height - b);
			g.setColor(col);
			//if (fileIcon != null) {
			//	fileIcon.paintIcon(c, g, x + width, y_p);
			//}
		}
		public int getIconWidth() {
			return width;
		}
		public int getIconHeight() {
			return height;
		}
		public Rectangle getBounds() {
			return new Rectangle(x_pos, y_pos, width, height);
		}
	}
}
