/*
 * Created on Dec 15, 2006
 *
 */
package org.gk.render;

/**
 * This class is used to describe proteins.
 * @author wgm
 *
 */
public class RenderableProtein extends Node {
    
    public RenderableProtein() {
        isFeatureAddable = true;
        isStateAddable = true;
        isMultimerFormable = true;
    }
    
    public String getType() {
        return "Protein";
    }
    
    public Renderable generateShortcut() {
        RenderableProtein shortcut = new RenderableProtein();
        generateShortcut(shortcut);
        return shortcut;
    }
}
