/*
 * RenderablePathway.java
 *
 * Created on June 12, 2003, 2:53 PM
 */

package org.gk.render;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;

/**
 * This class describes the renderable Pathways.
 * @author  wgm
 */
public class RenderablePathway extends ContainerNode {
    // Used to control if compartment names should be hidden
    private boolean hideCompartmentInNode;
    private int nextID = -1;
    // A RenderablePathway actually is correspondent to PathwayDiagram instance
    // in the Reactome schema. One PathwayDiagram instance may be used by more than
    // one Pathway instance.
    private Long reactomeDiagramId;
    // Used to handle two-layer drawing for disease pathways
    private List<Renderable> bgComponents; // For components drawn as background
    private List<Renderable> fgComponents; // For components drawn as foreground
    
    /** Creates a new instance of RenderablePathway */
    public RenderablePathway() {
        boundsBuffer = 8;
        hideComponents = true; // Used as default
        isTransferrable = false;
        isLinkable = false;
    }
    
    /**
     * Set the corresponding PathwayDiagram DB_ID for this RenderablePathway.
     * @param dbId
     */
    public void setReactomeDiagramId(Long dbId) {
        this.reactomeDiagramId = dbId;
    }
    
    public Long getReactomeDiagramId() {
        // Check if there is a PathwayDiagram instance set
        GKInstance instance = getInstance();
        if (instance != null && instance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram))
            return instance.getDBID();
        return this.reactomeDiagramId;
    }
    
    /**
     * This method has been deprecated in this RenderablePathway class. A RenderablePathway
     * may be corresponding to more than one Pathway instance. Use {@link #getReactomeDiagramId()} instead.
     */
    @Override
    @Deprecated
    public Long getReactomeId() {
        return super.getReactomeId();
    }

    /**
     * Set the flag to hide compartment names in nodes contained by this RenderablePathway
     * object. 
     * @param hide
     */
    public void setHideCompartmentInNode(boolean hide) {
        this.hideCompartmentInNode = hide;
        if (components == null || components.size() == 0 || !hide)
            return;
    }
    
    public boolean getHideCompartmentInNode() {
        return this.hideCompartmentInNode;
    }
    
    @Override
    public void setIsSelected(boolean isSelected) {
        super.setIsSelected(isSelected);
        // Highlight all contained events to show the contained
        // reactions.
        if (components != null) {
            for (Iterator it = components.iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof HyperEdge) {
                    ((HyperEdge)obj).setIsHighlighted(isSelected);
                }
            }
        }
    }

    public RenderablePathway(String displayName) {
    	this();
        setDisplayName(displayName);
    }
    
    /**
     * Layout RenderablePathway automatically.
     * @param type one of constants from GraphLayoutEngine:     
     * HIERARCHICAL_LAYOUT, FORCE_DIRECTED_LAYOUT.
     */
	public void layout(int type) {
        PathwayLayoutHelper helper = new PathwayLayoutHelper(this);
        helper.layout(type);
    }

	public Renderable generateShortcut() {
        RenderablePathway shortcut = new RenderablePathway();
	    generateShortcut(shortcut);
		return shortcut;
	}
	
	public int generateUniqueID() {
		++nextID;
		return nextID;
	}

	/**
	 * Reset the nextID so that the ID assigned to next Renderable object
	 * is unique.
	 */
	public void resetNextID() {
		// Find the highest id
		nextID = -1;
		if (components == null || components.size() == 0)
			return;
		for (Renderable r : components) {
		    if (r.getID() > nextID) 
		        nextID = r.getID();
		}
	}
	
	public String getType() {
	    return "Pathway";
	}
	
	@Override
    public void select(Rectangle rect) {
        if (!isVisible)
            return;
        super.select(rect);
    }

    public boolean isPicked(Point p) {
	    // Block selection if it is not visible
	    if (!isVisible)
	        return false;
        // Check the distance between P and the bound
        // east
        int x1 = bounds.x + bounds.width;
        int y1 = bounds.y;
        int x2 = x1;
        int y2 = bounds.y + bounds.height;
        double distSqr = Line2D.ptSegDistSq(x1, y1, x2, y2, p.x, p.y);
        if (distSqr < SENSING_DISTANCE_SQ) {
            return true;
        }
        // south
        x1 = x2;
        y1 = y2;
        x2 = bounds.x;
        y2 = y1;
        distSqr = Line2D.ptSegDistSq(x1, y1, x2, y2, p.x, p.y);
        if (distSqr < SENSING_DISTANCE_SQ) {
            return true;
        }
        // west
        x1 = x2;
        y1 = y2;
        x2 = x1;
        y2 = bounds.y;
        distSqr = Line2D.ptSegDistSq(x1, y1, x2, y2, p.x, p.y);
        if (distSqr < SENSING_DISTANCE_SQ) {
            return true;
        }
        // north
        x1 = x2;
        y1 = y2;
        x2 = bounds.x + bounds.width;
        y2 = y1;
        distSqr = Line2D.ptSegDistSq(x1, y1, x2, y2, p.x, p.y);
        if (distSqr < SENSING_DISTANCE_SQ) {
            return true;
        }
        return false;
    }
    
    protected void resetLinkWidgetPositions() {
        if (linkWidgetPositions == null) {
            linkWidgetPositions = new ArrayList();
            for (int i = 0; i < 4; i ++)
                linkWidgetPositions.add(null);
        }
    }

    public List getLinkWidgetPositions() {
        resetLinkWidgetPositions();
        return super.getLinkWidgetPositions();
    }
    
    @Override
    public boolean isAssignable(Renderable r) {
        return false;
    }

    @Override
    public void addComponent(Renderable renderable) {
        super.addComponent(renderable);
        needCheckBounds = true;
    }

    @Override
    public void validateBounds(Graphics g) {
        if (!needCheckBounds)
            return;
        if (bounds == null)
            bounds = new Rectangle();
        if (components == null || components.size() == 0)
            return;
        // Initialize by finding a no-empty components in case
        // the first component has no bounds
        int next = 0;
        for (int i = 0; i < components.size(); i++) {
            Renderable r = (Renderable) components.get(0);
            Rectangle rB = r.getBounds();
            if (rB != null) {
                bounds.x = rB.x;
                bounds.y = rB.y;
                bounds.width = rB.width;
                bounds.height = rB.height;
                next = i + 1;
                break;
            }
        }
        for (int i = next; i < components.size(); i++) {
            Renderable r = (Renderable) components.get(i);
            Rectangle rB = r.getBounds();
            if (rB == null)
                continue;
            int x1 = Math.min(bounds.x, rB.x);
            int x2 = Math.max(bounds.x + bounds.width, rB.x + rB.width);
            int y1 = Math.min(bounds.y, rB.y);
            int y2 = Math.max(bounds.y + bounds.height, rB.y + rB.height);
            bounds.x = x1;
            bounds.y = y1;
            bounds.width = x2 - x1;
            bounds.height = y2 - y1;
        }
    }

    /**
     * Check if the passed Renderable is contained by this RenderablePathway. A Renderable object
     * can be contained by several RenderablePathway. So a simple container based checking, which
     * is implemeneted in ContainerNode, cannot work in case this passed Renderable object is contained
     * by several pathways.
     */
    @Override
    public boolean contains(Renderable r) {
        if (components == null)
            return false;
        return components.contains(r);
    }
    
    public void setBgComponents(List<Renderable> comps) {
        this.bgComponents = comps;
    }
    
    public List<Renderable> getBgCompoennts() {
        return bgComponents;
    }
    
    public void setFgComponents(List<Renderable> comps) {
        this.fgComponents = comps;
    }
    
    public List<Renderable> getFgComponents() {
        return fgComponents;
    }
    
    // Shortcut to pathway is not supported!
}