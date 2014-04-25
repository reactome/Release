/*
 * Created on Aug 11, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.apache.batik.ext.swing.GridBagConstants;
import org.gk.render.Renderable;
import org.gk.render.RenderableReaction;
import org.gk.util.DialogControlPane;

/**
 * A customized JDialog to set stoichiometry for Reaction.
 * @author wgm
 *
 */
public class StoichiometrySettingDialog extends JDialog {
    private RenderableReaction reaction;
    private JLabel title;
    private JComboBox inputBox;
    private JTextField inputTF;
    private JComboBox outputBox;
    private JTextField outputTF;
    // All setting value: Use two maps in case a node is used as
    // both input and output.
    private Map<Renderable, Integer> inputValueMap;
    private Map<Renderable, Integer> outputValueMap;
    private boolean isChanged = false;
    
    public StoichiometrySettingDialog(JFrame owner,
                                      String title) {
        super(owner, title);
        init();
    }
    
    private void init() {
        JPanel contentPane = new JPanel();
        Border emptyBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        contentPane.setBorder(BorderFactory.createCompoundBorder(etchedBorder, 
                                                                 emptyBorder));
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        title = new JLabel("Title");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstants.CENTER;
        constraints.gridwidth = 3;
        contentPane.add(title, constraints);
        JLabel inputLabel = new JLabel("Set Stoichiometry for Input:");
        constraints.gridy = 1;
        constraints.anchor = GridBagConstants.WEST;
        constraints.fill = GridBagConstants.HORIZONTAL;
        contentPane.add(inputLabel, constraints);
        inputBox = new JComboBox();
        JLabel inpuStoiLabel = new JLabel("Stoichiometry:");
        inputTF = new JTextField();
        inputTF.setColumns(4);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        contentPane.add(inputBox, constraints);
        constraints.gridx = 1;
        contentPane.add(inpuStoiLabel, constraints);
        constraints.gridx = 2;
        contentPane.add(inputTF, constraints);
        // For output
        JLabel outputLabel = new JLabel("Set Stoichiometry for Output:");
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 3;
        contentPane.add(outputLabel, constraints);
        outputBox = new JComboBox();
        JLabel outputStoiLabel = new JLabel("Stoichiometry:");
        outputTF = new JTextField();
        outputTF.setColumns(4);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        contentPane.add(outputBox, constraints);
        constraints.gridx = 1;
        contentPane.add(outputStoiLabel, constraints);
        constraints.gridx = 2;
        contentPane.add(outputTF, constraints);
        // Control Panel
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                commit();
            }
        });
        getContentPane().add(contentPane, BorderLayout.CENTER);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
    }
    
    private void commit() {
        // Need to record the change
        String inputValue = inputTF.getText().trim();
        if (inputValue.length() > 0)
            inputValueMap.put((Renderable)inputBox.getSelectedItem(),
                              Integer.parseInt(inputValue));
        String outputValue = outputTF.getText().trim();
        if (outputValue.length() > 0)
            outputValueMap.put((Renderable)outputBox.getSelectedItem(),
                               Integer.parseInt(outputValue));
        boolean isChanged = false;
        for (Renderable r : inputValueMap.keySet()) {
            Integer value = inputValueMap.get(r);
            reaction.setInputStoichiometry(r, value);
            isChanged = true;
        }
        for (Renderable r : outputValueMap.keySet()) {
            Integer value = outputValueMap.get(r);
            reaction.setOutputStoichiometry(r, value);
            isChanged = true;
        }
    }
    
    public boolean isChanged() {
        return this.isChanged;
    }
    
    private ItemListener createBoxListener() {
        ItemListener listener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                JComboBox box = (JComboBox) e.getSource();
                Renderable item = (Renderable) e.getItem();
                Map<Renderable, Integer> valueMap = null;
                JTextField tf = null;
                if (box == inputBox) {
                    valueMap = inputValueMap;
                    tf = inputTF;
                }
                else {
                    valueMap = outputValueMap;
                    tf = outputTF;
                }
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Integer value = valueMap.get(item);
                    if (value != null)
                        tf.setText(value.toString());
                    else
                        tf.setText("");
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    String value = tf.getText().trim();
                    if (value.length() > 0)
                        valueMap.put(item,
                                     Integer.parseInt(value));
                }
            }
        };
        return listener;
    }
    
    public void setReaction(RenderableReaction reaction) {
        this.reaction = reaction;
        title.setText("<html><br><u>Set Stoichiometry for " + 
                      reaction.getDisplayName() + 
                      "</u></br></html>");
        // Need to set the inputs and outputs
        List inputs = reaction.getInputNodes();
        for (Object obj : inputs)
            inputBox.addItem(obj);
        List outputs = reaction.getOutputNodes();
        for (Object obj : outputs)
            outputBox.addItem(obj);
        inputValueMap = new HashMap<Renderable, Integer>(reaction.getInputStoichiometries());
        outputValueMap = new HashMap<Renderable, Integer>(reaction.getOutputStoichiometries());
        // Set the initial values
        Integer stoi = inputValueMap.get(inputBox.getSelectedItem());
        if (stoi != null)
            inputTF.setText(stoi + "");
        stoi = outputValueMap.get(outputBox.getSelectedItem());
        if (stoi != null)
            outputTF.setText(stoi + "");
        ItemListener listener = createBoxListener();
        inputBox.addItemListener(listener);
        outputBox.addItemListener(listener);
    }
    
}
