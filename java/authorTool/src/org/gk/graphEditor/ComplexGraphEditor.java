/*
 * Created on Jul 2, 2003
 */
package org.gk.graphEditor;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Iterator;

import org.gk.render.DefaultRenderConstants;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RendererFactory;
import org.gk.render.Shortcut;

/**
 * This subclass of GraphEditorPane is used for drawing an complex and
 * editing its composition. Only RenderableEntities and RenderableComplexes
 * can be inserted to this editor.
 * @author wgm
 */
public class ComplexGraphEditor extends GraphEditorPane {
	// A flag
	private boolean isDissociated = true;
	// Bounds when assoicated
	private Rectangle complexBounds = null;
	// Complex is selected as a whole when in associated state
	private boolean isSelected;
	// For ComplexEditor specific action
	private SelectAction complexSelectAction;
	private DragAction complexDragAction;
	// For keeping purpose
	private SelectAction commonSelectAction;
	private DragAction commonDragAction;
	
	public ComplexGraphEditor() {
		complexBounds = new Rectangle();
		complexSelectAction = new ComplexEditorSelectAction(this);
		commonSelectAction = super.selectAction;
		complexDragAction = new ComplexEditorDragAction(this);
		commonDragAction = super.dragAction;
	}
	
	public ComplexGraphEditor(Renderable complex) {
		this();
		setRenderable(complex);
	}
	
	public void paint(Graphics g) {
		super.paint(g);
        if (displayedObject == null)
            return;
        Graphics2D g2 = (Graphics2D) g;
        java.util.List components = displayedObject.getComponents();
		if (components != null) {
			Rectangle clip = g2.getClipBounds();
			for (Iterator it = components.iterator(); it.hasNext();) {
				Renderable renderable = (Renderable) it.next();
                 int stoi = ((RenderableComplex)displayedObject).getStoichiometry(renderable);
                 if (isEditing && renderable == editingNode) {
					editor.render(g);
                     drawStoichiometry(stoi, editingNode, g2);
					continue;
				}
                renderable.render(g);
                if (renderable instanceof Node)
                    drawStoichiometry(stoi, (Node)renderable, g2);
			}
		}
		// Draw selected widget
		if (!isDissociated && isSelected) {
			drawSelectionWidgets(g2);
		}
		// Draw drag rectangle
		drawDragRect(g2);
	}
	
	protected void drawSelectionWidgets(Graphics2D g2) {
		int widgetWidth = DefaultRenderConstants.SELECTION_WIDGET_WIDTH;
		int offset = widgetWidth / 2;
		g2.setPaint(DefaultRenderConstants.SELECTION_WIDGET_COLOR);
		// north-west
		int x = complexBounds.x - offset;
		int y = complexBounds.y - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// north
		x = complexBounds.x + complexBounds.width / 2 - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// north-east
		x = complexBounds.x + complexBounds.width - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// east
		y = complexBounds.y + complexBounds.height / 2 - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// south-east
		y = complexBounds.y + complexBounds.height - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// south
		x = complexBounds.x + complexBounds.width / 2 - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// south-west
		x = complexBounds.x - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
		// west
		y = complexBounds.y + complexBounds.height / 2 - offset;
		g2.fillRect(x, y, widgetWidth, widgetWidth);
	}	
	
