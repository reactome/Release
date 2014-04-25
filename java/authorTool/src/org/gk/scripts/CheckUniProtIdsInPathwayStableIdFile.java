/*
 * Created on Dec 21, 2010
 *
 */
package org.gk.scripts;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;
import org.junit.Test;

public class CheckUniProtIdsInPathwayStableIdFile {
    
    @Test
    public void check() throws Exception {
        String targetFileName = "/Users/wgm/Desktop/uniprot_2_pathways.stid.txt";
        Set<String> targetIds = new HashSet<String>();
        FileUtilities fu = new FileUtilities();
        fu.setInput(targetFileName);
        String line = null;
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            targetIds.add(tokens[0]);
        }
        fu.close();
        System.out.println("Total ids in file: " + targetIds.size());
        // Get ids from the released database
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_current_ver35",
                                            "root", 
                                            "macmysql01");
        GKInstance human = dba.fetchInstance(48887L);
        Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                       ReactomeJavaConstants.species, 
                                                       "=", 
                                                       human);
        Set<String> dbIds = new HashSet<String>();
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            GKInstance refDb = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            if (!refDb.getDisplayName().equals("UniProt"))
                continue;
            String identifier = (String) inst.getAttributeValue(ReactomeJavaConstants.identifier);
            if (identifier != null)
                dbIds.add(identifier);
        }
        System.out.println("Total ids in DB: " + dbIds.size());
        Set<String> shared = new HashSet<String>(dbIds);
        shared.retainAll(targetIds);
        System.out.println("Shared ids: " + shared.size());
        dbIds.removeAll(targetIds);
        System.out.println("DB identifiers not in file: " + dbIds.size());
        for (String id : dbIds)
            System.out.println(id);;
    }
    
}
