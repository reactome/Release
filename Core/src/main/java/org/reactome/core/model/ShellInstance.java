/*
 * Created on Feb 28, 2012
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This simple object is used to provide simple information in some cases for instances.
 * Theoretically all subclasses in DatabaseObject can be used for this purpose. But using
 * this simple class can avoid many pitfalls related to JAXB.
 * @author gwu
 *
 */
@XmlRootElement(name="Insatnce")
@Deprecated
public class ShellInstance {
    private Long dbId;
    private String displayName;
    private String className;
    
    public ShellInstance() {
        
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
    
    
    
}
