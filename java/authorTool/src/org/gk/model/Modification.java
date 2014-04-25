/*
 * Created on Jul 30, 2003
 */
package org.gk.model;

import java.io.Serializable;

/**
 * For modifications.
 * @author wgm
 */
public class Modification implements Serializable {
	private int coordinate = -1;
	private String residue;
	private String modification;
	private String modificationDbId;
	private Long db_ID; // DB_ID from the gk_central database.

	public Modification() {
	}
	
	public Modification(int position, String residue, String modification, String dbID) {
		this.coordinate = position;
		this.residue = residue;
		this.modification = modification;
		this.modificationDbId = dbID;
	}
	
	public void setDB_ID(Long id) {
		db_ID = id;
	}
	
	public Long getDB_ID() {
		return this.db_ID;
	}
	
	/**
	 * @return
	 */
	public int getCoordinate() {
		return coordinate;
	}

	/**
	 * @return
	 */
	public String getModificationDbID() {
		return modificationDbId;
	}

	/**
	 * @return
	 */
	public String getModification() {
		return modification;
	}

	/**
	 * @return
	 */
	public String getResidue() {
		return residue;
	}

	/**
	 * @param i
	 */
	public void setCoordinate(int i) {
		coordinate = i;
	}

	/**
	 * @param string
	 */
	public void setModificationDbID(String string) {
		modificationDbId = string;
	}

	/**
	 * @param string
	 */
	public void setModification(String string) {
		modification = string;
	}

	/**
	 * @param string
	 */
	public void setResidue(String string) {
		residue = string;
	}
	
	public Object clone() {
		Modification clone = new Modification();
		clone.coordinate = coordinate;
		clone.residue = residue;
		clone.modification = modification;
		clone.modificationDbId = modificationDbId;
		clone.db_ID = db_ID;
		return clone;
	}

}
