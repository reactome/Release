/*
 * Created on Jul 2, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;

import org.apache.batik.ext.swing.GridBagConstants;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.ReactionNodeGraphEditor;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.*;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;

/**
 * This class is used to manage popup menu for GraphEditorPane used in the main
 * application panel.
 * @author wgm
 *
 */
public class PopupMenuManager {
    
    private AuthorToolActionCollection actionCollection;
    private boolean isForElv = false;
    private boolean isForDrawing = false;
    
    public PopupMenuManager(AuthorToolActionCollection actionCollection) {
        this.actionCollection = actionCollection;
    }
    
    public void setIsForEntityLevelView(boolean isTrue) {
        this.isForElv = true;
    }
    
    public void setIsForDrawing(boolean value) {
        this.isForDrawing = value;
    }

    private void doBlockPopup(GraphEditorPane editor, Point p) {
        JPopupMenu popup = new JPopupMenu();
        popup.add(getDisplayFormatAction(editor));
        popup.addSeparator();
        popup.add(actionCollection.getDeleteAction());
        popup.show(editor, p.x, p.y);
    }

    private void doFlowLinePopup(FlowLine flowLine, 
                                 final PathwayEditor editor, 
                                 Point p) {
    	JPopupMenu menu = new JPopupMenu();
    	// Copy and Paste can work only for Node objects
    	menu.add(getAddBendPointAction(flowLine, editor, p));
        Action action = getRemoveBendPointAction(flowLine, editor);
        action.setEnabled(flowLine.isPointRemovable());
        menu.add(action);
        menu.add(getSelectionConnectedNodesAction(flowLine, editor));
        menu.addSeparator();
        menu.add(actionCollection.getDeleteAction());
    	menu.addSeparator();
    	JMenuItem item = menu.add(actionCollection.getLayoutEdgesAction());
    	item.setText("Layout");
    	menu.add(getDisplayFormatAction(editor));
        if (flowLine instanceof RenderableInteraction) {
            menu.addSeparator();
            if (!isForElv)
                menu.add(actionCollection.getRenameAction());
            menu.add(actionCollection.getEditPropertiesAction());
        }
    	menu.show(editor, p.x, p.y);
    }

    private void doNodePopup(final Renderable r, 
                             GraphEditorPane editor, 
                             Point p) {
        Node node = (Node) r;
        JPopupMenu popup = new JPopupMenu();
    	JMenuItem item = null;
    	if (r instanceof ProcessNode &&
    	    actionCollection.getExpandDiagramAction() != null) {
    	    popup.add(actionCollection.getExpandDiagramAction());
    	    popup.addSeparator();
    	}
    	// Add cut, copy and paste actions
        // Cut, copy works only for Biological Entities only
        if (r.isTransferrable()) {
            popup.add(actionCollection.getCutAction());
            popup.add(actionCollection.getCopyAction());
            if (!isForElv) // Disable clone for the time being
                popup.add(actionCollection.getCloneAction());
        }
    	popup.add(actionCollection.getDeleteAction());
        if (r instanceof RenderableComplex) {
            RenderableComplex complex = (RenderableComplex) r;
            if (actionCollection.isShowComponentActionNeeded(complex)) {
                //popup.add(getBreakComplexAction(editor));
                // These three actions will work only for a complex containing components
                if (complex.isComponentsHidden()) {
                    popup.add(getShowComponentAction(editor));
                }
                else {
                    popup.add(getLayoutComplexAction(editor));
                    popup.add(getHideComponentsAction(editor));
                }
            }
        }
        // Don't consider forming multimer in the entity level view.
        if (!isForElv && node.isMultimerFormable()) {
            Action action = getFormMultimerAction((Node)r,
                                                  editor);
            // Otherwise a multimer has been formed already.
            action.setEnabled(node.getMultimerMonomerNumber() < 2);
            popup.add(action);
            action = getSetMonomerNumberAction(node,
                                                      editor);
            // Only used for a multimer formed
            action.setEnabled(node.getMultimerMonomerNumber() > 1);
            popup.add(action);
        }
        popup.addSeparator();	
        // Block format action for a Pathway object that is used as a container
        if (!(r instanceof RenderablePathway)) {
            popup.add(getDisplayFormatAction(editor));
            popup.addSeparator();
        }
        // Add feature action. It should be disabled if the clicked
        // Node is not feature addable.
        if (!isForElv) { // These features have been disabled for the entity level view for the time being.
            Action action = getAddFeatureAction(node);
            action.setEnabled(node.isFeatureAddable());
            popup.add(action);
            action = getRemoveFeatureAction(node, editor);
            action.setEnabled(node.getSelectionPosition() == SelectionPosition.FEATURE);
            popup.add(action);
            action = getAddStateAction(node);
            action.setEnabled(node.isStateAddable());
            popup.add(action);
            action = getRemoveStateAction(node, editor);
            action.setEnabled(node.getSelectionPosition() == SelectionPosition.STATE);
            popup.add(action);
            popup.addSeparator();
        }
        if (!isForElv)
            popup.add(actionCollection.getRenameAction());
        popup.add(actionCollection.getEditPropertiesAction());
    	popup.show(editor, p.x, p.y);
    }

