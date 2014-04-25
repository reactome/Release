/*
 * Created on Jun 27, 2003
 */
package org.gk.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author wgm
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GKSchema implements Schema, Serializable {

    private Map classes = new HashMap();
	private Map attributes = new HashMap();
	private Map cache;
	private SchemaClass rootClass;
	private String timestamp = null;

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/*
	 * @see org.gk.schema.Schema#getClasses()
	 */
	public Collection getClasses() {
		return classes.values();
	}

	/*
	 * @see org.gk.schema.Schema#getClassNames()
	 */
	public Collection getClassNames() {
		return java.util.Arrays.asList(classes.keySet().toArray());
	}

	/*
	 * @see org.gk.schema.Schema#getClassByName(java.lang.String)
	 */
	public SchemaClass getClassByName(String name) {
		return (SchemaClass) classes.get((Object) name);
	}

	/*
	 * @see org.gk.schema.Schema#isValidClass(java.lang.String)
	 */
	public boolean isValidClass(String name) {
		return classes.containsKey((Object) name);
	}

	/*
	 * @see org.gk.schema.Schema#isValidClass(org.gk.schema.SchemaClass)
	 */
	public boolean isValidClass(SchemaClass class1) {
		return classes.containsValue((Object) class1);
	}

	public boolean isValidClassOrThrow(String name) throws InvalidClassException {
		if (isValidClass(name)) {
			return true;
		}
		throw(new InvalidClassException(name));
	}
	
	public void addClass(SchemaClass class1) {
		classes.put(class1.getName(), class1);
	}

	public void addAttribute(SchemaAttribute attribute) {
		attributes.put(attribute.getName(), attribute);
	}

	public void setCache(Map cache) {
		this.cache = cache;
	}

	public Map getCache() {
		return cache;
	}

	public SchemaAttribute getAttributeByName(String name) {
		return (SchemaAttribute) attributes.get(name);
	}
	
	public Collection getAttributes() {
		return attributes.values();
	}

	public void initialise() {
		Iterator i = cache.values().iterator();
		while (i.hasNext()) {
			Object o = i.next();
			//System.out.println("GKSchema.initialise()" + "\t" + o.toString());
			if (o instanceof GKSchemaAttribute) {
				GKSchemaAttribute a = (GKSchemaAttribute) o;
				Collection schemaClasses = a.getSchemaClass();
				if (schemaClasses.isEmpty()) {
					addAttribute(a);
				} else {
					Iterator j = schemaClasses.iterator();
					//There should be just one element in this collection this time
					GKSchemaClass schemaClass = (GKSchemaClass) j.next();
					schemaClass.addAttribute(a);
					a.setOrigin(schemaClass);
					setInverseAttribute(a);
				}
			} else if (o instanceof GKSchemaClass) {
				//SchemaTestApp.printObject(o);
				addClass((GKSchemaClass) o);
			}
		}
		findRootClassSetSubClasses();
		addAttributesToClasses();
		i = getClasses().iterator();
		while (i.hasNext()) {
			GKSchemaClass c = (GKSchemaClass) i.next();
			c.initialise();
		}
	}

	public SchemaClass getRootClass() {
		if (rootClass == null) {
			findRootClassSetSubClasses();
		}
		return rootClass;
	}

	private void findRootClassSetSubClasses() {
		Iterator i = classes.values().iterator();
		while (i.hasNext()) {
			GKSchemaClass c = (GKSchemaClass) i.next();
			//System.out.println("GKSchema.findRootClassSetSubClasses()" + "\t" + c.toString());
			//SchemaTestApp.printObject(c);
			Collection superClasses = c.getSuperClasses();
			if (superClasses.isEmpty()) {
				rootClass = c;
			} else {
				Iterator j = superClasses.iterator();
				while (j.hasNext()) {
					GKSchemaClass sc = (GKSchemaClass) j.next();
					sc.addSubClass(c);
				}
			}
		}
	}

	private void addAttributesToClasses() {
		List classes = new ArrayList();		
		Set seen = new HashSet();
		classes.add(rootClass);
		while (! classes.isEmpty()) {
			Iterator i = classes.iterator();
			List subclasses = new ArrayList();
			while (i.hasNext()) {
				GKSchemaClass current = (GKSchemaClass) i.next();
				//System.out.println("GKSchema.addAttributesToClasses()" + "\t" + current.toString());
				//SchemaTestApp.printObject(current);
				if (!seen.contains(current)) {
					seen.add(current);
					subclasses.addAll(current.getSubClasses());
					current.copyAttributesAndSortAncestors();
				}
				//SchemaTestApp.printObject(current);
			}
			classes.clear();
			classes.addAll(subclasses);
		}
	}

	private void setInverseAttribute(GKSchemaAttribute a) {
		GKSchemaAttribute baseAttribute = (GKSchemaAttribute) cache.get(a.getName());
		a.setInverseSchemaAttribute(baseAttribute.getInverseSchemaAttribute());
	}

	/**
	 * A method for getting all original SchemaAttributes, i.e. attributes
	 * as they 1st occur on a class in given class hierarchy. If a subclass
	 * over-rides it's inherited attribute, the over-ridden attribute will
	 * not be returned. 
	 * @return Set of SchemaAttributes
	 */
	public Set getOriginalAttributes() {
		Set out = new HashSet();
		for (Iterator ci = getClasses().iterator(); ci.hasNext();) {
			SchemaClass cls = (SchemaClass) ci.next();
			for (Iterator ai = cls.getAttributes().iterator(); ai.hasNext();) {
				SchemaAttribute att = (SchemaAttribute) ai.next();
				if (att.getOrigin() == cls) {
					out.add(att);
				}
			}
		}
		return out;
	}

    /**
     * Returns original attributes which have the name as attName. E.g.
     * called with "instanceOf" the method will return instanceOf attributes
     * of PhysicalEntity, Event, GO_BiologicalProcess etc.
     * @param String attName
     * @return Set of SchemaAttributes with name as attName
     */
	public Set getOriginalAttributesByName(String attName) {
		Set out = new HashSet();
		for (Iterator ai = getOriginalAttributes().iterator(); ai.hasNext();) {
			SchemaAttribute att = (SchemaAttribute) ai.next();
			if (att.getName().equals(attName)) {
				out.add(att);
			}
		}
		return out;
	}

}
