package org.reactome.web.fireworks.interfaces;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Deprecated
public interface Clickable {

    public boolean isMouseOver(Integer mouseX, Integer mouseY);
    public void setFocused(boolean focused);
    public void setHovered(boolean hovered);
    public void setSelected(boolean selected);

}