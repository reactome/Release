/*
 * Created on Sep 21, 2006
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/** 
 *  getTests
 *  
 * @author croft
 */
public class InstanceBiologicalMeaning {
	private List commonAttributeNames = null;
	private List retiredAttributeNames = null;
	private List newAttributeNames = null;

	private String newSchemaClassName = null;
	private List deletedAttributesWithContent = null;
	private List addedAttributesWithContent = null;
	private List changedAttributes = null;
	
	public InstanceBiologicalMeaning(SchemaClass currentSchemaClass, SchemaClass previousSchemaClass) {
		generateAttributeNameLists(currentSchemaClass, previousSchemaClass);
	}
	
	/**
	 * Resets to null all of the internal variables used to record
	 * the differences between a pair of instances.
	 *
	 */
	public void resetChangeRecords () {
		newSchemaClassName = null;
		deletedAttributesWithContent = null;
		addedAttributesWithContent = null;
		changedAttributes = null;		
	}
	
	private void generateAttributeNameLists(SchemaClass currentSchemaClass, SchemaClass previousSchemaClass) {
		commonAttributeNames = getCommonAttributeNames(previousSchemaClass, currentSchemaClass);
		retiredAttributeNames = getRetiredAttributeNames(previousSchemaClass, currentSchemaClass);
		newAttributeNames = getNewAttributeNames(previousSchemaClass, currentSchemaClass);
	}
	
