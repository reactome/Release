/*
 * Created on Apr 20, 2004
 */
package org.gk.geneX;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * 
 * @author wugm
 */
public class AppendSwissIDToOMIMData {

	public static void main(String[] args) {
		String dir = "C:\\eclipse\\workspace\\JavaTest\\geneExpression" + File.separator;
		try {
			// Create map from locusID to mimID
			FileReader fileReader = new FileReader(dir + "mim2loc1.txt");
			BufferedReader reader = new BufferedReader(fileReader);
			String line = null;
			Map loc2mimMap = new HashMap();
			int index;
			String mimID;
			String locusID;
			while ((line = reader.readLine()) != null) {
				index = line.indexOf("  ");
				mimID = line.substring(0, index);
				locusID = line.substring(index + 2);
				loc2mimMap.put(locusID, mimID);
			}
			reader.close();
			fileReader.close();
			System.out.println("Map size: " + loc2mimMap.size());
			// Generate file fr swissID to mimID
			fileReader = new FileReader(dir + "acToUn.txt");
			reader = new BufferedReader(fileReader);
			// Escape the first line
			reader.readLine();
			StringTokenizer tokenizer = null;
			String swissID = null;
			int c = 0;
			FileWriter fileWriter = new FileWriter(dir + "swissTomim1.txt");
			PrintWriter writer = new PrintWriter(fileWriter);
			writer.println("SwissID\tOmimID");
			while ((line = reader.readLine()) != null) {
				tokenizer = new StringTokenizer(line, "\t");
				// First is GK_ID
				tokenizer.nextToken();
				locusID = tokenizer.nextToken();
				if (loc2mimMap.containsKey(locusID)) {
					// unigene
					tokenizer.nextToken(); 
					swissID = tokenizer.nextToken();
					writer.println(swissID + "\t" + loc2mimMap.get(locusID));
					c ++;
				}
			}
			System.out.println("Size: " + c);
			reader.close();
			fileReader.close();
			writer.close();
			fileWriter.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

}
