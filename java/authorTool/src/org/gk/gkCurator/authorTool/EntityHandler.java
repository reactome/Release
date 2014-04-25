/*
 * Created on Sep 23, 2006
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.Modification;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.Renderable;
import org.gk.render.RenderableChemical;
import org.gk.render.RenderableGene;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableProtein;
import org.gk.render.RenderableRNA;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.SchemaClass;

public class EntityHandler extends RenderableHandler {
    
    protected GKInstance convertChanged(Renderable r) throws Exception {
        Long dbId = (Long) getDbId(r);
        if (dbId == null)
            return null; // Just in case. Should not occur.
        // Fetch the db instance
        GKInstance dbInstance = dbAdaptor.fetchInstance(dbId);
        if (dbInstance == null)
            return null;  // Might be deleted
        // These Types are convertable
        SchemaClass cls = dbInstance.getSchemClass();
        String localClsName = null;
//        if (cls.isa(ReactomeJavaConstants.PhysicalEntity) &&
//                !cls.isa(ReactomeJavaConstants.Complex)) // Complex cannot be used
//            localClsName = cls.getName();
//        else
            localClsName = getEntityType(r); // Type might be changed: adding a new accession 
                                             // will convert it to a set
        GKInstance local = createNewWithID(localClsName, dbId);
        return local;
    }
    
    private String getEntityType(Renderable r) {
        String clsName = null;
        DatabaseIdentifier externalDBID = (DatabaseIdentifier) r.getAttributeValue(RenderablePropertyNames.DATABASE_IDENTIFIER);
        if (externalDBID != null) {
            String accession = externalDBID.getAccessNo();
            if (accession != null && accession.indexOf(",") > 0) {
                return ReactomeJavaConstants.DefinedSet;
            }
        }
        return getNonSetEntityType(r, externalDBID);
    }

    private String getNonSetEntityType(Renderable r, 
                                       DatabaseIdentifier externalDBID) {
        String clsName = null;
        // clsName should be based on types of r
        if (r instanceof RenderableProtein || 
            r instanceof RenderableRNA ||
            r instanceof RenderableGene) {
            clsName = ReactomeJavaConstants.EntityWithAccessionedSequence;
        }
        else if (r instanceof RenderableChemical) {
            clsName = ReactomeJavaConstants.SimpleEntity;
        }
        // Only EWAS can have modification
        else if (r.getAttributeValue(RenderablePropertyNames.MODIFICATION) != null) {
            clsName = ReactomeJavaConstants.EntityWithAccessionedSequence;
        }
        else { // Try to determine based on external databases
            // No need to sort. It should be sorted already.
            if (externalDBID != null && externalDBID.getDbName() != null) {
                String dbName = externalDBID.getDbName();
                if (dbName.equals("UniProt")) 
                    clsName = ReactomeJavaConstants.EntityWithAccessionedSequence;
                else if (dbName.equals("ChEBI"))
                    clsName = ReactomeJavaConstants.SimpleEntity;
            }
            else 
                clsName = ReactomeJavaConstants.OtherEntity;
        }
        // Use OtherEntity as default
        if (clsName == null)
            clsName = ReactomeJavaConstants.OtherEntity;
        return clsName;
    }
    
    public GKInstance createNew(Renderable r) throws Exception {
        String clsType = getEntityType(r);
        GKInstance newInstance = fileAdaptor.createNewInstance(clsType);
        return newInstance;
    }
    
    protected void convertPropertiesForNew(GKInstance instance, 
                                           Renderable entity, 
                                           Map rToIMap) throws Exception {
        extractNames(entity, instance);
        extractTaxon(entity, instance);
        extractModifications(entity, instance);
        extractDatabaseIdentifier(entity, instance);
        extractLocalization(entity, instance);
    }
    
    private void extractModifications(Renderable entity, 
                                      GKInstance instance) throws Exception {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasModifiedResidue))
            return;
        java.util.List modifications = (java.util.List) entity.getAttributeValue(RenderablePropertyNames.MODIFICATION);
        if (modifications == null || modifications.size() == 0) {
            instance.setAttributeValue(ReactomeJavaConstants.hasModifiedResidue, null);
            return;
        }
        java.util.List mdInstances = new ArrayList(modifications.size());
        Modification modification = null;
        GKInstance mdInstance = null;
        int c = 0;
        for (Iterator it = modifications.iterator(); it.hasNext();) {
            modification = (Modification) it.next();
            mdInstance = createModifiedResidue(modification, mdInstance);
            mdInstances.add(mdInstance);
        }
        instance.setAttributeValue(ReactomeJavaConstants.hasModifiedResidue, mdInstances);
    }

    private GKInstance createModifiedResidue(Modification modification, 
                                             GKInstance mdInstance) throws Exception {
        if (modification.getDB_ID() != null)
            mdInstance = getInstanceByDBID(modification.getDB_ID());
        if (mdInstance == null) {
            mdInstance = fileAdaptor.createNewInstance(ReactomeJavaConstants.ModifiedResidue);
        }
        if (modification.getCoordinate() != -1) { // default value is -1
            mdInstance.setAttributeValue(ReactomeJavaConstants.coordinate,
                                         new Integer(modification.getCoordinate()));
        }
        String mdf = modification.getModification();
        //create a SimpleEntity for mdf
        if (mdf != null && mdf.length() > 0) {
            GKInstance instance1 = getInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule, 
                                                          ReactomeJavaConstants._displayName, 
                                                          mdf);
            GKInstance modificationInstance = instance1;
            mdInstance.setAttributeValue(ReactomeJavaConstants.modification,
                                         modificationInstance);
        }
        //create a concreteSimpleEntiry for residue
        String residue = modification.getResidue();
        if (residue != null && residue.length() > 0) {
            GKInstance instance1 = getInstanceByAttribute(ReactomeJavaConstants.ReferenceMolecule, 
                                                          ReactomeJavaConstants._displayName, 
                                                          residue);
            GKInstance residueInstance = instance1;
            mdInstance.setAttributeValue(ReactomeJavaConstants.residue, 
                                         residueInstance);
        }
        InstanceDisplayNameGenerator.setDisplayName(mdInstance);
        return mdInstance;
    }
    
    private void extractDatabaseIdentifier(Renderable entity,
                                           GKInstance instance) throws Exception {
        DatabaseIdentifier dbIdentifier = (DatabaseIdentifier)entity.getAttributeValue(RenderablePropertyNames.DATABASE_IDENTIFIER);
        // Escape if dbName is null since no database can be specified.
        if (dbIdentifier == null || 
            dbIdentifier.getDbName() == null || 
            dbIdentifier.getAccessNo() == null) {
            return;
        }
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.DefinedSet)) {
            assignDatabaseIdentifier(instance, 
                                     dbIdentifier.getDB_ID(),
                                     dbIdentifier.getDbName(),
                                     dbIdentifier.getAccessNo());
        }
        else {
            // Need to do some complicated converting
            String clsName = getNonSetEntityType(entity, 
                                                 dbIdentifier);
            String[] ids = dbIdentifier.getAccessNo().split(",");
            for (int i = 0; i < ids.length; i++) {
                GKInstance member = fileAdaptor.createNewInstance(clsName);
                copyPropertiesFromSetToMember(instance, member);
                instance.addAttributeValue(ReactomeJavaConstants.hasMember, member);
                assignDatabaseIdentifier(member, 
                                         null, 
                                         dbIdentifier.getDbName(), 
                                         ids[i].trim()); // May contain space
            }
        }
    }

    private void assignDatabaseIdentifier(GKInstance instance, 
                                          Long dbId,
                                          String dbName,
                                          String access) throws Exception {
        GKInstance dbIdInstance = createExDBIdentifier(dbId,
                                                       dbName,
                                                       access);
        // Try referenceEntity first
        boolean isAdded = false;
        if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
            GKSchemaAttribute attribute = (GKSchemaAttribute) instance.getSchemClass().getAttribute(ReactomeJavaConstants.referenceEntity);
            if (attribute.isValidClass(dbIdInstance.getSchemClass())) {
                instance.setAttributeValue(attribute, dbIdInstance);
                isAdded = true;
            }
        }
        if (!isAdded) {
            // Used as crossReference
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.crossReference)) {
                GKSchemaAttribute attribute = (GKSchemaAttribute) instance.getSchemClass().getAttribute(ReactomeJavaConstants.crossReference);
                if (attribute.isValidClass(dbIdInstance.getSchemClass()))
                    instance.addAttributeValue(attribute, dbIdInstance);
            }
        }
    }
    
    private void copyPropertiesFromSetToMember(GKInstance instance,
                                               GKInstance member) throws Exception {
        SchemaClass memberCls = member.getSchemClass();
        for (Iterator it = instance.getSchemClass().getAttributes().iterator();
             it.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute) it.next();
            if (att.getName().equals(ReactomeJavaConstants.DB_ID) ||
                att.getName().equals(ReactomeJavaConstants.created) ||
                att.getName().equals(ReactomeJavaConstants.modified))
                continue; 
            List values = instance.getAttributeValuesList(att);
            if (values == null || values.size() == 0)
                continue;
            if (memberCls.isValidAttribute(att.getName())) {       
                member.setAttributeValueNoCheck(att.getName(), new ArrayList(values));
            }
        }
    }
    
    private GKInstance createExDBIdentifier(Long dbID, 
                                            String dbName,
                                            String identifier) throws Exception {
        GKInstance dbIDInstance = null;
        if (dbID != null) {
            dbIDInstance = getInstanceByDBID(dbID);
        }
        if (dbIDInstance == null) {
            if (dbName.equals("UniProt")) {
                String clsName = null;
                if (fileAdaptor.getSchema().isValidClass(ReactomeJavaConstants.ReferenceGeneProduct))
                    clsName = ReactomeJavaConstants.ReferenceGeneProduct;
                else
                    clsName = ReactomeJavaConstants.ReferencePeptideSequence;
                dbIDInstance = getInstanceByAttribute(clsName, 
                                                      ReactomeJavaConstants.identifier, 
                                                      identifier);
            } 
            else {
                dbIDInstance = getInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier, 
                                                      ReactomeJavaConstants.identifier,
                                                      identifier);
            }
        }
        if ((dbIDInstance.getDBID().longValue() < 0) &&
            (dbName != null)) { // New instance, assign database name
            GKInstance refDB = getInstanceByAttribute(ReactomeJavaConstants.ReferenceDatabase,
                                                      ReactomeJavaConstants._displayName,
                                                      dbName);
            dbIDInstance.setAttributeValue(ReactomeJavaConstants.referenceDatabase, 
                                           refDB);
        }
        InstanceDisplayNameGenerator.setDisplayName(dbIDInstance);
        return dbIDInstance;
    }
}
