/*
 * Created on Jan 30, 2013
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * A wrapper for an ArrayList so that JAXB can work. This should be used as few as possible, and only in
 * cases like List<List<Event>>, since nothing works.
 * @author gwu
 *
 */
@XmlRootElement(name="DatabaseObjects")
public class DatabaseObjectList {
    private List<? extends DatabaseObject> databaseObject;
        
    public DatabaseObjectList() {
    }
    
    public DatabaseObjectList(List<? extends DatabaseObject> objects) {
        this.databaseObject = objects;
    }
    
    @XmlElements(
                 {
                     @XmlElement(type = Pathway.class),
                     @XmlElement(type = ReactionlikeEvent.class),
                     @XmlElement(type = PhysicalEntity.class),
                 }
            )    
    public List<? extends DatabaseObject> getDatabaseObject() {
        return databaseObject;
    }

    public void setDatabaseObject(List<? extends DatabaseObject> databaseObjects) {
        this.databaseObject = databaseObjects;
    }

    
}
