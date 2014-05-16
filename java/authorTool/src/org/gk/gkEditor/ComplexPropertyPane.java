/*
 * Created on May 24, 2004
 */
package org.gk.gkEditor;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.gk.property.GeneralPropertyPane;
import org.gk.property.TextSummationPane;
import org.gk.render.Renderable;

/**
 * 
 * @author wugm
 */
public class ComplexPropertyPane extends PropertyPane {
	private TextSummationPane textPane;
	
	public ComplexPropertyPane() {
		initTabs();
	}
	
    protected JComponent createRequiredPane() {
        JPanel requiredPane = new JPanel();
        requiredPane.setLayout(new BoxLayout(requiredPane, BoxLayout.Y_AXIS));
        generalPane = new GeneralPropertyPane();
        requiredPane.add(generalPane);
        requiredPane.setPreferredSize(new Dimension(300, 120));
        return requiredPane;
    }
	
    protected JComponent createOptionalPane() {
        JPanel optionalPane = new JPanel();
        optionalPane.setLayout(new BoxLayout(optionalPane, BoxLayout.Y_AXIS));
        JPanel reactomeIdPane = createReactomeIDPane();
        reactomeIdPane.setBorder(BorderFactory.createEtchedBorder());
        optionalPane.add(reactomeIdPane);
        optionalPane.add(Box.createVerticalStrut(4));
        // For setting alternative names
        alternativeNamePane = createAlternativeNamesPane();
        optionalPane.add(alternativeNamePane);
        optionalPane.add(Box.createVerticalStrut(4));
        textPane = new TextSummationPane();
        optionalPane.add(textPane);
        // Assign size
        alternativeNamePane.setPreferredSize(new Dimension(300, 90));
        textPane.setPreferredSize(new Dimension(300, 250));
        optionalPane.setPreferredSize(new Dimension(300, 380));
        return optionalPane;
    }
    
	public void setRenderable(Renderable r) {
		needFirePropertyChange = false; // Disable property change
		                                // because it reading data
		super.setRenderable(r);
		generalPane.setRenderable(r);
		textPane.setRenderable(r);
		needFirePropertyChange = true;
	}
	
	public void refresh() {
		if (r == null)
			return;
		generalPane.setRenderable(r);
		textPane.setRenderable(r);
	}
}
