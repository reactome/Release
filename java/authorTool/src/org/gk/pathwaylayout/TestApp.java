/*
 * Created on Mar 31, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.gk.pathwaylayout;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;




/**
 * @author vastrik
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestApp implements Runnable {

	protected Map<GKInstance, Vertex> reactionInstance2NodeMap = Collections.synchronizedMap(new HashMap<GKInstance, Vertex>());
	protected Map<GKInstance, Set> reaction2output = new HashMap<GKInstance, Set>();
	protected Map<GKInstance, Set> reaction2input = new HashMap<GKInstance, Set>();
	protected MySQLAdaptor dba;
	protected GraphModel model = new DefaultGraphModel();
	protected JGraph graph = new JGraph(model);

	protected Set edges = Collections.synchronizedSet(new HashSet());
	protected List<Vertex> verteces = Collections.synchronizedList(new ArrayList<Vertex>());
	protected AttributeMap node2attributes = new AttributeMap();

	protected Thread main;
	protected Thread relaxer;
	public boolean relax = false;
	protected boolean relaxWithoutRepaint = false;
	protected boolean inUpdateView = false;
	protected boolean inRelax = false;
	public boolean save = false;

	protected Map<Vertex, List> node2neighbours = Collections.synchronizedMap(new HashMap<Vertex, List>());
	protected Collection hiddenEntities;
	protected Map<Vertex, Set> enode2rnodes = new HashMap<Vertex, Set>();
	
	public TestApp() throws Exception {
		this.dba = new MySQLAdaptor("localhost","test_gk_central_20070531","ro","loe",3306);
		main = Thread.currentThread();
		init();
	}

	public TestApp(MySQLAdaptor dba) throws Exception {
		this.dba = dba;
		init();
	}

	public void init() throws Exception {
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
		JPanel controlPanel = new JPanel();
		panel.add(controlPanel, BorderLayout.SOUTH);
		JButton relaxButton = new JButton("Stop/start layout");
		relaxButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!relax) {
					unfixOverlappingEntityNodes();
					setNeighbouringVerteces();
				}
				relax = ! relax;
			}
		});
/*		JButton relaxWithoutRepaintButton = new JButton("Do/Don't repaint");
		relaxButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				relaxWithoutRepaint = ! relaxWithoutRepaint;
			}
		});*/
		JButton saveButton = new JButton("Save image");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				save = true;
			}
		});		
		JButton zoomInButton = new JButton("+");
		zoomInButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				graph.setScale(graph.getScale() * PathwayLayoutConstants.ZOOM_STEP);
			}
		});
		JButton zoomOutButton = new JButton("-");
		zoomOutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				graph.setScale(graph.getScale() / PathwayLayoutConstants.ZOOM_STEP);
			}
		});		
		controlPanel.add(relaxButton);
