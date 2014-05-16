/*
 * Created on Dec 7, 2006
 *
 */
package org.gk.graphEditor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.render.*;
import org.gk.util.DrawUtilities;

/**
 * This helper class is used to generate a popup list so that the user
 * can choose a semantic link type from a list. This list can be inhibit,
 * activate, etc for proteins, RNAs, etc; and input, output, catalyst, etc
 * for reactions. 
 * @author guanming
 *
 */
public class ConnectionPopupManager implements DefaultRenderConstants {
    // A list of roles
    protected List<String> roleList;
    // Some extra buffer
    private final int BUFFER = 4;
    // Need all information to generate correct role type
    private ConnectWidget connectWidget;
    private Renderable anchorObject;
    // To control if popup should appear
    private boolean needPopup;
    // Bounds for the popup
    private Rectangle bounds;
    // Extra space before the first text line
    private float padding;
    // TextLayout to be drawn
    private List textLayouts;
    // a flag for text layouts
    private boolean invalid;
    // Used to choose the type
    private int mouseY;
    // keep the selected Text Layout
    private int selectedTLIndex;
    // Entry point: used for Reaction popup
    private Point entryPoint;
    
    public ConnectionPopupManager() {
        needPopup = true;
        bounds = new Rectangle();
        textLayouts = new ArrayList();
        roleList = new ArrayList<String>();
    }
    
    public boolean isConnectPopupShown() {
        return (anchorObject != null && needPopup);
    }
    
    public void setConnectWdiget(ConnectWidget widget) {
        this.connectWidget = widget;
    }
    
    public void setAnchorObject(Renderable r) {
        this.anchorObject = r;
        invalid = true;
        resetRoleList();
    }
    
    private void resetRoleList() {
        roleList.clear();
        Renderable edge = connectWidget.getEdge();
        if (edge instanceof FlowLine) {
            resetFlowLineRoleList((FlowLine)edge);
        }
        else if (edge instanceof RenderableReaction) {
            resetReactionRoleList();
        }
    }

    private void resetReactionRoleList() {
        switch (connectWidget.getRole()) {
            case HyperEdge.INPUT :
                roleList.add("Input");
                break;
            case HyperEdge.OUTPUT :
                roleList.add("Output");
                break;
            case HyperEdge.CATALYST :
                roleList.add("Catalyst");
                break;
            case HyperEdge.ACTIVATOR :
                roleList.add("Activator");
                break;
            case HyperEdge.INHIBITOR :
                roleList.add("Inhibitor");
                break;
        }
    }

    /**
     * TODO: the following mapping should be externalized and control by
     * a configure file.
     * @param fl
     */
    private void resetFlowLineRoleList(FlowLine fl) {
        int connectRole = connectWidget.getRole();
        if (connectRole == HyperEdge.OUTPUT) {
            // input should not be null
            Node input = fl.getInputNode(0);
            if (input == null)
                throw new IllegalStateException("ConnectionPopupManager.resetFlowLineRoleList(): input is null");
            if (input instanceof ProcessNode) {
                if (anchorObject instanceof ProcessNode ||
                    anchorObject instanceof ReactionNode)
                    roleList.add("Precede");
                else if (anchorObject instanceof Node) {
                    roleList.add("Activate");
                    roleList.add("Inhibit");
                }
            }
            // To make these popup menu working for the curator tool
            else if (input instanceof ReactionNode) {
                if (anchorObject instanceof ReactionNode ||
                    anchorObject instanceof ProcessNode)
                    roleList.add("Precede");
            }
            // Gene can only Activate or Inhibit other objects
            else if (input instanceof RenderableGene) {
                // Make Gene to interact with Nodes only
                if (anchorObject instanceof Node) {
                    roleList.add("Activate");
                    roleList.add("Inhibit");
                    // Add an "Encode" from Gene to Protein
                    if (anchorObject instanceof RenderableProtein)
                        roleList.add("Encode");
                }
            }
            // Need to check the connection is for input or output
            else if (anchorObject instanceof RenderableGene ||
                     anchorObject instanceof ProcessNode) {
                roleList.add("Activate");
                roleList.add("Inhibit");
            }
            else if (anchorObject instanceof SourceOrSink ||
                     input instanceof SourceOrSink) {
                roleList.add("Produce");
            }
            else if (anchorObject instanceof Node) {
                setRolesForNodeToNode();
            }
            else if (anchorObject instanceof RenderableReaction) {
                roleList.add("Input");
                roleList.add("Output");
                roleList.add("Catalyze");
                roleList.add("Activate");
                roleList.add("Inhibit");
            }
        }
        else if (connectRole == HyperEdge.INPUT) {
            // This dependent on InteractionType, which is defined during output connecting.
            // This will occur after detaching.
            if (fl instanceof RenderableInteraction) {
                InteractionType type = ((RenderableInteraction)fl).getInteractionType();
                if (type == InteractionType.INHIBIT)
                    roleList.add("Inhibitor");
                else if (type == InteractionType.ACTIVATE)
                    roleList.add("Activator");
                else if (type == InteractionType.INTERACT)
                    roleList.add("Interactor");
            }
            else {
                roleList.add("Preceder");
            }
        }
    }

