package org.gk.pathwaylayout;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

public class PathwayByPathwayNoGUI extends PathwayByPathway {
		
	public PathwayByPathwayNoGUI() throws Exception {
		super();
	}
	
	public PathwayByPathwayNoGUI(MySQLAdaptor dba, GKInstance species, GKInstance pathway) throws Exception {
		super(dba, species, pathway);
	}

	public void init2() throws Exception {
		// No need to set the scale, i.e. the default is OK.
		//graph.setScale(0.8);
		// Important: transparent black colour (0,0,0,0) does not result in transparent bg.
		graph.setBackground(new Color(255,255,255,0));
		List nodes = new ArrayList();
		for (Iterator ri = getReactions().iterator(); ri.hasNext();) {
			nodes.add(createNodeForReaction((GKInstance)ri.next()));
		}
		graph.getGraphLayoutCache().setSelectsAllInsertedCells(false);
		model.insert(nodes.toArray(),null,null,null,null);

		graph.setAntiAliased(true);
		JPanel panel = new JPanel();
		panel.setDoubleBuffered(false);
		panel.add(graph);
		panel.setVisible(true);
		panel.setEnabled(true);
		panel.addNotify();
		panel.validate();
		
		//hiddenEntities = Utils.fetchHiddenEntities(dba);
		insertEntities();
		resetEntityNodeBounds();
		//placeEntitiesWithSingleEdge();
		//spreadEntityNodesWithIdenticalConnectivity();
		//collapseClusteredCatalystVerteces();
		//bringReactionNodesToFront();
		//relocateVerteces();
		//storeGraphInDb();
		/*
		 * This is woodoo for me. Image creation has to happen on a different thread where issuing
		 * graph.paintImmediately() actually works and the image is rendered completely. Furthermore,
		 * there's no more need for sleep() etc.
		 * If I call saveAsTile() from here directly, i.e. on this thread, images are partially rendered.
		 */
//		relaxer = new Thread(this, "Relaxer");
//		relaxer.start();
//		relaxer.join();
	}

	
	public void saveAsTiles() throws IOException {
		double[] zoomLevels = PathwayLayoutConstants.ZOOM_LEVELS;
		File topDir = new File("tiles" + "/" + this.dba.getDBName() + "/" + this.focusSpecies.getDBID() + "/" + this.focusPathway.getDBID());
		if (! topDir.exists())
			topDir.mkdirs();
		for (int i = 0; i < 4; i++) {
			File levelDir = new File(topDir, String.valueOf(i + 1));
			if (! levelDir.exists())
				levelDir.mkdir();
			try {
				saveZoomLevelAsTiles(zoomLevels[i], levelDir);
			} catch (Exception e) {
				e.printStackTrace();
				//i--;
			}
		}
		saveAsThumbnail(100, topDir);
	}
	
	public void saveZoomLevelAsTiles(double zoomLevel, File dir) throws IOException {
		System.out.printf("Starting saveZoomLevelAsTiles(%s, %s)\n", zoomLevel, dir);
		graph.setScale(zoomLevel);
		Point br = findBottomRight();
		int w = (int) (br.x * zoomLevel);
		int h = (int) (br.y * zoomLevel);
		System.out.printf("w = %d\th = %d\n", w, h);
		BufferedImage img = graph.getImage(null, 0);
		int tileWidth = 200;
		w = img.getWidth();
		h = img.getHeight();
		System.out.printf("w = %d\th = %d\n", w, h);
		for (int xs = 0; xs*tileWidth < w; xs++) {
			int tmp = w - xs*tileWidth;
			int trueTileW = (tmp < tileWidth) ? tmp : tileWidth;
			for (int ys = 0; ys*tileWidth < h; ys++) {
				tmp = h - ys*tileWidth;
				int trueTileH = (tmp < tileWidth) ? tmp : tileWidth;
				String fileName = xs + "x" + ys + ".png";
				File tileFile = new File(dir, fileName);
				//System.out.println(tileFile);
				BufferedImage simg = img.getSubimage(xs * tileWidth, ys * tileWidth, trueTileW, trueTileH);
				ImageIO.write(simg, "png", tileFile);
			}
		}
	}
	
