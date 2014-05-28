/*
 * GraphEditorAction.java
 *
 * Created on June 16, 2003, 10:10 PM
 */

package org.gk.graphEditor;

/**
 * A general interface that is used for mouse or other actions in complex, reaction and pathway editors.
 * @author  wgm
 */
public interface GraphEditorAction {
    /**
     * Do something here.
     */
    public void doAction(java.awt.event.MouseEvent e);
}
