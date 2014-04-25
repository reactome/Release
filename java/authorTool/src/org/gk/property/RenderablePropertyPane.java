/*
 * Created on May 25, 2004
 */
package org.gk.property;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JPanel;

import org.gk.render.Renderable;

/**
 * A customized JPanel for displaying and editing Renderable properties.
 * @author wugm
 */
public abstract class RenderablePropertyPane extends JPanel {
	// The displayed Renderable object.
	protected Renderable r;
	// To catch RenderablePropertyChange events
	protected java.util.List rPropListeners;
	// To use one Event instance.
	protected RenderablePropertyChangeEvent rPropEvent;
    // This flag is used to block some listeners actions
    protected boolean duringSetting;

	public void setRenderable(Renderable r) {
        this.r = r;
	}
	
	public Renderable getRenderable() {
		return this.r;
	}
	
	public void addRenderablePropertyChangeListener(RenderablePropertyChangeListener l) {
		if (rPropListeners == null)
			rPropListeners = new ArrayList();
		rPropListeners.add(l);
	}
	
	public void removeRenderablePropertyChangeListener(RenderablePropertyChangeEvent l) {
		if (rPropListeners != null)
			rPropListeners.remove(l);
	}
	
	public void fireRenderablePropertyChange(Renderable r, String propName, Object oldValue, Object newValue) {
		if (rPropEvent == null)
			rPropEvent = new RenderablePropertyChangeEvent(r, propName, oldValue, newValue);
		else {
			rPropEvent.setRenderable(r);
			rPropEvent.setPropName(propName);
			rPropEvent.setOldValue(oldValue);
			rPropEvent.setNewValue(newValue);
		}
		fireRenderablePropertyChange(rPropEvent);
	}
	
	public void fireRenderablePropertyChange(RenderablePropertyChangeEvent e) {
		if (rPropListeners == null)
			return;
		RenderablePropertyChangeListener l = null;
		for (Iterator it = rPropListeners.iterator(); it.hasNext();) {
			l = (RenderablePropertyChangeListener) it.next();
			l.propertyChange(e);
		}
	}
}
