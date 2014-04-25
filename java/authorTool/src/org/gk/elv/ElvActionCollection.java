/*
 * Created on Nov 20, 2008
 *
 */
package org.gk.elv;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;

import org.gk.database.AttributeEditManager;
import org.gk.database.HierarchicalEventPaneActions;
import org.gk.database.InstanceSelectDialog;
import org.gk.gkEditor.AuthorToolActionCollection;
import org.gk.gkEditor.PathwayEditorInsertActions;
import org.gk.gkEditor.PopupMenuManager;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.UndoableDeleteEdit;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.*;
import org.gk.schema.SchemaClass;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.GKApplicationUtilities;

/**
 * This class is used to manage the actions used by the EntityLevelView. The reason
 * to subclass to AuthorToolActionCollection is to take advantage of PopupMenuManager.
 * @author wgm
 *
 */
public class ElvActionCollection extends AuthorToolActionCollection {
    private EntityLevelView elv;
    // For popup menu management
    private PopupMenuManager popupManager;
    private Action autoAssigCompAction;
    private Action openDiagramAction;
    private Action expandDiagramAction;
    private Action autoPrecedingEventAction;
    private Action hideCompartmentInNameAction;
    private Action createLinkAction;
    private Action encapsulateDiagramAction;
    
    public ElvActionCollection(EntityLevelView elv) {
        this.elv = elv;
        setPathwayEditor(elv.getZoomablePathwayEditor().getPathwayEditor());
        popupManager = new PopupMenuManager(this);
        popupManager.setIsForEntityLevelView(true);
        installTreePopupListener();
    }
    
    public void setUsedAsDrawingTool(boolean value) {
        popupManager.setIsForDrawing(value);
    }

