/*
 * Created on Jul 10, 2003
 */
package org.gk.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class describes a literature reference.
 * @author wgm
 */
public class Reference implements Serializable {

	private long pmid;
	@Deprecated
	private String author;
	private String journal;
	private int year;
	private String volume; // Should be a String for : 10 Suppl 1.
	private String page = null;
	private String title;
	private Long DB_ID;
	private List<Person> authors;
	
	public Reference() {
	}
	
	public Reference(long pmid, String author, int year,
	                 String volume, String page, 
	                 String title) {
		this.pmid = pmid;
		this.author = author;
		this.year = year;
		this.volume = volume;
		this.page = page;
		this.title = title;
	}
	
	public String getAuthor() {
		return author;
	}

	/**
	 * @return the name of the journal.
	 */
	public String getJournal() {
		return journal;
	}


	/**
	 * @return the PubMed ID.
	 */
	public long getPmid() {
		return pmid;
	}

	/**
	 * @return the artile's title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the volume.
	 */
	public String getVolume() {
		return volume;
	}

	public int getYear() {
		return year;
	}

	public void setAuthor(String string) {
		author = string;
	}
	
	public void setJournal(String string) {
		journal = string;
	}

	public void setPmid(long id) {
		pmid = id;
	}

	public void setTitle(String string) {
		title = string;
	}

	public void setVolume(String i) {
		volume = i;
	}

	public void setYear(int i) {
		year = i;
	}
	
	public void setPage(String page) {
		this.page = page;
	}
	
	public String getPage() {
		return this.page;
	}
	
	public Long getDB_ID() {
		return DB_ID;
	}

	public void setDB_ID(Long long1) {
		DB_ID = long1;
	}
	
	public void addAuthor(Person author) {
	    if (authors == null)
	        authors = new ArrayList<Person>();
	    authors.add(author);
	}
	
	public List<Person> getAuthors() {
	    return this.authors;
	}

}
