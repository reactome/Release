/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/** 
 * Determines if and how a previous instance should be found
 * 
 * @author croft
 *
 */
class DbIdPreviousInstanceFinder extends PreviousInstanceFinder {
	public DbIdPreviousInstanceFinder(String currentReleaseNum, MySQLAdaptor previousDba, boolean allowIDsFromUnspecifiedReleases, IdentifierDatabase identifierDatabase) {
		super(currentReleaseNum, previousDba, allowIDsFromUnspecifiedReleases, identifierDatabase);
	}
	
	public boolean isStableIdentifierAllowed() {
		return true;
	}
	
	public GKInstance getPreviousInstance() {
		// Look to see if there is stable ID info in the previous
		// release for the given instance (use DB_ID as key).
		GKInstance previousInstance = null;
		
		try {
			Long dbId = currentInstance.getDBID();
			
			if (previousDba!=null)
				previousInstance = previousDba.fetchInstance(dbId);
			if (previousInstance != null) {
				// If the previous instance is in some way weird (not one of
				// the default set of allowed instance classes or explicitly
				// forbiden) then don't accept it.  Instead, assume that the
				// current instance is being encountered for the first time.
				if (!IncludeInstances.isInDefaultClasses(previousInstance))
					previousInstance = null;
				else if (IncludeInstances.isInForbiddenClasses(previousInstance))
					previousInstance = null;
			}
							
			// Just because the stable ID from the database specified
			// by the user may not have been there does not mean that
			// it did not exist in some other release.  So, check to
			// see if it can be found in earlier databases.
			if (allowIDsFromUnspecifiedReleases && previousDba!=null && previousInstance==null) {
				previousInstance = generateIDsFromUnspecifiedReleases(dbId, projectName, currentReleaseNum);
			}
		} catch (Exception e) {
			System.err.println("PreviousInstanceFinder.getPreviousIdentifier: problem fetching instances");
			e.printStackTrace();
		}
		
		return previousInstance;				
	}
}