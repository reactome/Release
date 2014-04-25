/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;

/** 
 * Uses the orthologs slot to determine if two predicted
 * events should share the same stable ID.
 * 
 * @author croft
 *
 */
class OrthologousEventPreviousInstanceFinder extends PreviousInstanceFinder {
	private boolean deleteBackToSource = false;
	
	public OrthologousEventPreviousInstanceFinder(String currentReleaseNum, MySQLAdaptor previousDba, boolean allowIDsFromUnspecifiedReleases, IdentifierDatabase identifierDatabase, boolean deleteBackToSource) {
		super(currentReleaseNum, previousDba, allowIDsFromUnspecifiedReleases, identifierDatabase);
		
		this.deleteBackToSource = deleteBackToSource;
	}
	
	/**
	 * For stable ID generation to be able to work, the supplied
	 * current instance must:
	 * 
	 * 1. Be of instance class Event;
	 * 2. Have an evidenceType "inferred by electronic annotation".
	 * 
	 */
	public boolean isStableIdentifierAllowed() {
		return isStableIdentifierAllowed(currentInstance);
	}
	
	/**
	 * For stable ID generation to be able to work, the supplied
	 * current instance must:
	 * 
	 * 1. Be of instance class Event;
	 * 2. Have an evidenceType "inferred by electronic annotation".
	 * 
	 */
	public static boolean isStableIdentifierAllowed(GKInstance instance) {
		if (instance == null) {
			System.out.println("OrthologousEventPreviousInstanceFinder.isStableIdentifierAllowed: WARNING - instance is null!!");
			return false;
		}
		
		if (instance.getSchemClass().isa("Event")) {
			try {
				if (instance.getSchemClass().isValidAttribute("evidenceType")) {
					GKInstance evidenceType = (GKInstance)instance.getAttributeValue("evidenceType");
					if (evidenceType!=null) {
						List names = evidenceType.getAttributeValuesList("name");
						for (Iterator it = names.iterator(); it.hasNext();) {
							String name = (String)it.next();

							if (name.equals("inferred by electronic annotation") || name.equals("IEA"))
								return true;
						}
					}
				}
			} catch (Exception e) {
				System.out.println("OrthologousEventPreviousInstanceFinder.isStableIdentifierAllowed: problem extracting evidence type from instance");
				e.printStackTrace(System.err);
			}
		}

		return false;
	}
	
	public GKInstance getPreviousInstance() {
		GKInstance previousInstance = _getPreviousInstance(previousDba, currentInstance);
		
		if (deleteBackToSource && previousInstance != null)
			deleteStableIdsBackToSource(previousInstance);
		
		return previousInstance;				
	}
	
	public GKInstance _getPreviousInstance(MySQLAdaptor previousDba, GKInstance instance) {
		if (previousDba==null)
			return null;
		if (instance==null)
			return null;
		
		// Look to see if there is stable ID info in the previous
		// release for the given instance (use stable ID as key).
		GKInstance previousInstance = null;
		
		if (deleteBackToSource) {
			
		}
		if (previousInstance != null)
			return previousInstance;
		
		try {
			Collection referers = instance.getReferers("orthologousEvent");
			if (!referers.iterator().hasNext()) {
				return null;
			}
			// Gets the instance for which currentInstance is an
			// orthologousEvent. This is most probably the Event
			// from which currentInstance was derived, by orthology
			// prediction, most likely with species homo sapiens.
			// This is wimpy, we should get all referers
			GKInstance currentReferer = (GKInstance)referers.iterator().next();
			
			GKInstance species = (GKInstance)instance.getAttributeValue("species");
			// Gets first, and presumably most definitive, name for species.
			String speciesName = (String)species.getAttributeValue("name");
			
			// Get referer from previous release using stable
			// ID as a key
			GKInstance currentRefererStableIdentifier = (GKInstance)currentReferer.getAttributeValue("stableIdentifier");
			String currentRefererIdentifier = (String)currentRefererStableIdentifier.getAttributeValue("identifier");
			Collection previousRefererStableIdentifiers = previousDba.fetchInstanceByAttribute("StableIdentifier", "identifier", "=", currentRefererIdentifier);
			GKInstance previousRefererStableIdentifier = null;
			GKInstance previousReferer = null;
			if (previousRefererStableIdentifiers.iterator().hasNext()) {
				previousRefererStableIdentifier = (GKInstance)previousRefererStableIdentifiers.iterator().next();
				Collection previousRefererStableIdentifierReferers = previousRefererStableIdentifier.getReferers("stableIdentifier");
				if (previousRefererStableIdentifierReferers.iterator().hasNext())
					previousReferer = (GKInstance)previousRefererStableIdentifierReferers.iterator().next();
			}
			
			if (previousReferer==null)
				return null;
			
			if (!previousReferer.getSchemClass().isa("Event")) {
				String previousRefererClass = previousReferer.getSchemClass().getName();
				System.err.println("OrthologousEventPreviousInstanceFinder.getPreviousIdentifier: previous referer is not of class Event!  class=" + previousRefererClass);
				return null;
			}
			List orthologousEvents = previousReferer.getAttributeValuesList("orthologousEvent");
			GKInstance orthologousEvent;
			GKInstance orthologousSpecies;
			String orthologousSpeciesName;
			
			// Looks at all the events in other species for the
			// previous release that were predicted by orthology
			// from previousReferer.  If one of them has the
			// same species as currentInstance, assumes that
			// currentInstance and orthologousEvent event are
			// actually the same event in current and previous
			// release respectively.
			for (Iterator it = orthologousEvents.iterator(); it.hasNext();) {
				orthologousEvent = (GKInstance)it.next();
				orthologousSpecies = (GKInstance)orthologousEvent.getAttributeValue("species");
				orthologousSpeciesName = (String)orthologousSpecies.getAttributeValue("name");
				// TODO: species comparison relies on name remaining
				// unchanged from one release to the next, which may
				// not be realistic.
				if (orthologousSpeciesName.equals(speciesName)) {
					previousInstance = orthologousEvent;
					break;
				}
			}

			if (previousInstance != null) {
				// If the previous instance not an event, assume that the
				// current instance is being encountered for the first time.
				if (!previousInstance.getSchemClass().isa("Event"))
					previousInstance = null;
			}
		} catch (Exception e) {
			System.err.println("OrthologousEventPreviousInstanceFinder.getPreviousIdentifier: problem fetching instances");
			e.printStackTrace(System.err);
		}
		
		return previousInstance;				
	}
	
