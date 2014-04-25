/*
 * Created on Jun 30, 2003
 */
package org.gk.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * The default implementation of Instance for GK.
 * @author wgm
 */
@SuppressWarnings("rawtypes")
public class GKInstance implements Instance, Cloneable {
	// For holding attributes: Keys are the names of the attributes
	// values are the values of the attributes.
	private Map attributes = new HashMap();
	// This shouldn't be here (?)
	///// For display and label
	// private String displayName;
	private Long dbID; // a unique id for db
	// SchemaClass is the defintion of Instance
	private SchemaClass schemaClass;
	private Map referers = new HashMap();
	//private Map referersByName = new HashMap();
	private transient PersistenceAdaptor dbAdaptor;
	private boolean isInflated = false;
	// Mark if a GKInstance is a shell
	private boolean isShell = false;
    // Mark is a GKInstance has been modified
    private boolean isDirty = false;
	public boolean debug = false;
	
	public GKInstance() {
	}
	
	public GKInstance(String displayName) {
		this();
		setDisplayName(displayName);
	}
	
	public GKInstance(SchemaClass schemaClass) {
		this.schemaClass = schemaClass;
	}
	
	public GKInstance(SchemaClass schemaClass, String displayName) {
		this(schemaClass);
		setDisplayName(displayName);
	}

	public GKInstance(SchemaClass schemaClass, Long dbID, PersistenceAdaptor dbAdaptor) {
		this(schemaClass);
		this.dbID = dbID;
		this.dbAdaptor = dbAdaptor;
	}
	
	/* 
	 * @see org.gk.model.Instance#getSchemClass()
	 */
	public SchemaClass getSchemClass() {
		return this.schemaClass;
	}

	/* 
	 * @see org.gk.model.Instance#setSchemaClass(org.gk.schema.SchemaClass)
	 */
	public void setSchemaClass(SchemaClass schemaClass) {
		this.schemaClass = schemaClass;
	}

	/* 
	 * @see org.gk.model.Instance#getSchemaAttributes()
	 */
	public Collection getSchemaAttributes() {
		if (schemaClass != null)
			return schemaClass.getAttributes();
		return null;
	}

	/* 
	 * @see org.gk.model.Instance#setAttributeValue(java.lang.String, java.lang.Object)
	 */
	public void setAttributeValue(String attributeName, Object value) throws InvalidAttributeException, InvalidAttributeValueException {
		setAttributeValue(schemaClass.getAttribute(attributeName), value);
	}
    
    /**
     * Remove all values from the specified attribute slot.
     * @param attributeName
     */
    public void emptyAttributeValues(String attributeName) throws InvalidAttributeException {
        ((GKSchemaClass)schemaClass).isValidAttributeOrThrow(attributeName);
        SchemaAttribute att = (SchemaAttribute) schemaClass.getAttribute(attributeName);
        attributes.remove(att);
    }

	/* 
	 * @see org.gk.model.Instance#setAttributeValue(org.gk.schema.SchemaAttribute, java.lang.Object)
	 */
	public void setAttributeValue(SchemaAttribute attribute, Object value) throws InvalidAttributeException, InvalidAttributeValueException {
		((GKSchemaClass) schemaClass).isValidAttributeOrThrow(attribute);
		if (attribute.isMultiple()) {
			setMultiValueAttribute((GKSchemaAttribute) attribute, value);
		} else {
			setSingleValueAttribute((GKSchemaAttribute) attribute, value);
		}
	}
	
	public void addAttributeValueNoCheck(String attName, Object value) {
		if (value == null)	
			return;
		List values = (java.util.List) attributes.get(attName);
		if (values == null) {
			values = new ArrayList();
			attributes.put(attName, values);
		}
		values.add(value);
	}
	
	public void removeAttributeValueNoCheck(String attName, Object value) {
		List values = (java.util.List) attributes.get(attName);
		if (values != null) {
			//System.out.println("Before remove:\t" + values);
			values.remove(value);
			//System.out.println("After remove:\t" + values);
		}
	}

