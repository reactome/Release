/*
 * Created on Mar 26, 2004
 */
package org.gk.util;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The class used for layout a generic graph.
 * @author wugm
 */
public class GraphLayoutEngine {
	public static final int HIERARCHICAL_LAYOUT = 0;
	public static final int FORCE_DIRECTED_LAYOUT = 1;
	//public static final int CIRCULAR_LAYOUT = 2;
	public static final int MIN_X = 30;
	public static final int MIN_Y = 30;
	// To control layout graph
	private int layerDist = 80;
	private int nodeSep = 75;
	private int nodeWidth = 100;
	private int edgeLen = 150;
	// Layout program
	public static String dot = "/Users/wgm/ProgramFiles/graphiviz-1.13-v16/Graphviz.app/Contents/MacOS/dot";
	private String neato = "/Users/wgm/ProgramFiles/graphiviz-1.13-v16/Graphviz.app/Contents/MacOS/neato";
    //private String dot = "/usr/local/bin/dot";
    //private String neato = "/usr/local/bin/neato";
	//private String neato = "C:\\graphviz\\Graphviz\\bin\\neato.exe";
	//private String dot = "C:\\graphviz\\Graphviz\\bin\\dot.exe";
	//private String twopi = "C:\\graphviz\\Graphviz\\bin\\twopi.exe";

	private static GraphLayoutEngine engine = null;
	
	public GraphLayoutEngine() {
	}
	
	public static GraphLayoutEngine getEngine() {
		if (engine ==  null)
			engine = new GraphLayoutEngine();
		return engine;
	}
	
	public void setLayerDist(int layerDist) {
		this.layerDist = layerDist;
	}
	
	public int getLayerdDist() {
		return this.layerDist;
	}
	
	public void setNodeSep(int nodeSep) {
		this.nodeSep = nodeSep;
	}
	
	public int getNodeSep() {
		return this.nodeSep;
	}
	
	public void setEdgeLen(int edgeLen) {
		this.edgeLen = edgeLen;
	}
	
	public int getEdgeLen() {
		return this.edgeLen;
	}
	
	public void setNodeWidth(int width) {
	    this.nodeWidth = width;
	}
	
	public void layout(Graph graph, int type) {
		switch(type) {
			case HIERARCHICAL_LAYOUT :
				hierarchicalLayout(graph);
				break;
			case FORCE_DIRECTED_LAYOUT :
				forceDirectedLayout(graph);
				break;
//			case CIRCULAR_LAYOUT :
//				circularLayout(graph);
//				break;
		}
	}
	
	private void hierarchicalLayout(Graph graph) {
		// Check if dot installed
		File dotFile = new File(dot);
		if (!dotFile.exists()) {
		    new HierarchicalLayout().layout(graph);
			return;
		}
		try {
			attLayout(dot, graph);
		}
		catch(Exception e) {
			System.err.println("GraphLayoutEngine.hierachicalLayout(): " + e);
			e.printStackTrace();
			new HierarchicalLayout().layout(graph);
		}
	}
	
	private void validateOrigin(Graph graph) {
		Vertex v = null;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int x, y;
		for (Iterator it = graph.getVertices().iterator(); it.hasNext();) {
			v = (Vertex) it.next();
			x = v.pos.x - v.size.width / 2;
			if (x < minX)
				minX = x;
			y = v.pos.y - v.size.height / 2;
			if (y < minY)
				minY = y;
		}
		int dx = 0;
		int dy = 0;
		if (minX < MIN_X)
			dx = MIN_X - minX;
		if (minY < MIN_Y)
			dy = MIN_Y - minY;
		// Move all vertexes
		if (dx > 0 || dy > 0) {
			for (Iterator it = graph.getVertices().iterator(); it.hasNext();) {
				v = (Vertex) it.next();
				v.getPosition().translate(dx, dy);
			}
		}
	}
	