	/**
	 * Compares the attributes and schema classes from two instances. If
	 * anything has changed, returns true.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	public boolean isBiologicalMeaningChanged(GKInstance instance1, GKInstance instance2, List schemaChangeIgnoredAttributes) {
		// Check for change in instance type
		if (schemaClassChanged(instance1, instance2))
			return true;
		
		// Check for deletion of attributes with content
		if (attributesDeleted(instance1, instance2, schemaChangeIgnoredAttributes))
			return true;
		
		// Check for addition of attributes with content
		if (attributesAdded(instance1, instance2, schemaChangeIgnoredAttributes))
			return true;
		
		// Check for changes in common in attributes
		if (attributesChanged(instance1, instance2, schemaChangeIgnoredAttributes))
			return true;
		
		return false;
	}
	
	/**
	 * Compares the schema classes from two instances. If anythinh has
	 * changed, returns true.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	public boolean schemaClassChanged(GKInstance instance1, GKInstance instance2) {
		if (!instance1.getSchemClass().getName().equals(instance2.getSchemClass().getName()))
			return true;
		return false;
	}
	
	/**
	 * Compares the attributes from two instances. If
	 * any old attributes have been deleted, returns true.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	private boolean attributesDeleted(GKInstance instance1, GKInstance instance2, List schemaChangeIgnoredAttributes) {
		// Test
		long db_id = instance1.getDBID().longValue();
		
		// Check for deletion of attributes with content
		for (Iterator it = retiredAttributeNames.iterator(); it.hasNext();) {
			String name = (String)it.next();
			
			// Attributes to be ignored in the comparison
			if (name.equals("stableIdentifier") ||
					name.equals("modified") ||
					name.equals("_Protege_id") ||
					name.equals("_timestamp") ||
					name.equals("doi") ||
					schemaChangeIgnoredAttributes.contains(name))
				continue;
			
			List attributes1 = null;
			try {
				attributes1 = instance1.getAttributeValuesList(name);
			} catch (Exception e) {
				System.err.println("InstanceBiologicalMeaning.attributesDeleted: problem getting name=" + name);
//				e.printStackTrace();
				continue;
			}
			
			if (attributes1!=null && attributes1.size()>0 && attributes1.get(0)!=null)
				return true;
		}
		
		return false;
	}
	
	/**
	 * Compares the attributes from two instances. If
	 * any new attributes have been added, returns true.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	private boolean attributesAdded(GKInstance instance1, GKInstance instance2, List schemaChangeIgnoredAttributes) {
		// Test
		long db_id = instance1.getDBID().longValue();
		
		// Check for addition of attributes with content
		for (Iterator it = newAttributeNames.iterator(); it.hasNext();) {
			String name = (String)it.next();
			
			// Attributes to be ignored in the comparison
			if (name.equals("stableIdentifier") ||
					name.equals("modified") ||
					name.equals("_Protege_id") ||
					name.equals("_timestamp") ||
					schemaChangeIgnoredAttributes.contains(name))
				continue;
			
			List attributes2 = null;
			try {
				attributes2 = instance2.getAttributeValuesList(name);
			} catch (Exception e) {
				System.err.println("InstanceBiologicalMeaning.attributesAdded: problem getting name=" + name);
//				e.printStackTrace();
				continue;
			}
			
			if (attributes2!=null && attributes2.size()>0 && attributes2.get(0)!=null)
				return true;
		}
			
		return false;	
	}
	
	/**
	 * Compares the attributes from two instances. If
	 * any attributes have been changed, returns true.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	private boolean attributesChanged(GKInstance instance1, GKInstance instance2, List schemaChangeIgnoredAttributes) {
		// Test
		long db_id = instance1.getDBID().longValue();
		
		// Check for changes in common in attributes
		for (Iterator it = commonAttributeNames.iterator(); it.hasNext();) {
			String name = (String)it.next();
			
			// Attributes to be ignored in the comparison
			if (name.equals("stableIdentifier") ||
					name.equals("modified") ||
					name.equals("_Protege_id") ||
					name.equals("_timestamp") ||
					schemaChangeIgnoredAttributes.contains(name))
				continue;
			
			List attributes1 = null;
			List attributes2 = null;
			try {
				attributes1 = instance1.getAttributeValuesList(name);
				attributes2 = instance2.getAttributeValuesList(name);
			} catch (Exception e) {
				System.err.println("InstanceBiologicalMeaning.attributesChanged: problem getting name=" + name);
//				e.printStackTrace();
				continue;
			}
			
			if (attributes1==null && attributes2==null)
				continue;
			else if (attributes1==null || attributes2==null)
				return true;
			else {
				if (attributes1.size()!=attributes2.size())
					return true;
				else {
					for (int i=0; i<attributes1.size(); i++)
						if (attributeChanged(attributes1.get(i), attributes2.get(i)))
							return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Find all of the things that have chenged between two instances, and
	 * make a note of the changes.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	public void biologicalMeaningChanged(GKInstance instance1, GKInstance instance2) {
		// Check for change in instance type
		computeNewSchemaClassName(instance1, instance2);
		
		// Check for deletion of attributes with content
		computeDeletedAttributesWithContent(instance1, instance2);
		
		// Check for addition of attributes with content
		computeAddedAttributesWithContent(instance1, instance2);
		
		// Check for changes in common in attributes
		computeChangedAttributes(instance1, instance2);
	}
	
	/**
	 * Compares the schema classes from two instances. If anythinh has
	 * changed, returns the new name, otherwise return null.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	private void computeNewSchemaClassName(GKInstance instance1, GKInstance instance2) {
		if (instance1.getSchemClass().getName().equals(instance2.getSchemClass().getName()))
			newSchemaClassName = null;
		else
			newSchemaClassName = instance2.getSchemClass().getName();
	}
	
	/**
	 * Compares the attributes from two instances. If
	 * any old attributes have been deleted, the names are
	 * added onto a list, which is returned.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	private void computeDeletedAttributesWithContent(GKInstance instance1, GKInstance instance2) {
		deletedAttributesWithContent = new ArrayList();
		
		// Check for deletion of attributes with content
		for (Iterator it = retiredAttributeNames.iterator(); it.hasNext();) {
			String name = (String)it.next();
			
			// Attributes to be ignored in the comparison
			if (name.equals("stableIdentifier"))
				continue;
			
			List attributes1 = null;
			try {
				attributes1 = instance1.getAttributeValuesList(name);
			} catch (Exception e) {
				System.err.println("InstanceBiologicalMeaning.computeDeletedAttributesWithContent: problem getting name=" + name);
//				e.printStackTrace();
				continue;
			}
			
			if (attributes1!=null && attributes1.size()>0 && attributes1.get(0)!=null)
				deletedAttributesWithContent.add(name);
		}
	}
	
	/**
	 * Compares the attributes from two instances. If
	 * any new attributes have been added, the names are
	 * added onto a list, which is returned.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	private void computeAddedAttributesWithContent(GKInstance instance1, GKInstance instance2) {
		addedAttributesWithContent = new ArrayList();
		
		for (Iterator it = newAttributeNames.iterator(); it.hasNext();) {
			String name = (String)it.next();
			
			// Attributes to be ignored in the comparison
			if (name.equals("stableIdentifier"))
				continue;
			
			List attributes2 = null;
			try {
				attributes2 = instance2.getAttributeValuesList(name);
			} catch (Exception e) {
				System.err.println("InstanceBiologicalMeaning.attributesChanged: problem getting name=" + name);
//				e.printStackTrace();
				continue;
			}
			
			if (attributes2!=null && attributes2.size()>0 && attributes2.get(0)!=null)
				addedAttributesWithContent.add(name);
		}
	}	
	
	/**
	 * Compares the attributes from two instances. If
	 * any attributes have been changed, the names are
	 * added onto a list, which is returned.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return
	 */
	private void computeChangedAttributes(GKInstance instance1, GKInstance instance2) {
		changedAttributes = new ArrayList();
			
		// Check for changes in common in attributes
		for (Iterator it = commonAttributeNames.iterator(); it.hasNext();) {
			String name = (String)it.next();
			
			// Attributes to be ignored in the comparison
			if (name.equals("stableIdentifier"))
				continue;
			
			List attributes1 = null;
			List attributes2 = null;
			try {
				attributes1 = instance1.getAttributeValuesList(name);
				attributes2 = instance2.getAttributeValuesList(name);
			} catch (Exception e) {
				continue;
			}
			
			if (attributes1==null && attributes2==null)
				continue;
			else if (attributes1==null || attributes2==null)
				changedAttributes.add(name);
			else {
				if (attributes1.size()!=attributes2.size())
					changedAttributes.add(name);
				else {
					for (int i=0; i<attributes1.size(); i++)
						if (attributeChanged(attributes1.get(i), attributes2.get(i)))
							changedAttributes.add(name);
				}
			}
		}
	}
	
