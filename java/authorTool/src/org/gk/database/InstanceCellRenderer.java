/*
 * Created on Sep 22, 2003
 */
package org.gk.database;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.util.AuthorToolAppletUtilities;


public class InstanceCellRenderer extends DefaultListCellRenderer {
	// We only need one icon. Use it as a static variable.
	private static Icon icon = AuthorToolAppletUtilities.createImageIcon("Instance.gif");
    private boolean isSpeciesDisplayed;
    private boolean isSpeciesAfterName;
	
	public InstanceCellRenderer() {
		super();
	}
    
    public void setIsSpeciesDisplayed(boolean displayed) {
        this.isSpeciesDisplayed = displayed;
    }
    
    public boolean isSpeciesDisplayed() {
        return this.isSpeciesDisplayed;
    }
    
    public void setIsSpeciesAfterName(boolean isAfter) {
        this.isSpeciesAfterName = isAfter;
    }
    
    public boolean isSpeciesAfterName() {
        return this.isSpeciesAfterName;
    }
	
	public Component getListCellRendererComponent(JList list, Object value,
	                                     int index, boolean isSelected, 
	                                     boolean hasFocus) {
		Component comp = super.getListCellRendererComponent(list, value,
		                                 index, isSelected, hasFocus());
		String text = null;
        String tooltipText = null;
		boolean isDirty = false;
		if (value instanceof GKInstance) {
			GKInstance instance = (GKInstance) value;
			text = instance.getDisplayName();
			if (text == null ||
			   (text != null && text.length() == 0)) {
				text = instance.getExtendedDisplayName();
			}
			setIcon(icon);
			isDirty = ((GKInstance)value).isDirty();
            tooltipText = instance.getExtendedDisplayName();
		}
		else if (value != null) {
			text = value.toString();
            tooltipText = text;
			setIcon(null);
		}
		if (text == null) {
			text = "";
			setIcon(null);
		}
        if (isSpeciesDisplayed) {
            String species = getSpecies(value);
            if (species != null) {
                if (isSpeciesAfterName) {
                    text = text + " [" + species + "]";
                }
                else
                    text = "[" + species + "] " + text; 
            }
        }
		if (isDirty)
			setText(">" + text);
		else
			setText(text);
		setToolTipText(tooltipText);
		return comp;
	}
    
    private String getSpecies(Object value) {
        if (!(value instanceof GKInstance))
            return null;
        GKInstance instance = (GKInstance) value;
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species))
            return null;
        try {
            GKInstance species = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
            if (species == null)
                return null;
            return species.getDisplayName();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}