    protected void setRolesForNodeToNode() {
        roleList.add("Produce");
        roleList.add("Associate");
        roleList.add("Phosphorylate");
        roleList.add("Activate");
        roleList.add("Inhibit");
        roleList.add("Interact");
    }
    
    public void showPopup(boolean show) {
        this.needPopup = show;
        if (!show)
            mouseY = 0; // reset
    }
    
    public Renderable getAnchorObject() {
        return this.anchorObject;
    }
    
    public void setEntryPoint(Point p) {
        if (entryPoint == null)
            entryPoint = new Point();
        entryPoint.x = p.x;
        entryPoint.y = p.y;
    }
    
    public boolean isPopupPicked(Point p) {
        mouseY = p.y;
        return bounds.contains(p);
    }
    
    public String getSelectedText() {
        if (selectedTLIndex == -1)
            return null;
        if (roleList.size() == 0)
            return null;
        return (String) roleList.get(selectedTLIndex);
    }
    
    public void drawConnectionPopup(Graphics2D g2) {
        if (!needPopup ||
            (anchorObject == null) ||
            roleList.size() == 0)
            return;
        setTextLayouts(g2);
        Composite c = g2.getComposite();
        // Use a transparent background
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);
        g2.setComposite(ac);
        g2.setPaint(Color.LIGHT_GRAY);
        g2.fill3DRect(bounds.x,
                      bounds.y,
                      bounds.width,
                      bounds.height,
                      true);
        // Draw the bounds for the selected text
        drawSelectedTextLayout(g2);
        g2.setComposite(c);
        g2.setPaint(Color.BLUE);
        DrawUtilities.drawString(textLayouts,
                                 bounds,
                                 (int)padding,
                                 g2);
    }
    
    private void drawSelectedTextLayout(Graphics2D g2) {
        if (textLayouts.size() == 0)
            return ;
        TextLayout tl = null;
        float y = bounds.y + padding;
        float h = 0;
        selectedTLIndex = -1;
        int index = 0;
        for (Iterator it = textLayouts.iterator(); it.hasNext();) {
            tl = (TextLayout) it.next();
            h = tl.getAscent() + tl.getDescent() + tl.getLeading();
            if (y + h > mouseY) {
                g2.setPaint(HIGHLIGHTED_COLOR);
                // Add 1 to avoid round off
                g2.fill3DRect(bounds.x, 
                              (int) (y + 1), 
                              bounds.width, 
                              (int) (h + 1),
                              true);
                selectedTLIndex = index;
                break;
            }
            y += h;
            index ++;
        }
        // Special case if the bounds is too tall
        if (selectedTLIndex == -1) {
            selectedTLIndex = textLayouts.size() - 1;
            tl = (TextLayout) textLayouts.get(selectedTLIndex);
            y -= h;
            g2.setPaint(HIGHLIGHTED_COLOR);
            g2.fill3DRect(bounds.x, 
                          (int) (y + 1), 
                          bounds.width, 
                          (int) (h + 1),
                          true);
        }
    }
    
    private void setTextLayouts(Graphics2D g2) {
        if (!invalid) {
            return;
        }
        textLayouts.clear();
        // Use bold font
        Font oldFont = g2.getFont();
        // Make the font a little bigger so that it is a little easier to see
        Font font = oldFont.deriveFont(Font.BOLD, 
                                       oldFont.getSize2D() + 1.0f);
        FontRenderContext frc = g2.getFontRenderContext();
        for (int i = 0; i < roleList.size(); i++) {
            TextLayout tl = new TextLayout((String)roleList.get(i),
                                           font,
                                           frc);
            textLayouts.add(tl);
        }
        validateBounds();
    }
    
    private void validateBounds() {
        float totalHeight = 0;
        // get width and height from text layouts first
        float w = 0.0f, h = 0.0f;
        for (Iterator it = textLayouts.iterator(); it.hasNext();) {
            TextLayout layout = (TextLayout) it.next();
            if (w < layout.getAdvance())
                w = layout.getAdvance();
            h += (layout.getAscent() + layout.getDescent() + layout.getLeading());
        }
        totalHeight = h;
        w += (2 * BUFFER);
        h += (2 * BUFFER);
        if (anchorObject instanceof Node) {
            Rectangle rect = anchorObject.getBounds();
            bounds.x = rect.x;
            bounds.y = rect.y;
            if (rect.width + BUFFER > w)
                bounds.width = rect.width + BUFFER;
            else
                bounds.width = (int) w;
            if (rect.height + BUFFER > h)
                bounds.height = rect.height + BUFFER;
            else
                bounds.height = (int) h;
        }
        else if (anchorObject instanceof HyperEdge) {
            // Use the entry point to set the bounds
            bounds.x = entryPoint.x - BUFFER;
            bounds.y = entryPoint.y - BUFFER;
            bounds.width = (int) w;
            bounds.height = (int) h;
        }
        padding = (bounds.height - totalHeight) / 2.0f;
    }
    
}
