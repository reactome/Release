/*
 * Created on Jun 11, 2008
 *
 */
package org.gk.render;

/**
 * This class is used to describe state information for Node (Protein or other macromolecules).
 * For example, active, open, close state. For in the current implementation of RenderableState, 
 * label and description should be the same.
 * @author wgm
 *
 */
public class RenderableState extends NodeAttachment {
    private String desc;
    
    public RenderableState() {
        textPadding = 4;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public String getLabel() {
        return desc;
    }

    @Override
    public void setDescription(String description) {
        this.desc = description;
    }

    @Override
    public void setLabel(String label) {
        this.desc = label;
    }
    
    @Override
    public NodeAttachment duplicate() {
        RenderableState clone = new RenderableState();
        clone.desc = desc;
        clone.setRelativePosition(relativeX, relativeY);
        clone.setTrackId(trackId);
        return clone;
    }
    
}
