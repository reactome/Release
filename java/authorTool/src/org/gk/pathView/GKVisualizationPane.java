/*
 * Created on Sep 25, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gk.pathView;

/**
 * @author vastrik
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.gk.database.FrameManager;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

public class GKVisualizationPane extends JComponent implements Runnable, VisualizationConstants {
	public static final int EDGE_SENSING_DIST_SQR = 16;
	public static final int VERTEX_SENSING_DIST_SQR = 25;
    // Property key for enabling write permission of Reaction coordinates
    public static final String COOR_WRITE_ENABLE_KEY = "enableSaveRxnCoordinates";
	// Class name of reaction coordinates
	private final String COORDINATE_CLASS_NAME = "ReactionCoordinates";
	protected Map instance2ReactionEdgeMap = new HashMap();
	protected Set edges = Collections.synchronizedSet(new HashSet());
	protected Set newEdges = Collections.synchronizedSet(new HashSet());
	protected Set loadedEdges = Collections.synchronizedSet(new HashSet());
	//protected Collection edges = Collections.synchronizedList(new ArrayList());
	//protected Collection newEdges = Collections.synchronizedList(new ArrayList());
	//protected Collection loadedEdges = Collections.synchronizedList(new ArrayList());
	protected Set verteces = Collections.synchronizedSet(new HashSet());
	protected Set newVerteces = Collections.synchronizedSet(new HashSet());
	protected Set loadedVerteces = Collections.synchronizedSet(new HashSet());
	//protected List verteces = Collections.synchronizedList(new ArrayList());
	//protected List newVerteces = Collections.synchronizedList(new ArrayList());
	//protected List loadedVerteces = Collections.synchronizedList(new ArrayList());
	public static int CONNECTION_LENGTH = 1;
	public static int REACTION_LENGTH = 20;
	public static int ARROW_SIZE = 5;
	public static int SURROUNDING_SPACE = 3;
	public static int MIN_VERTEX_DISTANCE = REACTION_LENGTH;
	protected Thread relaxer;
	protected boolean relax = true;
	protected boolean relaxerSuspended = true; // Default disable
	protected Dimension dimension;
	protected static Color DEFAULT_REACTION_COLOR = new Color(0,0,0,127);
	protected static Color DEFAULT_LOADED_REACTION_COLOR = new Color(0,0,127,200);
	protected static Color CONNECTION_COLOR = new Color(127,127,127,127);
	protected static Color DEFAULT_AURA_COLOR = new Color(255,0,0,16);
	//private static Color CONNECTION_COLOR = Color.white;
	protected static Color BACKGROUND_COLOR = Color.white;
	protected static BasicStroke REACTION_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	//protected static BasicStroke CONNECTION_STROKE = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    protected static BasicStroke CONNECTION_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	private final BasicStroke SELECT_RECTANGLE_STROKE = new BasicStroke(1.0f);
	protected boolean relaxWithoutRepaint = false;
	protected JButton screenUpdateButton;
	protected PathwayBrowserPanel hierarchyPanel;
	protected JFrame frame;
	protected double MAGNIFICATION = 1;
	protected JViewport viewport;
	protected Collection selected = new HashSet(); //contains selected GK Reactions, not Edges
	private GKInstance locationContext;
	private JLabel contextLabel;
	private ActionCollection actionCollection;
	private MySQLAdaptor dba;
	private Schema schema;
	private boolean ignoreTreeSelectionEvents = false;
	public static int WIDTH_HEIGHT_RATIO = 6;
	public static int DEFAULT_CANVAS_WIDTH = 600;
	public boolean debug = false;
	// For edge selection
	private ReactionEdge selectedEdge = null;
	// For rectangle selection
	private Rectangle selectRect = new Rectangle();
	private VisualizationToolActions toolActions = new VisualizationToolActions(this);
	// For view box selection
	private boolean isViewBoxDragged;
	private Rectangle scrollViewRect = new Rectangle(0, 0, RESIZE_WIDGET_WIDTH, RESIZE_WIDGET_WIDTH);
	// For status information
	protected JLabel statusLabel;
	// For overall dialog
	protected OverallViewPane overallPane;
	private boolean isFromOverallView = false;
	// For one step undo support
	protected UndoableMoveEdit moveEdit = new UndoableMoveEdit(this);
	// To control selection synchronization
	protected boolean synchronizingEdgeSelection = true; // Default is true
	// To control save
	protected boolean isDirty = false;
	// For data overaly
	protected JSlider slider = null;
	// To control browser 
	protected JSplitPane jsp;
	// To control the selection
	private TreeSelectionListener treeSelectionListener;
	// TO control zoom factors
	protected double[] zoomFactors = new double[]{0.2, 0.4, 0.6, 0.8, 1.0, 1.5, 2.0};
	private JToolBar toolbar;
    // To control if the user can save coordinates to the database
    private boolean isWritable = false;
	
	public GKVisualizationPane() {
	}

	public GKVisualizationPane(MySQLAdaptor dba) {
        setDba(dba);
        setSchema(dba.getSchema());
		init();
		try {
            hierarchyPanel.setMySQLAdaptor(dba, true);
			loadEventLocations();
		} 
        catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Add this method for curator tool
	public JFrame getFrame() {
		return frame;
	}

	public void init() {
		actionCollection = new ActionCollection(this);
		frame = new JFrame();
		ImageIcon icon = GKApplicationUtilities.createImageIcon(getClass(), "R-small.png");
		frame.setIconImage(icon.getImage());
		FrameManager.getManager().setIconImage(icon.getImage());
		// For data overlay
		slider = new JSlider();
		// Add a menubar
		JMenuBar menubar = new VisualizationMenuBar(this);
		frame.setJMenuBar(menubar);
		// Add a JToolBar
		toolbar = new VisualizationToolToolBar(toolActions);
		toolbar.setFloatable(false);
		frame.getContentPane().add(toolbar, BorderLayout.NORTH);
		// To synchronize the zoom selection
		final JSlider zoomSlider = ((VisualizationToolToolBar)toolbar).getZoomSlider();
		final JMenu zoomMenu = ((VisualizationMenuBar)menubar).getZoomMenu();
		synchronizeZoom(zoomSlider, zoomMenu);
		// Commented out for curator tool
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Add these listener for curator tool
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				relax = false; // Set mark to stop relaxation.
			}
		});
		frame.setTitle("Pathway Visualization Tool");
		JPanel panel = new JPanel(new BorderLayout());
		hierarchyPanel = new PathwayBrowserPanel();
		treeSelectionListener = new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				handleTreeSelection(e);
			}
		};
		hierarchyPanel.addSelectionListener(treeSelectionListener);
		final JScrollPane scrollPane = new JScrollPane(this);
		viewport = scrollPane.getViewport();
		panel.add(scrollPane, BorderLayout.CENTER);
		//panel.add(createControlPanel(), BorderLayout.SOUTH);
		panel.add(slider, BorderLayout.NORTH);
		// Default invisible
		slider.setVisible(false);
		
		jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, hierarchyPanel);
		jsp.setPreferredSize(new Dimension(800,600));
		jsp.setDividerLocation(300);
		frame.getContentPane().add(jsp);
		// For status
		statusLabel = new JLabel();
		Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 4);
		Border bevelBorder = BorderFactory.createLoweredBevelBorder();
		statusLabel.setBorder(BorderFactory.createCompoundBorder(bevelBorder, emptyBorder));
		frame.getContentPane().add(statusLabel, BorderLayout.SOUTH);
		setInitialDimension();
		frame.pack();
		//frame.setVisible(true);
		MouseHandler mouseHandler = new MouseHandler();
		addMouseListener(mouseHandler);
		addMouseMotionListener(mouseHandler);
		relaxer = new Thread(this, "Relaxer");
		relax = true;
		relaxer.start();
		// To enable tooltip text
		setToolTipText("");
		statusLabel.setText("Viewbox: " + dimension.width + " : " + dimension.height);
		// For overall view pane
		overallPane = new OverallViewPane();
		overallPane.setWidthToHeightRatio(WIDTH_HEIGHT_RATIO);
		overallPane.setEdges(edges);
		overallPane.setOriginalDimension(dimension);
		viewport.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (isFromOverallView) {
					isFromOverallView = false; // Don't pump the state change event
					return;
				}
				Rectangle viewRect = new Rectangle(viewport.getViewRect());
				viewRect.x /= MAGNIFICATION;
				viewRect.y /= MAGNIFICATION;
				viewRect.width /= MAGNIFICATION ;
				viewRect.height /= MAGNIFICATION;
				overallPane.setVisibleRect(viewRect);
			}
		});
		overallPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				String propName = e.getPropertyName();
				if (propName.equals("isVisible")) {
					Boolean newValue = (Boolean) e.getNewValue();
					if (newValue.booleanValue()) 
						toolActions.getOverallViewAction().putValue(Action.NAME, "Hide Overall View");
					else 
						toolActions.getOverallViewAction().putValue(Action.NAME, "Show Overall View");
				}
				else if (propName.equals("visibleRect")) {
					Rectangle visibleRect = (Rectangle) e.getNewValue();
					isFromOverallView = true;
					Point p = new Point(visibleRect.getLocation());
					p.x *= MAGNIFICATION;
					p.y *= MAGNIFICATION;
					viewport.setViewPosition(p);
				}
			}
		});
	}
	
	private void synchronizeZoom(final JSlider zoomSlider, final JMenu zoomMenu) {
		zoomSlider.addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
		        JSlider slider = (JSlider) e.getSource();
		        int value = slider.getValue();
		        JRadioButton btn = (JRadioButton) zoomMenu.getMenuComponent(value);
		        btn.setSelected(true);
		    }
		});
		ActionListener l = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        JRadioButton btn = (JRadioButton) e.getSource();
		        int index = 0;
		        for (int i = 0; i < zoomMenu.getMenuComponentCount(); i++) {
		            if (zoomMenu.getMenuComponent(i) == btn) {
		                index = i;
		                break;
		            }
		        }
		        zoomSlider.setValue(index);
		    }
		};
		JRadioButton btn;
		for (int i = 0; i < zoomMenu.getMenuComponentCount(); i++) {
		    btn = (JRadioButton) zoomMenu.getMenuComponent(i);
		    btn.addActionListener(l);
		}
	}

	private void setInitialDimension() {
		int max_x = 0;
		int max_y = 0;
		try {
			ResultSet rs = dba.executeQuery("SELECT MAX(sourceX),MAX(sourceY),MAX(targetX),MAX(targetY) FROM " +				                            COORDINATE_CLASS_NAME, null);
			if (rs != null) {
				rs.next();
				int sx = rs.getInt(1);
				int sy = rs.getInt(2);
				int tx = rs.getInt(3);
				int ty = rs.getInt(4);
				max_x = Math.max(sx, tx);
				max_y = Math.max(sy, ty);
				max_x = Math.max(max_x, max_y * WIDTH_HEIGHT_RATIO);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if ((max_x > 0) && (max_y > 0)) {
			dimension = new Dimension(max_x, max_y);
		} else {
			dimension = new Dimension(DEFAULT_CANVAS_WIDTH, DEFAULT_CANVAS_WIDTH / WIDTH_HEIGHT_RATIO);
		}
		setPreferredSize(dimension);
		//System.out.println(dimension);
	}

	private Collection getReactionEdges (Collection reactions) {
		Collection out = new HashSet();
		for (Iterator ri = reactions.iterator(); ri.hasNext();) {
			Object o;
			if ((o = instance2ReactionEdgeMap.get(ri.next())) != null) {
				out.add(o);
			}
		}
		return out;
	}

	private Collection insertAndConnectReactions (Collection events) throws InvalidAttributeException, Exception {
		Collection c = grepReactionsWithoutInstances(events);
		Collection edges = insertReactions(c);
		connectReactions(c);
		return edges;
	}

	public void colorSelected() {
		for (Iterator ei = edges.iterator(); ei.hasNext();) {
			AbstractEdge edge = (AbstractEdge) ei.next();
			if (edge instanceof ReactionEdge) {
				if (selected.contains(((ReactionEdge) edge).getReactionInstance())) {
					edge.setColor(SELECTED_REACTION_COLOR);
				} else {
					edge.setColor(null);
				}
			}
		}
		if (overallPane != null)
			overallPane.select(selected);
	}

//	public void colorSelected() {
//		for (Iterator ei = edges.iterator(); ei.hasNext();) {
//			AbstractEdge edge = (AbstractEdge) ei.next();
//			if (edge instanceof ReactionEdge) {
//				if (selected.contains(edge)) {
//					edge.setColor(SELECTED_REACTION_COLOR);
//				} else {
//					edge.setColor(null);
//				}
//			}
//		}
//	}

	public void setReactionColor(Collection reactions, Color color) {
		for (Iterator ei = edges.iterator(); ei.hasNext();) {
			AbstractEdge edge = (AbstractEdge) ei.next();
			if (edge instanceof ReactionEdge) {
				ReactionEdge re = (ReactionEdge) edge;
				if (reactions.contains(re.getReactionInstance())) {
					edge.setColor(color);
				} else {
					edge.setColor(null);
				}
			}
		}
	}

	public void setReactionColor(GKInstance reaction, Color color) {
		for (Iterator ei = edges.iterator(); ei.hasNext();) {
			AbstractEdge edge = (AbstractEdge) ei.next();
			if (edge instanceof ReactionEdge) {
				ReactionEdge re = (ReactionEdge) edge;
				if (reaction == re.getReactionInstance()) {
					edge.setColor(color);
				} else {
					edge.setColor(null);
				}
			}
		}
	}

	protected Collection grepReactionsWithoutInstances(Collection events) {
		events = InstanceUtilities.grepSchemaClassInstances(events, new String[] {ReactomeJavaConstants.ReactionlikeEvent},true);
		Collection out = new ArrayList();
		for (Iterator ii = events.iterator(); ii.hasNext();) {
			try {
				GKInstance instance = (GKInstance) ii.next();
				//System.out.println("grepReactionsWithoutInstances\t" + instance.toString());
				if (instance.getSchemClass().isValidAttribute("hasInstance") &&
				    instance.getAttributeValue("hasInstance") != null) {
				    continue;
				}
				out.add(instance);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return out;
	}

	private Collection insertReactions(Collection reactions) {
		int insert_x = (int) (Math.random() * (dimension.width - 2 * SURROUNDING_SPACE) + SURROUNDING_SPACE);
		int insert_y = (int) (Math.random() * (dimension.height - 2 * SURROUNDING_SPACE - REACTION_LENGTH) + SURROUNDING_SPACE);
		Collection edges = new HashSet();
		for (Iterator ri = reactions.iterator(); ri.hasNext();) {
			GKInstance reaction = (GKInstance) ri.next();
			//System.out.println(reaction.toString());
			if (! instance2ReactionEdgeMap.containsKey(reaction)) {
				//int insert_x = (int) (Math.random() * (dimension.width - 2 * SURROUNDING_SPACE) + SURROUNDING_SPACE);
				//int insert_y = (int) (Math.random() * (dimension.height - 2 * SURROUNDING_SPACE - REACTION_LENGTH) + SURROUNDING_SPACE);
				ReactionEdge reactionEdge = new ReactionEdge(reaction, insert_x, insert_y);
				instance2ReactionEdgeMap.put(reaction,reactionEdge);
				edges.add(reactionEdge);
			} else {
				edges.add(instance2ReactionEdgeMap.get(reaction));
			}
		}
		return edges;
	}

    public boolean isReactionLoaded(GKInstance reaction) {
        return instance2ReactionEdgeMap.containsKey(reaction);
    }

    public void addPrecedingAndFollowingReactions(Collection reactions)
                    throws InvalidAttributeException, Exception {
		Set tmp = new HashSet();
		tmp.addAll(reactions);
		Set tmp2 = new HashSet();
		while (! tmp.isEmpty()) {
			for (Iterator ti = tmp.iterator(); ti.hasNext();) {
				GKInstance reaction = (GKInstance) ti.next();
				Collection preceding = reaction.getAttributeValuesList("precedingEvent");
				if (preceding != null) {
					for (Iterator pi = preceding.iterator(); pi.hasNext();) {
						Object o = pi.next();
						if (! reactions.contains(o)) {
							reactions.add(o);
							tmp2.add(o);
						}
					}
				}
				Collection following = reaction.getReferers("precedingEvent");
				if (following != null) {
					for (Iterator fi = following.iterator(); fi.hasNext();) {
						Object o = fi.next();
						if (! reactions.contains(o)) {
							reactions.add(o);
							tmp2.add(o);
						}
					}
				}
			}
			tmp.clear();
			tmp.addAll(tmp2);
			tmp2.clear();
		}
	}

	private void connectReactions(Collection reactions) throws Exception {
		for (Iterator ii = reactions.iterator(); ii.hasNext();) {
			GKInstance reaction = (GKInstance) ii.next();
			Collection currentReactions = new ArrayList();
			currentReactions.add(reaction);
			Collection allPrecedingEvents = new HashSet();
			Collection precedingEvents = new HashSet();
			Collection c;
			if ((c = reaction.getAttributeValuesListNoCheck("instanceOf")) != null)
				currentReactions.addAll(c);
			for (Iterator cri = currentReactions.iterator(); cri.hasNext();) {
				GKInstance cr = (GKInstance) cri.next();
				if ((c = cr.getAttributeValuesList("precedingEvent")) != null) {
					allPrecedingEvents.addAll(c);
				}
			}
			for (Iterator apei = allPrecedingEvents.iterator(); apei.hasNext();) {
				GKInstance pe = (GKInstance) apei.next();
				if (pe.getSchemClass().isValidAttribute("hasInstance") &&
				    pe.getAttributeValue("hasInstance") != null) {
					precedingEvents.addAll(pe.getAttributeValuesList("hasInstance"));
				}
				else {
					precedingEvents.add(pe);
				}
			}
			ReactionEdge reactionEdge = (ReactionEdge) instance2ReactionEdgeMap.get(reaction);
			for (Iterator pei = precedingEvents.iterator(); pei.hasNext();) {
				GKInstance pe = (GKInstance) pei.next();
				if (reactions.contains(pe)) {
					ReactionEdge precedingEdge = (ReactionEdge) instance2ReactionEdgeMap.get(pe);
					if (precedingEdge != null) {
						makeConnectionIfNecessary(precedingEdge,reactionEdge);
					} else {
						System.err.println("No ReactionEdge for " + pe);
					}
				}	
			}
		}
	}

	private ConnectionEdge makeConnectionIfNecessary(ReactionEdge precedingEdge, ReactionEdge reactionEdge) {
		ConnectionEdge connectionEdge;
		if ((connectionEdge = (ConnectionEdge) reactionEdge.getSourceVertex().getConnectionEdges().get(precedingEdge)) == null) {
			connectionEdge = new ConnectionEdge(precedingEdge,reactionEdge);
			reactionEdge.getSourceVertex().getConnectionEdges().put(precedingEdge, connectionEdge);
			//Commented out because not necessary as yet.
			//precedingEdge.getTargetVertex().getConnectionEdges().put(reactionEdge, connectionEdge);
		}
		return connectionEdge;
	}
	
	private void setSelected(GKInstance instance) {
		
	}
	
	abstract class AbstractEdge implements IEdge {
		protected Vertex sourceVertex;
		protected Vertex targetVertex;
		protected int preferredLength;
		public GKInstance instance;
		protected Color color = null;
		
		public AbstractEdge() {
		}

		public AbstractEdge(GKInstance reaction) {
			instance = reaction;
		}
		/**
		 * @return
		 */
		public Vertex getSourceVertex() {
			return sourceVertex;
		}
		
		/**
		 * @return
		 */
		public Vertex getTargetVertex() {
			return targetVertex;
		}
		
		/**
		 * @param vertex
		 */
		public void setSourceVertex(Vertex vertex) {
			sourceVertex = vertex;
		}

		/**
		 * @param vertex
		 */
		public void setTargetVertex(Vertex vertex) {
			targetVertex = vertex;
		}

		/**
		 * @return
		 */
		public int getPreferredLength() {
			return preferredLength;
		}

		/**
		 * @param i
		 */
		public void setPreferredLength(int i) {
			preferredLength = i;
		}
		
		public Dimension getDimension() {
			return new Dimension(targetVertex.x - sourceVertex.x, targetVertex.y - sourceVertex.y);
		}
		
		public double getLength() {
			Dimension d = getDimension();
			return Math.sqrt(d.getWidth() * d.getWidth() + d.getHeight() * d.getHeight());
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append(this.getClass().getName());
			buf.append(" from:" + getSourceVertex().toString() + " to:" + getTargetVertex().toString());
			//buf.append(" getDistance:" + getDimension().toString() + "\n");
			buf.append(" length:" + getLength() + " preferredLength:" + getPreferredLength() + "\n");
			return buf.toString();
		}

		/**
		 * @return
		 */
		public Color getColor() {
			return color;
		}

		/**
		 * @param color
		 */
		public void setColor(Color color) {
			this.color = color;
		}

		protected boolean isFixed = false;
		/**
		* @return
		*/
		public boolean isFixed() {
			return isFixed;
		}

		/**
		* @param b
		*/
		public void setFixed(boolean b) {
			isFixed = b;
			getSourceVertex().isFixed = b;
			getTargetVertex().isFixed = b;
		}
		
		public void translate(int x, int y) {
			sourceVertex.x += x;
			sourceVertex.y += y;
			targetVertex.x += x;
			targetVertex.y += y;
		}
		
		public void setTail(int x, int y) {
			sourceVertex.x = x;
			sourceVertex.y = y;
		}
		
		public void setHead(int x, int y) {
			targetVertex.x = x;
			targetVertex.y = y;
		}

		protected void finalize() {
			if (debug) System.out.println("Finalized " + this);
		}
		
		public int getTailX() {
			return sourceVertex.x;
		}
		
		public int getTailY() {
			return sourceVertex.y;
		}
		
		public int getHeadX() {
			return targetVertex.x;
		}
		
		public int getHeadY() {
			return targetVertex.y;
		}
		
		public int length() {
			return (int) Point2D.distance(sourceVertex.x, sourceVertex.y,
			                              targetVertex.x, targetVertex.y);
		}
		
		public Object getUserObject() {
			return null;
		}

	}

	class ReactionEdge extends AbstractEdge {

		private boolean isLoaded = false;
		private GKInstance coordinatesInstance;
		
		public ReactionEdge(GKInstance instance) {
			int sx = 0;
			int sy = 0;
			int tx = 0;
			int ty = 0;
			try {
				if (instance.getSchemClass().isa(COORDINATE_CLASS_NAME)) {
					setCoordinatesInstance(instance);
					sx = ((Integer) instance.getAttributeValue("sourceX")).intValue();
					sy = ((Integer) instance.getAttributeValue("sourceY")).intValue();
					tx = ((Integer) instance.getAttributeValue("targetX")).intValue();
					ty = ((Integer) instance.getAttributeValue("targetY")).intValue();				
				} else { // Assume it is a Reaction instance
					setCoordinatesInstance(createCoordinatesInstance(instance));
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setPreferredLength(REACTION_LENGTH);
			setSourceVertex(new Vertex(sx,sy,this));
			setTargetVertex(new Vertex(tx,ty,this));	
			newEdges.add(this);
		}
		
		public ReactionEdge(GKInstance reaction, int x, int y) {
			this(reaction);
			//System.err.println(reaction.toString() + " x=" + x + " y=" + y);
			setPreferredLength(REACTION_LENGTH);
			setSourceVertex(new Vertex(x,y,this));
			setTargetVertex(new Vertex(x, y + REACTION_LENGTH,this));	
			newEdges.add(this);
		}

		public ReactionEdge(GKInstance reaction, int sx, int sy, int tx, int ty) {
			this(reaction);
			//System.err.println(reaction.toString() + " x=" + x + " y=" + y);
			setPreferredLength(REACTION_LENGTH);
			setSourceVertex(new Vertex(sx,sy,this));
			setTargetVertex(new Vertex(tx,ty,this));	
			newEdges.add(this);
		}
		
		private GKInstance createCoordinatesInstance(GKInstance reaction) throws InvalidAttributeException, InvalidAttributeValueException {
			SchemaClass cls = getSchema().getClassByName(COORDINATE_CLASS_NAME);
			GKInstance el = new GKInstance(cls);
			if (cls != null)
				el.setAttributeValue("locatedEvent", reaction);
			el.setIsInflated(true);
			return el;
		}
		
		public void disconnect() {
			getSourceVertex().removeAllConnectionEdges();
			for (Iterator vi = newVerteces.iterator(); vi.hasNext();) {
				Vertex v = (Vertex) vi.next();
				v.disconnectFromReactionEdge(this);
			}
		}
		
		/**
		 * @return
		 */
		public boolean isLoaded() {
			return isLoaded;
		}

		/**
		 * @param b
		 */
		public void setLoaded(boolean b) {
			isLoaded = b;
		}

		/**
		 * @return
		 */
		public GKInstance getCoordinatesInstance() {
			return coordinatesInstance;
		}

		/**
		 * @return
		 */
		public GKInstance getReactionInstance() {
			GKInstance ri = null;
			try {
				if (coordinatesInstance != null &&
				    coordinatesInstance.getSchemClass() != null)
					ri = (GKInstance) coordinatesInstance.getAttributeValue("locatedEvent");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ri;
		}

		/**
		 * @param instance
		 */
		public void setCoordinatesInstance(GKInstance instance) {
			coordinatesInstance = instance;
		}
		
		public boolean isPicked(int x, int y) {
			int distSqr = (int) Line2D.ptSegDistSq(sourceVertex.x, sourceVertex.y,
			                                       targetVertex.x, targetVertex.y,
			                                       x, y);
			if (distSqr < EDGE_SENSING_DIST_SQR)
				return true;
			return false;
		}
		
		public int getType() {
			return REACTION_EDGE;
		}
		
		public Object getUserObject() {
			return getReactionInstance();
		}

	}

	class ConnectionEdge extends AbstractEdge {
		private ReactionEdge from;
		private ReactionEdge to;

		public ConnectionEdge(ReactionEdge from, ReactionEdge to) {
			this.from = from;
			this.to = to;
			setPreferredLength(CONNECTION_LENGTH);
			setSourceVertex(from.getTargetVertex());
			setTargetVertex(to.getSourceVertex());
			newEdges.add(this);
		}
		
		public int getType() {
			return LINK_EDGE;
		}
	}
    
	class Vertex {
    	
		public double dx;
		public double dy;
    	public int x;
    	public int y;
    	public boolean isFixed = false;
		public boolean inFocus = false;
		public ReactionEdge reactionEdge = null;
		private Map connectionEdges = new HashMap();
    	    	
		public Vertex (int x, int y, ReactionEdge reactionEdge) {
			this.x = x;
			this.y = y;
			this.reactionEdge = reactionEdge;
			newVerteces.add(this); 
		}
    	
		public Vertex() {
			this(100,100,null);
		}
    	
		public Dimension getDistance(Vertex vertex) {
			return new Dimension(this.x - vertex.x, this.y - vertex.y);
		}

//		public void translate(int x, int y) {
//			this.x += x;
//			this.y += y;
//		}
    	
		public void translate(int x, int y) {
			int new_x = this.x + x;
			int new_y = this.y + y;
			if (new_x < 1) {
				new_x = 1;
			}
			if (new_y < 1) {
				new_y = 1;
			} else if (new_y > dimension.height - 1) {
				new_y = dimension.height - 1;
			}
			this.x = new_x;
			this.y = new_y;
		}
		
		public boolean isPicked(int x, int y) {
			double distSqr = Point2D.distanceSq(this.x, this.y, x, y);
			if (distSqr < VERTEX_SENSING_DIST_SQR)
				return true;
			return false;
		}

//		public void translate(int dx, int dy) {
//			int new_x = this.x + dx;
//			int new_y = this.y + dy;
//			if ((new_x > SURROUNDING_SPACE) && (new_x < (dimension.width - SURROUNDING_SPACE))) {
//				this.x = new_x;
//			}
//			if ((new_y > SURROUNDING_SPACE) && (new_y < (dimension.height - SURROUNDING_SPACE))) {
//				this.y = new_y;
//			}
//		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append(this.getClass().getName());
			buf.append(" [" + x + "," + y + "]");
			return buf.toString();
		}
		/**
		 * @return
		 */
		public Map getConnectionEdges() {
			return connectionEdges;
		}

		public void disconnectFromReactionEdge(ReactionEdge re) {
			ConnectionEdge ce = (ConnectionEdge) connectionEdges.get(re);
			if (ce != null) {
				newEdges.remove(ce);
				connectionEdges.remove(re);
			}
		}
		
		public void removeAllConnectionEdges() {
			newEdges.removeAll(connectionEdges.values());
			connectionEdges.clear();
		}
		
		public void setX(int x) {
			this.x = x;
		}
		
		public void setY(int y) {
			this.y = y;
		}
		
		public int getX() {
			return x;
		}
		
		public int getY() {
			return y;
		}

	}
    
	class MouseHandler implements MouseMotionListener, MouseListener {

		private Vertex focus;
		//private Set selectedEdges = new HashSet();
		private Collection selectedEdges = null;
		private Point prevPoint = new Point();
		private double focusLength;
		private boolean isDragging;
				
		public void mouseDragged(MouseEvent e) {
			int x = screen2ModelCoordinate(e.getX());
			int y = screen2ModelCoordinate(e.getY());
			if (isDragging) {
//				int dx = x - focus.x;
//				int dy = y - focus.y;
				int dx = x - prevPoint.x;
				int dy = y - prevPoint.y;
				if (selectedEdges == null)
					selectedEdges = getSelectedEdges();
				translateEdges(selectedEdges, dx, dy);
				prevPoint.x = x;
				prevPoint.y = y;
				revalidate();
				scrollViewRect.x = x + RESIZE_WIDGET_WIDTH;
				scrollViewRect.y = y + RESIZE_WIDGET_WIDTH;
				scrollRectToVisible(scrollViewRect);
				repaint(getVisibleRect());
				isDirty = true;
			} 
			else {
				if (isViewBoxDragged) {
					dimension.width = x;
					dimension.height = x / WIDTH_HEIGHT_RATIO;
					showStatus();
					revalidate();
					scrollViewRect.x = dimension.width + RESIZE_WIDGET_WIDTH;
					scrollViewRect.y = dimension.height + RESIZE_WIDGET_WIDTH;
					scrollRectToVisible(scrollViewRect);
					repaint(getVisibleRect());
				}
				else if (focus != null && focus.reactionEdge.isLoaded()) {
					Vertex v = null;
					if (focus.reactionEdge.targetVertex == focus)
						v = focus.reactionEdge.sourceVertex;
					else
						v = focus.reactionEdge.targetVertex;
					double dist = Point2D.distance(v.x, v.y, x, y);
					double ratio = focusLength / dist;
					double dy = ratio * (y - v.y);
					double dx = ratio * (x - v.x);
					focus.x = (int)(dx + v.x);
					focus.y = (int)(dy + v.y);
					repaint(getVisibleRect());
				}
				else { // Draw selection rectangle
					selectRect.width = x - selectRect.x;
					selectRect.height = y - selectRect.y;
					selectEdges(selectRect);
					colorSelected();
					int dirtyX = (int)((selectRect.x - 10) * MAGNIFICATION);
					int dirtyY = (int)((selectRect.y - 10) * MAGNIFICATION);
					int dirtyW = (int)((selectRect.width + 20) * MAGNIFICATION);
					int dirtyH = (int)((selectRect.height + 20) * MAGNIFICATION);
					repaint(dirtyX, dirtyY, dirtyW, dirtyH);
				}
			}
		}

		public void mouseMoved(MouseEvent arg0) {
		}

		public void mouseClicked(MouseEvent e) {
		}
		
		private boolean isForDragging(int x, int y) {
			if (selected.size() == 0)
				return false;
			for (Iterator it = selected.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				ReactionEdge edge = (ReactionEdge) instance2ReactionEdgeMap.get(instance);
				if (edge != null && edge.isPicked(x, y))
					return true;
			}
			return false;
		}

		public void mousePressed(MouseEvent e) {
			requestFocus();
			if (e.isPopupTrigger()) {
				toolActions.doPopup(e);
				return;
			}
			int x = screen2ModelCoordinate(e.getX());
			int y = screen2ModelCoordinate(e.getY());
			// Check view box selection first
			if (Point2D.distanceSq(dimension.width, dimension.height, x, y) < VERTEX_SENSING_DIST_SQR * 2) {
				isViewBoxDragged = true;
				isDragging = false;
				repaint(getVisibleRect());
				return;
			}
			isViewBoxDragged = false;
			selectedEdges = null;
			prevPoint.x = x;
			prevPoint.y = y;
			if (focus != null && focus.isPicked(x, y)) {
				isDragging  = false;
				return;
			}
			isDragging = e.isShiftDown() ? false : isForDragging(x, y);
			if (isDragging) {
				moveEdit.storeOldPositions(getSelectedEdges());
				return;
			}
			focus = findClosestVertexToPosition(x, y);
			if (focus == null) {
				selectedEdge = selectEdge(x, y);
			}
			// Nothing selected and clear all selections.
			if (focus == null && selectedEdge == null) {
				if (selected.size() > 0 && !e.isShiftDown()) {
					for (Iterator it = selected.iterator(); it.hasNext();) {
						Object obj = it.next();
						ReactionEdge edge = (ReactionEdge)instance2ReactionEdgeMap.get(obj);
						if (edge != null)
							edge.setColor(null);
						it.remove();
					}
					repaint(getVisibleRect());
				}
				selectRect.x = x;
				selectRect.y = y;
				isDragging = false;
				return;
			}
			if (focus != null) {
				focus.inFocus = true;
				Vertex v = null;
				if (focus.reactionEdge.targetVertex == focus)
					v = focus.reactionEdge.sourceVertex;
				else
					v = focus.reactionEdge.targetVertex;
				focusLength = Point2D.distance(v.x, v.y, focus.x, focus.y);
				if (focus.reactionEdge.isLoaded()) {
					focus.x = x;
					focus.y = y;
					if (e.isControlDown()) {
						focus.isFixed = !focus.isFixed;
					}
					//repaint();
				}
			}
			else 
				isDragging = true;
			if (! e.isShiftDown()) {
				selected.clear();
			}
			GKInstance r = null;
			if (focus != null)
				r = focus.reactionEdge.getReactionInstance();
			else
				r = selectedEdge.getReactionInstance();
			if (selected.contains(r)) {
				selected.remove(r);
			} else {
				selected.add(r);
			}
			colorSelected();
			moveEdit.storeOldPositions(getSelectedEdges());
			repaint(getVisibleRect());
			//System.out.println(selected + "\n");
			//highliteInHierarchy(focus.reactionEdge.getReactionInstance());
			if (synchronizingEdgeSelection) {
				// Block the selection listener
				hierarchyPanel.removeSelectionListener(treeSelectionListener);
				hierarchyPanel.highliteNodes(selected);
				hierarchyPanel.addSelectionListener(treeSelectionListener);
			}
			firePropertyChange("selection", null, selected);
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
				toolActions.doPopup(e);
				return;
			}
			if (isDragging) {
				isDragging = false;
				// Make sure no edges less than 0
				java.util.List edges = getSelectedEdges();
				if(validateEdgeCoordinates(edges))
					repaint(getVisibleRect());
				moveEdit.storeNewPositions(edges);
				toolActions.initUndoActions(true);
			}
			if (focus != null) {
				focus.inFocus = false;
				moveEdit.storeNewPositions(getSelectedEdges());
				toolActions.initUndoActions(true);
//				if (focus.reactionEdge.isLoaded) {
//					focus.x = screen2ModelCoordinate(e.getX());
//					focus.y = screen2ModelCoordinate(e.getY());
//				}
//				focus = null;
			}
//			repaint();
//			e.consume();
			if (!selectRect.isEmpty()) {// Empty
				int dirtyX = (int)((selectRect.x - 10) * MAGNIFICATION);
				int dirtyY = (int)((selectRect.y - 10) * MAGNIFICATION);
				int dirtyW = (int)((selectRect.width + 20) * MAGNIFICATION);
				int dirtyH = (int)((selectRect.height + 20) * MAGNIFICATION);
				selectRect.width = selectRect.height = 0;
				repaint(dirtyX, dirtyY, dirtyW, dirtyH);
				// Create a sub view
			}
			if (isViewBoxDragged) {
				isViewBoxDragged = false;
				repaint(getVisibleRect());
			}
		}

		public void mouseEntered(MouseEvent arg0) {
			ignoreTreeSelectionEvents = true;
		}

		public void mouseExited(MouseEvent arg0) {
			ignoreTreeSelectionEvents = false;
		}
		
		/**
		 * @param edges
		 * @return true if any coordinates changed.
		 */
		private boolean validateEdgeCoordinates(java.util.List edges) {
			AbstractEdge edge = null;
			int buffer = 0; // A little extra space
			boolean isChanged = false;
			for (Iterator it = edges.iterator(); it.hasNext();) {
				edge = (AbstractEdge) it.next();
				if (edge.getTailX() < buffer) {
					edge.setTail(5, edge.getTailY());
					isChanged = true;
				}
				if (edge.getTailY() < buffer) {
					edge.setTail(edge.getTailX(), buffer);
					isChanged = true;
				}
				if (edge.getHeadX() < buffer) {
					edge.setHead(buffer, edge.getHeadY());
					isChanged = true;
				}
				if (edge.getHeadY() < buffer) {
					edge.setHead(edge.getHeadX(), buffer);
					isChanged = true;
				}
			}
			return true;
		}
	}
    
    public Vertex findClosestVertexToPosition(int x, int y) {
    	Vertex closest = null;
		//double closestDist = Double.MAX_VALUE;
		int sensingDis = 36;
		int distSqr = 0;
		for(Iterator vi = verteces.iterator();vi.hasNext();) {
			Vertex v = (Vertex) vi.next();
			distSqr = (v.x - x) * (v.x - x) + (v.y - y) * (v.y - y);
			//double dist = (v.x - x) * (v.x - x) + (v.y - y) * (v.y - y);
//			if (dist < closestDist) {
//				closestDist = dist;
//				closest = v;
//			}
			if (distSqr < sensingDis) {
				closest = v;
				break;
			}
		}
    	return closest;
    }
    
    /**
     * Select a displayed ReactionEdge based on the specified coordinates.
     * @param x
     * @param y
     * @return
     */
    public ReactionEdge selectEdge(int x, int y) {
    	if (edges.size() > 0) {
    		for (Iterator it = edges.iterator(); it.hasNext();) {
    			Object obj = it.next();
    			if (obj instanceof ReactionEdge) {
    				ReactionEdge edge = (ReactionEdge) obj;
    				if (edge.isPicked(x, y))
    					return edge;
    			}
    		}
    	}
    	return null;
    }
    
    /**
     * Use a Rectangle to select ReactionEdges.
     * @param rect
     */
    public void selectEdges(Rectangle rect) {
    	selected.clear();
    	for (Iterator it = edges.iterator(); it.hasNext();) {
    		Object obj = it.next();
    		if (obj instanceof ReactionEdge) {
    			ReactionEdge edge = (ReactionEdge) obj;
    			if (rect.contains(edge.sourceVertex.x, edge.sourceVertex.y) &&
    			    rect.contains(edge.targetVertex.x, edge.targetVertex.y))
    			    selected.add(edge.getReactionInstance());
    		}
    	}
    }
    
    public void selectAll() {
    	selected.clear();
    	for (Iterator it = edges.iterator(); it.hasNext();) {
    		Object obj = it.next();
    		if (obj instanceof ReactionEdge) {
    			ReactionEdge edge = (ReactionEdge) obj;
    			selected.add(edge.getReactionInstance());
    		}
    	}
    	colorSelected();
    	repaint();
    }
    
    public String getToolTipText(MouseEvent e) {
    	int x = screen2ModelCoordinate(e.getX());
    	int y = screen2ModelCoordinate(e.getY());
    	ReactionEdge edge = selectEdge(x, y);
		if (edge != null) {
			//return edge.locatedEventInstance.getDBID() + "";
            GKInstance instance = edge.getReactionInstance();
			return instance.getDBID() + ": " + instance.getDisplayName();
		}
    	return null;
    }
    
	public Vertex findAndColorClosestVertexToPosition(int x, int y, Color c) {
		Vertex closest = null;
		double closestDist = Double.MAX_VALUE;
		for(Iterator vi = verteces.iterator();vi.hasNext();) {
			Vertex v = (Vertex) vi.next();
			v.reactionEdge.setColor(DEFAULT_REACTION_COLOR);
			double dist = (v.x - x) * (v.x - x) + (v.y - y) * (v.y - y);
			if (dist < closestDist) {
				closestDist = dist;
				closest = v;
			}
		}
		closest.reactionEdge.setColor(c);
		return closest;
	}

	public void translateEdges(Collection edges, int x, int y) {
		for (Iterator ei = edges.iterator(); ei.hasNext();) {
			AbstractEdge e = (AbstractEdge) ei.next();
			if ((e instanceof ReactionEdge) && ! (((ReactionEdge) e).isLoaded)) 
				continue;
			e.translate(x, y);
		}
	}

   	public synchronized void relax() {
		for (Iterator ei = loadedEdges.iterator(); ei.hasNext();) {
			AbstractEdge e = (AbstractEdge) ei.next();
			if (e.isFixed()) continue;
			Dimension dimension = e.getDimension();
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

		int max_x = 0;
		for (Iterator vi1 = loadedVerteces.iterator(); vi1.hasNext();) {
			Vertex n1 = (Vertex) vi1.next();
			if (n1.isFixed) continue;
			max_x = Math.max(max_x, n1.x);
			//System.out.println(Thread.currentThread().getName() + "\t" + n1.toString() + "\t" + n1.getBounds());
			double dx = 0;
			double dy = 0;

			for (Iterator vi2 = loadedVerteces.iterator(); vi2.hasNext();) {
				Vertex n2 = (Vertex) vi2.next();
				if (n1 == n2) continue;
				if (n2.isFixed) continue;
				Dimension d = n1.getDistance(n2);
				double vx = d.getWidth();
				double vy = d.getHeight();
				double len = vx * vx + vy * vy;
				if (len == 0) {
					dx += Math.random() - 0.5;
					dy += Math.random() - 0.5;
				} else if (len < 100 * 100) {
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

		if (dimension.width < max_x) {
			dimension.width = max_x + 1;
			dimension.height = dimension.width / WIDTH_HEIGHT_RATIO;
			//setPreferredSize(dimension);
			viewport.setViewPosition(viewport.getViewPosition());
			revalidate();
			showStatus();
		}

		for (Iterator vi = loadedVerteces.iterator(); vi.hasNext();) {	
			Vertex n1 = (Vertex) vi.next();
			if (!(n1.inFocus || n1.isFixed)) {
				n1.translate((int) Math.max(-2, Math.min(2, n1.dx)), (int) Math.max(-2, Math.min(2, n1.dy)));
			}
			n1.dx /= 2;
			n1.dy /= 2;
//			for (int j = i + 1; j < verteces.size(); j++) {
//				Vertex n2 = (Vertex) verteces.get(j);
//				if (n1.reactionEdge == n2.reactionEdge) {
//					continue;
//				}
//				Dimension distance = n1.getDistance(n2);
//				if (Math.sqrt(distance.width * distance.width + distance.height * distance.height) < MIN_VERTEX_DISTANCE) {
//					if (n1.x > n2.x) {
//						n1.translate(-distance.width/2, 0);
//						n2.translate(distance.width/2, 0);
//					} else {
//						n1.translate(distance.width/2, 0);
//						n2.translate(-distance.width/2, 0);
//					}
//					if (n1.y  n2.y) {
//						n1.translate(0, -distance.height/2);
//						n2.translate(0, distance.height/2);
//					} else {
//						n1.translate(0, distance.height/2);
//						n2.translate(0, -distance.height/2);
//					}
//				}
//			}
		}
		if (!relaxWithoutRepaint) {
			repaint();
		}
		isDirty = true;
	}
	
	private void showStatus() {
		statusLabel.setText("Viewbox: " + dimension.width + " : " + dimension.height);
	}
    
	public void viewStuff() {
		JFrame frame = new JFrame();
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(this, BorderLayout.CENTER);
		//JScrollPane scrollPane = new JScrollPane(this);
		//scrollPane.setPreferredSize(new Dimension(600,600));
		//panel.add(scrollPane, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(600,400));
		frame.getContentPane().add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	
	public void zoom(double zoomFactor) {
	    if (Math.abs(zoomFactor - MAGNIFICATION) < 0.001)
	        return;
	    MAGNIFICATION = zoomFactor;
	    revalidate();
	    repaint(getVisibleRect());
	}
	
	public Dimension getPreferredSize() {
		Dimension size = new Dimension(dimension);
		// Have to make sure all edges are in the view
		AbstractEdge edge = null;
		int maxX = 0;
		int maxY = 0;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (AbstractEdge) it.next();
			if (edge.getTailX() > maxX)
				maxX = edge.getTailX();
			if (edge.getHeadX() > maxX)
				maxX = edge.getHeadX();
			if (edge.getTailY() > maxY)
				maxY = edge.getTailY();
			if (edge.getHeadY() > maxY)
				maxY = edge.getHeadY();
		}
		// Need to use the maxX or maxY
		if (maxX > size.width || maxY > size.height) {
			if (maxY * WIDTH_HEIGHT_RATIO > maxX) { // Use maxY as the reference 
				size.height = maxY;
				size.width = maxY * WIDTH_HEIGHT_RATIO;
			}
			else { // Use maxX as the reference
				size.width = maxX;
				size.height = (int) (maxX * 1.0 / WIDTH_HEIGHT_RATIO);
			}
		}
		// A little buffer
		size.width += RESIZE_WIDGET_WIDTH;
		size.height += RESIZE_WIDGET_WIDTH;
		size.width *= MAGNIFICATION;
		size.height *= MAGNIFICATION;
		return size;
	}

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.scale(MAGNIFICATION,MAGNIFICATION);
		g2.setColor(BACKGROUND_COLOR);
		g2.fill3DRect(1, 1, dimension.width, dimension.height, true);
		if (isViewBoxDragged) {
			g2.setPaint(SELECTION_COLOR);
			g2.drawRect(0, 0, dimension.width, dimension.height);
			g2.fillRect(dimension.width - RESIZE_WIDGET_WIDTH / 2,
			            dimension.height - RESIZE_WIDGET_WIDTH / 2,
			            RESIZE_WIDGET_WIDTH,
			            RESIZE_WIDGET_WIDTH);
		}
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color color;
		for (Iterator ei = edges.iterator(); ei.hasNext();) {
			AbstractEdge e = (AbstractEdge) ei.next();
			if (e instanceof ReactionEdge) {
				ReactionEdge re = (ReactionEdge) e;
				if (re.isLoaded()) {
					if ((color = re.getColor()) != null) {
						g2.setPaint(color);
					} else {
						g2.setPaint(DEFAULT_LOADED_REACTION_COLOR);
					}
				} else {
					if ((color = re.getColor()) != null) {
						g2.setPaint(color);
					} else {
						g2.setPaint(DEFAULT_REACTION_COLOR);
					}
				}
				g2.setStroke(REACTION_STROKE);
				g2.draw(reactionShape(e));
			} else {
				g2.setPaint(CONNECTION_COLOR);
				g2.setStroke(CONNECTION_STROKE);
				g2.draw(connectionShape(e));
			}
		}
		// Draw selection rectangle
		if (!selectRect.isEmpty()) {
			g2.setPaint(Color.BLUE);
			g2.setStroke(SELECT_RECTANGLE_STROKE);
			g2.draw(selectRect);
		}
		// For overall view
		if (overallPane.isVisible())
			overallPane.repaint();
	}

	public void drawAura (Graphics2D g2, ReactionEdge re) {
		g2.setPaint(DEFAULT_AURA_COLOR);
		Vertex v = re.getSourceVertex();
		int l = (int) re.getPreferredLength();
//		g2.fillOval(v.x - 4 * l )
	}

	public Point getPerimeterPoint(Rectangle bounds, Point source, Point p) {
		int x = bounds.x;
		int y = bounds.y;
		int width = bounds.width;
		int height = bounds.height;
		int xCenter = (int) (x + width / 2);
		int yCenter = (int) (y + height / 2);
		int dx = p.x - xCenter; // Compute Angle
		int dy = p.y - yCenter;
		double alpha = Math.atan2(dy, dx);
		int xout = 0, yout = 0;
		double pi = Math.PI;
		double pi2 = Math.PI / 2.0;
		double beta = pi2 - alpha;
		double t = Math.atan2(height, width);
		if (alpha < -pi + t || alpha > pi - t) { // Left edge
			xout = x;
			yout = yCenter - (int) (width * Math.tan(alpha) / 2);
		} else if (alpha < -t) { // Top Edge
			yout = y;
			xout = xCenter - (int) (height * Math.tan(beta) / 2);
		} else if (alpha < t) { // Right Edge
			xout = x + width;
			yout = yCenter + (int) (width * Math.tan(alpha) / 2);
		} else { // Bottom Edge
			yout = y + height;
			xout = xCenter + (int) (height * Math.tan(beta) / 2);
		}
		return new Point(xout, yout);
	}

	public Point getPerimeterPoint(Point center, Point out, int radius) {
		int dx = out.x - center.x;
		int dy = out.x - center.y;
		double len = Math.sqrt(dx * dx + dy * dy);
		double ratio = radius / len;
		int x = (int) (center.x + dx * ratio);
		int y = (int) (center.y + dy * ratio);
		return new Point(x, y);
	}

	protected Shape createLineEnd(int size, int style, Point src, Point dst) {
		int d = (int) Math.max(1, dst.distance(src));
		int ax = - (size * (dst.x - src.x) / d);
		int ay = - (size * (dst.y - src.y) / d);
		GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
		path.moveTo(dst.x + ax + ay / 2, dst.y + ay - ax / 2);
		path.lineTo(dst.x, dst.y);
		path.lineTo(dst.x + ax - ay / 2, dst.y + ay + ax / 2);
		return path;
	}

	protected Shape edgeShape(Point start, Point end) {
		int radius = 10;
		int size = 10;
		Point realStart = getPerimeterPoint(start, end, radius);
		Point realEnd = getPerimeterPoint(end, start, radius);
		int d = (int) Math.max(1, realEnd.distance(realStart));
		int ax = - (size * (realEnd.x - realStart.x) / d);
		int ay = - (size * (realEnd.y - realStart.y) / d);
		GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
		path.moveTo(realEnd.x + ax + ay / 2, realEnd.y + ay - ax / 2);
		path.lineTo(realEnd.x, realEnd.y);
		path.lineTo(realEnd.x + ax - ay / 2, realEnd.y + ay + ax / 2);
		path.moveTo(realEnd.x, realEnd.y);
		path.lineTo(realStart.x, realStart.y);
		return path;
	}
	
	protected Shape reactionShape(AbstractEdge edge) {
		int sx = edge.getSourceVertex().x;
		int sy = edge.getSourceVertex().y;
		int tx = edge.getTargetVertex().x;
		int ty = edge.getTargetVertex().y;
		int dx = tx - sx;
		int dy = ty - sy;
		int d = (int) Math.max(1, Math.sqrt(dx * dx + dy * dy));
		int ax = - (ARROW_SIZE * dx / d);
		int ay = - (ARROW_SIZE * dy / d);
		GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
		path.moveTo(tx + ax + ay / 2, ty + ay - ax / 2);
		path.lineTo(tx, ty);
		path.lineTo(tx + ax - ay / 2, ty + ay + ax / 2);
		path.moveTo(tx, ty);
		path.lineTo(sx, sy);
		return path;
	}

	protected int model2ScreenCoordinate(int i) {
		return (int) (i * MAGNIFICATION);
	}

	protected int screen2ModelCoordinate(int i) {
		return (int) (i / MAGNIFICATION);
	}

	protected Shape connectionShape(AbstractEdge edge) {
		GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, 4);
		path.moveTo(edge.getTargetVertex().x, edge.getTargetVertex().y);
		path.lineTo(edge.getSourceVertex().x, edge.getSourceVertex().y);
		return path;
	}

	protected void releaseAll() {
		for (Iterator i = instance2ReactionEdgeMap.values().iterator(); i.hasNext();) {
			ReactionEdge e = (ReactionEdge) i.next();
			e.setFixed(false);
		}		
	}

	protected void fixAll() {
		for (Iterator i = instance2ReactionEdgeMap.values().iterator(); i.hasNext();) {
			ReactionEdge e = (ReactionEdge) i.next();
			e.setFixed(true);
		}		
	}

	protected void releaseSelected() {
		for (Iterator si = selected.iterator(); si.hasNext();) {
			GKInstance i = (GKInstance) si.next();
			ReactionEdge re = (ReactionEdge) instance2ReactionEdgeMap.get(i);
			if (re != null) {
				re.setFixed(false);
			}
		}		
	}

	protected void loadSelected() {
		Collection events = selected;
		if (debug) System.out.println("actionPerformed()\n" + events);
		if (events != null) {
			Collection edges = null;
			try {
				edges = insertAndConnectReactions(events);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			edges2Loaded(edges);
			colorSelected();
			toolActions.initUndoActions(false);
			repaint();
		}		
	}

	protected void loadSelectedAndConnected() {
		Collection events = selected;
		if (debug) System.out.println("actionPerformed()\n" + events);
		if (events != null) {
			Collection edges = null;
			try {
				addPrecedingAndFollowingReactions(events);
				edges = insertAndConnectReactions(events);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			edges2Loaded(edges);
			colorSelected();
			toolActions.initUndoActions(false);
			repaint();
		}		
	}

	protected void unloadReactions() {
		for (Iterator lei = loadedEdges.iterator(); lei.hasNext();) {
			Object o = lei.next();
			if (o instanceof ReactionEdge) {
				ReactionEdge re = (ReactionEdge) o;
				if (re.getCoordinatesInstance().getDBID() == null) {
					edges.remove(o);
				}
				re.disconnect();
				re.setLoaded(false);
			} else {
				edges.remove(o);
			}
		}
		loadedEdges.clear();
		loadedVerteces.clear();
		repaint(getVisibleRect());
	}

	protected void delete() {
		Collection events = selected;
		if (events != null) {
		    Map deletedNodes = new HashMap();
			for (Iterator ei = events.iterator(); ei.hasNext();) {
				GKInstance e = (GKInstance) ei.next();
				ReactionEdge re = (ReactionEdge) instance2ReactionEdgeMap.get(e);
				if ((re != null) && (re.isLoaded())) {
					re.disconnect();
					edges.remove(re);
					deletedNodes.put(re.getSourceVertex(), null);
					deletedNodes.put(re.getTargetVertex(), null);
					loadedEdges.remove(re);
					instance2ReactionEdgeMap.remove(e);
					GKInstance le = re.getCoordinatesInstance();
					if (le.getDBID() != null) {
						try {getDba().deleteInstance(le);} catch (Exception e1) {e1.printStackTrace();}
					}
				}
			}
			// Need to clean up ConnectionEdge
			ConnectionEdge ce = null;
			for (Iterator it = edges.iterator(); it.hasNext();) {
			    Object edge = it.next();
			    if (edge instanceof ConnectionEdge) {
			        ce  = (ConnectionEdge) edge;
			        if (deletedNodes.containsKey(ce.getSourceVertex()) ||
			            deletedNodes.containsKey(ce.getTargetVertex()))
			            it.remove();
			    }
			}
			repaint(getVisibleRect());
		}		
	}

	protected void fixedSelected() {
		for (Iterator si = selected.iterator(); si.hasNext();) {
			GKInstance i = (GKInstance) si.next();
			ReactionEdge re = (ReactionEdge) instance2ReactionEdgeMap.get(i);
			if (re != null) {
				re.setFixed(true);
			}
		}		
	}
	
	public void exit() {
		if (isDirty) {
			int reply = JOptionPane.showConfirmDialog(this,
			                                          "Do you want to store locations?",
			                                          "Store Locations?",
			                                          JOptionPane.YES_NO_CANCEL_OPTION);
			if (reply == JOptionPane.CANCEL_OPTION)
				return;
			if (reply == JOptionPane.YES_OPTION) {
				storeLocations();
			}
		}
		System.exit(0);
	}
	
	protected void storeLocations() {
        if (!isWritable) {
            JOptionPane.showMessageDialog(this,
                                          "Your write permission is not enabled. You cannot store coordinates to the database.",
                                          "Error in Saving", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            List toBeCommitted = new ArrayList();
            for (Iterator ei = loadedEdges.iterator(); ei.hasNext();) {
                Object o = ei.next();
                if (!(o instanceof ReactionEdge))
                    continue;
                ReactionEdge reactionEdge = (ReactionEdge) o;
                GKInstance location = reactionEdge.getCoordinatesInstance();
                location.setAttributeValue("sourceX", new Integer(reactionEdge
                                                .getSourceVertex().x));
                location.setAttributeValue("sourceY", new Integer(reactionEdge
                                                .getSourceVertex().y));
                location.setAttributeValue("targetX", new Integer(reactionEdge
                                                .getTargetVertex().x));
                location.setAttributeValue("targetY", new Integer(reactionEdge
                                                .getTargetVertex().y));
                //				if (locationContext != null) {
                //					location.setAttributeValue("eventLocationContext",
                // locationContext);
                //				} else if (location.getAttributeValue("eventLocationContext") == null) {
                //					throw(new Exception("Need to set locationContext"));
                //				}						
                //System.err.println(location.toStanza());
                toBeCommitted.add(location);
            }
            if (!toBeCommitted.isEmpty()) {
                if (debug)
                    System.out.println("actionPerformed()\n" + toBeCommitted + "\n");
                //dba.txStoreOrUpdate(toBeCommitted);
                dba.storeOrUpdate(toBeCommitted);
                if (debug)
                    System.out.println("Storing done!");
            }
            isDirty = false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

	protected JLabel createContextLabel() {
		contextLabel = new JLabel();
		contextLabel.setMinimumSize(new Dimension(600,20));
		return contextLabel;
	}

	public static void main(String[] args) {
	    Properties prop = new Properties();
	    try {
	        File file = GKApplicationUtilities.getPropertyFile("curator.prop");
	        FileInputStream fis = new FileInputStream(file);
	        prop.load(fis);
	        fis.close();
	    }
	    catch(IOException e) {
	        System.err.println("GKVisualizationPane.main(): " + e);
	        e.printStackTrace();
	    }
	    DBConnectionPane connectionPane = new DBConnectionPane();
	    connectionPane.setValues(prop);
	    if (!connectionPane.showInDialog(null)) {
	        System.exit(0);
	    }
	    try {
	        MySQLAdaptor dba =
	            new MySQLAdaptor(prop.getProperty("dbHost"),
	                             prop.getProperty("dbName"),
	                             prop.getProperty("dbUser"),
	                             prop.getProperty("dbPwd"),
	                             Integer.parseInt(prop.getProperty("dbPort")));
	        //dba.debug = true;
	        GKVisualizationPane app = new GKVisualizationPane(dba);
	        String writable = prop.getProperty(COOR_WRITE_ENABLE_KEY);
	        if (writable != null && writable.equalsIgnoreCase("true"))
	            app.setCoordinatesWritable(true);
	        else
	            app.setCoordinatesWritable(false);
	        JFrame frame = app.getFrame();
	        frame.addWindowListener(new WindowAdapter() {
	            public void windowClosing(WindowEvent e) {
	                System.exit(0);
	            }
	        });
            GKApplicationUtilities.center(frame);
	        frame.setVisible(true);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Thread me = Thread.currentThread();
		while ((me == relaxer) && relax) {
			//dimension = getSize();
			try {
				if (!relaxerSuspended)
					relax();
			} catch (Exception e) {

			}
			try {
				if (! relaxWithoutRepaint)
					Thread.sleep(10);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	
	class ActionCollection {
		private Action setContextAction;
		
		private GKVisualizationPane app;
		
		public ActionCollection(GKVisualizationPane app) {
			this.app = app;
		}
		
		public Action getSetContextAction() {
			if (setContextAction == null) {
				setContextAction = new AbstractAction("Set as context") {
					public void actionPerformed(ActionEvent e) {
						GKInstance instance = app.getHierarchyPanel().getSelectedInstance();
						app.setLocationContext(instance);
					}
				};
				setContextAction.putValue(Action.SHORT_DESCRIPTION, "Set selected pathway as layout context");
			}
			return setContextAction;
		}
	}

	/**
	 * @return
	 */
	public GKInstance getLocationContext() {
		return locationContext;
	}

	/**
	 * @param instance
	 */
	public void setLocationContext(GKInstance instance) {
		locationContext = instance;
		frame.setTitle("Context:" + instance.toString());
	}

	/**
	 * @return
	 */
	public PathwayBrowserPanel getHierarchyPanel() {
		return hierarchyPanel;
	}

	/**
	 * @return
	 */
	public MySQLAdaptor getDba() {
		return dba;
	}

	/**
	 * @param adaptor
	 */
	public void setDba(MySQLAdaptor adaptor) {
		dba = adaptor;
	}

	public void loadEventLocations() throws Exception {
		Collection elc = dba.fetchInstancesByClass(COORDINATE_CLASS_NAME);
		if (elc != null) {
//			for (Iterator elci = elc.iterator(); elci.hasNext();) {
//				GKInstance el = (GKInstance) elci.next();
//				ReactionEdge re = new ReactionEdge(el);
//				instance2ReactionEdgeMap.put(re.getReactionInstance(), re);
//			}
			loadEventLocations(elc);
		}
		newEdges2Edges();
		newVerteces2Verteces();
		newEdges.clear();
		newVerteces.clear();
	}
	
	private void loadEventLocations(Collection elc) throws Exception {
		// For speedy searching
		Map nodeMap = new HashMap();
		GKInstance location = null;
		for (Iterator it = elc.iterator(); it.hasNext();) {
			location = (GKInstance) it.next();
			nodeMap.put(location.getDBID(), location);
		}
		Connection conn = dba.getConnection();
		Statement stat = conn.createStatement();
		String query = "SELECT DB_ID, locatedEvent, sourceX, sourceY, targetX, targetY FROM " +				       COORDINATE_CLASS_NAME;
		ResultSet resultSet = stat.executeQuery(query);
		while (resultSet.next()) {
			long dbID = resultSet.getLong(1);
			long reactionID = resultSet.getLong(2);
			int sourceX = resultSet.getInt(3);
			int sourceY = resultSet.getInt(4);
			int targetX = resultSet.getInt(5);
			int targetY = resultSet.getInt(6);
			location = (GKInstance) nodeMap.get(new Long(dbID));
			if (location != null) {
				location.setAttributeValueNoCheck("sourceX", new Integer(sourceX));
				location.setAttributeValueNoCheck("sourceY", new Integer(sourceY));
				location.setAttributeValueNoCheck("targetX", new Integer(targetX));
				location.setAttributeValueNoCheck("targetY", new Integer(targetY));
				GKInstance reaction = dba.fetchInstance("Event", new Long(reactionID));
				if (reaction != null)
					location.setAttributeValueNoCheck("locatedEvent", reaction);
                else {
                    System.out.println("This is empty: " + location.getDBID() + " -> " + reactionID);
                }
				ReactionEdge re = new ReactionEdge(location);
				instance2ReactionEdgeMap.put(re.getReactionInstance(), re);
			}
		}
		resultSet.close();
		stat.close();
		// Make sure all loaded reactions are still valid
		validateLoadedReactions();
	}
  
  /**
   * Make sure all loaded Reactions are still in the database. The reason why it is needed
   * is that ReactionCoordinate instances are independent on Reactions. This is a limit in
   * both database schema and Reactome schema.
   *
   */
	private void validateLoadedReactions() {
	    Collection allReactions = null;
	    try {
	        allReactions = dba.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
	        GKInstance reaction = null;
	        List deletedRxn = new ArrayList();
	        for (Iterator it = instance2ReactionEdgeMap.keySet().iterator(); it.hasNext();) {
	            reaction = (GKInstance) it.next();
	            if (reaction == null)
	                continue;
	            if (!allReactions.contains(reaction))
	                deletedRxn.add(reaction);
	        }
	        if (deletedRxn.size() > 0) {
	            InstanceUtilities.sortInstances(deletedRxn);
	            StringBuffer buffer = new StringBuffer();
	            if (deletedRxn.size() == 1)
	                buffer.append("This reaction is deleted but still in the sky:\n");
	            else
	                buffer.append("These reactions are deleted but still in the sky:\n");
	            for (Iterator it = deletedRxn.iterator(); it.hasNext();) {
	                reaction = (GKInstance) it.next();
	                buffer.append(reaction.getDisplayName() + " (" + reaction.getDBID() + ")");
	                if (it.hasNext())
	                    buffer.append("\n");
	            }
	            JOptionPane.showMessageDialog(this,
	                                          buffer.toString(),
	                                          "Warning: Database Inconsistence",
	                                          JOptionPane.WARNING_MESSAGE);
	        }
	    }
	    catch(Exception e) {
	        System.err.println("GKVisualizationPane.validateLoadedReactions(): " + e);
	        e.printStackTrace();
	    }
	}

	/**
	 * @return
	 */
	public Schema getSchema() {
		return schema;
	}

	/**
	 * @param schema
	 */
	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	private void newEdges2Edges() {
		edges.addAll(newEdges);
	}

	private void newVerteces2Verteces() {
		verteces.addAll(newVerteces);
	}

	protected void edges2Loaded(Collection reactionEdges) {
		newEdges2Edges();
		newVerteces2Verteces();
		newEdges.clear();
		newVerteces.clear();
		loadedEdges.addAll(reactionEdges);
		edges.addAll(reactionEdges);
		for (Iterator rei = reactionEdges.iterator(); rei.hasNext();) {
			ReactionEdge re = (ReactionEdge) rei.next();
			re.setLoaded(true);
			Vertex sv = re.getSourceVertex();
			loadedVerteces.add(sv);
			loadedEdges.addAll(sv.getConnectionEdges().values());
			Vertex tv = re.getTargetVertex();
			loadedVerteces.add(tv);
			loadedEdges.addAll(tv.getConnectionEdges().values());
		}
	}

	public void handleTreeSelection(TreeSelectionEvent tse) {
		handleTreeSelection(tse, false);
	}
	
	/**
	 * Synchronized the tree selection with graph pane selection.
	 * @param tse
	 * @param needValidate true to make sure the correct reaction instances are selected.
	 */
	public void handleTreeSelection(TreeSelectionEvent tse, boolean needValidate) {
		if (ignoreTreeSelectionEvents) 
			return;
		TreeSelectionModel model = (TreeSelectionModel) tse.getSource();
		TreePath[] tps = model.getSelectionPaths();
		selected.clear();
		Set events = new HashSet();
		for (int i = 0; (tps != null) && i < tps.length; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) tps[i].getLastPathComponent();
			events.add(node.getUserObject());
			for (Enumeration ne = node.breadthFirstEnumeration(); ne.hasMoreElements();) {
				DefaultMutableTreeNode descendant = (DefaultMutableTreeNode) ne.nextElement();
				events.add(descendant.getUserObject());
			}
		}
		if (needValidate) {
			GKInstance instance1 = null;
			GKInstance instance2 = null;
			for (Iterator it = events.iterator(); it.hasNext();) {
				instance1 = (GKInstance) it.next();
				try {
					instance2 = dba.fetchInstance(instance1.getSchemClass().getName(), instance1.getDBID());
					if (instance2 != null)
						selected.add(instance2); 
				}
				catch(Exception e) {
					System.err.println("GKVisualizationPane.handleTreeSelection(): " + e);
					e.printStackTrace();
				}
			}
		}
		else {
			selected.addAll(events);
		}
		colorSelected();
		repaint();
	}
	
	public void clearSelection() {
		selected.clear();
		colorSelected();
		repaint();
	}
	
	public VisualizationToolActions getToolActions() {
		return toolActions;
	}
	
	public java.util.List getSelectedEdges() {
		java.util.List selectedEdges = new ArrayList();
		for (Iterator si = selected.iterator(); si.hasNext();) {
			Object r = si.next();
			Object re = instance2ReactionEdgeMap.get(r);
			if (re != null) {
				selectedEdges.add(re);
			}
		}
		return selectedEdges;
	}
  
  /**
   * Check if reaction coordinates can be written to the database.
   * @return
   */
  public boolean isCoordinatesWritable() {
    return this.isWritable;
  }
  
  /**
   * Set if reaction coordinates can be written to the database.
   * @param isWritable
   */
  public void setCoordinatesWritable(boolean isWritable) {
    this.isWritable = isWritable;
  }

}
