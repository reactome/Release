/*
 * Created on May 24, 2004
 */
package org.gk.gkEditor;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.gk.property.AttachmentPane;
import org.gk.property.GeneralPropertyPane;
import org.gk.property.ReferencePane;
import org.gk.property.RenderablePropertyPane;
import org.gk.property.TextSummationPane;
import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableReaction;

/**
 * Property JTabbedPane for Reaction and Pathway. The difference between panes for Reaction
 * and Pathway is the one for Reaction has a isReversible checkbox, which is in the optional
 * tab.
 * @author wugm
 */
public class EventPropertyPane extends PropertyPane {
	private TextSummationPane textPane;
	private ReferencePane referencePane;
	private AttachmentPane attachmentPane;
    private ReversiblePane reversiblePane;

	public EventPropertyPane() {
		initTabs();
	}
	
	public EventPropertyPane(boolean isForReaction) {
		initTabs();
	}
    
    protected JComponent createRequiredPane() {
        JPanel requiredPane = new JPanel();
        requiredPane.setLayout(new BoxLayout(requiredPane, BoxLayout.Y_AXIS));
        generalPane = new GeneralPropertyPane();
        generalPane.setBorder(BorderFactory.createEtchedBorder());
        requiredPane.add(generalPane);
        referencePane = new ReferencePane();
        Border empty = BorderFactory.createEmptyBorder(0, 1, 0, 1);
        Border etched = BorderFactory.createEtchedBorder();
        referencePane.setBorder(BorderFactory.createCompoundBorder(etched, empty));
        requiredPane.add(Box.createVerticalStrut(6));
        requiredPane.add(referencePane);
        textPane = new TextSummationPane();
        requiredPane.add(textPane);
        // Make size correct
        generalPane.setPreferredSize(new Dimension(300, 100));
        referencePane.setPreferredSize(new Dimension(300, 100));
        textPane.setPreferredSize(new Dimension(300, 300));
        requiredPane.setPreferredSize(new Dimension(300, 500));
        return requiredPane;
    }
    
    protected JComponent createOptionalPane() {
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
        // as of Jan 10, 2007, precedingEvent is not needed any more in 
        // the entity level view.
//        // For preceding pane
//        precedingPane = new PrecedingEventListPane();
//        optionalPane.add(Box.createVerticalStrut(6));
//        optionalPane.add(precedingPane);
        // For attachment
        attachmentPane = new AttachmentPane();
        attachmentPane.setBorder(BorderFactory.createEtchedBorder());
        optionalPane.add(Box.createVerticalStrut(6));
        optionalPane.add(attachmentPane);
        initReversiblePane(optionalPane);
        optionalPane.setPreferredSize(new Dimension(300, 280));
        return optionalPane;
    }
    
    public void setIsReversiblePaneVisible(boolean isVisible) {
        reversiblePane.setVisible(isVisible);
    }
    
    private void initReversiblePane(JPanel optionalPane) {
        reversiblePane = new ReversiblePane();
        optionalPane.add(Box.createVerticalStrut(6));
        optionalPane.add(reversiblePane);
    }

    protected void installListeners() {
		super.installListeners();
        textPane.addRenderablePropertyChangeListener(propChangeListener);
		referencePane.addRenderablePropertyChangeListener(propChangeListener);
		attachmentPane.addRenderablePropertyChangeListener(propChangeListener);
        reversiblePane.addRenderablePropertyChangeListener(propChangeListener);
	}
	
	public void setRenderable(Renderable r) {
		needFirePropertyChange = false; // Disable property change event since
		                                // it is initializing values
		super.setRenderable(r);
        if (r instanceof RenderablePathway)
            reversiblePane.setVisible(false);
        else
            reversiblePane.setVisible(true);
		textPane.setRenderable(r);
		referencePane.setRenderable(r);
		attachmentPane.setRenderable(r);
        if (reversiblePane.isVisible()) {
            reversiblePane.setRenderable(r);
        }
		needFirePropertyChange = true;
	}
	
	public void refresh() {
		if (r == null)
			return;
		generalPane.setRenderable(r);
		textPane.setRenderable(r);
		referencePane.setRenderable(r);
		attachmentPane.setRenderable(r);
        reversiblePane.setRenderable(r);
	}
    
    private class ReversiblePane extends RenderablePropertyPane {
        private JCheckBox isReversibleBox;
        
        public ReversiblePane() {
            init();
        }
        
        private void init() {
            isReversibleBox = new JCheckBox("Is Reversible Reaction");
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setBorder(BorderFactory.createEtchedBorder());
            add(isReversibleBox);
            isReversibleBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (duringSetting)
                        return;
                    doReversibleChange();
                }
            });
        }
        
        private void doReversibleChange() {
            if (reversiblePane.isVisible() && 
               (r instanceof ReactionNode || r instanceof RenderableReaction)) {
                boolean isReversible = isReversibleBox.isSelected();
                String booleanStr = (String) r.getAttributeValue(RenderablePropertyNames.IS_REVERSIBLE);
                if (booleanStr == null)
                    booleanStr = "false";
                if (!booleanStr.equals(isReversible + "")) {
                    r.setAttributeValue(RenderablePropertyNames.IS_REVERSIBLE,
                                        isReversible + "");
                    RenderableReaction reaction = null;
                    if (r instanceof RenderableReaction)
                        reaction = (RenderableReaction) r;
                    else if (r instanceof ReactionNode)
                        reaction = ((ReactionNode)r).getReaction();
                    reaction.setNeedInputArrow(isReversible);
                    r.setIsChanged(true);
                    fireRenderablePropertyChange(r, 
                                                 RenderablePropertyNames.IS_REVERSIBLE, 
                                                 Boolean.valueOf(booleanStr),
                                                 Boolean.valueOf(isReversible));
                }
            }
        }

        public void setRenderable(Renderable r) {
            duringSetting = true;
            super.setRenderable(r);
            if (r != null) {
                String isReversible = (String) r.getAttributeValue(RenderablePropertyNames.IS_REVERSIBLE);
                if (isReversible != null && isReversible.equals("true"))
                    isReversibleBox.setSelected(true);
                else
                    isReversibleBox.setSelected(false);
            }
            duringSetting = false;
        }
    }

}
