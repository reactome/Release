/*
 * Created on Nov 4, 2008
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.gk.property.RenderablePropertyChangeListener;
import org.gk.render.FlowLine;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.render.Shortcut;

/**
 * This customized JPanel is used to hold panels to do property editing.
 * @author wgm
 *
 */
public class PropertyEditorPane extends JPanel {
    // These TabbedPropertyPane will be displayed
    private PropertyPane entityPane;
    private PropertyPane complexPane;
    private PropertyPane eventPane;
    private JLabel titleLabel;
    // cached property change listner
    private RenderablePropertyChangeListener propertyChangeListener;
    
    public PropertyEditorPane() {
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        
        titleLabel = new JLabel("Property Editor");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        Border emptyBorder = BorderFactory.createEmptyBorder(3, 2, 3, 2);
        titleLabel.setBorder(emptyBorder);

        add(titleLabel, BorderLayout.NORTH);
        setMinimumSize(new Dimension(50, 50));
    }
    
    public PropertyPane getPropertyPane(Renderable r) {
        if (r instanceof RenderablePathway ||
            r instanceof RenderableReaction ||
            r instanceof FlowLine) { // Flow line is used as Interaction
            if (eventPane == null) {
                eventPane = new EventPropertyPane();
                eventPane.setBorder(BorderFactory.createEtchedBorder());
                installPropertyChangeListener(eventPane);
            }
            return eventPane;
        }
        if (r instanceof RenderableComplex) {
            if (complexPane == null) {
                complexPane = new ComplexPropertyPane();
                complexPane.setBorder(BorderFactory.createEtchedBorder());
                installPropertyChangeListener(complexPane);
            }
            return complexPane;
        }
        if (r instanceof Node) {
            if (entityPane == null) {
                entityPane = new EntityPropertyPane();
                entityPane.setBorder(BorderFactory.createEtchedBorder());
                installPropertyChangeListener(entityPane);
            }
            return entityPane;
        }
        return null;
    }
    
    public void setPropertyChangeListener(RenderablePropertyChangeListener l) {
        this.propertyChangeListener = l;
    }
    
    private void installPropertyChangeListener(PropertyPane propertyPane) {
        if (propertyChangeListener != null)
            propertyPane.addRenderablePropertyChangeListener(propertyChangeListener);
    }
    
    /**
     * Display properties for a Renderable object.
     * @param r
     */
    public void display(Renderable r) {
        if (r == null) {
            Component[] comps = getComponents();
            for (int i = 0; i < comps.length; i++) {
                if (comps[i] instanceof PropertyPane) {
                    // Hide this PropertyPane instead removing
                    // to avoid focus problem.
                    comps[i].setVisible(false);
                    break;
                }
            }
            return;
        }
        else {
            PropertyPane tabbedPane = getPropertyPane(r);
            if (tabbedPane == null)
                return;
            if (!tabbedPane.isVisible())
                tabbedPane.setVisible(true);
            // Need to replace displayed tabbedPane
            Component[] comp = getComponents();
            for (int i = 0; i < comp.length; i++) {
                if (comp[i] instanceof PropertyPane) {
                    if (comp[i] == tabbedPane) {
                        tabbedPane.setRenderable(r);
                        return; // It is there already
                    }
                    else {
                        comp[i].setVisible(false);
                        // Used as a flag
                        ((PropertyPane)comp[i]).setRenderable(null);
                        remove(i);
                        break;
                    }
                }
            }
            tabbedPane.setRenderable(r);
            add(tabbedPane, BorderLayout.CENTER);
            invalidate();
            validate();
            repaint();
        }
    }
    
    /**
     * Refresh the displayed Renderable object.
     * @param r
     */
    public void refresh(Renderable r) {
        PropertyPane propertyPane = getDisplayedPropertyPane();
        if (propertyPane != null) {
            Renderable displayed = propertyPane.getRenderable();
            if (r instanceof Shortcut)
                r = ((Shortcut)r).getTarget();
            if (displayed == r)
                propertyPane.refresh();
        }
    }
    
    private PropertyPane getDisplayedPropertyPane() {
        // Need to replace displayed tabbedPane
        Component[] comp = getComponents();
        for (int i = 0; i < comp.length; i++) {
            if (comp[i] instanceof PropertyPane) {
                return (PropertyPane) comp[i];
            }
        }
        return null;
    }
    
}
