/*
 * Created on Sep 4, 2003
 */
package org.gk.graphEditor;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

import org.gk.render.*;

/**
 * A customized TransferHandler to support DnD and cut, copy and paste actions.
 * This class is modified from http://java.sun.com/docs/books/tutorial/uiswing/
 * misc/example-1dot4/ArrayListTransferHandler.java.
 * Only serial flavor is supported to inhibit the same instance pasting by 
 * local JVM flavor.
 * @author wgm
 */
public class GraphEditorTransferHandler extends TransferHandler {

	protected DataFlavor serialArrayListFlavor;
	private static GraphEditorTransferHandler instance;
	// Control the import data mode
	private boolean isForAliase = true;
	
	public static GraphEditorTransferHandler getInstance() {
		if (instance == null)
			instance = new GraphEditorTransferHandler();
		return instance;
	}
	
	protected GraphEditorTransferHandler() {
		serialArrayListFlavor = new DataFlavor(ArrayList.class,
											  "ArrayList");
	}
    
    public boolean importListOfRenderables(ArrayList aList, 
                                           GraphEditorPane graphPane) {
        if (aList != null && aList.size() > 0) {
            // Make sure there is no circular reference occur
            if (!circularRefValidate(aList, graphPane))
                return false; 
            // Make sure graphPane can add correct data
            validateTypes(aList, graphPane);
            // Remove the first source
            String sourceName = (String) aList.remove(0);
            if (sourceName.equals("dbBrowser")) {
                if (!checkDuplications(aList, graphPane))
                    return true;
                // Use shortcuts if necessary
                validateShortcutsForDB(aList);
                assignUniqueID(aList, graphPane.getRenderable());
                insertRenderabls(aList, graphPane);
                return true;
            }
            // Have to validate connections first before something changed to the list
            validateConnections(aList);
            // Check the shortcuts
            validateShortcuts(aList);
            if (isForAliase)
                importDataAsAlias(aList, graphPane);
            else {
                importDataAsNewInstance(aList, graphPane);
                isForAliase = true; // Always make for aliases as the default.
                                    // The is the default behavior for DnD
            }
        }
        return true;
    }
    
    private boolean circularRefValidate(List aList, GraphEditorPane graphPane) {
        // Need to check for Pathway and Complex only
        if (graphPane instanceof PathwayEditor ||
            graphPane instanceof ComplexGraphEditor) {
            Renderable container = graphPane.getRenderable();
            for (int i = 1; i < aList.size(); i++) {
                // The first element should be escaped since it is a string
                Renderable r = (Renderable) aList.get(i);
                if(!circularRefValidate(container, r, graphPane))
                    return false;
            }
        }
        return true;
    }
    
    private boolean circularRefValidate(Renderable container, Renderable contained, GraphEditorPane graphPane) {
        String circularRefName = RenderUtility.searchCircularRef(container, contained);
        if (circularRefName != null) {
                JOptionPane.showMessageDialog(graphPane,
                        "Circular reference for \"" + circularRefName + "\" can be created.\n" +
                        "This drag-and-drop cannot be allowed.",
                        "Error in Drag-And-Drop",
                        JOptionPane.ERROR_MESSAGE);
                return false;
        }
        return true;
    }

	public boolean importData(JComponent c, Transferable t) {
		ArrayList aList = null;
		if (!canImport(c, t.getTransferDataFlavors())) {
			return false;
		}
		try {
			if (hasSerialArrayListFlavor(t.getTransferDataFlavors())) {
				aList = (ArrayList)t.getTransferData(serialArrayListFlavor);
			} 
			else {
				return false;
			}
		} catch (UnsupportedFlavorException ufe) {
			System.err.println("importData: unsupported data flavor");
			return false;
		} catch (IOException ioe) {
			System.err.println("importData: I/O exception");
			return false;
		}
		return importListOfRenderables(aList, (GraphEditorPane)c);
	}
	
