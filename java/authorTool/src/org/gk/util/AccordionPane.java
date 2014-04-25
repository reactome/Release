/*
 * Created on Nov 6, 2008
 *
 */
package org.gk.util;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A customized JPanel that can be used as an accordion pane.
 * @author wgm
 *
 */
public class AccordionPane extends JPanel {
    private Color titleColor;
    private PropertyChangeListener selectionController;
    private List<SectionTitlePane> titlePanes;
    
    public AccordionPane() {
        init();
    }
    
    public void setTitleColor(Color color) {
        this.titleColor = color;
    }
    
    public Color getTitleColor() {
        return this.titleColor;
    }
    
    /**
     * Add a component to this AccordionPane with a provided title.
     * @param title
     * @param tab
     */
    public void addTab(String title,
                       JComponent tab) {
        SectionTitlePane titlePane = new SectionTitlePane(title);
        titlePane.setBackground(titleColor);
        titlePane.setSectionPane(tab);
        titlePane.addPropertyChangeListener(selectionController);
        // Add to the pane
        titlePanes.add(titlePane);
        if (titlePanes.size() > 1) {// Give some pad
            add(Box.createVerticalStrut(2));
            titlePane.setIsClosed(true);
        }
        add(titlePane);
        add(tab);
    }
    
    /**
     * Set the named tab to be isClosed.
     * @param title
     * @param isClosed
     */
    public void setIsClosed(String title, boolean isClosed) {
        for (SectionTitlePane titlePane : titlePanes) {
            String tmp = titlePane.getTitle();
            if (tmp.equals(title)) {
                titlePane.setIsClosed(isClosed);
                break;
            }
        }
    }
    
    /**
     * Check if the specified tab is closed.
     * @param title
     * @return
     */
    public boolean isClosed(String title) {
        for (SectionTitlePane titlePane : titlePanes) {
            String tmp = titlePane.getTitle();
            if (tmp.equals(title)) {
                return titlePane.isClosed();
            }
        }
        return false;
    }
    
    private void init() {
        // As the default title color
        titleColor = new Color(204, 204, 255); // A kind light purple
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        titlePanes = new ArrayList<SectionTitlePane>();
        // Initialize a PropertyChangeListener
        // Make sure only one section is visible
        selectionController = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propName = e.getPropertyName();
                if (propName.equals("isClosed")) {
                    toggleSections(e);
                }
            }
        };
    }
    
    private void toggleSections(PropertyChangeEvent e) {
        if (titlePanes.size() == 1)
            return; // Don't close it since it has just one tab.
        Object src = e.getSource();
        Boolean isClosed = (Boolean) e.getNewValue();
        if (isClosed) {
            // Need to find the second title closed to the first
            int index = titlePanes.indexOf(src);
            index ++;
            if (index == titlePanes.size()) {
                index = 0;
            }
            SectionTitlePane nextTitle = titlePanes.get(index);
            nextTitle.setIsClosed(false);
        }
        else {
            // Close other titles
            for (SectionTitlePane titlePane : titlePanes) {
                if (titlePane == src)
                    continue; // Escape it
                else if (!titlePane.isClosed()) {
                    titlePane.setIsClosed(true);
                }
            }
        }
        firePropertyChange("sectionChanged", null, null);
    }
    
    /**
     * Get the SectionTitlePane that is opened in this AccordionPane.
     * @return
     */
    public SectionTitlePane getOpenedPane() {
        for (SectionTitlePane titlePane : titlePanes) {
            if (!titlePane.isClosed())
                return titlePane;
        }
        return null;
    }
    
}