    public void doPathwayEditorPopup(final PathwayEditor editor, 
                                        final Point p) {
        JPopupMenu menu = new JPopupMenu();
        if (editor.getSelection().size() == 0) {
            // For auto layout
            menu.add(actionCollection.getLayoutAction());
            if (isForElv) {
                menu.add(actionCollection.getRemoveCompartFromNameAction());
                menu.add(actionCollection.getPrivateNoteAction());
                menu.add(actionCollection.getTightBoundsAction());
                menu.add(actionCollection.getWrapTextIntoNodesAction());
                menu.add(actionCollection.getDoNotReleaseAction());
            }
            menu.add(actionCollection.getToggleShowPathwayAction());
            menu.addSeparator();
            // Add cut, copy and paste actions
            menu.add(actionCollection.getPasteAsAliasAction());
            if (isForElv) {
                // Display the properties for the displayed pathway
                menu.addSeparator();
                menu.add(actionCollection.getEditPropertiesAction());
                menu.addSeparator();
                menu.add(actionCollection.getExportDiagramAction());
            }
            //menu.add(getPasteAsNewInstanceAction());
            menu.show(editor, p.x, p.y);
        }
        else if (editor.getSelection().size() == 1) {
            Renderable selected = (Renderable) editor.getSelection().get(0);
            if (selected instanceof Node) 
                doNodePopup(selected, editor, p);
            else if (selected instanceof RenderableCompartment)
                doBlockPopup(editor, p);
            else if (selected instanceof RenderableReaction)
                doReactionPopup((RenderableReaction)selected, editor, p);
            else if (selected instanceof FlowLine)
                doFlowLinePopup((FlowLine)selected, editor, p);
        }
        else { // More than one objects are selected
            int edgeNumber = 0;
            int nodeNumber = 0;
            int possiblePathwayCompNumber = 0;
            List selection = editor.getSelection();
            boolean isCopySupported = actionCollection.isTransferrable(selection);
            // Tally numbers
            for (Iterator it = selection.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r instanceof HyperEdge) {
                    edgeNumber ++;
                    possiblePathwayCompNumber ++;
                }
                else if (r instanceof Node) {
                    nodeNumber ++;
                    if (r instanceof RenderablePathway) {
                        possiblePathwayCompNumber ++;
                    }
                }
            }
            if (isCopySupported) {
                menu.add(actionCollection.getCutAction());
                menu.add(actionCollection.getCopyAction());
                if (!isForDrawing)
                    menu.add(actionCollection.getCloneAction());
            }
            menu.add(actionCollection.getDeleteAction());
            // Check if "add link" can be added: "add link" should be allowed
            // between an entity node and a process node only. A FlowLine should
            // not be allowed between two ProcessNodes in the drawing mode since
            // a FlowLink between two ProcessNodes are used to describe preceding/following
            // relationship.
            if (isForDrawing && selection.size() == 2) {
                Renderable r1 = (Renderable) selection.get(0);
                Renderable r2 = (Renderable) selection.get(1);
                if ((r1 instanceof ProcessNode && r2 instanceof Node) ||
                    (r1 instanceof Node && r2 instanceof ProcessNode))
                    menu.add(actionCollection.getCreateLinkAction());
                else if (isEntitySetAndMember(r1, r2)) { // Check if it is possible to add a link between an EntitySet and its member
                    menu.add(actionCollection.getCreateSetAndMemberLinkAction());
                }
                else if (canAddLinkForTwoEntitySets(r1, r2)) {
                    menu.add(actionCollection.getCreateSetAndSetLinkAction());
                }
            }
            if (nodeNumber > 1 && edgeNumber == 0 && !isForDrawing)
                menu.add(getFormComplexAction(editor)); // cannot contain edges
            if (possiblePathwayCompNumber > 1)
                menu.add(getCreatePathwayAction(editor));
            menu.addSeparator();
            JMenuItem item = new JMenuItem("Align Vertically");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editor.alignSelectionVertically();
                }
            });
            menu.add(item);
            item = new JMenuItem("Aligh Horizontally");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editor.alignSelectionHorizontally();
                }
            });
            menu.add(item);
            if (edgeNumber > 0)
                menu.add(actionCollection.getLayoutEdgesAction());
            menu.add(getDisplayFormatAction(editor));
            menu.show(editor, p.x, p.y);
        }
    }
    
    private boolean isEntitySetAndMember(Renderable r1, Renderable r2) {
        if (!(r1 instanceof Node) || !(r2 instanceof Node))
            return false;
        if (r1.getReactomeId() == null || r2.getReactomeId() == null)
            return false;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        if (fileAdaptor == null)
            return false; // Cannot be checked! Just in case.
        try {
            GKInstance inst1 = fileAdaptor.fetchInstance(r1.getReactomeId());
            GKInstance inst2 = fileAdaptor.fetchInstance(r2.getReactomeId());
            if (inst1 == null || inst2 == null)
                return false;
            if (inst1.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                if (InstanceUtilities.isEntitySetAndMember(inst1, inst2))
                    return true;
            }
            // Both instances can be EntitySet. So else if should not be used here.
            if (inst2.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                if (InstanceUtilities.isEntitySetAndMember(inst2, inst1))
                    return true;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean canAddLinkForTwoEntitySets(Renderable r1, Renderable r2) {
        if (r1.getReactomeId() == null || r2.getReactomeId() == null ||
            !(r1 instanceof Node) || !(r2 instanceof Node))
            return false;
        XMLFileAdaptor fileAdatpor = PersistenceManager.getManager().getActiveFileAdaptor();
        if (fileAdatpor == null)
            return false;
        try {
            GKInstance inst1 = fileAdatpor.fetchInstance(r1.getReactomeId());
            GKInstance inst2 = fileAdatpor.fetchInstance(r2.getReactomeId());
            if (inst1 == null || inst2 == null)
                return false;
            if (InstanceUtilities.hasSharedMembers(inst1, inst2))
                return true;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private Action getCreatePathwayAction(final GraphEditorPane graphPane) {
        Action createPathwayAction = new AbstractAction("Create Pathway") {
            public void actionPerformed(ActionEvent e) {
                createPathway(graphPane);
            }
        };
        return createPathwayAction;
    }
    
    private void createPathway(GraphEditorPane graphPane) {
        List selection = graphPane.getSelection();
        List events = new ArrayList();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object next = it.next();
            if (next instanceof RenderableReaction ||
                next instanceof RenderablePathway ||
                next instanceof RenderableInteraction)
                events.add(next);
        }
        if (events.size() == 0) {
            JOptionPane.showMessageDialog(graphPane,
                                          "You have to choose at least one reaction, pathway, or interaction to add a new pathway.",
                                          "Error in Adding Pathway",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        Renderable container = graphPane.getRenderable();
        RenderablePathway newPathway = (RenderablePathway) RenderableFactory.generateRenderable(RenderablePathway.class,
                                                                                                container);
        String name = RenderableRegistry.getRegistry().generateUniqueName(newPathway);
        newPathway.setDisplayName(name);
        RenderableRegistry.getRegistry().add(newPathway);
        // Add all selected objects to new pathway to minimize all drawing work
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            newPathway.addComponent(r);
            // RenderablePathway is used as a virtual group container. All
            // other hierarchical is still kept for drawing purpose.
            //r.setContainer(newPathway);
            //container.removeComponent(r);
            // There is a serious caveat here: r is contained by the topmost container
            // and newPathway right now but its container is newPathway only. This makes
            // drawing easy, but easy to bring in a lot of other bugs. Also this way
            // is different from the one used to handle Complex. Another problem is in
            // deleting: have to manually figure out in the property tree.
        }
        newPathway.setBoundsFromComponents();
        graphPane.insertNode(newPathway);
        graphPane.setSelection(newPathway);
        graphPane.repaint(graphPane.getVisibleRect());
    }
    
    private Action getFormComplexAction(final GraphEditorPane graphPane) {
        Action formComplexAction = new AbstractAction("Form Complex",
                                                      AuthorToolAppletUtilities.createImageIcon("Pack.gif")) {
            public void actionPerformed(ActionEvent e) {
                formComplex(graphPane);
            }
        };
        return formComplexAction;
    }
    
    private void formComplex(final GraphEditorPane graphPane) {
        // Get the selected 
        List selection = graphPane.getSelection();
        if (selection.size() < 1)
            return;
        // Get all nodes
        List nodes = new ArrayList();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object obj = it.next();
            if ((obj instanceof Node) && !(obj instanceof Note))
                nodes.add(obj);
        }
        if (nodes.size() == 0)
            return;
        UndoableFormComplexEdit edit = new UndoableFormComplexEdit(nodes,
                                                                   graphPane);
        final RenderableComplex complex = new RenderableComplex();
        String name = RenderableRegistry.getRegistry().generateUniqueName(complex);
        complex.setDisplayName(name);
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            // If this node is contained by a RenderblePathway, let it be.
            // Otherwise, it should be removed from its container
            if (node.getContainer() instanceof RenderableComplex)
                node.getContainer().removeComponent(node);
            // The above way to try to copy the same method as for creating a new RenderablePahtway
            // This is used to simply the drawing and selection. However, extra attention should be
            // paid to this data structure.
            complex.addComponent(node);
            node.setContainer(complex);
        }
        complex.setBoundsFromComponents();
        // In case they are still selected
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            node.setIsSelected(false);
        }
        graphPane.insertNode(complex);
        RenderableRegistry.getRegistry().add(complex);
        graphPane.setSelection(complex);
        // Do a simple layout. This may need to be changed
        // in the future so that the original components
        // layout can be kept.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                complex.layout();
                graphPane.repaint(graphPane.getVisibleRect());
            }
        });
        graphPane.repaint(graphPane.getVisibleRect());
        edit.setComplex(complex);
        graphPane.addUndoableEdit(edit);
    }
    
    public Action getSetStoichiometryAction(final RenderableReaction reaction,
                                            final PathwayEditor pathwayEditor) {
        Action action = new AbstractAction("Set Stoichiometry...") {
            public void actionPerformed(ActionEvent e) {
                setStoichiometry(reaction,
                                 pathwayEditor);
            }
        };
        return action;
    }
    
    private void setStoichiometry(RenderableReaction reaction,
                                  PathwayEditor editor) {
        JFrame owner = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, editor);
        StoichiometrySettingDialog dialog = new StoichiometrySettingDialog(owner,
                                                                           "Set Stoichiometry");
        dialog.setReaction(reaction);
        dialog.setLocationRelativeTo(editor);
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
        if (dialog.isChanged()) {
            actionCollection.editorFrame.enableSaveAction(true);
            editor.repaint(reaction.getBounds());
        }
    }

    private void doReactionPopup(final RenderableReaction reaction, 
                                 final PathwayEditor editor, Point p) {
    	JPopupMenu menu = new JPopupMenu();
        // copy and paste are limit to Nodes only
    	menu.add(getAddBendPointAction(reaction, editor, p));
    	Action action = getRemoveBendPointAction(reaction, editor);
    	action.setEnabled(reaction.isPointRemovable());
    	menu.add(action);
    	menu.add(getSelectionConnectedNodesAction(reaction, editor));
    	menu.addSeparator();
        menu.add(actionCollection.getDeleteAction());
        menu.addSeparator();
    	// As of July 10, 2008, all actions to add branches have been removed.
    	// The user should use popup edges to do these. This is to remove too many
    	// menus for edges.
//    	JMenuItem addInput = new JMenuItem("Add Input");
//    	addInput.addActionListener(new ActionListener() {
//    		public void actionPerformed(ActionEvent e) {
//    			reaction.addInput();
//    			editor.repaint(editor.getVisibleRect());
//    		}
//    	});
//    	menu.add(addInput);
//    	JMenuItem addOutput = new JMenuItem("Add Output");
//    	addOutput.addActionListener(new ActionListener() {
//    		public void actionPerformed(ActionEvent e) {
//    			reaction.addOutput();
//    			editor.repaint(editor.getVisibleRect());
//    		}
//    	});
//    	menu.add(addOutput);
//    	JMenuItem addHelper = new JMenuItem("Add Catalyst");
//    	addHelper.addActionListener(new ActionListener() {
//    		public void actionPerformed(ActionEvent e) {
//    			reaction.addHelperBranch();
//    			editor.repaint(editor.getVisibleRect());
//    		}
//    	});
//    	menu.add(addHelper);
//        JMenuItem addInhibitor = new JMenuItem("Add Inhibitor");
//        addInhibitor.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                reaction.addInhibitorBranch();
//                editor.repaint(editor.getVisibleRect());
//            }
//        });
//        menu.add(addInhibitor);
//        JMenuItem addActivator = new JMenuItem("Add Activator");
//        addActivator.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                reaction.addActivatorBranch();
//                editor.repaint(editor.getVisibleRect());
//            }
//        });
//        menu.add(addActivator);
//    	JMenuItem item = generateRemoveBranchItem(reaction, editor);
//    	if (item != null)
//    		menu.add(item);
//    	menu.addSeparator();
    	JMenuItem item = menu.add(actionCollection.getLayoutEdgesAction());
    	item.setText("Layout");
        menu.add(getDisplayFormatAction(editor));
        menu.addSeparator();
        if (!isForElv)
            menu.add(actionCollection.getRenameAction());
        menu.add(getChangeReactionTypeAction(reaction, editor));
        menu.add(getSetStoichiometryAction(reaction, editor));
        menu.add(actionCollection.getEditPropertiesAction());
    	menu.show(editor, p.x, p.y);
    }
    
    private Action getChangeReactionTypeAction(final RenderableReaction reaction,
                                               final GraphEditorPane graphPane) {
        Action changeType = new AbstractAction("Change Type...") {
            public void actionPerformed(ActionEvent e) {
                changeReactionType(reaction,
                                   graphPane);
            }
        };
        return changeType;
    }
    
    private void changeReactionType(final RenderableReaction reaction,
                                    final GraphEditorPane graphPane) {
        // Need to create a dialog
        final JDialog dialog = GKApplicationUtilities.createDialog(graphPane, 
                                                             "Change Reaction Type");
        // Set up GUIs for the dialog
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstants.WEST;
        constraints.insets = new Insets(4, 4, 4, 4);
        JLabel label = new JLabel("<html><b><u>Choose a Type:</u></b></html>");
        panel.add(label, constraints);
        final JComboBox box = new JComboBox(ReactionType.values());
        // Set the selected value
        if (reaction.getReactionType() == null)
            box.setSelectedIndex(0);
        else
            box.setSelectedItem(reaction.getReactionType());
        constraints.gridy = 1;
        panel.add(box, constraints);
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ReactionType type = (ReactionType) box.getSelectedItem();
                reaction.setReactionType(type);
                graphPane.fireGraphEditorActionEvent(GraphEditorActionEvent.REACTION_TYPE);
                dialog.dispose();
            }
        });
        dialog.getContentPane().add(panel, BorderLayout.CENTER);
        dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
        dialog.setSize(330, 230);
        dialog.setModal(true);
        // If the graph pane is very large (most cases) embedded in a scrollpane,
        // the diagram may be in placed in a pretty far away
        dialog.setLocationRelativeTo(dialog.getOwner());
