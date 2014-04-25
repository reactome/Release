/*
 * GraphNode.java
 *
 * Created on June 23, 2003, 3:19 PM
 */

package org.gk.render;

import static org.gk.render.SelectionPosition.NORTH_WEST;
import static org.gk.render.SelectionPosition.SOUTH_EAST;
import static org.gk.render.SelectionPosition.SOUTH_WEST;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 *
 * @author  wgm
 */
public class Node extends Renderable {
    static final int RESIZE_WIDGET_WIDTH = 4;
	// The node width. All nodes should have the same width currently.
	private static int width = DefaultRenderConstants.DEFAULT_NODE_WIDTH;
	private static double widthRatioOfBoundsToText = 1.3;
	private static double heightRatioOfBoundsToText = 1.5;
	protected static boolean ensureBoundsInView = true;
	protected int boundsBuffer = 4;
	// For editing display name
	private transient boolean isEditing;
	// For line wrapping
	private transient java.util.List textLayouts;
	protected List<Renderable> components;
	// For stoichiometry
	private transient Rectangle stoiBounds;
    // positions for link widgets
    protected transient List linkWidgetPositions;
    // used as a reference point for link edges
    protected transient Point linkPoint;
    // Used to flag the moving 
    protected boolean duringMoving = false;
    // Track what part of Node has been selected: default should be none
    protected SelectionPosition selectionPosition = SelectionPosition.NONE;
    // A flag to indicate that if a Node can be linked to other node.
    // A Node contained by a complex cannot be linked to other node
    protected boolean isLinkable = true; // default should be true
    // For SequenceFeatures
    protected boolean isFeatureAddable = false;
    // For RenderableStates
    protected boolean isStateAddable = false;
    protected List<NodeAttachment> attachments;
    // Some nodes cannot be edited
    protected boolean isEditable = true;
    // Check if a multimer can be formed: a read-only property
    protected boolean isMultimerFormable;
    protected int multimerMonomerNumber;
    // This list should be shared by all shortcuts.
    // Note: This is a transient variable. The client should figure out 
    // shortcuts itself after read/write of this object.
    protected transient List<Renderable> shortcuts;
    protected Integer minWidth = 10;
    protected boolean needDashedBorder;
    
    /** Creates a new instance of GraphNode */
    public Node() {
        setConnectInfo(new NodeConnectInfo());
    }
    
    public Node(String displayName) {
        this();
        setDisplayName(displayName);
    }
    
    public static void setEnsureBoundsInView(boolean ensure) {
        ensureBoundsInView = ensure;
    }
    
    public void setMinWidth(Integer width) {
        this.minWidth = width;
    }
    
    public Integer getMinWidth() {
        return this.minWidth;
    }
    
    public boolean isNeedDashedBorder() {
        return needDashedBorder;
    }

    public void setNeedDashedBorder(boolean needDashedBorder) {
        this.needDashedBorder = needDashedBorder;
    }

    /**
     * Return a read-only property.
     * @return
     */
    public boolean isMultimerFormable() {
        return this.isMultimerFormable;
    }
    
    public void setMultimerMonomerNumber(int number) {
        if (!isMultimerFormable)
            throw new IllegalStateException("Node.setMultimerMonomerNumber(): this type of Node cannot form multimer!");
        this.multimerMonomerNumber = number;
    }
    
    public int getMultimerMonomerNumber() {
        return this.multimerMonomerNumber;
    }
    
    public boolean isPicked(Point p) {
        // A node should not be picked if it is hidden
        if (getBounds() == null || !isVisible)
            return false;
        if(isResizeWidgetPicked(p))
            return true;
        if(isNodeAttachmentPicked(p))
            return true;
        boolean isSelected = getBounds().contains(p);
        if (isSelected)
            selectionPosition = SelectionPosition.NODE;
        return isSelected;
    }

