/*
 * Created on Aug 24, 2004
 */
package org.gk.pathView;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gk.util.GKApplicationUtilities;

/**
 * 
 * @author wugm
 */
public class VisualizationToolToolBar extends JToolBar {
    private VisualizationToolActions actions;
    private JSlider zoomSlider;
    private JLabel zoomLbl;
    
    public VisualizationToolToolBar(VisualizationToolActions actions) {
        super();
        this.actions = actions;
        init();
    }
    
    private void init() {
        Insets insets = new Insets(0, 0, 0, 0);
        JButton saveBtn = add(actions.getSaveAction());
        JButton exportSVGBtn = add(actions.getExportSVGAction());
        addSeparator();
        JButton loadSelectedBtn = add(actions.getLoadSelectedAction());
        JButton loadSelectedAndConnectedBtn = add(actions.getLoadSelectedAndConnectedAction());
        JButton undoBtn = add(actions.getUndoAction());
        JButton redoBtn = add(actions.getRedoAction());
        addSeparator();
        JButton arrangeVerticalLineBtn = add(actions.getArrangeVerticalLineAction());
        JButton arrangeHorizonalLineBtn = add(actions.getArrangeHorizontalLineAction());
        JButton parallelArrowVerticalBtn = add(actions.getParallelArrowVerticalAction());
        JButton parallelArrowHorizontalBtn = add(actions.getParallelArrowHorizontalAction());
        JButton arrangeVerticalBtn = add(actions.getArrangeVerticalAction());
        JButton arrangeHorizontalBtn = add(actions.getArrangeHorizontalAction());
        JButton assignLengthBtn = add(actions.getAssignLengthAction());
        if (!GKApplicationUtilities.isMac()) {
            saveBtn.setMargin(insets);
            exportSVGBtn.setMargin(insets);
            loadSelectedBtn.setMargin(insets);
            loadSelectedAndConnectedBtn.setMargin(insets);
            undoBtn.setMargin(insets);
            redoBtn.setMargin(insets);
            arrangeVerticalLineBtn.setMargin(insets);
            arrangeVerticalBtn.setMargin(insets);
            arrangeHorizonalLineBtn.setMargin(insets);
            arrangeHorizontalBtn.setMargin(insets);
            parallelArrowHorizontalBtn.setMargin(insets);
            parallelArrowVerticalBtn.setMargin(insets);
            assignLengthBtn.setMargin(insets);
        }
        addSeparator();
        zoomLbl = new JLabel("Zoom (x1.0):");
        add(zoomLbl);
        zoomSlider = new JSlider();
        zoomSlider.setMaximumSize(new Dimension(200, 24));
        zoomSlider.setMinimum(0);
        zoomSlider.setMaximum(actions.getToolPane().zoomFactors.length - 1);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setMinorTickSpacing(1);
        zoomSlider.setMajorTickSpacing(4);
        zoomSlider.setSnapToTicks(true); // Use the selected value only
        // Select the default setting
        int index = 0;
        double[] factors = actions.getToolPane().zoomFactors;
        for (int i = 0; i < factors.length; i++) {
            if (Math.abs(factors[i] - 1.0) < 0.001) {
                index = i;
                break;
            }
        }
        zoomSlider.setValue(index);
        zoomSlider.addChangeListener(createChangeListener());
        add(zoomSlider);
    }
    
    private ChangeListener createChangeListener() {
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                int value = slider.getValue();
                GKVisualizationPane tool = actions.getToolPane();
                tool.zoom(tool.zoomFactors[value]);
                zoomLbl.setText("Zoom (x" + tool.zoomFactors[value] + "):");
            }
        };
        return listener;
    }
    
    public JSlider getZoomSlider() {
        return this.zoomSlider;
    }
}
