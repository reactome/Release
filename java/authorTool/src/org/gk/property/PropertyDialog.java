/*
 * Created on Jul 1, 2003
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.util.BrowserLauncher;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized JDialog used as a super class for other property dialogs to edit
 * properties.
 * @author wgm
 */
public abstract class PropertyDialog extends JDialog {
    // Control Buttons
	protected JButton okBtn;
	protected JButton cancelBtn;
    protected JButton browseDBBtn;
	// For tab
	protected JTabbedPane tabbedPane;
	protected Renderable renderable;
	// The reply
	private boolean isOKClicked;
	// To control if name checking is needed
	protected boolean needNameChecking = true;
    // Common properties GUIs
    protected GeneralPropertyPane generalPropPane;
    private JLabel reactomeIdLabel;
    protected AlternativeNameListPane alternativeNamePane;
    private JButton browseBtn;
	
	public PropertyDialog() {
		super();
		init();
	}
	
	public PropertyDialog(JFrame ownerFrame) {
		super(ownerFrame);
		init();
	}
	
	public PropertyDialog(JDialog ownerDialog) {
		super(ownerDialog);
		init();
	}
	
	public PropertyDialog(JFrame ownerFrame, Renderable entity) {
		this(ownerFrame);
		setRenderable(entity);
	}
	
	public void setRenderable(Renderable renderable) {
		this.renderable = renderable;
         setTitle(renderable.getType() + " Properties: " + renderable.getDisplayName());
         generalPropPane.setRenderable(renderable);
         // Reactome ID
         Long reactomeId = renderable.getReactomeId();
         if (reactomeId != null) {
             reactomeIdLabel.setText(reactomeId.toString());
             browseBtn.setEnabled(true);
         }
         else {
             reactomeIdLabel.setText("Unknown");
             browseBtn.setEnabled(false);
         }
         // Alternative names
         if (alternativeNamePane != null) {
             alternativeNamePane.setRenderable(renderable);
         }
    }
    
    public void refresh() {
        setRenderable(this.renderable);
    }
	
	public Renderable getRenderable() {
		return this.renderable;
	}
	
	private void init() {
	    JPanel controlPane = new JPanel();
	    controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 8));
	    browseDBBtn = new JButton("Browse DB");
	    browseDBBtn.setMnemonic('B');
        controlPane.add(browseDBBtn);
        // Hide browsebtn as default
        browseDBBtn.setVisible(false);
	    okBtn = new JButton("OK");
	    okBtn.setActionCommand("ok");
	    okBtn.setMnemonic('O');
	    cancelBtn = new JButton("Cancel");
	    cancelBtn.setActionCommand("cancel");
	    cancelBtn.setMnemonic('C');
	    ActionListener l = new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            String command = e.getActionCommand();
	            if (command.equals("ok")) {
	                if(commit()){
	                    dispose();
	                }
	            }
	            else {
	                isOKClicked = false;
	                dispose();		
	            }		
	        }
	    };
	    okBtn.addActionListener(l);
	    cancelBtn.addActionListener(l);
	    okBtn.setPreferredSize(cancelBtn.getPreferredSize());
	    controlPane.add(okBtn);
	    controlPane.add(cancelBtn);
	    okBtn.setDefaultCapable(true);
	    getRootPane().setDefaultButton(okBtn);
	    getContentPane().add(controlPane, BorderLayout.SOUTH);
	    tabbedPane = new JTabbedPane();
	    getContentPane().add(tabbedPane, BorderLayout.CENTER);
	    JPanel requiredPropPane = createRequiredPropPane();
	    tabbedPane.add("Required", requiredPropPane);
	    JPanel optionalPropPane = createOptionalPropPane();
	    tabbedPane.add("Optional", optionalPropPane);
	    // Have to be modal
	    setModal(true);
	    // Make the display name field as the first focus component
	    addWindowListener(new WindowAdapter() {
	        public void windowOpened(WindowEvent e) {
	            if (generalPropPane != null) {
	                JTextField tf = generalPropPane.getDisplayNameField();
	                tf.selectAll();
	                tf.requestFocus();
	            }
	        }
	    });
	}
    
    public JButton getBrowseDBButton() {
        return this.browseDBBtn;
    }
    
    protected abstract JPanel createRequiredPropPane();
    
    protected abstract JPanel createOptionalPropPane();
	
	/**
	 * Select a tab for a specified property. Do nothing in the default implementation.
	 * @param propertyName the name of the propery.
	 */
	public void selectTabForProperty(String propertyName) {
	}
	
	protected boolean commit() {
	    isOKClicked = true;
	    boolean rtn = generalPropPane.commit();
	    if (!rtn) {
	        tabbedPane.setSelectedIndex(0);
	        generalPropPane.getDisplayNameField().requestFocus();
	        return rtn;
	    }
	    if (alternativeNamePane != null) {
            java.util.List names = alternativeNamePane.getValues();
            setListAttributeValue(names, "names");
	    }
	    return true;
	}
    
    protected GeneralPropertyPane createGeneralPropPane() {
        return new GeneralPropertyPane();
    }

	public void addTab(String title, JPanel tab) {
		tabbedPane.add(title, tab);
	}
	
	public boolean isOKClicked() {
		return isOKClicked;
	}
    
    protected JPanel createReactomeIDPane() {
        JPanel reactomeIdPane = new JPanel();
        reactomeIdPane.setLayout(new BorderLayout());
        JPanel idPane = new JPanel();
        idPane.setLayout(new FlowLayout(FlowLayout.LEADING));
        JLabel label = new JLabel("Reactome ID:");
        reactomeIdLabel = new JLabel("Unknown");
        idPane.add(label);
        idPane.add(reactomeIdLabel);
        reactomeIdPane.add(idPane, BorderLayout.WEST);
        browseBtn = new JButton("View in Reactome", 
                GKApplicationUtilities.createImageIcon(getClass(), "WebComponent16.gif"));
        browseBtn.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) { 
               String url = GKApplicationUtilities.REACTOME_INSTANCE_URL;
               String id = reactomeIdLabel.getText();
               if (id == null || id.length() == 0)
                   return;
               try {
                   BrowserLauncher.displayURL(url + id, PropertyDialog.this);
               } 
               catch (IOException e1) {
                   System.err.println("PropertyDialog.createReactomeIDPane(): " + e1);
                   e1.printStackTrace();
               }
           }
        });
        reactomeIdPane.add(browseBtn, BorderLayout.EAST);
        return reactomeIdPane;
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
    
    /**
     * A refactor method to set a attribute value that is a List.
     * @param newValues
     */
    protected void setListAttributeValue(List newValues, String attName) {
        List oldValues = (List) renderable.getAttributeValue(attName);
        if ((oldValues == null && newValues != null && newValues.size() > 0) ||
            (oldValues != null && !oldValues.equals(newValues))) { 
            renderable.setAttributeValue(attName, newValues);
            renderable.setIsChanged(true);
        }
    }
}
