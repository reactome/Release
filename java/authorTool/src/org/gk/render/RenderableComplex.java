/*
 * RenderableComplex.java
 *
 * Created on June 12, 2003, 2:41 PM
 */

package org.gk.render;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.util.GKApplicationUtilities;
/**
 * This class describes a renderable Complex.
 * @author  wgm
 */
public class RenderableComplex extends ContainerNode {
    // For stoichiometries
    private Map stoichiometries;
    // Cache the hierarchy of all contained components for performance reason
    private transient List<Renderable> componentsInHiearchy;
    // This is used to keep the old bounds so that they can be recovered for
    // components hiding.
    // Note: to make it a little simplier, the old bounds for this complex is saved
    // in this map too.
    private Map<Integer, Rectangle> oldIdToBounds;
    
    /** Creates a new instance of RenderableComplex */
    public RenderableComplex() {
        super();
        init();
    }
    
    public RenderableComplex(String displayName) {
        super(displayName);
        init();
    }
    
    private void init() {
        stoichiometries = new HashMap();
        components = new ArrayList();
        backgroundColor = new Color(204, 255, 255);
        isFeatureAddable = true;
        isStateAddable = true;
        isMultimerFormable = true;
        textPadding = 0;
    }
    
    public void addComponent(Renderable renderable) {
        // as of Jan 22, stoichiometry is disabled.
//      // Check stoichiometries first
//      Renderable target = getTarget(renderable);
//      // Have to make stoichiometries consistent
//      Integer value = (Integer) stoichiometries.get(target);
//      if (value == null)
//      stoichiometries.put(target, new Integer(1));
//      else 
//      stoichiometries.put(target, new Integer(value.intValue() + 1));
//      // Check if renderable has been added to components
//      for (Iterator it = components.iterator(); it.hasNext();) {
//      Renderable r = (Renderable) it.next();
//      if (r instanceof Shortcut)
//      r = ((Shortcut)r).getTarget();
//      if (r == target)
//      return;
//      }
        components.add(renderable);
        rebuildHierarchy();
        if (hideComponents) {
            renderable.setIsVisible(false);
            if (renderable instanceof ContainerNode)
                ((ContainerNode)renderable).hideComponents(true);
        }
    }
    
    /**
     * Call this method to rebuild the internal hiearchy structure. This
     * method should be called when the client has a doubt regarding the 
     * internal hierarchy structure of this complex.
     */
    public void rebuildHierarchy() {
        componentsInHiearchy = RenderUtility.getComponentsInHierarchy(this);
    }
    
    /**
     * Remove a list of Objects from the contained components.
     * @param deletion a list of Objects to be removed.
     * @return a list of objects that are removed actually.
     */
    public java.util.List removeAll(java.util.List deletion) {
        java.util.List list = new ArrayList();
        for (Iterator it = deletion.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            Object deleted = removeComponent(r);
            if (deleted != null)
                list.add(deleted);
        }
        return list;
    }
    
    public Renderable removeComponent(Renderable renderable) {
        Renderable target = getTarget(renderable);
        stoichiometries.remove(target);
        if (componentsInHiearchy != null)
            componentsInHiearchy.remove(renderable);
        if (oldIdToBounds != null)
            oldIdToBounds.remove(renderable.getID());
        return super.removeComponent(renderable);
    }
    
    public Renderable generateShortcut() {
        RenderableComplex shortcut = new RenderableComplex();
        shortcut.hideComponents = hideComponents;
        generateShortcut(shortcut);
        if (textBounds != null) {
            shortcut.setTextPosition(textBounds.x, 
                                     textBounds.y);
        }
        return shortcut;
    }
    
    public Set getSubunits() {
        Set subunits = new HashSet();
        getSubunits(this, subunits);
        return subunits;
    }
    
