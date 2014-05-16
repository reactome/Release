/*
 * Created on Nov 5, 2003
 */
package org.gk.util;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * A customized JScrollPane that is used for auto completing the name editing.
 * @author wugm
 */
public class AutoCompletionPane extends JScrollPane {
	// Text Rectangle
	private Rectangle textBounds;
	private final int HEIGHT = 100;
	private JComponent invokingComponent;
	// The invoking components may be scaled
	private double scaleX = 1.0d;
	private double scaleY = 1.0d;
	// The actual list for display
	private JList list;
	private java.util.List data;
	// Remember the old index
	private String startText;
	private AutoCompletable target;
	// A thread to setStartText
	private Runnable thread;
	// A flag to control if displayed
	private boolean blocking;

	public AutoCompletionPane() {
		super();
		init();
	}

	private void init() {
		list = new JList();
		list.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if (keyCode == KeyEvent.VK_ENTER) {
					if (target != null) {
						target.setText(list.getSelectedValue().toString());
					}
					setDisplay(false);
					e.consume();
					if (invokingComponent != null)
						invokingComponent.requestFocus();
					startText = null;
					blocking = true;
				}
				else if (keyCode == KeyEvent.VK_UP) {
					int index = list.getSelectedIndex();
					if (index == 0)
						list.setSelectedIndex(list.getModel().getSize() - 1);
					else
						list.setSelectedIndex(index - 1);
					index = list.getSelectedIndex();
					Rectangle rect = list.getCellBounds(index, index);
					list.scrollRectToVisible(rect);
					e.consume();
				}
				else if (keyCode == KeyEvent.VK_DOWN) {
					int index = list.getSelectedIndex();
					if (index == list.getModel().getSize() - 1)
						list.setSelectedIndex(0);
					else
						list.setSelectedIndex(index + 1);
					index = list.getSelectedIndex();
					Rectangle rect = list.getCellBounds(index, index);
					list.scrollRectToVisible(rect);
					e.consume();
				}
				else if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
					if (invokingComponent != null)
						invokingComponent.requestFocus();
				}
			}
			public void keyReleased(KeyEvent e) {
				e.consume(); // Block key event popuping to the parent component.
			}
		});
		list.addMouseListener(new MouseAdapter() {		    
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					if (target != null) {
						target.setText(list.getSelectedValue().toString());
					}
					setDisplay(false);
					if (invokingComponent != null)
						invokingComponent.requestFocus();
					startText = null;
					blocking = true;
				}
			}
		});
		getViewport().setView(list);
		setVisible(false);
	}
	
	public void setData(java.util.List data) {
		this.data = data;
	}
	
	public JList getList() {
	    return this.list;
	}

	public void setDisplay(boolean display) {
		if (display && !isVisible()) {
			JLayeredPane layeredPane = SwingUtilities.getRootPane(invokingComponent).getLayeredPane();
			int x = textBounds.x;
			int y = textBounds.y + textBounds.height + 2;
			x *= scaleX;
			y *= scaleY;
			Point p = SwingUtilities.convertPoint(invokingComponent, 
			                                      x,
			                                      y,
			                                      layeredPane);
			setBounds(p.x, 
			          p.y, 
			          textBounds.width,
			          HEIGHT);
			layeredPane.add(this, JLayeredPane.POPUP_LAYER);
			setVisible(true);
		}
		else if (!display && isVisible()) {
			if (getParent() != null) {
				Container parent = getParent();
				parent.remove(this);
				parent.repaint(getX(), getY(), getWidth(), getHeight()); // Force to repaint
			}
			setVisible(false);
		}
	}

	public void setScaleX(double scaleX) {
	    this.scaleX = scaleX;
	}
	
	public void setScaleY(double scaleY) {
	    this.scaleY = scaleY;
	}
	
	public void setStartText(String text) {
		if (blocking) {
			// Only need to blocking once
			blocking = false;
			return;
		}
		if (text.equals(startText))
			return;
		startText = text;
		if (thread == null) {
			thread = new Runnable() {
				public void run() {
					DefaultListModel model = new DefaultListModel();
					if (startText.length() > 0) {
						int index = Collections.binarySearch(data, startText);
						int size = data.size();
						if (index < 0)
							index = -index - 1;
						// Search down needed only
						for (int i = index; i < size; i++) {
							String txt = (String)data.get(i);
							if (txt.startsWith(startText))
								model.addElement(txt);
							else
								break;
						}
					}
					list.setModel(model);
					if (model.getSize() == 0)
						setDisplay(false);
					else
						setDisplay(true);
				}
			};
		}
		new Thread(thread).start();
	}

	public void start() {
		if (isVisible()) {
			list.setSelectedIndex(0);
			list.requestFocus();
		}
	}

	public void setTextBounds(Rectangle rect) {
		textBounds = rect;
	}

	public Rectangle getTextBounds() {
		return textBounds;
	}

	public void setInvokingComponent(JComponent comp) {
		this.invokingComponent = comp;
	}

	public void setTarget(AutoCompletable target) {
		this.target = target;
	}
}
