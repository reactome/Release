/*
 * Renderer.java
 *
 * Created on June 12, 2003, 2:26 PM
 */

package org.gk.render;

import java.awt.Graphics;
import java.io.Serializable;

/**
 * An interface defines methods that can be used for rendering Renderable objects.
 * @author  wgm
 */
public interface Renderer extends Serializable {
    
    public void setRenderable(Renderable r);
    
    /**
     * Render method.
     */
    public void render(Graphics g);
    
}
