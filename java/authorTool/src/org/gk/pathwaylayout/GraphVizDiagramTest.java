package org.gk.pathwaylayout;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.gk.gkEditor.CoordinateSerializer;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.Project;
import org.gk.render.Node;
import org.gk.render.PathwayLayoutHelper;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntity;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableProtein;
import org.gk.render.RenderableReaction;
import org.gk.render.RenderableRegistry;
import org.gk.schema.InvalidAttributeException;
import org.gk.util.SwingImageCreator;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

public class GraphVizDiagramTest extends PathwayByPathwayNoGUI{

	//protected Map<GKInstance, Vertex> entityInstance2NodeMap = Collections.synchronizedMap(new HashMap<GKInstance, Vertex>());
	
	public GraphVizDiagramTest() throws Exception {
	}
	
	public GraphVizDiagramTest(MySQLAdaptor dba, GKInstance pathway) throws Exception {
		GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
		this.dba = dba;
		this.focusSpecies = species;
		this.focusPathway = pathway;
		init2();
		System.out.println("Finished init2()");
		relaxer = new Thread(this, "Relaxer");
		relaxer.start();
		relaxer.join();
	}

	public void run () {
		try {
			graph.setScale(0.6);
			BufferedImage img;
			File imgFile;
//			img = graph.getImage(null, 0);
//			System.out.println("Finished graph.getImage() " + img);
//			imgFile = new File(this.focusPathway.getDBID() + "-1.png");
//			ImageIO.write(img, "png", imgFile);
//			System.out.println("Finished 1st ImageIO.write()");
			RenderablePathway renderablePathway = createRenderablePathwayFromPathwayByPathway(this.focusPathway);
			System.out.println("Finished createRenderablePathwayFromPathwayByPathway()");
			PathwayEditor pathwayEditor = new PathwayEditor(renderablePathway);
			System.out.println("Finished PathwayEditor()");
			// IMPORTANT: Have to call this before attempting to do the layout!!!
			img = SwingImageCreator.createImage(pathwayEditor);
			System.out.println("Finished SwingImageCreator.createImage()");
//			imgFile = new File(this.focusPathway.getDBID() + "-2.png");
//			ImageIO.write(img, "png", imgFile);
//			System.out.println("Finished 2nd ImageIO.write()");
			PathwayLayoutHelper pathwayLayoutHelper = new PathwayLayoutHelper(renderablePathway);
			System.out.println("Finished PathwayLayoutHelper()");
			pathwayLayoutHelper.layout(0);
			System.out.println("Finished layout()");
			img = SwingImageCreator.createImage(pathwayEditor);
			System.out.println("Finished SwingImageCreator.createImage()");
			imgFile = new File(this.focusPathway.getDBID() + "-3.png");
			ImageIO.write(img, "png", imgFile);
			System.out.println("Finished 3rd ImageIO.write()");
			Project project = new Project(renderablePathway);
			CoordinateSerializer serializer = new CoordinateSerializer();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void init2() throws Exception {
		//graph.setBackground(new Color(255,255,255,0));
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
		
		//hiddenEntities.addAll((Collection<GKInstance>) Utils.fetchHiddenEntities(dba));
		insertEntities();
		resetEntityNodeBounds();
		placeEntitiesWithSingleEdge();
		spreadEntityNodesWithIdenticalConnectivity();
		collapseClusteredCatalystVerteces();
		bringReactionNodesToFront();
		relocateVerteces();
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
	
	public void resetEntityNodeBounds() throws InvalidAttributeException, Exception {
		Map nested = new Hashtable();
		for (Vertex v : verteces) {
			Rectangle2D bounds = graph.getCellBounds(v);
			System.out.println(v + "\t" + bounds);
			Map attMap = new Hashtable();
			GraphConstants.setBounds(attMap,new Rectangle((int)bounds.getX(),(int)bounds.getY(),(int)bounds.getWidth(),(int)bounds.getHeight()));
			nested.put(v, attMap);
		}
		model.edit(nested, null, null, null);
	}
	
    public RenderablePathway createRenderablePathwayFromPathwayByPathway (GKInstance pathway) throws Exception {
        Map<Vertex, Node> vToNode = new HashMap<Vertex, Node>();
        RenderablePathway renderablePathway = new RenderablePathway(pathway.getDisplayName());
        RenderableRegistry registry = RenderableRegistry.getRegistry();
        registry.add(renderablePathway);
        List<Vertex> reactionVertex = new ArrayList<Vertex>();
        Node.setWidthRatioOfBoundsToText(1.0d);
        Node.setHeightRatioOfBoundsToText(1.0d);
        Node.setNodeWidth(100);
        for (Vertex v : this.verteces) {
            GKInstance inst = (GKInstance) v.getUserObject();
            Node node = null;
            if (inst.getSchemClass().isa(ReactomeJavaConstants.Reaction)) {
                //node = new RenderableEntity();
                //node.setDisplayName(inst.getDBID() + "");
                reactionVertex.add(v);
                continue;
            }
            else if (inst.getSchemClass().isa(ReactomeJavaConstants.GenomeEncodedEntity)) {
                node = new RenderableProtein();
                assignShortName(node, inst);
            }
            else if (inst.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                node = new RenderableComplex();
                assignShortName(node, inst);
            }
            else {
                node = new RenderableEntity();
                assignShortName(node, inst);
            }
            Node shortcut = (Node) registry.getSingleObject(node.getDisplayName());
            if (shortcut != null) {
                node = (Node) shortcut.generateShortcut();
            }
            else
                registry.add(node);
            renderablePathway.addComponent(node);
            vToNode.put(v, node);
            node.setPosition((int)(v.getBounds().x * PathwayLayoutConstants.ZOOM_LEVELS[0]),
                             (int)(v.getBounds().y * PathwayLayoutConstants.ZOOM_LEVELS[0]));
            node.setBounds(null);
        }
        Map<Vertex, List<Edge>> rxtVertexToEdges = generateRxtVertexToEdges(this.edges);
        for (Vertex rxtVertex : rxtVertexToEdges.keySet()) {
            GKInstance rxtInstance = (GKInstance) rxtVertex.getUserObject();
            List<Edge> edges = rxtVertexToEdges.get(rxtVertex);
            // Create a reaction
            RenderableReaction reaction = new RenderableReaction();
            reaction.setPosition((int)(rxtVertex.getBounds().x * PathwayLayoutConstants.ZOOM_LEVELS[0]),
                                 (int)(rxtVertex.getBounds().y * PathwayLayoutConstants.ZOOM_LEVELS[0]));
            renderablePathway.addComponent(reaction);
            reaction.setDisplayName(rxtInstance.getDisplayName());
            registry.add(reaction);
            // Need to figure out the inputs and outputs
            for (Edge edge : edges) {
                Vertex source = edge.getSourceVertex();
                Vertex target = edge.getTargetVertex();
                if (source == rxtVertex) {
                    Node output = vToNode.get(target);
                    reaction.addOutput(output);
                }
                else if (target == rxtVertex) {
                    Node input = vToNode.get(source);
                    reaction.addInput(input);
                }
            }
        }
        return renderablePathway;
    }
	
    private Map<Vertex, List<Edge>> generateRxtVertexToEdges(Set<Edge> edges) {
        Map<Vertex, List<Edge>> reactionVertexToEdges = new HashMap<Vertex, List<Edge>>();
        for (Edge edge : edges) {
            Vertex source = edge.getSourceVertex();
            GKInstance sourceInst = (GKInstance) source.getUserObject();
            Vertex target = edge.getTargetVertex();
            GKInstance targetInst = (GKInstance) target.getUserObject();
            Vertex rxtVertex = null;
            if (sourceInst.getSchemClass().isa(ReactomeJavaConstants.Reaction)) {
                rxtVertex = source;
            }
            else if (targetInst.getSchemClass().isa(ReactomeJavaConstants.Reaction)) {
                rxtVertex = target;
            }
            if (rxtVertex == null)
                continue;
            List<Edge> list = reactionVertexToEdges.get(rxtVertex);
            if (list == null) {
                list = new ArrayList<Edge>();
                reactionVertexToEdges.put(rxtVertex, list);
            }
            list.add(edge);
        }
        return reactionVertexToEdges;
    }
    
    private void assignShortName(Node node,
                                 GKInstance instance) throws Exception {
        boolean hasBeenAssigned = false;
        if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.shortName)) {
            String name = (String) instance.getAttributeValue(ReactomeJavaConstants.shortName);
            if (name != null) {
                node.setDisplayName(name);
                hasBeenAssigned = true;
            }
        }
        if (!hasBeenAssigned) {
            String shortName = Utils.findShortestName(instance);
            if (shortName != null)
                node.setDisplayName(shortName);
        }
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
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length < 6) {
			System.out.println("Usage java -Djava.awt.headless=true GraphVizDiagramTest dbHost dbName dbUser dbPwd dbPort pathwayDbId");
			System.exit(0);
		}
		try {
			MySQLAdaptor dba = new MySQLAdaptor(args[0],args[1],args[2],args[3],Integer.parseInt(args[4]));
			GKInstance pathway = dba.fetchInstance(new Long(Integer.parseInt(args[5])));
			GraphVizDiagramTest gdt = new GraphVizDiagramTest(dba, pathway);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
