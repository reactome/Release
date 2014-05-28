/*
 * Created on Oct 31, 2006
 *
 */
package org.gk.gkEditor;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.gk.property.DBInfoPane;
import org.gk.property.GeneralPropertyPane;
import org.gk.property.ModificationPane;
import org.gk.render.Renderable;
import org.gk.render.RenderableChemical;

public class EntityPropertyPane extends PropertyPane {
    private DBInfoPane dbInfoPane;
    private ModificationPane modificationPane;
    
    public EntityPropertyPane() {
        initTabs();
    }
    
    protected JComponent createRequiredPane() {
        JPanel requiredPane = new JPanel();
        requiredPane.setLayout(new BoxLayout(requiredPane, BoxLayout.Y_AXIS));
        dbInfoPane = new DBInfoPane();
        Border border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                      "Database Info");
        dbInfoPane.setBorder(border);
        generalPane = new GeneralPropertyPane();
        generalPane.setBorder(BorderFactory.createEtchedBorder());
        requiredPane.add(generalPane);
        requiredPane.add(Box.createVerticalStrut(6));
        requiredPane.add(dbInfoPane);
        requiredPane.setPreferredSize(new Dimension(300, 250));
        return requiredPane;
    }
    
    protected JComponent createOptionalPane() {
        JPanel optionalPane = new JPanel();
        BoxLayout layout = new BoxLayout(optionalPane, BoxLayout.Y_AXIS);
        optionalPane.setLayout(new BoxLayout(optionalPane, BoxLayout.Y_AXIS));
        JPanel reactomeIdPane = createReactomeIDPane();
        reactomeIdPane.setBorder(BorderFactory.createEtchedBorder());
        optionalPane.add(reactomeIdPane);
        optionalPane.add(Box.createVerticalStrut(4));
        // For setting alternative names
        alternativeNamePane = createAlternativeNamesPane();
        // Set a rather big preferred size so that this panel can occupy 
        // more space.
        alternativeNamePane.setPreferredSize(new Dimension(200, 400));
        optionalPane.add(alternativeNamePane);
        optionalPane.add(Box.createVerticalStrut(4));
        modificationPane = new ModificationPane();
        modificationPane.setBorder(BorderFactory.createEtchedBorder());
        optionalPane.add(modificationPane);
        optionalPane.setPreferredSize(new Dimension(300, 250));
        return optionalPane;
    }
    
    public void setRenderable(Renderable r) {
        super.setRenderable(r);
        if (r instanceof RenderableChemical) {
            generalPane.setTaxonSettingVisible(false);
            modificationPane.setVisible(false);
        }
        else {
            generalPane.setTaxonSettingVisible(true);
            modificationPane.setVisible(true);
            modificationPane.setRenderable(r);
        }
        dbInfoPane.setRenderable(r);
    }
}
