/*
 * Created on Apr 8, 2004
 */
package org.gk.pathView;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.gk.model.GKInstance;

/**
 * A class for load gene expression data.
 * @author wgm
 */
public class DefaultGeneExpressionDataImporter implements GeneExpressionDataImporter {
	// To be colored
	protected java.util.List edges = null;
	// To cache data
	protected Map dataMap; // Key: IEdge Value: double array for data
	// For roll back
	protected Map originalColorMap;
	// Data title
	protected java.util.List dataPoints;
	// recorde the previous state
	protected int preIndex = -1;
	
	public DefaultGeneExpressionDataImporter() {
		dataMap = new HashMap();
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
		dataPoints = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			dataPoints.add(new Integer(tokenizer.nextToken()));
		}
		// Loaded Data
		Map loadedDataMap = new HashMap(); // Key: SwissID, Values: double array
		String swissID = null;
		int index = 0;
		while ((line = reader.readLine()) != null) {
			tokenizer = new StringTokenizer(line);
			swissID = tokenizer.nextToken();
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
	}
	
	protected double[] computeReactionData(GKInstance rxtInstance, int dataLength, Map loadedDataMap) throws Exception {
		double[] reactionData = new double[dataLength];
		int c = 0;
		java.util.List positiveEntities = new ArrayList();
		java.util.List inputs = rxtInstance.getAttributeValuesList("input");
		if (inputs != null)
			positiveEntities.addAll(inputs);
		java.util.List catalysts = getCatalysts(rxtInstance);
		if (catalysts != null && catalysts.size() > 0)
			positiveEntities.addAll(catalysts);
		//java.util.List activators = getPositiveRegulator(rxtInstance);
		//if (activators != null && activators.size() > 0)
		//	positiveEntities.addAll(activators);
		for (Iterator it = positiveEntities.iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance)it.next();
			if (entity.getSchemClass().isa("Complex")) {
				double[] complexData = computeComplexData(entity, dataLength, loadedDataMap);
				if (complexData != null) {
					for (int i = 0; i < reactionData.length; i++)
						reactionData[i] += complexData[i];
					c++;
				}
			}
			else if (entity.getSchemClass().isa("EntityWithAccessionedSequence")) {
			    GKInstance dbID = (GKInstance) entity.getAttributeValue("referenceEntity");
			    if (dbID != null) {
			        double[] entityData = getDataForProtein(dbID, loadedDataMap);
			        if (entityData != null) {
			            for (int i = 0; i < dataLength; i++)
			                reactionData[i] += entityData[i];
			            c ++;
			        }
			    }
			}
		}
		if (c > 1) { // Average
			for (int i = 0; i < reactionData.length; i++) {
				reactionData[i] /= c;
			}
		}
		if (c > 0)
			return reactionData;
		else
			return null;
	}
	
	private java.util.List getCatalysts(GKInstance reaction) throws Exception {
		java.util.List list = reaction.getAttributeValuesList("catalystActivity");
		if (list == null || list.size() == 0)
			return null;
		java.util.List catalysts = new ArrayList();
		for (Iterator it = list.iterator(); it.hasNext();) {
			GKInstance ca = (GKInstance) it.next();
			GKInstance catalyst = (GKInstance) ca.getAttributeValue("physicalEntity");
			if (catalyst != null)
				catalysts.add(catalyst);
		}
		return catalysts;
	}
	
	private java.util.List getPositiveRegulator(GKInstance reaction) throws Exception {
		java.util.List regulations = reaction.getAttributeValuesList("regulation");
		if (regulations == null || regulations.size() == 0)
			return null;
		java.util.List regulators = new ArrayList();
		for (Iterator it = regulations.iterator(); it.hasNext();) {
			GKInstance regulation = (GKInstance) it.next();
			if (regulation.getSchemClass().isa("PositiveRegulation")) {
				GKInstance regulator = (GKInstance) regulation.getAttributeValue("regulator");
				if (regulator.getSchemClass().isa("PhysicalEntity"))
					regulators.add(regulator);
			}
		}
		return regulators;
	}
    
    private double[] getDataForProtein(GKInstance refInstance, Map loadedDataMap) throws Exception {
        Set ids = new HashSet();
        if (refInstance.getSchemClass().isValidAttribute("identifier")) {
            List list = refInstance.getAttributeValuesList("identifier");
            if (list != null)
                ids.addAll(list);
        }
        if (refInstance.getSchemClass().isValidAttribute("secondaryIdentifier")) {
            List list = refInstance.getAttributeValuesList("secondaryIdentifier");
            if (list != null)
                ids.addAll(list);
        }
        if (refInstance.getSchemClass().isValidAttribute("variantIdentifier")) {
            List list = refInstance.getAttributeValuesList("variantIdentifier");
            if (list != null)
                ids.addAll(list);
        }
        String id = null;
        for (Iterator it = ids.iterator(); it.hasNext();) {
            id = (String) it.next();
            double[] entityData = (double[]) loadedDataMap.get(id);
            if (entityData != null) {
                return entityData;
            }
        }
        return null;
    }
	
	protected double[] computeComplexData(GKInstance complex, int dataLength, Map loadedDataMap) throws Exception {
		java.util.List components = complex.getAttributeValuesList("hasComponent");
		if (components == null || components.size() == 0)
			return null;
        double[] complexData = new double[dataLength];
        int c = 0;
		for (Iterator it = components.iterator(); it.hasNext();) {
			GKInstance comp = (GKInstance) it.next();
			if (comp.getSchemClass().isa("EntityWithAccessionedSequence")) {
			    GKInstance dbID = (GKInstance) comp.getAttributeValue("referenceEntity");
			    if (dbID != null) {
			        double[] entityData = getDataForProtein(dbID, loadedDataMap);
			        if (entityData != null) {
			            for (int i = 0; i < dataLength; i++)
			                complexData[i] += entityData[i];
			            c ++;
			        }
			    }
			}
			else if (comp.getSchemClass().isa("Complex")) {
				double[] data = computeComplexData(comp, dataLength, loadedDataMap);
				if (data != null) {
					for (int i = 0; i < dataLength; i++) {
						complexData[i] += data[i];
					}
					c ++;
				}
			}
		}
		if (c > 1) {
			for (int i = 0; i < dataLength; i++)
				complexData[i] /= c;
		}
		if (c == 0)
			return null;
		else
			return complexData;
	}
	
	public java.util.List getDataPoints() {
		return dataPoints;
	}
	
	public void setColorableEdge(java.util.List edges) {
		this.edges = edges;
	}
	
	public void setDataIndex(int index) {
		if (preIndex == index)
			return;
		IEdge edge = null;
		for (Iterator it = dataMap.keySet().iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			double[] data = (double[]) dataMap.get(edge);
			double value = data[index];
			float rValue = (float) (value + 2.0 / 4.0);
			if (rValue > 1.0f)
				rValue = 1.0f;
			if (rValue < 0.0f)
				rValue = 0.0f;
			Color c = new Color(rValue, 1.0f - rValue, 0.0f);
			edge.setColor(c);
		}
		preIndex = index;	
	}
	
	public void resetEdges() {
		IEdge edge = null;
		Color c = null;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			c = (Color) originalColorMap.get(edge);
			edge.setColor(c);
		}
	}
	
	public int getMajorTickSpacing() {
		return 1;
	}
	
	public boolean isDataPointsUsedForLabels() {
		return true;
	}
}
