/*
 * Created on May 22, 20056
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;

/** 
 *  Reacts to the buttons in IncludeInstancesPane
 * @author croft
 */
public class IncludeInstances {
	// Classes that might reasonably be considered as
	// defaults when creating new stable IDs
	private static String[] defaultClasses = {
			"Event",
			"PhysicalEntity",
			"Regulation",
	};
	
	private static String[] forbiddenClasses = {
			"StableIdentifier",
	};
	
	public List removeForbiddenClasses(List selectedClasses) {
		List newSelectedClasses = new ArrayList();
		GKSchemaClass selectedClass;
		String forbiddenClass;
		for (Iterator itc = selectedClasses.iterator(); itc.hasNext();) {
			selectedClass = (GKSchemaClass)itc.next();
			for (int i=0; i<forbiddenClasses.length; i++) {
				forbiddenClass = forbiddenClasses[i];
				if (selectedClass.getName().equals(forbiddenClass))
					continue;
				newSelectedClasses.add(selectedClass);
			}
		}
		return newSelectedClasses;
	}
	
	/**
	 * If all subclasses of a given schema class are present
	 * in the list, delete the subclasses and replace with
	 * the schema class.
	 * 
	 * @param classes
	 * @return
	 */
	public List extractRootClasses(List classes) {
		List rootClasses = new ArrayList(classes); // clone
		GKSchemaClass schemaClass;
		for (Iterator itc = classes.iterator(); itc.hasNext();) {
			schemaClass = (GKSchemaClass)itc.next();
			deleteSubclasses(classes, rootClasses, schemaClass);
		}
		
		return rootClasses;
	}
	
	private void deleteSubclasses(List classes, List rootClasses, GKSchemaClass schemaClass) {
		Collection subclasses = schemaClass.getSubClasses();
		GKSchemaClass classesClass;
		GKSchemaClass subclass;
		String subclassName;
		for (Iterator its = subclasses.iterator(); its.hasNext();) {
			subclass = (GKSchemaClass)its.next();
			subclassName = subclass.getName();
			for (Iterator it = classes.iterator(); it.hasNext();) {
				classesClass = (GKSchemaClass)it.next();
				if (classesClass.getName().equals(subclassName)) {
					deleteSubclasses(classes, rootClasses, classesClass);
					rootClasses.remove(classesClass);
				}
			}
		}
	}

	public static String[] getDefaultClasses() {
		return defaultClasses;
	}

	public static String[] getForbiddenClasses() {
		return forbiddenClasses;
	}
	
	public static boolean isInDefaultClasses(GKInstance instance) {
		return isInClasses(defaultClasses, instance);
	}
	
	public static boolean isInForbiddenClasses(GKInstance instance) {
		return isInClasses(forbiddenClasses, instance);
	}
	
	public static boolean isInClasses(String[] classeNames, GKInstance instance) {
		if (instance==null)
			return false;
		
		SchemaClass schemaClass = instance.getSchemClass();
		String className;
		for (int i=0; i<classeNames.length; i++) {
			className = classeNames[i];
			if (schemaClass.isa(className))
				return true;
		}
		
		return false;
	}
}