	public void addAttributeValueNoCheck(SchemaAttribute attribute, Object value) {
		List valueList;
		if ((valueList = (List) attributes.get(attribute.getName())) == null) {
			valueList = new ArrayList();
			attributes.put(attribute.getName(), valueList);
		}
		if (value == null) {
			System.err.println("Attempting to add null value to attribute '" + attribute.getName() + "'.");
			return;
		}
		if (value instanceof java.util.List) {
			valueList.addAll((Collection) value);
		} else {
			valueList.add(value);
		}
	}

	public void setAttributeValuePosition(GKSchemaAttribute attribute, int index, Object value) throws InvalidAttributeException, InvalidAttributeValueException {
		((GKSchemaClass) schemaClass).isValidAttributeOrThrow(attribute);
		attribute.isValidValueOrThrow(value);
		setAttributeValuePositionNoCheck(attribute, index, value);
	}

	public void setAttributeValuePositionNoCheck(SchemaAttribute attribute, int index, Object value) {
		List valueList;
		if ((valueList = (List) attributes.get(attribute.getName())) == null) {
			valueList = new ArrayList();
			attributes.put(attribute.getName(), valueList);
		}
		if (value == null) {
			System.err.println("Attempting to set null value to attribute '" + attribute.getName() + "' in position " + index + "'.");
			return;
		}
        if (index < valueList.size())
            valueList.set(index, value);
        else {
            // have to increase the size of valueList first to make set call happy
            int originalSize = valueList.size();
            for (int i = 0; i < index - originalSize; i++)
                valueList.add(null);
            valueList.add(value);
        }
//		if (index >= valueList.size()) {
//			valueList.setSize(index + 1);
//		}
//		valueList.set(index, value);
	}

	/**
	 * (re)sets an attribute value w/o checking the validity of neither attribute
	 * nor value. null value causes the attribute value to be set to an empty List
	 * (thus indicating that the value has been set to empty).
	 * 
	 * @param attribute
	 * @param value
	 * @return List containing the previous value(s) of this attribute or null
	 * if the attribute hasn't been set or added to before.
	 */

	public Object setAttributeValueNoCheck(SchemaAttribute attribute, Object value) {
		return setAttributeValueNoCheck(attribute.getName(), value);
	}

	public Object setAttributeValueNoCheck(String attributeName, Object value) {
		if (value instanceof java.util.List) {
			return attributes.put(attributeName, value);
		} else {
			List valueList = new ArrayList();
			if (value != null) {
				valueList.add(value);
			}
			return attributes.put(attributeName, valueList);
		}
	}

	private Object setMultiValueAttribute(
		GKSchemaAttribute attribute,
		Object value) throws InvalidAttributeValueException {
		if (value != null) {
			if (value instanceof java.util.List) {
				attribute.areValidValuesOrThrow((List) value);
			} else {
				attribute.isValidValueOrThrow(value);
			}
		}
		return setAttributeValueNoCheck(attribute, value);
	}

	private Object setSingleValueAttribute(GKSchemaAttribute attribute, Object value) throws InvalidAttributeValueException {
		if (value != null) {
			attribute.isValidValueOrThrow(value);
		}
		return setAttributeValueNoCheck(attribute, value);
	}
	
	/* 
	 * @see org.gk.model.Instance#getAttributeValue(java.lang.String)
	 */
	public Object getAttributeValue(String attributeName) throws InvalidAttributeException, Exception {
		return getAttributeValue(schemaClass.getAttribute(attributeName));
	}

	/* 
	 * @see org.gk.model.Instance#getAttributeValue(org.gk.schema.SchemaAttribute)
	 */
	public Object getAttributeValue(SchemaAttribute attribute) throws Exception {
		//System.err.println("public Object getAttributeValue(SchemaAttribute attribute)");
		((GKSchemaClass) schemaClass).isValidAttributeOrThrow(attribute);
		return getAttributeValueNoCheck(attribute);
	}

