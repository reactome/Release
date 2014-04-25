/*
 * Created on Aug 27, 2003
 */
package org.gk.util;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * This class implements a hierarchical layout based on the book "Graph Drawing"
 * by Battista, G., Eades, P., Tamassia, R & Tollis, I.G. The last step,
 * assignment of x coordinates is based on the paper "A Technique for Drawing Directed
 * Graphs" by Gansner, E.R., Koutsofios, E., North, S.C. & Vo, KP.
 * @author wgm
 */
public class HierarchicalLayout {
	private boolean isDebug = false;
	// The top-left corner position
	private final int MIN_X = 30;
	private final int MIN_Y = 30;
	// a static properties
	private static int nodeDist = 50;
	private static int layerDist = 40;

	public HierarchicalLayout() {
	}
	
	public static void setNodeDistance(int dist) {
		nodeDist = dist;
	}
	
	public static void setLayerDistance(int dist) {
		layerDist = dist;
	}
	
	public static int getNodeDistance() {
		return nodeDist;
	}
	
	public static int getLayerDistance() {
		return layerDist;
	}
	
	public void layout(Graph g) {
		if (g.getVertexSize() < 2) // No need to do layout
			return ;
		java.util.List sortedList = removeCycles(g);
		java.util.List layers = assignLayers(sortedList);
		addDummyNodes(layers, g);
		reduceCrossings(layers);
		assignCoordinates(layers);
		optimize(g.getVertices());
	}
	
	/**
	 * Do some clean up work.
	 * @param layers
	 */
	private void optimize(java.util.List vertices) {
		// Find the up-left corner position
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int x, y;
		Vertex v = null;
		for (Iterator it = vertices.iterator(); it.hasNext();) {
			v = (Vertex)it.next();
			x = v.pos.x - v.size.width / 2;
			if (x < minX)
				minX = x;
			y = v.pos.y - v.size.height / 2;
			if (y < minY)
				minY = y;
		}
		int diffX = MIN_X - minX;
		if (diffX != 0) {
			for (Iterator it = vertices.iterator(); it.hasNext();) {
				v = (Vertex) it.next();
				v.pos.x += diffX;
			}
		}
		int diffY = MIN_Y - minY;
		if (diffY != 0) {
			for (Iterator it = vertices.iterator(); it.hasNext();) {
				v = (Vertex) it.next();
				v.pos.y += diffY;
			}
		}
	}
	
