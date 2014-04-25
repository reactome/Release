/*
 * Created on Aug 31, 2005
 *
 */
package org.gk.model;

import org.gk.schema.SchemaClass;


/**
 * A wrapper to attach a stoichiometry value to a GKInstance object to be used
 * in display for Complex composition or Reaction inputs and outputs.
 * @author guanming
 *
 */
public class StoichiometryInstance extends GKInstance implements Comparable {
    // The wrapped instance. Read only property
    private GKInstance instance;
    // Stoichiometry
    private int stoi;
    
    public StoichiometryInstance(GKInstance instance, int stoichiometry) {
        this.instance = instance;
        this.stoi = stoichiometry;
    }
    
    public int getStoichiometry() {
        return this.stoi;
    }
    
    public void setStoichiometry(int value) {
        this.stoi = value;
    }
    
    public GKInstance getInstance() {
        return this.instance;
    }
    
    public String getDisplayName() {
        if (stoi == 0)
            return instance.getDisplayName();
        return stoi + " x " + instance.getDisplayName();
    }
    
    public String toString() {
        return getDisplayName();
    }
    
    public String getExtendedDisplayName() {
        return instance.getExtendedDisplayName();
    }
    
    public SchemaClass getSchemClass() {
        return instance.getSchemClass();
    }
    
    public int compareTo(Object obj) {
        if (!(obj instanceof StoichiometryInstance))
            throw new IllegalArgumentException("StoichiometryInstance.compareTo(): " +
                    "the argument is not StoichiometryInstance");
        GKInstance outInstance = ((StoichiometryInstance)obj).getInstance();
        String displayName1 = instance.getDisplayName();
        if (displayName1 == null)
            displayName1 = "";
        String displayName2 = outInstance.getDisplayName();
        if (displayName2 == null)
            displayName2 = "";
        int reply = displayName1.compareTo(displayName2);
        if (reply == 0)
            return instance.getDBID().compareTo(outInstance.getDBID());
        else
            return reply;
    }

}