//		controlPanel.add(relaxWithoutRepaintButton);
		controlPanel.add(saveButton);
		controlPanel.add(zoomInButton);
		controlPanel.add(zoomOutButton);
		JButton layoutButton = new JButton("layout");
		layoutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				placeEntitiesWithSingleEdge();
				//setNeighbouringVerteces();
				//nudgeOverlappingNodes();
			}
		});
		controlPanel.add(layoutButton);
		JFrame frame = new JFrame();
		graph.setAntiAliased(true);
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		hiddenEntities = Utils.fetchHiddenEntities(dba);
		insertConnectingEntities();
		//insertCloseEntities();
		//insertEntities();
		//reportEntityVertecesWithMultipleOutputEdges();
		resetEntityNodeBounds();
		placeEntitiesWithSingleEdge();
		//spreadEntityNodesWithIdenticalCentrePoint();
		spreadEntityNodesWithIdenticalConnectivity();
		//unfixOverlappingEntityNodes();
		//setNeighbouringVerteces();
		relaxer = new Thread(this, "Relaxer");
		//relax = true;
		relaxer.start();		
	}

	public static void main(String[] args) throws Exception {
		TestApp a = new TestApp();
		a.printSomeStats();
	}

	public Vertex createNodeForReaction(GKInstance reaction) throws Exception {
		Vertex node = new Vertex(reaction);
		node.setFixed(true);
		Point reactionMidPoint = Utils.getReactionMidPoint(reaction);
		GraphConstants.setBounds(node.getAttributes(), new Rectangle(
				(int)reactionMidPoint.getX()*PathwayLayoutConstants.COORDINATE_SCALING_FACTOR,
				(int)reactionMidPoint.getY()*PathwayLayoutConstants.COORDINATE_SCALING_FACTOR,
				PathwayLayoutConstants.REACTION_NODE_DIAMETER,
				PathwayLayoutConstants.REACTION_NODE_DIAMETER));
//		System.out.printf("%s\t%d\t%d\n",
//				reaction,
//				(int)reactionMidPoint.getX()*PathwayLayoutConstants.COORDINATE_SCALING_FACTOR,
//				(int)reactionMidPoint.getY()*PathwayLayoutConstants.COORDINATE_SCALING_FACTOR);
		GraphConstants.setOpaque(node.getAttributes(), true);
		GraphConstants.setBackground(node.getAttributes(), Color.black);
		//GraphConstants.setBorderColor(node.getAttributes(), Color.black);
		node.addPort();
		reactionInstance2NodeMap.put(reaction,node);
		verteces.add(node);
		return node;
	}

	public Vertex createNodeForEntity(GKInstance entity) throws InvalidAttributeException, Exception {
		Vertex node = new Vertex(entity);
		GraphConstants.setOpaque(node.getAttributes(), true);
		GraphConstants.setResize(node.getAttributes(), true);
		//GraphConstants.setAutoSize(node.getAttributes(), true);
		//GraphConstants.setHorizontalTextPosition(node.getAttributes(), SwingConstants.CENTER);
		//GraphConstants.setBorder(node.getAttributes(), BorderFactory.createRaisedBevelBorder());
		Color c;
		if (entity.getSchemClass().getName().equals(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
			if (entity.getAttributeValue(ReactomeJavaConstants.referenceEntity) != null && 
					((GKInstance)entity.getAttributeValue(ReactomeJavaConstants.referenceEntity)).getSchemClass().getName().equals(ReactomeJavaConstants.ReferencePeptideSequence)) {
				c = PathwayLayoutConstants.PROTEIN_COLOR;
			} else {
				c = PathwayLayoutConstants.SEQUENCE_COLOR;
			}
		} else if (entity.getSchemClass().getName().equals(ReactomeJavaConstants.Complex)) {
			c = PathwayLayoutConstants.COMPLEX_COLOR;
		} else if (entity.getSchemClass().getName().equals(ReactomeJavaConstants.GenomeEncodedEntity)) {
			c = PathwayLayoutConstants.GEE_COLOR;
		} else if (entity.getSchemClass().getName().equals(ReactomeJavaConstants.DefinedSet)) {
			c = PathwayLayoutConstants.DEFINEDSET_COLOR;
		} else if (entity.getSchemClass().getName().equals(ReactomeJavaConstants.CandidateSet)) {
			c = PathwayLayoutConstants.CANDIDATESET_COLOR;
		} else if (entity.getSchemClass().getName().equals(ReactomeJavaConstants.OpenSet)) {
			c = PathwayLayoutConstants.OPENSET_COLOR;
		} else if (entity.getSchemClass().getName().equals(ReactomeJavaConstants.SimpleEntity)) {
			c = PathwayLayoutConstants.SIMPLEENTITY_COLOR;
		} else if (entity.getSchemClass().getName().equals(ReactomeJavaConstants.Polymer)) {
			c = PathwayLayoutConstants.POLYMER_COLOR;
		} else {
			c = PathwayLayoutConstants.OTHER_COLOR;
		}
		GraphConstants.setBackground(node.getAttributes(), c);
		node.addPort();
		verteces.add(node);
		return node;
	}

	public Collection getReactions() throws Exception {
		Collection reactions = Utils.getLocatedReactionsForSpecies(dba,"Homo sapiens");
		//Collection reactions = Utils.getSampleReactions(dba);
		dba.loadInstanceAttributeValues(reactions, new String[]{ReactomeJavaConstants.input,ReactomeJavaConstants.output,ReactomeJavaConstants.catalystActivity});
		dba.loadInstanceReverseAttributeValues(reactions, new String[]{ReactomeJavaConstants.regulatedEntity});
		return reactions;
	}

	public static void moveTest(GraphModel model, DefaultGraphCell vertex) {
		AttributeMap map = model.getAttributes(vertex);
		Map nested = new Hashtable();
		nested.put(vertex,map);
		for (int i = 0; i < 50; i++) {
			map.translate(-1,-1);
			model.edit(nested,null,null,null);
		}
	}

	public void insertEntities() throws Exception {
		List toBeInserted = new ArrayList();
		for (Iterator i = reactionInstance2NodeMap.keySet().iterator(); i.hasNext();) {
			GKInstance reaction = (GKInstance)i.next();
			List graphCells = insertEntities(reaction);
			toBeInserted.addAll(graphCells);
		}
		// Now do 2nd pass to insert remaining outputs
		for (Iterator i = reactionInstance2NodeMap.keySet().iterator(); i.hasNext();) {
			GKInstance reaction = (GKInstance)i.next();
			List graphCells = insertOutputEntitiesIfNecessary(reaction);
			toBeInserted.addAll(graphCells);
		}
		model.insert(toBeInserted.toArray(),null,null,null,null);
	}

	public void insertConnectingEntities() throws Exception {
		List toBeInserted = new ArrayList();
		for (Iterator i = reactionInstance2NodeMap.keySet().iterator(); i.hasNext();) {
			GKInstance reaction = (GKInstance)i.next();
			List graphCells = null;
//			if ((reaction.getAttributeValue(ReactomeJavaConstants.precedingEvent) == null) && (reaction.getReferers(ReactomeJavaConstants.precedingEvent) == null)) {
//				//System.out.printf("1st pass\tinsertEntitiesOfIsolatedReaction\t%s\n",reaction);
//				graphCells = insertEntitiesOfIsolatedReaction(reaction);
//			} else {
//				//System.out.printf("1st pass\tinsertConnectingEntitiesOnce\t%s\n",reaction);
//				graphCells = insertConnectingEntitiesOnce(reaction);
//			}
			//List graphCells = insertConnectingEntities(reaction);
			graphCells = insertCloseConnectingEntitiesOnce(reaction);
			if ((graphCells != null) && !graphCells.isEmpty())
				toBeInserted.addAll(graphCells);
		}
		// Now do 2nd pass to insert remaining outputs
		for (Iterator i = reactionInstance2NodeMap.keySet().iterator(); i.hasNext();) {
			GKInstance reaction = (GKInstance)i.next();
			List graphCells;
//			if ((reaction.getAttributeValue(ReactomeJavaConstants.precedingEvent) != null) || (reaction.getReferers(ReactomeJavaConstants.precedingEvent) != null)) {
//				//System.out.printf("2nd pass\tinsertTerminalOutputEntitiesIfNecessary\t%s\n",reaction);
//				//graphCells = insertTerminalOutputEntitiesIfNecessary(reaction);
//				graphCells = insertSomeOutputEntitiesIfNecessary(reaction);
//				if ((graphCells != null) && !graphCells.isEmpty())
//					toBeInserted.addAll(graphCells);
//			}
			graphCells = insertSomeOutputEntitiesIfNecessary(reaction);
			if ((graphCells != null) && !graphCells.isEmpty())
				toBeInserted.addAll(graphCells);
		}
		model.insert(toBeInserted.toArray(),null,null,null,null);
	}

	/*
	 * This method is derived from insertConnectingEntities. However, unlike insertConnectingEntities it
	 * inserts <B>all</B> entities and not just the connecting ones + "necessary" others.
	 */
	public void insertCloseEntities() throws Exception {
		List toBeInserted = new ArrayList();
		for (Iterator i = reactionInstance2NodeMap.keySet().iterator(); i.hasNext();) {
			GKInstance reaction = (GKInstance)i.next();
			List graphCells = null;
			graphCells = insertCloseEntities(reaction);
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

	/*
	 * Insert all output entities whicg=h haven't been inserted already.
	 */
	private List insertOutputEntities(GKInstance reaction) throws InvalidAttributeException, Exception {
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
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createOutputEdge(rnode,enode);
			//edge.setUserObject(new Integer(Utils.getCountInCollection(entity, outputEntities)));
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // I'm just lazy and want to use the existing method (setEntityNodeBounds(Vertex, Set))
			reactionNodes.add(rnode);
			setEntityNodeBounds(enode, reactionNodes);
			setReactionOutputInserted(reaction,entity);
		}
		return toBeInserted;
	}
	
	/**
	 * This method inserts output entities which have not been inserted already and which are not on the list
	 * of hidden entities.
	 * @param reaction
	 * @return
	 * @throws Exception
	 * @throws InvalidAttributeException
	 */
	private List insertOutputEntitiesIfNecessary(GKInstance reaction) throws InvalidAttributeException, Exception {
		List toBeInserted = new ArrayList(); // new things which will have to be inserted into model
		Set<GKInstance> handledEntity = new HashSet<GKInstance>();
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		List outputEntities = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
		for (Iterator it = outputEntities.iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			if (isReactionOutputInserted(reaction,entity))
				continue;
			if (isHiddenEntity(entity))
				continue;
			if (handledEntity.contains(entity))
				continue;
			handledEntity.add(entity);
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createOutputEdge(rnode,enode);
			//edge.setUserObject(new Integer(Utils.getCountInCollection(entity, outputEntities)));
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // I'm just lazy and want to use the existing method (setEntityNodeBounds(Vertex, Set))
			reactionNodes.add(rnode);
			setEntityNodeBounds(enode, reactionNodes);
			setReactionOutputInserted(reaction,entity);
		}
		return toBeInserted;
	}

	private List insertTerminalOutputEntitiesIfNecessary(GKInstance reaction) throws InvalidAttributeException, Exception {
		List toBeInserted = new ArrayList(); // new things which will have to be inserted into model
		Map entityToCountMap = new HashMap();
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.output).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			if (isReactionOutputInserted(reaction,entity))
				continue;
			if(isHiddenEntity(entity))
				continue;
			// Only insert entities which are not used as an input anywhere. However, want to consider only localised Reactions
			Collection consumingEvents = entity.getReferers(ReactomeJavaConstants.input);
			if (consumingEvents != null) { 
				Collection<GKInstance> filteredConsumingEvents = new ArrayList<GKInstance>();
				for (Iterator cei = consumingEvents.iterator(); cei.hasNext();) {
					GKInstance consumingEvent = (GKInstance)cei.next();
					if (reactionInstance2NodeMap.containsKey(consumingEvent)) 
						filteredConsumingEvents.add(consumingEvent);
				}
				if (!filteredConsumingEvents.isEmpty()) {
					continue;
				}
			}
			Integer count;
			if ((count = (Integer) entityToCountMap.get(entity)) == null) {
				entityToCountMap.put(entity,new Integer(1));
			} else {
				entityToCountMap.put(entity, new Integer(count.intValue() + 1));
				continue;
			}
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createOutputEdge(rnode,enode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // I'm just lazy and want to use the existing method (setEntityNodeBounds(Vertex, Set))
			reactionNodes.add(rnode);
			setEntityNodeBounds(enode, reactionNodes);
		}
		for (Iterator i = entityToCountMap.keySet().iterator(); i.hasNext();) {
			GKInstance entity = (GKInstance)i.next();
			setReactionOutputInserted(reaction,entity);
		}
		return toBeInserted;
	}	

	/**
	 * This method inserts output entities only if none of the outputs has been inserted thus far.
	 * Preference is given to "terminal" entities, i.e. those entities which are not used as an input anywhere.
	 * If none exists, other, non-hidden entities are inserted. If none of those exists either, hidden entities
	 * are inserted.
	 * @param reaction
	 * @return list of things to be inserted into the graph
	 * @throws InvalidAttributeException
	 * @throws Exception
	 */
	private List insertSomeOutputEntitiesIfNecessary(GKInstance reaction) throws InvalidAttributeException, Exception {
		if ((reaction.getAttributeValue(ReactomeJavaConstants.output) == null) || isReactionAnyOutputInserted(reaction))
			return null;
		Set<GKInstance> hiddenEntities = new HashSet<GKInstance>();
		Set<GKInstance> terminalEntities = new HashSet<GKInstance>();
		Set<GKInstance> otherEntities = new HashSet<GKInstance>();
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		List outputEntities = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
		for (Iterator it = outputEntities.iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			Integer count;
			if(isHiddenEntity(entity)) {
				hiddenEntities.add(entity);
			} else {
				Collection consumingEvents = entity.getReferers(ReactomeJavaConstants.input);
				if (consumingEvents != null) { 
					Collection<GKInstance> filteredConsumingEvents = new ArrayList<GKInstance>();
					for (Iterator cei = consumingEvents.iterator(); cei.hasNext();) {
						GKInstance consumingEvent = (GKInstance)cei.next();
						if (reactionInstance2NodeMap.containsKey(consumingEvent)) 
							filteredConsumingEvents.add(consumingEvent);
					}
					if (filteredConsumingEvents.isEmpty())
						terminalEntities.add(entity);
					else
						otherEntities.add(entity);
				} else {
					terminalEntities.add(entity);
				}
			}
		}
		Set<GKInstance> entities;
		if (!terminalEntities.isEmpty())
			entities = terminalEntities;
		else if (!otherEntities.isEmpty())
			entities = otherEntities;
		else
			entities = hiddenEntities;
		List toBeInserted = new ArrayList(); // new things which will have to be inserted into model
		for (GKInstance entity : entities) {
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createOutputEdge(rnode,enode);
			//edge.setUserObject(new Integer(Utils.getCountInCollection(entity, outputEntities)));
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet();
			reactionNodes.add(rnode);
			setEntityNodeBounds(enode, reactionNodes);
			setReactionOutputInserted(reaction,entity);
		}
		return toBeInserted;
	}	
	
	public List insertEntities(GKInstance reaction) throws InvalidAttributeException, Exception {
		List toBeInserted = new ArrayList(); // new things which will have to be inserted into model
		Map entityToCountMap = new HashMap();
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.input).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			Integer count;
			if ((count = (Integer) entityToCountMap.get(entity)) == null) {
				entityToCountMap.put(entity,new Integer(1));
			} else {
				entityToCountMap.put(entity, new Integer(count.intValue() + 1));
				continue;
			}
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createInputEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
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
					reactionNodes.add(rnode2);
					setReactionOutputInserted(event,entity);
				}
			}
			setEntityNodeBounds(enode, reactionNodes);
		}
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance)((GKInstance) it.next()).getAttributeValue(ReactomeJavaConstants.physicalEntity);
			// in case there are CatalystActivities w/o physicalEntity
			if (entity == null)
				continue;
			// skip catalysts which are also input
			if (entityToCountMap.containsKey(entity))
				continue;
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createCatalystEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
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
					reactionNodes.add(rnode2);
					setReactionOutputInserted(event,entity);
				}
			}
			setEntityNodeBounds(enode, reactionNodes);
		}
		Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
		if (regulations != null) {
			for (Iterator it = regulations.iterator(); it.hasNext();) {
				GKInstance regulation = (GKInstance)it.next();
				GKInstance entity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
				if (entity == null || !entity.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
					continue;
				Vertex enode = createNodeForEntity(entity);
				toBeInserted.add(enode);
				DefaultEdge edge = createRegulatorEdge(enode,rnode,regulation.getSchemClass().getName());
				toBeInserted.add(edge);
				Set reactionNodes = new HashSet(); // for calculating the entity node initial position
				reactionNodes.add(rnode);
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
						reactionNodes.add(rnode2);
						setReactionOutputInserted(event,entity);
					}
				}
				setEntityNodeBounds(enode, reactionNodes);
			}
		}
		return toBeInserted;
	}

	public List insertEntitiesOfIsolatedReaction(GKInstance reaction) throws InvalidAttributeException, Exception {
		List toBeInserted = new ArrayList(); // new things which will have to be inserted into model
		Map entityToCountMap = new HashMap();
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.input).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			if (isHiddenEntity(entity))
				continue;
			Integer count;
			if ((count = (Integer) entityToCountMap.get(entity)) == null) {
				entityToCountMap.put(entity,new Integer(1));
			} else {
				entityToCountMap.put(entity, new Integer(count.intValue() + 1));
				continue;
			}
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createInputEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
			setEntityNodeBounds(enode, reactionNodes);
		}
		entityToCountMap.clear();
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.output).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			if (isHiddenEntity(entity))
				continue;
			Integer count;
			if ((count = (Integer) entityToCountMap.get(entity)) == null) {
				entityToCountMap.put(entity,new Integer(1));
			} else {
				entityToCountMap.put(entity, new Integer(count.intValue() + 1));
				continue;
			}
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createOutputEdge(rnode,enode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
			setEntityNodeBounds(enode, reactionNodes);
		}
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance)((GKInstance) it.next()).getAttributeValue(ReactomeJavaConstants.physicalEntity);
			// in case there are CatalystActivities w/o physicalEntity
			if (entity == null)
				continue;
			// skip catalysts which are also input
			if (entityToCountMap.containsKey(entity))
				continue;
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createCatalystEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
			setEntityNodeBounds(enode, reactionNodes);
		}
		Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
		if (regulations != null) {
			for (Iterator it = regulations.iterator(); it.hasNext();) {
				GKInstance regulation = (GKInstance)it.next();
				GKInstance entity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
				if (entity == null || !entity.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
					continue;
				Vertex enode = createNodeForEntity(entity);
				toBeInserted.add(enode);
				DefaultEdge edge = createRegulatorEdge(enode,rnode,regulation.getSchemClass().getName());
				toBeInserted.add(edge);
				Set reactionNodes = new HashSet(); // for calculating the entity node initial position
				reactionNodes.add(rnode);
				setEntityNodeBounds(enode, reactionNodes);
			}
		}
		return toBeInserted;
	}
	
	public List insertConnectingEntities(GKInstance reaction) throws InvalidAttributeException, Exception {
		List toBeInserted = new ArrayList(); // new things which will have to be inserted into model
		Map entityToCountMap = new HashMap();
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.input).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			List helperList = new ArrayList();
			for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
				GKInstance event = (GKInstance)it2.next();
				if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
					continue;
				if(event.getAttributeValuesList(ReactomeJavaConstants.output).contains(entity)) {
					helperList.add(event);
				}
			}
			// skip entities not produced by explicit preceding events but are nevertheless produced
			if (helperList.isEmpty() && (entity.getReferers(ReactomeJavaConstants.output) != null)) {
				continue;
			}
			Integer count;
			if ((count = (Integer) entityToCountMap.get(entity)) == null) {
				entityToCountMap.put(entity,new Integer(1));
			} else {
				entityToCountMap.put(entity, new Integer(count.intValue() + 1));
				continue;
			}
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createInputEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);

			for (Iterator it2 = helperList.iterator(); it2.hasNext();) {
				GKInstance event = (GKInstance)it2.next();
				//make link between entity and preceding reaction
				Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(event);
				if (rnode2 == null)
					continue;
				edge = createOutputEdge(rnode2,enode);
				toBeInserted.add(edge);
				reactionNodes.add(rnode2);
				setReactionOutputInserted(event,entity);
			}
			setEntityNodeBounds(enode, reactionNodes);			
		}
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance)((GKInstance) it.next()).getAttributeValue(ReactomeJavaConstants.physicalEntity);
			// in case there are CatalystActivities w/o physicalEntity
			if (entity == null)
				continue;
			// skip catalysts which are also input
			if (entityToCountMap.containsKey(entity))
				continue;
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createCatalystEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
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
					reactionNodes.add(rnode2);
					setReactionOutputInserted(event,entity);
				}
			}
			setEntityNodeBounds(enode, reactionNodes);
		}
		Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
		if (regulations != null) {
			for (Iterator it = regulations.iterator(); it.hasNext();) {
				GKInstance regulation = (GKInstance)it.next();
				GKInstance entity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
				if (entity == null || !entity.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
					continue;
				Vertex enode = createNodeForEntity(entity);
				toBeInserted.add(enode);
				DefaultEdge edge = createRegulatorEdge(enode,rnode,regulation.getSchemClass().getName());
				toBeInserted.add(edge);
				Set reactionNodes = new HashSet(); // for calculating the entity node initial position
				reactionNodes.add(rnode);
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
						reactionNodes.add(rnode2);
						setReactionOutputInserted(event,entity);
					}
				}
				setEntityNodeBounds(enode, reactionNodes);
			}
		}
		return toBeInserted;
	}

	public List insertConnectingEntitiesOnce(GKInstance reaction) throws InvalidAttributeException, Exception {
		List<DefaultGraphCell> toBeInserted = new ArrayList<DefaultGraphCell>(); // new things which will have to be inserted into model
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.input).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			if (isReactionInputInserted(reaction, entity)) 
				continue;
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
			// skip entities not produced by explicit preceding events and which are on the list of "hidden" entities
			if (helperList.isEmpty() && helperList2.isEmpty() && isHiddenEntity(entity)) {
				continue;
			}
