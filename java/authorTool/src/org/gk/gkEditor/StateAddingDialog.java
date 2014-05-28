/*
 * Created on Jun 16, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.gk.render.RenderableState;
import org.gk.util.DialogControlPane;

/**
 * This customized JDialog is used to add a RenderableState to a Node.
 * @author wgm
 *
 */
public class StateAddingDialog extends NodeAttachmentAddingDialog {
    // GUIs
    private JLabel stateLabel;
    private JRadioButton openBtn;
    private JRadioButton closedBtn;
    private JRadioButton activeBtn;
    private JRadioButton inactiveBtn;
    private JRadioButton otherBtn;
    private ButtonGroup group;
    private JTextField otherTF;
    
    public StateAddingDialog(JFrame parentFrame) {
        super(parentFrame);
    }
    
    protected void initGUIs() {
        setTitle("Choose a State");
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        contentPane.setLayout(new GridBagLayout());
        stateLabel = new JLabel("<html><b><u>Choose a state:</u></b></html>");
        // Set up the constraints
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(stateLabel, constraints);
        openBtn = new JRadioButton("Open");
        closedBtn = new JRadioButton("Closed");
        activeBtn = new JRadioButton("Active");
        inactiveBtn = new JRadioButton("Inactive");
        otherBtn = new JRadioButton("Other State:");
        group = new ButtonGroup();
        group.add(openBtn);
        group.add(closedBtn);
        group.add(activeBtn);
        group.add(inactiveBtn);
        group.add(otherBtn);
        ActionListener l = createBtnListener();
        openBtn.addActionListener(l);
        closedBtn.addActionListener(l);
        activeBtn.addActionListener(l);
        inactiveBtn.addActionListener(l);
        otherBtn.addActionListener(l);
        openBtn.setSelected(true);
        // Add these buttons
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        contentPane.add(openBtn, constraints);
        constraints.gridx = 1;
        contentPane.add(closedBtn, constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        contentPane.add(activeBtn, constraints);
        constraints.gridx = 1;
        contentPane.add(inactiveBtn, constraints);
        constraints.gridx = 0;
        constraints.gridy = 3;
        contentPane.add(otherBtn, constraints);
        otherTF = new JTextField();
        otherTF.setColumns(10);
        otherTF.setEnabled(false);
        constraints.gridx = 1;
        contentPane.add(otherTF, constraints);
        
        getContentPane().add(contentPane, BorderLayout.CENTER);
        DialogControlPane controlPane = createControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        setSize(370, 230);
    }
    
    public ActionListener createBtnListener() {
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == otherBtn)
                    otherTF.setEnabled(true);
                else
                    otherTF.setEnabled(false);
            }
        };
        return l;
    }
    
    protected boolean createNodeAttachment() {
        boolean isLocal = isLocalOnly("state");
        return createState(isLocal);
    }
    
    private boolean createState(boolean isLocal) {
        String label = null;
        if (otherBtn.isSelected()) {
            String text = otherTF.getText().trim();
            if (text.length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              "Please enter a state description when you choose other state.",
                                              "Error in Choosing a State",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            label = text;
        }
        else {
            // Want to find which one is selected
            Enumeration<AbstractButton> btns = group.getElements();
            while (btns.hasMoreElements()) {
                AbstractButton btn = btns.nextElement();
                if (btn.isSelected()) {
                    label = btn.getText();
                    break;
                }
            }
        }
        RenderableState state = new RenderableState();
        state.setDescription(label);
        // Want to place at the center of the north side
        state.setRelativePosition(0.5d, 0.0d);
        if (isLocal)
            node.addStateLocally(state);
        else
            node.addState(state);
        return true;
    }
    
}
