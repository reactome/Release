/*
 * Created on Jun 25, 2003
 */
package org.gk.property;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableRegistry;

/**
 * This customized JPanel is used for name editing.
 * @author wgm
 */
public class GeneralPropertyPane extends RenderablePropertyPane {
	protected JTextField displayNameField;
	private JComboBox taxonBox;
    private JLabel taxonLabel;
    private JComboBox localizationBox;
    private JLabel localizationLabel;
	// Cache the old name for comparison
    private String oldName;
    private String oldTaxon = "";
    private String oldLocalization = "";
	
	public GeneralPropertyPane() {
	    init();
    }
	
	private void init() {
		setLayout(new GridBagLayout());
		// Add Display Name
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
		initNameSection(constraints);
		// Add taxon
        initTaxonSection(constraints);
        initLocalizationSection(constraints);
        // As of July 14, 2008, location values will
        // be calculated from the graph editor pane.
        localizationBox.setVisible(false);
        localizationLabel.setVisible(false);
	}
	
	protected void initNameSection(GridBagConstraints constraints) {
		// Add Display Name
		JLabel displayNameLabel = new JLabel("Name:");
		add(displayNameLabel, constraints);
		displayNameField = new JTextField();
        displayNameField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateDisplayName();
            }
        });
        displayNameField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateDisplayName();
            }
        });
		constraints.gridx = 1;
		add(displayNameField, constraints);
	}
    
    private void updateDisplayName() {
        String newText = displayNameField.getText().trim();
        if (r == null)
            return;
        String oldName = r.getDisplayName();
        if (newText.equals(oldName))
            return;
        fireRenderablePropertyChange(r, 
                                     RenderablePropertyNames.DISPLAY_NAME, 
                                     oldName, 
                                     newText);
    }
    
    public void setTaxonSettingVisible(boolean isVisible) {
        taxonLabel.setVisible(isVisible);
        taxonBox.setVisible(isVisible);
    }
	
	protected void initTaxonSection(GridBagConstraints constraints) {
	    taxonLabel = new JLabel("Taxon:");
	    constraints.gridx = 0;
	    constraints.gridy = 1;
	    add(taxonLabel, constraints);
	    ComboBoxModel taxonModel = PropertyManager.getManager().getTaxonModel();
	    taxonBox = new JComboBox();
        constraints.gridx = 1;
	    add(taxonBox, constraints);
        taxonBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Don't care if during setting or r is null
                if (duringSetting || r == null)
                    return;
                String newTaxon = (String) taxonBox.getSelectedItem();
                if (newTaxon == null)
                    newTaxon = "";
                String oldTaxon = (String) r.getAttributeValue(RenderablePropertyNames.TAXON);
                if (oldTaxon == null)
                    oldTaxon = "";
                if (newTaxon.equals(oldTaxon))
                    return;
                fireRenderablePropertyChange(r, 
                                             RenderablePropertyNames.TAXON, 
                                             oldTaxon, 
                                             newTaxon);
                PropertyManager.getManager().ensureNewTaxon(newTaxon);
            }
        });
        taxonBox.setEditable(true);
	}
	
	protected void initLocalizationSection(GridBagConstraints constraints) {
        localizationLabel = new JLabel("Localization:");
        constraints.gridx = 0;
        constraints.gridy = 2;
        add(localizationLabel, constraints);
        localizationBox = new JComboBox();
        localizationBox.setEditable(true);
        // Fill in data
        ComboBoxModel model = PropertyManager.getManager().getLocalizationModel();
        localizationBox.setModel(model);
        constraints.gridx = 1;
        add(localizationBox, constraints);
        localizationBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (r == null)
                    return;
                String newLoc = (String) localizationBox.getSelectedItem();
                if (newLoc == null)
                    newLoc = "";
                String oldLoc = (String) r.getAttributeValue(RenderablePropertyNames.LOCALIZATION);
                if (oldLoc == null)
                    oldLoc = "";
                if (newLoc.equals(oldLoc))
                    return;
                PropertyManager.getManager().ensureNewLocalization(newLoc);
                fireRenderablePropertyChange(r, 
                                             RenderablePropertyNames.LOCALIZATION,
                                             oldLoc,
                                             newLoc);
            }
        });
    }

	public String getDisplayName() {
		return displayNameField.getText().trim();
	}
	
	public void setDisplayName(String displayName) {
		displayNameField.setText(displayName);
        this.oldName = displayName;
	}
	
	public void setTaxon(String taxonName) {
		if (taxonBox != null) {
		    // Need to use a new model to avoid conflict between differnt views (
		    // pathways, entities or complexes)
			taxonBox.setModel(PropertyManager.getManager().getTaxonModel());
		    taxonBox.setSelectedItem(taxonName);
		}
        this.oldTaxon = taxonName;
        if (oldTaxon == null)
            oldTaxon = ""; // Ease comparison
	}
	
	public String getTaxon() {
        String rtn = null;
		if (taxonBox != null) {
			rtn = (String) taxonBox.getSelectedItem();
        }
		return rtn;
	}
    
    public void setLocalization(String localizationName) {
        localizationBox.setSelectedItem(localizationName);
        this.oldLocalization = localizationName;
        if (oldLocalization == null)
            oldLocalization = "";
    }
    
    public String getLocalization() {
        String rtn = (String) localizationBox.getSelectedItem();
        return rtn;
    }

	public JTextField getDisplayNameField() {
		return this.displayNameField;
	}
	
	public void setRenderable(Renderable renderable) {
	    duringSetting = true;
		super.setRenderable(renderable);
		if (renderable == null)
			return;
		setDisplayName(renderable.getDisplayName());
		setTaxon((String)renderable.getAttributeValue(RenderablePropertyNames.TAXON));
        setLocalization((String)renderable.getAttributeValue(RenderablePropertyNames.LOCALIZATION)); 
        duringSetting = false;
	}
    
    public boolean commit() {
        Renderable renderable = getRenderable();
        String newDisplayName = getDisplayName();
        if (!newDisplayName.equals(oldName)) {
            if (newDisplayName.length() == 0) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty. \nPlease input a non-empty name.",
                                              "Empty Name Error",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (RenderableRegistry.getRegistry().contains(newDisplayName)) {
                JOptionPane.showMessageDialog(this, 
                        "Name \"" + newDisplayName + 
                "\" has already been used by another object in the project.\nPlease use another name.",
                "Name Duplication Error", 
                JOptionPane.ERROR_MESSAGE);
                return false;
            }
            RenderUtility.rename(renderable, newDisplayName);
            renderable.setIsChanged(true);
        }
        String taxon = getTaxon();
        String oldTaxon = (String) renderable.getAttributeValue("taxon");
        boolean needSet = false;
        if (oldTaxon == null && taxon != null)
            needSet = true;
        else if (oldTaxon != null && !oldTaxon.equals(taxon))
            needSet = true;
        if (needSet) {
            if (taxon != null) {
                if (RenderUtility.checkProperty(renderable, "taxon", taxon)) {
                    renderable.setAttributeValue("taxon", taxon);
                    RenderUtility.setPropertyForDescendents(renderable, "taxon", taxon);
                }
                else {
                    JOptionPane.showMessageDialog(
                                                  this,
                                                  "The selected taxon value cannot be used because the container of \n\""
                                                  + renderable.getDisplayName()
                                                  + "\" has different taxon value.",
                                                  "Taxon Setting Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    // It is OK. Don't need to return false
                }
            }
            else
                renderable.setAttributeValue("taxon", null);
            renderable.setIsChanged(true);
        }
        String localization = getLocalization();
        needSet = false;
        String oldLoc = (String) renderable.getAttributeValue(RenderablePropertyNames.LOCALIZATION);
        if (oldLoc == null && localization != null)
            needSet = true;
        else if (oldLoc != null && !oldLoc.equals(localization))
            needSet = true;
        if (needSet) {
            if (localization == null)
                renderable.setAttributeValue(RenderablePropertyNames.LOCALIZATION,
                                             null);
            else
                renderable.setAttributeValue(RenderablePropertyNames.LOCALIZATION,
                                             localization);
            renderable.setIsChanged(true);
        }
        return true;
    }
}
