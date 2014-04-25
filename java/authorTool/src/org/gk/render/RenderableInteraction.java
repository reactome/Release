/*
 * Created on Jan 5, 2007
 *
 */
package org.gk.render;

public class RenderableInteraction extends FlowLine {
    private InteractionType interactionType;

    public RenderableInteraction() {
    }
    
    public void setInteractionType(InteractionType type) {
        this.interactionType = type;
    }
    
    public InteractionType getInteractionType() {
        return this.interactionType;
    }
    
    public String getType() {
        return "Interaction";
    }
}