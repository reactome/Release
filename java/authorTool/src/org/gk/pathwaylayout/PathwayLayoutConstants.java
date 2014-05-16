/*
 * Created on Apr 4, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.gk.pathwaylayout;

import java.awt.Color;
import org.jgraph.graph.GraphConstants;

/**
 * @author vastrik
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PathwayLayoutConstants {
	
	public static final float EDGE_THICKNESS = 3.0f;
	
	public static final int INPUT_EDGE = 1;
	public static final Color INPUT_EDGE_COLOR = Color.black;
	public static final int INPUT_EDGE_END = GraphConstants.ARROW_NONE;
	public static final boolean INPUT_EDGE_END_FILL = false;
	
	public static final int OUTPUT_EDGE = 2;
	public static final Color OUTPUT_EDGE_COLOR = Color.black;
	public static final int OUTPUT_EDGE_END = GraphConstants.ARROW_SIMPLE;
	public static final boolean OUTPUT_EDGE_END_FILL = false;
	
	public static final int CATALYST_EDGE = 3;
	//public static final Color CATALYST_EDGE_COLOR = Color.red;
	public static final Color CATALYST_EDGE_COLOR = Color.black;
	public static final int CATALYST_EDGE_END = GraphConstants.ARROW_NONE;
	public static final boolean CATALYST_EDGE_END_FILL = false;
	public static final float[] CATALYST_EDGE_PATTERN = {2 * EDGE_THICKNESS, EDGE_THICKNESS};
	
	public static final int REQUIREMENT_EDGE = 4;
	//public static final Color REQUIREMENT_EDGE_COLOR = Color.orange;
	public static final Color REQUIREMENT_EDGE_COLOR = Color.black;
	public static final int REQUIREMENT_EDGE_END = GraphConstants.ARROW_CIRCLE;
	public static final boolean REQUIREMENT_EDGE_END_FILL = true;
	
	public static final int POSREGULATION_EDGE = 5;
	//public static final Color POSREGULATION_EDGE_COLOR = Color.orange;
	public static final Color POSREGULATION_EDGE_COLOR = Color.black;
	public static final int POSREGULATION_EDGE_END = GraphConstants.ARROW_CIRCLE;
	public static final boolean POSREGULATION_EDGE_END_FILL = false;
	
	public static final int NEGREGULATION_EDGE = 6;
	//public static final Color NEGREGULATION_EDGE_COLOR = Color.orange;
	public static final Color NEGREGULATION_EDGE_COLOR = Color.black;
	public static final int NEGREGULATION_EDGE_END = GraphConstants.ARROW_LINE;
	public static final boolean NEGREGULATION_EDGE_END_FILL = false;
	
	public static final float[] REGULATION_EDGE_PATTERN = {EDGE_THICKNESS, 2 * EDGE_THICKNESS};

	public static final int COORDINATE_SCALING_FACTOR = 15;	
	//public static final int REACTION_NODE_DIAMETER = COORDINATE_SCALING_FACTOR * 1;
	public static final int REACTION_NODE_DIAMETER = 10;
	public static final int DEFAULT_ENTITY_NODE_WIDTH = COORDINATE_SCALING_FACTOR * 4;
	public static final int DEFAULT_ENTITY_NODE_HEIGHT = COORDINATE_SCALING_FACTOR * 2;
	public static final int DEFAULT_EDGE_LENGTH = COORDINATE_SCALING_FACTOR * 2;
	public static final int NEIGHBOURHOOD_RADIUS = COORDINATE_SCALING_FACTOR * 20;
	
	public static final Color SIMPLEENTITY_COLOR = new Color(255,255,255);
	public static final Color COMPLEX_COLOR = new Color(255,130,71);
	public static final Color POLYMER_COLOR = new Color(255,130,71);
	public static final Color PROTEIN_COLOR = new Color(176,196,222);
	public static final Color SEQUENCE_COLOR = new Color(222,176,196);
	public static final Color OTHER_COLOR = new Color(200,200,200);
	public static final Color GEE_COLOR = new Color(200,100,100);
	public static final Color DEFINEDSET_COLOR = new Color(0,200,200);
	public static final Color OPENSET_COLOR = new Color(0,200,100);
	public static final Color CANDIDATESET_COLOR = new Color(0,100,200);
	
	public static final double ZOOM_STEP = 1.5;
	public static final double MAX_REACTION_DISTANCE_FOR_SHARED_ENTITIES = COORDINATE_SCALING_FACTOR * 100;
	
	//public static final double[] ZOOM_LEVELS = new double[]{0.8, 0.4, 0.2, 0.1, 0.05};
	public static final double[] ZOOM_LEVELS = new double[]{1.0, 0.5, 0.25, 0.125, 0.06125};
}
