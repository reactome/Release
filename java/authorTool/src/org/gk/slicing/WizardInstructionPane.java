/*
 * Created on May 6, 2005
 */
package org.gk.slicing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * This customized JPanel is used in a wizard to list instructions so that the user
 * can click to do related actions. It is targeted to EventReleaseWizard, but should
 * be generalized easily (not done yet).
 * @author wgm
 */
public class WizardInstructionPane extends JPanel {
    // All JLabels
    private JLabel[] stepLabels;
    private Color backgroundColor;
    private Font font;
    private Color hiliteColor;
    private Font hiliteFont;
    // the current selected step
    private int selectedIndex;
    
    public WizardInstructionPane() {
        init();
    }
    
    private void init() {
        setBackground(Color.WHITE);
        backgroundColor = getBackground();
        hiliteColor = UIManager.getColor("List.selectionBackground");
        font = UIManager.getFont("Label.font");
        hiliteFont = font.deriveFont(Font.BOLD);
        
        initStepLabels();
        // To change color based on mouse in/out
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        JLabel steps = createLabel("Steps");
        add(steps);
        JPanel separator = createSeparator();
        add(separator);
        for (int i = 0; i < stepLabels.length; i ++)
            add(stepLabels[i]);
        select(0); // Start from the first step
    }
    
    /**
     * 
     */
    private void initStepLabels() {
        stepLabels = new JLabel[4];
        stepLabels[0] = createLabel("1. Choose one top-level event for releasing");
        stepLabels[1] = createLabel("2. Check events in the selected event for releasing");
        stepLabels[2] = createLabel("3. Preview the released events");
        stepLabels[3] = createLabel("4. Commit to the database");
    }

    private JPanel createSeparator() {
        final JPanel separator = new JPanel();
        Dimension dim = new Dimension(1000, 1);
        separator.setPreferredSize(dim);
        separator.setMaximumSize(dim);
        separator.setBackground(UIManager.getColor("Panel.foreground"));
        // TO control the width of the separator
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                separator.setSize(getWidth(), separator.getHeight());
            }
        });
        return separator;
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 6));
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setOpaque(true);
        label.setBackground(getBackground());
        return label;
    }
    
    public void next() {
        if (selectedIndex == stepLabels.length - 1)
            return; // Last step
        select(selectedIndex + 1);
    }
    
    public void back() {
        if (selectedIndex == 0)
            return; // No steps to back
        select(selectedIndex - 1);
    }
    
    public void select(int index) {
        if (index < 0 || index >= stepLabels.length)
            return; // out of range
        stepLabels[selectedIndex].setBackground(backgroundColor);
        stepLabels[selectedIndex].setFont(font);
        selectedIndex = index;
        stepLabels[selectedIndex].setBackground(hiliteColor);
        stepLabels[selectedIndex].setFont(hiliteFont);
    }
    
    public int getSelectedIndex() {
        return this.selectedIndex;
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        WizardInstructionPane pane = new WizardInstructionPane();
        frame.getContentPane().add(pane, BorderLayout.CENTER);
        frame.setSize(400, 400);
        frame.setVisible(true);
        pane.select(2);
    }    
}
