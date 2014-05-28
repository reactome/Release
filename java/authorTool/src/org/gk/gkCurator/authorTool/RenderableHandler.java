/*
 * Created on Sep 23, 2006
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.Map;

import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;

/**
 * This class is used to convert a Renderable object to a GKInstance.
 * @author guanming
 *
 */
public abstract class RenderableHandler {
    protected XMLFileAdaptor fileAdaptor;
    protected MySQLAdaptor dbAdaptor;
    private RenderableHandlerHelper helper;
    
    public RenderableHandler() {
        helper = new RenderableHandlerHelper();
    }
    
    public MySQLAdaptor getDbAdaptor() {
        return dbAdaptor;
    }

    public void setDbAdaptor(MySQLAdaptor dbAdaptor) {
        this.dbAdaptor = dbAdaptor;
        helper.setDBAdaptor(dbAdaptor);
    }

    public XMLFileAdaptor getFileAdaptor() {
        return fileAdaptor;
    }

    public void setFileAdaptor(XMLFileAdaptor fileAdaptor) {
        this.fileAdaptor = fileAdaptor;
        helper.setFileAdaptor(fileAdaptor);
    }
    
    protected Long getDbId(Renderable r) {
        return r.getReactomeId();
    }
    
    public GKInstance convert(Renderable r, 
                              Map rToIMap) throws Exception {
        Long dbId = getDbId(r);
        GKInstance converted = null;
        if (dbId == null)
            converted = createNew(r);
        else if (r.isChanged())
            converted = convertChanged(r);
        else
            converted = convertUnChanged(r);
        if (converted == null)
            converted = createNew(r); // In case instance is deleted
        // Use displayName from Renderable since displayName is ID in the author tool
        converted.setDisplayName(r.getDisplayName());
        rToIMap.put(r, converted);
        return converted;
    }
    
    public void convertProperties(Renderable r,
                                  Map rToIMap) throws Exception {
        GKInstance converted = (GKInstance) rToIMap.get(r);
        if (converted.getDBID().longValue() > 0 &&
            dbAdaptor.exist(converted.getDBID())) { // Might have be deleted
            if (r.isChanged())
                convertPropertiesForChanged(converted, r, rToIMap);
            else {
                GKInstance dbInstance = dbAdaptor.fetchInstance(converted.getDBID());
                if (dbInstance == null) {
                    System.err.println("Instance has been deleted: " + converted.getDBID());
                    convertPropertiesForNew(converted, r, rToIMap);
                    return;
                }
                SynchronizationManager.getManager().updateFromDB(converted, dbInstance);
                converted.setIsDirty(false);
            }
        }
        else
            convertPropertiesForNew(converted, r, rToIMap);
    }
    
    public void convertPropertiesForNew(GKInstance converted,
                                        Renderable r) throws Exception {
        
    }
    
    protected abstract void convertPropertiesForNew(GKInstance converted,
                                                    Renderable r,
                                                    Map rToIMap) throws Exception;
    
    protected abstract GKInstance convertChanged(Renderable r) throws Exception;
    
    public abstract GKInstance createNew(Renderable r) throws Exception;
    
    protected GKInstance createNewWithID(String clsName,
                                         Long dbId) throws Exception {
        GKInstance instance = fileAdaptor.createNewInstance(clsName);
        Long oldId = instance.getDBID();
        instance.setDBID(dbId);
        fileAdaptor.dbIDUpdated(oldId, instance);
        return instance;
    }
        
    protected GKInstance convertUnChanged(Renderable r) throws Exception {
        Long id = r.getReactomeId();
        GKInstance dbInstance = dbAdaptor.fetchInstance(id);
        if (dbInstance == null) // Database instance might be deleted already
                                // Treat it as new
            return createNew(r);
        GKInstance local = createNewWithID(dbInstance.getSchemClass().getName(),
                                           id);
        return local;
    }
    
    protected void extractNames(Renderable r,
                                GKInstance instance) throws Exception {
        helper.extractNames(r, instance);
    }
    
    protected void extractAttachments(Renderable r,
                                      GKInstance instance) throws Exception {
        helper.extractAttachments(r, instance);
    }
    
    protected void extractTaxon(Renderable r,
                                GKInstance instance) throws Exception {
        if (!(instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species)))
            return; // Do nothing if an entity cannot have species value
        helper.extractPropertyFromName(r, 
                                       RenderablePropertyNames.TAXON, 
                                       instance, 
                                       ReactomeJavaConstants.species, 
                                       ReactomeJavaConstants.Species);
    }
    
    protected void extractLocalization(Renderable r,
                                       GKInstance instance) throws Exception {
        if (r.getLocalization() == null)
            return;
        String locationClsName = null;
        GKSchemaAttribute att = (GKSchemaAttribute) instance.getSchemClass().getAttribute(ReactomeJavaConstants.compartment);
        Schema schema = instance.getDbAdaptor().getSchema();
        SchemaClass cls = schema.getClassByName(ReactomeJavaConstants.Compartment);
        if (att.isValidClass(cls))
            locationClsName = cls.getName();
        else // Narrow down. It should be for PhysicalEntity
            locationClsName = ReactomeJavaConstants.EntityCompartment;
        helper.extractLocalization(r, 
                                   instance, 
                                   ReactomeJavaConstants.compartment, 
                                   locationClsName);
    }
    
    protected void extractReference(Renderable r,
                                    GKInstance instance) throws Exception {
        helper.extractReference(r, instance);
    }
    
    protected void extractSummation(Renderable r,
                                    GKInstance instance) throws Exception {
        helper.extractSummation(r, instance);
    }
    
    protected GKInstance getInstanceByAttribute(String clsName,
                                                String attName,
                                                Object value) throws Exception {
        return helper.getInstanceByAttribute(clsName, attName, value);
    }
    
    protected GKInstance getInstanceByDBID(Long dbId) throws Exception {
        return helper.getInstanceByDBID(dbId);
    }

    protected void convertPropertiesForChanged(GKInstance converted, 
                                               Renderable r, 
                                               Map rToIMap) throws Exception {
        GKInstance dbCopy = dbAdaptor.fetchInstance(converted.getDBID());
        // dbCopy should NOT be null
        // Update from database first, then make changes
        // Type might be changed: add more database accessions
        SchemaClass originalCls = converted.getSchemClass();
        SynchronizationManager.getManager().updateFromDB(converted, 
                                                         dbCopy);
        if (!originalCls.equals(converted.getSchemClass())) {
            fileAdaptor.switchType(converted, (GKSchemaClass)originalCls);
        }
        convertPropertiesForNew(converted, r, rToIMap);
    }
}
