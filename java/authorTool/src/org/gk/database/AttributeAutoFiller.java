/*
 * Created on Aug 3, 2005
 *
 */
package org.gk.database;

import java.awt.Component;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;

/**
 * An AttributeAutoFiller can be used to fill other attributes by fetching information
 * from other database based on identity. For example, attributes other than PMID can
 * be automatically fetched from PubMED by querying with PMID specified.
 * <p />
 * To make it work, a concrete implementation of AttributeAutoFiller should be configured
 * in curator.xml to be loaded automatically.
 * @author guanming
 *
 */
public interface AttributeAutoFiller {
    
    /**
     * Set the data source to be used for attribute assignment. A data source can be
     * a local or database source.
     * @param adaptor
     */
    public void setPersistenceAdaptor(PersistenceAdaptor adaptor);
    
    /**
     * Ask the user if he or she wants to do auto-filling. The client should call this
     * method first to get the user's confirmation for auto-filling.
     * @param parentComp 
     * @param instance GKInstance to be processed.
     * @return true for being able to continuing. 
     */
    public boolean getApprove(Component parentComp, GKInstance instance);
    
    /**
     * The actual method to fill attributes automatically. The client calling this method
     * should assign a non-null PersistenceAdaptor first.
     * @param instance GKInstance to be processed.
     * @param parentComp
     * @see setPersistenceAdapter(PersistenceAdapter)
     */
    public void process(GKInstance instance, Component parentComp) throws Exception;
    
    /**
     * During auto-filling, some instances might be created automatically to be used
     * by the instance for auto-filling.
     * @param l
     */
    public List getAutoCreatedInstances();
    
    /**
     * Set the parent component that can be used for generating dialogs.
     * @param parentComp
     */
    public void setParentComponent(Component parentComp);
}