	private java.util.List removeCycles(Graph g) {
		java.util.List vertices = new ArrayList(g.getVertices());
		int n = vertices.size();
		java.util.List leftList = new ArrayList();
		java.util.List rightList = new ArrayList();
		// Label vertices with degree
		Vertex v = null;
		for (Iterator it = vertices.iterator(); it.hasNext();) {
			v = (Vertex) it.next();
			v.label = v.getInDegree();
			v.label1 = v.getOutDegree();
		}
		while (vertices.size() > 0) {
			// Find all sinks and put them in the right list
			boolean hasSinks = true;
			while (hasSinks) {
				hasSinks = false;
				for (Iterator it = vertices.iterator(); it.hasNext();) {
					v = (Vertex)it.next();
					if (v.label1 == 0) {
						hasSinks = true;
						rightList.add(0, v);
						it.remove();
						// Update v's neighbor's labels
						java.util.List inNeighbor = v.getInNeighbor();
						if (inNeighbor != null) {
							for (Iterator it1 = inNeighbor.iterator(); it1.hasNext();) {
								Vertex v1 = (Vertex)it1.next();
								v1.label1--;
							}
						}
					}
				}
			}
			// Find all sources
			boolean hasSources = true;
			while (hasSources) {
				hasSources = false;
				for (Iterator it = vertices.iterator(); it.hasNext();) {
					v = (Vertex) it.next();
					if (v.label == 0) {
						hasSources = true;
						leftList.add(v);
						it.remove();
						// Update v's neighbor's labels
						java.util.List outNeighbor = v.getOutNeighbor();
						if (outNeighbor != null) {
							for (Iterator it1 = outNeighbor.iterator(); it1.hasNext();) {
								Vertex v1 = (Vertex) it1.next();
								v1.label --;
							}
						}
					}
				}
			}
			// Find a vertex with max outDeg - inDeg
			int max = Integer.MIN_VALUE;
			Vertex v1 = null;
			int index = 0;
			int diff;
			for (int i = 0; i < vertices.size(); i++) {
				v = (Vertex) vertices.get(i);
				diff = v.getOutInDegreeDiff();
				if (diff > max) {
					max = diff;
					v1 = v;
					index = i;
				}	
			}
			// Remove v1 
			if (v1 != null) {
				vertices.remove(index);
				leftList.add(v1);
				java.util.List inNeighbor = v1.getInNeighbor();
				if (inNeighbor != null) {
					for (Iterator it = inNeighbor.iterator(); it.hasNext();) {
						v = (Vertex)it.next();
						v.label1--;
					}
				}
				java.util.List outNeighbor = v1.getOutNeighbor();
				if (outNeighbor != null) {
					for (Iterator it = outNeighbor.iterator(); it.hasNext();) {
						v = (Vertex)it.next();
						v.label--;
					}
				}
			}
		}
		java.util.List sortedList = new ArrayList(leftList);
		sortedList.addAll(rightList);
		// Label vertex with sorted index
		for (int i = 0; i < sortedList. size(); i++) {
			v = (Vertex) sortedList.get(i);
			v.label = i;
		}
		// Check all edges 
		java.util.List edges = g.getEdges();
		if (edges != null) {
			Edge e = null;
			for (Iterator it = edges.iterator(); it.hasNext();) {
				e = (Edge)it.next();
                // Maybe an unfinished node
                if (e.tail == null || e.head == null)
                    continue;
				if (e.tail.label > e.head.label) { // A leftward edge. Revert it.
					// Do revertion
					e.tail.removeOutVertex(e.head);
					e.tail.addInVertex(e.head);
					e.head.removeInVertex(e.tail);
					e.head.addOutVertex(e.tail);
				}
			}
		}
		// A test
		if (isDebug) {
			StringBuffer b = new StringBuffer();
			for (Iterator it = sortedList.iterator(); it.hasNext();) {
				v = (Vertex)it.next();
				b.append(v + ", ");
			}
			System.out.println("Sorted List: " + b.toString());
		}
		return sortedList;
	}
	
	/**
	 * This method is an modification of Coffman-Graham-Layering algorithm. The sorted list from
	 * removing cycles method is directly used for assigning layers. No extra labeling procedure 
	 * is used.
	 * @param vertices the sorted list generated from greedy cycle removal method.
	 * @return a list of layers (i.e. A list of Vertex objects).
	 */
	private java.util.List assignLayers(java.util.List vertices) {
		// calculate the width of the layer
		int n = vertices.size();
		int w = (int) Math.sqrt(n);
		java.util.List layers = new ArrayList();
		java.util.List layer = new ArrayList();
		// First layer (i.e. the bottom layer)
		java.util.List list = new ArrayList(vertices);
		layers.add(layer);
		Vertex v = null;
		int index = 0;
		int size = list.size();
		java.util.List outNeighbors;
		while (size > 0) {
			v = (Vertex) list.get(size - 1);
			// Find the minimum layer index based on its outneighbors
			outNeighbors = v.getOutNeighbor();
			if (outNeighbors != null && outNeighbors.size() > 0) {
				int max = Integer.MIN_VALUE;
				for (Iterator it = outNeighbors.iterator(); it.hasNext();) {
					Vertex v1 = (Vertex) it.next();
					if (v1.label > max)
						max = v1.label;
				}
				index = max + 1; // Should be at least one layer higher than the outneighbors.
			}
			if (index > layers.size() - 1) { 
				layer = new ArrayList();
				layers.add(layer);
				index = layers.size() - 1;
			}
			else {
				layer = (java.util.List)layers.get(index);
				if (layer.size() > w) { // Need to check higher layer
					while (true) {
						++index;
						if (index > layers.size() - 1) {
							layer = null;
							break;
						}
						layer = (java.util.List)layers.get(index);
						if (layer.size() < w)
							break;
					}
					if (layer == null) {
						layer = new ArrayList();
						layers.add(layer);
					}
				}
			}
			layer.add(v);
			v.label = index;
			list.remove(size - 1);
			size --;
		}
		// Need to push the sinks up as high as possible since the creation
		// of the sorted list.
		for (Iterator it = layers.iterator(); it.hasNext();) {
			layer = (java.util.List) it.next();
			for (Iterator it1 = layer.iterator(); it1.hasNext();) {
				v = (Vertex) it1.next();
				if (v.getOutDegree() == 0) { // This is a sink
					// Find the topmost layer this vertex can be
					int min = layers.size();
					java.util.List inNeighobors = v.getInNeighbor();
					if (inNeighobors != null && inNeighobors.size() > 0) {
						for (Iterator it2 = inNeighobors.iterator(); it2.hasNext();) {
							Vertex v1 = (Vertex) it2.next();
							if (v1.label < min)
								min = v1.label;
						}
					}
					if (v.label < min - 1) {
						java.util.List layer1 = (java.util.List) layers.get(min - 1);
						layer1.add(v);
						v.label = min - 1;
						it1.remove();
					}
				}
			}
		}
		if (isDebug)
			showLayers(layers, "Layer assignment");
		return layers;
	}
	
