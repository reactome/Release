/*
 * Created on Feb 27, 2013
 *
 */
package org.reactome.core.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * This class is used to create a map from a PhysicalEntity to one or more
 * ReferenceEntity instances. The relationship is recorded as DB_IDs to 
 * DB_IDs.
 * @author gwu
 *
 */
@XmlRootElement
public class PhysicalToReferenceEntityMap {
    private Long peDbId;
    private String displayName;
    private String schemaClass;
    private List<Long> refDbIds;
    private List<ReferenceEntity> refEntities;
    
    public PhysicalToReferenceEntityMap() {
    }

    public Long getPeDbId() {
        return peDbId;
    }
    
    public String getDisplayName(){
    	return displayName;
    }
    
    public String getSchemaClass() {
		return schemaClass;
	}

	public void setPeDbId(Long peDbId) {
        this.peDbId = peDbId;
    }

    public List<Long> getRefDbIds() {
        return refDbIds;
    }

    public void setRefDbIds(List<Long> refDbIds) {
        this.refDbIds = refDbIds;
    }
    
    public void setDisplayName(String displayName){
    	this.displayName = displayName;
    }
    
	public void setSchemaClass(String schemaClass) {
		this.schemaClass = schemaClass;
	}

	public List<ReferenceEntity> getRefEntities() {
		return refEntities;
	}

	public void setRefEntities(List<ReferenceEntity> refEntities) {
		this.refEntities = refEntities;
	}
    
}
