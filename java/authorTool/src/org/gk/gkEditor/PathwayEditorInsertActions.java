/*
 * Created on Dec 14, 2006
 *
 */
package org.gk.gkEditor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.UndoableInsertEdit;
import org.gk.render.*;
import org.gk.util.AuthorToolAppletUtilities;

/**
 * This is a collection of all adding actions for PathwayEditor.
 * @author guanming
 *
 */
public class PathwayEditorInsertActions {
    private PathwayEditor pathwayEditor;
    
    public PathwayEditorInsertActions() {
    }
    
    public void setPathwayEditor(PathwayEditor editor) {
        this.pathwayEditor = editor;
    }
    
    private Action getInsertFlowLineAction() {
        Action insertFlowLine = new AddRenderableAction<FlowLine>(FlowLine.class,
                                                                 "Insert flow arrow", 
                                                                  "Flow Arrow");
        return insertFlowLine;
    }
    
    private Action getInsertChemicalAction() {
        Action insertChemical = new AddRenderableAction<RenderableChemical>(RenderableChemical.class,
                                                                            "Insert small molecule",
                                                                            "Compound");
        return insertChemical;
    }
    
    private Action getInsertRNAAction() {
        Action insertRNA = new AddRenderableAction<RenderableRNA>(RenderableRNA.class,
                                                                  "Insert RNA",
                                                                  "RNA");
        return insertRNA;
    }
    
    private Action getInsertProteinAction() {
        Action insertProtein = new AddRenderableAction<RenderableProtein>(RenderableProtein.class, 
                                                                          "Insert protein", 
                                                                        "Protein");
        return insertProtein;
    }
    
    private Action getInsertEntityAction() {
        Action addEntity = new AddRenderableAction<RenderableEntity>(RenderableEntity.class, "Insert entity", "Entity");
        return addEntity;
    }
    
    private Action getInsertComplexAction() {
        Action addComplex = new AddRenderableAction<RenderableComplex>(RenderableComplex.class, "Insert complex", "Complex");
        return addComplex;
    }
    
    private Action getInsertGeneAction() {
        Action addGene = new AddRenderableAction<RenderableGene>(RenderableGene.class, "Insert gene", "Gene");
        return addGene;
    }
    
    private Action getInsertProcessNodeAction() {
        Action insertProcess = new AddRenderableAction<ProcessNode>(ProcessNode.class,
                            "Insert process", 
                        "Process");
        return insertProcess;
    }
    
    private Action getInsertReactionAction() {
        Action insertReaction = new AddRenderableAction<RenderableReaction>(RenderableReaction.class, 
                                                          "Insert reaction",
                                                          "Reaction");
        return insertReaction;
    }
    
    private Action getInsertCompartmentAction() {
        Action insertBlock = new AddRenderableAction<RenderableCompartment>(RenderableCompartment.class,
                                                     "Insert compartment",
                                                     "Compartment");
        return insertBlock;
    }
    
    private Action getInsertTextAction() {
        Action insertText = new AddRenderableAction<Note>(Note.class, 
                "Insert text note", 
                "Note");
        return insertText;
    }
    
    private Action getInsertSourceOrSinkAction() {
        Action insertSourceOrSink = new AddRenderableAction<SourceOrSink>(SourceOrSink.class,
                "Insert Source/Sink",
                "Source/Sink");
        return insertSourceOrSink;
    }
    
    /**
     * Get a group of insert actions defined in this object.
     * @return
     */
    public List<Action> getInsertActions() {
        List<Action> actions = new ArrayList<Action>();
        actions.add(getInsertProteinAction());
        actions.add(getInsertComplexAction());
        actions.add(getInsertChemicalAction());
        actions.add(getInsertCompartmentAction());
        actions.add(getInsertReactionAction());
        //actions.add(getInsertFlowLineAction());
        actions.add(getInsertGeneAction());
        actions.add(getInsertRNAAction());
        actions.add(getInsertProcessNodeAction());
        actions.add(getInsertEntityAction());
        actions.add(getInsertSourceOrSinkAction());
        actions.add(getInsertTextAction());
        return actions;
    }
    
    public JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        JLabel insertLabel = new JLabel("Palette: ");
        Icon icon = AuthorToolAppletUtilities.createImageIcon("samples16.png");
        insertLabel.setIcon(icon);
        insertLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        // Give it a bold font
        Font font = insertLabel.getFont();
        insertLabel.setFont(font.deriveFont(Font.BOLD));
        toolbar.add(insertLabel);
        MouseAdapter adaptor = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                JButton btn = (JButton) e.getSource();
                btn.setBorderPainted(true);
            }
            public void mouseExited(MouseEvent e) {
                JButton btn = (JButton) e.getSource();
                btn.setBorderPainted(false);
            }
        };
        for (Action action : getInsertActions()) {
            JButton btn = toolbar.add(action);
            formatButton(adaptor, btn);
        }
        return toolbar;
    }

    private void formatButton(MouseAdapter adaptor, 
                              JButton btn) {
        btn.setText(btn.getAction().getValue(Action.NAME).toString());
        btn.setVerticalTextPosition(JButton.BOTTOM);
        //btn.setRolloverEnabled(true);
        //btn.setBorderPainted(false);
        //btn.addMouseListener(adaptor);
    }
    
    /**
     * Inner class for adding renderable to pathway editor.
     * @author guanming
     *
     */
    class AddRenderableAction<T> extends AbstractAction {
        private Class<T> type;
        
        public AddRenderableAction(Class<T> type,
                                   String description,
                                   String name) {
            this.type = type;
            Icon icon = RenderableFactory.getFactory().getIcon(type);
            putValue(Action.SMALL_ICON, icon);
            putValue(Action.SHORT_DESCRIPTION, description);
            putValue(Action.NAME, name);
        }
                
        public void actionPerformed(ActionEvent e) {
            Renderable r = generateRenderable();
            if (r == null)
                return;
            RenderableRegistry.getRegistry().add(r);
            if (r instanceof RenderableCompartment) {
                pathwayEditor.insertCompartment((RenderableCompartment)r);
                pathwayEditor.setSelection(r);
            }
            else if (r instanceof Node) {
                Node node = (Node) r;
                pathwayEditor.insertNode(node);
                pathwayEditor.prepareForEditing(node);
            }
            else if (r instanceof HyperEdge) {
                pathwayEditor.insertEdge((HyperEdge)r, true);
                pathwayEditor.setSelection(r);
            }
            // Add an undo
            UndoableInsertEdit edit = new UndoableInsertEdit(pathwayEditor, r);
            pathwayEditor.addUndoableEdit(edit);
        }
        
        private Renderable generateRenderable() {
            try {
                Renderable rtn = (Renderable) type.newInstance();
                rtn.setDisplayName(rtn.getType());
                Renderable container = pathwayEditor.getRenderable();
                rtn.setContainer(container);
                String name = RenderableRegistry.getRegistry().generateUniqueName(rtn);
                rtn.setDisplayName(name);
                // New instances should be marked as isChanged
                rtn.setIsChanged(true);
                return rtn;
            }
            catch(Exception e) {
                System.err.println("AddRenderableAction.generateRenderable(): " + e);
                e.printStackTrace();
            }
            return null;
        }
        
    }
}
