/*
 * Created on May 24, 2004
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import org.gk.property.AlternativeNameListPane;
import org.gk.property.GeneralPropertyPane;
import org.gk.property.RenderablePropertyChangeEvent;
import org.gk.property.RenderablePropertyChangeListener;
import org.gk.property.RenderablePropertyPane;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.BrowserLauncher;
import org.gk.util.SectionTitlePane;
/**
 * A customized JPanel to display properties for Renderable objects.
 * @author wugm
 */
public class PropertyPane extends JPanel {
	
	protected Renderable r;
	// To catch RenderablePropertyChangeEvents
	protected java.util.List renderablePropertyChangeListeners;
	// To control property firing
	protected boolean needFirePropertyChange = true;
    private JLabel reactomeIdLabel;
    private JButton browseBtn;
    protected GeneralPropertyPane generalPane;
    protected AlternativeNameListPane alternativeNamePane;
    // A relay for firing property change event. This listener
    // is added for individual PropertyPane, e.g., PropertyListPane
    // ReferencePane, etc.
    protected RenderablePropertyChangeListener propChangeListener;
	
	public PropertyPane() {
		super();
		//setTabPlacement(JTabbedPane.LEFT);
	}
	
	public void setRenderable(Renderable r) {
        // Call refresh if refresh is needed.
        if (this.r == r)
            return;
		this.r = r;
        setProperties(r);
    }

    private void setProperties(Renderable r) {
        if (r == null)
            return;
        // Reactome ID
        String reactomeId = getReactomeId(r);
        if (reactomeId != null) {
            reactomeIdLabel.setText(reactomeId.toString());
            // Negative DB_ID cannot be used to search database
            browseBtn.setEnabled(!reactomeId.startsWith("-"));
        }
        else {
            reactomeIdLabel.setText("Unknown");
            browseBtn.setEnabled(false);
        }	
        generalPane.setRenderable(r);
        alternativeNamePane.setRenderable(r);
    }
    
    private String getReactomeId(Renderable r) {
        Set<Long> dbIds = new HashSet<Long>();
        List<Renderable> shortcuts = r.getShortcuts();
        if (shortcuts == null || shortcuts.size() == 0) {
            Long dbId = r.getReactomeId();
            if (dbId != null)
                dbIds.add(dbId);
        }
        else {
            // Want to get all DB_IDs
            for (Renderable tmp : shortcuts) {
                if (tmp.getReactomeId() != null)
                    dbIds.add(tmp.getReactomeId());
            }
        }
        if (dbIds.size() == 0)
            return null;
        List<Long> idList = new ArrayList<Long>(dbIds);
        Collections.sort(idList);
        StringBuilder builder = new StringBuilder();
        for (Iterator<Long> it = idList.iterator(); it.hasNext();) {
            Long id = it.next();
            builder.append(id);
            if (it.hasNext())
                builder.append(", ");
        }
        return builder.toString();
    }

	public Renderable getRenderable() {
		return this.r;
	}
	
