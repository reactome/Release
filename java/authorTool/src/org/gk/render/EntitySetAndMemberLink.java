/*
 * Created on Oct 17, 2011
 *
 */
package org.gk.render;

import java.awt.Color;
import java.util.List;

/**
 * This customized FlowLine class is used to link EntitySet and its members in a pathway diagram.
 * @author gwu
 *
 */
public class EntitySetAndMemberLink extends FlowLine {
    
    public EntitySetAndMemberLink() {
        // Default settings
        lineWidth = 0.5f;
        lineColor = Color.black;
        setNeedOutputArrow(false);
    }
    
    public void setMember(Node node) {
        List<Node> inputs = getInputNodes();
        if (inputs != null && inputs.size() > 0)
            throw new IllegalStateException("EntitySet has been set already!");
        // Make sure nothing in the input
        addInput(node);
    }
    
    public Node getMember() {
        List<Node> inputs = getInputNodes();
        if (inputs == null || inputs.size() == 0)
            return null;
        return inputs.get(0);
    }
    
    public void setEntitySet(Node member) {
        List<Node> outputs = getOutputNodes();
        if (outputs != null && outputs.size() > 0)
            throw new IllegalStateException("Member has been set already!");
        addOutput(member);
    }
    
    public Node getEntitySet() {
        List<Node> outputs = getOutputNodes();
        if (outputs == null || outputs.size() == 0)
            return null;
        return outputs.get(0);
    }
    
}
