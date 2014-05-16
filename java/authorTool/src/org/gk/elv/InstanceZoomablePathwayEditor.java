/*
 * Created on Nov 19, 2008
 *
 */
package org.gk.elv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;

import org.gk.database.AttributeEditEvent;
import org.gk.database.AttributeEditManager;
import org.gk.database.FrameManager;
import org.gk.gkCurator.authorTool.InstanceHandler;
import org.gk.gkCurator.authorTool.InstanceHandlerFactory;
import org.gk.gkCurator.authorTool.RenderableHandler;
import org.gk.gkCurator.authorTool.RenderableHandlerFactory;
import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.AttachActionEvent;
import org.gk.graphEditor.ConnectionPopupManager;
import org.gk.graphEditor.DetachActionEvent;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.*;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.gk.util.DialogControlPane;

/**
 * Wrapper around the original ZoomablePathwayEditor to do instance and renderable
 * converting.
 * @author wgm
 *
 */
public class InstanceZoomablePathwayEditor extends ZoomablePathwayEditor implements Selectable {
    private XMLFileAdaptor fileAdaptor;
    private SelectionMediator selectionMediator;
    private ElvReactionEditHelper reactionHelper;
    private ElvEWASEditHandler ewasHelper;
    private ElvComplexEditHandler complexHelper;
    private ElvPathwayEditHelper pathwayHelper;
    private ElvPhysicalEntityEditHandler peHelper;
    // A flag
    private boolean changeFromEditor = false;
    // Flag indicating the process of hiding complex components
    private boolean disableExistCheck = false;
    // Flag to control is this editor is used as a pure drawing tool
    private boolean usedAsDrawingTool = false;
    // track if _doNotRelease events should be visible
    private boolean doNotReleaseEventVisible = true;
    // Used to track detach/attach nodes in a drawing mode
    // In a drawing mode, detach/attach node should be the same. 
    // Otherwise, an error should be generated, and the reaction should be linked 
    // to the original detach node
    private Renderable detached;
    
    
    public InstanceZoomablePathwayEditor() {
        init();
    }
    