	public String getNewSchemaClassName() {
		return newSchemaClassName;
	}

	public List getDeletedAttributesWithContent() {
		return deletedAttributesWithContent;
	}

	public List getAddedAttributesWithContent() {
		return addedAttributesWithContent;
	}

	public List getChangedAttributes() {
		return changedAttributes;
	}

	private boolean attributeChanged(Object value1, Object value2) {
		if (value1==null && value2==null)
			return false;
		else if (value1==null || value2==null)
			return true;
		else if (value1 instanceof GKInstance && value2 instanceof GKInstance) {
			GKInstance instanceValue1 = (GKInstance)value1;
			GKInstance instanceValue2 = (GKInstance)value2;
			Long dbId1 = instanceValue1.getDBID();
			Long dbId2 = instanceValue2.getDBID();
			
			if (dbId1.longValue()!=dbId2.longValue())
				return true;
		} else if (value1 instanceof Collection && value2 instanceof Collection) {
			Collection collection1 = (Collection)value1;
			Collection collection2 = (Collection)value2;
			
			if (collection1.size() != collection2.size())
				return true;
		} else if (value1 instanceof Boolean && value2 instanceof Boolean) {
			Boolean boolean1 = (Boolean)value1;
			Boolean boolean2 = (Boolean)value2;
			
			if (boolean1.booleanValue()!=boolean2.booleanValue())
				return true;
		} else if (value1 instanceof Long && value2 instanceof Long) {
			Long long1 = (Long)value1;
			Long long2 = (Long)value2;
			
			if (long1.longValue()!=long2.longValue())
				return true;
		} else if (value1 instanceof String && value2 instanceof String) {
			String stringValue1 = (String)value1;
			String stringValue2 = (String)value2;
			
			if (!stringValue1.equals(stringValue2))
				return true;
		} else {
			if (value1!=value2)
				return true;
		}
		
		return false;
	}
	
