/*
 * Created on Dec 1, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * A customized JDialog to display a list of GKInstances.
 * @author wugm
 */
public class InstanceListDialog extends JDialog {
	private InstanceListAttributePane contentPane;
	
	public InstanceListDialog(String title, boolean needInput) {
		super();
		setTitle(title);
		init(needInput);
	}
	
	public InstanceListDialog(JFrame parentFrame, String title) {
		this(parentFrame, title, false);
	}
	
	public InstanceListDialog(JFrame parentFrame, String title, boolean needInput) {
		super(parentFrame, title);
		init(needInput);
	}
	
	public InstanceListDialog(JDialog parentDialog, String title, boolean needInput) {
		super(parentDialog, title);
		init(needInput);
	}
	
	private void init(boolean needOkBtn) {
	    contentPane = new InstanceListAttributePane(needOkBtn);
	    getContentPane().add(contentPane, BorderLayout.CENTER);
	    getRootPane().setDefaultButton(contentPane.getDefaultButton());
	}
	
	public void setSubTitle(String title) {
		contentPane.setSubTitle(title);
		getContentPane().validate();
	}
	
	public boolean isOKClicked() {
		return contentPane.isOKClicked();
	}
	
	/**
	 * Override the super class method. InstanceListPane and AttributePane should
	 * not be viewable if the dialog is modal.
	 */
	public void setModal(boolean isModal) {
		super.setModal(isModal);
		contentPane.getInstanceListPane().setIsViewable(!isModal);
	}
	
	public void setDisplayedInstances(java.util.List instances) {
		contentPane.setDisplayedInstances(instances);
	}
	
	public InstanceListPane getInstanceListPane() {
	    return contentPane.getInstanceListPane();
	}
}
