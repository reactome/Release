/*
 * GraphHyperEdge.java
 *
 * Created on June 23, 2003, 3:28 PM
 */

package org.gk.render;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A Renderable for connecting Nodes.
 * @author  wgm
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class HyperEdge extends Renderable {
    public static final int NONE = 0;
    public static final int INPUT = 1;
    public static final int OUTPUT = 2;
    public static final int CATALYST = 3;
    public static final int INHIBITOR = 4;
    public static final int ACTIVATOR = 5;
    public static final int BACKBONE = 10;
    // backbone is the central line that input or output can be attached. backbonesPoints shouldn't be null if
    // the reaction needed to be displayed.
    protected java.util.List<Point> backbonePoints;
    // input is a list of lists. inputPoints should be null when there is no or one input.
    private java.util.List<List<Point>> inputPoints;
    // output is another list of lists. outputPoints should be null when there is no or one output.
    private java.util.List<List<Point>> outputPoints;
    // helper is similar to output. Helper should be attached to position
    private java.util.List<List<Point>> helperPoints;
    protected java.util.List<List<Point>> inhibitorPoints;
    protected java.util.List<List<Point>> activatorPoints;
    private boolean needInputArrow;
    private boolean needOutputArrow;
    // For selection
    protected HyperEdgeSelectionInfo selectionInfo;
    /** Creates a new instance of ReactionRenderHelper */
    public HyperEdge() {
        // Make sure the backbone points it not null.
        backbonePoints = new ArrayList(3);
        for (int i = 0; i < 3; i++)
            backbonePoints.add(new Point());
        // Have to specify a position in the backbone list
        position = (Point) backbonePoints.get(1);
        selectionInfo = new HyperEdgeSelectionInfo();
        setConnectInfo(new HyperEdgeConnectInfo());
        bounds = new Rectangle();
        isTransferrable = false;
    }
    
    public void setPosition(Point pos) {
        if (pos == null)
            return;
        position.x = pos.x;
        position.y = pos.y;
        needCheckBounds = true;
    }
    
    /**
     * Layout this HyperEdge based on the terminal points.
     */
    public void layout() {
    	Renderable node = null;
    	java.util.List inputTerms = new ArrayList();
    	java.util.List inputNodes = getInputNodes();
    	if (inputPoints == null || inputPoints.size() == 0) {
    		if (inputNodes.size() == 0)
    			inputTerms.add(getInputHub());
    		else {
    			node = (Renderable)inputNodes.get(0);
    			inputTerms.add(node.getPosition());
    		}
    	}
    	else {
    		for (int i = 0; i < inputPoints.size(); i++) {
    			if (i < inputNodes.size()) {
    				node = (Renderable) inputNodes.get(i);
    				inputTerms.add(node.getPosition());
    			}
    			else {
    				java.util.List list = (java.util.List) inputPoints.get(i);
    				inputTerms.add(list.get(0));
    			}
    		}
    	}
    	java.util.List outputTerms = new ArrayList();
    	java.util.List outputNodes = getOutputNodes();
    	if (outputPoints == null || outputPoints.size() == 0) {
    		if (outputNodes.size() == 0) 
    			outputTerms.add(getOutputHub());
    		else {
    			node = (Renderable) outputNodes.get(0);
    			outputTerms.add(node.getPosition());
    		}
    	}
    	else {
    		for (int i = 0; i < outputPoints.size(); i++) {
    			if (i < outputNodes.size()) {
    				node = (Renderable) outputNodes.get(i);
    				outputTerms.add(node.getPosition());
    			}
    			else {
    				java.util.List list = (java.util.List) outputPoints.get(i);
    				outputTerms.add(list.get(0));
    			}
    		}
    	}
    	// These four situations can be used for layouting.
    	// All inputs are above the outputs
    	boolean isOK = true;
    	for (Iterator it = inputTerms.iterator(); it.hasNext() && isOK;) {
    		Point inputTerm = (Point) it.next();
    		for (Iterator it1 = outputTerms.iterator(); it1.hasNext();) {
    			Point outputTerm = (Point) it1.next();
    			if (inputTerm.y >= outputTerm.y) {
    				isOK = false;
    				break;
    			}
    		}
    	}
    	// Do input up, output down alignment
    	if (isOK) {
    		int x1 = 0; 
    		int y1 = Integer.MIN_VALUE;
    		for (Iterator it = inputTerms.iterator(); it.hasNext();) {
    			Point p = (Point) it.next();
    			x1 += p.x;
    			if (y1 < p.y)
    				y1 = p.y;
    		}
    		x1 /= inputTerms.size();
    		int x2 = 0;
    		int y2 = Integer.MAX_VALUE;
    		for (Iterator it = outputTerms.iterator(); it.hasNext();) {
    			Point p = (Point) it.next();
    			x2 += p.x;
    			if (y2 > p.y)
    				y2 = p.y;
    		}
    		x2 /= outputTerms.size();
    		// Align the backbone points.
    		double buffer = 1 / 6.0; // for spaces in the two terminals.
    		int size = backbonePoints.size();
    		double step = (1 - 2 * buffer) / (size - 1);
    		double factor = buffer;
    		int x = (x1 + x2) / 2;
    		int diffY = y2 - y1;
    		for (int i = 0; i < backbonePoints.size(); i++) {
    			Point p = (Point) backbonePoints.get(i);
    			p.x = x;
    			p.y = (int)(y1 + diffY * factor);
    			factor += step;
    		}
			validatePointsForLayout();
    		invalidateConnectWidgets();
    		needCheckBounds = true;
    		return;
    	}
    	// All outputs are above all outputs
    	isOK = true;
    	for (Iterator it = inputTerms.iterator(); it.hasNext() && isOK;) {
    		Point inputTerm = (Point) it.next();
    		for (Iterator it1 = outputTerms.iterator(); it1.hasNext();) {
    			Point outputTerm = (Point) it1.next();
    			if (outputTerm.y >= inputTerm.y) {
    				isOK = false;
    				break;
    			}
    		}
    	}
    	// Do input down, output up layout
    	if (isOK) {
			int x1 = 0; 
			int y1 = Integer.MAX_VALUE;
			for (Iterator it = inputTerms.iterator(); it.hasNext();) {
				Point p = (Point) it.next();
				x1 += p.x;
				if (y1 > p.y)
					y1 = p.y;
			}
			x1 /= inputTerms.size();
			int x2 = 0;
			int y2 = Integer.MIN_VALUE;
			for (Iterator it = outputTerms.iterator(); it.hasNext();) {
				Point p = (Point) it.next();
				x2 += p.x;
				if (y2 < p.y)
					y2 = p.y;
			}
			x2 /= outputTerms.size();
			// Align the backbone points.
			double buffer = 1 / 6.0; // for spaces in the two terminals.
			int size = backbonePoints.size();
			double step = (1 - 2 * buffer) / (size - 1);
			double factor = buffer;
			int x = (x1 + x2) / 2;
			int diffY = y2 - y1;
			for (int i = 0; i < backbonePoints.size(); i++) {
				Point p = (Point) backbonePoints.get(i);
				p.x = x;
				p.y = (int)(y1 + diffY * factor);
				factor += step;
			}
			validatePointsForLayout();
			invalidateConnectWidgets();
			needCheckBounds = true;
			return;
    	}
    	// All inputs are left to all outputs
    	isOK = true;
    	for (Iterator it = inputTerms.iterator(); it.hasNext() && isOK;) {
    		Point inputTerm = (Point) it.next();
    		for (Iterator it1 = outputTerms.iterator(); it1.hasNext();) {
    			Point outputTerm = (Point) it1.next();
    			if (outputTerm.x <= inputTerm.x) {
    				isOK = false;
    				break;
    			}
    		}
    	}
    	if (isOK) {
    		int x1 = Integer.MIN_VALUE;
    		int y1 = 0;
    		for (Iterator it = inputTerms.iterator(); it.hasNext();) {
    			Point p = (Point) it.next();
    			if (p.x > x1)
    				x1 = p.x;
    			y1 += p.y;
    		}
    		y1 /= inputTerms.size();
    		int x2 = Integer.MAX_VALUE;
    		int y2 = 0;
    		for (Iterator it = outputTerms.iterator(); it.hasNext();) {
    			Point p = (Point) it.next();
    			if (p.x < x2)
    				x2 = p.x;
    			y2 += p.y;
    		}
    		y2 /= outputTerms.size();
			// Align the backbone points.
			double buffer = 1 / 6.0; // for spaces in the two terminals.
			int size = backbonePoints.size();
			double step = (1 - 2 * buffer) / (size - 1);
			double factor = buffer;
			int y = (y1 + y2) / 2;
			int diffX = x2 - x1;
            //TODO Need to consider cases where only one input or one output
			for (int i = 0; i < backbonePoints.size(); i++) {
				Point p = (Point) backbonePoints.get(i);
				p.y = y;
				p.x = (int)(x1 + diffX * factor);
				factor += step;
			}
			validatePointsForLayout();
			invalidateConnectWidgets();    		
			needCheckBounds = true;
			return;
    	}
    	// All outputs are left to all inputs
    	isOK = true;
    	for (Iterator it = inputTerms.iterator(); it.hasNext() && isOK;) {
    		Point inputTerm = (Point) it.next();
    		for (Iterator it1 = outputTerms.iterator(); it1.hasNext();) {
    			Point outputTerm = (Point) it1.next();
    			if (outputTerm.x >= inputTerm.x) {
    				isOK = false;
    				break;
    			}
    		}
    	}
    	// Do outputs left and input right
    	if (isOK) {
			int x1 = Integer.MAX_VALUE;
			int y1 = 0;
			for (Iterator it = inputTerms.iterator(); it.hasNext();) {
				Point p = (Point) it.next();
				if (p.x < x1)
					x1 = p.x;
				y1 += p.y;
			}
			y1 /= inputTerms.size();
			int x2 = Integer.MIN_VALUE;
			int y2 = 0;
			for (Iterator it = outputTerms.iterator(); it.hasNext();) {
				Point p = (Point) it.next();
				if (p.x > x2)
					x2 = p.x;
				y2 += p.y;
			}
			y2 /= outputTerms.size();
			// Align the backbone points.
			double buffer = 1 / 6.0; // for spaces in the two terminals.
			int size = backbonePoints.size();
			double step = (1 - 2 * buffer) / (size - 1);
			double factor = buffer;
			int y = (y1 + y2) / 2;
			int diffX = x2 - x1;
			for (int i = 0; i < backbonePoints.size(); i++) {
				Point p = (Point) backbonePoints.get(i);
				p.y = y;
				p.x = (int)(x1 + diffX * factor);
				factor += step;
			}
			validatePointsForLayout();
			invalidateConnectWidgets();
			needCheckBounds = true;
			return;   		
    	}
    	// Just in case
    	Point p = (Point) inputTerms.get(0);
    	int x1 = p.x;
    	int y1 = p.y;
		p = (Point) outputTerms.get(0);
		int x2 = p.x;
		int y2 = p.y;
		// Align the backbone points.
		double buffer = 1 / 6.0; // for spaces in the two terminals.
		int size = backbonePoints.size();
		double step = (1 - 2 * buffer) / (size - 1);
		double factor = buffer;
		int y = (y1 + y2) / 2;
		int diffX = x2 - x1;
		for (int i = 0; i < backbonePoints.size(); i++) {
			p = (Point) backbonePoints.get(i);
			p.y = y;
			p.x = (int)(x1 + diffX * factor);
			factor += step;
		}
		validatePointsForLayout();
		invalidateConnectWidgets();
		needCheckBounds = true;    	
    }
    
    /**
     * Use this method to check if some points used in the lines should
     * be removed. If points should be removed, they will be removed in 
     * this method.
     * @return true for some points are removed false for no points removed.
     */
    protected boolean validatePointsForLayout() {
        boolean rtn = false;
        boolean needValidateControl = validatePoints(inputPoints);
        if (needValidateControl)
            validateWidgetControlPoints(INPUT);
        rtn |= needValidateControl;
        needValidateControl = validatePoints(outputPoints);
        if (needValidateControl)
            validateWidgetControlPoints(OUTPUT);
        rtn |= needValidateControl;
        needValidateControl = validatePoints(helperPoints);
        if (needValidateControl)
            validateWidgetControlPoints(CATALYST);
        rtn |= needValidateControl;
        needValidateControl = validatePoints(inhibitorPoints);
        if (needValidateControl)
            validateWidgetControlPoints(INHIBITOR);
        rtn |= needValidateControl;
        needValidateControl = validatePoints(activatorPoints);
        if (needValidateControl)
            validateWidgetControlPoints(ACTIVATOR);
        rtn |= needValidateControl;
        validatePosition();
        return rtn;
    }
    
    private boolean validatePoints(List pointsList) {
        boolean rtn = false;
        if (pointsList != null && pointsList.size() > 0) {
            for (Iterator it = pointsList.iterator(); it.hasNext();) {
                List points = (List) it.next();
                while (points.size() > 1) {
                    points.remove(1);
                    rtn = true;
                }
            }
        }
        return rtn;
    }
    
    /**
     * Automatically rearrange position of all input, output and catalyst positions.
     * @param pos
     */
    public void layout(Point pos) {
    	if (pos == null)
    		return;
    	position.x = pos.x;
    	position.y = pos.y;
    	// Reset the backbone positions
    	int w = 75;
    	Point p1 = (Point) backbonePoints.get(0); // Input hub
    	p1.x = pos.x - 100;
    	p1.y = pos.y;
    	backbonePoints.set(1, position);
    	Point p2 = (Point) backbonePoints.get(backbonePoints.size() - 1); // Output hub
    	p2.x = pos.x + 100;
    	p2.y = pos.y;
    	backbonePoints.set(2, p2);
    	// Remove all extra points
    	for (int i = 3; i < backbonePoints.size(); i++) 
    		backbonePoints.remove(3);
    	// Arrange inputBranches
    	if (inputPoints != null && inputPoints.size() > 0) {  	
    		int size = inputPoints.size();
    		double div = Math.PI / size;
    		double shift = div / 2;
    		int x, y;
  			for (int i = 0; i < size; i++) {
  				x = (int) (w * Math.sin((i + 1) * div - shift));
  				y = (int) (w * Math.cos((i + 1) * div - shift));
  				java.util.List list = (java.util.List) inputPoints.get(i);
   				Point p = (Point) list.get(0);
   				p.x = p1.x - x;
   				p.y = p1.y - y;
   				// Purge the points
   				for (int j = 1; j < list.size(); j++)
   					list.remove(1);
   			}
        }
        // Arrange outputBranches
        if (outputPoints != null && outputPoints.size() > 0) {
       		int size = outputPoints.size();
       		double div = Math.PI / size;
       		double shift = div / 2;
       		int x, y;
       		for (int i = 0; i < size; i++) {
       			x = (int) (w * Math.sin((i + 1) * div - shift));
       			y = (int) (w * Math.cos((i + 1) * div - shift));
       			java.util.List list = (java.util.List) outputPoints.get(i);
       			Point p = (Point) list.get(0);
       			p.x = p2.x + x;
       			p.y = p2.y - y;
       			// Purge the points
       			for (int j = 1; j < list.size(); j++)
       				list.remove(1);
       		}
        }
        // Arrange helperBranches, inhibitorBranches and activatorBranches
        java.util.List branches = new ArrayList();
        if (helperPoints != null)
        	branches.addAll(helperPoints);
        if (inhibitorPoints != null)
        	branches.addAll(inhibitorPoints);
        if (activatorPoints != null)
        	branches.addAll(activatorPoints);
        if (branches.size() > 0) {
        	int size = branches.size();
        	double div = 2 * Math.PI / size;
        	int x, y;
        	for (int i = 0; i < size; i++) {
        		x = (int) (w * Math.sin(i * div));
        		y = (int) (w * Math.cos(i * div));
        		java.util.List list = (java.util.List) branches.get(i);
        		Point p = (Point) list.get(0);
        		p.x = pos.x - x;
        		p.y = pos.y - y;
        		// Purge the points
        		for (int j = 1; j < list.size(); j++)
        			list.remove(1);
        	}
        }
     	// Check nodes that are connected
    	java.util.List widgets = connectInfo.getConnectWidgets();
    	if (widgets != null && widgets.size() > 0) {
    		for (Iterator it = widgets.iterator(); it.hasNext();) {
    			ConnectWidget widget = (ConnectWidget) it.next();
    			if (widget.getConnectedNode() == null)
    				continue;
    			Renderable node = widget.getConnectedNode();
    			java.util.List list = null;
    			Point p = null;
    			if (widget.getRole() == INPUT) {
    				if (inputPoints == null || inputPoints.size() == 0)
    					p = (Point) backbonePoints.get(0);
    				else {
    					list = (java.util.List) inputPoints.get(widget.getIndex());
    					p = (Point) list.get(0);
    				}
    			}
    			else if (widget.getRole() == OUTPUT) {
    				if (outputPoints == null || outputPoints.size() == 0)
    					p = (Point) backbonePoints.get(2);
    				else {
    					list = (java.util.List) outputPoints.get(widget.getIndex());
    					p = (Point) list.get(0);
    				}
    			}
    			else if (widget.getRole() == CATALYST) {
    				list = (java.util.List) helperPoints.get(widget.getIndex());
    				p = (Point) list.get(0);
    			}
    			else if (widget.getRole() == INHIBITOR) {
    				list = (java.util.List) inhibitorPoints.get(widget.getIndex());
    				p = (Point) list.get(0);
    			}
    			else if (widget.getRole() == ACTIVATOR) {
    				list = (java.util.List) activatorPoints.get(widget.getIndex());
    				p = (Point) list.get(0);
    			}
    			// Use move only. It is not right to set the position directly.
    			Point oldPos = node.getPosition();
    			if (oldPos == null)
    			    node.setPosition((Point)p.clone());
    			else {
    			    // Do move
    			    int dx = p.x - oldPos.x;
    			    int dy = p.y - oldPos.y;
    			    node.move(dx, dy);
    			}
    			widget.invalidate();
    		}
    	}
    	needCheckBounds = true;
    }
    
    public List<Node> getInputNodes() {
        java.util.List inputWidgets = ((HyperEdgeConnectInfo)connectInfo).getInputWidgets();
        return getNodesFromWidgets(inputWidgets);
    }
    
    private List<Node> getNodesFromWidgets(List widgets) {
        if (widgets == null)
            return new ArrayList<Node>();
        List<Node> list = new ArrayList<Node>(widgets.size());
        // Note: the following may not correct. Have to check if index is important for widget.
        //for (int i = 0; i < widgets.size(); i++)
        //    list.add(null);
        if (widgets != null) {
            ConnectWidget widget = null;
            for (Iterator it = widgets.iterator(); it.hasNext();) {
                widget = (ConnectWidget) it.next();
                //list.set(widget.getIndex(), widget.getConnectedNode());
                list.add(widget.getConnectedNode());
            }
        }
        return list;
    }
    
    public Node getInputNode(int index) {
    	java.util.List inputWidgets = ((HyperEdgeConnectInfo)connectInfo).getInputWidgets();
    	return checkWidgetForNode(inputWidgets, index);
    }
    
    public List<Node> getOutputNodes() {
    	java.util.List widgets = ((HyperEdgeConnectInfo)connectInfo).getOutputWidgets();
    	return getNodesFromWidgets(widgets);
    }
    
    public Node getOutputNode(int index) {
		java.util.List widgets = ((HyperEdgeConnectInfo)connectInfo).getOutputWidgets();
		return checkWidgetForNode(widgets, index);
    }
    
    public List<Node> getHelperNodes() {
    	java.util.List widgets = ((HyperEdgeConnectInfo)connectInfo).getHelperWidgets();
    	return getNodesFromWidgets(widgets);
    }
    
    public List<Node> getInhibitorNodes() {
    	java.util.List widgets = ((HyperEdgeConnectInfo)connectInfo).getInhibitorWidgets();
    	return getNodesFromWidgets(widgets);
    }
    
    public List<Node> getActivatorNodes() {
    	java.util.List widgets = ((HyperEdgeConnectInfo)connectInfo).getActivatorWidgets();
    	return getNodesFromWidgets(widgets);
    }
    
    public Renderable getHelperNode(int index) {
		java.util.List widgets = ((HyperEdgeConnectInfo)connectInfo).getHelperWidgets();
		return checkWidgetForNode(widgets, index);
    }
    
	public Renderable getInhibitorNode(int index) {
		java.util.List widgets = ((HyperEdgeConnectInfo)connectInfo).getInhibitorWidgets();
		return checkWidgetForNode(widgets, index);
	}
	
	private Node checkWidgetForNode(java.util.List widgets, int index) {
		if (widgets != null) {
			ConnectWidget widget = null;
			for (Iterator it = widgets.iterator(); it.hasNext();) {
				widget = (ConnectWidget)it.next();
				if (widget.getIndex() == index)
					return widget.getConnectedNode();
			}
		}
		return null;		
	}
	
	public Renderable getActivatorNode(int index) {
		java.util.List widgets = ((HyperEdgeConnectInfo)connectInfo).getActivatorWidgets();
		return checkWidgetForNode(widgets, index);
	}
    
    public Point getPosition() {
        return this.position;
    }
    
    public void setBackbonePoints(java.util.List<Point> points) {
        // Find the position point
        boolean found = false;
        for (Iterator it = points.iterator(); it.hasNext();) {
        	Point p = (Point) it.next();
        	if (p.equals(position)) {
        		position = p;
        		found = true;
        		break;
        	}
        }
        if (!found) 
        	throw new IllegalArgumentException("HyperEdge.setBackbonePoints(): backbone points should contain the position point: " + 
                                                getDisplayName() + " (" + getID() + ")");
    	else
    		this.backbonePoints = points;
    }
    
    public java.util.List<Point> getBackbonePoints() {
        return this.backbonePoints;
    }
    
    public void addBackbonePoint(Point p, int index) {
        if (backbonePoints == null)
            backbonePoints = new ArrayList();
        backbonePoints.add(index, p);
    }
    
    public void removeBackbonePoint(Point p) {
        if (backbonePoints != null) {
            backbonePoints.remove(p);
            validateWidgetControlPoints();
        }
    }
    
    public void setInputHub(Point p) {
        // Set the first backbone point
        Point inputHub = (Point) backbonePoints.get(0);
        inputHub.x = p.x;
        inputHub.y = p.y;
    }
    
    public void setOutputHub(Point p) {
        // Set the last backbone point
        Point outputHub = (Point) backbonePoints.get(backbonePoints.size() - 1);
        outputHub.x = p.x;
        outputHub.y = p.y;
    }
    
    public Point getInputHub() {
        return (Point) backbonePoints.get(0);
    }
    
    public Point getOutputHub() {
        return (Point) backbonePoints.get(backbonePoints.size() - 1);
    }
    
    /** Getter for property helperPoints.
     * @return Value of property helperPoints, which should be a list of list of Points.
     */
    public java.util.List<List<Point>> getHelperPoints() {
        return helperPoints;
    }
    
    /** Setter for property helperPoints.
     * @param helperPoints New value of property helperPoints, which should be a list of list of Points.
     */
    public void setHelperPoints(java.util.List helperPoints) {
        this.helperPoints = helperPoints;
    }
    
    /**
     * Add a helper point to the helperPoint List.
     * @param p the Point to be added
     * @param helperIndex the index of helper
     * @param pointIndex the point index.
     */
    public void addHelperPoint(Point p, int helperIndex, int pointIndex) {
        if (helperPoints == null)
            return;
        if (helperIndex < 0 || helperIndex >= helperPoints.size())
            return;
        java.util.List list = (java.util.List) helperPoints.get(helperIndex);
        list.add(pointIndex, p);
    }
    
    /**
     * Remove a helper point in a specified helper branch.
     */
    public void removeHelperPoint(Point p, int helperIndex) {
        if (helperPoints == null || helperIndex < 0 || helperIndex >= helperPoints.size())
            return;
        java.util.List list = (java.util.List) helperPoints.get(helperIndex);
        list.remove(p);
        if (list.size() == 0) // If nothing in the list, remove it.
            helperPoints.remove(helperIndex);
    }
    
    /** Getter for property inputPoints.
     * @return Value of property inputPoints.
     */
    public java.util.List<List<Point>> getInputPoints() {
        return inputPoints;
    }
    
    /** Setter for property inputPoints.
     * @param inputPoints New value of property inputPoints.
     */
    public void setInputPoints(java.util.List<List<Point>> inputPoints) {
        this.inputPoints = inputPoints;
    }
    
    /**
     * Add a point to a specified input branch at the specified index.
     */
    public void addInputPoint(Point p, 
                              int inputIndex, 
                              int pointIndex) {
        if (inputPoints == null || inputIndex < 0 || inputIndex >= inputPoints.size())
            return;
        java.util.List list = (java.util.List)inputPoints.get(inputIndex);
        list.add(pointIndex, p);
    }
    
    /** Getter for property outputPoints.
     * @return Value of property outputPoints.
     */
    public java.util.List<List<Point>> getOutputPoints() {
        return outputPoints;
    }
    
    /** Setter for property outputPoints.
     * @param outputPoints New value of property outputPoints.
     */
    public void setOutputPoints(java.util.List outputPoints) {
        this.outputPoints = outputPoints;
    }
    
    public void addOutputPoint(Point p, int outputIndex, int pointIndex) {
        if (outputPoints == null || outputIndex < 0 || outputIndex > outputPoints.size())
            return;
        java.util.List list = (java.util.List) outputPoints.get(outputIndex);
        list.add(pointIndex, p);
    }
    
    /** Getter for property needInputArrow.
     * @return Value of property needInputArrow.
     */
    public boolean isNeedInputArrow() {
        return needInputArrow;
    }
    
    /** Setter for property needInputArrow.
     * @param needInputArrow New value of property needInputArrow.
     */
    public void setNeedInputArrow(boolean needInputArrow) {
        this.needInputArrow = needInputArrow;
    }
    
    /** Getter for property needOutputArrow.
     * @return Value of property needOutputArrow.
     */
    public boolean isNeedOutputArrow() {
        return needOutputArrow;
    }
    
    /** Setter for property needOutputArrow.
     * @param needOutputArrow New value of property needOutputArrow.
     */
    public void setNeedOutputArrow(boolean needOutputArrow) {
        this.needOutputArrow = needOutputArrow;
    }
    
    public void move(int dx, int dy) {
        // If there is one point is selected, just move that point
        if (selectionInfo.selectPoint != null) {
            selectionInfo.selectPoint.x += dx;
            selectionInfo.selectPoint.y += dy;
            // Make positive
            if (selectionInfo.selectPoint.x < pad)
            	selectionInfo.selectPoint.x = pad;
            if (selectionInfo.selectPoint.y < pad)
            	selectionInfo.selectPoint.y = pad;
        }
        // Otherwise move all points
        else {
            java.util.List points = getAllPoints();
            Point p = null;
            for (Iterator it = points.iterator(); it.hasNext();) {
                p = (Point) it.next();
                p.x += dx;
                p.y += dy;
                // Make positive
                if (p.x < pad)
                	p.x = pad;
                if (p.y < pad)
                	p.y = pad;
            }
        }
        needCheckBounds = true;
        // Invalidate all ConnectWidgets
        connectInfo.invalidate();
    }
    
    private boolean isBranchPointPicked(java.util.List branches, 
                                        int direction, 
                                        Point p) {
        java.util.List branch = null;
        int index = 0;
        double distSq;
        Point p1;
        Point hub = null;
        switch (direction) {
            case INPUT :
                hub = getInputHub();
                break;
            case OUTPUT :
                hub = getOutputHub();
                break;
            case CATALYST :
                hub = getPosition();
                break;
            case INHIBITOR :
            	hub = getPosition();
            	break;
            case ACTIVATOR :
            	hub = getPosition();
            	break;
        }
        for (Iterator it = branches.iterator(); it.hasNext();) {
            branch = (java.util.List) it.next();
            if (branch == null || branch.size() == 0)
                continue;
            p1 = (Point) branch.get(0);
            distSq = p1.distanceSq(p);
            if (distSq < SENSING_DISTANCE_SQ) {
                selectionInfo.selectPoint = p1;
                selectionInfo.selectedBranch = index;
                selectionInfo.selectedType = direction;
                if (branch.size() == 1)
                    selectionInfo.connectWidget = new ConnectWidget(selectionInfo.selectPoint, hub, direction, index);
                else
                    selectionInfo.connectWidget = new ConnectWidget(selectionInfo.selectPoint, (Point) branch.get(1), direction, index);
                return true;
            }
            for (int i = 1; i < branch.size(); i++) {
                p1 = (Point) branch.get(i);
                distSq = p1.distanceSq(p);
                if (distSq < SENSING_DISTANCE_SQ) {
                    selectionInfo.selectPoint = p1;
                    selectionInfo.selectedBranch = index;
                    selectionInfo.selectedType = direction;
                    return true;
                }
            }
            index ++;
        }
        return false;
    }
    
    protected boolean isBackbonePicked(Point p) {
        Point prevP, nextP;
        prevP = (Point) backbonePoints.get(0);
        double distSq;
        for (int i = 1; i < backbonePoints.size(); i++) {
            nextP = (Point) backbonePoints.get(i);
            distSq = Line2D.ptSegDistSq(prevP.x, prevP.y, nextP.x, nextP.y, p.x, p.y);
            if (distSq < SENSING_DISTANCE_SQ) {
                // When a FlowLine has nodes attached, insert points automatically.
                // Otherwise do nothing.
                selectionInfo.selectedType = BACKBONE;
                return true;
            }
            prevP = nextP;
        }
        return false;
    }
    
    protected void validateWidgetControlPoints(int type) {
        List widgets = connectInfo.getConnectWidgets();
        if (widgets == null || widgets.size() == 0)
            return;
        ConnectWidget widget = null;
        Renderable node = null;
        int index = 0;
        List branch = getBranchFromType(type);
        Point hub = getControlFromBackbone(type);
        for (Iterator it = widgets.iterator(); it.hasNext();) {
            widget = (ConnectWidget) it.next();
            index = widget.getIndex();
            int role = widget.getRole();
            Point controlP = null;
            if (role != type)
                continue;
            if (branch == null || branch.size() == 0)
                controlP = hub;
            else {
                List points = (List) branch.get(index);
                if (points.size() > 1)
                    controlP = (Point) points.get(1);
                else {
                    controlP = hub;
                }
            }
            widget.setControlPoint(controlP);
        }
    }
    
    public List getBranchFromType(int type) {
        switch (type) {
            case INPUT :
                return inputPoints;
            case OUTPUT :
                return outputPoints;
            case CATALYST :
                return helperPoints;
            case INHIBITOR :
                return inhibitorPoints;
            case ACTIVATOR :
                return activatorPoints;
            case BACKBONE :
                return backbonePoints;
        }
        return null;
    }
    
    private Point getControlFromBackbone(int type) {
        switch (type) {
            case INPUT :
                if (inputPoints != null && inputPoints.size() > 0)
                    return (Point) backbonePoints.get(0);
                else
                    return (Point) backbonePoints.get(1);
            case OUTPUT :
                if (outputPoints != null && outputPoints.size() > 0)
                    return (Point) backbonePoints.get(backbonePoints.size() - 1);
                else
                    return (Point) backbonePoints.get(backbonePoints.size() - 2);
            case INHIBITOR : case ACTIVATOR : case CATALYST :
                return getPosition();
        }
        return null;
    }
    
    private boolean isBranchPicked(java.util.List branches, 
                                   Point hub,
                                   Point p,
                                   int type) {
        java.util.List branch = null;
        Point prevP, nextP = null;
        double distSq;
        int index = 0;
        for (Iterator it = branches.iterator(); it.hasNext();) {
            branch = (java.util.List) it.next();
            if (branch == null || branch.size() == 0)
                continue;
            for (int i = 0; i < branch.size() - 1; i++) {
                prevP = (Point) branch.get(i);
                nextP = (Point) branch.get(i + 1);
                distSq = Line2D.ptSegDistSq(prevP.x,
                                            prevP.y, 
                                            nextP.x, 
                                            nextP.y, 
                                            p.x,
                                            p.y);
                if (distSq < SENSING_DISTANCE_SQ) {
                    selectionInfo.selectedBranch = index;
                    selectionInfo.selectedType = type;
                    return true;
                }
            }
            // In case there is only one point in the branch
            nextP = (Point) branch.get(branch.size() - 1);
            distSq = Line2D.ptSegDistSq(nextP.x, 
                                        nextP.y, 
                                        hub.x,
                                        hub.y, 
                                        p.x,
                                        p.y);
            if (distSq < SENSING_DISTANCE_SQ) {
                selectionInfo.selectedBranch = index;
                selectionInfo.selectedType = type;
                return true;
            }
            index ++;
        }
        return false;
    }
    
    public boolean isPicked(Point p) {
        double distSq;
        // Check all points
        Point p1;
        selectionInfo.reset();
        // If this edge has been selected, check if points is selected
        if (inputPoints != null && inputPoints.size() > 0) {
            if (isBranchPointPicked(inputPoints, INPUT, p))
                return true;
        }
        // Check backbone Points
        p1 = (Point) backbonePoints.get(0);
        distSq = p1.distanceSq(p);
        if (distSq < SENSING_DISTANCE_SQ) {
            selectionInfo.selectPoint = p1;
            selectionInfo.selectedType = BACKBONE;
            if (inputPoints == null || inputPoints.size() == 0)
                selectionInfo.connectWidget = new ConnectWidget(selectionInfo.selectPoint, (Point) backbonePoints.get(1), INPUT, 0);
            return true;
        }
        p1 = (Point) backbonePoints.get(backbonePoints.size() - 1);
        distSq = p1.distanceSq(p);
        if (distSq < SENSING_DISTANCE_SQ) {
            selectionInfo.selectPoint = p1;
            selectionInfo.selectedType = BACKBONE;
            if (outputPoints == null || outputPoints.size() == 0)
                selectionInfo.connectWidget = new ConnectWidget(selectionInfo.selectPoint, (Point)backbonePoints.get(backbonePoints.size() - 2),
                OUTPUT, 0);
            return true;
        }
        for (int i = 1; i < backbonePoints.size() - 1; i++) {
            p1 = (Point) backbonePoints.get(i);
            distSq = p1.distanceSq(p);
            if (distSq < SENSING_DISTANCE_SQ) {
                selectionInfo.selectPoint = p1;
                selectionInfo.selectedType = BACKBONE;
                return true;
            }
        }
        // Check output points
        if (outputPoints != null && outputPoints.size() > 0) {
            if (isBranchPointPicked(outputPoints, OUTPUT, p))
                return true;
        }
        // Check helper points
        if (helperPoints != null && helperPoints.size() > 0) {
            if (isBranchPointPicked(helperPoints, CATALYST, p))
                return true;
        }
        // Check inhibitor points
        if (inhibitorPoints != null && inhibitorPoints.size() > 0) {
            if (isBranchPointPicked(inhibitorPoints, INHIBITOR, p))
                return true;
        }
        // Check activator points
        if (activatorPoints != null && activatorPoints.size() > 0) {
            if (isBranchPointPicked(activatorPoints, ACTIVATOR, p))
                return true;
        }
        // Check line segments now
        // Check input branches
        if (inputPoints != null && inputPoints.size() > 0) {
            if (isBranchPicked(inputPoints, 
                               getInputHub(), 
                               p,
                               INPUT)) {
                return true;
            }
        }
        // Check backbone
        if (isBackbonePicked(p))
            return true;
        // Check output branches
        if (outputPoints != null && outputPoints.size() > 0) {
            if (isBranchPicked(outputPoints, 
                               getOutputHub(),
                               p,
                               OUTPUT)) {
                return true;
            }
        }
        // Check helper branches
        if (helperPoints != null && helperPoints.size() > 0)
            if (isBranchPicked(helperPoints, 
                               position, 
                               p,
                               CATALYST)) {
                return true;
            }
        // Check inhibitor branches
        if (inhibitorPoints != null && inhibitorPoints.size()> 0) {
            if (isBranchPicked(inhibitorPoints,
                               position,
                               p,
                               INHIBITOR)) {
                return true;
            }
        }
        // Check activator branches
        if (activatorPoints != null && activatorPoints.size() > 0) {
            if (isBranchPicked(activatorPoints, 
                               position, 
                               p,
                               ACTIVATOR)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * This method is used to check if a passed point can be used to pick up
     * this HyperEdge. This method is different from another method isPicked(Point).
     * The internal data structure will not be changed in this method. So a client
     * that should not make changes to the internal data structure should call this method.
     * For example, getToolTipText(MouseEvent)
     * @param p
     * @return
     */
    public boolean canBePicked(Point p) {
        Point p1;
        double distSq;
        // Check backbone points
        if(canPointBePicked(p))
            return true;
        // Check backbone
        Point prevP, nextP;
        for (int i = 0; i < backbonePoints.size() - 1; i++) {
            prevP = (Point) backbonePoints.get(i);
            nextP = (Point) backbonePoints.get(i + 1);
            distSq = Line2D.ptSegDistSq(prevP.x, prevP.y, nextP.x, nextP.y, p.x, p.y);
            if (distSq < SENSING_DISTANCE_SQ) {
                return true;
            }
        }
        // Check line segments now
        // Check input branches
        if (canBranchBePicked(inputPoints, 
                              getInputHub(), 
                              p)) 
            return true;
        // Check output branches
        if (canBranchBePicked(outputPoints,
                              getOutputHub(),
                              p))
            return true;
        // Check helper branches
        if (canBranchBePicked(helperPoints,
                              position,
                              p))
            return true;
        if (canBranchBePicked(inhibitorPoints,
                              position,
                              p))
            return true;
        // Check activator branches
        if (canBranchBePicked(activatorPoints,
                              position,
                              p))
            return true;
        return false;
    }

    private boolean canPointBePicked(Point p) {
        Point p1;
        double distSq;
        for (Iterator it = backbonePoints.iterator(); it.hasNext();) {
            p1 = (Point) it.next();
            distSq = p1.distanceSq(p);
            if (distSq < SENSING_DISTANCE_SQ) {
                return true;
            }
        }
        // Check input points
        if (canBranchPointsBePicked(inputPoints, p))
            return true;
        // Check output points
        if (canBranchPointsBePicked(outputPoints, p))
            return true;
        // Check helper points
        if (canBranchPointsBePicked(helperPoints, p))
            return true;
        // Check inhibitor points
        if (canBranchPointsBePicked(inhibitorPoints, p))
            return true;
        // Check activator points
        if (canBranchPointsBePicked(activatorPoints, p))
            return true;
        return false;
    }
    
    /**
     * A helper method to check if a Point in a branch can be picked up.
     * @param branches
     * @param p
     * @return
     */
    private boolean canBranchPointsBePicked(List branches, 
                                            Point p) {
        if (branches == null || branches.size() == 0)
            return false;
        java.util.List branch = null;
        Point p1 = null;
        double distSq;
        for (Iterator it = branches.iterator(); it.hasNext();) {
            branch = (java.util.List) it.next();
            // Just in case
            if (branch == null || branch.size() == 0)
                continue;
            for (int i = 0; i < branch.size(); i++) {
                p1 = (Point) branch.get(i);
                distSq = p1.distanceSq(p);
                if (distSq < SENSING_DISTANCE_SQ) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * A helper methods to check if a line segment in a branch can 
     * be picked up.
     * @param branches
     * @param p
     * @return
     */
    private boolean canBranchBePicked(List branches,
                                      Point hub,
                                      Point p) {
        if (branches == null || branches.size() == 0)
            return false;
        java.util.List branch = null;
        Point prevP, nextP = null;
        double distSq;
        for (Iterator it = branches.iterator(); it.hasNext();) {
            branch = (java.util.List) it.next();
            if (branch == null || branch.size() == 0)
                continue;
            for (int i = 0; i < branch.size() - 1; i++) {
                prevP = (Point) branch.get(i);
                nextP = (Point) branch.get(i + 1);
                distSq = Line2D.ptSegDistSq(prevP.x,
                                            prevP.y, 
                                            nextP.x, 
                                            nextP.y, 
                                            p.x,
                                            p.y);
                if (distSq < SENSING_DISTANCE_SQ) {
                    return true;
                }
            }
            // In case there is only one point in the branch
            nextP = (Point) branch.get(branch.size() - 1);
            distSq = Line2D.ptSegDistSq(nextP.x, 
                                        nextP.y, 
                                        hub.x,
                                        hub.y, 
                                        p.x,
                                        p.y);
            if (distSq < SENSING_DISTANCE_SQ) {
                return true;
            }
        }
        return false;
    }
    
    public java.util.List getAllPoints() {
        java.util.List points = new ArrayList();
        if (inputPoints != null) {
            java.util.List inputBranch = null;
            for (Iterator it = inputPoints.iterator(); it.hasNext();) {
                inputBranch = (java.util.List) it.next();
                if (inputBranch != null && inputBranch.size() > 0)
                    points.addAll(inputBranch);
            }
        }
        points.addAll(backbonePoints);
        if (outputPoints != null) {
            java.util.List outputBranch = null;
            for (Iterator it = outputPoints.iterator(); it.hasNext();) {
                outputBranch = (java.util.List) it.next();
                if (outputBranch != null && outputBranch.size() > 0)
                    points.addAll(outputBranch);
            }
        }
        if (helperPoints != null) {
            java.util.List helperBranch = null;
            for (Iterator it = helperPoints.iterator(); it.hasNext();) {
                helperBranch = (java.util.List) it.next();
                if (helperBranch != null && helperBranch.size() > 0)
                    points.addAll(helperBranch);
            }
        }
        if (inhibitorPoints != null) {
        	java.util.List inhibitorBranch = null;
        	for (Iterator it = inhibitorPoints.iterator(); it.hasNext();) {
        		inhibitorBranch = (java.util.List) it.next();
        		if (inhibitorBranch != null && inhibitorBranch.size() > 0)
        			points.addAll(inhibitorBranch);
        	}
        }
        if (activatorPoints != null) {
        	java.util.List activatorBranch = null;
        	for (Iterator it = activatorPoints.iterator(); it.hasNext();) {
        		activatorBranch = (java.util.List) it.next();
        		if (activatorBranch != null && activatorBranch.size() > 0)
        			points.addAll(activatorBranch);
        	}
        }
        return points;
    }
    
    /**
     * Get the selected Point. This selected Point is picked by method isPicked(Point).
     */
    public Point getSelectedPoint() {
        return selectionInfo.selectPoint;
    }
    
    public ConnectWidget getConnectWidget() {
        ConnectWidget widget = selectionInfo.connectWidget;
        if (widget != null) {
            // Make sure only one widget is created for a sinlge Point
            ConnectWidget oldWidget = connectInfo.searchConnectWidget(widget.getPoint());
            if (oldWidget != null)
                return oldWidget;
            widget.setEdge(this);
        }
        return widget;
    }
    
    public List<Node> getConnectedNodes() {
        List<Node> inputs = getInputNodes();
        List<Node> outputs = getOutputNodes();
        List<Node> helpers = getHelperNodes();
        List<Node> activators = getActivatorNodes();
        List<Node> inhibitors = getInhibitorNodes();
        Set<Node> all = new HashSet<Node>();
        all.addAll(inputs);
        all.addAll(outputs);
        all.addAll(helpers);
        all.addAll(activators);
        all.addAll(inhibitors);
        return new ArrayList<Node>(all);
    }
    
    public void addInput() {
        Point p = null;
        // For update ConnectWidget
        if (inputPoints == null || inputPoints.size() == 0)
            p = getInputHub();
        addInputBranch();
        // Branch from zero. ConnectWidget needs to be updated.
        if (p != null) {
            ConnectWidget connectWidget = connectInfo.searchConnectWidget(p);
            if (connectWidget != null)
                connectWidget.setControlPoint(getInputHub());
        }
    }
    
    public void addInput(Node node) {
    	java.util.List inputConnectWidgets = ((HyperEdgeConnectInfo)connectInfo).getInputWidgets();
    	// Search an empty input branch
    	Point inputP = null;
    	Point controlP = null;
    	int index = 0;
    	if (inputPoints == null || inputPoints.size() == 0) {
    		if (inputConnectWidgets == null || inputConnectWidgets.size() == 0) {// No input
    			inputP = getInputHub();
    			controlP = (Point) backbonePoints.get(1);
    			index = 0;
    		}
    		else { // Create a new input branch.
    			addInput();
    			java.util.List inputBranch = (java.util.List)inputPoints.get(1);
    			index = 1;
    			inputP = (Point) inputBranch.get(0);
    			if (inputBranch.size() > 1)
    				controlP = (Point) inputBranch.get(1);
    			else
    				controlP = getInputHub();
    		}
    	}
    	else {
    		// Search if there is an empty slot.
    		index = -1; // Mark
    		ConnectWidget widget1 = null;
    		boolean found = false;
    		for (int i = 0; i < inputPoints.size(); i++) {
    			// Search if the index i is used
    			found = false;
    			for (Iterator it = inputConnectWidgets.iterator(); it.hasNext();) {
    				widget1 = (ConnectWidget)it.next();
    				if (widget1.getIndex() == i) {
    					found = true;
    					break;
    				}
    			}
    			if (!found) {
    				index = i;
    				break;
    			}
    		}
    		if (index != -1) { // There is an empty slot
    			java.util.List inputBranch = (java.util.List) inputPoints.get(index);
    			inputP = (Point) inputBranch.get(0);
    			if (inputBranch.size() > 1)
    				controlP = (Point) inputBranch.get(1);
    			else
    				controlP = getInputHub();
    		}
    		else { // Create a new branch
    			addInput();
    			java.util.List inputBranch = (java.util.List) inputPoints.get(inputPoints.size() - 1);
    			index = inputPoints.size() - 1;
    			inputP = (Point) inputBranch.get(0);
    			if (inputBranch.size() > 1)
    				controlP = (Point) inputBranch.get(1);
    			else
    				controlP = getInputHub();
    		}
    	}
    	if (node.getPosition() == null)
    		node.setPosition(new Point(inputP));
    	ConnectWidget widget = new ConnectWidget(inputP, 
    	                                         controlP, 
    	                                         INPUT,
    	                                         index);
		widget.setConnectedNode(node);
		widget.setEdge(this);
		widget.invalidate();
		widget.connect();
		addConnectWidget(widget);
    }
    
    public void removeInput(int index) {
        if (index == 0 && (inputPoints == null || inputPoints.size() == 0)) {
            // Just disconnect
            removeConnectWidget(index, INPUT);
            return;
        }
        java.util.List inputBranch = removeInputBranch(index);
        removeBranch(inputBranch);
    }
    
    public void addOutput() {
        Point p = null;
        // For update ConnectWidget
        if (outputPoints == null || outputPoints.size() == 0)
            p = getOutputHub();
        addOutputBranch();
        // Branch from zero. ConnectWidget needs to be updated.
        if (p != null) {
            ConnectWidget connectWidget = connectInfo.searchConnectWidget(p);
            if (connectWidget != null)
                connectWidget.setControlPoint(getOutputHub());
        }
    }
    
    /**
     * Check if an selected Point can be removed. A position that
     * is attached to helper branched cannot be removed.
     */
    public boolean isPointRemovable() {
        if (selectionInfo.selectPoint == null)
            return false;
        if (selectionInfo.selectPoint == position) {
            if (helperPoints != null && helperPoints.size() > 0)
                return false;
            if (inhibitorPoints != null && inhibitorPoints.size() > 0)
                return false;
            if (activatorPoints != null && activatorPoints.size() > 0)
                return false;
        }
        // Cannot remove more if there is only two points in the backbone
        if (backbonePoints.size() == 2)
            return false;
        // Don't remove the first or last point in the backbones or other
        // branches
        List branch = getBranchFromType(selectionInfo.selectedType);
        if (selectionInfo.selectedType == BACKBONE) {
            int index = branch.indexOf(selectionInfo.selectPoint);
            if (index == 0 || index == branch.size() - 1)
                return false;
        }
        else {
            // Need to get the actual branch
            branch = (List) branch.get(selectionInfo.getSelectedBranch());
            int index = branch.indexOf(selectionInfo.selectPoint);
            if (index == 0)
                return false;
        }
        return true;
    }
    
    public void removeSelectedPoint() {
        if (!(isPointRemovable()))
            return;
        int index = selectionInfo.getSelectedBranch();
        int type = selectionInfo.getSelectedType();
        List branch = getBranchFromType(type);
        List points = null;
        if (type == BACKBONE)
            points = backbonePoints;
        else
            points = (List) branch.get(index);
        points.remove(selectionInfo.selectPoint);
        selectionInfo.selectPoint = null;
        selectionInfo.selectedBranch = -1;
        selectionInfo.selectedType = NONE;
        if (type == BACKBONE)
            validateWidgetControlPoints();
        else
            validateWidgetControlPoints(type);
        validatePosition();
    }
    
    /**
     * Make sure the position is still tracked by the object.
     */
    public void validatePosition() {
        // Make sure the position point is still in the backbone
        if (!backbonePoints.contains(position)) {
            // Pick up the nearest point as the position
            double min = Double.MAX_VALUE;
            Point candidate = null;
            // Avoid using the first and last point if possible
            for (int i = 1; i < backbonePoints.size() - 1; i++) {
                Point p = (Point) backbonePoints.get(i);
                double distSqr = p.distanceSq(position.x, position.y);
                if (distSqr < min) {
                    candidate = p;
                    min = distSqr;
                }
            }
            // Have to use the first or last Point if there are only
            // two points in the backbone. Cannot use the middle point
            // since the two end points may changed during moving.
            if (candidate == null) {
                candidate = (Point) backbonePoints.get(0);
            }
            position = candidate;
        }
        else {
            // Check if position is the first or last point
            int index = backbonePoints.indexOf(position);
            if ((index == 0 || index == backbonePoints.size() - 1) &&
                backbonePoints.size() > 0) {
                // Try to pick up the middle point
                index = backbonePoints.size() / 2;
                position = (Point) backbonePoints.get(index);
            }
        }
    }
    
    /**
     * Add a new bending point to a selected branch or backbone.
     * @param point
     */
    public void addPoint(Point point) {
        // Make a copy in case this point has been changed
        point = new Point(point);
        int index = selectionInfo.getSelectedBranch();
        int type = selectionInfo.getSelectedType();
        List branch = getBranchFromType(type);
        List points = null;
        if (type == BACKBONE)
            points = backbonePoints;
        else
            points = (List) branch.get(index);
        // Need to find where the point should be inserted
        double min = Double.MAX_VALUE;
        int insert = 0;
        List<Point> checking = new ArrayList<Point>(points);
        if (type != BACKBONE) {
            // Need to add the position
            checking.add(getPosition());
        }
        for (int i = 0; i < checking.size() - 1; i++) {
            Point p1 = checking.get(i);
            Point p2 = checking.get(i + 1);
            double dist = Line2D.ptSegDistSq(p1.x, 
                                             p1.y, 
                                             p2.x, 
                                             p2.y, 
                                             point.x,
                                             point.y);
            if (dist < min) {
                min = dist;
                insert = i;
            }
        }
        if (insert == points.size()) // Just before the position in case non-backbone points
            points.add(point);
        else
            points.add(insert + 1, 
                       point);
        if (type == BACKBONE)
            validateWidgetControlPoints();
        else
            validateWidgetControlPoints(type);
        selectionInfo.selectPoint = point;
        // Use this method to make sure a new point can be
        // picked up from a two points backbone to three point
        // backbone.
        validatePosition(); 
    }
    
    /**
     * All control points should be validated
     */
    public void validateWidgetControlPoints() {
        validateWidgetControlPoints(INPUT);
        validateWidgetControlPoints(OUTPUT);
        validateWidgetControlPoints(CATALYST);
        validateWidgetControlPoints(INHIBITOR);
        validateWidgetControlPoints(ACTIVATOR);
    }
    
    public void addOutput(Node node) {
    	// Don't add an empty node
    	if (node==null)
    		return;
		java.util.List outputWidgets = ((HyperEdgeConnectInfo)connectInfo).getOutputWidgets();
		Point outputP = null;
		Point controlP = null;
		int index = 0;
		if (outputPoints == null || outputPoints.size() == 0) {
			if (outputWidgets == null || outputWidgets.size() == 0) {// No input
				outputP = getOutputHub();
				controlP = (Point) backbonePoints.get(backbonePoints.size() - 2);
				index = 0;
			}
			else { // Create a new input branch.
				addOutput();
				java.util.List outputBranch = (java.util.List)outputPoints.get(1);
				index = 1;
				outputP = (Point) outputBranch.get(0);
				if (outputBranch.size() > 1)
					controlP = (Point) outputBranch.get(1);
				else
					controlP = getOutputHub();
			}
		}
		else {
			// Search if there is an empty slot.
			index = -1; // Mark
			ConnectWidget widget1 = null;
			boolean found = false;
			for (int i = 0; i < outputPoints.size(); i++) {
				// Search if the index i is used
				found = false;
				for (Iterator it = outputWidgets.iterator(); it.hasNext();) {
					widget1 = (ConnectWidget)it.next();
					if (widget1.getIndex() == i) {
						found = true;
						break;
					}
				}
				if (!found) {
					index = i;
					break;
				}
			}
			if (index != -1) { // There is an empty slot
				java.util.List outputBranch = (java.util.List) outputPoints.get(index);
				outputP = (Point) outputBranch.get(0);
				if (outputBranch.size() > 1)
					controlP = (Point) outputBranch.get(1);
				else
					controlP = getOutputHub();
			}
			else { // Create a new branch
				addOutput();
				java.util.List outputBranch = (java.util.List) outputPoints.get(outputPoints.size() - 1);
				index = outputPoints.size() - 1;
				outputP = (Point) outputBranch.get(0);
				if (outputBranch.size() > 1)
					controlP = (Point) outputBranch.get(1);
				else
					controlP = getOutputHub();
			}
		}
		if (node.getPosition() == null)
			node.setPosition(new Point(outputP));
		ConnectWidget widget = new ConnectWidget(outputP, controlP, OUTPUT, index);
		widget.setConnectedNode(node);
		widget.setEdge(this);
		widget.invalidate();
		widget.connect();
		addConnectWidget(widget);    
    }
    
    private void removeBranch(java.util.List branch) {
        if (branch != null) {
            Point p = (Point) branch.get(0);
            ConnectWidget widget = connectInfo.searchConnectWidget(p);
            if (widget != null) {
                widget.disconnect();
                connectInfo.removeConnectWidget(widget);
            }
        }
    }

    public void removeOutput(int index) {
        if (index == 0 &&
            (outputPoints == null || outputPoints.size() == 0)) {
            removeConnectWidget(index, OUTPUT);
            return;
        }
        java.util.List outputBranch = removeOutputBranch(index);
        removeBranch(outputBranch);
    }
    
    /**
     * Detach a Node from this HyperEdge.
     * @param node
     */
	public void remove(Renderable node) {
		java.util.List connectWidgets = connectInfo.getConnectWidgets();
		if (connectWidgets != null) {
			// Remove connecting info.
			for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
				ConnectWidget widget = (ConnectWidget)it.next();
				if (widget.getConnectedNode() == node) {
					it.remove();
					node.removeConnectWidget(widget);
				}
			}
		}
	}
	
	private void removeConnectWidget(int index,
	                                 int type) {
	    java.util.List connectWidgets = connectInfo.getConnectWidgets();
	    if (connectWidgets != null) {
	        // Remove connecting info.
	        ConnectWidget found = null;
	        for (Iterator it = connectWidgets.iterator(); it.hasNext();) {
	            ConnectWidget widget = (ConnectWidget)it.next();
	            if (widget.getRole() == type &&
	                    widget.getIndex() == index) {
	                found = widget;
	                break;
	            }
	        }
	        if (found != null)
	            found.disconnect();
	    }
	}
	
	public void remove(Renderable node,
	                   int type) {
	    switch (type) {
	        case HyperEdge.INPUT :
	            // Find the index
	            List<Node> inputs = getInputNodes();
	            int index = inputs.indexOf(node);
	            if (index >= 0)
	                removeInput(index);
	            break;
	        case HyperEdge.OUTPUT :
	            List<Node> outputs = getOutputNodes();
	            index = outputs.indexOf(node);
	            if (index >= 0)
	                removeOutput(index);
	            break;
	        case HyperEdge.CATALYST :
	            List<Node> helpers = getHelperNodes();
	            index = helpers.indexOf(node);
	            if (index >= 0)
	                removeHelper(index);
	            break;
	        case HyperEdge.INHIBITOR :
	            List<Node> inhibitors = getInhibitorNodes();
	            index = inhibitors.indexOf(node);
	            if (index >= 0)
	                removeInhibitor(index);
	            break;
	        case HyperEdge.ACTIVATOR :
	            List<Node> activators = getActivatorNodes();
	            index = activators.indexOf(node);
	            if (index >= 0)
	                removeActivator(index);
	            break;
	    }
	}
	
    public void addHelper(Node node) {
		java.util.List helperWidgets = ((HyperEdgeConnectInfo)connectInfo).getHelperWidgets();
		java.util.List helperBranch = null;
		int index = 0;
		if (helperPoints == null || helperPoints.size() == 0) {
			addHelperBranch();
			helperBranch = (java.util.List) helperPoints.get(0);
			index = 0;
		}
		else {
			// Search if there is an empty slot.
			index = -1; // Mark
			ConnectWidget widget1 = null;
			boolean found = false;
			for (int i = 0; i < helperPoints.size(); i++) {
				// Search if the index i is used
				found = false;
				for (Iterator it = helperWidgets.iterator(); it.hasNext();) {
					widget1 = (ConnectWidget)it.next();
					if (widget1.getIndex() == i) {
						found = true;
						break;
					}
				}
				if (!found) {
					index = i;
					break;
				}
			}
			if (index != -1) { // There is an empty slot
				helperBranch = (java.util.List) helperPoints.get(index);
			}
			else { // Create a new branch
				addHelperBranch();
				helperBranch = (java.util.List) helperPoints.get(helperPoints.size() - 1);
				index = helperPoints.size() - 1;
			}
		}
		ensureThreePointsInBackbone();
		Point helperP = (Point) helperBranch.get(0);
		Point controlP = null;
		if (helperBranch.size() > 1)
			controlP = (Point) helperBranch.get(1);
		else
			controlP = getPosition();
        if (node.getPosition() == null)
            node.setPosition(new Point(helperP));
		ConnectWidget widget = new ConnectWidget(helperP, controlP, CATALYST, index);
		widget.setConnectedNode(node);
		widget.setEdge(this);
		widget.invalidate();
		widget.connect();
		addConnectWidget(widget);        	
    }
    
    /**
     * Make sure there are three points in the backbone so that
     * a helper/inhibitor/activator branch will not attach to
     * the first or last point which is used for input or output.
     */
    private void ensureThreePointsInBackbone() {
        if (backbonePoints.size() > 2)
            return;
        // Add a point to backbone
        Point p1 = (Point) backbonePoints.get(0);
        Point p2 = (Point) backbonePoints.get(1);
        Point newPoint = new Point();
        newPoint.x = (p1.x + p2.x) / 2;
        newPoint.y = (p1.y + p2.y) / 2;
        backbonePoints.add(1, newPoint);
        position = newPoint;
        // Have to validate all control points
        validateWidgetControlPoints();
    }
    
    public void removeHelper(int index) {
        java.util.List branch = removeHelperBranch(index);
        removeBranch(branch);
    }
    
    public void addInputBranch() {
        // Create a Point for the new inputBranch
        if (inputPoints == null || inputPoints.size() == 0) {
            inputPoints = new ArrayList();
            // Start a new InputBranch from the backbone points.
            java.util.List branch = new ArrayList(1);
            Point p = getInputHub();
            Point ctrlP = (Point)backbonePoints.get(1);
            Point p1 = new Point();
            p1.x = (ctrlP.x + p.x) / 2;
            p1.y = (ctrlP.y + p.y) / 2;
            backbonePoints.set(0, p1);
            // Original inputHub (first point in the backbone)
            // has been transformed into branch.
            branch.add(p);
            inputPoints.add(branch);
            // positions and widget control points may be changed.
            validatePosition();
            validateWidgetControlPoints();
        }
        java.util.List inputBranch = new ArrayList();
        Point inputHub = getInputHub();
        // Find a good initial position
        Point newP = generateRandomPoint(inputPoints);
        inputBranch.add(newP);
        inputPoints.add(inputBranch);
        // Make it the default select widget
		selectionInfo.selectPoint = newP;
		int index = inputPoints.size() - 1;
		selectionInfo.selectedBranch = index;
		selectionInfo.selectedType = INPUT;
		selectionInfo.connectWidget = new ConnectWidget(newP, getInputHub(), INPUT, index);
    }
    
    private Point generateRandomPoint(java.util.List branches) {
    	Point p = new Point();
    	int minX = Integer.MAX_VALUE;
    	int maxX = Integer.MIN_VALUE;
    	int minY = Integer.MAX_VALUE;
    	int maxY = Integer.MIN_VALUE;
    	for (Iterator it = branches.iterator(); it.hasNext();) {
    		java.util.List list = (java.util.List) it.next();
    		Point tmpP = (Point) list.get(0);
    		if (minX > tmpP.x)
    			minX = tmpP.x;
    		if (minY > tmpP.y)
    			minY = tmpP.y;
    		if (maxX < tmpP.x)
    			maxX = tmpP.x;
    		if (maxY < tmpP.y)
    			maxY = tmpP.y;
    	}
    	// Make 30 as the minimum difference
    	int diffX = maxX - minX;
    	if (diffX < 30)
    		diffX = 30;
    	int diffY = maxY - minY;	
    	if (diffY < 30)
    		diffY = 30;
    	p.x = minX + (int)(diffX * Math.random());
    	p.y = minY + (int)(diffY * Math.random());
    	return p;
    }
    
    /**
     * @return return the removed inputBranch.
     */
    public java.util.List removeInputBranch(int index) {
        if (inputPoints == null || index < 0 || index > inputPoints.size() - 1)
            return null;
        java.util.List branch = (java.util.List) inputPoints.remove(index);
        if (inputPoints.size() == 1) {
            // Merge the last branch with the backbone.
            java.util.List branch1 = (java.util.List) inputPoints.get(0);
            // Remove the first backbone point to do merging.
            Point p = (Point) backbonePoints.remove(0);
            // Replace the control point for the connect widgets
            java.util.List inputWidgets = ((HyperEdgeConnectInfo)connectInfo).getInputWidgets();
            if (inputWidgets != null && inputWidgets.size() > 0) {
            	ConnectWidget widget = null;
            	for (Iterator it = inputWidgets.iterator(); it.hasNext();) {
            		widget = (ConnectWidget) it.next();
            		if (widget.getControlPoint() == p)
            			widget.setControlPoint((Point)backbonePoints.get(0));
            	}
            }
            for (Iterator it = branch1.iterator(); it.hasNext();) {
                backbonePoints.add(0, (Point)it.next());
            }
            inputPoints.clear();
            inputPoints = null;
            validatePosition();
            if (selectionInfo.selectedType == INPUT)
                selectionInfo.selectedType = BACKBONE;
        }
        // Have to update index in the ConnectWidget objects
        java.util.List inputWidgets = ((HyperEdgeConnectInfo)connectInfo).getInputWidgets();
        ConnectWidget widget = null;
        for (Iterator it = inputWidgets.iterator(); it.hasNext();) {
        	widget = (ConnectWidget) it.next();
        	if (widget.getIndex() >= index)
        		widget.setIndex(widget.getIndex() - 1);
        }
        return branch;
    }
    
    public void addOutputBranch() {
        // Create a Point for the new OutputBranch
        if (outputPoints == null || outputPoints.size() == 0) {
            outputPoints = new ArrayList();
            java.util.List branch = new ArrayList();
            Point p = getOutputHub();
            Point ctrlP = (Point) backbonePoints.get(backbonePoints.size() - 2);
            Point p1 = new Point();
            p1.x = (ctrlP.x + p.x) / 2;
            p1.y = (ctrlP.y + p.y) / 2;
            backbonePoints.set(backbonePoints.size() - 1, p1);
            // The original output hub has been transformed into the first output
            // branch.
            branch.add(p);
            outputPoints.add(branch);
            // positions and widget control points may be changed.
            validatePosition();
            validateWidgetControlPoints();
        }
        java.util.List outputBranch = new ArrayList();
        Point outputHub = getOutputHub();
        // Almost random
        Point newP = generateRandomPoint(outputPoints);
        outputBranch.add(newP);
        outputPoints.add(outputBranch);
		// Make it the default select widget
		selectionInfo.selectPoint = newP;
		int index = outputPoints.size() - 1;
		selectionInfo.selectedBranch = index;
		selectionInfo.selectedType = OUTPUT;
		selectionInfo.connectWidget = new ConnectWidget(newP, getOutputHub(), OUTPUT, index);    
    }
    
    public java.util.List removeOutputBranch(int index) {
        if (outputPoints == null || index < 0 || index > outputPoints.size() - 1)
            return null;
        java.util.List branch = (java.util.List) outputPoints.remove(index);
        if (outputPoints.size() == 1) {
            // Merge the last branch with the backbone.
			// Remove the first backbone point to do merging.
			int indexTmp = backbonePoints.size() - 1;
			Point p = (Point) backbonePoints.remove(indexTmp);
			// Replace the control point for the connect widgets
			java.util.List outputWidgets = ((HyperEdgeConnectInfo)connectInfo).getOutputWidgets();
			if (outputWidgets != null && outputWidgets.size() > 0) {
				ConnectWidget widget = null;
				for (Iterator it = outputWidgets.iterator(); it.hasNext();) {
					widget = (ConnectWidget) it.next();
					if (widget.getControlPoint() == p)
						widget.setControlPoint((Point)backbonePoints.get(indexTmp - 1));
				}
			}

            java.util.List branch1 = (java.util.List) outputPoints.get(0);
            for (Iterator it = branch1.iterator(); it.hasNext();) {
                backbonePoints.add((Point)it.next());
            }
            outputPoints.clear();
            outputPoints = null;
            validatePosition(); // The position may be changed
            if (selectionInfo.selectedType == OUTPUT)
                selectionInfo.selectedType = BACKBONE;
        }
		// Have to update index in the ConnectWidget objects
		java.util.List outputWidgets = ((HyperEdgeConnectInfo)connectInfo).getOutputWidgets();
		ConnectWidget widget = null;
		for (Iterator it = outputWidgets.iterator(); it.hasNext();) {
			widget = (ConnectWidget) it.next();
			if (widget.getIndex() >= index)
				widget.setIndex(widget.getIndex() - 1);
		}        
        return branch;
    }
    
    /**
     * Create a new helper branch.
     */
    public void addHelperBranch() {
    	Point p = null;
    	if (helperPoints == null)
    		helperPoints = new ArrayList();
        if (helperPoints.size() == 0) {
            p = new Point();
            p.x = position.x + (int)(30 * Math.random());
            p.y = position.y - 30 + (int)(60 * Math.random());
        }
        else
        	p = generateRandomPoint(helperPoints);
        java.util.List branch = new ArrayList();
        branch.add(p);
        helperPoints.add(branch);
		// Make it the default select widget
		selectionInfo.selectPoint = p;
		int index = helperPoints.size() - 1;
		selectionInfo.selectedBranch = index;
		selectionInfo.selectedType = CATALYST;
		selectionInfo.connectWidget = new ConnectWidget(p, position, CATALYST, index);        
    }
    
    public java.util.List removeHelperBranch(int index) {
        if (helperPoints == null || index < 0 || index > helperPoints.size() - 1)
            return null;
		java.util.List branch = (java.util.List) helperPoints.remove(index);
        // Have to update ConnectWidgets
        java.util.List helperWidgets = ((HyperEdgeConnectInfo)connectInfo).getHelperWidgets();
        ConnectWidget widget = null;
        for (Iterator it = helperWidgets.iterator(); it.hasNext();) {
        	widget = (ConnectWidget) it.next();
        	if (widget.getIndex() >= index)
        		widget.setIndex(widget.getIndex() - 1);
        }
        return branch;
    }
    
    /**
     * This method is used to remove a branch in this HyperEdge that is not connected
     * to other node.
     * @param connectWidget
     */
    public void deleteUnAttachedBranch(ConnectWidget connectWidget) {
        int role = connectWidget.getRole();
        int index = connectWidget.getIndex();
        if (role == HyperEdge.INPUT)
            removeInputBranch(index);
        else if (role == HyperEdge.OUTPUT)
            removeOutputBranch(index);
        else if (role == HyperEdge.CATALYST)
            removeHelperBranch(index);
        else if (role == HyperEdge.INHIBITOR)
            removeInhibitorBranch(index);
        else if (role == HyperEdge.ACTIVATOR)
            removeActivatorBranch(index);
    }
    
    public boolean hasEmptyInputSlot() {
    	java.util.List inputNodes = getInputNodes();
    	if (inputNodes.size() == 0)
    		return true;
    	if (inputPoints != null && inputPoints.size() > inputNodes.size())
    		return true;
    	return false;
    }
    
    public boolean hasEmptyOutputSlot() {
    	java.util.List outputNodes = getOutputNodes();
    	if (outputNodes.size() == 0)
    		return true;
    	if (outputPoints != null && outputPoints.size() > outputNodes.size())
    		return true;
    	return false;
    }
    
    public boolean hasEmptyHelperSlot() {
    	java.util.List helperNodes = getHelperNodes();
    	if (helperPoints != null && helperPoints.size() >  helperNodes.size())
    		return true;
    	return false;
    }
    
    public HyperEdgeSelectionInfo getSelectionInfo() {
        return this.selectionInfo;
    }
    
    public java.util.List getComponents() {
        return null;
    }
    
	public void addInhibitorBranch() {
		Point p = null;
		if (inhibitorPoints == null)
			inhibitorPoints = new ArrayList();
		if (inhibitorPoints.size() == 0) {
			p = new Point();
			p.x = position.x + (int)(30 * Math.random());
			p.y = position.y - 30 + (int)(60 * Math.random());
		}
		else
			p = generateRandomPoint(inhibitorPoints);
		java.util.List branch = new ArrayList();
		branch.add(p);
		inhibitorPoints.add(branch);
		// Make it the default select widget
		selectionInfo.selectPoint = p;
		int index = inhibitorPoints.size() - 1;
		selectionInfo.selectedBranch = index;
		selectionInfo.selectedType = CATALYST;
		selectionInfo.connectWidget = new ConnectWidget(p, position, INHIBITOR, index);        
	}
	
	public void removeInhibitor(int index) {
		java.util.List branch = removeInhibitorBranch(index);
		removeBranch(branch);
	}
	
	public java.util.List removeInhibitorBranch(int index) {
		if (inhibitorPoints == null || index < 0 || index > inhibitorPoints.size() - 1)
			return null; // In case 
		java.util.List branch = (java.util.List) inhibitorPoints.remove(index);
		// Have to update ConnectWidgets
		java.util.List inhibitorWidgets = ((HyperEdgeConnectInfo)connectInfo).getInhibitorWidgets();
		ConnectWidget widget = null;
		for (Iterator it = inhibitorWidgets.iterator(); it.hasNext();) {
			widget = (ConnectWidget) it.next();
			if (widget.getIndex() >= index)
				widget.setIndex(widget.getIndex() - 1);
		}
		return branch;
	}
	
	public void removeActivator(int index) {
		java.util.List branch = removeActivatorBranch(index);
		removeBranch(branch);
	}
	
	public java.util.List removeActivatorBranch(int index) {
		if (activatorPoints == null || index < 0 || index > activatorPoints.size() - 1)
			return null; // In case 
		java.util.List branch = (java.util.List) activatorPoints.remove(index);
		// Have to update ConnectWidgets
		java.util.List activatorWidgets = ((HyperEdgeConnectInfo)connectInfo).getActivatorWidgets();
		ConnectWidget widget = null;
		for (Iterator it = activatorWidgets.iterator(); it.hasNext();) {
			widget = (ConnectWidget) it.next();
			if (widget.getIndex() >= index)
				widget.setIndex(widget.getIndex() - 1);
		}
		return branch;
	}
	
	/**
	 * Add 
	 * @param node
	 */
	public void addInhibitor(Node node) {
		java.util.List inhibitorWidgets = ((HyperEdgeConnectInfo)connectInfo).getInhibitorWidgets();
		java.util.List inhibitorBranch = null;
		int index = 0;
		if (inhibitorPoints == null || inhibitorPoints.size() == 0) {
			addInhibitorBranch();
			inhibitorBranch = (java.util.List) inhibitorPoints.get(0);
			index = 0;
		}
		else {
			// Search if there is an empty slot.
			index = -1; // Mark
			ConnectWidget widget1 = null;
			boolean found = false;
			for (int i = 0; i < inhibitorPoints.size(); i++) {
				// Search if the index i is used
				found = false;
				for (Iterator it = inhibitorWidgets.iterator(); it.hasNext();) {
					widget1 = (ConnectWidget)it.next();
					if (widget1.getIndex() == i) {
						found = true;
						break;
					}
				}
				if (!found) {
					index = i;
					break;
				}
			}
			if (index != -1) { // There is an empty slot
				inhibitorBranch = (java.util.List) inhibitorPoints.get(index);
			}
			else { // Create a new branch
				addInhibitorBranch();
				inhibitorBranch = (java.util.List) inhibitorPoints.get(inhibitorPoints.size() - 1);
				index = inhibitorPoints.size() - 1;
			}
		}
	    ensureThreePointsInBackbone();
		Point inhibitorP = (Point) inhibitorBranch.get(0);
		Point controlP = null;
		if (inhibitorBranch.size() > 1)
			controlP = (Point) inhibitorBranch.get(1);
		else
			controlP = getPosition();
        if (node.getPosition() == null)
            node.setPosition(new Point(inhibitorP));
		ConnectWidget widget = new ConnectWidget(inhibitorP, controlP, INHIBITOR, index);
		widget.setConnectedNode(node);
		widget.setEdge(this);
		widget.invalidate();
		widget.connect();
		addConnectWidget(widget);        	
	}
	
	public java.util.List<List<Point>> getInhibitorPoints() {
		return inhibitorPoints;
	}
	
	public void setInhibitorPoints(java.util.List<List<Point>> points) {
		this.inhibitorPoints = points;
	}
	
	public void addActivatorBranch() {
		Point p = null;
		if (activatorPoints == null)
			activatorPoints = new ArrayList();
		if (activatorPoints.size() == 0) {
			p = new Point();
			p.x = position.x + (int)(30 * Math.random());
			p.y = position.y - 30 + (int)(60 * Math.random());
		}
		else
			p = generateRandomPoint(activatorPoints);
		java.util.List branch = new ArrayList();
		branch.add(p);
		activatorPoints.add(branch);
		// Make it the default select widget
		selectionInfo.selectPoint = p;
		int index = activatorPoints.size() - 1;
		selectionInfo.selectedBranch = index;
		selectionInfo.selectedType = CATALYST;
		selectionInfo.connectWidget = new ConnectWidget(p, position, ACTIVATOR, index);        
	}
	
	public void addActivator(Node node) {
		java.util.List activatorWidgets = ((HyperEdgeConnectInfo)connectInfo).getActivatorWidgets();
		java.util.List activatorBranch = null;
		int index = 0;
		if (activatorPoints == null || activatorPoints.size() == 0) {
			addActivatorBranch();
			activatorBranch = (java.util.List) activatorPoints.get(0);
			index = 0;
		}
		else {
			// Search if there is an empty slot.
			index = -1; // Mark
			ConnectWidget widget1 = null;
			boolean found = false;
			for (int i = 0; i < activatorPoints.size(); i++) {
				// Search if the index i is used
				found = false;
				for (Iterator it = activatorWidgets.iterator(); it.hasNext();) {
					widget1 = (ConnectWidget)it.next();
					if (widget1.getIndex() == i) {
						found = true;
						break;
					}
				}
				if (!found) {
					index = i;
					break;
				}
			}
			if (index != -1) { // There is an empty slot
				activatorBranch = (java.util.List) activatorPoints.get(index);
			}
			else { // Create a new branch
				addActivatorBranch();
				activatorBranch = (java.util.List) activatorPoints.get(activatorPoints.size() - 1);
				index = activatorPoints.size() - 1;
			}
		}
		ensureThreePointsInBackbone();
		Point activatorP = (Point) activatorBranch.get(0);
		Point controlP = null;
		if (activatorBranch.size() > 1)
			controlP = (Point) activatorBranch.get(1);
		else
			controlP = getPosition();
        if (node.getPosition() == null)
            node.setPosition(new Point(activatorP));
		ConnectWidget widget = new ConnectWidget(activatorP, controlP, ACTIVATOR, index);
		widget.setConnectedNode(node);
		widget.setEdge(this);
		widget.invalidate();
		widget.connect();
		addConnectWidget(widget);        	
	}
	
	public java.util.List<List<Point>> getActivatorPoints() {
		return activatorPoints;
	}
	
	public void setActivatorPoints(java.util.List<List<Point>> points) {
		this.activatorPoints = points;
	}
    
    /**
     * Use this method to initialize the position info for a brand new RenderableReaction.
     * @param p the position.
     */
    public void initPosition(Point p) {
        setPosition(p);
        Point inputHub = new Point(p.x - 40, p.y);
        setInputHub(inputHub);
        Point outputHub = new Point(p.x + 40, p.y);
        setOutputHub(outputHub);
        needCheckBounds = true;
    }       
    
    public void validateConnectInfo() {
        connectInfo.validate();
    }
    
    public Object getGKObject() {
        return null;
    }
    
    public Rectangle getBounds() {
    	if (needCheckBounds)
    		validateBounds();
    	return this.bounds;
    }
    
    public void validateBounds() {
    	// Reset the bounds
    	bounds.x = position.x;
    	bounds.y = position.y;
    	bounds.width = 5; // Minimum
    	bounds.height = 5; // Minimum
    	Point p = null;
    	// Get all points related to this edge to do checking
    	java.util.List points = new ArrayList();
    	points.addAll(backbonePoints);
    	if (inputPoints != null) {
    		for (Iterator it = inputPoints.iterator(); it.hasNext();){
    			java.util.List list = (java.util.List) it.next();
    			points.addAll(list);
    		}
    	}
    	if (outputPoints != null) {
    		for (Iterator it = outputPoints.iterator(); it.hasNext();) {
    			java.util.List list = (java.util.List) it.next();
    			points.addAll(list);
    		}
    	}
    	if (helperPoints != null) {
    		for (Iterator it = helperPoints.iterator(); it.hasNext();) {
    			java.util.List list = (java.util.List) it.next();
    			points.addAll(list);
    		}
    	}
        if (inhibitorPoints != null) {
            for (Iterator it = inhibitorPoints.iterator(); it.hasNext();) {
                List list = (List) it.next();
                points.addAll(list);
            }
        }
        if (activatorPoints != null) {
            for (Iterator it = activatorPoints.iterator(); it.hasNext();) {
                List list = (List) it.next();
                points.addAll(list);
            }
        }
    	for (Iterator it = points.iterator(); it.hasNext();) {
    		p = (Point) it.next();
    		if (p.x > bounds.width + bounds.x) {
    			bounds.width = p.x - bounds.x;
    		}
    		else if (p.x < bounds.x) {
    			bounds.width += (bounds.x - p.x);
    			bounds.x = p.x; 
    		}
    		if (p.y > bounds.height + bounds.y) {
    			bounds.height = p.y - bounds.y;
    		}
    		else if (p.y < bounds.y) {
    			bounds.height += (bounds.y - p.y);
    			bounds.y = p.y;
    		}
    	}
    	needCheckBounds = false;
    }
    
    public String getType() {
    	return "Hyperedge";
    }
    
    public List getLinkWidgetPositions() {
        return null;
    }
    
    /**
     * Create a shallow copy of this HyperEdge. All attributes in the returned copy are shared
     * with this objects.
     * @return
     */
    public HyperEdge shallowCopy() {
        try {
            HyperEdge copy = (HyperEdge) getClass().newInstance();
            copy.backbonePoints = backbonePoints;
            copy.inputPoints = inputPoints;
            copy.outputPoints = outputPoints;
            copy.helperPoints = helperPoints;
            copy.inhibitorPoints = inhibitorPoints;
            copy.activatorPoints = activatorPoints;
            copy.needInputArrow = needInputArrow;
            copy.needOutputArrow = needOutputArrow;
            copy.lineWidth = lineWidth;
            copy.position = position;
            copy.attributes = attributes;
            copy.foregroundColor = foregroundColor;
            copy.backgroundColor = backgroundColor;
            copy.lineColor = lineColor;
            copy.isVisible = isVisible;
            copy.bounds = bounds;
            copy.renderer = renderer;
            copy.selectionInfo = selectionInfo;
            // Make a new copy of connect info
            HyperEdgeConnectInfo connectInfoCopy = new HyperEdgeConnectInfo();
            copy.connectInfo = connectInfoCopy;
            if (connectInfo.getConnectWidgets() != null) {
                for (ConnectWidget widget : connectInfo.getConnectWidgets()) {
                    ConnectWidget widgetClone = widget.shallowCopy();
                    widgetClone.setConnectedNode(widget.getConnectedNode());
                    widgetClone.setEdge(copy);
                    widgetClone.connect();
                }
            }
            return copy;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This is a simple version of isPicked(Point) to check if the input or output hub is picked.
     * An input or output hub in the end point of a FlowLine object.
     * @param p
     * @return
     */
    public boolean isPointPicked(Point p) {
        if (!isSelected()) // Make sure this edge is selected.
            return false;
        return canPointBePicked(p);
//        if (backbonePoints == null || backbonePoints.size() < 2)
//            return false; // Not in the right state
//        Point inputHub = (Point) backbonePoints.get(0);
//        double distSq = Point2D.distanceSq(inputHub.x, inputHub.y, p.x, p.y);
//        if (distSq < SENSING_DISTANCE_SQ)
//            return true;
//        Point outputHub = (Point) backbonePoints.get(backbonePoints.size() - 1);
//        distSq = Point2D.distanceSq(outputHub.x, outputHub.y, p.x, p.y);
//        if (distSq < SENSING_DISTANCE_SQ)
//            return true;
//        return false;
    }
}
