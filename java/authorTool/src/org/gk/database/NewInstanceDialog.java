/*
 * Created on Nov 24, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JDialog is used to create a new GKInstance object.
 * @author wugm
 */
public class NewInstanceDialog extends JDialog {
	// GUIs
	private JComboBox clsBox;
	protected AttributePane attributePane;
    // In case the action is cancelled to remove autocreated instances
    private List autoCreatedInstances;
	private boolean isOKClicked;
	
	public NewInstanceDialog() {
	}

	public NewInstanceDialog(JDialog parentDialog, String title) {
		super(parentDialog, title);
		init();
	}
	
	public NewInstanceDialog(JFrame parentFrame, String title) {
		super(parentFrame, title);
		init();
	}

	protected void init() {
		JPanel contentPane = createContentPane();
		getContentPane().add(contentPane, BorderLayout.CENTER);
		// Create links between GUIs
		installListeners();
	}
	
	protected JPanel createContentPane() {
	    JPanel contentPane = new JPanel();
	    contentPane.setLayout(new BorderLayout());
	    JPanel northPane = createTitlePane();
	    contentPane.add(northPane, BorderLayout.NORTH);
	    // Add instance pane
	    setUpAttributePane();
	    contentPane.add(attributePane, BorderLayout.CENTER);
	    // Add buttons
	    JPanel southPane = createControlPane();
	    contentPane.add(southPane, BorderLayout.SOUTH);
	    return contentPane;
	}

    protected JPanel createTitlePane() {
        JPanel northPane = new JPanel();
	    Border border1 = BorderFactory.createEtchedBorder();
	    Border border2 = BorderFactory.createEmptyBorder(4, 4, 4, 4);
	    northPane.setBorder(BorderFactory.createCompoundBorder(border1, border2));
	    northPane.setLayout(new BoxLayout(northPane, BoxLayout.X_AXIS));
	    JLabel label = new JLabel("Choose a schema class: ");
	    clsBox = new JComboBox();
	    clsBox.setRenderer(new SearchPane.SchemaClassListCellRenderer());
	    northPane.add(label);
	    northPane.add(clsBox);
        return northPane;
    }

    protected void setUpAttributePane() {
        attributePane = new AttributePane();
	    attributePane.setEditable(true);
	    attributePane.setBorder(BorderFactory.createRaisedBevelBorder());
	    attributePane.setTitle("Specify properties for the new instance:");
	    // Add a callback method to get any useful information
	    attributePane.getController().addPropertyChangeListener(new PropertyChangeListener() {
	        public void propertyChange(PropertyChangeEvent e) {
	            String propName = e.getPropertyName();
	            if (propName.equals("autoCreatedInstances")) {
	                autoCreatedInstances = (List) e.getNewValue();
	            }
	        }
	    });
    }