	private void validateTypes(java.util.List list, GraphEditorPane graphPane) {
		java.util.List entities = new ArrayList();
		java.util.List events = new ArrayList();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof RenderableEntity || obj instanceof RenderableComplex)
				entities.add(obj);
			else if (obj instanceof Renderable) // It might be a string: A string is used 
			                                    // for marking source
				events.add(obj);
		}
		if (graphPane instanceof PathwayEditor) {
//			if (entities.size() > 0) {
//				JOptionPane.showMessageDialog(
//					graphPane,
//					"Entities cannot be pasted or drag-dropped to a pathway graph pane.",
//					"Error",
//					JOptionPane.ERROR_MESSAGE);
//				list.removeAll(entities);
//				return;
//			}
		}
		else {
			if (events.size() > 0) {
				String msg = "Pathways or reactions cannot be pasted or drag-dropped to a ";
				if (graphPane instanceof ComplexGraphEditor) {
					msg += "complex graph pane.";
				}
				else if (graphPane instanceof ReactionNodeGraphEditor) {
					msg += "reaction graph pane.";
				}
				JOptionPane.showMessageDialog(graphPane,
				                              msg,
				                              "Error",
				                              JOptionPane.ERROR_MESSAGE);
				list.removeAll(events);
				return;
			}
		}
	}
	
	private void validateShortcutsForDB(java.util.List list) {
		// Get the list of all Renderable objects in the list.
		Renderable r = null;
		Map map = new HashMap();
		for (int i = 0; i < list.size(); i++) {
			r = (Renderable) list.get(i);
			if (r instanceof FlowLine) // Escape FlowLines
				continue;
			if (r instanceof Shortcut)
				continue;
			if (map.containsKey(r.getDisplayName())) {
				// Generate a shortcut
				Renderable target = (Renderable) map.get(r.getDisplayName());
				Renderable shortcut = (Renderable) target.generateShortcut();
				if (target != null) {
					shortcut.setID(r.getID());
					RenderUtility.switchRenderInfo(r, shortcut);
					list.set(i, shortcut);
				}
			}
			else {
				map.put(r.getDisplayName(), r);
				// Also need to check children
				if (r.getComponents() != null && r.getComponents().size() > 0)
					validateShortcutsForDB(r, map);
			}
		}
	}
	
	private void validateShortcutsForDB(Renderable r1, Map map) {
		// Get the list of all Renderable objects in the list.
		Renderable r = null;
		java.util.List list = r1.getComponents();
		for (int i = 0; i < list.size(); i++) {
			r = (Renderable) list.get(i);
			if (r instanceof FlowLine)
				continue; // Ignore FlowLine
			if (r instanceof Shortcut)
				continue;
			if (r instanceof RenderableReaction && r1 instanceof ReactionNode)
				continue; // Escape it
			if (map.containsKey(r.getDisplayName())) {
				// Generate a shortcut
				Renderable target = (Renderable) map.get(r.getDisplayName());
				Renderable shortcut = (Renderable) target.generateShortcut();
				if (shortcut != null) {
					shortcut.setID(r.getID());
					RenderUtility.switchRenderInfo(r, shortcut);
					list.set(i, shortcut);
				}
				continue; // Don't need go down to the children
			}
			else {
				map.put(r.getDisplayName(), r);
				// Also need to check children
				if (r.getComponents() != null && r.getComponents().size() > 0)
					validateShortcutsForDB(r, map);
			}
		}		
	}
	
	private void assignUniqueID(java.util.List list, Renderable container) {
		Renderable r = null;
		container = RenderUtility.getTopMostContainer(container);
		for (Iterator it = list.iterator(); it.hasNext();) {
			r = (Renderable) it.next();
			RenderableRegistry.getRegistry().assignUniqueID(r);
			java.util.List children = RenderUtility.getAllDescendents(r);
			for (Iterator it1 = children.iterator(); it1.hasNext();) {
				Renderable r1 = (Renderable) it1.next();
				RenderableRegistry.getRegistry().assignUniqueID(r1);
			}
		}		
	}
	
	private boolean checkDuplications(java.util.List list, GraphEditorPane editor) {
		// Get the list of all Renderable objects in the list.
		java.util.List wList = new ArrayList();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			wList.add(r);
			java.util.List children = RenderUtility.getAllDescendents(r);
			wList.removeAll(children); // To Remove duplication
			wList.addAll(children);
		}
		// Check if there is a duplication in the whole list
		boolean contains = false;
		for (Iterator it = wList.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			if(RenderableRegistry.getRegistry().contains(r.getDisplayName())) {
				contains = true;
				break;
			}
		}
		if (contains) {
            // As of Jan 11, 2006, the existing objects will be used always
            JOptionPane.showMessageDialog(editor,
                                          "Some objects already exist in the editing project.\n" +
                                          "Shortcuts to these existing objects will be created.",
                                          "Object Duplication Warning",
                                          JOptionPane.INFORMATION_MESSAGE);
//			Object[] choices = new String[]{"Replace Properties", "Keep Properties"};
//			String reply = (String) JOptionPane.showInputDialog(editor, "One or more objects already exist in the editing project." +
//			             				" Do you want to keep the \nproperties for these objects or use the properties from the database?", "Object Duplication",
//			             				JOptionPane.QUESTION_MESSAGE, null,
//			             				choices, "Replace Properties");
//			if (reply == null)
//				return false;
//			if (reply.startsWith("Replace")) {
//				// Do replacing
//				// Use for update GUIs
//				for (Iterator it = wList.iterator(); it.hasNext();) {
//					Renderable r = (Renderable) it.next();
//					String name = r.getDisplayName();
//					if (RenderableRegistry.getRegistry().contains(name)) {
//						Renderable target = RenderableRegistry.getRegistry().get(name);
//						RenderUtility.copyProperties(r, target);
//						GraphEditorActionEvent e = new GraphEditorActionEvent(target,
//						                                GraphEditorActionEvent.PROP_CHANGING);
//						editor.fireGraphEditorActionEvent(e);                               
//					}
//				}
//			}
			// Create shortcuts for those objects that exist in the editing process
			// But the creation should stop at the shortcut.
			java.util.List list1 = new ArrayList(list);
			java.util.List list2 = new ArrayList();
			while (list1.size() > 0) {
				for (Iterator it = list1.iterator(); it.hasNext();) {
					Renderable r = (Renderable)it.next();
					String name = r.getDisplayName();
					if (RenderableRegistry.getRegistry().contains(name)) {
						Renderable target = RenderableRegistry.getRegistry().getSingleObject(name);
						Renderable shortcut = (Renderable)target.generateShortcut();
						if (shortcut != null) {
							RenderUtility.switchRenderInfo(r, shortcut);
							Renderable container = r.getContainer();
							if (container != null) {
								container.removeComponent(r);
								container.addComponent(shortcut);
							}
							else { // It might be the top level objects
								int index = list.indexOf(r);
								if (index >= 0) {
									list.set(index, shortcut);
								}
							}
						}
					}
					else if (r.getComponents() != null && r.getComponents().size() > 0)
						list2.addAll(r.getComponents());
				}
				list1.clear();
				list1.addAll(list2);
				list2.clear();
			}
		}
		return true;
	}
	
	private void importDataAsNewInstance(ArrayList list, GraphEditorPane target) {
		// Make the names unique
		Renderable renderable = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
		    renderable = (Renderable) it.next();
		    if (renderable.getDisplayName() != null) {
		        String newName = "Clone of " + renderable.getDisplayName();
		        renderable.setDisplayName(newName);
		        // Make sure this name is valid
		        newName = RenderableRegistry.getRegistry().generateUniqueName(renderable);
		        renderable.setDisplayName(newName);
		    }
		    // Block DB_ID copy
		    renderable.setReactomeId(null);
		    if (renderable instanceof RenderableComplex) {
		        replaceComponentsWithShortcuts(renderable);
		    }
		}
		insertRenderabls(list, target);
	}
	
	private Renderable generateShortcut(Renderable r) {
	    Renderable shortcut = r.generateShortcut();
	    // Need to register to the displayed object
	    Renderable existed = RenderableRegistry.getRegistry().getSingleObject(r.getDisplayName());
	    if (existed != null) {
	        shortcut.setAttributes(existed.getAttributes());
	        List<Renderable> shortcuts = existed.getShortcuts();
	        if (shortcuts == null) {
	            shortcuts = new ArrayList<Renderable>();
	            shortcuts.add(existed);
	            existed.setShortcuts(shortcuts);
	        }
	        shortcuts.add(shortcut);
	        shortcut.setShortcuts(shortcuts);
	    }
	    return shortcut;
	}
	
	private void replaceComponentsWithShortcuts(Renderable container) {
	    List components = container.getComponents();
	    if (components == null || components.size() == 0)
	        return;
	    List<Renderable> shortcutComps = new ArrayList<Renderable>();
	    for (Iterator it = components.iterator(); it.hasNext();) {
	        Renderable comp = (Renderable) it.next();
	        Renderable shortcut = generateShortcut(comp);
	        shortcutComps.add(shortcut);
	        comp.setContainer(null);
	        it.remove();
	        shortcut.setContainer(container);
	        if (comp instanceof RenderableComplex) {
	            generateCompsForShortcut(comp, shortcut);
            }
	    }
	    // Need to call this method instead directly manipulate the list
	    for (Iterator it = components.iterator(); it.hasNext();) {
	        container.removeComponent((Renderable)it.next());
	    }
	    for (Renderable r : shortcutComps)
	        container.addComponent(r);
	}
	
	/**
	 * Validate the Shortcut Objects to make sure the targets to the Shortcuts
	 * contained in the list. Otherwise, use the targets instead of the shortcuts.
	 * However, the positions to the shortcuts should be used.
	 * @param list the list of Renderables to be validated
	 * @return a new List that might be changed
	 */
	private void validateShortcuts(java.util.List list) {
		Renderable renderable = null;
		Renderable target = null;
		ArrayList rtn = new ArrayList(list);
		Map<String, List<Renderable>> nameToObjects = new HashMap<String, List<Renderable>>();
		for (Iterator it = list.iterator(); it.hasNext();) {
			renderable = (Renderable) it.next();
			List<Renderable> objects = nameToObjects.get(renderable.getDisplayName());
			if (objects == null) {
			    objects = new ArrayList<Renderable>();
			    nameToObjects.put(renderable.getDisplayName(),
			                      objects);
			}
			objects.add(renderable);
		}
		// Make sure all objects having same name referrred to each other via shortcuts
		for (String name : nameToObjects.keySet()) {
		    List<Renderable> objects = nameToObjects.get(name);
		    if (objects.size() == 1)
		        continue;
		    List<Renderable> shortcuts = new ArrayList<Renderable>(objects);
		    for (Renderable r : objects) {
		        r.setShortcuts(shortcuts);
		    }
		}
	}
	
	/**
	 * Check the HyperEdges in the list and delete the ConnectWidgets that
	 * use Nodes not in the list.
	 */
	private void validateConnections(java.util.List list) {
		Renderable edge = null;
		Renderable node = null;
		Object obj = null;
		for (Iterator it = list.iterator(); it.hasNext();) {
			obj = it.next();
			if (obj instanceof Node) {
				node = (Renderable) obj;
				if (node.getConnectInfo() != null) {
					java.util.List widgets = node.getConnectInfo().getConnectWidgets();
					if (widgets != null && widgets.size() > 0) {
						for (Iterator it1 = widgets.iterator(); it1.hasNext();) {
							ConnectWidget widget = (ConnectWidget)it1.next();
							Renderable reaction = widget.getEdge();
							if (!list.contains(reaction)) {
								it1.remove();
								reaction.removeConnectWidget(widget);
							}
						}
					}
				}
			}
			else if (obj instanceof HyperEdge) {
				edge = (Renderable) obj;
				java.util.List widgets = edge.getConnectInfo().getConnectWidgets();
				if (widgets != null && widgets.size() > 0) {
					for (Iterator it1 = widgets.iterator(); it1.hasNext();) {
						ConnectWidget widget = (ConnectWidget) it1.next();
						Renderable node1 = widget.getConnectedNode();
						if (!list.contains(node1)) {
							it1.remove();
							node1.removeConnectWidget(widget);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Make sure no aliases are added to themselves.
	 * @param list
	 * @param target
	 * @return true for be able to adding.
	 */
	private boolean checkAncestorsForAliases(ArrayList list, GraphEditorPane target) {
		for (Iterator it = list.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			// Check display
			Renderable parent = target.getRenderable();
			while (parent != null) {
				if (parent.getDisplayName().equals(r.getDisplayName())) {
					JOptionPane.showMessageDialog(target,
                                                  "Instance \"" + r.getDisplayName() + "\" cannot be pasted as alias, because\n" +
                                                  "it cannot be inserted as its own descendant.",
                                                  "Error in Pasting",
                                                  JOptionPane.ERROR_MESSAGE);
					return false;
				}
				parent = parent.getContainer();
			}
		}
		return true;
	}
	
	private void importDataAsAlias(ArrayList list, 
	                               GraphEditorPane target) {
		// Make sure no Renderable objects are added to themselves, i.e., no
		// circles are allowed.
		if(!checkAncestorsForAliases(list, target))
			return;
		// Create a list of Shortcuts
		List<Renderable> insertList = new ArrayList<Renderable>(list.size());
		Renderable renderable = null;
		RenderableRegistry registry = RenderableRegistry.getRegistry();
		for (Iterator it = list.iterator(); it.hasNext();) {
			renderable = (Renderable) it.next();
			if (renderable instanceof HyperEdge) { // Don't create aliases for HyperEdges
				insertList.add(renderable);
			}
			else {
			    if (registry.contains(renderable.getDisplayName())) {
			        Renderable shortcut = generateShortcut(renderable);
			        insertList.add(shortcut);
			        if (renderable instanceof RenderableComplex) {
                        generateCompsForShortcut(renderable, shortcut);
                    }
			    }
			    else
			        insertList.add(renderable);
			}
		}
		insertRenderabls(insertList, target);
	}

    private void generateCompsForShortcut(Renderable renderable,
                                          Renderable shortcut) {
        replaceComponentsWithShortcuts(renderable);
        // Make a copy of complex components
        if (renderable.getComponents() != null) {
            for (Iterator it1 = renderable.getComponents().iterator(); it1.hasNext();) {
                Renderable tmp = (Renderable) it1.next();
                shortcut.addComponent(tmp);
                tmp.setContainer(shortcut);
            }
        }
    }
	
	private void insertRenderabls(java.util.List list, 
	                              GraphEditorPane target) {
	    // Just in case
	    if (list == null || list.size() == 0)
	        return;
		// A little shift to make the imported objects to be seen easily.
		Point defaultInsertPos = target.defaultInsertPos;
		Renderable renderable = (Renderable) list.get(0);
		Point pos = renderable.getPosition();
		int dx = defaultInsertPos.x - pos.x;
		int dy = defaultInsertPos.y - pos.y;
		RenderableRegistry registry = RenderableRegistry.getRegistry();
		for (Iterator it = list.iterator(); it.hasNext();) {
			renderable = (Renderable)it.next();
			pos = renderable.getPosition();
			renderable.move(dx, dy);
			if (renderable instanceof Node) {
                Set<Renderable> allComponents = RenderUtility.getAllContainedComponents(renderable);
                // Call to add to the displayed components directly to avoid
                // checking the overlay relationships.
                Renderable container = target.getRenderable();
                for (Renderable r : allComponents) {
                    registry.add(r);
                    registry.assignUniqueID(r);
                    container.addComponent(r);
                    // Don't set container.
                }
			    target.insertNode((Node)renderable);
				registry.add(renderable);
				// Objects from import will bypass the constructor so that
	            // unique ids are duplicated.
	            registry.assignUniqueID(renderable);
			}
			else if (renderable instanceof HyperEdge) {
				target.insertEdge((HyperEdge)renderable, false);
				registry.add(renderable);
				registry.assignUniqueID(renderable);
			}
		}
		target.setSelection(list);
		target.revalidate();
		target.repaint(target.getVisibleRect());
		target.updateDefaultInsertPos();
	}

	protected void exportDone(JComponent c, Transferable data, int action) {
		if (action == MOVE &&
		    (c instanceof GraphEditorPane)) {
		    GraphEditorPane graphPane = (GraphEditorPane) c;
			graphPane.deleteSelection();
			graphPane.repaint(graphPane.getVisibleRect());
		}
	}

	private boolean hasSerialArrayListFlavor(DataFlavor[] flavors) {
		if (serialArrayListFlavor == null) {
			return false;
		}

		for (int i = 0; i < flavors.length; i++) {
			if (flavors[i].equals(serialArrayListFlavor)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean canImport(JComponent c, DataFlavor[] flavors) {
		if (hasSerialArrayListFlavor(flavors)) { 
			return true; 
		}
		return false;
	}

	protected Transferable createTransferable(JComponent c) {
		if (c instanceof GraphEditorPane) {
			GraphEditorPane graphPane = (GraphEditorPane) c;
			List selection = graphPane.getSelection();
			// Need to fitler out those that should not be copied
			ArrayList<Object> list = new ArrayList<Object>();
			for (Iterator it = selection.iterator(); it.hasNext();) {
			    Renderable r = (Renderable) it.next();
			    if (r.isTransferrable())
			        list.add(r);
			}
			list.add(0, "graphPane");
			return new ArrayListTransferable(list);
		}
		return null;
	}

	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}
	
	public void setIsForAliase(boolean isForAliase) {
		this.isForAliase = isForAliase;
	}
	
	public boolean isForAliase() {
		return this.isForAliase;
	}
}