	private void deleteStableIdsBackToSource(GKInstance instance) {
		List<List<Object>> releaseNumsAndInstances = getReleaseNumsAndInstances(instance);
		for (int i=0; i<releaseNumsAndInstances.size(); i++) {
			String releaseNum = (String) releaseNumsAndInstances.get(i).get(0);
			GKInstance event = (GKInstance) releaseNumsAndInstances.get(i).get(1);
			if (event == null)
				break;

			deleteStableIdentifierInstancesAssociatedWithAnEventAndRelease(event, releaseNum);
		}
	}
	
	private void deleteStableIdentifierInstancesAssociatedWithAnEventAndRelease(GKInstance event, String releaseNum) {
		try {
			GKInstance stableIdentifier = (GKInstance) event.getAttributeValue("stableIdentifier");
			if (stableIdentifier == null) {
				System.err.println("OrthologousEventPreviousInstanceFinder.getPreviousIdentifier: WARNING - stableIdentifier == null for releaseNum: " + releaseNum);
				return;
			}
			String identifier = (String) stableIdentifier.getAttributeValue("identifier");
			Long dbId = event.getDBID();
			
			deleteStableIdentifierVersionForRelease(dbId, identifier, releaseNum);
		} catch (Exception e) {
			System.err.println("OrthologousEventPreviousInstanceFinder.deleteStableIdsBackToSource: problem fetching instances");
			e.printStackTrace(System.err);
		}		
	}
	
