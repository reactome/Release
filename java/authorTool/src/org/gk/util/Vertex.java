/*
 * Created on Aug 27, 2003
 */
package org.gk.util;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * A pure digraph vertex.
 * @author wgm
 */
public class Vertex {
	private String name; // The text label
	private List inNeighbor; // Vertices linked to this Vertex
	private List outNeighbor; // Vertices this Vertex linked to
	protected int label; // An integer label
	protected int label1; // Another label might be needed
	protected double label2; // A double label
	protected boolean label3; // A boolean label
	protected Dimension size; // The physical size
	protected Point pos;
	
	public Vertex() {
	}
	
	public Vertex(String name) {
		this();
		setName(name);
	}

	public List getInNeighbor() {
		return inNeighbor;
	}

	public int getLabel() {
		return label;
	}

	public String getName() {
		return name;
	}

	public List getOutNeighbor() {
		return outNeighbor;
	}

	public Point getPosition() {
		return pos;
	}

	public Dimension getSize() {
		return size;
	}

	public void setInNeighbor(List list) {
		inNeighbor = list;
	}
	
	public void addInVertex(Vertex v) {
		if (inNeighbor == null)
			inNeighbor = new ArrayList();
		inNeighbor.add(v);
	}
	
	public void removeInVertex(Vertex v) {
		if (inNeighbor != null)
			inNeighbor.remove(v);
	}

	public void setLabel(int i) {
		label = i;
	}

	public void setName(String string) {
		name = string;
	}

	public void setOutNeighbor(List list) {
		outNeighbor = list;
	}
	
	public void addOutVertex(Vertex v) {
		if (outNeighbor == null)
			outNeighbor = new ArrayList();
		outNeighbor.add(v);
	}
	
	public void removeOutVertex(Vertex v) {
		if (outNeighbor != null)
			outNeighbor.remove(v);
	}

	public void setPosition(Point position) {
		pos = position;
	}

	public void setSize(Dimension dimension) {
		size = dimension;
	}
	
	public int getInDegree() {
		if (inNeighbor == null)
			return 0;
		return inNeighbor.size();
	}
	
	public int getOutDegree() {
		if (outNeighbor == null)
			return 0;
		return outNeighbor.size();
	}
	
	public int getOutInDegreeDiff() {
		int inDegree = getInDegree();
		int outDegree = getOutDegree();
		return outDegree - inDegree;
	}
	
	public String toString() {
		if (name != null)	
			return name;
		return super.toString();
	}

}
