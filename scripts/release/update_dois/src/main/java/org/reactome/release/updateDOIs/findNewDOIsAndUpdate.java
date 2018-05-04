package org.reactome.release.updateDOIs;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.util.GKApplicationUtilities;

public class findNewDOIsAndUpdate {

  final static Logger logger = Logger.getLogger(findNewDOIsAndUpdate.class);

  private MySQLAdaptor dbaTestReactome;
  private MySQLAdaptor dbaGkCentral;

  // Create adaptors for test_reactome and gk_central
  public void setTestReactomeAdaptor(MySQLAdaptor adaptor) {
    this.dbaTestReactome = adaptor;
  }

  public void setGkCentralAdaptor(MySQLAdaptor adaptor) {
    this.dbaGkCentral = adaptor;
  }

  @SuppressWarnings({"unchecked","rawtypes"})
public void findAndUpdateDOIs(long authorId, String pathToReport) {

    Collection<GKInstance> dois;
    Collection<GKInstance> gkdois;

    // TODO: instanceEdits implementation
//	String creatorFile = "org.reactome.release.updateDOIs.UpdateDOIs";
//	GKInstance instanceEditTestReactome = this.createInstanceEdit(authorId, creatorFile);
//	GKInstance instanceEditGkCentral = this.createInstanceEdit(authorId, creatorFile);

    HashMap reportContents = this.getReportContents(pathToReport);
    int reportHits = 0;
    int fetchHits = 0;
    ArrayList updatedDOIs = new ArrayList();
    try {

     dois = this.dbaTestReactome.fetchInstanceByAttribute("Pathway", "doi", "NOT REGEXP", "^10.3180");
//     dois = this.dbaTestReactome.fetchInstanceByAttribute("Pathway", "DB_ID", "REGEXP", "8939211|9006115|9018678|9033241");

      if (!dois.isEmpty()) {
        for (GKInstance doi : dois) {
          String stableIdFromDb = ((GKInstance) doi.getAttributeValue("stableIdentifier")).getDisplayName();
          String nameFromDb = doi.getAttributeValue("name").toString();
          String updatedDoi = "10.3180/" + stableIdFromDb;

          fetchHits++;
          updatedDOIs.add(updatedDoi);
          if (reportContents.get(updatedDoi) != null && reportContents.get(updatedDoi).toString().equals(nameFromDb))
          {
        	  reportHits++;
          }

          String dbId = doi.getAttributeValue("DB_ID").toString();
          doi.setAttributeValue("doi", updatedDoi);
          this.dbaTestReactome.updateInstanceAttribute(doi, "doi");

          // Grabs instance from gk central based on DB_ID taken from test_reactome, and updated doi field
          gkdois = this.dbaGkCentral.fetchInstanceByAttribute("Pathway", "DB_ID", "=", dbId);
          if (!gkdois.isEmpty()) {
            for (GKInstance gkdoi : gkdois) {
              gkdoi.setAttributeValue("doi", updatedDoi);
              this.dbaGkCentral.updateInstanceAttribute(gkdoi, "doi");
              logger.info("Updated DOI: " + updatedDoi + " for " + nameFromDb);
            }
          } else {
        	  logger.error("Could not find attribute in gk_central");
          }
        }

        // Checking if provided list matched
        if (reportContents.size() != 0 && reportHits < reportContents.size())
        {
        	for (Object updatedDOI : updatedDOIs)
        	{
        		reportContents.remove(updatedDOI);
        	}
        	logger.warn("The following DOIs from the provided list were not updated: ");
        	logger.warn("  " + reportContents.keySet());
        } else if (reportContents.size() != 0 && fetchHits > reportContents.size()) {

        	logger.warn("The following DOIs were unexpectedly updated: ");
        	for (Object updatedDOI : updatedDOIs)
        	{
        		if (reportContents.get(updatedDOI) == null)
        		{
        			logger.warn("  " + updatedDOI);
        		}
        	}
        } else if (reportContents.size() != 0) {

        	logger.info("All expected DOIs updated");
        }
      } else {
    	  logger.info("No DOIs to update");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Parses input report and places each line's contents in HashMap
  @SuppressWarnings("rawtypes")
  public HashMap getReportContents(String pathToReport) {

	HashMap reportContents = new HashMap();
	try {
	  	FileReader fr = new FileReader(pathToReport);
	  	BufferedReader br = new BufferedReader(fr);

	  	String sCurrentLine;
	  	while ((sCurrentLine = br.readLine()) != null)
	  	{
	  		String[] splitLine = sCurrentLine.split(",");
	  		reportContents.put(splitLine[0], splitLine[1]);
	  	}
	  	br.close();
	  	fr.close();

	} catch (Exception e) {
		logger.warn("No input file found -- Continuing without checking DOIs");
	}
	return reportContents;
  }

  /**
     * Create an InstanceEdit.
     * @param personID - ID of the associated Person entity.
     * @param creatorName - The name of the thing that is creating this InstanceEdit. Typically, you would want to use the package and classname that
     * uses <i>this</i> object, so it can be traced to the appropriate part of the program.
     * @return
     */
    public GKInstance createInstanceEdit(long personID, String creatorName) {
        GKInstance instanceEdit = null;
        try
        {
            instanceEdit = createDefaultIE(this.dbaTestReactome, personID, true, "Inserted by " + creatorName);
            instanceEdit.getDBID();
            this.dbaTestReactome.updateInstance(instanceEdit);
        }
        catch (Exception e)
        {
            // logger.error("Exception caught while trying to create an InstanceEdit: {}", e.getMessage());
            e.printStackTrace();
        }
        return instanceEdit;
    }

    // This code below was taken from 'add-links' repo: org.reactomeaddlinks.db.ReferenceCreator
    /**
     * Create and save in the database a default InstanceEdit associated with the Person entity whose DB_ID is <i>defaultPersonId</i>.
     * @param dba
     * @param defaultPersonId
     * @param needStore
     * @return an InstanceEdit object.
     * @throws Exception
     */
    public static GKInstance createDefaultIE(MySQLAdaptor dba, Long defaultPersonId, boolean needStore, String note) throws Exception {
        GKInstance defaultPerson = dba.fetchInstance(defaultPersonId);
        if (defaultPerson != null) {
            GKInstance newIE = findNewDOIsAndUpdate.createDefaultInstanceEdit(defaultPerson);
            newIE.addAttributeValue(ReactomeJavaConstants.dateTime, GKApplicationUtilities.getDateTime());
            newIE.addAttributeValue(ReactomeJavaConstants.note, note);
            InstanceDisplayNameGenerator.setDisplayName(newIE);

            if (needStore) {
                dba.storeInstance(newIE);
            } else {
            // This 'else' block wasn't here when first copied from ReferenceCreator. Added to reduce future potential headaches. (JC)
              System.out.println("needStore set to false");
            }
            return newIE;
        } else {
            throw new Exception("Could not fetch Person entity with ID " + defaultPersonId + ". Please check that a Person entity exists in the database with this ID.");
        }
      }

      public static GKInstance createDefaultInstanceEdit(GKInstance person) {
        GKInstance instanceEdit = new GKInstance();
        PersistenceAdaptor adaptor = person.getDbAdaptor();
        instanceEdit.setDbAdaptor(adaptor);
        SchemaClass cls = adaptor.getSchema().getClassByName(ReactomeJavaConstants.InstanceEdit);
        instanceEdit.setSchemaClass(cls);

        try
        {
          instanceEdit.addAttributeValue(ReactomeJavaConstants.author, person);
        }
        catch (InvalidAttributeException | InvalidAttributeValueException e)
        {
          e.printStackTrace();
          // throw this back up the stack - no way to recover from in here.
          throw new Error(e);
        }

        return instanceEdit;
      }
}
