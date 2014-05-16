/*
 * Created on Jun 26, 2003
 */
package org.gk.property;

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableReaction;

/**
 * The dialog for editing pathway properties.
 * @author wgm
 */
public class EventPropertyDialog extends PropertyDialog {
	protected TextSummationPane summationPane;
	protected ReferencePane referencePane;
	protected AttachmentPane attachmentPane;
    protected PrecedingEventListPane precedingPane;
    private JCheckBox isReversibleBox;
    private JPanel reversiblePane;
	
	/**
	 * Only for subclasses.
	 * @param parentFrame
	 */
	protected EventPropertyDialog(JFrame parentFrame) {
		super(parentFrame);
	}
	
	public EventPropertyDialog(JFrame parentFrame, Renderable pathway) {
		super(parentFrame);
		setRenderable(pathway);
	}
	
	public EventPropertyDialog(JFrame parentFrame, Renderable pathway, boolean needNameChecking) {
		this(parentFrame, pathway);
		super.needNameChecking = needNameChecking;
	}

	public void setRenderable(Renderable renderable) {
		super.setRenderable(renderable);
		summationPane.setRenderable(renderable);
		referencePane.setRenderable(renderable);
        precedingPane.setRenderable(renderable);
        attachmentPane.setRenderable(renderable);
        String isReversible = (String) renderable.getAttributeValue(RenderablePropertyNames.IS_REVERSIBLE);
        if (isReversible != null && isReversible.equals("true"))
            isReversibleBox.setSelected(true);
        else
            isReversibleBox.setSelected(false);
	}
    
	protected boolean commit() {
		boolean rtn = super.commit();
		if (!rtn)
			return false;
		// For extra preceding events
		java.util.List precedingEvents = precedingPane.getPrecedingEvents();
        setListAttributeValue(precedingEvents, "precedingEvent");
		summationPane.commit();
		java.util.List references = referencePane.getReferences();
        setListAttributeValue(references, "references");
        setListAttributeValue(attachmentPane.getAttachments(), "attachments");
        if (reversiblePane.isVisible() && (renderable instanceof ReactionNode)) {
            boolean isReversible = isReversibleBox.isSelected();
            RenderableReaction reaction = ((ReactionNode)renderable).getReaction();
            String booleanStr = (String) renderable.getAttributeValue(RenderablePropertyNames.IS_REVERSIBLE);
            if (booleanStr == null)
                booleanStr = "false";
            if (!booleanStr.equals(isReversible + "")) {
                renderable.setAttributeValue(RenderablePropertyNames.IS_REVERSIBLE,
                                             isReversible + "");
                reaction.setNeedInputArrow(isReversible);
                renderable.setIsChanged(true);
            }
        }
	    return true;
	}
	
	/**
	 * Override the method in the super class.
	 * @param propertyName the name of the property. 
	 */
	public void selectTabForProperty(String propertyName) {
		if (propertyName == null)
			return;
		if (propertyName.equals("names") ||
		    propertyName.equals("definition")) 
		    tabbedPane.setSelectedIndex(0);
		else 
			tabbedPane.setSelectedIndex(1);
	}
    
    protected JPanel createRequiredPropPane() {
        JPanel requiredPane = new JPanel();
        requiredPane.setLayout(new BoxLayout(requiredPane, BoxLayout.Y_AXIS));
        generalPropPane = createGeneralPropPane();
        Border border = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                                                         "General");
        generalPropPane.setBorder(border);
        requiredPane.add(generalPropPane);
        referencePane = new ReferencePane();
        referencePane.setBorder(BorderFactory.createEtchedBorder());
        requiredPane.add(Box.createVerticalStrut(6));
        requiredPane.add(referencePane);
        summationPane = new TextSummationPane();
        requiredPane.add(Box.createVerticalStrut(6));
        requiredPane.add(summationPane);
        return requiredPane;
    }
    
    protected JPanel createOptionalPropPane() {
        JPanel optionalPane = new JPanel();
        optionalPane.setLayout(new BoxLayout(optionalPane, BoxLayout.Y_AXIS));
        JPanel reactomeIdPane = createReactomeIDPane();
        reactomeIdPane.setBorder(BorderFactory.createEtchedBorder());
        optionalPane.add(reactomeIdPane);
        optionalPane.add(Box.createVerticalStrut(6));
        // For setting alternative names
        alternativeNamePane = createAlternativeNamesPane();
        optionalPane.add(alternativeNamePane);
        optionalPane.add(Box.createVerticalStrut(6));
        // For preceding pane
        precedingPane = new PrecedingEventListPane();
        optionalPane.add(Box.createVerticalStrut(6));
        optionalPane.add(precedingPane);
        // For attachment
        attachmentPane = new AttachmentPane();
        optionalPane.add(Box.createVerticalStrut(6));
        optionalPane.add(attachmentPane);
        initReversiblePane(optionalPane);
        return optionalPane;
    }	
    
    public void setIsReversiblePaneVisible(boolean isVisible) {
        reversiblePane.setVisible(isVisible);
    }
    
    private void initReversiblePane(JPanel optionalPane) {
        isReversibleBox = new JCheckBox("Is Reversible Reaction");
        reversiblePane = new JPanel();
        reversiblePane.setLayout(new FlowLayout(FlowLayout.LEFT));
        reversiblePane.setBorder(BorderFactory.createEtchedBorder());
        reversiblePane.add(isReversibleBox);
        optionalPane.add(Box.createVerticalStrut(6));
        optionalPane.add(reversiblePane);
    }
}