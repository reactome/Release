/*
 * Created on Mar 28, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.gk.pathwaylayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidClassException;

/**
 * @author vastrik
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SpeciesTopLevelPathways {

	private MySQLAdaptor dba;
	private String dotPath = "/usr/local/bin/dot";
	/**
	 * 
	 */
	public SpeciesTopLevelPathways(MySQLAdaptor dba, String dotPath) {
		this.dba = dba;
		if (dotPath != null) {
			this.dotPath = dotPath;
		}
	}
	
	public Collection getTopLevelPathwaysForSpecies(GKInstance species) throws Exception {
		Collection spc = new ArrayList();
		spc.add(species);
		return getTopLevelPathwaysForSpecies(spc);
	}

	public Collection getTopLevelPathwaysForSpecies(Collection species) throws Exception {
		ArrayList qr = new ArrayList();
		qr.add(dba.createAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.species, "=", species));
		qr.add(dba.createReverseAttributeQueryRequest(ReactomeJavaConstants.Pathway, ReactomeJavaConstants.hasEvent, "IS NULL", null));
		return dba.fetchInstance(qr);
	}
	
	public Collection getTopLevelPathwaysForSpecies(String speciesName) throws Exception {
		Collection species = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Species,ReactomeJavaConstants.name,"=",speciesName);
		return getTopLevelPathwaysForSpecies(species);
	}

	public void createAndStorePathwayDiagramForPathways(Collection pathways) throws Exception {
		GraphvizDotGenerator generator = new GraphvizDotGenerator();
		generator.setEntitiesDisplayedAsMultipleNodes(dba,null);
		for(Iterator i = pathways.iterator(); i.hasNext();) {
			GKInstance pathway = (GKInstance)i.next();
			System.out.println("Now handling " + pathway);
			String dotString = generator.generatePathwayDiagramInFormat(pathway,dotPath,"dot");
			if (dotString == null)
				continue;
			generator.createAndStorePathwayDiagram(pathway, dba, dotString);
			generator.reset();
		}
	}
	
	public void createAndStorePathwayDiagramsForSpecies(String species) throws Exception {
		Collection pathways = getTopLevelPathwaysForSpecies(species);
		createAndStorePathwayDiagramForPathways(pathways);
	}
	
	public static void main(String[] args) {
		if (args.length < 6) {
			System.out.println("Usage java org.gk.pathwaylayout.GraphvizDotGenerator " +
			"dbHost dbName dbUser dbPwd dbPort 'species name' dotPath");
			System.exit(0);
		}
		try {
			MySQLAdaptor dba = new MySQLAdaptor(
				args[0],
				args[1],
				args[2],
				args[3],
				Integer.parseInt(args[4]));
			String dotPath = null;
			if (args.length == 7 && !args[6].equals("")) {
				dotPath = args[6];
			}
			SpeciesTopLevelPathways stpl = new SpeciesTopLevelPathways(dba, dotPath);
			stpl.createAndStorePathwayDiagramsForSpecies(args[5]);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
