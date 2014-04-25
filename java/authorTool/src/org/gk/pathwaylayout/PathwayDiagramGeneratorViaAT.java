/*
 * Created on Oct 3, 2008
 *
 */
package org.gk.pathwaylayout;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.gk.database.SynchronizationManager;
import org.gk.gkCurator.authorTool.CuratorToolToAuthorToolConverter;
import org.gk.gkEditor.AuthorToolActionCollection;
import org.gk.gkEditor.CoordinateSerializer;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.GKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.util.GraphLayoutEngine;
import org.junit.Test;


/**
 * This class is used to generate all necessary information for the entity level view
 * via the author tool. 
 * @author wgm
 *
 */
public class PathwayDiagramGeneratorViaAT {
    private MySQLAdaptor dba;
    private Long defaultPersonId;
    private static Logger logger = Logger.getLogger(PathwayDiagramGeneratorViaAT.class);
    
    public PathwayDiagramGeneratorViaAT() {
    }
    
    public void setDefaultPersonId(Long id) {
        this.defaultPersonId = id;
    }
    
    public MySQLAdaptor getDBA() throws Exception {
        if (dba == null) {
            dba = new MySQLAdaptor("localhost",
                                   "pathway_diagram_072908_2", //"reactome_25_pathway_diagram",
                                   "root",
                                   "macmysql01",
                                   3306);
        }
        return dba;
    }
    
    public void setMySQLAdaptor(MySQLAdaptor dba) {
        this.dba = dba;
    }
    
    protected Project convertToATProject (GKInstance pathway) throws Exception {
        DoPathwayByPathwayNoGUI converter = new DoPathwayByPathwayNoGUI();
        return converter.convertToATProject(pathway);
    }
    
    /**
     * Test the method generateTilesForATDiagram
     * @throws Exception
     */
    @Test
    public void testGenerateImageForATDiagram() throws Exception {
        //String fileName = "tmp/Apoptosis_tweaked.gkb";
        //GKBReader reader = new GKBReader();
        //Project project = reader.open(fileName);
        GKInstance pathway = getTestPathway();
        setDefaultPersonId(140537L); // 140537 is for Wu, G.
        generateImageForAT(pathway, true);
    }
    
    /**
     * Generate tiles directly from a pathway in GKInstance.
     * @param pathway
     * @param useSimpleView true to use Imre's simplifying view from PathwayByPathway class. false to
     * use the direct output from GKInstance.
     * @throws Exception
     */
    public void generateImageForAT(GKInstance pathway,
                                   boolean useSimpleView) throws Exception {
        if (defaultPersonId == null)
            throw new IllegalStateException("Please provide the default person id for InstanceEdit instances!");
        Project project = null;
        if (useSimpleView) {
            project = convertToATProject(pathway);
        }
        else {
            CuratorToolToAuthorToolConverter converter = new CuratorToolToAuthorToolConverter();
            project = converter.convert(pathway, null);
        }
        project.getProcess().setReactomeId(pathway.getDBID());
        GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
        generateImageForAT(project, species);
    }
    
    /**
     * Generate tiles directly from a pathway in GKInstance.
     * Tries to generate the "best" image - if a curated pathway is available, that
     * will be used, otherwise, coordinates will be taken from the Sky and an image
     * will be generated with the help of layouting algorithms.
     * @param pathway
     * @throws Exception
     */
    public void generateBestImageForAT(GKInstance pathway) throws Exception {
//        if (defaultPersonId == null)
//            throw new IllegalStateException("Please provide the default person id for InstanceEdit instances!");
//        CuratorToolToAuthorToolConverter converter = new CuratorToolToAuthorToolConverter();
//        Project project = converter.convertNullReturn(pathway, null);
//        if (project == null) {
//        	System.err.println("Using Sky coords");
//            project = convertToATProject(pathway); // Use Sky coords
//        }
//        project.getProcess().setReactomeId(pathway.getDBID());
//        GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
//        generateImageForAT(project, species);
    }
    
