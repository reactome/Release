/*
 * Created on Mar 12, 2004
 */
package org.gk.pathView;

import java.awt.Color;

/**
 * An interface for defining edge. This is a temperate way. The AbstractEdge
 * and ReactionEdge in TestApp should be extracted as top-level classes.
 * @author wugm
 */
public interface IEdge {
	public static final int LINK_EDGE = 0;
	public static final int REACTION_EDGE = 1;

	/**
	 * Get the x coordinate for the tail Vertex.
	 * @return
	 */
	public int getTailX();
	
	/**
	 * Get the x coordinate for the head vertex.
	 * @return
	 */
	public int getHeadX();
	
	/**
	 * Get the y coordinate for the tail vertex.
	 * @return
	 */
	public int getTailY();
	
	/**
	 * Get the y coordinate for the head vertex.
	 * @return
	 */
	public int getHeadY();
	
	/**
	 * Transalte this edge.
	 * @param dx
	 * @param dy
	 */
	public void translate(int dx, int dy);
	
	public void setHead(int x, int y);
	
	public void setTail(int x, int y);
	
	public int length();
	
	public int getType();
	
	public Color getColor();
	
	public void setColor(Color c);
	
	/**
	 * Embed data object.
	 * @return
	 */
	public Object getUserObject(); 
}
