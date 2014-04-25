package org.gk.pathwaylayout;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.Schema;
import org.jgraph.graph.GraphConstants;

public class PathwayByPathway extends ReactionCoordinatebasedLayout {

	public GKInstance focusPathway = null;
	protected Set<GKInstance> reactions = null;
	
	public PathwayByPathway() throws Exception {
	}
	
	public PathwayByPathway(MySQLAdaptor dba, GKInstance species, GKInstance pathway) throws Exception {
		this.dba = dba;
		this.focusSpecies = species;
		this.focusPathway = pathway;
		init2();
	}

	public Set<GKInstance> getReactions() throws Exception {
		if (reactions == null) {
			reactions = Utils.getComponentReactions(this.focusPathway);
			dba.loadInstanceAttributeValues(reactions, new String[]{ReactomeJavaConstants.input,ReactomeJavaConstants.output,ReactomeJavaConstants.catalystActivity});
			dba.loadInstanceReverseAttributeValues(reactions, new String[]{ReactomeJavaConstants.regulatedEntity});
		}
		return reactions;
	}
	
	public void relocateVerteces() {
		Point topLeft = findTopLeft();
		//topLeft.x -= 10;
		//topLeft.y -= 10;
		Map<Vertex,Map> nested = new HashMap<Vertex,Map>();
		for (int i = 0; i < verteces.size(); i++) {
			Vertex v = (Vertex)verteces.get(i);
			Rectangle bounds = v.getBounds();
			bounds.x -= topLeft.x;
			bounds.y -= topLeft.y;
			Map attMap = new HashMap();
			GraphConstants.setBounds(attMap,bounds);
			nested.put(v, attMap);
		}
		model.edit(nested, null, null, null);
	}
	
	public Point findTopLeft() {
		int min_x = Integer.MAX_VALUE;
		int min_y = Integer.MAX_VALUE;
		for (int i = 0; i < verteces.size(); i++) {
			Vertex n = (Vertex)verteces.get(i);
			min_x = Math.min(min_x, n.getX());
			min_y = Math.min(min_y, n.getY());
		}
		return new Point(min_x,min_y);
	}
	
	public void checkBounds() {
		Point br = findBottomRight();
		Rectangle2D bounds = graph.getCellBounds(graph.getRoots());
		if (bounds.getWidth() > br.x) {
			System.out.printf("\nWidth > right: %d > %d\n\n", (int) bounds.getWidth(), br.x);
		}
		if (bounds.getMaxX() > br.x) {
			System.out.printf("\nMaxX > right: %d > %d\n\n", (int) bounds.getMaxX(), br.x);
		}
	}
	
	public void init2() throws Exception {
		graph.setScale(0.125);
		List nodes = new ArrayList();
		for (Iterator ri = getReactions().iterator(); ri.hasNext();) {
			nodes.add(createNodeForReaction((GKInstance)ri.next()));
		}
		graph.getGraphLayoutCache().setSelectsAllInsertedCells(false);
		model.insert(nodes.toArray(),null,null,null,null);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setDoubleBuffered(false);
		JScrollPane scrollPane = new JScrollPane(graph);
		scrollPane.setDoubleBuffered(false);
		scrollPane.setPreferredSize(new Dimension(1000,500));
		panel.add(scrollPane, BorderLayout.CENTER);
		JFrame frame = new JFrame();
		graph.setAntiAliased(true);
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		hiddenEntities = Utils.fetchHiddenEntities(dba);
		insertConnectingEntities();
		resetEntityNodeBounds();
		placeEntitiesWithSingleEdge();
		spreadEntityNodesWithIdenticalConnectivity();
		collapseClusteredCatalystVerteces();
		bringReactionNodesToFront();
		relocateVerteces();
		storeGraphInDb();
		relaxer = new Thread(this, "Relaxer");
		relaxer.start();
		//save = true;
		relaxer.join();
		frame.dispose();
	}
	
