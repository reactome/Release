/*
 * Created on Jun 27, 2003
 */
package org.gk.schema;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author wgm
 */
@SuppressWarnings("rawtypes")
public class GKSchemaClass implements SchemaClass, Serializable {

	private String id;
	private String name;
	private Map attributes = new HashMap();
	private boolean isAbstract = false;
	private Map superClasses = new HashMap();
	private Map subClasses = new HashMap();
	private Map ownAttributes = new HashMap();
	private Map ancestors = new HashMap();
	private List orderedAncestors;
	private Set reverseAttributes = new HashSet();
	private Set reverseAttributeNames = new HashSet();
	private static Comparator classAncestorSorter;
	private Collection[] definingAttributes;


	public GKSchemaClass() {
	}

	public GKSchemaClass(String id) {
		this.id = id;
	}

	/*
	 * @see org.gk.schema.SchemaClass#getAttribute(java.lang.String)
	 */
	public SchemaAttribute getAttribute(String attributeName) throws InvalidAttributeException {
		isValidAttributeOrThrow(attributeName);
		return (SchemaAttribute) attributes.get(attributeName);
	}

	public SchemaAttribute getAttributeNoCheck(String attributeName) {
		return (SchemaAttribute) attributes.get(attributeName);
	}

	public void addAttribute(SchemaAttribute attribute) {
		//System.out.println("GKSchema.addAttribute\t" + attribute.toString());
		attributes.put(attribute.getName(), attribute);
	}

	private void addOwnAttribute(SchemaAttribute attribute) {
		ownAttributes.put(attribute.getName(), attribute);
	}

	/*
	 * @see org.gk.schema.SchemaClass#getAttributes()
	 */
	public Collection getAttributes() {
		return attributes.values();
	}

	public Collection getOwnAttributes() {
		return ownAttributes.values();
	}
	
	protected void findOwnAttributes() {
		Iterator i = getAttributes().iterator();
		while (i.hasNext()) {
			GKSchemaAttribute a = (GKSchemaAttribute) i.next();
			if (a.getOrigin() == this) {
				addOwnAttribute(a);
			}
		}
	}

	/*
	 * @see org.gk.schema.SchemaClass#getName()
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/*
	 * @see org.gk.schema.SchemaClass#getOrderedAncestors()
	 */
	public List getOrderedAncestors() {
		return orderedAncestors;
	}

	/*
	 * @see org.gk.schema.SchemaClass#getReferers()
	 */
	public Collection getReferers() {
		return (Collection) reverseAttributes;
	}

	public Collection getReferersByName(String attName) {
		Collection out = new ArrayList();
		for (Iterator i = getReferers().iterator(); i.hasNext();) {
			SchemaAttribute a = (SchemaAttribute) i.next();
			if (a.getName().equals(attName)) {
				out.add(a);
			}
		}
		return out;
	}

	private void findReferers() {
		Iterator ai = getAttributes().iterator();
		while (ai.hasNext()) {
			GKSchemaAttribute a = (GKSchemaAttribute) ai.next();
			Iterator aci = a.getAllowedClasses().iterator();
			while (aci.hasNext()) {
				GKSchemaClass ac = (GKSchemaClass) aci.next();
				ac.addReverseAttribute(a);
				Collection subClasses = new ArrayList(ac.getSubClasses());
				while (! subClasses.isEmpty()) {
					Collection subSubClasses = new ArrayList();
					Iterator sci = subClasses.iterator();
					while (sci.hasNext()) {
						GKSchemaClass sc = (GKSchemaClass) sci.next();
						subSubClasses.addAll(sc.getSubClasses());
						sc.addReverseAttribute(a);
					}
					subClasses.clear();
					subClasses.addAll(subSubClasses);
				}
			}
		}
	}

	private void addReverseAttribute(SchemaAttribute reverseAttribute) {
		reverseAttributes.add(((GKSchemaAttribute) reverseAttribute).getOriginalAttribute());
		reverseAttributeNames.add(reverseAttribute.getName());
	}

	/*
	 * @see org.gk.schema.SchemaClass#getSuperClasses()
	 */
	public Collection getSuperClasses() {
		return superClasses.values();
	}

	public void addSuperClass(SchemaClass superClass) {
		superClasses.put(superClass.getName(), superClass);
	}

	public Collection getSubClasses() {
		return subClasses.values();
	}

	public void addSubClass(SchemaClass subClass) {
		subClasses.put(subClass.getName(), subClass);
	}

