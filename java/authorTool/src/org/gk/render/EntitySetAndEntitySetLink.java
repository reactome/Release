/*
 * Created on Mar 12, 2012
 *
 */
package org.gk.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * This customized FlowLine is used to link two EntitySet instances together that share members.
 * @author gwu
 *
 */
public class EntitySetAndEntitySetLink extends FlowLine {
    
    public EntitySetAndEntitySetLink() {
        lineWidth = 0.5f;
        lineColor = Color.GRAY;
        setNeedOutputArrow(false);
    }
    
    /**
     * Set two EntitySet nodes that should be linked together.
     * @param set1
     * @param set2
     */
    public void setEntitySets(Node set1, Node set2) {
        addInput(set1);
        addOutput(set2);
    }
    
    /**
     * Get the two EntitySet nodes that are linked together by this object.
     * @return
     */
    public List<Node> getEntitySets() {
        List<Node> nodes = new ArrayList<Node>();
        Node input = getInputNode(0);
        if (input != null)
            nodes.add(input);
        Node output = getOutputNode(0);
        if (output != null)
            nodes.add(output);
        return nodes;
    }
    
}
