/*
 * Created on Aug 27, 2003
 */
package org.gk.util;

/**
 * A pure digraph edge.
 * @author wgm
 */
public class Edge {
	Vertex tail;
	Vertex head;

	public Edge() {
	}
	
	public Edge(Vertex tail, Vertex head) {
		this.tail = tail;
		this.head = head;
	}
	
	public Vertex getTail() {
	    return this.tail;
	}
	
	public Vertex getHead() {
	    return this.head;
	}
	
}