/*			// skip entities not produced by explicit preceding events but are nevertheless produced
			if (helperList.isEmpty() && helperList2.isEmpty() && (entity.getReferers(ReactomeJavaConstants.output) != null)) {
				continue;
			}*/
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
			Set<Vertex> reactionNodes = new HashSet<Vertex>(); // for calculating the entity node initial position
			
			for (GKInstance followingEvent : followingEvents) {
				Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(followingEvent);
				if (rnode2 == null)
					continue;
				DefaultEdge edge = createInputEdge(enode, rnode2);
				toBeInserted.add(edge);
				reactionNodes.add(rnode2);
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
				reactionNodes.add(rnode2);
				setReactionOutputInserted(event,entity);
			}
			setEntityNodeBounds(enode, reactionNodes);			
		}
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance)((GKInstance) it.next()).getAttributeValue(ReactomeJavaConstants.physicalEntity);
			// in case there are CatalystActivities w/o physicalEntity
			if (entity == null)
				continue;
			// skip catalysts which are also input
			if (reaction.getAttributeValuesList(ReactomeJavaConstants.input).contains(entity))
				continue;
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createCatalystEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
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
					reactionNodes.add(rnode2);
					setReactionOutputInserted(event,entity);
				}
			}
			setEntityNodeBounds(enode, reactionNodes);
		}
		Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
		if (regulations != null) {
			for (Iterator it = regulations.iterator(); it.hasNext();) {
				GKInstance regulation = (GKInstance)it.next();
				GKInstance entity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
				if (entity == null || !entity.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
					continue;
				Vertex enode = createNodeForEntity(entity);
				toBeInserted.add(enode);
				DefaultEdge edge = createRegulatorEdge(enode,rnode,regulation.getSchemClass().getName());
				toBeInserted.add(edge);
				Set reactionNodes = new HashSet(); // for calculating the entity node initial position
				reactionNodes.add(rnode);
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
						reactionNodes.add(rnode2);
						setReactionOutputInserted(event,entity);
					}
				}
				setEntityNodeBounds(enode, reactionNodes);
			}
		}
		return toBeInserted;
	}
	
	public List insertCloseConnectingEntitiesOnce (GKInstance reaction) throws InvalidAttributeException, Exception {
		List<DefaultGraphCell> toBeInserted = new ArrayList<DefaultGraphCell>(); // new things which will have to be inserted into model
		List<DefaultGraphCell> l = insertSomeInputEntitiesIfNecessary(reaction);
		if (l != null && !l.isEmpty())
			toBeInserted.addAll(l);
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		Set<GKInstance> seen = new HashSet<GKInstance>();
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance)((GKInstance) it.next()).getAttributeValue(ReactomeJavaConstants.physicalEntity);
			// in case there are CatalystActivities w/o physicalEntity
			if (entity == null)
				continue;
			// There are some events with multiple CatalystActivities but same PhysicalEntity
			if (seen.contains(entity))
				continue;
			seen.add(entity);
			// skip catalysts which are also input
			if (reaction.getAttributeValuesList(ReactomeJavaConstants.input).contains(entity))
				continue;
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createCatalystEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
			for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
				GKInstance event = (GKInstance)it2.next();
//				if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
//					continue;
				if (!reactionInstance2NodeMap.containsKey(event))
					continue;
				if (reactionDistance(reaction, event) > PathwayLayoutConstants.MAX_REACTION_DISTANCE_FOR_SHARED_ENTITIES) {
					System.err.println("TOO FAR:\t" + event + "\t->\t" + entity + "\t->\t" + reaction);
					continue;
				}
				if(event.getAttributeValuesList(ReactomeJavaConstants.output).contains(entity)) {
					//make link between entity and preceding reaction
					Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(event);
					if (rnode2 == null)
						continue;
					edge = createOutputEdge(rnode2,enode);
					toBeInserted.add(edge);
					reactionNodes.add(rnode2);
					setReactionOutputInserted(event,entity);
				}
			}
			setEntityNodeBounds(enode, reactionNodes);
		}
		seen.clear();
		Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
		if (regulations != null) {
			for (Iterator it = regulations.iterator(); it.hasNext();) {
				GKInstance regulation = (GKInstance)it.next();
				GKInstance entity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
				if (entity == null || !entity.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
					continue;
				Vertex enode = createNodeForEntity(entity);
				toBeInserted.add(enode);
				DefaultEdge edge = createRegulatorEdge(enode,rnode,regulation.getSchemClass().getName());
				toBeInserted.add(edge);
				Set reactionNodes = new HashSet(); // for calculating the entity node initial position
				reactionNodes.add(rnode);
				for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
					GKInstance event = (GKInstance)it2.next();
//					if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
//						continue;
					if (!reactionInstance2NodeMap.containsKey(event))
						continue;
					if (reactionDistance(reaction, event) > PathwayLayoutConstants.MAX_REACTION_DISTANCE_FOR_SHARED_ENTITIES) {
						System.err.println("TOO FAR:\t" + event + "\t->\t" + entity + "\t->\t" + reaction);
						continue;
					}
					if(event.getAttributeValuesList(ReactomeJavaConstants.output).contains(entity)) {
						//make link between entity and preceding reaction
						Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(event);
						if (rnode2 == null)
							continue;
						edge = createOutputEdge(rnode2,enode);
						toBeInserted.add(edge);
						reactionNodes.add(rnode2);
						setReactionOutputInserted(event,entity);
					}
				}
				setEntityNodeBounds(enode, reactionNodes);
			}
		}
		return toBeInserted;
	}
	
	public List insertCloseEntities (GKInstance reaction) throws InvalidAttributeException, Exception {
		List<DefaultGraphCell> toBeInserted = new ArrayList<DefaultGraphCell>(); // new things which will have to be inserted into model
		Set<GKInstance>inputEntities = new HashSet();
		inputEntities.addAll(reaction.getAttributeValuesList(ReactomeJavaConstants.input));
		List<DefaultGraphCell> l = insertConnectingInputEntities(reaction, inputEntities);
		if (l != null && !l.isEmpty())
			toBeInserted.addAll(l);
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		Set<GKInstance> seen = new HashSet<GKInstance>();
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance)((GKInstance) it.next()).getAttributeValue(ReactomeJavaConstants.physicalEntity);
			// in case there are CatalystActivities w/o physicalEntity
			if (entity == null)
				continue;
			// There are some events with multiple CatalystActivities but same PhysicalEntity
			if (seen.contains(entity))
				continue;
			seen.add(entity);
			// skip catalysts which are also input
			if (reaction.getAttributeValuesList(ReactomeJavaConstants.input).contains(entity))
				continue;
			Vertex enode = createNodeForEntity(entity);
			toBeInserted.add(enode);
			DefaultEdge edge = createCatalystEdge(enode,rnode);
			toBeInserted.add(edge);
			Set reactionNodes = new HashSet(); // for calculating the entity node initial position
			reactionNodes.add(rnode);
			for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
				GKInstance event = (GKInstance)it2.next();
