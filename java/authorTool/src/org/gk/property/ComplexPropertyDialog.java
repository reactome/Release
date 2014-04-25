/*
 * Created on Sep 13, 2005
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

import org.gk.render.Renderable;

public class ComplexPropertyDialog extends PropertyDialog {
    
    private TextSummationPane summationPane;
    
    public ComplexPropertyDialog() {
        super();
    }
    
    public ComplexPropertyDialog(JFrame ownerFrame) {
        super(ownerFrame);
    }
    
    public ComplexPropertyDialog(JDialog ownerDialog) {
        super(ownerDialog);
    }
    
    public ComplexPropertyDialog(JFrame ownerFrame, Renderable entity) {
        this(ownerFrame);
        setRenderable(entity);
    }
    
    public ComplexPropertyDialog(JDialog ownerDialog, Renderable entity) {
        super(ownerDialog);
        setRenderable(entity);
    }

    protected JPanel createRequiredPropPane() {
        JPanel requiredPane = new JPanel();
        requiredPane.setLayout(new BoxLayout(requiredPane, BoxLayout.Y_AXIS));
        generalPropPane = createGeneralPropPane();
        Border border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                         "General");
        generalPropPane.setBorder(border);
        requiredPane.add(generalPropPane);
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
        summationPane = new TextSummationPane();
        optionalPane.add(summationPane);
        return optionalPane;
    }
    
    public void setRenderable(Renderable renderable) {
        super.setRenderable(renderable);
        summationPane.setRenderable(renderable);
    }
    
    public boolean commit() {
        boolean rtn = super.commit();
        if (!rtn)
            return false;
        summationPane.commit();
        return true; 
    }
}