    private void getSubunits(Renderable node, Set subunits) {
        List comps = node.getComponents();
        if (comps == null || comps.size() == 0) {
            // Treat an empty complex as a subunit. This might not be good.
            if (node instanceof Shortcut)
                subunits.add(((Shortcut)node).getTarget());
            else
                subunits.add(node);
        }
        else {
            Node tmp = null;
            for (Iterator it = comps.iterator(); it.hasNext();) {
                tmp = (Node) it.next();
                getSubunits(tmp, subunits);
            }
        }
    }
    
    public boolean isPicked(Point p) {
        // reset
        selectionPosition = SelectionPosition.NONE;
        if (!isVisible || bounds == null)
            return false;
        // Check contained components first
        // Need to check contained components in an order
        if (componentsInHiearchy != null && componentsInHiearchy.size() > 0) {
            for (Renderable r : componentsInHiearchy) {
                if (r.isPicked(p))
                    return false;
            }
        }
        if (isResizeWidgetPicked(p))
            return true;
//      // Check if text label is picked
//      if (isTextPicked(p)) {
//      selectionPosition = SelectionPosition.TEXT;
//      return true;
//      }
        if (isNodeAttachmentPicked(p))
            return true;
        return bounds.contains(p);
    }
    
    /**
     * Search for a suitable container in this complex for a Node.
     */
    public RenderableComplex pickUpContainer(Node node) {
        if (componentsInHiearchy != null) {
            for (Renderable r : componentsInHiearchy) {
                if (!(r instanceof RenderableComplex))
                    continue;
                RenderableComplex complex = (RenderableComplex) r;
                if (complex.isAssignable(node))
                    return complex;
            }
        }
        return isAssignable(node) ? this : null;
    }
    
    /**
     * Get the stoichiometries.
     * @return key is Renderable object, while value is Integer for stoichiometry.
     */
    public Map getStoichiometries() {
        return stoichiometries;
    }
    
