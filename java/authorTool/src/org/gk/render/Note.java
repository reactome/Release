/*
 * Created on Dec 20, 2006
 *
 */
package org.gk.render;


public class Note extends Node {
    // A private Note will not be shown in a deployed diagram for the public.
    private boolean isPrivate;
    
    public Note() {
        isLinkable = false;
        isTransferrable = true;
    }
    
    public String getType() {
        return "Note";
    }

    protected void validateConnectWidget(ConnectWidget widget) {
        // Do nothing
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
    
}