//        dialog.setLocationRelativeTo(graphPane);
        dialog.setVisible(true);
    }

    private Action getDisplayFormatAction(final GraphEditorPane graphPane) {
        Action  displayFormatAction = new AbstractAction("Format Display...") {
            public void actionPerformed(ActionEvent e) {
                formatDisplay(graphPane);
            }
        };
        return displayFormatAction;
    }

    private void formatDisplay(final GraphEditorPane graphPane) {
        if (graphPane == null)
            return;
        List selection = graphPane.getSelection();
        if (selection == null || selection.size() == 0)
            return;
        // Used as an example
        final Renderable r = (Renderable) selection.get(0);
        RenderableDisplayFormatDialog dialog = new RenderableDisplayFormatDialog(actionCollection.editorFrame);
        dialog.setPrivateNoteSupport(isForElv);
        dialog.setRenderables(selection);
        dialog.setEditorPane(graphPane);
        // Try to make location nice
        dialog.setLocationRelativeTo(SwingUtilities.getAncestorOfClass(Container.class, graphPane));
        final UndoableFormatEdit edit = new UndoableFormatEdit(selection, graphPane);
        dialog.addOKListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                graphPane.addUndoableEdit(edit);
                actionCollection.enableSave();
                fireGraphEditEvent(r,
                                   GraphEditorActionEvent.FORMAT);
            }
        });
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void addNodeAttachment(Node node,
                                   Class type) {
        int oldSize = node.getNodeAttachments() == null ? 0 : node.getNodeAttachments().size();
        // Need a simple dialog for adding feature
        NodeAttachmentAddingDialog dialog = null;
        if (type.equals(RenderableFeature.class))
            dialog = new FeatureAddingDialog(actionCollection.editorFrame);
        else if (type.equals(RenderableState.class))
            dialog = new StateAddingDialog(actionCollection.editorFrame);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(actionCollection.editorFrame);
        dialog.setNode(node);
        dialog.setVisible(true);
        int newSize = node.getNodeAttachments() == null ? 0 : node.getNodeAttachments().size();
        if (newSize > oldSize) {
            actionCollection.enableSave();
            actionCollection.getPathwayEditor().killUndo();
            fireGraphEditEvent(node, GraphEditorActionEvent.FEATURE);
        }
    }

    private Action getAddBendPointAction(final HyperEdge edge,
                                         final GraphEditorPane graphPane,
                                         final Point point) {
        Action action = new AbstractAction("Add Bending") {
            public void actionPerformed(ActionEvent e) {
                // Need to add scale to the point
                Point copy = new Point(point);
                copy.x = (int)(point.x / graphPane.getScaleX());
                copy.y = (int)(point.y / graphPane.getScaleY());
                edge.addPoint(copy);
                actionCollection.enableSave();
                graphPane.repaint(graphPane.getVisibleRect());
                fireGraphEditEvent(edge, GraphEditorActionEvent.BENDING);
                graphPane.killUndo();
            }
        };
        return action;
    }
    
    private Action getSelectionConnectedNodesAction(final HyperEdge edge,
                                                    final GraphEditorPane graphPane) {
        Action action = new AbstractAction("Select Connected Nodes") {
            public void actionPerformed(ActionEvent e) {
                List<Node> nodes = edge.getConnectedNodes();
                for (Node node : nodes) {
                    if (node.isSelected())
                        continue;
                    node.setIsSelected(true);
                    graphPane.addSelected(node);
                }
                graphPane.repaint(graphPane.getVisibleRect());
            }
        };
        return action;
    }
    
    private void fireGraphEditEvent(Renderable r,
                                    GraphEditorActionEvent.ActionType type) {
        GraphEditorActionEvent event = new GraphEditorActionEvent(r);
        event.setID(type);
        actionCollection.getPathwayEditor().fireGraphEditorActionEvent(event);
    }

    private Action getAddFeatureAction(final Node node) {
        Action addFeatureAction = new AbstractAction("Add Feature") {
            public void actionPerformed(ActionEvent e) {
                addNodeAttachment(node, RenderableFeature.class);
            }
        };
        return addFeatureAction;
    }

    private Action getAddStateAction(final Node node) {
        Action addStateAction = new AbstractAction("Add State") {
            public void actionPerformed(ActionEvent e) {
                addNodeAttachment(node, RenderableState.class);
            }
        };
        return addStateAction;
    }

    private Action getRemoveBendPointAction(final HyperEdge edge,
                                            final GraphEditorPane graphPane) {
        Action action = new AbstractAction("Remove Bending") {
            public void actionPerformed(ActionEvent e) {
                edge.removeSelectedPoint();
                actionCollection.enableSave();
                graphPane.repaint(graphPane.getVisibleRect());
                graphPane.killUndo();
                fireGraphEditEvent(edge, GraphEditorActionEvent.BENDING);
            }
        };
        return action;
    }

    private Action getRemoveFeatureAction(final Node node,
                                         final GraphEditorPane graphPane) {
        Action removeFeatureAction = new AbstractAction("Remove Feature") {
            public void actionPerformed(ActionEvent e) {
                removeNodeAttachment(node, graphPane, "feature");
            }
        };
        return removeFeatureAction;                                            
    }

    private Action getRemoveStateAction(final Node node,
                                       final GraphEditorPane graphPane) {
        Action removeStateAction = new AbstractAction("Remove State") {
            public void actionPerformed(ActionEvent e) {
                removeNodeAttachment(node, graphPane, "state");
            }
        };
        return removeStateAction;
    }

    /**
     * A helper method to remove a selected RenderableFeature for a Node.
     */
    private void removeNodeAttachment(Node node,
                                      GraphEditorPane graphPane,
                                      String type) {
        // Check if the dialog is needed
        // Check if there is any shortcuts available
        Node target = RenderUtility.getShortcutTarget(node);
        boolean isLocal = true;
        if (target.getShortcuts() != null && target.getShortcuts().size() > 0) {
            // Need to check if it should be local
            String message = "Do you want to remove the selected " + type + " in other objects having same name?";
            int reply = JOptionPane.showConfirmDialog(actionCollection.toolPane,
                                                      message,
                                                      "Remove " + type,
                                                      JOptionPane.YES_NO_OPTION);
            isLocal = (reply == JOptionPane.NO_OPTION);
        }
        boolean rtn = false;
        if (isLocal)
            rtn = node.removeSelectedAttachmentLocally();
        else
            rtn = node.removeSelectedAttachment();
        if (rtn) {
            // Do a big paint in case there are shortcuts
            graphPane.repaint(graphPane.getVisibleRect());
            actionCollection.enableSave();
            fireGraphEditEvent(node, 
                               GraphEditorActionEvent.FEATURE);
        }
    }

    private void breakdownComplex(GraphEditorPane graphPane) {
        // Do several checks 
        List selection = graphPane.getSelection();
        if (selection.size() != 1)
            return; 
        Renderable r = (Renderable) selection.get(0);
        if (!(r instanceof RenderableComplex))
            return;
        RenderableComplex complex = (RenderableComplex) r;
        List components = complex.getComponents();
        // add the contained entities: Should call this first
        // in case components data has been changed
        if (components != null) {
            for (Iterator it = components.iterator(); it.hasNext();) {
                Node node = (Node) it.next();
                graphPane.insertNode(node);
                it.remove(); // Make the deleting complex empty
            }
        }
        // delete this complex first
        graphPane.deleteSelection();
        graphPane.repaint(graphPane.getVisibleRect());
    }

    private Action getBreakComplexAction(final GraphEditorPane graphPane) {
        Action breakComplexAction = new AbstractAction("Dissociate Complex",
                                                       AuthorToolAppletUtilities.createImageIcon("Unpack.gif")) {
            public void actionPerformed(ActionEvent e) {
                breakdownComplex(graphPane);
            }
        };
        return breakComplexAction;
    }

    private Action getHideComponentsAction(final GraphEditorPane graphPane) {
        Action hideComponentsAction = new AbstractAction("Hide Components") {
            public void actionPerformed(ActionEvent e) {
                actionCollection.hideComponents(true);
            }
        };
        return hideComponentsAction;
    }

    private Action getShowComponentAction(final GraphEditorPane graphPane) {
        Action showComponentsAction = new AbstractAction("Show Components") {
            public void actionPerformed(ActionEvent e) {
                actionCollection.hideComponents(false);
            }
        };
        return showComponentsAction;
    }

    private Action getLayoutComplexAction(final GraphEditorPane graphPane) {
        Action layoutComplexAction = new AbstractAction("Layout Complex") {
            public void actionPerformed(ActionEvent e) {
                layoutComplex(graphPane);
            }
        };
        return layoutComplexAction;
    }

    private void layoutComplex(GraphEditorPane graphPane) {
        List selection = graphPane.getSelection();
        List complexes = new ArrayList();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object next = it.next();
            if (next instanceof RenderableComplex)
                complexes.add(next);
        }
        if (complexes.size() == 0)
            return;
        for (Iterator it = complexes.iterator(); it.hasNext();) {
            RenderableComplex complex = (RenderableComplex) it.next();
            layoutComplex(complex,
                          graphPane.getGraphics());
            complex.invalidateConnectWidgets();
        }
        graphPane.repaint(graphPane.getVisibleRect());
        actionCollection.enableSave();
    }

    /**
     * Recursive deepest first layout
     * @param complex
     */
    private void layoutComplex(RenderableComplex complex,
                               Graphics g) {
        List components = complex.getComponents();
        if (components == null || components.size() == 0)
            return;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Object r = it.next();
            if (r instanceof RenderableComplex)
                layoutComplex((RenderableComplex)r, g);
        }
        complex.layout();
        complex.validateBounds(g);
    }

    private Action getFormMultimerAction(final Node node,
                                         final GraphEditorPane graphPane) {
        Action action = new AbstractAction("Form Multimer...") {
            public void actionPerformed(ActionEvent e) {
                if(!setMonomerNumber(node, false)) 
                    return;
                graphPane.killUndo();
                graphPane.repaint(node.getBounds());
            }
        };
        return action;
    }

    private boolean setMonomerNumber(Node multimer,
                                     boolean oneIsAllowed) {
        while (true) {
            String message = null;
            if (oneIsAllowed) {
                message = "Enter the number of monomer. The number should be greater than 1.\n" +
                          "If you enter number 1 or 0, the multimer will become the monomer.";
            }
            else {
                message = "Enter the number of monomers (>=2):";
            }
            String input = JOptionPane.showInputDialog(actionCollection.editorFrame,
                                                       message,
                                                       "Monomer Number",
                                                       JOptionPane.QUESTION_MESSAGE);
            if (input == null || input.length() == 0)
                return false; // Canceled
            try {
                int number = Integer.parseInt(input);
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(actionCollection.editorFrame,
                                              "Please use integer only.",
                                              "Error in Monomer Number",
                                              JOptionPane.ERROR_MESSAGE);
                continue;
            }
            int number = Integer.parseInt(input);
            message = null;
            if (number < 0)
                message = "Number should not be less than 0.";
            else if (number < 2 && !oneIsAllowed)
                message = "The number of monomers should be greater than 1.";
            if (message != null) {
                JOptionPane.showMessageDialog(actionCollection.editorFrame,
                                              message,
                                              "Error in Monomer Number",
                                              JOptionPane.ERROR_MESSAGE);
                continue;
            }
            multimer.setMultimerMonomerNumber(number);
            break;
        }
        return true;
    }
    
    private Action getSetMonomerNumberAction(final Node multimer,
                                             final GraphEditorPane graphPane) {
        Action action = new AbstractAction("Set Monomer Number...") {
            public void actionPerformed(ActionEvent e) {
                setMonomerNumber(multimer, true);
                if (multimer instanceof Node) {
                    graphPane.killUndo();
                    graphPane.repaint(((Node)multimer).getBounds());
                }
            }
        };
        return action;
    }

    private JMenuItem generateRemoveBranchItem(final RenderableReaction reaction, final GraphEditorPane editor) {
        JMenuItem item = null;
        // Check if any branch is selected
        final HyperEdgeSelectionInfo selectionInfo = reaction.getSelectionInfo();
        if (selectionInfo.getSelectedType() == HyperEdge.INPUT) {
            item = new JMenuItem("Remove Input");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (editor instanceof ReactionNodeGraphEditor) {
                        Renderable input = reaction.getInputNode(selectionInfo.getSelectedBranch());
                        if (input != null)
                            ((ReactionNodeGraphEditor)editor).delete(input);
                    }
                    reaction.removeInput(selectionInfo.getSelectedBranch());
                    editor.repaint(editor.getVisibleRect());
                }
            });
        }
        else if (selectionInfo.getSelectedType() == HyperEdge.OUTPUT) {
            item = new JMenuItem("Remove Output");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (editor instanceof ReactionNodeGraphEditor) {
                        Renderable output = reaction.getOutputNode(selectionInfo.getSelectedBranch());
                        if (output != null)
                            ((ReactionNodeGraphEditor)editor).delete(output);
                    }
                    reaction.removeOutput(selectionInfo.getSelectedBranch());
                    editor.repaint(editor.getVisibleRect());
                }
            });
        }
        else if (selectionInfo.getSelectedType() == HyperEdge.CATALYST) {
            item = new JMenuItem("Remove Catalyst");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (editor instanceof ReactionNodeGraphEditor) {
                        Renderable helper = reaction.getHelperNode(selectionInfo.getSelectedBranch());
                        if (helper != null)
                            ((ReactionNodeGraphEditor)editor).delete(helper);
                    }
                    reaction.removeHelper(selectionInfo.getSelectedBranch());
                    editor.repaint(editor.getVisibleRect());
                }
            });
        }
        else if (selectionInfo.getSelectedType() == HyperEdge.INHIBITOR) {
            item = new JMenuItem("Remove Inhibitor");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (editor instanceof ReactionNodeGraphEditor) {
                        Renderable inhibitor = reaction.getInhibitorNode(selectionInfo.getSelectedBranch());
                        if (inhibitor != null)
                            ((ReactionNodeGraphEditor)editor).delete(inhibitor);
                    }
                    reaction.removeInhibitor(selectionInfo.getSelectedBranch());
                    editor.repaint(editor.getVisibleRect());                    
                }
            });
        }
        else if (selectionInfo.getSelectedType() == HyperEdge.ACTIVATOR) {
            item = new JMenuItem("Remove Activator");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (editor instanceof ReactionNodeGraphEditor) {
                        Renderable activator = reaction.getActivatorNode(selectionInfo.getSelectedBranch());
                        if (activator != null)
                            ((ReactionNodeGraphEditor)editor).delete(activator);
                    }
                    reaction.removeActivator(selectionInfo.getSelectedBranch());
                    editor.repaint(editor.getVisibleRect());                    
                }
            });         
        }
        return item;
    }
    
}
