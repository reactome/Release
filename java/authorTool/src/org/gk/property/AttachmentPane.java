/*
 * Created on Oct 20, 2003
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.util.AuthorToolAppletUtilities;

/**
 * This customized JPanel is used to edit attachments.
 * @author wgm
 */
public class AttachmentPane extends RenderablePropertyPane {
	private JLabel titleLabel;
	private JList fileList;
	
	public AttachmentPane() {
		init();
	}
	
	public AttachmentPane(Renderable renderable) {
		init();
		setRenderable(renderable);
	}
	
	public void setRenderable(Renderable r) {
		super.setRenderable(r);
        duringSetting = true;
        DefaultListModel model = (DefaultListModel) fileList.getModel();
        model.clear();
        if (r != null) {
            java.util.List attachments = (java.util.List) r.getAttributeValue(RenderablePropertyNames.ATTACHMENT);
            if (attachments != null) {
                for (Iterator it = attachments.iterator(); it.hasNext();)
                    model.addElement(it.next());
            }
        }
        duringSetting = false;
		//titleLabel.setText("Attachments for " + r.getDisplayName());
	}
	
	private void init() {
		setLayout(new BorderLayout());
		JPanel northPane = new JPanel();
		northPane.setLayout(new BorderLayout());
		titleLabel = new JLabel("Image Attachments");
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		northPane.add(titleLabel, BorderLayout.WEST);
		JToolBar toolbar = new JToolBar();
		toolbar.setRollover(true);
		toolbar.setFloatable(false);
		Dimension btnSize = new Dimension(20, 20);
		JButton addBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Add16.gif"));
		addBtn.setPreferredSize(btnSize);
		addBtn.setActionCommand("add");
		addBtn.setToolTipText("Click to attach an image file");
		final JButton removeBtn = new JButton(AuthorToolAppletUtilities.createImageIcon("Remove16.gif"));
		removeBtn.setPreferredSize(btnSize);
		removeBtn.setActionCommand("remove");
		removeBtn.setToolTipText("Click to remove the selected file(s)");
		toolbar.add(addBtn);
		toolbar.add(removeBtn);
		addBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnAction(e);
			}
		});
		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnAction(e);
			}
		});
		removeBtn.setEnabled(false);
		northPane.add(toolbar, BorderLayout.EAST);
        add(northPane, BorderLayout.NORTH);
		fileList = new JList();
		fileList.setModel(new DefaultListModel());
		add(new JScrollPane(fileList), BorderLayout.CENTER);
		// Add selection listner
		fileList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (fileList.getSelectedIndex() >= 0) {
					removeBtn.setEnabled(true);
				}
				else {
					removeBtn.setEnabled(false);
				}
			}
		});
		// To catch changes
		fileList.getModel().addListDataListener(new ListDataListener() {
			public void intervalAdded(ListDataEvent e) {
				doListChange();
			}
			public void intervalRemoved(ListDataEvent e) {
				doListChange();
			}
			public void contentsChanged(ListDataEvent e) {
				doListChange();
			} 
		});
	}
	
	private void doListChange() {
        if (duringSetting)
            return;
		java.util.List oldValues = (java.util.List) r.getAttributeValue(RenderablePropertyNames.ATTACHMENT);
		java.util.List newValues = getAttachments();
		fireRenderablePropertyChange(r, 
                                     RenderablePropertyNames.ATTACHMENT,
		                             oldValues, 
                                     newValues);
	}
	
	private void btnAction(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("add")) {
			addAttachment();
		}
		else if (command.equals("remove")) {
			Object[] selections = fileList.getSelectedValues();
			DefaultListModel model = (DefaultListModel) fileList.getModel();
			for (int i = 0; i < selections.length; i++)
				model.removeElement(selections[i]);
		}
	}
    
	private void addAttachment() {
	    if (AuthorToolAppletUtilities.isInApplet) {
	        String message = "Please enter the file name for your image attachment. \n" + 
	        "Please email your image file to the Reactome curator.";
	        String input = JOptionPane.showInputDialog(this,
	                                                   message,
	                                                   "Add Image Attachment",
	                                                   JOptionPane.INFORMATION_MESSAGE);
	        if (input == null)
	            return;
	        input = input.trim();
	        DefaultListModel model = (DefaultListModel) fileList.getModel();
	        model.addElement(input);
	    }
	    else {
	        JFileChooser chooser = new JFileChooser();
	        chooser.setDialogTitle("Choose an Attachment");
	        FileFilter imageFilter = new FileFilter() {
	            public boolean accept(File file) {
	                if (file.isDirectory())
	                    return true;
	                String name = file.getName();
	                name = name.toLowerCase();
	                if (name.endsWith(".jpg") ||
	                        name.endsWith(".jpeg") ||
	                        name.endsWith(".gif") ||
	                        name.endsWith(".png"))
	                    return true;
	                return false;
	            }
	            public String getDescription() {
	                return "Image Files (*.jpg, *.jpeg, *.gif, *.png)";
	            }
	        };
	        chooser.addChoosableFileFilter(imageFilter);
	        chooser.setFileFilter(imageFilter);
	        int reply = chooser.showOpenDialog(this);
	        if (reply == JFileChooser.APPROVE_OPTION) {
	            File file = chooser.getSelectedFile();
	            DefaultListModel model = (DefaultListModel) fileList.getModel();
	            String fileName = file.toString();
	            model.addElement(fileName);
                // Add a information
                JOptionPane.showMessageDialog(this,
                                              "Please don't forget to email your image file to the Reactome curator.", 
                                              "Reminding",
                                              JOptionPane.INFORMATION_MESSAGE);
	        }
	    }
	}
	
	public void setTitle(String title) {
		titleLabel.setText(title);
	}
	
	public java.util.List getAttachments() {
		DefaultListModel model = (DefaultListModel) fileList.getModel();
		if (model.getSize() == 0)
			return null;
		java.util.List list = new ArrayList(model.getSize());
		for (int i = 0; i < model.getSize(); i++)
			list.add(model.get(i));
			return list;
	}
}
