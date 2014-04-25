/*
 * Created on Jun 27, 2003
 */
package org.gk.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author wgm
 */
public class InstanceCache {
	private Map cache = new HashMap();
	
	/**
	 * 
	 */
	public void clear() {
		cache.clear();
	}

	/**
	 * @param arg0
	 * @return
	 */
	public boolean containsKey(Object arg0) {
		return cache.containsKey(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 */
	public boolean containsValue(GKInstance arg0) {
		return cache.containsValue(arg0);
	}

	/**
	 * @return
	 */
	public Set entrySet() {
		return cache.entrySet();
	}

	/**
	 * @param arg0
	 * @return
	 */
	public GKInstance get(Object arg0) {
		return (GKInstance) cache.get(arg0);
	}

	public GKInstance get(long dbID) {
		return (GKInstance) cache.get(new Long(dbID));
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {
		return cache.isEmpty();
	}

	/**
	 * @return
	 */
	public Set keySet() {
		return cache.keySet();
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public GKInstance put(Object arg0, Instance arg1) {
		return (GKInstance) cache.put(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 */
	public Instance put(Instance arg0) {
		return put(arg0.getDBID(), arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public GKInstance put(long dbId, GKInstance arg1) {
		return put(new Long(dbId), arg1);
	}

	public GKInstance put(Long dbId, GKInstance arg1) {
		return put(dbId, arg1);
	}

	/**
	 * @param arg0
	 */
	public void putAll(Map arg0) {
		cache.putAll(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 */
	public Object remove(Object arg0) {
		return cache.remove(arg0);
	}

	/**
	 * @return
	 */
	public int size() {
		return cache.size();
	}

	/**
	 * @return
	 */
	public Collection values() {
		return cache.values();
	}

}
