/*
 * GraphObject.java
 *
 * Created on June 23, 2003, 3:13 PM
 */

package org.gk.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;

/**
 * The super class for all objects that can be displayed in a graph editor
 * pane.
 * @author  wgm
 */
public abstract class Renderable implements Serializable, RenderablePropertyNames {
    // To control bounds
    protected int pad = 4;
    protected Point position;
    protected ConnectInfo connectInfo;
    protected boolean isSelected;
    private boolean isHighlighted;
    // Whole bounds for this Renderable
    protected Rectangle bounds;   
    protected transient boolean needCheckBounds;
    protected transient boolean needCheckTextBounds;
    // Text bounds only for display name.
    protected Rectangle textBounds;
    // Another renderable contains this Renderable. This field is transient
    // so that container will not be output during DnD.
    protected transient Renderable container;
    // Used to store all attribute values
    protected Map attributes;
    // For property change event
    private java.util.List propertyChangeListeners;
    // Color mode
    protected Color foregroundColor;
    protected Color backgroundColor;
    protected Color lineColor;
    // The unique id
    private int id = -1; // Default
    // Used for rendering
    protected Renderer renderer;
    protected final int SENSING_DISTANCE_SQ = 16;
    // A flag to control if this Renderable should be visible
    protected boolean isVisible = true;
    // A flag to control this Renderable can be copy/pasted
    protected boolean isTransferrable = true;
    // Move DB_ID from the shared attributes among shortcuts to here
    // So that two shortcuts in different compartments can have different
    // DB_IDs. This is a try to make it workable for the current Reactome
    // date model.
    private Long reactomeId;
    // Use as the back-up model
    private GKInstance instance;
    // As with reactomeId, I have to move localization out from the attributes
    // so that two shortcuts in different compartments can have different values
    private String localization;
    protected Float lineWidth;
    
    public Renderable() {
        init();
    }
    
    private void init() {
        // A flyweight design pattern is used here
        setRenderer(RendererFactory.getFactory().getRenderer(this));
        setID(RenderableRegistry.getRegistry().nextId());
        attributes = new HashMap();
    }
    
    public void setRenderer(Renderer r) {
        this.renderer = r;
    }
    
    public Renderer getRenderer() {
        return this.renderer;
    }
    
    public void render(Graphics g) {
        if (renderer != null) {
            renderer.setRenderable(this);
            renderer.render(g);
        }
    }
    
    public boolean isTransferrable() {
        return this.isTransferrable;
    }
    
    public Rectangle getTextBounds() {
        return textBounds;
    }
    
    public void setTextBounds(Rectangle rect) {
        this.textBounds = rect;
    }
    
    public void setLineColor(Color color) {
        this.lineColor = color;
    }
    
    public Color getLineColor() {
        return this.lineColor;
    }
    
	public void setContainer(Renderable renderable) {
		this.container = renderable;
//		if (container != null) {
//			String taxon = (String)container.getAttributeValue("taxon");
//			if (taxon != null)
//				setAttributeValue("taxon", taxon);
//		}
	}
    
	public void setInstance(GKInstance instance) {
	    this.instance = instance;
	}
	
	public GKInstance getInstance() {
	    return this.instance;
	}
	
	public void setReactomeId(Long id) {
	    this.reactomeId = id;
	}
	
	public Long getReactomeId() {
	    if (instance != null)
	        return instance.getDBID();
	    return this.reactomeId;
	}
	
	public void setLocalization(String localization) {
	    this.localization = localization;
	}
	
	public String getLocalization() {
	    return this.localization;
	}
	
    public Renderable getContainer() {
    	return this.container;
    }
    
    public void setPosition(Point p) {
        this.position = p;
    }
    
    public void setPosition(int x, int y) {
    	if (position == null)
    		position = new Point();
    	position.x = x;
    	position.y = y;
    }
    
    public Point getPosition() {
        return this.position;
    }
    
    /**
     * Get the bounding rectangle.
     */
    public Rectangle getBounds() {
        return this.bounds;
    }
    
    /**
     * Check if this Renderable should be picked at the specified Point p.
     * @param p the checking Point.
     */
    public abstract boolean isPicked(Point p);
    
    /**
     * Check if this Renderable object can be picked. This method is different from isPicked(Point)
     * for HyperEdge. This method will not make any changes to the internal states of a Renderable object.
     * For checking if a passed point can be picked by a Renderable object, this method should be used.
     * @param p
     * @return
     */
    public boolean canBePicked(Point p) {
        return isPicked(p);
    }
    
    /**
     * Check if this Renderable is selected.
     */
    public boolean isSelected() {
        return this.isSelected;
    }
    
    /**
     * Mark if this Renderable is selected.
     */
    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
    
    /**
     * Use a Rectangle to select this Renderable. If this Renderable
     * can be selected, its isSelected flag will be true. Otherwise,
     * its isSelected flag will be false.
     * @param rect
     */
    public void select(Rectangle rect) {
        if (!isVisible)
            return; // Block this option if it is not visible.
        isSelected = rect.contains(getPosition());
    }
    
