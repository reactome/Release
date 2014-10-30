/*
 * Created on Oct 11, 2005
 *
 */
package org.reactome.core.controller;

import org.gk.model.GKInstance;

/**
 * This inteface is used to map a property in the Reactome class to a property in the caBIO
 * class.
 *
 * @author guanming
 */
public interface CustomizableMapper {

    /**
     * Set the converter so that this converter can be used by the concrete implementation of
     * CustomizableMapper. Since there should be only one converter used, the cached converted
     * object can be used via passing the sole converter.
     *
     * @param converter
     */
    public void setReactomeToCaBIOConverter(ReactomeToRESTfulAPIConverter converter);

    /**
     * Map to a property in the specified caBIO org.reactome.restfulapi.domain object. The value
     * for this property should be fetched from the specified sourceInstance.
     *
     * @param caBioTarget    the object should be mapped
     * @param caBioPropName  the property name in the target object
     * @param sourceInstance the source GKInstance object form the Reactome.
     */
    public void map(Object caBioTarget,
                    String caBioPropName,
                    GKInstance sourceInstance) throws Exception;

}