	private void showLayers(java.util.List layers, String label) {
		// A test
		StringBuffer buffer = new StringBuffer();
		java.util.List layer = null;
		Vertex v = null;
		for (Iterator it = layers.iterator(); it.hasNext();) {
			layer = (java.util.List) it.next();
			for (Iterator i = layer.iterator(); i.hasNext();) {
				v = (Vertex) i.next();
				buffer.append(v.getName() + ", ");
			}
			buffer.append("\n");
		}
		System.out.println(label + ": \n" + buffer.toString());
	}
	
	private void addDummyNodes(java.util.List layers, Graph g) {
		java.util.List layer = null;
		java.util.List edges = g.getEdges();
		if (edges == null)
			return;
		java.util.List newEdges = new ArrayList();
		java.util.List removedEdges = new ArrayList();
		Edge edge = null;
		Dimension emptySize = new Dimension(5, 5);
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (Edge) it.next();
            if (edge.head == null || edge.tail == null)
                continue;
			Vertex vHead = edge.head;
			for (int i = edge.head.label + 1; i < edge.tail.label; i++) {
				layer = (java.util.List) layers.get(i);
				Vertex v = new Vertex("dummy");
				v.pos = new Point(); // Give it a non-empty position
				v.setSize(emptySize);
				layer.add(v);
				newEdges.add(new Edge(v, vHead)); 
				vHead = v;
			}
			if (vHead != edge.head) {
				newEdges.add(new Edge(edge.tail, vHead));
				removedEdges.add(edge);
			}
		}
		for (Iterator it = newEdges.iterator(); it.hasNext();) {
			edge = (Edge) it.next();
			g.addEdge(edge);
		}
		for (Iterator it = removedEdges.iterator(); it.hasNext();) {
			edge = (Edge) it.next();
			g.removeEdge(edge);
		}
		if (isDebug)
			showLayers(layers, "Add Dummy Nodes");
	}
	
	private void reduceCrossings(java.util.List layers) {
		java.util.List layer1 = null;
		java.util.List layer2 = null;
		Vertex v = null;
		Comparator comp = new Comparator() {
			public int compare(Object obj1, Object obj2) {
				Vertex v1 = (Vertex) obj1;
				Vertex v2 = (Vertex) obj2;
				int rtn = v1.label1 - v2.label1;
				if (rtn == 0) {
					double diff = v1.label2 - v2.label2;
					if (diff > 0.0)
						rtn = 1;
					else if (diff < 0.0)
						rtn = -1;
					else
						rtn = 0;
				}
				return rtn;
			}
		};
		// Initialize the first layer
		layer1 = (java.util.List) layers.get(0);
		for (int i = 0; i < layer1.size(); i++) {
			v = (Vertex) layer1.get(i);
			v.label1 = i;
		}
		// Vertex.label1 for median position
		// Vertex.label2 for barycenter
		for (int i = 0; i < layers.size() - 1; i++) {
			layer1 = (java.util.List) layers.get(i);
			layer2 = (java.util.List) layers.get(i + 1);
			// calculate the median of the vertices on layer2
			for (Iterator it = layer2.iterator(); it.hasNext();) {
				v = (Vertex) it.next();
				java.util.List outNeighbors = v.getOutNeighbor();
				if (outNeighbors == null || outNeighbors.size() == 0) {
					v.label1 = 0;
					v.label2 = 0.0;
					continue;
				}
				Collections.sort(outNeighbors, comp);
				int index = outNeighbors.size() / 2;
				v.label1 = ((Vertex)outNeighbors.get(index)).label;
				int total = 0;
				for (Iterator i1 = outNeighbors.iterator(); i1.hasNext();) {
					Vertex v1 = (Vertex) i1.next();
					total += v1.label1;
				}
				v.label2 = (double)total / outNeighbors.size();
			}
			Collections.sort(layer2, comp);
			// Assign positions
			for (int j = 0; j < layer2.size(); j++) {
				v = (Vertex) layer2.get(j);
				v.label1 = j;
			}
		}
		if (isDebug)
			showLayers(layers, "Reduce Crossings");
	}
	
	private void assignCoordinates(java.util.List layers) {
		java.util.List layer = null;
		Vertex v = null;
		// Initialize the coordinates
		// Start position
		int x = 50;
		int y = 50;
		// Start from the top
		for (int i = layers.size() - 1; i >= 0; i --) {
			layer = (java.util.List) layers.get(i);
			// Find the tallest node
			int tallest = 0;
			for (Iterator it = layer.iterator(); it.hasNext();) {
				v = (Vertex) it.next();
				if (v.size.height > tallest)
					tallest = v.size.height;
			}
			y += tallest / 2;
			for (Iterator it = layer.iterator(); it.hasNext();) {
				v = (Vertex) it.next();
				v.pos.x = (x + v.size.width / 2);
				v.pos.y = y;
				x += (v.getSize().width + nodeDist);
			}
			y += (tallest / 2 + layerDist);
			x = 50;
		}
		// Before assinging median positions, have to make the first layer first
		layer = (java.util.List) layers.get(0);
		for (Iterator it = layer.iterator(); it.hasNext();) {
			v = (Vertex) it.next();
			v.label3 = true;
		}
		// Assign median positions
		// A comparator for assign prority
		Comparator comp = new Comparator() {
			public int compare(Object obj1, Object obj2) {
				Vertex v1 = (Vertex) obj1;
				Vertex v2 = (Vertex) obj2;
				int d1 = v1.getOutDegree();
				int d2 = v2.getOutDegree();
				return d2 - d1;
			}
		};
		java.util.List sortedLayer;
		for (int i = 1; i < layers.size(); i++) {
			layer = (java.util.List) layers.get(i);
			sortedLayer = new ArrayList(layer);
			// Sort layer2 
			Collections.sort(sortedLayer, comp);
			assignMedian(layer, sortedLayer, false);
		}
		// A sorter based on the diff between in and out degree
		comp = new Comparator() {
			public int compare(Object obj1, Object obj2) {
				Vertex v1 = (Vertex) obj1;
				Vertex v2 = (Vertex) obj2;
				int d1 = v1.getInDegree() - v1.getOutDegree();
				int d2 = v2.getInDegree() - v2.getOutDegree();
				return d2 - d1;
			}
		};
		// Need to do another scan from top to down
		for (int i = layers.size() - 2; i >= 0; i --) {
			layer = (java.util.List) layers.get(i);
			sortedLayer = new ArrayList(layer);
			Collections.sort(sortedLayer, comp);
			// Assign median based on the first layer
			assignMedian(layer, sortedLayer, true);
		}
	}
	
	private void assignMedian(java.util.List layer2, java.util.List sortedLayer2, boolean isTopDown) {
		Vertex v;
		int x;
		java.util.List neighbors;
		for (int j = 0; j < sortedLayer2.size(); j++) {
			v = (Vertex) sortedLayer2.get(j);
			// Calculate the median position
			if (isTopDown)
				neighbors  = v.getInNeighbor();
			else
				neighbors = v.getOutNeighbor(); // Outneighbors have been sorted in reducing crossings.
			if (neighbors == null || neighbors.size() == 0)
				continue;
			int size = neighbors.size();
			int index = size / 2;
			int diff = 0;
			if (size % 2 == 0) { // Use two median average
				Vertex v1 = (Vertex) neighbors.get(index - 1);
				Vertex v2 = (Vertex) neighbors.get(index);
				x = (v1.pos.x + v2.pos.x) / 2;
				diff = x - v.pos.x;
			}
			else { // Use the median 
				Vertex v1 = (Vertex) neighbors.get(index);
				diff = v1.pos.x - v.pos.x;
			}
			if (diff > 0) {
				// Need to find the leftmost node whose position has been assigned.
				int leftMost = layer2.size();
				for (int k = v.label1 + 1; k < layer2.size(); k++) {
					Vertex v1 = (Vertex)layer2.get(k);
					if (v1.label3) {
						leftMost = k;
						break;
					}
				}
				if (leftMost == layer2.size()) { // No right bound
					for (int k = v.label1; k < layer2.size(); k++) {
						Vertex v1 = (Vertex)layer2.get(k);
						v1.pos.x += diff;
					}
				}
				else { // Find the maximum moving distance
					Vertex v1 = (Vertex)layer2.get(leftMost - 1);
					Vertex v2 = (Vertex)layer2.get(leftMost);
					// Calculate the empty distance between these two vertices
					int diff1 = v2.pos.x - v1.pos.x - v1.getSize().width / 2 - v2.getSize().width / 2 - nodeDist;
					if (diff1 < diff)
						diff = diff1;
					for (int k = v.label1; k < leftMost; k++) {
						v1 = (Vertex)layer2.get(k);
						v1.pos.x += diff;
					}
				}
				// Move the vertices left to v that are not handled in case 
				// some nodes will not be adjusted (i.e. sources)
				if (diff != 0) {
					for (int k = v.label1 - 1; k >= 0; k --) {
						Vertex v1 = (Vertex) layer2.get(k);
						if (v1.label3)
							break;
						v1.pos.x += diff;
					}
				}
			}
			else if (diff < 0) {
				// Need to find the rightmost node whose position has been assigned
				int rightMost = -1;
				for (int k = v.label1 - 1; k >= 0; k--) {
					Vertex v1 = (Vertex) layer2.get(k);
					if (v1.label3) {
						rightMost = k;
						break;
					}
				}
				if (rightMost == -1) { // No left bound
					for (int k = v.label1; k >= 0; k --) {
						Vertex v1 = (Vertex) layer2.get(k);
						v1.pos.x += diff;
					}
				}
				else { // Find the maximum moving distance
					Vertex v1 = (Vertex)layer2.get(rightMost + 1);
					Vertex v2 = (Vertex)layer2.get(rightMost);
					// Calculate the empty distance between these two vertices
					int diff1 = v2.pos.x - v1.pos.x + v1.getSize().width / 2 + v2.getSize().width / 2 + nodeDist;
					if (diff1 > diff)
						diff = diff1;
					if (diff != 0) {
						for (int k = v.label1; k < rightMost; k++) {
							v1 = (Vertex)layer2.get(k);
							v1.pos.x += diff;
						}
					}
				}
				if (diff != 0) {
					// Move the vertices right to v that are not handled in case 
					// some nodes will not be adjusted (i.e. sources)
					for (int k = v.label1 + 1; k < layer2.size(); k++) {
						Vertex v1 = (Vertex)layer2.get(k);
						if (v1.label3)
							break;
						v1.pos.x += diff;
					}
				}
			}
			v.label3 = true;
		}
	}
}
