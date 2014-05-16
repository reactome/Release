package org.gk.database;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;
import org.gk.util.AuthorToolAppletUtilities;

/**
 * A customized TreeCellRenderer for display Event GKInstance.
 * @author wugm
 */
public class EventCellRenderer extends JPanel implements TreeCellRenderer {
	protected JLabel textLabel;
	private JLabel icon1Label;
	private JLabel icon2Label;
	private JLabel icon3Label;
	protected Icon pathwayIcon;
	protected Icon reactionIcon; 
	protected Icon genericIcon; 
	protected Icon concreteIcon;
	protected Icon conceptualEventIcon;
	protected Icon equivalentEventSetIcon;
    protected Icon blackboxEventIcon;
    protected Icon polymerizationIcon;
    private Icon depolymerizationIcon;
    private Icon failedReactionIcon;
    // These are for _doNotRelease property
    private JLabel dnrLabel;
    private ImageIcon selectedIcon;
    private ImageIcon nonselectedIcon;
	private boolean needReleaseIcon;
    private Border selectionBorder;
	private Border lineBorder;
	// For icons
	private Map node2Icon = new HashMap();
	// Used to display for ELV
	private boolean isGrayOn;

	/**
	 * Default constructor. The value in _doNotRelease will not be displayed.
	 *
	 */
	public EventCellRenderer() {
	    this(false);
	}
	
	/**
	 * An overloaded constructor. If needReleaseIcon set to true, an event with
	 * _doNotRelease value false will be checked, with _doNotRelease value null 
	 * or true will not be checked. 
	 * @param needReleaseIcon true for displaying value in _doNotRelease slot while
	 * false for not displaying.
	 */
	public EventCellRenderer(boolean needReleaseIcon) {
	    this.needReleaseIcon = needReleaseIcon;
		initIcons();
		initGUIs();
	}
	
