/*
 * Created on Apr 20, 2004
 */
package org.gk.geneX;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author wugm
 */
public class AppendSwissIDToExpressionData {

	private static final String dir = "C:\\temp\\forBijay\\";

	public static void main(String[] args) {
		try {
			// Load SwallID to AffyID map
			FileReader idFile = new FileReader(dir + "gk_id_matches.txt");
			BufferedReader reader = new BufferedReader(idFile);
			Map affyIDToSwissID = new HashMap();
			String line = null;
			int index = 0;
			String affyID = null;
			String swallID = null;
			while ((line = reader.readLine()) != null) {
				index = line.indexOf(" ");
				swallID = line.substring(0, index);
				affyID = line.substring(index + 1);
				index = swallID.indexOf(":");
				swallID = swallID.substring(index + 1);	
				affyIDToSwissID.put(affyID, swallID);
			}
			reader.close();
			idFile.close();
			System.out.println("Map size: " + affyIDToSwissID.size());
			// Attach SwallID to the expression data file.
			FileReader dataFile = new FileReader(dir + "affy_micarr_170304.txt");
			reader = new BufferedReader(dataFile);
			FileWriter fileWriter = new FileWriter(dir + "bijayData.txt");
			PrintWriter writer = new PrintWriter(fileWriter);
			line = reader.readLine(); // Title line
			writer.println("SwissID\t" + line);
			int c = 0;
			while ((line = reader.readLine()) != null) {
				index = line.indexOf("\t");
				affyID = line.substring(0, index);
				swallID = (String) affyIDToSwissID.get(affyID);
				if (swallID != null) {
					writer.println(swallID + "\t" + line);
					c ++;
				}
			}
			System.out.println("Usable data: " + c);
			reader.close();
			dataFile.close();
			writer.close();
			fileWriter.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

}
