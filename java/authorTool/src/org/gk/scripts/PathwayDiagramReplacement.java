/*
 * Created on May 19, 2010
 *
 */
package org.gk.scripts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.gk.database.AttributeEditConfig;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.BrowserLauncher;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * This class is used to replace PathwayDiagrams from one database to another database.
 * @author wgm
 *
 */
public class PathwayDiagramReplacement {
    private MySQLAdaptor sourceDba;
    private MySQLAdaptor targetDba;
    
    public PathwayDiagramReplacement() {
    }
    
    public void setSourceDBA(MySQLAdaptor dba) {
        this.sourceDba = dba;
    }
    
    public void setTargetDBA(MySQLAdaptor dba) {
        this.targetDba = dba;
    }
    
    public void replace() throws Exception {
        cleanUpDiagramsInTarget();
        List<GKInstance> diagrams = loadDiagramsFromSource();
        cleanUpDiagramsFromSource(diagrams);
        saveDiagramsToTarget(diagrams);
    }
    
    private void cleanUpDiagramsInTarget() throws Exception {
        // Delete all Pathway diagrams in the target database
        Collection instances = targetDba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        if (instances == null || instances.size() == 0)
            return;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            targetDba.deleteInstance(inst);
        }
    }
    
    private List<GKInstance> loadDiagramsFromSource() throws Exception {
        Collection instances = sourceDba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        List<GKInstance> list = new ArrayList<GKInstance>();
        if (instances != null && instances.size() > 0) {
            for (Iterator it = instances.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                list.add(inst);
            }
        }
        return list;
    }
    
    private void cleanUpDiagramsFromSource(List<GKInstance> diagrams) throws Exception {
        // Just want to make things simpler: remove both modified and created IEs
        for (GKInstance inst : diagrams) {
            sourceDba.fastLoadInstanceAttributeValues(inst);
            inst.setAttributeValue(ReactomeJavaConstants.created, null);
            inst.setAttributeValue(ReactomeJavaConstants.modified, null);
            // Flip the DB_Ids to avoid DB_Ids are used in other instances
            inst.setDBID(-inst.getDBID());
        }
    }
    
    private void saveDiagramsToTarget(List<GKInstance> diagrams) throws Exception {
        targetDba.storeLocalInstances(diagrams);
    }
    
    @Test
    public void runReplace() throws Exception {
        MySQLAdaptor source = new MySQLAdaptor("localhost",
                                               "test_slice_1000",
                                               "root",
                                               "macmysql01");
        MySQLAdaptor target = new MySQLAdaptor("localhost",
                                               "test_reactome_1000",
                                               "root",
                                               "macmysql01");
        setSourceDBA(source);
        setTargetDBA(target);
        replace();
    }
    
    @Test
    public void checkDeployedELVs() throws IOException {
        String fileName = "/Users/wgm/Desktop/EVLDirectories.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        Set<String> bakFiles = new HashSet<String>();
        Set<String> deployedFiles = new HashSet<String>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split(" ");
            String file = tokens[tokens.length - 1];
            if (file.endsWith(".bak")) {
                bakFiles.add(file);
            }
            else
                deployedFiles.add(file);
        }
        fu.close();
        System.out.println("Total baked files: " + bakFiles.size());
        System.out.println("Total deployed files: " + deployedFiles.size());
        // Check if any  baked file is in in the deployed set
        Set<String> toBeDeleted = new HashSet<String>();
        for (String file : bakFiles) {
            int index = file.indexOf(".");
            String tmp = file.substring(0, index);
            if (deployedFiles.contains(tmp))
                toBeDeleted.add(file);
        }
        System.out.println("To be deleted: " + toBeDeleted.size());
        bakFiles.removeAll(toBeDeleted);
        System.out.println();;
        for (String file : bakFiles) {
            int index = file.indexOf(".");
            System.out.print(file.substring(0, index) + ", ");
        }
    }
    
    @Test
    public void batchDeployELVs() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "gk_central",
                                            "authortoor",
                                            "T001test");
        String idText = "174143, 196854, 194068, 453279, 376176, 418592, 76002, " +
        		"375165, 73923, 453274, 375170, 69620, 373760, 199991, 202131, " +
        		"162906, 425397, 201451, 211859, 373752, 191273, 71387, 392170, " +
        		"556833, 525793, 379724, 535734, 168254, 453277, 428157, 209962, " +
        		"373755, 69242";
        String[] tokens = idText.split(", ");
        System.out.println("Total ids: " + tokens.length);
        int count = 0;
        final String serviceUrl = "http://reactomedev.oicr.on.ca:8080/ELVWebApp/ElvService";
        System.out.println("Service URL: " + serviceUrl);
        for (String id : tokens) {
            System.out.println("Deploying " + id + "...");
            Long dbId = new Long(id);
            GKInstance pathway = dba.fetchInstance(dbId);
            System.out.println("Pathway: " + pathway);
            Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram,
                                                           ReactomeJavaConstants.representedPathway,
                                                           "=",
                                                           pathway);
            if (c.size() != 1) {
                System.out.println(pathway + " has more than one diagram!");
                continue;
            }
            GKInstance diagram = (GKInstance) c.iterator().next();
            URL url = new URL(serviceUrl);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            Map<String, Object> info = new HashMap<String, Object>();
            info.put("pdId", diagram.getDBID());
            GKInstance latestIE = InstanceUtilities.getLatestIEFromInstance(diagram);
            info.put("pdIE", latestIE.getDBID());
            // Use char array for some security
            info.put("user", dba.getDBUser().toCharArray());
            info.put("dbName", dba.getDBName().toCharArray());
            oos.writeObject(info);
            oos.close();
            os.close();
            // Now waiting for reply from the server
            InputStream is = connection.getInputStream();
            // Get the response
            BufferedReader bd = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = bd.readLine()) != null) {
                builder.append(line).append("\n");
            }
            bd.close();
            is.close();
            String message = builder.toString();
            if (message.startsWith("The selected pathway diagram has been deployed successfully!")) {
                System.out.println(message);
                count ++;
            }
            else {
                System.out.println("Cannot deploy: " + message);
            }
        }
        dba.cleanUp();
        System.out.println("Deployed diagrams: " + count);
    }
}