    protected JPanel createControlPane() {
        JPanel southPane = new JPanel();
        southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
	    JButton okBtn = new JButton("OK");
	    okBtn.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            isOKClicked = true;
	            attributePane.stopEditing();
	            dispose();
	            commit();
	        }
	    });
	    okBtn.setMnemonic('O');
	    JButton cancelBtn = new JButton("Cancel");
	    cancelBtn.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            isOKClicked = false;
	            // Call this method to keep the data integrity
	            cleanUp();
	            dispose();
	        }
	    });
	    cancelBtn.setMnemonic('C');
	    okBtn.setDefaultCapable(true);
	    okBtn.setPreferredSize(cancelBtn.getPreferredSize());
	    southPane.add(okBtn);
	    southPane.add(cancelBtn);
	    return southPane;
    }
	
	/**
	 * Am empty method for sub-class.
	 */
	protected void commit() {
	    
	}
	
	protected void installListeners() {
		clsBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
                     SchemaClass cls = (SchemaClass)clsBox.getSelectedItem();
					if (cls == null)
						return;
                     GKInstance preInstance = getNewInstance();
                     if (preInstance == null) {
                         XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
                         GKInstance instance = adaptor.createInstance(adaptor.getNextLocalID(), cls);
                         try {
                             setDefaultValues(instance);
                         }
                         catch(Exception e1) {
                             System.err.println("NewInstanceDialog.installListener(): " + e1);
                             e1.printStackTrace();
                         }
                         attributePane.setInstance(instance);
                         attributePane.setTitle("Specify properties for new instance:");
                     }
                     else {
                         // Change the type and copy all valid attributes
                         SchemaAttribute att = null;
                         String attName = null;
                         try {
                             for (Iterator it = preInstance.getSchemaAttributes().iterator(); it.hasNext();) {
                                 att = (SchemaAttribute) it.next();
                                 attName = att.getName();
                                 if (!cls.isValidAttribute(attName)) {
                                     preInstance.emptyAttributeValues(attName);
                                 }
                             }
                             preInstance.setSchemaClass(cls);
                             // Preset values might be overwrote by setDefaultValues()
                             setDefaultValues(preInstance);
                             attributePane.refresh();
                             attributePane.setTitle("Specify properties for new instance:");
                         }
                         catch(Exception e1) {
                             System.err.println("NewInstanceDialog.installListener(): " + e1);
                             e1.printStackTrace();
                         }
                     }
				}
			}
		});
	}
    
    private void setDefaultValues(GKInstance instance) throws Exception {
        instance.setDefaultValues();
        // Set the date value to a new InstanceEdit
        if (instance.getSchemClass().isa(ReactomeJavaConstants.InstanceEdit)) {
            String dateTime = GKApplicationUtilities.getDateTime();
            instance.setAttributeValue(ReactomeJavaConstants.dateTime,
                                       dateTime);
        }
    }
	
	public GKInstance getNewInstance() {
		return (GKInstance) attributePane.getInstance();
	}
	
	/**
	 * Clean up the referers for the newly created GKInstance. This method should
	 * be called if cancel is clicked in the dialog.
	 */ 
	public void cleanUp() {
		GKInstance newInstance = getNewInstance();
		try {
		    // Have to delete instances that are auto-generated
            // care about the local project only
            if ((autoCreatedInstances != null && autoCreatedInstances.size() > 0) &&
                (newInstance.getDbAdaptor() instanceof XMLFileAdaptor)) {
		        XMLFileAdaptor fileAdaptor = (XMLFileAdaptor) newInstance.getDbAdaptor();
                for (Iterator it = autoCreatedInstances.iterator(); it.hasNext();) {
                    fileAdaptor.deleteInstance((GKInstance) it.next());
                }
            }
		    // This is possible if there is an inverse attribute 
		    Map referers = newInstance.getReferers();
            for (Iterator it = referers.keySet().iterator(); it.hasNext();) {
		        SchemaAttribute att = (SchemaAttribute)it.next();
		        java.util.List instances = (java.util.List)referers.get(att);
		        if (instances != null && instances.size() > 0) {
		            for (Iterator it1 = instances.iterator(); it1.hasNext();) {
		                GKInstance referer = (GKInstance)it1.next();
		                java.util.List attValues = referer.getAttributeValuesList(att.getName());
		                attValues.remove(newInstance);
		                if (attValues.size() == 0) {
		                    referer.setAttributeValueNoCheck(att, null);
		                }
		            }
		        }
		    }
		    // This method is not useful anymore with XMLFileAdaptor.
//		    for (Iterator it = newInstance.getSchemClass().getAttributes().iterator(); it.hasNext();) {
//		    SchemaAttribute att = (SchemaAttribute) it.next();
//		    if (!att.isInstanceTypeAttribute())
//		    continue;
//		    java.util.List values = newInstance.getAttributeValuesList(att);
//		    if (values == null || values.size() == 0)
//		    continue;
//		    for (Iterator it1 = values.iterator(); it1.hasNext();) {
//		    GKInstance reference = (GKInstance) it1.next();
//		    reference.removeRefererNoCheck(att, newInstance);
//		    }
//		    }
		}
		catch (Exception e) {
			System.err.println("NewInstanceDialog.cleanUp(): " + e);
			e.printStackTrace();
		}
	}
	
	public boolean isOKClicked() {
		return this.isOKClicked;
	}
	
	/**
	 * Imports the data that will be operated upon.
	 * 
	 * @param schemaClasses All known classes
	 * @param selectedClass The user selected class
	 * 
	 */
	public void setSchemaClasses(java.util.List schemaClasses, SchemaClass selectedClass) {
		clsBox.removeAllItems();
        ItemListener[] listeners = clsBox.getItemListeners();
        // Disable 
        for (int i = 0; i < listeners.length; i++)
            clsBox.removeItemListener(listeners[i]);
        if (schemaClasses == null || schemaClasses.size() == 0)
			return ;
		for (Iterator it = schemaClasses.iterator(); it.hasNext();) {
			SchemaClass cls = (SchemaClass) it.next();
			if (!cls.isAbstract())
				clsBox.addItem(cls);
		}
        // Deselect
        clsBox.setSelectedIndex(-1);
        // Enable
        for (int i =0; i < listeners.length; i++)
            clsBox.addItemListener(listeners[i]);
		if (selectedClass == null)
			clsBox.setSelectedIndex(0);
		else
			clsBox.setSelectedItem(selectedClass);
	}
}
