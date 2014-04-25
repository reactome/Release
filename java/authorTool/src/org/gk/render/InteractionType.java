/*
 * Created on Dec 11, 2006
 *
 */
package org.gk.render;

import java.io.Serializable;


/**
 * This is a enum class to list types for Interaction. It should be implemented as
 * Enum in JDK1.5.0.
 * @author guanming
 *
 */
public final class InteractionType implements Serializable {
    // A list of types
    private static final int ACTIVATE_INT = 0;
    private static final int INHIBIT_INT = 1;
    private static final int INTERACT_INT = 2;
    private static final int REPRESS_INT = 3;
    private static final int ENHANCE_INT = 4;
    private static final int ENCODE_INT = 5;
    private static final int UNKNOWN_INT = 100;
    // A list of type names
    private static final String ACTIVATE_NAME = "Activate";
    private static final String INHIBIT_NAME = "Inhibit";
    private static final String INTERACT_NAME = "Interact";
    private static final String REPRESS_NAME = "Repress";
    private static final String ENHANCE_NAME = "Enhance";
    private static final String ENCODE_NAME = "Encode";
    private static final String UNKNOWN_NAME = "Unkown";
    // predefined types
    public static final InteractionType ACTIVATE = new InteractionType(ACTIVATE_INT);
    public static final InteractionType INHIBIT = new InteractionType(INHIBIT_INT);
    public static final InteractionType INTERACT = new InteractionType(INTERACT_INT);
    public static final InteractionType REPRESS = new InteractionType(REPRESS_INT);
    public static final InteractionType ENHANCE = new InteractionType(ENHANCE_INT);
    public static final InteractionType ENCODE = new InteractionType(ENCODE_INT);
    public static final InteractionType UNKNOWN = new InteractionType(UNKNOWN_INT);
    // member variable
    private int type;
    
    private InteractionType(int type) {
        this.type = type;
    }
    
    public static InteractionType getType(String typeName) {
       if (typeName.equals(ACTIVATE_NAME))
           return ACTIVATE;
       if (typeName.equals(INHIBIT_NAME))
           return INHIBIT;
       if (typeName.equals(INTERACT_NAME))
           return INTERACT;
       if (typeName.equals(REPRESS_NAME))
           return REPRESS;
       if (typeName.equals(ENHANCE_NAME))
           return ENHANCE;
       if (typeName.equals(ENCODE_NAME))
           return ENCODE;
       if (typeName.equals(UNKNOWN_NAME))
           return UNKNOWN;
       return null;
    }
    
    public String getTypeName() {
        switch (type) {
            case ACTIVATE_INT :
                return ACTIVATE_NAME;
            case INHIBIT_INT :
                return INHIBIT_NAME;
            case INTERACT_INT :
                return INTERACT_NAME;
            case REPRESS_INT :
                return REPRESS_NAME;
            case ENHANCE_INT :
                return ENHANCE_NAME;
            case ENCODE_INT :
                return ENCODE_NAME;
            default :
                return UNKNOWN_NAME;
        }
    }
    
}
