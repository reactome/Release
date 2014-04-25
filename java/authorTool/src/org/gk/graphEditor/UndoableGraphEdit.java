/*
 * Created on Aug 14, 2008
 *
 */
package org.gk.graphEditor;

import java.awt.Point;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;

import org.gk.render.Renderable;


/**
 * The super class for all undoable edit related GraphEditorPane. There is not not much
 * in this class. This is more for organization purpose.
 * @author wgm
 *
 */
public class UndoableGraphEdit extends AbstractUndoableEdit {
    
    protected GraphEditorPane graphPane;
    
    public void setGraphEditorPane(GraphEditorPane graphPane) {
        this.graphPane = graphPane;
    }
    
    protected void storeContainerInfo(Map<Renderable, Renderable> rToContainer,
                                    List<Renderable> objects) {
        for (Renderable r : objects) {
            rToContainer.put(r, r.getContainer());
        }
    }
    
    protected void recoverContainerInfo(Renderable r,
                                        Map<Renderable, Renderable> rToContainer) {
        Renderable container = rToContainer.get(r);
        r.setContainer(container);
        if (container != null) {
            // Check if r is already contained by the container
            if (container.getComponents() == null ||
                !container.getComponents().contains(r))
                container.addComponent(r);
        }
    }
    
    /**
     * A small data structure to store information for edge.
     * @author wgm
     *
     */
    protected class EdgePositionInfo {
        List<Point> backbone;
        List<List<Point>> inputs;
        List<List<Point>> outputs;
        List<List<Point>> catalysts;
        List<List<Point>> activators;
        List<List<Point>> inhibitors;
        
        public EdgePositionInfo() {
        }
        
    }

}