    /**
     * Set the stoichiometry for a specified Renderable object.
     * @param renderable the Renderable object.
     * @param value the new stoichiometry.
     */
    public void setStoichiometry(Renderable renderable, int value) {
        if (value <= 0)
            throw new IllegalArgumentException("RenderableComplex.setStoichiometry(): value should be greater than 0.");
        Integer oldValue = (Integer) stoichiometries.get(renderable);
        if (oldValue != null && oldValue.intValue() == value)
            return;
        Renderable target = getTarget(renderable);
        stoichiometries.put(target, new Integer(value));
        // Have to make sure renderable is in the components
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof Shortcut)
                r = ((Shortcut)r).getTarget();
            if (r == target)
                return;
        }
        addComponent(renderable);
    }
    
    private Renderable getTarget(Renderable r) {
        if (r instanceof Shortcut)
            return ((Shortcut)r).getTarget();
        return r;
    }
    
    /**
     * Override the super class method to handle components if they have been hidden.
     */
    @Override
    public void move(int dx, int dy) {
        super.move(dx, dy);
        if (hideComponents && isResizing()) {
            // Want to make all components to have the same sizing as the current components
            copyBoundsToComponents();
        }
    }
    
    @Override
    public void hideComponents(boolean hide) {
        this.hideComponents = hide;
        if (componentsInHiearchy == null)
            return;
        for (Renderable r : componentsInHiearchy) {
            Node node = (Node) r;
            node.setIsVisible(!hide);
            if (r instanceof RenderableComplex) {
                ((RenderableComplex)r).hideComponents = hide;
            }
        }
        // The following statements are related to bounds.
        if (bounds == null)
            return; 
        if (hide) {
            saveOldBounds();
            copyBoundsToComponents();
        }
        else {
            recoverOldBounds();
            // Make sure a complex container has enough size
            Renderable container = this.container;
            Rectangle childBounds = this.bounds;
            while (container instanceof RenderableComplex) {
                Rectangle cBounds = container.getBounds();
                if (!cBounds.contains(childBounds)) {
                    ((RenderableComplex)container).layout();
                }
                childBounds = container.getBounds();
                container = container.getContainer();
            }
        }
    }
    
    private void recoverOldBounds() {
        // Make sure it is recoverable. If not, just do a simple 
        // layout
        if (!isOldBoundsRecoverable()) {
            // Copy any known bounds
            if (oldIdToBounds != null) {
                for (Renderable r : componentsInHiearchy) {
                    Rectangle bounds = oldIdToBounds.get(r.getID());
                    if (bounds == null)
                        continue;
                    if (r.bounds != null) {
                        r.bounds.width = bounds.width;
                        r.bounds.height = bounds.height;
                    }
                    else
                        r.bounds = new Rectangle(bounds);
                }
            }
            // Just do an auto layout. Should start from the smalled complexes
            for (Renderable r : componentsInHiearchy) {
                if (r instanceof RenderableComplex)
                    ((RenderableComplex)r).layout();
            }
            layout();
            return;
        }
        Rectangle oldBounds = oldIdToBounds.get(getID());
        int dx = bounds.x - oldBounds.x;
        int dy = bounds.y - oldBounds.y;
        bounds.width = oldBounds.width;
        bounds.height = oldBounds.height;
        invalidateTextBounds();
        for (Renderable r : componentsInHiearchy) {
            oldBounds = oldIdToBounds.get(r.getID());
            oldBounds.translate(dx, dy);
            Rectangle newBounds = r.getBounds();
            if (newBounds == null) {
                newBounds = new Rectangle(oldBounds);
                r.setBounds(newBounds);
            }
            else {
                newBounds.x = oldBounds.x;
                newBounds.y = oldBounds.y;
                newBounds.width = oldBounds.width;
                newBounds.height = oldBounds.height;
            }
            ((Node)r).invalidateTextBounds();
        }
    }
    
    private boolean isOldBoundsRecoverable() {
        if (oldIdToBounds == null || oldIdToBounds.size() == 0)
            return false;
        // Make sure all nodes have been registered
        if (componentsInHiearchy == null ||
            componentsInHiearchy.size() == 0)
            return false;
        for (Renderable r : componentsInHiearchy) {
            if (!oldIdToBounds.containsKey(r.getID()))
                return false;
        }
        // Check if a complex component has the same size as its container.
        // This may occur when a hidecomponent complex forms a complex with
        // another Node.
        for (Renderable r : componentsInHiearchy) {
            if (!(r instanceof RenderableComplex))
                continue;
            RenderableComplex complex = (RenderableComplex) r;
            Rectangle complexBounds = oldIdToBounds.get(complex.getID());
            List<Renderable> list = RenderUtility.getComponentsInHierarchy(complex);
            for (Renderable tmp : list) {
                Rectangle tmpBounds = oldIdToBounds.get(tmp.getID());
                if (tmpBounds.width == complexBounds.width &&
                    tmpBounds.height == complexBounds.height)
                    return false;
            }
        }
        return true;
    }
    
    private void saveOldBounds() {
        if (oldIdToBounds == null)
            oldIdToBounds = new HashMap<Integer, Rectangle>();
        else
            oldIdToBounds.clear();
        // Save the bounds for this RenderableComplex. This bounds
        // will be used as the reference for future recovering
        oldIdToBounds.put(getID(), 
                          new Rectangle(bounds));
        for (Renderable r : componentsInHiearchy) {
            Rectangle rBounds = r.getBounds();
            if (rBounds != null) { // Just in case
                oldIdToBounds.put(r.getID(),
                                  new Rectangle(rBounds));
            }
        }
    }
    
    
    /**
     * Get the old bounds for hidden components.
     * @return
     */
    public Map<Integer, Rectangle> getOldBounds() {
        return this.oldIdToBounds;
    }
    
    /**
     * Set the old bounds for hidden components.
     * @param oldBounds
     */
    public void setOldBounds(Map<Integer, Rectangle> oldBounds) {
        this.oldIdToBounds = oldBounds;
    }

    /**
     * This method is used to copy the bounds of this complex to 
     * all it contained components.
     */
    public void copyBoundsToComponents() {
        if (componentsInHiearchy == null ||
            componentsInHiearchy.size() == 0)
            return;
        for (Renderable r : componentsInHiearchy) {
            if (r.bounds == null) {
                r.bounds = new Rectangle(bounds);
                if (r.getPosition() == null)
                    r.setPosition(new Point());
                ((Node)r).validatePositionFromBounds();
            }
            else {
                r.bounds.x = bounds.x;
                r.bounds.y = bounds.y;
                r.bounds.width = bounds.width;
                r.bounds.height = bounds.height;
            }
            ((Node)r).invalidateTextBounds();
        }
    }
    
    private boolean isResizing() {
        if (selectionPosition == SelectionPosition.NORTH_EAST ||
            selectionPosition == SelectionPosition.NORTH_WEST ||
            selectionPosition == SelectionPosition.SOUTH_EAST ||
            selectionPosition == SelectionPosition.SOUTH_WEST)
            return true;
        return false;
    }
    
    /**
     * Automatically layout this RenderableComplex.
     *
     */
    public void layout() {
        if (hideComponents) {
            return;  // Don't do layout for hiding components
        }
        List list = getComponents();
        if (list == null || list.size() == 0)
            return;
        int size = list.size();
        int c = (int) Math.ceil(Math.sqrt(size));
        Node rs[][] = new Node[c][c];
        int index = 0;
        // Distribute
        for (int i = 0; i < c && index < size; i++) { // Row
            for (int j = 0; j < c && index < size; j++) { // Col
                rs[i][j] = (Node) list.get(index);
                index ++;
            }
        }
        // Assign positions
        // Original position
        int x = position.x;
        int y = position.y;
        Dimension layerSize = new Dimension();
        boolean isDone = false;
        Node r = null;
        Rectangle bounds = null;
        int dx = 0;
        int x0 = 0;
        int y0 = 0;
        for (int i = 0; i < c && !isDone; i++) { // Row
            // Get the center for each layer
            layerSize.width = 0;
            layerSize.height = 0;
            for (int j = 0; j < c; j++) { // Col
                if (rs[i][j] == null) {
                    isDone = true;
                    break;
                }
                r = rs[i][j];
                if (r.getStoiBounds() != null) {
                    layerSize.width += r.getStoiBounds().width;
                }
                if (r.getBounds() != null) {
                    layerSize.width += r.getBounds().width;
                    if (r.getBounds().height > layerSize.height)
                        layerSize.height = r.getBounds().height;
                }
                else {
                    layerSize.width += Node.getNodeWidth();
                    layerSize.height += 20; // arbitrarily
                }
            }
            if (layerSize.width == 0) // nothing is layered.
                break;
            // Assign positions to this layer.
            x = -layerSize.width / 2 + position.x;
            y += layerSize.height / 2;
            for (int j = 0; j < c; j++) {
                if (rs[i][j] == null)
                    break;
                r = rs[i][j];
                // All are nodes
                dx = 0;
                if (r.getStoiBounds() != null)
                    dx = r.getStoiBounds().width;
                if (r.getBounds() != null)
                    dx += r.getBounds().width / 2;
                else
                    dx += Node.getNodeWidth() / 2;
                x += dx;
                x0 = r.position.x;
                y0 = r.position.y;
                // Need to call move to change positions of components in subcomplexes.
                r.move(x - x0,
                       y - y0);
                dx += 1; // A little cushion
                x += dx; // Move to the right end
            }
            y += layerSize.height / 2 + 2; // Make subunits look together
        }
        // Want to keep at the original position
        x0 = position.x;
        y0 = position.y;
        setBoundsFromComponents();
        dx = x0 - position.x;
        int dy = y0 - position.y;
        move(dx, dy);
        // Should not call the following method since it will invalidate the
        // layout results.
        invalidateTextBounds();
    }
    
    @Override
    public void setBoundsFromComponents() {
        super.setBoundsFromComponents();
        // Need to calculate the text layout position
        if (components != null && components.size() > 0 && textBounds != null) {
            // Create a union
            textBounds.y = bounds.y + bounds.height + 2;
            int x1 = Math.min(textBounds.x, bounds.x);
            int y1 = Math.min(textBounds.y, bounds.y);
            int x2 = Math.max(textBounds.x + textBounds.width,
                              bounds.x + bounds.width);
            int y2 = Math.max(textBounds.y + textBounds.height,
                              bounds.y + bounds.height);
            bounds.x = x1;
            bounds.y = y1;
            bounds.width = x2 - x1;
            bounds.height = y2 - y1;
            // Make sure text in the middle of the bottom
            textBounds.x = bounds.x + (bounds.width - textBounds.width) / 2;
            validatePositionFromBounds();
            needCheckBounds = false;
            needCheckTextBounds = false;
        }
    }
    
    /**
     * Get the stoichiometry for a specified Renderable object.
     * @param renderable the query object.
     * @return the stoichiometry.
     */
    public int getStoichiometry(Renderable renderable) {
        Integer value = (Integer) stoichiometries.get(getTarget(renderable));
        if (value == null)
            return 0;
        else
            return value.intValue();
    }
    
    public Renderable getComponentByName(String name) {
        Renderable renderable = null;
        for (Iterator it = components.iterator(); it.hasNext();) {
            renderable = (Renderable) it.next();
            if (renderable.getDisplayName().equals(name))
                return renderable;
        }
        return null;
    }
    
    public String getType() {
        return "Complex";
    }
    
    public List getLinkWidgetPositions() {
        // Just recalculate the positions
        // since it is not reset in validateBounds().
        resetLinkWidgetPositions();
        return super.getLinkWidgetPositions();
    }
    
    /**
     * Check if a passed Renderable object can be a Complex's component.
     * @param r
     * @return
     */
    @Override
    public boolean isAssignable(Renderable r) {
        if (bounds == null ||
            r == this) // Don't point it to itself
            return false; // This container has not be materialized
        if (r instanceof Node) {
            if (r instanceof RenderableCompartment ||
                    r instanceof RenderablePathway ||
                    r instanceof Note)
                return false;
            // Need to check based on bounds. Should have a full containing
            if (r.getBounds() == null)
                return bounds.contains(r.getPosition());
            else
                return bounds.contains(r.getBounds());
        }
        return false;
    }
    
    @Override
    protected void setTextPositionFromBounds() {
        if (bounds == null)
            return; // Wait for bounds is setting.
        if (textBounds == null) {
            textBounds = new Rectangle(bounds);
        }
        else {
            textBounds.x = bounds.x + (bounds.width - textBounds.width) / 2;
            textBounds.y = bounds.y + bounds.height - textBounds.height;
        }
    }
    
    /**
     * Break the links of this complex to other shortcuts.
     */
    public void delinkToShortcuts() {
        if (shortcuts == null)
            return;
        // An empty shortcuts list maybe used by other shortcuts.
        shortcuts.remove(this);
        if (shortcuts.size() == 1)
            shortcuts.clear(); // Don't need to track it.
        shortcuts = null;
        // Use the following funny way to do a deep clone of attributes.
        try {
            attributes = (Map) GKApplicationUtilities.cloneViaIO(attributes);
        }
        catch(Exception e) {
            System.err.println("RenderableComplex.delinkToShortcuts(): " + e);
            e.printStackTrace();
        }
    }

    @Override
    protected void initBounds(Graphics g) {
        super.initBounds(g);
        ensureTextInBounds(false);
        if (hideComponents)
            copyBoundsToComponents();
    }
    
}