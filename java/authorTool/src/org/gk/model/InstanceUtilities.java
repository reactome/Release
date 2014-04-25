/*
 * Created on Sep 16, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gk.model;

import java.util.*;
import java.util.regex.Pattern;

import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;


/**
 * @author vastrik
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class InstanceUtilities {
	// These constants are used for comparing two instances
	/**
	 * Two instances have the same attributes.
	 */
	public static final int IS_SAME = 0;
	/**
	 * The first instance has more attributes than the second
	 */
	public static final int IS_GREATER = 1;
	/**
	 * The second instance has more attributes than the first
	 */
	public static final int IS_LESS = 2;
	/**
	 * Two instances has conflicting attributes.
	 */
	public static final int IS_CONFLICTING = 3;
	
	/**
	 * Takes an instance and Collection of ClassAttributeFollowingInstruction and returns
	 * a Map of Instances (as DB_ID:Instance pairs) which can be reached from the given
	 * Instance by listed instructions.
	 * @param instance - given instance to be followed
	 * @param instructions - List of ClassAttributeFollowingInstruction
	 * @return Map of found instances
	 */
	public static Map followInstanceAttributes(GKInstance instance, Collection instructions) throws InvalidAttributeException, Exception {
		List current = new ArrayList();
		current.add(instance);
		Map out = new HashMap();
		while(! current.isEmpty()) {
			List values = new ArrayList();
			for (Iterator ci = current.iterator(); ci.hasNext();) {
				GKInstance currentInstance = (GKInstance) ci.next();
				if (out.put(currentInstance.getDBID(), currentInstance) != null) {
					continue;
				}
				SchemaClass cls = currentInstance.getSchemClass();
				for (Iterator ii = instructions.iterator(); ii.hasNext();) {
					ClassAttributeFollowingInstruction cafi = (ClassAttributeFollowingInstruction) ii.next();
					if (cls.isa(cafi.getClassName())) {
						Collection c;
						for (Iterator ai = cafi.getAttributes().iterator(); ai.hasNext();) {
							String attName = (String) ai.next();
							if ((c = currentInstance.getAttributeValuesList(attName)) != null) {
								values.addAll(c);
							}
						}
						for (Iterator rai = cafi.getReverseAttributes().iterator(); rai.hasNext();) {
							String revAttName = (String) rai.next();
							if ((c = currentInstance.getReferers(revAttName)) != null) {
								values.addAll(c);
							}
						}
					}
				}
			}
			current.clear();
			current = values;
		}
		return out;
	}
    

	/*
	 * Differs from followInstanceAttributes by requiring the class to match exactly, i.e. an
	 * instance belonging to the sub-class is not followed.
	 */
	public static Map followInstanceAttributesStrictly(GKInstance instance, Collection instructions) throws InvalidAttributeException, Exception {
		List current = new ArrayList();
		current.add(instance);
		Map out = new HashMap();
		while(! current.isEmpty()) {
			List values = new ArrayList();
			for (Iterator ci = current.iterator(); ci.hasNext();) {
				GKInstance currentInstance = (GKInstance) ci.next();
				if (out.put(currentInstance.getDBID(), currentInstance) != null) {
					continue;
				}
				SchemaClass cls = currentInstance.getSchemClass();
				for (Iterator ii = instructions.iterator(); ii.hasNext();) {
					ClassAttributeFollowingInstruction cafi = (ClassAttributeFollowingInstruction) ii.next();
					if (cls.getName().equals(cafi.getClassName())) {
						Collection c;
						for (Iterator ai = cafi.getAttributes().iterator(); ai.hasNext();) {
							String attName = (String) ai.next();
							if ((c = currentInstance.getAttributeValuesList(attName)) != null) {
								values.addAll(c);
							}
						}
						for (Iterator rai = cafi.getReverseAttributes().iterator(); rai.hasNext();) {
							String revAttName = (String) rai.next();
							if ((c = currentInstance.getReferers(revAttName)) != null) {
								values.addAll(c);
							}
						}
					}
				}
			}
			current.clear();
			current = values;
		}
		return out;
	}
	
	public static Set followInstanceAttributes(GKInstance instance, Collection instructions, String[] clsList) throws InvalidAttributeException, Exception {
		Map m = followInstanceAttributes(instance, instructions);
		if ((clsList == null) || (clsList.length == 0)) {
			return m.entrySet();
		}
		Set out = new HashSet();
		out.addAll(grepSchemaClassInstances(m, clsList, true).values());
		return out;
		//return grepSchemaClassInstances(m, clsList, true).entrySet();
	}
	
	public static Set followInstanceAttributesStrictly(GKInstance instance, Collection instructions, String[] clsList) throws InvalidAttributeException, Exception {
		Map m = followInstanceAttributesStrictly(instance, instructions);
		if ((clsList == null) || (clsList.length == 0)) {
			return m.entrySet();
		}
		return grepSchemaClassInstances(m, clsList, true).entrySet();
	}
	
	/**
	 * A function for selecting/excluding Instances of listed SchemaClasses
	 * @param instanceMap - A Map of Instances as DB_ID:Instance key:value pairs
	 * @param clsList - List of SchemaClass names
	 * @param returnListed - Boolean to indicate whether the instances to be returned are those matching the
	 * classes on the list or those not matching.
	 * @return A Map of Instances as DB_ID:Instance key:value pairs selected/excluded according to List of
	 * class names and the boolean 3rd argument.
	 */
	public static Map grepSchemaClassInstances(Map instanceMap, List clsList, boolean returnListed) {
		Map out = new HashMap();
		for (Iterator ii = instanceMap.values().iterator(); ii.hasNext();) {
			Instance instance = (Instance) ii.next();
			SchemaClass cls = instance.getSchemClass();
			if (returnListed) {
				for (Iterator ci = clsList.iterator(); ci.hasNext();) {
					String clsName = (String) ci.next();
					if (cls.isa(clsName)) {
						out.put(instance.getDBID(), instance);
						break;
					}
				}
			} else {
				for (Iterator ci = clsList.iterator(); ci.hasNext();) {
					String clsName = (String) ci.next();
					if (cls.isa(clsName)) {
						break;
					}
				}
				out.put(instance.getDBID(), instance);
			}
		}
		return out;
	}

	public static Map grepSchemaClassInstances(Map instanceMap, String[] clsList, boolean returnListed) {
		return grepSchemaClassInstances(instanceMap,Arrays.asList(clsList),returnListed);
	}

	public static Collection grepSchemaClassInstances(Collection instanceCollection, List clsList, boolean returnListed) {
		Collection out = new ArrayList();
		for (Iterator ii = instanceCollection.iterator(); ii.hasNext();) {
			Instance instance = (Instance) ii.next();
			SchemaClass cls = instance.getSchemClass();
			if (returnListed) {
				for (Iterator ci = clsList.iterator(); ci.hasNext();) {
					String clsName = (String) ci.next();
					if (cls.isa(clsName)) {
						out.add(instance);
						break;
					}
				}
			} else {
				for (Iterator ci = clsList.iterator(); ci.hasNext();) {
					String clsName = (String) ci.next();
					if (cls.isa(clsName)) {
						break;
					}
				}
				out.add(instance);
			}
		}
		return out;
	}
	
	public static Collection grepSchemaClassInstances(Collection instanceCollection, String[] clsList, boolean returnListed) {
		return grepSchemaClassInstances(instanceCollection,Arrays.asList(clsList),returnListed);
	}
	
	public static Set findPrecedinglessReactions(Map reactions) throws InvalidAttributeException, Exception {
		Set precedingless = new HashSet();
		for (Iterator ri = reactions.values().iterator(); ri.hasNext();) {
			GKInstance reaction = (GKInstance) ri.next();
			Collection precedingReactions = reaction.getAttributeValuesList("precedingEvent");
			if (precedingReactions == null) {
				precedingless.add(reaction);
				continue;
			}
			boolean isPrecedingless = true;
			for (Iterator pri = precedingReactions.iterator(); pri.hasNext();) {
				GKInstance precedingReaction = (GKInstance) pri.next();
				if (reactions.containsValue(precedingReaction)) {
					isPrecedingless = false;
					break;
				}
			}
			if (isPrecedingless) {
				precedingless.add(reaction);
			}
		}
		return precedingless;
	}
	
	/**
	 * List all referred instances for the specified instance.
	 * @param instance
	 * @return
	 * @throws Exception
	 */
	public static Map<SchemaClass, List<GKInstance>> listDownloadableInstances(GKInstance instance) throws Exception {
	    Map<SchemaClass, List<GKInstance>> rtn = new HashMap<SchemaClass, List<GKInstance>>();
	    SchemaClass cls = instance.getSchemClass();
	    addToSchemaMap(instance, rtn);
	    for (Iterator it = cls.getAttributes().iterator(); it.hasNext();) {
	        SchemaAttribute att = (SchemaAttribute) it.next();
	        if (!att.isInstanceTypeAttribute()) {
	            continue; // Just care about instances
	        }
	        List values = instance.getAttributeValuesList(att);
	        if (values == null || values.size() == 0)
	            continue;
	        for (Iterator it1 = values.iterator(); it1.hasNext();) {
	            GKInstance value = (GKInstance) it1.next();
	            // All values will be checked out as shell instances
	            value.setIsShell(true);
	            addToSchemaMap(value, rtn);
	        }
	    }
	    return rtn;
	}
	
	private static void addToSchemaMap(GKInstance instance,
	                            Map<SchemaClass, List<GKInstance>> schemaMap) {
	    List<GKInstance> list = schemaMap.get(instance.getSchemClass());
	    if (list == null) {
	        list = new ArrayList<GKInstance>();
	        schemaMap.put(instance.getSchemClass(), list);
	    }
	    list.add(instance);
	}
	
	public static void clearShellFlags(Map instanceMap) {
		for (Iterator it = instanceMap.keySet().iterator(); it.hasNext();) {
		    // value in the map maybe List or Set
		    Collection list = (Collection) instanceMap.get(it.next());
			if (list != null && list.size() > 0) {
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					GKInstance instance = (GKInstance) it1.next();
					instance.setIsShell(false);
				}
			}
		}
	}
	
	/**
	 * Get all SchemaClasses in the specified list of top-level SchemaClasses.
	 * @param topLevelClasses
	 * @return a list of SchemaClasses.
	 */
	public static java.util.List getAllSchemaClasses(Collection topLevelClasses) {
		java.util.List list = new ArrayList();
		for (Iterator it = topLevelClasses.iterator(); it.hasNext();) {
			GKSchemaClass cls = (GKSchemaClass)it.next();
			getDescendentClasses(list, cls);
		}
		sortSchemaClasses(list);
		return list;
	}

	/**
	 * Pull all descendent classes into the specified list. It also includes the specified
	 * GKSchemaClass object.
	 * @param list
	 * @param schemaClass
	 */
	public static void getDescendentClasses(java.util.List list, GKSchemaClass schemaClass) {
		if (!list.contains(schemaClass))
			list.add(schemaClass);
		if (schemaClass.getSubClasses() != null && schemaClass.getSubClasses().size() > 0) {
			for (Iterator it = schemaClass.getSubClasses().iterator(); it.hasNext();) {
				GKSchemaClass subClass = (GKSchemaClass)it.next();
				getDescendentClasses(list, subClass);
			}
		}
	}
	
	public static void sortSchemaClasses(java.util.List schemaClasses) {
		Collections.sort(schemaClasses, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				GKSchemaClass class1 = (GKSchemaClass) obj1;
				GKSchemaClass class2 = (GKSchemaClass) obj2;
				return class1.getName().compareTo(class2.getName());
			}
		});
	}
	
	public static void sortInstances(java.util.List instances) {
		Collections.sort(instances, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				Instance instance1 = (Instance) obj1;
				Instance instance2 = (Instance) obj2;
				String dn1 = instance1.getDisplayName();
				if (dn1 == null) dn1 = "";
				String dn2 = instance2.getDisplayName();
				if (dn2 == null) dn2 = "";
				int rtn = dn1.compareTo(dn2);
				if (rtn == 0) {
					return instance1.getDBID().compareTo(instance2.getDBID());
				}
				return rtn;
			}
		});
	}
    
    /**
     * Group changed instances together and sort them alphabetically based on displayNames.
     * @param instances
     */
    public static void groupInstances(java.util.List instances) {
        Collections.sort(instances, new Comparator() {
            public int compare(Object obj1, Object obj2) {
                GKInstance instance1 = (GKInstance) obj1;
                GKInstance instance2 = (GKInstance) obj2;
                boolean isChanged1 = instance1.isDirty();
                String displayName1 = instance1.getDisplayName();
                if (displayName1 == null)
                    displayName1 = ""; // Just in case
                boolean isChanged2 = instance2.isDirty();
                String displayName2 = instance2.getDisplayName();
                if (displayName2 == null)
                    displayName2 = "";
                if (isChanged1 == isChanged2) {
                    int rtn = displayName1.compareTo(displayName2);
                    if (rtn == 0) 
                        rtn = instance1.getDBID().compareTo(instance2.getDBID());
                    return rtn;
                }
                else if (isChanged1 && !isChanged2)
                    return -1;
                else
                    return 1;
            }
        });
    }
    
    /**
     * Compare if two InstanceEdit are the same. If two InstanceEdits have the same
     * DB_IDs and same dateTime, they will be treated the same. The author is not
     * used in comparsion, since these two attributes should be strong enough.
     * @param ie1
     * @param ie2
     * @return
     * @throws Exception
     */
    public static boolean compareInstanceEdits(GKInstance ie1,
                                               GKInstance ie2) throws Exception {
        Long id1 = ie1.getDBID();
        Long id2 = ie2.getDBID();
        if (!id1.equals(id2))
            return false;
        // DisplayName is not reliable. They may change in different release
        if (ie1.isShell() || ie2.isShell())
            return true; // cannot have more information
        String dateTime1 = (String) ie1.getAttributeValue(ReactomeJavaConstants.dateTime);
        if (dateTime1 == null)
            dateTime1 = "";
        // There are two types representation: local created is displayed like
        // this: 20071109191417, and value returned from db is: 2007-11-09 19:47:00.
        // Note: all in GMT. 
        if (dateTime1.indexOf(":") > 0) {
            // New JDBC adaptor add .0 to the end of dateTime
            int index = dateTime1.indexOf(".");
            if (index > 0)
                dateTime1 = dateTime1.substring(0, index);
            dateTime1 = dateTime1.replaceAll("( |-|:)", "");
        }
        String dateTime2 = (String) ie2.getAttributeValue(ReactomeJavaConstants.dateTime);
        if (dateTime2 == null)
            dateTime2 = "";
        if (dateTime2.indexOf(":") > 0) {
            int index = dateTime2.indexOf(".");
            if (index > 0)
                dateTime2 = dateTime2.substring(0, index);
            dateTime2 = dateTime2.replaceAll("( |-|:)", "");
        }
        return dateTime1.equals(dateTime2);
    }
	
	/**
	 * Compare if two GKInstance objects are the same. These two objects should be from
	 * the same SchemaClass. Otherwise an IllegalArgumentException will be thrown.
	 * @param instance1
	 * @param instance2
	 * @return true if the specified two Instance objects have the same set attribute values.
	 */
	public static boolean compare(GKInstance instance1, GKInstance instance2) {
		return compare(instance1, instance2, false);
	}
	
	/**
	 * Compare if two GKInstance objects are the same. These two objects should be from
	 * the same SchemaClass. Otherwise an IllegalArgumentException will be thrown.
	 * @param instance1
	 * @param instance2
	 * @param escapeInstanceEdit true for no considering InstanceEdit values in slots "created"
	 * and "modified"
	 * @return true if the specified two Instance objects have the same set attribute values.
	 */
	public static boolean compare(GKInstance instance1, GKInstance instance2, boolean escapeInstanceEdit) {
		SchemaClass cls1 = instance1.getSchemClass();
		SchemaClass cls2 = instance2.getSchemClass();
		if ((cls1 == null && cls2 != null) ||
		    (cls1 != null && cls2 == null))
			return false;
		if (!cls1.getName().equals(cls2.getName()))
			//throw new IllegalArgumentException("InstanceUtilities.compare(): " + 
			//								   "Two GKInstance objects are not from the same SchemaClass.");
		    return false;
	   try {
			for (Iterator it = cls1.getAttributes().iterator(); it.hasNext();) {
				SchemaAttribute att = (SchemaAttribute) it.next();
				String attName = att.getName();
				if (escapeInstanceEdit && (attName.equals("created") || attName.equals("modified")))
					continue;
				java.util.List values1 = instance1.getAttributeValuesList(attName);
				java.util.List values2 = instance2.getAttributeValuesList(attName);
				if (!compareAttValues(values1, values2, att))
					return false;
			}
		}
		catch (Exception e) {
			System.err.println("InstanceUtilities.compare(): " + e);
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Compare two instances.
	 * @param instance1
	 * @param instance2
	 * @return one of IS_SAVE, IS_GREATER, IS_LESSS, IS_CONFLICTING.
	 */
	public static int compareInstances(GKInstance instance1, GKInstance instance2) {
		SchemaClass cls1 = instance1.getSchemClass();
		SchemaClass cls2 = instance2.getSchemClass();
		if (!cls1.getName().equals(cls2.getName()))
			throw new IllegalArgumentException("InstanceUtilities.compare(): " + 
											   "Two GKInstance objects are not from the same SchemaClass.");
		try {
			int prev = IS_SAME;
			int next = prev;
			for (Iterator it = cls1.getAttributes().iterator(); it.hasNext();) {
				SchemaAttribute att = (SchemaAttribute) it.next();
				String attName = att.getName();
				// Use attName in case two instances are from the different sources
				java.util.List values1 = instance1.getAttributeValuesList(attName);
				java.util.List values2 = instance2.getAttributeValuesList(attName);
				next = compareAttributes(values1, values2, att);
				if (next == IS_CONFLICTING)
					return IS_CONFLICTING;
				if (prev != IS_SAME && next != prev)
					return IS_CONFLICTING;
				prev = next;
			}
			return next;
		}
		catch (Exception e) {
			System.err.println("InstanceUtilities.compare(): " + e);
			e.printStackTrace();
		}		
		return IS_CONFLICTING; // a conservative value.
	}
	
	private static int compareAttributes(java.util.List list1, java.util.List list2, SchemaAttribute att) throws Exception {
		if (list1 == null)
			list1 = new ArrayList();
		if (list2 == null)
			list2 = new ArrayList();
		if (list1.size() == 0 && list2.size() == 0) {
			return IS_SAME;
		}
		java.util.List list1Copy = new ArrayList(list1);
		java.util.List list2Copy = new ArrayList(list2);
		if (att.isInstanceTypeAttribute()) {
			GKInstance instance1 = null;
			GKInstance instance2 = null;
			for (Iterator it = list1Copy.iterator(); it.hasNext();) {
				instance1 = (GKInstance) it.next();
				for (Iterator it1 = list2Copy.iterator(); it1.hasNext();) {
					instance2 = (GKInstance) it1.next();
					if (instance2.getDbAdaptor().equals(instance1.getDBID())) {
						it1.remove();
						it.remove();
						break;
					}
				}
			}
		}
		else {
			Object value1 = null;
			Object value2 = null;
			for (Iterator it = list1Copy.iterator(); it.hasNext();) {
				value1 = it.next();
				for (Iterator it1 = list2Copy.iterator(); it1.hasNext();) {
					value2 = it1.next();
					if (value2.equals(value1)) {
						it1.remove();
						it.remove();
						break;
					}
				}
			}
		}
		if (list1Copy.size() == list2Copy.size())
			return IS_SAME;
		else if (list2Copy.size() == 0)
			return IS_GREATER;
		else if (list1Copy.size() == 0)
			return IS_LESS;
		else
			return IS_CONFLICTING;
	}
		
	/**
	 * A helper method to compare two attribute values.
	 * @param value1 Attribute value from one instance. It might be an Instance or other primitive types.
	 * @param value2 Attribute value from another instance. It might be an Instance or other primive types.
	 * @param att SchemaAttribute value1 and value2 are used
	 * @return true for the same
	 */
	public static boolean compareAttValues(Object value1, Object value2, SchemaAttribute att) {
		// Two SchemaClasses should be the same!
		if (value1 instanceof java.util.List && ((java.util.List)value1).size() == 0)
			value1 = null;
		if (value2 instanceof java.util.List && ((java.util.List)value2).size() == 0)
			value2 = null;
		if ((value1 == null && value2 != null) || (value1 != null && value2 == null))
			return false;
		if (value1 != null && value2 != null) { // Both should be Lists
			java.util.List list1 = (java.util.List) value1;
			java.util.List list2 = (java.util.List) value2;
			if (list1.size() != list2.size())
				return false;
			//HashSet set1 = new HashSet((java.util.List)value1);
			//HashSet set2 = new HashSet((java.util.List)value2);
			int type = att.getTypeAsInt();
			switch (type) {
				case SchemaAttribute.BOOLEAN_TYPE :
				case SchemaAttribute.INTEGER_TYPE :
				case SchemaAttribute.LONG_TYPE :
				case SchemaAttribute.STRING_TYPE :
				case SchemaAttribute.FLOAT_TYPE :
					if (!list1.equals(list2))
						return false;
					break;
				case SchemaAttribute.INSTANCE_TYPE :
					int size = list1.size();
					for (int i = 0; i < size; i++) {
						GKInstance instance1 = (GKInstance) list1.get(i);
						GKInstance instance2 = (GKInstance) list2.get(i);
						if (!instance1.getDBID().equals(instance2.getDBID()))
							return false;
					}
					break;
			}
		}
		return true;
	}
	
	/**
	 * Check if two instances are mergable. In other words, if two instances are identifical.
	 * This method is different from another method areReasonablyIdentical(GKInstance, GKINstance),
	 * which is based on a much weaker condition.
	 * Note: this method can be used for two instances in the same repository.
	 * @param instance1
	 * @param instance2
	 * @return
	 * @throws Exception
	 * @see areReasonablyIdentical(GKInstance, GKInstance)
	 */
	public static boolean areMergable(GKInstance instance1,
	                                  GKInstance instance2) throws Exception {
	    GKSchemaClass class1 = (GKSchemaClass) instance1.getSchemClass();
	    GKSchemaClass class2 = (GKSchemaClass) instance2.getSchemClass();
	    if (class1 != class2)
	        return false;
	    // For both ALL and ANY defining attribues
	    Collection attColl = class1.getDefiningAttributes();
	    if (attColl != null) {
	        for (Iterator dai = attColl.iterator(); dai.hasNext();) {
	            SchemaAttribute att = (SchemaAttribute) dai.next();
	            List vals1 = instance1.getAttributeValuesList(att);
	            if (vals1 == null)
	                vals1 = new ArrayList();
	            List vals2 = instance2.getAttributeValuesList(att);
	            if (vals2 == null)
	                vals2 = new ArrayList();
	            if (vals1.size() != vals2.size())
	                return false;
	            // All instances should be the same.
	            if (!vals1.equals(vals2))
	                return false;
	        }
	    }
	    return true;
	}

	/**
	 * Replace an instance with another one for a GKInstance.
	 * @param referrer
	 * @param oldReference
	 * @param newReference
	 * @throws Exception
	 */
	public static void replaceReference(GKInstance referrer, GKInstance oldReference, GKInstance newReference)
            throws Exception {
        GKSchemaAttribute att = null;
        for (Iterator it = referrer.getSchemaAttributes().iterator(); it.hasNext();) {
            att = (GKSchemaAttribute) it.next();
            if (!att.isInstanceTypeAttribute())
                continue;
            java.util.List values = referrer.getAttributeValuesList(att);
            if (values == null || values.size() == 0)
                continue;
            if (!att.isValidValue(newReference))
                continue;
            // Have to search the whole list in case oldReference is used
            // more than once, e.g. in "hasComponent".
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) == oldReference) {
                    values.set(i, newReference);
                }
            }
        }
    }
	
	   /**
     * Replace an instance with another one for a GKInstance for a specific attribute.
     * @param referrer
     * @param oldReference
     * @param newReference
     * @throws Exception
     */
    public static void replaceReference(GKInstance referrer, 
                                        GKInstance oldReference, 
                                        GKInstance newReference,
                                        String attributeName) throws Exception {
        SchemaAttribute att = referrer.getSchemClass().getAttribute(attributeName);
        if (!att.isValidValue(newReference))
            throw new InvalidAttributeException(referrer.getSchemClass(),
                                                attributeName);
        java.util.List values = referrer.getAttributeValuesList(att);
        if (values == null || values.size() == 0)
            return;
        // Have to search the whole list in case oldReference is used
        // more than once, e.g. in "hasComponent".
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == oldReference) {
                values.set(i, newReference);
            }
        }
    }
	
	/**
	 * A utility method to check if one instance is a descendent of another instance
	 * in a hierarchical structure.
	 * @param checkingInstance the instance to be checked for
	 * @param instance the instance to be checked against
	 * @return
	 */
	public static boolean isDescendentOf(GKInstance checkingInstance, GKInstance instance) {
	    Set next = new HashSet();
	    Set current = new HashSet();
	    current.add(instance);
	    GKInstance parent = null;
	    List values = null;
	    try {
            while (current.size() > 0) {
                for (Iterator it = current.iterator(); it.hasNext();) {
                    parent = (GKInstance) it.next();
                    if (parent.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
                        values = parent.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
                        if (values != null && values.size() > 0) {
                            if (values.contains(checkingInstance))
                                return true;
                            next.addAll(values); // Push for the next iteration
                        }
                    }
                    if (parent.getSchemClass().isValidAttribute("hasComponent")) {
                        values = parent.getAttributeValuesList("hasComponent");
                        if (values != null && values.size() > 0) {
                            if (values.contains(checkingInstance))
                                return true;
                            next.addAll(values); // Push for the next iteration
                        }
                    }
                    if (parent.getSchemClass().isValidAttribute("hasInstance")) {
                        values = parent.getAttributeValuesList("hasInstance");
                        if (values != null && values.size() > 0) {
                            if (values.contains(checkingInstance))
                                return true;
                            next.addAll(values);
                        }
                    }
                }
                current.clear();
                current.addAll(next);
                next.clear();
            }
        }
        catch (Exception e) {
            System.err.println("InstanceUtilities.isDescendentOf(): " + e);
            e.printStackTrace();
        }
        return false;
	}
	
	/**
	 * Grep the top level events from the specified collection of events. An top level
	 * event is an event 
	 * @param events
	 * @return
	 * @throws Exception
	 */
	public static List grepTopLevelEvents(Collection events) throws Exception {
	    // Grep all events that are contained by other events
	    Set containedEvents = new HashSet();
	    GKInstance event = null;
	    for (Iterator it = events.iterator(); it.hasNext();) {
	        event = (GKInstance) it.next();
            if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
                List components = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
                if (components != null) 
                    containedEvents.addAll(components);
            }
            if (event.getSchemClass().isValidAttribute("hasComponent")) {
	            List components = event.getAttributeValuesList("hasComponent");
	            if (components != null) 
	                containedEvents.addAll(components);
	        }
	        if (event.getSchemClass().isValidAttribute("hasInstance")) {
	            List instances = event.getAttributeValuesList("hasInstance");
	            if (instances != null)
	                containedEvents.addAll(instances);
	        }
	        if (event.getSchemClass().isValidAttribute("hasMember")) {
	            List instances = event.getAttributeValuesList("hasMember");
	            if (instances != null)
	                containedEvents.addAll(instances);
	        }
	        if (event.getSchemClass().isValidAttribute("hasSpecialisedForm")) {
	            List instances = event.getAttributeValuesList("hasSpecialisedForm");
	            if (instances != null)
	                containedEvents.addAll(instances);
	        }
	    }
	    List topEvents = new ArrayList(events);
	    topEvents.removeAll(containedEvents);
	    return topEvents;
	}
    
    /**
     * Some attributes in ReferenceSequencePeptide can be directly used by its
     * EntityWithAccessionedSequence referrers. Use this method to copy these
     * common attributes.
     * @param ewas
     * @param refPepSeq
     * @throws Exception
     */
    public static void copyAttributesFromRefPepSeqToEwas(GKInstance ewas,
                                                         GKInstance instance) throws Exception {
        // Copy properties from ReferencePeptideSequence
        ewas.setAttributeValue(ReactomeJavaConstants.referenceEntity,
                               instance);
        GKInstance species = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
        if (species != null)
            ewas.setAttributeValue(ReactomeJavaConstants.species,
                                   species);
        List<String> ewasNames = new ArrayList<String>();
        // The first gene name
        List names = instance.getAttributeValuesList(ReactomeJavaConstants.geneName);
        if (names != null && names.size() > 0) {
            String name = (String) names.get(0);
            ewasNames.add(name);
        }
        // Names: text before ( in description, uniprot ID in secondaryIdentifier,
        // and the first name in the geneName
        String name = (String) instance.getAttributeValue(ReactomeJavaConstants.description);
        if (name != null) {
            name = name.trim();
            int index = name.indexOf("(");
            if (index > 0)
                name = name.substring(0, index).trim();
            // Check if the new format is used (03/15/09)
            int index1 = name.indexOf("shortName");
            int index2 = name.indexOf("alternativeName");
            // Do another possible parse
            if (index1 > 0 || index2 > 0) {
                // Check which one should be used
                if (index1 < 0)
                    index1 = name.length();
                if (index2 < 0)
                    index2 = name.length();
                index = Math.min(index1, index2);
                name = name.substring(0, index).trim();
            }
            // Sometime, description starting with "recommendedName"
            if (name.startsWith("recommendedName")) {
                name = name.substring("recommendedName".length() + 1).trim(); // Remove: recommendedName:
            }
            if (!ewasNames.contains(name))
                ewasNames.add(name);
//            ewas.addAttributeValue(ReactomeJavaConstants.name, name);
        }
        // The first name
        names = instance.getAttributeValuesList(ReactomeJavaConstants.name);
        if (names != null && names.size() > 0) {
            name = (String) names.get(0);
            if (!ewasNames.contains(name))
                ewasNames.add(name);
//            ewas.addAttributeValue(ReactomeJavaConstants.name, name);
        }
        // uniprot id
        List ids = instance.getAttributeValuesList(ReactomeJavaConstants.secondaryIdentifier);
        // Pattern matching for uniprot id
        Pattern pattern = Pattern.compile("_(\\p{Upper})+$");
        if (ids != null && ids.size() > 0) {
            for (Iterator it1 = ids.iterator(); it1.hasNext();) {
                String id = (String) it1.next();
                if (pattern.matcher(id).find()) {
                    if (!ewasNames.contains(id))
                        ewasNames.add(id);
//                    ewas.addAttributeValue(ReactomeJavaConstants.name, id);
                    break;
                }
            }
        }
        ewas.setAttributeValue(ReactomeJavaConstants.name, ewasNames);
        InstanceDisplayNameGenerator.setDisplayName(ewas);
    }
    
    /**
     * Get entity components in a specified pathway instance.
     * @param pathway
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static Set<GKInstance> grepPathwayParticipants(GKInstance pathway) throws Exception {
        // First load all PhysicalEntities involved in Reactions
        Set<GKInstance> participants = new HashSet<GKInstance>();
        // Complexes have be pushed into this set too.
        Set<GKInstance> components = grepPathwayEventComponents(pathway);
        for (GKInstance tmp : components) {
            if (tmp.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                Set<GKInstance> rxtPaticipants = InstanceUtilities.getReactionParticipants(tmp);
                participants.addAll(rxtPaticipants);
            }
            else if (tmp.getSchemClass().isa(ReactomeJavaConstants.Interaction)) {
                List interactors = tmp.getAttributeValuesList(ReactomeJavaConstants.interactor);
                if (interactors != null)
                    participants.addAll(interactors);
            }
        }
        return participants;
    }
    
    /**
     * Get event component (ReactionlikeEvent or Interaction) in a specified pathway instance. Complex is not included.
     * @param pathway
     * @return
     * @throws Exception
     */
    public static Set<GKInstance> grepPathwayEventComponents(GKInstance pathway) throws Exception {
        // Load all pathway events, not including complexes
        Set<GKInstance> components = new HashSet<GKInstance>();
        // Note: pathway event component (reaction and pathway) counted only once. However,
        // complexes and entities are counted multiple times based reactions. Reaction can
        // be regarded as sentences in pathway documents.
        Set<GKInstance> current = new HashSet<GKInstance>();
        current.add(pathway);
        Set<GKInstance> next = new HashSet<GKInstance>();
        // To avoid any possible self-circular
        Set<GKInstance> checked = new HashSet<GKInstance>();
        while (current.size() > 0) {
            for (GKInstance tmp : current) {
                if (tmp.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                    components.add(tmp);
                }
                else if (tmp.getSchemClass().isa(ReactomeJavaConstants.Interaction)) {
                    components.add(tmp);
                }
                // Not use else if to account members for some reaction instances.
                if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) 
                    next.addAll(tmp.getAttributeValuesList(ReactomeJavaConstants.hasComponent));
                else if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
                    next.addAll(tmp.getAttributeValuesList(ReactomeJavaConstants.hasEvent));
                else if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasSpecialisedForm))
                    next.addAll(tmp.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm));
                else if (tmp.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember))
                    next.addAll(tmp.getAttributeValuesList(ReactomeJavaConstants.hasMember));
                checked.add(tmp);
            }
            current.clear();
            current.addAll(next);
            next.clear();
            current.removeAll(checked); // Remove any instance that has been checked!
        }
        return components;
    }
    
    /**
     * Grep all PhysicalEntities participating in a specified reaction
     * @param reaction
     * @return
     * @throws Exception
     */
    public static Set<GKInstance> getReactionParticipants(GKInstance reaction) throws Exception {
        if (!(reaction.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent) ||
              reaction.getSchemClass().isa(ReactomeJavaConstants.Reaction))) // For backward compatibilty
            throw new IllegalArgumentException("InstanceUtilities.getReactionParticipants(): " +
                    "the passed instance should be a Reaction.");
        Set<GKInstance> set = new HashSet<GKInstance>();
        List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        getReactionParticipants(inputs, set);
        List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        getReactionParticipants(outputs, set);
        List cas = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (cas != null && cas.size() > 0) {
            for (Iterator it = cas.iterator(); it.hasNext();) {
                GKInstance ca = (GKInstance) it.next();
                GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (catalyst != null)
                    set.add(catalyst);
            }
        }
        Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
        if (regulations != null && regulations.size() > 0) {
            for (Iterator it = regulations.iterator(); it.hasNext();) {
                GKInstance regulation = (GKInstance) it.next();
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator == null)
                    continue;
                // Only take physical entity
                if (regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                    set.add(regulator);
            }
        }
        return set;
    }
     
     private static void getReactionParticipants(List list,
                                                Set set) {
         if (list == null || list.size() == 0)
             return;
         for (Iterator it = list.iterator(); it.hasNext();)
             set.add(it.next());
     }
     
     /**
      * Get events (ReactionlikeEvent, Pathways) in a specified Pathway instance.
      * @param pathway
      * @return
      */
     public static Set<GKInstance> getContainedEvents(GKInstance pathway) {
         Set<GKInstance> set = null;
         try {
             set = getContainedInstances(pathway, 
                                         ReactomeJavaConstants.hasEvent,
                                         ReactomeJavaConstants.hasMember);
         }
         catch(Exception e) {
             System.err.println("InstanceUtilities.getContainedEvents(): " + e);
             e.printStackTrace();
         }
         if (set == null) // To avoid null exception in the client program.
             set = new HashSet<GKInstance>();
         return set;
     }
     
     public static Set<GKInstance> getContainedEvents(List<GKInstance> pathways) {
         Set<GKInstance> rtn = new HashSet<GKInstance>();
         for (GKInstance pathway : pathways)
             rtn.addAll(getContainedEvents(pathway));
         return rtn;
     }
     
     public static Set<GKInstance> getContainedInstances(GKInstance container, 
                                                         String... attNames) throws Exception {
         Set<GKInstance> set = new HashSet<GKInstance>();
         Set<GKInstance> current = new HashSet<GKInstance>();
         current.add(container);
         Set<GKInstance> next = new HashSet<GKInstance>();
         while (current.size() > 0) {
             for (GKInstance tmp : current) {
                 // Just in case there is a self-cycle
                 if (set.contains(tmp))
                     continue;
                 set.add(tmp);
                 for (String attName : attNames) {
                     if (tmp.getSchemClass().isValidAttribute(attName)) {
                         List list = tmp.getAttributeValuesList(attName);
                         if (list != null)
                             next.addAll(list);
                     }
                 }
             }
             current.clear();
             current.addAll(next);
             next.clear();
         }
         // Don't need the container itself.
         set.remove(container);
         return set;
     }
     
     public static Set<GKInstance> getContainedComponents(GKInstance complex) throws Exception {
         return getContainedInstances(complex, 
                                      ReactomeJavaConstants.hasComponent);
     }
     
     public static GKInstance getLatestIEFromInstance(GKInstance instance) throws Exception {
         GKInstance latestIE = null;
         List list = instance.getAttributeValuesList(ReactomeJavaConstants.modified);
         if (list != null && list.size() > 0)
             latestIE = (GKInstance) list.get(list.size() - 1);
         else {
             // Get the created one
             latestIE = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.created);
         }
         return latestIE;
     }
     
     /**
      * Get a set of RefPepSeq instances from a passed Pathway instance.
      * @param pathway
      * @return
      * @throws Exception
      */
     public static Set<GKInstance> grepRefPepSeqsFromPathway(GKInstance pathway) throws Exception {
         Set<GKInstance> participants = grepPathwayParticipants(pathway);
         Set<GKInstance> refPepSeqs = new HashSet<GKInstance>();
         for (GKInstance participant : participants) {
             if (!participant.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                 continue;
             refPepSeqs.addAll(grepRefPepSeqsFromPhysicalEntity(participant));
         }
         return refPepSeqs;
     }
     
     /**
      * This utilitiy method is used to pull out all ReferenceEntity instances related to the passed
      * PhyiscalEntity instance.
      * @param pe
      * @return
      * @throws Exception
      */
     public static Set<GKInstance> grepReferenceEntitiesForPE(GKInstance pe) throws Exception {
         if (!pe.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
             throw new IllegalArgumentException(pe + " should be a PhysicalEntity!");
         Set<GKInstance> allRelatedPEs = getContainedInstances(pe, 
                                                               ReactomeJavaConstants.hasComponent,
                                                               ReactomeJavaConstants.hasMember,
                                                               ReactomeJavaConstants.hasCandidate,
                                                               ReactomeJavaConstants.repeatedUnit);
         allRelatedPEs.add(pe);  // Need itself: this has been removed in the above method
         Set<GKInstance> refEntities = new HashSet<GKInstance>();
         for (GKInstance inst : allRelatedPEs) {
             if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
                 GKInstance refEntity = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                 refEntities.add(refEntity);
             }
         }
         return refEntities;
     }
     
     /**
      * Get a set of RefPepSeq instances from a passed PhysicalEntity.
      * @param interactor
      * @return
      * @throws Exception
      */
     public static Set<GKInstance> grepRefPepSeqsFromPhysicalEntity(GKInstance interactor) throws Exception {
         Set<GKInstance> refPepSeq = new HashSet<GKInstance>();
         if (interactor.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
             GKInstance ref = (GKInstance) interactor.getAttributeValue(ReactomeJavaConstants.referenceEntity);
             if (ref != null) {
                 if (ref.getSchemClass().isa(ReactomeJavaConstants.ReferencePeptideSequence) ||
                     ref.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct))
                     refPepSeq.add(ref);
             }
         }
         else if (interactor.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
             grepRefPepSeqFromInstanceRecursively(interactor, refPepSeq);
         }
         else if (interactor.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
             grepRefPepSeqFromInstanceRecursively(interactor, refPepSeq);
         }
         return refPepSeq;
     }
     
     private static void grepRefPepSeqFromInstanceRecursively(GKInstance complex, 
                                                       Set<GKInstance> refPepSeq) throws Exception {
         Set<GKInstance> current = new HashSet<GKInstance>();
         current.add(complex);
         Set<GKInstance> next = new HashSet<GKInstance>();
         Set<GKInstance> children = new HashSet<GKInstance>();
         while (current.size() > 0) {
             for (GKInstance inst : current) {
                 children.clear();
                 if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) {
                     List list = inst.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
                     if (list != null)
                         children.addAll(list);
                 }
                 if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                     List list = inst.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                     if (list != null)
                         children.addAll(list);
                 }
                 // Check for candidate set: added on June 24, 2009
                 if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
                     List list = inst.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                     if (list != null)
                         children.addAll(list);
                 }
                 if (children.size() == 0)
                     continue;
                 for (Iterator it = children.iterator(); it.hasNext();) {
                     GKInstance tmp = (GKInstance) it.next();
                     if (tmp.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                         next.add(tmp);
                     else if (tmp.getSchemClass().isa(ReactomeJavaConstants.Complex))
                         next.add(tmp);
                     else if (tmp.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
                         GKInstance ref = (GKInstance) tmp.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                         if (ref != null &&
                             (ref.getSchemClass().isa(ReactomeJavaConstants.ReferencePeptideSequence) ||
                              ref.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct)))
                             refPepSeq.add(ref);
                     }
                 }
             }
             current.clear();
             current.addAll(next);
             next.clear();
         }
     }
     
     /**
      * Check if two instances are member and set relationship.
      * @param set
      * @param member
      * @return
      * @throws Exception
      */
     public static boolean isEntitySetAndMember(GKInstance set, GKInstance member) throws Exception {
         if (set.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
             List<?> values = set.getAttributeValuesList(ReactomeJavaConstants.hasMember);
             if (values != null && values.contains(member)) {
                 return true;
             }
         }
         if (set.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
             // Check if hasCandidate can be used
             List<?> values = set.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
             if (values != null && values.contains(member)) {
                 return true;
             }
         }
         return false;
     }
     
     /**
      * Check if two EntitySet instances have shared members. Both hasMember and hasCandidate values
      * are checked if any. The checking is recursively, which means that if an EntitySet uses another 
      * EntitySet as its member, the members of another EntitySet will be checked. Also if these two
      * EntitySets have isEntitySetAndMember relationship, false will be returned.
      * @param inst1
      * @param inst2
      * @return
      * @throws Exception
      */
     public static boolean hasSharedMembers(GKInstance inst1, GKInstance inst2) {
         // If these two instances are the same, false should be returned
         if (inst1 == inst2)
             return false; 
         if (!inst1.getSchemClass().isa(ReactomeJavaConstants.EntitySet) ||
             !inst2.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
             return false; // Treat it as false to avoid throws exception
         try {
             if (isEntitySetAndMember(inst1, inst2) || isEntitySetAndMember(inst2, inst1))
                 return false; // These cases should be escaped
             Set<GKInstance> members1 = getContainedInstances(inst1,
                                                              ReactomeJavaConstants.hasMember, 
                                                              ReactomeJavaConstants.hasCandidate);
             Set<GKInstance> members2 = getContainedInstances(inst2,
                                                              ReactomeJavaConstants.hasMember,
                                                              ReactomeJavaConstants.hasCandidate);
             members2.retainAll(members1);
             return members2.size() > 0;
         }
         catch(Exception e) {
             e.printStackTrace();
         }
         return false;
     }
     
     /**
      * Match a list of gene names to a list of DB_IDs of PEs. A PE contains
      * a gene whose names is in the gene name is regarded as matched. The check
      * is done recursively. For example, if a PE is a complex, its components 
      * will be checked.
      * @param dbIds
      * @param geneNames
      * @param dba
      * @return
      * @throws Exception
      */
     public static List<Long> checkMatchEntityIds(List<Long> dbIds, 
                                           List<String> geneNames,
                                           PersistenceAdaptor dba) throws Exception {
         Set<GKInstance> refEntities = null;
         boolean isFound = false;
         Set<String> queryNames = new HashSet<String>();
         for (String geneName : geneNames)
             queryNames.add(geneName);
         List<Long> rtn = new ArrayList<Long>();
         for (Long dbId : dbIds) {
             GKInstance instance = dba.fetchInstance(dbId);
             // Just in case since we have switched the database
             if (instance == null)
                 continue;
             refEntities = null;
             if (instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
                 refEntities = grepRefPepSeqsFromPhysicalEntity(instance);
             else if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                 refEntities = grepRefPepSeqsFromPathway(instance);
             if (refEntities == null)
                 continue;
             for (GKInstance refEntity : refEntities) {
                 List<?> values = refEntity.getAttributeValuesList(ReactomeJavaConstants.geneName);
                 if (values == null)
                     continue;
                 isFound = false;
                 for (Iterator<?> it = values.iterator(); it.hasNext();) {
                     if (queryNames.contains(it.next())) {
                         isFound = true;
                         break;
                     }
                 }
                 if (isFound) {
                     rtn.add(dbId);
                     break;
                 }
             }
         }
         return rtn;
     }
	
	//public static String encodeLineSeparators(String text) {
	//    return text.replaceAll(FileAdaptor.LINE_END + "", "<br>");
	//}
}