    private ElvInstanceEditHandler getELVEditHandler(GKInstance instance) {
        if (instance.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
            return reactionHelper;
        if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex))
            return complexHelper;
        if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway))
            return pathwayHelper;
        if (instance.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
            return ewasHelper;
        // For some common PE related work
        if (instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
            return peHelper;
        return null;
    }
    
    @Override
    protected PathwayEditor createPathwayEditor() {
        PathwayEditor editor = new PathwayEditor() {
            @Override
            protected String getLocationLization(Renderable r) {
                if (usedAsDrawingTool) {
                    // Get localization from its instance
                    Long reactomeId = r.getReactomeId();
                    if (reactomeId == null)
                        return null;
                    GKInstance instance = fileAdaptor.fetchInstance(reactomeId);
                    if (instance == null)
                        return null;
                    try {
                        if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.compartment)) {
                            GKInstance compartment = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.compartment);
                            if (compartment != null)
                                return compartment.getDisplayName();
                        }
                    }
                    catch(Exception e) {
                        return null; // Just return.
                    }
                }
                return super.getLocationLization(r);
            }
        };
        return editor;
    }
    
    /**
     * Control if this pathway editor is used as a pure drawing tool for pathway layout. If it is used
     * as a drawing tool, editing features related to local project will be disabled.
     * @param value
     */
    public void setUsedAsDrawingTool(boolean value) {
        this.usedAsDrawingTool = value;
        pathwayEditor.setUsedAsDrawingTool(value);
        // Check toolbar buttons
        for (Component comp : toolbar.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                Action action = btn.getAction();
                String text = (String) action.getValue(Action.SHORT_DESCRIPTION);
                if (text.equals("Clone selected objects"))
                    btn.setVisible(!value);
                else if (text.startsWith("Auto-assign"))
                    btn.setVisible(!value);
                else if (text.startsWith("Insert")) {
                    if (!text.endsWith("compartment") &&
                        !text.endsWith("note") &&
                        !text.endsWith("flow arrow")) {
                        btn.setVisible(!value);
                    }
                }
            }
        }
    }
    
    public boolean isUsedAsDrawingTool() {
        return this.usedAsDrawingTool;
    }
    
    private void init() {
        // Link this selection to other displays
        pathwayEditor.getSelectionModel().addGraphEditorActionListener(new GraphEditorActionListener() {
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() == GraphEditorActionEvent.SELECTION) {
                    selectionMediator.fireSelectionEvent(InstanceZoomablePathwayEditor.this);
                }
            }
        });
        installListeners();
        reactionHelper = new ElvReactionEditHelper();
        reactionHelper.setZoomableEditor(this);
        ewasHelper = new ElvEWASEditHandler();
        ewasHelper.setZoomableEditor(this);
        complexHelper = new ElvComplexEditHandler();
        complexHelper.setZoomableEditor(this);
        pathwayHelper = new ElvPathwayEditHelper();
        pathwayHelper.setZoomableEditor(this);
        peHelper = new ElvPhysicalEntityEditHandler();
        peHelper.setZoomableEditor(this);
        // Don't want to do complex component editing, which is very error-prone
        getPathwayEditor().disableComplexComponentEdit(true);
        // We want to have a tight node
        Node.setHeightRatioOfBoundsToText(1.0);
        Node.setWidthRatioOfBoundsToText(1.0);
        // To handle connection popup
        ConnectionPopupManager popupManager = new ElvConnectionPopupManager();
        pathwayEditor.setConnectionPopupManager(popupManager);
    }
    
    public void setXMLFileAdaptor(XMLFileAdaptor fileAdaptor) {
        this.fileAdaptor = fileAdaptor;
        RenderableHandlerFactory.getFactory().setFileAdaptor(fileAdaptor);
        showEmptyDiagram();
    }

    private void showEmptyDiagram() {
        RenderablePathway pathway = new RenderablePathway();
        // As a place holder
        if (!isUsedAsDrawingTool())
            pathway.setDisplayName("Untitled");
        setDiagram(pathway); // Reset the view
    }
    
    public XMLFileAdaptor getXMLFileAdaptor() {
        return this.fileAdaptor;
    }
    
    public void disableExitenceCheck(boolean during) {
        this.disableExistCheck = during;
    }
    
    public void setSelectionMediator(SelectionMediator selectionMediator) {
        this.selectionMediator = selectionMediator;
        selectionMediator.addSelectable(this);
    }
    
    private void installListeners() {
        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                doGraphPropChange(e);
            }
        };
        pathwayEditor.addPropertyChangeListener(listener);
        GraphEditorActionListener graphListener = createGraphEditorListener();
        pathwayEditor.addGraphEditorActionListener(graphListener);
    }    
    
    private void doGraphPropChange(PropertyChangeEvent e) {
        String propName = e.getPropertyName();
        if (propName.equals("insert")) {
            Renderable newRenderable = (Renderable) e.getNewValue();
            insertNewInstance(newRenderable);
            flagDiagramInstance();
            resetIsOnElvFlag(newRenderable, Boolean.TRUE);
        }
        else if (propName.equals("delete")) {
            if (changeFromEditor)
                return;
            // It should return a list of deleted Renderable objects
            List<Renderable> deleted = (List<Renderable>) e.getNewValue();
            deleteInstances(deleted);
            flagDiagramInstance();
            for (Renderable r : deleted)
                resetIsOnElvFlag(r, Boolean.FALSE);
        }
    }
    
    private void resetIsOnElvFlag(Renderable r,
                                   Boolean value) {
        Long reactomeId = r.getReactomeId();
        if (reactomeId == null)
            return;
        GKInstance instance = fileAdaptor.fetchInstance(reactomeId);
        if (instance == null || !instance.getSchemClass().isa(ReactomeJavaConstants.Event))
            return;
        if (value)
            instance.setAttributeValueNoCheck("isOnElv", Boolean.TRUE);
        else
            instance.removeAttributeValueNoCheck("isOnElv", Boolean.TRUE);
    }
    
    protected void flagDiagramInstance() {
        GKInstance diagramInstance = getDisplayedPathwayDiagram(); 
        if (diagramInstance != null)
            fileAdaptor.markAsDirty(diagramInstance);
    }
    
    public void setDiagram(RenderablePathway pathway) {
        pathwayEditor.setRenderable(pathway);
        setTitle(pathway);
        // In the Curator Tool, RenderableRegistry is not used as a singleton any more.
        // It is more like an id management system.
        RenderableRegistry.getRegistry().clear();
        RenderableRegistry.getRegistry().registerAll(pathway);
        RenderableRegistry.getRegistry().resetNextIdFromPathway(pathway);
        // reset up
        pathwayEditor.killUndo();
        if (isUsedAsDrawingTool()) {
            if (pathway.getReactomeDiagramId() == null && pathway.getReactomeId() == null) {
                pathwayEditor.setVisible(false);
                toolbar.setVisible(false);
            }
            else {
                pathwayEditor.setVisible(true);
                toolbar.setVisible(true);
            }
        }
    }

    private void setTitle(RenderablePathway pathway) {
        if (pathway.getDisplayName() == null)
            titleLabel.setText("<html><u>Pathway Editor</u></html>");
        else
            titleLabel.setText("<html><u>Pathway Editor: " + pathway.getDisplayName() + "</u></html>");
    }
    
    /**
     * Since a diagram can be mapped to more than one Pathway instance. Please also see
     * another method {@link #getDisplayedPathwayDiagram()} should be used.
     * @return
     */
    public List<GKInstance> getDisplayedPathways() throws Exception {
        RenderablePathway r = (RenderablePathway) pathwayEditor.getRenderable();
        if (r == null)
            return new ArrayList<GKInstance>();
        return fileAdaptor.getRepresentedPathwaysInDiagram(r);
    }
    
    public GKInstance getDisplayedPathwayDiagram() {
        RenderablePathway diagram = (RenderablePathway) pathwayEditor.getRenderable();
        if (diagram == null || diagram.getReactomeDiagramId() == null)
            return null;
        return fileAdaptor.fetchInstance(diagram.getReactomeDiagramId());
    }
    
    public void assignCompartments() {
        AutoAssignmentHelper helper = new AutoAssignmentHelper(this);
        helper.assignCompartments();
    }
    
    private void deleteInstances(List<Renderable> deleted) {
        if (disableExistCheck)
            return;
        if (!usedAsDrawingTool) {
            // Used as an editing tool. Need to check if the deleted entity should be deleted from 
            // local project too during editing mode.
            JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
            CheckExistenceDialog dialog = new CheckExistenceDialog(parentFrame);
            dialog.checkAllBox.setVisible(deleted.size() > 1);
            // Need to create a dialog
            for (Renderable r : deleted) {
                GKInstance instance = fileAdaptor.fetchInstance(r.getReactomeId());
                // Compartment should not be deleted since it is not editable.
                if (instance == null || instance.getSchemClass().isa(ReactomeJavaConstants.Compartment))
                    continue; // It may be Renderable objects that cannot be converted (e.g. Text)
                // Have to check based on GKInstance. It cannot be based on the shortcuts since
                // shortcuts is not used when a diagram is generated from a GKInstance.
                List<Renderable> list = searchConvertedRenderables(instance);
                if (list.size() == 0) {
                    if (dialog.checkAllBox.isSelected()) {
                        if (dialog.isOkClicked)
                            fileAdaptor.deleteInstance(instance);
                        continue;
                    }
                    String message = "Instance, " + instance + ", is not used in the diagram any more. \n" +
                    "Do you want to delete it from the local project too?";
                    dialog.setMessage(message);
                    dialog.pack();
                    dialog.setModal(true);
                    dialog.setLocationRelativeTo(this);
                    dialog.setVisible(true);
                    if (dialog.isOkClicked)
                        fileAdaptor.deleteInstance(instance);
                }
            }
        }
        if (deleted.size() > 0)
            pathwayEditor.killUndo();
    }
    
    /**
     * Call this method if an instance has been deleted from the local project.
     * @param instance
     */
    public void deleteInstance(GKInstance instance) {
        // Check if the displayed PathwayDiagram has been deleted
        if (instance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram) && pathwayEditor.getRenderable() != null) {
            Long reactomeId = pathwayEditor.getRenderable().getReactomeId();
            if (reactomeId != null) {
                try {
                    List<?> pathways = instance.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
                    if (pathways != null) {
                        for (Object obj : pathways) {
                            GKInstance pathway = (GKInstance) obj;
                            if (pathway.getDBID().equals(reactomeId)) {
                                showEmptyDiagram();
                                return;
                            }
                        }
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // This is a apecial case since Regulation is attached to a Reaction
        if (instance.getSchemClass().isa(ReactomeJavaConstants.Regulation)) {
            try {
                GKInstance reaction = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
                if (reaction != null && reaction.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent)) {
                    GKInstance regulator = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.regulator);
                    reactionHelper.validateDisplayedRegulation(reaction, 
                                                               instance, 
                                                               regulator,
                                                               AttributeEditEvent.REMOVING);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return;
        }
        List<Renderable> list = searchConvertedRenderables(instance);
        if (list == null || list.size() == 0)
            return;
        changeFromEditor = true;
        for (Renderable r : list)
            pathwayEditor.delete(r);
        pathwayEditor.killUndo();
        changeFromEditor = false;
    }
    
    private boolean isDisplayable(GKInstance instance) {
        return instance.getSchemClass().isa(ReactomeJavaConstants.Event) ||
               instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity) ||
               instance.getSchemClass().isa(ReactomeJavaConstants.Regulation) ||
               instance.getSchemClass().isa(ReactomeJavaConstants.CatalystActivity) ||
               instance.getSchemClass().isa(ReactomeJavaConstants.ModifiedResidue) ||
               instance.getSchemClass().isa(ReactomeJavaConstants.Compartment) ||
               instance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram);
    }
    
    /**
     * Search a list of Renderable objects converted from the passed GKInstance object.
     * @param instance
     * @return
     */
    public List<Renderable> searchConvertedRenderables(GKInstance instance) {
        return searchConvertedRenderables(instance.getDBID());
    }
    
    /**
     * Search a list of Renderable objects having reactomeIds as the passed DB_ID.
     * @param dbId
     * @return
     */
    public List<Renderable> searchConvertedRenderables(Long dbId) {
        List<Renderable> list = new ArrayList<Renderable>();
        for (Iterator it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (dbId.equals(r.getReactomeId()))
                list.add(r);
        }
        return list;
    }
    
    /**
     * Call this method if any attribute editing has occurred.
     * @param editEvent
     */
    public void updateInstance(AttributeEditEvent editEvent) {
        if (changeFromEditor)
            return; // Don't need to edit self
        GKInstance instance = editEvent.getEditingInstance();
        SchemaClass schemaClass = instance.getSchemClass();
        if (!isDisplayable(instance) ||
            editEvent.getAttributeName() == null) // Just ignore it if it is a download
            return;
        // need to update _displayName
        if (editEvent.getAttributeName().equals(ReactomeJavaConstants._displayName)) {
            // Special case
            if (instance == getDisplayedPathwayDiagram()) {
                // Update title
                Renderable pathway = pathwayEditor.getRenderable();
                pathway.setDisplayName(instance.getDisplayName());
                setTitle((RenderablePathway)pathway);
            }
            else {
                List<Renderable> renderables = searchConvertedRenderables(instance);
                for (Renderable r : renderables)
                    r.setDisplayName(instance.getDisplayName());
                if (renderables.size() > 0)
                    pathwayEditor.repaint(pathwayEditor.getVisibleRect());
            }
        }
        // And inputs, outputs, etc. for reactions.
        else if (schemaClass.isa(ReactomeJavaConstants.ReactionlikeEvent)) {
            List<Renderable> renderables = searchConvertedRenderables(editEvent.getEditingInstance());
            if (renderables.size() > 0) {
                // There should be only one reaction in the diagram
                RenderableReaction reaction = (RenderableReaction) renderables.get(0);
                reactionHelper.reactionEdit(editEvent,
                                            reaction);
            }
        }
        else if (schemaClass.isa(ReactomeJavaConstants.Pathway)) {
            pathwayHelper.pathwayEdit(editEvent);
        }
        else if (schemaClass.isa(ReactomeJavaConstants.CatalystActivity)) {
            if (editEvent.getAttributeName().equals(ReactomeJavaConstants.physicalEntity)) {
                reactionHelper.catalystActivityEdit(editEvent.getEditingInstance());
            }
        }
        else if (schemaClass.isa(ReactomeJavaConstants.Regulation)) {
            reactionHelper.regulationEdit(editEvent);
        }
        else if (schemaClass.isa(ReactomeJavaConstants.Complex)) {
            complexHelper.complexEdit(editEvent);
        }
        else if (schemaClass.isa(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
            ewasHelper.ewasEdit(editEvent);
        }
        else if (schemaClass.isa(ReactomeJavaConstants.ModifiedResidue))
            ewasHelper.modifiedResidueEdit(editEvent);
        else if (schemaClass.isa(ReactomeJavaConstants.EntitySet))
            peHelper.entitySetEdit(editEvent);
    }
    
    protected Renderable addComplexComponent(RenderableComplex complex,
                                              GKInstance comp) {
        return complexHelper.addComplexComponent(complex, comp);
    }
    
    public Renderable insertInstance(GKInstance instance) {
        try {
            ElvInstanceEditHandler editHandler = getELVEditHandler(instance);
            InstanceHandler handler = InstanceHandlerFactory.getFactory().getHandler(instance);
            Renderable container = pathwayEditor.getRenderable();
            // Don't set container so that the following simpleConvert()
            // will not add the converted instance directly to this container.
            // Let the insert statments figure this out.
            //handler.setContainer(container);
            handler.setContainer(null);
            Renderable r = handler.simpleConvert(instance);
            if (editHandler != null && !editHandler.isInsertable(instance, r)) {
                JOptionPane.showMessageDialog(this,
                                              "\"" + instance.getDisplayName() + "\" cannot be inserted into the diagram.\n" +
                                              "Most likely, this instance has been in the diagram and aliasing is not allowed\n" +
                                              "for this type of instance.",
                                              "Error in Inserting",
                                              JOptionPane.ERROR_MESSAGE);
                return null;
            }
            handler.simpleConvertProperties(instance, 
                                            r, 
                                            null);
//            // TODO: This is a hack and should be changed in the future! In the above
//            // converting, the converted Renderable object has been inserted into the list 
//            // of container. However, the following method will do again. So delete it here
//            if (container.getComponents() != null && container.getComponents().contains(r))
//                container.removeComponent(r);
            if (r instanceof Node) {
                Node node = (Node) r;
                if ((container instanceof RenderablePathway) &&
                    ((RenderablePathway)container).getHideCompartmentInNode())
                    RenderUtility.hideCompartmentInNodeName(node);
                pathwayEditor.insertNode(node);
            }
            else if (r instanceof HyperEdge) {
                pathwayEditor.insertEdge((HyperEdge) r, 
                                         true);
            }
            if (editHandler != null)
                editHandler.postInsert(instance, r);
            pathwayEditor.killUndo();
            return r;
        }
        catch(Exception e) {
            System.err.println("InstanceZoomablePathwayEditor.insertInstance(): " + e);
            e.printStackTrace();
        }
        return null;
    }
    
    public void switchedType(GKSchemaClass oldCls,
                             GKInstance instance) {
        reInsertInstance(instance);
    }
    
    /**
     * Call this method if the type of instance has been changed.
     * @param instance
     */
    public void reInsertInstance(GKInstance instance) {
        List<Renderable> renderables = searchConvertedRenderables(instance);
        if (renderables == null || renderables.size() == 0)
            return;
        InstanceHandler handler = InstanceHandlerFactory.getFactory().getHandler(instance);
        try {
            for (Renderable existed : renderables) {
                Renderable r = handler.simpleConvert(instance);
                handler.simpleConvertProperties(instance, 
                                                r, 
                                                null);
                if (r instanceof Node) {
                    pathwayEditor.insertNode((Node)r);
                }
                else if (r instanceof HyperEdge)
                    pathwayEditor.insertEdge((HyperEdge) r, 
                                             true);
                RenderUtility.switchRenderInfo(existed, r);
                pathwayEditor.delete(existed);
            }
        }
        catch(Exception e) {
            System.err.println("InstanceZoomablePathwayEditor.reInsertInstance(): " + e);
            e.printStackTrace();
        }
    }
    
    public void insertReactionInFull(GKInstance reaction) {
        try {
            reactionHelper.insertReactionInFull(pathwayEditor, reaction);
        }
        catch(Exception e) {
            System.err.println("InstanceZoomablePathwayEditor.insertReactonInFull(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Get an Object for the passed instance that is not contained by any Complex.
     * @param instance
     * @return
     */
    protected Renderable getFreeFormObject(GKInstance instance) {
        List<Renderable> list = searchConvertedRenderables(instance);
        if (list == null)
            return null;
        for (Renderable r : list) {
            if (!(r.getContainer() instanceof RenderableComplex))
                return r;
        }
        return null;
    }

    private void insertNewInstance(Renderable r) {
        RenderablePathway displayedPathway = (RenderablePathway) pathwayEditor.getRenderable();
        // Check if the displayed Renderable is empty
        if (displayedPathway.getReactomeDiagramId() == null && displayedPathway.getReactomeId() == null) {
            Renderable diagram = pathwayEditor.getRenderable();
            // Need to generate a new pathway GKInstance
            JOptionPane.showMessageDialog(this,
                                          "A new pathway instance has been generated to hold the created object.\n" +
                                          "This new pathway instance has been named as \"" + diagram.getDisplayName() + "\"",
                                          "Create a Pathway",
                                          JOptionPane.INFORMATION_MESSAGE);
            GKInstance pathway = fileAdaptor.createNewInstance(ReactomeJavaConstants.Pathway);
            // Use NoCheck method to avoid try...catch...
            pathway.setAttributeValueNoCheck(ReactomeJavaConstants.name, 
                                             diagram.getDisplayName());
            InstanceDisplayNameGenerator.setDisplayName(pathway);
            diagram.setReactomeId(pathway.getDBID());
            AttributeEditManager.getManager().attributeEdit(pathway, 
                                                            ReactomeJavaConstants._displayName);
        }
        if (r instanceof Note)
            return; // Cannot convert note
        try {
            GKInstance instance = null;
            if (r.getReactomeId() == null) { // This should be a completely new instance
                // Need to change to GKInstance
                RenderableHandler handler = RenderableHandlerFactory.getFactory().getHandler(r);
                instance = handler.createNew(r);
                instance.setDisplayName(r.getDisplayName());
                AttributeEditManager.getManager().attributeEdit(instance, 
                                                                ReactomeJavaConstants._displayName);
                r.setReactomeId(instance.getDBID()); // Set back the Reactome id.
                if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.name)) {
                    instance.addAttributeValue(ReactomeJavaConstants.name, 
                                               r.getDisplayName());
                }
                // Set up some properties
                handler.convertPropertiesForNew(instance, r);
                // Check if a new Event has been added. If true, need to fire an attribute edit
                // event
                if (instance.getSchemClass().isa(ReactomeJavaConstants.Event)) {
                    Renderable container = pathwayEditor.getRenderable();
                    GKInstance containerInstance = fileAdaptor.fetchInstance(container.getReactomeId());
                    containerInstance.addAttributeValue(ReactomeJavaConstants.hasEvent, instance);
                    AttributeEditManager.getManager().attributeEdit(containerInstance,
                                                                    ReactomeJavaConstants.hasEvent);
                }
                else if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                    List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
                    if (components != null && components.size() > 0) {
                        AttributeEditManager.getManager().attributeEdit(instance,
                                                                        ReactomeJavaConstants.hasComponent);
                    }
                }
            }
            // Undo is not allowed
            pathwayEditor.killUndo();
        }
        catch(Exception exp) {
            System.err.println("InstanceZoomablePathwayEditor.doGraphPropChange(): " + exp);
            exp.printStackTrace();
        }
    }

    public List<GKInstance> getSelection() {
        // Use set in case multiple Renderables pointing to the same GKInstance.
        Set<GKInstance> set = new HashSet<GKInstance>();
        List selection = getPathwayEditor().getSelection();
        if (selection != null && selection.size() > 0) {
            for (Iterator it = selection.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                Long reactomeId = r.getReactomeId();
                GKInstance instance = fileAdaptor.fetchInstance(reactomeId);
                if (instance == null)
                    continue;
                set.add(instance);
            }
        }
        else {
            // Want to use the displayed GKInstance as the default
            RenderablePathway pathway = (RenderablePathway) getPathwayEditor().getRenderable();
            if (pathway != null) {
                try {
                    List<GKInstance> pathways = fileAdaptor.getRepresentedPathwaysInDiagram(pathway);
                    set.addAll(pathways);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<GKInstance>(set);
    }

    /**
     * Set the selection in GKInstances to this ZoomablePathwayEditor.
     */
    public void setSelection(List selection) {
        List all = getPathwayEditor().getDisplayedObjects();
        if (all == null || all.size() == 0)
            return; // Nothing to be selected
        // Convert selection to rectome id to fast search
        Set<Long> selectedIds = new HashSet<Long>();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            selectedIds.add(instance.getDBID());
            if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                // Select all contained events too
                addContainedEvents(instance, selectedIds);
            }
        }
        List<Renderable> renderables = new ArrayList<Renderable>();
        for (Iterator it = all.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            Long reactomeId = r.getReactomeId();
            if (selectedIds.contains(reactomeId)) {
                renderables.add(r);
            }
        }
        getPathwayEditor().setSelection(renderables);
    }
    
    private void addContainedEvents(GKInstance instance,
                                    Set<Long> ids) {
        try {
            Set<GKInstance> containedEvents = InstanceUtilities.getContainedEvents(instance);
            if (containedEvents == null || containedEvents.size() == 0)
                return;
            for (GKInstance event : containedEvents) {
                ids.add(event.getDBID());
            }
        }
        catch(Exception e) {
            System.err.println("InstanceZoomablePathwayEditor.addContainedEvents(): " + e);
            e.printStackTrace();
        }
    }
    
    private GraphEditorActionListener createGraphEditorListener() {
        GraphEditorActionListener listener = new GraphEditorActionListener() {
            public void graphEditorAction(GraphEditorActionEvent e) {
                // Double click
                if (e.getID() == GraphEditorActionEvent.ACTION_DOUBLE_CLICKED) {
                    showPropertyForSelection();
                }
                else if (e.getID() == GraphEditorActionEvent.REACTION_ATTACH ||
                         e.getID() == GraphEditorActionEvent.REACTION_DETACH) {
                    changeFromEditor = true;
                    validateReactionAttributes(e);
                    changeFromEditor = false;
                }
                else if (e.getID() == GraphEditorActionEvent.NAME_EDITING) {
                    Object obj = e.getSource();
                    rename((Renderable)obj);
                }
                if (e.isSavableEvent())
                    flagDiagramInstance();
            }
        };
        return listener;
    }
    
    /**
     * Handle rename.
     * @param r
     */
    private void rename(Renderable r) {
        if (r.getReactomeId() == null)
            return;
        GKInstance instance = fileAdaptor.fetchInstance(r.getReactomeId());
        instance.setDisplayName(r.getDisplayName());
        // Use this method to avoid try ... catch ...
        instance.setAttributeValueNoCheck(ReactomeJavaConstants.name, r.getDisplayName());
        AttributeEditManager.getManager().attributeEdit(instance,
                                                        ReactomeJavaConstants._displayName);
    }
    
    /**
     * Used to handle attach/detach in the drawing mode. Make sure detach/attach working
     * on the same instance.
     * @param e
     */
    private void handleDetachAttachInDrawingMode(GraphEditorActionEvent e) {
        HyperEdge edge = (HyperEdge) e.getSource();
        if (e.getID() == GraphEditorActionEvent.REACTION_DETACH) {
            DetachActionEvent detachEvent = (DetachActionEvent) e;
            detached = detachEvent.getDetached();
        }
        else if (e.getID() == GraphEditorActionEvent.REACTION_ATTACH) {
            AttachActionEvent attachEvent = (AttachActionEvent) e;
            Renderable attached = attachEvent.getAttached();
            // It is possible that attached is just null in case a null linking
            if ((attached == null && detached != null) ||
                (attached != null && detached == null) ||
                (attached != null && detached != null && !detached.getReactomeId().equals(attached.getReactomeId()))) { // Use equal, not == to check reactomeId since it is a Long object
                String message = "In the ELV drawing mode, an edge can only be linked to an alias to the original detached entity.";
                JOptionPane.showMessageDialog(this,
                                              message,
                                              "Error in Linking",
                                              JOptionPane.ERROR_MESSAGE);
                // Link to the original one
                relinkNode(attachEvent);
            }
        }
    }
    
    private void relinkNodeForFlowLine(AttachActionEvent attachEvent) {
        FlowLine flowLine = (FlowLine) attachEvent.getSource();
        Node node = (Node) detached;
        ConnectWidget widget = attachEvent.getConnectWidget();
        if (widget != null) {
            // Have to hard link it
            if (widget.getRole() == HyperEdge.INPUT) {
                flowLine.removeInput(0);
                flowLine.addInput(node);
            }
            else if (widget.getRole() == HyperEdge.OUTPUT) {
                flowLine.removeOutput(0);
                flowLine.addOutput(node);
            }
        }
        else { // This is a deletion: a branch is deleted and the widget is gone.
            // Have to hard link it
            if (attachEvent.getRole() == HyperEdge.INPUT) {
                flowLine.addInput(node);
            }
            else if (attachEvent.getRole() == HyperEdge.OUTPUT) {
                flowLine.addOutput(node);
            }
        }
    }

    private void relinkNode(AttachActionEvent attachEvent) {
        if (!(detached instanceof Node))
            return;
        if (attachEvent.getSource() instanceof FlowLine) {
            relinkNodeForFlowLine(attachEvent);
            return;
        }
        Node node = (Node) detached;
        ConnectWidget widget = attachEvent.getConnectWidget();
        if (widget != null) {
            // Have to hard link it
            widget.setConnectedNode(node);
            widget.invalidate();
            widget.connect();
        }
        else { // This is a deletion: a branch is deleted and the widget is gone.
            int role = attachEvent.getRole();
            HyperEdge reaction = (HyperEdge) attachEvent.getSource();
            switch (role) {
                case HyperEdge.INPUT :
                    reaction.addInput(node);
                    break;
                case HyperEdge.OUTPUT :
                    reaction.addOutput(node);
                    break;
                case HyperEdge.CATALYST :
                    reaction.addHelper(node);
                    break;
                case HyperEdge.ACTIVATOR :
                    reaction.addActivator(node);
                    break;
                case HyperEdge.INHIBITOR :
                    reaction.addInhibitor(node);
                    break;
            }
        }
    }
    
    /**
     * A helper method to make sure reaction's inputs, outputs, catalysts and modifiers
     * are still correct after a detach or attach event.
     * @param edge
     */
    private void validateReactionAttributes(GraphEditorActionEvent e) {
        // In a drawing mode, no need to check the original reaction attributes
        if (usedAsDrawingTool) {
            handleDetachAttachInDrawingMode(e);
            return;
        }
        try {
            HyperEdge edge = (HyperEdge) e.getSource();
            reactionHelper.validateReactionAttributes(edge);
        }
        catch(Exception exp) {
            System.err.println("InstanceZoomablePathwayEditor.validateReactionAttributes(): " + exp);
            exp.printStackTrace();
        }
    }
    
    private void handleNoteClick(Note note) {
        pathwayEditor.setIsEditing(true);
        pathwayEditor.setEditingNode(note);
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
    }
    
    public void setDoNotReleaseEventVisible(boolean visible) {
        if (pathwayEditor.getDisplayedObjects() == null)
            return;
        try {
            for (Iterator it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r.getReactomeId() != null) {
                    GKInstance inst = fileAdaptor.fetchInstance(r.getReactomeId());
                    if (inst == null)
                        continue;
                    if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants._doRelease)) {
                        Boolean doRelease = (Boolean) inst.getAttributeValue(ReactomeJavaConstants._doRelease);
                        if (doRelease != null && doRelease)
                            continue;
                        setDoNotReleaseEventVisible(inst, r, visible);
                    }
                }
            }
            // Make sure FlowLines are not hanging there
            for (Iterator<?> it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r instanceof FlowLine) {
                    FlowLine link = (FlowLine) r;
                    Node input = link.getInputNode(0);
                    Node output = link.getOutputNode(0);
                    if (input == null || !input.isVisible() ||
                        output == null || !output.isVisible())
                        link.setIsVisible(false);
                    else
                        link.setIsVisible(true);
                }
            }
            pathwayEditor.repaint(pathwayEditor.getVisibleRect());
            this.doNotReleaseEventVisible = visible;
        }
        catch(Exception e) {
            System.err.println("EvlActionCollection.setDoNotReleaseEventVisible(): " + e);
            e.printStackTrace();
        }
    }
    
    protected void updateDoNotReleaseEventVisible(GKInstance instance) {
        if (doNotReleaseEventVisible)
            return; // Don't need to do anything since it is always displayed
        List<Renderable> r = searchConvertedRenderables(instance);
        try {
            Boolean doRelease = (Boolean) instance.getAttributeValue(ReactomeJavaConstants._doRelease);
            for (Renderable r1 : r) {
                setDoNotReleaseEventVisible(instance, 
                                            r1,
                                            doRelease == null ? false : doRelease);
            }
            pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        }
        catch(Exception e) {
            System.err.println("InstanceZoomablePathwayEditor.updateDoNotReleaseEventVisible(): " + e);
            e.printStackTrace();
        }
    }
    
    private void setDoNotReleaseEventVisible(GKInstance event,
                                             Renderable r,
                                             boolean visible) throws Exception {
        // r may be a ProcessNode
        if (r instanceof ProcessNode) {
            r.setIsVisible(visible);
            List<HyperEdge> edges = ((ProcessNode)r).getConnectedReactions();
            for (HyperEdge edge : edges) {
                if (edge instanceof FlowLine) {
                    edge.setIsVisible(visible);
                }
            }
        }
        else if (r instanceof HyperEdge) {
            HyperEdge edge = (HyperEdge) r;
            edge.setIsVisible(visible);
            // Want to check linked nodes
            List<Node> linked = edge.getConnectedNodes();
            for (Node node : linked) {
                if (visible)
                    node.setIsVisible(true);
                else {
                    List<HyperEdge> connectedReactions = node.getConnectedReactions();
                    boolean hide = true;
                    for (HyperEdge connectedEdge : connectedReactions) {
                        // Escape FlowLine connections
                        if (connectedEdge instanceof FlowLine)
                            continue; // Should hide
                        GKInstance inst = fileAdaptor.fetchInstance(connectedEdge.getReactomeId());
                        if (inst == null) {
                            hide = false;
                            break; // This has been linked to some FlowLines
                        }
                        Boolean doRelease = (Boolean) inst.getAttributeValue(ReactomeJavaConstants._doRelease);
                        if (doRelease != null && doRelease) {
                            hide = false;
                            break; // Nothing to worry
                        }
                    }
                    node.setIsVisible(!hide);
                }
            }
        }
    }

    
    /**
     * Show the first selected GKInstance in this InstanceZoomablePathwayEditor.
     */
    protected void showPropertyForSelection() {
        // Check a special case
        List list = pathwayEditor.getSelection();
        if (list != null && list.size() == 1 && list.get(0) instanceof Note) {
            Note note = (Note) list.get(0);
            handleNoteClick(note);
            return;
        }
        List<GKInstance> selection = getSelection();
        if (selection == null || selection.size() == 0)
            return;
        // Want to display the first instance only
        GKInstance first = selection.get(0);
        // Make compartment not editable
        if (first.getSchemClass().isa(ReactomeJavaConstants.Compartment)) {
            FrameManager.getManager().showInstance(first, 
                                                   false);
        }
        else {
            if (first.isShell())
                FrameManager.getManager().showShellInstance(first, pathwayEditor);
            else
                FrameManager.getManager().showInstance(first, pathwayEditor.isEditable());
        }
    }
    
    private class CheckExistenceDialog extends JDialog {
        private DialogControlPane controlPane;
        private JCheckBox checkAllBox;
        private boolean isOkClicked;
        private JTextArea ta;
        
        public CheckExistenceDialog(JFrame frame) {
            super(frame);
            init();
        }
        
        private void init() {
            JPanel contentPane = new JPanel();
            Border outer = BorderFactory.createEtchedBorder();
            Border inner = BorderFactory.createEmptyBorder(12, 24, 12, 24);
            contentPane.setBorder(BorderFactory.createCompoundBorder(outer, inner));
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            ta = new JTextArea();
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setBackground(contentPane.getBackground());
            constraints.gridheight = 3;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 0.5;
            contentPane.add(ta, constraints);
            constraints.gridy += constraints.gridheight;
            constraints.gridheight = 1;
            checkAllBox = new JCheckBox("Apply to other objects in this deletion");
            contentPane.add(checkAllBox, constraints);
            constraints.gridy ++;
            controlPane = new DialogControlPane();
            controlPane.getOKBtn().setText("Yes");
            controlPane.getCancelBtn().setText("No");
            controlPane.getCancelBtn().setDefaultCapable(true);
            getRootPane().setDefaultButton(controlPane.getCancelBtn());
            contentPane.add(controlPane, constraints);
            getContentPane().add(contentPane, BorderLayout.CENTER);
            contentPane.setPreferredSize(new Dimension(350, 275));
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    if (e.getSource() == controlPane.getOKBtn())
                        isOkClicked = true;
                }
            };
            controlPane.getOKBtn().addActionListener(listener);
            controlPane.getCancelBtn().addActionListener(listener);
        }
        
        public void setMessage(String message) {
            ta.setText(message);
        }
        
    }
    
}
