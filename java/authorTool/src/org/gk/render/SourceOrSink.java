/*
 * Created on Jun 25, 2008
 *
 */
package org.gk.render;

import java.awt.Graphics;

/**
 * This class is used to describe a SourceOrSink.
 * @author wgm
 *
 */
public class SourceOrSink extends Node {
    
    public SourceOrSink() {
       isEditable = false;
    }

    @Override
    public String getType() {
        return "Source/Sink";
    }

    @Override
    protected void initBounds(Graphics g) {
        // Default size
        int w = 35;
        int h = 35;
        bounds.x = position.x - w / 2;
        bounds.y = position.y - h / 2;
        bounds.width = w;
        bounds.height = h;   
    }
    
    
    
}
