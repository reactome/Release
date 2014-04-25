/*
 * Created on Jul 29, 2008
 *
 */
package org.gk.model;

/**
 * When an instance should be in some persistence place, but cannot be found,
 * this exception should be thrown.
 */
public class InstanceNotFoundException extends Exception {
    
    public InstanceNotFoundException(String cls,
                                     Long dbId) {
        super(cls + " [" + dbId + "] cannot be found.");
    }
    
    public InstanceNotFoundException(Long dbId) {
        super("Instance with dbId, " + dbId + ", cannot be found.");
    }
    
}
