/*
 * Created on Mar 12, 2004
 */
package org.gk.pathView;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.gk.database.FrameManager;
import org.gk.database.InstanceListPane;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.AuthorToolAppletUtilities;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * A collection of actions used in the GK visualization tool.
 * @author wugm
 */
public class VisualizationToolActions {
    private GKVisualizationPane tool;
	// Actions
	private Action parallelArrowVerticalAction;
	private Action parallelArrowHorizontalAction;
	private Action arrangeVerticalAction;
	private Action arrangeHorizontalAction;
	private Action arrangeVerticalLineAction;
	private Action arrangeHorizontalLineAction;
	private Action assignLengthAction;
	private Action selectAllAction;
	private Action overallViewAction;
	private Action undoAction;
	private Action redoAction;
	private Action saveAction;
	private Action compareDBAction;
	private Action closeCompareDBAction;
    private Action compareReactionsInSkyAction;
	private Action searchAction;
	private Action loadSelectedAction;
	private Action loadSelectedAndConnectedAction;
	private Action unloadReactionAction;
	private Action fixSelectedAction;
	private Action fixAllAction;
	private Action releaseSelectedAction;
	private Action releaseAllAction;
	private Action deleteAction;
	private Action exportSVGAction;
	private Action viewInstanceAction;
	// For synchronizing selection
	private PropertyChangeListener selectionListener;
	private ComparableBrowserPane compareBrowserPane;
	private TreeSelectionListener compTreeSelectionL;

	public VisualizationToolActions(GKVisualizationPane tool) {
		this.tool = tool;
	}
	
	public Action getExportSVGAction() {
	    if (exportSVGAction == null) {
	        exportSVGAction = new AbstractAction("Export to SVG",
	                                             AuthorToolAppletUtilities.createImageIcon("Export16.gif")) {
	            public void actionPerformed(ActionEvent e) {
	                exportSVG();
	            }
	        };
	        exportSVGAction.putValue(Action.SHORT_DESCRIPTION, "Export to SVG");
	    }
	    return exportSVGAction;
	}
	
	private void exportSVG() {
		if (tool == null)
			return;
		// Get the output file name
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Choose a file for svg exporting...");
		int reply = fileChooser.showSaveDialog(tool);
		if (reply != JFileChooser.APPROVE_OPTION)
			return;
		File file = fileChooser.getSelectedFile();
		// Get a DOMImplementation
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		// Create an instance of org.w3c.dom.Document
		Document document = domImpl.createDocument(null, "svg", null);

		// Create an instance of the SVG Generator
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		// Ask the test to render into the SVG Graphics2D implementation
		tool.paint(svgGenerator);

		// Finally, stream out SVG to the standard output using UTF-8
		// character to byte encoding
		try {
			FileOutputStream fos = new FileOutputStream(file);
			Writer out = new OutputStreamWriter(fos, "UTF-8");
			svgGenerator.stream(out, false);
			fos.close();
			out.close();
		}
		catch (Exception e) {
			System.err.println("VisualizationMenuBar.exportSVG(): " + e);
			e.printStackTrace();
		}
	}
	
	public Action getDeleteAction() {
		if (deleteAction == null) {
			deleteAction = new AbstractAction("Delete") {
				public void actionPerformed(ActionEvent e) {
					tool.delete();
				}
			};
		}
		return deleteAction;
	}
	
	public Action getLoadSelectedAction() {
		if (loadSelectedAction == null) {
			loadSelectedAction = new AbstractAction("Load Selected",
			                                        AuthorToolAppletUtilities.createImageIcon("LoadSelected.gif")) {
				public void actionPerformed(ActionEvent e) {
					tool.loadSelected();
				}
			};
			loadSelectedAction.putValue(Action.SHORT_DESCRIPTION, "Load selected reactions in tree");
		}
		return loadSelectedAction;
	}
	
