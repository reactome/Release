/*
 * Created on Jul 28, 2003
 */
package org.gk.model;

import java.io.Serializable;

/**
 * For describing the instance editing event.
 * @author wgm
 */
public class InstanceEdit implements Serializable {
	private String authorName;
	private String date;
	
	public InstanceEdit() {
	}
	
	public InstanceEdit(String author, String date) {
		this.authorName = author;
		this.date = date;
	}

	/**
	 * @return
	 */
	public String getAuthorName() {
		return authorName;
	}

	/**
	 * @return
	 */
	public String getDate() {
		return date;
	}

	/**
	 * @param string
	 */
	public void setAuthorName(String author) {
		authorName = author;
	}

	/**
	 * @param date
	 */
	public void setDate(String date) {
		this.date = date;
	}

}
