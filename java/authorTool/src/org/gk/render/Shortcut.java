/*
 * Created on Jul 21, 2003
 */
package org.gk.render;

import java.awt.Point;

/**
 * A marker for a shortcut.
 * @author wgm
 */
public interface Shortcut {
	/**
	 * Get the actual Renderable that this Shortcut refers to.
	 * @return
	 */
	public Renderable getTarget();
	
	public void setPosition(Point pos);
	
	public Point getPosition();
	
}
