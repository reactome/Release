/*
 * Created on Aug 22, 2003
 */
package org.gk.render;

import java.awt.Graphics;

/**
 * An Editor to edit the display name of a Renderable.
 * @author wgm
 */
public interface Editor {
	
	public void setRenderable(Renderable renderable);
	
	public Renderable getRenderable();
	
	public void setCaretPosition(int pos);
	
	public void setCaretPosition(int x, int y);
	
	public int getCaretPosition();
	
	public void moveCaretToLeft();
	
	public void moveCaretToRight();
	
	/**
	 * Move the caret up to one line.
	 * @return true if the moving actually happens otherwise false since 
	 * the caret is already at the top of the lines.
	 */
	public boolean moveCaretUp();
	
	/**
	 * Move the caret down to one line.
	 * @return true if the moving actually happens otherwise false since
	 * the caret is already at the bottom of the lines.
	 */
	public boolean moveCaretDown();
	/**
	 * Render method.
	 */
	public void render(Graphics g);
	
	// These methods are for selection.
	public void setSelectionStart(int start);
	
	public void setSelectionEnd(int end);
	
	/**
	 * Remove the selection and reset the internal data.
	 */
	public void clearSelection();

	public int getSelectionStart();
	
	public int getSelectionEnd();
	
	/**
	 * Reset to an empty state.
	 */
	public void reset();
	
	public void setIsChanged(boolean isChanged);
	
	/**
	 * Query if there is a change for the Renderable name.
	 * @return
	 */
	public boolean isChanged();
}
