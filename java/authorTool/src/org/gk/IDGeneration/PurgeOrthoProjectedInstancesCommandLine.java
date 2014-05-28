/*
 * Created on June 6, 2011
 */
package org.gk.IDGeneration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/** 
 *  This is the main class for removing all references to ortho-projected events from
 *  the identifier database, and all stable identifiers associated with these events
 *  from the corresponding release databases.
 *  
 * @author croft
 */
public class PurgeOrthoProjectedInstancesCommandLine {
	private boolean testMode;
	private DbParams identifierDbParams;
	private IdentifierDatabase identifierDatabase = new IdentifierDatabase();
	private static Map<Long, Long> dbids = null;
	
	public PurgeOrthoProjectedInstancesCommandLine() {
		init();
	}
	
	public boolean isTestMode() {
		return testMode;
	}

	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}

	public void setDbName(String dbName) {
		if (identifierDbParams.dbName.equals(""))
			identifierDbParams.dbName = dbName;
	}

	public void setHostname(String hostname) {
		if (identifierDbParams.hostname.equals(""))
			identifierDbParams.hostname = hostname;
	}

	public void setPassword(String password) {
		if (identifierDbParams.password.equals(""))
			identifierDbParams.password = password;
	}

	public void setPort(String port) {
		if (identifierDbParams.port.equals(""))
			identifierDbParams.port = port;
	}

	public void setUsername(String username) {
		if (identifierDbParams.username.equals(""))
			identifierDbParams.username = username;
	}

	public void setDbids(Map<Long, Long> dbids) {
		this.dbids = dbids;
	}

	private void init() {
		testMode = false;
		identifierDbParams = new DbParams();
	}
	
    public void run() {
    	// Check arguments
    	if (!testMode)
    		handleYesNo("Running this program will change live databases irreversibly.");
    	
		MySQLAdaptor dba = identifierDbParams.getDba();
		purgeOrthoProjectedInstances(dba, testMode);
    }
    
    private void handleError(String text) {
    	System.err.println(text);
    	System.exit(1);
    }
    
    private void handleYesNo(String text) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println(text);
		System.out.print("Are you sure you want to do this?  (y/n) ");
		String answer = "n";
		try {
			answer = reader.readLine();
		} catch (IOException e) {
		}
		if (answer.equals("n"))
			System.exit(0);
    }
    
    static private void printHelp() {
    	System.out.println("Usage:");
    	System.out.println("");
    	System.out.println("java org.gk.IDGeneration.IDGenerationCommandLine <options>");
    	System.out.println("");
    	System.out.println("The following options are available:");
    	System.out.println("");
    	System.out.println(" -t run in test mode (nothing inserted into datbases)");
    	System.out.println(" -idbname <db_name> identifier database name");
    	System.out.println(" -host <hostname> default hostname for all databases");
    	System.out.println(" -user <username> default user name for all databases");
    	System.out.println(" -port <port> default port for all databases");
    	System.out.println(" -pass <password> default password for all databases");
    	
    	System.exit(0);
    }
    
	static public void main(String[] args) {
		PurgeOrthoProjectedInstancesCommandLine commandLine = new PurgeOrthoProjectedInstancesCommandLine();
		
		// Parse arguments
		String s;
		for (int i=0; i<args.length; i++) {
			s = args[i];
			if (s.equals("-t"))
				commandLine.setTestMode(true);
			else if (s.equals("-host")) {
				i++;
				if (i<args.length)
					commandLine.setHostname(args[i]);
				else {
					System.err.println("PurgeOrthoProjectedInstancesCommandLine.main: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-user")) {
				i++;
				if (i<args.length)
					commandLine.setUsername(args[i]);
				else {
					System.err.println("PurgeOrthoProjectedInstancesCommandLine.main: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-port")) {
				i++;
				if (i<args.length)
					commandLine.setPort(args[i]);
				else {
					System.err.println("PurgeOrthoProjectedInstancesCommandLine.main: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-pass")) {
				i++;
				if (i<args.length)
					commandLine.setPassword(args[i]);
				else {
					System.err.println("PurgeOrthoProjectedInstancesCommandLine.main: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-idbname")) {
				i++;
				if (i<args.length)
					commandLine.setDbName(args[i]);
				else {
					System.err.println("PurgeOrthoProjectedInstancesCommandLine.main: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-dbids")) {
				i++;
				if (i<args.length) {
					String[] splits = args[i].split(",");
					Map<Long, Long> dbids = new HashMap<Long, Long>();
					for (int j=0; j<splits.length; j++)
						dbids.put(new Long(splits[j]), new Long(splits[j]));
					commandLine.setDbids(dbids);
				} else {
					commandLine.handleError("IDGenerationCommandLine: missing argument");
				}
			}
			else if (s.equals("--help") || s.equals("-help")) {
				commandLine.printHelp();
			} else
				commandLine.handleError("Unknown argument" + args[i]);
		}
		
		// Alright, puff, pant, we now have all the things we need
		// to know to actually go ahead and generate stable IDs for
		// the new release.
		commandLine.run();
	}
	
	/** 
	 *  Removes stable IDs for instances that have been generated by othology projection.
	 */
	public static void purgeOrthoProjectedInstances(MySQLAdaptor dba, boolean testMode) {
		IdentifierDatabase identifierDatabase = new IdentifierDatabase();
		if (dba==null) {
			System.err.println("PurgeOrthoProjectedInstancesCommandLine.checkVersionNums: WARNING: no DBA available");
			return;
		}
		IdentifierDatabase.setDba(dba);

		List stableIdentifiers = IdentifierDatabase.getInstances("StableIdentifier", dba);
		GKInstance stableIdentifier;
		GKInstance stableIdentifierVersion;
		List stableIdentifierVersions;
		List releaseIds;
		GKInstance releaseId;
		GKInstance release;
		String releaseNum;
		String projectName;
		Long instanceDB_ID;
		MySQLAdaptor releaseDba;
		GKInstance releaseInstance;
		int stableIdentifierVersionsAllOrthologCounter;
		int releaseIdsAllOrthologCounter;
		try {
			// Get rid of stable ID instances for ortho predicted events
			for (Iterator ri = stableIdentifiers.iterator(); ri.hasNext();) {
				stableIdentifier = (GKInstance)ri.next();
				stableIdentifierVersionsAllOrthologCounter = 0;
				stableIdentifierVersions = stableIdentifier.getAttributeValuesList("stableIdentifierVersion");
				for (Iterator siv = stableIdentifierVersions.iterator(); siv.hasNext();) {
					stableIdentifierVersion = (GKInstance)siv.next();
					releaseIdsAllOrthologCounter = 0;
					releaseIds = stableIdentifierVersion.getAttributeValuesList("releaseIds");
					for (Iterator riv = releaseIds.iterator(); riv.hasNext();) {
						releaseId = (GKInstance)riv.next();

						instanceDB_ID = new Long(((Integer)releaseId.getAttributeValue("instanceDB_ID")).toString());
						if (dbids != null && dbids.get(instanceDB_ID) != instanceDB_ID) {
							continue;
						}
						
						release = (GKInstance)releaseId.getAttributeValue(IdentifierDatabase.getReleaseColumn());
						releaseNum = (String)release.getAttributeValue("num");
						projectName = (String)(((GKInstance)release.getAttributeValue("project")).getAttributeValue("name"));
						releaseDba = identifierDatabase.getReleaseDbaFromReleaseNum(releaseNum, projectName);
						if (releaseDba==null) {
							System.err.println("PurgeOrthoProjectedInstancesCommandLine.purgeOrthoProjectedInstances: WARNING - releaseDba is null for releaseNum=" + releaseNum);
							continue;
						}
						
						releaseInstance = releaseDba.fetchInstance(instanceDB_ID);
						// check to see if this is an ortho-projected instance
						if (OrthologousEventPreviousInstanceFinder.isStableIdentifierAllowed(releaseInstance) && releaseInstance.getSchemClass().isValidAttribute("stableIdentifier")) {
							releaseIdsAllOrthologCounter++;
							// get stable identifier and delete it from release database
							stableIdentifier = (GKInstance)releaseInstance.getAttributeValue("stableIdentifier");
							if (stableIdentifier != null) {
								System.err.println("PurgeOrthoProjectedInstancesCommandLine.purgeOrthoProjectedInstances: deleting stableIdentifier instance from release database");
								if (!testMode)
									releaseDba.deleteInstance(stableIdentifier);
								continue;
							}
						}
					}
					// if all release ID stable IDs get removed, remove release ID as well
					if (releaseIdsAllOrthologCounter == releaseIds.size()) {
						stableIdentifierVersionsAllOrthologCounter++;
						System.err.println("PurgeOrthoProjectedInstancesCommandLine.purgeOrthoProjectedInstances: deleting stableIdentifierVersion instance from identifier database, stableIdentifierVersionsAllOrthologCounter=" + stableIdentifierVersionsAllOrthologCounter);
						if (!testMode)
							dba.deleteInstance(stableIdentifierVersion);
						continue;
					}
				}
				// if all release IDs get removed, remove stable ID version as well
				if (stableIdentifierVersionsAllOrthologCounter == stableIdentifierVersions.size()) {
					System.err.println("PurgeOrthoProjectedInstancesCommandLine.purgeOrthoProjectedInstances: deleting stableIdentifier instance from identifier database, stableIdentifierVersionsAllOrthologCounter=" + stableIdentifierVersionsAllOrthologCounter);
					if (!testMode)
						dba.deleteInstance(stableIdentifier);
				}
			}
			
			// Get rid of all ReactomeRelease instances without referrers
			List<GKInstance> reactomeReleases = IdentifierDatabase.getInstances("ReactomeRelease", dba);
			Map referrers;
			for (GKInstance reactomeRelease: reactomeReleases) {
				referrers = reactomeRelease.getReferers();
				if (referrers == null || referrers.size() == 0) {
					System.err.println("PurgeOrthoProjectedInstancesCommandLine.purgeOrthoProjectedInstances: deleting reactomeRelease instance from identifier database, reactomeRelease.num=" + reactomeRelease.getAttributeValue("num"));
					if (!testMode)
						dba.deleteInstance(reactomeRelease);
				}
			}
			
			// Get rid of all DbParams instances without referrers
			List<GKInstance> dbParamss = IdentifierDatabase.getInstances("DbParams", dba);
			for (GKInstance dbParams: dbParamss) {
				referrers = dbParams.getReferers();
				if (referrers == null || referrers.size() == 0) {
					System.err.println("PurgeOrthoProjectedInstancesCommandLine.purgeOrthoProjectedInstances: deleting reactomeRelease instance from identifier database, reactomeRelease.dbName=" + dbParams.getAttributeValue("dbName"));
					if (!testMode)
						dba.deleteInstance(dbParams);
				}
			}
		} catch (Exception e) {
			System.err.println("PurgeOrthoProjectedInstancesCommandLine.purgeOrthoProjectedInstances: WARNING - something nasty happened while trying to get an Identifier instance for the identifier database");
			e.printStackTrace();
		}
	}
}