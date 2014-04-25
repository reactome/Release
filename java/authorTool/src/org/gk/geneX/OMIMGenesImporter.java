/*
 * Created on Apr 20, 2004
 */
package org.gk.geneX;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.pathView.DefaultGeneExpressionDataImporter;
import org.gk.pathView.IEdge;

/**
 * 
 * @author wugm
 */
public class OMIMGenesImporter extends DefaultGeneExpressionDataImporter {

	public OMIMGenesImporter() {
	}
	
	public void setDataSource(File file) throws IOException {
		if (edges == null || edges.size() == 0)
			return; // Don't do anything if nothing in
		// Load data from the file
		FileReader fileReader = new FileReader(file);
		BufferedReader reader = new BufferedReader(fileReader);
		String line = reader.readLine(); // It should be the title line
		dataPoints = new ArrayList();
		dataPoints.add("OMIM Genes");
		// Loaded Data
		Map loadedDataMap = new HashMap(); // Key: SwissID, Values: double array
		String swissID = null;
		int index = 0;
		while ((line = reader.readLine()) != null) {
			index = line.indexOf("\t");
			if (index > 0)
			    swissID = line.substring(0, index);
			else
			    swissID = line;
		    // Assume the value is 1.0 if there is an entry in the OMIM database.
			double[] data = new double[]{1.0}; 
			loadedDataMap.put(swissID, data);
		}
		// Populate the data to dataMap to cache all useful data.
		dataMap.clear();
		IEdge edge = null;
		GKInstance instance = null;
		int dataLenght = dataPoints.size();
		try {
			for (Iterator it = edges.iterator(); it.hasNext();) {
				edge = (IEdge)it.next();
				instance = (GKInstance)edge.getUserObject();
				if (instance == null)
					continue;
				double[] data = computeReactionData(instance, dataLenght, loadedDataMap);
				if (data != null)
					dataMap.put(edge, data);
			}
		}
		catch (Exception e) {
			System.err.println("GeneExpressionDataImporter.setDataSource(): " + e);
			e.printStackTrace();
		}
		// To record the original color for rolling back
		if (originalColorMap == null)
			originalColorMap = new HashMap();
		else
			originalColorMap.clear();
		for (Iterator it = dataMap.keySet().iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			originalColorMap.put(edge, edge.getColor());
		}
	}

	public void setDataIndex(int index) {
		if (preIndex == index)
			return;
		IEdge edge = null;
		for (Iterator it = dataMap.keySet().iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			double[] data = (double[]) dataMap.get(edge);
			double value = data[index];
			if (value > 0.0) {
				Color c = new Color(1.0f, 0.0f, 0.0f);
				edge.setColor(c);
			}
		}
		preIndex = index;	
	}

}
