/*
 * GraphEditorPane.java
 *
 * Created on June 16, 2003, 10:13 PM
 */

package org.gk.graphEditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.undo.UndoableEdit;

import org.gk.property.PropertyManager;
import org.gk.render.*;
import org.gk.util.AutoCompletable;
import org.gk.util.AutoCompletionPane;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.SwingImageCreator;
import org.gk.util.ZoomableJPanel;

/**
 * This is a generic graph editor pane. Complex, reaction and pathway editor can extends this generic graph editor pane to provide 
 * more specific functions.
 * @author  wgm
 */
public class GraphEditorPane extends ZoomableJPanel implements MouseMotionListener, MouseListener, Selectable {
	// For link widgets
	public static final int LINK_WIDGET_NONE = -1;
	public static final int LINK_WIDGET_EAST = 0;
	public static final int LINK_WIDGET_NORTH = 1;
	public static final int LINK_WIDGET_SOUTH = 2;
	public static final int LINK_WIDGET_WEST = 3;
	//	Color mode
	protected Color background = Color.white;
    // These actions are used for common operations
    SetCursorAction setCursorAction;
    DragAction dragAction;
    SelectAction selectAction;
    protected ConnectAction connectAction;
    GraphEditorAction currentAction;
    protected EditAction editingAction;
    protected LinkWidgetAction linkWidgetAction;
    // To store selected objects
    protected GraphEditorSelectionModel selectionModel;
    // The rendered object
    protected Renderable displayedObject;
    protected Point defaultInsertPos;
    // A list of GraphEditorActionListeners for handling GraphEditorActionEvents.
    private java.util.List editorActionListeners;
    // For rectangle selection
    protected Rectangle dragRect;
    // For calculating preferred size
    private Rectangle preferredRect;
    // For editing
    protected boolean isEditing;
    protected Node editingNode;
    private String oldName; // For reverting back
    private KeyListener editingKeyAction;
    protected Editor editor;
    // To control if it is editable
    protected boolean isEditable = true;
    // Another flag to control this graph editor is used as a drawing tool only
    protected boolean usedAsDrawingTool = false;
    // For auto completion
    // Disabled for the time being as of build 27, version 3.0
    private AutoCompletionPane autoPane;

    // Cache Renderable object to improve performance
    private Renderable mouseOveredRenderable;
    // To support Undo/Redo
    protected GraphEditorUndoManager undoManager;
    // Key event for moving
    private KeyListener moveKeyAction;
    
    /** Creates a new instance of GraphEditorPane */
    public GraphEditorPane() {
        init();
    }
    
