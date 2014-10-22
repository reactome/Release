/*
 * Created on Jun 5, 2012
 *
 */
package org.reactome.core.controller;

import org.gk.model.GKInstance;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.core.model.DatabaseIdentifier;
import org.reactome.core.model.DatabaseObject;
import org.reactome.core.model.ReferenceSequence;


/**
 * This class is used to handle post-processing after reflection based mapping from
 * Reactome model to RESTful model.
 * @author gwu
 *
 */
public abstract class ReactomeModelPostMapper {
    // The Reactome class name
    private String className;
    
    public ReactomeModelPostMapper() {
        
    }
    
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * The method that is used to post-process mapping.
     * @param inst
     * @param obj
     * @param converter TODO
     */
    public abstract void postProcess(GKInstance inst, 
                                     DatabaseObject obj,
                                     ReactomeToRESTfulAPIConverter converter) throws Exception;
    
    /**
     * This method is used to fill attributes needed for detailed view.
     * @param inst
     * @param obj
     * @param converter
     * @throws Exception
     */
    public abstract void fillDetailedView(GKInstance inst,
                                          DatabaseObject obj,
                                          ReactomeToRESTfulAPIConverter converter) throws Exception;
    
    /**
     * This method is used to add some extra properties for a shell DatabaseObject.
     * @param inst
     * @param obj
     * @throws Exception
     */
    public abstract void postShellProcess(GKInstance inst,
                                          DatabaseObject obj) throws Exception;
    
    protected abstract boolean isValidObject(DatabaseObject obj);

    /**
     * Check if the passed instance can be processed by this ReactomeModelPostMapper.
     * @param inst
     * @return
     */
    protected boolean validParameters(GKInstance inst,
                                      DatabaseObject obj) throws Exception {
        return inst.getSchemClass().isa(className) && isValidObject(obj);
    }
    
    /**
     * One of utility methods.
     * @param dba
     * @param dbi
     * @throws Exception
     */
    protected void assignValidURLToDatabaseIdentifier(PersistenceAdaptor dba,
                                                      DatabaseIdentifier dbi) throws Exception {
        GKInstance dbiInst = dba.fetchInstance(dbi.getDbId());
        String url = getURL(dbiInst);
        dbi.setUrl(url);
    }
    
    /**
     * One of utility methods.
     * @param dba
     * @throws Exception
     */
    protected void assignValidURLToDatabaseIdentifier(PersistenceAdaptor dba,
    												  ReferenceSequence rs) throws Exception {
        GKInstance dbiInst = dba.fetchInstance(rs.getDbId());
        String url = getURL(dbiInst);
        rs.setUrl(url);
    }
       
    private String getURL(GKInstance dbiInst) throws Exception{
        if (!dbiInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.identifier))
            return null;
    	String id = (String) dbiInst.getAttributeValue(ReactomeJavaConstants.identifier);
        if (id == null)
            return null;
        if (!dbiInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceDatabase))
            return null;
        GKInstance dbInst = (GKInstance) dbiInst.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
        if (dbInst == null){
        	return null;
        }
        String accessUrl = (String) dbInst.getAttributeValue(ReactomeJavaConstants.accessUrl);
        if (accessUrl == null)
        	return null;
        String url = accessUrl.replace("###ID###", id);
        return url;
    }
}
