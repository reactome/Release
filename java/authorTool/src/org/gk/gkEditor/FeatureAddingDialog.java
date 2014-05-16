/*
 * Created on Jun 12, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.gk.render.RenderableFeature;
import org.gk.render.RenderableFeature.FeatureType;
import org.gk.util.DialogControlPane;

/**
 * This customized JDialog is used to add a RenderableFeature to a Node.
 * @author wgm
 *
 */
public class FeatureAddingDialog extends NodeAttachmentAddingDialog {
    // GUIs
    private JRadioButton modificationFeatureBtn;
    private JRadioButton bindingFeatureBtn;
    private JLabel residueLbl;
    private JTextField residueTA;
    // JComboBox for modification type
    private JLabel typeLbl;
    private JComboBox typeBox;
    // Add GUIs for the second Binding feature
    private JLabel nameLbl;
    private JTextField nameTF;
    
    public FeatureAddingDialog(JFrame frame) {
        super(frame);
    }
    
    protected void initGUIs() {
        setTitle("Choose a Feature");
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        contentPane.setLayout(new GridBagLayout());
        JLabel chooseFeatureLabel = new JLabel("<html><b><u>Choose a Feature:</u></b></html>");
        // Set up the constraints
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(chooseFeatureLabel, constraints);
        modificationFeatureBtn = new JRadioButton("Modification Feature");
        bindingFeatureBtn = new JRadioButton("Binding Feature");
        ActionListener l = createFeatureBtnListener();
        modificationFeatureBtn.addActionListener(l);
        bindingFeatureBtn.addActionListener(l);
        
        ButtonGroup group = new ButtonGroup();
        group.add(modificationFeatureBtn);
        group.add(bindingFeatureBtn);
        // Default should be a modificationFeature group
        modificationFeatureBtn.setSelected(true);
        // Add GUIs for modification type
        constraints.gridx = 0;
        constraints.gridy = 1;
        contentPane.add(modificationFeatureBtn, 
                        constraints);
        // Text for residue
        residueLbl = new JLabel("Residue:");
        residueTA = new JTextField();
        Dimension tfSize = new Dimension(40, 20);
        residueTA.setPreferredSize(tfSize);
        // JComboBox for modification type
        typeLbl = new JLabel("Type:");
        typeBox = new JComboBox();
        initTypeBox(typeBox);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        contentPane.add(residueLbl,
                        constraints);
        constraints.gridx = 1;
        contentPane.add(residueTA,
                        constraints);
        constraints.gridx = 2;
        constraints.insets = new Insets(4, 12, 4, 4);
        contentPane.add(typeLbl,
                        constraints);
        constraints.gridx = 3;
        constraints.insets = new Insets(4, 4, 4, 4);
        contentPane.add(typeBox,
                        constraints);
        // Add GUIs for the second Binding feature
        nameLbl = new JLabel("Name:");
        nameTF = new JTextField();
        nameTF.setPreferredSize(tfSize);
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 4;
        contentPane.add(bindingFeatureBtn, constraints);
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        contentPane.add(nameLbl, constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 3;
        contentPane.add(nameTF, constraints);
        // Default should be for modification
        nameLbl.setEnabled(false);
        nameTF.setEnabled(false);
        
        getContentPane().add(contentPane, BorderLayout.CENTER);
        DialogControlPane controlPane = createControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        setSize(400, 300);
    }
    
    private ActionListener createFeatureBtnListener() {
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Disable some of GUIs that are not selected
                if (modificationFeatureBtn.isSelected()) {
                    residueLbl.setEnabled(true);
                    residueTA.setEnabled(true);
                    typeLbl.setEnabled(true);
                    typeBox.setEnabled(true);
                    nameLbl.setEnabled(false);
                    nameTF.setEnabled(false);
                }
                else if (bindingFeatureBtn.isSelected()) {
                    residueLbl.setEnabled(false);
                    residueTA.setEnabled(false);
                    typeLbl.setEnabled(false);
                    typeBox.setEnabled(false);
                    nameLbl.setEnabled(true);
                    nameTF.setEnabled(true);
                }
            }
        };
        return l;
    }
    
    private void initTypeBox(JComboBox box) {
        FeatureType[] types = FeatureType.values();
        for (FeatureType type : types)
            box.addItem(type);
    }
    
    protected boolean createNodeAttachment() {
        boolean isLocal = isLocalOnly("feature");
        createFeature(isLocal);
        return true;
    }
    
    /**
     * A helper method to create a feature if the user click OK button.
     */
    private void createFeature(boolean isLocal) {
        if (node == null)
            return;
        RenderableFeature feature = new RenderableFeature();
        // A modified feature
        if (modificationFeatureBtn.isSelected()) {
            if (residueTA.getText().trim().length() > 0) {
                String description = residueTA.getText().trim();
                feature.setDescription(description);
            }
            FeatureType type = (FeatureType) typeBox.getSelectedItem();
            if (type != null) {
                feature.setFeatureType(type);
            }
        }
        else if (bindingFeatureBtn.isSelected()) {
            if (nameTF.getText().trim().length() > 0) {
                String description = nameTF.getText().trim();
                feature.setDescription(description);
            }
        }
        if (feature != null) {
            // Set the coordinates before it is added to node
            // to take care of shortcuts for node.
            // Need to assign a default position
            // middle of the left line. 
            feature.setRelativePosition(0.0d, 0.5d);
            if (isLocal)
                node.addFeatureLocally(feature);
            else
                node.addFeature(feature);
        }
    }
}
