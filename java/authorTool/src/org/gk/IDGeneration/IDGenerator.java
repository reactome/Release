/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;

/** 
 *  Generates a list of stable IDs, given a list of
 *  Instance classes for ID generation, the database 
 *  adaptor for the previous release and the database
 *  adaptor for the current release.  Method:
 *  
 *  generateIDs
 *  
 *  Its also possible to run a set of standard tests on the
 *  stable IDs generated in this way.  Test results are
 *  stored internally.  Methods:
 *  
 *  testIDs
 *  getTests
 *  
 * @author croft
 */
public class IDGenerator {
	private IDGeneratorTests tests; // list of tests performed
	private IdentifierDatabase identifierDatabase;
	private MySQLAdaptor previousDba = null;
	private MySQLAdaptor currentDba = null;
	private MySQLAdaptor gk_centraldba = null;
	private PreviousInstanceFinder previousInstanceFinder;
	private String currentReleaseNum;
	private String currentProjectName;
	private List<Long> limitingCurrentDbIds = null;
	
	/**
	 * Requires as arguments the database adaptors for the main
	 * databases that will be dealt with, namely, the previous
	 * and current releases, plus the development database (usually
	 * called gk_central or something similar).  If previousDba
	 * is null, it is assumed that currentDba is the very first
	 * database in a series of releases, and brand new stable IDs
	 * will be generated.  If a previousDba is also specified,
	 * then stable IDs will be carried over from the previous
	 * to the current release where possible.  If gk_centraldba
	 * is specified, checks will also be made for instances
	 * marked as "_doRelease".
	 * 
	 * @param previousDba - can be null
	 * @param currentDba - must be defined
	 * @param gk_centraldba - can be null
	 */
	public IDGenerator(MySQLAdaptor previousDba, MySQLAdaptor currentDba, MySQLAdaptor gk_centraldba, IdentifierDatabase identifierDatabase) {
		this.previousDba = previousDba;
		this.currentDba = currentDba;
		this.gk_centraldba = gk_centraldba;
		this.identifierDatabase = identifierDatabase;
		
		previousInstanceFinder = new DbIdPreviousInstanceFinder(currentReleaseNum, previousDba, true, identifierDatabase);
	}
	
	public IDGenerator(MySQLAdaptor previousDba, MySQLAdaptor currentDba, MySQLAdaptor gk_centraldba, IdentifierDatabase identifierDatabase, PreviousInstanceFinder previousInstanceFinder) {
		this.previousDba = previousDba;
		this.currentDba = currentDba;
		this.gk_centraldba = gk_centraldba;
		this.identifierDatabase = identifierDatabase;
		this.previousInstanceFinder = previousInstanceFinder;
		this.previousInstanceFinder = previousInstanceFinder;
	}
	
	public IDGenerator(MySQLAdaptor previousDba, MySQLAdaptor currentDba, MySQLAdaptor gk_centraldba, IdentifierDatabase identifierDatabase, PreviousInstanceFinder previousInstanceFinder, List<Long> limitingCurrentDbIds) {
		this.previousDba = previousDba;
		this.currentDba = currentDba;
		this.gk_centraldba = gk_centraldba;
		this.identifierDatabase = identifierDatabase;
		this.previousInstanceFinder = previousInstanceFinder;
		this.limitingCurrentDbIds = limitingCurrentDbIds;
	}
	
	/**
	 * Takes the list of schema classes and generates stable IDs for
	 * all instances in these classes.  Reactome database and identifier
	 * database are updated to contain the new stable IDs.
	 * 
	 * @param classes
	 */
	public void generateIDs(List classes) {
		generateIDs(classes, false, null);
	}
	
	public void testForRepeatedStableIdsInCurrentRelease() {
		if (currentDba == null) {
			System.err.println("IDGenerator.generateIDs: WARNING - currentDba == null");
			return;
		}
		
		boolean duplicates = false;
		Map<String,List<Long>> stableIdentifierHash = new HashMap<String,List<Long>>();
		int stableIdentifierCount = 0;
		try {
			Collection<GKInstance> stableIdentifiers = currentDba.fetchInstancesByClass("StableIdentifier");
			for (GKInstance stableIdentifier: stableIdentifiers) {
				String stableIdentifierString = (String)stableIdentifier.getAttributeValue("identifier");
				List<Long> dbIds = stableIdentifierHash.get(stableIdentifierString);
				if (dbIds == null)
					dbIds = new ArrayList<Long>();
				else
					duplicates = true;
				dbIds.add(stableIdentifier.getDBID());
				stableIdentifierHash.put(stableIdentifierString, dbIds);
				stableIdentifierCount++;
			}
		} catch (Exception e) {
			System.err.println("IDGenerator.testForRepeatedStableIdsInCurrentRelease: WARNING - problem fetching instances");
			e.printStackTrace(System.err);
		}
		
		if (duplicates) {
			List<String> keys = new ArrayList<String>(stableIdentifierHash.keySet());
			Collections.sort(keys);
			System.err.println("IDGenerator.testForRepeatedStableIdsInCurrentRelease: out of " + stableIdentifierCount + ", the following stable IDs in the current release database have multiple matching DB_IDs:");
			for (String key: keys) {
				System.err.print(key + ": [");
				for (Long dbId: stableIdentifierHash.get(key))
					System.err.print(dbId + ", ");
				System.err.println("]");
			}
		} else
			System.err.println("IDGenerator.testForRepeatedStableIdsInCurrentRelease: good news - out of " + stableIdentifierCount + ", no stable IDs in the current release database have multiple matching DB_IDs");
	}

