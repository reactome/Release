/*
 * Created on Jul 6, 2003
 */
package org.gk.graphEditor;

import java.util.EventObject;

/**
 * An event generated from actions in this package.
 * @author wgm
 */
public class GraphEditorActionEvent extends EventObject{
    
    /**
     * Predefined action types
     * @author wgm
     *
     */
    public enum ActionType {
        NOTHING {
            public boolean isSavable() {return false; }
            
            public boolean isRepaintable() {return false; }
        },
        ACTION_DOUBLR_CLICKED{
            public boolean isSavable() {return false; }
            
            public boolean isRepaintable() {return false; }
        },
        REACTION_ATTACH {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        REACTION_DETACH {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        SELECTION {
            public boolean isSavable() {return false; }
            
            public boolean isRepaintable() {return true; }
        },
        MOVING {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        NAME_EDITING{
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        PROP_CHANGING {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return false; }
        },
        COMPLEX_COMPONENT_CHANGED {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        INSERT {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        DELETE {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        DE_LINKSHORTCUTS {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return false; }
        },
        BENDING {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        FEATURE {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        FORMAT {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        },
        REACTION_TYPE {

            @Override
            public boolean isRepaintable() {
                return true;
            }

            @Override
            public boolean isSavable() {
                return true;
            }
            
        },
        LAYOUT {
            public boolean isSavable() {return true; }
            
            public boolean isRepaintable() {return true; }
        };  // Need to use semi-colon
        
        public abstract boolean isSavable();
        
        public abstract boolean isRepaintable();
    }
    
	// Event ID
	public static final ActionType ACTION_DOUBLE_CLICKED = ActionType.ACTION_DOUBLR_CLICKED;
	public static final ActionType REACTION_ATTACH = ActionType.REACTION_ATTACH;
	public static final ActionType REACTION_DETACH = ActionType.REACTION_DETACH;
	public static final ActionType SELECTION = ActionType.SELECTION;
	public static final ActionType MOVING = ActionType.MOVING;
	public static final ActionType NAME_EDITING = ActionType.NAME_EDITING;
	public static final ActionType PROP_CHANGING = ActionType.PROP_CHANGING;
	public static final ActionType COMPLEX_COMPONENT_CHANGED = ActionType.COMPLEX_COMPONENT_CHANGED;
	public static final ActionType INSERT = ActionType.INSERT;
	public static final ActionType DELET = ActionType.DELETE;
	public static final ActionType DE_LINKSHORTCUTS = ActionType.DE_LINKSHORTCUTS;
	public static final ActionType LAYOUT = ActionType.LAYOUT;
	public static final ActionType BENDING = ActionType.BENDING;
	public static final ActionType FEATURE = ActionType.FEATURE;
	public static final ActionType FORMAT = ActionType.FORMAT;
	public static final ActionType REACTION_TYPE = ActionType.REACTION_TYPE;
	
	private ActionType eventID = ActionType.NOTHING;

	public GraphEditorActionEvent(Object source) {
		super(source);
	}
	
	public GraphEditorActionEvent(Object source, ActionType id) {
		this(source);
		eventID = id;
	}
	
	public void setID(ActionType id) {
		this.eventID = id;
	}
	
	public ActionType getID() {
		return this.eventID;
	}
	
	/**
	 * Check if this GraphEditorActionEvent is something that
	 * make the content dirty.
	 * @return
	 */
	public boolean isSavableEvent() {
	    return eventID.isSavable();
	}
	
	/**
	 * Check if this GraphEditorActionEvent is a repaintable event.
	 * @return
	 */
	public boolean isRepaintableEvent() {
	    return eventID.isRepaintable();
	}
	
}