    private void installTreePopupListener() {
        MouseAdapter popupAdaptor = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    doTreePopup(e);
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doTreePopup(e);
            }
        };
        elv.getTreePane().getComplexPane().addTreeMouseListener(popupAdaptor);
        elv.getTreePane().getEntityPane().addTreeMouseListener(popupAdaptor);
    }
    
    private void doTreePopup(MouseEvent e) {
        JTree tree = (JTree) e.getSource();
        InstanceTreePane treePane = (InstanceTreePane) SwingUtilities.getAncestorOfClass(InstanceTreePane.class, 
                                                                                         tree);
        JPopupMenu popup = new JPopupMenu();
        // Borrow the actions originalled designed for the hierarchicalEvent pane.
        HierarchicalEventPaneActions actions = new HierarchicalEventPaneActions();
        actions.setWrappedTree(tree);
        popup.add(actions.getViewAction());
        popup.add(actions.getViewReferrersAction());
        popup.addSeparator();
        Action deleteAction = actions.getDeleteAction();
        // This may not be the user wants to do: deleting actually delete the selected
        // instance from the local project. For complex, it is more likely the user wants to
        // delete it from complex component list.
        // TODO: to be changed in the future!
        popup.add(deleteAction);
        if (treePane instanceof ComplexHierarchicalPane) {
            popup.addSeparator();
            popup.add(actions.getExpandNodeAction());
            popup.add(actions.getCollapseNodeAction());
        }
        popup.show(tree, e.getX(), e.getY());
    }
    
    public void setSystemProperties(Properties properties) {
        this.systemProperties = properties;
    }
    
    public Action getAutoPrecedingEventAction() {
        if (autoPrecedingEventAction != null)
            return autoPrecedingEventAction;
        autoPrecedingEventAction = new AbstractAction("Auto-assign PecedingEvent Values",
                                                      AuthorToolAppletUtilities.createImageIcon("AutoPrecedingEvent.gif")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    autoAssignPrecedingEvent();
                }
                catch(Exception e1) {
                    System.err.println("ElvActionCollection.getAutoPrecedingEventAction(): " + e1);
                    e1.printStackTrace();
                }
            }
        };
        autoPrecedingEventAction.putValue(Action.SHORT_DESCRIPTION, 
                                          "Auto-assign precedingEvent value");
        return autoPrecedingEventAction;
    }
    
    /**
     * A helper method to assign precentEvent attribute value automatically based on the reaction and pathway
     * relationships in the diagrams.
     */
    private void autoAssignPrecedingEvent() throws Exception {
        PathwayEditor pathwayEditor = elv.getZoomablePathwayEditor().getPathwayEditor();
        if (pathwayEditor.getDisplayedObjects() == null ||
            pathwayEditor.getDisplayedObjects().size() == 0)
            return;
        // Check if the original value should be kept
        int reply = JOptionPane.showConfirmDialog(elv,
                                                  "Do you want to keep the original assigned precedingEvent attribute value?",
                                                  "Assigning precedingEvent Value",
                                                  JOptionPane.YES_NO_OPTION);
        boolean keepOriginal = (reply == JOptionPane.YES_OPTION);
        XMLFileAdaptor fileAdaptor = elv.getZoomablePathwayEditor().getXMLFileAdaptor();
        if (!keepOriginal) {
            // Reset all precedingEvent information
            for (Iterator it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r instanceof ProcessNode ||
                    r instanceof RenderableReaction) {
                    GKInstance instance = fileAdaptor.fetchInstance(r.getReactomeId());
                    List list = instance.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
                    if (list != null && list.size() > 0) {
                        instance.setAttributeValueNoCheck(ReactomeJavaConstants.precedingEvent, null);
                        AttributeEditManager.getManager().attributeEdit(instance, 
                                                                        ReactomeJavaConstants.precedingEvent);
                    }
                }
            }
        }
        // Get all RenderableReaction and FlowLine
        List<RenderableReaction> reactions = new ArrayList<RenderableReaction>();
        for (Iterator it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof FlowLine) {
                FlowLine fl = (FlowLine) r;
                Node input = fl.getInputNode(0);
                if (input == null)
                    continue;
                Node output = fl.getOutputNode(0);
                if (output == null)
                    continue;
                GKInstance precedingEvent = fileAdaptor.fetchInstance(input.getReactomeId());
                GKInstance followingEvent = fileAdaptor.fetchInstance(output.getReactomeId());
                List list = followingEvent.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
                if (list == null || !list.contains(precedingEvent)) {
                    followingEvent.addAttributeValue(ReactomeJavaConstants.precedingEvent,
                                                     precedingEvent);
                    AttributeEditManager.getManager().attributeEdit(followingEvent, 
                                                                    ReactomeJavaConstants.precedingEvent);
                }
            }
            else if (r instanceof RenderableReaction) {
                reactions.add((RenderableReaction)r);
            }
        }
        assignPrecedingEventForReactions(reactions, fileAdaptor);
    }
    
    /**
     * This method is used to assign precedingEvent attribute values for reactions.
     * @param reactions
     * @param fileAdaptor
     */
    private void assignPrecedingEventForReactions(List<RenderableReaction> reactions,
                                                  XMLFileAdaptor fileAdaptor) throws Exception {
        if (reactions == null || reactions.size() < 2)
            return;
        for (RenderableReaction rxt1 : reactions) {
            GKInstance rxtInstance1 = fileAdaptor.fetchInstance(rxt1.getReactomeId());
            List<Node> outputs1 = rxt1.getOutputNodes();
            // Check if any output is an input or catalyst of another reaction
            for (RenderableReaction rxt2 : reactions) {
                if (rxt2 == rxt1)
                    continue; // Don't consider itself
                List<Node> inputs2 = rxt2.getInputNodes();
                List<Node> catalysts2 = rxt2.getHelperNodes();
                GKInstance rxtInstance2 = fileAdaptor.fetchInstance(rxt2.getReactomeId());
                for (Node output1 : outputs1) {
                    if (inputs2.contains(output1) ||
                            catalysts2.contains(output1)) {
                        List list = rxtInstance2.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
                        if (list == null || !list.contains(rxtInstance1)) {
                            rxtInstance2.addAttributeValue(ReactomeJavaConstants.precedingEvent, 
                                                           rxtInstance1);
                            AttributeEditManager.getManager().attributeEdit(rxtInstance2, 
                                                                            ReactomeJavaConstants.precedingEvent);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    public Action getCreateLinkAction() {
        if (createLinkAction != null)
            return createLinkAction;
        createLinkAction = new AbstractAction("Create Flow Link") {
            public void actionPerformed(ActionEvent e) {
                createLink();
            }
        };
        return createLinkAction;
    }
    
    private void createLink() {
        // Create a link between an Entity and a ProcessNode. This is used for information flow.
        PathwayEditor editor = elv.getZoomablePathwayEditor().getPathwayEditor();
        List<Renderable> selection = editor.getSelection();
        if (selection == null || selection.size() != 2)
            return;
        boolean needLink = false;
        Renderable r1 = (Renderable) selection.get(0);
        Renderable r2 = (Renderable) selection.get(1);
        if ((r1 instanceof ProcessNode && r2 instanceof Node) ||
            (r1 instanceof Node && r2 instanceof ProcessNode)) 
            needLink = true;
        if (!needLink)
            return;
        Node input = (Node) r1;
        Node output = (Node) r2; 
        RenderableInteraction flowLine = new RenderableInteraction();
        flowLine.addInput(input);
        flowLine.addOutput(output);
        flowLine.layout();
        // Default using Activate. It should be changeable!
        flowLine.setInteractionType(InteractionType.ACTIVATE);
        editor.insertEdge(flowLine, false);
        editor.repaint(editor.getVisibleRect());
        elv.getZoomablePathwayEditor().flagDiagramInstance();
    }
    
    @Override
    public Action getCreateSetAndMemberLinkAction() {
        Action createSetAndMemberAction = new AbstractAction("Add EntitySet and Member Link") {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                addEntitySetAndMemberLink();
            }
        };
        return createSetAndMemberAction;
    }
    
    @Override
    public Action getCreateSetAndSetLinkAction() {
        Action createSetAndSetAction = new AbstractAction("Add EntitySet and EntitySet Link") {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSetAndSetLink();
            }
        };
        return createSetAndSetAction;
    }
    
    private void addSetAndSetLink() {
        PathwayEditor editor = elv.getZoomablePathwayEditor().getPathwayEditor();
        List<Renderable> selection = editor.getSelection();
        if (selection == null || selection.size() != 2)
            return;
        Renderable r1 = (Renderable) selection.get(0);
        Renderable r2 = (Renderable) selection.get(1);
        // Make sure the above two Renderable objects can be linked
        if (r1.getReactomeId() == null || r2.getReactomeId() == null ||
                !(r1 instanceof Node) || !(r1 instanceof Node))
            return;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        GKInstance inst1 = fileAdaptor.fetchInstance(r1.getReactomeId());
        GKInstance inst2 = fileAdaptor.fetchInstance(r2.getReactomeId());
        try {
            if (!InstanceUtilities.hasSharedMembers(inst1, inst2))
                return;
            Node node1 = (Node) r1;
            Node node2 = (Node) r2;
            // Check if there is a link existing already
            List<HyperEdge> edges = node1.getConnectedReactions();
            if (edges != null) {
                for (HyperEdge edge : edges) {
                    if (edge instanceof EntitySetAndEntitySetLink) {
                        List<Node> setNodes = ((EntitySetAndEntitySetLink)edge).getEntitySets();
                        if (setNodes.contains(node2)) {
                            JOptionPane.showMessageDialog(elv,
                                                          "Cannot add link: a link exists between the two selected EntitySet instances.",
                                                          "Error in Addling Link",
                                                          JOptionPane.ERROR_MESSAGE);
                            return;
                                                    
                        }
                    }
                }
            }
            // Add a link
            EntitySetAndEntitySetLink link = new EntitySetAndEntitySetLink();
            link.setEntitySets((Node)r1, (Node)r2);
            link.layout();
            editor.insertEdge(link, false);
            editor.repaint(editor.getVisibleRect());
            elv.getZoomablePathwayEditor().flagDiagramInstance();
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(elv, 
                                          "Error in adding EntitySet and EntitySet link: " + e,
                                          "Error in Adding Link",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addEntitySetAndMemberLink() {
        PathwayEditor editor = elv.getZoomablePathwayEditor().getPathwayEditor();
        List<Renderable> selection = editor.getSelection();
        if (selection == null || selection.size() != 2)
            return;
        Renderable r1 = (Renderable) selection.get(0);
        Renderable r2 = (Renderable) selection.get(1);
        // Make sure the above two Renderable objects can be linked
        if (r1.getReactomeId() == null || r2.getReactomeId() == null ||
            !(r1 instanceof Node) || !(r1 instanceof Node))
            return;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        GKInstance inst1 = fileAdaptor.fetchInstance(r1.getReactomeId());
        GKInstance inst2 = fileAdaptor.fetchInstance(r2.getReactomeId());
        // Need to find Set and member
        Node set = null;
        Node member = null;
        try {
            if (inst1.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                if (InstanceUtilities.isEntitySetAndMember(inst1, inst2)) {
                    set = (Node) r1;
                    member = (Node) r2;
                }
            }
            if (set == null || member == null) {
                if (inst2.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                    if (InstanceUtilities.isEntitySetAndMember(inst2, inst1)) {
                        set = (Node) r2;
                        member = (Node) r1;
                    }
                }
            }
            if (set == null || member == null)
                return;
            // Check if a link has been there already
            List<HyperEdge> edges = member.getConnectedReactions();
            for (HyperEdge edge : edges) {
                if (edge instanceof EntitySetAndMemberLink) {
                    Node tempSet = ((EntitySetAndMemberLink)edge).getEntitySet();
                    if (tempSet == set) {
                        JOptionPane.showMessageDialog(elv,
                                                      "Cannot create a link: there is an existed link between the selected EntitySet\n" +
                                                      "and its member.",
                                                      "Error in Creating Link",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }
            // Add a link
            EntitySetAndMemberLink link = new EntitySetAndMemberLink();
            link.setEntitySet(set);
            link.setMember(member);
            link.layout();
            editor.insertEdge(link, false);
            editor.repaint(editor.getVisibleRect());
            elv.getZoomablePathwayEditor().flagDiagramInstance();
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(elv,
                                          "Cannot create a link between an EntitySet and its member: " + e,
                                          "Error in Creating Link",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public Action getExpandDiagramAction() {
        if (expandDiagramAction != null)
            return expandDiagramAction;
        expandDiagramAction = new AbstractAction("Expand Diagram") {
            public void actionPerformed(ActionEvent e) {
                expandDiagram();
            }
        };
        return expandDiagramAction;
    }
    
    private void expandDiagram() {
        PathwayEditor editor = elv.getZoomablePathwayEditor().getPathwayEditor();
        // Get the selected process node
        List selection = editor.getSelection();
        if (selection == null || selection.size() != 1)
            return;
        Renderable r = (Renderable) selection.get(0);
        if (!(r instanceof ProcessNode))
            return;
        // Give the user an warning
        int reply = JOptionPane.showConfirmDialog(elv,
                                                  "Are you use you want to expand the selected sub-pathway in the current diagram?",
                                                  "Expanding Diagram Confirmation", 
                                                  JOptionPane.OK_CANCEL_OPTION);
        if (reply != JOptionPane.OK_OPTION)
            return;
        // Delete this process node
        XMLFileAdaptor fileAdaptor = elv.getZoomablePathwayEditor().getXMLFileAdaptor();
        GKInstance pathway = fileAdaptor.fetchInstance(r.getReactomeId());
        // Check if a diagram can be fetched for this process node
        RenderablePathway diagram = elv.getDiagramHandler().getDiagram(pathway, elv);
        if (diagram == null)
            return ;
        // Delete any links to this ProcessNode
        List<Renderable> toBeDeleted = new ArrayList<Renderable>();
        for (Iterator it = editor.getDisplayedObjects().iterator();
             it.hasNext();) {
            Renderable r1 = (Renderable) it.next();
            if (r1 instanceof FlowLine) {
                FlowLine fl = (FlowLine) r1;
                if (fl.getInputNode(0) == r ||
                    fl.getOutputNode(0) == r)
                    toBeDeleted.add(r1);
            }
        }
        toBeDeleted.add(r);
        elv.getZoomablePathwayEditor().disableExitenceCheck(true);
        for (Renderable r1 : toBeDeleted)
            editor.delete(r1);
        elv.getZoomablePathwayEditor().disableExitenceCheck(false);
        expandDigram(editor, 
                     fileAdaptor, 
                     diagram);
    }

    /**
     * This method is used to expand a diagram from a sub-pathway. Three steps are included in this method:
     * 1. Add all objects from this diagram
     * 2. Delete HyperEdge instances that are in the diagram already since no shortcuts for HyperEdges are 
     * allowed.
     * 3. Delete entities linked to HyperEdges deleted in 2, but not linked to other edges.
     */
    private void expandDigram(PathwayEditor editor,
                              XMLFileAdaptor fileAdaptor,
                              RenderablePathway diagram) {
        if (diagram.getComponents() == null || diagram.getComponents().size() == 0)
            return; // Nothing in this diagram.
        // Add diagram from the pathway
        List<Renderable> list = new ArrayList<Renderable>();
        try {
            RenderablePathway container = (RenderablePathway) editor.getRenderable();
            diagram.setHideCompartmentInNode(container.getHideCompartmentInNode());
            diagram = elv.getDiagramHandler().cloneDiagram(diagram, fileAdaptor);
            List<Renderable> original = new ArrayList<Renderable>(editor.getDisplayedObjects());
            // Step 1: Add all objects from the diagram
            for (Iterator it = diagram.getComponents().iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                container.addComponent(r);
                list.add(r);
                if (r.getReactomeId() != null) {
                    GKInstance instance = fileAdaptor.fetchInstance(r.getReactomeId());
                    if (instance != null && instance.getSchemClass().isa(ReactomeJavaConstants.Event))
                        instance.setAttributeValueNoCheck("isOnElv", Boolean.TRUE);
                }
            }
            // Step 2: Delete HyperEdge that are in the diagram already
            // Used to track Entities linked to edges to be deleted
            Set<Node> linkedEntities = new HashSet<Node>();
            for (Renderable r : list) {
                if (!(r instanceof HyperEdge))
                    continue;
                HyperEdge edge = (HyperEdge) r;
                if (edge.getReactomeId() == null)
                    continue;
                // Check if this edge has been in the original display
                Long reactomeId = edge.getReactomeId();
                for (Renderable r0 : original) {
                    if (r0.getReactomeId() != null && 
                        r0.getReactomeId().equals(reactomeId)) {
                        List<Node> linked = edge.getConnectedNodes();
                        linkedEntities.addAll(linked);
                        editor.delete(edge);
                    }
                }
            }
            // Step 3: delete nodes linked to deleted edges, but not to others.
            for (Node node : linkedEntities) {
                if (node.getConnectedReactions() == null || node.getConnectedReactions().size() == 0) {
                    editor.delete(node);
                }
            }
//            // Nodes should be added, which are based on linked nodes that are not in the current diagram
//            // Need to copy all components
//            for (Iterator it = diagram.getComponents().iterator(); it.hasNext();) {
//                Renderable r1 = (Renderable) it.next();
//                // Don't add it if it is there already
//                GKInstance instance = fileAdaptor.fetchInstance(r1.getReactomeId());
//                Renderable existed;
//                if (AttributeEditConfig.getConfig().isMultipleCopyEntity(instance)) 
//                    existed = null; // Always create a new entity
//                else 
//                    existed = elv.getZoomablePathwayEditor().getFreeFormObject(instance);
//                if (existed == null) {
//                    container.addComponent(r1);
//                    list.add(r1);
//                    if (instance.getSchemClass().isa(ReactomeJavaConstants.Event)) {
//                        instance.setAttributeValueNoCheck("isOnElv", Boolean.TRUE);
//                    }
//                }
//                else {
//                    mergeConnectionInfo(r1, // r1 is from another diagram 
//                                        existed);
//                }
//            }
            // Reset all house keeping ids
            RenderableRegistry.getRegistry().open(container);
            RenderableRegistry.getRegistry().resetAllIdsInPathway(container);
            editor.setSelection(list);
            editor.repaint(editor.getVisibleRect());
        }
        catch(Exception e) {
            System.err.println("ElvActionCollection.expandDiagram(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(elv, 
                                          "Error in expanding diagram: " + e,
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * This helper method is used to merge the connection information from the source to the 
     * target.
     * @param source
     * @param target
     */
    private void mergeConnectionInfo(Renderable source,
                                     Renderable target) {
        if (source instanceof HyperEdge)
            return;
        // Source and target should have the same reactomeId
        if (!source.getReactomeId().equals(target.getReactomeId()))
            return;
        ConnectInfo sourceInfo = source.getConnectInfo();
        if (sourceInfo == null)
            return;
        ConnectInfo targetInfo = target.getConnectInfo();
        java.util.List connectWidgets = sourceInfo.getConnectWidgets();
        if (connectWidgets != null && connectWidgets.size() > 0) {
            ConnectWidget widget = null;
            if (targetInfo == null) {
                targetInfo = new NodeConnectInfo();
                target.setConnectInfo(targetInfo);
            }
            // The list of connectWidgets is changed after disconnect.
            List tmpList = new ArrayList(connectWidgets);
            for (Iterator it = tmpList.iterator(); it.hasNext();) {
                widget = (ConnectWidget) it.next();
                widget.disconnect();
                widget.setConnectedNode((Node)target);
                widget.connect();
                widget.invalidate();
                targetInfo.addConnectWidget(widget);
            }
        }
    }
    
    public Action getAutoAssignCompAction() {
        if (autoAssigCompAction == null) {
            autoAssigCompAction = new AbstractAction("Auto-Assign Compartments",
                                                     AuthorToolAppletUtilities.createImageIcon("AutoCompartment.gif")) {
                public void actionPerformed(ActionEvent e) {
                    elv.getZoomablePathwayEditor().assignCompartments();
                }
            };
            autoAssigCompAction.putValue(Action.SHORT_DESCRIPTION, "Auto-assign compartments");
        }
        return autoAssigCompAction;
    }
    
    /**
     * A helper method used to create a JToolbar.
     * @return
     */
    public JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        List<JButton> buttons = new ArrayList<JButton>();
        JButton btn = toolbar.add(getCutAction());
        buttons.add(btn);
        btn = toolbar.add(getCopyAction());
        buttons.add(btn);
        btn = toolbar.add(getPasteAsAliasAction());
        buttons.add(btn);
        btn = toolbar.add(getCloneAction());
        buttons.add(btn);
        btn = toolbar.add(getDeleteAction());
        buttons.add(btn);
        btn = toolbar.add(getUndoAction());
        buttons.add(btn);
        btn = toolbar.add(getRedoAction());
        buttons.add(btn);
        btn = toolbar.add(getLayoutAction());
        buttons.add(btn);
        btn = toolbar.add(getLayoutEdgesAction());
        buttons.add(btn);
        btn = toolbar.add(getAutoAssignCompAction());
        buttons.add(btn);
        btn = toolbar.add(getAutoPrecedingEventAction());
        buttons.add(btn);
        PathwayEditorInsertActions peis = new PathwayEditorInsertActions();
        peis.setPathwayEditor(pathwayEditor);
        List<Action> insertActions = peis.getInsertActions();
        toolbar.addSeparator();
        for (Action action : insertActions) {
            if (action.getValue(Action.NAME).equals("Compartment")) {
                action = createInsertCompartmentAction();
            }
            btn = toolbar.add(action);
            buttons.add(btn);
        }
        // Need to remove any margins under non-mac system.
        if (!GKApplicationUtilities.isMac()) {
            Insets insets = new Insets(0, 0, 0, 0);
            for (JButton tmp : buttons)
                tmp.setMargin(insets);
        }
        updateSelectRelatedAction();
        return toolbar;
    }
    
    private Action createInsertCompartmentAction() {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                insertCompartment();
            }
        };
        Icon icon = RenderableFactory.getFactory().getIcon(RenderableCompartment.class);
        action.putValue(Action.SMALL_ICON, icon);
        action.putValue(Action.SHORT_DESCRIPTION, "Insert compartment");
        action.putValue(Action.NAME, "Compartment");
        return action;
    }
    
    private void insertCompartment() {
        // Select for a new compartment instance
        JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, pathwayEditor);
        InstanceSelectDialog dialog1 = new InstanceSelectDialog(parentFrame, "Select a Compartment");
        List<SchemaClass> topLevelClasses = new ArrayList<SchemaClass>();
        SchemaClass compartmentCls = elv.getZoomablePathwayEditor().getXMLFileAdaptor().getSchema().getClassByName(ReactomeJavaConstants.Compartment);
        topLevelClasses.add(compartmentCls);
        dialog1.setTopLevelSchemaClasses(topLevelClasses);
        dialog1.setIsMultipleValue(false);
        dialog1.setModal(true);
        dialog1.setSize(1000, 700);
        GKApplicationUtilities.center(dialog1);
        dialog1.setVisible(true); 
        if (dialog1.isOKClicked()) {
            java.util.List instances = dialog1.getSelectedInstances();
            if (instances == null || instances.size() == 0)
                return;
            GKInstance compartment = (GKInstance) instances.get(0);
            RenderableCompartment rCompartment = new RenderableCompartment();
            rCompartment.setReactomeId(compartment.getDBID());
            rCompartment.setDisplayName(compartment.getDisplayName());
            pathwayEditor.insertCompartment(rCompartment);
        }
    }
    
    /**
     * Set the target editor these actions will be applied to.
     * @param editor
     */
    public void setPathwayEditor(PathwayEditor editor) {
        this.pathwayEditor = editor;
        editor.getSelectionModel().addGraphEditorActionListener(new GraphEditorActionListener() {
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() == GraphEditorActionEvent.SELECTION)
                    updateSelectRelatedAction();
            }
        });
        // Used to popup the popup menu
        editor.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    popupManager.doPathwayEditorPopup(pathwayEditor, e.getPoint());
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    popupManager.doPathwayEditorPopup(pathwayEditor, e.getPoint());
            }
        });
    }
    
    @Override
    protected void hideComponents(boolean isHidden) {
        List selection = pathwayEditor.getSelection();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (!(obj instanceof RenderableComplex))
                continue;
            RenderableComplex complex = (RenderableComplex) obj;
            if (isHidden)
                hideComplexComponents(complex);
            else
                showComplexComponents(complex);
        }
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        enableSave();
    }
    
    private void hideComplexComponents(RenderableComplex complex) {
        List components = RenderUtility.getAllDescendents(complex);
        if (components == null || components.size() == 0)
            return;
        InstanceZoomablePathwayEditor zoomableEditor = elv.getZoomablePathwayEditor();
        zoomableEditor.disableExitenceCheck(true);
        for (Iterator it = components.iterator(); it.hasNext();) {
            Node node = (Node) it.next();
            complex.removeComponent(node);
            pathwayEditor.delete(node);
            node.setContainer(null);
        }
        complex.hideComponents(true);
        complex.invalidateBounds();
        complex.invalidateConnectWidgets();
        zoomableEditor.disableExitenceCheck(false);
    }
    
    private void showComplexComponents(final RenderableComplex complex) {
        InstanceZoomablePathwayEditor zoomableEditor = elv.getZoomablePathwayEditor();
        XMLFileAdaptor fileAdpator = zoomableEditor.getXMLFileAdaptor();
        GKInstance complexInst = fileAdpator.fetchInstance(complex.getReactomeId());
        try {
            List list = complexInst.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            if (list == null || list.size() == 0)
                return; // Just a sanity check
            complex.hideComponents(false);
            // Want to create components now
            for (Iterator it = list.iterator(); it.hasNext();) {
                GKInstance comp = (GKInstance) it.next();
                zoomableEditor.addComplexComponent(complex, comp);
            }
            complex.layout();
            complex.invalidateBounds();
            complex.invalidateConnectWidgets();
            pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        }
        catch(Exception e) {
            System.err.println("ElvActionCollection.showComplexComponent(): " + e);
            e.printStackTrace();
        }
    }

    @Override
    protected boolean isShowComponentActionNeeded(RenderableComplex complex) {
        XMLFileAdaptor fileAdaptor = elv.getZoomablePathwayEditor().getXMLFileAdaptor();
        GKInstance complexInst = fileAdaptor.fetchInstance(complex.getReactomeId());
        try {
            // A complex DefinedSet can be mapped to RenderableComplex. This should be excluded
            if (!complexInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
                return false;
            List components = complexInst.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            return components != null && components.size() > 0;
        }
        catch(Exception e) {
            System.err.println("ElvActionCollection.isShowComponentActionNeeded(): " + e);
            e.printStackTrace();
        }
        return false;
    }
    
    public Action getOpenDiagramAction() {
        if (openDiagramAction == null) {
            openDiagramAction = new AbstractAction("Open Diagram") {
                public void actionPerformed(ActionEvent e) {
                    List<GKInstance> selections = elv.getTreePane().getSelection();
                    // Want to get the selected event
                    GKInstance pathway = null;
                    for (GKInstance instance : selections) {
                        if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                            pathway = instance;
                            break;
                        }
                    }
                    if (pathway == null)
                        return;
                    try {
                        elv.displayEvent(pathway);
                    }
                    catch(Exception e1) {
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(elv,
                                                      "Error in openning diagram: " + e1, 
                                                      "Error in Openning Diagram",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
        }
        return openDiagramAction;
    }
    
    public Action getEncapsulateDiagramAction() {
        if (encapsulateDiagramAction == null) {
            encapsulateDiagramAction = new AbstractAction("Encapsulate Diagram") {
                public void actionPerformed(ActionEvent e) {
                    encapsulateDiagram();
                }
            };
        }
        return encapsulateDiagramAction;
    }
    
    private void encapsulateDiagram() {
        List<GKInstance> selections = elv.getTreePane().getSelection();
        if (selections.size() != 1)
            return;
        // Want to get the selected event
        GKInstance pathway = (GKInstance) selections.get(0);
        if (!pathway.getSchemClass().isa(ReactomeJavaConstants.Pathway))
            return;
        try {
            PathwayEncapsulateHelper helper = new PathwayEncapsulateHelper();
            helper.encapsulateDiagram(pathway, elv);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(elv.getTreePane(),
                                          "Cannot encapsulate the selected pathway: " + e,
                                          "Error in Diagram Encapsulating",
                                          JOptionPane.ERROR_MESSAGE);
            System.err.println("ElvActionCollection.encapsulateDiagram(): " + e);
            e.printStackTrace();
        }
    }
    
    public Action getRemoveCompartFromNameAction() {
        if (hideCompartmentInNameAction == null) {
            hideCompartmentInNameAction = new AbstractAction("Hide Compartment in Name") {
                public void actionPerformed(ActionEvent e) {
                    toggleCompartmentInName();
                }
            };
        }
        // Check the caption for the action
        RenderablePathway diagram = (RenderablePathway) elv.getZoomablePathwayEditor().getPathwayEditor().getRenderable();
        if (diagram.getHideCompartmentInNode())
            hideCompartmentInNameAction.putValue(Action.NAME, "Show Compartment in Name");
        else
            hideCompartmentInNameAction.putValue(Action.NAME, "Hide Compartment in Name");
        return hideCompartmentInNameAction;
    }
    
    private void toggleCompartmentInName() {
        RenderablePathway diagram = (RenderablePathway) elv.getZoomablePathwayEditor().getPathwayEditor().getRenderable();
        boolean hideCompnent = diagram.getHideCompartmentInNode();
        if (hideCompnent) {
            // Need to show the compartment
            diagram.setHideCompartmentInNode(false);
            // Use the default display name for Nodes
            if (diagram.getComponents() != null) {
                XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
                for (Object r : diagram.getComponents()) {
                    if (!(r instanceof Node))
                        continue;
                    Node node = (Node) r;
                    GKInstance instance = fileAdaptor.fetchInstance(node.getReactomeId());
                    if (instance == null)
                        continue;
                    node.setDisplayName(instance.getDisplayName());
                }
            }
        }
        else {
            diagram.setHideCompartmentInNode(true);
            RenderUtility.hideCompartmentInNodeName(diagram);
        }
        PathwayEditor editor = elv.getZoomablePathwayEditor().getPathwayEditor();
        editor.repaint(editor.getVisibleRect());
    }

    @Override
    protected Action getEditPropertiesAction() {
        Action editPropertiesAction = new AbstractAction("Edit Properties") {
            public void actionPerformed(ActionEvent e) {
                elv.getZoomablePathwayEditor().showPropertyForSelection();
            }
        };
        return editPropertiesAction;
    }
    
    @Override 
    protected void cut() {
        if (elv.isUsedAsDrawingTool()) {
            JOptionPane.showMessageDialog(SwingUtilities.getAncestorOfClass(JFrame.class, elv), 
                                          "Cut is not supported in the ELV drawing mode.",
                                          "Cut Not Supported",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        super.cut();
    }
    
    private boolean isDeletableFlowLineInDrawing(FlowLine flowLine) {
        if (flowLine instanceof EntitySetAndMemberLink ||
            flowLine instanceof EntitySetAndEntitySetLink)
            return true;
        Node input = flowLine.getInputNode(0);
        Node output = flowLine.getOutputNode(0);
        if (input == null || output == null)
            return true;
        if (input instanceof ProcessNode &&
            !(output instanceof ProcessNode))
            return true;
        if (output instanceof ProcessNode &&
            !(input instanceof ProcessNode))
            return true;
        return false;
    }
    
    /**
     * Override to provide some checks
     */
    @Override
    protected void delete() {
        java.util.List selection = pathwayEditor.getSelection();
        if (selection == null || selection.size() == 0)
            return;
        InstanceZoomablePathwayEditor zoomableEditor = elv.getZoomablePathwayEditor();
        if (zoomableEditor.isUsedAsDrawingTool()) {
            // Check to make sure selected entities have no reaction linked
            for (Iterator it = selection.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r instanceof FlowLine) {
                    FlowLine flowLine = (FlowLine) r;
                    if (!isDeletableFlowLineInDrawing(flowLine)) {
                        // Check if this flow line is between ProcessNode and normal node
                        String message = "In the ELV drawing mode, a flow arrow that is used to link pathway nodes cannot be deleted!\n" +
                                         "Tip: Use attribute edit table to remove links between pathway nodes.";
                        JOptionPane.showMessageDialog(zoomableEditor,
                                                      message,
                                                      "Delete Error",
                                                      JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                else if ((r instanceof Node) && !(r instanceof ProcessNode)) {
                    // Check if any linked reactions has also been selected, if not,
                    // generate a dialog, and abort deletion.
                    List<HyperEdge> edges = ((Node)r).getConnectedReactions();
                    // Check if these edges are in the selection
                    for (HyperEdge edge : edges) {
                        if (!selection.contains(edge)) {
                            String message = "In the ELV drawing mode, an Entity cannot be deleted if it is linked to a reaction!\n" +
                                             "Tip: If you want to re-link a reaction to an alias, use unlink/re-link feature.";
                            JOptionPane.showMessageDialog(zoomableEditor,
                                                          message,
                                                          "Delete Error",
                                                          JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
            }
        }
        String msg = "Are you sure you want to delete the selected object"
            + ((selection.size() == 1) ? "?" : "s?");
        int reply =
            JOptionPane.showConfirmDialog(zoomableEditor, // Use toolPane to make the focus correctly. Otherwise, the focus might be shifted to panel
                                          msg,
                                          "Delete Confirmation",
                                          JOptionPane.YES_NO_CANCEL_OPTION);
        if (reply != JOptionPane.YES_OPTION)
            return;
        // Ask the user if she wants to delete any connected entities that are not linked to other reactions
        Set<Node> linkedNodes = new HashSet<Node>();
        boolean hasFlowLine = false;
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof HyperEdge) {
                List<Node> nodes = ((HyperEdge)obj).getConnectedNodes();
                linkedNodes.addAll(nodes);
                if (obj instanceof FlowLine)
                    hasFlowLine = true;
            }
        }
        linkedNodes.removeAll(selection);
        // Check if these nodes have been linked to other Reactions
        for (Iterator<Node> it = linkedNodes.iterator(); it.hasNext();) {
            Node node = it.next();
            List<HyperEdge> edges = node.getConnectedReactions();
            edges.removeAll(selection);
            if (edges.size() > 0)
                it.remove();
        }
        if (linkedNodes.size() > 0) {
            msg = "One or more objects are linked to " + 
                  (hasFlowLine ? "edge(s)" : "reaction(s)") +
                  " to be deleted and\n" +
            	  "not linked to other " +
                  (hasFlowLine ? "edge(s)" : "reaction(s)") +
                  ". Do you want to delete these \n" +
            	  "objects too?";
            reply = JOptionPane.showConfirmDialog(zoomableEditor, 
                                                  msg,
                                                  "Delete Linked Objects?",
                                                  JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION)
                return;
            if (reply == JOptionPane.YES_OPTION) {
                // This is a hack. The internal structure should not be broken
                selection.addAll(linkedNodes);
            }
        }
        // Check if any flow line should be deleted for ProcessNode
        // Place this block under the above statements since there is an cancel option.
        // Otherwise the selection will be changed in the user cancels.
        if (zoomableEditor.isUsedAsDrawingTool()) {
            List<HyperEdge> toBeDeleted = new ArrayList<HyperEdge>();
            for (Iterator it = selection.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r instanceof ProcessNode) {
                    ProcessNode node = (ProcessNode) r;
                    List<HyperEdge> edges = node.getConnectedReactions();
                    if (edges != null)
                        toBeDeleted.addAll(edges); // All HyperEdges linked to ProcessNodes should be deleted too.
                }
            }
            selection.addAll(toBeDeleted);
        }
        UndoableDeleteEdit edit = new UndoableDeleteEdit(pathwayEditor, new ArrayList<Renderable>(selection));
        pathwayEditor.deleteSelection();
        pathwayEditor.addUndoableEdit(edit);
        updateSelectRelatedAction();
    }

    @Override
    protected void setDoNotReleaseEventVisible(boolean visible) {
        elv.getZoomablePathwayEditor().setDoNotReleaseEventVisible(visible);
    }
    
}