	public void testForRepeatedStableIdsInIdentifierDatabase() {
		if (identifierDatabase == null) {
			System.err.println("IDGenerator.generateIDs: WARNING - identifierDatabase == null");
			return;
		}
		
		boolean duplicates = false;
		Map<String,List<Long>> stableIdentifierHash = new HashMap<String,List<Long>>();
		int stableIdentifierCount = 0;
		try {
			identifierDatabase.getDba().refresh(); // clear cache to avoid heap space problems
			Collection<GKInstance> stableIdentifiers = identifierDatabase.getDba().fetchInstancesByClass("StableIdentifier");
			for (GKInstance stableIdentifier: stableIdentifiers) {
				String stableIdentifierString = (String)stableIdentifier.getAttributeValue("identifierString");
				List<Long> dbIds = stableIdentifierHash.get(stableIdentifierString);
				if (dbIds == null)
					dbIds = new ArrayList<Long>();
				else
					duplicates = true;
				dbIds.add(stableIdentifier.getDBID());
				stableIdentifierHash.put(stableIdentifierString, dbIds);
				stableIdentifierCount++;
			}
		} catch (Exception e) {
			System.err.println("IDGenerator.testForRepeatedStableIdsInIdentifierDatabase: WARNING - problem fetching instances");
			e.printStackTrace(System.err);
		}
		
		if (duplicates) {
			List<String> keys = new ArrayList<String>(stableIdentifierHash.keySet());
			Collections.sort(keys);
			System.err.println("IDGenerator.testForRepeatedStableIdsInIdentifierDatabase: out of " + stableIdentifierCount + ", the following stable IDs in the stable identifier database are duplicates:");
			for (String key: keys) {
				System.err.print(key + ": [");
				for (Long dbId: stableIdentifierHash.get(key))
					System.err.print(dbId + ", ");
				System.err.println("]");
			}
		} else
			System.err.println("IDGenerator.testForRepeatedStableIdsInIdentifierDatabase: good news - out of " + stableIdentifierCount + ", no stable IDs in the stable identifier database are duplicates");
	}

	/**
	 * Takes the list of schema classes and generates stable IDs for
	 * all instances in these classes.  Reactome database and identifier
	 * database are updated to contain the new stable IDs.
	 * 
	 * @param classes
	 */
	public void generateIDs(List classes, List schemaChangeIgnoredAttributes) {
		generateIDs(classes, false, schemaChangeIgnoredAttributes);
	}
	
	/**
	 * Takes the list of schema classes and generates stable IDs for
	 * all instances in these classes.  Reactome database and identifier
	 * database are updated to contain the new stable IDs.
	 * 
	 * @param classes
	 * @param testMode - if true, nothing will be inserted into database
	 */
	public void generateIDs(List classes, boolean testMode, List schemaChangeIgnoredAttributes) {
		currentReleaseNum = identifierDatabase.getReleaseNumFromReleaseDba(currentDba);
		currentProjectName = identifierDatabase.getProjectNameFromReleaseDba(currentDba);
		if (currentReleaseNum==null || currentReleaseNum.equals("")) {
			System.err.println("IDGenerator.generateIDs: WARNING - no release specified, cannot proceed.  currentReleaseNum=" + currentReleaseNum + ", currentProjectName=" + currentProjectName);
			return;
		}

		// Make sure that the most recent stable ID has a sane value,
		// to avoid possible stable ID duplications.
		IdentifierDatabase.sanitizeMostRecentStableIdentifierFromState(previousDba);
		
		// Stash the largest known DB_ID from the current release database
		if (!testMode) {
			long maxDbId = currentDba.fetchMaxDbId();
			if (maxDbId>=0)
				identifierDatabase.insertMaxDbId(currentReleaseNum, currentProjectName, maxDbId);
		}
		
		if (classes==null)
			return;
		
		tests = new IDGeneratorTests();
		tests.setCurrentDba(currentDba);

		List consolidatedClasses = consolidateClassList(classes);
		
		// Go through all instances of each schema class .
		for (Iterator it = consolidatedClasses.iterator(); it.hasNext();) {
			SchemaClass currentSchemaClass = (SchemaClass)it.next();
			
			System.err.println("IDGenerator.generateIDs: currentSchemaClass=" + currentSchemaClass.getName());
				
			generateIDs(currentSchemaClass, testMode, schemaChangeIgnoredAttributes);
		}
		
		if (!testMode) {
			identifierDatabase.insertTests(currentReleaseNum, currentProjectName, tests);
		}
		
		System.err.println("IDGenerator.generateIDs: finished.");
	}
	
