/*
 * NodeConnectInfo.java
 *
 * Created on June 20, 2003, 4:17 PM
 */

package org.gk.render;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * This class contains information related to nodes connecting to reactions.
 * @author  wgm
 */
public abstract class ConnectInfo implements Serializable {
    // A list of ConnectWidgets
    protected List<ConnectWidget> connectWidgets;
    
    /** Creates a new instance of NodeConnectInfo */
    public ConnectInfo() {
    }
    
    public void setConnectWidgets(List<ConnectWidget> widgets) {
        this.connectWidgets = widgets;
    }
    
    public List<ConnectWidget> getConnectWidgets() {
        return this.connectWidgets;
    }
    
    public void addConnectWidget(ConnectWidget widget) {
        if (connectWidgets == null)
            connectWidgets = new ArrayList<ConnectWidget>();
        if (!connectWidgets.contains(widget))
            connectWidgets.add(widget);
    }
    
    public void removeConnectWidget(ConnectWidget widget) {
        if (connectWidgets != null)
            connectWidgets.remove(widget);
    }
    
    public ConnectWidget searchConnectWidget(Point p) {
        if (connectWidgets == null)
            return null;
        for (ConnectWidget widget : connectWidgets) {
            if (widget.getPoint() == p)
                return widget;
        }
        return null;
    }
    
    public void invalidate() {
        if (connectWidgets != null) {
            HyperEdge edge = null;
            for (ConnectWidget widget : connectWidgets) {
                if (widget.isInvalidate())
                	continue;
                widget.invalidate();
				edge = widget.getEdge();
				// Have to invalidate both ends
				if (edge.getBackbonePoints().size() == 2) {
					edge.getConnectInfo().invalidate();
				}
            }
        }
    }
    
    public void validate() {
        if (connectWidgets == null)
            return;
        for (ConnectWidget widget : connectWidgets) {
            widget.validate();
        }
    }    
    
    public abstract void clear();
}
