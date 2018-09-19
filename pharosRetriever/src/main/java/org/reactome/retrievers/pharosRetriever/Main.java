package org.reactome.retrievers.pharosRetriever;

/**
 * Queries the API of the Pharos resource (https://pharos.nih.gov/idg/index) for all 'tdark' targets, which are those that are not well known/understood. 
 * 
 * The results are returned in JSON, so this program goes through all 'tdark' targets programmatically, and outputs the results in TSV format.
 *  
 * @author jcook
 */


public class Main {

	public static void main(String[] args) {
		
		String filename = RetrieveData.queryPharos();
		System.out.println("Pharos data retrieval completed, output: " + filename );
	}
}
