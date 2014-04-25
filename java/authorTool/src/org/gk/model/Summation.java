/*
 * Created on Feb 3, 2004
 */
package org.gk.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A class to encapsulate the summation text.
 * @author wugm
 */
public class Summation implements Serializable {
	
	private String text;
	private Long DB_ID;
	private java.util.List references;
    // To track changes
    private boolean isChanged = false;
	
	public Summation() {
	}
	
	public Summation(String text, Long db_ID) {
		this.text = text;
		this.DB_ID = db_ID;
	}
	
	public void setText(String txt) {
		this.text = txt;
	}
	
	public String getText() {
		return this.text;
	}
	
	public void setDB_ID(Long id) {
		this.DB_ID = id;
	}
	
	public Long getDB_ID() {
		return this.DB_ID;
	}
	
	public java.util.List getReferences() {
		return this.references;
	}
	
	public void setReferences(java.util.List references) {
		this.references = references;
	}
	
	public void addReference(Reference reference) {
		if (references == null)
			references = new ArrayList();
		references.add(reference);
	}
	
	public void removeReference(Reference reference) {
		if (references != null)
			references.remove(reference);
	}
	
	public boolean isEmpty() {
		if (DB_ID == null && references == null && text == null)
			return true;
		return false;
	}
	
	public Object clone() {
		Summation clone = new Summation();
		clone.setDB_ID(DB_ID);
		clone.setText(text);
		if (references != null)
			clone.setReferences(new ArrayList(references));
		return clone;
	}
    
    public boolean isChanged() {
        return this.isChanged;
    }
    
    public void setIsChanged(boolean isChanged) {
        this.isChanged = isChanged;
    }

}
