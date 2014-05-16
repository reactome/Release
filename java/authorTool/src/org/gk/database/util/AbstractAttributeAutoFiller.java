/*
 * Created on Aug 4, 2005
 *
 */
package org.gk.database.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.gk.database.AttributeAutoFiller;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;

/**
 * This is a template for concrete implementation of AttributeAutoFiller.
 * @author guanming
 *
 */
public abstract class AbstractAttributeAutoFiller implements AttributeAutoFiller {
    // To hold auto-created instances
    protected List<GKInstance> autoCreatedInstances;
    protected PersistenceAdaptor adaptor;
    protected Component parentComponent;
    
    /* (non-Javadoc)
     * @see org.gk.database.AttributeAutoFiller#getApprove(java.awt.Component)
     */
    public boolean getApprove(Component parentComp, GKInstance instance) {
        try {
            Object attValue = getRequiredAttribute(instance);
            if (attValue == null) // Cannot do auto-filling
                return false;
            int reply = JOptionPane.showConfirmDialog(parentComp,
                                                      getConfirmationMessage(), 
                                                      "Fetching Information?",
                                                      JOptionPane.YES_NO_OPTION);
            return reply == JOptionPane.YES_OPTION;
        }
        catch (Exception e1) {
            System.err.println("AbstractAttributePaneController.getApprove(): " + e1);
            e1.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get the required attribute for auto-filling.
     * @param instance
     * @return
     * @throws Exception
     */
    protected abstract Object getRequiredAttribute(GKInstance instance) throws Exception;
    
    /**
     * Get the message to be displayed in the confirmation dialog.
     * @return
     */
    protected abstract String getConfirmationMessage();
    
    public void setPersistenceAdaptor(PersistenceAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    
    public abstract void process(GKInstance instance, Component parentComp) throws Exception;   

    public List<GKInstance> getAutoCreatedInstances() {
        // To avoid exposing the interal data structure
        if (autoCreatedInstances == null)
            return new ArrayList<GKInstance>();
        return new ArrayList<GKInstance>(autoCreatedInstances);
    }
    
    /**
     * A helper method to get a ReferenceDatabase instance for the specified instance.
     * @param dbName
     * @return
     * @throws Exception
     */
    protected GKInstance getReferenceDatabasae(String dbName) throws Exception {
        // Using _displayName can fetch local shell instances.
        Collection<?> list = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase, 
                                                              ReactomeJavaConstants._displayName, 
                                                              "=",
                                                              dbName);
        GKInstance refDb = null;
        if (list.size() > 0)
            refDb = (GKInstance) list.iterator().next();
        if (refDb == null) {
            if (adaptor instanceof XMLFileAdaptor) {
                // Try to get it from the database
                MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(parentComponent);
                if (dba != null) {
                    list = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase,
                                                        ReactomeJavaConstants._displayName,
                                                        "=",
                                                        dbName);
                    if (list != null && list.size() > 0) {
                        GKInstance dbRefDb = (GKInstance) list.iterator().next();
                        refDb = PersistenceManager.getManager().getLocalReference(dbRefDb);
                    }
                }
                if (refDb == null) { // Need to create a new one
                    refDb = ((XMLFileAdaptor)adaptor).createNewInstance(ReactomeJavaConstants.ReferenceDatabase);
                    refDb.setAttributeValue(ReactomeJavaConstants.name, 
                                            dbName);
                    InstanceDisplayNameGenerator.setDisplayName(refDb);
                    if (autoCreatedInstances == null) { // In case it is null
                        autoCreatedInstances = new ArrayList<GKInstance>();
                    }
                    autoCreatedInstances.add(refDb);
                }
            }
        }
        return refDb;
    }

    @Override
    public void setParentComponent(Component parentComp) {
        this.parentComponent = parentComp;
    }
    
}
