/*
 * Created on Jun 27, 2003
 */
package org.gk.model;

import org.gk.schema.SchemaAttribute;

/**
 * For query.
 * @author wgm
 */
public class Query {
	private SchemaAttribute attribute;
	private java.util.List values; // attribute values;
	private String operator;
	
	public Query(SchemaAttribute attribute, java.util.List values, String operator) {
		this.attribute = attribute;
		this.values = values;
		this.operator = operator;
	}
	
	public SchemaAttribute getAttribute() {
		return attribute;
	}
	
	public java.util.List getValues() {
		return values;
	}
	
	public String getOperator() {
		return operator;
	}
	
}