	public void insertNode(Node node) {
        super.insertNode(node);
		java.util.List components = displayedObject.getComponents();
		if (components == null || components.size() == 0) {
			// Set the position for node
			// Get the center of the visible rectangle
			Rectangle rect = getVisibleRect();
			int x = rect.x + rect.width / 2;
			int y = rect.y + rect.height / 2;
			node.setPosition(new Point(x, y));
			displayedObject.addComponent(node);
		}
		else { // Add as a neighbor to the rightmost node
			// Get the right most rectangle
			int rightX = 20, rightY = 20;
			Rectangle rect = null;
			Renderable tmpNode = null;
			for (Iterator it = components.iterator(); it.hasNext();) {
				tmpNode = (Renderable) it.next();
				rect = tmpNode.getBounds();
				if (rect != null && rect.getMaxX() > rightX) {
					rightX = (int) rect.getMaxX();
					rightY = tmpNode.getPosition().y;
				}
				else if (rect == null)
					continue;
			}
			node.setPosition(new Point(0, 0));
			node.validateBounds(getGraphics());
			rect = node.getBounds();
			node.setPosition(new Point(rightX + rect.getBounds().width / 2, rightY));
			displayedObject.addComponent(node);
		}
		if (node.getContainer() != displayedObject)
			node.setContainer(displayedObject);
		// Dissociate it
		dissociateComplex();
		Renderable parent = null;
		if (displayedObject instanceof Shortcut)
			parent = ((Shortcut)displayedObject).getTarget();
		else
			parent = displayedObject;
		firePropertyChange("insert", parent, node);
	}
	
	/**
	 * A simple implementation to form an aggregate.
	 */
	public void formComplex() {
		removeSelection();
		isDissociated = false;
		if (displayedObject.getComponents() == null)
			return;
		// Form a tight complex.
		// TO BE DONE SOON!
		// recalculate the complex bounds.
		Rectangle rect = null;
		Renderable renderable = (Renderable) displayedObject.getComponents().get(0);
		complexBounds = (Rectangle)renderable.getBounds().clone();
		int index = 0;
		for (Iterator it = displayedObject.getComponents().iterator(); it.hasNext();) {
			if (index++ == 0)
				continue;	
			renderable = (Renderable) it.next();
			complexBounds.add(renderable.getBounds());
		}
		selectAction = complexSelectAction;
		dragAction = complexDragAction;
	}
	
	public void dissociateComplex() {
		isDissociated = true;
		selectAction = commonSelectAction;
		dragAction = commonDragAction;
	}
	
	/**
	 * Query the status of dissociation of this ComplexEditor.
	 * @return
	 */
	public boolean isDissociated() {
		return this.isDissociated;
	}
	
	public Rectangle getComplexBounds() {
		return this.complexBounds;
	}
	
	public void setIsComplexSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
	
	public boolean isComplexSelected() {
		return this.isSelected;
	}
	
	public void moveComplex(int dx, int dy) {
		if (displayedObject.getComponents() == null)
			return;
		Renderable renderable = null;
		for (Iterator it = displayedObject.getComponents().iterator(); it.hasNext();) {
			renderable = (Renderable) it.next();
			renderable.move(dx, dy);
		}
		complexBounds.translate(dx, dy);
	}
	
	/**
	 * Override the super class method. When in association state, the whole complex should
	 * be selected.
	 */
	public void selectAll() {
		if (isDissociated)
			super.selectAll();
		else
			isSelected = true;
	}
	
	public java.util.List deleteSelection() {
		java.util.List rtn = super.deleteSelection();
		dissociateComplex();
		return rtn;
	}
	
	/**
	* Use this method to remove a Renderable from the complex. The removed
	* objects includes renderable's shortcuts.
	* @param renderable
	*/
	public void delete(Renderable renderable) {
		super.delete(renderable);
		dissociateComplex();		
	}
	
	public void setEditingNode(Node node) {
		super.setEditingNode(node);
		editor = RendererFactory.getFactory().getEditor(node);
		editor.setRenderable(node);
		editor.setCaretPosition(0);
		if (node != null) {
			editor.setSelectionStart(0);
			editor.setSelectionEnd(node.getDisplayName().length());
		}
	}
	
	public void layoutRenderable() {
	    RenderableComplex complex = (RenderableComplex) displayedObject;
        complex.layout();
		// Manually validate bounds to make preferredSize correct.
		Graphics g = getGraphics();
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
									   RenderingHints.VALUE_ANTIALIAS_ON);
		Renderable r;
        for (Iterator it = displayedObject.getComponents().iterator(); it.hasNext();) {
			r = (Renderable) it.next();
            if (r instanceof Node) {
                Node node = (Node) r;
                node.validateBounds(g);
            }
		}
		centerRenderable();
		revalidate();
		repaint(getVisibleRect());
	}
}
