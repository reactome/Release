/*
 * DefaultRenderConstants.java
 *
 * Created on June 18, 2003, 10:24 AM
 */

package org.gk.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
/**
 * A list of constants that are used in rendering
 * @author  wgm
 */
public interface DefaultRenderConstants {
    // For drawing selection widgets
    public static final int SELECTION_WIDGET_WIDTH = 4;
    
    public static final int EDGE_SELECTION_WIDGET_WIDTH = 8;
    
    public static final int EDGE_TYPE_WIDGET_WIDTH = 12;
    
    public static final int EDGE_MODULATION_WIDGET_WIDTH = 8;
    
    public static final int EDGE_INHIBITION_WIDGET_WIDTH = 8;
    
    public static final Color SELECTION_WIDGET_COLOR = Color.blue;//UIManager.getColor("EditorPane.selectionBackground");
    
    public static final Color SELECTION_WIDGET_BG_COLOR = new Color(0, 0, 255, 50);
    
    public static final Color HIGHLIGHTED_COLOR = Color.green;
    
    public static final Color DEFAULT_BACKGROUND = new Color(204, 255, 204);
    
    public static final Color DEFAULT_DISEASE_BACKGROUND = Color.RED;
    
    public static final Color COMPARTMENT_COLOR = new Color(250, 240, 240);
    
    public static final Color COMPARTMENT_OUTLINE_COLOR = new Color(255, 153, 102);
    
    public static final Color PANEL_BACKGROUND = Color.white;
    
    public static final Color DEFAULT_FOREGROUND = Color.black;
    
    /**
     * Use a transparent color to draw the background of a container pathway
     */
    public static final Color DEFAULT_PATHWAY_SELECTION_BACKGROUND = new Color(204 / 255.0f, 
                                                                               204 / 255.0f, 
                                                                               255 / 255.0f, 
                                                                               0.5f);
    
    public static final Color DEFAULT_OUTLINE_COLOR = Color.BLACK;
    
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
    
    public static final Stroke DEFAULT_LINE_SELECTION_STROKE = new BasicStroke(1.5f);
    
    public static final Stroke DEFAULT_THICK_STROKE = new BasicStroke(2.0f);
    
    public static final int RECTANGLE_DIST = 10;
    
    public static final int MULTIMER_RECT_DIST = 3;
    
    public static final Stroke SELECTION_STROKE = new BasicStroke(2.0f);
    
    public static final Stroke BROKEN_LINE_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT,
                                                                    BasicStroke.JOIN_BEVEL, 0,
                                                                    new float[]{12, 12}, 0);
    
    public static final Stroke THICK_BROKEN_LINE_STROKE = new BasicStroke(2, 
                                                                          BasicStroke.CAP_BUTT,
                                                                          BasicStroke.JOIN_BEVEL, 
                                                                          0,
                                                                          new float[]{6, 6}, 
                                                                          0);
    
    public static final int LINK_WIDGET_WIDTH = 16;
    
    public static final Color LINK_WIDGET_COLOR = Color.LIGHT_GRAY;
    
    public static final int ROUND_RECT_ARC_WIDTH = 12;
    
    public static final int COMPLEX_RECT_ARC_WIDTH = ROUND_RECT_ARC_WIDTH / 2;
    
    public static final Stroke GENE_SYMBOL_STROKE = new BasicStroke(2.0f);
    
    public static final Font WIDGET_FONT = new Font("Monospaced", Font.BOLD, 10);
    
    public static final int DEFAULT_NODE_WIDTH = 130;
    
    public static final int DEFAULT_RED_CROSS_WIDTH = 3;
}
