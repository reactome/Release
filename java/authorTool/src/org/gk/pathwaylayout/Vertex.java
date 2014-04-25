/*
 * Created on Mar 31, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.gk.pathwaylayout;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.tree.MutableTreeNode;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.Port;

/**
 * @author vastrik
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Vertex extends DefaultGraphCell {
	
	public int dx;
	public int dy;
	private boolean isFixed = false;
	private boolean inFocus = false;
	private String wrappedLabel = null;
	public GKInstance storageInstance;
	
	/**
	 * 
	 */
	public Vertex() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 */
	public Vertex(Object arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public Vertex(Object arg0, AttributeMap arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 */
	public Vertex(Object arg0, AttributeMap arg1, MutableTreeNode[] arg2) {
		super(arg0, arg1, arg2);
		// TODO Auto-generated constructor stub
	}

	public String toString1() {
		try {
			GKInstance instance = (GKInstance)getUserObject();
			return "<html>" + Utils.findShortestName(instance) + "</html>";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.toString();
	}
	
	public String toString() {
		if (wrappedLabel == null) {
			try {
				//GKInstance instance = (GKInstance)getUserObject();
				//return "<html>" + Utils.findShortestName(instance) + "</html>";
				//wrappedLabel = "<html><center>" + wrapLabel(Utils.findShortestName((GKInstance)getUserObject())) + "</center></html>";
				//wrappedLabel = "<html><center>" + wrapLabel((String)((GKInstance)getUserObject()).getAttributeValue(ReactomeJavaConstants.shortName)) + "</center></html>";
				String name = Utils.findShortestName((GKInstance)getUserObject());
				if (name.length() > 60) {
					name = name.substring(0, 58).concat("...");
				}
				wrappedLabel = "<html><center>" + wrapLabel(name) + "</center></html>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				wrappedLabel = super.toString();
			}
		}
		return wrappedLabel;
	}

    public static String wrapLabel(String s) {
    	int l = s.length();
    	int cpr;
    	if (l <= 10) {
    		cpr = l;
    	} else if (l <= 20) {
    		cpr = (int)Math.ceil(l / 2.0);
    	} else if (l <= 60) {
    		cpr = (int)Math.ceil(l / 3.0);
    	} else {
    		int h = 21;
    		cpr = 22;
    		for (int i = 22; i >= 18; i--) {
    			if (l % i == 0) {
    				cpr = i;
    				continue;
    			} else {
    				int j = i - (l % i);
    				if (j < h) {
    					h = j;
    					cpr = i;
    				}
    			}
    		}
    	}
    	StringBuffer out = new StringBuffer(s.substring(0,cpr));
    	for (int i = cpr; i < l; i += cpr) {
    		out.append("<BR>");
    		out.append(s.substring(i, Math.min(i + cpr, l)));
    	}
    	return out.toString();
    }
	
    public String wrapLabel2() throws Exception {
    	GKInstance instance = (GKInstance)getUserObject();
    	String s = Utils.findShortestName(instance);
        StringTokenizer st = new StringTokenizer(s, " ", true);
        StringBuffer buf = new StringBuffer();
        String line= "";
        String token = "";
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if ((token.length() + line.length()) < 20) {
            	if ((token.equals(" ")) && (line.length() == 0)) {
                } else {
                    line += token;
                }
            } else if (! token.equals(" ")) {
                if (buf.length() > 0) {
                    buf.append("<BR>");
                }
                if (line.length() > 0) {
                    buf.append(line);
                }
                line = token;
            }
        }
        if (line.length() > 0) {
            if (buf.length() > 0) {
                buf.append("<BR>");
            }
            buf.append(line);
        }
        return buf.toString();
    }

	
	/**
	 * @return
	 */
	public Rectangle getBounds() {
		return GraphConstants.getBounds(getAttributes()).getBounds();
	}
	/**
	 * @return Returns the isFixed.
	 */
	public boolean isFixed() {
		return isFixed;
	}
	/**
	 * @param isFixed The isFixed to set.
	 */
	public void setFixed(boolean isFixed) {
		this.isFixed = isFixed;
	}
	/**
	 * @return Returns the inFocus.
	 */
	public boolean isInFocus() {
		return inFocus;
	}
	/**
	 * @param inFocus The inFocus to set.
	 */
	public void setInFocus(boolean inFocus) {
		this.inFocus = inFocus;
	}

    public int getX () {
    	//System.err.println(getBounds() + "\t" + getBounds().getX() + "\t" + (int) getBounds().getX());
    	//return (int) getBounds().getX();
    	return (int) GraphConstants.getBounds(getAttributes()).getX();
    }
    
    public int getY () {
    	//return (int) getBounds().getY();
        return (int) GraphConstants.getBounds(getAttributes()).getY();
    }

	public void translate(int x, int y) {
		//System.out.println(this + " translate: " + x + " ," + y);
		getAttributes().translate(x, y);
	}
	
	public Iterator edges() {
		Port p = (Port)getChildAt(0);
		return p.edges();
	}
	
	public Set<Edge> getEdges() {
		DefaultPort p = (DefaultPort)getChildAt(0);
		return (Set<Edge>)p.getEdges();
	}
	
	public List<Edge> outputEdges() {
		List<Edge> edges = new ArrayList<Edge>();
		for (Object o : getEdges()) {
			Edge e = (Edge)o;
			if (e.getType() == PathwayLayoutConstants.OUTPUT_EDGE) {
				edges.add(e);
			}
		}
		return edges;
	}
	
	public int getOutputEdgeCount() {
		return outputEdges().size();
	}
	
	public boolean isEntityVertex() {
		return (((GKInstance)getUserObject()).getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) ? true : false;
	}
	
	public Set<Vertex> getConnectedNodes() {
		Set<Vertex> out = new HashSet<Vertex>();
		for (Edge e : getEdges()) {
			if (e.sourceVertex != this) {
				out.add(e.sourceVertex);
			} else {
				out.add(e.targetVertex);
			}
		}
		return out;
	}
	
	public double distanceFromCentreToBoundaryAtBearing(double phi) {
		Rectangle b = getBounds();
		double d = Utils.distanceFromCentreToBoundaryAtBearing(b.width, b.height, phi);
		//System.out.printf("%s\t%s\t%s\t%s\t%s\n", b.width, b.height, phi, d, getUserObject());
		return d;
	}
	
	public int area() {
		Rectangle b = getBounds();
		return b.width*b.height;
	}
	
	/*
	 * Calculate the distances between the centres of this nodes and given node.
	 */
	public double centreDistanceTo (Vertex v) {
		Rectangle r1 = getBounds();
		Rectangle r2 = v.getBounds();
		return Math.sqrt(Math.pow(r2.getCenterX() - r1.getCenterX(), 2) + Math.pow(r2.getCenterY() - r1.getCenterY(), 2));
	}
	
	public double centreDistanceTo (Point.Double p) {
		Rectangle r1 = getBounds();
		return Math.sqrt(Math.pow(p.x - r1.getCenterX(), 2) + Math.pow(p.y - r1.getCenterY(), 2));
	}
	
	public static void main(String[] args) {
		String s = wrapLabel("fibrin multimer");
		System.out.println(s);
	}

	/* 
	 * Returns true if the entity has a catalyst edge. The entity may also have other
	 * roles which are not considered by this method.
	 */
	public boolean isCatalyst() {
		for (Edge e : getEdges()) {
			if (e.getType() == PathwayLayoutConstants.CATALYST_EDGE) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isCatalystOnly() {
		Set<Edge> edges = getEdges();
		if ((edges == null) || (edges.isEmpty())) return false;
		for (Edge e : getEdges()) {
			if (e.getType() != PathwayLayoutConstants.CATALYST_EDGE) {
				return false;
			}
		}
		return true;
	}
	
	public void removeEdge(Edge e) {
		Port p = (Port)getChildAt(0);
		p.removeEdge(e);
	}
	
	public List<Edge> removeDuplicateEdges () {
		Map<Vertex, Set<Integer>> m = new HashMap<Vertex, Set<Integer>>();
		List<Edge> removed = new ArrayList<Edge>();
		for (Edge e : getEdges()) {
			Vertex v;
			if (e.getSourceVertex() == this) {
				v = e.getTargetVertex();
			} else {
				v = e.getSourceVertex();
			}
			Integer type = new Integer(e.getType());
			Set<Integer> types = m.get(v);
			if (types == null) {
				types = new HashSet<Integer>();
				types.add(type);
				m.put(v, types);
			} else {
				if (types.contains(type)) {
					removeEdge(e);
					removed.add(e);
				} else {
					types.add(type);
				}
			}
		}
		return removed;
	}
}