    protected void initTabs() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        Border etchedBorder = BorderFactory.createEtchedBorder();
        JComponent required = createRequiredPane();
        SectionTitlePane requiredTitle = createTitlePane("Required Properties",
                                                         required);
        JComponent optional = createOptionalPane();
        SectionTitlePane optionalTitle = createTitlePane("Optional Properties",
                                                         optional);
        optionalTitle.setSectionPane(optional);
        contentPane.add(requiredTitle);
        contentPane.add(required);
        contentPane.add(Box.createVerticalStrut(6));
        contentPane.add(optionalTitle);
        contentPane.add(optional);
        // To catch the properties editing events.
        setLayout(new BorderLayout());
        JScrollPane jsp = new JScrollPane(contentPane);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(jsp, BorderLayout.CENTER);
        installListeners();
    }
    
    private SectionTitlePane createTitlePane(String title,
                                             JComponent sectionPane) {
        SectionTitlePane titlePane = new SectionTitlePane(title);
        titlePane.setBackground(new Color(204, 204, 255)); // A kind light purple
        titlePane.setSectionPane(sectionPane);
        return titlePane;
    }
	
	public void addRenderablePropertyChangeListener(RenderablePropertyChangeListener l) {
		if (renderablePropertyChangeListeners == null)
			renderablePropertyChangeListeners = new ArrayList();
		renderablePropertyChangeListeners.add(l);
	}
	
	public void removeRenderablePropertyChangeListener(RenderablePropertyChangeEvent l) {
		if (renderablePropertyChangeListeners != null)
			renderablePropertyChangeListeners.remove(l);
	}
	
	public void fireRenderablePropertyChange(RenderablePropertyChangeEvent e) {
		if (!needFirePropertyChange || renderablePropertyChangeListeners == null)
			return;
		RenderablePropertyChangeListener l = null;
		for (Iterator it = renderablePropertyChangeListeners.iterator(); it.hasNext();) {
			l = (RenderablePropertyChangeListener) it.next();
			l.propertyChange(e);
		}
	}
	
	public void refresh() {
        if (r != null)
            setProperties(r);
	}
    
    protected void installListeners() {
        propChangeListener = new RenderablePropertyChangeListener() {
            public void propertyChange(RenderablePropertyChangeEvent e) {
                Renderable r = e.getRenderable();
                // Commit property changes here
                // A special case for displayName
                String propName = e.getPropName();
                if (propName.equals(RenderablePropertyNames.DISPLAY_NAME)) {
                    RenderUtility.rename(r, (String)e.getNewValue());
                }
                else if (!propName.equals("stoichiometry"))  // Another special case
                    r.setAttributeValue(propName, e.getNewValue());
                r.setIsChanged(true);
                // Forward propertyChangeEvent
                fireRenderablePropertyChange(e);
            }
        };
        // Add this propertyChangeListener to all RenderablePropertyPane
        installListener(this);
    }
    
    private void installListener(JComponent comp) {
        if (comp instanceof RenderablePropertyPane) {
            ((RenderablePropertyPane)comp).addRenderablePropertyChangeListener(propChangeListener);
            return;
        }
        Component[] comps = comp.getComponents();
        if (comps == null)
            return;
        for (int i = 0; i < comps.length; i++) {
            installListener((JComponent)comps[i]);
        }
    }
    
    protected JComponent createRequiredPane() {
        return null;
    }
    
    protected JComponent createOptionalPane() {
        return null;
    }
    
    protected JPanel createReactomeIDPane() {
        JPanel reactomeIdPane = new JPanel();
        reactomeIdPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 0);
        JLabel label = new JLabel("Reactome ID:");
        reactomeIdLabel = new JLabel("Unknown");
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 0.5;
        reactomeIdPane.add(label, constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(4, 0, 4, 4);
        reactomeIdPane.add(reactomeIdLabel, constraints);
        browseBtn = new JButton("View in Reactome", AuthorToolAppletUtilities.createImageIcon("WebComponent16.gif"));
        browseBtn.setToolTipText("Click to go the Reactome web site for this instance");
        browseBtn.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) { 
               viewInReactome();
           }
        });
        constraints.gridx = 2;
        constraints.anchor = GridBagConstraints.EAST;
        reactomeIdPane.add(browseBtn, constraints);
        return reactomeIdPane;
    }
    
    private void viewInReactome() {
        String url = AuthorToolAppletUtilities.REACTOME_INSTANCE_URL;
           String id = reactomeIdLabel.getText();
           if (id == null || id.length() == 0)
               return;
           // Display the first id for the time being
           int index = id.indexOf(",");
           if (index > 0) {
               // Need to concatenate ids together
               String[] ids = id.split(",");
               StringBuilder builder = new StringBuilder();
               for (int i = 0; i < ids.length; i++) {
                   builder.append(ids[i].trim());
                   if (i < ids.length - 1)
                       builder.append("&ID=");
               }
               id = builder.toString();
           }
           if (AuthorToolAppletUtilities.isInApplet) {
               AuthorToolAppletUtilities.displayURL(url + id, 
                                                    AuthorToolAppletUtilities.REACTOME_BROWSER_NAME);
           }
           else {
               try {
                   BrowserLauncher.displayURL(url + id, PropertyPane.this);
               } 
               catch (IOException e1) {
                   System.err.println("PropertyDialog.createReactomeIDPane(): " + e1);
                   e1.printStackTrace();
               }
           }
    }
    
    protected AlternativeNameListPane createAlternativeNamesPane() {
        // For setting alternative names
        AlternativeNameListPane alternativeNamePane = new AlternativeNameListPane("Alternative Names");
        Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        Border compoundBorder = BorderFactory.createCompoundBorder(etchedBorder, emptyBorder);
        alternativeNamePane.setBorder(compoundBorder);
        return alternativeNamePane;
    }

}