    protected void tightNodeBounds(PathwayEditor editor) {
        Node.setWidthRatioOfBoundsToText(1.0d);
        Node.setHeightRatioOfBoundsToText(1.0d);
        Node.setNodeWidth(100);
        Renderable r = null;
        for (Iterator it = editor.getDisplayedObjects().iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            if (r instanceof Node) {
                Node node = (Node) r;
                node.setBounds(null);
            }
        }
        double[] scales = new double[]{1.0, 0.5, 0.1};
        for (int i = 0; i < scales.length; i++) {
        	editor.setScale(scales[i], scales[i]);
        	Dimension size = editor.getPreferredSize();
        	// Used a buffered image to repaint
        	try {
        		BufferedImage image = new BufferedImage(size.width,
        				size.height,
        				BufferedImage.TYPE_INT_RGB);
        		Graphics g = image.createGraphics();
        		g.setFont(new Font("Dialog", Font.PLAIN, 12));
        		g.setClip(0, 0, size.width, size.height);
        		editor.paint(g);
        		break;
        	} catch (OutOfMemoryError e) {
        		if (i == scales.length - 1) {
        			logger.warn("Failed to tightNodeBounds() of " + editor.getRenderable());
        		}
        	}
        }
    }
    
    /**
     * This method is used to generate tiles for an author tool project.
     * @param project
     * @throws Exception
     */
    public void generateImageForAT(Project project,
                                   GKInstance species) throws Exception {
        PathwayEditor editor = new PathwayEditor();
        editor.setRenderable(project.getProcess());
        // Remove compartment
        new AuthorToolActionCollection(null).removeCompartmentFromName(project.getProcess().getComponents());
        tightNodeBounds(editor);
        // Do an auto-layout
        project.getProcess().layout(GraphLayoutEngine.HIERARCHICAL_LAYOUT);

        File topDir = getTopDir(project.getProcess().getReactomeId(), 
                                species);
        // Output as the author tool project
        //outputProject(project, topDir);
        // Save as thumbnail
        saveAsThumbnail(100, editor, topDir);
        // Output as tiles for the web applications
        saveAsTiles(editor, topDir);
        // save the coordinates into the database
        storeCoordinatesToDB(project, editor);
/*        logger.info("Starting to save full image.");
        //File file = new File(topDir, project.getProcess().getDisplayName() + ".png");
        File file = new File(topDir, "img.png");
		try {
			editor.setScale(1.0d, 1.0d);
			BufferedImage img = paintOnImage(editor);
	        ImageIO.write(img, "png", file);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
		logger.info("Finished saving full image.");*/
    }
    
    /**
     * Store the coordinates into the databas.
     * @param project
     * @param editor
     * @throws Exception
     */
    protected void storeCoordinatesToDB(Project project,
                                      PathwayEditor editor) throws Exception {
    	logger.info("Starting storeCoordinatesToDB");
    	editor.setScale(1.0d, 1.0d);
        Dimension size = editor.getPreferredSize();
        // Initialize some requirements
        PersistenceManager.getManager().setActiveMySQLAdaptor(getDBA());
        // Reset the XMLFileAdaptor in case to overwrite the instances for other pathways
        XMLFileAdaptor fileAdpator = PersistenceManager.getManager().getActiveFileAdaptor();
        if (fileAdpator == null) {
            fileAdpator = new XMLFileAdaptor();
            PersistenceManager.getManager().setActiveFileAdaptor(fileAdpator);
        }
        else
            fileAdpator.reset();
        SynchronizationManager.getManager().setDefaultPerson(defaultPersonId);
        CoordinateSerializer serializer = new CoordinateSerializer();
        serializer.storeCoordinates(project, 
                                    size.width, 
                                    size.height, 
                                    null); // Don't need to pass in a component, which is used for GUIs.
    	logger.info("Finished storeCoordinatesToDB");
    }
    
    public File saveAsThumbnail(double maxDim, 
                                 PathwayEditor editor,
                                 File dir) throws Exception {
        // Need to calculate a scale based on width
        editor.setScale(1.0d, 1.0d);
        Dimension size = editor.getPreferredSize();
        double coeff = maxDim / Math.max(size.width, size.height);
        editor.setScale(coeff, coeff);
        BufferedImage img = paintOnImage(editor);
        File outFile = new File(dir, "thumb.png");
        ImageIO.write(img, "png", outFile);
        return outFile;
    }
    
    protected File getTopDir(Long pathwayId,
                             GKInstance species) {
        File topDir = new File("tiles" + "/" + this.dba.getDBName() + "/" + 
                               species.getDBID() + "/" + pathwayId);
        // Want to empty
        if (topDir.exists())
            topDir.delete();
        topDir.mkdirs();
        return topDir;
    }
    
