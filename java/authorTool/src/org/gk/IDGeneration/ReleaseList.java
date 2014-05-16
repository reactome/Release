/*
 * Created on Nov 13, 2013
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

/** 
 * Stuff for getting lists of releases
 * 
 * @author croft
 *
 */
class ReleaseList {
	public static List<GKInstance> createValidReleaseList(String currentProjectName, int maxReleaseNum) {
		List<GKInstance> releases = IdentifierDatabase.getReleases(currentProjectName);
		int releaseNumInt;
		
		// Create a list of valid releases
		List<GKInstance> validReleases = new ArrayList<GKInstance>();
		try {
			for (Iterator it = releases.iterator(); it.hasNext();) {
				GKInstance release = (GKInstance)it.next();
				String projectName = (String)(((GKInstance)release.getAttributeValue("project")).getAttributeValue("name"));
				if (!projectName.equals(currentProjectName))
					continue;
				String releaseNum = (String)release.getAttributeValue("num");
				if (releaseNum==null)
					continue;
				try {
					releaseNumInt = (new Integer(releaseNum)).intValue();
					if (releaseNumInt > maxReleaseNum)
						continue;
				} catch (NumberFormatException e) {
					continue;
				}
				
				validReleases.add(release);
			}
		} catch (InvalidAttributeException e) {
			System.err.println("ReleaseList.ReleaseComparator.createValidReleaseList: project name or num are not valid attributes");
			e.printStackTrace(System.err);
		} catch (Exception e) {
			System.err.println("ReleaseList.ReleaseComparator.createValidReleaseList: problem getting release list");
			e.printStackTrace(System.err);
		}
		
		// Sort the releases, so that the highest numbered releases
		// come first.
		Collections.sort(validReleases, new ReleaseComparator());
//		Collections.reverse(validReleases);
		
		return validReleases;
	}

	/**
	 * Used for comparing pairs of Release instances, to determine
	 * their ordering in a sort.
	 * 
	 * @author croft
	 *
	 */
	private static class ReleaseComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			GKInstance release1 = null;
			GKInstance release2 = null;
			try {
				release1 = (GKInstance)o1;
				release2 = (GKInstance)o2;
			} catch (ClassCastException e) {
				System.err.println("ReleaseList.ReleaseComparator.compare(): One of the objects for comparison does not have type GKInstance");
				e.printStackTrace();
				return 0;
			}
			
			String releaseNum1 = "0";
			String releaseNum2 = "0";
			try {
				releaseNum1 = (String)release1.getAttributeValue("num");
				releaseNum2 = (String)release2.getAttributeValue("num");
			} catch (Exception e) {
				System.err.println("ReleaseList.ReleaseComparator.compare(): problem getting attribute value");
				e.printStackTrace();
			}
			
			int releaseNumInt1 = (new Integer(releaseNum1)).intValue();
			int releaseNumInt2 = (new Integer(releaseNum2)).intValue();
			
			// Do a simple numeric comparison of the release
			// numbers.
			return releaseNumInt2 - releaseNumInt1;
		}
	}
}