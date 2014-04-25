/*
 * Created on Oct 8, 2003
 */
package org.gk.render;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class is used for name registry. This is a singleton. One application
 * should have only one regirsty.
 * @author wgm
 */
public class RenderableRegistry {
	private static RenderableRegistry instance;
	// For event firing
	private PropertyChangeSupport propSupport;
    // Used to keep track ids
    private int nextId = 0;
    // Renderable may have several different Renderable objects that have the same
    // name. In this case, only one of them is registered, which should be the first
    // one usually. 
    private Map<String, Renderable> nameToObject;
	
	private RenderableRegistry() {
		propSupport = new PropertyChangeSupport(this);
		nameToObject = new HashMap<String, Renderable>();
	}
    
    /**
     * Call this method to get the next unused id. The current id will be increment
     * after this method call.
     * @return
     */
    public int nextId() {
        nextId ++;
        return nextId;
    }
    
    public void resetNextId(int id) {
        nextId = id;
    }
    
    public void open(RenderablePathway container) {
        clear();
        registerAll(container);
        resetNextIdFromPathway(container);
    }
    
	public static RenderableRegistry getRegistry() {
		if (instance == null)
			instance = new RenderableRegistry();
		return instance;
	}
	
	/**
	 * Reset the nextId from a RenderablePathway. This RenderablePathway should
	 * contain all displayed Renderable objects.
	 * @param pathway
	 */
	public void resetNextIdFromPathway(RenderablePathway pathway) {
	    int max = 0;
	    if (pathway.getComponents() != null) {
	        for (Iterator it = pathway.getComponents().iterator(); it.hasNext();) {
	            Renderable r = (Renderable) it.next();
	            if (r.getID() > max)
	                max = r.getID();
	        }
	    }
	    nextId = max;
	}
	
	/**
	 * Reset all ids used by Renderable objects contained by the passed RenderablePathway.
	 * All Renderable objects should be contained directly by the passed pathway.
	 * @param pathway
	 */
	public void resetAllIdsInPathway(Renderable pathway) {
	    int id = 0;
	    for (Iterator it = pathway.getComponents().iterator(); it.hasNext();) {
	        Renderable r = (Renderable) it.next();
	        id ++;
	        r.setID(id);
	    }
	    nextId = id;
	}
	
	/**
     * Get a unique name for a specified Renderable in a container.
     */
    public String generateUniqueName(Renderable renderable) {
        String name = renderable.getDisplayName();
        if (name == null)
            name = renderable.getType();
        String rtnName = name;
        int count = 1;
        while (nameToObject.containsKey(rtnName)) {
            rtnName = name + count;
            count ++;
        }
        return rtnName;
    }
    
	/**
	 * Registry a Renderable Object.
	 * @param renderable 
	 */
	public void add(Renderable renderable) {
	    // Don't register note so that it can have multiple copies having same display name.
	    if (blockRegister(renderable))
	        return;
		if (nameToObject.containsKey(renderable.getDisplayName()))
		    return;
		nameToObject.put(renderable.getDisplayName(),
		                 renderable);
	}
	
	/**
	 * De-register a Renderbale object.
	 * @param renderable
	 */
	public boolean remove(Renderable renderable) {
	    renderable.removeShortcut(renderable);
	    // Make sure a target is not removed if it contains shortcuts
	    Renderable target = nameToObject.get(renderable.getDisplayName());
	    if (target == renderable) {
	        // Need to switch to another Renderable object with the same name
	        List<Renderable> shortcuts = renderable.getShortcuts();
	        Renderable shortcut = null;
	        if (shortcuts != null) {
	            for (Renderable r : shortcuts) {
	                if (r != renderable) {
	                    shortcut = r;
	                    break;
	                }
	            }
	        }
	        if (shortcut != null) 
	            nameToObject.put(shortcut.getDisplayName(),
	                             shortcut);
	        else {
	            nameToObject.remove(renderable.getDisplayName());
	            return true;
	        }
	    }
	    return false;
	}

