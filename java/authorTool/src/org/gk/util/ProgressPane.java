package org.gk.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressPane extends JComponent {
    // Use to display a title text
    private JLabel titleLabel;
    private JLabel label;
    private JProgressBar progressBar;
    private JButton cancelBtn;
    private boolean isCancelled;
    
    public ProgressPane() {
        init();
    }
    
    public boolean isCancelled() {
        return this.isCancelled;
    }
    
    public void setTitle(String title) {
        if (!titleLabel.isVisible())
            titleLabel.setVisible(true);
        // Want to use HTML bold text
        titleLabel.setText("<html><b><u>" + title + "</u></b></html>");
    }
    
    private void init() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createRaisedBevelBorder());
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        // A hidden title
        titleLabel = new JLabel();
        titleLabel.setVisible(false);
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(0, 4, 16, 4);
        contentPane.add(titleLabel, constraints);
        label = new JLabel();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridy = 1;
        contentPane.add(label, constraints);
        constraints.gridy = 2;
        progressBar = new JProgressBar();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(progressBar, constraints);
        cancelBtn = new JButton("Cancel");
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;
        contentPane.add(cancelBtn, constraints);
        // Default should be invisible
        cancelBtn.setVisible(false);
        
        Dimension size = new Dimension(300, 200);
        contentPane.setMinimumSize(size);
        contentPane.setPreferredSize(size);
        
        // blocks all user input
        addMouseListener(new MouseAdapter() { });
        addMouseMotionListener(new MouseMotionAdapter() { });
        addKeyListener(new KeyAdapter() { });
        
        setFocusTraversalKeysEnabled(false);
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent evt) {
                requestFocusInWindow();
            }
        });
        
        setLayout(new GridBagLayout());
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(contentPane, constraints);
    }
    
    public void enableCancelAction(ActionListener l) {
        cancelBtn.setVisible(true);
        cancelBtn.addActionListener(l);
        // Add another ActionListener so that isCancelled can be set to true
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isCancelled = true;
            }
        });
    }
    
    public void setText(String text) {
        label.setText(text);
    }
    
    public void setIndeterminate(boolean isInderminate) {
        progressBar.setIndeterminate(isInderminate);
    }
    
    public void setMinimum(int min) {
        progressBar.setMinimum(min);
    }
    
    public void setMaximum(int max) {
        progressBar.setMaximum(max);
    }
    
    public void setValue(int value) {
        progressBar.setValue(value);
    }
    
    public void paintComponent(Graphics g) {
        Rectangle clip = g.getClipBounds();
        Color alphaWhite = new Color(1.0f, 1.0f, 1.0f, 0.65f);
        g.setColor(alphaWhite);
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
        super.paintComponent(g);
    }
}