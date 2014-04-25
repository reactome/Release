/*
 * Created on Apr 3, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.gk.pathwaylayout;

import java.awt.Color;

import org.gk.model.GKInstance;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultPort;

/**
 * @author vastrik
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Edge extends DefaultEdge {
	protected Vertex sourceVertex;
	protected Vertex targetVertex;
	protected int preferredLength = PathwayLayoutConstants.DEFAULT_EDGE_LENGTH;
	public GKInstance storageInstance;
	protected Color color = null;
	protected int edgeType;
	
	public Edge() {
	}

	public Edge(Vertex from, Vertex to) {
		// Fetch the ports from the new vertices, and connect them with the edge
    	//setSource(from.getChildAt(0));
    	//setTarget(to.getChildAt(0));
    	setSourceVertex(from);
    	setTargetVertex(to);
	}
	
	public Edge(Vertex from, Vertex to, int edgeType) {
		this(from, to);
		setType(edgeType);
	}
	
	/**
	 * @return
	 */
	public Vertex getSourceVertex() {
		return sourceVertex;
	}
	
	/**
	 * @return
	 */
	public Vertex getTargetVertex() {
		return targetVertex;
	}
	
	/**
	 * @param vertex
	 */
	public void setSourceVertex(Vertex vertex) {
		sourceVertex = vertex;
		DefaultPort p = (DefaultPort)vertex.getChildAt(0);
		setSource(p);
		//setSource(vertex.getChildAt(0));
		p.addEdge(this);
	}

	/**
	 * @param vertex
	 */
	public void setTargetVertex(Vertex vertex) {
		targetVertex = vertex;
		DefaultPort p = (DefaultPort)vertex.getChildAt(0);
		setTarget(p);
		//setTarget(vertex.getChildAt(0));
		p.addEdge(this);
	}

	/**
	 * @return
	 */
	public int getPreferredLength() {
		return preferredLength;
	}

	/**
	 * @param i
	 */
	public void setPreferredLength(int i) {
		preferredLength = i;
	}
	
/*	public Dimension getDimension() {
		return new Dimension(targetVertex.x - sourceVertex.x, targetVertex.y - sourceVertex.y);
	}
	
	public double getLength() {
		Dimension d = getDimension();
		return Math.sqrt(d.getWidth() * d.getWidth() + d.getHeight() * d.getHeight());
	}*/
	
/*	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(this.getClass().getName());
		buf.append(" from:" + getSourceVertex().toString() + " to:" + getTargetVertex().toString());
		//buf.append(" getDistance:" + getDimension().toString() + "\n");
		//buf.append(" length:" + getLength() + " preferredLength:" + getPreferredLength() + "\n");
		return buf.toString();
	}*/

	public String toString2() {
		return getType() + "\t" + getSourceVertex() + " -> " + getTargetVertex();
	}
	
	/**
	 * @return
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * @param color
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	protected boolean isFixed = false;
	/**
	* @return
	*/
	public boolean isFixed() {
		return isFixed;
	}

	public int getType() {
		return edgeType;
	}

	public void setType(int edgeType) {
		this.edgeType = edgeType;
	}

	/**
	* @param b
	*/
/*	public void setFixed(boolean b) {
		isFixed = b;
		getSourceVertex().isFixed = b;
		getTargetVertex().isFixed = b;
	}
	
	public void translate(int x, int y) {
		sourceVertex.x += x;
		sourceVertex.y += y;
		targetVertex.x += x;
		targetVertex.y += y;
	}
	
	public void setTail(int x, int y) {
		sourceVertex.x = x;
		sourceVertex.y = y;
	}
	
	public void setHead(int x, int y) {
		targetVertex.x = x;
		targetVertex.y = y;
	}

	protected void finalize() {
		if (debug) System.out.println("Finalized " + this);
	}
	
	public int getTailX() {
		return sourceVertex.x;
	}
	
	public int getTailY() {
		return sourceVertex.y;
	}
	
	public int getHeadX() {
		return targetVertex.x;
	}
	
	public int getHeadY() {
		return targetVertex.y;
	}
	
	public int length() {
		return (int) Point2D.distance(sourceVertex.x, sourceVertex.y,
		                              targetVertex.x, targetVertex.y);
	}*/
}
