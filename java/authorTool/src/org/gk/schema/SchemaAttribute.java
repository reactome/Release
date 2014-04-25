/*
 * Created on Jun 27, 2003
 */
package org.gk.schema;

import java.io.Serializable;

/**
 * This interface describes the attributes for SchemaClasses.
 * @author wgm
 */
public interface SchemaAttribute extends Serializable {
	// These constants are used in attribute comparisons. ALL means all
	// values should be identical, ANY only needs one, NONE no needing 
	// for comparison.
	public static final int ALL_DEFINING = 2;
	public static final int ANY_DEFINING = 1;
	public static final int NONE_DEFINING = 0;
	public static final int UNDEFINED = -1;
	
	public static final int INSTANCE_TYPE = 1;
	public static final int STRING_TYPE = 2;
	public static final int INTEGER_TYPE = 3;
	public static final int LONG_TYPE = 4;
	public static final int FLOAT_TYPE = 5;
	public static final int BOOLEAN_TYPE = 6;
	public static final int ENUM_TYPE = 7;
	
	public static final int MANDATORY = 1;
	public static final int REQUIRED = 2;
	public static final int OPTIONAL = 3;
	public static final int NOMANUALEDIT = 4;
	
	/**
	 * Get the name of the Attribute.
	 * @return
	 */
	public String getName();
	
	/**
	 * Get the owner SchemaClass for the Attribute.
	 * @return
	 */
	public java.util.Collection getSchemaClass();
	
	/**
	 * Get the inverse SchemaAttribute. A inverse SchemaAttribute is another
	 * attribute whose valuse is the inverse of this attribute value, e.g., Reaction is
	 * a Component of Pathway. Attribute isComponentOf in Reaction is the inverse attribute of
	 * hasComponent in Pathway.
	 * @return the inverse SchemaAttribute
	 */
	public SchemaAttribute getInverseSchemaAttribute();
	
	/**
	 * Get the topmost schema class containing this attribute.
	 * @return
	 */
	public SchemaClass getOrigin();
	
	public boolean isMultiple();
	
	public int getMinCardinality();
	
	public int getMaxCardinality();
	
	public Class getType();
	
	public int getTypeAsInt();
	
	public java.util.Collection getAllowedClasses();
	
	/**
	 * Strictness means all values in a list should be the same.
	 * @return one of ALL_DEFINING, ANY_DEFINING, and NONE_DEFINING.
	 */
	public int getDefiningType();
	
	public boolean isValidValue(Object value);
	
	public boolean isInstanceTypeAttribute();
	
	public int getCategory();
}