	public Action getLoadSelectedAndConnectedAction() {
		if (loadSelectedAndConnectedAction == null) {
			loadSelectedAndConnectedAction = new AbstractAction("Load Selected and Connected",
			        AuthorToolAppletUtilities.createImageIcon("LoadSelectedConnected.gif")) {
				public void actionPerformed(ActionEvent e) {
					tool.loadSelectedAndConnected();
				}
			};
			loadSelectedAndConnectedAction.putValue(Action.SHORT_DESCRIPTION, "Load selected and connected reactions");
		}
		return loadSelectedAndConnectedAction;
	}
	
	public Action getUnloadReactionAction() {
		if (unloadReactionAction == null) {
			unloadReactionAction = new AbstractAction("Unload Reactions") {
				public void actionPerformed(ActionEvent e) {
					tool.unloadReactions();
				}
			};
		}
		return unloadReactionAction;
	}
	
	public Action getFixSelectedAction() {
		if (fixSelectedAction == null) {
			fixSelectedAction = new AbstractAction("Fix Selected") {
				public void actionPerformed(ActionEvent e) {
					tool.fixedSelected();
				}
			};
		}
		return fixSelectedAction;
	}
	
	public Action getFixAllAction() {
		if (fixAllAction == null) {
			fixAllAction = new AbstractAction("Fix All") {
				public void actionPerformed(ActionEvent e) {
					tool.fixAll();
				}
			};
		}
		return fixAllAction;
	}
	
	public Action getReleaseSelectedAction() {
		if (releaseSelectedAction == null) {
			releaseSelectedAction = new AbstractAction("Release Selected") {
				public void actionPerformed(ActionEvent e) {
					tool.releaseSelected();
				}
			};
		}
		return releaseSelectedAction;
	}
	
	public Action getReleaseAllAction() {
		if (releaseAllAction == null) {
			releaseAllAction = new AbstractAction("Release All") {
				public void actionPerformed(ActionEvent e) {
					tool.releaseAll();
				}
			};
		}
		return releaseAllAction;
	}
	
	public Action getSearchAction() {
		if (searchAction == null) {
			searchAction = new AbstractAction("Find") {
				public void actionPerformed(ActionEvent e) {
					search();
				}
			};
		}
		return searchAction;
	}
	
	private void search() {
		// Get the search key
		JFrame parentFrame = (JFrame)SwingUtilities.getRoot(tool);
		String input = JOptionPane.showInputDialog(parentFrame,
		                                           "Please input an event name exactly:");
		if (input == null || input.trim().length() == 0)
			return;
		if (compareBrowserPane != null) {
			compareBrowserPane.search(input.trim());
		}
		else {
			tool.getHierarchyPanel().search(input.trim());
		}
	}
	
	public Action getCloseCompareDBAction() {
		if (closeCompareDBAction == null) {
			closeCompareDBAction = new AbstractAction("Close Comparison") {
				public void actionPerformed(ActionEvent e) {
					closeDBComparison();
				}
			};
		}
		return closeCompareDBAction;
	}
	
	private void closeDBComparison() {
		if (compareBrowserPane == null)
			return; // Already removed.
		PathwayBrowserPanel panel = tool.getHierarchyPanel();
		int pos = tool.jsp.getDividerLocation();
		tool.jsp.setBottomComponent(panel);
		tool.jsp.setDividerLocation(pos);
		compareBrowserPane = null; 
		tool.removePropertyChangeListener(selectionListener);
		selectionListener = null;
		tool.clearSelection();
	}
	
	public Action getCompareDBAction() {
		if (compareDBAction == null) {
			compareDBAction = new AbstractAction("Compare to Another DB") {
				public void actionPerformed(ActionEvent e) {
					compareToAnotherDB();
				}
			};
		}
		return compareDBAction;
	}
  
  public Action getCompareReactionsInSkyAction() {
    if (compareReactionsInSkyAction == null) {
      compareReactionsInSkyAction = new AbstractAction("Compare Reactions in Sky") {
        public void actionPerformed(ActionEvent e) {
          compareReactionsInSky();
        }
      };
    }
    return compareReactionsInSkyAction;
  }
  
