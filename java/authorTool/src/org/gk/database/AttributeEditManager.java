/*
 * Created on Dec 10, 2003
 */
package org.gk.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;

/**
 * This class is used as a router for attribute editing. This is a singleton.
 * @author wugm
 */
public class AttributeEditManager {
	
	private java.util.List editListeners;
	// To reuse the object initialization
	private AttributeEditEvent event;
	// The singleton.
	private static AttributeEditManager manager;
	
	public static AttributeEditManager getManager(){
		if (manager == null)
			manager = new AttributeEditManager();
		return manager;
	}

	private AttributeEditManager() {
	}
	
	public void addAttributeEditListener(AttributeEditListener l) {
		if (editListeners == null)
			editListeners = new ArrayList();
		if (!editListeners.contains(l))
			editListeners.add(l);
	}
	
	public void removeAttributeEditListener(AttributeEditListener l) {
		if (editListeners != null)
			editListeners.remove(l);
	}
	
	public void attributeEdit(Instance instance, String attName) {
		if (event == null)
			event = new AttributeEditEvent(this);
		event.setEditingInstance((GKInstance)instance);
		event.setAttributeName(attName);
		attributeEdit(event);
	}
	
	public void attributeEdit(Instance instance) {
		if (event == null)
			event = new AttributeEditEvent(this);
		event.setEditingInstance((GKInstance)instance);
		event.setAttributeName(null);
		attributeEdit(event);
	}
	
	public void attributeEdit(AttributeEditEvent e) {
        // Have to mark the editing instance. Call this first before fire events since
        // some changes depends on this dirty setting. Theoreticaly, the calling sequence
        // should not be an issue. PropertyChangeListener should be added to XMLFileAdaptor
        // to listen to the dirt flag setting.
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        Instance instance = e.getEditingInstance();
        fileAdaptor.markAsDirty(instance);
		if (editListeners != null) {
			AttributeEditListener l = null;
			for (Iterator it = editListeners.iterator(); it.hasNext();) {
				l = (AttributeEditListener) it.next();
				l.attributeEdit(e);
			}
		}
	}
    
    /**
     * A refactor method to make sure _displayName is still valid after some editing.
     * @param instance
     * @return true for name has been changed.
     */
    public boolean validateDisplayName(GKInstance instance) {
        if (instance == null) // It might be null
            return false; 
        String newName = InstanceDisplayNameGenerator.generateDisplayName(instance);
        String oldName = instance.getDisplayName();
        boolean displayNameChanged = false;
        if (newName != null && oldName != null && !newName.equals(oldName)) {
            instance.setDisplayName(newName);
            displayNameChanged = true;
        }
        else if (newName != null && oldName == null) {
            instance.setDisplayName(newName);
            displayNameChanged = true;
        }
        if (displayNameChanged) {
            AttributeEditManager.getManager().attributeEdit(instance, "_displayName");
            // DisplayName change might result in _displayName changes in other instances referring to
            // instance. For example, CatalystActivity use Activity's _displayName.
            // It should work only for local instance
            XMLFileAdaptor adaptor = (XMLFileAdaptor) instance.getDbAdaptor();
            try {
                List referrers = adaptor.getReferers(instance);
                GKInstance referrer = null;
                for (Iterator it = referrers.iterator(); it.hasNext();) {
                    referrer = (GKInstance) it.next();
                    oldName = referrer.getDisplayName();
                    newName = InstanceDisplayNameGenerator.generateDisplayName(referrer);
                    if (oldName.equals(newName))
                        continue;
                    referrer.setDisplayName(newName);
                    AttributeEditManager.getManager().attributeEdit(referrer, "_displayName");
                }
            }
            catch(Exception e) {
                System.err.println("AttributeEditManager.validateDisplayName(): " + e);
                e.printStackTrace();
            }
        }
        return displayNameChanged;
    }   
}
