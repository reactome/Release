/*
 * Created on Apr 6, 2005
 *
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.model.GKInstance;

/**
 * A combination of InstanceListPane and AttributePane plus a JTextArea to set the subtitle.
 * This customized JPanel also contains buttons.
 * @author wgm
 *
 */
public class InstanceListAttributePane extends JPanel {

	private InstanceListPane instancePane;
	private AttributePane attributePane;
	private JTextArea titleArea;	
	private boolean isOKClicked = false;
	private JButton defaultBtn;
	private JPanel controlPane;
	
	public InstanceListAttributePane() {
	    this(false);
	}
	
	public InstanceListAttributePane(boolean needInput) {
	    init(needInput);
	}
	
	public void hideControlPane() {
	    controlPane.setVisible(false);
	}
	
	private void init(boolean needOkBtn) {
	    setLayout(new BorderLayout());
		titleArea = new JTextArea();
		titleArea.setLineWrap(true);
		titleArea.setWrapStyleWord(true);
		Border border1 = BorderFactory.createRaisedBevelBorder();
		Border border2 = BorderFactory.createEmptyBorder(4, 2, 4, 2);
		titleArea.setBorder(BorderFactory.createCompoundBorder(border1, border2));
		titleArea.setEditable(false);
		titleArea.setFont(titleArea.getFont().deriveFont(Font.BOLD));
		titleArea.setBackground(getBackground());
		add(titleArea, BorderLayout.NORTH);
		titleArea.setVisible(false); // Default is invisible.
		instancePane = new InstanceListPane();
		attributePane = new AttributePane();
		attributePane.setMinimumSize(new Dimension(20, 20));
		// Make them viewable as default.
		//instancePane.setIsViewable(false);
		//attributePane.setIsViewable(false);
		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
		                                instancePane,
		                                attributePane);
		jsp.setBorder(border1);
		jsp.setResizeWeight(0.5);
		jsp.setDividerLocation(300);                                
		add(jsp, BorderLayout.CENTER);
		controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton okBtn = null;
		if (needOkBtn) {
			okBtn = new JButton("OK");
			okBtn.setMnemonic('O');
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = true;
					dispose();
				}
			});
			controlPane.add(okBtn);
			okBtn.setDefaultCapable(true);
			defaultBtn = okBtn;
		}
		JButton closeBtn = new JButton("Close");
		if (needOkBtn) {
			closeBtn.setText("Cancel");
			okBtn.setPreferredSize(closeBtn.getPreferredSize());
		}
		else {
			closeBtn.setDefaultCapable(true);
			defaultBtn = closeBtn;
		}
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		closeBtn.setMnemonic('C');
		controlPane.add(closeBtn);
		add(controlPane, BorderLayout.SOUTH); 
		// Create links
		instancePane.addSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				java.util.List selectedInstances = instancePane.getSelection();
				if (selectedInstances != null && selectedInstances.size() == 1) {
					GKInstance instance = (GKInstance) selectedInstances.get(0);
					attributePane.setInstance(instance);
				}
				else
					attributePane.setInstance(null);
			}
		});                      
	}
	
	private void dispose() {
		Window window = SwingUtilities.getWindowAncestor(InstanceListAttributePane.this);
		if (window != null)
		    window.dispose();
	}
	
	public void setSubTitle(String title) {
		titleArea.setText(title);
		if (title == null || title.length() == 0)
			titleArea.setVisible(false);
		else
			titleArea.setVisible(true);
		titleArea.invalidate();
	}
	
	public boolean isOKClicked() {
		return isOKClicked;
	}
	
	public void setDisplayedInstances(java.util.List instances) {
		instancePane.setDisplayedInstances(instances);
		instancePane.setTitle("Instances: " + instances.size());
	}
	
	public InstanceListPane getInstanceListPane() {
	    return this.instancePane;
	}    
	
	public JButton getDefaultButton() {
	    return this.defaultBtn;
	}
    
	public void setEditable(boolean isEditable) {
	    instancePane.setEditable(isEditable);
	    attributePane.setEditable(isEditable);
	}
	
	public AttributePane getAttributePane() {
		return this.attributePane;
	}
}
