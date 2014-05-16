/*
 * Created on Mar 11, 2010
 *
 */
package org.gk.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.FileUtilities;
import org.junit.Test;


/**
 * This script is used to list human UniProt in gk_central that have not been used for Lisa.
 * @author wgm
 *
 */
public class UnusedUniProtLister {
    
    public UnusedUniProtLister() {
    }
    
    /**
     * Used to help Lisa to merge files.
     * @throws IOException
     */
    @Test
    public void mergeFile() throws IOException {
        String dirName = "/Users/wgm/Documents/gkteam/Lisa/";
        String fileName1 = dirName + "GO_and_UnUsed_FIScores_042010.txt";
        String fileName2 = dirName + "Uncurated_prioritized_GO_list_042010.txt";
        String fileName3 = dirName + "Prioritized_uncurated_w_GOterms_042010.txt";
        String outFileName = dirName + "Unused_FIScores_GO_Number_List_042010.txt";
        Map<String, String[]> idToInfo = new HashMap<String, String[]>();
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName2);
        String line = fu.readLine();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            String[] info = new String[3];
            info[0] = tokens[0];
            info[1] = tokens[1];
            info[2] = tokens[3];
            idToInfo.put(tokens[2], info);
        }
        fu.close();
        fu.setInput(fileName1);
        line = fu.readLine();
        FileUtilities outFu = new FileUtilities();
        outFu.setOutput(outFileName);
        StringBuilder builder = new StringBuilder();
        builder.append("Identifier\tSymbol\tName\tFI Partners\tAnnotated FI Partners\tRanker Score\tGO Term Number");
        outFu.printLine(builder.toString());
        builder.setLength(0);
        Map<String, String[]> idToFIInfo = new HashMap<String, String[]>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            String[] info1 = new String[3];
            info1[0] = tokens[2];
            info1[1] = tokens[3];
            info1[2] = tokens[4];
            idToFIInfo.put(tokens[0], info1);
            builder.append(tokens[0]).append("\t").append(tokens[1]).append("\t");
            String[] info = idToInfo.get(tokens[0]);
            if (info != null) {
                builder.append(info[1]);
            }
            builder.append("\t").append(tokens[2]).append("\t").append(tokens[3]).append("\t").append(tokens[4]).append("\t");
            if (info != null)
                builder.append(info[2]);
            outFu.printLine(builder.toString());
            builder.setLength(0);
        }
        outFu.close();
        fu.close();
        outFileName = dirName + "Prioritized_uncurated_w_GOterms_FI_Scores_042010.txt";
        outFu.setOutput(outFileName);
        fu.setInput(fileName3);
        line = fu.readLine();
        outFu.printLine(line + "\tFI Partners\tAnnotated FI Partners\tRanker Scores");
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            String[] info = idToFIInfo.get(tokens[2]);
            if (info != null) {
                outFu.printLine(line + "\t" + 
                                info[0] + "\t" +
                                info[1] + "\t" +
                                info[2]);
            }
            else
                outFu.printLine(line);
        }
        fu.close();
        outFu.close();
        
    }
    
    /**
     * This method is used to generate scores for annotation candidatance.
     * @throws Exception
     */
    @Test
    public void generateFIScoresForUnUsedUniProtIds() throws Exception {
        MySQLAdaptor dba = getDBA();
        // Check UniProt only
        GKInstance uniprot = dba.fetchInstance(2L);
        Collection refGeneProducts = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                                  ReactomeJavaConstants.referenceDatabase,
                                                                  "=",
                                                                  uniprot);
        // Remove non-human proteins
        for (Iterator it = refGeneProducts.iterator(); it.hasNext();) {
            GKInstance refGene = (GKInstance) it.next();
            GKInstance species = (GKInstance) refGene.getAttributeValue(ReactomeJavaConstants.species);
            if (species == null || !species.getDBID().equals(48887L))
                it.remove();
        }
        System.out.println("Total human ReferenceGeneProduct: " + refGeneProducts.size());
        // Check if a Uniprot is used as ReferenceEntity in EWAS
        SchemaClass ewasCls = dba.getSchema().getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
        SchemaAttribute refEntAtt = ewasCls.getAttribute(ReactomeJavaConstants.referenceEntity);
        Set<GKInstance> usedRefGenes = new HashSet<GKInstance>();
        Set<GKInstance> notUsedRefGenes = new HashSet<GKInstance>();
        Set<String> allIdentifiers = new HashSet<String>();
        Set<String> usedIdentifiers = new HashSet<String>();
        // Parse name 
        Map<String, String> identifer2Name = new HashMap<String, String>();
        int index = 0;
        for (Iterator it = refGeneProducts.iterator(); it.hasNext();) {
            GKInstance refGene = (GKInstance) it.next();
            String identifier = (String) refGene.getAttributeValue(ReactomeJavaConstants.identifier);
            allIdentifiers.add(identifier);
            String name = refGene.getDisplayName();
            index = name.indexOf(" ");
            name = name.substring(index + 1);
            identifer2Name.put(identifier, name);
            Collection referrers = refGene.getReferers(refEntAtt);
            if (referrers != null && referrers.size() > 0) {
                usedRefGenes.add(refGene);
                usedIdentifiers.add(identifier);
            }
            else
                notUsedRefGenes.add(refGene);
        }
        System.out.println("Human ReferenceGeneProduct has been used: " + usedRefGenes.size());
        System.out.println("Human ReferenceGeneProduct has not been used in EWAS referenceEntity: " + notUsedRefGenes.size());
        // Sometimes only isoforms are used. It is more accurate to remove used identifiers from all identifiers to
        // get the list of identifiers that are not used.
        System.out.println("UniProt accessions have been used: " + usedIdentifiers.size());
        allIdentifiers.removeAll(usedIdentifiers);
        Set<String> notUsedIdentifiers = allIdentifiers;
        System.out.println("UniProt accessions have not be used: " + notUsedIdentifiers.size());
        
        String outFileName = "/Users/wgm/Documents/gkteam/Lisa/UnUsedUniProts031110_FIScores.txt";
        FileUtilities fu = new FileUtilities();
        fu.setOutput(outFileName);
        fu.printLine("Identifier\tName\tFI Partners\tAnnotated FI Partners\tRanker Scores");
        Map<String, Set<String>> idToPartners = loadFIs();
        Map<String, Double> geneToRanker = loadGeneRankers();
        for (String id : notUsedIdentifiers) {
            Set<String> partners = idToPartners.get(id);
            String name = identifer2Name.get(id);
            if (partners == null) {
//                fu.printLine(id + "\t" +
//                             name + "\t" +
//                             "NA\t" + 
//                             "NA\t" + 
//                             "");
                continue;
            }
            Set<String> copy = new HashSet<String>(partners);
            copy.retainAll(usedIdentifiers);
            Double ranker = geneToRanker.get(name);
            fu.printLine(id + "\t" +
                         name + "\t" +
                         partners.size() + "\t" + 
                         copy.size() + "\t" + 
                         (ranker == null ? "NA" : ranker));
        }
        fu.close();
    }
    
    /**
     * Load a list of genes ranked by MSKCC method (downloaded from 
     * http://awabi.cbio.mskcc.org/tcga_gbm/jsp/index.jsp?filer_type=by_score&score_threshold=1).
     * @return
     * @throws IOException
     */
    private Map<String, Double> loadGeneRankers() throws IOException {
        String fileName = "/Users/wgm/datasets/TCGA/GBM/Gene_Ranker_All_Fixed.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        Map<String, Double> geneToScore = new HashMap<String, Double>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            String gene = tokens[2];
            Double score = new Double(tokens[1]);
            geneToScore.put(gene, score);
        }
        fu.close();
        return geneToScore;
    }

    private Map<String, Set<String>> loadFIs() throws IOException {
        String fiFileName = "/Users/wgm/Documents/EclipseWorkspace/caBigR3/results/v3/FIs_043009.txt";
        FileUtilities fu = new FileUtilities();
        // Load FIs
        fu.setInput(fiFileName);
        String line = null;
        int index = 0;
        Map<String, Set<String>> idToPartners = new HashMap<String, Set<String>>();
        while ((line = fu.readLine()) != null) {
            index = line.indexOf(" ");
            String id1 = line.substring(0, index);
            String id2 = line.substring(index + 1);
            Set<String> partners1 = idToPartners.get(id1);
            if (partners1 == null) {
                partners1 = new HashSet<String>();
                idToPartners.put(id1, partners1);
            }
            partners1.add(id2);
            Set<String> partners2 = idToPartners.get(id2);
            if (partners2 == null) {
                partners2 = new HashSet<String>();
                idToPartners.put(id2, partners2);
            }
            partners2.add(id1);
        }
        fu.close();
        return idToPartners;
    }
    
    @Test
    public void list() throws Exception {
        MySQLAdaptor dba = getDBA();
        // Check UniProt only
        GKInstance uniprot = dba.fetchInstance(2L);
        Collection refGeneProducts = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                                  ReactomeJavaConstants.referenceDatabase,
                                                                  "=",
                                                                  uniprot);
        // Remove non-human proteins
        for (Iterator it = refGeneProducts.iterator(); it.hasNext();) {
            GKInstance refGene = (GKInstance) it.next();
            GKInstance species = (GKInstance) refGene.getAttributeValue(ReactomeJavaConstants.species);
            if (species == null || !species.getDBID().equals(48887L))
                it.remove();
        }
        System.out.println("Total human ReferenceGeneProduct: " + refGeneProducts.size());
        // Check if a Uniprot is used as ReferenceEntity in EWAS
        SchemaClass ewasCls = dba.getSchema().getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
        SchemaAttribute refEntAtt = ewasCls.getAttribute(ReactomeJavaConstants.referenceEntity);
        Set<GKInstance> used = new HashSet<GKInstance>();
        for (Iterator it = refGeneProducts.iterator(); it.hasNext();) {
            GKInstance refGene = (GKInstance) it.next();
            Collection referrers = refGene.getReferers(refEntAtt);
            if (referrers != null && referrers.size() > 0) {
                it.remove();
                used.add(refGene);
            }
        }
        System.out.println("Human ReferenceGeneProduct has not been used in EWAS referenceEntity: " + refGeneProducts.size());
        // Generate out for this list.
