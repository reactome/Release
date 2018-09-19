package org.reactome.retrievers.pharosRetriever;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class RetrieveData {

	@SuppressWarnings("unchecked")
	public static String queryPharos() {
		
		boolean continuing = true;
		int skipCount = 0;
		int totalCount = 0;
		int elementKeyCount = 31;
		List<Object> keyListForComparison = null;
		ArrayList<String> tsvLines = new ArrayList<String>();
		HashSet<String> specialHeaders = new HashSet<String>();
		String tsvFilename = "pharos_tdark.tsv";
		
		try {
			// Since we don't know what the total number of calls we have to make at the start of the script, 
			// we just run indefinitely and changing the 'continuing' boolean once we've reached the total value found in each response
			while (continuing) {
				String urlString = "https://pharos.nih.gov/idg/api/v1/targets/search?facet=IDG%20Development%20Level/Tdark&top=50&skip=";
				urlString += skipCount;
				System.out.println(urlString);
				URL url = new URL(urlString);
				InputStreamReader inputStream = new InputStreamReader(url.openStream(), "UTF-8");
				BufferedReader reader = new BufferedReader(inputStream);
				
				// Iterate through JSON-formatted response
				for (String line; (line = reader.readLine()) != null;) {
					JSONParser parser = new JSONParser();
					JSONObject jsonObject = (JSONObject) parser.parse(line);
					// Information we care about is stored in 'content' array
					JSONArray jsonArray = (JSONArray) jsonObject.get("content");
					for (JSONObject jsonElementObj : (Collection<JSONObject>) jsonArray) {
						totalCount++;
						if (jsonElementObj.keySet().size() == elementKeyCount) {
							ArrayList<String> tsvList = new ArrayList<String>();
							// Sort keys so that each line added to file will be the same structure
							Set<String> keyList = jsonElementObj.keySet();
							List<Object> keyListSorted = keyList.stream().collect(Collectors.toList());
							
							// First iteration will set the keyList structure that will be adhered to on all subsequent iterations
							if (keyListForComparison == null) {
								keyListForComparison = keyListSorted;
							} else if (!keyListForComparison.equals(keyListSorted)) {
								System.out.println("Header list is not ordered correctly");
								System.exit(0);
							}
							// Values that we care about the most, so they are getting special treatment
							tsvList.add((String) jsonElementObj.get("gene"));
							tsvList.add((String) jsonElementObj.get("accession"));
							tsvList.add((String) jsonElementObj.get("name"));
							tsvList.add((String) jsonElementObj.get("idgTDL"));
							
							for (Object key : keyListSorted) {
								if (key.equals("gene") || key.equals("accession") || key.equals("name") || key.equals("idgTDL")) {
									continue;
								}
								// Null elements are stored as such
								if (jsonElementObj.get(key) == null) {
									tsvList.add("null");
								// Some of the elements are not single-value, but json objects themselves
								} else if (jsonElementObj.get(key).getClass().getSimpleName().equals("JSONObject")) {
									JSONObject subJSONObject = (JSONObject) jsonElementObj.get(key);
									tsvList.add((String) subJSONObject.get("count").toString());
									tsvList.add((String) subJSONObject.get("href"));
									specialHeaders.add((String) key);
								// Single-value elements
								} else {
									tsvList.add((String) jsonElementObj.get(key).toString());
								}
							}

							String tsvString = String.join("\t", tsvList);
							tsvLines.add(tsvString);
						} else {
							System.out.println("Array does not contain correct number of keys");
							System.exit(0);
						}
						
					}
					// Keeps track of the 'skip-count', that we base each api call off of
					long count = (Long) jsonObject.get("count");
					skipCount += (int) count;
					
					// This value tells us the total -- when that count is reached, we set 'continuing' to false, which terminates the API-call loop
					long total = (Long) jsonObject.get("total");
					if (total == totalCount) {
						System.out.println("Total count reached -- ending API-call loop");
						continuing = false;
					}
				}
			}
			// Header formatting to account for prioritized values and for elements that contained their own sub-elements
			ArrayList<String> headerArray = new ArrayList<String>();
			headerArray.add("gene");
			headerArray.add("accession");
			headerArray.add("name");
			headerArray.add("idgTDL");
			for (Object header : keyListForComparison) {
				if (header.equals("gene") || header.equals("accession") || header.equals("name") || header.equals("idgTDL")) {
					continue;
				}
				// As of writing, the sub-element containing values had 'count' and 'href' as their sub-keys. If this changes, the script will need to be updated.
				if (specialHeaders.contains(header)) {
					String countHeader = header + "_count";
					String hrefHeader = header + "_href";
					headerArray.add(countHeader);
					headerArray.add(hrefHeader);
				} else {
					headerArray.add((String) header);
				}
			}
			// Write file
			String headerLine = String.join("\t", headerArray) + "\n";
			
			PrintWriter tsvFile = new PrintWriter(tsvFilename);
			tsvFile.close();
			Files.write(Paths.get(tsvFilename), headerLine.getBytes(), StandardOpenOption.APPEND);
			String tsvLinesString = String.join("\n", tsvLines);
			Files.write(Paths.get(tsvFilename), tsvLinesString.getBytes(), StandardOpenOption.APPEND);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tsvFilename;
	}
}