	/**
	 * Takes a schema class and generates stable IDs for
	 * all instances in this class.
	 * 
	 * @param schemaClass
	 * @param testMode - if true, nothing will be inserted into database
	 */
	public void generateIDs(SchemaClass currentSchemaClass, boolean testMode, List schemaChangeIgnoredAttributes) {
		if (currentDba==null) {
			System.err.println("IDGenerator.generateIDs: currentDba==null!");
			return;
		}

		SchemaClass previousSchemaClass = null;
		if (previousDba!=null) {
			previousSchemaClass = previousDba.getSchema().getClassByName(currentSchemaClass.getName());
		}
		InstanceBiologicalMeaning instanceBiologicalMeaning = new InstanceBiologicalMeaning(currentSchemaClass, previousSchemaClass);
		try {
			Collection instances = currentDba.fetchInstancesByClass(currentSchemaClass);
			
			for (Iterator it = instances.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance)it.next();
				Long currentDbId = instance.getDBID();
				if (limitingCurrentDbIds != null && !limitingCurrentDbIds.contains(currentDbId))
					continue;
				System.err.print("IDGenerator.generateIDs: instance=" + currentDbId.toString() + ", " + instance.getDisplayName());
				if (instance.getSchemClass().isValidAttribute("species")) {
					if (instance.getAttributeValue("species") == null)
						System.err.println(" [null]");
					else
						System.err.println(" [" + ((GKInstance)instance.getAttributeValue("species")).getDisplayName() + "]");
				} else
					System.err.println("");
				generateIDs(instanceBiologicalMeaning, instance, testMode, schemaChangeIgnoredAttributes);
			}
		} catch (Exception e) {
			System.err.println("IDGenerator.generateIDs: problem fetching current instances for schema class " + currentSchemaClass.getName());
			e.printStackTrace(System.err);
		}
		