	/**
	 * Convert a Graph to graphviz dot file.
	 * @param graph the specifed graph to be converted
	 * @param nodeMap the map information: keys are node names in the dot
	 * values are vertex names
	 * @return A String object containing converted dot information.
	 */
	public String convertToDotGraph(Graph graph, Map nodeMap) {
		int res;
		try {	
			res = Toolkit.getDefaultToolkit().getScreenResolution();
		} catch (Exception e) {
			res = 72;
		}
		double nodeSep = (double)(nodeWidth + this.nodeSep)/ res;
		double layerSep = (double) this.layerDist / res;
		double edgeLen = (double) this.edgeLen / res;
		// Convert to dot-recognizable graph
		StringBuffer buffer = new StringBuffer();
		buffer.append("digraph test {");
		buffer.append("\n");
		// The following three properties are for dot
		buffer.append("\t nodesep=\"" + nodeSep + "\";\n"); 
		buffer.append("\t ranksep=\"" + layerSep + " equally\";\n");
		buffer.append("\t node [shape=point];\n");
		// The following property is for neato
		buffer.append("\t edge [len=\"" + edgeLen + "\"];\n");
		Vertex v = null;
		//double ratio = 0.1;
		for (Iterator it = graph.getVertices().iterator(); it.hasNext();) {
			v = (Vertex) it.next();
			buffer.append("\t");
			String name = v.getName();
			String key = "\"" + v.getName() + "\""; // Escape space
			nodeMap.put(key, v);
			buffer.append(key);
			buffer.append(";\n");
		}
		if (graph.getEdges() != null) {
			Edge edge = null;
			for (Iterator it = graph.getEdges().iterator(); it.hasNext();) {
				edge = (Edge)it.next();
				buffer.append("\t");
				if (edge.tail != null && edge.head != null) {
					buffer.append("\"" + edge.head.getName() + "\" -> \"" + edge.tail.getName() + "\"");
					buffer.append(";\n");
				}
			}
		}
		buffer.append("}\n");
		//System.out.println(buffer);
		return buffer.toString();
	}
	
	private void extractDotGraph(BufferedReader reader, Map nodeMap) throws IOException {
	    String line = null;
	    String nameEnd = "[pos=";
	    int index = 0;
	    int index1;
	    int index2;
	    Vertex v = null;
        String wholeLine = null;
	    while ((line = reader.readLine()) != null) {
            if (line.startsWith("digraph"))
                continue; // 
            if (line.startsWith("}"))
                break; // Last line
	        if (line.endsWith("\\")) {// Partial line. Should be merged with the previous one.
                                     // The line ends with ";"
	            if (wholeLine == null)
                    wholeLine = line.substring(0, line.length() - 1); // First line
                else
                    wholeLine += line.substring(0, line.length() - 1); // Remove "\"
                continue; // Continue getting lines
            }
            if (line.endsWith(";")) {
                if (wholeLine != null)
                    wholeLine += line;
                else
                    wholeLine = line;
            }
            index = wholeLine.indexOf(nameEnd);
	        if (index == -1) {
	            wholeLine = null; // Treated as parsed
                continue;
            }
	        String name = wholeLine.substring(0, index).trim();
	        if (!name.startsWith("\"")) // Quotation might be rip off.
	            name = "\"" + name + "\"";
	        v = (Vertex) nodeMap.get(name);
	        if (v == null)
	            break; // Done
	        index1 = wholeLine.indexOf("\"", index);
	        index2 = wholeLine.indexOf("\"", index1 + 1);
	        String posStr = wholeLine.substring(index1 + 1, index2);
	        index = posStr.indexOf(",");
	        int x = Integer.parseInt(posStr.substring(0, index));
	        int y = Integer.parseInt(posStr.substring(index + 1));
	        v.getPosition().setLocation(x, y);
            wholeLine = null; // Reset whole line.
	    }
	}
	
	private void forceDirectedLayout(Graph graph) {
		// Check if dot installed
		File neatoFile = new File(neato);
		if (!neatoFile.exists()) {
			throw new IllegalStateException("GraphLayoutEngine.fireceDirectedLayout(): no neato.exe specified.");
		}
		try {
			attLayout(neato, graph);
		}
		catch(Exception e) {
			System.err.println("GraphLayoutEngine.forceDirectedLayout(): " + e);
			e.printStackTrace();
		}
	}
	
//	private void circularLayout(Graph graph) {
//		// Check if circular layout exits
//		File circoFile = new File(twopi);
//		if (!circoFile.exists()) {
//			throw new IllegalStateException("GraphLayoutEngine.circularLayout(): no circo.exe specified.");
//		}
//		try {
//			attLayout(twopi, graph);
//		}
//		catch(Exception e) {
//			System.err.println("GraphLayoutEngine.circularLayout(): " + e);
//			e.printStackTrace();
//		}
//	}
	
	private void attLayout(String exeName, Graph graph) throws Exception {
		Process process = Runtime.getRuntime().exec(exeName);
		OutputStream os = process.getOutputStream();
		Map nodeMap = new HashMap();
		String dotGraph = convertToDotGraph(graph, nodeMap);
		os.write(dotGraph.getBytes());
		os.flush();
		os.close();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		extractDotGraph(reader, nodeMap);
		reader.close();
		process.destroy();
		// Manually move the top and left side because of the point shape used for layout.
		validateOrigin(graph);
	}

}
