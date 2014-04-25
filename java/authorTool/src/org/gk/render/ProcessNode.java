/*
 * Created on Jul 7, 2008
 *
 */
package org.gk.render;


/**
 * This class is used to describe a process. The functions of this class are
 * refactored from RenderablePathway.
 * @author wgm
 *
 */
public class ProcessNode extends Node {
    
    public ProcessNode() {
        boundsBuffer = 8;
        // Make sure a ProcessNode cannot be copy/paste to make
        // preceding/following event links clear!
        isTransferrable = false;
    }
    
    public ProcessNode(String displayName) {
        this();
        setDisplayName(displayName);
    }
    
    public Renderable generateShortcut() {
        ProcessNode shortcut = new ProcessNode();
        generateShortcut(shortcut);
        return shortcut;
    }
        
    public String getType() {
        return "Process";
    }
    
}
