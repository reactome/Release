/*
 * Created on May 27, 2004
 */
package org.gk.util;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * A customized JPanel contains OK and Cancel buttons to control a JDialog.
 * @author wugm
 */
public class DialogControlPane extends JPanel {
	private JButton okBtn;
	private JButton cancelBtn;

	public DialogControlPane() {
		init();
	}
	
	private void init() {
		setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		okBtn = new JButton("OK");
		okBtn.setMnemonic('O');
		okBtn.setDefaultCapable(true);
		add(okBtn);
		cancelBtn = new JButton("Cancel");
		cancelBtn.setMnemonic('C');
		add(cancelBtn);
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
	}
	
	public JButton getOKBtn() {
		return this.okBtn;
	}
	
	public JButton getCancelBtn() {
		return this.cancelBtn;
	}

}
