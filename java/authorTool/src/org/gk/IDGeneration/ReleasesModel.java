/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.gk.model.GKInstance;


/** 
 *  This class mainly provides a layer between the
 *  identifier database and the ReleasePane GUI.  It
 *  also provides a convenient place to store releases
 *  that are especially interesting.
 *  
 *  Inherits from JTableModel so that it can be used
 *  as the model for releasesTable in ReleasesPane
 *  Implements the methods needed for this, allowing
 *  direct access to the identifier database through
 *  these methods.
 *  
 * @author croft
 */
public class ReleasesModel extends AbstractTableModel {
	private GKInstance previousRelease = null;
	private GKInstance currentRelease = null;
	private IdentifierDatabase identifierDatabase;
	
	public ReleasesModel(IdentifierDatabase identifierDatabase) {
		if (identifierDatabase==null)
			System.err.println("ReleasesModel: WARNING - identifierDatabase is null");
		
		this.identifierDatabase = identifierDatabase;
		
		resynchronize();
	}
	
	public Class getColumnClass(int col) {
		return String.class;
	}
	
	public boolean isCellEditable(int row, int col) {
		return true;
	}
	
	public GKInstance getPreviousRelease() {
		return previousRelease;
	}
	
	public void setPreviousRelease(GKInstance previousRelease) {
		this.previousRelease = previousRelease;
	}
	
	public GKInstance getCurrentRelease() {
		return currentRelease;
	}

	public void setCurrentRelease(GKInstance currentRelease) {
		this.currentRelease = currentRelease;
	}

	/**
	 * Stores a new release in the database.
	 * 
	 * @param newRelease
	 */
	public void storeRelease(GKInstance release) {
		identifierDatabase.storeRelease(release);
		
		// Let table know that something has changed
		resynchronize();
	}
	
	/**
	 * Assumes that the given release is already in the database
	 * and updates the database to accomodate any changes to the
	 * release.
	 * 
	 * @param newRelease
	 */
	public void updateRelease(GKInstance release) {
		IdentifierDatabase.updateRelease(release);
		
		// Let table know that something has changed
		resynchronize();
	}
	
	/**
	 * Assumes that the release in the given table row is already in the database
	 * and updates the database to accomodate any changes to the
	 * release.
	 * 
	 * @param newRelease
	 */
	public void updateRelease(int index) {
		updateRelease(getReleaseByIndex(index));
	}
	
	/**
	 * Assumes that the given release is already in the database
	 * and updates the database to delete the release.
	 * 
	 * @param newRelease
	 */
	public void deleteRelease(GKInstance release) {
		IdentifierDatabase.deleteInstance(release);
		
		// Let table know that something has changed
		resynchronize();
	}
	
	public int getColumnCount() {
		return 2;
	}

    public String getColumnName(int col) {
    	if (col==0)
    		return "Release";
    	else if (col==1)
    		return "Date";
    	else
    		return "";
    }
    
    private int getReleasesCount() {
    	// TODO: set project name to something sensible
		int releasesCount = IdentifierDatabase.getReleasesCount(null);
		
		return releasesCount;
    }

	public int getRowCount() {
		int releasesCount = getReleasesCount();
		
		return releasesCount;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
			GKInstance release = getReleaseByIndex(rowIndex);
			if (release==null)
				return null;
			
			return getValueAtColumn(release, columnIndex);
	}
	
	/**
	 * Get a release from the list of known releases in the database.
	 * @param index
	 * @return
	 */
	public GKInstance getReleaseByIndex(int rowIndex) {
		if (rowIndex<0)
			return null;
		
		int releasesCount = getReleasesCount();
    	// TODO: set project name to something sensible
		List releases = IdentifierDatabase.getReleasesSorted(null);
		if (releases!=null && releasesCount>rowIndex)
			return (GKInstance)releases.toArray()[rowIndex];
		
		return null;
	}
	
	private Object getValueAtColumn(GKInstance release, int columnIndex) {
		if (release==null)
			return null;
		
		try {
			if (columnIndex==0)
				return release.getAttributeValue("num");
			else if (columnIndex==1)
				return release.getAttributeValue("dateTime");
			System.err.println("getValueAt: unknown column " + columnIndex);
		} catch (Exception e) {
			System.err.println("getValueAt: couldnt get release number or date");
			e.printStackTrace();
		}
		
		return null;
	}
	

	/**
	 * This method is used to edit a value in the cell.
	 */
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		GKInstance release = getReleaseByIndex(rowIndex);
		if (release!=null)
			setValueAtColumn(value, release, columnIndex);
	}
	
	private void setValueAtColumn(Object value, GKInstance release, int columnIndex) {
		if (release==null)
			return;
		
		try {
			if (columnIndex==0)
				release.setAttributeValue("num", value);
			else if (columnIndex==1)
				release.setAttributeValue("dateTime", value);
		} catch (Exception e) {
			System.err.println("getValueAt: couldnt get release number or date");
			e.printStackTrace();
		}
	}
	
	public String getLastNonNullReleaseNum() {
    	// TODO: set project name to something sensible
		String lastNonNullReleaseNumString = IdentifierDatabase.getLastNonNullReleaseNum(null);

		return lastNonNullReleaseNumString;		
	}
	
	public String getReleaseNum(int index) {
		GKInstance release = getReleaseByIndex(index);

		if (release==null)
			return "";
		
		try {
			return (String)release.getAttributeValue("num");
		} catch (Exception e) {
			System.err.println("getReleaseNum: couldnt get release number or date");
			e.printStackTrace();
		}
		
		return "";		
	}
	
	/**
	 * Get a release from the list of known releases in the database.
	 * @param index
	 * @return
	 */
	public void copySliceToReleaseDbParamsByIndex(int rowIndex) {
		GKInstance release = getReleaseByIndex(rowIndex);
		
		// Nothing selected
		if (release==null)
			return;
		
		
		updateRelease(release);
	}
	
	public void resynchronize() {
		// Let table know that something has changed
		fireTableDataChanged();
	}	
}