/*
 * Created on May 25, 2004
 */
package org.gk.property;

import java.util.EventListener;

/**
 * An Event listener to catch property change in a Renderable object.
 * @author wugm
 */
public interface RenderablePropertyChangeListener extends EventListener {

	public void propertyChange(RenderablePropertyChangeEvent e);

}
