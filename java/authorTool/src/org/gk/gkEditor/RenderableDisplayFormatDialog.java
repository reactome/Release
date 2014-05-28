/*
 * Created on Dec 20, 2006
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.gk.graphEditor.GraphEditorPane;
import org.gk.render.DefaultRenderConstants;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Note;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderableGene;
import org.gk.render.SourceOrSink;

class RenderableDisplayFormatDialog extends JDialog {
    // GUIs
    private JPanel bgPanel;
    private JPanel fgPanel;
    private JPanel lineColorPanel;
    private JPanel linePanel;
    private JPanel controlPane;
    private JButton okBtn;
    private JLabel fgValueLabel;
    private JLabel bgValueLbl;
    private JLabel lineColorValueLabel;
    private JTextField widthTF;
    // For isPrivate
    private JCheckBox isPrivateBox;
    // For Node border
    private JCheckBox needDashedLine;
    private GraphEditorPane editorPane;
    // A flag
    private boolean isOKClicked = false;
    // for cancel
    private Map<Renderable, Color> bgMap = new HashMap<Renderable, Color>();
    private Map<Renderable, Color> fgMap = new HashMap<Renderable, Color>();
    private Map<Renderable, Color> lineColorMap = new HashMap<Renderable, Color>();
    private Map<Renderable, Float> lineWidthMap = new HashMap<Renderable, Float>();
    private Map<Renderable, Boolean> isPrivateMap = new HashMap<Renderable, Boolean>();
    private Map<RenderableCompartment, String> hideDoubleEdgeMap = new HashMap<RenderableCompartment, String>();
    private Map<Node, Boolean> needDashedLineMap = new HashMap<Node, Boolean>();
    // cached Renderables
    private List<Renderable> renderables;
    private boolean isPreviewCalled = false;
    // To control if a private note is support
    private boolean supportPrivateNote = false;

    RenderableDisplayFormatDialog(Frame parentFrame) {
        super(parentFrame);
        init();
    }
    
    public void setEditorPane(GraphEditorPane pane) {
        this.editorPane = pane;
    }
    
    private void init() {
        JPanel panel = new JPanel();
        Border etchedBorder = BorderFactory.createEtchedBorder();
        panel.setBorder(BorderFactory.createTitledBorder(etchedBorder, "Settings"));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        Dimension valueSize = new Dimension(75, 20);
        Dimension btnSize = new Dimension(20, 20);
        // Add three setting panels
        createFgPanel(constraints, valueSize, btnSize);
        createBgPanel(constraints, valueSize, btnSize);
        createLineColorPane(constraints, valueSize, btnSize);
        createLinePanel(constraints, valueSize);
        constraints.weightx = 0.5;
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(fgPanel, constraints);
        constraints.gridy = 1;
        panel.add(bgPanel, constraints);
        constraints.gridy = 2;
        panel.add(lineColorPanel, constraints);
        constraints.gridy = 3;
        panel.add(linePanel, constraints);
        JPanel isPrivateBoxPane = createPrivateBoxPane(constraints);
        constraints.gridy = 4;
        constraints.gridx = 0;
        panel.add(isPrivateBoxPane, constraints);
        needDashedLine = new JCheckBox("Need Dashed Border");
        constraints.gridy = 5;
        panel.add(needDashedLine, constraints);
        
        getContentPane().add(panel, BorderLayout.CENTER);
        
        createControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        setTitle("Format Display");
        setSize(350, 300);
    }
    
    public void setPrivateNoteSupport(boolean support) {
        this.supportPrivateNote = support;
    }
    
    public void addOKListener(ActionListener l) {
        okBtn.addActionListener(l);
    }
    
    private void createControlPane() {
        controlPane = new JPanel();
        controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton previewBtn = new JButton("Preview");
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                apply(true);
            }
        });
        controlPane.add(previewBtn);
        okBtn = new JButton("OK");
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                apply(false);
                dispose();
            }
        });
        controlPane.add(okBtn);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
                dispose();
            }
        });
        controlPane.add(cancelBtn);
    }
    
    private void apply(boolean isForPreview) {
        Color fgColor = fgValueLabel.getBackground();
        Color bgColor = bgValueLbl.getBackground();
        Color lineColor = lineColorValueLabel.getBackground();
        // reset all values
        Renderable r = null;
        Color c = null;
        for (Iterator it = fgMap.keySet().iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            r.setForegroundColor(fgColor);
        }
        for (Iterator it = bgMap.keySet().iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            r.setBackgroundColor(bgColor);
        }
        for (Renderable r1 : lineColorMap.keySet()) {
            r1.setLineColor(lineColor);
        }
        for (Renderable r1 : isPrivateMap.keySet()) {
            if (r1 instanceof Note) {
                boolean isSelected = isPrivateBox.isSelected();
                ((Note) r1).setPrivate(isSelected);
            }
        }
        Float newWidth = getLineWidth();
        if (newWidth != null) {
            // Line width
            for (Iterator it = lineWidthMap.keySet().iterator(); it.hasNext();) {
                r = (Renderable) it.next();
                r.setLineWidth(newWidth.floatValue());
            }
        }
        boolean isChecked = needDashedLine.isSelected();
        for (Node node : needDashedLineMap.keySet()) {
            node.setNeedDashedBorder(isChecked);
        }
        if (editorPane != null) {
            if (isForPreview) {
                editorPane.removeSelection();
                isPreviewCalled = true;
            }
            else if (isPreviewCalled) {
                editorPane.setSelection(renderables);
            }
            editorPane.repaint(editorPane.getVisibleRect());
        }
    }
    
    private void cancel() {
        // reset all values
        for (Renderable r : fgMap.keySet()) {
            Color c = fgMap.get(r);
            r.setForegroundColor(c);
        }
        for (Renderable r : bgMap.keySet()) {
            Color c = bgMap.get(r);
            r.setBackgroundColor(c);
        }
        for (Renderable r : lineColorMap.keySet()) {
            Color color = lineColorMap.get(r);
            r.setLineColor(color);
        }
        for (Renderable r : isPrivateMap.keySet()) {
            Boolean isPrivate = isPrivateMap.get(r);
            if (r instanceof Note) {
                ((Note) r).setPrivate(isPrivate);
            }
        }
        for (Node node : needDashedLineMap.keySet()) {
            Boolean need = needDashedLineMap.get(node);
            node.setNeedDashedBorder(need);
        }   
        // Line width
        Renderable r = null;
        Float width = null;
        for (Iterator it = lineWidthMap.keySet().iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            width = (Float) lineWidthMap.get(r);
            r.setLineWidth(width.floatValue());
        }
        if (isPreviewCalled)
            editorPane.setSelection(renderables);
        editorPane.repaint(editorPane.getVisibleRect());
    }
    
    private void createBgPanel(GridBagConstraints constraints,
                               Dimension valueSize,
                               Dimension btnSize) {
        bgPanel = new JPanel();
        bgPanel.setLayout(new GridBagLayout());
        JLabel bgLbl = new JLabel("Background: ");
        constraints.gridx = 0;
        constraints.gridy = 1;
        bgPanel.add(bgLbl, constraints);
        bgValueLbl = new JLabel("");
        bgValueLbl.setMinimumSize(valueSize);
        bgValueLbl.setPreferredSize(valueSize);
        bgValueLbl.setOpaque(true);
        bgValueLbl.setBorder(BorderFactory.createEtchedBorder());
        constraints.gridx = 1;
        bgPanel.add(bgValueLbl, constraints);
        final JButton bgBtn = new JButton("...");
        bgBtn.setToolTipText("Click to choose a color");
        bgBtn.setPreferredSize(btnSize);
        bgBtn.setMaximumSize(btnSize);
        constraints.gridx = 2;
        bgPanel.add(bgBtn, constraints);      
        bgBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                chooseColorForBg();       
            }
        });
        bgValueLbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                chooseColorForBg();
            }
        });
    }

    private void chooseColorForBg() {
        Color selectedColor = JColorChooser.showDialog(RenderableDisplayFormatDialog.this,
                                                       "Choose Background Color",
                                                       bgValueLbl.getBackground());
        if (selectedColor != null) {
            bgValueLbl.setBackground(selectedColor);    
        }
    }
    
    private void createFgPanel(GridBagConstraints constraints,
                               Dimension valueSize,
                               Dimension btnSize) {
        fgPanel = new JPanel();
        fgPanel.setLayout(new GridBagLayout());
        JLabel txtColorLbl = new JLabel("Text Color: ");
        constraints.gridx = 0;
        constraints.gridy = 0;
        fgPanel.add(txtColorLbl, constraints);
        fgValueLabel = new JLabel("");
        fgValueLabel.setPreferredSize(valueSize);
        fgValueLabel.setMinimumSize(valueSize);
        fgValueLabel.setOpaque(true);
        fgValueLabel.setBorder(BorderFactory.createEtchedBorder());
        constraints.gridx = 1;
        fgPanel.add(fgValueLabel, constraints);
        final JButton txtColorBtn = new JButton("...");
        txtColorBtn.setToolTipText("Click to choose a color");
        txtColorBtn.setPreferredSize(btnSize);
        txtColorBtn.setPreferredSize(btnSize);
        constraints.gridx = 2;
        fgPanel.add(txtColorBtn, constraints);
        txtColorBtn.addActionListener(new ActionListener() {;
            public void actionPerformed(ActionEvent e) {
                chooseColorForFg();
            }
        });
        // Try to mimik a button action for this label
        fgValueLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                chooseColorForFg();
            }
        });
    }
    
    private JPanel createPrivateBoxPane(GridBagConstraints constraints) {
        isPrivateBox = new JCheckBox("Is Private");
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(isPrivateBox, constraints);
        return panel;
    }
    
    private void chooseColorForFg() {
        Color selectedColor = JColorChooser.showDialog(RenderableDisplayFormatDialog.this,
                                                       "Choose Text Color",
                                                       fgValueLabel.getBackground());
        if (selectedColor != null) {
            fgValueLabel.setBackground(selectedColor);
        }
    }
    
    private void chooseLineColor() {
        Color selectedColor = JColorChooser.showDialog(RenderableDisplayFormatDialog.this,
                                                       "Choose Line Color",
                                                       lineColorValueLabel.getBackground());
        if (selectedColor != null) {
            lineColorValueLabel.setBackground(selectedColor);
        }
    }
    
    
    private void createLinePanel(GridBagConstraints constraints,
                                 Dimension valueSize) {
        linePanel = new JPanel();
        linePanel.setLayout(new GridBagLayout());
        JLabel widthLabel = new JLabel("Line Width: ");
        constraints.gridx = 0;
        constraints.gridy = 0;
        linePanel.add(widthLabel, constraints);
        widthTF = new JTextField();
        widthTF.setColumns(8);
        widthTF.setMinimumSize(valueSize);
        widthTF.setPreferredSize(valueSize);
        widthTF.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    float width = Float.parseFloat(widthTF.getText());
                }
                catch(NumberFormatException e1) {
                    JOptionPane.showMessageDialog(RenderableDisplayFormatDialog.this,
                                                  "Width should be a number.",
                                                  "Width Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    widthTF.requestFocus();
                    return;                 
                }
            }
        });
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        linePanel.add(widthTF, constraints);
    }
    
    private void createLineColorPane(GridBagConstraints constraints,
                                 Dimension valueSize,
                                 Dimension btnSize) {
        lineColorPanel = new JPanel();
        lineColorPanel.setLayout(new GridBagLayout());
        JLabel lineColorLabel = new JLabel("Line Color: ");
        constraints.gridx = 0;
        constraints.gridy = 0;
        lineColorPanel.add(lineColorLabel, constraints);
        lineColorValueLabel = new JLabel("");
        lineColorValueLabel.setPreferredSize(valueSize);
        lineColorValueLabel.setMinimumSize(valueSize);
        lineColorValueLabel.setOpaque(true);
        lineColorValueLabel.setBorder(BorderFactory.createEtchedBorder());
        constraints.gridx = 1;
        lineColorPanel.add(lineColorValueLabel, constraints);
        final JButton lineColorBtn = new JButton("...");
        lineColorBtn.setToolTipText("Click to choose a color");
        lineColorBtn.setPreferredSize(btnSize);
        lineColorBtn.setPreferredSize(btnSize);
        constraints.gridx = 2;
        lineColorPanel.add(lineColorBtn, constraints);
        lineColorBtn.addActionListener(new ActionListener() {;
            public void actionPerformed(ActionEvent e) {
                chooseLineColor();
            }
        });
        // Try to mimic a button action for this label
        lineColorLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                chooseLineColor();
            }
        });
    }
    
    private Float getLineWidth() {
        String txt = widthTF.getText().trim();
        if (txt.length() == 0)
            return null;
        return Float.valueOf(txt);
    }
    
    public boolean isOKClicked() {
        return this.isOKClicked;
    }
    
    public void setRenderables(List list) {
        if (list == null || list.size() == 0)
            return;
        renderables = new ArrayList<Renderable>(list);
        cache(list);
        validateGUIs();
    }
    
    private void cache(List renderables) {
        // reset maps
        fgMap.clear();
        bgMap.clear();
        lineColorMap.clear();
        lineWidthMap.clear();
        isPrivateMap.clear();
        Renderable r = null;
        StringBuilder builder = new StringBuilder();
        for (Iterator it = renderables.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            if (r instanceof Note) {
                fgMap.put(r, r.getForegroundColor());
                isPrivateMap.put(r, ((Note) r).isPrivate());
            }
            else if (r instanceof HyperEdge) {
                lineColorMap.put(r, r.getLineColor());
                float lineWidth = r.getLineWidth();
                lineWidthMap.put(r, new Float(lineWidth));
            }
            else if (r instanceof RenderableGene) {
                fgMap.put(r, r.getForegroundColor());
                lineColorMap.put(r, r.getLineColor());
            }
            else if (r instanceof SourceOrSink) {
                lineColorMap.put(r, r.getLineColor());
                bgMap.put(r, r.getBackgroundColor());
            }
            else { // It should be Node
                fgMap.put(r, r.getForegroundColor());
                bgMap.put(r, r.getBackgroundColor());
                lineColorMap.put(r, r.getLineColor());
                Float lineWidth = r.getLineWidth();
                if (lineWidth == null)
                    lineWidth = 1.0f; // Used as default
                lineWidthMap.put(r, lineWidth);
                if (r instanceof Node) {
                    Node node = (Node) r;
                    needDashedLineMap.put((Node)r, node.isNeedDashedBorder());
                }
            }
        }
    }
    
    private void validateGUIs() {
        // Default no panels are used
        if (fgMap.size() == 0)
            fgPanel.setVisible(false);
        else {
            Color c = (Color) fgMap.values().iterator().next(); // Get the first color
            setDisplayedForeground(c);
        }
        if (bgMap.size() == 0)
            bgPanel.setVisible(false);
        else {
            Color c = (Color) bgMap.values().iterator().next();
            setDisplayedBackground(c);
        }
        if (lineColorMap.size() == 0)
            lineColorPanel.setVisible(false);
        else {
            Color c = (Color) lineColorMap.values().iterator().next();
            setDisplayedLineColor(c);
        }
        if (lineWidthMap.size() == 0)
            linePanel.setVisible(false);
        else {
            Object width = lineWidthMap.values().iterator().next();
            widthTF.setText(width.toString());
        }
        if (needDashedLineMap.size() == 0)
            needDashedLine.setVisible(false);
        else {
            needDashedLine.setVisible(true);
            needDashedLine.setSelected(needDashedLineMap.values().iterator().next());
        }
        if (supportPrivateNote) {
            if (isPrivateMap.size() == 0)
                isPrivateBox.setVisible(false);
            else {
                Boolean isPrivate = isPrivateMap.values().iterator().next();
                isPrivateBox.setSelected(isPrivate);
                isPrivateBox.setVisible(true);
            }
        }
    }
    
    private void setDisplayedLineColor(Color c) {
        if (c == null)
            lineColorValueLabel.setBackground(DefaultRenderConstants.DEFAULT_OUTLINE_COLOR);
        else
            lineColorValueLabel.setBackground(c);
    }
    
    private void setDisplayedBackground(Color c) {
        if (c == null)
            bgValueLbl.setBackground(DefaultRenderConstants.DEFAULT_BACKGROUND);
        else
            bgValueLbl.setBackground(c);
    }
    
    private void setDisplayedForeground(Color c) {
        if (c == null)
            fgValueLabel.setBackground(DefaultRenderConstants.DEFAULT_FOREGROUND);
        else
            fgValueLabel.setBackground(c);
    }
}    
