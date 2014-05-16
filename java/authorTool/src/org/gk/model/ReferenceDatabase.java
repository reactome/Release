/*
 * Created on Jul 29, 2003
 */
package org.gk.model;

import java.io.Serializable;

/**
 * This class describes the ReferenceDatabase.
 * @author wgm
 */
public class ReferenceDatabase implements Serializable {
	private String name;
	private String queryURL;
	private String accessURL;
	
	public ReferenceDatabase() {
	}
	
	public ReferenceDatabase(String name, String queryURL, String accessURL) {
		this.name = name;
		this.queryURL = queryURL;
		this.accessURL = accessURL;
	}

	public String getAccessURL() {
		return accessURL;
	}

	public String getName() {
		return name;
	}

	public String getQueryURL() {
		return queryURL;
	}

	public void setAccessURL(String string) {
		accessURL = string;
	}

	public void setName(String string) {
		name = string;
	}

	public void setQueryURL(String string) {
		queryURL = string;
	}

	public String toString() {
		return this.name;
	}
}
