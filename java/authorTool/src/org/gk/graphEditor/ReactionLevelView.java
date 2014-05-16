/*
 * Created on Mar 8, 2004
 */
package org.gk.graphEditor;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.gk.render.FlowLine;
import org.gk.render.Node;
import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntity;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.render.Shortcut;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.GraphLayoutEngine;

/**
 * An event level view for RenderablePathway objects.
 * @author wugm
 */
public class ReactionLevelView extends PathwayEditor {
	// The displayed RenderablePathway.
	private RenderablePathway pathway;

	public ReactionLevelView() {
		setEditable(false); // Just a view
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				requestFocus();
				if (e.isPopupTrigger()) {
					doPopup(e);
					return;
				}
				if (e.getClickCount() == 2)
					displaySelected();
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doPopup(e);
			}
		});
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_A &&
				    (e.getModifiers() & InputEvent.CTRL_MASK) > 0) {
					selectAll();
					repaint(getVisibleRect());
				}
			}
		});
	}
	
	public ReactionLevelView(RenderablePathway pathway) {
		this();
		setPathway(pathway);
	}
	
	private void doPopup(MouseEvent e) {
		ActionListener layoutActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (cmd.equals("hierarchicalLayout")) {
					layoutPathway(GraphLayoutEngine.HIERARCHICAL_LAYOUT);
				}
				else if (cmd.equals("forceLayout")) {
					layoutPathway(GraphLayoutEngine.FORCE_DIRECTED_LAYOUT);
				}
//				else if (cmd.equals("circularLayout")) {
//					layoutPathway(GraphLayoutEngine.CIRCULAR_LAYOUT);
//				}
			}
		};
		
		ActionListener zoomActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (cmd.equals("zoomIn")) {
					zoom(scaleX * 2, scaleY * 2);
				}
				else if (cmd.equals("zoomOut")) {
					zoom(scaleX / 2.0, scaleY / 2.0);
				}
				else if (cmd.equals("zoomToFit")) {
					zoomToFit();
				}
				else if (cmd.equals("unZoom")) {
					zoom(1, 1);
				}
			}
		};
		JPopupMenu popup = new JPopupMenu();
		java.util.List selection = getSelection();
		if (selection.size() == 0) {
			if (GKApplicationUtilities.isDeployed) {
				JMenuItem layoutItem = new JMenuItem("Automatic Layout");
				layoutItem.setActionCommand("hierarchicalLayout");
				layoutItem.addActionListener(layoutActionListener);
				popup.add(layoutItem);
			}
			else {
				JMenuItem hierarchicalLayoutItem = new JMenuItem("Hierarchical Layout");
				hierarchicalLayoutItem.setActionCommand("hierarchicalLayout");
				hierarchicalLayoutItem.addActionListener(layoutActionListener);
				popup.add(hierarchicalLayoutItem);
				JMenuItem forceLayoutItem = new JMenuItem("Force Directed Layout");
				forceLayoutItem.setActionCommand("forceLayout");
				forceLayoutItem.addActionListener(layoutActionListener);
				popup.add(forceLayoutItem);
			}
			// Don't support circular layout. More documents are needed.
			//		JMenuItem circularLayoutItem = new JMenuItem("Circular Layout");
			//		circularLayoutItem.setActionCommand("circularLayout");
			//		circularLayoutItem.addActionListener(layoutActionListener);
			//		popup.add(circularLayoutItem);
			popup.addSeparator();
			JMenuItem zoomInItem = new JMenuItem("Zoom In");
			zoomInItem.setActionCommand("zoomIn");
			zoomInItem.addActionListener(zoomActionListener);
			popup.add(zoomInItem);
			JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
			zoomOutItem.setActionCommand("zoomOut");
			zoomOutItem.addActionListener(zoomActionListener);
			popup.add(zoomOutItem);
			JMenuItem zoomToFitItem = new JMenuItem("Zoom to Fit");
			zoomToFitItem.setActionCommand("zoomToFit");
			zoomToFitItem.addActionListener(zoomActionListener);
			popup.add(zoomToFitItem);
			JMenuItem unZoomItem = new JMenuItem("Unzoom");
			unZoomItem.setActionCommand("unZoom");
			unZoomItem.addActionListener(zoomActionListener);
			popup.add(unZoomItem);
			popup.show(this, e.getX(), e.getY());
		}
		else if (selection.size() == 1) {
			Renderable r = (Renderable) selection.get(0);
			if (r instanceof FlowLine || r instanceof RenderableEntity)
				return;
			JMenuItem openItem = new JMenuItem("Open");
			openItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					displaySelected();
				}
			});
			popup.add(openItem);
			popup.show(this, e.getX(), e.getY());
		}
	}
	
	protected void displaySelected() {
		if (getSelection().size() != 1)
			return;
		Object obj = getSelection().get(0);
		if (obj instanceof FlowLine)
			return;
		ReactionNode node = (ReactionNode) obj;
		displayReaction(node.getReaction());
	}
	
	protected void displayReaction(RenderableReaction reaction) {
		// Create a copy of node so that the position changes will not affect
		// the original one
		ReactionNode reactionClone = cloneReaction(reaction);
		JFrame parentFrame = (JFrame) SwingUtilities.getRoot(this);
		final JDialog dialog = new JDialog(parentFrame, "Reaction: " + reaction.getDisplayName());
		final ReactionNodeGraphEditor graphEditor = new ReactionNodeGraphEditor();
		graphEditor.addMouseListener(createComplexPopupListener(graphEditor, dialog));
		graphEditor.setEditable(false);
		graphEditor.setRenderable(reactionClone);
		dialog.getContentPane().add(new JScrollPane(graphEditor), BorderLayout.CENTER);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setModal(true);
		dialog.setSize(500, 400);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				graphEditor.layoutRenderable();
			}
			public void windowClosing(WindowEvent e) {
				// Clear shortcuts
				clearShortcuts(graphEditor.getRenderable());
			}
		});
		dialog.setVisible(true);		
	}
	
	private ReactionNode cloneReaction(RenderableReaction reaction) {
		ReactionNode newNode = new ReactionNode();
		RenderableReaction reactionClone =  new RenderableReaction();
		newNode.setReaction(reactionClone);
		reactionClone.initPosition((Point)reaction.getPosition().clone());
		java.util.List list = reaction.getInputNodes();
		Renderable node = null;
		Node shortcut = null;
		if (list != null && list.size() > 0) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				node = (Renderable) it.next();
				shortcut = (Node) node.generateShortcut();
				newNode.addComponent(shortcut);
				reactionClone.addInput(shortcut);
			}
		}
		list = reaction.getOutputNodes();
		if (list != null && list.size() > 0) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				node = (Renderable) it.next();
				shortcut = (Node) node.generateShortcut();
				newNode.addComponent(shortcut);
				reactionClone.addOutput(shortcut);
			}
		}
		list = reaction.getHelperNodes();
		if (list != null && list.size() > 0) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				node = (Renderable) it.next();
				shortcut = (Node) node.generateShortcut();
				newNode.addComponent(shortcut);
				reactionClone.addHelper(shortcut);
			}
		}
		list = reaction.getInhibitorNodes();
		if (list != null && list.size() > 0) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				node = (Renderable) it.next();
				shortcut = (Node) node.generateShortcut();
				newNode.addComponent(shortcut);
				reactionClone.addInhibitor(shortcut);
			}
		}
		list = reaction.getActivatorNodes();
		if (list != null && list.size() > 0) {
			for (Iterator it = list.iterator(); it.hasNext();) {
				node = (Renderable) it.next();
				shortcut = (Node) node.generateShortcut();
				newNode.addComponent(shortcut);
				reactionClone.addActivator(shortcut);
			}
		}
		return newNode;
	}
	
	private MouseListener createComplexPopupListener(final GraphEditorPane graphEditor, final JDialog dialog) {
		MouseAdapter adaptor = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					// Try to display complex
					java.util.List selection = graphEditor.getSelection();
					if (selection.size() != 1)
						return;
					Renderable r = (Renderable) selection.get(0);
					if (r instanceof RenderableComplex) {
						JDialog complexDialog = new JDialog(dialog);
						displayComplex((RenderableComplex)r, complexDialog);
					}
				}
			}
		};
		return adaptor;
	}
	
	protected void displayComplex(final RenderableComplex complex, JDialog complexDialog) {
		complexDialog.setTitle("Complex: " + complex.getDisplayName());
		final ComplexGraphEditor complexEditor = new ComplexGraphEditor();
		complexEditor.addMouseListener(createComplexPopupListener(complexEditor, complexDialog));
		complexEditor.setEditable(false);
		complexEditor.setRenderable(cloneComplex(complex));
		complexDialog.getContentPane().add(new JScrollPane(complexEditor), BorderLayout.CENTER);
		complexDialog.setLocationRelativeTo(complexDialog.getOwner());
		complexDialog.setSize(400, 300);
		complexDialog.setModal(true);
		complexDialog.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				Graphics g = complexEditor.getGraphics();
				Renderable displayedComplex = complexEditor.getRenderable();
				if (g != null && displayedComplex.getComponents() != null) {
					// Have to validate bounds manually in case it is null
					for (Iterator it = displayedComplex.getComponents().iterator(); it.hasNext();) {
						Node node = (Node) it.next();
						node.validateBounds(g);
					}
					complexEditor.layoutRenderable();
				}
			}
			public void windowClosing(WindowEvent e) {
				clearShortcuts(complexEditor.getRenderable());
			}
		});
		complexDialog.setVisible(true);		
	}
	
	private RenderableComplex cloneComplex(RenderableComplex complex) {
		RenderableComplex complexClone = new RenderableComplex();
		if (complex.getComponents() != null && 
		    complex.getComponents().size() > 0) {
			Renderable r = null;
			Renderable shortcut = null;
			for (Iterator it = complex.getComponents().iterator(); it.hasNext();) {
				r = (Renderable) it.next();
				shortcut = (Renderable) r.generateShortcut();
				complexClone.addComponent(shortcut);
			}
		}
		return complexClone;
	}
	
	public void setPathway(RenderablePathway pathway) {
		setPathway(pathway, true); // Default need shortcut
	}
	
	public void setPathway(RenderablePathway pathway, boolean needShortcut) {
		this.pathway = pathway;
		extractRenderInfo(pathway, needShortcut);
		// Need to call these two lines explicitly.
		//layoutRenderable();
		//repaint();
	}
	
	/**
	 * A helper method to digest a RenderablePathway to a list of linked
	 * renderable events. Each event will be represented as Node, and nodes
	 * will be linked together based on precedingEvent info.
	 * @param pathway
	 */
	protected void extractRenderInfo(RenderablePathway pathway, boolean needShortcut) {
		java.util.List comps = displayedObject.getComponents();
		if (comps != null)
			comps.clear();
		// Extract all reactions
		java.util.List reactions = new ArrayList();
		Map precedingEventMap = new HashMap();
		java.util.List current = new ArrayList();
		java.util.List next = new ArrayList();
		current.add(pathway);
		while (current.size() > 0) {
			for (Iterator it = current.iterator(); it.hasNext();) {
				Renderable r = (Renderable) it.next();
				// Check components for reactions
				if (r.getComponents() != null && r.getComponents().size() > 0) {
					for (Iterator it1 = r.getComponents().iterator(); it1.hasNext();) {
						Renderable r1 = (Renderable) it1.next();
						if (r1 instanceof Shortcut)
							continue;
						if (r1 instanceof ReactionNode)
							reactions.add(r1);
						else if (r1 instanceof RenderablePathway)
							next.add(r1);
						else if (r1 instanceof FlowLine) { // Extract preceding event info
							FlowLine flowLine = (FlowLine) r1;
							java.util.List inputs = flowLine.getInputNodes();
							java.util.List outputs = flowLine.getOutputNodes();
							if (inputs.size() > 0 && outputs.size() > 0) {
								// There should be only one input and one output
								Renderable input = (Renderable) inputs.get(0);
								Renderable output = (Renderable) outputs.get(0);
								if (input instanceof Shortcut)
									input = ((Shortcut)input).getTarget();
								if (output instanceof Shortcut)
									output = ((Shortcut)output).getTarget();
								// Only ReactionNode can be linked
								if (output instanceof ReactionNode && input instanceof ReactionNode) {
									java.util.List preList = (java.util.List)precedingEventMap.get(output);
									if (preList == null) {
										preList = new ArrayList();
										precedingEventMap.put(output, preList);
									}
									preList.add(input);
								}
							}
						}
					}
				}
			}
			current.clear();
			current.addAll(next);
			next.clear();
		}
		// Convert extra preceding event information
		for (Iterator it = reactions.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			java.util.List extraPreList = (java.util.List) r.getAttributeValue("precedingEvent");
			if (extraPreList == null || extraPreList.size() == 0)
				continue;
			for (Iterator it1 = extraPreList.iterator(); it1.hasNext();) {
				Renderable r1 = (Renderable) it1.next();
				if (r1 instanceof ReactionNode) {
					java.util.List list = (java.util.List) precedingEventMap.get(r);
					if (list == null) {
						list = new ArrayList();
						precedingEventMap.put(r, list);
					}
					list.add(r1);
				}
			}
		}
		if (needShortcut) {
			// Convert ReactionNode to shortcut
			Map shortcutMap = new HashMap();
			for (Iterator it = reactions.iterator(); it.hasNext();) {
				ReactionNode node = (ReactionNode)it.next();
				Renderable shortcut = node.generateShortcut();
				shortcutMap.put(node, shortcut);
				displayedObject.addComponent((Renderable)shortcut);
			}
			// Add FlowLine objects for connected reactions
			for (Iterator it = precedingEventMap.keySet().iterator(); it.hasNext();) {
				ReactionNode node = (ReactionNode)it.next();
				java.util.List list = (java.util.List)precedingEventMap.get(node);
				if (list == null || list.size() == 0)
					continue;
				ReactionNode shortcut = (ReactionNode)shortcutMap.get(node);
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					ReactionNode preNode = (ReactionNode)it1.next();
					ReactionNode shortcut1 = (ReactionNode)shortcutMap.get(preNode);
					FlowLine flowLine = new FlowLine();
					if (shortcut1 != null)
						flowLine.addInput(shortcut1);
					flowLine.addOutput(shortcut);
					displayedObject.addComponent(flowLine);
				}
			}
		}
		else {
			for (Iterator it = reactions.iterator(); it.hasNext();) {
				ReactionNode node = (ReactionNode)it.next();
				displayedObject.addComponent(node);
			}
			// Add FlowLine objects for connected reactions
			for (Iterator it = precedingEventMap.keySet().iterator(); it.hasNext();) {
				ReactionNode node = (ReactionNode)it.next();
				java.util.List list = (java.util.List)precedingEventMap.get(node);
				if (list == null || list.size() == 0)
					continue;
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					ReactionNode preNode = (ReactionNode)it1.next();
					FlowLine flowLine = new FlowLine();
					if (preNode != null)
						flowLine.addInput(preNode);
					flowLine.addOutput(node);
					displayedObject.addComponent(flowLine);
				}
			}			
		}
	}	
	
	/**
	 * Automatically layout the displayed reactions.
	 * @param type one of HIERARCHICAL_LAYOUT, FORCE_DIRECTED_LAYOUT and CIRCULAR_LAYOUT in
	 * GraphLayoutEngine class.
	 */
	public void layoutPathway(int type) {
		RenderablePathway pathway = (RenderablePathway) displayedObject;
		// Using hierarchical layout as default.
		pathway.layout(type);
		// Center the pathway
		centerRenderable();
		revalidate();
		repaint();
	}
	
	public RenderablePathway getPathway() {
		return this.pathway;
	}
	
	public void clearShortcuts() {
		if (displayedObject.getComponents() == null ||
		    displayedObject.getComponents().size() == 0)
		    return;
		boolean isShortcutUsed = false;
		Renderable r = (Renderable) displayedObject.getComponents().get(0);
		if (r instanceof Shortcut)
			isShortcutUsed = true;
		if (!isShortcutUsed)
			return;
		clearShortcuts(displayedObject);
	}
	
	private void clearShortcuts(Renderable container) {
		if (container.getComponents() != null) {
			Renderable target = null;
			Shortcut shortcut = null;
			for (Iterator it = container.getComponents().iterator(); it.hasNext();) {
				Object obj = it.next();
				if (obj instanceof Shortcut) {
					shortcut = (Shortcut) obj;
					target = shortcut.getTarget();
					target.removeShortcut((Renderable)shortcut);
				}
			}			
		}
	}
}
