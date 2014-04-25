/*
 * Created on Aug 27, 2003
 */
package org.gk.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A pure DiGraph. 
 * @author wgm
 */
public class Graph {
	// A list of all vertices
	private List vertices;
	// A list of all edges
	private List edges;
	
	public Graph() {
	}
	
	public void addVertex(Vertex v) {
		if (vertices == null)
			vertices = new ArrayList();
		vertices.add(v);
	}
	
	public List getVertices() {
		return vertices;
	}
	
	public int getVertexSize() {
		if (vertices == null)
			return 0;
		return vertices.size();
	}
	
	public List getEdges() {
		return this.edges;
	}
	
	/**
	 * Add an Edge. Adding an Edge will add in or out Vertex to the out or in
	 * Vertex's neighbor list.
	 * @param edge
	 */
	public void addEdge(Edge edge) {
		if (edges == null)
			edges = new ArrayList();
		edges.add(edge);
		// Make the data structure consistent.
		if (edge.tail != null && edge.head != null) {
			edge.tail.addOutVertex(edge.head);
			edge.head.addInVertex(edge.tail);
		}
	}
	
	public void addEdge(Vertex tail, Vertex head) {
		addEdge(new Edge(tail, head));
	}
	
	public void removeEdge(Edge edge) {
		if (edges != null)
			edges.remove(edge);
		// Make the data structure consistent
		if (edge.tail != null && edge.head != null) {
			edge.tail.removeOutVertex(edge.head);
			edge.head.removeInVertex(edge.tail);
		}
	}
	
	public void setVerteces(java.util.List vertices) {
		this.vertices = vertices;
	}
	
	public void setEdges(java.util.List edges) {
		this.edges = edges;
	}
}