	public void saveAsTiles() throws IOException {
		relax = false;
		while(inRelax || inUpdateView) {
			System.out.println("Waiting for relaxer and updateView() to finish...");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		double[] zoomLevels = PathwayLayoutConstants.ZOOM_LEVELS;
		File topDir = new File("tiles" + "/" + this.dba.getDBName() + "/" + this.focusSpecies.getDBID() + "/" + this.focusPathway.getDBID());
		if (! topDir.exists())
			topDir.mkdirs();
		//saveCoordinates(zoomLevels[0], topDir);
		//for (int i = 0; i < zoomLevels.length; i++) {
		for (int i = 0; i < 4; i++) {
			File levelDir = new File(topDir, String.valueOf(i + 1));
			if (! levelDir.exists())
				levelDir.mkdir();
			try {
				saveZoomLevelAsTiles(zoomLevels[i], levelDir);
			} catch (Exception e) {
				e.printStackTrace();
				i--;
			}
		}
		saveAsThumbnail(100, topDir);
	}
	
	public void saveAsThumbnail(double maxDim, File dir) {
		System.out.println("Starting saveAsThumbnail()");
		Point br = findBottomRight();
		System.out.println(br);
		double largest = (br.getX() > br.getY()) ? br.getX() : br.getY();
		double coeff = maxDim / largest;
		graph.setScale(coeff);
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace();}
		File outFile = new File(dir, "thumb.png");
		BufferedImage img = graph.getImage(null, GraphConstants.DEFAULTINSET);
		int w = Math.min(img.getWidth(), (int) (br.x * coeff));
		int h = Math.min(img.getHeight(), (int) (br.y * coeff));
		System.out.printf("w = %d, h = %d\n", w, h);
		BufferedImage simg = img.getSubimage(0, 0, w, h);
		try {
			ImageIO.write(simg, "png", outFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Finished saveAsThumbnail()");
	}
	
	public void storeGraphInDb () throws InvalidAttributeException, Exception {
		System.out.println("Starting storeGraphInDb");
		if (dba.supportsTransactions())
			dba.startTransaction();
		GKInstance view = createInstance();
		dba.storeInstance(view);
		for (Vertex v : verteces) {
			GKInstance i = createInstance(v);
			i.setAttributeValue("pathwayDiagram", view);
			dba.storeInstance(i);
		}
		for (Edge e : edges) {
			GKInstance i = createInstance(e);
			dba.storeInstance(i);
		}
		if (dba.supportsTransactions())
			dba.commit();
		System.out.println("Finished storeGraphInDb");
	}
	
	private GKInstance createInstance (Vertex v) throws InvalidAttributeException, Exception {
		Schema schema = dba.getSchema();
		GKInstance i;
		if (v.isEntityVertex()) {
			i = new GKInstance(schema.getClassByName(ReactomeJavaConstants.EntityVertex));
		} else {
			i = new GKInstance(schema.getClassByName(ReactomeJavaConstants.ReactionVertex));
		}
		i.setAttributeValue(ReactomeJavaConstants.representedInstance,(GKInstance) v.getUserObject());
		Rectangle b = v.getBounds();
		i.setAttributeValue(ReactomeJavaConstants.x, (int) (b.x * PathwayLayoutConstants.ZOOM_LEVELS[0]));
		i.setAttributeValue(ReactomeJavaConstants.y, (int) (b.y * PathwayLayoutConstants.ZOOM_LEVELS[0]));
		i.setAttributeValue(ReactomeJavaConstants.width, (int) (b.width * PathwayLayoutConstants.ZOOM_LEVELS[0]));
		i.setAttributeValue(ReactomeJavaConstants.height, (int) (b.height * PathwayLayoutConstants.ZOOM_LEVELS[0]));
		i.setIsInflated(true);
		v.storageInstance = i;
		return i;
	}
	
	private GKInstance createInstance (Edge e) throws InvalidAttributeException, Exception {
		Schema schema = dba.getSchema();
		GKInstance i = new GKInstance(schema.getClassByName(ReactomeJavaConstants.Edge));
		i.setAttributeValue(ReactomeJavaConstants.sourceVertex, e.sourceVertex.storageInstance);
		i.setAttributeValue(ReactomeJavaConstants.targetVertex, e.targetVertex.storageInstance);
		i.setIsInflated(true);
		e.storageInstance = i;
		return i;
	}
	
	private GKInstance createInstance () throws InvalidAttributeException, Exception {
		Schema schema = dba.getSchema();
		//Rectangle bounds = graph.getCellBounds(graph.getRoots()).getBounds();
		//int w = (int) ((10 + bounds.getWidth() + bounds.getX()) * PathwayLayoutConstants.ZOOM_LEVELS[0]);
		//int h = (int) ((10 + bounds.getHeight() + bounds.getY()) * PathwayLayoutConstants.ZOOM_LEVELS[0]);
		Point br = findBottomRight();
		int w = (int) (br.x * PathwayLayoutConstants.ZOOM_LEVELS[0]);
		int h = (int) (br.y * PathwayLayoutConstants.ZOOM_LEVELS[0]);
		GKInstance i = new GKInstance(schema.getClassByName("PathwayDiagram"));
		i.setAttributeValue(ReactomeJavaConstants.width, w);
		i.setAttributeValue(ReactomeJavaConstants.height, h);
		//i.setAttributeValue(ReactomeJavaConstants.species, this.focusSpecies);
		i.setAttributeValue("representedPathway", this.focusPathway);
		i.setIsInflated(true);
		return i;
	}
	
	public static void main(String[] args) {
		if (args.length < 5) {
			System.out.println("Usage java XXX" +
			" dbHost dbName dbUser dbPwd dbPort");
			System.exit(0);
		}
		try {
			MySQLAdaptor dba = new MySQLAdaptor(args[0],args[1],args[2],args[3],Integer.parseInt(args[4]));
			String speciesName = "Homo sapiens";
			GKInstance focusSpecies =  (GKInstance)dba.fetchInstanceByAttribute(ReactomeJavaConstants.Species,ReactomeJavaConstants.name,"=",speciesName).iterator().next();
			Collection<GKInstance> topPathways = Utils.getTopLevelPathwaysForSpecies(dba, focusSpecies);
			for (GKInstance pathway : topPathways) {
				System.out.println("Now handling " + pathway);
				PathwayByPathway pbp = new PathwayByPathway(dba, focusSpecies, pathway);
			}
			//GKInstance p = dba.fetchInstance(new Long(346896));
			//PathwayByPathway pbp = new PathwayByPathway(dba, (GKInstance) p.getAttributeValue("species"), p);
			//GKInstance pathway = (GKInstance)dba.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway,ReactomeJavaConstants.DB_ID,"=",202131).iterator().next();
			//PathwayByPathway pbp = new PathwayByPathway(dba, focusSpecies, pathway);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
