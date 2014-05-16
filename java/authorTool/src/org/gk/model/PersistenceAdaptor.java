/*
 * Created on Nov 11, 2003
 */
package org.gk.model;

import java.util.Collection;

import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * An interface to connect to a persistence layer to load the necessary 
 * information.
 * @author wugm
 */
public interface PersistenceAdaptor {

	public Collection fetchInstanceByAttribute(SchemaAttribute attribute, 
	                                           String operator,
	                                           Object value) throws Exception;
	                                           
	public Collection fetchInstanceByAttribute(String className,
							                   String attributeName,
							                   String operator,
							                   Object value) throws Exception;
	
	public Collection fetchInstancesByClass(SchemaClass class1) throws Exception;
	
	public void loadInstanceAttributeValues(GKInstance instace, 
	                                        SchemaAttribute attribute) throws Exception;
	                                        
	public void updateInstanceAttribute(GKInstance instance, String attributeName) throws Exception;	 
	       
	public Long storeInstance(GKInstance instance) throws Exception;
	       
	public Schema getSchema();	   
	
	public Schema fetchSchema() throws Exception;    
	
	public long getClassInstanceCount(SchemaClass schemaClass) throws Exception;                                    

	public GKInstance fetchInstance(String className, Long dbID) throws Exception;
	
	public GKInstance fetchInstance(Long reactomeId) throws Exception;
}
