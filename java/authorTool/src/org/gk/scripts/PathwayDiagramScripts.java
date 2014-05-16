/*
 * Created on Jun 6, 2011
 *
 */
package org.gk.scripts;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.PropertyConfigurator;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.pathwaylayout.PathwayDiagramXMLGenerator;
import org.gk.pathwaylayout.PredictedPathwayDiagramGeneratorFromDB;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.MySQLAdaptor;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderablePathway;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.slicing.PathwayDiagramSlicingHelper;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * @author wgm
 *
 */
public class PathwayDiagramScripts {
    
    public PathwayDiagramScripts() {
    }
    
    public static void main(String[] args) {
        if (args.length != 6) {
            System.err.println("Usage java -Xmx1024m org.gk.scripts.PathwayDiagramScripts dbHost dbName dbUser dbPwd diagramId imageBaseDir");
            System.exit(1);
        }
        PropertyConfigurator.configure("resources/log4j.properties");
        try {
            MySQLAdaptor dba = new MySQLAdaptor(args[0],
                                                args[1],
                                                args[2],
                                                args[3]);
            Long dbId = new Long(args[4]);
            PathwayDiagramScripts scripts = new PathwayDiagramScripts();
            scripts.fixDiagramsForRelease(dba, dbId, args[5]);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * This method is used to fix a wrong diagram for release 42.
     * @throws Exception
     */
    public void fixDiagramsForRelease(MySQLAdaptor dba,
                                      Long dbId,
                                      String imageBaseDir) throws Exception {
        // Need to get the original form from gk_central
        MySQLAdaptor gk_central = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                                   "gk_central",
                                                   "authortool",
                                                   "T001test");
        GKInstance gkCentralInst = gk_central.fetchInstance(dbId);
        DiagramGKBReader diagramReader = new DiagramGKBReader();
        RenderablePathway pathway = diagramReader.openDiagram(gkCentralInst);
        PathwayDiagramSlicingHelper slicingHelper = new PathwayDiagramSlicingHelper();
        slicingHelper.removeDoNotReleaseEvents(pathway, gkCentralInst, gk_central);
        String xml = (String) gkCentralInst.getAttributeValue(ReactomeJavaConstants.storedATXML);
        System.out.println(xml);
        
        // Do an update in the target DBA
        // Need to update the PathwayDigaram first
        GKInstance sourceInst = dba.fetchInstance(dbId);
        SchemaClass cls = dba.getSchema().getClassByName(ReactomeJavaConstants.PathwayDiagram);
        SchemaAttribute att = cls.getAttribute(ReactomeJavaConstants.storedATXML);
        sourceInst.setAttributeValue(att, xml);
        dba.updateInstanceAttribute(sourceInst, att);
        
        // Generate image files for the original diagram
        PredictedPathwayDiagramGeneratorFromDB diagramGenerator = new PredictedPathwayDiagramGeneratorFromDB();
        diagramGenerator.setImageBaseDir(imageBaseDir);
        diagramGenerator.setMySQLAdaptor(dba);
        diagramGenerator.setDefaultPersonId(140537L);
        
        GKInstance pathwayInst = (GKInstance) sourceInst.getAttributeValue(ReactomeJavaConstants.representedPathway);
        if (pathwayInst == null)
            throw new IllegalStateException("Cannot find pathway for " + sourceInst);
        diagramGenerator.generateELVInstancesAndFiles(pathwayInst,
                                                      sourceInst);
        
        // Check predicted pathway diagrams
        List<GKInstance> orthologousEvents = pathwayInst.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
        for (GKInstance orEvent : orthologousEvents) {
            Collection<GKInstance> referrers = orEvent.getReferers(ReactomeJavaConstants.representedPathway);
            if (referrers != null && referrers.size() > 0) {
                for (GKInstance referrer : referrers)
                    dba.deleteInstance(referrer);
            }
            GKInstance predictedPathway = diagramGenerator.generatePredictedDiagram(orEvent, 
                                                                                    pathwayInst, 
                                                                                    sourceInst);
            predictedPathway.addAttributeValue(ReactomeJavaConstants.representedPathway,
                                               orEvent);
            dba.updateInstanceAttribute(predictedPathway, 
                                        ReactomeJavaConstants.representedPathway);
            diagramGenerator.generateELVInstancesAndFiles(orEvent,
                                                          predictedPathway);
        }
    }
    
    private MySQLAdaptor getMySQLAdaptor() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "gk_central",
                                            "authortool",
                                            "T001test");
        return dba;
    }
    
    @Test
    public void geneatePathwayDiagramList() throws Exception {
        MySQLAdaptor dba = getMySQLAdaptor();
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        SchemaClass cls = dba.getSchema().getClassByName(ReactomeJavaConstants.PathwayDiagram);
        dba.loadInstanceAttributeValues(c, cls.getAttribute(ReactomeJavaConstants.representedPathway));
        dba.loadInstanceAttributeValues(c, cls.getAttribute(ReactomeJavaConstants.width));
        List<GKInstance> list = new ArrayList<GKInstance>();
        for (Object obj : c) {
            GKInstance pd = (GKInstance) obj;
            Integer width = (Integer) pd.getAttributeValue(ReactomeJavaConstants.width);
            if (width == null || width <= 10)
                continue; // Empty diagrams
            List<GKInstance> pathways = pd.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
            if (pathways != null)
                list.addAll(pathways);
        }
        InstanceUtilities.sortInstances(list);
        for (GKInstance inst : list)
            System.out.println(inst.getDisplayName() + " [" + inst.getDBID() + "]");
    }
    
    private List<String> loadDBIds(String fileName) throws IOException {
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        List<String> rtn = new ArrayList<String>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            rtn.add(tokens[0]);
        }
        fu.close();
        return rtn;
    }
    
    @Test
    public void generateProcessXMLWithDisplayNames() throws Exception {
        MySQLAdaptor dba = getMySQLAdaptor();
//        Long dbId = 507988L;
//        String output = "tmp/EGFR_Simple_36.xml";
//        String output = "tmp/EGFR_Simple_gk_central.xml";
        
//        Long dbId = 1500930L;
//        String output = "tmp/XMLTest.xml";
        Long dbId = 480128L;
        String output = "tmp/Mitotic G1-G1_S phases.xml";
        
        GKInstance pd = dba.fetchInstance(dbId);
        
        PathwayDiagramXMLGenerator xmlGenerator = new PathwayDiagramXMLGenerator();
        xmlGenerator.generateXMLForPathwayDiagram(pd, new File(output));
    }
    
    private List<Long> loadPDIdsFromAddSetAndMemberLinksFile() throws IOException {
        String fileName = "/Users/gwu/Documents/wgm/work/reactome/AddSetAndMemberLinks_gk_central_102011.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        List<Long> ids = new ArrayList<Long>();
        // Pattern to get DB_ID
        Pattern pattern = Pattern.compile("\\[PathwayDiagram:((\\d)+)\\]");
        while ((line = fu.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String value = matcher.group(1);
                ids.add(new Long(value));
            }
        }
        return ids;
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void checkExtraCellularInDiagrams() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central_050313", 
                                            "root", 
                                            "macmysql01");
        Collection<GKInstance> c = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        DiagramGKBReader reader = new DiagramGKBReader();
        int count = 0;
        System.out.println("DB_ID\tDisplayName\tCurator");
        for (GKInstance inst : c) {
            RenderablePathway diagram = reader.openDiagram(inst);
            if (diagram == null)
                continue;
            List<Renderable> list = diagram.getComponents();
            if (list == null || list.size() == 0)
                continue;
            for (Renderable r : list) {
                if (r instanceof RenderableCompartment) {
                    if (r.getDisplayName().contains("extracellular")) {
                        count ++;
                        GKInstance ie = ScriptUtilities.getAuthor(inst);
                        System.out.println(inst.getDBID() + "\t" +
                                           inst.getDisplayName() + "\t"+ 
                                           ie.getDisplayName());
                    }
                }
            }
        }
    }
    
    
    @Test
    public void testloadPDIdsFromAddSetAndMemberLinksFile() throws IOException {
        List<Long> ids = loadPDIdsFromAddSetAndMemberLinksFile();
        System.out.println("Total ids: " + ids.size());
    }
    
    @Test
    public void runBatchDeployment() throws Exception {
//        String fileName = "/Users/wgm/Documents/gkteam/Lisa/pathway_diagram_DBIDs.txt";
//        List<String> pdIds = loadDBIds(fileName);
        List<Long> pdIds = loadPDIdsFromAddSetAndMemberLinksFile();
        MySQLAdaptor dba = getMySQLAdaptor();
        String serviceUrl = "http://reactomedev.oicr.on.ca:8080/ELVWebApp/ElvService";
        int index = 0;
        for (Long dbId : pdIds) {
            GKInstance pd = dba.fetchInstance(dbId);
            System.out.println(index + ": Deploy diagram for " + pd.getDisplayName());
            index ++;
            URL url = new URL(serviceUrl);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            Map<String, Object> info = new HashMap<String, Object>();
            info.put("pdId", pd.getDBID());
            GKInstance latestIE = InstanceUtilities.getLatestIEFromInstance(pd);
            info.put("pdIE", latestIE.getDBID());
            // Use char array for some security
            info.put("user", "wgm".toCharArray());
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
            System.out.println(builder.toString());
        }
    }
    
    /**
     * This method is used to check how many pathways that have detailed pathway diagrams.
     * @throws Exception
     */
    @Test
    public void checkDetailedPathwayDiagrams() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_current_ver42",
                                            "root",
                                            "macmysql01");
        Collection<GKInstance> pathways = dba.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
        dba.loadInstanceAttributeValues(pathways, new String[]{ReactomeJavaConstants.species, ReactomeJavaConstants.hasComponent});
        Collection<GKInstance> diagrams = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        dba.loadInstanceAttributeValues(diagrams, new String[]{ReactomeJavaConstants.representedPathway});
        List<GKInstance> selectedPathways = new ArrayList<GKInstance>();
        DiagramGKBReader reader = new DiagramGKBReader();
        long time1 = System.currentTimeMillis();
        for (GKInstance diagram : diagrams) {
            GKInstance pathway = (GKInstance) diagram.getAttributeValue(ReactomeJavaConstants.representedPathway);
            GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
            if (species.getDBID().equals(48887L)) {
                RenderablePathway rDiagram = reader.openDiagram(diagram);
                if (rDiagram.getComponents() == null || rDiagram.getComponents().size() == 0)
                    continue;
                for (Object o : rDiagram.getComponents()) {
                    Renderable r = (Renderable) o;
                    Long dbId = r.getReactomeId();
                    if (dbId == null)
                        continue;
                    GKInstance inst = dba.fetchInstance(dbId);
                    if (inst.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)) {
                        selectedPathways.add(pathway);
                        break;
                    }
                }
            }
        }
        long time2 = System.currentTimeMillis();
        System.out.println("Total time: " + (time2 - time1));
        System.out.println("Total Selected Pathways: " + selectedPathways.size());
    }
    
}
