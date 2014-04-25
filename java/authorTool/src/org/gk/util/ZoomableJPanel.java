/*
 * Created on Jul 29, 2010
 *
 */
package org.gk.util;

import javax.swing.JPanel;

/**
 * A customized JPanel that can be zoomed.
 * @author wgm
 *
 */
public class ZoomableJPanel extends JPanel {
    // To support zooming
    protected double scaleX = 1;
    protected double scaleY = 1;
    
    public ZoomableJPanel() {
        super();
    }
    
    /**
     * This method is used to set scale of this pane without repaint.
     * @param scaleX
     * @param scaleY
     * @see zoom(double, double)
     */
    public void setScale(double scaleX, double scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }
    
    
    public double getScaleX() {
        return this.scaleX;
    }
    
    public double getScaleY() {
        return this.scaleY;
    }
}
