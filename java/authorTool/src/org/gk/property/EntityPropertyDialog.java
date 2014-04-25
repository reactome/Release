/*
 * Created on Jun 24, 2003
 *
 */
package org.gk.property;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.ReferenceDatabase;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;

/**
 * @author wgm
 * This is a dialog container for other property JPanel.
 */
public class EntityPropertyDialog extends PropertyDialog {
	private DBInfoPane dbInfoPane;
    private ModificationPane modificationPane;

	public EntityPropertyDialog() {
		super();
	}
	
	public EntityPropertyDialog(JFrame ownerFrame) {
		super(ownerFrame);
	}
	
	public EntityPropertyDialog(JDialog ownerDialog) {
		super(ownerDialog);
	}
	
	public EntityPropertyDialog(JFrame ownerFrame, Renderable entity) {
		this(ownerFrame);
		setRenderable(entity);
	}
	
	public EntityPropertyDialog(JDialog ownerDialog, Renderable entity) {
		super(ownerDialog);
		setRenderable(entity);
    }
	
	public void setRenderable(Renderable renderable) {
		super.setRenderable(renderable);
		dbInfoPane.setRenderable(renderable);
		modificationPane.setRenderable(renderable);
	}
	
	protected boolean commit() {
		boolean rtn = super.commit();
		if (!rtn)
			return false;
		// For reference db
		DatabaseIdentifier identifier = (DatabaseIdentifier) renderable.getAttributeValue(RenderablePropertyNames.DATABASE_IDENTIFIER);
		if (identifier == null) {
			identifier = new DatabaseIdentifier();
		}
        DatabaseIdentifier newIdentifier = new DatabaseIdentifier();
		ReferenceDatabase database = dbInfoPane.getSelectedDB();
		if (database != null)
			newIdentifier.setDbName(database.toString());
		else
			newIdentifier.setDbName(null);
		// For accession no
		String accessNo = dbInfoPane.getAccessNo();
		newIdentifier.setAccessNo(accessNo);
		if (!identifier.equals(newIdentifier)) {
		    renderable.setAttributeValue(RenderablePropertyNames.DATABASE_IDENTIFIER, 
                                         newIdentifier.isEmpty() ? null : newIdentifier);
		    renderable.setIsChanged(true);
        }
		// For modification
		modificationPane.commit();
		return true;
	}
	
	public void selectTabForProperty(String propertyName) {
		if (propertyName == null)
			return;
		if (propertyName.equals("names") ||
			propertyName.equals("defintion") ||
			propertyName.equals("taxon")) 
			tabbedPane.setSelectedIndex(0);
		else if (propertyName.equals("referenceDB") ||
		         propertyName.equals("accessNo"))
		    tabbedPane.setSelectedIndex(1);
		else if (propertyName.equals("modification"))
			tabbedPane.setSelectedIndex(2);
	}
    
    protected JPanel createRequiredPropPane() {
        JPanel requiredPane = new JPanel();
        requiredPane.setLayout(new BoxLayout(requiredPane, BoxLayout.Y_AXIS));
        generalPropPane = createGeneralPropPane();
        Border border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                         "General");
        generalPropPane.setBorder(border);
        requiredPane.add(generalPropPane);
        requiredPane.add(Box.createVerticalStrut(6));
        dbInfoPane = new DBInfoPane();
        border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                  "Database Info");
        dbInfoPane.setBorder(border);
        requiredPane.add(dbInfoPane);
        return requiredPane;
    }
    
    protected JPanel createOptionalPropPane() {
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
        modificationPane = new ModificationPane();
        modificationPane.setBorder(BorderFactory.createEtchedBorder());
        optionalPane.add(modificationPane);
        return optionalPane;
    }
}