	public void saveZoomLevelAsTiles1stSuccess(double zoomLevel, File dir) throws IOException {
		System.out.printf("Starting saveZoomLevelAsTiles(%s, %s)\n", zoomLevel, dir);
		graph.setScale(zoomLevel);
		Point br = findBottomRight();
		int w = (int) ((10 + br.x) * zoomLevel);
		int h = (int) ((10 + br.y) * zoomLevel);
		System.out.printf("w = %d\th = %d\n", w, h);
		graph.paintImmediately(0,0,w,h);
		BufferedImage img = graph.getImage(null, 0);
		String fileName = "zzz.png";
		File tileFile = new File(dir, fileName);
		ImageIO.write(img, "png", tileFile);
	}
	
	public void insertEntities() throws Exception {
		List toBeInserted = new ArrayList();
		for (Iterator i = reactionInstance2NodeMap.keySet().iterator(); i.hasNext();) {
			GKInstance reaction = (GKInstance)i.next();
			List graphCells = null;
			graphCells = insertNonSimpleEntitiesOnce(reaction);
			if ((graphCells != null) && !graphCells.isEmpty())
				toBeInserted.addAll(graphCells);
		}
		// Now do 2nd pass to insert remaining outputs
		for (Iterator i = reactionInstance2NodeMap.keySet().iterator(); i.hasNext();) {
			GKInstance reaction = (GKInstance)i.next();
			List graphCells;
			graphCells = insertOutputEntities(reaction);
			if ((graphCells != null) && !graphCells.isEmpty())
				toBeInserted.addAll(graphCells);
		}
		model.insert(toBeInserted.toArray(),null,null,null,null);
	}
	