	public void remove(Renderable renderable, 
	                   boolean recursively) {
		if (recursively) {
			List current = new ArrayList();
			List next = new ArrayList();
			current.add(renderable);
			Renderable r = null;
			while (current.size() > 0) {
			    for (Iterator it = current.iterator(); it.hasNext();) {
			        r = (Renderable) it.next();
			        if (r instanceof RenderableReaction)
			            continue;
			        if (remove(r)) {
			            if (r.getComponents() != null)
			                next.addAll(r.getComponents());
			        }
			    }
				current.clear();
				current.addAll(next);
				next.clear();
			}
		}
		else {
			remove(renderable);
		}
	}
	
	/**
	 * Check if a passed Renderable object is a registered object. A
	 * Renderable object may not be registered since another Renderable object
	 * having same name has be registered first.
	 * @param renderable
	 * @return
	 */
	public boolean isRegistered(Renderable renderable) {
	    Renderable r = nameToObject.get(renderable.getDisplayName());
	    return r == renderable;
	}
	
	public void clear() {
		nameToObject.clear();
        nextId = 0;
	}
	
	public Collection<Renderable> getAllRenderables() {
	    return nameToObject.values();
	}
	
	/**
	 * Change the name of a Renderable.
	 * @param r the Renderable object that uses the new name. 
	 * @param oldName
	 */
	public void changeName(Renderable r, 
	                       String oldName) {
	    // Only registerable renderable should be run change name.
	    if (blockRegister(r))
	        return;
	    nameToObject.remove(oldName);
	    nameToObject.put(r.getDisplayName(),
		                 r);
	}
    
    /**
     * Use this method to register a pathway and its contained pathways and
     * reactions, and other descendents.
     * @param pathway
     */
    public void registerAll(Renderable pathway) {
        // Register all Renderable objects in the process.
        java.util.List renderables = RenderUtility.getAllDescendents(pathway);
        for (Iterator it = renderables.iterator(); it.hasNext();) {
            Renderable renderable = (Renderable) it.next();
            add(renderable);
        }
        // Don't forget to add this passed pathway too.
        add(pathway);
    }
    
    /**
     * Un-register based on the passed name.
     * @param name
     */
    public void unregister(String name) {
        nameToObject.remove(name);
    }
    
    /**
     * Check if there is any Renderable object with the passed name registered already.
     * @param name
     * @return
     */
    public boolean contains(String name) {
        return nameToObject.containsKey(name);
    }
    
    /**
     * Get a Renderable object with the passed name.
     * @param name
     * @return
     */
    public Renderable getSingleObject(String name) {
        return nameToObject.get(name);
    }
    
    /**
     * Check if a Renderable object should be blocked for registration.
     * @param r
     * @return
     */
    private boolean blockRegister(Renderable r) {
        if (r instanceof Note)
            return true;
        if (r instanceof FlowLine &&
            r.getDisplayName() == null) // No FlowLine
            return true;
        return false;
    }
    
	/**
	 * Add a propertyChangeListener to this registry. There are three PropertyChangeEvent can be 
	 * fired from this object: "shortcutToTarget" with shortcut (Renderable object) as oldValue 
	 * and target (another Renderable object) as newValue, "precedingEvent" with null oldValue 
	 * and the changed Renderable object as newValue; "delete" with null as oldValue and deleted 
	 * Renderable object as newValue.
	 * @param l
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		propSupport.addPropertyChangeListener(l);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener l) {
		propSupport.removePropertyChangeListener(l);
	}
	
	public void firePropertyChange(String propName, Object oldValue, Object newValue) {
		propSupport.firePropertyChange(propName, oldValue, newValue);
	}

    public void assignUniqueID(Renderable renderable) {
        int id = getRegistry().nextId();
    	renderable.setID(id);
    }
}
