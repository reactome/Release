/*
 * Created on Oct 5, 2005
 *
 */
package org.reactome.core.controller;

import org.apache.log4j.Logger;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.reactome.core.model.DatabaseObject;
import org.reactome.core.model.PhysicalEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class is used to convert GKInstance to caBIO org.reactome.restfulapi.domain objects. There are two modes during
 * initialization: recursive, and non-recursive. Recursive will convert all contained instances
 * recursively. The mode should be decided when an ReactomeToRESTfulAPIConverter is initialized and
 * should NOT be changed after it is constructed. Otherwise, the results might not be what the
 * client expects.
 *
 * @author guanming
 */
@SuppressWarnings("unchecked")
public class ReactomeToRESTfulAPIConverter {
    private static Logger logger = Logger.getLogger(ReactomeToRESTfulAPIConverter.class);
    // Cache all mapped GKInstance
    // Since GKInstance has been cached already, don't cache any converted DatabaseObject
//    private Map<Long, DatabaseObject> reactomeToCaBioMap;
    private ReactomeToRESTfulAPIMapper mapper;

    private ReactomeModelPostMapperFactory postMapperFactory;

    public ReactomeToRESTfulAPIConverter() {
        logger.info("ReactomeToRESTfulAPIConverter has been initialized...");
    }

    public void setMapper(ReactomeToRESTfulAPIMapper mapper) {
        this.mapper = mapper;
    }

    public ReactomeToRESTfulAPIMapper getMapper() {
        return this.mapper;
    }
    

    public ReactomeModelPostMapperFactory getPostMapperFactory() {
        return postMapperFactory;
    }

    public void setPostMapperFactory(ReactomeModelPostMapperFactory postMapperFactory) {
        this.postMapperFactory = postMapperFactory;
    }

    /**
     * Use this method to convert an GKInstance to a caBIO object in the package:
     * org.reactome.cabig.org.reactome.restfulapi.domain. The returned object should
     * have all attributes filled based on the Reactome data model, but not based on the
     * attributes in the Java class in this domain package. For example, some property (e.g.
     * followingEvent in the Event class) may not be filled. To get a full view of this
     * object, fillInDetails() should be called.
     *
     * @param instance
     * @return
     * @throws Exception
     */
    public DatabaseObject convert(GKInstance instance) throws Exception {
        DatabaseObject obj = createObject(instance);
        fill(obj, instance);
        return obj;
    }
    
    public void fillInDetails(GKInstance instance,
                              DatabaseObject obj) throws Exception {
        ReactomeModelPostMapper mapper = postMapperFactory.getPostMapper(instance);
        if (mapper != null)
            mapper.fillDetailedView(instance, 
                                    obj,
                                    this);
    }

    /**
     * A helper method that is used to get a list of converted PhysicalEntity instances contained by a 
     * passed Pathway instance.
     * @param pathwayInstance
     * @return
     * @throws Exception
     */
    protected List<PhysicalEntity> listPathwayParticipants(GKInstance pathwayInstance) throws Exception {
        Set<GKInstance> eventPartcipants = InstanceUtilities.grepPathwayParticipants(pathwayInstance);
        List<PhysicalEntity> entities = new ArrayList<PhysicalEntity>();
        for (GKInstance entity : eventPartcipants) {
            PhysicalEntity convertedEntity = (PhysicalEntity) createObject(entity);
            entities.add(convertedEntity);
        }
        return entities;
    }

    /**
     * Create a Database object for the passed instance. Only dbId and _displayName have been placed in
     * the created DatabaseObject.
     * @param instance
     * @return
     * @throws Exception
     */
    public DatabaseObject createObject(GKInstance instance) throws Exception {
        DatabaseObject obj = null;
        String clsName = "org.reactome.core.model." + instance.getSchemClass().getName();
        Class<DatabaseObject> cls = (Class<DatabaseObject>) Class.forName(clsName);
        if(!cls.isEnum()) {
            obj = cls.newInstance();
            obj.setDbId(instance.getDBID());
            obj.setDisplayName(instance.getDisplayName());
            ReactomeModelPostMapper mapper = postMapperFactory.getPostMapper(instance);
            if (mapper != null)
                mapper.postShellProcess(instance, obj);
        }
        return obj;
    }

    /**
     * Set all JavaBean properties for the species object.
     *
     * @param obj
     * @throws Exception
     */
    private void fill(DatabaseObject obj, GKInstance instance) throws Exception {
        if (obj == null)
            return;
        Class cls = obj.getClass();
        // Get all properties via setXXX methods
        List<String> propNames = ReflectionUtility.getJavaBeanProperties(cls);
        if (propNames == null || propNames.size() == 0)
            return; // No writable properties are defined.
        String rAttName = null;
        for (String propName : propNames) {
            rAttName = ReflectionUtility.lowerFirst(propName);
            // Always use a simple mapper first
            if (rAttName != null && instance.getSchemClass().isValidAttribute(rAttName)) {
                List attValues = instance.getAttributeValuesList(rAttName);
                if (attValues == null || attValues.size() == 0)
                    continue;
                // Check the required type is a list or just a value
                Method setMethod = ReflectionUtility.getNamedMethod(obj, "set" + propName);
                Class argType = setMethod.getParameterTypes()[0]; // Only the first matters
                if (argType.equals(List.class)) {
                    // Need the whole list
                    // During reflection, the generic type in the list is gone.
                    // Use a really generic List type to avoid the warding from the compiler
                    List<Object> caValues = new ArrayList<Object>();
                    for (Object value : attValues) {
                        if (value instanceof GKInstance) {
                            value = createObject((GKInstance) value);
                        }
                        if (value == null)
                            continue;
                        if (!caValues.contains(value))
                            caValues.add(value);
                    }
                    // Convert Set to List.
                    setMethod.invoke(obj, new Object[]{caValues});
                } 
                else {
                    // One value is enough
                    Object value = attValues.get(0);
                    // Need to convert to caBIO type
                    if (value instanceof GKInstance) {
                        value = createObject((GKInstance) value);
                    }
                    // All other types can be converted safely
                    try {
                        setMethod.invoke(obj, new Object[]{value});
                    } catch (IllegalArgumentException e) {
                        logger.error(obj.getClass().getName() + "." + setMethod.getName() + " using " + value, e);
                    }
                }
            }
        }
        ReactomeModelPostMapper mapper = postMapperFactory.getPostMapper(instance);
        if (mapper != null)
            mapper.postProcess(instance, 
                               obj,
                               this);
    }
}