    protected void saveAsTiles(PathwayEditor editor,
                               File topDir) throws Exception {
        double[] zoomLevels = PathwayLayoutConstants.ZOOM_LEVELS;
        // Make sure zoomlevel = 1.0 to do first. Otherwise, a smaller zooming level
        // make effect the TextLayout calculation.
        for (int i = 0; i < zoomLevels.length; i++) {
//        for (int i = zoomLevels.length-1; i >= 0; i--) {
            File levelDir = new File(topDir, (i + 1) + "");
            levelDir.mkdir();
            try {
                saveZoomLevelAsTiles(editor,
                                     zoomLevels[i], 
                                     levelDir);
            } 
            catch (OutOfMemoryError e) {
            	logger.warn("Diagram of [" + editor.getRenderable().getReactomeId() + "] " + editor.getRenderable() + " too large at zoom level " + zoomLevels[i]);
            	//System.out.println("Diagram of " + editor.getRenderable() + " too large at zoom level " + zoomLevels[i]);
            	//logger.debug("Diagram of " + editor.getRenderable() + " too large at zoom level " + zoomLevels[i], e);
            	break;
            }
        }
    }
    
    public void saveZoomLevelAsTiles(PathwayEditor editor,
                                     double zoomLevel, 
                                     File dir) throws IOException {
        editor.setScale(zoomLevel, zoomLevel);
        BufferedImage img = paintOnImage(editor);
        //File imgFile = new File(dir, editor.getRenderable().getDisplayName() + ".png");
        File imgFile = new File(dir, "diagram.png");
        ImageIO.write(img, "png", imgFile);
        int tileWidth = 200;
        int w = img.getWidth();
        int h = img.getHeight();
        for (int xs = 0; xs*tileWidth < w; xs++) {
            int tmp = w - xs*tileWidth;
            int trueTileW = (tmp < tileWidth) ? tmp : tileWidth;
            for (int ys = 0; ys*tileWidth < h; ys++) {
                tmp = h - ys*tileWidth;
                int trueTileH = (tmp < tileWidth) ? tmp : tileWidth;
                String fileName = xs + "x" + ys + ".png";
                File tileFile = new File(dir, fileName);
                //System.out.println(tileFile);
                BufferedImage simg = img.getSubimage(xs * tileWidth, 
                                                     ys * tileWidth, 
                                                     trueTileW, 
                                                     trueTileH);
                ImageIO.write(simg, "png", tileFile);
            }
        }
    }

    public BufferedImage paintOnImage(PathwayEditor editor) {
        Dimension size = editor.getPreferredSize();
        //System.out.printf("width = %d, height = %d\n", size.width, size.height);
        BufferedImage img = new BufferedImage(size.width,
                                              size.height,
                                              BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D grap = img.createGraphics();
        //grap.setFont(new Font("Dialog", Font.PLAIN, 12));
        grap.setFont(editor.getFont());
        // Need to set clip with the whole size so that everything can be drawn
        Rectangle clip = new Rectangle(size);
        grap.setClip(clip);
//        grap.fillRect(0,
//                      0,
//                      img.getWidth(),
//                      img.getHeight());
        // Want to draw a transparent image
        //Color color = new Color(0, 0, 0, 0);
        //editor.setBackground(color);
        editor.paint(grap);
        return img;
    }

    protected void outputProject(Project project,
                                 File topDir) throws Exception {
        File file = new File(topDir, project.getProcess().getDisplayName() + ".gkb");
        GKBWriter writer = new GKBWriter();
        writer.save(project, new FileOutputStream(file));
    }
    
    @Test
    public void testExportIntoATProjectViaKnownCoordinates() throws Exception {
        GKInstance pathway = getTestPathway();
        exportIntoATProjectViaKnownCoordinates(pathway);
    }

    private GKInstance getTestPathway() throws Exception {
        MySQLAdaptor dba = getDBA();
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        // This is used to test human apoptosis
        Long dbId = 109581L;
        GKInstance pathway = dba.fetchInstance(dbId);
        return pathway;
    }

    public void exportIntoATProjectViaKnownCoordinates(GKInstance pathway) throws Exception {
        MySQLAdaptor dba = (MySQLAdaptor) pathway.getDbAdaptor();
        Collection collection = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram, 
                                                             ReactomeJavaConstants.representedPathway,
                                                             "=", 
                                                             pathway);
        GKInstance pathwayDiagram = (GKInstance) collection.iterator().next();
        // Need to get vertex
        // Create a map to speed up search
        Map<Long, GKInstance> idToVertex = loadVertex(pathwayDiagram, dba);
        CuratorToolToAuthorToolConverter converter = new CuratorToolToAuthorToolConverter();
        Project project = converter.convert(pathway, null);
        // Just a quick check
        System.out.println("Project name: " + project.getProcess().getDisplayName());
        // This is used to copy the coordinates from vertex
        List<Node> notMapped = extractCoordinates(idToVertex, project);
        // Want to delete Nodes that don't have any coordinates
        deleteNodes(project, notMapped);
        // Output the converted project
        String fileName = "tmp/Apoptosis.gkb";
        GKBWriter writer = new GKBWriter();
        writer.save(project, fileName);
    }
    