  private void compareReactionsInSky() {
    // Get newly added reactions
    final InstanceListPane listPane = new InstanceListPane();
    listPane.addSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        List selected = listPane.getSelection();
        if (selected != null && selected.size() > 0) {
          tool.hierarchyPanel.highliteNodes(selected);
        }
      }
    });
    // To avoid duplicate a set is used
    ComparingReactionsInSkyModel model = new ComparingReactionsInSkyModel();
    model.setMySQLAdapter(tool.getDba());
    List newReactions = getNewReactions(model);
    InstanceUtilities.sortInstances(newReactions);
    listPane.setDisplayedInstances(newReactions);
    listPane.setTitle("New Reactions in Released Pathways: " + newReactions.size());
    PathwayBrowserPanel panel = tool.getHierarchyPanel();
    panel.setSplitPaneOrientation(JSplitPane.VERTICAL_SPLIT);
    JSplitPane jsp1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                     panel, 
                                     listPane);
    jsp1.setOneTouchExpandable(true);
    // Calculate the divider position
    int w = tool.jsp.getWidth();
    int h = tool.jsp.getHeight();
    jsp1.setDividerLocation((int)(w * 0.5));
    int pos = tool.jsp.getDividerLocation();
    tool.jsp.setBottomComponent(jsp1);
    tool.jsp.setDividerLocation(pos);
  }
  
  /**
   * This method should be refactored to ComparingReactionsInSkyModel class.
   * However, it will involve a lot of change in both PathwayBrowserPane and
   * GKVisualizationPane.
   * @return
   */
    private List getNewReactions(ComparingReactionsInSkyModel model) {
        List reactions = model.getReactionsInSky();
        // Check if reactions is loaded
        GKInstance rxn = null;
        List rtn = new ArrayList();
        for (Iterator it = reactions.iterator(); it.hasNext();) {
            rxn = (GKInstance) it.next();
            if (!tool.isReactionLoaded(rxn))
                rtn.add(rxn);
        }
        return rtn;
    }
  
	private void compareToAnotherDB() {
		// Popup another connection window
		Properties prop = new Properties();
		MySQLAdaptor dba = tool.getDba();
		prop.setProperty("dbHost", dba.getDBHost());
		//prop.setProperty("dbName", dba.getDBName());
		prop.setProperty("dbUser", dba.getDBUser());
		prop.setProperty("dbPort", 3306 + "");
		DBConnectionPane connectionPane = new DBConnectionPane();
		connectionPane.setValues(prop);
		if (connectionPane.showInDialog(tool)) {
			String dbHost = prop.getProperty("dbHost");
			String dbName = prop.getProperty("dbName");
			String dbUser = prop.getProperty("dbUser");
			String dbPwd = prop.getProperty("dbPwd");
			String dbPort = prop.getProperty("dbPort");
			try {
				MySQLAdaptor oldAdaptor = new MySQLAdaptor(dbHost,
				                                           dbName,
				                                           dbUser,
				                                           dbPwd, 
				                                           Integer.parseInt(dbPort));
				if (compareBrowserPane != null)
					closeDBComparison();
				compareBrowserPane = new ComparableBrowserPane();
				compareBrowserPane.setMySQLAdaptors(oldAdaptor, tool.getDba());
				// Have to make tree building explicitly
				compareBrowserPane.oldPane.setMySQLAdaptor(oldAdaptor, true);
				buildTree(compareBrowserPane.newPane);
				int pos = tool.jsp.getDividerLocation();
				tool.jsp.setBottomComponent(compareBrowserPane);
				tool.jsp.setDividerLocation(pos);
				// To synchronize the selections
				selectionListener = new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent e) {
						String propName = e.getPropertyName();
						if (propName.equals("selection") && tool.synchronizingEdgeSelection) {
							compareBrowserPane.removeSelectionListener(compTreeSelectionL);
							Collection selected = (Collection) e.getNewValue();
							compareBrowserPane.select(selected);
							compareBrowserPane.addSelectionListener(compTreeSelectionL);
						}
					}
				};
				tool.addPropertyChangeListener(selectionListener);
				// From tree to pane
				if (compTreeSelectionL == null) {
					compTreeSelectionL = new TreeSelectionListener() {
						public void valueChanged(TreeSelectionEvent e) {
							tool.handleTreeSelection(e, true);
						}
					};
				}
				compareBrowserPane.addSelectionListener(compTreeSelectionL);
				tool.clearSelection();
			}
			catch (SQLException e) {
				System.err.println("visualization.compareToAnotherDB(): " + e);
				e.printStackTrace();
			}
		}
	}
	
	private void buildTree(PathwayBrowserPanel browserPane) {
		// Get the top level pathways
		DefaultTreeModel model = (DefaultTreeModel) tool.getHierarchyPanel().getEventTree().getModel(); 
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		java.util.List topEvents = new ArrayList(root.getChildCount());
		DefaultMutableTreeNode treeNode = null;
		for (int i = 0; i < root.getChildCount(); i++) {
			treeNode = (DefaultMutableTreeNode) root.getChildAt(i);
			topEvents.add(treeNode.getUserObject());
		}
		try {
			browserPane.buildTree(topEvents);
		}
		catch(Exception e) {
			System.err.println("VisualizationToolActions.buildTree(): " + e);
			e.printStackTrace();
		}
	}
	
	public Action getSaveAction() {
		if (saveAction == null) {
			saveAction = new AbstractAction("Store Locations",
			                                AuthorToolAppletUtilities.createImageIcon("Save16.gif")) {
				public void actionPerformed(ActionEvent e) {
					tool.storeLocations();
				}
			};
			saveAction.putValue(Action.SHORT_DESCRIPTION, "Store reaction locations to DB");
		}
		return saveAction;
	}
	
	public void initUndoActions(boolean enabled) {
		undoAction.setEnabled(enabled);
		redoAction.setEnabled(false);
	}
	
	public Action getUndoAction() {
		if (undoAction == null) {
			undoAction = new AbstractAction("Undo",
			        AuthorToolAppletUtilities.createImageIcon("Undo16.gif")) {
				public void actionPerformed(ActionEvent e) {
					try {
						tool.moveEdit.undo();
						undoAction.setEnabled(false);
						redoAction.setEnabled(true);
					}
					catch(CannotUndoException e1) {
						undoAction.setEnabled(false);
						redoAction.setEnabled(false);
						System.err.println("VisualizationToolActions.getUndoAction(): " + e1);
						e1.printStackTrace();
					}
				}
			};
			undoAction.putValue(Action.SHORT_DESCRIPTION, "Undo16.gif");
			undoAction.setEnabled(false);
		}
		return undoAction;
	}
	
	public Action getRedoAction() {
		if (redoAction == null) {
			redoAction = new AbstractAction("Redo",
			                AuthorToolAppletUtilities.createImageIcon("Redo16.gif")) {
				public void actionPerformed(ActionEvent e) {
					try {
						tool.moveEdit.redo();
						redoAction.setEnabled(false);
						undoAction.setEnabled(true);
					}
					catch(CannotRedoException e1) {
						undoAction.setEnabled(false);
						redoAction.setEnabled(false);
						System.err.println("VisualiztionToolActions.getRedoAction(): " + e1);
						e1.printStackTrace();
					}
				}
			};
			redoAction.putValue(Action.SHORT_DESCRIPTION, "Redo");
			redoAction.setEnabled(false);
		}
		return redoAction;
	}
	
	public Action getOverallViewAction() {
		if (overallViewAction == null) {
			overallViewAction = new AbstractAction("Show Overall View") {
				public void actionPerformed(ActionEvent e) {
					String text = (String) overallViewAction.getValue(Action.NAME);
					if (text.startsWith("Show")) {
						overallViewAction.putValue(Action.NAME, "Hide Bird View");
						tool.overallPane.showView(tool.frame);
						Rectangle viewRect = new Rectangle(tool.viewport.getViewRect());
						double scale = tool.MAGNIFICATION;
						viewRect.x /= scale;
						viewRect.y /= scale;
						viewRect.width /= scale;
						viewRect.height /= scale;
						tool.overallPane.setVisibleRect(viewRect);
					}
					else {
						overallViewAction.putValue(Action.NAME, "Show Bird View");
						tool.overallPane.hideView();
					}
				}
			};
		}
		return overallViewAction;
	}
	
	public Action getSelectAllAction() {
		if (selectAllAction == null) {
			selectAllAction = new AbstractAction("Select All") {
				public void actionPerformed(ActionEvent e) {
					tool.selectAll();
				}
			};
		}
		return selectAllAction;
	}
	
	public void doPopup(MouseEvent e) {
		JPopupMenu popup = new JPopupMenu();
		popup.add(getArrangeVerticalLineAction());
		popup.add(getArrangeHorizontalLineAction());
		popup.addSeparator();
		popup.add(getParallelArrowVerticalAction());
		popup.add(getParallelArrowHorizontalAction());
		popup.addSeparator();
		popup.add(getArrangeVerticalAction());
		popup.add(getArrangeHorizontalAction());
		popup.addSeparator();
		popup.add(getAssignLengthAction());
		Component comp = (Component) e.getSource();
		java.util.List edges = tool.getSelectedEdges();
        if (edges != null && edges.size() == 1) {
            final IEdge edge = (IEdge) edges.get(0);
            if (edge.getType() == IEdge.REACTION_EDGE) {
        		popup.addSeparator();
        		popup.add(getViewInstanceAction());
            }
        }
		popup.show(comp, e.getX(), e.getY());
	}
	
	public Action getViewInstanceAction() {
	    if (viewInstanceAction == null) {
	        viewInstanceAction = new AbstractAction("View Instance",
	                				                AuthorToolAppletUtilities.createImageIcon("ViewInstance.gif")) {
	            public void actionPerformed(ActionEvent e) {
	                java.util.List edges = tool.getSelectedEdges();
	                if (edges == null || edges.size() != 1)
	                    return;
	                IEdge edge = (IEdge) edges.get(0);
	                if (edge.getType() == IEdge.REACTION_EDGE) {
	                    GKInstance reaction = (GKInstance) edge.getUserObject();
	                    FrameManager.getManager().showInstance(reaction);
	                }
	            }
	        };
	        //viewInstanceAction.putValue(Action.SHORT_DESCRIPTION, "View Instance");
	    }
	    return viewInstanceAction;
	}
	
	public Action getParallelArrowVerticalAction() {
		if (parallelArrowVerticalAction == null) {
			parallelArrowVerticalAction = new AbstractAction("Parallel Arrows Vertically",
			        AuthorToolAppletUtilities.createImageIcon("ParallelArrowsVertically.gif")) {
				// Make all selected arrows vertical paralleling.
				public void actionPerformed(ActionEvent e) {
					java.util.List edges = tool.getSelectedEdges();
					if (edges.size() == 0)
						return;
					tool.moveEdit.storeOldPositions(edges);
					parallelEdgeVertically(edges);
					tool.moveEdit.storeNewPositions(edges);
					initUndoActions(true);
					tool.repaint(tool.getVisibleRect());
					tool.isDirty = true;
				}
			};
			parallelArrowVerticalAction.putValue(Action.SHORT_DESCRIPTION,
			                                     "Parallel reactions vertically");
		}
		return parallelArrowVerticalAction;
	}

	private void parallelEdgeVertically(java.util.List edges) {
		IEdge edge = null;
		int length = 0;
		// Have to determine the direction: Up or down based the first edge
		int upNumber = 0;
		int downNumber = 0;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			if ((edge.getHeadY() - edge.getTailY()) < 0)
				upNumber ++;
			else
				downNumber ++;
		}
		boolean isUp;
		if (upNumber > downNumber)
			isUp = true;
		else
			isUp = false;
		if (isUp) {
			for (Iterator it = edges.iterator(); it.hasNext();) {
				edge = (IEdge)it.next();
				length = edge.length();
				edge.setHead(edge.getTailX(), edge.getTailY() - length);
			}
		}
		else {
			for (Iterator it = edges.iterator(); it.hasNext();) {
				edge = (IEdge) it.next();
				length = edge.length();
				edge.setHead(edge.getTailX(), edge.getTailY() + length);
			}
		}
	}


	public Action getArrangeHorizontalAction() {
		if (arrangeHorizontalAction == null) {
			arrangeHorizontalAction = new AbstractAction("Arrange Horizontally",
			        AuthorToolAppletUtilities.createImageIcon("ArrangeHorizontally.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List edges = tool.getSelectedEdges();
					if (edges.size() == 0)
						return;
					tool.moveEdit.storeOldPositions(edges);
					arrangeEdgeHorizontally(edges);
					tool.moveEdit.storeNewPositions(edges);
					initUndoActions(true);
					tool.repaint(tool.getVisibleRect());
					tool.isDirty = true;
				}
			};
			arrangeHorizontalAction.putValue(Action.SHORT_DESCRIPTION,
			                                 "Arrange reactions horizontally");
		}
		return arrangeHorizontalAction;
	}
	
	private void arrangeEdgeHorizontally(java.util.List edges) {
		IEdge edge = null;
		// Have to determine the middle point
		int totalY = 0;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge)it.next();
			totalY += (edge.getHeadY() + edge.getTailY()) / 2;
		}
		int midY = totalY / edges.size();
		int y;
		int dy;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge)it.next();
			y = (edge.getHeadY() + edge.getTailY()) / 2;
			dy = midY - y;
			edge.translate(0, dy);
		}		
	}

	public Action getArrangeHorizontalLineAction() {
		if (arrangeHorizontalLineAction == null) {
			arrangeHorizontalLineAction = new AbstractAction("Arrange on Horizontal Line",
			        AuthorToolAppletUtilities.createImageIcon("ArrangeHorizontalLine.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List edges = tool.getSelectedEdges();
					if (edges.size() == 0)
						return;
					tool.moveEdit.storeOldPositions(edges);
					parallelEdgeHorizontally(edges);
					tool.moveEdit.storeNewPositions(edges);
					initUndoActions(true);
					arrangeEdgeHorizontally(edges);
					tool.repaint(tool.getVisibleRect());
					tool.isDirty = true;
				}
			};
			arrangeHorizontalLineAction.putValue(Action.SHORT_DESCRIPTION, "Arrange on a horizontal line");
		}
		return arrangeHorizontalLineAction;
	}

	public Action getArrangeVerticalAction() {
		if (arrangeVerticalAction == null) {
			arrangeVerticalAction = new AbstractAction("Arrange Vertically",
			        AuthorToolAppletUtilities.createImageIcon("ArrangeVertically.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List edges = tool.getSelectedEdges();
					if (edges.size() == 0)
						return;
					tool.moveEdit.storeOldPositions(edges);
					arrangeEdgeVertically(edges);
					tool.moveEdit.storeNewPositions(edges);
					initUndoActions(true);
					tool.repaint(tool.getVisibleRect());
					tool.isDirty = true;
				}
			};
			arrangeVerticalAction.putValue(Action.SHORT_DESCRIPTION, "Arrange reactions vertically");
		}
		return arrangeVerticalAction;
	}

	private void arrangeEdgeVertically(java.util.List edges) {
		IEdge edge = null;
		// Have to determine the middle point
		int totalX = 0;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			totalX += (edge.getHeadX() + edge.getTailX()) / 2;
		}
		int midX = totalX / edges.size();
		int x;
		int dx;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			x = (edge.getHeadX() + edge.getTailX()) / 2;
			dx = midX - x;
			edge.translate(dx, 0);
		}
	}

	public Action getArrangeVerticalLineAction() {
		if (arrangeVerticalLineAction == null) {
			arrangeVerticalLineAction = new AbstractAction("Arrange on Vertical Line",
			        AuthorToolAppletUtilities.createImageIcon("ArrangeVerticalLine.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List edges = tool.getSelectedEdges();
					if (edges.size() == 0)
						return;
					tool.moveEdit.storeOldPositions(edges);
					parallelEdgeVertically(edges);
					arrangeEdgeVertically(edges);
					tool.moveEdit.storeNewPositions(edges);
					initUndoActions(true);
					tool.repaint(tool.getVisibleRect());
					tool.isDirty = true;
				}
			};
			arrangeVerticalLineAction.putValue(Action.SHORT_DESCRIPTION, "Arrange on a vertical line");
		}
		return arrangeVerticalLineAction;
	}

	public Action getAssignLengthAction() {
		if (assignLengthAction == null) {
			assignLengthAction = new AbstractAction("Assign Same Length",
			        AuthorToolAppletUtilities.createImageIcon("AssignLength.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List edges = tool.getSelectedEdges();
					if (edges.size() == 0)
						return;
					tool.moveEdit.storeOldPositions(edges);
					assignEdgeLength(edges);
					tool.moveEdit.storeNewPositions(edges);
					initUndoActions(true);
					tool.repaint(tool.getVisibleRect());
					tool.isDirty = true;
				}
			};
			assignLengthAction.putValue(Action.SHORT_DESCRIPTION,
			                            "Assign same length to selected reactions");
		}
		return assignLengthAction;
	}
	
	private void assignEdgeLength(java.util.List edges) {
		// Get the average
		Map lengthMap = new HashMap();
		IEdge edge = null;
		int total = 0;
		int len = 0;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			len = edge.length();
			lengthMap.put(edge, new Integer(len));
			total += len;
		}
		int average = (int)((double) total / edges.size());
		String input = JOptionPane.showInputDialog(tool, "Please input an edge length", average + "");
		if (input != null) {
			int l = 0;
			try {
				l = Integer.parseInt(input);
			}
			catch(NumberFormatException e) {
				JOptionPane.showMessageDialog(tool, "Edge lenght should be a positive integer",
				                              "Error in Edge Lenght", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (l <= 0) {
				JOptionPane.showMessageDialog(tool, "Edge lenght should be a positive integer",
											  "Error in Edge Lenght", JOptionPane.ERROR_MESSAGE);
				return;				
			}
			double ratio;
			int x, y;
			for (Iterator it = edges.iterator(); it.hasNext();) {
				edge = (IEdge) it.next();
				len = ((Integer) lengthMap.get(edge)).intValue();
				ratio = (double) l / len;
				x = (int)(ratio * (edge.getHeadX() - edge.getTailX())) + edge.getTailX();
				y = (int)(ratio * (edge.getHeadY() - edge.getTailY())) + edge.getTailY();
				edge.setHead(x, y);
			}
		}
	}

	public Action getParallelArrowHorizontalAction() {
		if (parallelArrowHorizontalAction == null) {
			parallelArrowHorizontalAction = new AbstractAction("Parallel Arrows Horizontally",
			        AuthorToolAppletUtilities.createImageIcon("ParallelArrowsHorizontally.gif")) {
				public void actionPerformed(ActionEvent e) {
					java.util.List edges = tool.getSelectedEdges();
					if (edges.size() == 0)
						return;
					tool.moveEdit.storeOldPositions(edges);
					parallelEdgeHorizontally(edges);
					tool.moveEdit.storeNewPositions(edges);
					initUndoActions(true);
					tool.repaint(tool.getVisibleRect());
					tool.isDirty = true;
				}
			};
			parallelArrowHorizontalAction.putValue(Action.SHORT_DESCRIPTION,
			                                      "Parallel reactions horizontally");
		}
		return parallelArrowHorizontalAction;
	}

	private void parallelEdgeHorizontally(java.util.List edges) {
		IEdge edge = null;
		int length = 0;
		// Vote if left or right
		int leftNumber = 0;
		int rightNumber = 0;
		for (Iterator it = edges.iterator(); it.hasNext();) {
			edge = (IEdge) it.next();
			if ((edge.getHeadX() - edge.getTailX()) < 0)
				rightNumber ++;
			else
				leftNumber ++;
		}
		boolean isLeft;
		if (rightNumber > leftNumber)
			isLeft = false;
		else
			isLeft = true;
		// Point to left or right
		if (isLeft) {
			for (Iterator it = edges.iterator(); it.hasNext();) {
				edge = (IEdge) it.next();
				length = edge.length();
				edge.setHead(edge.getTailX() + length, edge.getTailY());
			}
		}
		else {
			for (Iterator it = edges.iterator(); it.hasNext();) {
				edge = (IEdge) it.next();
				length = edge.length();
				edge.setHead(edge.getTailX() - length, edge.getTailY());
			}
		}
	}
	
	public GKVisualizationPane getToolPane() {
	    return this.tool;
	}
}