	/*
	 * @see org.gk.schema.SchemaClass#isAbstract()
	 */
	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isValidAttribute(String attributeName) {
		return attributes.containsKey(attributeName);
	}

	public boolean isValidAttribute(SchemaAttribute attribute) {
		return attributes.containsValue(attribute);
	}

	public boolean isValidAttributeOrThrow(SchemaAttribute attribute) throws InvalidAttributeException {
		if (isValidAttribute(attribute)) {
			return true;
		} else {
			throw(new InvalidAttributeException(this, attribute));
		}
	}

	public boolean isValidAttributeOrThrow(String attributeName) throws InvalidAttributeException {
		if (isValidAttribute(attributeName)) {
			return true;
		} else {
			throw(new InvalidAttributeException(this, attributeName));
		}
	}

	private void addAncestors(GKSchemaClass superClass) {
		ancestors.put(superClass.getName(), superClass);
		ancestors.putAll(superClass.ancestors);
	}

	protected SchemaAttribute findClosestAncestralAttribute(String attName) {
		Set atts = new HashSet();
		Collection classes = new HashSet();
		classes.add(this);
		while(! classes.isEmpty()) {
			Set superClasses = new HashSet();
			for (Iterator ci = classes.iterator(); ci.hasNext();) {
				GKSchemaClass cls = (GKSchemaClass) ci.next();
				superClasses.addAll(cls.getSuperClasses());
				SchemaAttribute ancAtt;
				if ((ancAtt = cls.getAttributeNoCheck(attName)) != null) {
					if (ancAtt.getSchemaClass().contains(cls)) {
						atts.add(ancAtt);
					}
				}
				//System.out.println(this.getName() + ":" + attName + "\t" + cls + "\t" + ancAtt);
			}
			if (! atts.isEmpty()) {
				break;
			} else {
				classes.clear();
				classes.addAll(superClasses);
			}
		}
		if (atts.isEmpty()) {
			new Exception("Can't find ancestral attribute: " + this + ":" + attName).printStackTrace();
		}
		else if (atts.size() > 1) {
			new Exception("Can't resolve closest ancestral attribute: " + this + ":" + attName).printStackTrace();
		}
		return (SchemaAttribute) atts.iterator().next();
	}

	protected int findClosestAncestralAttributeDefiningType(String attName) {
		Set defTypes = new HashSet();
		Collection classes = new HashSet();
		classes.add(this);
		while(! classes.isEmpty()) {
			Set superClasses = new HashSet();
			for (Iterator ci = classes.iterator(); ci.hasNext();) {
				GKSchemaClass cls = (GKSchemaClass) ci.next();
				superClasses.addAll(cls.getSuperClasses());
				SchemaAttribute ancAtt;
				if ((ancAtt = cls.getAttributeNoCheck(attName)) != null) {
					if (ancAtt.getSchemaClass().contains(cls)) {
						int defType = ancAtt.getDefiningType();
						if (defType != SchemaAttribute.UNDEFINED) {
							defTypes.add(new Integer(defType));
						}
					}
				}
				//System.out.println(this.getName() + ":" + attName + "\t" + cls + "\t" + defTypes);
			}
			if (! defTypes.isEmpty()) {
				break;
			} else {
				classes.clear();
				classes.addAll(superClasses);
			}
		}
		if (defTypes.isEmpty()) {
			return SchemaAttribute.UNDEFINED;
		}
		else if (defTypes.size() > 1) {
			new Exception("Can't resolve closest defining type of attribute: " + this + ":" + attName).printStackTrace();
		}
		return ((Integer) defTypes.iterator().next()).intValue();
	}

	protected SchemaClass findAttributeOrigin(SchemaAttribute att) {
		Collection classes = new HashSet();
		List ancestors = getOrderedAncestors();
		classes.add(ancestors.get(0));
		Set origins = new HashSet();
		while(! classes.isEmpty()) {
			Set subClasses = new HashSet();
			for (Iterator ci = classes.iterator(); ci.hasNext();) {
				GKSchemaClass cls = (GKSchemaClass) ci.next();
				for (Iterator sci = cls.getSubClasses().iterator(); sci.hasNext();) {
					GKSchemaClass subCls = (GKSchemaClass) sci.next();
					if (ancestors.contains(subCls)) subClasses.add(subCls);
				}
				if (cls.getAttributeNoCheck(att.getName()) != null) {
					origins.add(cls);
				}
			}
			if (! origins.isEmpty()) {
				break;
			} else {
				classes.clear();
				classes.addAll(subClasses);
			}
		}
		if (origins.isEmpty()) {
			new Exception("Can't find attribute origin: " + this + ":" + att).printStackTrace();
		}
		else if (origins.size() > 1) {
			new Exception("Can't resolve attribute origin: " + this + ":" + att).printStackTrace();
		}
		return (SchemaClass) origins.iterator().next();
	}