    private void deleteNodes(Project project,
                             List<Node> toBeDeleted) {
        // Borrow the PathwayEditor to do deletion
        PathwayEditor editor = new PathwayEditor();
        editor.setRenderable(project.getProcess());
        editor.setSelection(toBeDeleted);
        editor.deleteSelection();
    }

    private List<Node> extractCoordinates(Map<Long, GKInstance> idToVertex,
                                          Project project) throws Exception {
        List components = project.getProcess().getComponents();
        List<HyperEdge> hyperEdges = new ArrayList<HyperEdge>();
        List<Node> notMapped = new ArrayList<Node>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof HyperEdge) {
                hyperEdges.add((HyperEdge)r);
                continue;
            }
            Long rId = r.getReactomeId();
            GKInstance vertex = idToVertex.get(rId);
            if (vertex == null) {
                notMapped.add((Node)r);
                continue;
            }
            // Extract coordinate
            int x0 = (Integer) vertex.getAttributeValue(ReactomeJavaConstants.x);
            int y0 = (Integer) vertex.getAttributeValue(ReactomeJavaConstants.y);
            if (r.getBounds() == null)
                r.setPosition(x0, y0);
            else {
                int x1 = r.getPosition().x;
                int y1 = r.getPosition().y;
                int dx = x0 - x1;
                int dy = y0 - y1;
                r.move(dx, dy);
            }
        }
        // The following code is copied from org.gk.render.PahtwayLayoutHelper
        for (HyperEdge edge : hyperEdges) {
            //List backbonePoints = edge.getBackbonePoints();
            //if (backbonePoints.size() < 3)
            // Layout every edge so that all added bending points will not be left
            // out. This is a simple approach. It will be better to convert adding
            // bending points during converting to graph. Right now, they are not!
                edge.layout();
        }
        // Do a sanity check for edges
        for (Iterator it = hyperEdges.iterator(); it.hasNext();) {
            HyperEdge edge = (HyperEdge) it.next();
            List inputs = edge.getInputNodes();
            if (inputs == null || inputs.size() == 0) {
                edge.setInputHub(edge.getPosition());
            }
            List outputs = edge.getOutputNodes();
            if (outputs == null || outputs.size() == 0) {
                edge.setOutputHub(edge.getPosition());
            }
        }
        return notMapped;
    }
    
    private Map<Long, GKInstance> loadVertex(GKInstance pathwayDiagram,
                                             MySQLAdaptor dba) throws Exception {
        // This is used to test human apoptosis
        // Need to get vertex
        Collection collection = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Vertex, 
                                                  ReactomeJavaConstants.pathwayDiagram,
                                                  "=", 
                                                  pathwayDiagram);
        System.out.println("Total vertex: " + collection.size());
        dba.loadInstanceAttributeValues(collection);
        // Create a map to speed up search
        Map<Long, GKInstance> idToVertex = new HashMap<Long, GKInstance>();
        for (Iterator it = collection.iterator(); it.hasNext();) {
            GKInstance vertex = (GKInstance) it.next();
            GKInstance represented = (GKInstance) vertex.getAttributeValue(ReactomeJavaConstants.representedInstance);
            idToVertex.put(represented.getDBID(),
                           vertex);
        }
        return idToVertex;
    }
    
    public void regenerateDiagram (GKInstance pathway) throws Exception {
        MySQLAdaptor dba = getDBA();
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        Collection collection = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram, 
                                                             ReactomeJavaConstants.representedPathway,
                                                             "=", 
                                                             pathway);
        GKInstance pathwayDiagram = (GKInstance) collection.iterator().next();
        CuratorToolToAuthorToolConverter converter = new CuratorToolToAuthorToolConverter();
        Project project = converter.convert(pathway, null);
        PathwayEditor editor = new PathwayEditor();
        editor.setRenderable(project.getProcess());
        GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
        File topDir = getTopDir(project.getProcess().getReactomeId(), 
                                species);
        // Save as thumbnail
        saveAsThumbnail(100, editor, topDir);
        // Output as tiles for the web applications
        saveAsTiles(editor, topDir);
        File file = new File(topDir, project.getProcess().getDisplayName() + ".png");
		try {
			BufferedImage img = paintOnImage(editor);
	        ImageIO.write(img, "png", file);
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
    }
    
}
