/*
 * Created on Jun 27, 2003
 */
package org.gk.schema;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.Instance;

/**
 * 
 * @author wgm
 */
@SuppressWarnings("rawtypes")
public class GKSchemaAttribute implements SchemaAttribute, Serializable {

	private String id;
	private String name;
	private Map owners = new HashMap();
	private SchemaClass origin;
	private Map allowedClasses = new HashMap();
	private Class type;
	private int definingType = UNDEFINED;
	private boolean isMultiple = false;
	private int minCardinality = 0;
	private int maxCardinality = -1;
	//private Set inverseAttributes = new HashSet();
	private SchemaAttribute inverseAttribute;
	private int typeAsInt;
	private Object defaultValue;
	private int category = UNDEFINED;
	// A special case for String type: a list of allowed values have been provided
	private List<String> allowedValues;

	public GKSchemaAttribute() {
	}
	
	public GKSchemaAttribute(String id) {
		setId(id);
	}
	
	public List<String> getAllowedValues() {
	    return this.allowedValues;
	}
	
	public void setAllowedValues(List<String> values) {
	    this.allowedValues = values;
	}

	/* 
	 * @see org.gk.schema.SchemaAttribute#getName()
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/* 
	 * @see org.gk.schema.SchemaAttribute#getSchemaClass()
	 */
	public Collection getSchemaClass() {
		return owners.values();
	}

	public void addSchemaClass(SchemaClass owner) {
		owners.put((Object) owner.getName(), (Object) owner);
	}

	/*
	* @see org.gk.schema.SchemaAttribute#getInverseSchemaAttribute()
	*/
	public SchemaAttribute getInverseSchemaAttribute() {
		return inverseAttribute;
	}

	public void setInverseSchemaAttribute(SchemaAttribute inverseAttribute) {
		this.inverseAttribute = inverseAttribute;
	}

	/* 
	 * @see org.gk.schema.SchemaAttribute#getAllowedClasses()
	 */
	public Collection getAllowedClasses() {
		return allowedClasses.values();
	}

	public void addAllowedClass(SchemaClass allowedClass) {
		allowedClasses.put(
			((GKSchemaClass) allowedClass).getId(),
			allowedClass);
	}

	/* 
	* @see org.gk.schema.SchemaAttribute#getDefiningType()
	*/
	public int getDefiningType() {
		return definingType;
	}

	public void setDefiningType(int definingType) {
		this.definingType = definingType;
	}

	public void setDefiningType(String definingType) {
		if (definingType == null) {
			setDefiningType(NONE_DEFINING);
		} else if (definingType.toLowerCase().equals("all")) {
			setDefiningType(ALL_DEFINING);
		} else if (definingType.toLowerCase().equals("any")) {
			setDefiningType(ANY_DEFINING);
		} else {
			System.err.println(
				"Unknown SchemaAttribute's definingType " + definingType);
		}
	}

	/* 
	* @see org.gk.schema.SchemaAttribute#getMaxCardinality()
	*/
	public int getMaxCardinality() {
		return maxCardinality;
	}

	public void setMaxCardinality(int maxCardinality) {
		this.maxCardinality = maxCardinality;
	}

	/* 
	* @see org.gk.schema.SchemaAttribute#getMinCardinality()
	*/
	public int getMinCardinality() {
		return minCardinality;
	}

	public void setMinCardinality(int minCardinality) {
		this.minCardinality = minCardinality;
	}

	/* 
	 * @see org.gk.schema.SchemaAttribute#getOrigin()
	 */
	public SchemaClass getOrigin() {
		return origin;
	}

	public void setOrigin(SchemaClass origin) {
		this.origin = origin;
	}

	/* 
	 * @see org.gk.schema.SchemaAttribute#getType()
	 */
	public Class getType() {
		return type;
	}

	public void setType(Class type) {
		this.type = type;
	}

	/* 
	 * @see org.gk.schema.SchemaAttribute#isMultiple()
	 */
	public boolean isMultiple() {
		return isMultiple;
	}

	public boolean isOriginMuliple() throws InvalidAttributeException {
		return getOrigin().getAttribute(this.getName()).isMultiple();
	}

	public void setMultiple(boolean isMultiple) {
		this.isMultiple = isMultiple;
	}

	public String toString() {
		return getId();
//		if (origin == null) {
//			return getName();
//		} else {
//			return origin.getName() + ":" + getName();
//		}
	}
	
	/**
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param string
	 */
	private void setId(String string) {
		id = string;
	}

	public boolean isValidValueOrThrow(Object value) throws InvalidAttributeValueException {
		if (isValidValue(value)) {
			return true;
		} else {
			throw(new InvalidAttributeValueException(this, value));
		}
	}

	public boolean areValidValuesOrThrow(List valueList) throws InvalidAttributeValueException {
		Iterator i = valueList.iterator();
		while (i.hasNext()) {
			Object value = i.next();
			isValidValueOrThrow(value);
		}
		return true;
	}

	public boolean isValidValue(Object value) {
		if (! getType().isInstance(value)) {
			return false;
		}
		if (value instanceof org.gk.model.Instance) {
			Instance instance = (Instance) value;
			Iterator i = getAllowedClasses().iterator();
			while (i.hasNext()) {
				SchemaClass sc = (SchemaClass) i.next();
				if (instance.getSchemClass().isa(sc)) {
					return true;
				}
			}
			return false;
		} else {
			// Assume here that attributes which accept non-instance
			// values don't have any value restrictions.
			return true;
		}
	}
	
	/**
	 * Check a specified SchemaClass is a valid class for this
	 * GKSchemaAttribute object.
	 * @param cls
	 * @return
	 */
	public boolean isValidClass(SchemaClass cls) {
	    SchemaClass sc;
		Iterator i = getAllowedClasses().iterator();
		while (i.hasNext()) {
			sc = (SchemaClass) i.next();
			if (cls.isa(sc)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInstanceTypeAttribute() {
		return (typeAsInt == INSTANCE_TYPE);
	}

	/* (non-Javadoc)
	 * @see org.gk.schema.SchemaAttribute#getTypeAsInt()
	 */
	public int getTypeAsInt() {
		return typeAsInt;
	}

	public void setTypeAsInt(int typeAsInt) {
		this.typeAsInt = typeAsInt;
	}
	
	public SchemaAttribute getOriginalAttribute() {
		return ((GKSchemaClass) origin).getAttributeNoCheck(name);
	}

	/**
	 * @return Returns the defaultValue.
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}
	/**
	 * @param defaultValue The defaultValue to set.
	 */
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}
	/**
	 * @return Returns the category.
	 */
	public int getCategory() {
		return category;
	}
	/**
	 * @param category The category to set.
	 */
	public void setCategory(int category) {
		this.category = category;
	}
	
	public void setCategory(String category) {
		if (category.equalsIgnoreCase("MANDATORY")) {
			setCategory(SchemaAttribute.MANDATORY);
		} else if (category.equalsIgnoreCase("REQUIRED")) {
			setCategory(SchemaAttribute.REQUIRED);
		} else if (category.equalsIgnoreCase("OPTIONAL")) {
			setCategory(SchemaAttribute.OPTIONAL);
		} else if (category.equalsIgnoreCase("NOMANUALEDIT")) {
			setCategory(SchemaAttribute.NOMANUALEDIT);
		} else {
			System.err.println(
				"Unknown SchemaAttribute's category " + category);
		}	
	}
}