	/**
	 * Gets the names of the attributes in schemaClass.
	 * 
	 * @param schemaClass
	 * @return
	 */
	private List getAttributeNames(SchemaClass schemaClass) {
		Collection attributes = schemaClass.getAttributes();
		
		List attributeNames = new ArrayList();
		
		String name;
		SchemaAttribute schemaAttribute;
		for (Iterator it = attributes.iterator(); it.hasNext();) {
			schemaAttribute = (SchemaAttribute)it.next();
			name = schemaAttribute.getName();
			attributeNames.add(name);
		}
		
		return attributeNames;
	}
		
	/**
	 * Gets the names of the attributes common to both schemaClass1
	 * and schemaClass2.
	 * 
	 * @param schemaClass1
	 * @param schemaClass2
	 * @return
	 */
	private List getCommonAttributeNames(SchemaClass schemaClass1, SchemaClass schemaClass2) {
		if (schemaClass1==null)
			return getAttributeNames(schemaClass2);
		
		Collection attributes1 = schemaClass1.getAttributes();
		
		List commonAttributeNames = new ArrayList();
		
		String name1;
		SchemaAttribute schemaAttribute;
		for (Iterator it = attributes1.iterator(); it.hasNext();) {
			schemaAttribute = (SchemaAttribute)it.next();
			name1 = schemaAttribute.getName();
			for (Iterator it1 = attributes1.iterator(); it1.hasNext();) {
				schemaAttribute = (SchemaAttribute)it1.next();
				if (schemaAttribute.getName().equals(name1)) {
					commonAttributeNames.add(name1);
					break;
				}
			}
		}
		
		return commonAttributeNames;
	}
		
	private List getRetiredAttributeNames(SchemaClass schemaClass1, SchemaClass schemaClass2) {
		List commonAttributeNames = getCommonAttributeNames(schemaClass1, schemaClass2);
		
		List retiredAttributeNames = new ArrayList();
		
		if (schemaClass1==null)
			return retiredAttributeNames;

		Collection attributes1 = schemaClass1.getAttributes();
		
		String name1;
		SchemaAttribute schemaAttribute;
		for (Iterator it = attributes1.iterator(); it.hasNext();) {
			schemaAttribute = (SchemaAttribute)it.next();
			name1 = schemaAttribute.getName();
			
			if (!commonAttributeNames.contains(name1))
				retiredAttributeNames.add(name1);
		}
		
		return retiredAttributeNames;
	}
	
	private List getNewAttributeNames(SchemaClass schemaClass1, SchemaClass schemaClass2) {
		List commonAttributeNames = getCommonAttributeNames(schemaClass1, schemaClass2);
		
		List newAttributeNames = new ArrayList();

		if (schemaClass1==null)
			return newAttributeNames;

		Collection attributes2 = schemaClass2.getAttributes();
		
		String name2;
		SchemaAttribute schemaAttribute;
		for (Iterator it = attributes2.iterator(); it.hasNext();) {
			schemaAttribute = (SchemaAttribute)it.next();
			name2 = schemaAttribute.getName();
			
			if (!commonAttributeNames.contains(name2))
				newAttributeNames.add(name2);
		}
		
		return newAttributeNames;
	}
	
}