	public Object getAttributeValueNoCheck(SchemaAttribute attribute) {
		//System.err.println("public Object getAttributeValueNoCheck(SchemaAttribute attribute)");
		if (! isAttributeValueLoaded(attribute) && ! isInflated) {
			try {
				loadAttributeValues((GKSchemaAttribute) attribute);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return getAttributeValueNoCheck(attribute.getName());
	}

	public Object getAttributeValueNoCheck(String attributeName) {
		Object o;
		if ((o = attributes.get(attributeName)) == null) {
			return null;
		} else {
			List valueList = (List) o;
			if (valueList.isEmpty()) {
				return null;
			} else {
				return valueList.get(0);
			}
		}
	}
	
	public java.util.List getAttributeValuesListNoCheck(String attributeName) {
		return (java.util.List) attributes.get(attributeName);
	}


	/*
	 * returns attribute values as a List regardless of if 
	 * it's a single value or a List already.
	 */

	public List getAttributeValuesList(SchemaAttribute attribute) throws Exception {
		((GKSchemaClass) schemaClass).isValidAttributeOrThrow(attribute);
		if (! isAttributeValueLoaded(attribute) && ! isInflated) {
			loadAttributeValues((GKSchemaAttribute) attribute);
            // Mark it if the value is null
            if (!attributes.containsKey(attribute.getName()))
                attributes.put(attribute.getName(), new ArrayList()); // To avoid returning null
		}
		Object o;
		if ((o = attributes.get(attribute.getName())) == null) {
		    List list = new ArrayList();
		    attributes.put(attribute.getName(), list);
			return list; // Have to make sure the returned list is the cached one!
		} else {
			return (List) o;
		}
	}

	public List getAttributeValuesList(String attributeName) throws InvalidAttributeException, Exception {
		return getAttributeValuesList(schemaClass.getAttribute(attributeName));
	}

	/* 
	 * @see org.gk.model.Instance#addAttributeValue(java.lang.String, java.lang.Object)
	 */
	public void addAttributeValue(String attributeName, Object value) throws InvalidAttributeException, InvalidAttributeValueException {
		addAttributeValue(schemaClass.getAttribute(attributeName), value);
	}

	/* 
	 * @see org.gk.model.Instance#addAttributeValue(org.gk.schema.SchemaAttribute, java.lang.Object)
	 */
	public void addAttributeValue(SchemaAttribute attribute, Object value) throws InvalidAttributeException, InvalidAttributeValueException {
		((GKSchemaClass) schemaClass).isValidAttributeOrThrow(attribute);
		if (attribute.isMultiple()) {
			addMultiValueAttributeValue(attribute, value);
		} else {
			Object previous = setSingleValueAttribute((GKSchemaAttribute) attribute, value);
			//System.err.println("Overwrote attribute '" + attribute.getName() + "' value '" + previous.toString() + "'.");
		}
	}

	private void addMultiValueAttributeValue(
		SchemaAttribute attribute,
		Object value) throws InvalidAttributeValueException {
		if (value != null) {
			if (value instanceof java.util.List) {
				((GKSchemaAttribute) attribute).areValidValuesOrThrow((List) value);
			} else {
				((GKSchemaAttribute) attribute).isValidValueOrThrow(value);
			}
			addAttributeValueNoCheck(attribute, value);
		}
	}

	/* 
	 * @see org.gk.model.Instance#getReferers(java.lang.String)
	 */
	public Collection getReferers(String attributeName) throws Exception {
		if (debug) System.out.println(this + "\tgetReferers(String)\t" + attributeName);
		Collection out = new ArrayList();
		for (Iterator i = ((GKSchemaClass) schemaClass).getReferersByName(attributeName).iterator(); i.hasNext();) {
			SchemaAttribute att = (SchemaAttribute) i.next();
			Collection c = getReferers(att);
			if (c != null) {
				out.addAll(c);
			}
		}
		if (debug) System.out.println(this + "\tgetReferers(String)\t" + attributeName + "\t" + out);
		if (out.isEmpty()) {
			return null;
		} else {
			return out;
		}
	}

	/* 
	 * @see org.gk.model.Instance#getReferers(org.gk.schema.SchemaAttribute)
	 */
	public Collection getReferers(SchemaAttribute attribute) throws Exception {
		if (debug) System.out.println(this + "\tgetReferers(SchemaAttribute)\t" + attribute);
		SchemaAttribute originalAtt = ((GKSchemaAttribute) attribute).getOriginalAttribute();
		if (! referers.containsKey(originalAtt)) {
			if (dbAdaptor != null) {
				Collection c = dbAdaptor.fetchInstanceByAttribute(originalAtt,"=",this);
				referers.put(originalAtt, c);
				return c;
			}
		}
		Object o = referers.get(originalAtt);
		if (o == null) {
			return null;
		} else {
			return (Collection) o;
		}
	}
	
	public void addRefererNoCheck(SchemaAttribute att, Object referer) throws Exception {
		SchemaAttribute originalAtt = ((GKSchemaAttribute) att).getOriginalAttribute();
		Collection c = (Collection) referers.get(originalAtt);
		if (c == null) {
			c = new ArrayList();
			c.add(referer);
			referers.put(originalAtt, c);
		}
		else {
			c.add(referer);
		}
	}
	
	public void removeRefererNoCheck(SchemaAttribute att, Instance referer) {
		SchemaAttribute originalAtt = ((GKSchemaAttribute) att).getOriginalAttribute();
		Collection c = (Collection) referers.get(originalAtt);
		if (c != null)
			c.remove(referer);
	}
	
	public void setRefererNoCheck(SchemaAttribute att, Object list) throws Exception {
		SchemaAttribute originalAtt = ((GKSchemaAttribute) att).getOriginalAttribute();
		referers.put(originalAtt, list);
	}
	
	/**
	 * Empty all referer values.
	 */
	public void clearReferers() {
		referers.clear();
	}
	
	public Map getReferers() {
		return referers;
	}
	
	public boolean areReferersEmpty() {
		if (referers.size() == 0)
			return true;
		for (Iterator it = referers.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			java.util.List list = (java.util.List) referers.get(key);
			if (list != null && list.size() > 0) {
				return false;
			}
		}
		return true;
	}
	
	/* 
	 * @see org.gk.model.Instance#getDBID()
	 */
	public Long getDBID() {
		return this.dbID;
	}

	/* 
	 * @see org.gk.model.Instance#setDBID(long)
	 */
	public void setDBID(Long id) {
		this.dbID = id;
		// Have to set the attribute value for it
		setAttributeValueNoCheck("DB_ID", id);
	}

	/* 
	 * @see org.gk.model.Instance#getDisplayName()
	 */
	public String getDisplayName() {
		 try {
			return (String) getAttributeValue("_displayName");
		} catch (InvalidAttributeException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getExtendedDisplayName() {
        return "[" + getSchemClass().getName() + ":" + getDBID() + "] " + getDisplayName();
	}

	public String toString() {
		return getExtendedDisplayName();
	}

	/* 
	 * @see org.gk.model.Instance#setDisplayName(java.lang.String)
	 */
	public void setDisplayName(String name) {
		setAttributeValueNoCheck("_displayName", name);
	}
	
	private void loadAttributeValues(GKSchemaAttribute attribute) throws Exception {
		//System.err.println("private void loadAttributeValues(GKSchemaAttribute attribute)");
		if (dbAdaptor != null) {
			dbAdaptor.loadInstanceAttributeValues(this, attribute);
			// There is a bug in MySQLAdaptor: if null is returned from the database
			// query. The loading is not marked. So database query will be performed
			// multiple times!
			// This is a workaround
			if (!attributes.containsKey(attribute.getName()))
			    attributes.put(attribute.getName(), new ArrayList()); // Avoid null return
		} else {
			new Exception("No dbAdaptor").printStackTrace();
		}
	}

	public boolean isAttributeValueLoaded (SchemaAttribute att) {
		return attributes.containsKey(att.getName());
		//return attributes.get(att.getName()) != null;
	}

	public boolean isRefererValueLoaded (SchemaAttribute att) {
		SchemaAttribute originalAtt = ((GKSchemaAttribute) att).getOriginalAttribute();
		return referers.containsKey(originalAtt);
	}
	
	public String toStanza() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append(getExtendedDisplayName() + "\n");
		for (Iterator i = schemaClass.getAttributes().iterator(); i.hasNext();) {
			SchemaAttribute att = (SchemaAttribute) i.next();
			Collection vals = getAttributeValuesList(att);
			if (vals != null) {
				for (Iterator j = vals.iterator(); j.hasNext();) {
					sb.append(att.getName() + "\t" + j.next().toString() + "\n");
				}
//			} else {
//				sb.append(att.getName() + "\t<none>\n");
			}
		}
		for (Iterator i = schemaClass.getReferers().iterator(); i.hasNext();) {
			SchemaAttribute att = (SchemaAttribute) i.next();
			//System.out.println(att);
			Collection vals = getReferers(att);
			if (vals != null) {
				for (Iterator j = vals.iterator(); j.hasNext();) {
					sb.append("(" + att.getName() + ")\t" + j.next().toString() + "\n");
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * @return
	 */
	public PersistenceAdaptor getDbAdaptor() {
		return dbAdaptor;
	}

	/**
	 * @param adaptor
	 */
	public void setDbAdaptor(PersistenceAdaptor adaptor) {
		dbAdaptor = adaptor;
	}
	
	/**
	 * Mark this GKInstance isInflated. An instance has all its attributes are filled
	 * is called "inflated" instance. It should not mark as inflated if only part of 
	 * attributes are filled.
	 * @param inflated
	 */
	public void setIsInflated(boolean inflated) {
		this.isInflated = inflated;
		if (inflated) {
			setIsShell(false);
		}
	}
	
	public boolean isInflated() {
		return this.isInflated;
	}
	
	public void setIsShell(boolean isShell) {
		this.isShell = isShell;
	}

	/**
	 * Mark this GKInstance isShell. An GKInstance is a shell instance if it has
	 * only DB_ID and display name, and all its attributes should be loaded from
	 * only database.
	 * @param inflated
	 */	
	public boolean isShell() {
		return this.isShell;
	}
	
	/**
	 * Clone this GKInstance. This is a deep clone. All attributes are cloned.
	 * Note: DB_ID is null for the returned, cloned object.
	 */
	public Object clone() {
		GKInstance clone = new GKInstance();
		// Copy non-attribute values.
		clone.setDbAdaptor(dbAdaptor);
		clone.setSchemaClass(schemaClass);
		clone.setIsInflated(isInflated);
		clone.setIsShell(isShell);
		// Don't copy DB_ID
		// Copy attributes
		try {
			for (Iterator it = schemaClass.getAttributes().iterator(); it.hasNext();) {
				SchemaAttribute att = (SchemaAttribute)it.next();
				if (att.getName().equals("DB_ID")) // Escape DB_ID
					continue;
				java.util.List values = getAttributeValuesList(att);
				if (values != null && values.size() > 0) {
					java.util.List valueCopy = new ArrayList(values);
					clone.setAttributeValueNoCheck(att, valueCopy);
				}
			}
		}
		catch (Exception e) {
			System.err.println("GKInstance.clone(): " + e);
			e.printStackTrace();
		}
		return clone;
	}
	
	/**
	 * Removes attribute and reverese attribute (referers) values.
	 */
	public void deflate () {
		attributes.clear();
		referers.clear();
		this.setIsInflated(false);
	}
    
    /**
     * Set if this object is touched by editing.
     * @param isDirty
     */
    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }
    
    /**
     * Check if this GKInstance object is touched by editing.
     * @return
     */
    public boolean isDirty() {
        return this.isDirty;
    }
    
    /**
     * This method should only be called upon creation of a new instance from scratch
     * and not when getting the instance from db.
     * @throws InvalidAttributeException
     * @throws InvalidAttributeValueException
     */
    public void setDefaultValues() throws InvalidAttributeException, InvalidAttributeValueException {
    	for (Iterator it = getSchemaAttributes().iterator(); it.hasNext();) {
    		GKSchemaAttribute att = (GKSchemaAttribute)it.next();
    		if (att.getDefaultValue() != null) {
    			this.setAttributeValue(att, att.getDefaultValue());
    		}
    	}
    }
//
//	@Override
//	public boolean equals(Object obj) {
//		if (!(obj instanceof GKInstance))
//			return false;
//		GKInstance other = (GKInstance) obj;
//		// Only two GKInstances coming from the same data source
//		// and have the same DB_IDs are equal.
//		if (getDbAdaptor() == other.getDbAdaptor() &&
//		    getDBID().equals(other.getDBID()))
//			return true;
//		return false;
//	}
//
////     The following implementation creates some un-expected results when using HashMap: a value cannot be got
////     using the key!!!
//	@Override
//	public int hashCode() {
//	    if (getDbAdaptor() == null)
//	        return new HashCodeBuilder(17, 37).append(getDBID()).toHashCode();
//	    else
//	        return new HashCodeBuilder(17, 37).append(getDbAdaptor().hashCode()).append(getDBID()).toHashCode();
//	}
    
}
