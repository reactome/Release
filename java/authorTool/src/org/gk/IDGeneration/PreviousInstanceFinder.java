/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/** 
 * Determines if and how a previous instance should be found
 * 
 * @author croft
 *
 */
abstract class PreviousInstanceFinder {
	protected GKInstance currentInstance;
	protected String currentReleaseNum;
	protected String previousReleaseNum;
	protected String projectName;
	protected MySQLAdaptor previousDba = null;
	protected boolean allowIDsFromUnspecifiedReleases;
	protected IdentifierDatabase identifierDatabase;
	protected boolean test = false;
	
	protected boolean iDsFromUnspecifiedReleases;
	
	public PreviousInstanceFinder (String currentReleaseNum, MySQLAdaptor previousDba, boolean allowIDsFromUnspecifiedReleases, IdentifierDatabase identifierDatabase) {
		this.currentReleaseNum = currentReleaseNum;
		this.previousDba = previousDba;
		this.allowIDsFromUnspecifiedReleases = allowIDsFromUnspecifiedReleases;
		this.identifierDatabase = identifierDatabase;
	}
	
	public String getPreviousReleaseNum() {
		return previousReleaseNum;
	}

	public void setPreviousReleaseNum(String previousReleaseNum) {
		this.previousReleaseNum = previousReleaseNum;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String previousProjectName) {
		this.projectName = previousProjectName;
	}

	public void setCurrentInstance(GKInstance currentInstance) {
		this.currentInstance = currentInstance;
		
		iDsFromUnspecifiedReleases = false;
	}

	public void setAllowIDsFromUnspecifiedReleases(
			boolean allowIDsFromUnspecifiedReleases) {
		this.allowIDsFromUnspecifiedReleases = allowIDsFromUnspecifiedReleases;
	}

	public boolean isTest() {
		return test;
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	/**
	 * Depending on the implementation of the previous instance finder,
	 * different measures of identicality may be needed.  This default
	 * implementation simply compares the DB_IDs of the two instances.
	 * 
	 * @param instance1
	 * @param instance2
	 * @return true if instances are identical, false otherwise
	 */
	public boolean areInstancesIdentical(GKInstance instance1, GKInstance instance2) {
		if (instance1 == null && instance2 == null)
			return true;
		if (instance1 == null || instance2 == null)
			return false;
		return instance1.getDBID().equals(instance2.getDBID());
	}

	/**
	 * Returns true if the current instance is permitted for generating
	 * stable IDs, false if not.
	 * 
	 * @return
	 */
	public abstract boolean isStableIdentifierAllowed();
	
	/**
	 * Returns true if corresponding instance was found in a release
	 * older than the previous release, otherwise returns false.
	 * 
	 * @return
	 */
	public boolean isIDsFromUnspecifiedReleases() {
		return iDsFromUnspecifiedReleases;
	}

	/**
	 * Gets corresponding instance from previous release
	 * 
	 * @return
	 */
	public abstract GKInstance getPreviousInstance();
	
	/**
	 * Looks through the full set of known releases to find instances with the
	 * DB_ID=dbId.  Returns the corresponding reactome Release StableIdentifier
	 * instance if such an instance is found.  If nothing is found, returns null.
	 * @param dbId
	 * @param currentReleaseNum
	 * @return previousStableID
	 * @throws Exception
	 */
	public GKInstance generateIDsFromUnspecifiedReleases(Long dbId, String currentProjectName, String currentReleaseNum) throws Exception {
		GKInstance previousInstance = null;
		
		if (dbId == null) {
			System.err.println("PreviousInstanceFinder.generateIDsFromUnspecifiedReleases: dbId is null!!");
			return previousInstance;
		}
		
		int previousReleaseNumInt = (-1);
		int currentReleaseNumInt = (-1);
		try {
			previousReleaseNumInt = (new Integer(previousReleaseNum)).intValue();
			currentReleaseNumInt = (new Integer(currentReleaseNum)).intValue();
		} catch (NumberFormatException e) {
		}
		
		if (currentReleaseNumInt == (-1))
			return previousInstance;
		
		int oldestReleaseNumInt = currentReleaseNumInt;
		if (previousReleaseNumInt >=0 && previousReleaseNumInt < currentReleaseNumInt)
			oldestReleaseNumInt = previousReleaseNumInt;
		List validReleases = ReleaseList.createValidReleaseList(currentProjectName, oldestReleaseNumInt);
		
		// Look to see if any of these previous releases contain
		// the DB_ID.
		int releaseNumInt;
		for (Iterator it = validReleases.iterator(); it.hasNext();) {
			GKInstance release = (GKInstance)it.next();
			String releaseNum = (String)release.getAttributeValue("num");
			String projectName = (String)(((GKInstance)release.getAttributeValue("project")).getAttributeValue("name"));
			releaseNumInt = (new Integer(releaseNum)).intValue();
			
			if (releaseNumInt<currentReleaseNumInt) {
				GKInstance sliceDbParams = (GKInstance)release.getAttributeValue("sliceDbParams");
				if (sliceDbParams==null)
					continue;
				String dbName = (String)sliceDbParams.getAttributeValue("dbName");
				if (dbName==null)
					continue;
				MySQLAdaptor dba = identifierDatabase.getReleaseDbaFromReleaseNum(releaseNum, projectName);
				if (dba==null) {
					System.err.println("PreviousInstanceFinder.generateIDsFromUnspecifiedReleases: was not able to get DBA for releaseNum=" + releaseNum);
					continue;
				}
				try {
					previousInstance = dba.fetchInstance(dbId);
				} catch (NullPointerException e) {
					System.err.println("PreviousInstanceFinder.generateIDsFromUnspecifiedReleases: probable missing Schema in database: " + dbName);
					previousInstance = null;
				} catch (Exception e) {
					System.err.println("PreviousInstanceFinder.generateIDsFromUnspecifiedReleases: problem occurred when trying to fetch an instance from database: " + dbName);
					e.printStackTrace();
					previousInstance = null;
				}
				if (previousInstance != null) {
					setPreviousReleaseNum(releaseNum);
					break;
				}
			}
		}
		
		if (previousInstance != null) {
			// If the previous instance is in some way weird (not one of
			// the default set of allowed instance classes or explicitly
			// forbiden) then don't accept it.  Instead, assume that the
			// current instance is being encountered for the first time.
			if (!IncludeInstances.isInDefaultClasses(previousInstance))
				previousInstance = null;
			else if (IncludeInstances.isInForbiddenClasses(previousInstance))
				previousInstance = null;
			
			if (previousInstance!=null)
				iDsFromUnspecifiedReleases = true;
		}
		
		return previousInstance;
	}
}