	protected Set createAttributeNameSet() {
		Set atts = new HashSet();
		for (Iterator sci = getSuperClasses().iterator(); sci.hasNext();) {
			GKSchemaClass superClass = (GKSchemaClass) sci.next();
			for (Iterator scai = superClass.getAttributes().iterator(); scai.hasNext();) {
				GKSchemaAttribute a = (GKSchemaAttribute) scai.next();
				atts.add(a.getName());
			}
		}
		return atts;
	}

	protected void copyAttributes() {
		Set ancAtts = new HashSet();
		Collection attNames = createAttributeNameSet();
		for (Iterator ani = attNames.iterator(); ani.hasNext();) {
			String attName = (String) ani.next();
			SchemaAttribute ancAtt = findClosestAncestralAttribute(attName);
			GKSchemaAttribute existingAttribute = (GKSchemaAttribute) attributes.get(attName);
			if (existingAttribute == null) {
				addAttribute(ancAtt);
			} else {
				existingAttribute.setOrigin(findAttributeOrigin(existingAttribute));
			}
			existingAttribute = (GKSchemaAttribute) attributes.get(attName);
			if (existingAttribute.getDefiningType() == SchemaAttribute.UNDEFINED) {
				int defType = findClosestAncestralAttributeDefiningType(attName);
				if (defType != SchemaAttribute.UNDEFINED) {
					existingAttribute.setDefiningType(defType);
					//System.out.println(this + "\t" + existingAttribute + "\t" + defType);
				}
			}
		}
	}

	protected void copyAttributesAndSortAncestors() {
		addAndSortAncestors();
		copyAttributes();
	}

	protected void copyAttributesAndSortAncestors2() {
		for (Iterator sci = getSuperClasses().iterator(); sci.hasNext();) {
			GKSchemaClass superClass = (GKSchemaClass) sci.next();
			for (Iterator scai = superClass.getAttributes().iterator(); scai.hasNext();) {
				GKSchemaAttribute a = (GKSchemaAttribute) scai.next();
				GKSchemaAttribute existingAttribute = (GKSchemaAttribute) attributes.get(a.getName());
				if (existingAttribute == null) {
					// Completely inherited atribute, copy attribute from superclass
					addAttribute(a);
					a.addSchemaClass(this);
				} else {
					// Class over-rides the attribute. Reset the origin.
					existingAttribute.setOrigin(a.getOrigin());
				}
			}
			addAncestors(superClass);
		}
		sortAncestors();
	}

	protected void copyAttributesAndSortAncestors1() {
		Iterator sci = getSuperClasses().iterator();
		while (sci.hasNext()) {
			GKSchemaClass superClass = (GKSchemaClass) sci.next();
			Iterator scai = superClass.getAttributes().iterator();
			while (scai.hasNext()) {
				GKSchemaAttribute a = (GKSchemaAttribute) scai.next();
				GKSchemaAttribute existingAttribute = (GKSchemaAttribute) attributes.get(a.getName());
				if (existingAttribute == null) {
					// Completely inherited atribute, copy attribute from superclass
					addAttribute(a);
					a.addSchemaClass(this);
				} else {
					// Class over-rides the attribute. Reset the origin.
					existingAttribute.setOrigin(a.getOrigin());
				}
			}
			addAncestors(superClass);
		}
		sortAncestors();
	}

	protected void addAndSortAncestors() {
		Iterator sci = getSuperClasses().iterator();
		while (sci.hasNext()) {
			GKSchemaClass superClass = (GKSchemaClass) sci.next();
			addAncestors(superClass);
		}
		sortAncestors();
	}
	
	public int ancestorCount() {
		return ancestors.values().size();
	}
	
	private void sortAncestors() {
		orderedAncestors = Arrays.asList(ancestors.values().toArray());
		Collections.sort(orderedAncestors, getClassAncestorSorter());
	}
	
