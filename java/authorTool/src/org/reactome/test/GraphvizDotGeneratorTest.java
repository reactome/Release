/*
 * Created on Mar 16, 2006
 *
 */
package org.reactome.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.gk.gkCurator.GraphvizDotGenerator;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

public class GraphvizDotGeneratorTest extends TestCase {
    private MySQLAdaptor dba = null;
    private String dirName = "/Users/guanming/Documents/tmp/svgexporter/";
    
    public GraphvizDotGeneratorTest() {   
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        dba = new MySQLAdaptor("localhost",
                               "gk_current_ver22",
                               "root",
                               "macmysql01",
                               3306);
    }
    
    public void countReactions() throws Exception {
//        List topics = getTopics();
//        GraphvizDotGenerator generator = new GraphvizDotGenerator();
//        for (Iterator it = topics.iterator(); it.hasNext();) {
//            GKInstance pathway = (GKInstance) it.next();
//            Set reactions = generator.listReactions(pathway);
//            System.out.println(pathway.getDisplayName() + ": " + reactions.size());
//        }
    }
    
    private List getTopics() throws Exception {
        Collection frontPageItem = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
        GKInstance frontpage = (GKInstance) frontPageItem.iterator().next();
        List topics = frontpage.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);
        return topics;
    }
    
    public void testAll() throws Exception {
        GraphvizDotGenerator generator = new GraphvizDotGenerator();
        generator.setUseURL(true);
        List topics = getTopics();
        for (Iterator it = topics.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            if (!pathway.getDBID().equals(new Long(15869L)))
                continue;
            long time1 = System.currentTimeMillis();
            String dot = generator.generatePathwayDiagramInSVGForId(pathway,
                                                                    "http://www.reactome.org",
                                                                    "/Users/guanming/ProgramFiles/Graphviz_v16/Graphviz.app/Contents/MacOS/dot");
            output(dot, pathway, ".svg");
            long time2 = System.currentTimeMillis();
            System.out.println(pathway.toString() + ": " + (time2 - time1));
        }
    }
    
    public void testDotGenerator() throws Exception {
        GKInstance apoptosis = dba.fetchInstance(new Long(15869));
        GraphvizDotGenerator generator = new GraphvizDotGenerator();
        generator.setUseURL(true);
        String dot = generator.generateDot(apoptosis);
        output(dot, apoptosis, ".dot");
    }
    
    public void testPNGGeneration() throws Exception {
        long time1 = System.currentTimeMillis();
        GKInstance apoptosis = dba.fetchInstance(new Long(109581));
        GraphvizDotGenerator generator = new GraphvizDotGenerator();
        generator.setUseURL(true);
        File file = new File("Apoptosis.png");
        System.out.println("File: " + file.getAbsolutePath());
        String dotPath = "/usr/local/graphviz-2.12/bin/dot";
        generator.generatePathwayDiagramInPNG(apoptosis,
                                              dotPath,
                                              file);
        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1));
    }
    
    public void testAllDotGenerator() throws Exception {
        GraphvizDotGenerator generator = new GraphvizDotGenerator();
        generator.setUseURL(true);
        List topics = getTopics();
        for (Iterator it = topics.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            long time1 = System.currentTimeMillis();
            String dot = generator.generateDot(pathway);
            output(dot, pathway, ".dot");
            long time2 = System.currentTimeMillis();
            System.out.println(pathway.toString() + ": " + (time2 - time1));
        }
    }
    
    private void output(String dot, GKInstance pathway, String ext) throws IOException {
        FileWriter fileWriter = new FileWriter(dirName + pathway.getDisplayName() + ext);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.write(dot);
        printWriter.close();
        fileWriter.close();
    }
    
    public void testWebSites() throws Exception {
        String biopaxUrl = "http://www.reactome.org/cgi-bin/biopaxexporter?DB=gk_current&ID=";
        String svgUrl = "http://www.reactome.org/cgi-bin/svgexporter?DB=gk_current&ID=";
        List topics = getTopics();
        for (Iterator it = topics.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            URL url = new URL(biopaxUrl + pathway.getDBID());
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            String output = read(is);
            output(output, pathway, ".xml");
            url = new URL(svgUrl + pathway.getDBID());
            connection = url.openConnection();
            is = connection.getInputStream();
            output = read(is);
            output(output, pathway, ".svg");
        }
    }
    
    private String read(InputStream is) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null) {
            buffer.append(line);
            buffer.append("\n");
        }
        br.close();
        is.close();
        return buffer.toString();
    }

}
