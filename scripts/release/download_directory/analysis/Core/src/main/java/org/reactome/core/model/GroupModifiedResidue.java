/*
 * Created on Jun 26, 2012
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author gwu
 *
 */
@XmlRootElement
public class GroupModifiedResidue extends TranslationalModification {
    private DatabaseObject modification;
    
    public DatabaseObject getModification() {
        return modification;
    }

    public void setModification(DatabaseObject modification) {
        this.modification = modification;
    }

    public GroupModifiedResidue() {
        
    }
    
}