    protected boolean isResizeWidgetPicked(Point p) {
        // reset
        selectionPosition = SelectionPosition.NONE;
        if (bounds == null)
            return false;
        // Check if any resize widget is picked
        // north-east
        Rectangle resizeWidget = new Rectangle();
        resizeWidget.width = 2 * RESIZE_WIDGET_WIDTH;
        resizeWidget.height = 2 * RESIZE_WIDGET_WIDTH;
        resizeWidget.x = bounds.x + bounds.width - RESIZE_WIDGET_WIDTH;
        resizeWidget.y = bounds.y - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SelectionPosition.NORTH_EAST;
            return true;
        }
        // southeast
        resizeWidget.x = bounds.x + bounds.width - RESIZE_WIDGET_WIDTH;
        resizeWidget.y = bounds.y + bounds.height - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SOUTH_EAST;
            return true;
        }
        // southwest
        resizeWidget.x = bounds.x - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = SOUTH_WEST;
            return true;
        }
        // northwest
        resizeWidget.y = bounds.y - RESIZE_WIDGET_WIDTH;
        if (resizeWidget.contains(p)) {
            selectionPosition = NORTH_WEST;
            return true;
        }
        return false;
    }

    protected boolean isNodeAttachmentPicked(Point p) {
        // check if and NodeAttachment is selected
        if (attachments != null && attachments.size() > 0) {
            NodeAttachment selected = null;
            for (NodeAttachment attachment : attachments) {
                if (!attachment.isEditable())
                    continue;
                if (attachment.isPicked(p)) {
                    attachment.setIsSelected(true);
                    selected = attachment;
                }
                else
                    attachment.setIsSelected(false);
            }
            if (selected instanceof RenderableFeature) {
                selectionPosition = SelectionPosition.FEATURE;
                return true;
            }
            else if (selected instanceof RenderableState) {
                selectionPosition = SelectionPosition.STATE;
                return true;
            }
        }
        return false;
    }
    
    protected void validateMinimumBounds() {
        if (bounds.width < minWidth)
            bounds.width = minWidth;
        if (bounds.height < minWidth)
            bounds.height = minWidth;
    }
    
    /** 
     * Move this Renderable with a specified distance.
     */
    public void move(int dx, int dy) {
        if (bounds == null)
            return;
        duringMoving = false;
        switch (selectionPosition) {
            // Do resizing
            case NORTH_EAST :
                // Make sure the minimum size
                bounds.width += dx;
                bounds.height -= dy;
                bounds.y += dy;
                validateMinimumBounds();
                validateBoundsInView();
                break;
            case SOUTH_EAST :
                bounds.width += dx;
                bounds.height += dy;
                validateMinimumBounds();
                validateBoundsInView();
                break;
            case SOUTH_WEST :
                bounds.width -= dx;
                bounds.height += dy;
                bounds.x += dx;
                validateMinimumBounds();
                validateBoundsInView();
                break;
            case NORTH_WEST :
                bounds.width -= dx;
                bounds.height -= dy;
                bounds.x += dx;
                bounds.y += dy;
                validateMinimumBounds();
                validateBoundsInView();
                break;
            // for node feature
            case FEATURE : case STATE :
                moveNodeAttachment(dx, dy);
                break;
            // Treat as line for default
            default :
                bounds.x += dx;
                bounds.y += dy;
                duringMoving = true;
                validateBoundsInView();
                break;
        }
        validatePositionFromBounds();
        // Cannot move widgets to catch up the node's moving.
        // calculation is not fast enough.
        invalidateConnectWidgets();
        // Call this method instead just setting the flag
        //invalidateBounds();
        invalidateTextBounds();
        invalidateNodeAttachments();
    }
    
    /**
     * Make sure this node is in the view.
     */
    protected void validateBoundsInView() {
        if (!ensureBoundsInView)
            return;
        if (bounds.x < pad)
            bounds.x = pad;
        if (bounds.y < pad)
            bounds.y = pad;
    }
    
    protected void moveNodeAttachment(int dx, int dy) {
        // Find which one is picked
        for (NodeAttachment f : attachments) {
            if (f.isSelected()) {
                f.move(dx, dy, bounds);
                return;
            }
        }
    }
    
    public void invalidateNodeAttachments() {
        if (attachments != null && attachments.size() > 0) {
            for (NodeAttachment attachment : attachments)
                attachment.invalidateBounds();
        }
    }
    
    /**
     * Flag to show text bounds should be re-calculated.
     */
    public void invalidateTextBounds() {
        needCheckTextBounds = true;
    }
    
    protected void validatePositionFromBounds() {
        position.x = (int) bounds.getCenterX();
        position.y = (int) bounds.getCenterY();
    }
    
    public void setPosition(Point p) {
    	this.position = p;
    	invalidateConnectWidgets();
    	needCheckBounds = true;
    }
    
    public void setPosition(int x, int y) {
    	if (position == null)
    		position = new Point();
    	position.x = x;
    	position.y = y;
    	invalidateConnectWidgets();
    	needCheckBounds = true;
    }
        
    /**
     * This method is used to validate text bounds for nodes that can display
     * texts. This method should be called when the bounds of the node changes
     * resulting from resizing.
     * @param g
     */
    protected void validateTextBounds(Graphics g) {
        validateTextSize(g);
        setTextPositionFromBounds();
    }
    
    /**
     * Recalculate the bounds.
     * TODO: Bounds and text bounds calculation should be handled by renderer in the future
     * since they should be involved in the rendering process.
     */
    public void validateBounds(Graphics g) {
        // Have to make sure there is bounds available
        if (bounds == null) {
            bounds = new Rectangle();
            initBounds(g); // Get bounds from text as the minimum
        }
        // Have to set to the original transform. Otherwise
        // the first validateTextBounds() works not correctly
        // Use unscaled Graphics
        Graphics2D g2 = (Graphics2D) g;
        // Have to use non-transform Graphics context
        AffineTransform originalAT = g2.getTransform();
        try {
            g2.transform(originalAT.createInverse());
        }
        catch(NoninvertibleTransformException e) {
            System.err.println("Node.valiateBounds(): " + e);
        }
        if (textBounds == null || textBounds.isEmpty()) {
            validateTextBounds(g);
        }
        // Have to make sure the text is there
        if (!needCheckBounds && !needCheckTextBounds) {
            g2.setTransform(originalAT);
            return;
        }
        // Only text bounds need to be checking
        if (needCheckTextBounds) {
            validateTextBounds(g);
            resetLinkWidgetPositions();
            g2.transform(originalAT);
            needCheckTextBounds = false;
            return;
        }
        if (!needCheckBounds)
            return;
        needCheckBounds = false;
        g2.transform(originalAT);
        // calculate link widgets
        resetLinkWidgetPositions();
        // default behaviors
        setTextPositionFromBounds();
    }
    
    /**
     * When a new Node is first created, this method should be called to generate a bounds
     * based on encapsulated text.
     */
    protected void initBounds(Graphics g) {
        validateTextSize(g);
        // Want to make the bounds larger
        int w = (int)(textBounds.width * widthRatioOfBoundsToText) + 1; // Give some extra pixels for double to integer conversion
        if (w < minWidth) // Make sure it takes minum width for layout in a pathway diagram purpose
            w = minWidth;
        int h = (int)(textBounds.height * heightRatioOfBoundsToText) + 1;
        //int w = (int)(textBounds.width * 1.0);
        //int h = (int)(textBounds.height * 1.0);
        bounds.x = position.x - w / 2;
        bounds.y = position.y - h / 2;
        bounds.width = w;
        bounds.height = h;        
    }
    
    /**
     * Use this method to calculate the size of text bounds.
     * @return
     */
    protected void validateTextSize(Graphics g) {
        String tmpName = getDisplayName();
        if (tmpName == null)
            tmpName = "...";
        else if (tmpName.length() == 0)
            tmpName = " "; // For an empty Strings
        validateTextSize(g, tmpName);
    }

    protected void validateTextSize(Graphics g, String tmpName) {
        splitName(tmpName, g);
        // Get width and height from text layouts
        double w = 0.0, h = 0.0;
        for (Iterator it = textLayouts.iterator(); it.hasNext();) {
            TextLayout layout = (TextLayout) it.next();
            if (w < layout.getAdvance())
                w = layout.getAdvance();
            h += (layout.getAscent() + layout.getDescent() + layout.getLeading());
        }
        w += (2 * boundsBuffer); 
        h += (2 * boundsBuffer);
        if (textBounds == null)
            textBounds = new Rectangle();
        textBounds.width = (int) w;
        textBounds.height = (int) h;
    }
    
    /**
     * Check if the wrapped text in a Node is overflowed.
     * @return
     */
    public boolean isTextOverflowed() {
        if (bounds == null || textBounds == null)
            return false; // Cannot determine
        // A minimum rectangle
        if (bounds.getWidth() < textBounds.getWidth() - 2 * boundsBuffer) // Minimum requirement
            return true;
        if (bounds.getHeight() < textBounds.getHeight() - 2 * boundsBuffer)
            return true;
        return false;
    }
    
    protected void resetLinkWidgetPositions() {
        if (!isLinkable)
            return;
        if (linkWidgetPositions == null) {
            linkWidgetPositions = new ArrayList();
            for (int i = 0; i < 4; i ++)
                linkWidgetPositions.add(new Point());
        }
        if (bounds == null)
            return;
        // East
        Point p = (Point) linkWidgetPositions.get(0);
        p.x = bounds.x + bounds.width;
        p.y = bounds.y + bounds.height / 2;
        // South
        p = (Point) linkWidgetPositions.get(1);
        p.x = bounds.x + bounds.width / 2;
        p.y = bounds.y + bounds.height;
        // West
        p = (Point) linkWidgetPositions.get(2);
        p.x = bounds.x;
        p.y = bounds.y + bounds.height / 2;
        // North
        p = (Point) linkWidgetPositions.get(3);
        p.x = bounds.x + bounds.width / 2;
        p.y = bounds.y;
    }
    
    private void splitName(String name, Graphics g) {
        if (duringMoving && textLayouts != null) { // textLayouts should not be null
            // Don't need do anything during moving. 
            // It seems that there is a bug in JDK 1.5 (MacOS), dimension
            // for the generated textlayout is changed somehow during validate
            // GraphEditorPane: variation between big or small.
            duringMoving = false;
            return; 
        }
    	Graphics2D g2 = (Graphics2D) g;
    	Map attributes = new HashMap();
    	// Use a small font
    	attributes.put(TextAttribute.FONT, g.getFont());
    	AttributedString as = new AttributedString(name, attributes);
    	AttributedCharacterIterator aci = as.getIterator();
    	FontRenderContext context = g2.getFontRenderContext();
    	LineBreakMeasurer lbm = new LineBreakMeasurer(aci, context);
    	if (textLayouts == null)
    		textLayouts = new ArrayList();
    	else
    		textLayouts.clear();
    	int end = aci.getEndIndex();
    	int width;
    	if (bounds == null || bounds.width == 0) {
    	    if (Node.width < minWidth)
    	        width = minWidth;
    	    else
    	        width = Node.width;
    	}
    	else
    	    width = bounds.width;
    	while (lbm.getPosition() < end) {
    		TextLayout layout = lbm.nextLayout(width - 2 * boundsBuffer);
    		textLayouts.add(layout);
    	}
    }

    /**
     * Return if the bounds is correct.
     */
    public boolean isBoundsValidate() {
        return !needCheckBounds;
    }
    
    public Object getGKObject() {
        return null;
    }
    
	/* 
	 * @see org.gk.render.Renderable#setDisplayName(java.lang.String)
	 */
	public void setDisplayName(String name) {
	    super.setDisplayName(name);
		if (bounds == null) // For newly create Node
		    invalidateBounds();
		else
		    invalidateTextBounds();
		invalidateConnectWidgets();
		List<Renderable> shortcuts = getShortcuts();
		if (shortcuts == null)
		    return;
		for (Renderable r : shortcuts) {
		    Node shortcut = (Node) r;
		    if (r == this)
		        continue;
		    if (shortcut.getBounds() == null)
		        shortcut.invalidateBounds();
		    else
		        shortcut.invalidateTextBounds();
		    shortcut.invalidateConnectWidgets();
		}
	}
	
	public List<HyperEdge> getConnectedReactions() {
		List<HyperEdge> reactions = new ArrayList<HyperEdge>();
		java.util.List widgets = connectInfo.getConnectWidgets();
		if (widgets != null && widgets.size() > 0) {
			for (Iterator it = widgets.iterator(); it.hasNext();) {
				ConnectWidget widget = (ConnectWidget) it.next();
				if (widget.getEdge() != null) {
					HyperEdge edge = widget.getEdge();
					if (!reactions.contains(edge))
						reactions.add(edge);
				}
			}
		}
		return reactions;
	}
	
	@Override
	public void removeShortcut(Renderable shortcut) {
	    if (shortcuts != null)
	        shortcuts.remove(shortcut);
	}
	
	public List<Renderable> getShortcuts() {
		return shortcuts;
	}
	
	public void setShortcuts(List<Renderable> shortcuts) {
	    this.shortcuts = shortcuts;
	}

    /**
	 * Set this Renderable object to be on/off a editing mode.
	 * @param editing true for being on a eding mode.
	 */
	public void setIsEditing(boolean editing) {
		this.isEditing = editing;
	}
	
	/**
	 * Query if this Renderable object is on editing mode.
	 * @return
	 */
	public boolean isEditing() {
		return this.isEditing;
	}	
	
	public boolean isEditable() {
	    return this.isEditable;
	}
	
	public List getTextLayouts() {
		return this.textLayouts;
	}
	
	public static void setNodeWidth(int w) {
		width = w;
	}
	
	public static int getNodeWidth() {
		return width;
	}
	
	/**
	 * Set the ratio of the width of the Node bounds to the wrapped text.
	 * @param ratio
	 */
	public static void setWidthRatioOfBoundsToText(double ratio) {
	    widthRatioOfBoundsToText = ratio;
	}
	
	public static double getWidthRatioOfBoundsToText() {
	    return widthRatioOfBoundsToText;
	}
	
	/**
	 * Set the ratio of the height of the Node bounds to the wrapped text.
	 * @param ratio
	 */
	public static void setHeightRatioOfBoundsToText(double ratio) {
	    heightRatioOfBoundsToText = ratio;
	}
	
	public static double getHeightRatioOfBoundsToText() {
	    return heightRatioOfBoundsToText;
	}
	
	/**
	 * To support seriazation.
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, 
											ClassNotFoundException {
		in.defaultReadObject();
		invalidateBounds();
	}

	public java.util.List getComponents() {
		return this.components;
	}
	
    
	public void addComponent(Renderable renderable) {
		if (components == null)
			components = new ArrayList();
		components.add(renderable);
	}
	
	public Renderable getComponentByName(String name) {
		if (components == null || components.size() == 0)
			return null;
		Renderable renderable = null;
		for (Iterator it = components.iterator(); it.hasNext();) {
			renderable = (Renderable) it.next();
             if (renderable instanceof FlowLine)
                 continue; // A flow line has null name
			if (renderable.getDisplayName().equals(name))
				return renderable;
		}
		return null;
	}
	
	public Renderable getComponentByID(int id) {
		if (components == null || components.size() == 0)
			return null;
		Renderable renderable = null;
		for (Iterator it = components.iterator(); it.hasNext();) {
			renderable = (Renderable) it.next();
			if (renderable.getID() == id)
				return renderable;
		}
		return null;
	}
	
	public void setStoiBounds(Rectangle r) {
		this.stoiBounds = r;
	}
	
	public Rectangle getStoiBounds() {
		return this.stoiBounds;
	}
	
	public String getType() {
		return "Node";
	}
    
    /**
     * A four element Point list: East, South, West and North.
     * @return
     */
    public List getLinkWidgetPositions() {
        return linkWidgetPositions;
    }
    
    public Point getLinkPoint() {
        return this.position;
    }

    protected void setTextPositionFromBounds() {
        if (bounds == null)
            return; // Wait for bounds is setting.
        if (textBounds == null) {
            textBounds = new Rectangle(bounds);
        }
        else {
            textBounds.x = bounds.x + (bounds.width - textBounds.width) / 2;
            textBounds.y = bounds.y + (bounds.height - textBounds.height) / 2;
        }
    }
    
    protected void validateConnectWidget(ConnectWidget widget) {
        Rectangle bounds = new Rectangle(getBounds());
        if (stoiBounds != null)
            bounds = bounds.union(stoiBounds); // A new bounds is created.
        Point linkPoint = null;
        if (widget.getRole() == HyperEdge.INPUT)
            linkPoint = getLinkPoint();
        else
            linkPoint = getPosition();
        int x0 = linkPoint.x;
        int y0 = linkPoint.y;
        Point controlPoint = widget.getControlPoint();
        double ratio = (double)(controlPoint.y - y0) / (controlPoint.x - x0);
        int x1 = bounds.x;
        int y1 = bounds.y;
        int x2 = bounds.x + bounds.width;
        int y2 = y1;
        Point point = widget.getPoint();
        // Check north
        if (Line2D.linesIntersect(controlPoint.x, controlPoint.y, x0, y0, x1, y1, x2, y2)) {
            point.x = (int)(x0 - 1 / ratio * bounds.height / 2);
            point.y = bounds.y - ConnectWidget.BUFFER;
            if (point.y == controlPoint.y) {
                point.y = bounds.y;
            }
            return;
        }
        // Check east
        x1 = x2;
        y1 = bounds.y + bounds.height;
        if (Line2D.linesIntersect(controlPoint.x, controlPoint.y, x0, y0, x1, y1, x2, y2)) {
            point.x = x1 + ConnectWidget.BUFFER;
            if (point.x == controlPoint.x)
                point.x = x1;
            point.y = (int)(y0 + ratio * bounds.width / 2);
            return;
        }
        // Check south
        x2 = bounds.x;
        y2 = y1;
        if (Line2D.linesIntersect(controlPoint.x, controlPoint.y, x0, y0, x1, y1, x2, y2)) {
            point.x = (int)(x0 + 1 / ratio * bounds.height / 2);
            point.y = y2 + ConnectWidget.BUFFER;
            if (point.y == controlPoint.y)
                point.y = y2;
            return;
        }
        // Check west
        x1 = x2;
        y1 = bounds.y;
        if (Line2D.linesIntersect(controlPoint.x, controlPoint.y, x0, y0, x1, y1, x2, y2)) {
            point.x = x1 - ConnectWidget.BUFFER;
            if (point.x == controlPoint.x)
                point.x = x1;
            point.y = (int)(y0 - ratio * bounds.width / 2);
            return;
        }
        // ControlPoint might be in the lines of bounds or in the
        // bounds.
        if ((controlPoint.x > x0) && (controlPoint.y < y0)) {
            point.x = bounds.x + bounds.width;
            point.y = controlPoint.y;
        }
        else if ((controlPoint.x > x0) && (controlPoint.y > y0)) {
            point.x = controlPoint.x;
            point.y = bounds.y + bounds.height;
        }
        else if ((controlPoint.x < x0) && (controlPoint.y > y0)) {
            point.x = bounds.x;
            point.y = controlPoint.y;
        }
        else if ((controlPoint.x < x0) && (controlPoint.y < y0)) {
            point.x = controlPoint.x;
            point.y = bounds.y;
        }
        else {
            // Choose a default point
            point.x = bounds.x;
            point.y = bounds.y;
        }
    }
    
    @Override
    public void setContainer(Renderable renderable) {
        super.setContainer(renderable);
        // A complex component cannot be used to link to other node.
        if (renderable instanceof RenderableComplex) {
            if (linkWidgetPositions != null) 
                linkWidgetPositions = null;
            //isLinkable = false;
        }
        else // in cases a node is moved out of a complex
            resetLinkWidgetPositions();
    }
    
    protected void generateShortcut(Node shortcut) {
        // Make all attributes copy to this target
        shortcut.attributes = this.attributes;
        // Need to duplicate NodeAttachments if any
        if (attachments != null) {
            List<NodeAttachment> copy = new ArrayList<NodeAttachment>();
            for (NodeAttachment attachment : attachments) {
                NodeAttachment tmp = attachment.duplicate();
                copy.add(tmp);
            }
            shortcut.attachments = copy;
        }
        // Use the same renderer
        shortcut.renderer = renderer;
        // Copy position info
        if (getPosition() != null) {
            Point p = new Point(getPosition());
            p.x += 20;
            p.y += 20;
            shortcut.setPosition(p);
        }
        shortcut.setBackgroundColor(getBackgroundColor());
        shortcut.setForegroundColor(getForegroundColor());
        if (isMultimerFormable)
            shortcut.setMultimerMonomerNumber(multimerMonomerNumber);
        // use an original bounds
        if (bounds != null)
            shortcut.bounds = new Rectangle(bounds);
        shortcut.invalidateBounds();
        if (shortcuts == null) {
            shortcuts = new ArrayList<Renderable>();
            shortcuts.add(this);
        }
        // Copy reactomeId if any. However, two shortcuts can have different
        // reactome id (e.g. in different compartments).
        shortcut.setReactomeId(getReactomeId());
        shortcut.isVisible = isVisible;
        shortcuts.add(shortcut);
        shortcut.shortcuts = shortcuts;
    }
    
    /**
     * Set if a RenderableFeature can be attached to this Node object.
     * @param isAddable
     */
    public void setIsFeatureAddable(boolean isAddable) {
        this.isFeatureAddable = isAddable;
    }
    
    /**
     * Check if a RenderableFeature can be attached to this Node object.
     * @return
     */
    public boolean isFeatureAddable() {
        return this.isFeatureAddable;
    }
    
    /**
     * Add a RenderableFeature to this Node object.
     * @param feature
     */
    public void addFeature(RenderableFeature feature) {
        // Have to make sure a RenderableFeature can be added to this Node.
        if (!isFeatureAddable)
            return;
        addNodeAttachment(feature);
    }
    
    /**
     * Add a feature to this Node only. If this Node has other shortcuts,
     * the added feature will not be propagated to other Nodes.
     * @param feature
     */
    public void addFeatureLocally(RenderableFeature feature) {
        if (!isFeatureAddable)
            return;
        addNodeAttachmentLocally(feature);
    }
    
    /**
     * The values set in this method will not be popped up to shortcuts if this is
     * a target or to target or other shortcuts if this is a shortcut. This method is
     * basically used to do a simple setting.
     * @param attachments
     */
    public void setNodeAttachmentsLocally(List<NodeAttachment> attachments) {
        this.attachments = attachments;
    }
    
    /**
     * Remove the selected NodeAttachment.
     */
    public boolean removeSelectedAttachment() {
        // Find the selected feature index
        int selectedId = -1;
        boolean rtn = false;
        for (NodeAttachment attachment : attachments) {
            if (attachment.isSelected()) {
                selectedId = attachment.getTrackId();
                break;
            }
        }
        // Nothing to delete
        if (selectedId == -1)
            return false;
        // Need to check shortcuts
        Node target = RenderUtility.getShortcutTarget(this);
        // Target and shortcuts may have different settings of features and states
        if (target.attachments != null) {
            for (Iterator<NodeAttachment> it = target.attachments.iterator(); it.hasNext();) {
                NodeAttachment attachment = it.next();
                if (attachment.getTrackId() == selectedId) {
                    it.remove();
                    break;
                }
            }
        }
        if (target.getShortcuts() != null) {
            for (Iterator it = target.getShortcuts().iterator(); it.hasNext();) {
                Node shortcut = (Node) it.next();
                if (shortcut.attachments == null || shortcut.attachments.size() == 0)
                    continue;
                for (Iterator<NodeAttachment> it1 = shortcut.attachments.iterator(); it1.hasNext();) {
                    NodeAttachment attachment = it1.next();
                    if (attachment.getTrackId() == selectedId) {
                        it1.remove();
                        break;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Remove the selected NodeAttachment in this Node only. Other shortcuts to this
     * Node should still keep the selected node.
     * @return
     */
    public boolean removeSelectedAttachmentLocally() {
        for (Iterator<NodeAttachment> it = attachments.iterator();
             it.hasNext();) {
            NodeAttachment attachment = it.next();
            if (attachment.isSelected()) {
                it.remove();
                return true;
            }
        }
        return false;
    }
    
    public void removeNodeAttachment(NodeAttachment attachment) {
        for (Iterator<NodeAttachment> it = attachments.iterator(); it.hasNext();) {
            if (attachment == it.next()) {
                it.remove();
                return;
            }
        }
    }
    
    /**
     * Return the selection position of this Node object.
     * @return one of values in enum SelectionPosition
     */
    public SelectionPosition getSelectionPosition() {
        return this.selectionPosition;
    }
    
    public void setSelectionPosition(SelectionPosition pos) {
        this.selectionPosition = pos;
    }
    
    
    public void setIsStateAddable(boolean isAddable) {
        this.isStateAddable = isAddable;
    }
    
    public boolean isStateAddable() {
        return this.isStateAddable;
    }
    
    public void addState(RenderableState state) {
        if (!isStateAddable)
            return ;
        addNodeAttachment(state);
    }
    
    /**
     * Add a RenderbleState to this Node object only. Any shortcuts to this Node
     * will not get the passed RenderableState object.
     * @param state
     */
    public void addStateLocally(RenderableState state) {
        if (!isStateAddable)
            return;
        addNodeAttachmentLocally(state);
    }
    
    /**
     * Use a method to add NodeAttachment esp to handle shortcuts.
     * @param attachment
     */
    private void addNodeAttachment(NodeAttachment attachment) {
        int trackId = generateUniqueAttachmentId();
        attachment.setTrackId(trackId);
        Node target = RenderUtility.getShortcutTarget(this);
        if (target.attachments == null)
            target.attachments = new ArrayList<NodeAttachment>();
        target.attachments.add(attachment);
        // Need to handle shortcuts
        List<Renderable> shortcuts = getShortcuts();
        if (shortcuts != null) {
            for (Renderable r : shortcuts) {
                if (r == this)
                    continue;
                Node node = (Node) r;
                if (node.attachments == null)
                    node.attachments = new ArrayList<NodeAttachment>();
                node.attachments.add(attachment.duplicate());
            }
        }
    }
    
    /**
     * Search all NodeAttachments attached to the target and its shortcuts to
     * find the maximum id.
     * @return
     */
    private int generateUniqueAttachmentId() {
        Node target = RenderUtility.getShortcutTarget(this);
        int max = -1; // The minum should be 0
        if (target.attachments != null) {
            for (NodeAttachment attachment : target.attachments) {
                if (attachment.getTrackId() > max)
                    max = attachment.getTrackId();
            }
        }
        List shortcuts = target.getShortcuts();
        if (shortcuts != null && shortcuts.size() > 0) {
            for (Iterator it = shortcuts.iterator(); it.hasNext();) {
                Node node = (Node) it.next();
                if (node.attachments != null) {
                    for (NodeAttachment attachment : node.attachments) {
                        if (attachment.getTrackId() > max)
                            max = attachment.getTrackId();
                    }
                }
            }
        }
        max ++;
        return max;
    }
    
    /**
     * A helper method to add a NodeAttachment to this Node only.
     * @param attachment
     */
    private void addNodeAttachmentLocally(NodeAttachment attachment) {
        int trackId = generateUniqueAttachmentId();
        attachment.setTrackId(trackId);
        if (attachments == null)
            attachments = new ArrayList<NodeAttachment>();
        attachments.add(attachment);
    }
    
    public List<NodeAttachment> getNodeAttachments() {
        return attachments;
    }
    
}