	public List insertNonSimpleEntitiesOnce(GKInstance reaction) throws InvalidAttributeException, Exception {
		List<DefaultGraphCell> toBeInserted = new ArrayList<DefaultGraphCell>(); // new things which will have to be inserted into model
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.input).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			if (isReactionInputInserted(reaction, entity)) 
				continue;
			if (entity.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity)) {
				List<GKInstance> helperList = new ArrayList<GKInstance>();
				List<GKInstance[]> helperList2 = new ArrayList<GKInstance[]>();
				for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
					GKInstance event = (GKInstance)it2.next();
					if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
						continue;
					List<GKInstance> outputs = event.getAttributeValuesList(ReactomeJavaConstants.output);
					if(outputs.contains(entity)) {
						helperList.add(event);
					}
					GKInstance relatedEntity = Utils.grepCollectionForSetMemberOrContainingSets(outputs, entity);
					if (relatedEntity != null) {
						helperList2.add(new GKInstance[]{relatedEntity, event});
					}
				}
				//Now find all the reactions which also use the same entity as an input and are annotated
				//as following events of the preceding events. Complicated.
				Set<GKInstance> followingEvents = new HashSet<GKInstance>();
				followingEvents.add(reaction);
				for (GKInstance precedingEvent : helperList) {
					Set<GKInstance> followingEvents2 = Utils.getFollowingEventsConnectedOverInputEntity(precedingEvent, entity);
					followingEvents.addAll(followingEvents2);
				}

				Vertex enode = createNodeForEntity(entity);
				toBeInserted.add(enode);
				
				for (GKInstance followingEvent : followingEvents) {
					Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(followingEvent);
					if (rnode2 == null)
						continue;
					DefaultEdge edge = createInputEdge(enode, rnode2);
					toBeInserted.add(edge);
					setReactionInputInserted(followingEvent,entity);
				}

				for (Iterator it2 = helperList.iterator(); it2.hasNext();) {
					GKInstance event = (GKInstance)it2.next();
					//make link between entity and preceding reaction
					Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(event);
					if (rnode2 == null)
						continue;
					DefaultEdge edge = createOutputEdge(rnode2,enode);
					toBeInserted.add(edge);
					setReactionOutputInserted(event,entity);
				}
			} else {
				Vertex enode = getNodeForEntity(entity);
				DefaultEdge edge = createInputEdge(enode, rnode);
				toBeInserted.add(edge);
				setReactionInputInserted(reaction,entity);
			}
		}
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance)((GKInstance) it.next()).getAttributeValue(ReactomeJavaConstants.physicalEntity);
			// in case there are CatalystActivities w/o physicalEntity
			if (entity == null)
				continue;
			// skip catalysts which are also input
			if (reaction.getAttributeValuesList(ReactomeJavaConstants.input).contains(entity))
				continue;
			Vertex enode = getNodeForEntity(entity);
			DefaultEdge edge = createCatalystEdge(enode,rnode);
			toBeInserted.add(edge);
		}
		Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
		if (regulations != null) {
			for (Iterator it = regulations.iterator(); it.hasNext();) {
				GKInstance regulation = (GKInstance)it.next();
				GKInstance entity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
				if (entity == null || !entity.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
					continue;
				if (entity.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity)) {
					Vertex enode = createNodeForEntity(entity);
					toBeInserted.add(enode);
					DefaultEdge edge = createRegulatorEdge(enode,rnode,regulation.getSchemClass().getName());
					toBeInserted.add(edge);
					for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
						GKInstance event = (GKInstance)it2.next();
						if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
							continue;
						if(event.getAttributeValuesList(ReactomeJavaConstants.output).contains(entity)) {
							//make link between entity and preceding reaction
							Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(event);
							if (rnode2 == null)
								continue;
							edge = createOutputEdge(rnode2,enode);
							toBeInserted.add(edge);
							setReactionOutputInserted(event,entity);
						}
					}
				} else {
					Vertex enode = getNodeForEntity(entity);
					DefaultEdge edge = createRegulatorEdge(enode,rnode,regulation.getSchemClass().getName());
					toBeInserted.add(edge);
				}
			}
		}
		model.insert(toBeInserted.toArray(),null,null,null,null);
		return toBeInserted;
	}
	
	public List insertOutputEntities(GKInstance reaction) throws InvalidAttributeException, Exception {
		List toBeInserted = new ArrayList(); // new things which will have to be inserted into model
		Set<GKInstance> handledEntity = new HashSet<GKInstance>();
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		List outputEntities = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
		for (Iterator it = outputEntities.iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			if (isReactionOutputInserted(reaction,entity))
				continue;
			if (handledEntity.contains(entity))
				continue;
			handledEntity.add(entity);
			if (entity.getSchemClass().isa(ReactomeJavaConstants.SimpleEntity)) {
				Vertex enode = createNodeForEntity(entity);
				toBeInserted.add(enode);
				DefaultEdge edge = createOutputEdge(rnode,enode);
				//edge.setUserObject(new Integer(Utils.getCountInCollection(entity, outputEntities)));
				toBeInserted.add(edge);
				setReactionOutputInserted(reaction,entity);
			} else {
				Vertex enode = getNodeForEntity(entity);
				DefaultEdge edge = createOutputEdge(rnode,enode);
				toBeInserted.add(edge);
			}
		}
		return toBeInserted;
	}
	
    public Vertex getNodeForEntity (GKInstance entity) throws Exception {
    	Vertex v = this.entityInstance2NodeMap.get(entity);
    	if (v == null) {
    		v = createNodeForEntity(entity);
    		entityInstance2NodeMap.put(entity,v);
    		model.insert(new Object[]{v},null,null,null,null);
    	}
    	return v;
    }
    
	public void resetEntityNodeBounds() throws InvalidAttributeException, Exception {
		Map nested = new Hashtable();
		for (Vertex v : verteces) {
			Rectangle2D bounds = graph.getCellBounds(v);
			//System.out.println(v + "\t" + bounds);
			Map attMap = new Hashtable();
			GraphConstants.setBounds(attMap,new Rectangle((int)bounds.getX(),(int)bounds.getY(),(int)bounds.getWidth(),(int)bounds.getHeight()));
			nested.put(v, attMap);
		}
		model.edit(nested, null, null, null);
	}
}
