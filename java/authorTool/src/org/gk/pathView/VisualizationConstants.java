/*
 * Created on Mar 14, 2004
 */
package org.gk.pathView;

import java.awt.BasicStroke;
import java.awt.Color;

/**
 * A collection of constants for the GK visualization tool.
 * @author wgm
 */
public interface VisualizationConstants {
	public static final Color SELECTION_COLOR = Color.blue;
	public static final Color SELECTED_REACTION_COLOR = Color.red;
	public static final int RESIZE_WIDGET_WIDTH = 8;
	public static BasicStroke REACTION_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	public static BasicStroke CONNECTION_STROKE = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	public static Color CONNECTION_COLOR = new Color(127,127,127,127);
	public static Color DEFAULT_LOADED_REACTION_COLOR = new Color(0,0,127,200);
	public static int ARROW_SIZE = 5;
	public static Color BACKGROUND_COLOR = Color.white;
}
