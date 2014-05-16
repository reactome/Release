/*
 * Created on Aug 28, 2007
 *
 */
package org.reactome.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.gk.database.util.MODReactomeAnalyzer;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;

/**
 * This test class is used to run MODReactomeAnalyzer class to generate a Reactome curator tool project that
 * can be merged into gk_central.
 * @author wgm
 */
public class MODReactomeMergetTest extends TestCase {
    
    /**
     * Reset the dirty flag based on two IE instances provided by Esther.
     * @throws Exception
     */
    public void resetDirtyFlagsForEstherIE() throws Exception {
        String dirName = "/Users/guanming/Documents/gkteam/Esther/";
        String fixedFileName = dirName + "fixed_drosophila_merger_20071217.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        fileAdaptor.setSource(fixedFileName);
        // All instances referred by these two IEs should be flag as dirty
        Long[] ieIds = new Long[] {
                new Long(209522),
                new Long(209524)
        };
        for (int i = 0; i < ieIds.length; i++) {
            GKInstance ie = fileAdaptor.fetchInstance(ieIds[i]);
            Set referrers = new HashSet();
            Collection c = ie.getReferers(ReactomeJavaConstants.modified);
            if (c != null)
                referrers.addAll(c);
            c = ie.getReferers(ReactomeJavaConstants.created);
            if (c != null)
                referrers.addAll(c);
            for (Iterator it = referrers.iterator();it.hasNext();) {
                GKInstance referrer = (GKInstance) it.next();
                referrer.setIsDirty(false);
            }
        }
        fileAdaptor.save(dirName + "fixed_fixed_drosophila_merger_20071217.rtpj");        
    }
    
    /**
     * Did a project fix based on two InstanceEdit instances.
     * @throws Exception
     */
    public void projectFixForEstherIE() throws Exception {
        String dirName = "/Users/guanming/Documents/gkteam/Esther/";
        String sourceFileName = dirName + "drosophila_merger_20071217.rtpj";
        String fixedFileName = dirName + "fixed_drosophila_merger_20071217.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        fileAdaptor.setSource(sourceFileName);
        // All instances referred by these two IEs should be flag as dirty
        Long[] ieIds = new Long[] {
                new Long(187679),
                new Long(187684)
        };
        for (int i = 0; i < ieIds.length; i++) {
            GKInstance ie = fileAdaptor.fetchInstance(ieIds[i]);
            ie.setDBID(new Long(-ieIds[i].longValue()));
            ie.setIsDirty(true);
            Set referrers = new HashSet();
            Collection c = ie.getReferers(ReactomeJavaConstants.modified);
            if (c != null)
                referrers.addAll(c);
            c = ie.getReferers(ReactomeJavaConstants.created);
            if (c != null)
                referrers.addAll(c);
            for (Iterator it = referrers.iterator();it.hasNext();) {
                GKInstance referrer = (GKInstance) it.next();
                referrer.setIsDirty(true);
            }
        }
        fileAdaptor.save(fixedFileName);
    }
    
    /**
     * This method is used to count the instances from a curated Reactome MOD database.
     * @throws Exception
     */
    public void testCheckMODInstances() throws Exception {
        // MOD database
        MySQLAdaptor mod = new MySQLAdaptor("localhost",
                                            "drosophila_reactome",
                                            "root",
                                            "macmysql01",
                                            3306);
        MODReactomeAnalyzer merger = new MODReactomeAnalyzer();
        merger.setModReactome(mod);
        long time1 = System.currentTimeMillis();
        merger.checkMODInstances();
        long time2 = System.currentTimeMillis();
        System.out.println("Time for checking: " + (time2 - time1));
        Set newInstances = merger.getNewMODInstances();
        Set changedInstances = merger.getChangedMODInstances();
        System.out.println("New Instances in MOD: " + newInstances.size());
        System.out.println("Changed Instances in MOD: " + changedInstances);
        // Want to get new pathways
        List newPathways = new ArrayList();
        for (Iterator it = newInstances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                newPathways.add(instance);
            }
        }
        System.out.println("Total new pathways: " + newPathways.size());
        // To exclude pathways induced by Esther's script
        // Get this IEE first
        Collection c = mod.fetchInstanceByAttribute(ReactomeJavaConstants.EvidenceType, 
                                                    ReactomeJavaConstants.name, 
                                                    "=", 
                                                    "inferred by electronic annotation");
        GKInstance iea = null;
        if (c != null)
            iea = (GKInstance) c.iterator().next();
        int count = 1;
        for (Iterator it = newPathways.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            GKInstance evidenceType = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.evidenceType);
            if (evidenceType != null && evidenceType == iea)
                continue; // Exclude these pathways since they are from predicated pathways
            // However, if these instances have been modified, actually they should be picked up!
            System.out.println(count + ": " + pathway);
            count ++;
        }
    }
    
    /**
     * This method is used to generate a curator tool project from a curated MOD Reactome
     * database.
     * @throws Exception
     */
    public void testCheckOutAsProject() throws Exception {
        // Set up the local adaptor
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        // MOD database
        MySQLAdaptor mod = new MySQLAdaptor("localhost",
                                            "drosophila_reactome",
                                            "root",
                                            "macmysql01",
                                            3306);
        MODReactomeAnalyzer merger = new MODReactomeAnalyzer();
        merger.setModReactome(mod);
        long time1 = System.currentTimeMillis();
        merger.checkMODInstances();
        long time2 = System.currentTimeMillis();
        System.out.println("Time for checking: " + (time2 - time1));
        Set newInstances = merger.getNewMODInstances();
        Set changedInstances = merger.getChangedMODInstances();
        System.out.println("New Instances in MOD: " + newInstances.size());
        System.out.println("Changed Instances in MOD: " + changedInstances.size());
        merger.checkOutAsNewProject();
        long time3 = System.currentTimeMillis();
        System.out.println("Check out: " + (time3 - time2));
        fileAdaptor.save("/Users/guanming/Documents/gkteam/Esther/Drosophila_1.rtpj");
    }
    
}
