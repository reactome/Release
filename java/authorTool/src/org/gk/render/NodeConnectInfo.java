/*
 * NodeConnectInfo.java
 *
 * Created on June 20, 2003, 4:25 PM
 */

package org.gk.render;

import java.util.Iterator;
/**
 * A subclass of ConnectInfo for a Node that is connected to reactions.
 * @author  wgm
 */
public class NodeConnectInfo extends ConnectInfo {
    
    /** Creates a new instance of NodeConnectInfo */
    public NodeConnectInfo() {
        super();
    }
    
    public void clear() {
        if (connectWidgets != null) {
            for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
                ConnectWidget widget = (ConnectWidget) it.next();
                widget.getEdge().removeConnectWidget(widget);
                // Don't want to keep any unused branch
                widget.getEdge().deleteUnAttachedBranch(widget);
                it.remove();
            }
            connectWidgets = null;
        }
    }
    
}
