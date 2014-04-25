/*
 * RenderableReaction.java
 *
 * Created on June 12, 2003, 2:52 PM
 */

package org.gk.render;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * This class describe the renderable Reactions.
 * @author  wgm
 */
public class RenderableReaction extends HyperEdge {
    // To control rendering option
    private boolean drawAsNode = false;
    // Type of this reaction for drawing purposes
    private ReactionType type;
    
    /** Creates a new instance of RenderableReaction */
    public RenderableReaction() {
        super();
        setNeedOutputArrow(true);
        lineWidth = 1.0f;
    }       
    
    
    
    @Override
    public HyperEdge shallowCopy() {
        RenderableReaction edge = (RenderableReaction) super.shallowCopy();
        if (edge != null)
            edge.type = type;
        return edge;
    }

    /**
     * Set the type of this Reaction.
     * @param type
     */
    public void setReactionType(ReactionType type) {
        this.type = type;
    }
    
    /**
     * Get the type of this Reaction.
     * @return
     */
    public ReactionType getReactionType() {
        return this.type;
    }
    
    /**
     * To control if this RenderableReaction should be renderer as a Node.
     * @param asNode
     */
    public void setDisplayAsNode(boolean asNode) {
    	this.drawAsNode = asNode;
    }
    
    public boolean isDisplayAsNode() {
    	return this.drawAsNode;
    }
    
    public ReactionNode generateReactionNode() {
    	return new ReactionNode(this);
    }
    
    /**
     * This operation is not support.
     */
    public Renderable generateShortcut() {
        throw new IllegalStateException("RenderableReaction.generateShortcut() is not supported!");
	}
	
	private java.util.List clonePointList(java.util.List points) {
		if (points == null)
			return null;
		java.util.List list = new ArrayList(points.size());
		for (Iterator it = points.iterator(); it.hasNext();) {
			Point p = (Point) it.next();
			list.add(p.clone());
		}
		return list;
	}
	
	public void setInputStoichiometry(Renderable input, int stoichiometry) {		
		HyperEdgeConnectInfo edgeConnectInfo = (HyperEdgeConnectInfo) connectInfo;
		ConnectWidget widget = edgeConnectInfo.getInputConnectWidget(input);
		if (widget != null)
			widget.setStoichiometry(stoichiometry);
	}
	
	public int getInputStoichiometry(Renderable input) {
		HyperEdgeConnectInfo edgeConnectInfo = (HyperEdgeConnectInfo) connectInfo;
		ConnectWidget widget = edgeConnectInfo.getInputConnectWidget(input);
		if (widget != null)
			return widget.getStoichiometry();
		return 0;
	}
	
	public Map<Renderable, Integer> getInputStoichiometries() {
	    Map<Renderable, Integer> stoiMap = new HashMap<Renderable, Integer>();
	    List<Node> inputs = getInputNodes();
	    for (Node input : inputs) {
	        int stoi = getInputStoichiometry(input);
	        if (stoi != 0)
	            stoiMap.put(input, stoi);
	    }
	    return stoiMap;
	}
	
	public Map<Renderable, Integer> getOutputStoichiometries() {
	    Map<Renderable, Integer> stoiMap = new HashMap<Renderable, Integer>();
	    List<Node> inputs = getOutputNodes();
	    for (Node output : inputs) {
	        int stoi = getOutputStoichiometry(output);
	        if (stoi != 0)
	            stoiMap.put(output, stoi);
	    }
	    return stoiMap;
	}
	
	public void setOutputStoichiometry(Renderable output, int stoichiometry) {
		HyperEdgeConnectInfo edgeConnectInfo = (HyperEdgeConnectInfo) connectInfo;
		ConnectWidget widget = edgeConnectInfo.getOutputConnectWidget(output);
		if (widget != null)
			widget.setStoichiometry(stoichiometry);
	}
	
	public int getOutputStoichiometry(Renderable output) {
		HyperEdgeConnectInfo edgeConnectInfo = (HyperEdgeConnectInfo) connectInfo;
		ConnectWidget widget = edgeConnectInfo.getOutputConnectWidget(output);
		if (widget != null)
			return widget.getStoichiometry();
		return 0;
	}
	
	
	public int getStoichiometry(Renderable r) {
		HyperEdgeConnectInfo edgeConnectInfo = (HyperEdgeConnectInfo) connectInfo;
		ConnectWidget widget = edgeConnectInfo.getConnectWidget(r);
		if (widget != null)
			return widget.getStoichiometry();
		return 0;
	}
	
	public void setDisplayName(String name) {
		super.setDisplayName(name);
		if (getContainer() instanceof ReactionNode) {
			ReactionNode node = (ReactionNode) getContainer();
			if (node.getDisplayName() == null) {
				node.setDisplayName(name);
				node.invalidateBounds();
			}
			else if (!node.getDisplayName().equals(name)) {
				node.setDisplayName(name);
				node.invalidateBounds();
			}
		}
	}
	
	public String getType() {
		return "Reaction";
	}
}
