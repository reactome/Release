/*
 * Created on Jan 25, 20057
 */
package org.gk.IDGeneration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/** 
 *  This is the main class for correcting errors in version
 *  numbers.  It has a number of options:
 *  
 *  -t run in test mode (nothing inserted into datbases)
 *  -n <dbname> name for identifier database
 *  -h <hostname> hostname for identifier database
 *  -u <username> user name for identifier database
 *  -p <port> port for identifier database
 *  -P <password> password for identifier database
 *  
 * @author croft
 */
public class VersionNumCorrectionCommandLine  extends JFrame {
	private boolean testMode;
	private DbParams identifierDbParams;
	private IdentifierDatabase identifierDatabase = new IdentifierDatabase();
	
	public VersionNumCorrectionCommandLine() {
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

	private void init() {
		testMode = false;
		identifierDbParams = new DbParams();
	}
	
    public void run() {
    	// Check arguments
    	if (!testMode)
    		handleYesNo("Running this program will change live databases irreversibly.");
    	
		MySQLAdaptor dba = identifierDbParams.getDba();
    	correctVersionNums(dba, testMode);
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
    	System.out.println(" -h <hostname> default hostname for all databases");
    	System.out.println(" -u <username> default user name for all databases");
    	System.out.println(" -p <port> default port for all databases");
    	System.out.println(" -P <password> default password for all databases");
    	
    	System.exit(0);
    }
    
	static public void main(String[] args) {
		VersionNumCorrectionCommandLine iDGenerationCommandLine = new VersionNumCorrectionCommandLine();
		
		// Parse arguments
		String s;
		for (int i=0; i<args.length; i++) {
			s = args[i];
			if (s.equals("-t"))
				iDGenerationCommandLine.setTestMode(true);
			else if (s.equals("-h")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setHostname(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-u")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setUsername(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-p")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setPort(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-P")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setPassword(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-n")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setDbName(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("--help") || s.equals("-help")) {
				iDGenerationCommandLine.printHelp();
			} else
				iDGenerationCommandLine.handleError("Unknown argument" + args[i]);
		}
		
		// Alright, puff, pant, we now have all the things we need
		// to know to actually go ahead and generate stable IDs for
		// the new release.
		iDGenerationCommandLine.run();
	}
	
	/** 
	 *  Looks at the versions for each stable identifier and
	 *  checks if the version number decreases from one release
	 *  to the next.  If testMode is true, simply reports these
	 *  problem cases on STDERR.  If testMode is false, then
	 *  besides reporting the errors, it will also attempt to
	 *  correct them.
	 */
	public static void correctVersionNums(MySQLAdaptor dba, boolean testMode) {
		IdentifierDatabase identifierDatabase = new IdentifierDatabase();
		if (dba==null) {
			System.err.println("IdentifierDatabase.checkVersionNums: WARNING: no DBA available");
			return;
		}
		IdentifierDatabase.setDba(dba);

		List stableIdentifiers = IdentifierDatabase.getInstances("StableIdentifier", dba);
		GKInstance stableIdentifier;
		GKInstance stableIdentifierVersion;
		List stableIdentifierVersions;
		String identifier;
		String identifierVersion;
		int identifierVersionInt;
		GKInstance previousStableIdentifierVersion;
		String previousIdentifierVersion;
		int previousIdentifierVersionInt;
		GKInstance prePreviousStableIdentifierVersion;
		String prePreviousIdentifierVersion;
		int prePreviousIdentifierVersionInt;
		List releaseIds;
		GKInstance releaseId;
		Map releaseIdToVersionNumMap;
		GKInstance release;
		Set keys;
		List keyList;
		String releaseNum;
		String projectName;
		Long instanceDB_ID;
		MySQLAdaptor releaseDba;
		String newVersion;
		GKInstance previousReleaseInstance;
		GKInstance previousReleaseStableIdentifier;
		String previousReleasedentifierVersion;
		try {
			for (Iterator ri = stableIdentifiers.iterator(); ri.hasNext();) {
				stableIdentifier = (GKInstance)ri.next();
				releaseIdToVersionNumMap = new HashMap();
			
				// Build up a hash for this stable identifier,
				// mapping each release onto a version number.
				identifier = (String)stableIdentifier.getAttributeValue("identifierString");
				stableIdentifierVersions = stableIdentifier.getAttributeValuesList("stableIdentifierVersion");
				for (Iterator siv = stableIdentifierVersions.iterator(); siv.hasNext();) {
					stableIdentifierVersion = (GKInstance)siv.next();
					
					identifierVersion = (String)stableIdentifierVersion.getAttributeValue("identifierVersion");
					releaseIds = stableIdentifierVersion.getAttributeValuesList("releaseIds");
					for (Iterator riv = releaseIds.iterator(); riv.hasNext();) {
						releaseId = (GKInstance)riv.next();
						
						release = (GKInstance)releaseId.getAttributeValue(IdentifierDatabase.getReleaseColumn());
						instanceDB_ID = new Long(((Integer)releaseId.getAttributeValue("instanceDB_ID")).toString());
						Object[] value = {stableIdentifierVersion, instanceDB_ID};
						releaseIdToVersionNumMap.put(release, value);
					}
				}
				
				// Get the keys (releases) of the hash, and
				// sort them numerically into a list.
				keys = releaseIdToVersionNumMap.keySet();
				keyList = new ArrayList(keys);
				IdentifierDatabase.ReleaseComparator releaseComparator = identifierDatabase.getNewReleaseComparator();
				Collections.sort(keyList, releaseComparator);
				
				// Compare version numbers pairwise, from release
				// to release.
				prePreviousStableIdentifierVersion = null;
				previousStableIdentifierVersion = null;
				previousReleaseInstance = null;
				for (Iterator k = keyList.iterator(); k.hasNext();) {
					release = (GKInstance)k.next();
					Object[] value = (Object[])releaseIdToVersionNumMap.get(release);
					stableIdentifierVersion = (GKInstance)value[0];
					identifierVersion = (String)stableIdentifierVersion.getAttributeValue("identifierVersion");
					identifierVersionInt = Integer.parseInt(identifierVersion);
					previousIdentifierVersionInt = Integer.MIN_VALUE;
					if (previousStableIdentifierVersion!=null) {
						previousIdentifierVersion = (String)previousStableIdentifierVersion.getAttributeValue("identifierVersion");
						previousIdentifierVersionInt = Integer.parseInt(previousIdentifierVersion);
					}
					prePreviousIdentifierVersionInt = Integer.MIN_VALUE;
					if (prePreviousStableIdentifierVersion!=null) {
						prePreviousIdentifierVersion = (String)prePreviousStableIdentifierVersion.getAttributeValue("identifierVersion");
						prePreviousIdentifierVersionInt = Integer.parseInt(prePreviousIdentifierVersion);
					}
					releaseNum = (String)release.getAttributeValue("num");
					projectName = (String)(((GKInstance)release.getAttributeValue("project")).getAttributeValue("name"));
					if (previousIdentifierVersionInt>identifierVersionInt) {
						
						System.err.println("IdentifierDatabase.checkVersionNums: version number incongruity, identifier=" + identifier + ", identifierVersionInt=" + identifierVersionInt + ", previousIdentifierVersionInt=" + previousIdentifierVersionInt + ", releaseNum=" + releaseNum);
						
						if (identifierVersionInt>=prePreviousIdentifierVersionInt)
							newVersion = identifierVersion;
						else
							newVersion = Integer.toString(prePreviousIdentifierVersionInt);
						
						System.err.println("IdentifierDatabase.getMaxReleaseStableIdentifier: inserting newVersion=" + newVersion);
						
						// Update identifier database
						if (!testMode) {
							previousStableIdentifierVersion.setAttributeValue("identifierVersion", newVersion);
							dba.updateInstanceAttribute(previousStableIdentifierVersion, "identifierVersion");
						}
						
						// Update release database, if necessary
						if (previousReleaseInstance==null)
							System.err.println("IdentifierDatabase.getMaxReleaseStableIdentifier: WARNING - previousReleaseInstance is null, that shouldn't be so!");
						else {
							previousReleaseStableIdentifier = (GKInstance)previousReleaseInstance.getAttributeValue("stableIdentifier");
							if (previousReleaseStableIdentifier==null)
								System.err.println("IdentifierDatabase.getMaxReleaseStableIdentifier: WARNING - previousReleaseStableIdentifier is null!!");
							else {
								previousReleasedentifierVersion = (String)previousReleaseStableIdentifier.getAttributeValue("identifierVersion");
								if (previousReleasedentifierVersion.equals(newVersion))
									System.err.println("IdentifierDatabase.getMaxReleaseStableIdentifier: previousReleasedentifierVersion same as newVersion, no need for change to release DB");
								else {
									System.err.println("IdentifierDatabase.getMaxReleaseStableIdentifier: overwriting previousReleasedentifierVersion=" + previousReleasedentifierVersion);
									
									if (!testMode) {
										previousReleaseStableIdentifier.setAttributeValue("identifierVersion", newVersion);
										dba.updateInstanceAttribute(previousReleaseStableIdentifier, "identifierVersion");
									}
								}
							}
						}
					}
					
					instanceDB_ID = (Long)value[1];
					releaseDba = identifierDatabase.getReleaseDbaFromReleaseNum(releaseNum, projectName);
					if (releaseDba==null) {
//						System.err.println("IdentifierDatabase.getMaxReleaseStableIdentifier: WARNING - releaseDba is null for releaseNum=" + releaseNum);
					} else {
						previousReleaseInstance = releaseDba.fetchInstance(instanceDB_ID);
					}
					
					prePreviousStableIdentifierVersion = previousStableIdentifierVersion;
					previousStableIdentifierVersion = stableIdentifierVersion;
				}
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getMaxReleaseStableIdentifier: something nasty happened while trying to get an Identifier instance for the identifier database");
			e.printStackTrace();
		}
	}
}