	private static Comparator getClassAncestorSorter() {
		if (classAncestorSorter == null) {
			classAncestorSorter = new Comparator() {
				public int compare(Object class1, Object class2) {
					int i1 = ((GKSchemaClass) class1).ancestorCount();
					int i2 = ((GKSchemaClass) class2).ancestorCount();
					if (i1 < i2) {
						return -1;
					} else if (i1 == i2) {
						return 0;
					} else {
						return 1;
					}
				}
			};
		}
		return classAncestorSorter;
	}
	
	protected void initialise() {
		findOwnAttributes();
		findReferers();
	}
	
	public String toString() {
		return "[" + super.toString() + "] " + getName();
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

	public boolean isValidReverseAttribute(SchemaAttribute attribute) {
		return reverseAttributes.contains(((GKSchemaAttribute) attribute).getOriginalAttribute());
	}

	public boolean isValidReverseAttribute(String attributeName) {
		return reverseAttributeNames.contains(attributeName);
	}

	public Collection getReverseAttributeOriginByName(String attributeName) {
		Set origins = new HashSet();
		Iterator rai = getReferers().iterator();
		while (rai.hasNext()) {
			GKSchemaAttribute a = (GKSchemaAttribute) rai.next();
			origins.add(a.getOrigin());
		}
		return origins;
	}

	/* (non-Javadoc)
	 * @see org.gk.schema.SchemaClass#isa(org.gk.schema.SchemaClass)
	 */
	public boolean isa(SchemaClass schemaClass) {
		if (this == schemaClass) {
			return true;
		}
		return ancestors.containsValue(schemaClass);
	}
	
	/**
	 * Check against the whole list of ancestor classes.
	 * @param className
	 * @return
	 * @see isa(String)
	 */
	public boolean isa(String className) {
		if (getName().equals(className)) {
			return true;
		}
		return ancestors.containsKey(className);
	}

	/**
	 * 
	 * @param defType
	 * @return a Collection of GKSchemaAttributes with given defining type,
	 * i.e. either ALL or ANY
	 * Rather than finding the appropriate attributes each time from all
	 * attributes this method caches them in definingAttributes[] the 1st
	 * time it is called.
	 */
	public Collection getDefiningAttributes(int defType) {
		if ((defType < 0) || (defType > 2)) {
			return null;
		}
		if (definingAttributes == null) {
			definingAttributes = new Collection[3];
		}
		if (definingAttributes[defType] == null) {
			definingAttributes[defType] = new HashSet();
			for (Iterator ai = getAttributes().iterator(); ai.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute) ai.next();
				if (att.getDefiningType() == defType) {
					definingAttributes[defType].add(att);
				}	
			}
		}
		if (definingAttributes[defType].isEmpty()) {
			return null;
		}
		return definingAttributes[defType];
	}

	/**
	 * 
	 * @return Collection of GKSchemaAttributes which are defining, i.e. with type
	 * ALL or ANY.
	 * Similarly to getDefiningAttributes(int) also this method caches the values
	 * on definingAttributes[].
	 */
	public Collection getDefiningAttributes() {
//		if (definingAttributes == null) {
//			definingAttributes = new Collection[4];
//		}
//		if (definingAttributes[3] == null) {
//			definingAttributes[3] = new HashSet();
//			Collection c = getDefiningAttributes(SchemaAttribute.ALL_DEFINING);
//			if (c != null) {
//				definingAttributes[3].addAll(c);
//			}
//			if (c != null) {
//				definingAttributes[3].addAll(c);
//			}
//		}
//		if (definingAttributes[3].isEmpty()) {
//			return null;
//		}
//		return definingAttributes[3];
	    HashSet set = new HashSet();
	    Collection c = getDefiningAttributes(SchemaAttribute.ALL_DEFINING);
	    if (c != null)
	    	set.addAll(c);
	    c = getDefiningAttributes(SchemaAttribute.ANY_DEFINING);
	    if (c != null)
	    	set.addAll(c);
	    return set;
	}
	
	/**
	 * Returns a collection of SchemaAttributes in given category, i.e. MANDATORY, REQUIRED, OPTIONAL or NOMANUALEDIT
	 * @param category
	 * @return
	 */
	public Collection getAttributesOfCategory(int category) {
		HashSet attributes = new HashSet();
		for (Iterator ai = getAttributes().iterator(); ai.hasNext();) {
			GKSchemaAttribute att = (GKSchemaAttribute) ai.next();
			if (att.getCategory() == category) {
				attributes.add(att);
			}
		}
		return attributes;
	}
}
