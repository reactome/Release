/*
 * Created on Jun 14, 2012
 *
 */
package org.reactome.core.mapper;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.reactome.core.controller.ReactomeModelPostMapper;
import org.reactome.core.controller.ReactomeToRESTfulAPIConverter;
import org.reactome.core.model.DatabaseObject;
import org.reactome.core.model.StableIdentifier;


/**
 * @author gwu
 *
 */
public class StableIdentifierMapper extends ReactomeModelPostMapper {
    
    public StableIdentifierMapper() {
        
    }
    
    /* (non-Javadoc)
     * @see org.reactome.restfulapi.ReactomeModelPostMapper#postProcess(org.gk.model.GKInstance, org.reactome.restfulapi.models.DatabaseObject, org.reactome.restfulapi.ReactomeToRESTfulAPIConverter)
     */
    @Override
    public void postProcess(GKInstance inst, DatabaseObject obj,
                            ReactomeToRESTfulAPIConverter converter)
            throws Exception {
    }
    
    @Override
    public void fillDetailedView(GKInstance inst, DatabaseObject obj,
                                 ReactomeToRESTfulAPIConverter converter)
            throws Exception {
    }

    /* (non-Javadoc)
     * @see org.reactome.restfulapi.ReactomeModelPostMapper#postShellProcess(org.gk.model.GKInstance, org.reactome.restfulapi.models.DatabaseObject)
     */
    @Override
    public void postShellProcess(GKInstance inst, DatabaseObject obj)
            throws Exception {
        // Make sure _displayName is not null
        if (obj.getDisplayName() == null) {
            String displayName = InstanceDisplayNameGenerator.generateDisplayName(inst);
            obj.setDisplayName(displayName);
        }
    }
    
    /* (non-Javadoc)
     * @see org.reactome.restfulapi.ReactomeModelPostMapper#isValidObject(org.reactome.restfulapi.models.DatabaseObject)
     */
    @Override
    protected boolean isValidObject(DatabaseObject obj) {
        return (obj instanceof StableIdentifier);
    }
    
}