	public void deleteStableIdentifierVersionForRelease(Long dbId, String identifier, String releaseNum) {
		GKInstance stableIdentifier = identifierDatabase.getStableIdentifierInstance(identifier, false);
		
		try {
			List<GKInstance> stableIdentifierVersions = stableIdentifier.getAttributeValuesList("stableIdentifierVersion");
			for (GKInstance existingStableIdentifierVersion: stableIdentifierVersions) {
				List<GKInstance> releaseIds = existingStableIdentifierVersion.getAttributeValuesList("releaseIds");
				for (GKInstance releaseId: releaseIds) {
					Integer existingDbId = (Integer)releaseId.getAttributeValue("instanceDB_ID");
					GKInstance release = (GKInstance)releaseId.getAttributeValue(identifierDatabase.getReleaseColumn());
					String existingReleaseNum = (String)release.getAttributeValue("num");
					String existingProjectName = (String)((GKInstance)release.getAttributeValue("project")).getAttributeValue("name");
					if (existingDbId.intValue() == dbId.intValue() && existingReleaseNum.equals(releaseNum) && existingProjectName.equals(existingProjectName)) {
						System.err.println("IdentifierDatabase.getStableIdentifierVersion: deleting releaseId for dbId=" + dbId + ", identifier=" + identifier + ", releaseNum=" + releaseNum);
						if (!test)
							identifierDatabase.deleteInstance(releaseId);
					}
				}
				
				// Check to see if there are any release IDs left for this stableIdentifierVersion,
				// and if not, get rid of it.
				releaseIds = existingStableIdentifierVersion.getAttributeValuesList("releaseIds");
				if (releaseIds == null || releaseIds.size() == 0) {
					System.err.println("IdentifierDatabase.getStableIdentifierVersion: deleting existingStableIdentifierVersion for identifier=" + identifier + ", releaseNum=" + releaseNum);
					if (!test)
						identifierDatabase.deleteInstance(existingStableIdentifierVersion);
				}
			}
			
			// Check to see if there are any stable ID versions left for this stableIdentifier,
			// and if not, get rid of it.
			stableIdentifierVersions = stableIdentifier.getAttributeValuesList("stableIdentifierVersion");
			if (stableIdentifierVersions == null || stableIdentifierVersions.size() == 0) {
				System.err.println("IdentifierDatabase.getStableIdentifierVersion: deleting stableIdentifier for identifier=" + identifier);
				if (!test)
					identifierDatabase.deleteInstance(stableIdentifier);
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getStableIdentifierVersion: WARNING - problem getting stuff from stable identifier database");
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Gets a list of release number/event pairs, representing a "history" of the supplied
	 * instance from the current release back through to the first known release for
	 * the current project.  Ordered reverse historically, with element 0 corresponding
	 * to the most recent release, and subsequent elements going ever further into the
	 * past.  Event value will be null in releases containing no equivalent instance.
	 * 
	 * @param instance
	 * @return
	 */
	private List<List<Object>> getReleaseNumsAndInstances(GKInstance instance) {
		List<GKInstance> validReleases = getValidReleaseList();
//		Collections.reverse(validReleases); // TODO: do we need this?
		GKInstance event = instance;
		List<List<Object>> releaseNumsAndInstances = new ArrayList<List<Object>>();
		int validReleaseCount = validReleases.size();
		int i1;
		for (int i=0; i<validReleaseCount; i++) {
			GKInstance release = validReleases.get(i);
			try {
				String releaseNum = (String)release.getAttributeValue("num");
				
				List<Object> releaseNumAndInstance = new ArrayList<Object>();
				releaseNumAndInstance.add(releaseNum);
				releaseNumAndInstance.add(event);
				releaseNumsAndInstances.add(releaseNumAndInstance);
				
				// Once we have hit a null event, assume that all future values will
				// also be null.
				if (event == null) {
					if (i > 0) {
						List<Object> lastReleaseNumAndInstance = releaseNumsAndInstances.get(i - 1);
						lastReleaseNumAndInstance.set(1, null);
					}
					continue;
				}
				
				i1 = i + 1;
				if (i1 < validReleaseCount) {
					release = validReleases.get(i1);
					releaseNum = (String)release.getAttributeValue("num");
					MySQLAdaptor dba = null;
					try {
						dba = identifierDatabase.getReleaseDbaFromReleaseNum(releaseNum, projectName);
					} catch (Exception e1) {
						System.err.println("OrthologousEventPreviousInstanceFinder.getPreviousInstances: could not get a database adaptor for release " + releaseNum);
					}
					event = _getPreviousInstance(dba, event); // value may be null, that's OK.
				} else
					event = null;
			} catch (Exception e) {
				System.err.println("OrthologousEventPreviousInstanceFinder.getPreviousInstances: problem fetching instances");
				e.printStackTrace(System.err);
			}		
		}
		
		return releaseNumsAndInstances;
	}

	private List<GKInstance> getValidReleaseList() {
		int currentReleaseNumInt = (-1);
		try {
			currentReleaseNumInt = (new Integer(currentReleaseNum)).intValue();
		} catch (NumberFormatException e) {
		}
		List<GKInstance> validReleases = null;
		if (currentReleaseNumInt >= 0)
			validReleases = ReleaseList.createValidReleaseList(projectName, currentReleaseNumInt);
		
		return validReleases;
	}

	/**
	 * Depending on the implementation of the previous instance finder,
	 * different measures of identicality may be needed.
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
		boolean identical = false;
		try {
			identical = areReasonablyIdentical(instance1, instance2);
		} catch (Exception e) {
			System.err.println("OrthologousEventPreviousInstanceFinder.getPreviousIdentifier: WARNING - problem when comparing instances");
			e.printStackTrace(System.err);
		}
		
		return identical;
	}
	
	/**
	 * Returns false if the two instances have different schema classes.  Otherwise,
	 * returns true if the specified instances have equal number of values in all
	 * of their type ALL defining attributes or if there are no type ALL defining
	 * attributes.
	 * @param instance1
	 * @param instance2
	 * @return
	 * @throws Exception
	 */
	// TODO: this method was copied from MySQLAdaptor, where is is private.  It
	// would be good to have just one, public, method for approximate comparison
	// of instances.
	private boolean areReasonablyIdentical(GKInstance instance1, GKInstance instance2) throws Exception {
		GKSchemaClass class1 = (GKSchemaClass) instance1.getSchemClass();
		GKSchemaClass class2 = (GKSchemaClass) instance2.getSchemClass();
		if (!(class2.getName().equals(class1.getName())))
			return false;
		Collection<SchemaAttribute> attColl = class1.getDefiningAttributes(SchemaAttribute.ALL_DEFINING);
		if (attColl != null) {
			for (SchemaAttribute att: attColl) {
				String attName = att.getName();
				List<Object> vals1 = instance1.getAttributeValuesList(attName);
				int count1 = 0;
				if (vals1 != null)
					count1 = vals1.size();
				List<Object> vals2 = instance2.getAttributeValuesList(attName);
				int count2 = 0;
				if (vals2 != null)
					count2 = vals2.size();
				if (count1 != count2)
					return false;
			}
		}
		return true;
	}
}