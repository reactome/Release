/*
 * ReactionConnectInfo.java
 *
 * Created on June 19, 2003, 10:23 AM
 */

package org.gk.render;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * This class holds information related to a Reaction connecting to other nodes (i.e., Entities, Complexes, or Pathways).
 * @author  wgm
 */
public class HyperEdgeConnectInfo extends ConnectInfo {
    
    /** Creates a new instance of ReactionConnectInfo */
    public HyperEdgeConnectInfo() {
        super();
    }
    
    /**
     * Get a list of ConnectWidgets that are used for input entities.
     */
    public java.util.List getInputWidgets() {
        java.util.List inputs = new ArrayList();
        if (connectWidgets != null) {
            ConnectWidget widget = null;
            for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
                widget = (ConnectWidget) it.next();
                if (widget.getRole() == HyperEdge.INPUT)
                    inputs.add(widget);
            }
        }
        return inputs;
    }
    
    /**
     * Get a list of ConnectWidgets that are used for output entities.
     */
    public java.util.List getOutputWidgets() {
        java.util.List outputs = new ArrayList();
        if (connectWidgets != null) {
            ConnectWidget widget = null;
            for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
                widget = (ConnectWidget) it.next();
                if (widget.getRole() == HyperEdge.OUTPUT)
                    outputs.add(widget);
            }
        }
        return outputs;
    }
    
    /**
     * Get a list of ConnectWidgets that are used for helper entities.
     */
    public java.util.List getHelperWidgets() {
        java.util.List helpers = new ArrayList();
        if (connectWidgets != null) {
            ConnectWidget widget = null;
            for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
                widget = (ConnectWidget) it.next();
                if (widget.getRole() == HyperEdge.CATALYST)
                    helpers.add(widget);
            }
        }
        return helpers;
    }
    
 	/**
 	 * Get a list of ConnectWidgets that are used for inhibitors.
 	 * @return
 	 */
    public java.util.List getInhibitorWidgets() {
    	java.util.List inhibitors = new ArrayList();
    	if (connectWidgets != null) {
    		ConnectWidget widget = null;
    		for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
    			widget = (ConnectWidget) it.next();
    			if (widget.getRole() == HyperEdge.INHIBITOR)
    				inhibitors.add(widget);
    		}
    	}
    	return inhibitors;
    }
    
    /**
     * Get a list of ConnectWidgets for activators.
     * @return
     */
    public java.util.List getActivatorWidgets() {
    	java.util.List activators = new ArrayList();
    	if (connectWidgets != null) {
    		ConnectWidget widget = null;
    		for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
    			widget = (ConnectWidget) it.next();
    			if (widget.getRole() == HyperEdge.ACTIVATOR)
    				activators.add(widget);
    		}
    	}
    	return activators;
    }
    
    /**
     * Get the ConnectWidget that is used to connect renderable.
     * @param renderable
     * @return
     */
    public ConnectWidget getInputConnectWidget(Renderable renderable) {
    	if (connectWidgets != null) {
    		for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
    			ConnectWidget widget = (ConnectWidget) it.next();
    			if (widget.getRole() == HyperEdge.INPUT &&
    			    widget.getConnectedNode() == renderable)
    				return widget;
    		}
    	}
    	return null;
    }
    
    /**
     * Get the ConnectWidget that is used to connect a specified ouput renderable.
     * @param renderable
     * @return
     */
    public ConnectWidget getOutputConnectWidget(Renderable renderable) {
    	if (connectWidgets != null) {
    		for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
    			ConnectWidget widget = (ConnectWidget) it.next();
    			if (widget.getRole() == HyperEdge.OUTPUT &&
    			    widget.getConnectedNode() == renderable)
    			    return widget;
    		}
    	}
    	return null;
    }
    
    public ConnectWidget getConnectWidget(Renderable node) {
    	if (connectWidgets != null) {
    		for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
    			ConnectWidget widget = (ConnectWidget) it.next();
    			if (widget.getConnectedNode() == node)
    				return widget;
    		}
    	}
    	return null;
    }
    
    public void clear() {
        if (connectWidgets != null) {
            for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
                ConnectWidget widget = (ConnectWidget) it.next();
                widget.getConnectedNode().removeConnectWidget(widget);
                it.remove();
            }
            connectWidgets = null;
        }
    }

    public void validate() {
        super.validate();
        if (connectWidgets == null || connectWidgets.size() == 0)
            return;
        // Split a little bit for inputs greater than 1
        Map nodeToWidget = new HashMap();
        for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
            ConnectWidget widget = (ConnectWidget) it.next();
            Node node = widget.getConnectedNode();
            if (node == null)
                continue;
            List list = (List) nodeToWidget.get(node);
            if (list == null) {
                list = new ArrayList();
                nodeToWidget.put(node, list);
            }
            list.add(widget);
        }
        for (Iterator it = nodeToWidget.keySet().iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            List list = (List) nodeToWidget.get(node);
            if (list.size() == 1)
                continue;
            if (!isJiggleNeeded(list))
                continue;
            Rectangle bounds = node.getBounds();
            // Need to find which value should be jiggled: x or y
            ConnectWidget widget = (ConnectWidget) list.get(0);
            Point p = widget.getPoint();
            if (p.x > bounds.x && p.x < bounds.getMaxX())
                jiggleX(list, bounds);
            else
                jiggleY(list, bounds);
        }
    }
    
    private boolean isJiggleNeeded(List widgets) {
        // Check if jiggle is needed. If two widgets even though there are more than two
        // don't have the same position, pass.
        ConnectWidget widget1 = (ConnectWidget) widgets.get(0);
        ConnectWidget widget2 = (ConnectWidget) widgets.get(1);
        return widget1.getPoint().equals(widget2.getPoint());
    }
    
    private void jiggleX(List widgets, Rectangle bounds) {
        ConnectWidget widget = null;
        Point p = null;
        int step = 7;
        int c = widgets.size();
        int start = -c / 2;
        for (int i = 0; i < c; i++) {
            widget = (ConnectWidget) widgets.get(i);
            p = widget.getPoint();
            p.x += start * step;
            if (p.x < bounds.x)
                p.x = bounds.x;
            if (p.x > bounds.getMaxX())
                p.x = (int) bounds.getMaxX();
            start ++;
        }
    }
    
    private void jiggleY(List widgets, Rectangle bounds) {
        ConnectWidget widget = null;
        Point p = null;
        int step = 7;
        int c = widgets.size();
        int start = -c / 2;
        for (int i = 0; i < c; i++) {
            widget = (ConnectWidget) widgets.get(i);
            p = widget.getPoint();
            p.y += start * step;
            if (p.y < bounds.y)
                p.y = bounds.y;
            if (p.y > bounds.getMaxY())
                p.y = (int) bounds.getMaxY();
            start ++;
        }
    }
}
