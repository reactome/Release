/*
 * Created on Jun 16, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.render.Node;
import org.gk.render.Shortcut;
import org.gk.util.DialogControlPane;

/**
 * A super class for adding NodeAttachment to a Node.
 * @author wgm
 *
 */
public abstract class NodeAttachmentAddingDialog extends JDialog {
    protected Node node;
    
    public NodeAttachmentAddingDialog(JFrame parentFrame) {
        super(parentFrame);
        initGUIs();
    }
    
    /**
     * Set the Node object that will contain the created NodeAttachment object.
     * @param node
     */
    public void setNode(Node node) {
        this.node = node;
    }
    
    public Node getNode() {
        return this.node;
    }
    
    protected abstract void initGUIs();
    
    protected DialogControlPane createControlPane() {
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getOKBtn().addActionListener(createOKAction());
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        return controlPane;
    }
    
    private ActionListener createOKAction() {
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(createNodeAttachment())
                    dispose();
            }
        };
        return l;
    }
    
    /**
     * Check if the new attachment should be added to other nodes with same name.
     * @return
     */
    protected boolean isLocalOnly(String type) {
        // Check if there is any shortcuts available
        if (!(node instanceof Shortcut) && 
            (node.getShortcuts() == null || node.getShortcuts().size() == 0))
            return false;
        String message = "Do you want to add this new " + type + " to other objects having same name?";
        int reply = JOptionPane.showConfirmDialog(this,
                                                  message,
                                                  "Apply new " + type, 
                                                  JOptionPane.YES_NO_OPTION);
        return reply != JOptionPane.YES_OPTION;
    }
    
    protected abstract boolean createNodeAttachment();
    
}
