/*
 * Created on Nov 4, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gk.graphEditor.PathwayEditor;

/**
 * This customized JPanel is used to create a zoomable pathwayEditor.
 * @author wgm
 *
 */
public class ZoomablePathwayEditor extends JPanel {
    protected JLabel titleLabel;
    protected PathwayEditor pathwayEditor;
    private JScrollPane pathwayEditorJSP;
    // Track toolbar
    protected JToolBar toolbar;
    
    public ZoomablePathwayEditor() {
        init();
    }
    
    private void init() {
        this.setLayout(new BorderLayout());
        
        titleLabel = new JLabel("<html><u>Pathway Editor</u><html>");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        Border emptyBorder = BorderFactory.createEmptyBorder(3, 2, 3, 2);
        titleLabel.setBorder(emptyBorder);
        this.add(titleLabel, BorderLayout.NORTH);
        
        this.setBorder(BorderFactory.createEtchedBorder());
        pathwayEditor = createPathwayEditor();
        pathwayEditorJSP = new JScrollPane(pathwayEditor);
        this.add(pathwayEditorJSP, BorderLayout.CENTER);
        this.add(createZoomScrollPane(), BorderLayout.SOUTH);
        this.setMinimumSize(new Dimension(50, 50));
    }
    
    /**
     * To be overridden in a subclass.
     * @return
     */
    protected PathwayEditor createPathwayEditor() {
        return new PathwayEditor();
    }
    
    public void setTitle(String title) {
        this.titleLabel.setText(title);
    }
    
    /**
     * Add a toolbar to this ZoomablePathwayEditor.
     * @param toolbar
     */
    public void installToolbar(JToolBar toolbar) {
        remove(titleLabel);
        JPanel northPane = new JPanel();
        northPane.setLayout(new BorderLayout());
        northPane.setBorder(BorderFactory.createEtchedBorder());
        JPanel labelPane = new JPanel();
        labelPane.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        labelPane.add(titleLabel);
        northPane.add(labelPane, BorderLayout.NORTH);
        northPane.add(toolbar, BorderLayout.SOUTH);
        add(northPane, BorderLayout.NORTH);
        this.toolbar = toolbar;
    }
    
    private JPanel createZoomScrollPane() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Slide to Zoom:");
        panel.add(label);
        final JSlider slider = new JSlider();
        slider.setMinimum(1);
        slider.setMaximum(10);
        slider.setMinorTickSpacing(1);
        slider.setSnapToTicks(true);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int value = slider.getValue();
                double ratio = (double) value / 10;
                pathwayEditor.zoom(ratio, ratio);
            }
        });
        panel.add(slider);
        return panel;
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }
    
    public JScrollPane getPathwayScrollPane() {
        return pathwayEditorJSP;
    }
}