//				if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
//					continue;
				if (!reactionInstance2NodeMap.containsKey(event))
					continue;
				if (reactionDistance(reaction, event) > PathwayLayoutConstants.MAX_REACTION_DISTANCE_FOR_SHARED_ENTITIES) {
					System.err.println("TOO FAR:\t" + event + "\t->\t" + entity + "\t->\t" + reaction);
					continue;
				}
				if(event.getAttributeValuesList(ReactomeJavaConstants.output).contains(entity)) {
					//make link between entity and preceding reaction
					Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(event);
					if (rnode2 == null)
						continue;
					edge = createOutputEdge(rnode2,enode);
					toBeInserted.add(edge);
					reactionNodes.add(rnode2);
					setReactionOutputInserted(event,entity);
				}
			}
			setEntityNodeBounds(enode, reactionNodes);
		}
		seen.clear();
		Collection regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
		if (regulations != null) {
			for (Iterator it = regulations.iterator(); it.hasNext();) {
				GKInstance regulation = (GKInstance)it.next();
				GKInstance entity = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
				if (entity == null || !entity.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
					continue;
				Vertex enode = createNodeForEntity(entity);
				toBeInserted.add(enode);
				DefaultEdge edge = createRegulatorEdge(enode,rnode,regulation.getSchemClass().getName());
				toBeInserted.add(edge);
				Set reactionNodes = new HashSet(); // for calculating the entity node initial position
				reactionNodes.add(rnode);
				for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
					GKInstance event = (GKInstance)it2.next();
//					if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
//						continue;
					if (!reactionInstance2NodeMap.containsKey(event))
						continue;
					if (reactionDistance(reaction, event) > PathwayLayoutConstants.MAX_REACTION_DISTANCE_FOR_SHARED_ENTITIES) {
						System.err.println("TOO FAR:\t" + event + "\t->\t" + entity + "\t->\t" + reaction);
						continue;
					}
					if(event.getAttributeValuesList(ReactomeJavaConstants.output).contains(entity)) {
						//make link between entity and preceding reaction
						Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(event);
						if (rnode2 == null)
							continue;
						edge = createOutputEdge(rnode2,enode);
						toBeInserted.add(edge);
						reactionNodes.add(rnode2);
						setReactionOutputInserted(event,entity);
					}
				}
				setEntityNodeBounds(enode, reactionNodes);
			}
		}
		return toBeInserted;
	}
	
	private List<DefaultGraphCell> insertSomeInputEntitiesIfNecessary(GKInstance reaction) throws InvalidAttributeException, Exception {
		if (reaction.getAttributeValue(ReactomeJavaConstants.input) == null)
			return null;
		int CONNECTING_ENTITY_IDX = 0;
		int CONNECTING_VIA_SET_ENTITY_IDX = 1;
		int TERMINAL_ENTITY_IDX = 2;
		int OTHER_ENTITY_IDX = 3;
		int HIDDEN_ENTITY_IDX = 4;
		int INSERTED_ENTITY_IDX = 5;
		Set<GKInstance>[] classifiedEntities= new HashSet[]{new HashSet<GKInstance>(),new HashSet<GKInstance>(),new HashSet<GKInstance>(),
				new HashSet<GKInstance>(),new HashSet<GKInstance>(),new HashSet<GKInstance>()};
		for (Iterator it = reaction.getAttributeValuesList(ReactomeJavaConstants.input).iterator(); it.hasNext();) {
			GKInstance entity = (GKInstance) it.next();
			boolean entityIsClassified = false;
			if (isReactionInputInserted(reaction, entity)) {
				classifiedEntities[INSERTED_ENTITY_IDX].add(entity);
				continue;
			}
			for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
				GKInstance event = (GKInstance)it2.next();
//				if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
//					continue;
				if (!reactionInstance2NodeMap.containsKey(event))
					continue;
				List<GKInstance> outputs = event.getAttributeValuesList(ReactomeJavaConstants.output);
				if(outputs.contains(entity)) {
					classifiedEntities[CONNECTING_ENTITY_IDX].add(entity);
					entityIsClassified = true;
					break;
				}
/*				GKInstance relatedEntity = Utils.grepCollectionForSetMemberOrContainingSets(outputs, entity);
				if (relatedEntity != null) {
					classifiedEntities[CONNECTING_VIA_SET_ENTITY_IDX].add(entity);
					entityIsClassified = true;
				}*/
			}
			if (entityIsClassified) continue;
			if (entity.getReferers(ReactomeJavaConstants.output) == null) {
				classifiedEntities[TERMINAL_ENTITY_IDX].add(entity);
				continue;
			}
			if (isHiddenEntity(entity)) {
				classifiedEntities[HIDDEN_ENTITY_IDX].add(entity);
				continue;
			}
			classifiedEntities[OTHER_ENTITY_IDX].add(entity);
		}
		/*
		 * Now try to remove hidden entities from connecting entities
		 */
		if (!classifiedEntities[CONNECTING_ENTITY_IDX].isEmpty()) {
			Set<GKInstance> helperSet = new HashSet<GKInstance>();
			for (GKInstance entity : classifiedEntities[CONNECTING_ENTITY_IDX]) {
				if (!isHiddenEntity(entity))
					helperSet.add(entity);
			}
			if (!helperSet.isEmpty())
				classifiedEntities[CONNECTING_ENTITY_IDX] = helperSet;
		}
		
		if (classifiedEntities[CONNECTING_ENTITY_IDX].isEmpty() && classifiedEntities[CONNECTING_VIA_SET_ENTITY_IDX].isEmpty()) {
			if (!classifiedEntities[INSERTED_ENTITY_IDX].isEmpty()) {
				return null;
			} else if (!classifiedEntities[TERMINAL_ENTITY_IDX].isEmpty()) {
				return insertEntityAsInput(reaction, classifiedEntities[TERMINAL_ENTITY_IDX]);
			} else if (!classifiedEntities[OTHER_ENTITY_IDX].isEmpty()) {
				return insertEntityAsInput(reaction, classifiedEntities[OTHER_ENTITY_IDX]);
			} else if (!classifiedEntities[HIDDEN_ENTITY_IDX].isEmpty()) {
				return insertEntityAsInput(reaction, classifiedEntities[HIDDEN_ENTITY_IDX]);
			}
		} else {
			return insertConnectingInputEntities(reaction, classifiedEntities[CONNECTING_ENTITY_IDX]);
		}
		return null;
	}
	
	private List insertEntityAsInput (GKInstance reaction, Set entities) throws Exception {
		List toBeInserted = new ArrayList();
		for (Iterator i = entities.iterator(); i.hasNext();) {
			GKInstance entity = (GKInstance)i.next();
			toBeInserted.addAll(insertEntityAsInput(reaction, entity));
		}
		return toBeInserted;
	}
	
	private List insertEntityAsInput (GKInstance reaction, GKInstance entity) throws Exception {
		List toBeInserted = new ArrayList();
		setReactionInputInserted(reaction, entity);
		Vertex rnode = (Vertex)reactionInstance2NodeMap.get(reaction);
		Vertex enode = createNodeForEntity(entity);
		setEntityNodeAboveReactionNode(enode, rnode);
		toBeInserted.add(enode);
		DefaultEdge edge = createInputEdge(enode,rnode);
		int stochiometry = Utils.getCountInCollection(entity, reaction.getAttributeValuesList(ReactomeJavaConstants.input));
		//edge.setUserObject(new Integer(stochiometry));
		toBeInserted.add(edge);
		return toBeInserted;
	}
	
	private List insertConnectingInputEntities (GKInstance reaction, Set entities) throws Exception {
		List toBeInserted = new ArrayList();
		for (Iterator i = entities.iterator(); i.hasNext();) {
			GKInstance entity = (GKInstance)i.next();
			if (!isReactionInputInserted(reaction, entity))
				toBeInserted.addAll(insertConnectingInputEntities(reaction, entity));
		}
		return toBeInserted;
	}
	
	private List insertConnectingInputEntities (GKInstance reaction, GKInstance entity) throws Exception {
		List toBeInserted = new ArrayList();
		setReactionInputInserted(reaction, entity);
		List<GKInstance> helperList = new ArrayList<GKInstance>();
		for (Iterator it2 = reaction.getAttributeValuesList(ReactomeJavaConstants.precedingEvent).iterator(); it2.hasNext();) {
			GKInstance event = (GKInstance)it2.next();
//			if(!event.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
//				continue;
			if (!reactionInstance2NodeMap.containsKey(event))
				continue;
			if (reactionDistance(reaction, event) > PathwayLayoutConstants.MAX_REACTION_DISTANCE_FOR_SHARED_ENTITIES) {
				System.err.println("TOO FAR:\t" + event + "\t->\t" + entity + "\t->\t" + reaction);
				continue;
			}
			List<GKInstance> outputs = event.getAttributeValuesList(ReactomeJavaConstants.output);
			if(outputs.contains(entity)) {
				helperList.add(event);
			}
		}
		Set<GKInstance> followingEvents = new HashSet<GKInstance>();
		followingEvents.add(reaction);
		for (GKInstance precedingEvent : helperList) {
			Set<GKInstance> followingEvents2 = Utils.getFollowingEventsConnectedOverInputEntity(precedingEvent, entity);
			for (GKInstance followingEvent : followingEvents2) {
				if (!reactionInstance2NodeMap.containsKey(followingEvent))
					continue;
				if (reactionDistance(precedingEvent, followingEvent) > PathwayLayoutConstants.MAX_REACTION_DISTANCE_FOR_SHARED_ENTITIES) {
					System.err.println("TOO FAR:\t" + precedingEvent + "\t->\t" + entity + "\t->\t" + followingEvent);
					continue;
				}
				// Consider also checking the distance of this reaction and the followingEvent
				followingEvents.add(followingEvent);
			}
		}

		Vertex enode = createNodeForEntity(entity);
		toBeInserted.add(enode);
		Set<Vertex> reactionNodes = new HashSet<Vertex>(); // for calculating the entity node initial position
		
		for (GKInstance followingEvent : followingEvents) {
			Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(followingEvent);
			if (rnode2 == null)
				continue;
			DefaultEdge edge = createInputEdge(enode,rnode2);
			int stochiometry = Utils.getCountInCollection(entity, followingEvent.getAttributeValuesList(ReactomeJavaConstants.input));
			//edge.setUserObject(new Integer(stochiometry));
			toBeInserted.add(edge);
			reactionNodes.add(rnode2);
			setReactionInputInserted(followingEvent,entity);
		}

		for (Iterator it2 = helperList.iterator(); it2.hasNext();) {
			GKInstance event = (GKInstance)it2.next();
			//make link between entity and preceding reaction
			Vertex rnode2 = (Vertex)reactionInstance2NodeMap.get(event);
			if (rnode2 == null)
				continue;
			DefaultEdge edge = createOutputEdge(rnode2,enode);
			int stochiometry = Utils.getCountInCollection(entity, event.getAttributeValuesList(ReactomeJavaConstants.output));
			//edge.setUserObject(new Integer(stochiometry));
			toBeInserted.add(edge);
			reactionNodes.add(rnode2);
			setReactionOutputInserted(event,entity);
		}
		setEntityNodeBounds(enode, reactionNodes);	
		return toBeInserted;
	}
	
	private void setReactionInputInserted(GKInstance reaction, GKInstance entity) {
		Set<GKInstance> r2i = (Set)reaction2input.get(reaction);
		if (r2i== null) {
			r2i = new HashSet<GKInstance>();
			reaction2input.put(reaction,r2i);
		}
		r2i.add(entity);
	}

	private void setReactionOutputInserted(GKInstance reaction, GKInstance entity) {
		Set<GKInstance> r2o = (Set)reaction2output.get(reaction);
		if (r2o == null) {
			r2o = new HashSet<GKInstance>();
			reaction2output.put(reaction,r2o);
		}
		r2o.add(entity);
	}

	private boolean isReactionOutputInserted(GKInstance reaction, GKInstance entity) {
		Set r2o = (Set)reaction2output.get(reaction);
		if (r2o == null)
			return false;
		if (r2o.contains(entity))
			return true;
		return false;
	}
	
	private boolean isReactionAnyOutputInserted(GKInstance reaction) {
		Set r2o = (Set)reaction2output.get(reaction);
		if (r2o == null)
			return false;
		if (!r2o.isEmpty())
			return true;
		return false;
	}
	
	private boolean isReactionInputInserted(GKInstance reaction, GKInstance entity) {
		Set r2i = (Set)reaction2input.get(reaction);
		if (r2i == null)
			return false;
		if (r2i.contains(entity))
			return true;
		return false;
	}

	public Edge createEdge(Vertex from, Vertex to, int edgeType) {
		Edge edge = new Edge(from, to);
		edge.setType(edgeType);
		GraphConstants.setLineWidth(edge.getAttributes(), 3.0f);
		edges.add(edge);
		return edge;
	}

	public Edge createInputEdge(Vertex from, Vertex to) {
		Edge edge = createEdge(from, to, PathwayLayoutConstants.INPUT_EDGE);
		GraphConstants.setLineColor(edge.getAttributes(), PathwayLayoutConstants.INPUT_EDGE_COLOR);
		GraphConstants.setLineEnd(edge.getAttributes(), PathwayLayoutConstants.INPUT_EDGE_END);
		GraphConstants.setEndFill(edge.getAttributes(), PathwayLayoutConstants.INPUT_EDGE_END_FILL);
		return edge;
	}

	public Edge createOutputEdge(Vertex from, Vertex to) {
		Edge edge = createEdge(from, to, PathwayLayoutConstants.OUTPUT_EDGE);
		GraphConstants.setLineColor(edge.getAttributes(), PathwayLayoutConstants.OUTPUT_EDGE_COLOR);
		GraphConstants.setLineEnd(edge.getAttributes(), PathwayLayoutConstants.OUTPUT_EDGE_END);
		GraphConstants.setEndFill(edge.getAttributes(), PathwayLayoutConstants.OUTPUT_EDGE_END_FILL);
		return edge;
	}

	public Edge createCatalystEdge(Vertex from, Vertex to) {
		Edge edge = createEdge(from, to, PathwayLayoutConstants.CATALYST_EDGE);
		GraphConstants.setLineColor(edge.getAttributes(), PathwayLayoutConstants.CATALYST_EDGE_COLOR);
		GraphConstants.setLineEnd(edge.getAttributes(), PathwayLayoutConstants.CATALYST_EDGE_END);
		GraphConstants.setEndFill(edge.getAttributes(), PathwayLayoutConstants.CATALYST_EDGE_END_FILL);
		return edge;
	}

	public DefaultEdge createRegulatorEdge(Vertex from, Vertex to, String regulation) {
		Edge edge = createEdge(from, to, 0);
		if (regulation.equals(ReactomeJavaConstants.Requirement)) {
			//GraphConstants.setLineColor(edge.getAttributes(), PathwayLayoutConstants.REQUIREMENT_EDGE_COLOR);
			GraphConstants.setLineEnd(edge.getAttributes(), PathwayLayoutConstants.REQUIREMENT_EDGE_END);
			GraphConstants.setEndFill(edge.getAttributes(), PathwayLayoutConstants.REQUIREMENT_EDGE_END_FILL);
			edge.setType(PathwayLayoutConstants.REQUIREMENT_EDGE);
		} else if (regulation.equals(ReactomeJavaConstants.PositiveRegulation)) {
			//GraphConstants.setLineColor(edge.getAttributes(), PathwayLayoutConstants.POSREGULATION_EDGE_COLOR);
			GraphConstants.setLineEnd(edge.getAttributes(), PathwayLayoutConstants.POSREGULATION_EDGE_END);
			GraphConstants.setEndFill(edge.getAttributes(), PathwayLayoutConstants.POSREGULATION_EDGE_END_FILL);
			edge.setType(PathwayLayoutConstants.POSREGULATION_EDGE);
		} else {
			//GraphConstants.setLineColor(edge.getAttributes(), PathwayLayoutConstants.NEGREGULATION_EDGE_COLOR);
			GraphConstants.setLineEnd(edge.getAttributes(), PathwayLayoutConstants.NEGREGULATION_EDGE_END);
			GraphConstants.setEndFill(edge.getAttributes(), PathwayLayoutConstants.NEGREGULATION_EDGE_END_FILL);
			edge.setType(PathwayLayoutConstants.NEGREGULATION_EDGE);
		}
		//float[] p = [5.0,5.0];
		//GraphConstants.setDashPattern(edge.getAttributes(), p);
		return edge;
	}

	/*
	 * Place the entity according to the average of reaction coordinates.
	 */
	public void setEntityNodeBounds1(Vertex enode, Set rnodes) {
		double x = 0;
		double y = 0;
		for (Iterator<Vertex> i = rnodes.iterator(); i.hasNext();) {
			Vertex rn = i.next();
			x += GraphConstants.getBounds(rn.getAttributes()).getCenterX();
			y += GraphConstants.getBounds(rn.getAttributes()).getCenterY();
		}
		if (rnodes.size() > 1) {
			x /= rnodes.size();
			y /= rnodes.size();
			x -= PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH / 2;
			y -= PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT / 2;
			if (rnodes.size() == 2) enode2rnodes.put(enode, rnodes);
		} else {
			x -= 3 * PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH;
			y -= PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT / 2;
		}
		GraphConstants.setBounds(enode.getAttributes(),new Rectangle(
				(int)x,(int)y,PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH,PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT));
	}

	/*
	 * Place the entity in the middle of extremes (not average).
	 */
	public void setEntityNodeBounds(Vertex enode, Set rnodes) {
		double x = 0;
		double y = 0;
		double min_x = Double.MAX_VALUE;
		double min_y = Double.MAX_VALUE;
		double max_x = -Double.MAX_VALUE;
		double max_y = -Double.MAX_VALUE;
		for (Iterator<Vertex> ri = rnodes.iterator(); ri.hasNext();) {
			Vertex rn = ri.next();
			x = GraphConstants.getBounds(rn.getAttributes()).getCenterX();
			min_x = Math.min(min_x, x);
			max_x = Math.max(max_x, x);
			y = GraphConstants.getBounds(rn.getAttributes()).getCenterY();
			min_y = Math.min(min_y, y);
			max_y = Math.max(max_y, y);
		}
		if (rnodes.size() > 1) {
			x = (min_x + max_x) / 2;
			y = (min_y + max_y) / 2;
			x -= PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH / 2;
			y -= PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT / 2;
			enode.setFixed(true);
			if (rnodes.size() == 2) enode2rnodes.put(enode, rnodes);
		} else {
			x -= 3 * PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH;
			y -= PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT / 2;
		}
		GraphConstants.setBounds(enode.getAttributes(),new Rectangle(
				(int)x,(int)y,PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH,PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT));
	}
	
	/*
	 * This is somewhat ridiculous: what I want is to know the node size at the time of placing it.
	 * Initially I tried inserting them into the graph, measuring their size and then re-locating
	 * straightaway. However, I constantly get a weird NullPointerException for seemingly random
	 * node at some point of the process. Hence the need to cache the neighbours and re-adjust 
	 * the location after everything has been inserted. Thannkfully this appoach works thus far.
	 */ 
	public void resetEntityNodeBounds() throws InvalidAttributeException, Exception {
		Map nested = new Hashtable();
		for (Iterator<Vertex> ei = enode2rnodes.keySet().iterator(); ei.hasNext();) {
			Vertex enode = ei.next();
			enode.setFixed(true);
			Set rnodes = enode2rnodes.get(enode);
			double x = 0;
			double y = 0;
			for (Iterator<Vertex> ri = rnodes.iterator(); ri.hasNext();) {
				Vertex rn = ri.next();
				x += GraphConstants.getBounds(rn.getAttributes()).getCenterX();
				y += GraphConstants.getBounds(rn.getAttributes()).getCenterY();
			}
			x /= rnodes.size();
			y /= rnodes.size();
			Rectangle2D bounds = graph.getCellBounds(enode);
/*			int[] units = Utils.calculateEntityNodeDimensions((GKInstance) enode.getUserObject());
			units[0] *= 4;
			units[1] *= 2;
			int w = (bounds.getWidth() > units[0]) ? (int) bounds.getWidth() : units[0];
			int h = (bounds.getHeight() > units[1]) ? (int) bounds.getHeight() : units[1];
			x -= w / 2;
			y -= h / 2;*/
			x -= bounds.getWidth() / 2;
			y -= bounds.getHeight() / 2;
//			if ((Math.abs(bounds.getX() - x) > 3) || (Math.abs(bounds.getY() - y) > 3)) {
//				System.err.printf("[%d, %d]\t[%d, %d]\t%s\n", (int)bounds.getX(), (int)x, (int)bounds.getY(), (int)y, enode);
//			}
			Map attMap = new Hashtable();
			GraphConstants.setBounds(attMap,new Rectangle((int)x,(int)y,(int)bounds.getWidth(),(int)bounds.getHeight()));
			//GraphConstants.setBounds(attMap,new Rectangle((int)x,(int)y,w,h));
			//GraphConstants.setResize(attMap, false);
			nested.put(enode, attMap);
		}
		model.edit(nested, null, null, null);
	}
	
	public void resetEntityNodeBounds1() throws InvalidAttributeException, Exception {
		Map nested = new Hashtable();
		for (Iterator<Vertex> ei = enode2rnodes.keySet().iterator(); ei.hasNext();) {
			Vertex enode = ei.next();
			enode.setFixed(true);
			Set rnodes = enode2rnodes.get(enode);
			double min_x = Double.MAX_VALUE;
			double min_y = Double.MAX_VALUE;
			double max_x = -Double.MAX_VALUE;
			double max_y = -Double.MAX_VALUE;
			for (Iterator<Vertex> ri = rnodes.iterator(); ri.hasNext();) {
				Vertex rn = ri.next();
				double x = GraphConstants.getBounds(rn.getAttributes()).getCenterX();
				min_x = Math.min(min_x, x);
				max_x = Math.max(max_x, x);
				double y = GraphConstants.getBounds(rn.getAttributes()).getCenterY();
				min_y = Math.min(min_y, y);
				max_y = Math.max(max_y, y);
			}
			double x = (min_x + max_x) / 2;
			double y = (min_y + max_y) / 2;
			Rectangle2D bounds = graph.getCellBounds(enode);
			x -= bounds.getWidth() / 2;
			y -= bounds.getHeight() / 2;
			Map attMap = new Hashtable();
			GraphConstants.setBounds(attMap,new Rectangle((int)x,(int)y,(int)bounds.getWidth(),(int)bounds.getHeight()));
			//GraphConstants.setBounds(attMap,new Rectangle((int)x,(int)y,w,h));
			//GraphConstants.setResize(attMap, false);
			nested.put(enode, attMap);
		}
		model.edit(nested, null, null, null);
	}
	
	public void setEntityNodeBounds2(Vertex enode, Set rnodes) {
		DefaultGraphCell tv = new DefaultGraphCell(enode.toString());
		GraphConstants.setResize(tv.getAttributes(), true);
		System.err.printf("%s\n", tv);
		GraphConstants.setBounds(tv.getAttributes(),new Rectangle(0,0,20,20));
		Object[] t = new Object[]{tv};
		model.insert(t, null, null, null, null);
		Rectangle2D bounds = graph.getCellBounds(tv);
		int w = (int) Math.ceil(bounds.getWidth());
		int h = (int) Math.ceil(bounds.getHeight());
		//int w = (int) Math.ceil(GraphConstants.getBounds(tv.getAttributes()).getWidth());
		//int h = (int) Math.ceil(GraphConstants.getBounds(tv.getAttributes()).getHeight());
		System.err.printf("w=%d, h=%d\n", w, h);
		model.remove(t);
		double x = 0;
		double y = 0;
		for (Iterator i = rnodes.iterator(); i.hasNext();) {
			Vertex rn = (Vertex)i.next();
			x += GraphConstants.getBounds(rn.getAttributes()).getCenterX();
			y += GraphConstants.getBounds(rn.getAttributes()).getCenterY();
		}
		if (rnodes.size() > 1) {
			x /= rnodes.size();
			y /= rnodes.size();
			x -= w / 2;
			y -= h / 2;
		} else {
			x -= 3 * PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH;
			y -= PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT / 2;
		}
		GraphConstants.setBounds(enode.getAttributes(),new Rectangle((int)x,(int)y, w, h));
	}

	public void setEntityNodeAboveReactionNode(Vertex enode, Vertex rnode) {
		double x = rnode.getBounds().getCenterX() - PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH / 2;
		double y = rnode.getBounds().getCenterY() - 3 * PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT;
		GraphConstants.setBounds(enode.getAttributes(),new Rectangle(
				(int)x,(int)y,PathwayLayoutConstants.DEFAULT_ENTITY_NODE_WIDTH,PathwayLayoutConstants.DEFAULT_ENTITY_NODE_HEIGHT));
	}
	
	public void run() {
		Thread me = Thread.currentThread();
		while (me == relaxer) {
			/*
			 * Can't do save on the thread which responds to button click - somehow this yields in
			 * partial or empty tiles.
			 */
			if (save) {
				try {
					saveAsTiles();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				save = false;
			} else if (relax) {
				relaxNeighbourhood();
				updateView();
//				if (! relaxWithoutRepaint) {
//					updateView();
//				}
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	synchronized void relax() {
		double start = new Date().getTime();
		for (Iterator ei = edges.iterator(); ei.hasNext();) {
			if (! relax)
				return;
			Edge e = (Edge) ei.next();
			if (e.isFixed()) continue;
			Dimension dimension = smallestDistance(e.getTargetVertex().getBounds(), e.getSourceVertex().getBounds());
			double vx = dimension.getWidth();
			double vy = dimension.getHeight();
			double len = Math.sqrt(vx * vx + vy * vy);
			len = (len == 0) ? .0001 : len;
			double f = (e.getPreferredLength() - len) / (len * 3);
			double dx = f * vx;
			double dy = f * vy;
			e.getTargetVertex().dx += dx;
			e.getTargetVertex().dy += dy;
			e.getSourceVertex().dx += -dx;
			e.getSourceVertex().dy += -dy;
			//System.out.println(e.toString());
		}

		for (int i = 0 ; i < verteces.size() ; i++) {
			if (! relax)
				return;
			Vertex n1 = (Vertex)verteces.get(i);
			double dx = 0;
			double dy = 0;

			for (int j = i + 1 ; j < verteces.size() ; j++) {
				Vertex n2 = (Vertex)verteces.get(j);
				//System.err.println(n1 + "\t" + n2);
				Dimension d = smallestDistance(n1.getBounds(), n2.getBounds());
				double vx = d.getWidth();
				double vy = d.getHeight();
				double len = vx * vx + vy * vy;
				if (len == 0) {
					dx += Math.random() - 0.5;
					dy += Math.random() - 0.5;
				} else if (len < 100*100) {
					dx += vx / len;
					dy += vy / len;
				}
			}
			double dlen = dx * dx + dy * dy;
			if (dlen > 0) {
				dlen = Math.sqrt(dlen) / 2;
				n1.dx += dx / dlen;
				n1.dy += dy / dlen;
			}
		}

		for (int i = 0 ; i < verteces.size() ; i++) {
			if (! relax)
				return;
			Vertex n1 = (Vertex)verteces.get(i);
			if (! (n1.isInFocus() || n1.isFixed())) {
				n1.translate((int)Math.max(-5, Math.min(5, n1.dx)),
						(int)Math.max(-5, Math.min(5, n1.dy))
				);
			}
			n1.dx /= 2;
			n1.dy /= 2;
			for (int j = i + 1; j < verteces.size() ; j++) {
				Vertex n2 = (Vertex)verteces.get(j);
				if (n1.getBounds().intersects(n2.getBounds())) {
					Rectangle is = n1.getBounds().intersection(n2.getBounds());
					//if (is.height < is.width) {
						if (Math.random() > 0.5) {
							if (n1.getBounds().y < n2.getBounds().y) {
								if (n1.isInFocus() || n1.isFixed()) {
									n2.translate(0, (int) (is.height));
								} else if (n2.isInFocus() || n2.isFixed()) {
									n1.translate(0, (int) (-is.height));
								} else {
									n1.translate(0, (int) (-is.height/2));
									n2.translate(0, (int) (is.height/2));
								}
							} else {
								if (n1.isInFocus() || n1.isFixed()) {
									n2.translate(0, (int) (-is.height));
								} else if (n2.isInFocus() || n2.isFixed()) {
									n1.translate(0, (int) (is.height));
								} else {
									n2.translate(0, (int) (-is.height/2));
									n1.translate(0, (int) (is.height/2));
								}
							}
						} else {
							if (n1.getBounds().x < n2.getBounds().x) {
								if (n1.isInFocus() || n1.isFixed()) {
									n2.translate((int) (is.width), 0);
								} else if (n2.isInFocus() || n2.isFixed()) {
									n1.translate((int) (-is.width), 0);
								} else {
									n1.translate((int) (-is.width/2), 0);
									n2.translate((int) (is.width/2), 0);
								}
							} else {
								if (n1.isInFocus() || n1.isFixed()) {
									n2.translate((int) (-is.width), 0);
								} else if (n2.isInFocus() || n2.isFixed()) {
									n1.translate((int) (is.width), 0);
								} else {
									n2.translate((int) (-is.width/2), 0);
									n1.translate((int) (is.width/2), 0);
								}
							}
						}
				}
			}
		}
		double duration = (new Date().getTime() - start)/1000;
		if (duration > 5)
			System.out.println("relax() run for " + duration + "seconds");
	}

	synchronized void relaxNeighbourhood() {
		inRelax = true;
		double start = new Date().getTime();
		int max_move = 0;
		for (Iterator ei = edges.iterator(); ei.hasNext();) {
			Edge e = (Edge) ei.next();
			if (e.isFixed()) continue;
			Vertex target = e.getTargetVertex();
			Vertex source = e.getSourceVertex();
			if (target.isFixed() && source.isFixed()) continue;
			Dimension dimension = smallestDistance(target.getBounds(), source.getBounds());
			double vx = dimension.getWidth();
			double vy = dimension.getHeight();
			double len = Math.sqrt(vx * vx + vy * vy);
			len = (len == 0) ? .0001 : len;
			double f = (e.getPreferredLength() - len) / (len * 3);
			double dx = f * vx;
			double dy = f * vy;
			target.dx += dx;
			target.dy += dy;
			source.dx -= dx;
			source.dy -= dy;
			//System.out.println(e.toString());
		}

		for (int i = 0 ; i < verteces.size() ; i++) {
			Vertex n1 = (Vertex)verteces.get(i);
			double dx = 0;
			double dy = 0;

			List neighbours = (List)node2neighbours.get(n1);
			for (Iterator it = neighbours.iterator(); it.hasNext();) {
				Vertex n2 = (Vertex)it.next();
				if (n1.isFixed() && n2.isFixed()) continue;
				//System.err.println(n1 + "\t" + n2);
				Dimension d = smallestDistance(n1.getBounds(), n2.getBounds());
				double vx = d.getWidth();
				double vy = d.getHeight();
				double len = vx * vx + vy * vy;
				if (len == 0) {
					dx += Math.random() - 0.5;
					dy += Math.random() - 0.5;
				} else if (len < 100*100) {
					dx += vx / len;
					dy += vy / len;
				}
			}
			double dlen = dx * dx + dy * dy;
			if (! n1.isFixed()) {
				if (dlen > 0) {
					dlen = Math.sqrt(dlen) / 2;
					n1.dx += dx / dlen;
					n1.dy += dy / dlen;
				}
			}
		}

		for (int i = 0 ; i < verteces.size() ; i++) {
			Vertex n1 = (Vertex)verteces.get(i);
			if (! (n1.isInFocus() || n1.isFixed())) {
/*				n1.translate((int)Math.max(-5, Math.min(5, n1.dx)),
						(int)Math.max(-5, Math.min(5, n1.dy))
				);
				n1.dx /= 2;
				n1.dy /= 2;*/
				int dx = (int)Math.max(-5, Math.min(5, n1.dx));
				int dy = (int)Math.max(-5, Math.min(5, n1.dy));
				n1.translate(dx, dy);
				n1.dx /= 2;
				n1.dy /= 2;
				max_move = (int) Math.max(max_move, Math.sqrt(dx*dx+dy*dy));
			}

			List neighbours = (List)node2neighbours.get(n1);
			for (Iterator it = neighbours.iterator(); it.hasNext();) {
				Vertex n2 = (Vertex)it.next();
				if (n1.isFixed() && n2.isFixed()) continue;
				if (n1.getBounds().intersects(n2.getBounds())) {
					Rectangle is = n1.getBounds().intersection(n2.getBounds());
					if (is.height < is.width) {
					//if (Math.random() > 0.5) {
						if (n1.getBounds().y < n2.getBounds().y) {
							if (n1.isInFocus() || n1.isFixed()) {
								n2.translate(0, (int) (is.height) + 2);
							} else if (n2.isInFocus() || n2.isFixed()) {
								n1.translate(0, (int) (-is.height) - 2);
							} else {
								n1.translate(0, (int) (-is.height/2) - 1);
								n2.translate(0, (int) (is.height/2) + 1);
							}
						} else {
							if (n1.isInFocus() || n1.isFixed()) {
								n2.translate(0, (int) (-is.height) - 2);
							} else if (n2.isInFocus() || n2.isFixed()) {
								n1.translate(0, (int) (is.height) + 2);
							} else {
								n2.translate(0, (int) (-is.height/2) - 1);
								n1.translate(0, (int) (is.height/2) + 1);
							}
						}
						max_move = Math.max(max_move, is.height + 2);
					} else {
						if (n1.getBounds().x < n2.getBounds().x) {
							if (n1.isInFocus() || n1.isFixed()) {
								n2.translate((int) (is.width) + 2, 0);
							} else if (n2.isInFocus() || n2.isFixed()) {
								n1.translate((int) (-is.width) - 2, 0);
							} else {
								n1.translate((int) (-is.width/2) - 1, 0);
								n2.translate((int) (is.width/2) + 1, 0);
							}
						} else {
							if (n1.isInFocus() || n1.isFixed()) {
								n2.translate((int) (-is.width) - 2, 0);
							} else if (n2.isInFocus() || n2.isFixed()) {
								n1.translate((int) (is.width) + 2, 0);
							} else {
								n2.translate((int) (-is.width/2) - 1, 0);
								n1.translate((int) (is.width/2) + 1, 0);
							}
						}
						max_move = Math.max(max_move, is.width + 2);
					}
				}
			}
		}
		System.out.printf("Max movement: %d\n", max_move);
		if (max_move < 10) {
			relax = false;
		}
		double duration = (new Date().getTime() - start)/1000;
		if (duration > 5)
			System.out.println("relax() run for " + duration + "seconds");
		inRelax = false;
	}
	
	public static Dimension smallestDistance (Rectangle r1, Rectangle r2) {
		Rectangle union = r1.union(r2);
		int w = union.width - r1.width - r2.width;
		w = (w < 0) ? 0 : w;
		if (r1.x < r2.x) {
			w = -w;
		}
		int h = union.height - r1.height - r2.height;
		h = (h < 0) ? 0 : h;
		if (r1.y < r2.y) {
			h = -h;
		}
		return new Dimension(w,h);
	}

	public static Dimension smallestDistance (Rectangle2D r1, Rectangle2D r2) {
		Rectangle2D.Double union = new Rectangle2D.Double();
		Rectangle2D.union(r1,r2,union);
		double w = union.width - r1.getWidth() - r2.getWidth();
		w = (w < 0) ? 0 : w;
		if (r1.getX() < r2.getX()) {
			w = -w;
		}
		double h = union.height - r1.getHeight() - r2.getHeight();
		h = (h < 0) ? 0 : h;
		if (r1.getY() < r2.getY()) {
			h = -h;
		}
		return new Dimension((int)w,(int)h);
	}

	public double reactionDistance (GKInstance reaction1, GKInstance reaction2) {
		Vertex rnode1 = reactionInstance2NodeMap.get(reaction1);
		Vertex rnode2 = reactionInstance2NodeMap.get(reaction2);
		//System.err.println(rnode1 + "\t" + rnode2);
		int dx = rnode1.getX() - rnode2.getX();
		int dy = rnode1.getY() - rnode2.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public void updateView() {
		//System.out.println("Starting updateView");
		inUpdateView = true;
		if (node2attributes.isEmpty()) {
			for (Iterator i = verteces.iterator(); i.hasNext();) {
				Vertex v = (Vertex)i.next();
				node2attributes.put(v, v.getAttributes());
			}
		}
		model.edit(node2attributes,null,null,null);
		inUpdateView = false;
		//System.out.println("Finished updateView");
	}

	public void saveAsImage() {
		if (inUpdateView)
			System.out.println("Waiting for updateView() to finish");
		while(inUpdateView) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		System.out.println("Starting saveAsImage()");
		relax = false;
		relaxWithoutRepaint = true;
		//graph.setScale(0.2);
		//System.out.println(graph.getBounds());
		BufferedImage img = graph.getImage(null, GraphConstants.DEFAULTINSET);
		//System.out.println(img);
		try {
			ImageIO.write(img, "png", new File("out.png"));
			//printHtml();
//			out.flush();
//			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Finished saveAsImage()");
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
 		//double[] zoomLevels = new double[]{0.125, 0.25, 0.5, 0.99};
 		double[] zoomLevels = new double[]{0.8, 0.4, 0.2, 0.1, 0.05};
 		//double[] zoomLevels = new double[]{0.25};
		File topDir = new File("tiles");
		if (! topDir.exists())
			topDir.mkdir();
		saveCoordinates(zoomLevels[0], topDir);
		for (int i = 0; i < zoomLevels.length; i++) {
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
	}
	
	/*
	 * This method is simply too slow
	 */
	public void saveZoomLevelAsTiles1(double zoomLevel, File dir) {
		System.out.printf("Starting saveZoomLevelAsTiles(%s, %s)\n", zoomLevel, dir);
		graph.setScale(zoomLevel);
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace();}
		Rectangle2D bounds = graph.getCellBounds(graph.getRoots());
		int w = (int) ((bounds.getWidth() + bounds.getX()) * zoomLevel);
		int h = (int) ((bounds.getHeight() + bounds.getY()) * zoomLevel);
		System.out.printf("w = %d\th = %d\n", w, h);
		int tileWitdh = 200;
		for (int x = 0; x*tileWitdh  < w; x++) {
			for (int y = 0; y*tileWitdh < h; y++) {
				String fileName = x + "x" + y + ".png";
				File tileFile = new File(dir, fileName);
				System.out.println(tileFile);
				BufferedImage img = new BufferedImage(tileWitdh,tileWitdh,BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = img.createGraphics();
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
				graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
				graphics.setComposite(AlphaComposite.SrcOver);
				graphics.translate(-x*tileWitdh,-y*tileWitdh);
				graph.print(graphics);	
				graphics.dispose();
				try {
					ImageIO.write(img, "png", tileFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void saveZoomLevelAsTiles(double zoomLevel, File dir) throws IOException {
		System.out.printf("Starting saveZoomLevelAsTiles(%s, %s)\n", zoomLevel, dir);
		graph.setScale(zoomLevel);
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace();}
		Rectangle2D bounds = graph.getCellBounds(graph.getRoots());
		int w = (int) ((bounds.getWidth() + bounds.getX()) * zoomLevel);
		int h = (int) ((bounds.getHeight() + bounds.getY()) * zoomLevel);
		System.out.printf("w = %d\th = %d\n", w, h);
		int c = 15;
		int tileWitdh = 200;
		int largeTileWidth = tileWitdh * c;
		BufferedImage img;
		BufferedImage simg;
		for (int x = 0; x*largeTileWidth  < w; x++) {
			int tmp = w - x*largeTileWidth;
			int trueTileW = (tmp < largeTileWidth) ? tmp : largeTileWidth;
			for (int y = 0; y*largeTileWidth < h; y++) {
				tmp = h - y*largeTileWidth;
				int trueTileH = (tmp < largeTileWidth) ? tmp : largeTileWidth;
				img = new BufferedImage(largeTileWidth,largeTileWidth,BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = img.createGraphics();
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
				graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
				graphics.setComposite(AlphaComposite.SrcOver);
				graphics.translate(-x*largeTileWidth,-y*largeTileWidth);
				graph.print(graphics);
				graphics.dispose();
//				File largeTileFile = new File(dir, x + "" + y + ".png");
//				try {
//					ImageIO.write(img, "png", largeTileFile);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
				for (int xs = 0; xs*tileWitdh < trueTileW; xs++) {
					for (int ys = 0; ys*tileWitdh < trueTileH; ys++) {
						String fileName = (x * c + xs) + "x" + (y * c + ys) + ".png";
						File tileFile = new File(dir, fileName);
						System.out.println(tileFile);
						simg = img.getSubimage(xs * tileWitdh, ys * tileWitdh, tileWitdh, tileWitdh);
						ImageIO.write(simg, "png", tileFile);
					}
				}
			}
		}
	}
	
	public void printSomeStats() {
		System.out.println("Reactions: " + reactionInstance2NodeMap.size());
		System.out.println("Verteces: " + verteces.size());
		System.out.println("Edges: " + edges.size());
		System.out.println("Bounds: " + graph.getCellBounds(graph.getRoots()));
	}

	public void setNeighbouringVerteces() {
		System.out.println("Starting setNeighbouringVerteces()");
		for (int i = 0; i < verteces.size(); i++) {
			Vertex n1 = (Vertex)verteces.get(i);
			List<Vertex> neighbours = new ArrayList<Vertex>();
			for (int j = i + 1 ; j < verteces.size() ; j++) {
				Vertex n2 = (Vertex)verteces.get(j);
				Dimension d = smallestDistance(n1.getBounds(), n2.getBounds());
				double vx = d.getWidth();
				double vy = d.getHeight();
				double len = vx * vx + vy * vy;
				if (len <= PathwayLayoutConstants.NEIGHBOURHOOD_RADIUS) {
					neighbours.add(n2);
				}
			}
			node2neighbours.put(n1,neighbours);
		}
		System.out.println("Finished setNeighbouringVerteces()");
	}

	public void reportEntityVertecesWithMultipleOutputEdges() {
		System.out.println("Starting reportVertecesWithMultipleOutputEdges()");
		for (int i = 0; i < verteces.size(); i++) {
			Vertex n1 = (Vertex)verteces.get(i);
			if (n1.isEntityVertex()) {
				int c = n1.getOutputEdgeCount();
				if (c > 1) {
					System.out.printf("%d\t%s\n", c, ((GKInstance)n1.getUserObject()).getDisplayName());
				}
			}
		}
		System.out.println("Finished reportVertecesWithMultipleOutputEdges()");
	}
	
	public void printHtml() throws FileNotFoundException {
		PrintStream ps = new PrintStream("out.html");
		ps.println("<HTML>\n" +
				"<head><title></title>\n" +
				"<link rel=\"stylesheet\" type=\"text/css\" href=\"/stylesheet.css\" />" +
				"</head><body>" +
				"<div id=\"dhtmltooltip\" class=\"dhtmltooltip\"></div>" +
				"<script language=\"javascript\" src=\"/javascript/tooltip.js\"></script>" +
				"<MAP NAME=\"img_map\">");
		String format = "<AREA SHAPE=\"rect\" COORDS=\"%.0f,%.0f,%.0f,%.0f\" HREF=\"/cgi-bin/eventbrowser?ID=%d\" ONMOUSEOVER=\"ddrivetip('%s','dcdcdc',250);\" ONMOUSEOUT='hideddrivetip();'>\n";		
		double scale = graph.getScale();
		for (Vertex v : verteces) {
			Rectangle bounds = v.getBounds();
			GKInstance i = (GKInstance)v.getUserObject();
			ps.printf(format, scale*bounds.x, scale*bounds.y, scale*(bounds.x+bounds.width), scale*(bounds.y+bounds.height), i.getDBID(), 
					org.apache.commons.lang.StringEscapeUtils.escapeHtml(i.getDisplayName()));
		}
		ps.println("</MAP><IMG USEMAP=#img_map BORDER=\"0\" SRC=\"out.png\"></BODY></HTML>");
		ps.close();
	}
	
	public void saveCoordinates(double zoomLevel, File dir) throws FileNotFoundException {
		File outFile = new File(dir, "coordinates.txt");
		PrintStream ps = new PrintStream(outFile);
		String format = "%d\t%.0f,%.0f,%.0f,%.0f\t\"%s\"\n";		
		for (Vertex v : verteces) {
			Rectangle bounds = v.getBounds();
			GKInstance i = (GKInstance)v.getUserObject();
			ps.printf(format, i.getDBID(), 
					zoomLevel*bounds.x, zoomLevel*bounds.y, zoomLevel*bounds.width, zoomLevel*bounds.height, 
					org.apache.commons.lang.StringEscapeUtils.escapeHtml(i.getDisplayName()));
		}
		ps.close();
	}
	
	protected boolean isHiddenEntity (GKInstance entity) {
		return hiddenEntities.contains(entity);
	}

	public void placeEntitiesWithSingleEdge1() {
		System.out.println("Starting placeEntitiesWithSingleEdge");
		Map nested = new HashMap();
		double edgeLength = 200;
		int total = reactionInstance2NodeMap.size();
		int c = 1;
		for (Vertex r : reactionInstance2NodeMap.values()) {
			System.out.printf("Now handling %d/%d %s\n", c++,total,r);
			Set<Vertex> fixedInput = new HashSet<Vertex>();
			Set<Vertex> looseInput = new HashSet<Vertex>();
			Set<Vertex> fixedOutput = new HashSet<Vertex>();
			Set<Vertex> looseOutput = new HashSet<Vertex>();
			Set<Vertex> fixedCatalyst = new HashSet<Vertex>();
			Set<Vertex> looseCatalyst = new HashSet<Vertex>();
			Set<Vertex> fixedRegulator = new HashSet<Vertex>();
			Set<Vertex> looseRegulator = new HashSet<Vertex>();
			Set<Vertex> fixedVerteces = new HashSet<Vertex>();
			for (Edge e : r.getEdges()) {
				//System.out.println(e.toString2());
				if (e.getType() == PathwayLayoutConstants.INPUT_EDGE) {
					Vertex v = e.getSourceVertex();
					if (v.isFixed()) {
						fixedInput.add(v);
						fixedVerteces.add(v);
						//System.out.printf("Fixed input %s\n", v);
					} else {
						looseInput.add(v);
						//System.out.printf("Loose input %s\n", v);
					}
				} else if (e.getType() == PathwayLayoutConstants.OUTPUT_EDGE) {
					Vertex v = e.getTargetVertex();
					if (v.isFixed()) {
						fixedOutput.add(v);
						fixedVerteces.add(v);
						//System.out.printf("Fixed output %s\n", v);
					} else {
						looseOutput.add(v);
						//System.out.printf("Loose output %s\n", v);
					}
				} else if (e.getType() == PathwayLayoutConstants.CATALYST_EDGE) {
					Vertex v = e.getSourceVertex();
					if (v.isFixed()) {
						fixedCatalyst.add(v);
						fixedVerteces.add(v);
						//System.out.printf("Fixed catalyst %s\n", v);
					} else {
						looseCatalyst.add(v);
						//System.out.printf("Loose catalyst %s\n", v);
					}
				} else {
					Vertex v = e.getSourceVertex();
					if (v.isFixed()) {
						fixedRegulator.add(v);
						fixedVerteces.add(v);
						//System.out.printf("Fixed regulator %s\n", v);
					} else {
						looseRegulator.add(v);
						//System.out.printf("Loose regulator %s\n", v);
					}
				}
			}
			Set<PolarCoordinates> fixedCoordinates = Utils.getPolarCoordinates(r, fixedVerteces);
			if (!looseInput.isEmpty()) {
				if (!fixedInput.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedInput);
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseInput, fixedCoordinates));
				} else if (!fixedOutput.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedOutput);
					pc.phi -= Math.PI;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseInput, fixedCoordinates));
				} else if (!fixedCatalyst.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedCatalyst);
					pc.phi -= Math.PI/2;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseInput, fixedCoordinates));
				} else if (!fixedRegulator.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedRegulator);
					pc.phi += Math.PI/2;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseInput, fixedCoordinates));
				} else {
					PolarCoordinates pc = new PolarCoordinates(edgeLength, -0.5 * Math.PI);
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseInput, fixedCoordinates));
				}
			}
			if (!looseOutput.isEmpty()) {
				if (!fixedOutput.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedOutput);
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseOutput, fixedCoordinates));
				} else if (!fixedInput.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedInput);
					pc.phi -= Math.PI;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseOutput, fixedCoordinates));
				} else if (!fixedCatalyst.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedCatalyst);
					pc.phi += Math.PI/2;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseOutput, fixedCoordinates));
				} else if (!fixedRegulator.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedRegulator);
					pc.phi -= Math.PI/2;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseOutput, fixedCoordinates));
				} else {
					PolarCoordinates pc = new PolarCoordinates(edgeLength, +0.5 * Math.PI);
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseOutput, fixedCoordinates));
				}
			}
			if (!looseCatalyst.isEmpty()) {
				if (!fixedCatalyst.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedCatalyst);
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseCatalyst, fixedCoordinates));
				} else if (!fixedOutput.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedOutput);
					pc.phi -= Math.PI/2;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseCatalyst, fixedCoordinates));
				} else if (!fixedInput.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedInput);
					pc.phi += Math.PI/2;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseCatalyst, fixedCoordinates));
				} else if (!fixedRegulator.isEmpty()) {
					PolarCoordinates pc = Utils.getMeanPolarCoordinates(r, fixedRegulator);
					pc.phi -= Math.PI;
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseCatalyst, fixedCoordinates));
				} else {
					PolarCoordinates pc = new PolarCoordinates(edgeLength, 0);
					nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseCatalyst, fixedCoordinates));
				}
			}
		}
		System.out.println("Starting model.edit");
		model.edit(nested, null, null, null);
		System.out.println("Finished placeEntitiesWithSingleEdge");
	}
	
	public void placeEntitiesWithSingleEdge() {
		System.out.println("Starting placeEntitiesWithSingleEdge");
		Map nested = new HashMap();
		double edgeLength = 200;
		int total = reactionInstance2NodeMap.size();
		int c = 1;
		for (Vertex r : reactionInstance2NodeMap.values()) {
			System.out.printf("Now handling %d/%d %s\n", c++,total,r);
			Set<Vertex> fixedInput = new HashSet<Vertex>();
			Set<Vertex> looseInput = new HashSet<Vertex>();
			Set<Vertex> fixedOutput = new HashSet<Vertex>();
			Set<Vertex> looseOutput = new HashSet<Vertex>();
			Set<Vertex> fixedCatalyst = new HashSet<Vertex>();
			Set<Vertex> looseCatalyst = new HashSet<Vertex>();
			Set<Vertex> fixedRegulator = new HashSet<Vertex>();
			Set<Vertex> looseRegulator = new HashSet<Vertex>();
			Set<Vertex> fixedVerteces = new HashSet<Vertex>();
			for (Edge e : r.getEdges()) {
				//System.out.println(e.toString2());
				if (e.getType() == PathwayLayoutConstants.INPUT_EDGE) {
					Vertex v = e.getSourceVertex();
					if (v.isFixed()) {
						fixedInput.add(v);
						fixedVerteces.add(v);
						//System.out.printf("Fixed input %s\n", v);
					} else {
						looseInput.add(v);
						//System.out.printf("Loose input %s\n", v);
					}
				} else if (e.getType() == PathwayLayoutConstants.OUTPUT_EDGE) {
					Vertex v = e.getTargetVertex();
					if (v.isFixed()) {
						fixedOutput.add(v);
						fixedVerteces.add(v);
						//System.out.printf("Fixed output %s\n", v);
					} else {
						looseOutput.add(v);
						//System.out.printf("Loose output %s\n", v);
					}
				} else if (e.getType() == PathwayLayoutConstants.CATALYST_EDGE) {
					Vertex v = e.getSourceVertex();
					if (v.isFixed()) {
						fixedCatalyst.add(v);
						fixedVerteces.add(v);
						//System.out.printf("Fixed catalyst %s\n", v);
					} else {
						looseCatalyst.add(v);
						//System.out.printf("Loose catalyst %s\n", v);
					}
				} else {
					Vertex v = e.getSourceVertex();
					if (v.isFixed()) {
						fixedRegulator.add(v);
						fixedVerteces.add(v);
						//System.out.printf("Fixed regulator %s\n", v);
					} else {
						looseRegulator.add(v);
						//System.out.printf("Loose regulator %s\n", v);
					}
				}
			}
			Set<PolarCoordinates> fixedCoordinates = Utils.getPolarCoordinates(r, fixedVerteces);
			PolarCoordinates rPc = Utils.getReactionLengthAndBearing((GKInstance) r.getUserObject());
			rPc.r *= PathwayLayoutConstants.COORDINATE_SCALING_FACTOR;
			if (!looseInput.isEmpty()) {
				PolarCoordinates pc = new PolarCoordinates(rPc.r/2, rPc.phi - Math.PI);
				nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseInput, fixedCoordinates));
			}
			if (!looseOutput.isEmpty()) {
				PolarCoordinates pc = new PolarCoordinates(rPc.r/2, rPc.phi);
				nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseOutput, fixedCoordinates));
			}
			if (!looseCatalyst.isEmpty()) {
				//PolarCoordinates pc = new PolarCoordinates(rPc.r/4, rPc.phi - Math.PI/2);
				// Assuming just one catalyst
				//Rectangle b = looseCatalyst.iterator().next().getBounds();
				//PolarCoordinates pc = new PolarCoordinates(Math.sqrt(Math.pow(b.width,2) + Math.pow(b.height, 2))/2 + PathwayLayoutConstants.REACTION_NODE_DIAMETER, rPc.phi - Math.PI/2);
				double phi = rPc.phi - Math.PI/2;
				PolarCoordinates pc = new PolarCoordinates(looseCatalyst.iterator().next().distanceFromCentreToBoundaryAtBearing(phi) + PathwayLayoutConstants.REACTION_NODE_DIAMETER, phi);
				nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseCatalyst, fixedCoordinates));
			}
			if (!looseRegulator.isEmpty()) {
				//TODO: CONSIDER USING SAME METHODOLOGY AS FOR CATALYSTS WITH SINGLE REGULATOR
				PolarCoordinates pc = new PolarCoordinates(rPc.r/2, rPc.phi + Math.PI/2);
				if (fixedInput.isEmpty() && looseInput.isEmpty()) {
					pc.phi = rPc.phi - Math.PI;
				} else if (fixedOutput.isEmpty() && looseOutput.isEmpty()) {
					pc.phi = rPc.phi;
				} else if (looseRegulator.size() == 1) {
					pc.r = looseRegulator.iterator().next().distanceFromCentreToBoundaryAtBearing(pc.phi) + PathwayLayoutConstants.REACTION_NODE_DIAMETER;
				}
				nested.putAll(spreadSatelliteNodesAroundPoleNode(r, pc, looseRegulator, fixedCoordinates));
			}
		}
		System.out.println("Starting model.edit");
		model.edit(nested, null, null, null);
		System.out.println("Finished placeEntitiesWithSingleEdge");
	}
	
	/*
	 * PolarCoordinates should contain the "bearing" around which the satellites will be placed.
	 * At the moment the satellites will be spread in the sector -c..c radians of the bearing
	 * of PolarCoordinates pc.
	 */
	public Map<Vertex,Map> spreadSatelliteNodesAroundPoleNode (Vertex pole, PolarCoordinates pc, Set<Vertex> satellites, Set<PolarCoordinates> usedCoordinates) {
		Rectangle pb = pole.getBounds();
		int px = (int) pb.getCenterX();
		int py = (int) pb.getCenterY();
		//double c = Math.PI * 0.25;
		double c = Math.min(Math.PI/12 * Math.sqrt(satellites.size()), Math.PI * 0.45); 
		double phi;
		double step;
		if (satellites.size() == 1) {
			phi = pc.phi;
			step = c;
		} else {
			phi = pc.phi - c;
			step = 2 * c / (satellites.size() - 1);
		}
		double r = Math.min(100, pc.r);
		r *= Math.pow(satellites.size(),0.25);
		//r *= Math.pow(satellites.size(),0.5);
		//System.out.printf("%s\t%s\t%s\n", pc.r, r, satellites.size());
		Map<Vertex,Map> nested = new HashMap<Vertex,Map>();
		List list = new ArrayList(satellites);
		Utils.sortByToString(list);
		List<Vertex> sorted = new ArrayList<Vertex>(list);
		//for (Vertex v : satellites) {
		for (Vertex v : sorted) {
			//System.out.printf("%s\t%s\n", phi, v);
			while(Utils.phiEqualToPhiInSet(phi, usedCoordinates)) {
				phi += step;
			}
			PolarCoordinates spc = new PolarCoordinates(r, phi);
			Point p = spc.toCartesian();
			p.x += px;
			p.y += py;
			Rectangle bounds = v.getBounds();
			//System.out.printf("\t%s\n", bounds);
			p.x -= bounds.width / 2;
			p.y -= bounds.height / 2;
			bounds.x = p.x;
			bounds.y = p.y;
			//System.out.printf("\t%s\n", bounds);
			Map attMap = new HashMap();
			GraphConstants.setBounds(attMap,bounds);
			//GraphConstants.setBackground(attMap,Color.magenta);
			nested.put(v, attMap);
			phi += step;
			v.setFixed(true);
		}
		//model.edit(nested, null, null, null);
		return nested;
	}
	
	/*
	 * This is rather useless
	 */
	public void nudgeOverlappingNodes () {
		System.out.println("Starting nudgeOverlappingNodes");
		for (int i = 0 ; i < verteces.size() ; i++) {
			Vertex n1 = (Vertex)verteces.get(i);
			if (((GKInstance) n1.getUserObject()).getSchemClass().isa(ReactomeJavaConstants.Event) || !n1.isFixed()) continue;
			//List neighbours = (List)node2neighbours.get(n1);
			//for (Iterator it = neighbours.iterator(); it.hasNext();) {
				//Vertex n2 = (Vertex)it.next();
			for (int j = i + 1; j < verteces.size() ; j++) {
				Vertex n2 = (Vertex)verteces.get(j);
				if (!n2.isEntityVertex() || !n2.isFixed()) continue;
				if (n1.getBounds().intersects(n2.getBounds())) {
					System.out.printf("%s OVERLAPS %s\n", n1, n2);
					Rectangle is = n1.getBounds().intersection(n2.getBounds());
					if (is.height < is.width) {
					//if (Math.random() > 0.5) {
						if (n1.getBounds().y < n2.getBounds().y) {
							translate(n1, 0, (int) (-is.height/2) - 1);
							translate(n2, 0, (int) (is.height/2) + 1);
						} else {
							translate(n2, 0, (int) (-is.height/2) - 1);
							translate(n1, 0, (int) (is.height/2) + 1);
						}
					} else {
						if (n1.getBounds().x < n2.getBounds().x) {
							translate(n1, (int) (-is.width/2) - 1, 0);
							translate(n2, (int) (is.width/2) + 1, 0);
						} else {
							translate(n2, (int) (-is.width/2) - 1, 0);
							translate(n1, (int) (is.width/2) + 1, 0);
						}
					}
				}
			}
		}
		System.out.println("Finished nudgeOverlappingNodes");
	}
	
	/*
	 * Finds overlaping entity nodes regardless of the extent of the overlap
	 */
	public Set<Vertex> findOverlappingEntityNodes () {
		Set<Vertex> s = new HashSet<Vertex>();
		for (int i = 0 ; i < verteces.size() ; i++) {
			Vertex n1 = (Vertex)verteces.get(i);
			for (int j = i + 1; j < verteces.size() ; j++) {
				Vertex n2 = (Vertex)verteces.get(j);
				if (n1.getBounds().intersects(n2.getBounds())) {
					s.add(n1);
					s.add(n2);
				}
			}
		}
		Set<Vertex> out = new HashSet<Vertex>();
		for (Vertex v : s) {
			if (!v.isEntityVertex()) continue;
			out.add(v);
		}
		return out;
	}
	
	/*
	 * Finds entity nodes with overlap area at least 0.9 of the area of the smaller node
	 */
	public Set<Vertex> findSignificantlyOverlappingEntityNodes () {
		Set<Vertex> s = new HashSet<Vertex>();
		double coef = 0.9;
		for (int i = 0 ; i < verteces.size() ; i++) {
			Vertex n1 = (Vertex)verteces.get(i);
			Rectangle bounds1 = n1.getBounds();
			double a1 = bounds1.width * bounds1.height * coef;
			for (int j = i + 1; j < verteces.size() ; j++) {
				Vertex n2 = (Vertex)verteces.get(j);
				Rectangle bounds2 = n2.getBounds();
				Rectangle is = bounds1.intersection(bounds2);
				if (is.isEmpty()) continue;
				double a2 = bounds2.width * bounds2.height * coef;
				double ai = is.width * is.height;
				if ((ai > a1) || (ai > a2)) {
					s.add(n1);
					s.add(n2);
				}
			}
		}
		Set<Vertex> out = new HashSet<Vertex>();
		for (Vertex v : s) {
			if (!v.isEntityVertex()) continue;
			out.add(v);
		}
		return out;
	}
	
	/*
	 * Finds sets (technically Lists though) of entity nodes which have identical
	 * centre point.
	 */
	public List<List<Vertex>> findSetsOfEntityNodesWithIdenticalCenter () {
		List<List<Vertex>> out = new ArrayList<List<Vertex>>();
		for (int i = 0 ; i < verteces.size() ; i++) {
			List<Vertex> l = null;
			Vertex n1 = (Vertex)verteces.get(i);
			if (!n1.isEntityVertex()) continue;
			Rectangle bounds1 = n1.getBounds();
			int x1 = (int) bounds1.getCenterX();
			int y1 = (int) bounds1.getCenterY();
			for (int j = i + 1; j < verteces.size() ; j++) {
				Vertex n2 = (Vertex)verteces.get(j);
				if (!n2.isEntityVertex()) continue;
				Rectangle bounds2 = n2.getBounds();
				int x2 = (int) bounds2.getCenterX();
				int y2 = (int) bounds2.getCenterY();
				if ((x1 == x2) && (y1 == y2)) {
					if (l == null) {
						l = new ArrayList<Vertex>();
						l.add(n1);
					}
					l.add(n2);
				}
			}
			if (l != null) {
				out.add(l);
				System.out.println(l);
			}
		}
		System.out.printf("%d sets of entity nodes with identical centrepoint\n", out.size());
		return out;
	}
	
	/* 
	 * Finds sets of entity nodes which have identical centre point and distributes them evenly
	 * on the perimter of a circle around the centrepoint. Circle diameter is the average of node
	 * diagonals. Starting bearing of the spreading is perpendicular to the average bearing (if
	 * phi < 0 phi = phi + PI) of the connected nodes. The initial idea was to spread the node on
	 * a straight line perpendicular to the average bearing (with the same qualification). However,
	 * circular spreading was easier to implement as I had bits ready.
	 */
	public void spreadEntityNodesWithIdenticalCentrePoint () {
		Map nested = new HashMap();
		List<List<Vertex>> entitySets = findSetsOfEntityNodesWithIdenticalCenter();
		for (List<Vertex> entitySet : entitySets) {
			Vertex pole = entitySet.get(0);
			Set<Vertex> satellites = pole.getConnectedNodes();
			Set<PolarCoordinates> polarCoordinates = Utils.getPolarCoordinates(pole, satellites);
			double phi = 0;
			for (PolarCoordinates pc : polarCoordinates) {
				if (pc.phi < 0)
					pc.phi += Math.PI;
				phi += pc.phi;
			}
			phi = phi/polarCoordinates.size() - Math.PI/2;
			nested.putAll(distributeNodesOnCircleStartingFromBearing(entitySet, phi, null));
		}
		model.edit(nested, null, null, null);
	}
	
	/*
	 * Find sets (well, technically Lists) of Nodes with identical connectivity 
	 * and with at least 2 connected nodes.
	 */
	public List<List<Vertex>> findSetsOfEntityNodesWithIdenticalConnectivity () {
		List<List<Vertex>> out = new ArrayList<List<Vertex>>();
		for (int i = 0 ; i < verteces.size() ; i++) {
			List<Vertex> l = null;
			Vertex n1 = (Vertex)verteces.get(i);
			if (!n1.isEntityVertex()) continue;
			Set<Vertex> c1 = n1.getConnectedNodes();
			if (c1.size() < 2) continue;
			for (int j = i + 1; j < verteces.size() ; j++) {
				Vertex n2 = (Vertex)verteces.get(j);
				if (!n2.isEntityVertex()) continue;
				Set<Vertex> c2 = n2.getConnectedNodes();
				if ((c1.size() == c2.size()) && c1.containsAll(c2)) {
					if (l == null) {
						l = new ArrayList<Vertex>();
						l.add(n1);
					}
					l.add(n2);
				}
			}
			if (l != null) {
				out.add(l);
				System.out.println(l);
			}
		}
		System.out.printf("%d sets of entity nodes with identical connectivity\n", out.size());
		return out;
	}
	
	/* 
	 * Finds sets of entity nodes which have identical connectivity and distributes them evenly
	 * on the perimter of a circle around the centrepoint. Circle diameter is the average of node
	 * diagonals. Starting bearing of the spreading is perpendicular to the average bearing (if
	 * phi < 0 phi = phi + PI) of the connected nodes. The initial idea was to spread the node on
	 * a straight line perpendicular to the average bearing (with the same qualification). However,
	 * circular spreading was easier to implement as I had bits ready.
	 */
	public void spreadEntityNodesWithIdenticalConnectivity () {
		Map nested = new HashMap();
		List<List<Vertex>> entitySets = findSetsOfEntityNodesWithIdenticalConnectivity();
		for (List<Vertex> entitySet : entitySets) {
			Vertex pole = entitySet.get(0);
			Set<Vertex> satellites = pole.getConnectedNodes();
			Set<PolarCoordinates> polarCoordinates = Utils.getPolarCoordinates(pole, satellites);
			double phi = 0;
			for (PolarCoordinates pc : polarCoordinates) {
				if (pc.phi < 0)
					pc.phi += Math.PI;
				phi += pc.phi;
			}
			phi = phi/polarCoordinates.size() - Math.PI/2;
			nested.putAll(distributeNodesOnCircleStartingFromBearing(entitySet, phi, null));
		}
		model.edit(nested, null, null, null);
	}
	
	/*
	 * Spreads given set of verteces on the perimeter of a circle starting from the given bearing
	 * and centred at Point center or, if null, at the average centrepoint of the verteces in the
	 * set.
	 */
	public Map<Vertex,Map> distributeNodesOnCircleStartingFromBearing (Collection<Vertex> nodes, double bearing, Point center) {
		int px;
		int py;
		if (center == null) {
			Rectangle bounds = nodes.iterator().next().getBounds();
			px = (int)bounds.getCenterX();
			py = (int)bounds.getCenterY();
		} else {
			px = center.x;
			py = center.y;			
		}
		double r = 0;
		for (Vertex v : nodes) {
			Rectangle bounds = v.getBounds();
			r += Math.sqrt(bounds.width*bounds.width+bounds.height*bounds.height)/2;
		}
		r /= nodes.size();
		double step = 2 * Math.PI / nodes.size();
		Map<Vertex,Map> nested = new HashMap<Vertex,Map>();
		for (Vertex v : nodes) {
			PolarCoordinates spc = new PolarCoordinates(r, bearing);
			Point p = spc.toCartesian();
			p.x += px;
			p.y += py;
			Rectangle bounds = v.getBounds();
			//System.out.printf("\t%s\n", bounds);
			p.x -= bounds.width / 2;
			p.y -= bounds.height / 2;
			bounds.x = p.x;
			bounds.y = p.y;
			//System.out.printf("\t%s\n", bounds);
			Map attMap = new HashMap();
			GraphConstants.setBounds(attMap,bounds);
			//GraphConstants.setBackground(attMap,Color.magenta);
			nested.put(v, attMap);
			bearing += step;
		}
		return nested;
	}
	
	public void unfixOverlappingEntityNodes () {
		Set<Vertex> overlappingEntitiesSet = findSignificantlyOverlappingEntityNodes();
		System.out.printf("Found %d significantly overlapping entities\n", overlappingEntitiesSet.size());
		for (Vertex v : verteces) {
			v.setFixed(overlappingEntitiesSet.contains(v) ? false: true);
		}
		Map nested = new HashMap();
		for (Vertex v : overlappingEntitiesSet) {
			Map attMap = new HashMap();
			GraphConstants.setBackground(attMap, Color.red);
			nested.put(v, attMap);
		}
		model.edit(nested, null, null, null);
	}
	
	/*
	 * ehem... vertex.translate() did not seem to work for a single translation
	 * only but is OK when issued from relax* functions.
	 */
	public void translate (Vertex v, int x, int y) {
		Rectangle bounds = v.getBounds();
		bounds.x += x;
		bounds.y += y;
		Map attMap = new HashMap();
		GraphConstants.setBounds(attMap,bounds);
		Map nested = new HashMap();
		nested.put(v, attMap);
		model.edit(nested, null, null, null);
	}
	
}