		if (previousDba!=null && gk_centraldba!=null) {
			try {
				Collection instances = previousDba.fetchInstancesByClass(currentSchemaClass);
				
				for (Iterator it = instances.iterator(); it.hasNext();) {
					GKInstance instance = (GKInstance)it.next();
					generateDoNotReleaseIDs(instance, testMode);
					generateDeletionComments(instance, testMode);
				}
			} catch (Exception e) {
				System.err.println("IDGenerator.generateIDs: problem fetching previous instances for schema class " + currentSchemaClass.getName());
				e.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * 
	 * Look at things that have been deleted since the last release
	 * and make a note if they still exist in gk_central but have
	 * been marked as "do not release".
	 */
	public void generateDoNotReleaseIDs(GKInstance previousInstance, boolean testMode) {
		try {
			Long dbId = previousInstance.getDBID();
			
			GKInstance currentInstance = currentDba.fetchInstance(dbId);
			// Instance exists in current release too, i.e. has not
			// been delteted/suppressed, so we don't need to continue.
			if (currentInstance!=null)
				return;
			
			GKInstance gk_centralInstance = gk_centraldba.fetchInstance(dbId);
			// Instance does not exist in gk_central, i.e. has
			// been delteted, so we don't need to continue.
			if (gk_centralInstance==null)
				return;
			
//			// If _doRelease has been set, don't proceed any further.
//			Boolean doRelease = null;
//			try {
//				doRelease = (Boolean)gk_centralInstance.getAttributeValue("_doRelease");
//			} catch (InvalidAttributeException e) {
//				System.err.println("IDGenerator.generateDoNotReleaseIDs: cannot find a _doRelease attribute for dbId=" + dbId);
//			}
//			if (doRelease!=null && doRelease.booleanValue()) {
//				System.err.println("IDGenerator.generateDoNotReleaseIDs: dbId=" + dbId + " is present in gk_central and is marked for release, but is not actually present in the current release!  This instance will be falsely categorized as deleted by the web GUI.");
//				return;
//			}
			
			// Get StableIdentifier instance from release database
			GKInstance stableIdentifier = (GKInstance)previousInstance.getAttributeValue("stableIdentifier");
			if (stableIdentifier==null)
				return;
			
			String identifier = (String)stableIdentifier.getAttributeValue("identifier");
			
			// Get StableIdentifier instance from identifier database
			stableIdentifier = IdentifierDatabase.getStableIdentifierInstance(identifier, false);
			if (stableIdentifier==null)
				return;
			
			stableIdentifier.setAttributeValue("doNotRelease", currentReleaseNum);
			
			if (!testMode) {
				MySQLAdaptor identifierDatabaseDba = IdentifierDatabase.getDba();
				identifierDatabaseDba.updateInstanceAttribute(stableIdentifier, "doNotRelease");
			}
		} catch (Exception e) {
			System.err.println("IDGenerator.generateDoNotReleaseIDs: problem fetching instances");
			e.printStackTrace(System.err);
		}
		
	}
	
	/**
	 * 
	 * Look at things that have been deleted since the last release
	 * and look to see if they have a corresponding "_Deleted" instance.
	 * If so, copy the curator comment over into the identifier database.
	 */
	public void generateDeletionComments(GKInstance previousInstance, boolean testMode) {
		try {
			Long dbId = previousInstance.getDBID();
			
			GKInstance currentInstance = currentDba.fetchInstance(dbId);
			// Instance exists in current release too, i.e. has not
			// been delteted/suppressed, so we don't need to continue.
			if (currentInstance!=null)
				return;
			
			// Get StableIdentifier instance from release database
			GKInstance stableIdentifier = (GKInstance)previousInstance.getAttributeValue("stableIdentifier");
			if (stableIdentifier==null)
				return;
			
			// Get _Deleted instance corresponding to dbId.  These are
			// only available from gk_central.
			Collection deleteds = gk_centraldba.fetchInstanceByAttribute("_Deleted", "deletedInstanceDB_ID", "=", dbId);
			if (deleteds==null || deleteds.size()==0)
				return;
			
			GKInstance deleted = (GKInstance)deleteds.toArray()[0];
			String curatorComment = (String)deleted.getAttributeValue("curatorComment");
			GKInstance reason = (GKInstance)deleted.getAttributeValue("reason");
			List replacementInstances = deleted.getAttributeValuesList("replacementInstances");
			if ((curatorComment==null || curatorComment.equals("")) &&
				reason==null &&
				(replacementInstances==null || replacementInstances.size()==0))
				return;
			String reasonText = "";
			if (reason!=null)
				reasonText = (String)reason.getAttributeValue("name");
			
			// Create a new Deleted instance for the identifier database and
			// transfer appropriate information to it
			GKInstance idbDeleted = identifierDatabase.createDeleted();
			idbDeleted.setAttributeValue("comment", curatorComment);
			idbDeleted.setAttributeValue("reason", reasonText);
			if (replacementInstances!=null) {
				for (Iterator it = replacementInstances.iterator(); it.hasNext();) {
					GKInstance replacementInstance = (GKInstance)it.next();
					GKInstance replacement = identifierDatabase.createReplacement();
					Long replacementDB_ID = replacementInstance.getDBID();
					int replacementDB_IDInt = replacementDB_ID.intValue();
					if (replacementDB_IDInt>=0)
						replacement.setAttributeValue("replacementDB_ID", new Integer(replacementDB_IDInt));
					GKInstance replacementStableIdentifier = (GKInstance)replacementInstance.getAttributeValue("stableIdentifier");
					if (replacementStableIdentifier!=null) {
						String replacementIdentifierString = (String)replacementStableIdentifier.getAttributeValue("identifier");
						replacement.setAttributeValue("replacementIdentfierString", replacementIdentifierString);
					}
					idbDeleted.addAttributeValue("replacement", replacement);
				}
			}
			idbDeleted.setAttributeValue("releaseNum", currentReleaseNum);
			
			String identifier = (String)stableIdentifier.getAttributeValue("identifier");
			
			// Get StableIdentifier instance from identifier database
			stableIdentifier = IdentifierDatabase.getStableIdentifierInstance(identifier, false);
			if (stableIdentifier==null)
				return;
			
			stableIdentifier.setAttributeValue("deleted", idbDeleted);
			
			if (!testMode) {
				MySQLAdaptor identifierDatabaseDba = IdentifierDatabase.getDba();
				identifierDatabaseDba.updateInstanceAttribute(stableIdentifier, "deleted");
			}
		} catch (Exception e) {
			System.err.println("IDGenerator.generateDoNotReleaseIDs: problem fetching instances");
			e.printStackTrace(System.err);
		}
		
	}
	
	/**
	 * Takes an instance and generates a stable IDs for
	 * it.
	 * 
	 * @param schemaClass
	 * @param testMode - if true, nothing will be inserted into database
	 */
	public void generateIDs(InstanceBiologicalMeaning instanceBiologicalMeaning, GKInstance currentInstance, boolean testMode, List schemaChangeIgnoredAttributes) {
			try {
				// If the current release already has a stable identifier,
				// log the fact.
				if (currentInstance.getAttributeValue("stableIdentifier")!=null) {
					tests.addInstance(IDGeneratorTest.ST_IDS_EXIST, currentInstance);
				}
			} catch (Exception e1) {
				System.err.println("IDGenerator.generateIDs: WARNING - problem getting stable ID for current instance");
				e1.printStackTrace(System.err);
			}
			
			// Get the previous instance (if there is one) and determine
			// its stable ID.
			previousInstanceFinder.setCurrentInstance(currentInstance);
			if (!previousInstanceFinder.isStableIdentifierAllowed())
				return;
			previousInstanceFinder.setPreviousReleaseNum(identifierDatabase.getReleaseNumFromReleaseDba(previousDba));
			previousInstanceFinder.setProjectName(identifierDatabase.getProjectNameFromReleaseDba(previousDba));
			GKInstance previousInstance = previousInstanceFinder.getPreviousInstance();
			if (previousInstanceFinder.isIDsFromUnspecifiedReleases()) {
				System.err.println("IDGenerator.generateIDs: stable ID obtained from previous release");
				tests.addInstance(IDGeneratorTest.ID_IN_OLD_RELEASE, currentInstance);
			}
			GKInstance previousStableID = null;
			Long previousDbId = null;
			try {
				if (previousInstance != null) {
					previousStableID = (GKInstance)previousInstance.getAttributeValue("stableIdentifier");
					previousDbId = previousInstance.getDBID();
				}
			} catch (Exception e1) {
				System.err.println("IDGenerator.generateIDs: WARNING - problem getting stable ID for previous instance");
				e1.printStackTrace(System.err);
			}
			
			// If the previous version of this instance already
			// had a stable ID, pass it over to the current release,
			// otherwise generate a new stable ID.
			String stableIdentifierString = null;
			String stableVersionString = null;
			GKInstance identifierDbStableIdentifier;
			tests.addInstance(IDGeneratorTest.ST_IDS_INSERTED, currentInstance);
			Long currentDbId = currentInstance.getDBID();
			try {
				if (previousStableID==null) {
					// Current instance has no previous stable ID - maybe the
					// instance is new, or this is the first time that stable
					// IDs have been generated, or perhaps the DB_ID has been
					// changed.
					tests.addInstance(IDGeneratorTest.NO_PREVIOUS_DB_ID, currentInstance);
					
					// Get a never-before-used stable ID - if the identifier database
					// is virgin, create a fresh one.  identifierDbStableIdentifier
					// should NEVER be null.  Doing this in test mode is maybe a bit
					// wasteful, but it doesn't actually do any harm, it just means
					// that gaps will appear in the ranges of stable identifiers
					// stored in the databases.
					if (!identifierDatabase.incrementMostRecentStableIdentifier()) {
						tests.addInstance(IDGeneratorTest.COULD_NOT_INC_ST_ID, currentInstance);
						System.err.println("IDGenerator.generateIDs: cannot find stable identifier " + stableIdentifierString + " in identifier database!!");
//					return;
					}
					identifierDbStableIdentifier = IdentifierDatabase.getMostRecentStableIdentifierFromState();
					
					// retrieve stable identifier and version for insertion
					// into the Reactome database
					stableIdentifierString = (String)identifierDbStableIdentifier.getAttributeValue("identifierString");
					stableVersionString ="1"; // initial version number
					
					// Create the hierarchy of classes below StableIdentifier
					// in the identifier database
					identifierDatabase.insertNewVersion(identifierDbStableIdentifier, stableVersionString, currentReleaseNum, currentProjectName, currentDbId, testMode);
				} else {
					String previousReleaseNum = previousInstanceFinder.getPreviousReleaseNum();
					String previousProjectName = previousInstanceFinder.getProjectName();
					
					// This was the old way of doing things: get stable ID and version from
					// previous release.  If something goes wrong when trying to get the values
					// from the identifier database, then these will serve as fallback values.
					String previousStableIdentifierString = (String)previousStableID.getAttributeValue("identifier");
					String previousStableVersionString = (String)previousStableID.getAttributeValue("identifierVersion");
					stableIdentifierString = previousStableIdentifierString;
					stableVersionString = previousStableVersionString;
					
					Long releaseDbId = currentDbId;
					if (previousDbId != null && previousInstanceFinder.getClass().getName().indexOf("rtholog") >= 0)
						releaseDbId = previousDbId;
					Set stableIdentifiers = IdentifierDatabase.getStableIdentifiersFromReleaseDB_ID(previousReleaseNum, previousProjectName, releaseDbId);
					if (stableIdentifiers.size()!=1) {
						// Catch potential problems with stable identifier and version
						// information. "Correct" by using stable ID information from
						// previous release database.  Can potentially lead to the propagation
						// of inconsistencies if stable ID generation is done more
						// than once during a release.

						if (stableIdentifiers.size()<1) {
							System.err.println("IDGenerator.generateIDs: no stable ID set for previousReleaseNum=" + previousReleaseNum + ", previousProjectName=" + previousProjectName + ", current DB_ID=" + releaseDbId + "; something is very wrong!!");
						} else if (stableIdentifiers.size()>1) {
							System.err.println("IDGenerator.generateIDs: multiple stable IDs for current DB_ID=" + releaseDbId + "; it is likely that stable ID generation has been run twice during a release");
							tests.addInstance(IDGeneratorTest.MULTIPLE_STABLE_IDS_FOR_SINGLE_DB_ID, currentInstance);
						}
					} else {
						// This is the clean way to do things, if the stable ID database
						// is in a consistent state: look up the stable ID and the most
						// recent version for the DB_ID in the stable ID database.
						stableIdentifierString = (String)(((GKInstance)(stableIdentifiers.toArray()[0])).getAttributeValue("identifierString"));
						stableVersionString = IdentifierDatabase.getStableIdentifierVersionForReleaseString(stableIdentifierString, previousReleaseNum, previousProjectName);
					}
					
					// Sanity crosscheck
					if (!stableIdentifierString.equals(previousStableIdentifierString)) {
						System.err.println("IDGenerator.generateIDs: mismatch between stable ID extracted from identifier database (" + stableIdentifierString + ") and slice (" + previousStableIdentifierString + ") for current instance DB_ID=" + currentDbId);
						tests.addInstance(IDGeneratorTest.DISCREPANCY_BETWEEN_IDDB_AND_PREVRELEASE, currentInstance);
					}
					if (!stableVersionString.equals(previousStableVersionString)) {
						System.err.println("IDGenerator.generateIDs: mismatch between version extracted from identifier database (" + stableVersionString + ") and slice (" + previousStableVersionString + ") for current instance DB_ID=" + currentDbId);
					}
					
					// Increment the version number if any attributes have changed
					// from the previous to the current release.
					if (instanceBiologicalMeaning.isBiologicalMeaningChanged(previousInstance, currentInstance, schemaChangeIgnoredAttributes)) {
						int stableVersionNum = 0;
						try {
							stableVersionNum = (new Integer(stableVersionString)).intValue();
							instanceBiologicalMeaning.biologicalMeaningChanged(previousInstance, currentInstance);
							tests.addInstance(IDGeneratorTest.BIOLOGICAL_MEANING_CHANGED, currentInstance);
						} catch (NumberFormatException e) {
							// This shouldn't happen
							System.err.println("IDGenerator.generateIDs: inappropriate stableVersionString, setting stableVersionNum to 0");
							e.printStackTrace(System.err);
						}
						stableVersionNum++;
						stableVersionString = (new Integer(stableVersionNum)).toString();
						
						System.err.println("IDGenerator.generateIDs: for stableIdentifierString=" + stableIdentifierString + " new stableVersionString=" + stableVersionString);
					}
					
					// Get a StableIdentifier object corresponding to stableIdentifierString
					// from the identifier database, if one already exists.
					identifierDbStableIdentifier = IdentifierDatabase.getStableIdentifierInstance(stableIdentifierString, false);
					if (identifierDbStableIdentifier==null) {
						// Something a bit nasty has happened if we find a stable ID
						// in a release but not in the identifier database.  Log this
						// fact as a test result, and then 
						// create a new StableIdentifier instance in the identifier database to plug the gap.
						identifierDbStableIdentifier = IdentifierDatabase.getStableIdentifierInstance(stableIdentifierString, true);
						tests.addInstance(IDGeneratorTest.MISSING_STABLE_ID, currentInstance);

						System.err.println("IDGenerator.generateIDs: cannot find stable identifier " + stableIdentifierString + " in identifier database.");
					}
					if (identifierDbStableIdentifier==null) {
						System.err.println("IDGenerator.generateIDs: cannot find or create stable identifier " + stableIdentifierString + " in identifier database!!");
//					return;
					}

					// Update the hierarchy of classes below StableIdentifier
					// in the identifier database
					identifierDatabase.insertNewVersion(identifierDbStableIdentifier, stableVersionString, currentReleaseNum, currentProjectName, currentDbId, testMode, instanceBiologicalMeaning);
					instanceBiologicalMeaning.resetChangeRecords();
					
					// This stuff does some sanity testing.  Look at the
					// stable ID(s) in the *current* release to see if
					// they are the same as the one in the previous release.
					// If not, it strongly suggests that a discrepancy has
					// crept into the identifier database that needs correcting.
					// But note: this is only really useful for human instances,
					// because non-human instances have different DB_IDs from
					// one release to the next.
					if (previousInstanceFinder.getClass().getName().indexOf("rtholog") < 0) {
						Long previous_DB_ID = previousInstance.getDBID();
						String currentReleaseNum = identifierDatabase.getReleaseNumFromReleaseDba(currentDba);
						String currentProjectName = identifierDatabase.getProjectNameFromReleaseDba(currentDba);
						stableIdentifiers = IdentifierDatabase.getStableIdentifiersFromReleaseDB_ID(currentReleaseNum, currentProjectName, previousInstance.getDBID());
						if (stableIdentifiers.size()<1) {
							System.err.println("IDGenerator.generateIDs: testing, no stable ID set for DB_ID=" + previous_DB_ID + "; something may be wrong!!");
							if (testMode)
								System.err.println("...however, this problem might well simply be due to the fact that you are running in test mode, and therefore have not written any information about the current release into the identifier database");
						} else if (stableIdentifiers.size()>1) {
							System.err.println("IDGenerator.generateIDs: testing, multiple stable IDs for DB_ID=" + previous_DB_ID + "; it is likely that stable ID generation has been run twice during a release");
						} else {
							String currentIdentifierString = (String)(((GKInstance)(stableIdentifiers.toArray()[0])).getAttributeValue("identifierString"));
							if (!stableIdentifierString.equals(currentIdentifierString)) {
								System.err.println("IDGenerator.generateIDs: testing, for DB_ID=" + previous_DB_ID + ", release " + previousReleaseNum + " stable ID=" + stableIdentifierString + " but release " + currentReleaseNum + " stable ID=" + currentIdentifierString);
							}
						}
					}
				}
			} catch (Exception e) {
				System.err.println("IDGenerator.generateIDs: WARNING - serious problem while trying to determine stable ID and version, aborting!");
				e.printStackTrace(System.err);
				return;
			}

			String displayNameString = stableIdentifierString + "." + stableVersionString;

			try {
				// Take a look into the current release to see if a StableIdentifier
				// instance for identifier/identifierVersion already exists; if
				// so, set it to be "currentStableID".  Otherwise,
				// create the StableIdentifier instance to be inserted in
				// the current release and put stable ID and version number
				// into it.
				GKInstance currentStableID = null;
				Collection currentStableIDs = currentDba.fetchInstanceByAttribute("StableIdentifier", "identifier", "=", stableIdentifierString);
				if (currentStableIDs==null || currentStableIDs.size()==0) {
					currentStableID = new GKInstance();
					currentStableID.setSchemaClass(currentDba.getSchema().getClassByName("StableIdentifier"));
					currentStableID.setDbAdaptor(currentDba);
					currentStableID.setAttributeValue("identifier", stableIdentifierString);
					
					currentStableID.setAttributeValue("identifierVersion", stableVersionString);
					if (!testMode)
						currentDba.storeInstance(currentStableID);
				} else {
					// Since gk_central now contains stable IDs, they will often
					// already be present in the slice.  In this case, recycle
					// the stable IDs found, rather than creating new ones.
					currentStableID = (GKInstance)currentStableIDs.toArray()[0];
					
					currentStableID.setAttributeValue("identifierVersion", stableVersionString);
					if (!testMode)
						currentDba.updateInstanceAttribute(currentStableID, "identifierVersion");
				}
				
				// Update the display name, just in case the automatic update is not working
				currentStableID.setAttributeValue("_displayName", displayNameString);

				currentInstance.setAttributeValue("stableIdentifier", currentStableID);
			} catch (Exception e) {
				System.err.println("IDGenerator.generateIDs: WARNING - problem setting up current instance stable ID");
				e.printStackTrace(System.err);
			}

			// Set up DOI
			String doi = null;
			try {
				if (currentInstance.getSchemClass().isValidAttribute("doi")) {
					doi = (String)currentInstance.getAttributeValue("doi");
					if (doi!=null && !(doi.equals(""))) {
						if (doi.toLowerCase().equals("needs doi")) {
							doi = "10.3180/" + displayNameString;
							currentInstance.setAttributeValue("doi", doi);
						}
					} else
						doi = null; // Just in case curator has put an empty string in doi field
				}

				if (!testMode) {
					// Update the instance in the current release so that it
					// contains the appropriate stable ID info.
					currentDba.updateInstanceAttribute(currentInstance, "stableIdentifier");
					
					// Update the instance in the current release so that it
					// contains the appropriate DOI info.
					if (doi!=null)
						currentDba.updateInstanceAttribute(currentInstance, "doi");
				}
			} catch (Exception e) {
				System.err.println("IDGenerator.generateIDs: WARNING - problem setting up DOI");
				e.printStackTrace(System.err);
			}
			
			
			try {
				// Also put the stable ID into gk_central, if we have the appropriate
				// dba.
				if (gk_centraldba!=null) {
					GKInstance gk_centralInstance = gk_centraldba.fetchInstance(currentInstance.getDBID());
					if (gk_centralInstance==null) {
						System.err.println("IDGenerator.generateIDs: WARNING - no gk_central instance for DB_ID=" + currentInstance.getDBID());
					} else {
						// Do this additional check, because if currentInstance comes from a release
						// database and it has been generated automatically by orthology inference,
						// then it might (by chance) have a DB_ID that is the same as an unreleased
						// gk_central instance.
						
						// TODO: it might actually be quicker to do things the other way around - i.e.
						// first find the StableIdentifier object corresponding to stableIdentifierString,
						// and then checking for the existence of a referrer.  This would cover most
						// cases in the future, since most gk_central instances will have stable IDs.
						// The areReasonablyIdentical thing (slow) would only be necessary for cases where
						// there is not yet a StableIdentifier instance in gk_central.
						if (previousInstanceFinder.areInstancesIdentical(currentInstance, gk_centralInstance)) {
							// Create or get the StableIdentifier instance to be inserted in
							// gk_central and put stable ID and version number
							// into it
							GKInstance gkcentralStableID = null;
							Collection gkcentralStableIDs = gk_centraldba.fetchInstanceByAttribute("StableIdentifier", "identifier", "=", stableIdentifierString);
							if (gkcentralStableIDs==null || gkcentralStableIDs.size()==0) {
								gkcentralStableID = new GKInstance();
								gkcentralStableID.setSchemaClass(gk_centraldba.getSchema().getClassByName("StableIdentifier"));
								gkcentralStableID.setDbAdaptor(gk_centraldba);
								gkcentralStableID.setAttributeValue("identifier", stableIdentifierString);
								if (!testMode) {
									gk_centraldba.storeInstance(gkcentralStableID);
								}
							} else {
								gkcentralStableID = (GKInstance)gkcentralStableIDs.toArray()[0];
							}
							
							// Version needs to be explicitly updated even for existing StableIdentifier
							// objects, because it might have changed since the last release.
							gkcentralStableID.setAttributeValue("identifierVersion", stableVersionString);
							
							gk_centralInstance.setAttributeValue("stableIdentifier", gkcentralStableID);
							
							// Propagate new DOI back to gk_central
							if (doi!=null)
								gk_centralInstance.setAttributeValue("doi", doi);
							
							if (!testMode) {
								// Make sure the version gets updated.
								gk_centraldba.updateInstanceAttribute(gkcentralStableID, "identifierVersion");
								// Update the instance in the current release so that it
								// contains the appropriate stable ID info.
								gk_centraldba.updateInstanceAttribute(gk_centralInstance, "stableIdentifier");
								// Update the instance in the current release so that it
								// contains the appropriate DOI info.
								if (doi!=null)
									gk_centraldba.updateInstanceAttribute(gk_centralInstance, "doi");
							}
						} else {
							System.err.println("IDGenerator.generateIDs: WARNING - gk_central has no valid instance corresponding to DB_ID=" + currentInstance.getDBID());
						}
					}
				}
			} catch (Exception e) {
				System.err.println("IDGenerator.generateIDs: WARNING - problem inserting stuff into gk_central");
				e.printStackTrace(System.err);
			}
	}
	
	/**
	 * If the list of classes contains a class and its super class,
	 * only the super class will be retained.
	 * 
	 * Assumption: super classes always occur before classes in the
	 * list order.
	 * 
	 * @param classes
	 * @return
	 */
	private List consolidateClassList(List classes) {
		List consolidatedClasses = new ArrayList();
		
		if (classes==null)
			return null;
		
		boolean topLevel;
		for (Iterator it = classes.iterator(); it.hasNext();) {
			SchemaClass schemaClass = (SchemaClass)it.next();
			Collection superClasses = schemaClass.getSuperClasses();
			topLevel = true; // assume schemaClass is top-level
			for (Iterator it1 = consolidatedClasses.iterator(); it1.hasNext();) {
				SchemaClass consolidatedSchemaClass = (SchemaClass)it1.next();
				
				for (Iterator it2 = superClasses.iterator(); it2.hasNext();) {
					SchemaClass superClass = (SchemaClass)it2.next();
					if (superClass==consolidatedSchemaClass) {
						// oh dear, schemaClass is not top-level
						topLevel = false;
						break;
					}
				}
				if (!topLevel)
					break;
			}
			
			if (topLevel)
				consolidatedClasses.add(schemaClass);
		}
		
		return consolidatedClasses;
	}
	
	/**
	 * Takes the list of schema classes and generates stable IDs for
	 * all instances in these classes.  Nothing is inserted into
	 * the databases.
	 * 
	 * @param classes
	 */
	public void testIDs(List classes) {
		generateIDs(classes, true, null);
	}
	
	/** 
	 *  Gets the List of tests.
	 */
	public IDGeneratorTests getTests() {
		return tests;
	}
	
	/** 
	 *  Removes all stable IDs and version numbers
	 *  from the current release.
	 *  
	 *  Additionally removes all references to these
	 *  IDs from the identifier database.
	 */
	public void rollbackIDs(List classes) {
		if (classes==null)
			return;
		
		List consolidatedClasses = consolidateClassList(classes);
		
		// Go through all instances of each schema class
		for (Iterator it = consolidatedClasses.iterator(); it.hasNext();) {
			SchemaClass currentSchemaClass = (SchemaClass)it.next();
			
			rollbackIDs(currentSchemaClass);
		}
	}
	
	/**
	 * Takes a schema class and removes stable IDs from
	 * all instances in this class.
	 * 
	 * @param schemaClass
	 */
	public void rollbackIDs(SchemaClass currentSchemaClass) {
		if (currentDba==null) {
			System.err.println("IDGenerator.rollbackIDs: currentDba==null!");
			return;
		}

		try {
			Collection currentInstances = currentDba.fetchInstancesByClass(currentSchemaClass);
			
			for (Iterator it = currentInstances.iterator(); it.hasNext();) {
				GKInstance currentInstance = (GKInstance)it.next();
				rollbackIDs(currentInstance);
			}
		} catch (Exception e) {
			System.err.println("IDGenerator.rollbackIDs: problem fetching instances");
			e.printStackTrace(System.err);
		}
	}
	/**
	 * Takes an instance and removes a stable IDs from
	 * it.
	 * 
	 * @param schemaClass
	 */
	public void rollbackIDs(GKInstance currentInstance) {
		try {
			GKInstance currentStableID = (GKInstance)currentInstance.getAttributeValue("stableIdentifier");
			
			// Nothing to roll back
			if (currentStableID==null)
				return;
				
			String identifier = (String)currentStableID.getAttributeValue("identifier");
			String identifierVersion = (String)currentStableID.getAttributeValue("identifierVersion");

			GKInstance identifierDbStableIdentifier = IdentifierDatabase.getStableIdentifierInstance(identifier, false);
			GKInstance stableIdentifierVersion = (GKInstance)identifierDbStableIdentifier.getAttributeValue("stableIdentifierVersion");
			GKInstance releaseIds = (GKInstance)stableIdentifierVersion.getAttributeValue("releaseIds");
			GKInstance release = (GKInstance)releaseIds.getAttributeValue("reactomeRelease");
			GKInstance project = (GKInstance)release.getAttributeValue("project");
			String projectName = (String)release.getAttributeValue("name");
			Long dbId = currentInstance.getDBID();

			// Update the hierarchy of classes below StableIdentifier
			// in the identifier database
			identifierDatabase.deleteVersion(identifierDbStableIdentifier, identifierVersion, projectName, currentReleaseNum, dbId);

			// Delete the StableIdentifier instance from the Reactome database
			currentDba.deleteInstance(currentStableID);
			
			// Update the instance in the current release so that it
			// contains the appropriate stable ID info.
			currentInstance.emptyAttributeValues("stableIdentifier");
			currentDba.updateInstanceAttribute(currentInstance, "stableIdentifier");
		} catch (Exception e) {
			System.err.println("IDGenerator.rollbackIDs: problem fetching instances");
			e.printStackTrace(System.err);
		}
	}
}
