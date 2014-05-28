/*
 * Created on Jan 21, 2005
 *
 */
package org.gk.scripts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/**
 * @author wgm
 * A class to use a map from Gene to Complex in Reactome.
 */
public class GeneInComplexExtracter {
	// A list of checked complexes: Key: GKInstance for complex
	// Value: HashSet of GKInstances for Genes
	private Map touchedComplex = new HashMap();
	private Map proteinMap = new HashMap();
	private Map geneMap = new HashMap();
	
	public GeneInComplexExtracter() {		
	}
	
	public void extractComplexList() {
		try {
			MySQLAdaptor adaptor = new MySQLAdaptor("brie8.cshl.org",
					"gk_central", "authortool", "T001test");
			Collection instances = adaptor.fetchInstanceByAttribute("Species",
					"_displayName", "=", "homo sapiens");
			GKInstance human = (GKInstance) instances.iterator().next();
			Collection complexes = adaptor.fetchInstanceByAttribute("Complex",
					"taxon", "=", human);
			StringBuffer buffer = new StringBuffer();
			for (Iterator it = complexes.iterator(); it.hasNext();) {
				GKInstance complex = (GKInstance) it.next();
				List subunits = complex.getAttributeValuesList("hasComponent");
				buffer.append(complex.getDisplayName());
				buffer.append("(");
				buffer.append(complex.getDBID());
				buffer.append(")");
				buffer.append("\t");
				for (Iterator it1 = subunits.iterator(); it1.hasNext();) {
					GKInstance subunit = (GKInstance) it1.next();
					if (!subunit.getSchemClass().isa("Complex"))
						continue;
					buffer.append(subunit.getDisplayName());
					buffer.append("(");
					buffer.append(subunit.getDBID());
					buffer.append(")\t");
				}
				if (it.hasNext())
					buffer.append("\n");
				
			}
			output(buffer, "complexList.txt");
			System.out.println(buffer.toString());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param buffer
	 * @throws IOException
	 */
	private void output(StringBuffer buffer, String fileName) throws IOException {
		FileWriter writer = new FileWriter(fileName);
		BufferedWriter bWriter = new BufferedWriter(writer);
		bWriter.write(buffer.toString());
		bWriter.close();
		writer.close();
	}

	public void extract() {
		try {
			MySQLAdaptor adaptor = new MySQLAdaptor("brie8.cshl.org",
					"gk_central", "authortool", "T001test");
			Collection instances = adaptor.fetchInstanceByAttribute("Species",
					"_displayName", "=", "homo sapiens");
			GKInstance human = (GKInstance) instances.iterator().next();
			Collection complexes = adaptor.fetchInstanceByAttribute("Complex",
					"taxon", "=", human);
			int c = 0;
			for (Iterator it = complexes.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				extract(instance);
				c++;
				System.out.println("Instance " + c + ": "
						+ instance.getDisplayName());
			}
			System.out.println("Total complexes: " + c);
			for (Iterator it = touchedComplex.keySet().iterator(); it.hasNext();) {
				GKInstance complex = (GKInstance) it.next();
				Set set = (Set) touchedComplex.get(complex);
				for (Iterator it1 = set.iterator(); it1.hasNext();) {
					GKInstance subunit = (GKInstance) it1.next();
					Set complexes1 = (Set) proteinMap.get(subunit);
					if (complexes1 == null) {
						complexes1 = new HashSet();
						proteinMap.put(subunit, complexes1);
					}
					complexes1.add(complex);
				}
			}
			for (Iterator it = proteinMap.keySet().iterator(); it.hasNext();) {
				GKInstance subunit = (GKInstance) it.next();
				// Check if this subunit has ENSEML associated
				GKInstance ensemblGene = getENSEMLId(subunit);
				if (ensemblGene == null)
					continue;
				Set complexes1 = (Set) proteinMap.get(subunit);
				Set complexes2 = (Set) geneMap.get(ensemblGene);
				if (complexes2 == null) {
					geneMap.put(ensemblGene, new HashSet(complexes1));
				}
				else {
					complexes.addAll(complexes1);
					System.out.println("Merging");
				}
				//proteinToGeneMap.put(ensemblGene, subunit);
			}
			StringBuffer buffer = new StringBuffer();
			for (Iterator it = geneMap.keySet().iterator(); it.hasNext();) {
				GKInstance gene = (GKInstance) it.next();
				Set complexes1 = (Set) geneMap.get(gene);
				buffer.append(gene.getDisplayName() + "(" + gene.getDBID() + ")\t");
				for (Iterator it1 = complexes1.iterator(); it1.hasNext();) {
					GKInstance complex = (GKInstance) it1.next();
					buffer.append(complex.getDisplayName() + "(" + complex.getDBID() + ")");
					if (it1.hasNext())
						buffer.append("\t");
				}
				if (it.hasNext())
					buffer.append("\n");
			}
			output(buffer, "geneInComplex.txt");
			System.out.println("Total genes: " + geneMap.size());
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private GKInstance getENSEMLId(GKInstance protein) throws Exception {
		GKInstance referenceEntity = (GKInstance) protein.getAttributeValue("referenceEntity");
		if (referenceEntity == null)
			return null;
		if (!referenceEntity.getSchemClass().isValidAttribute("referenceGene"))
			return null;
		List referenceGenes = referenceEntity.getAttributeValuesList("referenceGene");
		if (referenceGenes == null || referenceGenes.size() == 0)
			return null;
		// Check if there is one ENSEMBL gene referred
		for (Iterator it = referenceGenes.iterator(); it.hasNext();) {
			GKInstance gene = (GKInstance) it.next();
			String displayName = gene.getDisplayName();
			if (displayName.startsWith("ENSEMBL:"))
				return gene;
		}
		return null;
	}
	
	private void extract(GKInstance complex) throws Exception {
		// It has already extracted
		if (touchedComplex.containsKey(complex))
			return;
		Set subunits = new HashSet();
		extract(complex, subunits);
		touchedComplex.put(complex, subunits);
	}
	
	private void extract(GKInstance complex, Set subunits) throws Exception {
		List components = complex.getAttributeValuesList("hasComponent");
		if (components == null || components.size() == 0)
			return;		
		for (Iterator it = components.iterator(); it.hasNext();) {
			GKInstance sub = (GKInstance) it.next();
			if (sub.getSchemClass().isa("Complex")) {
				extract(sub);
				Set set = (HashSet) touchedComplex.get(sub);
				subunits.addAll(set);
			}
			else
				subunits.add(sub);
		}
	}
	
	public static void main(String[] args) {
		GeneInComplexExtracter extracter = new GeneInComplexExtracter();
		//extracter.extractComplexList();
		extracter.extract();
	}
	
}