//        String outFileName = "/Users/wgm/Documents/gkteam/Lisa/UnUsedUniProts040910.txt";
//        String outFileName = "/Users/gwu/Documents/gkteam/Lisa/UnUsedUniProts111511.txt";
        String outFileName = "/Users/gwu/Documents/gkteam/Lisa/UnUsedUniProts121511.txt";
        FileUtilities fu = new FileUtilities();
        fu.setOutput(outFileName);
        fu.printLine("List of human ReferenceGeneProduct instances that have not been used by EWAS referenceEntity:");
        fu.printLine("DB_ID\tName\tAccession");
        List list = new ArrayList(refGeneProducts);
        InstanceUtilities.sortInstances(list);
        Set<String> accessions = new HashSet<String>();
        for (Iterator it = list.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            String accession = (String) inst.getAttributeValue(ReactomeJavaConstants.identifier);
            accessions.add(accession);
            fu.printLine(inst.getDBID() + "\t" + inst.getDisplayName() + "\t" + accession);
        }
        // Some identifiers may be used by isoforms that share identifiers. These identifiers should be removed
        System.out.println("Before cleaning: " + accessions.size());
        for (GKInstance refGene : used) {
            String accession = (String) refGene.getAttributeValue(ReactomeJavaConstants.identifier);
            accessions.remove(accession);
        }
        System.out.println("After cleaning: " + accessions.size());
        List<String> accessionList = new ArrayList<String>(accessions);
        Collections.sort(accessionList);
        // Output accessions that have not been used.
        fu.printLine("\nList of UniProt accessions that have not been used:");
        for (String acc : accessionList)
            fu.printLine(acc);
        fu.close();
        System.out.println("Total accession not used: " + accessions.size());
    }
    
    private MySQLAdaptor getDBA() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central_121511",
                                            "root", 
                                            "macmysql01");
        return dba;
    }
    
}
