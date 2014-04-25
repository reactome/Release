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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.gk.model.GKInstance;
import org.gk.pathView.DefaultGeneExpressionDataImporter;
import org.gk.pathView.IEdge;

/**
 * A customized GeneExpressionDataImport for loading Bijay's expression data.
 * @author wugm
 */
public class BijayDataImporter extends DefaultGeneExpressionDataImporter {
	private java.util.List terminalData = null;

	public BijayDataImporter() {
	}

	public void setDataSource(File file) throws IOException {
		if (edges == null || edges.size() == 0)
			return; // Don't do anything if nothing in
		// Load data from the file
		FileReader fileReader = new FileReader(file);
		BufferedReader reader = new BufferedReader(fileReader);
		String line = reader.readLine(); // It should be the title line
		StringTokenizer tokenizer = new StringTokenizer(line, "\t");
		// Fist token should be SwissProt
		tokenizer.nextToken();
		// Second for affymetric name
		tokenizer.nextToken();
		// Third for DB_ID
		tokenizer.nextToken();
		dataPoints = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			dataPoints.add(tokenizer.nextToken());
		}
		// Loaded Data
		Map loadedDataMap = new HashMap(); // Key: SwissID, Values: double array
		String swissID = null;
		int index = 0;
		while ((line = reader.readLine()) != null) {
			tokenizer = new StringTokenizer(line, "\t");
			swissID = tokenizer.nextToken();
			// Escape two tokens: name and DB_ID
			tokenizer.nextToken();
			tokenizer.nextToken();
			double[] data = new double[dataPoints.size()];
			index = 0;
			while (tokenizer.hasMoreTokens())
				data[index ++] = Double.parseDouble(tokenizer.nextToken());
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
		// For terminal data
		terminalData = new ArrayList(dataPoints.size());
		for (int i = 0; i < dataPoints.size(); i++)
			terminalData.add(null);
	}
	
	public void setDataIndex(int index) {
		if (preIndex == index)
			return;
		IEdge edge = null;
		// Need find max and min values
		Map tmpDataMap = new HashMap();
		for (Iterator it = dataMap.keySet().iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			double[] data = (double[]) dataMap.get(edge);
			double value = data[index];
			tmpDataMap.put(edge, new Double(value));
		}
		double[] terminalValues = (double[]) terminalData.get(index);
		if (terminalValues == null) {
			// To calculate the first 5% and last 5%
			java.util.List valueList = new ArrayList(tmpDataMap.values());
			Collections.sort(valueList);
			int size = valueList.size();
			int firstFive = (int) (size * 0.05);
			int lastFive = (int) (size * 0.95);
			double min = ((Double)valueList.get(firstFive)).doubleValue();
			double max = ((Double)valueList.get(lastFive)).doubleValue();
			terminalValues = new double[] { min, max };
			terminalData.set(index, terminalValues);
		}
		double min = terminalValues[0];
		double max = terminalValues[1];
		double diff = max - min;
		for (Iterator it = tmpDataMap.keySet().iterator(); it.hasNext();) { 
			edge = (IEdge) it.next();
			double value = ((Double)tmpDataMap.get(edge)).doubleValue();
			float rValue = (float) (1.0 - (max - value) / diff);
			if (rValue > 1.0f)
				rValue = 1.0f;
			if (rValue < 0.0f)
				rValue = 0.0f;
			Color c = new Color(rValue, 1.0f - rValue, 0.0f);
			edge.setColor(c);
		}
		preIndex = index;	
	}
	
	public int getMajorTickSpacing() {
		return 10;
	}
	
	public boolean isDataPointsUsedForLabels() {
		return false;
	}
}