    private void init() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                Component focusOwner = e.getOppositeComponent();
                if (focusOwner == autoPane)
                    return;
                if (autoPane != null && focusOwner == autoPane.getList())
                    return;
                stopEditing();
            }
        });
        selectionModel = new GraphEditorSelectionModel();
        selectionModel.addGraphEditorActionListener(new GraphEditorActionListener() {
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() == GraphEditorActionEvent.SELECTION) {
                    hiliteForSelection(selectionModel.getSelection());
                }       
            }
        });
        // Set up actions
        selectAction = new SelectAction(this);
        dragAction = new DragAction(this);
        connectAction = new ConnectAction(this);
        editingAction = new EditAction(this);
        setCursorAction = new SetCursorAction(this);
        // Others
        defaultInsertPos = new Point(75, 75);
        // Have to explicitly set Font in order to control the size
        // the rectangle.
        //setFont(new Font("Dialog", Font.PLAIN, 12));
        // The following font should be available in all Java platform.
        // Use this font to avoid any text overflow
        Font font = new Font("Lucida Sans", Font.PLAIN, 12);
        setFont(font);
        // For dragging selection
        dragRect = new Rectangle();
        // For preferred size
        preferredRect = new Rectangle();
        // Enable tooltips
        setToolTipText("");
        // for editing
        editingKeyAction = createEditingKeyAction();
        // for key moving
        moveKeyAction = new KeyMoveAction();
        addKeyListener(moveKeyAction);
        // To support Dnd and cut, copy and paste
        setTransferHandler(GraphEditorTransferHandler.getInstance());
        // To support undo/redo
        undoManager = new GraphEditorUndoManager();
        // Support only ten level for the time being
        undoManager.setLimit(10);
    }
    
    /**
     * A help method to do key move.
     * @param dx
     * @param dy
     */
    private void moveObjects(int dx, int dy) {
        RenderableMoveHelper moveHelper = new RenderableMoveHelper();
        moveHelper.setGraphEditorPane(this);
        moveHelper.startMove(0, 0);
        moveHelper.move(dx, dy);
        moveHelper.completeMove(dx, dy);
        repaint(getVisibleRect());
    }
    
    private KeyListener createEditingKeyAction() {
    	KeyListener keyAction = new KeyAdapter() {
    		boolean isCaretDown = false;
    		public void keyPressed(KeyEvent e) {
				if (!isEditing || editingNode == null || editor == null)
					return;
				String text = editingNode.getDisplayName();
				char keyChar = e.getKeyChar();
				// See http://www.unicode.org/charts/PDF/U0000.pdf for a list
				// of Latin Unicode.
				if (keyChar >= ' ' && keyChar <= '~') {
					int index = editor.getCaretPosition();
					int start = editor.getSelectionStart();
					int end = editor.getSelectionEnd();
					String text1 = null;
					if (start < end) {
						if (start == 0 && end == text.length()) 
							text1 = keyChar + "";
						else if (start == 0) 
							text1 = keyChar + text.substring(end);
						else if (end == text.length())
							text1 = text.substring(0, start) + keyChar;
						else 
							text1 = text.substring(0, start) + keyChar + text.substring(end);
						editor.clearSelection();
						editor.setCaretPosition(start);
					}
					else {
						text1 = text.substring(0, index) + e.getKeyChar() + text.substring(index);
					}
					editingNode.setDisplayName(text1);
					editor.setIsChanged(true);
					editor.moveCaretToRight();
					repaint(getVisibleRect());
				}
				else {
					int vk = e.getKeyCode();
					boolean isShiftDown = (e.getModifiers() & KeyEvent.SHIFT_MASK) > 0;
					switch (vk) {
						case KeyEvent.VK_ENTER :
//							if (editor.isChanged()) {
//								GraphEditorActionEvent event = new GraphEditorActionEvent(editingNode);
//								event.setID(GraphEditorActionEvent.NAME_EDITING);
//								fireGraphEditorActionEvent(event);
//							}
						    stopEditing();
							break;
						case KeyEvent.VK_SHIFT :
							int i = editor.getCaretPosition();
							int start = editor.getSelectionStart();
							int end = editor.getSelectionEnd();
							// Reset the selection
							if (i != start && i != end) {
								editor.setSelectionEnd(i);
								editor.setSelectionStart(i);
							}
							// Otherwise do nothing.
							break;
						case KeyEvent.VK_LEFT :
							int pos0 = editor.getCaretPosition();
							editor.moveCaretToLeft();
							if (isShiftDown) {
								if (pos0 == editor.getSelectionStart())
									editor.setSelectionStart(editor.getCaretPosition());
								else
									editor.setSelectionEnd(editor.getCaretPosition());
							}
							else
								editor.clearSelection();
							repaint(editingNode.getBounds());
							break;
						case KeyEvent.VK_RIGHT :
							pos0 = editor.getCaretPosition();
							editor.moveCaretToRight();
							if (isShiftDown) {
								if (pos0 == editor.getSelectionEnd())
									editor.setSelectionEnd(editor.getCaretPosition());
								else
									editor.setSelectionStart(editor.getCaretPosition());
							}
							else
								editor.clearSelection();
							repaint(editingNode.getBounds());
							break;
						case KeyEvent.VK_END :
							editor.setCaretPosition(text.length());
							if (isShiftDown) 
								editor.setSelectionEnd(text.length());
							else
								editor.clearSelection();	
							repaint(editingNode.getBounds());
							break;
						case KeyEvent.VK_HOME :
							editor.setCaretPosition(0);
							if (isShiftDown)
								editor.setSelectionStart(0);
							else
								editor.clearSelection();
							break;
						case KeyEvent.VK_UP :
							pos0 = editor.getCaretPosition();
							editor.moveCaretUp();
							if (isShiftDown) {
								if (pos0 == editor.getSelectionEnd())
									editor.setSelectionEnd(editor.getCaretPosition());
								else
									editor.setSelectionStart(editor.getCaretPosition());
							}
							else
								editor.clearSelection();
							repaint(editingNode.getBounds());
							break;
						case KeyEvent.VK_DOWN :
							pos0 = editor.getCaretPosition();
							isCaretDown = editor.moveCaretDown();
							if (isShiftDown) {
								if (pos0 == editor.getSelectionEnd())
									editor.setSelectionEnd(editor.getCaretPosition());
								else
									editor.setSelectionStart(editor.getCaretPosition());
							}
							else
								editor.clearSelection();
							repaint(editingNode.getBounds());
							break;
						case KeyEvent.VK_BACK_SPACE :
							start = editor.getSelectionStart();
							end = editor.getSelectionEnd();
							String text1 = null;
							pos0 = editor.getCaretPosition();
							if (start < end) {
								if (start == 0 && end == text.length())
									text1 = "";
								else if (start == 0) {
									text1 = text.substring(end);
								}
								else if (end == text.length()) {
									text1 = text.substring(0, start);
								}
								else
									text1 = text.substring(0, start) + text.substring(end);
								editor.setCaretPosition(start);
								editor.clearSelection();
							}
							else {
								i = editor.getCaretPosition();
								if (i == 0)
									break;
								text1 = text.substring(0, i - 1) + text.substring(i);
								editor.moveCaretToLeft();
							}
							editingNode.setDisplayName(text1);
							editor.setIsChanged(true);
							repaint(getVisibleRect());
							break;
						case KeyEvent.VK_DELETE :
							start = editor.getSelectionStart();
							end = editor.getSelectionEnd();
							text1 = null;
							pos0 = editor.getCaretPosition();
							if (start < end) {
								if (start == 0 && end == text.length())
									text1 = "";
								else if (start == 0) {
									text1 = text.substring(end);
								}
								else if (end == text.length()) {
									text1 = text.substring(0, start);
								}
								else
									text1 = text.substring(0, start) + text.substring(end);
								editor.setCaretPosition(start);
								editor.clearSelection();
							}
							else {
								i = editor.getCaretPosition();
								if (i == text.length())
									break;
								text1 = text.substring(0, i - 1) + text.substring(i);
							}
							editingNode.setDisplayName(text1);
							editor.setIsChanged(true);
                            Rectangle bounds = editingNode.getBounds();
                            repaint((int)(bounds.x * scaleX),
                                    (int)(bounds.y * scaleY),
                                    (int)(bounds.width * scaleX + 1),
                                    (int)(bounds.height * scaleY + 1));
							//repaint(getVisibleRect());
							break;
					}
				}
				e.consume();
			}
			
			public void keyReleased(KeyEvent e) {
				if (editingNode == null && autoPane != null) {
					autoPane.setDisplay(false);
					autoPane = null; // Force gc.
					return;
				}
				if (autoPane == null)
					autoPane = createAutoPane();
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					if (isCaretDown) {
						return;
					}
					if (autoPane != null) {
						autoPane.requestFocus();
						autoPane.start();
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					// Force to disappear
					if (autoPane != null) {
						autoPane.setDisplay(false);
						autoPane = null;
					}
				}
				else if (autoPane != null) {
					autoPane.setStartText(editingNode.getDisplayName());
					// Need to repaint
					if (autoPane.getParent() != null) {
						Rectangle r = autoPane.getBounds();
						autoPane.getParent().repaint(r.x, r.y, r.width, r.height);
					}
				}
				e.consume(); // Don't want to popup the event.
			}
		};
    	return keyAction;
    }
    
    private AutoCompletionPane createAutoPane() {
     // To feed data into AutoCompletionPane
        String type = editingNode.getType();
        if (!(type.equals("Compartment")))
            return null;
    	AutoCompletionPane autoPane = new AutoCompletionPane();
    	autoPane.setTarget(new AutoCompletable() {
    		public void setText(String txt) {
    			if (editingNode != null) {
    				editingNode.setDisplayName(txt);
    				editor.setIsChanged(true);
    				editor.setCaretPosition(txt.length());
    				repaint(getVisibleRect());
    				// A little weird: Have to call requestFocus() here to
    				// grab the focus for an edit from less lines to more lines.
    				requestFocus();
    				stopEditing();
    			}
    		}
    	});
    	// Try to get the top container
    	Rectangle bounds = new Rectangle(editingNode.getTextBounds());
    	bounds.width = 300;
    	autoPane.setTextBounds(bounds);
    	autoPane.setInvokingComponent(this);
    	autoPane.setScaleX(scaleX);
    	autoPane.setScaleY(scaleY);
    	// To feed data into AutoCompletionPane
		autoPane.setData(PropertyManager.getManager().getCompartmentNames());
//		if (type.equals("Entity")) {
//			autoPane.setData(PropertyManager.getManager().getEntities());
//		}
//		else if (type.equals("Complex")) {
//			autoPane.setData(PropertyManager.getManager().getComplexes());
//		}
//		else if (type.equals("Reaction")) {
//			autoPane.setData(PropertyManager.getManager().getReactions());
//		}
//		else if (type.equals("Pathway")) {
//			autoPane.setData(PropertyManager.getManager().getPathways());
//		}
    	return autoPane;
    }
    
    public void setRenderable(Renderable displayedObject) {
    	boolean needValidate = false;
    	if (this.displayedObject != displayedObject)
    		needValidate = true;
        this.displayedObject = displayedObject;
        if (needValidate) {
        	revalidate();
        	repaint();
        	removeSelection();
        }
    }
    
    public Renderable getRenderable() {
        return this.displayedObject;
    }
    
    /**
     * Return the Renderer object used for a Renderable object.
     * @param r
     * @return
     */
    public Renderer getRenderer(Renderable r) {
    	return null;
    }
    
    /**
     * Get the displayed objects in this GraphEdiorPane. It should include all
     * children in the Renderable object.
     * @return a list of all Renderable objects that are displayed in this
     * GraphEditorPane.
     */
    public java.util.List getDisplayedObjects() {
    	if (displayedObject == null || displayedObject.getComponents() == null)
    		return new ArrayList();
    	return displayedObject.getComponents();
    }
    
    public void selectAll() {
    	java.util.List list = getDisplayedObjects();
        if (list != null) {
            java.util.List<Renderable> selection = new ArrayList<Renderable>();
            for (Iterator it = list.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (!r.isVisible())
                    continue;
                r.setIsSelected(true);
                selection.add(r);
            }
            selectionModel.setSelection(selection);
        }
        repaint(getVisibleRect());
    }
    
    public void select(Rectangle rect) {
    	java.util.List list = getDisplayedObjects();
    	if (list != null) {
    		java.util.List selection = selectionModel.getSelection();
    		Renderable renderable = null;
    		// Clear selection first
    		for (Iterator it = selection.iterator(); it.hasNext();) {
    			renderable = (Renderable) it.next();
    			renderable.setIsSelected(false);
    			it.remove();
    		}
    		// Add new selection
    		for (Iterator it = list.iterator(); it.hasNext();) {
    			renderable = (Renderable) it.next();
    			renderable.select(rect);
    			if (renderable.isSelected())
    			    selection.add(renderable);
    		}
    		selectionModel.fireSelectionChange();
    	}
    }
    
    public void addSelected(Object obj) {
        selectionModel.addSelected(obj);
    }
    
    public void removeSelected(Object obj) {
        selectionModel.removeSelected(obj);
    }
    
    public void addSelection(java.util.List selection) {
        selectionModel.addSelection(selection);
    }
    
    public void removeSelection(java.util.List selection) {
        selectionModel.removeSelection(selection);
    }
    
    public void removeSelection() {
    	selectionModel.removeSelection();
    }
    
    protected java.util.List<Renderable> deleteSelection(java.util.List<Renderable> list) {
        if (list.size() == 0)
            return new ArrayList<Renderable>();
        // selection might be changed in this operation. So use a copy of selection.
        Set<Renderable> toBeDeleted = new HashSet<Renderable>(list);
        // If a Complex has hidden its components, its components should be deleted too
        Set<Renderable> complexComps = new HashSet<Renderable>();
        for (Renderable r : toBeDeleted) {
            if (r instanceof RenderableComplex) {
                RenderableComplex complex = (RenderableComplex) r;
                if (complex.isComponentsHidden()) {
                    Set<Renderable> comps = RenderUtility.getAllContainedComponents(complex);
                    complexComps.addAll(comps);
                }
            }
        }
        toBeDeleted.addAll(complexComps);
        for (Iterator it = toBeDeleted.iterator(); it.hasNext();) {
            Renderable renderable = (Renderable)it.next();
            if (renderable instanceof ContainerNode) {
                setContainerForComponents(renderable);
            }
            java.util.List reactions = null;
            if (renderable instanceof Node) {
                reactions = ((Node)renderable).getConnectedReactions();
                // Tell the connected reactions
                // Call these firing before the actual removing
                // so that the connected info
                // NOTE: THIS MIGHT HAVE ANY SIDE EFFECTS
                if (reactions != null && reactions.size() > 0) {
                    for (Iterator it1 = reactions.iterator(); it1.hasNext();) {
                        Object obj = it1.next();
                        GraphEditorActionEvent event = new DetachActionEvent(obj);
                        fireGraphEditorActionEvent(event);
                    }
                }
            }
            else if (renderable instanceof HyperEdge) {
                GraphEditorActionEvent event = new DetachActionEvent(renderable);
                fireGraphEditorActionEvent(event);
            }
            renderable.clearConnectWidgets();
            if (renderable.getContainer() != null) {
                renderable.getContainer().removeComponent(renderable);
                validatePathwayComponent(renderable);
            }
            // It is always contained by the top-level container
            displayedObject.removeComponent(renderable);
            // keep this container information since these will be used to validate
            // the hierarchical tree. Need to keep an eye on this in case other bugs
            // occur -- May 30, 2007.
            // Container cannot be kept. Otherwise, the object list view will not
            // be corrected -- June 1, 2007
            renderable.setContainer(null);
            RenderableRegistry.getRegistry().remove(renderable);
        }
        list.clear();
        java.util.List<Renderable> rtn = new ArrayList<Renderable>(toBeDeleted);
        firePropertyChange("delete", displayedObject, rtn);
        fireGraphEditorActionEvent(GraphEditorActionEvent.DELET);
        revalidate();
        repaint(getVisibleRect());
        removeSelection();
        return rtn;
    }
    
    /**
     * A Renderable object may be contained by several RenderablePathways. However,
     * it can only be referred by one RenderablePathway via container. This is not
     * good. This method is used to fix this bug during deletion.
     * @param r
     */
    private void validatePathwayComponent(Renderable r) {
        for (Iterator it = getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable tmp = (Renderable) it.next();
            if (tmp instanceof RenderablePathway) {
                RenderablePathway pathway = (RenderablePathway) tmp;
                if (pathway.contains(r)) {
                    pathway.removeComponent(r);
                }
            }
        }
    }
    
    /**
     * Delete the selected Renderable objects.
     * @return a list of actually deleted objects.
     */
    public java.util.List<Renderable> deleteSelection() {
        java.util.List list = getSelection();
        return deleteSelection(list);
    }
    
    private void setContainerForComponents(Renderable r) {
        // RenderablePathway is used as a virtual grouping mechanism. It will
        // not change the original hierarchical setting.
        if (r instanceof RenderablePathway)
            return;
        Renderable container = r.getContainer();
        if (container == null)
            container = displayedObject;
        java.util.List components = r.getComponents();
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable tmp = (Renderable) it.next();
            tmp.setContainer(container);
            if (container != displayedObject)
                container.addComponent(tmp);
        }
    }
    
    /**
     * Delete Renderable objects in this GraphEditorPane. The deleted objects
     * include the target and its all Shortcuts that are displayed.
     * @param renderable this Renderable object should not be a Shortcut.
     */
    /**
     * TODO: Rewrite this method to call deleteSelection(List<Renderable>) to avoid any
     * duplication code in the delete method.
     */
    public void delete(Renderable renderable) {
    	Renderable target = renderable;
    	// Get the deleted objects
    	java.util.List list = new ArrayList();
    	for (Iterator it = displayedObject.getComponents().iterator(); it.hasNext();) {
    		Object obj = it.next();
    		if (obj == target)
    			list.add(obj);
    		else if (obj instanceof Shortcut) {
    			if (((Shortcut)obj).getTarget() == target)
    				list.add(obj);
    		}
    	}
    	java.util.List listCopy = new ArrayList(list);
		for (Iterator it = list.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			java.util.List reactions = null;
			if (r instanceof Node) {
				reactions = ((Node)r).getConnectedReactions();
			}
            // Tell the connected reactions
            // Call these firing before the actual removing
            // so that the connected info
            // NOTE: THIS MIGHT HAVE ANY SIDE EFFECTS
            if (reactions != null && reactions.size() > 0) {
                for (Iterator it1 = reactions.iterator(); it1.hasNext();) {
                    Object obj = it1.next();
                    GraphEditorActionEvent event = new DetachActionEvent(obj);
                    fireGraphEditorActionEvent(event);
                }
            }
			r.clearConnectWidgets();
			selectionModel.removeSelected(r);
			displayedObject.removeComponent(r);
			// Remove r from its container.
            if (renderable.getContainer() != null) {
                renderable.getContainer().removeComponent(renderable);
                validatePathwayComponent(renderable);
            }
            r.setContainer(null);
			it.remove();
		}
		revalidate();
		repaint(getVisibleRect());
		firePropertyChange("delete", 
		                   displayedObject, 
		                   listCopy);
		fireGraphEditorActionEvent(GraphEditorActionEvent.DELET);
		RenderableRegistry.getRegistry().remove(target);
    }

    public java.util.List getSelection() {
        return selectionModel.getSelection();
    }
    
    public void setSelection(java.util.List selection) {
        if (selection != null) // Don't want to select the container
            selection.remove(displayedObject); 
       selectionModel.setSelection(selection);
       ensureSelectionVisible();
       repaint(getVisibleRect());
    }
    
    private void hiliteForSelection(java.util.List<Renderable> selection) {
        // Make sure there are no entities are highlighted
        for (Iterator it = getDisplayedObjects().iterator(); it.hasNext();) {
            Object obj = it.next();
            Renderable r = (Renderable) obj;
            r.setIsHighlighted(false);
        }
        // Work for only one reaction selected
        if (selection.size() != 1)
            return;
        Renderable r = selection.get(0);
        if (r instanceof HyperEdge) {
            HyperEdge edge = (HyperEdge) r;
            java.util.List<Node> entities = edge.getConnectedNodes();
            for (Node node : entities)
                node.setIsHighlighted(true);
        }
        else if (r instanceof Node) {
            java.util.List<HyperEdge> edges = ((Node)r).getConnectedReactions();
            for (HyperEdge r1 : edges) {
                r1.setIsHighlighted(true);
            }
        }
    }
    
    public void ensureSelectionVisible() {
        java.util.List selection = getSelection();
        if (selection == null || selection.size() == 0)
            return;
        // Have to make sure at least one is visible
        Renderable r = null;
        Rectangle bounds = null;
        Rectangle visibleRect = getVisibleRect();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            bounds = r.getBounds();
            if (bounds == null)
                continue;
            bounds = scaleBounds(bounds);
            if (visibleRect.intersects(bounds))
                return;
        }
        // Pick the first one
        for (Iterator it = selection.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            bounds = r.getBounds();
            if (bounds != null) {
                bounds = scaleBounds(bounds);
                scrollRectToVisible(bounds);
                return;
            }
        }
    }
    
    private Rectangle scaleBounds(Rectangle bounds) {
        Rectangle rtn = new Rectangle(bounds);
        rtn.x *= scaleX;
        rtn.y *= scaleY;
        rtn.width *= scaleX;
        rtn.height *= scaleY;
        return rtn;
    }
    
    public void setSelection(Renderable r) {
        selectionModel.removeSelection();
        selectionModel.addSelected(r);
    }
    
    public void setSelectionModel(GraphEditorSelectionModel model) {
    	this.selectionModel = model;
    }
    
    public GraphEditorSelectionModel getSelectionModel() {
    	return this.selectionModel;
    }
    
    public void mouseClicked(MouseEvent mouseEvent) {
        // Fire double click action only if Button1 is clicked
    	if (mouseEvent.getClickCount() == 2 &&
        mouseEvent.getButton() == MouseEvent.BUTTON1) {
    		GraphEditorActionEvent event = new GraphEditorActionEvent(this, GraphEditorActionEvent.ACTION_DOUBLE_CLICKED);
    		fireGraphEditorActionEvent(event);
    	}
    	else if (currentAction != null){
    		currentAction.doAction(mouseEvent);
    	}
    }
    
    protected void drawDragRect(Graphics2D g2) {
        // dragRect's width or height might be negative
        if (dragRect.width == 0 || dragRect.height == 0)
            return; // This is an actual empty
        g2.setPaint(DefaultRenderConstants.SELECTION_WIDGET_COLOR);
        if (dragRect.width > 0 && dragRect.height > 0)
            g2.draw(dragRect);
        else {
            int x, y, w, h;
            if (dragRect.width < 0) {
                x = dragRect.x + dragRect.width;
                w = -dragRect.width;
            }
            else {
                x = dragRect.x;
                w = dragRect.width;
            }
            if (dragRect.height < 0) {
                y = dragRect.y + dragRect.height;
                h = -dragRect.height;
            }
            else {
                y = dragRect.y;
                h = dragRect.height;
            }
            g2.drawRect(x, y, w, h);
        }
    }
    
    public void mouseDragged(MouseEvent mouseEvent) {
    	if (currentAction != null)
            currentAction.doAction(mouseEvent);
    }
    
    public void mouseEntered(MouseEvent mouseEvent) {
    }
    
    public void mouseExited(MouseEvent mouseEvent) {
    }
    
    public void mouseMoved(MouseEvent mouseEvent) {
        setCursorAction.doAction(mouseEvent);
    }
    
    public void mousePressed(MouseEvent e) {
    	requestFocus();
        // Update default position (moved from mouseReleased)
        //defaultInsertPos.x = e.getX();
        //defaultInsertPos.y = e.getY();
    	currentAction = selectAction;
        currentAction.doAction(e);
		if (isEditing) {
			currentAction = editingAction;
			editingAction.doAction(e);
		}
    }
    
    public void mouseReleased(MouseEvent mouseEvent) {
        if (currentAction != null)
            currentAction.doAction(mouseEvent);
        // Update default position 
        defaultInsertPos.x = mouseEvent.getX();
        defaultInsertPos.y = mouseEvent.getY();
    }
    
    protected void updateDefaultInsertPos() {
        defaultInsertPos.x += 40;
        defaultInsertPos.y += 40;
        // Make sure it is visible
        Rectangle visibRect = getVisibleRect();
        if (defaultInsertPos.x > visibRect.getMaxX() - 20)
            defaultInsertPos.x = visibRect.x + 50;
        if (defaultInsertPos.y > visibRect.getMaxY() - 20)
            defaultInsertPos.y = visibRect.y + 50;
    }
    
    public void addGraphEditorActionListener(GraphEditorActionListener l) {
    	if (editorActionListeners == null)
    		editorActionListeners = new ArrayList();
    	if (!editorActionListeners.contains(l))
    		editorActionListeners.add(l);
    }	  
    
    public void removeGraphEditorActionListener(GraphEditorActionListener l) {
    	if (editorActionListeners != null)
    		editorActionListeners.remove(l);
    }
    
    public void fireGraphEditorActionEvent(GraphEditorActionEvent e) {
    	if (editorActionListeners == null)
    		return;
    	GraphEditorActionListener l = null;
    	for (Iterator it = editorActionListeners.iterator(); it.hasNext();) {
    		l = (GraphEditorActionListener) it.next();
    		l.graphEditorAction(e);
    	}
    }
    
    public void fireGraphEditorActionEvent(GraphEditorActionEvent.ActionType actionType) {
        GraphEditorActionEvent event = new GraphEditorActionEvent(this, actionType);
        fireGraphEditorActionEvent(event);
    }
    
    public Dimension getPreferredSize() {
    	// Reset the rectangle.
    	preferredRect.x = 0;
    	preferredRect.y = 0;
    	// Give it a minimum size
    	preferredRect.width = 10; 
    	preferredRect.height = 10;
    	if (displayedObject != null) {
    		Dimension size = RenderUtility.getDimension(displayedObject);
    		preferredRect.width = size.width;
    		preferredRect.height = size.height;
    	}
    	Dimension size = preferredRect.getSize();
    	size.width *= scaleX;
    	size.height *= scaleY;
    	return size;
    }

    public void alignSelectionVertically() {
    	java.util.List selection = getSelection();
    	if (selection.size() == 0)
    		return ;
    	// Align to the first Renderable
    	Renderable renderable = (Renderable) selection.get(0);
    	Point firstPos = renderable.getPosition();
    	Point pos = null;
    	for (int i = 1; i < selection.size(); i++) {
    		renderable = (Renderable) selection.get(i);
    		pos = renderable.getPosition();
    		renderable.move(firstPos.x - pos.x, 0);
    	}
    	if (selection.size() > 1)
    		repaint(getVisibleRect());
    }
    
    public void alignSelectionHorizontally() {
    	java.util.List selection = getSelection();
    	if (selection.size() == 0)
    		return;
    	Renderable renderable = (Renderable) selection.get(0);
    	Point firstPos = renderable.getPosition();
    	Point pos = null;
    	for (int i = 1; i < selection.size(); i++) {
    		renderable = (Renderable) selection.get(i);
    		pos = renderable.getPosition();
    		renderable.move(0, firstPos.y - pos.y);
    	}
    	if (selection.size() > 1)
    		repaint(getVisibleRect());
    }
    
    protected void setMouseOveredRenderable(Renderable r) {
        this.mouseOveredRenderable = r;
    }
    
    protected Renderable getMouseOveredRenderable() {
        return this.mouseOveredRenderable;
    }
    
	/**
	 * Generate tooltip text based on mouse position.
	 */
    public String getToolTipText(MouseEvent event) {
        java.util.List comps = getDisplayedObjects();
        if (comps == null || comps.size() == 0)
            return null;
        int size = comps.size();
        Point p = event.getPoint();
        p.x /= scaleX;
        p.y /= scaleY;
        if (mouseOveredRenderable != null) {
            if (mouseOveredRenderable.canBePicked(p))
                return generateToolTipText(mouseOveredRenderable);
        }
        for (int i = size - 1; i >= 0; i--) {
            Renderable renderable = (Renderable) comps.get(i);
            if (renderable.canBePicked(p)) {
                mouseOveredRenderable = renderable;
                return generateToolTipText(renderable);
            }
        }
        mouseOveredRenderable = null;
        return null;
    }   
    
    private String generateToolTipText(Renderable renderable) {
        String displayName = renderable.getDisplayName();
        String localization = getLocationLization(renderable);
        if (displayName == null)
            displayName = "";
        String tooltip = renderable.getType() + ": " + displayName;
        if (localization != null)
            tooltip += (" in " + localization);
        return tooltip;
    }
    
    protected String getLocationLization(Renderable renderable) {
        String localization = renderable.getLocalization();
        if (localization == null) {
            // Check container
            Renderable container = renderable.getContainer();
            if (container instanceof RenderableCompartment) {
                localization = PropertyManager.getManager().
                                    getLocalizationFromContainer((RenderableCompartment)container,
                                                                 renderable);
            }
        }
        return localization;
    }
	
	/**
	 * Stop name editing.
	 */
	public void stopEditing() {
		if (!isEditing)
			return;
		if (autoPane != null) {
            autoPane.setVisible(false);
            autoPane = null;
        }
        editingNode.setIsEditing(false);
		repaint(editingNode.getBounds());
		setIsEditing(false);
		setEditingNode(null); // Reset to null.
	} 
	
	public void setIsEditing(boolean editing) {
		if (isEditing != editing) {
			if (editing) {
			    removeKeyListener(moveKeyAction);
				addKeyListener(editingKeyAction);
			}
			else {
				removeKeyListener(editingKeyAction);
				addKeyListener(moveKeyAction);
				editor.reset();
			}
			this.isEditing = editing;
		}
	}
	
	public boolean isEditing() {
		return this.isEditing;
	}
	
	public void setEditingNode(Node node) {
		if (editingNode != node) {
			if (editingNode != null) {
				editingNode.setIsEditing(false);
				// Check if the editing name is empty when commiting
                String newName = editingNode.getDisplayName().trim();
                String message = null;
                rename(editingNode,
                       null,
                       newName,
                       oldName);
//				while ((message = valiateNodeName(newName)) != null) {
//					newName = (String) JOptionPane.showInputDialog(this, 
//					                                              message, 
//					                                              newName);
//					                                           
//					if (newName == null) {
//						newName = oldName;
//						break;
//					}
//				}
//				editingNode.setDisplayName(newName);
//                editingNode.setIsChanged(true);
//				RenderableRegistry.getRegistry().changeName(editingNode, oldName);
//				GraphEditorActionEvent e = new GraphEditorActionEvent(editingNode, 
//                                                                      GraphEditorActionEvent.NAME_EDITING);
//				fireGraphEditorActionEvent(e);
//				repaint(getVisibleRect());
//				if (autoPane != null) {
//					if (autoPane.isVisible())
//						autoPane.setDisplay(false);
//					autoPane = null;
//				}
			}
			if (node != null) {
				node.setIsEditing(true);
				oldName = node.getDisplayName();
			}
			editingNode = node;
		}
	}
    
    public void rename(Renderable r,
                       String message,
                       String newName,
                       String oldName) {
        do {
            if (message != null) { // This is a little awkward. Have to think out a new way
                // Use JFrame for window position if possible
                Container frame = SwingUtilities.getAncestorOfClass(Container.class, this);
                newName = (String) JOptionPane.showInputDialog(frame == null ? this : frame, 
                                                               message, 
                                                               newName);
                                                           
                if (newName == null) {
                    newName = oldName;
                    // In case it is changed already as in the on-line editing
                    r.setDisplayName(newName);
                    repaint(getVisibleRect());
                    break;
                }
            }
        }
        while ((message = valiateNodeName(newName, oldName)) != null);
        if (oldName.equals(newName))
            return; // Do nothing
        r.setDisplayName(newName);
        r.setIsChanged(true);
        RenderableRegistry.getRegistry().changeName(r, oldName);
        GraphEditorActionEvent e = new GraphEditorActionEvent(r, 
                                                              GraphEditorActionEvent.NAME_EDITING);
        fireGraphEditorActionEvent(e);
        repaint(getVisibleRect());
    }
                       
	
	private String valiateNodeName(String newName,
                                   String oldName) {
		String message = null;
		if (newName.length() == 0) {
			message = "Name cannot be empty. \nPlease input a non-empty name here:";
			return message;
		}
		if (!newName.equals(oldName) && 
		    RenderableRegistry.getRegistry().contains(newName)) {
			message = "Name \"" + newName + "\" has already been used by another object in the process.\n" +
					  "Please input another name here:";
			return message;
		}
		return message;		
	}
	
	public Renderable getEditingNode() {
		return this.editingNode;
	}
	
	public Editor getEditor() {
		return this.editor;
	}
	
	public void insertNode(Node node) {
	}
	
	public void insertEdge(HyperEdge edge, boolean useDefaultPosition) {
    }
	
	protected void validateCompartmentSetting(Renderable r) {
	}
	
	protected void validateComplexSetting(Renderable r) {
	}
	
	// These methods to support cut, copy and paste
	public void cut() {
		Action cutAction = TransferHandler.getCutAction();
		cutAction.actionPerformed(new ActionEvent(this, 
						                          ActionEvent.ACTION_PERFORMED,
						                          (String)cutAction.getValue(Action.NAME)));
	}
	
	public void pasteAsAliase() {
		GraphEditorTransferHandler transferHandler = (GraphEditorTransferHandler) getTransferHandler();
		transferHandler.setIsForAliase(true);
		Action pasteAction = TransferHandler.getPasteAction();
		ActionEvent event = new ActionEvent(this, 
		                                    ActionEvent.ACTION_PERFORMED,
		                                    (String)pasteAction.getValue(Action.NAME));
		pasteAction.actionPerformed(event);                                 
	}
	
	public void pasteAsNewInstance() {
		GraphEditorTransferHandler transferHandler = (GraphEditorTransferHandler) getTransferHandler();
		transferHandler.setIsForAliase(false);
		Action pasteAction = TransferHandler.getPasteAction();
		ActionEvent event = new ActionEvent(this, 
											ActionEvent.ACTION_PERFORMED,
											(String)pasteAction.getValue(Action.NAME));
		pasteAction.actionPerformed(event);                                 		
	}
    
    public void cloneInstances() {
        copy();
        pasteAsNewInstance();
    }
	
	public void copy() {
		Action copyAction = TransferHandler.getCopyAction();
		ActionEvent event = new ActionEvent(this,
		                                    ActionEvent.ACTION_PERFORMED,
		                                    (String) copyAction.getValue(Action.NAME));
		copyAction.actionPerformed(event);		                                  
	}
	
	public void setEditable(boolean editable) {
		this.isEditable = editable;
	}
	
	public boolean isEditable() {
		return this.isEditable;
	}
	
	/**
	 * If a GraphEditorPane is used as a drawing tool, no editing will be done.
	 * This mode is different from !isEditable, in this mode, a reaction may be 
	 * connected to another entities which is an alias to the original one.
	 * @param value
	 */
	public void setUsedAsDrawingTool(boolean value) {
	    this.usedAsDrawingTool = value;
	}
	
	public boolean isUsedAsDrawingTool() {
	    return this.usedAsDrawingTool;
	}
	
	public void layoutRenderable() {
	}
	
	/**
	 * Try to place the displayed Renderable object in the center of the
	 * visible area. 
	 */
	public void centerRenderable() {
		Dimension visibleSize = getVisibleRect().getSize();
		RenderUtility.center(displayedObject, visibleSize);
	}
	
	public int isLinkWidgetPicked(int x, int y) {
		return LINK_WIDGET_NONE;
	}

	public void setShouldDrawLinkWidgets(boolean shouldDraw) {
	}
    
    public ConnectionPopupManager getConnectionPopupManager() {
        return null;
    }
    
    /**
     * The client should call this method if it wants to repaint the whole pane.
     * @param scaleX
     * @param scaleY
     * @see setScale(double, double)
     */
    public void zoom(double scaleX, double scaleY) {
    	setScale(scaleX, scaleY);
        revalidate();
        ensureSelectionVisible();
    	repaint();
    }

    public void zoomToFit() {
    	Dimension size = getVisibleRect().getSize();
    	Dimension preferredSize = getPreferredSize();
    	double w = preferredSize.width / scaleX; // Scale back to the original dimension
    	double h = preferredSize.height / scaleY;
    	double scaleX = size.width / w;
    	double scaleY = size.height / h;
    	// Scale use the smaller one to fit to the page
    	if (scaleX < scaleY)
    		zoom(scaleX, scaleX);
    	else
    		zoom(scaleY, scaleY);
    }
    
    public RenderableCompartment pickUpCompartment(Renderable r) {
        return null;
    }
    
    public RenderableComplex pickUpComplex(Node node) {
        return null;
    }
    
    public java.util.List<Node> pickUpComplexComponents(RenderableComplex complex) {
        return null;
    }
    
    public java.util.List<Renderable> pickUpCompartmentComponents(RenderableCompartment compartment) {
        return null;
    }

    protected void drawStoichiometry(int stoi, Node node, Graphics2D g2) {
        if (stoi < 2) { 
            node.setStoiBounds(null);
            return; // Don't need to draw stoichiometry
        }
        Rectangle r = node.getBounds();
        FontMetrics metrics = g2.getFontMetrics();
        String text = stoi + "";
        Rectangle2D textBounds = metrics.getStringBounds(text, g2);
        int x = (int)(r.x - textBounds.getWidth() - 4);
        int y = (int)(r.y + r.height / 2 - textBounds.getHeight() / 2);
        y += metrics.getAscent();
        Rectangle stoiBounds = node.getStoiBounds();
        if (stoiBounds == null) {
            stoiBounds = new Rectangle();
            node.setStoiBounds(stoiBounds);
        }
        stoiBounds.x = x;
        stoiBounds.y = r.y;
        stoiBounds.width = (int)textBounds.getWidth();
        stoiBounds.height = r.height;
        g2.drawString(text, x, y);
    }
    
    /**
     * This method is refactored from suclasses to this class. All subclasses should
     * call this method first.
     */
    public void paint(Graphics g) {
        //Clear the editor
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        // In case g is provided by other graphics context (e.g during exporting)
        g2.setFont(getFont());
        g2.setBackground(background);
        g2.clearRect(0, 0, getWidth(), getHeight());
        // Enable zooming
        g2.scale(scaleX, scaleY);
    }
    
    /**
     * Set the background for this GraphEditorPane.
     */
    public void setBackground(Color bg) {
        this.background = bg;
    }
    
    /**
     * This method is used to select a node for editing name.
     * @param editingNode
     * @param editor
     */
    public void prepareForEditing(Node editingNode) {
        selectionModel.removeSelection();
        selectionModel.addSelected(editingNode);
        if (editingNode.isEditable()) {
            editingNode.setIsEditing(true);
            setEditingNode(editingNode);
            setIsEditing(true); 
        }
        // Can limit the repaint area to the editingNode
        repaint(getVisibleRect()); 
        requestFocus();  
    }
    
    public void exportDiagram() throws IOException {
        // Get the file name
        final JFileChooser fileChooser = new JFileChooser();
        Properties systemProperties = GKApplicationUtilities.getApplicationProperties();
        if (systemProperties != null) {
            String currentDir = systemProperties.getProperty("currentDir");
            if (currentDir != null)
                fileChooser.setCurrentDirectory(new File(currentDir));
        }
        fileChooser.setDialogTitle("Export Pathway Diagram ...");
//        // Need to do a scale back first in case the diagram has been scaled.
//        double scaleX = getScaleX();
//        double scaleY = getScaleY();
//        setScale(1.0d, 1.0d);
//        BufferedImage image = SwingImageCreator.createImage(this);
        SwingImageCreator.exportImage(GraphEditorPane.this, fileChooser);
//        setScale(scaleX, scaleY);
//        // Directly export image to avoid any scaling issues.
//        SwingImageCreator.exportImage(image,
//                                      fileChooser,
//                                      this);
        if (systemProperties != null)
            GKApplicationUtilities.storeCurrentDir(fileChooser, systemProperties);
    }
    
    /**
     * To control if pathways should be drawn.
     * @param isVisible
     */
    public void setPathwayVisible(boolean isVisible) {
    }
    
    public GraphEditorUndoManager getUndoManager() {
        return this.undoManager;
    }
    
    public void undo() {
        undoManager.undo();
    }
    
    public void redo() {
        undoManager.redo();
    }
    
    public boolean canUndo() {
        return undoManager.canUndo();
    }
    
    public boolean canRedo() {
        return undoManager.canRedo();
    }
    
    public void addUndoableEdit(UndoableEdit edit) {
        undoManager.addEdit(edit);
    }
    
    public void killUndo() {
        undoManager.die();
    }
    
    /**
     * This customized KeyAdaptor is used to do key based move.
     */
    private class KeyMoveAction extends KeyAdapter {
        private final int STEP = 2;
        private RenderableMoveHelper moveHelper;
        private int totalMoveX;
        private int totalMoveY;
        private boolean duringMove = false;
        
        KeyMoveAction() {
            moveHelper = new RenderableMoveHelper();
            moveHelper.setGraphEditorPane(GraphEditorPane.this);
        }
        
        public void keyPressed(KeyEvent e) {
            int vk = e.getKeyCode();
            if (vk == KeyEvent.VK_LEFT ||
                vk == KeyEvent.VK_RIGHT ||
                vk == KeyEvent.VK_UP ||
                vk == KeyEvent.VK_DOWN) {
                int dx = 0, dy = 0;
                switch (vk) {
                    case KeyEvent.VK_LEFT :
                        dx = -STEP;
                        break;
                    case KeyEvent.VK_RIGHT :
                        dx = STEP;
                        break;
                    case KeyEvent.VK_UP :
                        dy = -STEP;
                        break;
                    case KeyEvent.VK_DOWN :
                        dy = STEP;
                        break;
                }
                if (dx != 0 || dy != 0) {
                    if (!duringMove) {
                        // For some initial state
                        duringMove = true;
                        moveHelper.startMove(0, 0);
                        totalMoveX = 0;
                        totalMoveY = 0;
                    }
                    moveHelper.move(dx, dy);
                    repaint(getVisibleRect());
                    totalMoveX += dx;
                    totalMoveY += dy;
                }
                e.consume(); // Have to consume the keyevent here. Otherwise, it
                             // will popup up to the parent component. For example,
                             // it will scroll the pathway if a pathway is too big.
            }
        }
        
        public void keyReleased(KeyEvent e) {
            if (!duringMove)
                return;
            duringMove = false;
            moveHelper.completeMove(totalMoveX, totalMoveY);
            repaint(getVisibleRect());
            e.consume();
        }
    }
}
