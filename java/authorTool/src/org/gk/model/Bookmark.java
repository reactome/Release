/*
 * Created on Feb 17, 2004
 */
package org.gk.model;

/**
 * A data strucure to hold the bookmark information.
 * @author wugm
 */

public class Bookmark {
	private String displayName;
	private Long dbID;
	private String type;
	private String description;
	
	public Bookmark() {}

	public Bookmark(String displayName, Long dbID, String type) {
		this.displayName = displayName;
		this.dbID = dbID;
		this.type = type;
	}

	public Long getDbID() {
		return dbID;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getType() {
		return type;
	}

	public void setDbID(Long long1) {
		dbID = long1;
	}

	public void setDisplayName(String string) {
		displayName = string;
	}

	public void setType(String string) {
		type = string;
	}
	
	public void setDescription(String desc) {
	    this.description = desc;
	}
	
	public String getDescription() {
	    return this.description;
	}
}