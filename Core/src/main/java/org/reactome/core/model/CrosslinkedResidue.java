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
public class CrosslinkedResidue extends TranslationalModification {
    private DatabaseObject modification;
    private Integer secondCoordinate;
    
    public CrosslinkedResidue() {
        
    }

    public DatabaseObject getModification() {
        return modification;
    }

    public void setModification(DatabaseObject modification) {
        this.modification = modification;
    }

    public Integer getSecondCoordinate() {
        return secondCoordinate;
    }

    public void setSecondCoordinate(Integer secondCoordinate) {
        this.secondCoordinate = secondCoordinate;
    }
    
}