	private void initGUIs() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		if (needReleaseIcon) {
	        dnrLabel = new JLabel();
	        add(dnrLabel);
		}
		icon1Label = new JLabel();
		icon2Label = new JLabel();
		textLabel = new JLabel();
		textLabel.setFont(UIManager.getFont("Tree.font"));
		textLabel.setOpaque(true);
		icon3Label = new JLabel();
		add(icon1Label);
		add(icon2Label);
		add(textLabel);
		add(icon3Label);
		// Commont properties
		setFont(UIManager.getFont("Tree.font"));
		//setBackground(UIManager.getColor("Tree.background"));
		setOpaque(false);
		selectionBorder = BorderFactory.createLineBorder(UIManager.getColor("Tree.selectionBorderColor"));
		lineBorder = BorderFactory.createLineBorder(UIManager.getColor("Tree.background"));	    	    
	}
	
	protected void initIcons() {
		pathwayIcon = AuthorToolAppletUtilities.createImageIcon("Pathway.gif");
		reactionIcon = AuthorToolAppletUtilities.createImageIcon("Reaction.gif");
		genericIcon = AuthorToolAppletUtilities.createImageIcon("Generic.gif");
		concreteIcon = AuthorToolAppletUtilities.createImageIcon("Concrete.gif");
		equivalentEventSetIcon = AuthorToolAppletUtilities.createImageIcon("EquivalentEventSet.gif");
		conceptualEventIcon = AuthorToolAppletUtilities.createImageIcon("ConceptualEvent.gif");
        blackboxEventIcon = AuthorToolAppletUtilities.createImageIcon("BlackboxEvent.gif");
        polymerizationIcon = AuthorToolAppletUtilities.createImageIcon("Ploymerization.gif");
        depolymerizationIcon = AuthorToolAppletUtilities.createImageIcon("Deploymerization.gif");
        failedReactionIcon = AuthorToolAppletUtilities.createImageIcon("FailedReaction.gif");
		if (needReleaseIcon) {
	        selectedIcon = AuthorToolAppletUtilities.createImageIcon("Selected.png");
	        nonselectedIcon = AuthorToolAppletUtilities.createImageIcon("Unselected.png");
		}
	}
	
	public void setNode2IconMap(Map map) {
		this.node2Icon = map;
	}

	/**
	 * This flag is used for the entity-level view.
	 * @param isOn
	 */
	public void enableGrayOn(boolean isOn) {
	    this.isGrayOn = isOn;
	}
	
	public Component getTreeCellRendererComponent(
		JTree tree,
		Object value,
		boolean sel,
		boolean expanded,
		boolean leaf,
		int row,
		boolean hasFocus) {
		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
		GKInstance event = (GKInstance)treeNode.getUserObject();
		// Border
		if (sel) {
			textLabel.setBorder(selectionBorder);
			textLabel.setBackground(UIManager.getColor("Tree.selectionBackground"));
			textLabel.setForeground(UIManager.getColor("Tree.selectionForeground"));
		}
		else {
			textLabel.setBorder(lineBorder);
			textLabel.setBackground(UIManager.getColor("Tree.textBackground"));
			textLabel.setForeground(UIManager.getColor("Tree.textForeground"));
		}
		if (event != null) {
			if (event.isDirty())
				textLabel.setText(">" + event.getDisplayName());
			else
				textLabel.setText(event.getDisplayName());
			if (isGrayOn) {
			    Boolean isOnElv = (Boolean) event.getAttributeValueNoCheck("isOnElv");
			    if (isOnElv == null || !isOnElv)
			        textLabel.setForeground(Color.GRAY);
			    else
			        textLabel.setForeground(Color.BLACK);
			}
			SchemaClass schemaClass = event.getSchemClass();
			// Set Icon2
			if (schemaClass.isa("Reaction"))
				icon2Label.setIcon(reactionIcon);
			else if (schemaClass.isa("Pathway"))
				icon2Label.setIcon(pathwayIcon);
			else if (schemaClass.isa("ConceptualEvent"))
			    icon2Label.setIcon(conceptualEventIcon);
			else if (schemaClass.isa("EquivalentEventSet"))
			    icon2Label.setIcon(equivalentEventSetIcon);
            else if (schemaClass.isa(ReactomeJavaConstants.BlackBoxEvent))
                icon2Label.setIcon(blackboxEventIcon);
            else if (schemaClass.isa(ReactomeJavaConstants.Polymerisation))
                icon2Label.setIcon(polymerizationIcon);
            else if (schemaClass.isa(ReactomeJavaConstants.Depolymerisation))
                icon2Label.setIcon(depolymerizationIcon);
            else if (schemaClass.isa(ReactomeJavaConstants.FailedReaction))
                icon2Label.setIcon(failedReactionIcon);
			else
				icon2Label.setIcon(null);
			// Set Icon1
			if (node2Icon.containsKey(treeNode))
				icon1Label.setIcon((ImageIcon)node2Icon.get(treeNode));
			else
				icon1Label.setIcon(null);
			// Set icon3
			if (schemaClass.isa("GenericEvent"))
				icon3Label.setIcon(genericIcon);
			else if (schemaClass.isa("ConcreteEvent"))
				icon3Label.setIcon(concreteIcon);
			if (needReleaseIcon) {
			    if (event.isShell())
			        dnrLabel.setVisible(false);
			    else {
			        dnrLabel.setVisible(true);
			        // Check _doNotRelease. But not shell instances!
			        try {
                        boolean needRelease = false;
                        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants._doRelease)) {
                            Boolean doRelease = (Boolean) event.getAttributeValue(ReactomeJavaConstants._doRelease);
                            if (doRelease != null)
                                needRelease = doRelease.booleanValue();
                        }
                        else if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants._doNotRelease)) {
                            // For old schema
                            Boolean doNotRelease = (Boolean) event.getAttributeValue(ReactomeJavaConstants._doNotRelease);
                            if (doNotRelease != null)
                                needRelease = !doNotRelease.booleanValue();
                        }
			            if (needRelease)
			                dnrLabel.setIcon(selectedIcon);
			            else
			                dnrLabel.setIcon(nonselectedIcon);
			        }
			        catch(Exception e) {
			            System.err.println("EventCellRenderer.getTreecellRendererComponent(): " + e);
			            e.printStackTrace();
			        }
			    }
			}
		}
		else {
			textLabel.setText("");
			icon1Label.setIcon(null);
			icon2Label.setIcon(null);
		}
		// Have to call this method to make paint correctly.
		invalidate();
		return this;
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void repaint(long tm, int x, int y, int width, int height) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void repaint(Rectangle r) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		// Strings get interned...
		if (propertyName == "text")
			super.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, char oldValue, char newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, short oldValue, short newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, int oldValue, int newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, long oldValue, long newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, float oldValue, float newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, double oldValue, double newValue) {
	}

	/**
	 * Overridden for performance reasons.
	 */
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
	}
}