/*
 * Created on Sep 6, 2005
 *
 */
package org.gk.scripts;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;

/**
 * A utility class is used to list display names in files for Complex, SimpleEntity, Reactions
 * and Pathways.
 * @author guanming
 *
 */
public class DisplayNameExtractor {
    private MySQLAdaptor dba = null;
    
    public DisplayNameExtractor(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    public void extract(String dirName) throws Exception {
        System.out.println("Starting extracting...");
        extract(dirName, "Complex");
        extract(dirName, "SimpleEntity");
        extract(dirName, "Reaction");
        extract(dirName, "Pathway");
        System.out.println("Extracting done!");
    }
    
    private void extract(String dirName, String clsName) throws Exception {
        System.out.println("Starting extracting " + clsName + " ...");
        List complexes = new ArrayList(dba.fetchInstancesByClass(clsName));
        InstanceUtilities.sortInstances(complexes);
        FileWriter writer = new FileWriter(dirName + File.separator + clsName + ".txt");
        PrintWriter printWriter = new PrintWriter(writer);
        GKInstance instance = null;
        for (Iterator it = complexes.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            printWriter.println(instance.getDisplayName());
        }
        printWriter.close();
        writer.close();
        System.out.println("Ending extracting " + clsName);
    }
    
    public static void main(String[] args) {
        try {
            MySQLAdaptor dba = new MySQLAdaptor("brie8.cshl.edu",
                                                "gk_central",
                                                "authortool",
                                                "T001test",
                                                3306);
            DisplayNameExtractor extractor = new DisplayNameExtractor(dba);
            extractor.extract("resources");
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