    public boolean isHighlighted() {
        return this.isHighlighted;
    }
    
    public void setIsHighlighted(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }
    
    /**
     * Move this Renderable with a specified distance.
     */
    public abstract void move(int dx, int dy);
    
    
    
    /**
     * Add a ConnectWidget that containts a connect information.
     */
    public void addConnectWidget(ConnectWidget widget) {
        connectInfo.addConnectWidget(widget);
    }
    
    public void removeConnectWidget(ConnectWidget widget) {
        connectInfo.removeConnectWidget(widget);
    }
    
    public void clearConnectWidgets() {
        connectInfo.clear();
    }
    
    public void invalidateConnectWidgets() {
        connectInfo.invalidate();
    }
    
    /**
     * Use this method to invalidate the bounds of the node so that the bounds can be validated late.
     */
    public void invalidateBounds() {
        needCheckBounds = true;
        if (getContainer() != null && getContainer() instanceof RenderablePathway) {
            getContainer().invalidateBounds();
        }
    }
    
    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }
    
    public void setTextPosition(int x, int y) {
        
    }
    
    public void setConnectInfo(ConnectInfo connectInfo) {
        this.connectInfo = connectInfo;
    }
    
    public ConnectInfo getConnectInfo() {
    	return this.connectInfo;
    }
    
    public abstract java.util.List getComponents();
    
    public void addComponent(Renderable renderable) {
    }
    
    /**
     * Remove a specified Renderable.
     * @param renderable the Renderable to be removed.
     * @return the actual removed Object. It might be a Shortcut.
     */
    public Object removeComponent(Renderable renderable) {
    	return null;
    }
    
    public String toString() {
    	return getDisplayName();
    }

	public void setDisplayName(String name) {
        attributes.put(DISPLAY_NAME, name);
	}

	public String getDisplayName() {
        return (String) attributes.get(DISPLAY_NAME);
	}
	
	public Renderable generateShortcut() {
		return null;
	}
	
	public void removeShortcut(Renderable shortcut) {
	}
	
	public List<Renderable> getShortcuts() {
		return null;
	}

	public void setShortcuts(List<Renderable> shortcuts) {
	    
	}
	
	public void setAttributeValue(String attributeName, Object value) {
        // Use String for these types
        if (value instanceof Integer ||
            value instanceof Long ||
            value instanceof Short ||
            value instanceof Boolean)
            value = value.toString();
		attributes.put(attributeName, value);
	}

	public Object getAttributeValue(String attributeName) {
		return attributes.get(attributeName);
	}
    
    public Map getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map attributes) {
        this.attributes = attributes;
    }
	
	public Renderable getComponentByName(String name) {
		return null;
	}
	
	public Renderable getComponentByID(int id) {
		return null;
	}
	
	public void setForegroundColor(Color color) {
		this.foregroundColor = color;
	}
	
	public Color getForegroundColor() {
		return this.foregroundColor;
	}
	
	public void setBackgroundColor(Color color) {
		this.backgroundColor = color;
	}
	
	public Color getBackgroundColor() {
		return this.backgroundColor;
	}
	
	// The following three methods for property event.
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		if (propertyChangeListeners == null)
			propertyChangeListeners = new ArrayList();
		if (!propertyChangeListeners.contains(l))
			propertyChangeListeners.add(l);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener l) {
		if (propertyChangeListeners != null)
			propertyChangeListeners.remove(l);
		if (propertyChangeListeners != null && propertyChangeListeners.size() == 0)
			propertyChangeListeners = null; // To save resources.
	}
	
	protected void firePropertyChange(PropertyChangeEvent e) {
		if (propertyChangeListeners != null) {
			PropertyChangeListener l = null;
			for (Iterator it = propertyChangeListeners.iterator(); it.hasNext();) {
				l = (PropertyChangeListener) it.next();
				l.propertyChange(e);
			}
		}
	}
	
	/**
	 * Set the unique id for this Renderable. The id of a Renderable object 
	 * is unique in the whole process.
	 * @param id
	 */
	public void setID(int id) {
		this.id = id;
	}
	
	/**
	 * Get the unique id for this Renderable object.
	 * @return
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * The default implementaion always return 0. The topmost Renderable object
	 * should implement this method to assign unique id to its descedants.
	 * @return always return 0.
	 */
	public int generateUniqueID() {
		return 0;
	}
	
	/**
	 * A type name. A subclass should implement this method.
	 */
	public abstract String getType();

    public void setIsChanged(boolean isChanged) {
        attributes.put("isChanged", isChanged + "");
    }

    public boolean isChanged() {
        String value = (String) attributes.get("isChanged");
        if (value == null)
            return false;
        return Boolean.valueOf(value).booleanValue();
    }

    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    public void setLineWidth(Float lineWidth) {
    	this.lineWidth = lineWidth;
    }

    public Float getLineWidth() {
    	return this.lineWidth;
    }
}
